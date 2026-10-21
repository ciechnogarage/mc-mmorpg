"""Discoverable MCP input and output contracts.

The public tool schemas mirror the validation limits enforced by ``rag-api``
and retain the API's structured citation/snapshot payloads verbatim.
"""
from __future__ import annotations

from typing import Annotated, Any, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, StringConstraints

CollectionName = Literal[
    "expected_docs",
    "expected_config",
    "observed_runtime",
    "observed_benchmarks",
]
ExpectedCollectionName = Literal["expected_docs", "expected_config"]
ObservedCollectionName = Literal["observed_runtime", "observed_benchmarks"]
EventType = Literal[
    "commit",
    "documentation",
    "plugin",
    "test_completed",
    "runtime_report",
    "bot_benchmark",
]
WorktreeState = Literal["clean", "dirty", "unknown"]
EvidenceKind = Literal["expected", "observed"]

QueryValue = Annotated[str, StringConstraints(min_length=1, max_length=2000)]
PathValue = Annotated[str, StringConstraints(min_length=1, max_length=4096)]
CommitSha = Annotated[
    str, StringConstraints(pattern=r"^[0-9a-fA-F]{40}$")
]
SnapshotId = Annotated[
    str, StringConstraints(pattern=r"^(?:clean|dirty|unknown):[0-9a-f]{64}$")
]
LimitValue = Annotated[int, Field(ge=1, le=100)]
LineNumber = Annotated[int, Field(ge=1)]
ProvenanceValue = Annotated[str, StringConstraints(max_length=256)]
CollectionList = Annotated[list[CollectionName], Field(min_length=1, max_length=4)]
ExpectedCollectionList = Annotated[
    list[ExpectedCollectionName], Field(min_length=1, max_length=2)
]
ObservedCollectionList = Annotated[
    list[ObservedCollectionName], Field(min_length=1, max_length=2)
]
EventPaths = Annotated[list[PathValue], Field(min_length=1, max_length=100)]


class ContractModel(BaseModel):
    model_config = ConfigDict(extra="allow")


class SnapshotResponse(ContractModel):
    commit_sha: CommitSha
    worktree_state: WorktreeState
    snapshot_id: SnapshotId


class CitationResponse(SnapshotResponse):
    path: PathValue
    line_start: LineNumber
    line_end: LineNumber
    evidence_kind: EvidenceKind
    collection: CollectionName
    content_hash: Annotated[str, StringConstraints(pattern=r"^[0-9a-f]{64}$")]
    embedding_model: str
    embedding_model_revision: CommitSha
    heading: Optional[str] = None
    observed_at: Optional[str] = None


class ScoreResponse(ContractModel):
    fusion: float
    rerank: float
    score: float


class SearchResultResponse(ContractModel):
    text: str
    citation: CitationResponse
    scores: ScoreResponse


class SearchResponse(ContractModel):
    snapshot: SnapshotResponse
    results: list[SearchResultResponse]


class CompareResponse(ContractModel):
    snapshot: SnapshotResponse
    expected: list[SearchResultResponse]
    observed: list[SearchResultResponse]


class SourceResponse(ContractModel):
    path: PathValue
    snapshot: SnapshotResponse
    collection: CollectionName
    evidence_kind: EvidenceKind
    content_hash: Annotated[str, StringConstraints(pattern=r"^[0-9a-f]{64}$")]
    line_start: Annotated[int, Field(ge=0)]
    line_end: Annotated[int, Field(ge=0)]
    total_lines: Annotated[int, Field(ge=0)]
    truncated: bool
    content: str


class IndexStatusResponse(ContractModel):
    indexer: dict[str, Any]
    published_snapshot: dict[str, Any]


class EventResponse(ContractModel):
    event_type: EventType
