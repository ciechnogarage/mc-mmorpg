from __future__ import annotations

import copy
import json
import os
import threading

import pytest

from indexer.app.config import Settings
from indexer.app.service import (
    HistoricalSourceUnavailable,
    IndexingService,
    SourceChanged,
    SourceUnavailable,
)
from ragcore import contract
from ragcore.embedding_client import FakeEmbeddingClient
from ragcore.events import WebhookEvent
from ragcore.gitmeta import StaticGitInspector
from ragcore.qdrant_index import InMemoryVectorIndex
from ragcore.snapshot import SnapshotMutationError


SHA = "a" * 40


def write(root, path, text):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")
    return target


def make_service(tmp_path):
    settings = Settings(
        repo_path=str(tmp_path),
        repo_name="MC",
        state_path=str(tmp_path / ".index-state.json"),
        watch_interval=0.01,
        reconcile_interval=60,
    )
    index = InMemoryVectorIndex()
    service = IndexingService(
        settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        FakeEmbeddingClient(),
        index,
    )
    index.ensure_collections()
    return service, index


def inventory_for(service, index, snapshot):
    return index.snapshot_inventory(
        service.settings.repo_name,
        snapshot.commit_sha,
        snapshot.worktree_state,
        snapshot.snapshot_id,
    )


def seed_four_routes(tmp_path):
    write(tmp_path, "docs/guide.md", "# Guide\nexpected documentation")
    write(tmp_path, "plugins/MMOCore/config.yml", "enabled: true")
    write(tmp_path, "logs/runtime-report.md", "runtime observed ok")
    write(tmp_path, "reports/benchmarks/bot-benchmark.json", '{"score": 4}')


def test_reconcile_routes_four_collections_and_is_idempotent(tmp_path):
    seed_four_routes(tmp_path)
    service, index = make_service(tmp_path)
    first = service.reconcile()
    before = {
        name: {point_id: point.payload["content_hash"] for point_id, point in bucket.items()}
        for name, bucket in index.collections.items()
    }
    second = service.reconcile()
    after = {
        name: {point_id: point.payload["content_hash"] for point_id, point in bucket.items()}
        for name, bucket in index.collections.items()
    }
    assert first["files"] == second["files"] == 4
    assert all(index.collections[name] for name in contract.COLLECTION_NAMES)
    assert before == after


def test_initial_reconcile_batches_chunks_across_files(tmp_path):
    for index in range(5):
        write(tmp_path, f"docs/guide-{index}.md", f"# Guide {index}\nsmall document")
    service, _index = make_service(tmp_path)

    result = service.reconcile()

    assert result["files"] == 5
    assert result["embedded_files"] == 5
    assert result["chunks"] == 5
    assert service.embeddings.calls == 1


def test_reconcile_caps_cross_file_embedding_batches(tmp_path):
    for index in range(33):
        write(tmp_path, f"docs/guide-{index:02d}.md", f"small document {index}")
    service, _index = make_service(tmp_path)

    result = service.reconcile()

    assert result["chunks"] == 33
    assert service.embeddings.calls == 2


def test_embedding_revision_change_creates_new_snapshot_and_reembeds(tmp_path):
    write(tmp_path, "docs/guide.md", "stable text")
    state_path = str(tmp_path / ".index-state.json")
    index = InMemoryVectorIndex()
    index.ensure_collections()

    first_embeddings = FakeEmbeddingClient()
    first = IndexingService(
        Settings(
            repo_path=str(tmp_path),
            repo_name="MC",
            state_path=state_path,
            embedding_model_revision="1" * 40,
        ),
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        first_embeddings,
        index,
    )
    first.reconcile()
    first_snapshot = first.last_snapshot

    second_embeddings = FakeEmbeddingClient()
    second = IndexingService(
        Settings(
            repo_path=str(tmp_path),
            repo_name="MC",
            state_path=state_path,
            embedding_model_revision="2" * 40,
        ),
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        second_embeddings,
        index,
    )
    result = second.reconcile()

    assert second.last_snapshot != first_snapshot
    assert result["embedded_files"] == 1
    assert result["reused_files"] == 0
    assert second_embeddings.calls == 1
    point = next(
        point
        for point in index.collections["expected_docs"].values()
        if point.payload["snapshot_id"] == second.last_snapshot.snapshot_id
    )
    assert point.payload["embedding_model_revision"] == "2" * 40


