"""FastAPI application for the BGE-M3 embedding service.

Endpoints:
    GET  /health  -> process liveness (always 200 once uvicorn is up)
    GET  /ready   -> model readiness (200 when loaded, 503 otherwise)
    POST /embed   -> dense + sparse embeddings for a batch of texts
"""
from __future__ import annotations

import threading
from contextlib import asynccontextmanager
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from .adapters import EmbeddingAdapter, build_adapter
from .config import Settings, get_settings


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1)
    return_dense: bool = True
    return_sparse: bool = True


class SparseVector(BaseModel):
    indices: List[int]
    values: List[float]


class EmbedResponse(BaseModel):
    model: str
    revision: str
    backend: str
    dim: int
    dense: Optional[List[List[float]]] = None
    sparse: Optional[List[SparseVector]] = None


def _safe_load(adapter: EmbeddingAdapter) -> None:
    try:
        adapter.load()
    except Exception:  # pragma: no cover - error is captured on the adapter
        # Swallowed on purpose: readiness stays False and /ready surfaces the
        # error. Crashing the loader thread would not help the parent process.
        pass


def create_app(settings: Optional[Settings] = None) -> FastAPI:
    settings = settings or get_settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        adapter = build_adapter(settings)
        app.state.adapter = adapter
        app.state.settings = settings
        if settings.backend == "mock":
            adapter.load()  # instant, keep tests deterministic
        else:
            threading.Thread(target=_safe_load, args=(adapter,), daemon=True).start()
        yield

    app = FastAPI(
        title="MC RAG Embedding Service",
        version="0.1.0",
        lifespan=lifespan,
    )

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok", "service": "embedding-service"}

    @app.get("/ready")
    def ready(request: Request):
        adapter: EmbeddingAdapter = request.app.state.adapter
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

    @app.post("/embed", response_model=EmbedResponse)
    def embed(req: EmbedRequest, request: Request) -> EmbedResponse:
        adapter: EmbeddingAdapter = request.app.state.adapter
        if not adapter.ready:
            raise HTTPException(status_code=503, detail="model not ready")
        if not (req.return_dense or req.return_sparse):
            raise HTTPException(
                status_code=400,
                detail="at least one of return_dense/return_sparse must be true",
            )
        if len(req.texts) > settings.max_texts:
            raise HTTPException(
                status_code=400,
                detail=f"too many texts (max {settings.max_texts})",
            )
        result = adapter.embed(req.texts, req.return_dense, req.return_sparse)
        dense = result["dense"]
        dim = len(dense[0]) if dense else settings.dense_dim
        sparse = (
            [SparseVector(**sv) for sv in result["sparse"]]
            if result["sparse"] is not None
            else None
        )
        return EmbedResponse(
            model=adapter.model_name,
            revision=settings.model_revision,
            backend=settings.backend,
            dim=dim,
            dense=dense,
            sparse=sparse,
        )

    return app


app = create_app()
