"""Exact-snapshot indexing, durable event overrides and incremental reconcile."""
from __future__ import annotations

import hashlib
import json
import os
import threading
import time
from dataclasses import asdict, dataclass
from typing import Any, Optional

from ragcore import chunk, contract
from ragcore.embedding_client import EmbeddingClient
from ragcore.events import WebhookEvent
from ragcore.gitmeta import GitInspector
from ragcore.qdrant_index import IndexPoint, InventoryEntry, VectorIndex
from ragcore.paths import resolve_repo_file
from ragcore.sanitize import redact_secrets
from ragcore.snapshot import (
    SnapshotDescriptor,
    SnapshotMutationError,
    SourceRecord,
    descriptor_for,
    records_signature,
    scan_repository,
)

from .config import Settings

Snapshot = SnapshotDescriptor
_STATE_VERSION = 2
# Hard cap on how many lines a single /source read may return, so a citation
# lookup can never turn into an unbounded document dump.
_SOURCE_MAX_LINES = 2000
_RECONCILE_EMBED_BATCH_CHUNKS = 32


class SourceUnavailable(RuntimeError):
    """Requested source cannot be served (no publication or path not indexed)."""


class PublicationNotReady(SourceUnavailable):
    """No fully validated snapshot has been published yet."""


class HistoricalSourceUnavailable(SourceUnavailable):
    """A non-published (historical/dirty) snapshot was requested for source.

    The repository is mounted read-only and only the currently published
    generation is reconstructable from the working tree, so historical or
    superseded dirty source is refused rather than silently served from
    whatever bytes happen to be on disk now.
    """


class SourceChanged(SourceUnavailable):
    """The on-disk file no longer matches the published manifest fingerprint."""


@dataclass(frozen=True)
class EventOverride:
    fingerprint: str
    collection: str
    event_type: str
    observed_at: Optional[str] = None
    doc_version: Optional[str] = None


@dataclass(frozen=True)
class PreparedRecord:
    record: SourceRecord
    desired: dict[str, Any]
    chunks: list[chunk.Chunk]


