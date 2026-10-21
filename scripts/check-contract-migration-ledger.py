#!/usr/bin/env python3
"""Validate config-authority registry migration ledger consistency."""

from __future__ import annotations

from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LEDGER = ROOT / "docs/contracts/migration/config-authority-registry-migration-ledger.yaml"
EXPECTED_LEDGER_KIND = "config_authority_registry_migration_ledger"
EXPECTED_SNAPSHOT_KIND = "config_authority_registry_v1_snapshot"
ALLOWED_DISPOSITIONS = [
    "keep_as_migration_source",
    "split_into_claims",
    "mapped_to_v2_contract",
    "blocked_pending_identity",
    "blocked_pending_runtime_evidence",
    "superseded_after_cutover",
    "legacy_only",
    "rejected",
]
ALLOWED_STATUS_VALUES = {"todo", "in_progress", "complete", "blocked", "not_started", "not_applicable"}
REQUIRED_ENTRY_FIELDS = {
    "legacy_id",
    "source_digest",
    "source_status",
    "source_claims",
    "disposition",
    "target_contract_id",
    "target_contract_version",
    "inverse_supersession_present",
    "consumer_migration_status",
    "identity_backfill_status",
    "runtime_gate_status",
    "cutover_status",
    "rollback",
}


def fail(message: str) -> None:
    print(f"contract migration ledger check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load(path: Path):
    try:
        return yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover
        fail(f"cannot parse {path}: {error}")


def require_mapping(value, label: str) -> dict:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def require_string(value, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def require_list(value, label: str) -> list:
    if not isinstance(value, list):
        fail(f"{label} must be a list")
    return value


def require_bool(value, label: str) -> bool:
    if not isinstance(value, bool):
        fail(f"{label} must be a boolean")
    return value


def require_status_string(value, label: str) -> str:
    value = require_string(value, label)
    if value not in ALLOWED_STATUS_VALUES:
        fail(f"{label}={value!r} outside allowed status vocabulary")
    return value


def validate_entry(entry: dict, index: int, snapshot_digests: dict[str, str]) -> None:
    label = f"entries[{index}]"
    missing = sorted(REQUIRED_ENTRY_FIELDS - entry.keys())
    if missing:
        fail(f"{label} missing fields: {', '.join(missing)}")

    legacy_id = require_string(entry["legacy_id"], f"{label}.legacy_id")
    expected_digest = snapshot_digests.get(legacy_id)
    if expected_digest is None:
        fail(f"{label}.legacy_id={legacy_id!r} not present in snapshot contract_digests")

    source_digest = require_string(entry["source_digest"], f"{label}.source_digest")
    if source_digest != expected_digest:
        fail(f"{label}.source_digest does not match snapshot digest for {legacy_id}")

    source_status = require_mapping(entry["source_status"], f"{label}.source_status")
    for key in ("contract_status", "runtime_status", "release_status"):
        require_string(source_status.get(key), f"{label}.source_status.{key}")

    source_claims = require_list(entry["source_claims"], f"{label}.source_claims")
    for claim_index, claim in enumerate(source_claims, start=1):
        require_string(claim, f"{label}.source_claims[{claim_index}]")

    disposition = require_string(entry["disposition"], f"{label}.disposition")
    if disposition not in ALLOWED_DISPOSITIONS:
        fail(f"{label}.disposition={disposition!r} outside allowed dispositions")

    target_contract_id = entry["target_contract_id"]
    target_contract_version = entry["target_contract_version"]
    if disposition in {"mapped_to_v2_contract", "superseded_after_cutover", "split_into_claims"}:
        require_string(target_contract_id, f"{label}.target_contract_id")
        if not isinstance(target_contract_version, int) or target_contract_version < 1:
            fail(f"{label}.target_contract_version must be a positive integer")
    else:
        if target_contract_id not in ("", None):
            fail(f"{label}.target_contract_id must be empty for disposition {disposition}")
        if target_contract_version not in (0, None):
            fail(f"{label}.target_contract_version must be 0 or null for disposition {disposition}")

    inverse_supersession_present = require_bool(
        entry["inverse_supersession_present"],
        f"{label}.inverse_supersession_present",
    )
    if disposition == "superseded_after_cutover" and not inverse_supersession_present:
        fail(f"{label}.inverse_supersession_present must be true for superseded_after_cutover")

    require_status_string(entry["consumer_migration_status"], f"{label}.consumer_migration_status")
    require_status_string(entry["identity_backfill_status"], f"{label}.identity_backfill_status")
    require_status_string(entry["runtime_gate_status"], f"{label}.runtime_gate_status")
    require_status_string(entry["cutover_status"], f"{label}.cutover_status")
    require_string(entry["rollback"], f"{label}.rollback")


def main(argv: list[str]) -> None:
    target = Path(argv[0]) if argv else DEFAULT_LEDGER
    ledger_path = target if target.is_absolute() else ROOT / target

    ledger = require_mapping(load(ledger_path), str(target))
    snapshot_ref = require_mapping(ledger.get("source_snapshot"), f"{target}.source_snapshot")
    snapshot_path_value = require_string(snapshot_ref.get("path"), f"{target}.source_snapshot.path")
    snapshot_path = Path(snapshot_path_value)
    snapshot = require_mapping(load(ROOT / snapshot_path), snapshot_path_value)

    if ledger.get("schema_version") != 1:
        fail(f"{target}.schema_version must be 1")
    if ledger.get("ledger_kind") != EXPECTED_LEDGER_KIND:
        fail(f"{target}.ledger_kind must be {EXPECTED_LEDGER_KIND!r}")

    if snapshot.get("schema_version") != 1:
        fail(f"{snapshot_path_value}.schema_version must be 1")
    if snapshot.get("snapshot_kind") != EXPECTED_SNAPSHOT_KIND:
        fail(f"{snapshot_path_value}.snapshot_kind must be {EXPECTED_SNAPSHOT_KIND!r}")

    source_registry = require_mapping(ledger.get("source_registry"), f"{target}.source_registry")
    snapshot_source_registry = require_mapping(snapshot.get("source_registry"), f"{snapshot_path_value}.source_registry")

    for key in ("path", "sha256", "captured_at"):
        left = require_string(source_registry.get(key), f"{target}.source_registry.{key}")
        right = require_string(snapshot_source_registry.get(key), f"{snapshot_path_value}.source_registry.{key}")
        if left != right:
            fail(f"{target}.source_registry.{key} must match snapshot source_registry.{key}")

    snapshot_captured_at = require_string(snapshot_source_registry.get("captured_at"), f"{snapshot_path_value}.source_registry.captured_at")
    if require_string(snapshot_ref.get("captured_at"), f"{target}.source_snapshot.captured_at") != snapshot_captured_at:
        fail(f"{target}.source_snapshot.captured_at must match snapshot source_registry.captured_at")

    allowed = require_list(ledger.get("allowed_dispositions"), f"{target}.allowed_dispositions")
    if allowed != ALLOWED_DISPOSITIONS:
        fail(f"{target}.allowed_dispositions must match the published disposition list exactly")

    notes = require_list(ledger.get("notes"), f"{target}.notes")
    if not notes:
        fail(f"{target}.notes must not be empty")
    for index, note in enumerate(notes, start=1):
        require_string(note, f"{target}.notes[{index}]")

    entries = require_list(ledger.get("entries"), f"{target}.entries")
    snapshot_digests = {}
    for item in require_list(snapshot.get("contract_digests"), f"{snapshot_path_value}.contract_digests"):
        item = require_mapping(item, f"{snapshot_path_value}.contract_digests[]")
        legacy_id = require_string(item.get("legacy_id"), f"{snapshot_path_value}.contract_digests[].legacy_id")
        snapshot_digests[legacy_id] = require_string(
            item.get("source_digest"),
            f"{snapshot_path_value}.contract_digests[].source_digest",
        )

    seen_legacy_ids: set[str] = set()
    for index, entry in enumerate(entries, start=1):
        entry = require_mapping(entry, f"{target}.entries[{index}]")
        legacy_id = entry.get("legacy_id")
        if legacy_id in seen_legacy_ids:
            fail(f"{target}.entries has duplicate legacy_id {legacy_id!r}")
        seen_legacy_ids.add(legacy_id)
        validate_entry(entry, index, snapshot_digests)

    print(f"{target}: ok ({len(entries)} entries)")


if __name__ == "__main__":
    main(sys.argv[1:])
