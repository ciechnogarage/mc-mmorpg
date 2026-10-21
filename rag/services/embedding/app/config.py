"""Runtime configuration for the embedding service.

Settings are read from the environment on every call so that tests can
override them with ``monkeypatch.setenv`` before building the app.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass

DEFAULT_MODEL_REVISION = "5617a9f61b028005a4858fdac845db406aefb181"


def _model_revision(name: str) -> str:
    value = os.getenv(name, DEFAULT_MODEL_REVISION).strip().lower()
    if not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError(f"{name} must be an immutable 40-character commit SHA")
    return value


@dataclass(frozen=True)
class Settings:
    # "mock" (no model download, deterministic) or "bge" (real BGE-M3).
    backend: str
    model_name: str
    model_revision: str
    device: str
    max_length: int
    batch_size: int
    dense_dim: int
    max_texts: int
    port: int


def get_settings() -> Settings:
    return Settings(
        backend=os.getenv("EMBEDDING_BACKEND", "bge").strip().lower(),
        model_name=os.getenv("EMBEDDING_MODEL", "BAAI/bge-m3"),
        model_revision=_model_revision("EMBEDDING_MODEL_REVISION"),
        device=os.getenv("EMBEDDING_DEVICE", "cpu"),
        max_length=int(os.getenv("EMBEDDING_MAX_LENGTH", "8192")),
        batch_size=int(os.getenv("EMBEDDING_BATCH_SIZE", "8")),
        dense_dim=int(os.getenv("EMBEDDING_DENSE_DIM", "1024")),
        max_texts=int(os.getenv("EMBEDDING_MAX_TEXTS", "64")),
        port=int(os.getenv("PORT", "8000")),
    )
