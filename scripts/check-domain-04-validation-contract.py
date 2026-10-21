#!/usr/bin/env python3
"""Validate the Domain 04 validation contract against its accepted source documents.

The contract enumerates the closed Domain 04 invariant list, the cross-domain
checks tying Domain 04 to Domains 02, 03, 05, and 14, and the runtime evidence
gates required before `runtime_verified`.

This checker proves that the contract stays in sync with the Domain 04 documents
it claims to mirror, and that it never lets static parsing grant
`runtime_verified`. It proves nothing about runtime behaviour.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_TARGET = ROOT / "docs/design/classes/domain-04-validation-contract.yaml"

SKILL_CONTRACT = ROOT / "docs/design/classes/skill-library-contract.yaml"
ALIGNMENT_DOC = ROOT / "docs/design/classes/domain-02-03-alignment.md"
CLASS_DOC = ROOT / "docs/design/classes/class-framework.md"
LOADOUT_DOC = ROOT / "docs/design/classes/combat-loadout.md"
WEAPON_DOC = ROOT / "docs/design/classes/weapon-schools.md"
RACE_DOC = ROOT / "docs/design/classes/race-framework.md"
SCOPE_REGISTRY = ROOT / "docs/contracts/character-scope-registry.yaml"

REQUIRED_TOP_LEVEL_FIELDS = [
    "title",
    "status",
    "updated_at",
    "contract",
    "cls_gate",
    "source_documents",
    "vocabularies",
    "lifecycle_policy",
    "mirrors",
    "invariants",
    "runtime_evidence_gates",
    "open_cross_domain_gaps",
    "semantic_validator_contract",
    "record_validator_contract",
]

EXPECTED_ENFORCEMENT = ["static", "runtime", "hybrid"]
EXPECTED_LIFECYCLE = ["draft", "validated", "runtime_verified", "released"]
EXPECTED_GROUPS = [
    "class",
    "skill",
    "loadout",
    "progression_respec",
    "weapon_school",
    "race",
    "cross_domain",
]
EXPECTED_EVIDENCE_CHANNELS = ["rcon", "mineflayer", "plugin_api", "logs", "real_client"]
EXPECTED_BACKENDS = ["dev_lease_backend"]
EXPECTED_FORBIDDEN_BACKENDS = ["staging", "world", "items", "hub"]

REQUIRED_RUNTIME_TOPICS = [
    "cost",
    "cooldown",
    "reconnect",
    "death_respawn",
    "weapon_swap",
    "party",
    "pvp",
    "d03_consumer_binding",
    "restart_recovery",
]
REQUIRED_CROSS_DOMAINS = ["02", "03", "05", "14"]

GROUP_ID_PREFIX = {
    "class": "D04-INV-CLASS-",
    "skill": "D04-INV-SKILL-",
    "loadout": "D04-INV-LOADOUT-",
    "progression_respec": "D04-INV-PROG-",
    "weapon_school": "D04-INV-WEAPON-",
    "race": "D04-INV-RACE-",
    "cross_domain": "D04-INV-CROSS-",
}

# Groups whose covered requirements must exactly equal the requirement list in
# the source document. Anything else is drift in one direction or the other.
EXACT_COVERAGE_GROUPS = ["class", "skill", "loadout", "weapon_school", "race"]
# Progression invariants must cover at least the class framework progression
# references, and may add respec and permanence invariants on top.
SUBSET_COVERAGE_GROUPS = ["progression_respec"]

KNOWN_GAP_DETECTIONS = [
    "character_scope_registry_missing_domain_04_keys",
    "manual",
]

INVARIANT_ID_PATTERN = re.compile(r"^D04-INV-[A-Z]+-\d{3}$")
GATE_ID_PATTERN = re.compile(r"^D04-RG-[A-Z0-9-]+$")
GAP_ID_PATTERN = re.compile(r"^D04-GAP-[A-Z0-9-]+$")
TABLE_SEPARATOR_PATTERN = re.compile(r"^:?-{3,}:?$")
BACKTICKED_PATTERN = re.compile(r"^`(.+)`$")

EXPECTED_D03_BINDING_ITEM_FIELDS = {
    "mechanic_id": "namespaced_id",
    "mechanic_contract_version": "positive_integer",
    "required_readiness": "RUNTIME_VERIFIED",
    "mechanic_contract_ref": "immutable_ref",
    "gate_transition_envelope_ref": "path_at_sha256",
    "contract_bundle_digest": "sha256",
    "build_digest": "sha256",
}
EXPECTED_D03_LINEAGE_FIELDS = [
    "mechanic_id",
    "mechanic_contract_version",
    "mechanic_contract_ref",
    "gate_transition_envelope_ref",
    "contract_bundle_digest",
    "build_digest",
]
EXPECTED_D03_FALLBACK_SOURCES = [
    "formula_id",
    "coefficient_set_id",
    "effect_ids",
    "mechanic_tags",
    "control_refs",
    "implementation_provider",
    "implementation_ref",
    "provider_skill_id",
]
EXPECTED_D03_FAILURE_CODES = {
    "missing": "D03_BINDING_MISSING",
    "duplicate": "D03_BINDING_DUPLICATE",
    "stale": "D03_BINDING_STALE",
    "digest_mismatch": "D03_BINDING_DIGEST_MISMATCH",
    "downgraded": "D03_BINDING_READINESS_DOWNGRADED",
    "revoked": "D03_BINDING_REVOKED",
    "fallback_attempt": "D03_BINDING_FALLBACK_FORBIDDEN",
}
EXPECTED_D03_OUTCOME_SEMANTICS = {
    "admission": "REJECT_MECHANIC",
    "lifecycle_ceiling": "validated",
    "executable_provider": "DISABLED",
    "fallback": "FORBIDDEN",
}
EXPECTED_D03_GATE_PROVES = (
    "an executable skill resolves one non-revoked RUNTIME_VERIFIED Domain 03 "
    "binding per mechanic with matching contract, gate-envelope, bundle, and "
    "build digests; missing, stale, mismatched, downgraded, and revoked fixtures "
    "are rejected without local formula, effect, tag, or provider fallback"
)
EXPECTED_D03_BINDING_RULES = [
    "every catalogued combat mechanic executable by the skill has exactly one binding for its mechanic_id",
    "formula_id, coefficient_set_id, effect_ids, mechanic_tags, control refs, and provider refs do not substitute for a binding",
    "draft and design-only records may use an empty list only while implementation_provider is none and lifecycle status is not above validated",
    "missing, duplicate, stale, digest-mismatched, downgraded, or revoked binding suppresses or rejects that mechanic fail-closed",
    "mechanic_contract_ref and gate_transition_envelope_ref use immutable path@sha256 identity",
]
EXPECTED_D03_FAILURE_PRECEDENCE = [
    "fallback_attempt",
    "duplicate",
    "missing",
    "stale",
    "digest_mismatch",
    "downgraded",
    "revoked",
]
EXPECTED_SKILL_REQUIRED_FIELDS = [
    "skill_id",
    "schema_version",
    "ownership",
    "classification",
    "targeting",
    "requirements",
    "execution",
    "scaling",
    "control",
    "d03_consumer_bindings",
    "runtime",
    "lifecycle",
]
EXPECTED_SKILL_SCHEMA_FIELD_KEYS = {
    "skill_id",
    "schema_version",
    "display",
    "ownership",
    "classification",
    "targeting",
    "requirements",
    "execution",
    "scaling",
    "control",
    "d03_consumer_bindings",
    "runtime",
    "lifecycle",
}
EXPECTED_D03_GATE_KEYS = {
    "id",
    "topic",
    "proves",
    "evidence_contract",
    "evidence_channel",
    "backend",
    "lease_required",
    "covers_invariants",
}


class StrictLoader(yaml.SafeLoader):
    pass


def construct_unique_mapping(loader, node, deep=False):
    mapping = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in mapping:
            raise yaml.constructor.ConstructorError(
                "while constructing a mapping",
                node.start_mark,
                f"found duplicate key {key!r}",
                key_node.start_mark,
            )
        mapping[key] = loader.construct_object(value_node, deep=deep)
    return mapping


StrictLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
    construct_unique_mapping,
)


def fail(message: str) -> None:
    print(f"domain 04 validation contract check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def require_mapping(value, label: str) -> dict:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def require_list(value, label: str) -> list:
    if not isinstance(value, list) or not value:
        fail(f"{label} must be a non-empty list")
    return value


def require_non_empty_string(value, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value.strip()


def require_string_list(value, label: str) -> list:
    items = require_list(value, label)
    return [require_non_empty_string(item, f"{label}[{index}]") for index, item in enumerate(items)]


def require_exact_list(value, expected: list, label: str) -> None:
    actual = require_string_list(value, label)
    if actual != expected:
        fail(f"{label} must be exactly {expected}, got {actual}")


def load_yaml(path: Path, label: str):
    if not path.is_file():
        fail(f"{label} not found at {path}")
    try:
        return yaml.load(path.read_text(encoding="utf-8"), Loader=StrictLoader)
    except (UnicodeError, yaml.YAMLError) as error:
        fail(f"{label} is not valid strict UTF-8 YAML: {error}")


def read_document(path: Path, label: str) -> list:
    if not path.is_file():
        fail(f"{label} not found at {path}")
    return path.read_text(encoding="utf-8").splitlines()


def section_lines(lines: list, heading: str, label: str) -> list:
    start = None
    for index, line in enumerate(lines):
        if line.strip() == heading:
            start = index + 1
            break
    if start is None:
        fail(f"{label} is missing section {heading!r}")
    collected = []
    for line in lines[start:]:
        if line.startswith("## "):
            break
        collected.append(line)
    return collected


def normalize_requirement(value: str) -> str:
    text = value.replace("`", "")
    text = " ".join(text.split())
    return text.rstrip(";.").strip()


def section_bullets(lines: list) -> list:
    items = []
    current = None
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("- "):
            if current is not None:
                items.append(current)
            current = stripped[2:]
        elif current is not None:
            if not stripped:
                items.append(current)
                current = None
            elif line[:1].isspace():
                current = f"{current} {stripped}"
            else:
                items.append(current)
                current = None
    if current is not None:
        items.append(current)
    return [normalize_requirement(item) for item in items]


def section_table_rows(lines: list) -> list:
    rows = []
    for line in lines:
        stripped = line.strip()
        if not stripped.startswith("|"):
            continue
        cells = [cell.strip() for cell in stripped.strip("|").split("|")]
        if cells and all(TABLE_SEPARATOR_PATTERN.match(cell) for cell in cells):
            continue
        rows.append(cells)
    # The first surviving row is the table header.
    return rows[1:]


def first_column(lines: list, label: str, backticked: bool) -> list:
    values = []
    for cells in section_table_rows(lines):
        if not cells:
            fail(f"{label} has an empty table row")
        cell = cells[0]
        if backticked:
            match = BACKTICKED_PATTERN.match(cell)
            if not match:
                fail(f"{label} expected a backticked identifier in the first column, got {cell!r}")
            values.append(match.group(1))
        else:
            values.append(normalize_requirement(cell))
    if not values:
        fail(f"{label} produced no table rows")
    return values


def check_mirror(mirrors: dict, name: str, expected: list, label: str) -> list:
    entry = require_mapping(mirrors.get(name), f"mirrors.{name}")
    require_non_empty_string(entry.get("source_document"), f"mirrors.{name}.source_document")
    require_non_empty_string(entry.get("section"), f"mirrors.{name}.section")
    declared = require_string_list(entry.get("values"), f"mirrors.{name}.values")
    if declared != expected:
        fail(
            f"mirrors.{name}.values drifted from {label}: "
            f"declared {declared}, source has {expected}"
        )
    return declared


def strictly_equal(actual, expected) -> bool:
    """Compare YAML values without Python's bool/int coercion."""

    if type(actual) is not type(expected):
        return False
    if isinstance(expected, dict):
        return set(actual) == set(expected) and all(
            strictly_equal(actual[key], expected[key]) for key in expected
        )
    if isinstance(expected, list):
        return len(actual) == len(expected) and all(
            strictly_equal(actual_item, expected_item)
            for actual_item, expected_item in zip(actual, expected)
        )
    return actual == expected


