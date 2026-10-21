"""Embedding service adapters, including deterministic test fake."""

from __future__ import annotations

import hashlib
import math
import re
import time
from typing import Protocol, Sequence

_TOKEN_RE = re.compile(r"[a-z0-9_]+")


class EmbeddingClient(Protocol):
    def embed(self, texts: Sequence[str]) -> tuple[list[list[float]], list[dict]]:
        ...


class HttpEmbeddingClient:
    def __init__(
        self,
        base_url: str,
        timeout: float = 300.0,
        max_texts: int = 64,
        retry_attempts: int = 3,
        retry_backoff_seconds: float = 2.0,
    ) -> None:
        self.url = base_url.rstrip("/") + "/embed"
        self.timeout = timeout
        self.max_texts = max_texts
        self.retry_attempts = max(1, retry_attempts)
        self.retry_backoff_seconds = max(0.0, retry_backoff_seconds)

    def embed(self, texts: Sequence[str]) -> tuple[list[list[float]], list[dict]]:
        import httpx

        all_dense: list[list[float]] = []
        all_sparse: list[dict] = []
        items = list(texts)
        for start in range(0, len(items), self.max_texts):
            payload = {
                "texts": items[start : start + self.max_texts],
                "return_dense": True,
                "return_sparse": True,
            }
            response = None
            for attempt in range(1, self.retry_attempts + 1):
                try:
                    response = httpx.post(self.url, json=payload, timeout=self.timeout)
                    response.raise_for_status()
                    break
                except (
                    httpx.ConnectError,
                    httpx.ReadTimeout,
                    httpx.RemoteProtocolError,
                    httpx.HTTPStatusError,
                ):
                    if (
                        attempt >= self.retry_attempts
                        or response is not None
                        and response.status_code < 500
                    ):
                        raise
                    if self.retry_backoff_seconds > 0:
                        time.sleep(self.retry_backoff_seconds * attempt)
            assert response is not None
            body = response.json()
            if body.get("dense") is None or body.get("sparse") is None:
                raise RuntimeError("embedding service omitted requested vector channel")
            all_dense.extend(body["dense"])
            all_sparse.extend(body["sparse"])
        return all_dense, all_sparse


class FakeEmbeddingClient:
    """Small stable vectors; no model packages or network required."""

    def __init__(self, dim: int = 8) -> None:
        self.dim = dim
        self.calls = 0

    def embed(self, texts: Sequence[str]) -> tuple[list[list[float]], list[dict]]:
        self.calls += 1
        dense: list[list[float]] = []
        sparse: list[dict] = []
        for text in texts:
            digest = hashlib.sha256(text.encode("utf-8")).digest()
            seed = int.from_bytes(digest[:8], "big") or 1
            state = seed & 0xFFFFFFFFFFFFFFFF
            vec: list[float] = []
            for _ in range(self.dim):
                state = (
                    6364136223846793005 * state + 1442695040888963407
                ) & 0xFFFFFFFFFFFFFFFF
                vec.append((state / 2**64) * 2.0 - 1.0)
            norm = math.sqrt(sum(value * value for value in vec)) or 1.0
            dense.append([value / norm for value in vec])

            weights: dict[int, float] = {}
            for token in _TOKEN_RE.findall(text.lower()):
                slot = int(hashlib.sha256(token.encode("utf-8")).hexdigest(), 16) % 250002
                weights[slot] = weights.get(slot, 0.0) + 1.0
            items_sorted = sorted(weights.items())
            sparse.append(
                {
                    "indices": [index for index, _ in items_sorted],
                    "values": [value for _, value in items_sorted],
                }
            )
        return dense, sparse
