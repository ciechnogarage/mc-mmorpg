from __future__ import annotations

from types import SimpleNamespace

import pytest

from ragcore import chunk, classify, contract, events
from ragcore.qdrant_index import QdrantVectorIndex


@pytest.mark.parametrize(
    ("path", "collection"),
    [
        ("docs/guide.md", "expected_docs"),
        ("MCMMORPG/plugins/MMOCore/config.yml", "expected_config"),
        ("MCMMORPG/logs/runtime-report.md", "observed_runtime"),
        ("reports/benchmarks/bot-benchmark.json", "observed_benchmarks"),
    ],
)
def test_four_collection_routes(path, collection):
    assert classify.classify(path).collection == collection


@pytest.mark.parametrize(
    ("path", "collection"),
    [
        ("MCMMORPG/_validation/foundation_runtime.js", "expected_config"),
        (
            "MCMMORPG/_validation/test_active_runtime_review_drafts.js",
            "expected_config",
        ),
        ("docs/ai/foundation-runtime-baseline.md", "expected_docs"),
        (
            "MCMMORPG/plugin-src/minion-core/tools/runtime-evidence-contract.md",
            "expected_docs",
        ),
        ("reports/runtime/2026-07-21.json", "observed_runtime"),
        ("MCMMORPG/logs/runtime-report.md", "observed_runtime"),
        (
            "docs/audits/2026-07-13/runtime-startup-report.md",
            "observed_runtime",
        ),
    ],
)
def test_runtime_words_do_not_turn_durable_sources_into_observed_evidence(
    path, collection
):
    assert classify.classify(path).collection == collection


@pytest.mark.parametrize(
    "path",
    [
        "../outside.md",
        "/etc/passwd",
        "C:\\secrets.txt",
        "MCMMORPG/world/level.dat",
        "MCMMORPG/playerdata/player.json",
        "plugins/MMOCore/userdata/player.yml",
        "plugins/MythicMobs/data/players/player.yml",
        "plugins/FastAsyncWorldEdit/sessions/player.json",
        "plugins/MythicDungeons/backups/config.yml",
        "plugins/secret-token.yml",
        "plugins/plugin.jar",
        "ModelEngine/.data/cache.json",
        "MCMMORPG/plugins/Iris/packs/level_1/.iris/schema/biomes-schema.json",
        "models/boss.bbmodel",
        ".venv/cache.json",
    ],
)
def test_unsafe_or_sensitive_paths_are_excluded(path):
    assert classify.classify(path) is None


def test_chunking_is_deterministic_and_keeps_heading_lines():
    text = "# One\nalpha\nbeta\n## Two\ngamma"
    first = chunk.chunk_text(text, "doc.md", max_lines=3)
    second = chunk.chunk_text(text, "doc.md", max_lines=3)
    assert first == second
    assert [(item.heading, item.line_start, item.line_end) for item in first] == [
        ("One", 1, 3),
        ("Two", 4, 5),
    ]


def test_point_identity_includes_repo_and_snapshot():
    snap = "dirty:" + "c" * 64
    base = contract.point_id("MC", "a" * 40, "dirty", snap, "docs/a.md", 0)
    assert base == contract.point_id("MC", "a" * 40, "dirty", snap, "docs/a.md", 0)
    assert base != contract.point_id("fork", "a" * 40, "dirty", snap, "docs/a.md", 0)
    assert base != contract.point_id("MC", "b" * 40, "dirty", snap, "docs/a.md", 0)
    assert base != contract.point_id(
        "MC", "a" * 40, "dirty", "dirty:" + "d" * 64, "docs/a.md", 0
    )


def test_snapshot_id_is_content_bound_even_for_clean_worktree():
    from ragcore.snapshot import snapshot_id_for

    first = {"reports/runtime.json": SimpleNamespace(content_hash="a" * 64)}
    second = {"reports/runtime.json": SimpleNamespace(content_hash="b" * 64)}
    assert snapshot_id_for("a" * 40, "clean", first) != snapshot_id_for(
        "a" * 40, "clean", second
    )


def test_snapshot_id_is_canonical_and_bound_to_effective_retrieval_semantics():
    from ragcore.snapshot import snapshot_id_for

    records = {
        "b.json": SimpleNamespace(content_hash="b" * 64),
        "a.md": SimpleNamespace(content_hash="a" * 64),
    }
    first = {
        "b.json": {
            "collection": "expected_config",
            "event_type": None,
            "observed_at": None,
            "doc_version": None,
        },
        "a.md": {
            "collection": "expected_docs",
            "event_type": None,
            "observed_at": None,
            "doc_version": None,
        },
    }
    reordered = {"a.md": dict(first["a.md"]), "b.json": dict(first["b.json"])}
    observed = {
        **reordered,
        "b.json": {
            "collection": "observed_benchmarks",
            "event_type": "test_completed",
            "observed_at": "2026-07-21T12:00:00Z",
            "doc_version": "run-9",
        },
    }
    baseline = snapshot_id_for("a" * 40, "dirty", records, first)
    assert baseline == snapshot_id_for("a" * 40, "dirty", records, reordered)
    assert baseline != snapshot_id_for("a" * 40, "dirty", records, observed)


