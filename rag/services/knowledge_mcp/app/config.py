from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    """MCP server configuration.

    The MCP server only ever talks to ``rag-api``; it never reaches Qdrant or
    the indexer directly. ``mcp_path`` is the Streamable HTTP mount point that
    Codex registers as ``url = "http://127.0.0.1:<port>/mcp"``.
    """

    rag_api_url: str = "http://rag-api:8000"
    host: str = "0.0.0.0"
    port: int = 8000
    mcp_path: str = "/mcp"
    request_timeout: float = 30.0

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            rag_api_url=os.getenv("RAG_API_URL", "http://rag-api:8000"),
            host=os.getenv("MCP_HOST", "0.0.0.0"),
            port=int(os.getenv("PORT", "8000")),
            mcp_path=os.getenv("MCP_PATH", "/mcp"),
            request_timeout=float(os.getenv("MCP_REQUEST_TIMEOUT", "30")),
        )
