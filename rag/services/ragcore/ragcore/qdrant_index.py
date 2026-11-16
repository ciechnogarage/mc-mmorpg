"""Qdrant persistence and hybrid Query API adapters.

The real adapter uses Qdrant's universal Query API with two named-vector
prefetches and server-side reciprocal-rank fusion.  API reference:
https://python-client.qdrant.tech/qdrant_client.qdrant_client#qdrant_client.qdrant_client.QdrantClient.query_points
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Protocol, Sequence

from . import contract


@dataclass(frozen=True)
class IndexPoint:
    id: str
    dense: list[float]
    sparse: dict[str, list]
    payload: dict[str, Any]


@dataclass(frozen=True)
class HybridHit:
    id: str
    payload: dict[str, Any]
    fusion_score: float


InventoryEntry = tuple[tuple[str, int, str, str], ...]


class VectorIndex(Protocol):
    def ensure_collections(self) -> None:
        ...

    def replace_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
        points: Sequence[IndexPoint],
    ) -> None:
        ...

    def delete_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> None:
        ...

    def delete_stale_paths(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        live_paths: set[str],
    ) -> int:
        ...

    def hybrid_query(
        self,
        collection: str,
        dense: list[float],
        sparse: dict[str, list],
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        embedding_model: str,
        embedding_model_revision: str,
        limit: int,
        prefetch_limit: int,
    ) -> list[HybridHit]:
        ...

    def snapshot_inventory(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> dict[tuple[str, str], InventoryEntry]:
        ...

    def snapshot_embedding_profiles(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> set[tuple[str, str]]:
        ...

    def load_path_points(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> list[IndexPoint]:
        ...


def _snapshot_matches(
    payload: dict[str, Any],
    repo: str,
    commit_sha: str,
    worktree_state: str,
    snapshot_id: str,
) -> bool:
    return (
        payload.get("repo") == repo
        and payload.get("commit_sha") == commit_sha
        and payload.get("worktree_state") == worktree_state
        and payload.get("snapshot_id") == snapshot_id
    )


class InMemoryVectorIndex:
    """Behavioral fake used by tests; mirrors replace/delete semantics."""

    def __init__(self) -> None:
        self.collections: dict[str, dict[str, IndexPoint]] = {}
        self.replace_calls = 0

    def ensure_collections(self) -> None:
        for name in contract.COLLECTION_NAMES:
            self.collections.setdefault(name, {})

    def replace_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
        points: Sequence[IndexPoint],
    ) -> None:
        self.ensure_collections()
        bucket = self.collections[collection]
        changed = False
        for point_id, point in list(bucket.items()):
            if _snapshot_matches(
                point.payload, repo, commit_sha, worktree_state, snapshot_id
            ) and point.payload.get("path") == path:
                del bucket[point_id]
                changed = True
        bucket.update({point.id: point for point in points})
        if points or changed:
            self.replace_calls += 1

    def delete_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> None:
        self.ensure_collections()
        bucket = self.collections[collection]
        stale = [
            point_id
            for point_id, point in bucket.items()
            if _snapshot_matches(
                point.payload, repo, commit_sha, worktree_state, snapshot_id
            )
            and point.payload.get("path") == path
        ]
        for point_id in stale:
            del bucket[point_id]

    def delete_stale_paths(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        live_paths: set[str],
    ) -> int:
        self.ensure_collections()
        bucket = self.collections[collection]
        stale = [
            point_id
            for point_id, point in bucket.items()
            if _snapshot_matches(
                point.payload, repo, commit_sha, worktree_state, snapshot_id
            )
            and point.payload.get("path") not in live_paths
        ]
        for point_id in stale:
            del bucket[point_id]
        return len(stale)

    def hybrid_query(
        self,
        collection: str,
        dense: list[float],
        sparse: dict[str, list],
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        embedding_model: str,
        embedding_model_revision: str,
        limit: int,
        prefetch_limit: int,
    ) -> list[HybridHit]:
        del prefetch_limit
        wanted = dict(zip(sparse["indices"], sparse["values"]))
        ranked: list[HybridHit] = []
        for point in self.collections.get(collection, {}).values():
            if not _snapshot_matches(
                point.payload, repo, commit_sha, worktree_state, snapshot_id
            ):
                continue
            if (
                point.payload.get("embedding_model") != embedding_model
                or point.payload.get("embedding_model_revision")
                != embedding_model_revision
            ):
                continue
            dense_score = sum(a * b for a, b in zip(dense, point.dense))
            sparse_score = sum(
                wanted.get(index, 0.0) * value
                for index, value in zip(point.sparse["indices"], point.sparse["values"])
            )
            ranked.append(HybridHit(point.id, point.payload, dense_score + sparse_score))
        return sorted(ranked, key=lambda hit: hit.fusion_score, reverse=True)[:limit]

    def snapshot_inventory(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> dict[tuple[str, str], InventoryEntry]:
        grouped: dict[tuple[str, str], list[tuple[str, int, str, str]]] = {}
        for collection, bucket in self.collections.items():
            for point_id, point in bucket.items():
                if not _snapshot_matches(
                    point.payload, repo, commit_sha, worktree_state, snapshot_id
                ):
                    continue
                key = (collection, str(point.payload["path"]))
                grouped.setdefault(key, []).append(
                    (
                        str(point_id),
                        int(point.payload.get("chunk_index", -1)),
                        str(point.payload.get("content_hash", "")),
                        str(point.payload.get("document_hash", "")),
                    )
                )
        return {key: tuple(sorted(items, key=lambda item: (item[1], item[0]))) for key, items in grouped.items()}

    def snapshot_embedding_profiles(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> set[tuple[str, str]]:
        return {
            (
                str(point.payload.get("embedding_model", "")),
                str(point.payload.get("embedding_model_revision", "")),
            )
            for bucket in self.collections.values()
            for point in bucket.values()
            if _snapshot_matches(
                point.payload, repo, commit_sha, worktree_state, snapshot_id
            )
        }

    def load_path_points(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> list[IndexPoint]:
        return sorted(
            [
                point
                for point in self.collections.get(collection, {}).values()
                if _snapshot_matches(
                    point.payload, repo, commit_sha, worktree_state, snapshot_id
                )
                and point.payload.get("path") == path
            ],
            key=lambda point: (int(point.payload.get("chunk_index", -1)), point.id),
        )


class QdrantVectorIndex:
    """Thin adapter around qdrant-client 1.18.0."""

    def __init__(self, url: str, dense_dim: int = contract.DENSE_DIM) -> None:
        from qdrant_client import QdrantClient

        self.client = QdrantClient(url=url)
        self.dense_dim = dense_dim

    @staticmethod
    def _filter(
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str | None = None,
        embedding_model: str | None = None,
        embedding_model_revision: str | None = None,
    ):
        from qdrant_client import models

        exact = {
            "repo": repo,
            "commit_sha": commit_sha,
            "worktree_state": worktree_state,
            "snapshot_id": snapshot_id,
        }
        if path is not None:
            exact["path"] = path
        if embedding_model is not None:
            exact["embedding_model"] = embedding_model
        if embedding_model_revision is not None:
            exact["embedding_model_revision"] = embedding_model_revision
        return models.Filter(
            must=[
                models.FieldCondition(key=key, match=models.MatchValue(value=value))
                for key, value in exact.items()
            ]
        )

    def ensure_collections(self) -> None:
        from qdrant_client import models

        existing = {item.name for item in self.client.get_collections().collections}
        for name in contract.COLLECTION_NAMES:
            if name not in existing:
                self.client.create_collection(
                    collection_name=name,
                    vectors_config={
                        contract.DENSE_VECTOR: models.VectorParams(
                            size=self.dense_dim, distance=models.Distance.COSINE
                        )
                    },
                    sparse_vectors_config={
                        contract.SPARSE_VECTOR: models.SparseVectorParams()
                    },
                )
            for field in (
                "repo",
                "commit_sha",
                "worktree_state",
                "snapshot_id",
                "path",
                "evidence_kind",
                "embedding_model",
                "embedding_model_revision",
            ):
                self.client.create_payload_index(
                    collection_name=name,
                    field_name=field,
                    field_schema=models.PayloadSchemaType.KEYWORD,
                    wait=True,
                )

    def _point_ids(self, collection: str, query_filter) -> list[Any]:
        ids: list[Any] = []
        offset = None
        while True:
            records, offset = self.client.scroll(
                collection_name=collection,
                scroll_filter=query_filter,
                limit=256,
                offset=offset,
                with_payload=False,
                with_vectors=False,
            )
            ids.extend(record.id for record in records)
            if offset is None:
                return ids

    def _delete_ids(self, collection: str, ids: Sequence[Any]) -> None:
        if not ids:
            return
        from qdrant_client import models

        self.client.delete(
            collection_name=collection,
            points_selector=models.PointIdsList(points=list(ids)),
            wait=True,
        )

    def replace_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
        points: Sequence[IndexPoint],
    ) -> None:
        from qdrant_client import models

        old_ids = self._point_ids(
            collection,
            self._filter(repo, commit_sha, worktree_state, snapshot_id, path),
        )
        new_ids = {point.id for point in points}
        self._delete_ids(collection, [point_id for point_id in old_ids if str(point_id) not in new_ids])
        if points:
            self.client.upsert(
                collection_name=collection,
                points=[
                    models.PointStruct(
                        id=point.id,
                        vector={
                            contract.DENSE_VECTOR: point.dense,
                            contract.SPARSE_VECTOR: models.SparseVector(**point.sparse),
                        },
                        payload=point.payload,
                    )
                    for point in points
                ],
                wait=True,
            )

    def delete_path(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> None:
        ids = self._point_ids(
            collection,
            self._filter(repo, commit_sha, worktree_state, snapshot_id, path),
        )
        self._delete_ids(collection, ids)

    def delete_stale_paths(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        live_paths: set[str],
    ) -> int:
        query_filter = self._filter(repo, commit_sha, worktree_state, snapshot_id)
        stale: list[Any] = []
        offset = None
        while True:
            records, offset = self.client.scroll(
                collection_name=collection,
                scroll_filter=query_filter,
                limit=256,
                offset=offset,
                with_payload=["path"],
                with_vectors=False,
            )
            stale.extend(record.id for record in records if record.payload.get("path") not in live_paths)
            if offset is None:
                break
        self._delete_ids(collection, stale)
        return len(stale)

    def hybrid_query(
        self,
        collection: str,
        dense: list[float],
        sparse: dict[str, list],
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        embedding_model: str,
        embedding_model_revision: str,
        limit: int,
        prefetch_limit: int,
    ) -> list[HybridHit]:
        from qdrant_client import models

        exact_filter = self._filter(
            repo,
            commit_sha,
            worktree_state,
            snapshot_id,
            embedding_model=embedding_model,
            embedding_model_revision=embedding_model_revision,
        )
        response = self.client.query_points(
            collection_name=collection,
            prefetch=[
                models.Prefetch(
                    query=dense,
                    using=contract.DENSE_VECTOR,
                    filter=exact_filter,
                    limit=prefetch_limit,
                ),
                models.Prefetch(
                    query=models.SparseVector(**sparse),
                    using=contract.SPARSE_VECTOR,
                    filter=exact_filter,
                    limit=prefetch_limit,
                ),
            ],
            query=models.FusionQuery(fusion=models.Fusion.RRF),
            query_filter=exact_filter,
            limit=limit,
            with_payload=True,
            with_vectors=False,
        )
        return [
            HybridHit(str(point.id), dict(point.payload or {}), float(point.score))
            for point in response.points
        ]

    def snapshot_inventory(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> dict[tuple[str, str], InventoryEntry]:
        grouped: dict[tuple[str, str], list[tuple[str, int, str, str]]] = {}
        query_filter = self._filter(repo, commit_sha, worktree_state, snapshot_id)
        for collection in contract.COLLECTION_NAMES:
            offset = None
            while True:
                records, offset = self.client.scroll(
                    collection_name=collection,
                    scroll_filter=query_filter,
                    limit=256,
                    offset=offset,
                    with_payload=["path", "chunk_index", "content_hash", "document_hash"],
                    with_vectors=False,
                )
                for record in records:
                    key = (collection, str(record.payload.get("path", "")))
                    grouped.setdefault(key, []).append(
                        (
                            str(record.id),
                            int(record.payload.get("chunk_index", -1)),
                            str(record.payload.get("content_hash", "")),
                            str(record.payload.get("document_hash", "")),
                        )
                    )
                if offset is None:
                    break
        return {key: tuple(sorted(items, key=lambda item: (item[1], item[0]))) for key, items in grouped.items()}

    def snapshot_embedding_profiles(
        self,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
    ) -> set[tuple[str, str]]:
        profiles: set[tuple[str, str]] = set()
        query_filter = self._filter(repo, commit_sha, worktree_state, snapshot_id)
        for collection in contract.COLLECTION_NAMES:
            offset = None
            while True:
                records, offset = self.client.scroll(
                    collection_name=collection,
                    scroll_filter=query_filter,
                    limit=256,
                    offset=offset,
                    with_payload=["embedding_model", "embedding_model_revision"],
                    with_vectors=False,
                )
                profiles.update(
                    (
                        str(record.payload.get("embedding_model", "")),
                        str(record.payload.get("embedding_model_revision", "")),
                    )
                    for record in records
                )
                if offset is None:
                    break
        return profiles

    def load_path_points(
        self,
        collection: str,
        repo: str,
        commit_sha: str,
        worktree_state: str,
        snapshot_id: str,
        path: str,
    ) -> list[IndexPoint]:
        records = []
        offset = None
        while True:
            batch, offset = self.client.scroll(
                collection_name=collection,
                scroll_filter=self._filter(
                    repo, commit_sha, worktree_state, snapshot_id, path
                ),
                limit=256,
                offset=offset,
                with_payload=True,
                with_vectors=True,
            )
            records.extend(batch)
            if offset is None:
                break

        points: list[IndexPoint] = []
        for record in records:
            vectors = record.vector or {}
            dense = vectors.get(contract.DENSE_VECTOR, [])
            sparse = vectors.get(contract.SPARSE_VECTOR)
            if hasattr(sparse, "model_dump"):
                sparse_body = sparse.model_dump()
            elif isinstance(sparse, dict):
                sparse_body = sparse
            else:
                sparse_body = {
                    "indices": list(getattr(sparse, "indices", []) or []),
                    "values": list(getattr(sparse, "values", []) or []),
                }
            points.append(
                IndexPoint(
                    id=str(record.id),
                    dense=list(dense),
                    sparse={
                        "indices": list(sparse_body.get("indices", [])),
                        "values": list(sparse_body.get("values", [])),
                    },
                    payload=dict(record.payload or {}),
                )
            )
        return sorted(
            points,
            key=lambda point: (int(point.payload.get("chunk_index", -1)), point.id),
        )