def require_exact_mapping(value, expected: dict, label: str) -> dict:
    actual = require_mapping(value, label)
    if not strictly_equal(actual, expected):
        fail(f"{label} must be exactly {expected}, got {actual}")
    return actual


def validate_d03_consumer_binding_contract(skill_contract: dict) -> dict:
    """Validate the executable shape, coverage, lifecycle and failure contract.

    This intentionally reads structured schema and policy data. Release-gate
    prose is not sufficient evidence for the Domain 03 consumer binding.
    """

    record_schema = require_mapping(
        skill_contract.get("skill_record_schema"),
        "skill-library-contract.yaml skill_record_schema",
    )
    if set(record_schema) != {"required_fields", "fields"}:
        fail("skill-library-contract.yaml skill_record_schema keys must be exactly required_fields and fields")
    require_exact_list(
        record_schema.get("required_fields"),
        EXPECTED_SKILL_REQUIRED_FIELDS,
        "skill-library-contract.yaml skill_record_schema.required_fields",
    )

    fields = require_mapping(
        record_schema.get("fields"),
        "skill-library-contract.yaml skill_record_schema.fields",
    )
    if set(fields) != EXPECTED_SKILL_SCHEMA_FIELD_KEYS:
        fail(
            "skill-library-contract.yaml skill_record_schema.fields keys must be exactly "
            f"{sorted(EXPECTED_SKILL_SCHEMA_FIELD_KEYS)}"
        )
    classification = require_mapping(
        fields.get("classification"),
        "skill-library-contract.yaml skill_record_schema.fields.classification",
    )
    classification_fields = require_mapping(
        classification.get("fields"),
        "skill-library-contract.yaml classification.fields",
    )
    if classification_fields.get("catalogued_mechanic_ids") != "list[namespaced_id]":
        fail("classification.catalogued_mechanic_ids must be list[namespaced_id]")

    require_exact_mapping(
        fields.get("d03_consumer_bindings"),
        {
            "type": "list[d03_mechanic_consumer_binding]",
            "item_fields": EXPECTED_D03_BINDING_ITEM_FIELDS,
            "rules": EXPECTED_D03_BINDING_RULES,
        },
        "skill-library-contract.yaml skill_record_schema.fields.d03_consumer_bindings",
    )

    runtime = require_mapping(fields.get("runtime"), "skill-library-contract.yaml runtime")
    runtime_fields = require_mapping(runtime.get("fields"), "skill-library-contract.yaml runtime.fields")
    require_exact_mapping(
        runtime_fields,
        {
            "implementation_provider": "mmocore | mythicmobs | custom | none",
            "implementation_ref": "string | null",
            "provider_skill_id": "string | null",
        },
        "skill-library-contract.yaml runtime.fields",
    )
    lifecycle = require_mapping(fields.get("lifecycle"), "skill-library-contract.yaml lifecycle")
    lifecycle_fields = require_mapping(
        lifecycle.get("fields"), "skill-library-contract.yaml lifecycle.fields"
    )
    if lifecycle_fields.get("status") != "draft | validated | runtime_verified | released":
        fail("skill lifecycle.status must retain the closed four-state lifecycle")

    expected_policy = {
        "schema_version": 1,
        "binding_field": "d03_consumer_bindings",
        "reachable_mechanic_source_field": "classification.catalogued_mechanic_ids",
        "coverage_mode": "EXACTLY_ONE_PER_REACHABLE_MECHANIC",
        "required_readiness": "RUNTIME_VERIFIED",
        "authority_snapshot_request_field": "d03_authority_snapshot_ref",
        "authority_registry_ref": (
            "docs/contracts/domain-03-combat-mechanic-authority-registry.yaml@"
            "5c0dfed489c5a3fc4b415e0508102ec501d8026fc0c387f72019d9b6632d8cf8"
        ),
        "authority_contract_ref": (
            "/home/przemek/.traycer/epics/b638c9da-033f-4f61-8587-60459f79e9b2/"
            "artifacts/domain-03-combat-core-revision/index.md@"
            "33b076efb539dc5b0007c372d939ed447be6216116f975d95b20c76bf42e32e4"
        ),
        "authority_content_verification": "registry_snapshot_and_contract_sha256",
        "authority_scope_mode": "EXACT_REACHABLE_SET",
        "required_item_fields": list(EXPECTED_D03_BINDING_ITEM_FIELDS),
        "immutable_lineage_fields": EXPECTED_D03_LINEAGE_FIELDS,
        "planning_constraint": {
            "allowed_lifecycle_statuses": ["draft", "validated"],
            "empty_bindings_allowed": True,
            "implementation_provider": "none",
            "implementation_ref": None,
            "provider_skill_id": None,
        },
        "executable_constraint": {
            "allowed_lifecycle_statuses": ["runtime_verified", "released"],
            "empty_bindings_allowed": False,
            "forbidden_implementation_providers": ["none"],
            "implementation_ref_required": True,
            "provider_skill_id_required": True,
            "all_reachable_mechanics_must_resolve": True,
        },
        "forbidden_fallback_sources": EXPECTED_D03_FALLBACK_SOURCES,
        "failure_precedence": EXPECTED_D03_FAILURE_PRECEDENCE,
        "failure_outcomes": {
            case_id: {"reason_code": reason_code, **EXPECTED_D03_OUTCOME_SEMANTICS}
            for case_id, reason_code in EXPECTED_D03_FAILURE_CODES.items()
        },
    }
    policy = require_exact_mapping(
        skill_contract.get("consumer_binding_policy"),
        expected_policy,
        "skill-library-contract.yaml consumer_binding_policy",
    )
    return policy


