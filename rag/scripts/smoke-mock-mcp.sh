#!/usr/bin/env bash
set -euo pipefail

rag_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fixture_dir="$(mktemp -d)"
project_name="mc-rag-smoke-${USER:-local}-$$"

cleanup() {
  docker compose --project-directory "$rag_dir" -p "$project_name" down -v --remove-orphans >/dev/null 2>&1 || true
  rm -rf "$fixture_dir"
}
trap cleanup EXIT

cp -R "$rag_dir/fixtures/smoke/." "$fixture_dir/"
git -C "$fixture_dir" init -q
git -C "$fixture_dir" config user.name "MC RAG smoke"
git -C "$fixture_dir" config user.email "mc-rag-smoke@localhost"
git -C "$fixture_dir" add .
git -C "$fixture_dir" commit -qm "Create deterministic smoke fixture"

export MC_REPO_PATH="$fixture_dir"
export REPO_NAME="MC-smoke"
export EMBEDDING_BACKEND=mock
export EMBEDDING_IMAGE_TARGET=mock
export RERANKER_BACKEND=mock
export RERANKER_IMAGE_TARGET=mock
export QDRANT_HTTP_PORT="${QDRANT_HTTP_PORT:-26333}"
export QDRANT_GRPC_PORT="${QDRANT_GRPC_PORT:-26334}"
export EMBEDDING_PORT="${EMBEDDING_PORT:-28080}"
export RERANKER_PORT="${RERANKER_PORT:-28081}"
export RAG_INDEXER_PORT="${RAG_INDEXER_PORT:-28082}"
export RAG_API_PORT="${RAG_API_PORT:-28090}"
export MCP_PORT="${MCP_PORT:-28091}"

compose=(docker compose --project-directory "$rag_dir" -p "$project_name")
"${compose[@]}" build embedding-service reranker-service rag-indexer rag-api mc-knowledge-mcp
"${compose[@]}" up -d

ready=0
for _attempt in $(seq 1 90); do
  if curl -fs "http://127.0.0.1:${MCP_PORT}/health" >/dev/null \
    && curl -fs "http://127.0.0.1:${RAG_API_PORT}/ready" >/dev/null; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" != "1" ]]; then
  "${compose[@]}" ps
  "${compose[@]}" logs --no-color --tail=200
  exit 1
fi

"${compose[@]}" exec -T mc-knowledge-mcp python - <<'PY'
import json

import anyio
from mcp import ClientSession
from mcp.client.streamable_http import streamable_http_client


def decoded(result):
    if result.structuredContent is not None:
        return result.structuredContent
    return json.loads(result.content[0].text)


async def main():
    async with streamable_http_client("http://127.0.0.1:8000/mcp") as streams:
        async with ClientSession(streams[0], streams[1]) as session:
            initialized = await session.initialize()
            tools = (await session.list_tools()).tools
            assert {tool.name for tool in tools} == {
                "search_knowledge",
                "compare_expected_observed",
                "get_source",
                "get_index_status",
                "submit_index_event",
            }

            search = await session.call_tool(
                "search_knowledge", {"query": "obsidian dragon violet warning pulse"}
            )
            assert search.isError is False
            search_body = decoded(search)
            assert search_body["results"]
            citation = search_body["results"][0]["citation"]
            assert citation["path"] == "docs/expected.md"
            assert citation["commit_sha"]
            assert citation["snapshot_id"]
            assert citation["worktree_state"] == "clean"

            source = await session.call_tool(
                "get_source", {"path": citation["path"], "line_start": 1, "line_end": 8}
            )
            assert source.isError is False
            assert "violet warning pulse" in decoded(source)["content"]

            comparison = await session.call_tool(
                "compare_expected_observed",
                {"query": "obsidian dragon violet warning pulse"},
            )
            comparison_body = decoded(comparison)
            assert comparison_body["expected"]
            assert comparison_body["observed"]

            event = await session.call_tool(
                "submit_index_event",
                {"event_type": "documentation", "paths": ["docs/expected.md"]},
            )
            assert event.isError is False

            status = await session.call_tool("get_index_status", {})
            assert decoded(status)["published_snapshot"]["published"] is True
            print(
                json.dumps(
                    {
                        "protocol": initialized.protocolVersion,
                        "tools": len(tools),
                        "citation": citation,
                        "status": "ok",
                    },
                    sort_keys=True,
                )
            )


anyio.run(main)
PY
