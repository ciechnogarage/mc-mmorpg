"""Content- and retrieval-semantics-derived exact repository snapshots."""
from __future__ import annotations

import hashlib
import json
import os
from dataclasses import dataclass
from typing import Iterable, Mapping, Optional

from . import classify, contract
from .paths import resolve_repo_file


class SnapshotMutationError(RuntimeError):
    """The repository changed while an exact snapshot was being assembled."""


@dataclass(frozen=True)
class SourceRecord:
    path: str
    absolute: str
    route: classify.Classification
    content_hash: str
    size: int
    mtime_ns: int


@dataclass(frozen=True)
class SnapshotDescriptor:
    commit_sha: str
    worktree_state: str
    snapshot_id: str


def scan_repository(
    repo_root: str,
    max_file_bytes: int,
    *,
    excluded_absolute_paths: Optional[Iterable[str]] = None,
) -> dict[str, SourceRecord]:
    """Rescan all safe candidate text and hash raw content deterministically."""
    root = os.path.abspath(repo_root)
    excluded = {os.path.abspath(path) for path in (excluded_absolute_paths or ())}
    records: dict[str, SourceRecord] = {}
    for directory, dirs, files in os.walk(root, topdown=True, followlinks=False):
        rel_dir = os.path.relpath(directory, root).replace(os.sep, "/")
        safe_dirs = []
        for name in dirs:
            relative = name if rel_dir == "." else f"{rel_dir}/{name}"
            if classify.is_excluded(relative) or os.path.islink(os.path.join(directory, name)):
                continue
            safe_dirs.append(name)
        dirs[:] = safe_dirs
        for name in files:
            relative = name if rel_dir == "." else f"{rel_dir}/{name}"
            route = classify.classify(relative)
            if route is None:
                continue
            try:
                normalized, absolute = resolve_repo_file(root, relative)
                if absolute in excluded:
                    continue
                before = os.stat(absolute)
                if not os.path.isfile(absolute) or before.st_size > max_file_bytes:
                    continue
                with open(absolute, "rb") as handle:
                    raw = handle.read()
                raw.decode("utf-8", errors="strict")
                after = os.stat(absolute)
            except (OSError, UnicodeError, ValueError):
                continue
            if (before.st_mtime_ns, before.st_size) != (after.st_mtime_ns, after.st_size):
                raise SnapshotMutationError(f"{normalized} changed during repository scan")
            records[normalized] = SourceRecord(
                normalized,
                absolute,
                route,
                hashlib.sha256(raw).hexdigest(),
                after.st_size,
                after.st_mtime_ns,
            )
    return dict(sorted(records.items()))


def snapshot_id_for(
    commit_sha: str,
    worktree_state: str,
    records: dict[str, SourceRecord],
    retrieval_semantics: Optional[
        Mapping[str, Mapping[str, Optional[str]]]
    ] = None,
    index_profile: Optional[Mapping[str, str]] = None,
) -> str:
    commit_sha = contract.validate_commit_sha(commit_sha)
    digest = hashlib.sha256()
    digest.update(b"mc-rag-semantic-snapshot-v2\0")
    digest.update(commit_sha.encode("ascii"))
    digest.update(b"\0")
    digest.update(worktree_state.encode("ascii"))
    profile = json.dumps(
        dict(sorted((index_profile or {}).items())),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    digest.update(len(profile).to_bytes(8, "big"))
    digest.update(profile)
    for path, record in sorted(records.items()):
        semantics = (retrieval_semantics or {}).get(path, {})
        canonical = json.dumps(
            [
                path,
                record.content_hash,
                semantics.get("collection"),
                semantics.get("event_type"),
                semantics.get("observed_at"),
                semantics.get("doc_version"),
            ],
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
        digest.update(len(canonical).to_bytes(8, "big"))
        digest.update(canonical)
    return f"{worktree_state}:{digest.hexdigest()}"


def descriptor_for(
    commit_sha: str,
    worktree_state: str,
    records: dict[str, SourceRecord],
    retrieval_semantics: Optional[
        Mapping[str, Mapping[str, Optional[str]]]
    ] = None,
    index_profile: Optional[Mapping[str, str]] = None,
) -> SnapshotDescriptor:
    commit_sha = contract.validate_commit_sha(commit_sha)
    return SnapshotDescriptor(
        commit_sha,
        worktree_state,
        snapshot_id_for(
            commit_sha,
            worktree_state,
            records,
            retrieval_semantics,
            index_profile,
        ),
    )


def records_signature(records: dict[str, SourceRecord]) -> dict[str, str]:
    return {path: record.content_hash for path, record in records.items()}
