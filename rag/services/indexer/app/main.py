from __future__ import annotations

import threading
from contextlib import asynccontextmanager
from typing import Annotated, Optional

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, StringConstraints

from ragcore.embedding_client import HttpEmbeddingClient
from ragcore.events import EVENT_TYPES, WebhookEvent
from ragcore.gitmeta import SubprocessGitInspector
from ragcore.qdrant_index import QdrantVectorIndex

from .config import Settings
from .service import (
    HistoricalSourceUnavailable,
    IndexingService,
    PublicationNotReady,
    SourceChanged,
    SourceUnavailable,
)


PathValue = Annotated[str, StringConstraints(min_length=1, max_length=4096)]


class EventRequest(BaseModel):
    event_type: str
    paths: list[PathValue] = Field(default_factory=list, max_length=100)
    observed_at: Optional[str] = Field(default=None, max_length=256)
    doc_version: Optional[str] = Field(default=None, max_length=256)
    commit_sha: Optional[str] = None


def build_service(settings: Settings) -> IndexingService:
    return IndexingService(
        settings,
        SubprocessGitInspector(settings.repo_path),
        HttpEmbeddingClient(
            settings.embedding_url,
            timeout=settings.embedding_timeout_seconds,
            max_texts=settings.reconcile_embed_batch_chunks,
        ),
        QdrantVectorIndex(settings.qdrant_url),
    )


def create_app(settings: Optional[Settings] = None, service: Optional[IndexingService] = None) -> FastAPI:
    settings = settings or Settings.from_env()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        worker = service or build_service(settings)
        stop = threading.Event()
        thread = threading.Thread(target=worker.run_forever, args=(stop,), daemon=True)
        app.state.service = worker
        thread.start()
        yield
        stop.set()
        thread.join(timeout=max(1.0, settings.watch_interval + 1.0))

    app = FastAPI(title="MC RAG Indexer", version="0.1.0", lifespan=lifespan)

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok", "service": "rag-indexer"}

    @app.get("/ready")
    def ready(request: Request):
        worker: IndexingService = request.app.state.service
        body = {"ready": worker.ready, "error": worker.last_error}
        return body if worker.ready else JSONResponse(status_code=503, content=body)

    @app.get("/status")
    def status(request: Request) -> dict:
        worker: IndexingService = request.app.state.service
        return {
            "ready": worker.ready,
            "last_error": worker.last_error,
            "last_reconcile": worker.last_reconcile,
            "snapshot": worker.last_snapshot.__dict__ if worker.last_snapshot else None,
        }

    @app.get("/source")
    def source(
        request: Request,
        path: str = Query(min_length=1, max_length=4096),
        commit_sha: Optional[str] = None,
        worktree_state: Optional[str] = None,
        snapshot_id: Optional[str] = None,
        line_start: Optional[int] = Query(default=None, ge=1),
        line_end: Optional[int] = Query(default=None, ge=1),
    ) -> dict:
        worker: IndexingService = request.app.state.service
        try:
            return worker.source_for(
                path,
                commit_sha=commit_sha,
                worktree_state=worktree_state,
                snapshot_id=snapshot_id,
                line_start=line_start,
                line_end=line_end,
            )
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except PublicationNotReady as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        except (HistoricalSourceUnavailable, SourceChanged) as exc:
            raise HTTPException(status_code=409, detail=str(exc)) from exc
        except SourceUnavailable as exc:
            raise HTTPException(status_code=404, detail=str(exc)) from exc

    @app.get("/published-snapshot")
    def published_snapshot(request: Request):
        worker: IndexingService = request.app.state.service
        try:
            snapshot = worker.published_snapshot()
        except RuntimeError as exc:
            return JSONResponse(
                status_code=503,
                content={"published": False, "error": str(exc)},
            )
        return {
            "published": True,
            "repo": worker.settings.repo_name,
            "commit_sha": snapshot.commit_sha,
            "worktree_state": snapshot.worktree_state,
            "snapshot_id": snapshot.snapshot_id,
            "published_at": worker.last_reconcile,
        }

    @app.post("/events")
    def events(body: EventRequest, request: Request) -> dict:
        try:
            event = WebhookEvent(**body.model_dump())
            return request.app.state.service.handle_event(event)
        except (OSError, UnicodeError, ValueError) as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc

    @app.post("/reconcile")
    def reconcile(request: Request) -> dict:
        return request.app.state.service.reconcile()

    return app


app = create_app()
