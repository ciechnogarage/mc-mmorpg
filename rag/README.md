# MC hybrid RAG stack

Local, CPU-capable retrieval for the MC repository. Git, versioned config,
documentation, runtime reports and completed test/benchmark artifacts remain
the source of truth. Qdrant is only a disposable, rebuildable search index.

The stack is isolated under `rag/` and does not join the Paper/Velocity Docker
topology under `MCMMORPG/docker`.

## Services and ports

| Service | Purpose | Local URL |
| --- | --- | --- |
| `qdrant` | Dense+sparse index | `127.0.0.1:6333` |
| `embedding-service` | BGE-M3 dense and sparse vectors | `127.0.0.1:18080` |
| `reranker-service` | `bge-reranker-v2-m3` reranking | `127.0.0.1:18081` |
| `rag-indexer` | Published snapshot generation and event ingestion | `127.0.0.1:18082` |
| `rag-api` | Search, compare, source and operational API | `127.0.0.1:18090` |
| `mc-knowledge-mcp` | Five Codex tools over Streamable HTTP | `http://127.0.0.1:18091/mcp` |

Every unauthenticated host port is bound to `127.0.0.1`, not all interfaces.
Containers communicate over the private Compose network. The dependency graph
is acyclic: models/Qdrant → indexer → API → MCP.

## Evidence and snapshot contract

The four collections are:

- `expected_docs` and `expected_config` for expected behavior;
- `observed_runtime` and `observed_benchmarks` for observed behavior.

Every result is pinned to `(commit_sha, worktree_state, snapshot_id)` and has a
repository-relative path/line citation, evidence kind, collection and content
hash. Ordinary search may use only one evidence kind. Cross-evidence retrieval
is explicit through `/compare` or the `compare_expected_observed` MCP tool and
keeps `expected` and `observed` as separate groups.

`GET /source` is path-safe and serves only the current published snapshot. The
path must exist in the published manifest and its current bytes must match the
published fingerprint. Historical or superseded dirty source is explicitly
unavailable because the read-only working tree cannot reconstruct those bytes.
The API and MCP proxy this contract; neither reads repository files directly.

Publication is atomic. Search, source and status continue to use the last fully
published semantic snapshot while a candidate generation is being built or if
that build fails.

## Real and mock modes

Copy the example environment first:

```bash
cd rag
cp .env.example .env
```

Real CPU mode is the default:

```dotenv
EMBEDDING_BACKEND=bge
EMBEDDING_IMAGE_TARGET=real
RERANKER_BACKEND=bge
RERANKER_IMAGE_TARGET=real
```

It downloads BGE-M3 and `bge-reranker-v2-m3` into named volumes. On CPU, first
startup/build can take many minutes and several GB of download/storage. Runtime
latency depends on corpus size; conservative batch sizes and four OpenMP threads
are the defaults.

No-model mock mode is deterministic and does not install Torch, FlagEmbedding
or model weights:

```dotenv
EMBEDDING_BACKEND=mock
EMBEDDING_IMAGE_TARGET=mock
RERANKER_BACKEND=mock
RERANKER_IMAGE_TARGET=mock
```

Use mock mode for protocol, topology and indexing tests. It proves the complete
flow, not semantic model quality.

## Start and inspect

```bash
cd rag
docker compose config -q
docker compose up -d --build
docker compose ps
docker compose logs -f rag-indexer rag-api mc-knowledge-mcp
```

Health/readiness endpoints:

- model `/health` is process liveness; `/ready` requires the selected backend;
- indexer `/ready` requires a fully published snapshot;
- API `/ready` requires the indexer's published snapshot;
- MCP `/health` verifies the Streamable HTTP process is serving.

## MCP tools

`mc-knowledge-mcp` uses `mcp==1.28.1` and FastMCP Streamable HTTP. It talks only
to `rag-api` and exposes exactly:

- read-only `search_knowledge`;
- read-only `compare_expected_observed`;
- read-only `get_source`;
- read-only `get_index_status`;
- write `submit_index_event`.

Tool annotations mark the first four read-only and the event tool as a
non-destructive, idempotent local-index mutation. API failures are surfaced as
tool errors without replacing citation/snapshot payloads.

The MCP schemas publish the same enums and input bounds enforced by the API,
including the six event types, four collections, exact snapshot selectors and
structured citation/source/status/event response shapes.

### Codex registration

Register the local server:

```bash
codex mcp add mc-knowledge --url http://127.0.0.1:18091/mcp
codex mcp list
```

Equivalent `~/.codex/config.toml` (or trusted project `.codex/config.toml`):

```toml
[mcp_servers.mc-knowledge]
url = "http://127.0.0.1:18091/mcp"
```

