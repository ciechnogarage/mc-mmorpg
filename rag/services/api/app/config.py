from __future__ import annotations

import os
import re
from dataclasses import dataclass

DEFAULT_EMBEDDING_MODEL = "BAAI/bge-m3"
DEFAULT_EMBEDDING_MODEL_REVISION = "5617a9f61b028005a4858fdac845db406aefb181"


def _embedding_revision() -> str:
    value = os.getenv(
        "EMBEDDING_MODEL_REVISION", DEFAULT_EMBEDDING_MODEL_REVISION
    ).strip().lower()
    if not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError(
            "EMBEDDING_MODEL_REVISION must be an immutable 40-character commit SHA"
        )
    return value


@dataclass(frozen=True)
class Settings:
    repo_name: str = "MC"
    qdrant_url: str = "http://qdrant:6333"
    embedding_url: str = "http://embedding-service:8000"
    reranker_url: str = "http://reranker-service:8000"
    indexer_url: str = "http://rag-indexer:8000"
    embedding_model: str = DEFAULT_EMBEDDING_MODEL
    embedding_model_revision: str = DEFAULT_EMBEDDING_MODEL_REVISION
    default_limit: int = 10
    prefetch_limit: int = 40

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            repo_name=os.getenv("REPO_NAME", "MC"),
            qdrant_url=os.getenv("QDRANT_URL", "http://qdrant:6333"),
            embedding_url=os.getenv("EMBEDDING_URL", "http://embedding-service:8000"),
            reranker_url=os.getenv("RERANKER_URL", "http://reranker-service:8000"),
            indexer_url=os.getenv("INDEXER_URL", "http://rag-indexer:8000"),
            embedding_model=os.getenv("EMBEDDING_MODEL", DEFAULT_EMBEDDING_MODEL),
            embedding_model_revision=_embedding_revision(),
            default_limit=int(os.getenv("DEFAULT_QUERY_LIMIT", "10")),
            prefetch_limit=int(os.getenv("PREFETCH_LIMIT", "40")),
        )
