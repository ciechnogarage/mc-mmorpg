"""Thin, safe HTTP client for the indexer's operational endpoints.

The API is the only component the MCP surface talks to, so source retrieval,
index status and event submission are proxied through here rather than letting
callers reach the indexer (or Qdrant) directly. Every indexer failure is
normalised into an :class:`IndexerError` carrying an HTTP status and a plain
detail string, which the API layer re-raises as an ``HTTPException`` with the
same status. Source itself always originates from the repository-backed
indexer, never from this process.
"""
from __future__ import annotations

from typing import Any, Optional, Protocol

import httpx


class IndexerError(Exception):
    """An indexer call failed. ``status_code`` is safe to surface to callers."""

    def __init__(self, status_code: int, detail: str) -> None:
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


class IndexerGateway(Protocol):
    def get_source(self, params: dict[str, Any]) -> dict[str, Any]: ...

    def index_status(self) -> dict[str, Any]: ...

    def submit_event(self, payload: dict[str, Any]) -> dict[str, Any]: ...


def _detail(response: httpx.Response) -> str:
    try:
        body = response.json()
    except ValueError:
        return response.text or f"indexer returned HTTP {response.status_code}"
    if isinstance(body, dict):
        for key in ("detail", "error", "message"):
            value = body.get(key)
            if isinstance(value, str) and value:
                return value
    return f"indexer returned HTTP {response.status_code}"


class HttpIndexerClient:
    """Synchronous httpx client for the indexer control plane."""

    def __init__(self, base_url: str, timeout: float = 10.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def _get(self, path: str, params: Optional[dict[str, Any]] = None) -> httpx.Response:
        try:
            return httpx.get(
                f"{self.base_url}{path}",
                params=_drop_none(params or {}),
                timeout=self.timeout,
            )
        except httpx.HTTPError as exc:  # transport/connection level
            raise IndexerError(503, f"indexer is unreachable: {exc}") from exc

    def _post(self, path: str, json: dict[str, Any]) -> httpx.Response:
        try:
            return httpx.post(f"{self.base_url}{path}", json=json, timeout=self.timeout)
        except httpx.HTTPError as exc:
            raise IndexerError(503, f"indexer is unreachable: {exc}") from exc

    def get_source(self, params: dict[str, Any]) -> dict[str, Any]:
        response = self._get("/source", params)
        if response.status_code >= 400:
            raise IndexerError(response.status_code, _detail(response))
        return response.json()

    def index_status(self) -> dict[str, Any]:
        status_response = self._get("/status")
        if status_response.status_code >= 400:
            raise IndexerError(status_response.status_code, _detail(status_response))
        status = status_response.json()
        # /published-snapshot returns 200 when a generation is published and a
        # structured 503 body when none is; both are a normal status, not an error.
        published_response = self._get("/published-snapshot")
        try:
            published = published_response.json()
        except ValueError:
            published = {"published": False, "error": _detail(published_response)}
        return {"indexer": status, "published_snapshot": published}

    def submit_event(self, payload: dict[str, Any]) -> dict[str, Any]:
        response = self._post("/events", payload)
        if response.status_code >= 400:
            raise IndexerError(response.status_code, _detail(response))
        return response.json()


def _drop_none(params: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in params.items() if value is not None}
