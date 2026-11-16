#!/usr/bin/env python3
"""Validate the domain-02 character scope registry against the store contract."""

from __future__ import annotations

import sys
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REGISTRY = ROOT / "docs/contracts/character-scope-registry.yaml"

REQUIRED_ENTRY_KEYS = {
    "key",
    "scope",
    "owner_domain",
    "source_of_truth",
    "required_for_activation",
}
REQUIRED_RECORD_MODELS = {
    "account_record",
    "character_record",
    "active_session_record",
    "mmoprofiles_profile_mapping_record",
}
EXPECTED_D04_ENTRY_KEYS = {
    "character.identity.race_id",
    "character.identity.class_id",
    "character.identity.subclass_id",
    "character.progression.learned_skill_ids",
    "character.progression.weapon_school_unlock_ids",
    "character.loadout.active_loadout_id",
    "character.loadout.equipped_skill_ids",
    "character.progression.talent_allocations",
    "character.progression.respec_state",
    "character.progression.legacy_technique_ids",
    "character.progression.faith_scar_ids",
}
EXPECTED_D04_ENTRY_METADATA = {
    "semantic_owner_domain": "04",
    "persistence_owner_domain": "02",
    "canonical_definition_contract": "docs/design/classes/domain-02-03-alignment.md",
}
EXPECTED_LIFECYCLE = [
    "CREATING",
    "PLAYABLE",
    "DELETE_PENDING",
    "DELETED",
    "PURGED",
]
EXPECTED_CREATOR = [
    "CREATED",
    "IDENTITY_SELECTED",
    "RACE_SELECTED",
    "CLASS_SELECTED",
    "APPEARANCE_SELECTED",
    "VALIDATING",
    "READY",
]
EXPECTED_SLOT_INPUTS = ["base", "purchased", "granted", "temporary"]
EXPECTED_SCOPES = {"ACCOUNT", "CHARACTER", "PLUGIN_PROFILE", "SESSION", "ITEM_INSTANCE"}
EXPECTED_SOURCE_VALUES = {
    "DOMAIN_01_ACCOUNT_SERVICE",
    "DOMAIN_01_CHARACTER_REGISTRY",
    "DOMAIN_02_CHARACTER_DATABASE",
    "MMOProfiles",
    "MMOCore",
    "MMOInventory",
    "MMOItems",
    "CMI_VAULT",
    "LuckPerms",
    "PetCore",
    "NarrativeDomain",
    "ProfessionDomain",
    "CosmeticDomain",
    "UXDomain",
    "RewardDomain",
}
EXPECTED_REPLACEMENT_IDS = {
    "account_identity",
    "character_identity_lifecycle",
    "mmoprofiles_plugin_profile",
    "character_stage_presentation",
}
REQUIRED_AUTHORITY_FIELDS = {
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


def fail(message: str) -> None:
    print(f"character scope registry check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_yaml(path: Path):
    try:
        return yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as exc:  # pragma: no cover
        fail(f"cannot parse {path}: {exc}")


def require_mapping(value, label: str) -> dict:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def require_list(value, label: str) -> list:
    if not isinstance(value, list) or not value:
        fail(f"{label} must be a non-empty list")
    return value


def require_string(value, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def entry_by_key(entries: list[dict], key: str) -> dict:
    for entry in entries:
        if entry.get("key") == key:
            return entry
    fail(f"missing required entry {key}")


def validate_domain04_entries(entries: list[dict]) -> None:
    for key in sorted(EXPECTED_D04_ENTRY_KEYS):
        entry = require_mapping(entry_by_key(entries, key), key)
        if entry.get("owner_domain") != "02":
            fail(f"{key}.owner_domain must remain \"02\" because Domain 02 persists the state")
        if entry.get("scope") != "CHARACTER":
            fail(f"{key}.scope must be CHARACTER")
        if entry.get("source_of_truth") != "DOMAIN_02_CHARACTER_DATABASE":
            fail(f"{key}.source_of_truth must be DOMAIN_02_CHARACTER_DATABASE")
        for metadata_key, expected_value in EXPECTED_D04_ENTRY_METADATA.items():
            if entry.get(metadata_key) != expected_value:
                fail(f"{key}.{metadata_key} must be {expected_value!r}")


def validate_entry_vocabularies(registry: dict, entries: list[dict]) -> None:
    scopes = set(require_list(registry.get("scope_vocabulary"), "scope_vocabulary"))
    sources = set(require_list(registry.get("source_vocabulary"), "source_vocabulary"))
    if scopes != EXPECTED_SCOPES:
        fail("scope_vocabulary does not match the published domain vocabulary")
    if sources != EXPECTED_SOURCE_VALUES:
        fail("source_vocabulary does not match the published domain vocabulary")

    for index, entry in enumerate(entries, start=1):
        scope = entry.get("scope")
        if scope not in scopes:
            fail(f"entries[{index}].scope={scope!r} is outside scope_vocabulary")
        source = entry.get("source_of_truth")
        if source not in sources:
            fail(f"entries[{index}].source_of_truth={source!r} is outside source_vocabulary")


def validate_baseline(registry: dict, entries: list[dict], contract: dict) -> None:
    baseline = require_mapping(registry.get("domain02_contract_baseline"), "domain02_contract_baseline")
    if baseline.get("lifecycle_states") != EXPECTED_LIFECYCLE:
        fail("domain02_contract_baseline.lifecycle_states does not cover the full lifecycle")
    if baseline.get("creator_states") != EXPECTED_CREATOR:
        fail("domain02_contract_baseline.creator_states does not match creator lifecycle")

    baseline_gate = require_mapping(baseline.get("activation_gate"), "domain02_contract_baseline.activation_gate")
    baseline_slots = require_mapping(baseline.get("slot_categories"), "domain02_contract_baseline.slot_categories")
    if baseline_gate.get("lifecycle_status") != "PLAYABLE" or baseline_gate.get("creator_stage") != "READY":
        fail("domain02_contract_baseline.activation_gate has invalid readiness values")
    if baseline_slots.get("inputs") != EXPECTED_SLOT_INPUTS:
        fail("domain02_contract_baseline.slot_categories must preserve slot provenance")

    vocabularies = require_mapping(contract.get("state_vocabularies"), "store contract state_vocabularies")
    if vocabularies.get("lifecycle_status") != EXPECTED_LIFECYCLE:
        fail("canonical account-character-contract.yaml lifecycle vocabulary drifted")
    if vocabularies.get("creator_stage") != EXPECTED_CREATOR:
        fail("canonical account-character-contract.yaml creator vocabulary drifted")


def validate_petcore_strategy(registry: dict, entries: list[dict]) -> None:
    strategy = require_mapping(registry.get("petcore_cutover_strategy"), "petcore_cutover_strategy")
    if strategy.get("selected_strategy") != "read_through_then_one_time_migration":
        fail("petcore_cutover_strategy.selected_strategy must be read_through_then_one_time_migration")
    if strategy.get("legacy_subject_key") != "MMOProfiles.profileId":
        fail("petcore_cutover_strategy.legacy_subject_key must remain MMOProfiles.profileId")
    if strategy.get("target_subject_key") != "character_id":
        fail("petcore_cutover_strategy.target_subject_key must be character_id")
    phase_0 = require_mapping(strategy.get("phase_0_current"), "petcore_cutover_strategy.phase_0_current")
    phase_1 = require_mapping(strategy.get("phase_1_read_through"), "petcore_cutover_strategy.phase_1_read_through")
    if phase_0.get("canonical_write_key") != "MMOProfiles.profileId" or phase_0.get("character_id_claims") != "rejected":
        fail("PetCore phase-0 declaration must preserve the legacy write key")
    if phase_1.get("canonical_write_key") != "MMOProfiles.profileId":
        fail("PetCore phase-1 declaration must preserve the legacy write key")
    if phase_1.get("character_id_resolution") != "required":
        fail("PetCore phase-1 declaration must require character resolution")
    if phase_1.get("unresolved_mapping_policy") != "block_companion_actions":
        fail("PetCore unresolved mapping policy must block companion actions")
    if phase_1.get("ambiguous_mapping_policy") != "quarantine_and_block":
        fail("PetCore ambiguous mapping policy must quarantine and block")
    if strategy.get("no_dual_authority") is not True:
        fail("petcore_cutover_strategy.no_dual_authority must be true")

    pet_entry = entry_by_key(entries, "petcore.subject.character_id")
    for key, expected in {
        "selected_strategy": "read_through_then_one_time_migration",
        "legacy_subject_key": "MMOProfiles.profileId",
        "target_subject_key": "character_id",
        "canonical_write_key": "MMOProfiles.profileId",
        "unresolved_mapping_policy": "block_companion_actions",
        "ambiguous_mapping_policy": "quarantine_and_block",
    }.items():
        if pet_entry.get(key) != expected:
            fail(f"petcore.subject.character_id.{key} is incompatible with the selected strategy")
    if pet_entry.get("no_dual_authority") is not True:
        fail("petcore.subject.character_id.no_dual_authority must be true")


def validate_replacement_rows(registry: dict) -> None:
    replacement = require_mapping(registry.get("replacement_rows"), "replacement_rows")
    rows = require_list(replacement.get("config_authority_registry"), "replacement_rows.config_authority_registry")
    row_by_id = {}
    for index, row in enumerate(rows, start=1):
        row = require_mapping(row, f"replacement_rows.config_authority_registry[{index}]")
        missing = REQUIRED_AUTHORITY_FIELDS - row.keys()
        if missing:
            fail(f"replacement authority row {index} missing fields: {', '.join(sorted(missing))}")
        row_id = require_string(row.get("id"), f"replacement authority row {index}.id")
        if row_id in row_by_id:
            fail(f"replacement authority rows duplicate id {row_id}")
        row_by_id[row_id] = row
    if set(row_by_id) != EXPECTED_REPLACEMENT_IDS:
        fail("replacement authority rows must contain exactly the four domain-02 entries")

    authority_registry_path = ROOT / "docs/config_authority_registry.yaml"
    authority_registry = require_mapping(load_yaml(authority_registry_path), str(authority_registry_path))
    actual_rows = {
        row.get("id"): row
        for row in require_list(authority_registry.get("contracts"), "config_authority_registry.contracts")
        if isinstance(row, dict) and row.get("id") in EXPECTED_REPLACEMENT_IDS
    }
    if set(actual_rows) != EXPECTED_REPLACEMENT_IDS:
        fail("config_authority_registry.yaml is missing one or more replacement entries")
    for row_id in sorted(EXPECTED_REPLACEMENT_IDS):
        if actual_rows[row_id] != row_by_id[row_id]:
            fail(f"config_authority_registry.yaml entry {row_id} drifted from canonical replacement row")


def main() -> None:
    registry_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_REGISTRY
    registry = require_mapping(load_yaml(registry_path), str(registry_path))

    entries = require_list(registry.get("entries"), "entries")
    validate_entry_vocabularies(registry, entries)
    seen_keys: set[str] = set()
    for index, entry in enumerate(entries, start=1):
        entry = require_mapping(entry, f"entries[{index}]")
        missing = REQUIRED_ENTRY_KEYS - entry.keys()
        if missing:
            fail(f"entries[{index}] missing keys: {', '.join(sorted(missing))}")
        key = entry["key"]
        if key in seen_keys:
            fail(f"duplicate entry key: {key}")
        seen_keys.add(key)
        if not isinstance(entry["required_for_activation"], bool):
            fail(f"{key}.required_for_activation must be boolean")

    validate_domain04_entries(entries)

    lifecycle_entry = entry_by_key(entries, "character.lifecycle_status")
    if lifecycle_entry.get("allowed_values") != EXPECTED_LIFECYCLE:
        fail("character.lifecycle_status allowed_values do not match domain-02 lifecycle")

    creator_entry = entry_by_key(entries, "character.creator_stage")
    if creator_entry.get("allowed_values") != EXPECTED_CREATOR:
        fail("character.creator_stage allowed_values do not match domain-02 creator lifecycle")

    baseline = require_mapping(registry.get("domain02_contract_baseline"), "domain02_contract_baseline")
    activation_gate = require_mapping(baseline.get("activation_gate"), "domain02_contract_baseline.activation_gate")
    if activation_gate.get("lifecycle_status") != "PLAYABLE":
        fail("activation_gate.lifecycle_status must be PLAYABLE")
    if activation_gate.get("creator_stage") != "READY":
        fail("activation_gate.creator_stage must be READY")
    if activation_gate.get("required_flags") != [
        "all_required_profiles_loaded",
        "schema_version_supported",
        "session_lock_acquired",
        "no_concurrent_activation",
        "session_epoch_current",
    ]:
        fail("activation_gate.required_flags do not match activation guard contract")
    if activation_gate.get("failure_mode") != "fail_closed":
        fail("activation_gate.failure_mode must be fail_closed")

    slot_categories = require_mapping(baseline.get("slot_categories"), "domain02_contract_baseline.slot_categories")
    if slot_categories.get("inputs") != EXPECTED_SLOT_INPUTS:
        fail("slot_categories.inputs must preserve slot provenance")
    if slot_categories.get("reserved") != ["CREATING"]:
        fail("slot_categories.reserved must map only CREATING")
    if slot_categories.get("occupied") != ["PLAYABLE", "DELETE_PENDING"]:
        fail("slot_categories.occupied must map PLAYABLE and DELETE_PENDING")
    if slot_categories.get("released") != ["DELETED", "PURGED"]:
        fail("slot_categories.released must map DELETED and PURGED")
    derived_counts = require_mapping(slot_categories.get("derived_counts"), "slot_categories.derived_counts")
    if derived_counts.get("total_character_slots") != [
        "account.character_slots.base",
        "account.character_slots.purchased",
        "account.character_slots.granted",
        "account.character_slots.temporary",
    ]:
        fail("slot_categories.derived_counts.total_character_slots does not match slot provenance")
    available_formula = require_mapping(
        derived_counts.get("available_character_slots"),
        "slot_categories.derived_counts.available_character_slots",
    ).get("formula")
    if available_formula != "total_character_slots - occupied_character_count - reserved_character_count":
        fail("slot_categories available formula must be total - occupied - reserved")

    required_slot_keys = {
        "account.character_slots.base",
        "account.character_slots.purchased",
        "account.character_slots.granted",
        "account.character_slots.temporary",
        "account.character_slots.occupied_character_count",
        "account.character_slots.reserved_character_count",
        "account.character_slots.total_character_slots",
        "account.character_slots.available_character_slots",
    }
    for key in sorted(required_slot_keys):
        entry_by_key(entries, key)

    occupied_entry = entry_by_key(entries, "account.character_slots.occupied_character_count")
    occupied_status = require_mapping(
        require_mapping(occupied_entry.get("computed_from"), f"{occupied_entry['key']}.computed_from").get(
            "character.lifecycle_status"
        ),
        f"{occupied_entry['key']}.computed_from.character.lifecycle_status",
    )
    if occupied_status.get("occupies_slot") != ["PLAYABLE", "DELETE_PENDING"]:
        fail("occupied_character_count must be computed from PLAYABLE and DELETE_PENDING")
    if occupied_status.get("reserved_slot") != ["CREATING"]:
        fail("occupied_character_count reserved_slot must contain CREATING")
    if occupied_status.get("releases_slot") != ["DELETED", "PURGED"]:
        fail("occupied_character_count releases_slot must contain DELETED and PURGED")

    lifecycle_entry = entry_by_key(entries, "character.lifecycle_status")
    if lifecycle_entry.get("allowed_values") != EXPECTED_LIFECYCLE:
        fail("character.lifecycle_status allowed_values do not match lifecycle vocabulary")
    if lifecycle_entry.get("slot_occupancy") != {
        "CREATING": "reserved",
        "PLAYABLE": "occupied",
        "DELETE_PENDING": "occupied",
        "DELETED": "released",
        "PURGED": "released",
    }:
        fail("character.lifecycle_status.slot_occupancy does not cover every lifecycle state")

    reserved_entry = entry_by_key(entries, "account.character_slots.reserved_character_count")
    reserved_status = require_mapping(
        require_mapping(reserved_entry.get("computed_from"), f"{reserved_entry['key']}.computed_from").get(
            "character.lifecycle_status"
        ),
        f"{reserved_entry['key']}.computed_from.character.lifecycle_status",
    )
    if reserved_status.get("reserved_slot") != ["CREATING"]:
        fail("reserved_character_count must be computed from CREATING")

    total_entry = entry_by_key(entries, "account.character_slots.total_character_slots")
    total_from = require_mapping(total_entry.get("computed_from"), f"{total_entry['key']}.computed_from")
    if total_from.get("sum_of") != [
        "account.character_slots.base",
        "account.character_slots.purchased",
        "account.character_slots.granted",
        "account.character_slots.temporary",
    ]:
        fail("total_character_slots must be derived from base, purchased, granted, temporary")

    available_entry = entry_by_key(entries, "account.character_slots.available_character_slots")
    available_from = require_mapping(available_entry.get("computed_from"), f"{available_entry['key']}.computed_from")
    if available_from.get("formula") != (
        "account.character_slots.total_character_slots - "
        "account.character_slots.occupied_character_count - "
        "account.character_slots.reserved_character_count"
    ):
        fail("available_character_slots formula must be total - occupied - reserved")

    account_active = entry_by_key(entries, "account.active_character_id")
    account_active_validation = require_mapping(
        account_active.get("validation"),
        "account.active_character_id.validation",
    )
    for field in (
        "character_must_belong_to_account",
        "session_lock_required",
        "lifecycle_status_must_be_playable",
        "creator_stage_must_be_ready",
        "schema_version_must_be_supported",
        "required_profiles_must_be_loaded",
        "concurrent_activation_rejected",
        "stale_session_epoch_rejected",
    ):
        if account_active_validation.get(field) is not True:
            fail(f"account.active_character_id.validation.{field} must be true")

    active_session_id = entry_by_key(entries, "character.runtime.active_session_id")
    active_session_validation = require_mapping(
        active_session_id.get("validation"),
        "character.runtime.active_session_id.validation",
    )
    if active_session_validation.get("double_login_rejected_or_fenced") is not True:
        fail("character.runtime.active_session_id.validation.double_login_rejected_or_fenced must be true")
    if active_session_validation.get("active_session_blocks_parallel_activation") is not True:
        fail("character.runtime.active_session_id.validation.active_session_blocks_parallel_activation must be true")

    session_epoch = entry_by_key(entries, "character.runtime.session_epoch")
    session_epoch_mutable = require_mapping(
        session_epoch.get("mutable"),
        "character.runtime.session_epoch.mutable",
    )
    if session_epoch_mutable.get("monotonic_increment") is not True:
        fail("character.runtime.session_epoch.mutable.monotonic_increment must be true")
    session_epoch_validation = require_mapping(
        session_epoch.get("validation"),
        "character.runtime.session_epoch.validation",
    )
    if session_epoch_validation.get("stale_epoch_rejected") is not True:
        fail("character.runtime.session_epoch.validation.stale_epoch_rejected must be true")
    if session_epoch_validation.get("epoch_match_required_for_activation") is not True:
        fail("character.runtime.session_epoch.validation.epoch_match_required_for_activation must be true")

    contract_ref = registry.get("record_models_contract")
    if not isinstance(contract_ref, str) or not contract_ref.strip():
        fail("record_models_contract must be a non-empty path")
    contract_path = ROOT / contract_ref if not Path(contract_ref).is_absolute() else Path(contract_ref)
    if not contract_path.exists():
        fail(f"record_models_contract path does not exist: {contract_path}")

    contract = require_mapping(load_yaml(contract_path), str(contract_path))
    store_records = require_mapping(contract.get("store_records"), "store_records")
    missing_records = REQUIRED_RECORD_MODELS - store_records.keys()
    if missing_records:
        fail(f"store_records missing: {', '.join(sorted(missing_records))}")
    canonical_key = require_mapping(contract.get("canonical_key"), "canonical_key")
    character_key = require_mapping(canonical_key.get("character_id"), "canonical_key.character_id")
    account_key = require_mapping(canonical_key.get("account_id"), "canonical_key.account_id")
    profile_key = require_mapping(canonical_key.get("profile_id"), "canonical_key.profile_id")

    state_vocabularies = require_mapping(contract.get("state_vocabularies"), "state_vocabularies")
    if state_vocabularies.get("lifecycle_status") != EXPECTED_LIFECYCLE:
        fail("store contract lifecycle vocabulary does not match registry")
    if state_vocabularies.get("creator_stage") != EXPECTED_CREATOR:
        fail("store contract creator vocabulary does not match registry")

    account_record = require_mapping(store_records.get("account_record"), "store_records.account_record")
    account_fields = require_mapping(account_record.get("fields"), "store_records.account_record.fields")
    slot_counters = require_mapping(account_fields.get("slot_counters"), "account_record.fields.slot_counters")
    slot_counter_fields = require_mapping(slot_counters.get("fields"), "slot_counters.fields")
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
        if slot_counter_fields.get(field) != "int":
            fail(f"slot_counters.fields.{field} must be int")
    slot_derivation = require_mapping(slot_counters.get("derivation"), "slot_counters.derivation")
    if slot_derivation.get("total_character_slots") != (
        "base_character_slots + purchased_character_slots + granted_character_slots + temporary_character_slots"
    ):
        fail("slot_counters.derivation.total_character_slots does not preserve provenance")
    if slot_derivation.get("available_character_slots") != (
        "total_character_slots - occupied_character_count - reserved_character_count"
    ):
        fail("slot_counters.derivation.available_character_slots must be total - occupied - reserved")

    active_session_record = require_mapping(store_records.get("active_session_record"), "store_records.active_session_record")
    active_session_fields = require_mapping(active_session_record.get("fields"), "active_session_record.fields")
    for field in ("active_session_id", "session_epoch", "session_state"):
        if field not in active_session_fields:
            fail(f"active_session_record.fields missing {field}")
    session_state = require_mapping(active_session_fields.get("session_state"), "active_session_record.fields.session_state")
    if session_state.get("values") != ["INACTIVE", "ACTIVE", "EXPIRED"]:
        fail("active_session_record.fields.session_state.values must be INACTIVE, ACTIVE, EXPIRED")
    activation_concurrency = require_mapping(
        active_session_record.get("activation_concurrency"),
        "active_session_record.activation_concurrency",
    )
    if activation_concurrency.get("lock_conflict_policy") != "reject_concurrent_activation":
        fail("active_session_record.activation_concurrency.lock_conflict_policy must reject concurrent activation")
    if activation_concurrency.get("stale_session_epoch_policy") != "fail_closed_and_require_reactivation":
        fail("active_session_record.activation_concurrency.stale_session_epoch_policy must fail closed")
    if activation_concurrency.get("activation_allowed_session_states") != ["INACTIVE", "EXPIRED"]:
        fail("active_session_record.activation_allowed_session_states must be INACTIVE and EXPIRED")
    if activation_concurrency.get("activation_rejected_session_states") != ["ACTIVE"]:
        fail("active_session_record.activation_rejected_session_states must be ACTIVE")

    mapping_record = require_mapping(
        store_records.get("mmoprofiles_profile_mapping_record"),
        "store_records.mmoprofiles_profile_mapping_record",
    )
    if mapping_record.get("primary_key") != "profile_id":
        fail("mmoprofiles_profile_mapping_record.primary_key must be profile_id")
    if mapping_record.get("persistence_boundary") != "domain02_mmoprofiles_profile_mapping_store":
        fail("mmoprofiles_profile_mapping_record.persistence_boundary must be domain02_mmoprofiles_profile_mapping_store")
    if mapping_record.get("foreign_keys") != ["account_id", "character_id"]:
        fail("mmoprofiles_profile_mapping_record.foreign_keys must include account_id and character_id")
    mapping_invariants = require_mapping(mapping_record.get("mapping_invariants"), "mapping_record.mapping_invariants")
    for field in ("one_active_profile_per_character", "one_character_per_active_profile", "profile_id_is_technical_key_only"):
        if mapping_invariants.get(field) is not True:
            fail(f"mapping_record.mapping_invariants.{field} must be true")
    for field in ("unresolved_mapping_policy", "ambiguous_mapping_policy"):
        if mapping_invariants.get(field) != "fail_closed":
            fail(f"mapping_record.mapping_invariants.{field} must be fail_closed")
    mapping_fields = require_mapping(mapping_record.get("fields"), "mapping_record.fields")
    if set(mapping_fields) != {
        "profile_id",
        "character_id",
        "account_id",
        "binding_status",
        "lifecycle_status_snapshot",
        "mapping_schema_version",
    }:
        fail("mmoprofiles_profile_mapping_record.fields must preserve the durable mapping shape")
    profile_field = require_mapping(mapping_fields.get("profile_id"), "mapping_record.fields.profile_id")
    if profile_field.get("technical_key_only") is not True:
        fail("mapping_record.fields.profile_id.technical_key_only must be true")
    if profile_field.get("namespace") != profile_key.get("namespace"):
        fail("mapping_record.fields.profile_id namespace drifted")
    character_field = require_mapping(mapping_fields.get("character_id"), "mapping_record.fields.character_id")
    if character_field.get("namespace") != character_key.get("namespace"):
        fail("mapping_record.fields.character_id namespace drifted")
    account_field = require_mapping(mapping_fields.get("account_id"), "mapping_record.fields.account_id")
    if account_field.get("namespace") != account_key.get("namespace"):
        fail("mapping_record.fields.account_id namespace drifted")
    if require_mapping(mapping_fields.get("binding_status"), "mapping_record.fields.binding_status").get("values_ref") != "state_vocabularies.profile_binding_status":
        fail("mapping_record.fields.binding_status must use the canonical vocabulary")
    if require_mapping(mapping_fields.get("lifecycle_status_snapshot"), "mapping_record.fields.lifecycle_status_snapshot").get("values_ref") != "state_vocabularies.lifecycle_status":
        fail("mapping_record.fields.lifecycle_status_snapshot must use the lifecycle vocabulary")
    if require_mapping(mapping_fields.get("mapping_schema_version"), "mapping_record.fields.mapping_schema_version").get("type") != "int":
        fail("mapping_record.fields.mapping_schema_version.type must be int")

    validate_petcore_strategy(registry, entries)
    validate_replacement_rows(registry)

    validate_baseline(registry, entries, contract)
    print("character-scope-registry: static declarations ok (runtime not evaluated)")


if __name__ == "__main__":
    main()
