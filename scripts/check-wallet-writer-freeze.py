#!/usr/bin/env python3
"""Validate the pre-cutover wallet writer-freeze manifest."""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path
from typing import Any

import yaml


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml"
REQUIRED_BACKENDS = {"items", "world", "hub"}
REQUIRED_PROVIDER = {
    "selected_provider": "MCMMORPGCentralWalletVaultProvider",
    "provider_contract_version": 2,
    "write_mode": "context_required",
    "generic_write_failure_code": "WRITE_CONTEXT_REQUIRED",
}
CAPABILITY_ID = re.compile(r"^GOLD-(?:WRITER|CAP)-\d{3}$")
MATRIX_ROW = re.compile(r"^\| (GOLD-(?:WRITER|CAP)-\d{3}) \|", re.MULTILINE)
SHA256 = re.compile(r"^[0-9a-f]{64}$")
CLASSIFICATIONS = {
    "ACTIVE_CONFIGURED",
    "SOURCE_CONFIRMED_DISABLED",
    "DORMANT_CONFIGURED",
    "POTENTIAL_UNVERIFIED",
    "STALE_DOCUMENTATION",
}
BINARY_ORIGINS = {"vendor", "first_party", "configuration_only"}
EXPOSURES = {
    "pending",
    "disabled",
    "permission_denied",
    "context_required",
    "migrated_first_party",
    "not_gold_proven",
}
FREEZE_STATUSES = {"PENDING", "VERIFIED"}
PROOF_STATUSES = {"MISSING", "PENDING", "VERIFIED"}
REQUIRED_PROOFS = {
    "exact_jar_scan",
    "effective_permission",
    "provider_identity",
    "focused_mutation_rejection",
}


