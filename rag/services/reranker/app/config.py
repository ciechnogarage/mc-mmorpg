"""Runtime configuration for the reranker service."""
from __future__ import annotations

import os
import re
from dataclasses import dataclass

DEFAULT_MODEL_REVISION = "953dc6f6f85a1b2dbfca4c34a2796e7dde08d41e"


def _model_revision(name: str) -> str:
    value = os.getenv(name, DEFAULT_MODEL_REVISION).strip().lower()
    if not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError(f"{name} must be an immutable 40-character commit SHA")
    return value


@dataclass(frozen=True)
class Settings:
    # "mock" (no model download, deterministic) or "bge" (real reranker).
    backend: str
    model_name: str
    model_revision: str
    device: str
    max_length: int
    batch_size: int
    max_documents: int
    port: int


def get_settings() -> Settings:
    return Settings(
        backend=os.getenv("RERANKER_BACKEND", "bge").strip().lower(),
        model_name=os.getenv("RERANKER_MODEL", "BAAI/bge-reranker-v2-m3"),
        model_revision=_model_revision("RERANKER_MODEL_REVISION"),
        device=os.getenv("RERANKER_DEVICE", "cpu"),
        max_length=int(os.getenv("RERANKER_MAX_LENGTH", "512")),
        batch_size=int(os.getenv("RERANKER_BATCH_SIZE", "8")),
        max_documents=int(os.getenv("RERANKER_MAX_DOCUMENTS", "128")),
        port=int(os.getenv("PORT", "8000")),
    )
