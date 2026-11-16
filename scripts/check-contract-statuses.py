#!/usr/bin/env python3
"""Validate published contract status vocabularies and release gates."""

from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_VOCABULARY = {
    "contract_status": ["draft", "proposed", "accepted", "superseded", "rejected"],
    "runtime_status": ["not_tested", "blocked", "partial", "verified", "failed"],
    "release_status": ["blocked", "conditional", "approved"],
}
ALLOWED = {key: set(values) for key, values in EXPECTED_VOCABULARY.items()}


def fail(message: str) -> None:
    print(f"contract status check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load(path: Path):
    try:
        return yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover
        fail(f"cannot parse {path}: {error}")


def check_statuses(value, label: str) -> None:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")

    for key, allowed in ALLOWED.items():
        status = value.get(key)
        if status not in allowed:
            fail(f"{label}.{key}={status!r} outside allowed vocabulary")

    if value["release_status"] == "approved" and value["runtime_status"] != "verified":
        fail(f"{label} cannot be release approved without runtime_status: verified")


def main() -> None:
    registry = load(ROOT / "docs/config_authority_registry.yaml")
    if registry.get("status_vocabulary") != EXPECTED_VOCABULARY:
        fail("registry status_vocabulary does not match the published vocabulary")

    check_statuses(registry, "registry")
    for index, contract in enumerate(registry.get("contracts", []), start=1):
        check_statuses(contract, f"contract[{index}]/{contract.get('id')}")

    reward = load(ROOT / "docs/contracts/reward-contract.yaml")
    check_statuses(reward.get("contract"), "reward-contract.contract")

    print("release gate validated")


if __name__ == "__main__":
    main()
