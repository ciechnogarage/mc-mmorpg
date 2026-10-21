#!/usr/bin/env python3
"""Validate one Domain 04 skill record against pinned Domain 03 authority.

The command emits exactly one JSON object. Exit 0 means static admission under
the record's lifecycle. Exit 1 is a fail-closed structured rejection. Static
admission never proves runtime execution or release eligibility.
"""

from __future__ import annotations

import contextlib
import hashlib
import importlib.util
import io
import json
import re
import sys
from pathlib import Path

import yaml

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SKILL_CONTRACT = ROOT / "docs/design/classes/skill-library-contract.yaml"
DECLARATION_CHECKER = ROOT / "scripts/check-domain-04-validation-contract.py"
AUTHORITY_REGISTRY = ROOT / "docs/contracts/domain-03-combat-mechanic-authority-registry.yaml"
AUTHORITY_REGISTRY_SHA256 = "5c0dfed489c5a3fc4b415e0508102ec501d8026fc0c387f72019d9b6632d8cf8"
AUTHORITY_SNAPSHOT_ROOT = (ROOT / "docs/contracts/domain-03-authority-snapshots").resolve()
AUTHORITY_ID = "domain_03_combat_core"
AUTHORITY_CONTRACT_REF = (
    "/home/przemek/.traycer/epics/b638c9da-033f-4f61-8587-60459f79e9b2/"
    "artifacts/domain-03-combat-core-revision/index.md@"
    "33b076efb539dc5b0007c372d939ed447be6216116f975d95b20c76bf42e32e4"
)

HEX_SHA256 = re.compile(r"^[0-9a-f]{64}$")
NAMESPACED_ID = re.compile(r"^[a-z0-9][a-z0-9_.-]*:[a-z0-9][a-z0-9_./-]*$")
PATH_AT_SHA256 = re.compile(r"^(?P<path>[^@\s]+)@(?P<digest>[0-9a-f]{64})$")

REQUEST_KEYS = {
    "schema_version",
    "request_kind",
    "skill_record",
    "d03_authority_snapshot_ref",
    "fallback_attempted_mechanic_ids",
}
RECORD_REQUIRED_KEYS = {
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
}
RECORD_ALLOWED_KEYS = RECORD_REQUIRED_KEYS | {"display"}
DISPLAY_KEYS = {"name_key", "description_key", "icon_ref"}
OWNERSHIP_KEYS = {"source_type", "source_id"}
CLASSIFICATION_KEYS = {
    "activation_type",
    "ability_tags",
    "mechanic_tags",
    "catalogued_mechanic_ids",
    "damage_family_id",
    "damage_school_id",
    "effect_ids",
}
TARGETING_KEYS = {"target_type", "range_profile_id", "target_cap"}
REQUIREMENTS_KEYS = {
    "character_level_requirement_id",
    "class_ids",
    "subclass_ids",
    "weapon_school_ids",
    "state_requirement_ids",
    "resource_requirement_ids",
}
EXECUTION_KEYS = {
    "cast_time_rule_id",
    "channel_time_rule_id",
    "global_cooldown_rule_id",
    "cooldown_rule_id",
    "charge_rule_id",
    "resource_costs",
    "snapshot_rule",
}
RESOURCE_COST_KEYS = {"resource_id", "cost_rule_id", "cost_value_id"}
SCALING_KEYS = {"formula_id", "coefficient_set_id"}
CONTROL_KEYS = {"interruptible", "cancellable", "movement_rule", "cc_category_id"}
RUNTIME_KEYS = {"implementation_provider", "implementation_ref", "provider_skill_id"}
LIFECYCLE_KEYS = {"status", "validation_refs"}
BINDING_KEYS = {
    "mechanic_id",
    "mechanic_contract_version",
    "required_readiness",
    "mechanic_contract_ref",
    "gate_transition_envelope_ref",
    "contract_bundle_digest",
    "build_digest",
}
REGISTRY_KEYS = {
    "schema_version",
    "registry_kind",
    "authority_id",
    "authority_contract_ref",
    "snapshots",
}
REGISTRY_ENTRY_KEYS = {"snapshot_id", "snapshot_ref", "lifecycle"}
SNAPSHOT_KEYS = {
    "schema_version",
    "snapshot_kind",
    "snapshot_id",
    "authority_id",
    "authority_contract_ref",
    "scope_mechanic_ids",
    "bindings",
}
AUTHORITY_BINDING_KEYS = {
    "mechanic_id",
    "mechanic_contract_version",
    "readiness",
    "mechanic_contract_ref",
    "gate_transition_envelope_ref",
    "contract_bundle_digest",
    "build_digest",
    "revoked",
}

