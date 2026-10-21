from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from api.app.config import Settings
from api.app.main import create_app
from api.app.service import QuerySnapshot, RetrievalService, SnapshotCompatibilityError
from ragcore import contract
from ragcore.embedding_client import FakeEmbeddingClient
from ragcore.qdrant_index import InMemoryVectorIndex, IndexPoint
from ragcore.reranker_client import FakeRerankerClient


SHA = "a" * 40
SNAPSHOT = "dirty:" + "c" * 64
MODEL = "BAAI/bge-m3"
REVISION = "1" * 40


def point(point_id, collection, text, commit=SHA, snapshot_id=SNAPSHOT):
    evidence = contract.evidence_kind_of(collection)
    payload = contract.ChunkPayload(
        repo="MC",
        commit_sha=commit,
        worktree_state="dirty",
        snapshot_id=snapshot_id,
        path=f"{collection}/{point_id}.md",
        content_hash=contract.content_hash(text),
        document_hash=contract.content_hash(text),
        evidence_kind=evidence,
        collection=collection,
        source_type="test",
        chunk_index=0,
        line_start=2,
        line_end=4,
        text=text,
        embedding_model=MODEL,
        embedding_model_revision=REVISION,
    ).to_qdrant()
    embeddings = FakeEmbeddingClient()
    dense, sparse = embeddings.embed([text])
    return IndexPoint(point_id, dense[0], sparse[0], payload)


def make_service():
    index = InMemoryVectorIndex()
    index.ensure_collections()
    for item in [
        point("expected", "expected_docs", "dragon boss should have fire skill"),
        point("observed", "observed_runtime", "dragon boss fire skill passed runtime"),
        point("other-commit", "expected_docs", "secret other snapshot", "b" * 40),
    ]:
        payload = item.payload
        index.replace_path(
            payload["collection"],
            "MC",
            payload["commit_sha"],
            "dirty",
            payload["snapshot_id"],
            payload["path"],
            [item],
        )
    class PublishedClient:
        def get_snapshot(self):
            return QuerySnapshot(SHA, "dirty", SNAPSHOT)

    service = RetrievalService(
        "MC",
        FakeEmbeddingClient(),
        FakeRerankerClient(),
        index,
        PublishedClient(),
        prefetch_limit=8,
        embedding_model=MODEL,
        embedding_model_revision=REVISION,
    )
    return service


def test_search_requires_one_evidence_kind_and_exact_snapshot():
    service = make_service()
    with pytest.raises(ValueError, match="cannot mix"):
        service.search(
            "dragon fire",
            ["expected_docs", "observed_runtime"],
            QuerySnapshot(SHA, "dirty", SNAPSHOT),
            10,
        )
    results = service.search(
        "dragon fire", ["expected_docs"], QuerySnapshot(SHA, "dirty", SNAPSHOT), 10
    )
    assert len(results) == 1
    assert results[0]["citation"]["commit_sha"] == SHA
    assert results[0]["citation"]["evidence_kind"] == "expected"


def test_citation_is_complete_and_exposes_both_scores():
    result = make_service().search(
        "dragon fire", ["expected_docs"], QuerySnapshot(SHA, "dirty", SNAPSHOT), 1
    )[0]
    assert {
        "path",
        "line_start",
        "line_end",
        "commit_sha",
        "worktree_state",
        "evidence_kind",
        "collection",
        "content_hash",
        "snapshot_id",
        "embedding_model",
        "embedding_model_revision",
    } <= result["citation"].keys()
    assert {"fusion", "rerank", "score"} == result["scores"].keys()
    assert result["citation"]["embedding_model"] == MODEL
    assert result["citation"]["embedding_model_revision"] == REVISION


def test_explicit_snapshot_rejects_incompatible_embedding_revision():
    index = InMemoryVectorIndex()
    index.ensure_collections()
    old_snapshot = "dirty:" + "d" * 64
    old_point = point(
        "old",
        "expected_docs",
        "old embedding space",
        snapshot_id=old_snapshot,
    )
    old_point.payload["embedding_model_revision"] = "2" * 40
    index.replace_path(
        "expected_docs",
        "MC",
        SHA,
        "dirty",
        old_snapshot,
        old_point.payload["path"],
        [old_point],
    )

    class PublishedClient:
        def get_snapshot(self):
            return QuerySnapshot(SHA, "dirty", SNAPSHOT)

    service = RetrievalService(
        "MC",
        FakeEmbeddingClient(),
        FakeRerankerClient(),
        index,
        PublishedClient(),
        embedding_model=MODEL,
        embedding_model_revision=REVISION,
    )
    with pytest.raises(SnapshotCompatibilityError, match="embedding profile"):
        service.resolve_snapshot(SHA, "dirty", old_snapshot)


def test_http_search_returns_conflict_for_incompatible_snapshot():
    index = InMemoryVectorIndex()
    index.ensure_collections()
    old_snapshot = "dirty:" + "e" * 64
    old_point = point("old-http", "expected_docs", "old", snapshot_id=old_snapshot)
    old_point.payload["embedding_model_revision"] = "2" * 40
    index.replace_path(
        "expected_docs",
        "MC",
        SHA,
        "dirty",
        old_snapshot,
        old_point.payload["path"],
        [old_point],
    )

    class PublishedClient:
        def get_snapshot(self):
            return QuerySnapshot(SHA, "dirty", SNAPSHOT)

    service = RetrievalService(
        "MC",
        FakeEmbeddingClient(),
        FakeRerankerClient(),
        index,
        PublishedClient(),
        embedding_model=MODEL,
        embedding_model_revision=REVISION,
    )
    app = create_app(Settings(), service=service)
    with TestClient(app) as client:
        response = client.post(
            "/search",
            json={
                "query": "old",
                "collections": ["expected_docs"],
                "commit_sha": SHA,
                "worktree_state": "dirty",
                "snapshot_id": old_snapshot,
            },
        )
    assert response.status_code == 409
    assert "embedding profile" in response.json()["detail"]