class IndexingService:
    def __init__(
        self,
        settings: Settings,
        git: GitInspector,
        embeddings: EmbeddingClient,
        index: VectorIndex,
    ) -> None:
        self.settings = settings
        self.git = git
        self.embeddings = embeddings
        self.index = index
        self.lock = threading.RLock()
        self._published_lock = threading.Lock()
        self.ready = False
        self.last_error: Optional[str] = None
        self.last_reconcile: Optional[float] = None
        self.last_snapshot: Optional[Snapshot] = None
        self._fingerprints: dict[str, str] = {}
        self._manifest: dict[str, dict[str, Any]] = {}
        self._overrides: dict[str, EventOverride] = {}
        self._resume_snapshot: Optional[Snapshot] = None
        self._resume_manifest: dict[str, dict[str, Any]] = {}
        self._resume_overrides: dict[str, EventOverride] = {}
        self._state_loaded = False
        self._manifest_initialized = False

    def _scan(self) -> dict[str, SourceRecord]:
        return scan_repository(
            self.settings.repo_path,
            self.settings.max_file_bytes,
            excluded_absolute_paths=[self.settings.state_path],
        )

    def _scan_current_source(
        self,
    ) -> tuple[str, str, dict[str, SourceRecord]]:
        commit_before = contract.validate_commit_sha(self.git.commit_sha())
        state_before = self.git.worktree_state()
        records = self._scan()
        commit_after = contract.validate_commit_sha(self.git.commit_sha())
        state_after = self.git.worktree_state()
        if (commit_before, state_before) != (commit_after, state_after):
            raise SnapshotMutationError("Git HEAD or worktree state changed during reconcile")
        return commit_before, state_before, records

    def _snapshot_for_source(
        self,
        commit_sha: str,
        worktree_state: str,
        records: dict[str, SourceRecord],
        desired: dict[str, dict[str, Any]],
    ) -> Snapshot:
        semantics = {
            path: {
                "collection": metadata.get("collection"),
                "event_type": metadata.get("event_type"),
                "observed_at": metadata.get("observed_at"),
                "doc_version": metadata.get("doc_version"),
            }
            for path, metadata in desired.items()
        }
        return descriptor_for(
            commit_sha,
            worktree_state,
            records,
            retrieval_semantics=semantics,
            index_profile={
                "embedding_model": self.settings.embedding_model,
                "embedding_revision": self.settings.embedding_model_revision,
            },
        )

    def _desired_view(
        self,
        records: dict[str, SourceRecord],
        overrides: dict[str, EventOverride],
    ) -> dict[str, dict[str, Any]]:
        return {
            path: self._desired_metadata(record, overrides)
            for path, record in records.items()
        }

    def snapshot(self) -> Snapshot:
        with self.lock:
            self._load_state()
            commit_sha, worktree_state, records = self._scan_current_source()
            overrides = self._pruned_overrides(records, self._overrides)
            desired = self._desired_view(records, overrides)
            return self._snapshot_for_source(
                commit_sha, worktree_state, records, desired
            )

    def published_snapshot(self) -> Snapshot:
        with self._published_lock:
            if not self.ready or self.last_snapshot is None:
                raise RuntimeError("no published snapshot is available")
            return self.last_snapshot

    def _published_view(self) -> tuple[Snapshot, dict[str, dict[str, Any]]]:
        """The published snapshot and its manifest, taken atomically."""
        with self._published_lock:
            if not self.ready or self.last_snapshot is None or not self._manifest_initialized:
                raise PublicationNotReady("no published snapshot is available yet")
            return self.last_snapshot, dict(self._manifest)

    @staticmethod
    def _slice_source(
        text: str,
        line_start: Optional[int],
        line_end: Optional[int],
    ) -> tuple[str, int, int, int, bool]:
        """Return (content, line_start, line_end, total_lines, truncated).

        Lines are 1-based and inclusive. When no explicit range is given the
        whole file is returned but capped at ``_SOURCE_MAX_LINES``.
        """
        lines = text.splitlines()
        total = len(lines)
        if total == 0:
            return "", 0, 0, 0, False
        if line_start is None and line_end is None:
            start, end = 1, min(total, _SOURCE_MAX_LINES)
        else:
            start = 1 if line_start is None else max(1, int(line_start))
            end = total if line_end is None else int(line_end)
            if end < start:
                raise ValueError("line_end must be greater than or equal to line_start")
            start = min(start, total)
            end = min(end, total)
            end = min(end, start + _SOURCE_MAX_LINES - 1)
        truncated = (start, end) != (1, total)
        return "\n".join(lines[start - 1 : end]), start, end, total, truncated

    def source_for(
        self,
        path: str,
        *,
        commit_sha: Optional[str] = None,
        worktree_state: Optional[str] = None,
        snapshot_id: Optional[str] = None,
        line_start: Optional[int] = None,
        line_end: Optional[int] = None,
    ) -> dict[str, Any]:
        """Return path-safe, published, fingerprint-verified source for ``path``.

        Source is served only from the currently published snapshot. An explicit
        snapshot selector must match that generation exactly; a different
        (historical or superseded dirty) selector is refused. The on-disk bytes
        must still match the published manifest fingerprint, otherwise a rebuild
        is required before the source can be trusted. Secrets are redacted.
        """
        self._load_state()
        published, manifest = self._published_view()

        selector = (commit_sha, worktree_state, snapshot_id)
        if any(value is not None for value in selector):
            if not all(value is not None for value in selector):
                raise ValueError(
                    "commit_sha, worktree_state and snapshot_id must be supplied together"
                )
            commit_sha = contract.validate_commit_sha(str(commit_sha))
            if worktree_state not in {
                contract.WORKTREE_CLEAN,
                contract.WORKTREE_DIRTY,
                contract.WORKTREE_UNKNOWN,
            }:
                raise ValueError("invalid worktree_state")
            contract.validate_snapshot_id(str(snapshot_id), commit_sha, worktree_state)
            if (commit_sha, worktree_state, snapshot_id) != (
                published.commit_sha,
                published.worktree_state,
                published.snapshot_id,
            ):
                raise HistoricalSourceUnavailable(
                    "source is only available for the currently published snapshot; "
                    "historical or superseded dirty source cannot be reconstructed"
                )

        normalized, absolute = resolve_repo_file(self.settings.repo_path, path)
        entry = manifest.get(normalized)
        if entry is None:
            raise SourceUnavailable(
                f"{normalized} is not part of the published snapshot"
            )
        try:
            with open(absolute, "rb") as handle:
                raw = handle.read()
            text = raw.decode("utf-8", errors="strict")
        except (OSError, UnicodeError) as exc:
            raise SourceUnavailable(f"{normalized} is not readable as text") from exc
        digest = hashlib.sha256(raw).hexdigest()
        if digest != str(entry.get("fingerprint", "")):
            raise SourceChanged(
                f"{normalized} changed since publication; an index rebuild is required "
                "before its source can be served"
            )
        redacted = redact_secrets(text)
        content, start, end, total, truncated = self._slice_source(
            redacted, line_start, line_end
        )
        collection = str(entry.get("collection", ""))
        return {
            "path": normalized,
            "snapshot": asdict(published),
            "collection": collection,
            "evidence_kind": contract.evidence_kind_of(collection)
            if collection
            else None,
            "content_hash": digest,
            "line_start": start,
            "line_end": end,
            "total_lines": total,
            "truncated": truncated,
            "content": content,
        }

    def _load_state(self) -> None:
        if self._state_loaded:
            return
        self._state_loaded = True
        self._resume_snapshot = None
        self._resume_manifest = {}
        self._resume_overrides = {}
        try:
            with open(self.settings.state_path, "r", encoding="utf-8") as handle:
                body = json.load(handle)
        except (OSError, ValueError, TypeError):
            return
        if body.get("version") != _STATE_VERSION or body.get("repo") != self.settings.repo_name:
            return
        snapshot_body = body.get("snapshot")
        manifest = body.get("manifest", {})
        overrides = body.get("overrides", {})
        if snapshot_body is not None:
            try:
                snapshot = Snapshot(
                    contract.validate_commit_sha(snapshot_body["commit_sha"]),
                    snapshot_body["worktree_state"],
                    snapshot_body["snapshot_id"],
                )
                if not isinstance(manifest, dict) or not isinstance(overrides, dict):
                    return
                parsed_overrides = {
                    path: EventOverride(**value)
                    for path, value in overrides.items()
                    if isinstance(value, dict)
                }
            except (KeyError, TypeError, ValueError):
                return
            self.last_snapshot = snapshot
            self._manifest = manifest
            self._manifest_initialized = True
            self._fingerprints = {
                path: str(value.get("fingerprint", ""))
                for path, value in manifest.items()
                if isinstance(value, dict)
            }
            self._overrides = parsed_overrides
            self.last_reconcile = body.get("last_reconcile")

        in_progress = body.get("in_progress")
        if not isinstance(in_progress, dict):
            return
        try:
            progress_snapshot_body = in_progress["snapshot"]
            progress_manifest = in_progress["manifest"]
            progress_overrides = in_progress.get("overrides", {})
            if not isinstance(progress_manifest, dict) or not isinstance(progress_overrides, dict):
                return
            self._resume_snapshot = Snapshot(
                contract.validate_commit_sha(progress_snapshot_body["commit_sha"]),
                progress_snapshot_body["worktree_state"],
                progress_snapshot_body["snapshot_id"],
            )
            self._resume_manifest = progress_manifest
            self._resume_overrides = {
                path: EventOverride(**value)
                for path, value in progress_overrides.items()
                if isinstance(value, dict)
            }
        except (KeyError, TypeError, ValueError):
            self._resume_snapshot = None
            self._resume_manifest = {}
            self._resume_overrides = {}

    @staticmethod
    def _write_state_json(handle, body: dict[str, Any]) -> None:
        json.dump(body, handle, sort_keys=True)

    @staticmethod
    def _sync_state_file(handle) -> None:
        handle.flush()
        os.fsync(handle.fileno())

    @staticmethod
    def _replace_state(source: str, target: str) -> None:
        os.replace(source, target)

    @staticmethod
    def _sync_state_directory(directory: str) -> None:
        flags = os.O_RDONLY | getattr(os, "O_DIRECTORY", 0)
        descriptor = os.open(directory, flags)
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)

    def _state_body(
        self,
        snapshot: Optional[Snapshot],
        manifest: dict[str, dict[str, Any]],
        overrides: dict[str, EventOverride],
        last_reconcile: Optional[float],
        *,
        in_progress: Optional[dict[str, Any]] = None,
    ) -> dict[str, Any]:
        body = {
            "version": _STATE_VERSION,
            "repo": self.settings.repo_name,
            "snapshot": asdict(snapshot) if snapshot is not None else None,
            "manifest": manifest,
            "overrides": {
                path: asdict(override) for path, override in overrides.items()
            },
            "last_reconcile": last_reconcile,
        }
        if in_progress is not None:
            body["in_progress"] = in_progress
        return body

    def _in_progress_state_body(
        self,
        snapshot: Snapshot,
        manifest: dict[str, dict[str, Any]],
        overrides: dict[str, EventOverride],
    ) -> dict[str, Any]:
        published_snapshot = self.last_snapshot if self._manifest_initialized else None
        published_manifest = self._manifest if self._manifest_initialized else {}
        published_overrides = self._overrides if self._manifest_initialized else {}
        published_reconcile = self.last_reconcile if self._manifest_initialized else None
        return self._state_body(
            published_snapshot,
            published_manifest,
            published_overrides,
            published_reconcile,
            in_progress={
                "snapshot": asdict(snapshot),
                "manifest": manifest,
                "overrides": {
                    path: asdict(override) for path, override in overrides.items()
                },
            },
        )

    def _restore_previous_state(
        self,
        *,
        previous_exists: bool,
        rollback: str,
        directory: str,
    ) -> None:
        if previous_exists:
            self._replace_state(rollback, self.settings.state_path)
        else:
            try:
                os.unlink(self.settings.state_path)
            except FileNotFoundError:
                pass
        self._sync_state_directory(directory)

    def _persist_candidate_state(self, body: dict[str, Any]) -> None:
        directory = os.path.dirname(self.settings.state_path)
        directory = directory or "."
        os.makedirs(directory, exist_ok=True)
        candidate = self.settings.state_path + ".candidate"
        rollback = self.settings.state_path + ".rollback"
        previous_exists = os.path.exists(self.settings.state_path)
        if previous_exists:
            with open(self.settings.state_path, "rb") as handle:
                previous = handle.read()
        else:
            previous = b""
        try:
            with open(candidate, "w", encoding="utf-8") as handle:
                self._write_state_json(handle, body)
                self._sync_state_file(handle)
            if previous_exists:
                with open(rollback, "wb") as handle:
                    handle.write(previous)
                    self._sync_state_file(handle)
            self._replace_state(candidate, self.settings.state_path)
            try:
                self._sync_state_directory(directory)
            except OSError:
                self._restore_previous_state(
                    previous_exists=previous_exists,
                    rollback=rollback,
                    directory=directory,
                )
                raise
        finally:
            for path in (candidate, rollback):
                try:
                    os.unlink(path)
                except FileNotFoundError:
                    pass

    @staticmethod
    def _pruned_overrides(
        records: dict[str, SourceRecord], overrides: dict[str, EventOverride]
    ) -> dict[str, EventOverride]:
        return {
            path: override
            for path, override in overrides.items()
            if path in records and records[path].content_hash == override.fingerprint
        }

    def _desired_metadata(
        self,
        record: SourceRecord,
        overrides: Optional[dict[str, EventOverride]] = None,
    ) -> dict[str, Any]:
        override = (overrides if overrides is not None else self._overrides).get(
            record.path
        )
        collection = override.collection if override else record.route.collection
        return {
            "fingerprint": record.content_hash,
            "collection": collection,
            "event_type": override.event_type if override else None,
            "observed_at": override.observed_at if override else None,
            "doc_version": override.doc_version if override else None,
            "embedding_model": self.settings.embedding_model,
            "embedding_model_revision": self.settings.embedding_model_revision,
        }

    @staticmethod
    def _entry_matches(entry: dict[str, Any], desired: dict[str, Any]) -> bool:
        return all(entry.get(key) == value for key, value in desired.items())

    @staticmethod
    def _expected_inventory(
        manifest: dict[str, dict[str, Any]],
    ) -> dict[tuple[str, str], InventoryEntry]:
        return {
            (str(entry["collection"]), path): tuple(
                sorted(
                    [
                        (
                            str(point["id"]),
                            int(point["chunk_index"]),
                            str(point["content_hash"]),
                            str(point["document_hash"]),
                        )
                        for point in entry.get("points", [])
                    ],
                    key=lambda item: (item[1], item[0]),
                )
            )
            for path, entry in manifest.items()
            if int(entry.get("chunks", 0)) > 0
        }

    @classmethod
    def _entry_inventory(
        cls, path: str, entry: dict[str, Any]
    ) -> InventoryEntry | None:
        expected = cls._expected_inventory({path: entry})
        return expected.get((str(entry.get("collection", "")), path))

    def _inventory_matches(
        self, snapshot: Snapshot, manifest: dict[str, dict[str, Any]]
    ) -> bool:
        actual = self.index.snapshot_inventory(
            self.settings.repo_name,
            snapshot.commit_sha,
            snapshot.worktree_state,
            snapshot.snapshot_id,
        )
        return actual == self._expected_inventory(manifest)

    def _delete_other_routes(self, snapshot: Snapshot, path: str, keep: str) -> None:
        for collection in contract.COLLECTION_NAMES:
            if collection != keep:
                self.index.delete_path(
                    collection,
                    self.settings.repo_name,
                    snapshot.commit_sha,
                    snapshot.worktree_state,
                    snapshot.snapshot_id,
                    path,
                )

    def _prepare_record(
        self,
        record: SourceRecord,
        desired: dict[str, Any],
    ) -> PreparedRecord:
        with open(record.absolute, "rb") as handle:
            raw = handle.read()
        if hashlib.sha256(raw).hexdigest() != record.content_hash:
            raise SnapshotMutationError(f"{record.path} changed during reconcile")
        text = redact_secrets(raw.decode("utf-8", errors="strict"))
        return PreparedRecord(record, dict(desired), chunk.chunk_text(text, record.path))

    def _write_prepared_record(
        self,
        prepared: PreparedRecord,
        snapshot: Snapshot,
        dense: list[list[float]],
        sparse: list[dict],
    ) -> tuple[int, dict[str, Any]]:
        record = prepared.record
        desired = prepared.desired
        chunks = prepared.chunks
        if len(dense) != len(chunks) or len(sparse) != len(chunks):
            raise RuntimeError("embedding service returned an unexpected vector count")
        collection = str(desired["collection"])
        evidence_kind = contract.evidence_kind_of(collection)
        points: list[IndexPoint] = []
        for item, dense_vector, sparse_vector in zip(chunks, dense, sparse):
            payload = contract.ChunkPayload(
                repo=self.settings.repo_name,
                commit_sha=snapshot.commit_sha,
                worktree_state=snapshot.worktree_state,
                snapshot_id=snapshot.snapshot_id,
                path=record.path,
                content_hash=item.content_hash,
                document_hash=record.content_hash,
                evidence_kind=evidence_kind,
                collection=collection,
                source_type=record.route.source_type,
                chunk_index=item.chunk_index,
                line_start=item.line_start,
                line_end=item.line_end,
                text=item.text,
                heading=item.heading,
                doc_version=desired.get("doc_version"),
                observed_at=desired.get("observed_at"),
                event_type=desired.get("event_type"),
                embedding_model=self.settings.embedding_model,
                embedding_model_revision=self.settings.embedding_model_revision,
            ).to_qdrant()
            points.append(
                IndexPoint(
                    id=contract.point_id(
                        self.settings.repo_name,
                        snapshot.commit_sha,
                        snapshot.worktree_state,
                        snapshot.snapshot_id,
                        record.path,
                        item.chunk_index,
                    ),
                    dense=dense_vector,
                    sparse=sparse_vector,
                    payload=payload,
                )
            )
        self._delete_other_routes(snapshot, record.path, collection)
        self.index.replace_path(
            collection,
            self.settings.repo_name,
            snapshot.commit_sha,
            snapshot.worktree_state,
            snapshot.snapshot_id,
            record.path,
            points,
        )
        entry = dict(desired)
        entry["chunks"] = len(points)
        entry["points"] = [
            {
                "id": point.id,
                "chunk_index": int(point.payload["chunk_index"]),
                "content_hash": str(point.payload["content_hash"]),
                "document_hash": str(point.payload["document_hash"]),
            }
            for point in points
        ]
        return len(points), entry

    def _index_prepared_records(
        self,
        prepared_records: list[PreparedRecord],
        snapshot: Snapshot,
    ) -> dict[str, tuple[int, dict[str, Any]]]:
        texts = [
            item.text
            for prepared in prepared_records
            for item in prepared.chunks
        ]
        dense, sparse = self.embeddings.embed(texts) if texts else ([], [])
        if len(dense) != len(texts) or len(sparse) != len(texts):
            raise RuntimeError("embedding service returned an unexpected vector count")
        results: dict[str, tuple[int, dict[str, Any]]] = {}
        offset = 0
        for prepared in prepared_records:
            end = offset + len(prepared.chunks)
            results[prepared.record.path] = self._write_prepared_record(
                prepared,
                snapshot,
                dense[offset:end],
                sparse[offset:end],
            )
            offset = end
        return results

    def _index_record(
        self,
        record: SourceRecord,
        snapshot: Snapshot,
        desired: dict[str, Any],
    ) -> tuple[int, dict[str, Any]]:
        prepared = self._prepare_record(record, desired)
        return self._index_prepared_records([prepared], snapshot)[record.path]

    def _clone_record(
        self,
        record: SourceRecord,
        source_snapshot: Snapshot,
        target_snapshot: Snapshot,
        source_entry: dict[str, Any],
        desired: dict[str, Any],
    ) -> tuple[int, dict[str, Any]]:
        collection = str(desired["collection"])
        source_collection = str(source_entry["collection"])
        source_points = self.index.load_path_points(
            source_collection,
            self.settings.repo_name,
            source_snapshot.commit_sha,
            source_snapshot.worktree_state,
            source_snapshot.snapshot_id,
            record.path,
        )
        source_actual: InventoryEntry = tuple(
            (
                point.id,
                int(point.payload.get("chunk_index", -1)),
                str(point.payload.get("content_hash", "")),
                str(point.payload.get("document_hash", "")),
            )
            for point in source_points
        )
        if source_actual != self._entry_inventory(record.path, source_entry):
            raise RuntimeError(f"source vectors are incomplete for {record.path}")

        evidence_kind = contract.evidence_kind_of(collection)
        cloned: list[IndexPoint] = []
        for point in source_points:
            chunk_index = int(point.payload["chunk_index"])
            payload = dict(point.payload)
            payload.update(
                repo=self.settings.repo_name,
                commit_sha=target_snapshot.commit_sha,
                worktree_state=target_snapshot.worktree_state,
                snapshot_id=target_snapshot.snapshot_id,
                path=record.path,
                document_hash=record.content_hash,
                collection=collection,
                evidence_kind=evidence_kind,
                event_type=desired.get("event_type"),
                observed_at=desired.get("observed_at"),
                doc_version=desired.get("doc_version"),
                embedding_model=self.settings.embedding_model,
                embedding_model_revision=self.settings.embedding_model_revision,
            )
            cloned.append(
                IndexPoint(
                    id=contract.point_id(
                        self.settings.repo_name,
                        target_snapshot.commit_sha,
                        target_snapshot.worktree_state,
                        target_snapshot.snapshot_id,
                        record.path,
                        chunk_index,
                    ),
                    dense=list(point.dense),
                    sparse={
                        "indices": list(point.sparse["indices"]),
                        "values": list(point.sparse["values"]),
                    },
                    payload=payload,
                )
            )
        self._delete_other_routes(target_snapshot, record.path, collection)
        self.index.replace_path(
            collection,
            self.settings.repo_name,
            target_snapshot.commit_sha,
            target_snapshot.worktree_state,
            target_snapshot.snapshot_id,
            record.path,
            cloned,
        )
        entry = dict(desired)
        entry["chunks"] = len(cloned)
        entry["points"] = [
            {
                "id": point.id,
                "chunk_index": int(point.payload["chunk_index"]),
                "content_hash": str(point.payload["content_hash"]),
                "document_hash": str(point.payload["document_hash"]),
            }
            for point in cloned
        ]
        return len(cloned), entry

    def _translated_entry(
        self,
        record: SourceRecord,
        target_snapshot: Snapshot,
        source_entry: dict[str, Any],
        desired: dict[str, Any],
    ) -> dict[str, Any]:
        points = [
            {
                "id": contract.point_id(
                    self.settings.repo_name,
                    target_snapshot.commit_sha,
                    target_snapshot.worktree_state,
                    target_snapshot.snapshot_id,
                    record.path,
                    int(point["chunk_index"]),
                ),
                "chunk_index": int(point["chunk_index"]),
                "content_hash": str(point["content_hash"]),
                "document_hash": record.content_hash,
            }
            for point in source_entry.get("points", [])
        ]
        entry = dict(desired)
        entry["chunks"] = len(points)
        entry["points"] = points
        return entry

    def index_file(
        self,
        path: str,
        snapshot: Optional[Snapshot] = None,
        *,
        forced_collection: Optional[str] = None,
        event_type: Optional[str] = None,
        observed_at: Optional[str] = None,
        doc_version: Optional[str] = None,
    ) -> int:
        commit_sha, worktree_state, records = self._scan_current_source()
        overrides = self._pruned_overrides(records, self._overrides)
        desired_view = self._desired_view(records, overrides)
        try:
            record = records[path]
        except KeyError:
            raise ValueError(f"path is excluded or is not recognised text: {path}") from None
        desired = desired_view[path]
        if forced_collection is not None:
            desired.update(
                collection=forced_collection,
                event_type=event_type,
                observed_at=observed_at,
                doc_version=doc_version,
            )
        current_snapshot = self._snapshot_for_source(
            commit_sha, worktree_state, records, desired_view
        )
        snapshot = snapshot or current_snapshot
        if snapshot != current_snapshot:
            raise SnapshotMutationError("requested snapshot no longer matches repository")
        chunks_written, _entry = self._index_record(record, snapshot, desired)
        return chunks_written

    def reconcile(
        self,
        *,
        force_paths: Optional[set[str]] = None,
        override_updates: Optional[dict[str, EventOverride]] = None,
        override_removals: Optional[set[str]] = None,
    ) -> dict[str, int]:
        with self.lock:
            self._load_state()
            force_paths = force_paths or set()
            try:
                commit_sha, worktree_state, records = self._scan_current_source()
                candidate_overrides = dict(self._overrides)
                for path in override_removals or set():
                    candidate_overrides.pop(path, None)
                if override_updates:
                    candidate_overrides.update(override_updates)
                candidate_overrides = self._pruned_overrides(
                    records, candidate_overrides
                )
                desired = self._desired_view(records, candidate_overrides)
                snapshot = self._snapshot_for_source(
                    commit_sha, worktree_state, records, desired
                )
                source_snapshot = self.last_snapshot
                source_manifest = self._manifest if self._manifest_initialized else {}
                if self._resume_snapshot == snapshot and self._resume_manifest:
                    source_snapshot = self._resume_snapshot
                    source_manifest = dict(self._resume_manifest)

                source_inventory = (
                    self.index.snapshot_inventory(
                        self.settings.repo_name,
                        source_snapshot.commit_sha,
                        source_snapshot.worktree_state,
                        source_snapshot.snapshot_id,
                    )
                    if source_snapshot is not None and source_manifest
                    else {}
                )
                target_inventory = (
                    self.index.snapshot_inventory(
                        self.settings.repo_name,
                        snapshot.commit_sha,
                        snapshot.worktree_state,
                        snapshot.snapshot_id,
                    )
                    if source_snapshot != snapshot
                    else source_inventory
                )

                new_manifest: dict[str, dict[str, Any]] = {}
                chunks_written = 0
                embedded_files = 0
                reused_files = 0
                pending_embeddings: list[PreparedRecord] = []
                pending_chunks = 0
                checkpointed_entries = 0

                def persist_in_progress(force: bool = False) -> None:
                    nonlocal checkpointed_entries
                    if not force and len(new_manifest) - checkpointed_entries < 25:
                        return
                    self._persist_candidate_state(
                        self._in_progress_state_body(
                            snapshot,
                            dict(new_manifest),
                            candidate_overrides,
                        )
                    )
                    checkpointed_entries = len(new_manifest)

                def flush_pending_embeddings() -> None:
                    nonlocal chunks_written, embedded_files, pending_chunks
                    if not pending_embeddings:
                        return
                    indexed = self._index_prepared_records(pending_embeddings, snapshot)
                    for prepared in pending_embeddings:
                        written, entry = indexed[prepared.record.path]
                        chunks_written += written
                        embedded_files += 1
                        new_manifest[prepared.record.path] = entry
                    pending_embeddings.clear()
                    pending_chunks = 0
                    persist_in_progress(force=True)

                for path in sorted(records):
                    source_entry = source_manifest.get(path)
                    source_key = (
                        (str(source_entry.get("collection", "")), path)
                        if source_entry
                        else None
                    )
                    source_complete = bool(
                        source_entry
                        and source_key is not None
                        and self._entry_inventory(path, source_entry)
                        == source_inventory.get(source_key)
                    )
                    metadata_matches = bool(
                        source_entry
                        and self._entry_matches(source_entry, desired[path])
                    )
                    content_matches = bool(
                        source_entry
                        and source_entry.get("fingerprint")
                        == desired[path].get("fingerprint")
                    )
                    vector_profile_matches = bool(
                        source_entry
                        and source_entry.get("embedding_model")
                        == desired[path].get("embedding_model")
                        and source_entry.get("embedding_model_revision")
                        == desired[path].get("embedding_model_revision")
                    )

                    if (
                        source_complete
                        and metadata_matches
                        and source_snapshot == snapshot
                    ):
                        new_manifest[path] = dict(source_entry)
                        persist_in_progress()
                        continue

                    if (
                        source_complete
                        and content_matches
                        and vector_profile_matches
                        and source_snapshot is not None
                    ):
                        translated = self._translated_entry(
                            records[path], snapshot, source_entry, desired[path]
                        )
                        target_key = (str(desired[path]["collection"]), path)
                        if self._entry_inventory(path, translated) == target_inventory.get(
                            target_key
                        ):
                            new_manifest[path] = translated
                            reused_files += 1
                            persist_in_progress()
                            continue

                        written, entry = self._clone_record(
                            records[path],
                            source_snapshot,
                            snapshot,
                            source_entry,
                            desired[path],
                        )
                        chunks_written += written
                        reused_files += 1
                        new_manifest[path] = entry
                        persist_in_progress()
                        continue

                    prepared = self._prepare_record(records[path], desired[path])
                    record_chunks = len(prepared.chunks)
                    if (
                        pending_embeddings
                        and pending_chunks + record_chunks
                        > _RECONCILE_EMBED_BATCH_CHUNKS
                    ):
                        flush_pending_embeddings()
                    pending_embeddings.append(prepared)
                    pending_chunks += record_chunks
                    if pending_chunks >= _RECONCILE_EMBED_BATCH_CHUNKS:
                        flush_pending_embeddings()

                flush_pending_embeddings()

                live = {name: set() for name in contract.COLLECTION_NAMES}
                for path, metadata in desired.items():
                    live[str(metadata["collection"])].add(path)
                deleted_points = sum(
                    self.index.delete_stale_paths(
                        collection,
                        self.settings.repo_name,
                        snapshot.commit_sha,
                        snapshot.worktree_state,
                        snapshot.snapshot_id,
                        live[collection],
                    )
                    for collection in contract.COLLECTION_NAMES
                )

                end_commit, end_state, end_records = self._scan_current_source()
                if (
                    (commit_sha, worktree_state) != (end_commit, end_state)
                    or records_signature(records) != records_signature(end_records)
                ):
                    raise SnapshotMutationError("repository changed during reconcile")
                if not self._inventory_matches(snapshot, new_manifest):
                    raise RuntimeError("Qdrant inventory does not match completed manifest")

                candidate_fingerprints = records_signature(records)
                candidate_reconcile = time.time()
                candidate_body = self._state_body(
                    snapshot,
                    new_manifest,
                    candidate_overrides,
                    candidate_reconcile,
                )
                self._persist_candidate_state(candidate_body)
                with self._published_lock:
                    self._manifest = new_manifest
                    self._manifest_initialized = True
                    self._fingerprints = candidate_fingerprints
                    self._overrides = candidate_overrides
                    self._resume_snapshot = None
                    self._resume_manifest = {}
                    self._resume_overrides = {}
                    self.last_snapshot = snapshot
                    self.last_reconcile = candidate_reconcile
                    self.last_error = None
                    self.ready = True
                return {
                    "files": len(records),
                    "chunks": chunks_written,
                    "deleted": deleted_points,
                    "embedded_files": embedded_files,
                    "reused_files": reused_files,
                }
            except SnapshotMutationError:
                if self.last_snapshot is None:
                    with self._published_lock:
                        self.ready = False
                raise

    def handle_event(self, event: WebhookEvent) -> dict[str, Any]:
        with self.lock:
            self._load_state()
            commit_sha, _worktree_state, records = self._scan_current_source()
            if event.commit_sha is not None and event.commit_sha != commit_sha:
                raise ValueError("event commit_sha does not match the mounted repository HEAD")
            if event.is_reconcile:
                return {"event_type": event.event_type, **self.reconcile()}
            event_records: dict[str, SourceRecord] = {}
            for path in event.paths:
                try:
                    event_records[path] = records[path]
                except KeyError:
                    raise ValueError(
                        f"path is excluded or is not recognised text: {path}"
                    ) from None
            override_updates = {
                path: EventOverride(
                    fingerprint=record.content_hash,
                    collection=str(event.forced_collection),
                    event_type=event.event_type,
                    observed_at=event.observed_at,
                    doc_version=event.doc_version,
                )
                for path, record in event_records.items()
            }
            result = self.reconcile(
                force_paths=set(event.paths), override_updates=override_updates
            )
            for path in event.paths:
                if path not in self._overrides:
                    raise SnapshotMutationError(f"{path} changed while applying event override")
            return {
                "event_type": event.event_type,
                "paths": len(event.paths),
                "chunks": result["chunks"],
            }

    def watch_once(self) -> dict[str, int]:
        with self.lock:
            self._load_state()
            commit_sha, worktree_state, records = self._scan_current_source()
            candidate_overrides = self._pruned_overrides(records, self._overrides)
            desired = self._desired_view(records, candidate_overrides)
            snapshot = self._snapshot_for_source(
                commit_sha, worktree_state, records, desired
            )
            current = records_signature(records)
            changed = {
                path
                for path, fingerprint in current.items()
                if self._fingerprints.get(path) != fingerprint
            }
            deleted = set(self._fingerprints) - set(current)
            if snapshot == self.last_snapshot and not changed and not deleted:
                if self._manifest and self._inventory_matches(snapshot, self._manifest):
                    return {"changed": 0, "deleted": 0, "chunks": 0}
            result = self.reconcile()
            return {
                "changed": len(changed),
                "deleted": len(deleted),
                "chunks": result["chunks"],
            }

    def run_forever(self, stop: threading.Event) -> None:
        while not stop.is_set():
            try:
                self.index.ensure_collections()
                if not self.ready or self.last_reconcile is None:
                    self.reconcile()
                elif time.time() - self.last_reconcile >= self.settings.reconcile_interval:
                    self.reconcile()
                else:
                    self.watch_once()
                self.last_error = None
            except Exception as exc:
                self.last_error = f"{type(exc).__name__}: {exc}"
                if self.last_snapshot is None:
                    self.ready = False
            if stop.wait(self.settings.watch_interval):
                break
