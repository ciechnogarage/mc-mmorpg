"""The collection / evidence contract shared by the indexer and the API.

Everything that must agree on both sides lives here:

* the four Qdrant collections and which *evidence kind* each carries,
* the named dense + sparse vector configuration,
* the payload every point must carry (repository identity, exact commit SHA,
  working-tree state, path, content hash, evidence kind, ...),
* a deterministic point-identity scheme so re-indexing the same snapshot is an
  idempotent upsert rather than a duplicate insert.

A snapshot is ``(commit_sha, worktree_state, snapshot_id)``. Point identities
include the content-derived snapshot id so two dirty trees above one HEAD can
never overwrite or query as the same generation.

* re-indexing unchanged content overwrites the same point (idempotent), and
* a different commit or a clean/dirty transition yields a *different* point,
  which keeps snapshots from silently overwriting one another and lets
  retrieval pin exactly one snapshot.
"""
from __future__ import annotations

import hashlib
import re
import time
import uuid
from dataclasses import dataclass, field
from typing import Dict, List, Optional

# --- evidence kinds ---------------------------------------------------------

EVIDENCE_EXPECTED = "expected"
EVIDENCE_OBSERVED = "observed"
EVIDENCE_KINDS = (EVIDENCE_EXPECTED, EVIDENCE_OBSERVED)

# --- named vectors ----------------------------------------------------------

DENSE_VECTOR = "dense"
SPARSE_VECTOR = "sparse"
# BGE-M3 dense dimensionality; kept here so indexer and API never disagree.
DENSE_DIM = 1024
DENSE_DISTANCE = "Cosine"

WORKTREE_CLEAN = "clean"
WORKTREE_DIRTY = "dirty"
WORKTREE_UNKNOWN = "unknown"
_COMMIT_SHA_RE = re.compile(r"^[0-9a-fA-F]{40}$")
_SNAPSHOT_ID_RE = re.compile(r"^(?:clean|dirty|unknown):[0-9a-f]{64}$")


def validate_commit_sha(value: str) -> str:
    if not _COMMIT_SHA_RE.fullmatch(value):
        raise ValueError("commit_sha must be an exact 40-character hexadecimal SHA")
    return value.lower()


def validate_snapshot_id(value: str, commit_sha: str, worktree_state: str) -> str:
    validate_commit_sha(commit_sha)
    if not _SNAPSHOT_ID_RE.fullmatch(value):
        raise ValueError("snapshot_id is not a valid exact snapshot generation")
    if not value.startswith(f"{worktree_state}:"):
        raise ValueError("snapshot_id does not match worktree_state")
    return value


@dataclass(frozen=True)
class CollectionSpec:
    """Static description of one Qdrant collection."""

    name: str
    evidence_kind: str
    description: str


# The four collections, in a fixed order. This is the single source of truth
# for "which collections exist" across the whole stack.
COLLECTIONS: Dict[str, CollectionSpec] = {
    "expected_docs": CollectionSpec(
        name="expected_docs",
        evidence_kind=EVIDENCE_EXPECTED,
        description="Markdown and durable design/operational documentation.",
    ),
    "expected_config": CollectionSpec(
        name="expected_config",
        evidence_kind=EVIDENCE_EXPECTED,
        description="Versioned plugin/server configuration, source, manifests, "
        "and test definitions.",
    ),
    "observed_runtime": CollectionSpec(
        name="observed_runtime",
        evidence_kind=EVIDENCE_OBSERVED,
        description="Deliberately produced, sanitized runtime/validation reports.",
    ),
    "observed_benchmarks": CollectionSpec(
        name="observed_benchmarks",
        evidence_kind=EVIDENCE_OBSERVED,
        description="Bot outputs, completed test artifacts, and benchmark reports.",
    ),
}

