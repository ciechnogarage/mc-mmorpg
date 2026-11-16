"""Contract tests for the reranker service using the mock backend."""
import sys
import types

import pytest
import threading
import time
from fastapi.testclient import TestClient

from reranker.app.config import get_settings
from reranker.app.main import create_app
from reranker.app.adapters import BGERerankerAdapter


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setenv("RERANKER_BACKEND", "mock")
    monkeypatch.setenv("RERANKER_MODEL_REVISION", "c" * 40)
    app = create_app(get_settings())
    with TestClient(app) as c:
        yield c


def test_health_is_live(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_ready_true_for_mock(client):
    resp = client.get("/ready")
    assert resp.status_code == 200
    assert resp.json()["ready"] is True
    assert resp.json()["revision"] == "c" * 40


def test_rerank_orders_by_relevance(client):
    resp = client.post(
        "/rerank",
        json={
            "query": "moss stalker model engine",
            "documents": [
                "unrelated text about cooking recipes",
                "the moss stalker model engine blueprint",
                "model engine reference",
            ],
        },
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["backend"] == "mock"
    assert body["revision"] == "c" * 40
    # Most relevant document (index 1) should rank first.
    assert body["results"][0]["index"] == 1
    # Scores are monotonically non-increasing.
    scores = [r["score"] for r in body["results"]]
    assert scores == sorted(scores, reverse=True)


def test_top_n_truncates(client):
    resp = client.post(
        "/rerank",
        json={
            "query": "alpha beta",
            "documents": ["alpha", "beta", "gamma", "alpha beta"],
            "top_n": 2,
        },
    )
    body = resp.json()
    assert len(body["results"]) == 2


def test_rerank_is_deterministic(client):
    payload = {"query": "same query", "documents": ["doc a", "doc b same query"]}
    first = client.post("/rerank", json=payload).json()
    second = client.post("/rerank", json=payload).json()
    assert first["results"] == second["results"]


def test_empty_documents_is_unprocessable(client):
    resp = client.post("/rerank", json={"query": "q", "documents": []})
    assert resp.status_code == 422


def test_empty_query_is_unprocessable(client):
    resp = client.post("/rerank", json={"query": "", "documents": ["d"]})
    assert resp.status_code == 422


def test_too_many_documents_rejected(client):
    resp = client.post(
        "/rerank", json={"query": "q", "documents": ["d"] * 1000}
    )
    assert resp.status_code == 400


def test_real_reranker_inference_is_serialized():
    settings = get_settings()
    adapter = BGERerankerAdapter(settings)
    active = 0
    max_active = 0
    guard = threading.Lock()

    class Model:
        def compute_score(self, pairs, **kwargs):
            nonlocal active, max_active
            with guard:
                active += 1
                max_active = max(max_active, active)
            time.sleep(0.03)
            with guard:
                active -= 1
            return [1.0] * len(pairs)

    adapter._model = Model()
    threads = [
        threading.Thread(target=adapter.rerank, args=("q", ["d"], None))
        for _ in range(2)
    ]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    assert max_active == 1


def test_real_reranker_downloads_the_exact_configured_revision(monkeypatch):
    monkeypatch.setenv("RERANKER_MODEL_REVISION", "d" * 40)
    settings = get_settings()
    seen = {}

    hub = types.ModuleType("huggingface_hub")

    def snapshot_download(*, repo_id, revision):
        seen["download"] = (repo_id, revision)
        return "/models/pinned-reranker"

    hub.snapshot_download = snapshot_download
    flag = types.ModuleType("FlagEmbedding")

    class Model:
        def __init__(self, model_path, **kwargs):
            seen["load"] = (model_path, kwargs)

    flag.FlagReranker = Model
    monkeypatch.setitem(sys.modules, "huggingface_hub", hub)
    monkeypatch.setitem(sys.modules, "FlagEmbedding", flag)

    BGERerankerAdapter(settings)._load()
    assert seen["download"] == (settings.model_name, "d" * 40)
    assert seen["load"][0] == "/models/pinned-reranker"