def test_snapshot_id_is_bound_to_embedding_model_revision():
    from ragcore.snapshot import snapshot_id_for

    records = {"docs/a.md": SimpleNamespace(content_hash="a" * 64)}
    first = snapshot_id_for(
        "a" * 40,
        "dirty",
        records,
        index_profile={"embedding_model": "BAAI/bge-m3", "embedding_revision": "1" * 40},
    )
    second = snapshot_id_for(
        "a" * 40,
        "dirty",
        records,
        index_profile={"embedding_model": "BAAI/bge-m3", "embedding_revision": "2" * 40},
    )
    assert first != second


def test_exact_six_event_contract():
    assert events.EVENT_TYPES == (
        "commit",
        "documentation",
        "plugin",
        "test_completed",
        "runtime_report",
        "bot_benchmark",
    )
    with pytest.raises(ValueError):
        events.WebhookEvent("other")
    with pytest.raises(ValueError):
        events.WebhookEvent("documentation")
    with pytest.raises(ValueError):
        events.WebhookEvent("plugin", ["../escape.yml"])
    with pytest.raises(ValueError):
        contract.validate_commit_sha("main")


def test_secret_values_are_redacted_without_changing_line_count():
    from ragcore.sanitize import redact_secrets

    raw = 'password: hunter2\n"api_key": "abc123"\nurl: postgres://user:pass@db/name'
    safe = redact_secrets(raw)
    assert "hunter2" not in safe
    assert "abc123" not in safe
    assert "user:pass" not in safe
    assert safe.count("\n") == raw.count("\n")


@pytest.mark.parametrize(
    "path",
    [
        "MCMMORPG/ops.json",
        "MCMMORPG/whitelist.json",
        "MCMMORPG/banned-players.json",
        "MCMMORPG/banned-ips.json",
        "MCMMORPG/usercache.json",
        "MCMMORPG/docker/servers/world/ops.json",
        "MCMMORPG/logs/latest.log",
        "MCMMORPG/docker/servers/items/logs/latest.log",
        "MCMMORPG/logs/chat.json",
    ],
)
def test_player_operator_auth_stores_and_raw_logs_are_denied(path):
    assert classify.classify(path) is None


def test_dotted_hyphenated_and_environment_secret_keys_are_redacted():
    from ragcore.sanitize import redact_secrets

    raw = "\n".join(
        [
            "rcon.password=alpha",
            "db.password: beta",
            "MARIADB_PASSWORD=gamma",
            "client-secret: delta",
            "export API_TOKEN=epsilon",
            '"auth.token": "zeta",',
            "useful.setting=true",
        ]
    )
    safe = redact_secrets(raw)
    for secret in ("alpha", "beta", "gamma", "delta", "epsilon", "zeta"):
        assert secret not in safe
    assert "useful.setting=true" in safe


def test_camel_case_and_yaml_block_scalar_secrets_are_fully_redacted():
    from ragcore.sanitize import redact_secrets

    raw = "\n".join(
        [
            "accessToken: DELTA",
            "clientSecret: EPSILON",
            "privateKey: |",
            "  -----BEGIN PRIVATE KEY-----",
            "  SUPERSECRETKEY",
            "  -----END PRIVATE KEY-----",
            "databasePassword: >-",
            "  first continuation",
            "  second continuation",
            "usefulSetting: retained",
        ]
    )
    safe = redact_secrets(raw)
    for secret in (
        "DELTA",
        "EPSILON",
        "SUPERSECRETKEY",
        "first continuation",
        "second continuation",
    ):
        assert secret not in safe
    assert safe.count("\n") == raw.count("\n")
    assert "usefulSetting: retained" in safe


class RecordingClient:
    def __init__(self):
        self.kwargs = None

    def query_points(self, **kwargs):
        self.kwargs = kwargs
        return SimpleNamespace(
            points=[SimpleNamespace(id="point", payload={"path": "docs/a.md"}, score=0.7)]
        )


