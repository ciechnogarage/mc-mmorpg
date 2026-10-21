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
    repo_path: str = "/repo"
    repo_name: str = "MC"
    state_path: str = "/state/indexer.json"
    qdrant_url: str = "http://qdrant:6333"
    embedding_url: str = "http://embedding-service:8000"
    embedding_model: str = DEFAULT_EMBEDDING_MODEL
    embedding_model_revision: str = DEFAULT_EMBEDDING_MODEL_REVISION
    embedding_timeout_seconds: float = 300.0
    watch_interval: float = 2.0
    reconcile_interval: float = 300.0
    reconcile_embed_batch_chunks: int = 32
    max_file_bytes: int = 2_000_000

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            repo_path=os.getenv("REPO_PATH", "/repo"),
            repo_name=os.getenv("REPO_NAME", "MC"),
            state_path=os.getenv("INDEX_STATE_PATH", "/state/indexer.json"),
            qdrant_url=os.getenv("QDRANT_URL", "http://qdrant:6333"),
            embedding_url=os.getenv("EMBEDDING_URL", "http://embedding-service:8000"),
            embedding_model=os.getenv("EMBEDDING_MODEL", DEFAULT_EMBEDDING_MODEL),
            embedding_model_revision=_embedding_revision(),
            embedding_timeout_seconds=float(
                os.getenv("EMBEDDING_TIMEOUT_SECONDS", "300")
            ),
            watch_interval=float(os.getenv("WATCH_INTERVAL_SECONDS", "2")),
            reconcile_interval=float(os.getenv("RECONCILE_INTERVAL_SECONDS", "300")),
            reconcile_embed_batch_chunks=int(
                os.getenv("RECONCILE_EMBED_BATCH_CHUNKS", "32")
            ),
            max_file_bytes=int(os.getenv("MAX_FILE_BYTES", "2000000")),
        )
