"""The ``mc-knowledge`` MCP server (Model Context Protocol, Streamable HTTP).

Exposes MC repository knowledge to Codex as five tools over a thin, safe
surface on top of ``rag-api``. Four tools are read-only (search, compare,
source, status); one is a write tool that submits an indexing event for one of
exactly six event types. The server never talks to Qdrant or the indexer
directly — ``rag-api`` owns the snapshot/evidence boundary and repository-backed,
path-safe source retrieval — so citations and the expected/observed split are
preserved exactly as the API returns them.
"""
from __future__ import annotations

from typing import Optional

from mcp.server.fastmcp import FastMCP
from mcp.types import ToolAnnotations

from ragcore import contract
from ragcore.events import EVENT_TYPES

from .client import HttpRagApiClient, RagApiGateway
from .config import Settings
from .schemas import (
    CollectionList,
    CommitSha,
    CompareResponse,
    EventPaths,
    EventResponse,
    EventType,
    ExpectedCollectionList,
    IndexStatusResponse,
    LimitValue,
    LineNumber,
    ObservedCollectionList,
    PathValue,
    ProvenanceValue,
    QueryValue,
    SearchResponse,
    SnapshotId,
    SourceResponse,
    WorktreeState,
)

# The most operationally important guidance is kept in the first ~512 characters
# because some clients (Codex included) truncate server instructions.
INSTRUCTIONS = (
    "MC knowledge base for the MC Minecraft-MMORPG repository. Git, docs, "
    "config, tests, runtime reports and bot benchmarks are authoritative; "
    "Qdrant is a disposable, rebuildable index. Every result carries an exact "
    "(commit_sha, worktree_state, snapshot_id) snapshot and a path/line "
    "citation. Expected evidence (docs/config) and observed evidence "
    "(runtime/benchmarks) are never mixed silently — use "
    "compare_expected_observed to view them side by side. Read-only tools: "
    "search_knowledge, compare_expected_observed, get_source, "
    "get_index_status. Write tool: submit_index_event.\n\n"
    "Collections: expected_docs, expected_config (expected); observed_runtime, "
    "observed_benchmarks (observed). Omit all snapshot fields to use the "
    "indexer's last published generation; to pin a snapshot supply commit_sha, "
    "worktree_state and snapshot_id together. get_source only serves the "
    "currently published snapshot and refuses historical or superseded dirty "
    "source. submit_index_event accepts exactly: "
    + ", ".join(EVENT_TYPES)
    + "."
)

def _read_only(title: str) -> ToolAnnotations:
    return ToolAnnotations(
        title=title,
        readOnlyHint=True,
        destructiveHint=False,
        idempotentHint=True,
        openWorldHint=False,
    )


def _write(title: str) -> ToolAnnotations:
    # Reindex requests mutate only the local rebuildable index; authoritative
    # repository/report data is never modified or deleted.
    return ToolAnnotations(
        title=title,
        readOnlyHint=False,
        destructiveHint=False,
        idempotentHint=True,
        openWorldHint=False,
    )


