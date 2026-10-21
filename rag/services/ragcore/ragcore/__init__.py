"""Shared, pure-stdlib core for the MC RAG stack.

``ragcore`` holds everything the ``rag-indexer`` and ``rag-api`` services have
in common and that must behave identically on both sides:

* the collection / evidence contract and point-identity scheme (:mod:`contract`)
* deterministic source classification and safe exclusions (:mod:`classify`)
* deterministic chunking with line and heading provenance (:mod:`chunk`)
* Git snapshot inspection (:mod:`gitmeta`)
* thin clients for the embedding, reranker and Qdrant backends, each with a
  deterministic in-memory fake so the logic is testable without downloading
  multi-gigabyte models or running a Qdrant server (:mod:`embedding_client`,
  :mod:`reranker_client`, :mod:`qdrant_index`)
* the webhook event contract (:mod:`events`) and reciprocal-rank fusion
  (:mod:`fusion`)

The module imports nothing beyond the standard library at import time. Heavy or
optional third-party dependencies (``httpx``, ``qdrant_client``) are imported
lazily inside the real adapters, so importing ``ragcore`` in a test never pulls
them in.
"""
from __future__ import annotations

__all__ = [
    "contract",
    "classify",
    "chunk",
    "gitmeta",
    "events",
    "fusion",
    "embedding_client",
    "reranker_client",
    "qdrant_index",
    "paths",
    "sanitize",
    "snapshot",
]
