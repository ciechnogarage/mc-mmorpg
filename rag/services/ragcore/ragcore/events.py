"""The webhook event contract.

The indexer accepts exactly six explicit event types. Each type either triggers
a full reconcile (``commit``) or routes a set of paths to a *forced* collection,
overriding path-based classification. This is the mechanism by which the rest
of the system tells the indexer "these files are observed runtime output" even
when their path alone would look like ordinary config.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional

from . import contract
from .classify import normalize

COMMIT = "commit"
DOCUMENTATION = "documentation"
PLUGIN = "plugin"
TEST_COMPLETED = "test_completed"
RUNTIME_REPORT = "runtime_report"
BOT_BENCHMARK = "bot_benchmark"

# Exactly these six — no more, no fewer.
EVENT_TYPES = (
    COMMIT,
    DOCUMENTATION,
    PLUGIN,
    TEST_COMPLETED,
    RUNTIME_REPORT,
    BOT_BENCHMARK,
)

# Which collection each path-carrying event forces. ``commit`` is absent because
# it reconciles the whole tree rather than routing specific paths.
EVENT_TARGET_COLLECTION: Dict[str, str] = {
    DOCUMENTATION: "expected_docs",
    PLUGIN: "expected_config",
    TEST_COMPLETED: "observed_benchmarks",
    RUNTIME_REPORT: "observed_runtime",
    BOT_BENCHMARK: "observed_benchmarks",
}


def is_valid_event(event_type: str) -> bool:
    return event_type in EVENT_TYPES


def target_collection(event_type: str) -> Optional[str]:
    """Forced collection for a path-routing event, or ``None`` for ``commit``."""
    if not is_valid_event(event_type):
        raise ValueError(f"unknown event type: {event_type!r}")
    return EVENT_TARGET_COLLECTION.get(event_type)


@dataclass
class WebhookEvent:
    """A parsed, validated webhook event.

    ``paths`` are repo-relative. ``observed_at`` is stamped on observed points
    when provided. ``doc_version`` is carried through when the sender knows it.
    """

    event_type: str
    paths: List[str] = field(default_factory=list)
    observed_at: Optional[str] = None
    doc_version: Optional[str] = None
    commit_sha: Optional[str] = None

    def __post_init__(self) -> None:
        if not is_valid_event(self.event_type):
            raise ValueError(
                f"unknown event type: {self.event_type!r}; "
                f"expected one of {EVENT_TYPES}"
            )
        if self.event_type != COMMIT and not self.paths:
            raise ValueError(f"{self.event_type} event requires at least one path")
        self.paths = [normalize(path) for path in self.paths]
        if self.commit_sha is not None:
            self.commit_sha = contract.validate_commit_sha(self.commit_sha)

    @property
    def is_reconcile(self) -> bool:
        return self.event_type == COMMIT

    @property
    def forced_collection(self) -> Optional[str]:
        return target_collection(self.event_type)

    @property
    def evidence_kind(self) -> Optional[str]:
        col = self.forced_collection
        return contract.evidence_kind_of(col) if col else None
