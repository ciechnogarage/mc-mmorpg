from __future__ import annotations

import json

import pytest
from mcp.shared.memory import create_connected_server_and_client_session

from knowledge_mcp.app.client import HttpRagApiClient, RagApiError
from knowledge_mcp.app.config import Settings
from knowledge_mcp.app.server import INSTRUCTIONS, build_server
from ragcore.events import EVENT_TYPES


pytestmark = pytest.mark.anyio


@pytest.fixture
def anyio_backend():
    return "asyncio"


class FakeRagApi:
    def __init__(self) -> None:
        self.calls: list[tuple[str, dict]] = []

    def search(self, **kwargs):
        self.calls.append(("search", kwargs))
        return {
            "snapshot": {
                "commit_sha": "a" * 40,
                "worktree_state": "dirty",
                "snapshot_id": "dirty:" + "b" * 64,
            },
            "results": [
                {
                    "text": "boss validation",
                    "citation": {
                        "path": "docs/boss.md",
                        "line_start": 10,
                        "line_end": 14,
                        "commit_sha": "a" * 40,
                        "worktree_state": "dirty",
                        "snapshot_id": "dirty:" + "b" * 64,
                        "evidence_kind": "expected",
                        "collection": "expected_docs",
                        "content_hash": "c" * 64,
                        "embedding_model": "BAAI/bge-m3",
                        "embedding_model_revision": "1" * 40,
                    },
                    "scores": {"fusion": 0.5, "rerank": 0.9, "score": 0.9},
                }
            ],
        }

    def compare(self, **kwargs):
        self.calls.append(("compare", kwargs))
        return {
            "snapshot": {
                "commit_sha": "a" * 40,
                "worktree_state": "dirty",
                "snapshot_id": "dirty:" + "b" * 64,
            },
            "expected": [],
            "observed": [],
        }

    def get_source(self, **kwargs):
        self.calls.append(("source", kwargs))
        return {
            "path": kwargs["path"],
            "snapshot": {
                "commit_sha": "a" * 40,
                "worktree_state": "dirty",
                "snapshot_id": "dirty:" + "b" * 64,
            },
            "collection": "expected_docs",
            "evidence_kind": "expected",
            "content_hash": "c" * 64,
            "line_start": kwargs.get("line_start") or 1,
            "line_end": kwargs.get("line_end") or 1,
            "total_lines": 1,
            "truncated": False,
            "content": "source",
        }

    def index_status(self):
        self.calls.append(("status", {}))
        return {"published_snapshot": {"published": True}}

    def submit_event(self, **kwargs):
        self.calls.append(("event", kwargs))
        return {"event_type": kwargs["event_type"], "accepted": True}


def decoded(result):
    if result.structuredContent is not None:
        return result.structuredContent
    return json.loads(result.content[0].text)


def non_null_schema(schema):
    return next(
        (candidate for candidate in schema.get("anyOf", []) if candidate.get("type") != "null"),
        schema,
    )


