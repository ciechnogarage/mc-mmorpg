#!/usr/bin/env python3
"""Validate schema-version-2 contract documents."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_TARGET = ROOT / "docs/contracts/account-character-contract.yaml"

APPROVED_V1_CHARACTER_NAMESPACE = "mcmmorpg:character:mmoprofiles-profile:v1"
APPROVED_ACCOUNT_ISSUER = "mcmmorpg-dev-offline"
APPROVED_ACCOUNT_NAMESPACE = "mcmmorpg:account:dev-offline:v1"
APPROVED_ACCOUNT_ASSURANCE = "dev_offline_unverified"
APPROVED_ACCOUNT_SOURCE_PRINCIPAL = "velocity_forwarded_profile"
APPROVED_ACCOUNT_ONLINE_NAMESPACE = "mcmmorpg:account:online:v1"
APPROVED_OFFLINE_TO_ONLINE_POLICY = "explicit_mapping_required"
APPROVED_CHARACTER_SOURCE = "MMOProfiles.getCurrent().getUniqueId()"
APPROVED_PROFILE_SOURCE = "MMOProfiles.profileId"

EXPECTED_STATUSES = {
    "contract_status": {"draft", "proposed", "accepted", "superseded", "rejected"},
    "runtime_status": {"not_tested", "blocked", "partial", "verified", "failed"},
    "release_status": {"blocked", "conditional", "approved"},
}

REQUIRED_TOP_LEVEL_FIELDS = {
    "schema_version",
    "contract_id",
    "contract_version",
    "contract_status",
    "runtime_status",
    "release_status",
    "contract_owner",
    "domain_owner",
    "semantic_owner",
    "runtime_authority",
    "persistence_owner",
    "integration_coordinator",
    "data_scope",
    "canonical_key",
    "identity_namespace",
    "writers",
    "consumers",
    "commands",
    "api",
    "domain_events",
    "permissions",
    "pdc_keys",
    "migration_policy",
    "backward_compatibility",
    "dependencies",
    "affected_systems",
    "evidence",
    "open_questions",
    "supersedes",
    "superseded_by",
    "state_vocabularies",
    "store_records",
}


def fail(message: str) -> None:
    print(f"contracts v2 check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load(path: Path) -> dict[str, Any]:
    try:
        data = yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as exc:  # pragma: no cover
        fail(f"cannot parse {path}: {exc}")
    if not isinstance(data, dict):
        fail(f"{path} must be a mapping")
    return data


def require_mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def require_list(value: Any, label: str) -> list[Any]:
    if not isinstance(value, list):
        fail(f"{label} must be a list")
    return value


def require_non_empty_string(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def check_allowed_statuses(document: dict[str, Any], path_label: str) -> None:
    for key, allowed in EXPECTED_STATUSES.items():
        value = require_non_empty_string(document.get(key), f"{path_label}.{key}")
        if value not in allowed:
            fail(f"{path_label}.{key} must be one of {sorted(allowed)}")


def validate_writer_shape(document: dict[str, Any], path_label: str) -> None:
    seen_systems: set[str] = set()
    for index, writer in enumerate(require_list(document.get("writers"), f"{path_label}.writers"), start=1):
        writer_mapping = require_mapping(writer, f"{path_label}.writers[{index}]")
        system = require_non_empty_string(writer_mapping.get("system"), f"{path_label}.writers[{index}].system")
        if system in seen_systems:
            fail(f"{path_label}.writers has duplicate system {system!r}")
        seen_systems.add(system)
        for write_index, item in enumerate(
            require_list(writer_mapping.get("writes"), f"{path_label}.writers[{index}].writes"),
            start=1,
        ):
            require_non_empty_string(item, f"{path_label}.writers[{index}].writes[{write_index}]")


def validate_identity_sections(document: dict[str, Any], path_label: str) -> None:
    canonical_key = require_mapping(document.get("canonical_key"), f"{path_label}.canonical_key")
    identity_namespace = require_mapping(document.get("identity_namespace"), f"{path_label}.identity_namespace")
    api = require_mapping(document.get("api"), f"{path_label}.api")

    account_id = require_mapping(canonical_key.get("account_id"), f"{path_label}.canonical_key.account_id")
    character_id = require_mapping(canonical_key.get("character_id"), f"{path_label}.canonical_key.character_id")
    profile_id = require_mapping(canonical_key.get("profile_id"), f"{path_label}.canonical_key.profile_id")

    require_non_empty_string(account_id.get("issuer"), f"{path_label}.canonical_key.account_id.issuer")
    require_non_empty_string(account_id.get("namespace"), f"{path_label}.canonical_key.account_id.namespace")
    require_non_empty_string(account_id.get("assurance_level"), f"{path_label}.canonical_key.account_id.assurance_level")
    require_non_empty_string(account_id.get("source_principal"), f"{path_label}.canonical_key.account_id.source_principal")
    require_non_empty_string(
        account_id.get("production_online_namespace"),
        f"{path_label}.canonical_key.account_id.production_online_namespace",
    )
    require_non_empty_string(character_id.get("namespace"), f"{path_label}.canonical_key.character_id.namespace")
    require_non_empty_string(character_id.get("source"), f"{path_label}.canonical_key.character_id.source")
    require_non_empty_string(profile_id.get("namespace"), f"{path_label}.canonical_key.profile_id.namespace")
    require_non_empty_string(profile_id.get("source"), f"{path_label}.canonical_key.profile_id.source")

    if account_id.get("issuer") != APPROVED_ACCOUNT_ISSUER:
        fail(f"{path_label}.canonical_key.account_id.issuer must be {APPROVED_ACCOUNT_ISSUER}")
    if account_id.get("namespace") != APPROVED_ACCOUNT_NAMESPACE:
        fail(f"{path_label}.canonical_key.account_id.namespace must be {APPROVED_ACCOUNT_NAMESPACE}")
    if account_id.get("assurance_level") != APPROVED_ACCOUNT_ASSURANCE:
        fail(f"{path_label}.canonical_key.account_id.assurance_level must be {APPROVED_ACCOUNT_ASSURANCE}")
    if account_id.get("source_principal") != APPROVED_ACCOUNT_SOURCE_PRINCIPAL:
        fail(f"{path_label}.canonical_key.account_id.source_principal must be {APPROVED_ACCOUNT_SOURCE_PRINCIPAL}")
    if account_id.get("production_online_namespace") != APPROVED_ACCOUNT_ONLINE_NAMESPACE:
        fail(
            f"{path_label}.canonical_key.account_id.production_online_namespace "
            f"must be {APPROVED_ACCOUNT_ONLINE_NAMESPACE}"
        )

    if character_id.get("namespace") != APPROVED_V1_CHARACTER_NAMESPACE:
        fail(f"{path_label}.canonical_key.character_id.namespace must be {APPROVED_V1_CHARACTER_NAMESPACE}")
    if character_id.get("source") != APPROVED_CHARACTER_SOURCE:
        fail(f"{path_label}.canonical_key.character_id.source must be {APPROVED_CHARACTER_SOURCE}")
    if character_id.get("value_equal_to_profile_id_in_v1") is not True:
        fail(f"{path_label}.canonical_key.character_id.value_equal_to_profile_id_in_v1 must be true")

    if profile_id.get("source") != APPROVED_PROFILE_SOURCE:
        fail(f"{path_label}.canonical_key.profile_id.source must be {APPROVED_PROFILE_SOURCE}")
    if profile_id.get("technical_key_only") is not True:
        fail(f"{path_label}.canonical_key.profile_id.technical_key_only must be true")

    if identity_namespace.get("account") != account_id.get("namespace"):
        fail(f"{path_label}.identity_namespace.account must match account namespace")
    if identity_namespace.get("character") != character_id.get("namespace"):
        fail(f"{path_label}.identity_namespace.character must match character namespace")
    if identity_namespace.get("profile") != profile_id.get("namespace"):
        fail(f"{path_label}.identity_namespace.profile must match profile namespace")

    account_policy = require_mapping(api.get("account_identity_policy"), f"{path_label}.api.account_identity_policy")
    if account_policy.get("namespace") != account_id.get("namespace"):
        fail(f"{path_label}.api.account_identity_policy.namespace must match account namespace")
    if account_policy.get("issuer") != account_id.get("issuer"):
        fail(f"{path_label}.api.account_identity_policy.issuer must match account issuer")
    if account_policy.get("assurance_level") != account_id.get("assurance_level"):
        fail(f"{path_label}.api.account_identity_policy.assurance_level must match account assurance level")
    if account_policy.get("source_principal") != account_id.get("source_principal"):
        fail(f"{path_label}.api.account_identity_policy.source_principal must match account source_principal")
    if account_policy.get("production_online_namespace") != account_id.get("production_online_namespace"):
        fail(
            f"{path_label}.api.account_identity_policy.production_online_namespace "
            "must match canonical_key.account_id.production_online_namespace"
        )
    if account_policy.get("offline_to_online_policy") != APPROVED_OFFLINE_TO_ONLINE_POLICY:
        fail(f"{path_label}.api.account_identity_policy.offline_to_online_policy must be {APPROVED_OFFLINE_TO_ONLINE_POLICY}")
    if account_policy.get("silent_key_rewrite") != "forbidden":
        fail(f"{path_label}.api.account_identity_policy.silent_key_rewrite must be forbidden")

    character_policy = require_mapping(api.get("character_identity_policy"), f"{path_label}.api.character_identity_policy")
    if character_policy.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.api.character_identity_policy.namespace must match character namespace")
    if character_policy.get("derives_from_minecraft_uuid") is not False:
        fail(f"{path_label}.api.character_identity_policy.derives_from_minecraft_uuid must be false")
    if character_policy.get("derives_from_profile_id") is not False:
        fail(f"{path_label}.api.character_identity_policy.derives_from_profile_id must be false")
    if character_policy.get("profile_id_value_equivalent_in_v1") is not True:
        fail(f"{path_label}.api.character_identity_policy.profile_id_value_equivalent_in_v1 must be true")
    forbidden_sources = require_list(
        character_policy.get("forbidden_sources"),
        f"{path_label}.api.character_identity_policy.forbidden_sources",
    )
    for source in ("player_uuid", "minecraft_uuid", "display_name"):
        if source not in forbidden_sources:
            fail(f"{path_label}.api.character_identity_policy.forbidden_sources must include {source}")
    if character_policy.get("mutable") is not False:
        fail(f"{path_label}.api.character_identity_policy.mutable must be false")

    profile_mapping_policy = require_mapping(api.get("profile_mapping_policy"), f"{path_label}.api.profile_mapping_policy")
    if profile_mapping_policy.get("namespace") != profile_id.get("namespace"):
        fail(f"{path_label}.api.profile_mapping_policy.namespace must match profile namespace")
    if profile_mapping_policy.get("technical_key_only") is not True:
        fail(f"{path_label}.api.profile_mapping_policy.technical_key_only must be true")
    if profile_mapping_policy.get("semantic_character_identity") != "forbidden":
        fail(f"{path_label}.api.profile_mapping_policy.semantic_character_identity must be forbidden")

    activation_policy = require_mapping(api.get("activation_session_policy"), f"{path_label}.api.activation_session_policy")
    if activation_policy.get("lifecycle_status_required") != "PLAYABLE":
        fail(f"{path_label}.api.activation_session_policy.lifecycle_status_required must be PLAYABLE")
    if activation_policy.get("creator_stage_required") != "READY":
        fail(f"{path_label}.api.activation_session_policy.creator_stage_required must be READY")
    if activation_policy.get("required_flags") != [
        "schema_version_supported",
        "all_required_profiles_loaded",
        "session_lock_acquired",
        "no_concurrent_activation",
        "session_epoch_current",
    ]:
        fail(
            f"{path_label}.api.activation_session_policy.required_flags "
            "must be ['schema_version_supported', 'all_required_profiles_loaded', "
            "'session_lock_acquired', 'no_concurrent_activation', 'session_epoch_current']"
        )
    if activation_policy.get("stale_session_epoch_policy") != "fail_closed_and_require_reactivation":
        fail(
            f"{path_label}.api.activation_session_policy.stale_session_epoch_policy "
            "must be fail_closed_and_require_reactivation"
        )


def validate_store_records(document: dict[str, Any], path_label: str) -> None:
    store_records = require_mapping(document.get("store_records"), f"{path_label}.store_records")
    persistence_owner = require_mapping(document.get("persistence_owner"), f"{path_label}.persistence_owner")
    runtime_authority = require_mapping(document.get("runtime_authority"), f"{path_label}.runtime_authority")
    data_scope = require_list(document.get("data_scope"), f"{path_label}.data_scope")
    writers = require_list(document.get("writers"), f"{path_label}.writers")
    canonical_key = require_mapping(document.get("canonical_key"), f"{path_label}.canonical_key")
    account_id = require_mapping(canonical_key.get("account_id"), f"{path_label}.canonical_key.account_id")
    character_id = require_mapping(canonical_key.get("character_id"), f"{path_label}.canonical_key.character_id")
    profile_id = require_mapping(canonical_key.get("profile_id"), f"{path_label}.canonical_key.profile_id")
    api = require_mapping(document.get("api"), f"{path_label}.api")
    profile_mapping_policy = require_mapping(api.get("profile_mapping_policy"), f"{path_label}.api.profile_mapping_policy")

    account_record = require_mapping(store_records.get("account_record"), f"{path_label}.store_records.account_record")
    account_fields = require_mapping(account_record.get("fields"), f"{path_label}.store_records.account_record.fields")
    account_record_account_id = require_mapping(
        account_fields.get("account_id"),
        f"{path_label}.store_records.account_record.fields.account_id",
    )
    if account_record_account_id.get("namespace") != account_id.get("namespace"):
        fail(f"{path_label}.store_records.account_record.fields.account_id.namespace must match canonical_key.account_id.namespace")
    account_source_field = require_mapping(
        account_fields.get(account_id.get("source_principal")),
        f"{path_label}.store_records.account_record.fields.{account_id.get('source_principal')}",
    )
    if account_source_field.get("type") != "string":
        fail(f"{path_label}.store_records.account_record.fields.{account_id.get('source_principal')}.type must be string")
    if account_source_field.get("source") != runtime_authority.get("account_id"):
        fail(
            f"{path_label}.store_records.account_record.fields.{account_id.get('source_principal')}.source "
            "must match runtime_authority.account_id"
        )
    active_character_id = require_mapping(
        account_fields.get("active_character_id"),
        f"{path_label}.store_records.account_record.fields.active_character_id",
    )
    if active_character_id.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.store_records.account_record.fields.active_character_id.namespace must match canonical_key.character_id.namespace")

    slot_counters = require_mapping(
        account_fields.get("slot_counters"),
        f"{path_label}.store_records.account_record.fields.slot_counters",
    )
    if slot_counters.get("type") != "object":
        fail(f"{path_label}.store_records.account_record.fields.slot_counters.type must be object")
    slot_fields = require_mapping(
        slot_counters.get("fields"),
        f"{path_label}.store_records.account_record.fields.slot_counters.fields",
    )
    for field in (
        "base_character_slots",
        "purchased_character_slots",
        "granted_character_slots",
        "temporary_character_slots",
        "total_character_slots",
        "occupied_character_count",
        "reserved_character_count",
        "available_character_slots",
    ):
        if slot_fields.get(field) != "int":
            fail(f"{path_label}.store_records.account_record.fields.slot_counters.fields.{field} must be int")
    derivation = require_mapping(
        slot_counters.get("derivation"),
        f"{path_label}.store_records.account_record.fields.slot_counters.derivation",
    )
    if derivation.get("total_character_slots") != (
        "base_character_slots + purchased_character_slots + granted_character_slots + temporary_character_slots"
    ):
        fail(f"{path_label}.store_records.account_record slot total derivation must preserve slot provenance")
    if derivation.get("available_character_slots") != (
        "total_character_slots - occupied_character_count - reserved_character_count"
    ):
        fail(f"{path_label}.store_records.account_record slot availability derivation must be total - occupied - reserved")

    character_record = require_mapping(store_records.get("character_record"), f"{path_label}.store_records.character_record")
    character_fields = require_mapping(character_record.get("fields"), f"{path_label}.store_records.character_record.fields")
    character_record_character_id = require_mapping(
        character_fields.get("character_id"),
        f"{path_label}.store_records.character_record.fields.character_id",
    )
    if character_record_character_id.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.store_records.character_record.fields.character_id.namespace must match canonical_key.character_id.namespace")
    character_record_account_id = require_mapping(
        character_fields.get("account_id"),
        f"{path_label}.store_records.character_record.fields.account_id",
    )
    if character_record_account_id.get("namespace") != account_id.get("namespace"):
        fail(f"{path_label}.store_records.character_record.fields.account_id.namespace must match canonical_key.account_id.namespace")

    domain04_state = require_mapping(
        character_fields.get("domain04_character_state"),
        f"{path_label}.store_records.character_record.fields.domain04_character_state",
    )
    if domain04_state.get("semantic_owner_domain") != "04":
        fail(f"{path_label}.store_records.character_record.fields.domain04_character_state.semantic_owner_domain must be \"04\"")
    if domain04_state.get("persistence_owner_domain") != "02":
        fail(f"{path_label}.store_records.character_record.fields.domain04_character_state.persistence_owner_domain must be \"02\"")
    if domain04_state.get("canonical_definition_contract") != "docs/design/classes/domain-02-03-alignment.md":
        fail(
            f"{path_label}.store_records.character_record.fields.domain04_character_state.canonical_definition_contract "
            "must point to docs/design/classes/domain-02-03-alignment.md"
        )

    active_session_record = require_mapping(
        store_records.get("active_session_record"),
        f"{path_label}.store_records.active_session_record",
    )
    session_fields = require_mapping(
        active_session_record.get("fields"),
        f"{path_label}.store_records.active_session_record.fields",
    )
    session_account_id = require_mapping(
        session_fields.get("account_id"),
        f"{path_label}.store_records.active_session_record.fields.account_id",
    )
    if session_account_id.get("namespace") != account_id.get("namespace"):
        fail(f"{path_label}.store_records.active_session_record.fields.account_id.namespace must match canonical_key.account_id.namespace")
    session_character_id = require_mapping(
        session_fields.get("character_id"),
        f"{path_label}.store_records.active_session_record.fields.character_id",
    )
    if session_character_id.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.store_records.active_session_record.fields.character_id.namespace must match canonical_key.character_id.namespace")
    for field in ("active_session_id", "session_epoch", "session_state"):
        if field not in session_fields:
            fail(f"{path_label}.store_records.active_session_record.fields missing {field}")
    session_state = require_mapping(
        session_fields.get("session_state"),
        f"{path_label}.store_records.active_session_record.fields.session_state",
    )
    if session_state.get("values") != ["INACTIVE", "ACTIVE", "EXPIRED"]:
        fail(f"{path_label}.store_records.active_session_record.fields.session_state.values must be INACTIVE, ACTIVE, EXPIRED")
    activation_concurrency = require_mapping(
        active_session_record.get("activation_concurrency"),
        f"{path_label}.store_records.active_session_record.activation_concurrency",
    )
    if activation_concurrency.get("lock_conflict_policy") != "reject_concurrent_activation":
        fail(f"{path_label}.store_records.active_session_record.activation_concurrency.lock_conflict_policy must reject concurrent activation")
    if activation_concurrency.get("stale_session_epoch_policy") != "fail_closed_and_require_reactivation":
        fail(f"{path_label}.store_records.active_session_record.activation_concurrency.stale_session_epoch_policy must fail closed")
    if activation_concurrency.get("activation_allowed_session_states") != ["INACTIVE", "EXPIRED"]:
        fail(f"{path_label}.store_records.active_session_record.activation_allowed_session_states must be INACTIVE and EXPIRED")
    if activation_concurrency.get("activation_rejected_session_states") != ["ACTIVE"]:
        fail(f"{path_label}.store_records.active_session_record.activation_rejected_session_states must be ACTIVE")

    mapping_record = require_mapping(
        store_records.get("mmoprofiles_profile_mapping_record"),
        f"{path_label}.store_records.mmoprofiles_profile_mapping_record",
    )
    if mapping_record.get("primary_key") != "profile_id":
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.primary_key must be profile_id")
    if mapping_record.get("persistence_boundary") != persistence_owner.get("mmoprofiles_profile_mapping_record"):
        fail(
            f"{path_label}.store_records.mmoprofiles_profile_mapping_record.persistence_boundary "
            "must match persistence_owner.mmoprofiles_profile_mapping_record"
        )
    if persistence_owner.get("mmoprofiles_profile_mapping_record") != "domain02_mmoprofiles_profile_mapping_store":
        fail(
            f"{path_label}.persistence_owner.mmoprofiles_profile_mapping_record "
            "must be domain02_mmoprofiles_profile_mapping_store"
        )
    if "mmoprofiles_profile_mapping_record" not in data_scope:
        fail(f"{path_label}.data_scope must include mmoprofiles_profile_mapping_record")
    if not any(
        require_mapping(writer, f"{path_label}.writers[{index}]").get("system") == "Domain02CharacterService"
        and "mmoprofiles_profile_mapping_record"
        in require_list(
            require_mapping(writer, f"{path_label}.writers[{index}]").get("writes"),
            f"{path_label}.writers[{index}].writes",
        )
        for index, writer in enumerate(writers, start=1)
    ):
        fail(f"{path_label}.writers must include Domain02CharacterService writing mmoprofiles_profile_mapping_record")

    mapping_fields = require_mapping(
        mapping_record.get("fields"),
        f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields",
    )
    mapping_profile_id = require_mapping(
        mapping_fields.get("profile_id"),
        f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.profile_id",
    )
    if mapping_profile_id.get("namespace") != profile_id.get("namespace"):
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.profile_id.namespace must match canonical_key.profile_id.namespace")
    if mapping_profile_id.get("technical_key_only") != profile_id.get("technical_key_only"):
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.profile_id.technical_key_only must match canonical_key.profile_id.technical_key_only")
    if mapping_profile_id.get("technical_key_only") != profile_mapping_policy.get("technical_key_only"):
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.profile_id.technical_key_only must match api.profile_mapping_policy.technical_key_only")
    if profile_mapping_policy.get("namespace") != mapping_profile_id.get("namespace"):
        fail(f"{path_label}.api.profile_mapping_policy.namespace must match store_records.mmoprofiles_profile_mapping_record.fields.profile_id.namespace")

    mapping_character_id = require_mapping(
        mapping_fields.get("character_id"),
        f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.character_id",
    )
    if mapping_character_id.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.character_id.namespace must match canonical_key.character_id.namespace")

    mapping_account_id = require_mapping(
        mapping_fields.get("account_id"),
        f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.account_id",
    )
    if mapping_account_id.get("namespace") != account_id.get("namespace"):
        fail(f"{path_label}.store_records.mmoprofiles_profile_mapping_record.fields.account_id.namespace must match canonical_key.account_id.namespace")


def validate_document(document: dict[str, Any], path: Path) -> None:
    path_label = str(path)
    for field in REQUIRED_TOP_LEVEL_FIELDS:
        if field not in document:
            fail(f"{path_label} missing required top-level field {field}")

    if document.get("schema_version") != 2:
        fail(f"{path_label}.schema_version must be 2")

    for field in ("contract_owner", "domain_owner", "semantic_owner", "integration_coordinator"):
        require_non_empty_string(document.get(field), f"{path_label}.{field}")

    require_mapping(document.get("runtime_authority"), f"{path_label}.runtime_authority")
    require_mapping(document.get("persistence_owner"), f"{path_label}.persistence_owner")
    require_list(document.get("data_scope"), f"{path_label}.data_scope")
    require_list(document.get("consumers"), f"{path_label}.consumers")
    require_list(document.get("commands"), f"{path_label}.commands")

    check_allowed_statuses(document, path_label)
    validate_writer_shape(document, path)
    validate_identity_sections(document, path)
    validate_store_records(document, path)


def main(argv: list[str]) -> int:
    targets = [Path(arg) for arg in argv[1:]] or [DEFAULT_TARGET]
    for target in targets:
        document = load(target)
        validate_document(document, target)
        print(f"{target}: ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
