"""Strict snapshot/evidence retrieval orchestration."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Optional, Protocol, Sequence

from ragcore import contract
from ragcore.embedding_client import EmbeddingClient
from ragcore.qdrant_index import VectorIndex
from ragcore.reranker_client import RerankerClient


@dataclass(frozen=True)
class QuerySnapshot:
    commit_sha: str
    worktree_state: str
    snapshot_id: str


class SnapshotCompatibilityError(RuntimeError):
    """The selected snapshot was built with a different embedding profile."""


class PublishedSnapshotClient(Protocol):
    def get_snapshot(self) -> QuerySnapshot:
        ...


class HttpPublishedSnapshotClient:
    def __init__(self, base_url: str, timeout: float = 5.0) -> None:
        self.url = base_url.rstrip("/") + "/published-snapshot"
        self.timeout = timeout

    def get_snapshot(self) -> QuerySnapshot:
        import httpx

        response = httpx.get(self.url, timeout=self.timeout)
        response.raise_for_status()
        body = response.json()
        commit_sha = contract.validate_commit_sha(str(body["commit_sha"]))
        worktree_state = str(body["worktree_state"])
        if worktree_state not in {
            contract.WORKTREE_CLEAN,
            contract.WORKTREE_DIRTY,
            contract.WORKTREE_UNKNOWN,
        }:
            raise RuntimeError("indexer returned an invalid worktree_state")
        snapshot_id = contract.validate_snapshot_id(
            str(body["snapshot_id"]), commit_sha, worktree_state
        )
        return QuerySnapshot(commit_sha, worktree_state, snapshot_id)


class RetrievalService:
    def __init__(
        self,
        repo_name: str,
        embeddings: EmbeddingClient,
        reranker: RerankerClient,
        index: VectorIndex,
        published_snapshots: PublishedSnapshotClient,
        prefetch_limit: int = 40,
        embedding_model: str = "BAAI/bge-m3",
        embedding_model_revision: str = "5617a9f61b028005a4858fdac845db406aefb181",
    ) -> None:
        self.repo_name = repo_name
        self.embeddings = embeddings
        self.reranker = reranker
        self.index = index
        self.published_snapshots = published_snapshots
        self.prefetch_limit = prefetch_limit
        self.embedding_model = embedding_model
        self.embedding_model_revision = embedding_model_revision
        self._profile_cache: dict[QuerySnapshot, frozenset[tuple[str, str]]] = {}

    def _validate_snapshot_profile(
        self, snapshot: QuerySnapshot, published: QuerySnapshot
    ) -> None:
        profiles = self._profile_cache.get(snapshot)
        if profiles is None:
            profiles = frozenset(
                self.index.snapshot_embedding_profiles(
                    self.repo_name,
                    snapshot.commit_sha,
                    snapshot.worktree_state,
                    snapshot.snapshot_id,
                )
            )
            self._profile_cache[snapshot] = profiles
        expected = frozenset(
            {(self.embedding_model, self.embedding_model_revision)}
        )
        if profiles == expected:
            return
        if not profiles and snapshot == published:
            return
        raise SnapshotCompatibilityError(
            "selected snapshot embedding profile is unavailable or incompatible "
            f"with {self.embedding_model}@{self.embedding_model_revision}"
        )

    def resolve_snapshot(
        self,
        commit_sha: Optional[str],
        worktree_state: Optional[str],
        snapshot_id: Optional[str],
    ) -> QuerySnapshot:
        supplied = (commit_sha is not None, worktree_state is not None, snapshot_id is not None)
        if any(supplied) and not all(supplied):
            raise ValueError(
                "commit_sha, worktree_state and snapshot_id must be supplied together"
            )
        published = self.published_snapshots.get_snapshot()
        if commit_sha is None:
            snapshot = published
        else:
            if worktree_state not in {
                contract.WORKTREE_CLEAN,
                contract.WORKTREE_DIRTY,
                contract.WORKTREE_UNKNOWN,
            }:
                raise ValueError("invalid worktree_state")
            commit_sha = contract.validate_commit_sha(commit_sha)
            snapshot_id = contract.validate_snapshot_id(
                str(snapshot_id), commit_sha, worktree_state
            )
            snapshot = QuerySnapshot(commit_sha, worktree_state, snapshot_id)
        self._validate_snapshot_profile(snapshot, published)
        return snapshot

    @staticmethod
    def validate_group(collections: Sequence[str], evidence_kind: Optional[str] = None) -> list[str]:
        names = list(collections)
        if not names:
            raise ValueError("at least one collection is required")
        contract.validate_collections(names)
        if contract.spans_multiple_evidence_kinds(names):
            raise ValueError("search cannot mix expected and observed collections; use /compare")
        if evidence_kind and any(contract.evidence_kind_of(name) != evidence_kind for name in names):
            raise ValueError(f"collection group must contain only {evidence_kind} evidence")
        return names

    def search(
        self,
        query: str,
        collections: Sequence[str],
        snapshot: QuerySnapshot,
        limit: int,
    ) -> list[dict[str, Any]]:
        names = self.validate_group(collections)
        dense, sparse = self.embeddings.embed([query])
        candidates = []
        for collection in names:
            candidates.extend(
                self.index.hybrid_query(
                    collection,
                    dense[0],
                    sparse[0],
                    self.repo_name,
                    snapshot.commit_sha,
                    snapshot.worktree_state,
                    snapshot.snapshot_id,
                    self.embedding_model,
                    self.embedding_model_revision,
                    max(limit * 2, limit),
                    max(self.prefetch_limit, limit),
                )
            )
        candidates.sort(key=lambda item: item.fusion_score, reverse=True)
        candidates = candidates[: max(limit * 4, limit)]
        if not candidates:
            return []
        rerank_scores = self.reranker.rerank(query, [hit.payload["text"] for hit in candidates])
        ranked = sorted(
            zip(candidates, rerank_scores),
            key=lambda item: (item[1], item[0].fusion_score),
            reverse=True,
        )[:limit]
        results = []
        for hit, rerank_score in ranked:
            payload = hit.payload
            results.append(
                {
                    "text": payload["text"],
                    "citation": {
                        "path": payload["path"],
                        "line_start": payload["line_start"],
                        "line_end": payload["line_end"],
                        "commit_sha": payload["commit_sha"],
                        "worktree_state": payload["worktree_state"],
                        "snapshot_id": payload["snapshot_id"],
                        "evidence_kind": payload["evidence_kind"],
                        "collection": payload["collection"],
                        "content_hash": payload["content_hash"],
                        "heading": payload.get("heading"),
                        "observed_at": payload.get("observed_at"),
                        "embedding_model": payload.get("embedding_model"),
                        "embedding_model_revision": payload.get(
                            "embedding_model_revision"
                        ),
                    },
                    "scores": {
                        "fusion": hit.fusion_score,
                        "rerank": float(rerank_score),
                        "score": float(rerank_score),
                    },
                }
            )
        return results

    def compare(
        self,
        query: str,
        expected_collections: Sequence[str],
        observed_collections: Sequence[str],
        snapshot: QuerySnapshot,
        limit: int,
    ) -> dict[str, list[dict[str, Any]]]:
        expected = self.validate_group(expected_collections, contract.EVIDENCE_EXPECTED)
        observed = self.validate_group(observed_collections, contract.EVIDENCE_OBSERVED)
        return {
            "expected": self.search(query, expected, snapshot, limit),
            "observed": self.search(query, observed, snapshot, limit),
        }