def test_real_adapter_uses_query_api_prefetch_rrf_and_exact_filters():
    from qdrant_client import models

    adapter = object.__new__(QdrantVectorIndex)
    adapter.client = RecordingClient()
    adapter.dense_dim = 2
    hits = adapter.hybrid_query(
        "expected_docs",
        [0.1, 0.2],
        {"indices": [1], "values": [0.5]},
        "MC",
        "a" * 40,
        "dirty",
        "dirty:" + "c" * 64,
        "BAAI/bge-m3",
        "1" * 40,
        5,
        20,
    )
    call = adapter.client.kwargs
    assert [prefetch.using for prefetch in call["prefetch"]] == ["dense", "sparse"]
    assert call["query"].fusion is models.Fusion.RRF
    exact = {
        condition.key: condition.match.value
        for condition in call["query_filter"].must
    }
    assert exact == {
        "repo": "MC",
        "commit_sha": "a" * 40,
        "worktree_state": "dirty",
        "snapshot_id": "dirty:" + "c" * 64,
        "embedding_model": "BAAI/bge-m3",
        "embedding_model_revision": "1" * 40,
    }
    assert hits[0].fusion_score == 0.7


def test_http_model_clients_batch_to_service_limits(monkeypatch):
    import httpx

    from ragcore.embedding_client import HttpEmbeddingClient
    from ragcore.reranker_client import HttpRerankerClient

    calls = []

    class Response:
        def __init__(self, body):
            self.body = body

        def raise_for_status(self):
            return None

        def json(self):
            return self.body

    def fake_post(url, json, timeout):
        calls.append((url, json, timeout))
        if url.endswith("/embed"):
            size = len(json["texts"])
            return Response({"dense": [[1.0]] * size, "sparse": [{"indices": [], "values": []}] * size})
        return Response(
            {
                "results": [
                    {"index": index, "score": float(index + 1)}
                    for index in range(len(json["documents"]))
                ]
            }
        )

    monkeypatch.setattr(httpx, "post", fake_post)
    dense, sparse = HttpEmbeddingClient("http://embed", max_texts=2).embed(["a", "b", "c"])
    scores = HttpRerankerClient("http://rerank", max_documents=2).rerank(
        "q", ["a", "b", "c"]
    )
    assert len(dense) == len(sparse) == 3
    assert scores == [1.0, 2.0, 1.0]
    assert [len(call[1].get("texts", call[1].get("documents", []))) for call in calls] == [2, 1, 2, 1]
    assert [call[2] for call in calls[:2]] == [300.0, 300.0]


def test_http_embedding_client_retries_transient_503(monkeypatch):
    import httpx

    from ragcore.embedding_client import HttpEmbeddingClient

    calls = []

    class Response:
        def __init__(self, status_code, body):
            self.status_code = status_code
            self._body = body
            self.request = httpx.Request("POST", "http://embed/embed")

        def raise_for_status(self):
            if self.status_code >= 400:
                raise httpx.HTTPStatusError(
                    "transient failure",
                    request=self.request,
                    response=httpx.Response(
                        self.status_code, request=self.request, json=self._body
                    ),
                )

        def json(self):
            return self._body

    def fake_post(url, json, timeout):
        calls.append((url, json, timeout))
        if len(calls) == 1:
            return Response(503, {"detail": "loading"})
        size = len(json["texts"])
        return Response(
            200,
            {
                "dense": [[1.0]] * size,
                "sparse": [{"indices": [], "values": []}] * size,
            },
        )

    monkeypatch.setattr(httpx, "post", fake_post)
    monkeypatch.setattr("ragcore.embedding_client.time.sleep", lambda _seconds: None)

    dense, sparse = HttpEmbeddingClient(
        "http://embed", max_texts=2, retry_attempts=2, retry_backoff_seconds=0.1
    ).embed(["a", "b"])

    assert len(calls) == 2
    assert len(dense) == len(sparse) == 2


def test_http_embedding_client_retries_remote_protocol_error(monkeypatch):
    import httpx

    from ragcore.embedding_client import HttpEmbeddingClient

    calls = []

    def fake_post(url, json, timeout):
        calls.append((url, json, timeout))
        if len(calls) == 1:
            raise httpx.RemoteProtocolError("server disconnected")
        size = len(json["texts"])
        return httpx.Response(
            200,
            request=httpx.Request("POST", url),
            json={
                "dense": [[1.0]] * size,
                "sparse": [{"indices": [], "values": []}] * size,
            },
        )

    monkeypatch.setattr(httpx, "post", fake_post)
    monkeypatch.setattr("ragcore.embedding_client.time.sleep", lambda _seconds: None)

    dense, sparse = HttpEmbeddingClient(
        "http://embed", max_texts=2, retry_attempts=2, retry_backoff_seconds=0.1
    ).embed(["a", "b"])

    assert len(calls) == 2
    assert len(dense) == len(sparse) == 2