SOURCE_TYPES = {"class", "subclass", "weapon_school", "race", "faction", "religion", "legacy"}
ACTIVATION_TYPES = {"active", "passive", "toggle", "reactive"}
TARGET_TYPES = {"self", "unit", "area", "direction", "ground", "none"}
SNAPSHOT_RULES = {"cast", "impact", "periodic_tick"}
MOVEMENT_RULES = {"allowed", "slowed", "forbidden"}
PLANNING_STATUSES = {"draft", "validated"}
EXECUTABLE_STATUSES = {"runtime_verified", "released"}
PROVIDERS = {"mmocore", "mythicmobs", "custom", "none"}
FORBIDDEN_LOCAL_FIELDS = {
    "formula_text",
    "formula",
    "expression",
    "equation",
    "script_body",
    "inline_math",
    "stat_definition",
    "damage_formula_definition",
    "mitigation_order",
    "critical_rule",
    "resource_regeneration_rule",
    "resource_persistence_rule",
    "cooldown_cap_rule",
    "cooldown_recovery_rule",
    "cc_diminishing_returns_rule",
    "pvp_scaling_rule",
    "poise_rule",
    "tenacity_rule",
    "stealth_detection_rule",
}
FAIL_CLOSED = {
    "admission": "REJECT_MECHANIC",
    "lifecycle_ceiling": "validated",
    "executable_provider": "DISABLED",
    "fallback": "FORBIDDEN",
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


def emit(
    accepted: bool,
    reason_code: str,
    *,
    skill_id: str | None = None,
    mechanic_id: str | None = None,
    detail: str | None = None,
    outcome: dict | None = None,
) -> None:
    result = {
        "accepted": accepted,
        "reason_code": reason_code,
        **(
            outcome
            if outcome is not None
            else FAIL_CLOSED
        ),
    }
    if skill_id is not None:
        result["skill_id"] = skill_id
    if mechanic_id is not None:
        result["mechanic_id"] = mechanic_id
    if detail is not None:
        result["detail"] = detail
    print(json.dumps(result, sort_keys=True))
    raise SystemExit(0 if accepted else 1)


def invalid(detail: str, *, skill_id: str | None = None, mechanic_id: str | None = None) -> None:
    emit(False, "D04_SKILL_RECORD_INVALID", skill_id=skill_id, mechanic_id=mechanic_id, detail=detail)


def load_yaml_bytes(raw: bytes, label: str):
    try:
        text = raw.decode("utf-8")
        return yaml.load(text, Loader=StrictLoader)
    except (UnicodeError, yaml.YAMLError) as error:
        invalid(f"{label} is not valid strict UTF-8 YAML: {error}")


def read_bytes(path: Path, label: str) -> bytes:
    try:
        return path.read_bytes()
    except OSError as error:
        invalid(f"cannot read {label} at {path}: {error}")


def load_yaml(path: Path, label: str):
    return load_yaml_bytes(read_bytes(path, label), label)


def exact_mapping(value, keys: set[str], label: str, *, skill_id: str | None = None) -> dict:
    if type(value) is not dict:
        invalid(f"{label} must be a mapping", skill_id=skill_id)
    actual = set(value)
    if actual != keys:
        invalid(
            f"{label} keys must be exactly {sorted(keys)}; "
            f"missing={sorted(keys - actual)}, extra={sorted(actual - keys)}",
            skill_id=skill_id,
        )
    return value


def require_non_empty_string(value, label: str, *, skill_id: str | None = None) -> str:
    if type(value) is not str or not value.strip():
        invalid(f"{label} must be a non-empty string", skill_id=skill_id)
    return value


def require_string_or_null(value, label: str, *, skill_id: str | None = None):
    if value is not None and (type(value) is not str or not value.strip()):
        invalid(f"{label} must be a non-empty string or null", skill_id=skill_id)
    return value


def require_bool(value, label: str, *, skill_id: str | None = None) -> bool:
    if type(value) is not bool:
        invalid(f"{label} must be a boolean", skill_id=skill_id)
    return value


def require_int_or_null(value, label: str, *, skill_id: str | None = None):
    if value is not None and type(value) is not int:
        invalid(f"{label} must be an integer or null", skill_id=skill_id)
    return value


def require_string_list(value, label: str, *, skill_id: str | None = None) -> list[str]:
    if type(value) is not list:
        invalid(f"{label} must be a list", skill_id=skill_id)
    for index, item in enumerate(value):
        require_non_empty_string(item, f"{label}[{index}]", skill_id=skill_id)
    return value


def require_namespaced_id(value, label: str, *, skill_id: str | None = None) -> str:
    value = require_non_empty_string(value, label, skill_id=skill_id)
    if not NAMESPACED_ID.fullmatch(value):
        invalid(f"{label} must be a lowercase namespaced id", skill_id=skill_id)
    return value


def require_sha256(value, label: str, *, skill_id: str | None = None) -> str:
    value = require_non_empty_string(value, label, skill_id=skill_id)
    if not HEX_SHA256.fullmatch(value):
        invalid(f"{label} must be a lowercase sha256", skill_id=skill_id)
    return value


def split_path_at_sha256(value, label: str, *, skill_id: str | None = None) -> tuple[str, str]:
    value = require_non_empty_string(value, label, skill_id=skill_id)
    match = PATH_AT_SHA256.fullmatch(value)
    if not match:
        invalid(f"{label} must use immutable path@sha256 syntax", skill_id=skill_id)
    return match.group("path"), match.group("digest")


def require_path_at_sha256(value, label: str, *, skill_id: str | None = None) -> str:
    split_path_at_sha256(value, label, skill_id=skill_id)
    return value


def find_forbidden_field(value, path="skill_record") -> str | None:
    if type(value) is dict:
        for key, child in value.items():
            if key in FORBIDDEN_LOCAL_FIELDS:
                return f"{path}.{key}"
            found = find_forbidden_field(child, f"{path}.{key}")
            if found:
                return found
    elif type(value) is list:
        for index, child in enumerate(value):
            found = find_forbidden_field(child, f"{path}[{index}]")
            if found:
                return found
    return None


def validate_binding_shape(binding, label: str, skill_id: str) -> dict:
    binding = exact_mapping(binding, BINDING_KEYS, label, skill_id=skill_id)
    require_namespaced_id(binding["mechanic_id"], f"{label}.mechanic_id", skill_id=skill_id)
    version = binding["mechanic_contract_version"]
    if type(version) is not int or version <= 0:
        invalid(f"{label}.mechanic_contract_version must be a positive integer", skill_id=skill_id)
    if type(binding["required_readiness"]) is not str or binding["required_readiness"] != "RUNTIME_VERIFIED":
        invalid(f"{label}.required_readiness must be literal RUNTIME_VERIFIED", skill_id=skill_id)
    require_path_at_sha256(binding["mechanic_contract_ref"], f"{label}.mechanic_contract_ref", skill_id=skill_id)
    require_path_at_sha256(
        binding["gate_transition_envelope_ref"],
        f"{label}.gate_transition_envelope_ref",
        skill_id=skill_id,
    )
    require_sha256(binding["contract_bundle_digest"], f"{label}.contract_bundle_digest", skill_id=skill_id)
    require_sha256(binding["build_digest"], f"{label}.build_digest", skill_id=skill_id)
    return binding


def validate_authority_binding_shape(binding, label: str, skill_id: str) -> dict:
    binding = exact_mapping(binding, AUTHORITY_BINDING_KEYS, label, skill_id=skill_id)
    require_namespaced_id(binding["mechanic_id"], f"{label}.mechanic_id", skill_id=skill_id)
    version = binding["mechanic_contract_version"]
    if type(version) is not int or version <= 0:
        invalid(f"{label}.mechanic_contract_version must be a positive integer", skill_id=skill_id)
    require_non_empty_string(binding["readiness"], f"{label}.readiness", skill_id=skill_id)
    require_path_at_sha256(binding["mechanic_contract_ref"], f"{label}.mechanic_contract_ref", skill_id=skill_id)
    require_path_at_sha256(
        binding["gate_transition_envelope_ref"],
        f"{label}.gate_transition_envelope_ref",
        skill_id=skill_id,
    )
    require_sha256(binding["contract_bundle_digest"], f"{label}.contract_bundle_digest", skill_id=skill_id)
    require_sha256(binding["build_digest"], f"{label}.build_digest", skill_id=skill_id)
    require_bool(binding["revoked"], f"{label}.revoked", skill_id=skill_id)
    return binding


def validate_full_record_shape(record: dict) -> tuple[str, list[str], list[dict], dict, dict]:
    actual_keys = set(record) if type(record) is dict else set()
    if type(record) is not dict or not RECORD_REQUIRED_KEYS <= actual_keys or not actual_keys <= RECORD_ALLOWED_KEYS:
        invalid(
            "skill_record must contain exactly required keys plus optional display; "
            f"missing={sorted(RECORD_REQUIRED_KEYS - actual_keys)}, "
            f"extra={sorted(actual_keys - RECORD_ALLOWED_KEYS)}"
        )
    forbidden = find_forbidden_field(record)
    if forbidden:
        invalid(f"forbidden local authority field present at {forbidden}")

    skill_id = require_namespaced_id(record["skill_id"], "skill_record.skill_id")
    if type(record["schema_version"]) is not int or record["schema_version"] <= 0:
        invalid("skill_record.schema_version must be a positive integer", skill_id=skill_id)

    if "display" in record:
        display = exact_mapping(record["display"], DISPLAY_KEYS, "skill_record.display", skill_id=skill_id)
        for field in DISPLAY_KEYS:
            require_non_empty_string(display[field], f"skill_record.display.{field}", skill_id=skill_id)

    ownership = exact_mapping(record["ownership"], OWNERSHIP_KEYS, "skill_record.ownership", skill_id=skill_id)
    if type(ownership["source_type"]) is not str or ownership["source_type"] not in SOURCE_TYPES:
        invalid("skill_record.ownership.source_type is not canonical", skill_id=skill_id)
    require_non_empty_string(ownership["source_id"], "skill_record.ownership.source_id", skill_id=skill_id)

    classification = exact_mapping(
        record["classification"], CLASSIFICATION_KEYS, "skill_record.classification", skill_id=skill_id
    )
    if type(classification["activation_type"]) is not str or classification["activation_type"] not in ACTIVATION_TYPES:
        invalid("skill_record.classification.activation_type is not canonical", skill_id=skill_id)
    for field in ("ability_tags", "mechanic_tags", "effect_ids"):
        require_string_list(classification[field], f"skill_record.classification.{field}", skill_id=skill_id)
    for field in ("damage_family_id", "damage_school_id"):
        require_string_or_null(classification[field], f"skill_record.classification.{field}", skill_id=skill_id)
    reachable = require_string_list(
        classification["catalogued_mechanic_ids"],
        "skill_record.classification.catalogued_mechanic_ids",
        skill_id=skill_id,
    )
    for index, mechanic_id in enumerate(reachable):
        require_namespaced_id(
            mechanic_id,
            f"skill_record.classification.catalogued_mechanic_ids[{index}]",
            skill_id=skill_id,
        )
    if len(set(reachable)) != len(reachable):
        invalid("catalogued_mechanic_ids must not contain duplicates", skill_id=skill_id)

    targeting = exact_mapping(record["targeting"], TARGETING_KEYS, "skill_record.targeting", skill_id=skill_id)
    if type(targeting["target_type"]) is not str or targeting["target_type"] not in TARGET_TYPES:
        invalid("skill_record.targeting.target_type is not canonical", skill_id=skill_id)
    require_string_or_null(targeting["range_profile_id"], "skill_record.targeting.range_profile_id", skill_id=skill_id)
    require_int_or_null(targeting["target_cap"], "skill_record.targeting.target_cap", skill_id=skill_id)

    requirements = exact_mapping(
        record["requirements"], REQUIREMENTS_KEYS, "skill_record.requirements", skill_id=skill_id
    )
    require_string_or_null(
        requirements["character_level_requirement_id"],
        "skill_record.requirements.character_level_requirement_id",
        skill_id=skill_id,
    )
    for field in REQUIREMENTS_KEYS - {"character_level_requirement_id"}:
        require_string_list(requirements[field], f"skill_record.requirements.{field}", skill_id=skill_id)

    execution = exact_mapping(record["execution"], EXECUTION_KEYS, "skill_record.execution", skill_id=skill_id)
    for field in ("cast_time_rule_id", "channel_time_rule_id", "charge_rule_id"):
        require_string_or_null(execution[field], f"skill_record.execution.{field}", skill_id=skill_id)
    for field in ("global_cooldown_rule_id", "cooldown_rule_id"):
        require_non_empty_string(execution[field], f"skill_record.execution.{field}", skill_id=skill_id)
    if type(execution["snapshot_rule"]) is not str or execution["snapshot_rule"] not in SNAPSHOT_RULES:
        invalid("skill_record.execution.snapshot_rule is not canonical", skill_id=skill_id)
    if type(execution["resource_costs"]) is not list:
        invalid("skill_record.execution.resource_costs must be a list", skill_id=skill_id)
    for index, raw_cost in enumerate(execution["resource_costs"]):
        label = f"skill_record.execution.resource_costs[{index}]"
        cost = exact_mapping(raw_cost, RESOURCE_COST_KEYS, label, skill_id=skill_id)
        require_non_empty_string(cost["resource_id"], f"{label}.resource_id", skill_id=skill_id)
        require_non_empty_string(cost["cost_rule_id"], f"{label}.cost_rule_id", skill_id=skill_id)
        require_string_or_null(cost["cost_value_id"], f"{label}.cost_value_id", skill_id=skill_id)

    scaling = exact_mapping(record["scaling"], SCALING_KEYS, "skill_record.scaling", skill_id=skill_id)
    for field in SCALING_KEYS:
        require_non_empty_string(scaling[field], f"skill_record.scaling.{field}", skill_id=skill_id)

    control = exact_mapping(record["control"], CONTROL_KEYS, "skill_record.control", skill_id=skill_id)
    require_bool(control["interruptible"], "skill_record.control.interruptible", skill_id=skill_id)
    require_bool(control["cancellable"], "skill_record.control.cancellable", skill_id=skill_id)
    if type(control["movement_rule"]) is not str or control["movement_rule"] not in MOVEMENT_RULES:
        invalid("skill_record.control.movement_rule is not canonical", skill_id=skill_id)
    require_string_or_null(control["cc_category_id"], "skill_record.control.cc_category_id", skill_id=skill_id)

    bindings_value = record["d03_consumer_bindings"]
    if type(bindings_value) is not list:
        invalid("skill_record.d03_consumer_bindings must be a list", skill_id=skill_id)
    bindings = [
        validate_binding_shape(binding, f"skill_record.d03_consumer_bindings[{index}]", skill_id)
        for index, binding in enumerate(bindings_value)
    ]

    runtime = exact_mapping(record["runtime"], RUNTIME_KEYS, "skill_record.runtime", skill_id=skill_id)
    provider = runtime["implementation_provider"]
    if type(provider) is not str or provider not in PROVIDERS:
        invalid("skill_record.runtime.implementation_provider is not canonical", skill_id=skill_id)
    require_string_or_null(runtime["implementation_ref"], "skill_record.runtime.implementation_ref", skill_id=skill_id)
    require_string_or_null(runtime["provider_skill_id"], "skill_record.runtime.provider_skill_id", skill_id=skill_id)

    lifecycle = exact_mapping(record["lifecycle"], LIFECYCLE_KEYS, "skill_record.lifecycle", skill_id=skill_id)
    status = lifecycle["status"]
    if type(status) is not str or status not in PLANNING_STATUSES | EXECUTABLE_STATUSES:
        invalid("skill_record.lifecycle.status is not canonical", skill_id=skill_id)
    require_string_list(lifecycle["validation_refs"], "skill_record.lifecycle.validation_refs", skill_id=skill_id)
    return skill_id, reachable, bindings, runtime, lifecycle


def verify_authority_contract_ref(authority_ref: str) -> None:
    if authority_ref != AUTHORITY_CONTRACT_REF:
        invalid("authority_contract_ref is not the pinned Domain 03 authority")
    path_text, expected_digest = split_path_at_sha256(authority_ref, "authority_contract_ref")
    path = Path(path_text)
    raw = read_bytes(path, "pinned Domain 03 authority contract")
    if hashlib.sha256(raw).hexdigest() != expected_digest:
        invalid("pinned Domain 03 authority contract content digest mismatches")


def load_authority_registry() -> dict[str, dict]:
    raw = read_bytes(AUTHORITY_REGISTRY, "Domain 03 authority registry")
    if hashlib.sha256(raw).hexdigest() != AUTHORITY_REGISTRY_SHA256:
        invalid("Domain 03 authority registry digest mismatches the validator pin")
    registry = exact_mapping(load_yaml_bytes(raw, "Domain 03 authority registry"), REGISTRY_KEYS, "authority registry")
    if type(registry["schema_version"]) is not int or registry["schema_version"] != 1:
        invalid("authority registry schema_version must be integer 1")
    if registry["registry_kind"] != "domain_03_combat_mechanic_authority_registry":
        invalid("authority registry kind is not canonical")
    if registry["authority_id"] != AUTHORITY_ID:
        invalid("authority registry authority_id is not canonical")
    verify_authority_contract_ref(registry["authority_contract_ref"])
    if type(registry["snapshots"]) is not list or not registry["snapshots"]:
        invalid("authority registry snapshots must be a non-empty list")
    entries = {}
    for index, raw_entry in enumerate(registry["snapshots"]):
        label = f"authority registry snapshots[{index}]"
        entry = exact_mapping(raw_entry, REGISTRY_ENTRY_KEYS, label)
        snapshot_id = require_non_empty_string(entry["snapshot_id"], f"{label}.snapshot_id")
        require_path_at_sha256(entry["snapshot_ref"], f"{label}.snapshot_ref")
        if entry["lifecycle"] != "ACTIVE":
            invalid(f"{label}.lifecycle must be ACTIVE")
        if snapshot_id in entries:
            invalid(f"authority registry has duplicate snapshot_id {snapshot_id}")
        entries[snapshot_id] = entry
    return entries


def load_pinned_authority_snapshot(snapshot_ref: str, reachable: list[str], skill_id: str) -> list[dict]:
    entries = load_authority_registry()
    matching = [entry for entry in entries.values() if entry["snapshot_ref"] == snapshot_ref]
    if len(matching) != 1:
        invalid("authority snapshot ref is not uniquely ACTIVE in the pinned registry", skill_id=skill_id)
    path_text, expected_digest = split_path_at_sha256(snapshot_ref, "d03_authority_snapshot_ref", skill_id=skill_id)
    snapshot_path = (ROOT / path_text).resolve()
    if not snapshot_path.is_relative_to(AUTHORITY_SNAPSHOT_ROOT):
        invalid("authority snapshot path escapes the canonical snapshot root", skill_id=skill_id)
    raw = read_bytes(snapshot_path, "Domain 03 authority snapshot")
    if hashlib.sha256(raw).hexdigest() != expected_digest:
        invalid("authority snapshot content digest mismatches its pinned ref", skill_id=skill_id)
    snapshot = exact_mapping(load_yaml_bytes(raw, "Domain 03 authority snapshot"), SNAPSHOT_KEYS, "authority snapshot")
    if type(snapshot["schema_version"]) is not int or snapshot["schema_version"] != 1:
        invalid("authority snapshot schema_version must be integer 1", skill_id=skill_id)
    if snapshot["snapshot_kind"] != "domain_03_combat_mechanic_authority_snapshot":
        invalid("authority snapshot kind is not canonical", skill_id=skill_id)
    if snapshot["authority_id"] != AUTHORITY_ID:
        invalid("authority snapshot authority_id is not canonical", skill_id=skill_id)
    verify_authority_contract_ref(snapshot["authority_contract_ref"])
    if snapshot["snapshot_id"] != matching[0]["snapshot_id"]:
        invalid("authority snapshot_id mismatches registry entry", skill_id=skill_id)
    scope = require_string_list(snapshot["scope_mechanic_ids"], "authority snapshot scope", skill_id=skill_id)
    for index, mechanic_id in enumerate(scope):
        require_namespaced_id(mechanic_id, f"authority snapshot scope[{index}]", skill_id=skill_id)
    if len(set(scope)) != len(scope):
        invalid("authority snapshot scope contains duplicates", skill_id=skill_id)
    if set(scope) != set(reachable):
        invalid("authority snapshot scope must exactly equal reachable mechanic ids", skill_id=skill_id)
    raw_bindings = snapshot["bindings"]
    if type(raw_bindings) is not list:
        invalid("authority snapshot bindings must be a list", skill_id=skill_id)
    bindings = [
        validate_authority_binding_shape(binding, f"authority snapshot bindings[{index}]", skill_id)
        for index, binding in enumerate(raw_bindings)
    ]
    binding_ids = [binding["mechanic_id"] for binding in bindings]
    if len(set(binding_ids)) != len(binding_ids):
        invalid("authority snapshot contains duplicate mechanic ids", skill_id=skill_id)
    if set(binding_ids) != set(scope):
        invalid("authority snapshot bindings must exactly cover its declared scope", skill_id=skill_id)
    return bindings


def reject_policy(policy: dict, case_id: str, skill_id: str, mechanic_id: str | None, detail: str) -> None:
    outcome = policy["failure_outcomes"][case_id]
    emit(
        False,
        outcome["reason_code"],
        skill_id=skill_id,
        mechanic_id=mechanic_id,
        detail=detail,
        outcome={key: outcome[key] for key in FAIL_CLOSED},
    )


def validate_request(request: dict, policy: dict) -> None:
    request = exact_mapping(request, REQUEST_KEYS, "request")
    if type(request["schema_version"]) is not int or request["schema_version"] != 1:
        invalid("request.schema_version must be integer 1")
    if request["request_kind"] != "domain_04_skill_record_validation":
        invalid("request.request_kind must be domain_04_skill_record_validation")
    skill_id, reachable, bindings, runtime, lifecycle = validate_full_record_shape(request["skill_record"])
    fallback_attempts = require_string_list(
        request["fallback_attempted_mechanic_ids"], "fallback_attempted_mechanic_ids", skill_id=skill_id
    )
    for index, mechanic_id in enumerate(fallback_attempts):
        require_namespaced_id(mechanic_id, f"fallback_attempted_mechanic_ids[{index}]", skill_id=skill_id)
    if len(set(fallback_attempts)) != len(fallback_attempts):
        invalid("fallback_attempted_mechanic_ids contains duplicates", skill_id=skill_id)
    if set(fallback_attempts) - set(reachable):
        invalid("fallback attempts must be scoped to reachable mechanics", skill_id=skill_id)

    status = lifecycle["status"]
    provider = runtime["implementation_provider"]
    implementation_ref = runtime["implementation_ref"]
    provider_skill_id = runtime["provider_skill_id"]
    snapshot_ref = request["d03_authority_snapshot_ref"]
    authority_bindings = []

    if status in PLANNING_STATUSES:
        if provider != "none" or implementation_ref is not None or provider_skill_id is not None:
            emit(
                False,
                "D04_SKILL_LIFECYCLE_PROVIDER_INVALID",
                skill_id=skill_id,
                detail="planning lifecycle requires provider none and null implementation refs",
            )
        if not reachable and not bindings:
            if snapshot_ref is not None:
                invalid("empty planning record must not declare an authority snapshot", skill_id=skill_id)
            if fallback_attempts:
                reject_policy(policy, "fallback_attempt", skill_id, fallback_attempts[0], "planning fallback attempted")
            emit(
                True,
                "D04_SKILL_RECORD_ACCEPTED",
                skill_id=skill_id,
                outcome={
                    "admission": "ADMIT_PLANNING_RECORD",
                    "lifecycle_ceiling": status,
                    "executable_provider": "DISABLED",
                    "fallback": "FORBIDDEN",
                },
            )
    else:
        if provider == "none" or implementation_ref is None or provider_skill_id is None:
            emit(
                False,
                "D04_SKILL_LIFECYCLE_PROVIDER_INVALID",
                skill_id=skill_id,
                detail="executable lifecycle requires non-none provider and both implementation refs",
            )
        if not reachable or not bindings:
            reject_policy(
                policy,
                "missing",
                skill_id,
                reachable[0] if reachable else None,
                "executable lifecycle requires non-empty reachable mechanics and bindings",
            )

    if type(snapshot_ref) is not str:
        invalid("non-empty mechanic record requires d03_authority_snapshot_ref", skill_id=skill_id)
    authority_bindings = load_pinned_authority_snapshot(snapshot_ref, reachable, skill_id)
    authority_by_id = {binding["mechanic_id"]: binding for binding in authority_bindings}

    binding_counts = {mechanic_id: 0 for mechanic_id in reachable}
    for binding in bindings:
        mechanic_id = binding["mechanic_id"]
        if mechanic_id not in binding_counts:
            emit(
                False,
                "D04_BINDING_COVERAGE_EXTRA",
                skill_id=skill_id,
                mechanic_id=mechanic_id,
                detail="binding mechanic_id is outside catalogued_mechanic_ids",
            )
        binding_counts[mechanic_id] += 1

    for case_id in policy["failure_precedence"]:
        if case_id == "fallback_attempt":
            attempted = next((mechanic_id for mechanic_id in reachable if mechanic_id in fallback_attempts), None)
            if attempted is not None:
                reject_policy(policy, case_id, skill_id, attempted, "local or provider fallback was attempted")
        elif case_id == "duplicate":
            duplicate = next((mechanic_id for mechanic_id, count in binding_counts.items() if count > 1), None)
            if duplicate is not None:
                reject_policy(policy, case_id, skill_id, duplicate, "more than one binding resolves the mechanic")
        elif case_id == "missing":
            missing = next((mechanic_id for mechanic_id, count in binding_counts.items() if count == 0), None)
            if missing is not None:
                reject_policy(policy, case_id, skill_id, missing, "no binding resolves the reachable mechanic")
        else:
            for mechanic_id in reachable:
                binding = next(item for item in bindings if item["mechanic_id"] == mechanic_id)
                authority = authority_by_id[mechanic_id]
                if case_id == "stale" and (
                    authority["mechanic_contract_version"] != binding["mechanic_contract_version"]
                    or authority["mechanic_contract_ref"] != binding["mechanic_contract_ref"]
                    or authority["gate_transition_envelope_ref"] != binding["gate_transition_envelope_ref"]
                ):
                    reject_policy(policy, case_id, skill_id, mechanic_id, "binding lineage is stale")
                if case_id == "digest_mismatch" and (
                    authority["contract_bundle_digest"] != binding["contract_bundle_digest"]
                    or authority["build_digest"] != binding["build_digest"]
                ):
                    reject_policy(policy, case_id, skill_id, mechanic_id, "bundle or build digest mismatches authority")
                if case_id == "downgraded" and authority["readiness"] != "RUNTIME_VERIFIED":
                    reject_policy(policy, case_id, skill_id, mechanic_id, "authority readiness is below RUNTIME_VERIFIED")
                if case_id == "revoked" and authority["revoked"] is True:
                    reject_policy(policy, case_id, skill_id, mechanic_id, "authority binding is revoked")

    if status in PLANNING_STATUSES:
        emit(
            True,
            "D04_SKILL_RECORD_ACCEPTED",
            skill_id=skill_id,
            outcome={
                "admission": "ADMIT_PLANNING_RECORD",
                "lifecycle_ceiling": status,
                "executable_provider": "DISABLED",
                "fallback": "FORBIDDEN",
            },
        )
    emit(
        True,
        "D04_SKILL_RECORD_ACCEPTED",
        skill_id=skill_id,
        outcome={
            "admission": "ADMIT_STATIC_CONTRACT",
            "lifecycle_ceiling": "NOT_GRANTED",
            "executable_provider": "NOT_PROVEN",
            "fallback": "FORBIDDEN",
        },
    )


def load_declaration_policy(contract_path: Path) -> dict:
    contract = load_yaml(contract_path, "skill contract")
    if type(contract) is not dict:
        invalid("skill contract root must be a mapping")
    try:
        spec = importlib.util.spec_from_file_location("d04_declaration_checker", DECLARATION_CHECKER)
        if spec is None or spec.loader is None:
            invalid("cannot load declaration checker")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with contextlib.redirect_stderr(io.StringIO()):
            return module.validate_d03_consumer_binding_contract(contract)
    except SystemExit:
        invalid("skill contract consumer-binding declaration is invalid")
    except Exception as error:
        invalid(f"cannot validate skill contract declaration: {type(error).__name__}: {error}")


def run(argv: list[str]) -> None:
    if not argv or len(argv) > 2:
        invalid("usage: check-domain-04-skill-record.py <validation-request> [skill-contract]")
    request_path = Path(argv[0])
    if not request_path.is_absolute():
        request_path = ROOT / request_path
    contract_path = Path(argv[1]) if len(argv) == 2 else DEFAULT_SKILL_CONTRACT
    if not contract_path.is_absolute():
        contract_path = ROOT / contract_path
    policy = load_declaration_policy(contract_path)
    request = load_yaml(request_path, "validation request")
    validate_request(request, policy)


if __name__ == "__main__":
    try:
        run(sys.argv[1:])
    except SystemExit:
        raise
    except BaseException as error:
        emit(False, "D04_SKILL_RECORD_INVALID", detail=f"contained internal error: {type(error).__name__}: {error}")
