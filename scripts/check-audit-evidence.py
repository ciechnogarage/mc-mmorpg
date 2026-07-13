"""Validate the audit evidence register and its local evidence artifacts."""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
REGISTER = ROOT / "docs/audits/2026-07-13/evidence-register.yaml"
HEX_SHA256 = re.compile(r"^[0-9a-fA-F]{64}$")
ALLOWED_STATUSES = {
    "current",
    "validated",
    "prepared_not_live_validated",
    "mysql_disabled",
    "compatibility_seam_present",
    "partially_verified",
    "externally_supplied_evidence",
    "statically_analyzed",
    "not_activated",
    "pass",
    "pass_with_blockers",
    "missing",
}


def fail(message: str) -> None:
    raise SystemExit(f"audit evidence check failed: {message}")


def check_hash(path: Path, expected: str) -> None:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != expected.lower():
        fail(f"SHA-256 mismatch for {path}: expected {expected}, got {digest}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--allow-missing-external-hash",
        action="store_true",
        help="report a pending external hash as a blocker but exit successfully",
    )
    args = parser.parse_args()

    try:
        data = yaml.safe_load(REGISTER.read_text(encoding="utf-8"))
    except Exception as error:
        fail(f"cannot parse {REGISTER}: {error}")

    if not isinstance(data, dict) or data.get("version") != 1:
        fail("register must be a version 1 mapping")

    evidence = data.get("evidence")
    if not isinstance(evidence, list) or not evidence:
        fail("register must contain evidence entries")

    ids = set()
    pending_hashes = []
    for entry in evidence:
        if not isinstance(entry, dict):
            fail("each evidence entry must be a mapping")
        for field in ("id", "kind", "status"):
            if not entry.get(field):
                fail(f"evidence entry missing {field}")
        if entry["id"] in ids:
            fail(f"duplicate evidence id: {entry['id']}")
        ids.add(entry["id"])
        if entry["status"] not in ALLOWED_STATUSES and entry["status"] != "unavailable_external_file":
            fail(f"unknown status for {entry['id']}: {entry['status']}")

        if entry["kind"] != "external_binary":
            path = ROOT / entry.get("path", "")
            if not path.exists():
                fail(f"local evidence path does not exist: {entry['id']} -> {path}")
        else:
            if entry.get("tracked_in_git") is not False:
                fail("external binary cannot be marked tracked_in_git=true")
            if entry.get("runtime_status") == "active":
                fail("external binary cannot be active runtime content")
            sha256 = entry.get("sha256")
            if sha256 is None:
                pending_hashes.append(entry["id"])
            elif not HEX_SHA256.fullmatch(str(sha256)):
                fail(f"external binary has invalid SHA-256: {entry['id']}")
            else:
                archive_path = ROOT / entry.get("path", "")
                if archive_path.exists():
                    check_hash(archive_path, str(sha256))

    checks = data.get("checks")
    if not isinstance(checks, dict) or not checks:
        fail("register must contain checks")
    for check_id, check in checks.items():
        for field in ("command", "executed_at", "exit_code", "output", "status"):
            if field not in check:
                fail(f"check {check_id} missing {field}")
        output = ROOT / check["output"]
        if not output.exists() or not output.read_text(encoding="utf-8").strip():
            fail(f"check output missing or empty: {check_id} -> {output}")

    if pending_hashes:
        message = "missing SHA-256 for external evidence: " + ", ".join(pending_hashes)
        if not args.allow_missing_external_hash:
            fail(message)
        print(f"BLOCKED: {message}")

    print(f"audit evidence valid: {len(ids)} evidence entries; {len(checks)} command checks")


if __name__ == "__main__":
    main()