def test_reconcile_replaces_changed_content_and_deletes_stale_file(tmp_path):
    file = write(tmp_path, "docs/guide.md", "old text")
    service, index = make_service(tmp_path)
    service.reconcile()
    old = next(iter(index.collections["expected_docs"].values()))
    file.write_text("new text", encoding="utf-8")
    service.reconcile()
    new = next(
        point
        for point in index.collections["expected_docs"].values()
        if point.payload["snapshot_id"] == service.last_snapshot.snapshot_id
    )
    assert old.id != new.id
    assert old.payload["content_hash"] != new.payload["content_hash"]
    file.unlink()
    result = service.reconcile()
    # Deleting a file creates a new dirty generation. Historical generation
    # points remain queryable; the exact current inventory is empty.
    assert result["deleted"] == 0
    assert index.snapshot_inventory(
        "MC",
        service.last_snapshot.commit_sha,
        service.last_snapshot.worktree_state,
        service.last_snapshot.snapshot_id,
    ) == {}


@pytest.mark.parametrize(
    ("event_type", "path", "collection"),
    [
        ("documentation", "incoming/doc.md", "expected_docs"),
        ("plugin", "incoming/plugin.yml", "expected_config"),
        ("test_completed", "incoming/test.json", "observed_benchmarks"),
        ("runtime_report", "incoming/runtime.json", "observed_runtime"),
        ("bot_benchmark", "incoming/bot.json", "observed_benchmarks"),
    ],
)
def test_path_webhook_events_force_their_collection(tmp_path, event_type, path, collection):
    write(tmp_path, path, "event evidence")
    service, index = make_service(tmp_path)
    result = service.handle_event(
        WebhookEvent(event_type, [path], observed_at="2026-07-20T00:00:00Z")
    )
    assert result["chunks"] == 1
    point = next(iter(index.collections[collection].values()))
    assert point.payload["event_type"] == event_type
    assert point.payload["evidence_kind"] == contract.evidence_kind_of(collection)


def test_commit_event_runs_full_reconcile(tmp_path):
    write(tmp_path, "docs/a.md", "hello")
    service, _index = make_service(tmp_path)
    result = service.handle_event(WebhookEvent("commit", commit_sha=SHA))
    assert result["event_type"] == "commit"
    assert result["files"] == 1


def test_webhook_rejects_traversal_mismatched_commit_and_secret(tmp_path):
    outside = tmp_path.parent / "outside.md"
    outside.write_text("outside", encoding="utf-8")
    write(tmp_path, "credentials.json", "do not index")
    service, _index = make_service(tmp_path)
    with pytest.raises(ValueError):
        service.handle_event(WebhookEvent("documentation", ["../outside.md"]))
    with pytest.raises(ValueError):
        service.handle_event(WebhookEvent("documentation", ["credentials.json"]))
    with pytest.raises(ValueError):
        service.handle_event(WebhookEvent("commit", commit_sha="b" * 40))


def test_symlink_cannot_escape_repo(tmp_path):
    outside = tmp_path.parent / "outside.md"
    outside.write_text("outside", encoding="utf-8")
    os.symlink(outside, tmp_path / "escape.md")
    service, _index = make_service(tmp_path)
    with pytest.raises(ValueError):
        service.index_file("escape.md")


def test_in_repo_symlink_to_excluded_target_is_rejected(tmp_path):
    write(tmp_path, ".env", "MARIADB_PASSWORD=secret")
    os.symlink(tmp_path / ".env", tmp_path / "docs.md")
    service, _index = make_service(tmp_path)
    with pytest.raises(ValueError):
        service.index_file("docs.md")


def test_source_is_bound_to_current_published_manifest_and_fingerprint(tmp_path):
    path = write(tmp_path, "docs/guide.md", "line one\nline two\nline three")
    service, _index = make_service(tmp_path)
    service.reconcile()
    published = service.published_snapshot()

    source = service.source_for(
        "docs/guide.md",
        commit_sha=published.commit_sha,
        worktree_state=published.worktree_state,
        snapshot_id=published.snapshot_id,
        line_start=2,
        line_end=3,
    )
    assert source["content"] == "line two\nline three"
    assert source["snapshot"]["snapshot_id"] == published.snapshot_id
    assert source["content_hash"] == contract.content_hash(path.read_text())

    path.write_text("changed after publication", encoding="utf-8")
    with pytest.raises(SourceChanged, match="changed since publication"):
        service.source_for("docs/guide.md")


