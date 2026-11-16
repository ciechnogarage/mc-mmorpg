from __future__ import annotations

from fastapi.testclient import TestClient

from api.app.indexer_client import IndexerError
from api.app.main import create_app
from api.app.service import QuerySnapshot
from ragcore.qdrant_index import InMemoryVectorIndex


SHA = "a" * 40
SNAPSHOT_ID = "dirty:" + "b" * 64


class PublishedSnapshots:
    def get_snapshot(self):
        return QuerySnapshot(SHA, "dirty", SNAPSHOT_ID)


class RetrievalStub:
    def __init__(self):
        self.index = InMemoryVectorIndex()
        self.published_snapshots = PublishedSnapshots()


class IndexerStub:
    def __init__(self):
        self.calls = []

    def get_source(self, params):
        self.calls.append(("source", params))
        return {
            "path": params["path"],
            "snapshot": {
                "commit_sha": SHA,
                "worktree_state": "dirty",
                "snapshot_id": SNAPSHOT_ID,
            },
            "content": "published source",
        }

    def index_status(self):
        self.calls.append(("status", {}))
        return {"published_snapshot": {"published": True, "snapshot_id": SNAPSHOT_ID}}

    def submit_event(self, payload):
        self.calls.append(("event", payload))
        return {"accepted": True, "event_type": payload["event_type"]}


def test_operations_are_thin_indexer_proxies_with_snapshot_selectors():
    indexer = IndexerStub()
    with TestClient(create_app(service=RetrievalStub(), indexer=indexer)) as client:
        source = client.get(
            "/source",
            params={
                "path": "docs/guide.md",
                "commit_sha": SHA,
                "worktree_state": "dirty",
                "snapshot_id": SNAPSHOT_ID,
                "line_start": 3,
                "line_end": 7,
            },
        )
        status = client.get("/index-status")
        event = client.post(
            "/events",
            json={"event_type": "runtime_report", "paths": ["reports/run.md"]},
        )

    assert source.status_code == status.status_code == event.status_code == 200
    assert source.json()["snapshot"]["snapshot_id"] == SNAPSHOT_ID
    assert status.json()["published_snapshot"]["published"] is True
    assert event.json() == {"accepted": True, "event_type": "runtime_report"}
    assert indexer.calls[0] == (
        "source",
        {
            "path": "docs/guide.md",
            "commit_sha": SHA,
            "worktree_state": "dirty",
            "snapshot_id": SNAPSHOT_ID,
            "line_start": 3,
            "line_end": 7,
        },
    )


def test_indexer_errors_keep_status_and_detail_at_api_boundary():
    class FailingIndexer(IndexerStub):
        def get_source(self, params):
            raise IndexerError(409, "historical dirty source is unavailable")

    with TestClient(
        create_app(service=RetrievalStub(), indexer=FailingIndexer())
    ) as client:
        response = client.get("/source", params={"path": "docs/guide.md"})

    assert response.status_code == 409
    assert response.json() == {"detail": "historical dirty source is unavailable"}