def build_server(
    client: Optional[RagApiGateway] = None,
    settings: Optional[Settings] = None,
) -> FastMCP:
    settings = settings or Settings.from_env()
    client = client or HttpRagApiClient(settings.rag_api_url, settings.request_timeout)

    mcp = FastMCP(
        "mc-knowledge",
        instructions=INSTRUCTIONS,
        host=settings.host,
        port=settings.port,
        streamable_http_path=settings.mcp_path,
        stateless_http=True,
        json_response=True,
    )

    @mcp.custom_route("/health", methods=["GET"])
    async def health(request):  # noqa: ANN001 - starlette Request
        from starlette.responses import JSONResponse

        return JSONResponse({"status": "ok", "service": "mc-knowledge-mcp"})

    @mcp.tool(
        annotations=_read_only("Search MC knowledge"),
    )
    def search_knowledge(
        query: QueryValue,
        collections: Optional[CollectionList] = None,
        commit_sha: Optional[CommitSha] = None,
        worktree_state: Optional[WorktreeState] = None,
        snapshot_id: Optional[SnapshotId] = None,
        limit: LimitValue = 10,
    ) -> SearchResponse:
        """Hybrid (dense + sparse, reranked) search over one evidence kind.

        ``collections`` must all be expected OR all observed (never mixed); omit
        it to search expected docs/config. Returns snapshot-pinned results, each
        with source text and a full path/line/snapshot/evidence citation.
        """
        return client.search(
            query=query,
            collections=collections,
            commit_sha=commit_sha,
            worktree_state=worktree_state,
            snapshot_id=snapshot_id,
            limit=limit,
        )

    @mcp.tool(
        annotations=_read_only("Compare expected vs observed"),
    )
    def compare_expected_observed(
        query: QueryValue,
        expected_collections: Optional[ExpectedCollectionList] = None,
        observed_collections: Optional[ObservedCollectionList] = None,
        commit_sha: Optional[CommitSha] = None,
        worktree_state: Optional[WorktreeState] = None,
        snapshot_id: Optional[SnapshotId] = None,
        limit: LimitValue = 10,
    ) -> CompareResponse:
        """Retrieve expected and observed evidence independently at one snapshot.

        Returns separate ``expected`` and ``observed`` result groups (plus the
        resolved ``snapshot``); the two evidence kinds are never merged.
        """
        return client.compare(
            query=query,
            expected_collections=expected_collections,
            observed_collections=observed_collections,
            commit_sha=commit_sha,
            worktree_state=worktree_state,
            snapshot_id=snapshot_id,
            limit=limit,
        )

    @mcp.tool(
        annotations=_read_only("Get repository source"),
    )
    def get_source(
        path: PathValue,
        commit_sha: Optional[CommitSha] = None,
        worktree_state: Optional[WorktreeState] = None,
        snapshot_id: Optional[SnapshotId] = None,
        line_start: Optional[LineNumber] = None,
        line_end: Optional[LineNumber] = None,
    ) -> SourceResponse:
        """Return path-safe, secret-redacted source for a cited repository path.

        Source comes from the repository-backed indexer and is served only for
        the currently published snapshot, verified against the published
        manifest fingerprint. Historical or superseded dirty source is refused.
        Optionally restrict to an inclusive 1-based ``line_start``/``line_end``.
        """
        return client.get_source(
            path=path,
            commit_sha=commit_sha,
            worktree_state=worktree_state,
            snapshot_id=snapshot_id,
            line_start=line_start,
            line_end=line_end,
        )

    @mcp.tool(
        annotations=_read_only("Index status"),
    )
    def get_index_status() -> IndexStatusResponse:
        """Report indexer readiness and the last published snapshot generation."""
        return client.index_status()

    @mcp.tool(
        annotations=_write("Submit index event"),
    )
    def submit_index_event(
        event_type: EventType,
        paths: Optional[EventPaths] = None,
        observed_at: Optional[ProvenanceValue] = None,
        doc_version: Optional[ProvenanceValue] = None,
        commit_sha: Optional[CommitSha] = None,
    ) -> EventResponse:
        """Submit one indexing event (write). Reindex only; nothing is deleted.

        ``event_type`` must be exactly one of: commit, documentation, plugin,
        test_completed, runtime_report, bot_benchmark. ``commit`` reconciles the
        whole tree; every other type requires ``paths`` and forces those paths
        into their evidence collection. ``observed_at``/``doc_version`` are
        optional provenance for observed/expected events.
        """
        if event_type not in EVENT_TYPES:
            raise ValueError(
                f"unknown event_type {event_type!r}; must be one of: "
                + ", ".join(EVENT_TYPES)
            )
        if event_type != "commit" and not paths:
            raise ValueError(f"event_type {event_type!r} requires at least one path")
        return client.submit_event(
            event_type=event_type,
            paths=paths,
            observed_at=observed_at,
            doc_version=doc_version,
            commit_sha=commit_sha,
        )

    return mcp


# Module-level server for `python -m knowledge_mcp.app.server` / uvicorn-style run.
mcp = build_server()


def main() -> None:
    mcp.run(transport="streamable-http")


if __name__ == "__main__":
    main()