After adding or changing the server, restart the Codex client/extension and
open a new session. Use `/mcp` in the TUI to confirm the five tools are present.

## Event triggers

`POST /events` and `submit_index_event` accept exactly six event types:

| Event | Paths | Result |
| --- | --- | --- |
| `commit` | none | Full reconciliation; optional exact `commit_sha` guard |
| `documentation` | required | Routes paths to `expected_docs` |
| `plugin` | required | Routes paths to `expected_config` |
| `test_completed` | required | Routes paths to `observed_benchmarks` |
| `runtime_report` | required | Routes paths to `observed_runtime` |
| `bot_benchmark` | required | Routes paths to `observed_benchmarks` |

Examples through the public API boundary:

```bash
curl -fsS http://127.0.0.1:18090/events \
  -H 'content-type: application/json' \
  -d '{"event_type":"commit"}'

curl -fsS http://127.0.0.1:18090/events \
  -H 'content-type: application/json' \
  -d '{"event_type":"documentation","paths":["docs/server_environment.md"]}'

curl -fsS http://127.0.0.1:18090/events \
  -H 'content-type: application/json' \
  -d '{"event_type":"runtime_report","paths":["reports/runtime/2026-07-21.md"],"observed_at":"2026-07-21T12:00:00Z"}'
```

Webhook routing metadata is content-bound and durable. A later reconcile or
restart preserves it until the file content changes or the override is removed.
Polling plus periodic reconciliation recovers missed events.

## Source policy and exclusions

The repository bind is read-only. The scanner positively recognizes supported
text/config/source extensions and excludes unknown content. Conservative hard
exclusions include:

- `.git`, dependency trees, build/cache directories and virtual environments;
- Minecraft worlds, player data, sessions, stats and advancements;
- operator, whitelist, ban and user-cache stores;
- `.env`, credential/auth/session files and secret-looking names;
- jars, archives, databases, model binaries, Blockbench models and media;
- raw `.log` files; only deliberately named runtime/validation/test reports;
- every symlink, including an in-repository symlink.

Recognized config/report text is sanitized before embedding and source return.
Do not treat redaction as permission to index secret or player-data paths: those
paths remain excluded before file access.

## Rebuild and recovery

Qdrant and indexer state are derived data. A complete rebuild is safe:

```bash
cd rag
docker compose down -v
docker compose up -d --build
```

That command also removes cached model volumes, so real mode downloads weights
again. To retain model caches, stop the stack and remove only the Compose
project's `qdrant-storage` and `indexer-state` volumes, then start it again.
Check the exact volume names with `docker volume ls --filter label=com.docker.compose.project=mc-rag`.

For ordinary recovery, submit a `commit` event or call indexer `/reconcile`.
An unchanged, inventory-complete published snapshot performs zero embeddings;
missing Qdrant inventory forces a safe rebuild.

Both Hugging Face repositories are pinned to immutable 40-character commit
revisions in `.env.example`. The embedding model name and revision participate
in the semantic snapshot ID and every indexed point. Changing either value
therefore creates a new generation and re-embeds every file instead of cloning
vectors produced by an incompatible model. The reranker revision is also
reported by `/ready` and `/rerank`, but does not affect stored Qdrant vectors.
Search filters Qdrant by the active embedding profile and citations expose that
profile. An explicit historical snapshot built with a different or unknown
embedding revision is rejected with HTTP 409 instead of being queried with an
incompatible current model.

## Verification

Lightweight tests never load large models:

```bash
cd rag
python -m pytest -q
docker compose config -q
docker compose build mc-knowledge-mcp rag-api rag-indexer
```

The deterministic end-to-end smoke creates a temporary Git repository, builds
the lightweight model targets, starts all six services, performs a real MCP
initialize/list/call flow over Streamable HTTP, verifies citations/source,
compare/status/event tools, and removes its containers and volumes:

```bash
cd rag
./scripts/smoke-mock-mcp.sh
```

The fixture is under `fixtures/smoke/`. Full BGE model smoke is deliberately a
separate, optional environment/download gate.

For a real full-repository run, start Qdrant, the embedding service and the
indexer, then run the long-lived smoke supervisor. It waits for the atomic
published snapshot, starts the reranker/API/MCP, exercises real model inference,
API search/source and all MCP read paths, submits an unchanged commit event, and
leaves the verified stack running:

```bash
cd rag
docker compose up -d qdrant embedding-service rag-indexer
./scripts/smoke-real-bge.sh
```

`REAL_SMOKE_MAX_WAIT_SECONDS` defaults to 24 hours. Progress and the final
PASS/FAIL record are written to standard output, so the script can also run as
a user service or under another process supervisor.