def test_source_rejects_unsafe_unpublished_and_historical_paths(tmp_path):
    write(tmp_path, "docs/guide.md", "published")
    write(tmp_path, "secrets.env", "PASSWORD=secret")
    service, _index = make_service(tmp_path)
    service.reconcile()
    published = service.published_snapshot()

    with pytest.raises(ValueError):
        service.source_for("../outside.md")
    with pytest.raises(SourceUnavailable):
        service.source_for("secrets.env")
    with pytest.raises(HistoricalSourceUnavailable, match="historical"):
        service.source_for(
            "docs/guide.md",
            commit_sha=published.commit_sha,
            worktree_state=published.worktree_state,
            snapshot_id="dirty:" + "d" * 64,
        )


def test_symlink_directory_is_never_scanned_or_indexed(tmp_path):
    outside = tmp_path.parent / "outside-dir"
    write(outside, "guide.md", "must stay outside")
    os.symlink(outside, tmp_path / "linked-docs")
    service, index = make_service(tmp_path)
    result = service.reconcile()
    assert result["files"] == 0
    assert not any(index.collections.values())
    with pytest.raises(ValueError):
        service.index_file("linked-docs/guide.md")


def test_filesystem_watch_handles_change_and_delete(tmp_path):
    file = write(tmp_path, "docs/a.md", "one")
    service, index = make_service(tmp_path)
    service.reconcile()
    file.write_text("a much longer value", encoding="utf-8")
    changed = service.watch_once()
    assert changed["changed"] == 1
    file.unlink()
    deleted = service.watch_once()
    assert deleted["deleted"] == 1
    assert index.snapshot_inventory(
        "MC",
        service.last_snapshot.commit_sha,
        service.last_snapshot.worktree_state,
        service.last_snapshot.snapshot_id,
    ) == {}


def test_event_override_survives_watch_reconcile_and_restart(tmp_path):
    write(tmp_path, "incoming/test.json", '{"passed": true}')
    write(tmp_path, "docs/other.md", "unchanged companion")
    service, index = make_service(tmp_path)
    service.reconcile()
    baseline_embedding_calls = service.embeddings.calls
    baseline_replace_calls = index.replace_calls
    service.handle_event(
        WebhookEvent(
            "test_completed",
            ["incoming/test.json"],
            observed_at="2026-07-20T00:00:00Z",
            doc_version="run-7",
        )
    )
    assert service.embeddings.calls == baseline_embedding_calls
    assert index.replace_calls == baseline_replace_calls + 2
    service.watch_once()
    service.reconcile()

    restarted_embeddings = FakeEmbeddingClient()
    restarted = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        restarted_embeddings,
        index,
    )
    restarted.reconcile()
    assert restarted_embeddings.calls == 0
    current = restarted.published_snapshot()
    point = next(
        point
        for point in index.collections["observed_benchmarks"].values()
        if point.payload["snapshot_id"] == current.snapshot_id
    )
    assert (
        "expected_config",
        "incoming/test.json",
    ) not in inventory_for(restarted, index, current)
    assert point.payload["event_type"] == "test_completed"
    assert point.payload["observed_at"] == "2026-07-20T00:00:00Z"
    assert point.payload["doc_version"] == "run-7"


def test_indexed_config_redacts_broad_secret_assignments(tmp_path):
    write(
        tmp_path,
        "plugins/example/config.yml",
        "\n".join(
            [
                "rcon.password=alpha",
                "db.password: beta",
                "MARIADB_PASSWORD=gamma",
                "export API_TOKEN=delta",
                "useful.setting=true",
            ]
        ),
    )
    service, index = make_service(tmp_path)
    service.reconcile()
    indexed = "\n".join(
        point.payload["text"] for point in index.collections["expected_config"].values()
    )
    for secret in ("alpha", "beta", "gamma", "delta"):
        assert secret not in indexed
    assert "useful.setting=true" in indexed


