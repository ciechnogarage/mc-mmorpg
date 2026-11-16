#!/usr/bin/env python3
"""Validate the legacy v1 cross-domain authority registry."""

from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "docs/config_authority_registry.yaml"
REQUIRED_CONTRACT_FIELDS = {
    "id",
    "domain",
    "authority",
    "current_adapter",
    "target_persistence",
    "consumers",
    "contract_status",
    "runtime_status",
    "release_status",
    "evidence",
}
EXPECTED_STATUS_VALUES = {
    "contract_status": {"draft", "proposed", "accepted", "superseded", "rejected"},
    "runtime_status": {"not_tested", "blocked", "partial", "verified", "failed"},
    "release_status": {"blocked", "conditional", "approved"},
}
EXPECTED_STATUS_VOCABULARY = {
    "contract_status": ["draft", "proposed", "accepted", "superseded", "rejected"],
    "runtime_status": ["not_tested", "blocked", "partial", "verified", "failed"],
    "release_status": ["blocked", "conditional", "approved"],
}
DOMAIN_02_REPLACEMENT_IDS = {
    "account_identity",
    "character_identity_lifecycle",
    "mmoprofiles_plugin_profile",
    "character_stage_presentation",
}


def fail(message: str) -> None:
    print(f"contract registry check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def require_string(value, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def main() -> None:
    try:
        document = yaml.safe_load(REGISTRY.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover
        fail(f"cannot parse {REGISTRY}: {error}")

    if not isinstance(document, dict) or document.get("version") != 1:
        fail("registry must be a version 1 mapping")

    contracts = document.get("contracts")
    if not isinstance(contracts, list) or not contracts:
        fail("registry must contain a non-empty contracts list")

    status_vocabulary = document.get("status_vocabulary")
    if status_vocabulary != EXPECTED_STATUS_VOCABULARY:
        fail("registry.status_vocabulary does not match the published vocabulary")
    published_status_values = {
        field: set(values) for field, values in status_vocabulary.items()
    }

    ids: list[str] = []
    unresolved = 0

    for index, contract in enumerate(contracts, start=1):
        if not isinstance(contract, dict):
            fail(f"contract {index} is not a mapping")

        missing = sorted(REQUIRED_CONTRACT_FIELDS - contract.keys())
        if missing:
            fail(f"contract {index} missing fields: {', '.join(missing)}")

        contract_id = contract["id"]
        if contract_id in ids:
            fail(f"duplicate contract id: {contract_id}")
        ids.append(contract_id)

        require_string(contract_id, f"contract {index}.id")
        require_string(contract["domain"], f"contract {contract_id}.domain")
        require_string(contract["authority"], f"contract {contract_id}.authority")
        require_string(contract["target_persistence"], f"contract {contract_id}.target_persistence")
        for status_field, allowed in published_status_values.items():
            if contract.get(status_field) not in allowed:
                fail(f"{contract_id}.{status_field} is outside the published status vocabulary")

        consumers = contract["consumers"]
        if not isinstance(consumers, list) or not consumers:
            fail(f"contract {contract_id} must declare at least one consumer")

        adapter = require_string(contract["current_adapter"], f"contract {contract_id}.current_adapter")
        if adapter != "none" and not (ROOT / adapter).exists():
            fail(f"{contract_id} adapter path does not exist: {adapter}")

        evidence = require_string(contract["evidence"], f"contract {contract_id}.evidence")
        if not (ROOT / evidence).exists():
            fail(f"{contract_id} evidence path does not exist: {evidence}")

        if contract["authority"] == "unresolved":
            unresolved += 1

    expected_rows = {row["id"]: row for row in contracts if row.get("id") in DOMAIN_02_REPLACEMENT_IDS}
    if set(expected_rows) != DOMAIN_02_REPLACEMENT_IDS:
        fail("config authority registry must contain all four domain-02 replacement entries")
    scope_path = ROOT / "docs/contracts/character-scope-registry.yaml"
    try:
        scope_document = yaml.safe_load(scope_path.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover
        fail(f"cannot parse canonical replacement source {scope_path}: {error}")
    replacement = scope_document.get("replacement_rows", {}).get("config_authority_registry")
    if not isinstance(replacement, list):
        fail("canonical replacement source must contain replacement_rows.config_authority_registry")
    canonical_rows = {row.get("id"): row for row in replacement if isinstance(row, dict)}
    if set(canonical_rows) != DOMAIN_02_REPLACEMENT_IDS:
        fail("canonical replacement source must contain exactly the four domain-02 entries")
    for contract_id in sorted(DOMAIN_02_REPLACEMENT_IDS):
        if expected_rows[contract_id] != canonical_rows[contract_id]:
            fail(f"{contract_id} does not match canonical replacement source")

    print(f"{len(contracts)} contracts validated (static declarations only)")
    print(f"{unresolved} unresolved blocker(s) (static declarations only)")


if __name__ == "__main__":
    main()
