"""Reranker backends.

* ``MockRerankerAdapter`` — deterministic lexical-overlap scoring, pure
  stdlib, no model download. Used by the test suite.
* ``BGERerankerAdapter`` — the real ``BAAI/bge-reranker-v2-m3`` cross-encoder.
  Heavy imports deferred to ``_load``.

``rerank`` returns a list of ``{"index": int, "score": float}`` sorted by
descending score, truncated to ``top_n`` when provided. ``index`` refers to
the position of the document in the original request list.
"""
from __future__ import annotations

import math
import re
import threading
from abc import ABC, abstractmethod
from typing import List, Optional

from .config import Settings

_TOKEN_RE = re.compile(r"[a-z0-9_]+")


class RerankerAdapter(ABC):
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._ready = False
        self._error: Optional[str] = None

    @property
    def ready(self) -> bool:
        return self._ready

    @property
    def error(self) -> Optional[str]:
        return self._error

    @property
    @abstractmethod
    def model_name(self) -> str:
        ...

    def load(self) -> None:
        try:
            self._load()
        except Exception as exc:  # pragma: no cover - exercised via bge path
            self._error = f"{type(exc).__name__}: {exc}"
            raise
        self._ready = True

    @abstractmethod
    def _load(self) -> None:
        ...

    def rerank(
        self, query: str, documents: List[str], top_n: Optional[int]
    ) -> List[dict]:
        scores = self._score(query, documents)
        ranked = sorted(
            ({"index": i, "score": float(s)} for i, s in enumerate(scores)),
            key=lambda r: r["score"],
            reverse=True,
        )
        if top_n is not None:
            ranked = ranked[:top_n]
        return ranked

    @abstractmethod
    def _score(self, query: str, documents: List[str]) -> List[float]:
        ...


class MockRerankerAdapter(RerankerAdapter):
    """Cosine-of-token-sets overlap: deterministic and in [0, 1]."""

    @property
    def model_name(self) -> str:
        return f"mock:{self.settings.model_name}"

    def _load(self) -> None:
        return None

    def _score(self, query: str, documents: List[str]) -> List[float]:
        q = set(_TOKEN_RE.findall(query.lower()))
        scores: List[float] = []
        for doc in documents:
            d = set(_TOKEN_RE.findall(doc.lower()))
            if not q or not d:
                scores.append(0.0)
                continue
            inter = len(q & d)
            scores.append(inter / math.sqrt(len(q) * len(d)))
        return scores


class BGERerankerAdapter(RerankerAdapter):
    """Real bge-reranker-v2-m3 cross-encoder (CPU by default)."""

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)
        self._model = None
        self._inference_lock = threading.Lock()

    @property
    def model_name(self) -> str:
        return self.settings.model_name

    def _load(self) -> None:
        from FlagEmbedding import FlagReranker
        from huggingface_hub import snapshot_download

        model_path = snapshot_download(
            repo_id=self.settings.model_name,
            revision=self.settings.model_revision,
        )

        self._model = FlagReranker(
            model_path,
            use_fp16=False,  # fp16 is unsafe / unsupported on CPU
            devices=self.settings.device,
        )

    def _score(self, query: str, documents: List[str]) -> List[float]:
        if self._model is None:
            raise RuntimeError("model not loaded")
        pairs = [[query, doc] for doc in documents]
        with self._inference_lock:
            scores = self._model.compute_score(
                pairs, batch_size=self.settings.batch_size, normalize=True
            )
        if not isinstance(scores, list):
            scores = [scores]
        return [float(s) for s in scores]


def build_adapter(settings: Settings) -> RerankerAdapter:
    if settings.backend == "mock":
        return MockRerankerAdapter(settings)
    if settings.backend == "bge":
        return BGERerankerAdapter(settings)
    raise ValueError(f"unknown RERANKER_BACKEND: {settings.backend!r}")
