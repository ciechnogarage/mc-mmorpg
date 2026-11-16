"""Reranker service adapters."""
from __future__ import annotations

import re
from typing import Protocol, Sequence

_TOKEN_RE = re.compile(r"[a-z0-9_]+")


class RerankerClient(Protocol):
    def rerank(self, query: str, documents: Sequence[str]) -> list[float]:
        ...


class HttpRerankerClient:
    def __init__(self, base_url: str, timeout: float = 120.0, max_documents: int = 128) -> None:
        self.url = base_url.rstrip("/") + "/rerank"
        self.timeout = timeout
        self.max_documents = max_documents

    def rerank(self, query: str, documents: Sequence[str]) -> list[float]:
        import httpx

        items = list(documents)
        scores = [0.0] * len(items)
        for start in range(0, len(items), self.max_documents):
            batch = items[start : start + self.max_documents]
            response = httpx.post(
                self.url,
                json={"query": query, "documents": batch, "top_n": len(batch)},
                timeout=self.timeout,
            )
            response.raise_for_status()
            for item in response.json()["results"]:
                scores[start + item["index"]] = float(item["score"])
        return scores


class FakeRerankerClient:
    def rerank(self, query: str, documents: Sequence[str]) -> list[float]:
        wanted = set(_TOKEN_RE.findall(query.lower()))
        return [
            float(len(wanted & set(_TOKEN_RE.findall(document.lower()))))
            for document in documents
        ]