def test_event_override_expires_when_content_changes(tmp_path):
    path = write(tmp_path, "incoming/test.json", '{"passed": true}')
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot_a = service.published_snapshot()
    service.handle_event(WebhookEvent("test_completed", ["incoming/test.json"]))
    snapshot_b = service.published_snapshot()
    calls_before_expiry = service.embeddings.calls
    path.write_text('{"definition": "new unrelated content"}', encoding="utf-8")
    service.reconcile()
    snapshot_c = service.published_snapshot()
    assert len({snapshot_a.snapshot_id, snapshot_b.snapshot_id, snapshot_c.snapshot_id}) == 3
    assert service.embeddings.calls - calls_before_expiry == 1
    current = index.snapshot_inventory(
        "MC",
        service.last_snapshot.commit_sha,
        service.last_snapshot.worktree_state,
        service.last_snapshot.snapshot_id,
    )
    assert ("observed_benchmarks", "incoming/test.json") not in current
    assert ("expected_config", "incoming/test.json") in current
    point = next(
        point
        for point in index.collections["expected_config"].values()
        if point.payload["snapshot_id"] == service.last_snapshot.snapshot_id
    )
    assert "event_type" not in point.payload


def test_unchanged_reconcile_and_restart_do_not_embed_or_upsert(tmp_path):
    write(tmp_path, "docs/a.md", "stable")
    service, index = make_service(tmp_path)
    service.reconcile()
    embedding_calls = service.embeddings.calls
    replace_calls = index.replace_calls
    service.reconcile()
    assert service.embeddings.calls == embedding_calls
    assert index.replace_calls == replace_calls

    restarted_embeddings = FakeEmbeddingClient()
    restarted = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        restarted_embeddings,
        index,
    )
    restarted.reconcile()
    assert restarted_embeddings.calls == 0
    assert index.replace_calls == replace_calls


def test_manifest_with_missing_qdrant_data_forces_rebuild(tmp_path):
    write(tmp_path, "docs/a.md", "stable")
    service, index = make_service(tmp_path)
    service.reconcile()
    index.collections = {name: {} for name in contract.COLLECTION_NAMES}

    restarted_embeddings = FakeEmbeddingClient()
    restarted = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        restarted_embeddings,
        index,
    )
    restarted.reconcile()
    assert restarted_embeddings.calls == 1
    assert index.collections["expected_docs"]


def test_different_dirty_contents_get_different_snapshot_ids_and_point_ids(tmp_path):
    path = write(tmp_path, "docs/a.md", "one")
    service, index = make_service(tmp_path)
    service.reconcile()
    first_snapshot = service.last_snapshot
    first_id = next(iter(index.collections["expected_docs"]))
    path.write_text("two", encoding="utf-8")
    service.reconcile()
    second_snapshot = service.last_snapshot
    second_id = next(reversed(index.collections["expected_docs"]))
    assert first_snapshot.snapshot_id != second_snapshot.snapshot_id
    assert first_id != second_id


def test_new_snapshot_embeds_only_changed_file_and_reuses_other_vectors(tmp_path):
    paths = [
        write(tmp_path, "docs/a.md", "alpha"),
        write(tmp_path, "docs/b.md", "beta"),
        write(tmp_path, "docs/c.md", "gamma"),
    ]
    service, index = make_service(tmp_path)
    service.reconcile()
    old_snapshot = service.last_snapshot
    calls_before = service.embeddings.calls
    paths[1].write_text("beta changed", encoding="utf-8")

    result = service.reconcile()
    new_snapshot = service.last_snapshot
    assert result["embedded_files"] == 1
    assert service.embeddings.calls - calls_before == 1
    assert old_snapshot.snapshot_id != new_snapshot.snapshot_id
    new_inventory = index.snapshot_inventory(
        "MC",
        new_snapshot.commit_sha,
        new_snapshot.worktree_state,
        new_snapshot.snapshot_id,
    )
    old_inventory = index.snapshot_inventory(
        "MC",
        old_snapshot.commit_sha,
        old_snapshot.worktree_state,
        old_snapshot.snapshot_id,
    )
    assert {path for _collection, path in new_inventory} == {
        "docs/a.md",
        "docs/b.md",
        "docs/c.md",
    }
    assert {path for _collection, path in old_inventory} == {
        "docs/a.md",
        "docs/b.md",
        "docs/c.md",
    }


