#!/usr/bin/env python3
"""Validate the draft cross-domain authority registry."""

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
    "status",
    "evidence",
}


def fail(message: str) -> None:
    print(f"contract registry check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    try:
        document = yaml.safe_load(REGISTRY.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover - parser errors are environment-specific
        fail(f"cannot parse {REGISTRY}: {error}")

    if not isinstance(document, dict) or document.get("version") != 1:
        fail("registry must be a version 1 mapping")

    contracts = document.get("contracts")
    if not isinstance(contracts, list) or not contracts:
        fail("registry must contain a non-empty contracts list")

    ids = []
    unresolved = 0
    for index, contract in enumerate(contracts, start=1):
        if not isinstance(contract, dict):
            fail(f"contract {index} is not a mapping")
        missing = sorted(REQUIRED_CONTRACT_FIELDS - contract.keys())
        if missing:
            fail(f"contract {index} is missing: {', '.join(missing)}")
        if contract["id"] in ids:
            fail(f"duplicate contract id: {contract['id']}")
        ids.append(contract["id"])
        if not contract["consumers"]:
            fail(f"contract {contract['id']} has no consumers")

        adapter = contract["current_adapter"]
        if adapter != "none" and not (ROOT / adapter).exists():
            fail(f"{contract['id']} adapter path does not exist: {adapter}")
        evidence = contract["evidence"]
        if not (ROOT / evidence).exists():
            fail(f"{contract['id']} evidence path does not exist: {evidence}")

        is_unresolved = contract["authority"] == "unresolved"
        status = str(contract["status"])
        if is_unresolved:
            unresolved += 1
            if not status.startswith("blocker_"):
                fail(f"unresolved contract {contract['id']} must have blocker status")
        elif status.startswith("blocker_"):
            fail(f"resolved authority {contract['id']} cannot have blocker status")

    print(f"{len(contracts)} contracts validated; {unresolved} unresolved blocker(s).")


if __name__ == "__main__":
    main()
