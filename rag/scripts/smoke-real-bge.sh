#!/usr/bin/env bash
set -euo pipefail

rag_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
max_wait_seconds="${REAL_SMOKE_MAX_WAIT_SECONDS:-86400}"
deadline="$(( $(date +%s) + max_wait_seconds ))"
compose=(docker compose --project-directory "$rag_dir")

log() {
  printf '%s %s\n' "$(date --iso-8601=seconds)" "$*"
}

points_total() {
  local total=0 collection count
  for collection in expected_docs expected_config observed_runtime observed_benchmarks; do
    count="$(
      curl -fsS "http://127.0.0.1:6333/collections/${collection}" 2>/dev/null \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["result"].get("points_count", 0))' \
        2>/dev/null || printf 0
    )"
    total="$((total + count))"
  done
  printf '%s' "$total"
}

log "waiting for a fully published real-BGE repository snapshot"
attempt=0
while true; do
  if curl -fsS http://127.0.0.1:18082/ready >/dev/null 2>&1; then
    break
  fi
  if ! "${compose[@]}" ps --status running --services | grep -qx rag-indexer; then
    log "FAIL: rag-indexer is not running"
    "${compose[@]}" ps -a
    exit 1
  fi
  if (( $(date +%s) >= deadline )); then
    log "FAIL: snapshot was not published within ${max_wait_seconds}s"
    curl -sS http://127.0.0.1:18082/status || true
    exit 1
  fi
  attempt="$((attempt + 1))"
  if (( attempt % 5 == 0 )); then
    log "indexing: points=$(points_total) status=$(curl -fsS http://127.0.0.1:18082/status)"
  fi
  sleep 60
done

published="$(curl -fsS http://127.0.0.1:18082/published-snapshot)"
log "snapshot published: ${published}"

"${compose[@]}" up -d reranker-service
for _attempt in $(seq 1 180); do
  if curl -fsS http://127.0.0.1:18081/ready >/dev/null 2>&1; then
    break
  fi
  sleep 5
done
curl -fsS http://127.0.0.1:18081/ready >/dev/null

"${compose[@]}" up -d rag-api mc-knowledge-mcp
for _attempt in $(seq 1 120); do
  if curl -fsS http://127.0.0.1:18090/ready >/dev/null 2>&1 \
    && curl -fsS http://127.0.0.1:18091/health >/dev/null 2>&1; then
    break
  fi
  sleep 5
done
curl -fsS http://127.0.0.1:18090/ready >/dev/null
curl -fsS http://127.0.0.1:18091/health >/dev/null

"$rag_dir/.venv/bin/python" - <<'PY'
import json

import httpx

timeout = httpx.Timeout(600.0)
embedding = httpx.post(
    "http://127.0.0.1:18080/embed",
    json={
        "texts": [
            "ModelEngine animation atlas for an obsidian dragon",
            "runtime validation report for the Minecraft server",
        ],
        "return_dense": True,
        "return_sparse": True,
    },
    timeout=timeout,
)
embedding.raise_for_status()
embedding_body = embedding.json()
assert embedding_body["backend"] == "bge"
assert embedding_body["revision"] == "5617a9f61b028005a4858fdac845db406aefb181"
assert len(embedding_body["dense"]) == 2
assert len(embedding_body["dense"][0]) == 1024
assert embedding_body["sparse"][0]["indices"]

rerank = httpx.post(
    "http://127.0.0.1:18081/rerank",
    json={
        "query": "ModelEngine animation atlas",
        "documents": [
            "A guide to PostgreSQL backups.",
            "ModelEngine creation atlas with animation cards and runtime evidence.",
        ],
        "top_n": 2,
    },
    timeout=timeout,
)
rerank.raise_for_status()
rerank_body = rerank.json()
assert rerank_body["backend"] == "bge"
assert rerank_body["revision"] == "953dc6f6f85a1b2dbfca4c34a2796e7dde08d41e"
assert rerank_body["results"][0]["index"] == 1

search = httpx.post(
    "http://127.0.0.1:18090/search",
    json={"query": "ModelEngine creation atlas animation cards", "limit": 5},
    timeout=timeout,
)
search.raise_for_status()
search_body = search.json()
assert search_body["results"]
citation = search_body["results"][0]["citation"]
assert citation["commit_sha"]
assert citation["snapshot_id"] == search_body["snapshot"]["snapshot_id"]
assert citation["embedding_model"] == "BAAI/bge-m3"
assert citation["embedding_model_revision"] == "5617a9f61b028005a4858fdac845db406aefb181"

source = httpx.get(
    "http://127.0.0.1:18090/source",
    params={
        "path": citation["path"],
        "line_start": citation["line_start"],
        "line_end": min(citation["line_end"], citation["line_start"] + 3),
    },
    timeout=timeout,
)
source.raise_for_status()
assert source.json()["content"]

print(
    json.dumps(
        {
            "embedding": {
                "model": embedding_body["model"],
                "revision": embedding_body["revision"],
            },
            "reranker": {
                "model": rerank_body["model"],
                "revision": rerank_body["revision"],
            },
            "api_search_citation": citation,
            "status": "ok",
        },
        sort_keys=True,
    )
)
PY

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
                "search_knowledge",
                {"query": "ModelEngine creation atlas animation cards", "limit": 5},
            )
            assert search.isError is False
            search_body = decoded(search)
            assert search_body["results"]
            citation = search_body["results"][0]["citation"]
            assert citation["embedding_model"] == "BAAI/bge-m3"
            assert citation["embedding_model_revision"] == "5617a9f61b028005a4858fdac845db406aefb181"

            source = await session.call_tool(
                "get_source",
                {
                    "path": citation["path"],
                    "line_start": citation["line_start"],
                    "line_end": min(citation["line_end"], citation["line_start"] + 3),
                },
            )
            assert source.isError is False
            assert decoded(source)["content"]

            comparison = await session.call_tool(
                "compare_expected_observed",
                {"query": "runtime startup validation report", "limit": 3},
            )
            comparison_body = decoded(comparison)
            assert comparison_body["expected"]
            assert comparison_body["observed"]

            event = await session.call_tool(
                "submit_index_event", {"event_type": "commit"}
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

"${compose[@]}" ps
log "PASS: real BGE repository smoke completed"
command -v notify-send >/dev/null 2>&1 \
  && notify-send "MC RAG" "Real BGE repository smoke passed" \
  || true