def test_missing_single_chunk_repairs_only_affected_path(tmp_path):
    long_text = "\n".join(f"line {number}" for number in range(170))
    write(tmp_path, "docs/long.md", long_text)
    write(tmp_path, "docs/other.md", "unrelated")
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot = service.last_snapshot
    long_points = [
        (point_id, point)
        for point_id, point in index.collections["expected_docs"].items()
        if point.payload["path"] == "docs/long.md"
        and point.payload["snapshot_id"] == snapshot.snapshot_id
    ]
    assert len(long_points) == 3
    del index.collections["expected_docs"][long_points[1][0]]
    calls_before = service.embeddings.calls
    replace_before = index.replace_calls

    result = service.reconcile()
    repaired = [
        point
        for point in index.collections["expected_docs"].values()
        if point.payload["path"] == "docs/long.md"
        and point.payload["snapshot_id"] == snapshot.snapshot_id
    ]
    assert len(repaired) == 3
    assert result["embedded_files"] == 1
    assert service.embeddings.calls - calls_before == 1
    assert index.replace_calls - replace_before == 1


def test_failed_new_generation_keeps_previous_published_snapshot(tmp_path):
    path = write(tmp_path, "docs/a.md", "published")
    service, _index = make_service(tmp_path)
    service.reconcile()
    published = service.published_snapshot()
    path.write_text("new but failing", encoding="utf-8")

    class FailingEmbeddings(FakeEmbeddingClient):
        def embed(self, texts):
            raise RuntimeError("synthetic embedding failure")

    service.embeddings = FailingEmbeddings()
    with pytest.raises(RuntimeError, match="synthetic"):
        service.reconcile()
    assert service.published_snapshot() == published


def test_in_progress_generation_keeps_previous_published_snapshot_nonblocking(tmp_path):
    path = write(tmp_path, "docs/a.md", "published")
    service, _index = make_service(tmp_path)
    service.reconcile()
    published = service.published_snapshot()
    path.write_text("building", encoding="utf-8")
    entered = threading.Event()
    release = threading.Event()

    class BlockingEmbeddings(FakeEmbeddingClient):
        def embed(self, texts):
            entered.set()
            assert release.wait(2)
            return super().embed(texts)

    service.embeddings = BlockingEmbeddings()
    worker = threading.Thread(target=service.reconcile)
    worker.start()
    assert entered.wait(1)
    assert service.published_snapshot() == published
    release.set()
    worker.join(timeout=2)
    assert not worker.is_alive()
    assert service.published_snapshot() != published


def test_event_semantic_generation_keeps_old_route_until_durable_publish(
    tmp_path, monkeypatch
):
    write(tmp_path, "incoming/test.json", '{"passed": true}')
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot_a = service.published_snapshot()
    inventory_a = inventory_for(service, index, snapshot_a)
    embedding_calls = service.embeddings.calls
    entered = threading.Event()
    release = threading.Event()
    candidate_snapshot = {}
    original = service._write_state_json

    def block_candidate(handle, body):
        candidate_snapshot.update(body["snapshot"])
        entered.set()
        assert release.wait(2)
        return original(handle, body)

    failures = []

    def publish_event():
        try:
            service.handle_event(
                WebhookEvent(
                    "test_completed",
                    ["incoming/test.json"],
                    observed_at="2026-07-21T12:00:00Z",
                    doc_version="run-9",
                )
            )
        except Exception as exc:  # pragma: no cover - asserted below
            failures.append(exc)

    monkeypatch.setattr(service, "_write_state_json", block_candidate)
    worker = threading.Thread(target=publish_event)
    worker.start()
    assert entered.wait(1)
    assert candidate_snapshot["snapshot_id"] != snapshot_a.snapshot_id
    assert service.published_snapshot() == snapshot_a
    assert inventory_for(service, index, snapshot_a) == inventory_a
    assert ("expected_config", "incoming/test.json") in inventory_a
    assert service.embeddings.calls == embedding_calls
    release.set()
    worker.join(timeout=2)
    assert not worker.is_alive()
    assert failures == []

    snapshot_b = service.published_snapshot()
    assert snapshot_b.snapshot_id == candidate_snapshot["snapshot_id"]
    assert snapshot_b != snapshot_a
    assert inventory_for(service, index, snapshot_a) == inventory_a
    inventory_b = inventory_for(service, index, snapshot_b)
    assert ("observed_benchmarks", "incoming/test.json") in inventory_b
    assert ("expected_config", "incoming/test.json") not in inventory_b
    assert service.embeddings.calls == embedding_calls