async def test_protocol_lists_exact_tools_with_annotations_and_instructions():
    server = build_server(FakeRagApi(), Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(server) as session:
        tools = (await session.list_tools()).tools

    assert {tool.name for tool in tools} == {
        "search_knowledge",
        "compare_expected_observed",
        "get_source",
        "get_index_status",
        "submit_index_event",
    }
    annotations = {tool.name: tool.annotations for tool in tools}
    for name in (
        "search_knowledge",
        "compare_expected_observed",
        "get_source",
        "get_index_status",
    ):
        assert annotations[name].readOnlyHint is True
        assert annotations[name].destructiveHint is False
    assert annotations["submit_index_event"].readOnlyHint is False
    assert annotations["submit_index_event"].idempotentHint is True
    assert "Qdrant is a disposable, rebuildable index" in INSTRUCTIONS[:512]
    assert "never mixed silently" in INSTRUCTIONS[:512]
    assert "path/line citation" in INSTRUCTIONS[:512]


async def test_protocol_advertises_enums_bounds_and_structured_outputs():
    server = build_server(FakeRagApi(), Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(server) as session:
        tools = {tool.name: tool for tool in (await session.list_tools()).tools}

    search = tools["search_knowledge"]
    search_properties = search.inputSchema["properties"]
    assert search_properties["query"]["minLength"] == 1
    assert search_properties["query"]["maxLength"] == 2000
    assert search_properties["limit"]["minimum"] == 1
    assert search_properties["limit"]["maximum"] == 100
    collections = non_null_schema(search_properties["collections"])
    assert collections["maxItems"] == 4
    assert set(collections["items"]["enum"]) == {
        "expected_docs",
        "expected_config",
        "observed_runtime",
        "observed_benchmarks",
    }
    assert search.outputSchema is not None
    assert {"snapshot", "results"} <= set(search.outputSchema["properties"])

    source_properties = tools["get_source"].inputSchema["properties"]
    assert source_properties["path"]["maxLength"] == 4096
    assert non_null_schema(source_properties["commit_sha"])["pattern"].endswith("{40}$")
    assert set(non_null_schema(source_properties["worktree_state"])["enum"]) == {
        "clean",
        "dirty",
        "unknown",
    }
    assert non_null_schema(source_properties["snapshot_id"])["pattern"].endswith(
        "[0-9a-f]{64}$"
    )
    assert non_null_schema(source_properties["line_start"])["minimum"] == 1
    assert tools["get_source"].outputSchema is not None

    event = tools["submit_index_event"]
    event_properties = event.inputSchema["properties"]
    assert set(event_properties["event_type"]["enum"]) == set(EVENT_TYPES)
    paths = non_null_schema(event_properties["paths"])
    assert paths["maxItems"] == 100
    assert paths["items"]["maxLength"] == 4096
    assert event.outputSchema is not None


async def test_search_preserves_api_citations_over_protocol():
    fake = FakeRagApi()
    server = build_server(fake, Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(server) as session:
        result = await session.call_tool(
            "search_knowledge",
            {"query": "boss validation", "collections": ["expected_docs"]},
        )

    assert result.isError is False
    body = decoded(result)
    assert body["results"][0]["citation"]["path"] == "docs/boss.md"
    assert body["results"][0]["citation"]["snapshot_id"].startswith("dirty:")
    assert fake.calls == [
        (
            "search",
            {
                "query": "boss validation",
                "collections": ["expected_docs"],
                "commit_sha": None,
                "worktree_state": None,
                "snapshot_id": None,
                "limit": 10,
            },
        )
    ]


@pytest.mark.parametrize("event_type", EVENT_TYPES)
async def test_submit_index_event_accepts_exact_six_event_types(event_type):
    fake = FakeRagApi()
    server = build_server(fake, Settings(host="127.0.0.1"))
    arguments = {"event_type": event_type}
    if event_type != "commit":
        arguments["paths"] = ["docs/example.md"]
    async with create_connected_server_and_client_session(server) as session:
        result = await session.call_tool("submit_index_event", arguments)
    assert result.isError is False
    assert decoded(result)["event_type"] == event_type


async def test_protocol_surfaces_validation_and_rag_api_errors():
    server = build_server(FakeRagApi(), Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(server) as session:
        invalid = await session.call_tool(
            "submit_index_event", {"event_type": "delete_everything"}
        )
    assert invalid.isError is True
    assert "commit" in invalid.content[0].text
    assert "bot_benchmark" in invalid.content[0].text

    class FailingApi(FakeRagApi):
        def index_status(self):
            raise RagApiError(503, "indexer is unavailable")

    failing = build_server(FailingApi(), Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(failing) as session:
        unavailable = await session.call_tool("get_index_status", {})
    assert unavailable.isError is True
    assert "rag-api error 503" in unavailable.content[0].text


async def test_get_source_and_compare_are_api_only_pass_throughs():
    fake = FakeRagApi()
    server = build_server(fake, Settings(host="127.0.0.1"))
    async with create_connected_server_and_client_session(server) as session:
        source = await session.call_tool(
            "get_source", {"path": "docs/boss.md", "line_start": 10, "line_end": 14}
        )
        compare = await session.call_tool(
            "compare_expected_observed", {"query": "boss validation"}
        )
    assert decoded(source)["content"] == "source"
    assert decoded(compare).keys() == {"snapshot", "expected", "observed"}
    assert [name for name, _arguments in fake.calls] == ["source", "compare"]


async def test_http_client_preserves_explicit_empty_collection_lists(monkeypatch):
    payloads = []

    class Response:
        status_code = 200

        def json(self):
            return {"snapshot": {}, "results": [], "expected": [], "observed": []}

    def fake_post(url, json, timeout):
        payloads.append((url, json))
        return Response()

    monkeypatch.setattr("httpx.post", fake_post)
    client = HttpRagApiClient("http://rag-api")
    client.search(query="q", collections=[])
    client.compare(query="q", expected_collections=[], observed_collections=[])

    assert payloads[0][1]["collections"] == []
    assert payloads[1][1]["expected_collections"] == []
    assert payloads[1][1]["observed_collections"] == []
