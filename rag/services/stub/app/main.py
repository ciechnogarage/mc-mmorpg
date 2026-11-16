"""Placeholder service used by the not-yet-implemented RAG components.

``rag-indexer``, ``rag-api`` and ``mc-knowledge-mcp`` share this image so the
Compose topology is complete and wired with correct entrypoints, health and
dependency ordering. Each instance is distinguished by SERVICE_NAME.

Contract of the placeholder:
    GET /health -> 200, process is alive (used by the Docker healthcheck)
    GET /ready  -> 501, component is not implemented yet

/ready deliberately reports "not implemented" so it is obvious this is a
scaffold, while /health lets dependents that only need the process to exist
(e.g. mc-knowledge-mcp -> rag-api) start in the right order.
"""
from __future__ import annotations

import os

from fastapi import FastAPI
from fastapi.responses import JSONResponse

SERVICE_NAME = os.getenv("SERVICE_NAME", "rag-stub")

app = FastAPI(title=f"MC RAG placeholder: {SERVICE_NAME}", version="0.1.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": SERVICE_NAME}


@app.get("/ready")
def ready() -> JSONResponse:
    return JSONResponse(
        status_code=501,
        content={
            "ready": False,
            "service": SERVICE_NAME,
            "detail": "component not implemented in this foundation",
        },
    )