@pytest.mark.parametrize(
    "failure_method",
    [
        "_write_state_json",
        "_sync_state_file",
        "_replace_state",
        "_sync_state_directory",
    ],
)
def test_event_semantic_generation_failure_preserves_old_qdrant_and_retries(
    tmp_path, monkeypatch, failure_method
):
    write(tmp_path, "incoming/test.json", '{"passed": true}')
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot_a = service.published_snapshot()
    inventory_a = inventory_for(service, index, snapshot_a)
    state_a = (tmp_path / ".index-state.json").read_bytes()
    embedding_calls = service.embeddings.calls
    original = getattr(service, failure_method)
    calls = 0

    def fail_once(*args, **kwargs):
        nonlocal calls
        calls += 1
        if calls == 1:
            raise OSError(f"synthetic semantic {failure_method} failure")
        return original(*args, **kwargs)

    with monkeypatch.context() as scoped:
        scoped.setattr(service, failure_method, fail_once)
        with pytest.raises(OSError, match="synthetic semantic"):
            service.handle_event(
                WebhookEvent(
                    "test_completed",
                    ["incoming/test.json"],
                    observed_at="2026-07-21T12:00:00Z",
                    doc_version="run-9",
                )
            )

    assert service.published_snapshot() == snapshot_a
    assert inventory_for(service, index, snapshot_a) == inventory_a
    assert (tmp_path / ".index-state.json").read_bytes() == state_a
    assert service.embeddings.calls == embedding_calls
    candidate_ids = {
        point.payload["snapshot_id"]
        for point in index.collections["observed_benchmarks"].values()
        if point.payload["path"] == "incoming/test.json"
    }
    assert candidate_ids and snapshot_a.snapshot_id not in candidate_ids

    service.handle_event(
        WebhookEvent(
            "test_completed",
            ["incoming/test.json"],
            observed_at="2026-07-21T12:00:00Z",
            doc_version="run-9",
        )
    )
    snapshot_b = service.published_snapshot()
    assert snapshot_b.snapshot_id in candidate_ids
    assert snapshot_b != snapshot_a
    assert inventory_for(service, index, snapshot_a) == inventory_a
    assert service.embeddings.calls == embedding_calls

    restarted_embeddings = FakeEmbeddingClient()
    restarted = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        restarted_embeddings,
        index,
    )
    restarted.reconcile()
    assert restarted.published_snapshot() == snapshot_b
    assert restarted_embeddings.calls == 0


def test_override_removal_creates_new_semantic_generation_by_vector_reuse(tmp_path):
    write(tmp_path, "incoming/test.json", '{"passed": true}')
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot_a = service.published_snapshot()
    service.handle_event(WebhookEvent("test_completed", ["incoming/test.json"]))
    snapshot_b = service.published_snapshot()
    embedding_calls = service.embeddings.calls
    replace_calls = index.replace_calls
    inventory_a = inventory_for(service, index, snapshot_a)

    result = service.reconcile(override_removals={"incoming/test.json"})
    snapshot_c = service.published_snapshot()
    assert snapshot_a != snapshot_b
    assert snapshot_c == snapshot_a
    assert result["embedded_files"] == 0
    assert service.embeddings.calls == embedding_calls
    assert index.replace_calls == replace_calls
    assert inventory_for(service, index, snapshot_a) == inventory_a
    inventory_c = inventory_for(service, index, snapshot_c)
    assert ("expected_config", "incoming/test.json") in inventory_c
    assert ("observed_benchmarks", "incoming/test.json") not in inventory_c


