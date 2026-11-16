"""Synchronous client for ``rag-api``.

The MCP server is a thin, safe surface over the API. It never talks to Qdrant
or the indexer directly: search, compare, source, index status and event
submission all go through ``rag-api``, which owns the snapshot/evidence
boundary and the repository-backed, path-safe source retrieval. API failures
are normalised into :class:`RagApiError` with an HTTP status and a plain
message so the tool layer can surface a faithful error to the caller.
"""
from __future__ import annotations

from typing import Any, Optional, Protocol, Sequence

import httpx


class RagApiError(Exception):
    """A call to ``rag-api`` failed; ``status_code`` may be 0 for transport errors."""

    def __init__(self, status_code: int, detail: str) -> None:
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail

    def __str__(self) -> str:  # surfaced verbatim by the MCP tool layer
        if self.status_code:
            return f"rag-api error {self.status_code}: {self.detail}"
        return f"rag-api is unreachable: {self.detail}"


class RagApiGateway(Protocol):
    def search(self, **kwargs: Any) -> dict[str, Any]: ...

    def compare(self, **kwargs: Any) -> dict[str, Any]: ...

    def get_source(self, **kwargs: Any) -> dict[str, Any]: ...

    def index_status(self) -> dict[str, Any]: ...

    def submit_event(self, **kwargs: Any) -> dict[str, Any]: ...


def _detail(response: httpx.Response) -> str:
    try:
        body = response.json()
    except ValueError:
        return response.text or f"HTTP {response.status_code}"
    if isinstance(body, dict):
        for key in ("detail", "error", "message"):
            value = body.get(key)
            if isinstance(value, str) and value:
                return value
    return f"HTTP {response.status_code}"


def _drop_none(data: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in data.items() if value is not None}


class HttpRagApiClient:
    def __init__(self, base_url: str, timeout: float = 30.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def _post(self, path: str, json: dict[str, Any]) -> dict[str, Any]:
        try:
            response = httpx.post(f"{self.base_url}{path}", json=json, timeout=self.timeout)
        except httpx.HTTPError as exc:
            raise RagApiError(0, str(exc)) from exc
        if response.status_code >= 400:
            raise RagApiError(response.status_code, _detail(response))
        return response.json()

    def _get(self, path: str, params: Optional[dict[str, Any]] = None) -> dict[str, Any]:
        try:
            response = httpx.get(
                f"{self.base_url}{path}", params=_drop_none(params or {}), timeout=self.timeout
            )
        except httpx.HTTPError as exc:
            raise RagApiError(0, str(exc)) from exc
        if response.status_code >= 400:
            raise RagApiError(response.status_code, _detail(response))
        return response.json()

    def search(
        self,
        *,
        query: str,
        collections: Optional[Sequence[str]] = None,
        commit_sha: Optional[str] = None,
        worktree_state: Optional[str] = None,
        snapshot_id: Optional[str] = None,
        limit: int = 10,
    ) -> dict[str, Any]:
        payload = _drop_none(
            {
                "query": query,
                "collections": list(collections) if collections is not None else None,
                "commit_sha": commit_sha,
                "worktree_state": worktree_state,
                "snapshot_id": snapshot_id,
                "limit": limit,
            }
        )
        return self._post("/search", payload)

    def compare(
        self,
        *,
        query: str,
        expected_collections: Optional[Sequence[str]] = None,
        observed_collections: Optional[Sequence[str]] = None,
        commit_sha: Optional[str] = None,
        worktree_state: Optional[str] = None,
        snapshot_id: Optional[str] = None,
        limit: int = 10,
    ) -> dict[str, Any]:
        payload = _drop_none(
            {
                "query": query,
                "expected_collections": list(expected_collections)
                if expected_collections is not None
                else None,
                "observed_collections": list(observed_collections)
                if observed_collections is not None
                else None,
                "commit_sha": commit_sha,
                "worktree_state": worktree_state,
                "snapshot_id": snapshot_id,
                "limit": limit,
            }
        )
        return self._post("/compare", payload)

    def get_source(
        self,
        *,
        path: str,
        commit_sha: Optional[str] = None,
        worktree_state: Optional[str] = None,
        snapshot_id: Optional[str] = None,
        line_start: Optional[int] = None,
        line_end: Optional[int] = None,
    ) -> dict[str, Any]:
        params = {
            "path": path,
            "commit_sha": commit_sha,
            "worktree_state": worktree_state,
            "snapshot_id": snapshot_id,
            "line_start": line_start,
            "line_end": line_end,
        }
        return self._get("/source", params)

    def index_status(self) -> dict[str, Any]:
        return self._get("/index-status")

    def submit_event(
        self,
        *,
        event_type: str,
        paths: Optional[Sequence[str]] = None,
        observed_at: Optional[str] = None,
        doc_version: Optional[str] = None,
        commit_sha: Optional[str] = None,
    ) -> dict[str, Any]:
        payload = _drop_none(
            {
                "event_type": event_type,
                "paths": list(paths) if paths else [],
                "observed_at": observed_at,
                "doc_version": doc_version,
                "commit_sha": commit_sha,
            }
        )
        return self._post("/events", payload)
