"""Embedding backends.

Two interchangeable adapters implement the same contract:

* ``MockEmbeddingAdapter`` — pure-stdlib, deterministic, no model download.
  Used by the test suite and any CI that must not pull ~2 GB of weights.
* ``BGEM3Adapter`` — the real ``BAAI/bge-m3`` model producing dense vectors
  and BM25-style sparse (lexical) vectors. Heavy imports are deferred to
  ``_load`` so importing this module stays cheap.

Both return the same shape from ``embed``::

    {"dense": [[float, ...], ...] | None,
     "sparse": [{"indices": [int, ...], "values": [float, ...]}, ...] | None}
"""
from __future__ import annotations

import hashlib
import math
import re
import threading
from abc import ABC, abstractmethod
from typing import Dict, List, Optional

from .config import Settings

_TOKEN_RE = re.compile(r"[a-z0-9_]+")
# xlm-roberta / bge-m3 vocabulary size; keeps mock sparse indices in a
# realistic range so downstream code can treat both backends identically.
_MOCK_VOCAB_SIZE = 250002


class EmbeddingAdapter(ABC):
    """Common lifecycle: build cheaply, ``load`` once, then ``embed``."""

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

    @abstractmethod
    def embed(
        self, texts: List[str], return_dense: bool, return_sparse: bool
    ) -> Dict[str, Optional[list]]:
        ...


class MockEmbeddingAdapter(EmbeddingAdapter):
    """Deterministic embeddings derived from SHA-256 of the input text."""

    @property
    def model_name(self) -> str:
        return f"mock:{self.settings.model_name}"

    def _load(self) -> None:
        # Nothing to download; readiness is immediate.
        return None

    def _dense(self, text: str) -> List[float]:
        seed = int.from_bytes(
            hashlib.sha256(text.encode("utf-8")).digest()[:8], "big"
        ) or 1
        state = seed & 0xFFFFFFFFFFFFFFFF
        vec: List[float] = []
        for _ in range(self.settings.dense_dim):
            state = (6364136223846793005 * state + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
            vec.append((state >> 11) / float(1 << 53) * 2.0 - 1.0)
        norm = math.sqrt(sum(v * v for v in vec)) or 1.0
        return [v / norm for v in vec]

    def _sparse(self, text: str) -> Dict[str, list]:
        counts: Dict[int, int] = {}
        for tok in _TOKEN_RE.findall(text.lower()):
            idx = int.from_bytes(
                hashlib.sha256(tok.encode("utf-8")).digest()[:4], "big"
            ) % _MOCK_VOCAB_SIZE
            counts[idx] = counts.get(idx, 0) + 1
        indices = sorted(counts)
        values = [1.0 + math.log(counts[i]) for i in indices]
        return {"indices": indices, "values": values}

    def embed(
        self, texts: List[str], return_dense: bool, return_sparse: bool
    ) -> Dict[str, Optional[list]]:
        dense = [self._dense(t) for t in texts] if return_dense else None
        sparse = [self._sparse(t) for t in texts] if return_sparse else None
        return {"dense": dense, "sparse": sparse}


class BGEM3Adapter(EmbeddingAdapter):
    """Real BGE-M3 dense + sparse encoder (CPU by default)."""

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)
        self._model = None
        self._inference_lock = threading.Lock()

    @property
    def model_name(self) -> str:
        return self.settings.model_name

    def _load(self) -> None:
        # Imported lazily so the mock path and the test suite never need torch.
        from FlagEmbedding import BGEM3FlagModel
        from huggingface_hub import snapshot_download

        model_path = snapshot_download(
            repo_id=self.settings.model_name,
            revision=self.settings.model_revision,
        )
        self._model = BGEM3FlagModel(
            model_path,
            use_fp16=False,  # fp16 is unsafe / unsupported on CPU
            devices=self.settings.device,
        )

    def embed(
        self, texts: List[str], return_dense: bool, return_sparse: bool
    ) -> Dict[str, Optional[list]]:
        if self._model is None:
            raise RuntimeError("model not loaded")
        order = sorted(range(len(texts)), key=lambda index: len(texts[index]), reverse=True)
        ordered_texts = [texts[index] for index in order]
        with self._inference_lock:
            out = self._model.encode(
                ordered_texts,
                batch_size=self.settings.batch_size,
                max_length=self.settings.max_length,
                return_dense=return_dense,
                return_sparse=return_sparse,
                return_colbert_vecs=False,
            )
        dense = None
        if return_dense:
            ordered_dense = [list(map(float, row)) for row in out["dense_vecs"]]
            dense = [None] * len(texts)
            for ordered_index, original_index in enumerate(order):
                dense[original_index] = ordered_dense[ordered_index]
        sparse = None
        if return_sparse:
            ordered_sparse = []
            for weights in out["lexical_weights"]:
                items = sorted((int(k), float(v)) for k, v in weights.items())
                ordered_sparse.append(
                    {
                        "indices": [i for i, _ in items],
                        "values": [v for _, v in items],
                    }
                )
            sparse = [None] * len(texts)
            for ordered_index, original_index in enumerate(order):
                sparse[original_index] = ordered_sparse[ordered_index]
        return {"dense": dense, "sparse": sparse}


def build_adapter(settings: Settings) -> EmbeddingAdapter:
    if settings.backend == "mock":
        return MockEmbeddingAdapter(settings)
    if settings.backend == "bge":
        return BGEM3Adapter(settings)
    raise ValueError(f"unknown EMBEDDING_BACKEND: {settings.backend!r}")
