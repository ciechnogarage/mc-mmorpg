"""Contract tests for the embedding service using the mock backend.

These never download a model: EMBEDDING_BACKEND=mock is deterministic and
pure-stdlib, so the FastAPI request/response contract is fully covered.
"""
import sys
import types

import pytest
import threading
import time
from fastapi.testclient import TestClient

from embedding.app.config import get_settings
from embedding.app.main import create_app
from embedding.app.adapters import BGEM3Adapter


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setenv("EMBEDDING_BACKEND", "mock")
    monkeypatch.setenv("EMBEDDING_DENSE_DIM", "1024")
    monkeypatch.setenv("EMBEDDING_MODEL_REVISION", "a" * 40)
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
    body = resp.json()
    assert body["ready"] is True
    assert body["backend"] == "mock"
    assert body["revision"] == "a" * 40


def test_embed_returns_dense_and_sparse(client):
    resp = client.post("/embed", json={"texts": ["hello world", "second doc"]})
    assert resp.status_code == 200
    body = resp.json()
    assert body["backend"] == "mock"
    assert body["revision"] == "a" * 40
    assert body["dim"] == 1024
    assert len(body["dense"]) == 2
    assert len(body["dense"][0]) == 1024
    assert len(body["sparse"]) == 2
    assert body["sparse"][0]["indices"]
    assert len(body["sparse"][0]["indices"]) == len(body["sparse"][0]["values"])


def test_dense_vectors_are_normalized(client):
    resp = client.post("/embed", json={"texts": ["normalize me"], "return_sparse": False})
    vec = resp.json()["dense"][0]
    norm = sum(v * v for v in vec) ** 0.5
    assert abs(norm - 1.0) < 1e-6


def test_embeddings_are_deterministic(client):
    payload = {"texts": ["repeatable input"]}
    first = client.post("/embed", json=payload).json()
    second = client.post("/embed", json=payload).json()
    assert first["dense"] == second["dense"]
    assert first["sparse"] == second["sparse"]


def test_return_dense_only(client):
    resp = client.post(
        "/embed", json={"texts": ["only dense"], "return_sparse": False}
    )
    body = resp.json()
    assert body["dense"] is not None
    assert body["sparse"] is None


def test_rejecting_both_channels_disabled(client):
    resp = client.post(
        "/embed",
        json={"texts": ["x"], "return_dense": False, "return_sparse": False},
    )
    assert resp.status_code == 400


def test_empty_texts_is_unprocessable(client):
    resp = client.post("/embed", json={"texts": []})
    assert resp.status_code == 422


def test_too_many_texts_rejected(client, monkeypatch):
    resp = client.post("/embed", json={"texts": ["t"] * 1000})
    assert resp.status_code == 400


def test_real_embedding_inference_is_serialized(monkeypatch):
    settings = get_settings()
    adapter = BGEM3Adapter(settings)
    active = 0
    max_active = 0
    guard = threading.Lock()

    class Model:
        def encode(self, texts, **kwargs):
            nonlocal active, max_active
            with guard:
                active += 1
                max_active = max(max_active, active)
            time.sleep(0.03)
            with guard:
                active -= 1
            return {
                "dense_vecs": [[1.0] * settings.dense_dim for _ in texts],
                "lexical_weights": [{1: 1.0} for _ in texts],
            }

    adapter._model = Model()
    threads = [
        threading.Thread(target=adapter.embed, args=(["x"], True, True))
        for _ in range(2)
    ]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    assert max_active == 1


def test_real_embedding_sorts_by_length_and_restores_request_order():
    settings = get_settings()
    adapter = BGEM3Adapter(settings)
    seen = []

    class Model:
        def encode(self, texts, **kwargs):
            seen.extend(texts)
            return {
                "dense_vecs": [[float(len(text))] for text in texts],
                "lexical_weights": [{len(text): float(len(text))} for text in texts],
            }

    adapter._model = Model()
    result = adapter.embed(["a", "longest", "mid"], True, True)

    assert seen == ["longest", "mid", "a"]
    assert result["dense"] == [[1.0], [7.0], [3.0]]
    assert result["sparse"] == [
        {"indices": [1], "values": [1.0]},
        {"indices": [7], "values": [7.0]},
        {"indices": [3], "values": [3.0]},
    ]


def test_real_embedding_downloads_the_exact_configured_revision(monkeypatch):
    monkeypatch.setenv("EMBEDDING_MODEL_REVISION", "b" * 40)
    settings = get_settings()
    seen = {}

    hub = types.ModuleType("huggingface_hub")

    def snapshot_download(*, repo_id, revision):
        seen["download"] = (repo_id, revision)
        return "/models/pinned-embedding"

    hub.snapshot_download = snapshot_download
    flag = types.ModuleType("FlagEmbedding")

    class Model:
        def __init__(self, model_path, **kwargs):
            seen["load"] = (model_path, kwargs)

    flag.BGEM3FlagModel = Model
    monkeypatch.setitem(sys.modules, "huggingface_hub", hub)
    monkeypatch.setitem(sys.modules, "FlagEmbedding", flag)

    BGEM3Adapter(settings)._load()
    assert seen["download"] == (settings.model_name, "b" * 40)
    assert seen["load"][0] == "/models/pinned-embedding"