def test_compatible_historical_snapshot_remains_queryable():
    service = make_service()
    historical = "dirty:" + "f" * 64
    item = point("compatible-history", "expected_docs", "history", snapshot_id=historical)
    service.index.replace_path(
        "expected_docs",
        "MC",
        SHA,
        "dirty",
        historical,
        item.payload["path"],
        [item],
    )
    assert service.resolve_snapshot(SHA, "dirty", historical) == QuerySnapshot(
        SHA, "dirty", historical
    )


@pytest.mark.parametrize("legacy_revision", [None, "2" * 40])
def test_historical_snapshot_rejects_missing_or_mixed_profiles(legacy_revision):
    service = make_service()
    historical = "dirty:" + "9" * 64
    compatible = point("compatible", "expected_docs", "one", snapshot_id=historical)
    legacy = point("legacy", "expected_docs", "two", snapshot_id=historical)
    if legacy_revision is None:
        legacy.payload.pop("embedding_model_revision")
    else:
        legacy.payload["embedding_model_revision"] = legacy_revision
    for item in (compatible, legacy):
        service.index.replace_path(
            "expected_docs",
            "MC",
            SHA,
            "dirty",
            historical,
            item.payload["path"],
            [item],
        )
    with pytest.raises(SnapshotCompatibilityError):
        service.resolve_snapshot(SHA, "dirty", historical)


def test_current_empty_published_snapshot_is_valid():
    index = InMemoryVectorIndex()
    index.ensure_collections()

    class PublishedClient:
        def get_snapshot(self):
            return QuerySnapshot(SHA, "dirty", SNAPSHOT)

    service = RetrievalService(
        "MC",
        FakeEmbeddingClient(),
        FakeRerankerClient(),
        index,
        PublishedClient(),
        embedding_model=MODEL,
        embedding_model_revision=REVISION,
    )
    assert service.resolve_snapshot(None, None, None) == QuerySnapshot(
        SHA, "dirty", SNAPSHOT
    )


def test_compare_returns_expected_and_observed_as_separate_groups():
    groups = make_service().compare(
        "dragon fire",
        ["expected_docs", "expected_config"],
        ["observed_runtime", "observed_benchmarks"],
        QuerySnapshot(SHA, "dirty", SNAPSHOT),
        5,
    )
    assert groups.keys() == {"expected", "observed"}
    assert {r["citation"]["evidence_kind"] for r in groups["expected"]} == {"expected"}
    assert {r["citation"]["evidence_kind"] for r in groups["observed"]} == {"observed"}


def test_snapshot_defaults_to_current_and_partial_selector_is_rejected():
    service = make_service()
    resolved = service.resolve_snapshot(None, None, None)
    assert resolved.commit_sha == SHA
    assert resolved.worktree_state == "dirty"
    assert resolved.snapshot_id.startswith("dirty:")
    with pytest.raises(ValueError, match="supplied together"):
        service.resolve_snapshot(SHA, None, None)


def test_explicit_snapshot_id_is_required_and_validated():
    service = make_service()
    assert service.resolve_snapshot(SHA, "dirty", SNAPSHOT) == QuerySnapshot(
        SHA, "dirty", SNAPSHOT
    )
    with pytest.raises(ValueError, match="valid exact snapshot"):
        service.resolve_snapshot(SHA, "dirty", "dirty:short")


def test_same_head_dirty_generations_are_filtered_independently():
    service = make_service()
    other_snapshot = "dirty:" + "d" * 64
    other = point(
        "same-head-other-generation",
        "expected_docs",
        "other dirty generation",
        snapshot_id=other_snapshot,
    )
    service.index.replace_path(
        "expected_docs",
        "MC",
        SHA,
        "dirty",
        other_snapshot,
        other.payload["path"],
        [other],
    )
    current = service.search(
        "generation", ["expected_docs"], QuerySnapshot(SHA, "dirty", SNAPSHOT), 10
    )
    other_results = service.search(
        "generation",
        ["expected_docs"],
        QuerySnapshot(SHA, "dirty", other_snapshot),
        10,
    )
    assert {item["citation"]["snapshot_id"] for item in current} == {SNAPSHOT}
    assert {item["citation"]["snapshot_id"] for item in other_results} == {
        other_snapshot
    }


def test_default_snapshot_uses_published_indexer_contract_without_repo_scan(monkeypatch):
    service = make_service()

    class PublishedClient:
        def get_snapshot(self):
            return QuerySnapshot(SHA, "dirty", SNAPSHOT)

    service.published_snapshots = PublishedClient()

    def forbidden_scan(*args, **kwargs):
        raise AssertionError("default query must not scan the repository")

    monkeypatch.setattr("ragcore.snapshot.scan_repository", forbidden_scan)
    assert service.resolve_snapshot(None, None, None) == QuerySnapshot(
        SHA, "dirty", SNAPSHOT
    )


def test_compare_rejects_collections_in_wrong_group():
    with pytest.raises(ValueError, match="expected"):
        make_service().compare(
            "query",
            ["observed_runtime"],
            ["observed_runtime"],
            QuerySnapshot(SHA, "dirty", SNAPSHOT),
            5,
        )