@pytest.mark.parametrize(
    "failure_method",
    [
        "_write_state_json",
        "_sync_state_file",
        "_replace_state",
        "_sync_state_directory",
    ],
)
def test_atomic_publication_failure_preserves_a_on_disk_memory_and_restart(
    tmp_path, monkeypatch, failure_method
):
    path = write(tmp_path, "docs/a.md", "snapshot-a")
    service, index = make_service(tmp_path)
    service.reconcile()
    snapshot_a = service.published_snapshot()
    manifest_a = copy.deepcopy(service._manifest)
    fingerprints_a = dict(service._fingerprints)
    reconcile_a = service.last_reconcile
    state_a = (tmp_path / ".index-state.json").read_bytes()
    path.write_text("snapshot-b", encoding="utf-8")

    original = getattr(service, failure_method)
    calls = 0

    def fail_once(*args, **kwargs):
        nonlocal calls
        calls += 1
        if calls == 1:
            raise OSError(f"synthetic {failure_method} failure")
        return original(*args, **kwargs)

    with monkeypatch.context() as scoped:
        scoped.setattr(service, failure_method, fail_once)
        with pytest.raises(OSError, match="synthetic"):
            service.reconcile()

    assert calls >= 1
    assert service.published_snapshot() == snapshot_a
    assert service._manifest == manifest_a
    assert service._fingerprints == fingerprints_a
    assert service.last_reconcile == reconcile_a
    assert service.ready is True
    assert (tmp_path / ".index-state.json").read_bytes() == state_a

    restarted_a = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        FakeEmbeddingClient(),
        index,
    )
    restarted_a._load_state()
    assert restarted_a.last_snapshot == snapshot_a
    assert restarted_a._manifest == manifest_a

    service.reconcile()
    snapshot_b = service.published_snapshot()
    assert snapshot_b != snapshot_a
    persisted_b = json.loads(
        (tmp_path / ".index-state.json").read_text(encoding="utf-8")
    )
    assert persisted_b["snapshot"] == {
        "commit_sha": snapshot_b.commit_sha,
        "snapshot_id": snapshot_b.snapshot_id,
        "worktree_state": snapshot_b.worktree_state,
    }

    restarted_b = IndexingService(
        service.settings,
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        FakeEmbeddingClient(),
        index,
    )
    restarted_b._load_state()
    assert restarted_b.last_snapshot == snapshot_b
    assert restarted_b._manifest == service._manifest


def test_mid_reconcile_mutation_prevents_readiness(tmp_path):
    path = write(tmp_path, "docs/a.md", "before")
    service, _index = make_service(tmp_path)

    class MutatingEmbeddings(FakeEmbeddingClient):
        def embed(self, texts):
            result = super().embed(texts)
            path.write_text("after", encoding="utf-8")
            return result

    service.embeddings = MutatingEmbeddings()
    with pytest.raises(SnapshotMutationError, match="changed during reconcile"):
        service.reconcile()
    assert service.ready is False
    service.reconcile()
    assert service.ready is True


def test_reconcile_resumes_from_in_progress_checkpoint_after_restart(tmp_path):
    for index in range(33):
        write(tmp_path, f"docs/guide-{index:02d}.md", f"small document {index}")

    state_path = str(tmp_path / ".index-state.json")
    index = InMemoryVectorIndex()
    index.ensure_collections()

    class FailOnSecondBatch(FakeEmbeddingClient):
        def embed(self, texts):
            result = super().embed(texts)
            if self.calls == 2:
                raise RuntimeError("checkpointed batch failure")
            return result

    failing = IndexingService(
        Settings(
            repo_path=str(tmp_path),
            repo_name="MC",
            state_path=state_path,
            watch_interval=0.01,
            reconcile_interval=60,
        ),
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        FailOnSecondBatch(),
        index,
    )

    with pytest.raises(RuntimeError, match="checkpointed batch failure"):
        failing.reconcile()

    checkpoint = json.loads((tmp_path / ".index-state.json").read_text(encoding="utf-8"))
    assert checkpoint["snapshot"] is None
    assert len(checkpoint["in_progress"]["manifest"]) == 32

    resumed_embeddings = FakeEmbeddingClient()
    resumed = IndexingService(
        Settings(
            repo_path=str(tmp_path),
            repo_name="MC",
            state_path=state_path,
            watch_interval=0.01,
            reconcile_interval=60,
        ),
        StaticGitInspector(SHA, contract.WORKTREE_DIRTY),
        resumed_embeddings,
        index,
    )
    result = resumed.reconcile()

    assert result["files"] == 33
    assert result["embedded_files"] == 1
    assert resumed_embeddings.calls == 1
    assert resumed.ready is True
    assert resumed.last_reconcile is not None
    assert "in_progress" not in json.loads(
        (tmp_path / ".index-state.json").read_text(encoding="utf-8")
    )
