from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Annotated, Optional

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, StringConstraints, model_validator

from ragcore import contract
from ragcore.embedding_client import HttpEmbeddingClient
from ragcore.events import EVENT_TYPES
from ragcore.qdrant_index import QdrantVectorIndex
from ragcore.reranker_client import HttpRerankerClient

from .config import Settings
from .indexer_client import HttpIndexerClient, IndexerError, IndexerGateway
from .service import (
    HttpPublishedSnapshotClient,
    RetrievalService,
    SnapshotCompatibilityError,
)


PathValue = Annotated[str, StringConstraints(min_length=1, max_length=4096)]


class SearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    collections: list[str] = Field(
        default_factory=lambda: list(contract.EXPECTED_COLLECTIONS), max_length=4
    )
    commit_sha: Optional[str] = None
    worktree_state: Optional[str] = None
    snapshot_id: Optional[str] = None
    limit: int = Field(default=10, ge=1, le=100)


class CompareRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    expected_collections: list[str] = Field(
        default_factory=lambda: list(contract.EXPECTED_COLLECTIONS), max_length=2
    )
    observed_collections: list[str] = Field(
        default_factory=lambda: list(contract.OBSERVED_COLLECTIONS), max_length=2
    )
    commit_sha: Optional[str] = None
    worktree_state: Optional[str] = None
    snapshot_id: Optional[str] = None
    limit: int = Field(default=10, ge=1, le=100)


class EventRequest(BaseModel):
    event_type: str = Field(min_length=1)
    paths: list[PathValue] = Field(default_factory=list, max_length=100)
    observed_at: Optional[str] = Field(default=None, max_length=256)
    doc_version: Optional[str] = Field(default=None, max_length=256)
    commit_sha: Optional[str] = None

    @model_validator(mode="after")
    def validate_event(self) -> "EventRequest":
        if self.event_type not in EVENT_TYPES:
            raise ValueError(
                f"unknown event_type {self.event_type!r}; expected one of {EVENT_TYPES}"
            )
        if self.event_type != "commit" and not self.paths:
            raise ValueError(f"{self.event_type} event requires at least one path")
        return self


def build_service(settings: Settings) -> RetrievalService:
    return RetrievalService(
        settings.repo_name,
        HttpEmbeddingClient(settings.embedding_url),
        HttpRerankerClient(settings.reranker_url),
        QdrantVectorIndex(settings.qdrant_url),
        HttpPublishedSnapshotClient(settings.indexer_url),
        settings.prefetch_limit,
        settings.embedding_model,
        settings.embedding_model_revision,
    )


def create_app(
    settings: Optional[Settings] = None,
    service: Optional[RetrievalService] = None,
    indexer: Optional[IndexerGateway] = None,
) -> FastAPI:
    settings = settings or Settings.from_env()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        worker = service or build_service(settings)
        worker.index.ensure_collections()
        app.state.service = worker
        app.state.indexer = indexer or HttpIndexerClient(settings.indexer_url)
        yield

    app = FastAPI(title="MC Hybrid RAG API", version="0.1.0", lifespan=lifespan)

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok", "service": "rag-api"}

    @app.get("/ready")
    def ready(request: Request):
        worker: RetrievalService = request.app.state.service
        try:
            snapshot = worker.published_snapshots.get_snapshot()
        except Exception as exc:
            return JSONResponse(
                status_code=503,
                content={"ready": False, "service": "rag-api", "error": str(exc)},
            )
        return {
            "ready": True,
            "service": "rag-api",
            "snapshot": snapshot.__dict__,
        }

    @app.post("/search")
    def search(body: SearchRequest, request: Request) -> dict:
        worker: RetrievalService = request.app.state.service
        try:
            snapshot = worker.resolve_snapshot(
                body.commit_sha, body.worktree_state, body.snapshot_id
            )
            results = worker.search(body.query, body.collections, snapshot, body.limit)
        except SnapshotCompatibilityError as exc:
            raise HTTPException(status_code=409, detail=str(exc)) from exc
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except Exception as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        return {"snapshot": snapshot.__dict__, "results": results}

    @app.post("/compare")
    def compare(body: CompareRequest, request: Request) -> dict:
        worker: RetrievalService = request.app.state.service
        try:
            snapshot = worker.resolve_snapshot(
                body.commit_sha, body.worktree_state, body.snapshot_id
            )
            groups = worker.compare(
                body.query,
                body.expected_collections,
                body.observed_collections,
                snapshot,
                body.limit,
            )
        except SnapshotCompatibilityError as exc:
            raise HTTPException(status_code=409, detail=str(exc)) from exc
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except Exception as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        return {"snapshot": snapshot.__dict__, **groups}

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
        gateway: IndexerGateway = request.app.state.indexer
        try:
            return gateway.get_source(
                {
                    "path": path,
                    "commit_sha": commit_sha,
                    "worktree_state": worktree_state,
                    "snapshot_id": snapshot_id,
                    "line_start": line_start,
                    "line_end": line_end,
                }
            )
        except IndexerError as exc:
            raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc

    @app.get("/index-status")
    def index_status(request: Request) -> dict:
        gateway: IndexerGateway = request.app.state.indexer
        try:
            return gateway.index_status()
        except IndexerError as exc:
            raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc

    @app.post("/events")
    def events(body: EventRequest, request: Request) -> dict:
        gateway: IndexerGateway = request.app.state.indexer
        try:
            return gateway.submit_event(body.model_dump())
        except IndexerError as exc:
            raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc

    return app


app = create_app()