def main() -> None:
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_TARGET
    if not target.is_absolute():
        target = (ROOT / target).resolve()
    skill_contract_path = Path(sys.argv[2]) if len(sys.argv) > 2 else SKILL_CONTRACT
    if not skill_contract_path.is_absolute():
        skill_contract_path = (ROOT / skill_contract_path).resolve()
    if len(sys.argv) > 3:
        fail("usage: check-domain-04-validation-contract.py [validation-contract] [skill-contract]")

    document = load_yaml(target, "domain 04 validation contract")
    contract = require_mapping(document, "contract root")

    for field in REQUIRED_TOP_LEVEL_FIELDS:
        if field not in contract:
            fail(f"missing required top-level field {field}")

    meta = require_mapping(contract.get("contract"), "contract")
    require_non_empty_string(meta.get("id"), "contract.id")
    if meta.get("schema_version") != 1:
        fail("contract.schema_version must be 1")
    if meta.get("semantic_owner_domain") != "04":
        fail('contract.semantic_owner_domain must be "04"')

    cls_gate = require_mapping(contract.get("cls_gate"), "cls_gate")
    if cls_gate.get("concrete_class_definitions_in_scope") is not False:
        fail("cls_gate.concrete_class_definitions_in_scope must be false; this contract must not open CLS-* work")

    # ------------------------------------------------------------ vocabularies
    vocabularies = require_mapping(contract.get("vocabularies"), "vocabularies")
    require_exact_list(vocabularies.get("enforcement"), EXPECTED_ENFORCEMENT, "vocabularies.enforcement")
    require_exact_list(vocabularies.get("lifecycle_status"), EXPECTED_LIFECYCLE, "vocabularies.lifecycle_status")
    require_exact_list(vocabularies.get("invariant_groups"), EXPECTED_GROUPS, "vocabularies.invariant_groups")
    require_exact_list(
        vocabularies.get("evidence_channel"), EXPECTED_EVIDENCE_CHANNELS, "vocabularies.evidence_channel"
    )
    require_exact_list(vocabularies.get("backend"), EXPECTED_BACKENDS, "vocabularies.backend")
    require_exact_list(
        vocabularies.get("forbidden_backends"), EXPECTED_FORBIDDEN_BACKENDS, "vocabularies.forbidden_backends"
    )

    lifecycle_policy = require_mapping(contract.get("lifecycle_policy"), "lifecycle_policy")
    if lifecycle_policy.get("static_validation_max_status") != "validated":
        fail("lifecycle_policy.static_validation_max_status must be validated")
    if lifecycle_policy.get("runtime_verified_requires_runtime_evidence") is not True:
        fail("lifecycle_policy.runtime_verified_requires_runtime_evidence must be true")
    require_string_list(lifecycle_policy.get("rules"), "lifecycle_policy.rules")

    require_exact_mapping(
        contract.get("record_validator_contract"),
        {
            "command": "python3 scripts/check-domain-04-skill-record.py <validation-request.yaml>",
            "request_kind": "domain_04_skill_record_validation",
            "authority_input": "d03_authority_snapshot_ref",
            "authority_registry_ref": (
                "docs/contracts/domain-03-combat-mechanic-authority-registry.yaml@"
                "5c0dfed489c5a3fc4b415e0508102ec501d8026fc0c387f72019d9b6632d8cf8"
            ),
            "authority_contract_ref": (
                "/home/przemek/.traycer/epics/b638c9da-033f-4f61-8587-60459f79e9b2/"
                "artifacts/domain-03-combat-core-revision/index.md@"
                "33b076efb539dc5b0007c372d939ed447be6216116f975d95b20c76bf42e32e4"
            ),
            "authority_content_verification": "registry_snapshot_and_contract_sha256",
            "authority_scope_mode": "EXACT_REACHABLE_SET",
            "output_format": "json",
            "exactly_one_output_object": True,
            "accepted_exit_code": 0,
            "rejected_exit_code": 1,
            "static_validation_only": True,
        },
        "record_validator_contract",
    )

    # -------------------------------------------------------- source documents
    source_documents = require_mapping(contract.get("source_documents"), "source_documents")
    declared_sources = set()
    for group, values in source_documents.items():
        paths = require_string_list(values, f"source_documents.{group}")
        for relative in paths:
            if not (ROOT / relative).is_file():
                fail(f"source_documents.{group} references missing file {relative}")
            declared_sources.add(relative)

    # ---------------------------------------------------------------- mirrors
    mirrors = require_mapping(contract.get("mirrors"), "mirrors")

    alignment_lines = read_document(ALIGNMENT_DOC, "domain-02-03-alignment.md")
    owned_identifiers = first_column(
        section_lines(alignment_lines, "## Ownership", "domain-02-03-alignment.md"),
        "domain-02-03-alignment.md ## Ownership",
        backticked=True,
    )
    persisted_keys = first_column(
        section_lines(alignment_lines, "## Domain 02 Persistence Fields", "domain-02-03-alignment.md"),
        "domain-02-03-alignment.md ## Domain 02 Persistence Fields",
        backticked=True,
    )
    upstream_contracts = first_column(
        section_lines(alignment_lines, "## Domain 03 Upstream Gates", "domain-02-03-alignment.md"),
        "domain-02-03-alignment.md ## Domain 03 Upstream Gates",
        backticked=True,
    )

    class_lines = read_document(CLASS_DOC, "class-framework.md")
    mandatory_components = first_column(
        section_lines(class_lines, "## Mandatory Class Components", "class-framework.md"),
        "class-framework.md ## Mandatory Class Components",
        backticked=False,
    )
    progression_references = section_bullets(
        section_lines(class_lines, "## Progression Contract", "class-framework.md")
    )
    if not progression_references:
        fail("class-framework.md ## Progression Contract has no required reference bullets")

    check_mirror(mirrors, "domain_04_owned_identifiers", owned_identifiers, "domain-02-03-alignment.md ## Ownership")
    check_mirror(
        mirrors,
        "domain_02_persisted_keys",
        persisted_keys,
        "domain-02-03-alignment.md ## Domain 02 Persistence Fields",
    )
    check_mirror(
        mirrors,
        "domain_03_upstream_contracts",
        upstream_contracts,
        "domain-02-03-alignment.md ## Domain 03 Upstream Gates",
    )
    check_mirror(
        mirrors,
        "class_mandatory_components",
        mandatory_components,
        "class-framework.md ## Mandatory Class Components",
    )
    check_mirror(
        mirrors,
        "class_progression_required_references",
        progression_references,
        "class-framework.md ## Progression Contract",
    )

    loadout_lines = read_document(LOADOUT_DOC, "combat-loadout.md")
    slot_rows = section_table_rows(section_lines(loadout_lines, "## Slot Model", "combat-loadout.md"))
    source_slots = {}
    for cells in slot_rows:
        if len(cells) < 2:
            fail("combat-loadout.md ## Slot Model row must have at least two columns")
        match = BACKTICKED_PATTERN.match(cells[0])
        if not match:
            fail(f"combat-loadout.md ## Slot Model expected a backticked slot type, got {cells[0]!r}")
        try:
            source_slots[match.group(1)] = int(cells[1])
        except ValueError:
            fail(f"combat-loadout.md ## Slot Model slot {match.group(1)} has a non-integer count {cells[1]!r}")
    if not source_slots:
        fail("combat-loadout.md ## Slot Model produced no slots")

    slot_mirror = require_mapping(mirrors.get("loadout_slot_model"), "mirrors.loadout_slot_model")
    require_non_empty_string(slot_mirror.get("source_document"), "mirrors.loadout_slot_model.source_document")
    require_non_empty_string(slot_mirror.get("section"), "mirrors.loadout_slot_model.section")
    declared_slots = require_mapping(slot_mirror.get("slots"), "mirrors.loadout_slot_model.slots")
    if declared_slots != source_slots:
        fail(
            "mirrors.loadout_slot_model.slots drifted from combat-loadout.md ## Slot Model: "
            f"declared {declared_slots}, source has {source_slots}"
        )

    # ------------------------------------------------------- source coverage
    skill_contract = require_mapping(
        load_yaml(skill_contract_path, "skill library contract"),
        "skill library contract",
    )
    validate_d03_consumer_binding_contract(skill_contract)
    validation_policy = require_mapping(
        skill_contract.get("validation_policy"), "skill-library-contract.yaml validation_policy"
    )
    skill_requirements = [
        normalize_requirement(item)
        for item in require_string_list(
            validation_policy.get("minimum_release_gates"),
            "skill-library-contract.yaml validation_policy.minimum_release_gates",
        )
    ]

    loadout_requirements = section_bullets(
        section_lines(loadout_lines, "## Validation Requirements", "combat-loadout.md")
    )
    weapon_requirements = section_bullets(
        section_lines(
            read_document(WEAPON_DOC, "weapon-schools.md"), "## Validation Requirements", "weapon-schools.md"
        )
    )
    race_requirements = section_bullets(
        section_lines(read_document(RACE_DOC, "race-framework.md"), "## Validation Requirements", "race-framework.md")
    )

    for label, values in (
        ("combat-loadout.md", loadout_requirements),
        ("weapon-schools.md", weapon_requirements),
        ("race-framework.md", race_requirements),
    ):
        if not values:
            fail(f"{label} ## Validation Requirements has no bullets")

    expected_coverage = {
        "class": mandatory_components,
        "skill": skill_requirements,
        "loadout": loadout_requirements,
        "weapon_school": weapon_requirements,
        "race": race_requirements,
        "progression_respec": progression_references,
    }

    # ------------------------------------------------------------- invariants
    invariants = require_list(contract.get("invariants"), "invariants")
    seen_ids = set()
    by_group = {group: [] for group in EXPECTED_GROUPS}
    covered_requirements = {group: [] for group in EXPECTED_GROUPS}
    covered_domains = set()
    runtime_invariant_ids = set()
    gap_references = {}

    for index, raw in enumerate(invariants):
        entry = require_mapping(raw, f"invariants[{index}]")
        invariant_id = require_non_empty_string(entry.get("id"), f"invariants[{index}].id")
        if not INVARIANT_ID_PATTERN.match(invariant_id):
            fail(f"invariant id {invariant_id} must match D04-INV-<GROUP>-<NNN>")
        if invariant_id in seen_ids:
            fail(f"duplicate invariant id {invariant_id}")
        seen_ids.add(invariant_id)

        group = require_non_empty_string(entry.get("group"), f"{invariant_id}.group")
        if group not in EXPECTED_GROUPS:
            fail(f"{invariant_id}.group {group} is not in vocabularies.invariant_groups")
        if not invariant_id.startswith(GROUP_ID_PREFIX[group]):
            fail(f"{invariant_id} must start with {GROUP_ID_PREFIX[group]} for group {group}")
        by_group[group].append(invariant_id)

        require_non_empty_string(entry.get("statement"), f"{invariant_id}.statement")
        source_document = require_non_empty_string(entry.get("source_document"), f"{invariant_id}.source_document")
        if source_document not in declared_sources:
            fail(f"{invariant_id}.source_document {source_document} is not listed in source_documents")

        enforcement = require_non_empty_string(entry.get("enforcement"), f"{invariant_id}.enforcement")
        if enforcement not in EXPECTED_ENFORCEMENT:
            fail(f"{invariant_id}.enforcement {enforcement} is not in vocabularies.enforcement")
        max_status = require_non_empty_string(entry.get("max_status_granted"), f"{invariant_id}.max_status_granted")
        if max_status not in EXPECTED_LIFECYCLE:
            fail(f"{invariant_id}.max_status_granted {max_status} is not in vocabularies.lifecycle_status")
        if enforcement == "static" and max_status not in ("draft", "validated"):
            fail(
                f"{invariant_id} has enforcement static but grants {max_status}; "
                "static parsing must never grant runtime_verified or released"
            )
        if max_status == "released":
            fail(f"{invariant_id}.max_status_granted must not be released; release is outside this contract")
        if enforcement in ("runtime", "hybrid"):
            runtime_invariant_ids.add(invariant_id)

        requirement = entry.get("covers_source_requirement")
        if requirement is not None:
            covered_requirements[group].append(
                require_non_empty_string(requirement, f"{invariant_id}.covers_source_requirement")
            )

        if group == "cross_domain":
            domains = require_string_list(entry.get("related_domains"), f"{invariant_id}.related_domains")
            covered_domains.update(domains)
        elif entry.get("related_domains") is not None:
            fail(f"{invariant_id}.related_domains is only allowed on cross_domain invariants")

        blocked_by = entry.get("blocked_by_gap")
        if blocked_by is not None:
            gap_references[invariant_id] = require_non_empty_string(
                blocked_by, f"{invariant_id}.blocked_by_gap"
            )

    for group in EXPECTED_GROUPS:
        if not by_group[group]:
            fail(f"invariant group {group} has no invariants")

    for group in EXACT_COVERAGE_GROUPS:
        declared = sorted(covered_requirements[group])
        expected = sorted(expected_coverage[group])
        if declared != expected:
            missing = [item for item in expected if item not in declared]
            extra = [item for item in declared if item not in expected]
            fail(
                f"group {group} covers_source_requirement drifted from its source document; "
                f"missing {missing}, unexpected {extra}"
            )

    for group in SUBSET_COVERAGE_GROUPS:
        declared = set(covered_requirements[group])
        missing = [item for item in expected_coverage[group] if item not in declared]
        if missing:
            fail(f"group {group} does not cover required source requirements {missing}")

    missing_domains = [domain for domain in REQUIRED_CROSS_DOMAINS if domain not in covered_domains]
    if missing_domains:
        fail(f"cross_domain invariants must cover Domains {REQUIRED_CROSS_DOMAINS}; missing {missing_domains}")

    # --------------------------------------------- skill contract upstream refs
    skill_source = require_mapping(skill_contract.get("source_of_truth"), "skill-library-contract.yaml source_of_truth")
    skill_upstream = require_string_list(
        skill_source.get("upstream_required_domain_03_contracts"),
        "skill-library-contract.yaml source_of_truth.upstream_required_domain_03_contracts",
    )
    ungated = [item for item in skill_upstream if item not in upstream_contracts]
    if ungated:
        fail(
            "skill-library-contract.yaml references Domain 03 contracts that are not in the "
            f"domain-02-03-alignment.md upstream gate list: {ungated}"
        )

    # --------------------------------------------------- runtime evidence gates
    gates = require_list(contract.get("runtime_evidence_gates"), "runtime_evidence_gates")
    seen_gate_ids = set()
    seen_topics = []
    gate_covered_invariants = set()

    for index, raw in enumerate(gates):
        entry = require_mapping(raw, f"runtime_evidence_gates[{index}]")
        gate_id = require_non_empty_string(entry.get("id"), f"runtime_evidence_gates[{index}].id")
        if not GATE_ID_PATTERN.match(gate_id):
            fail(f"runtime evidence gate id {gate_id} must match D04-RG-<NAME>")
        if gate_id in seen_gate_ids:
            fail(f"duplicate runtime evidence gate id {gate_id}")
        seen_gate_ids.add(gate_id)

        topic = require_non_empty_string(entry.get("topic"), f"{gate_id}.topic")
        if topic in seen_topics:
            fail(f"duplicate runtime evidence topic {topic}")
        seen_topics.append(topic)

        proves = require_non_empty_string(entry.get("proves"), f"{gate_id}.proves")
        if topic == "d03_consumer_binding":
            if set(entry) != EXPECTED_D03_GATE_KEYS:
                fail(f"{gate_id} keys must be exactly {sorted(EXPECTED_D03_GATE_KEYS)}")
            if normalize_requirement(proves) != EXPECTED_D03_GATE_PROVES:
                fail(
                    f"{gate_id}.proves must state the complete executable Domain 03 "
                    "consumer-binding evidence claim"
                )
            evidence_contract = require_mapping(
                entry.get("evidence_contract"), f"{gate_id}.evidence_contract"
            )
            require_exact_mapping(
                evidence_contract,
                {
                    "reachable_mechanic_source_field": (
                        "classification.catalogued_mechanic_ids"
                    ),
                    "coverage_mode": "EXACTLY_ONE_PER_REACHABLE_MECHANIC",
                    "required_readiness": "RUNTIME_VERIFIED",
                    "immutable_lineage_fields": EXPECTED_D03_LINEAGE_FIELDS,
                    "lifecycle_provider_constraints": {
                        "planning_empty_bindings_max_status": "validated",
                        "planning_implementation_provider": "none",
                        "executable_empty_bindings_allowed": False,
                        "executable_implementation_provider_none_allowed": False,
                        "executable_implementation_ref_required": True,
                        "executable_provider_skill_id_required": True,
                    },
                    "forbidden_fallback_sources": EXPECTED_D03_FALLBACK_SOURCES,
                    "failure_precedence": EXPECTED_D03_FAILURE_PRECEDENCE,
                    "required_negative_outcomes": EXPECTED_D03_FAILURE_CODES,
                    "required_outcome_semantics": EXPECTED_D03_OUTCOME_SEMANTICS,
                },
                f"{gate_id}.evidence_contract",
            )

        channel = require_non_empty_string(entry.get("evidence_channel"), f"{gate_id}.evidence_channel")
        if channel not in EXPECTED_EVIDENCE_CHANNELS:
            fail(f"{gate_id}.evidence_channel {channel} is not in vocabularies.evidence_channel")

        backend = require_non_empty_string(entry.get("backend"), f"{gate_id}.backend")
        if backend in EXPECTED_FORBIDDEN_BACKENDS:
            fail(f"{gate_id}.backend {backend} is a forbidden backend for runtime evidence")
        if backend not in EXPECTED_BACKENDS:
            fail(f"{gate_id}.backend {backend} is not in vocabularies.backend")
        if entry.get("lease_required") is not True:
            fail(f"{gate_id}.lease_required must be true; runtime evidence requires an allocated dev lease")

        covered = require_string_list(entry.get("covers_invariants"), f"{gate_id}.covers_invariants")
        for invariant_id in covered:
            if invariant_id not in seen_ids:
                fail(f"{gate_id}.covers_invariants references unknown invariant {invariant_id}")
            if invariant_id not in runtime_invariant_ids:
                fail(
                    f"{gate_id}.covers_invariants references {invariant_id}, which is enforced statically; "
                    "a runtime evidence gate may only cover runtime or hybrid invariants"
                )
            gate_covered_invariants.add(invariant_id)

    missing_topics = [topic for topic in REQUIRED_RUNTIME_TOPICS if topic not in seen_topics]
    if missing_topics:
        fail(f"runtime evidence gates must cover topics {REQUIRED_RUNTIME_TOPICS}; missing {missing_topics}")
    unexpected_topics = [topic for topic in seen_topics if topic not in REQUIRED_RUNTIME_TOPICS]
    if unexpected_topics:
        fail(f"runtime evidence gates declare unknown topics {unexpected_topics}")

    uncovered = sorted(runtime_invariant_ids - gate_covered_invariants)
    if uncovered:
        fail(
            "every runtime or hybrid invariant must be covered by at least one runtime evidence gate; "
            f"uncovered {uncovered}"
        )

    # -------------------------------------------------------- declared gaps
    gaps = require_list(contract.get("open_cross_domain_gaps"), "open_cross_domain_gaps")
    seen_gap_ids = set()
    for index, raw in enumerate(gaps):
        entry = require_mapping(raw, f"open_cross_domain_gaps[{index}]")
        gap_id = require_non_empty_string(entry.get("id"), f"open_cross_domain_gaps[{index}].id")
        if not GAP_ID_PATTERN.match(gap_id):
            fail(f"gap id {gap_id} must match D04-GAP-<NAME>")
        if gap_id in seen_gap_ids:
            fail(f"duplicate gap id {gap_id}")
        seen_gap_ids.add(gap_id)

        require_non_empty_string(entry.get("owner_domain"), f"{gap_id}.owner_domain")
        require_non_empty_string(entry.get("statement"), f"{gap_id}.statement")
        require_non_empty_string(entry.get("resolution_owner"), f"{gap_id}.resolution_owner")

        blocks_status = require_non_empty_string(
            entry.get("blocks_lifecycle_status"), f"{gap_id}.blocks_lifecycle_status"
        )
        if blocks_status not in EXPECTED_LIFECYCLE:
            fail(f"{gap_id}.blocks_lifecycle_status {blocks_status} is not in vocabularies.lifecycle_status")

        for invariant_id in require_string_list(entry.get("blocks_invariants"), f"{gap_id}.blocks_invariants"):
            if invariant_id not in seen_ids:
                fail(f"{gap_id}.blocks_invariants references unknown invariant {invariant_id}")

        detection = require_non_empty_string(entry.get("detection"), f"{gap_id}.detection")
        if detection not in KNOWN_GAP_DETECTIONS:
            fail(f"{gap_id}.detection {detection} is not a known detection")

        if detection == "character_scope_registry_missing_domain_04_keys":
            registry = require_mapping(load_yaml(SCOPE_REGISTRY, "character scope registry"), "character scope registry")
            entries = require_list(registry.get("entries"), "character-scope-registry.yaml entries")
            registry_keys = set()
            for entry_index, raw_entry in enumerate(entries):
                mapping = require_mapping(raw_entry, f"character-scope-registry.yaml entries[{entry_index}]")
                registry_keys.add(
                    require_non_empty_string(
                        mapping.get("key"), f"character-scope-registry.yaml entries[{entry_index}].key"
                    )
                )
            still_missing = [key for key in persisted_keys if key not in registry_keys]
            if not still_missing:
                fail(
                    f"{gap_id} declares Domain 04 keys missing from the character scope registry, "
                    "but every declared key is now present; remove the resolved gap entry"
                )

    for invariant_id, gap_id in gap_references.items():
        if gap_id not in seen_gap_ids:
            fail(f"{invariant_id}.blocked_by_gap references unknown gap {gap_id}")

    # ---------------------------------------------- validator self-description
    validator = require_mapping(contract.get("semantic_validator_contract"), "semantic_validator_contract")
    command = require_non_empty_string(validator.get("command"), "semantic_validator_contract.command")
    if "scripts/check-domain-04-validation-contract.py" not in command:
        fail("semantic_validator_contract.command must invoke scripts/check-domain-04-validation-contract.py")
    require_string_list(validator.get("required_checks"), "semantic_validator_contract.required_checks")
    require_string_list(
        validator.get("explicitly_not_checked"), "semantic_validator_contract.explicitly_not_checked"
    )

    print(
        f"{len(seen_ids)} invariants validated, "
        f"{len(seen_gate_ids)} runtime evidence gates, "
        f"{len(seen_gap_ids)} open cross-domain gap(s)"
    )
    print(f"{target.name}: ok")


if __name__ == "__main__":
    main()