COLLECTION_NAMES = tuple(COLLECTIONS.keys())
EXPECTED_COLLECTIONS = tuple(
    n for n, c in COLLECTIONS.items() if c.evidence_kind == EVIDENCE_EXPECTED
)
OBSERVED_COLLECTIONS = tuple(
    n for n, c in COLLECTIONS.items() if c.evidence_kind == EVIDENCE_OBSERVED
)


def evidence_kind_of(collection: str) -> str:
    """Evidence kind for ``collection`` (raises on an unknown collection)."""
    try:
        return COLLECTIONS[collection].evidence_kind
    except KeyError:
        raise ValueError(f"unknown collection: {collection!r}") from None


def collections_for_evidence(kind: str) -> List[str]:
    if kind not in EVIDENCE_KINDS:
        raise ValueError(f"unknown evidence kind: {kind!r}")
    return [n for n, c in COLLECTIONS.items() if c.evidence_kind == kind]


def validate_collections(collections: List[str]) -> None:
    """Raise ``ValueError`` if any name is not one of the four collections."""
    for name in collections:
        if name not in COLLECTIONS:
            raise ValueError(f"unknown collection: {name!r}")


def spans_multiple_evidence_kinds(collections: List[str]) -> bool:
    """True if ``collections`` mixes expected and observed evidence."""
    kinds = {evidence_kind_of(c) for c in collections}
    return len(kinds) > 1


# --- content + point identity ----------------------------------------------

_POINT_NAMESPACE = uuid.UUID("6f9b7c1a-4d2e-4c8a-9b3f-2e7a1d0c5b84")


def content_hash(text: str) -> str:
    """Stable SHA-256 of chunk text (hex)."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def point_id(
    repo: str,
    commit_sha: str,
    worktree_state: str,
    snapshot_id: str,
    path: str,
    chunk_index: int,
) -> str:
    """Deterministic UUID for one chunk within one snapshot.

    Identical inputs always yield the same id, which makes upserts idempotent;
    a change to any component (including the commit or clean/dirty state) yields
    a different id, so snapshots never collide.
    """
    key = (
        f"{repo}\x00{commit_sha}\x00{worktree_state}\x00{snapshot_id}\x00"
        f"{path}\x00{chunk_index}"
    )
    return str(uuid.uuid5(_POINT_NAMESPACE, key))


@dataclass
class ChunkPayload:
    """The payload stored on every Qdrant point.

    Carries repository identity, the exact snapshot, source provenance and the
    evidence classification. ``doc_version`` and ``observed_at`` are optional
    and only populated when known.
    """

    repo: str
    commit_sha: str
    worktree_state: str
    snapshot_id: str
    path: str
    content_hash: str
    document_hash: str
    evidence_kind: str
    collection: str
    source_type: str
    chunk_index: int
    line_start: int
    line_end: int
    text: str
    heading: Optional[str] = None
    doc_version: Optional[str] = None
    observed_at: Optional[str] = None
    event_type: Optional[str] = None
    embedding_model: Optional[str] = None
    embedding_model_revision: Optional[str] = None
    indexed_at: str = field(default_factory=lambda: _utc_now())

    def to_qdrant(self) -> Dict[str, object]:
        """Flatten to a JSON-friendly Qdrant payload dict (drops ``None``)."""
        payload = {
            "repo": self.repo,
            "commit_sha": self.commit_sha,
            "worktree_state": self.worktree_state,
            "snapshot_id": self.snapshot_id,
            "path": self.path,
            "content_hash": self.content_hash,
            "document_hash": self.document_hash,
            "evidence_kind": self.evidence_kind,
            "collection": self.collection,
            "source_type": self.source_type,
            "chunk_index": self.chunk_index,
            "line_start": self.line_start,
            "line_end": self.line_end,
            "text": self.text,
            "heading": self.heading,
            "doc_version": self.doc_version,
            "observed_at": self.observed_at,
            "event_type": self.event_type,
            "embedding_model": self.embedding_model,
            "embedding_model_revision": self.embedding_model_revision,
            "indexed_at": self.indexed_at,
        }
        return {k: v for k, v in payload.items() if v is not None}


def _utc_now() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
