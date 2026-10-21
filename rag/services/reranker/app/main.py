"""FastAPI application for the bge-reranker-v2-m3 service.

Endpoints:
    GET  /health  -> process liveness (always 200 once uvicorn is up)
    GET  /ready   -> model readiness (200 when loaded, 503 otherwise)
    POST /rerank  -> reorder documents by relevance to a query
"""
from __future__ import annotations

import threading
from contextlib import asynccontextmanager
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from .adapters import RerankerAdapter, build_adapter
from .config import Settings, get_settings


class RerankRequest(BaseModel):
    query: str = Field(..., min_length=1)
    documents: List[str] = Field(..., min_length=1)
    top_n: Optional[int] = Field(default=None, ge=1)


class RerankResult(BaseModel):
    index: int
    score: float


class RerankResponse(BaseModel):
    model: str
    revision: str
    backend: str
    results: List[RerankResult]


def _safe_load(adapter: RerankerAdapter) -> None:
    try:
        adapter.load()
    except Exception:  # pragma: no cover - error captured on the adapter
        pass


def create_app(settings: Optional[Settings] = None) -> FastAPI:
    settings = settings or get_settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        adapter = build_adapter(settings)
        app.state.adapter = adapter
        app.state.settings = settings
        if settings.backend == "mock":
            adapter.load()
        else:
            threading.Thread(target=_safe_load, args=(adapter,), daemon=True).start()
        yield

    app = FastAPI(
        title="MC RAG Reranker Service",
        version="0.1.0",
        lifespan=lifespan,
    )

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok", "service": "reranker-service"}

    @app.get("/ready")
    def ready(request: Request):
        adapter: RerankerAdapter = request.app.state.adapter
        if adapter.ready:
            return {
                "ready": True,
                "backend": settings.backend,
                "model": adapter.model_name,
                "revision": settings.model_revision,
            }
        return JSONResponse(
            status_code=503,
            content={
                "ready": False,
                "backend": settings.backend,
                "model": adapter.model_name,
                "revision": settings.model_revision,
                "error": adapter.error,
            },
        )

    @app.post("/rerank", response_model=RerankResponse)
    def rerank(req: RerankRequest, request: Request) -> RerankResponse:
        adapter: RerankerAdapter = request.app.state.adapter
        if not adapter.ready:
            raise HTTPException(status_code=503, detail="model not ready")
        if len(req.documents) > settings.max_documents:
            raise HTTPException(
                status_code=400,
                detail=f"too many documents (max {settings.max_documents})",
            )
        results = adapter.rerank(req.query, req.documents, req.top_n)
        return RerankResponse(
            model=adapter.model_name,
            revision=settings.model_revision,
            backend=settings.backend,
            results=[RerankResult(**r) for r in results],
        )

    return app


app = create_app()