def fail(message: str) -> None:
    print(f"wallet writer-freeze check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_yaml(path: Path) -> dict[str, Any]:
    try:
        value = yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as error:
        fail(f"cannot parse {path}: {error}")
    if not isinstance(value, dict):
        fail(f"{path} must contain a mapping")
    return value


def require_mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def require_list(value: Any, label: str) -> list[Any]:
    if not isinstance(value, list):
        fail(f"{label} must be a list")
    return value


def require_string(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def local_path(value: Any, label: str) -> Path:
    relative = Path(require_string(value, label))
    if relative.is_absolute():
        fail(f"{label} must be repository-relative")
    resolved = (ROOT / relative).resolve()
    try:
        resolved.relative_to(ROOT.resolve())
    except ValueError:
        fail(f"{label} escapes the repository root")
    if not resolved.exists():
        fail(f"{label} does not exist: {relative}")
    return resolved


def validate_evidence_ref(value: Any, label: str, *, required: bool) -> None:
    if value is None and not required:
        return
    local_path(value, label)


def extract_matrix_ids(text: str) -> set[str]:
    return set(MATRIX_ROW.findall(text))


def plugin_versions(path: Path) -> dict[str, str]:
    manifest = load_yaml(path)
    plugins = require_list(manifest.get("plugins"), "plugin manifest.plugins")
    result: dict[str, str] = {}
    for index, plugin in enumerate(plugins, start=1):
        row = require_mapping(plugin, f"plugin manifest entry {index}")
        name = require_string(row.get("name"), f"plugin manifest entry {index}.name")
        raw_version = row.get("current_version")
        if raw_version is None or isinstance(raw_version, (bool, dict, list)):
            fail(f"plugin manifest entry {index}.current_version must be a scalar")
        version = str(raw_version)
        if name in result:
            fail(f"duplicate plugin manifest name: {name}")
        result[name] = version
    return result


def validate_structure(data: dict[str, Any], manifest_path: Path) -> tuple[int, int]:
    if data.get("schema_version") != 1:
        fail("manifest must use schema_version 1")
    require_string(data.get("manifest_id"), "manifest_id")
    require_string(data.get("snapshot_at"), "snapshot_at")
    if data.get("readiness") not in {"BLOCKED", "READY"}:
        fail("readiness must be BLOCKED or READY")

    sources = require_mapping(data.get("sources"), "sources")
    matrix_source = require_mapping(sources.get("capability_matrix"), "sources.capability_matrix")
    matrix_path = local_path(matrix_source.get("path"), "sources.capability_matrix.path")
    expected_matrix_hash = require_string(
        matrix_source.get("sha256"), "sources.capability_matrix.sha256"
    )
    if not SHA256.fullmatch(expected_matrix_hash):
        fail("sources.capability_matrix.sha256 must be lowercase SHA-256")
    actual_matrix_hash = hashlib.sha256(matrix_path.read_bytes()).hexdigest()
    if expected_matrix_hash != actual_matrix_hash:
        fail(
            "capability matrix SHA-256 mismatch: "
            f"expected {expected_matrix_hash}, got {actual_matrix_hash}"
        )
    matrix_ids = extract_matrix_ids(matrix_path.read_text(encoding="utf-8"))
    if not matrix_ids:
        fail("capability matrix does not contain machine-readable capability rows")

    contract_source = require_mapping(sources.get("wallet_contract"), "sources.wallet_contract")
    contract_path = local_path(contract_source.get("path"), "sources.wallet_contract.path")
    contract = load_yaml(contract_path)
    if contract_source.get("contract_id") != contract.get("contract_id"):
        fail("wallet contract ID drift")
    if contract_source.get("contract_version") != contract.get("contract_version"):
        fail("wallet contract version drift")

    plugin_source = require_mapping(sources.get("plugin_manifest"), "sources.plugin_manifest")
    plugin_path = local_path(plugin_source.get("path"), "sources.plugin_manifest.path")
    versions = plugin_versions(plugin_path)

    backends = require_list(data.get("required_backend_set"), "required_backend_set")
    if len(backends) != len(set(backends)) or set(backends) != REQUIRED_BACKENDS:
        fail("required_backend_set must contain exactly items, world, and hub")

    provider = require_mapping(data.get("provider_requirement"), "provider_requirement")
    if provider != REQUIRED_PROVIDER:
        fail("provider_requirement drifted from central wallet contract v2")

    capabilities = require_list(data.get("capabilities"), "capabilities")
    if not capabilities:
        fail("capabilities must not be empty")
    catalog: dict[str, dict[str, Any]] = {}
    for index, capability_value in enumerate(capabilities, start=1):
        capability = require_mapping(capability_value, f"capability {index}")
        capability_id = require_string(
            capability.get("capability_id"), f"capability {index}.capability_id"
        )
        if not CAPABILITY_ID.fullmatch(capability_id):
            fail(f"invalid capability ID: {capability_id}")
        if capability_id in catalog:
            fail(f"duplicate capability ID: {capability_id}")
        if capability.get("classification") not in CLASSIFICATIONS:
            fail(f"unknown classification for {capability_id}")
        if capability.get("binary_origin") not in BINARY_ORIGINS:
            fail(f"unknown binary_origin for {capability_id}")
        plugin_name = require_string(
            capability.get("plugin_name"), f"{capability_id}.plugin_name"
        )
        plugin_version = require_string(
            capability.get("plugin_version"), f"{capability_id}.plugin_version"
        )
        if versions.get(plugin_name) != plugin_version:
            fail(
                f"plugin version drift for {capability_id}: "
                f"manifest has {versions.get(plugin_name)!r}, freeze catalog has {plugin_version!r}"
            )
        applicable = require_list(
            capability.get("applicable_backends"), f"{capability_id}.applicable_backends"
        )
        if not applicable or len(applicable) != len(set(applicable)):
            fail(f"{capability_id}.applicable_backends must be unique and non-empty")
        if not set(applicable) <= REQUIRED_BACKENDS:
            fail(f"{capability_id} references an unknown backend")
        require_string(capability.get("mutation_route"), f"{capability_id}.mutation_route")
        require_string(capability.get("observed_state"), f"{capability_id}.observed_state")
        require_string(
            capability.get("required_cutover_state"), f"{capability_id}.required_cutover_state"
        )
        if not isinstance(capability.get("permission_only_freeze_allowed"), bool):
            fail(f"{capability_id}.permission_only_freeze_allowed must be boolean")
        catalog[capability_id] = capability

    if set(catalog) != matrix_ids:
        missing = sorted(matrix_ids - set(catalog))
        extra = sorted(set(catalog) - matrix_ids)
        fail(
            "capability catalog does not exactly cover matrix IDs: "
            f"missing={missing}, extra={extra}"
        )

    records = require_list(data.get("freeze_records"), "freeze_records")
    expected_tuples = {
        (backend, capability_id)
        for capability_id, capability in catalog.items()
        for backend in capability["applicable_backends"]
    }
    actual_tuples: set[tuple[str, str]] = set()
    for index, record_value in enumerate(records, start=1):
        record = require_mapping(record_value, f"freeze record {index}")
        backend = require_string(record.get("backend_id"), f"freeze record {index}.backend_id")
        capability_id = require_string(
            record.get("capability_id"), f"freeze record {index}.capability_id"
        )
        key = (backend, capability_id)
        if key in actual_tuples:
            fail(f"duplicate freeze tuple: {backend} {capability_id}")
        actual_tuples.add(key)
        capability = catalog.get(capability_id)
        if capability is None:
            fail(f"freeze record references unknown capability: {capability_id}")
        if backend not in capability["applicable_backends"]:
            fail(f"freeze record backend is not applicable: {backend} {capability_id}")
        for field in ("plugin_name", "plugin_version", "mutation_route"):
            if record.get(field) != capability.get(field):
                fail(f"freeze record {backend} {capability_id} has mismatched {field}")
        if record.get("effective_exposure") not in EXPOSURES:
            fail(f"freeze record {backend} {capability_id} has invalid effective_exposure")
        if record.get("freeze_status") not in FREEZE_STATUSES:
            fail(f"freeze record {backend} {capability_id} has invalid freeze_status")
        digest = record.get("vendor_jar_sha256")
        if digest is not None and (not isinstance(digest, str) or not SHA256.fullmatch(digest)):
            fail(f"freeze record {backend} {capability_id} has invalid vendor_jar_sha256")
        evidence_refs = require_list(
            record.get("evidence_refs"), f"freeze record {backend} {capability_id}.evidence_refs"
        )
        for evidence_index, evidence_ref in enumerate(evidence_refs, start=1):
            validate_evidence_ref(
                evidence_ref,
                f"freeze record {backend} {capability_id}.evidence_refs[{evidence_index}]",
                required=True,
            )
        proofs = require_mapping(
            record.get("proofs"), f"freeze record {backend} {capability_id}.proofs"
        )
        if set(proofs) != REQUIRED_PROOFS:
            fail(f"freeze record {backend} {capability_id} must contain all proof classes")
        for proof_name, proof_value in proofs.items():
            proof = require_mapping(
                proof_value, f"freeze record {backend} {capability_id}.proofs.{proof_name}"
            )
            if proof.get("status") not in PROOF_STATUSES:
                fail(f"freeze record {backend} {capability_id}.{proof_name} has invalid status")
            validate_evidence_ref(
                proof.get("evidence_ref"),
                f"freeze record {backend} {capability_id}.{proof_name}.evidence_ref",
                required=proof.get("status") == "VERIFIED",
            )

    if actual_tuples != expected_tuples:
        missing = sorted(expected_tuples - actual_tuples)
        extra = sorted(actual_tuples - expected_tuples)
        fail(f"freeze tuple coverage mismatch: missing={missing}, extra={extra}")

    acknowledgements = require_list(
        data.get("backend_acknowledgements"), "backend_acknowledgements"
    )
    ack_backends: set[str] = set()
    for index, ack_value in enumerate(acknowledgements, start=1):
        ack = require_mapping(ack_value, f"backend acknowledgement {index}")
        backend = require_string(ack.get("backend_id"), f"backend acknowledgement {index}.backend_id")
        if backend in ack_backends:
            fail(f"duplicate backend acknowledgement: {backend}")
        ack_backends.add(backend)
        if ack.get("status") not in {"PENDING", "VERIFIED"}:
            fail(f"backend acknowledgement {backend} has invalid status")
        validate_evidence_ref(
            ack.get("evidence_ref"),
            f"backend acknowledgement {backend}.evidence_ref",
            required=ack.get("status") == "VERIFIED",
        )
    if ack_backends != REQUIRED_BACKENDS:
        fail("backend acknowledgements must cover exactly items, world, and hub")

    legacy = require_mapping(data.get("legacy_snapshot"), "legacy_snapshot")
    if legacy.get("status") not in {"MISSING", "VERIFIED"}:
        fail("legacy_snapshot.status must be MISSING or VERIFIED")
    validate_evidence_ref(
        legacy.get("evidence_ref"),
        "legacy_snapshot.evidence_ref",
        required=legacy.get("status") == "VERIFIED",
    )

    return len(catalog), len(records)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    args = parser.parse_args()

    manifest_path = args.manifest.resolve()
    data = load_yaml(manifest_path)
    capability_count, record_count = validate_structure(data, manifest_path)

    print(f"STRUCTURE PASS: {capability_count} capabilities; {record_count} freeze records")
    if data["readiness"] == "READY":
        print("READINESS READY")
    else:
        print("READINESS BLOCKED: readiness evidence remains pending")


if __name__ == "__main__":
    main()
