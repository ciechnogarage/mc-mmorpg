#!/usr/bin/env python3
"""Validate runtime-critical evidence record integrity without runtime claims."""

from __future__ import annotations

from pathlib import Path
import re
import sys

import yaml


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REGISTER = ROOT / "docs/contracts/runtime-critical-evidence-register.yaml"
HEX_SHA256 = re.compile(r"^[0-9a-f]{64}$")
EXPECTED_KIND = "domain01_runtime_evidence_register"
REQUIRED_RECORD_FIELDS = {
    "record_id",
    "scenario_id",
    "contract_id",
    "server_start_log",
    "test_case_id",
    "commit",
    "backend",
    "paper_version",
    "plugin_jar_inventory",
    "configuration_hashes",
    "command_or_runner",
    "log_paths",
    "account_id",
    "account_namespace",
    "account_assurance_level",
    "character_namespace",
    "profile_id",
    "transaction_id",
    "correlation_id",
    "state_before",
    "state_after",
    "database_evidence",
    "restart_point",
    "result",
    "duplicate_count",
    "rollback_result",
    "evidence_hash",
}


def fail(message: str) -> None:
    print(f"contract evidence check failed: {message}", file=sys.stderr)
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


def require_list(value, label: str, *, allow_empty: bool = False) -> list:
    if not isinstance(value, list):
        fail(f"{label} must be a list")
    if not allow_empty and not value:
        fail(f"{label} must not be empty")
    return value


def validate_record(record: dict, index: int) -> None:
    label = f"records[{index}]"
    missing = sorted(REQUIRED_RECORD_FIELDS - record.keys())
    if missing:
        fail(f"{label} missing fields: {', '.join(missing)}")

    for field in (
        "record_id",
        "scenario_id",
        "contract_id",
        "server_start_log",
        "test_case_id",
        "commit",
        "backend",
        "paper_version",
        "plugin_jar_inventory",
        "command_or_runner",
        "account_id",
        "account_namespace",
        "account_assurance_level",
        "character_namespace",
        "profile_id",
        "transaction_id",
        "correlation_id",
        "restart_point",
        "result",
        "rollback_result",
    ):
        require_string(record.get(field), f"{label}.{field}")

    config_hashes = require_mapping(record.get("configuration_hashes"), f"{label}.configuration_hashes")
    if not config_hashes:
        fail(f"{label}.configuration_hashes must not be empty")
    for key, value in config_hashes.items():
        require_string(key, f"{label}.configuration_hashes key")
        hash_value = require_string(value, f"{label}.configuration_hashes[{key}]")
        if not HEX_SHA256.fullmatch(hash_value):
            fail(f"{label}.configuration_hashes[{key}] must be a lowercase sha256")

    log_paths = require_list(record.get("log_paths"), f"{label}.log_paths")
    for path_index, path in enumerate(log_paths, start=1):
        require_string(path, f"{label}.log_paths[{path_index}]")

    for field in ("state_before", "state_after", "database_evidence"):
        mapping = require_mapping(record.get(field), f"{label}.{field}")
        if not mapping:
            fail(f"{label}.{field} must not be empty")

    duplicate_count = record.get("duplicate_count")
    if not isinstance(duplicate_count, int) or duplicate_count < 0:
        fail(f"{label}.duplicate_count must be a non-negative integer")

    evidence_hash = require_string(record.get("evidence_hash"), f"{label}.evidence_hash")
    if not HEX_SHA256.fullmatch(evidence_hash):
        fail(f"{label}.evidence_hash must be a lowercase sha256")


def main(argv: list[str]) -> None:
    target = Path(argv[0]) if argv else DEFAULT_REGISTER
    path = target if target.is_absolute() else ROOT / target
    data = require_mapping(load(path), str(target))

    if data.get("schema_version") != 1:
        fail(f"{target}.schema_version must be 1")
    if data.get("register_kind") != EXPECTED_KIND:
        fail(f"{target}.register_kind must be {EXPECTED_KIND!r}")

    notes = require_list(data.get("notes"), f"{target}.notes")
    for index, note in enumerate(notes, start=1):
        require_string(note, f"{target}.notes[{index}]")

    records = require_list(data.get("records"), f"{target}.records", allow_empty=True)
    seen_ids: set[str] = set()
    for index, record in enumerate(records, start=1):
        record = require_mapping(record, f"{target}.records[{index}]")
        validate_record(record, index)
        record_id = record["record_id"]
        if record_id in seen_ids:
            fail(f"{target}.records has duplicate record_id {record_id!r}")
        seen_ids.add(record_id)

    print(f"{target}: ok ({len(records)} records)")


if __name__ == "__main__":
    main(sys.argv[1:])
