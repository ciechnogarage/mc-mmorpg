#!/usr/bin/env python3
"""Minimal parser/shape gate for domain-01 shell contracts."""

from __future__ import annotations

from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    print(f"domain-01 shell contract check failed: {message}", file=sys.stderr)
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


def require_non_empty_string(value, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def require_non_empty_list(value, label: str) -> list:
    if not isinstance(value, list) or not value:
        fail(f"{label} must be a non-empty list")
    return value


def resolve_contract_ref(ref: str) -> Path:
    path = Path(ref)
    return path if path.is_absolute() else ROOT / path


def check_legacy_contract_header(document: dict, path_label: str) -> dict:
    contract = require_mapping(document.get("contract"), f"{path_label}.contract")
    for key in ("id", "owner_domain", "semantic_owner", "contract_status", "runtime_status", "release_status"):
        require_non_empty_string(contract.get(key), f"{path_label}.contract.{key}")
    if not isinstance(contract.get("version"), int) or contract["version"] < 1:
        fail(f"{path_label}.contract.version must be a positive integer")
    require_non_empty_list(contract.get("purpose"), f"{path_label}.contract.purpose")
    require_non_empty_list(contract.get("dependencies"), f"{path_label}.contract.dependencies")
    return contract


def check_v2_contract_header(document: dict, path_label: str) -> dict:
    if document.get("schema_version") != 2:
        fail(f"{path_label}.schema_version must be 2")
    for key in (
        "contract_id",
        "contract_status",
        "runtime_status",
        "release_status",
        "contract_owner",
        "domain_owner",
        "semantic_owner",
        "integration_coordinator",
    ):
        require_non_empty_string(document.get(key), f"{path_label}.{key}")
    if not isinstance(document.get("contract_version"), int) or document["contract_version"] < 1:
        fail(f"{path_label}.contract_version must be a positive integer")
    require_non_empty_list(document.get("purpose"), f"{path_label}.purpose")
    require_non_empty_list(document.get("dependencies"), f"{path_label}.dependencies")
    require_mapping(document.get("runtime_authority"), f"{path_label}.runtime_authority")
    require_mapping(document.get("persistence_owner"), f"{path_label}.persistence_owner")
    require_non_empty_list(document.get("writers"), f"{path_label}.writers")
    require_non_empty_list(document.get("consumers"), f"{path_label}.consumers")
    require_non_empty_list(document.get("pdc_keys"), f"{path_label}.pdc_keys")
    return document


def check_reward_transaction_shell(path_label: str, document: dict) -> None:
    contract = check_legacy_contract_header(document, path_label)
    if contract["id"] != "reward-transaction-coordinator-contract-v1":
        fail(f"{path_label}.contract.id unexpected value")

    transaction_identity = require_mapping(document.get("transaction_identity"), f"{path_label}.transaction_identity")
    require_mapping(transaction_identity.get("transaction_id"), f"{path_label}.transaction_identity.transaction_id")
    require_mapping(transaction_identity.get("idempotency_key"), f"{path_label}.transaction_identity.idempotency_key")
    require_mapping(transaction_identity.get("owner_subject_key"), f"{path_label}.transaction_identity.owner_subject_key")

    transaction_scope = require_mapping(document.get("transaction_scope"), f"{path_label}.transaction_scope")
    flows = require_non_empty_list(transaction_scope.get("supported_flows"), f"{path_label}.transaction_scope.supported_flows")
    expected_flows = {"reward_grant", "craft_commit", "trade_settlement"}
    if set(flows) != expected_flows:
        fail(f"{path_label}.transaction_scope.supported_flows must equal {sorted(expected_flows)}")

    state_machine = require_mapping(document.get("state_machine"), f"{path_label}.state_machine")
    statuses = require_non_empty_list(state_machine.get("statuses"), f"{path_label}.state_machine.statuses")
    expected_statuses = ["RECEIVED", "VALIDATED", "RESERVED", "APPLYING", "COMMITTED", "COMPENSATED", "MANUAL_REVIEW"]
    if statuses != expected_statuses:
        fail(f"{path_label}.state_machine.statuses must match the agreed sequence exactly")

    delivery_contract = require_mapping(document.get("delivery_contract"), f"{path_label}.delivery_contract")
    if delivery_contract.get("delivery_semantics") != "at_least_once":
        fail(f"{path_label}.delivery_contract.delivery_semantics must be 'at_least_once'")
    if delivery_contract.get("consumer_requirement") != "idempotent":
        fail(f"{path_label}.delivery_contract.consumer_requirement must be 'idempotent'")
    if delivery_contract.get("exactly_once_claim") != "forbidden":
        fail(f"{path_label}.delivery_contract.exactly_once_claim must be 'forbidden'")

    migration_requirements = require_mapping(document.get("migration_requirements"), f"{path_label}.migration_requirements")
    claims = require_non_empty_list(migration_requirements.get("relevant_legacy_claims"), f"{path_label}.migration_requirements.relevant_legacy_claims")
    if set(claims) != {"loot_and_rewards", "currency_and_economy"}:
        fail(f"{path_label}.migration_requirements.relevant_legacy_claims must include loot_and_rewards and currency_and_economy")


def check_economy_currency_shell(path_label: str, document: dict) -> None:
    contract = check_legacy_contract_header(document, path_label)
    if contract["id"] != "economy-currency-scope-contract-v1":
        fail(f"{path_label}.contract.id unexpected value")

    vocabulary = require_non_empty_list(document.get("currency_scope_vocabulary"), f"{path_label}.currency_scope_vocabulary")
    expected_vocabulary = ["ACCOUNT", "CHARACTER", "SEASONAL_CHARACTER", "CONTENT_TOKEN", "FACTION", "PVP"]
    if vocabulary != expected_vocabulary:
        fail(f"{path_label}.currency_scope_vocabulary must match the agreed scope vocabulary exactly")

    runtime_authority = require_mapping(document.get("runtime_authority"), f"{path_label}.runtime_authority")
    if runtime_authority.get("provider_bridge") != "Vault":
        fail(f"{path_label}.runtime_authority.provider_bridge must be 'Vault'")
    if runtime_authority.get("current_runtime_provider") != "CMI":
        fail(f"{path_label}.runtime_authority.current_runtime_provider must be 'CMI'")

    invariants = require_mapping(document.get("invariants"), f"{path_label}.invariants")
    if invariants.get("Vault_is_bridge_not_semantic_owner") is not True:
        fail(f"{path_label}.invariants.Vault_is_bridge_not_semantic_owner must be true")
    if invariants.get("CMI_is_runtime_provider_not_policy_owner") is not True:
        fail(f"{path_label}.invariants.CMI_is_runtime_provider_not_policy_owner must be true")

    integration = require_mapping(document.get("integration_with_transactions"), f"{path_label}.integration_with_transactions")
    require_non_empty_string(integration.get("transaction_contract_ref"), f"{path_label}.integration_with_transactions.transaction_contract_ref")
    fields = require_non_empty_list(integration.get("required_context_fields"), f"{path_label}.integration_with_transactions.required_context_fields")
    if "currency_scope" not in fields:
        fail(f"{path_label}.integration_with_transactions.required_context_fields must include currency_scope")

    migration_requirements = require_mapping(document.get("migration_requirements"), f"{path_label}.migration_requirements")
    blockers = require_non_empty_list(migration_requirements.get("cutover_blockers"), f"{path_label}.migration_requirements.cutover_blockers")
    if "ECON-SCOPE-001_unresolved" not in blockers:
        fail(f"{path_label}.migration_requirements.cutover_blockers must include ECON-SCOPE-001_unresolved")


def check_reputation_standing_shell(path_label: str, document: dict) -> None:
    contract = check_v2_contract_header(document, path_label)
    if contract["contract_id"] != "reputation-standing-contract":
        fail(f"{path_label}.contract_id unexpected value")

    canonical_key = require_mapping(document.get("canonical_key"), f"{path_label}.canonical_key")
    record_id = require_mapping(canonical_key.get("reputation_record_id"), f"{path_label}.canonical_key.reputation_record_id")
    components = require_non_empty_list(record_id.get("composite_of"), f"{path_label}.canonical_key.reputation_record_id.composite_of")
    if components != ["character_id", "organization_category", "organization_id"]:
        fail(f"{path_label}.canonical_key.reputation_record_id.composite_of must match the agreed durable reputation key")

    character_id = require_mapping(canonical_key.get("character_id"), f"{path_label}.canonical_key.character_id")
    if character_id.get("namespace") != "mcmmorpg:character:mmoprofiles-profile:v1":
        fail(f"{path_label}.canonical_key.character_id.namespace must match the approved v1 MMOProfiles namespace")
    if character_id.get("v1_value_equivalent_to_mmoprofiles_profile_uuid") is not True:
        fail(f"{path_label}.canonical_key.character_id.v1_value_equivalent_to_mmoprofiles_profile_uuid must be true")
    contract_ref = require_non_empty_string(character_id.get("contract_ref"), f"{path_label}.canonical_key.character_id.contract_ref")

    identity_namespace = require_mapping(document.get("identity_namespace"), f"{path_label}.identity_namespace")
    if identity_namespace.get("character") != "mcmmorpg:character:mmoprofiles-profile:v1":
        fail(f"{path_label}.identity_namespace.character must match the approved v1 MMOProfiles namespace")

    account_character = require_mapping(load(resolve_contract_ref(contract_ref)), contract_ref)
    upstream_character = require_mapping(
        require_mapping(account_character.get("canonical_key"), f"{contract_ref}.canonical_key").get("character_id"),
        f"{contract_ref}.canonical_key.character_id",
    )
    if upstream_character.get("namespace") != character_id.get("namespace"):
        fail(f"{path_label}.canonical_key.character_id.namespace must match {contract_ref} canonical character namespace")
    if upstream_character.get("value_equal_to_profile_id_in_v1") is not True:
        fail(f"{contract_ref}.canonical_key.character_id.value_equal_to_profile_id_in_v1 must be true for v1 reputation consistency")

    api = require_mapping(document.get("api"), f"{path_label}.api")
    boundary_policy = require_mapping(api.get("boundary_policy"), f"{path_label}.api.boundary_policy")
    if boundary_policy.get("reputation_is_not_membership") is not True:
        fail(f"{path_label}.api.boundary_policy.reputation_is_not_membership must be true")
    if boundary_policy.get("reputation_is_not_stored_rank") is not True:
        fail(f"{path_label}.api.boundary_policy.reputation_is_not_stored_rank must be true")
    if boundary_policy.get("reputation_is_not_effective_rank") is not True:
        fail(f"{path_label}.api.boundary_policy.reputation_is_not_effective_rank must be true")
    if boundary_policy.get("reputation_is_not_trust_or_trust_debt") is not True:
        fail(f"{path_label}.api.boundary_policy.reputation_is_not_trust_or_trust_debt must be true")
    if boundary_policy.get("ui_badges_colors_and_tooltips_are_projection_only") is not True:
        fail(f"{path_label}.api.boundary_policy.ui_badges_colors_and_tooltips_are_projection_only must be true")

    authority_resolution = require_mapping(api.get("authority_resolution_policy"), f"{path_label}.api.authority_resolution_policy")
    forbidden_placeholders = require_non_empty_list(authority_resolution.get("forbidden_placeholder_authorities"), f"{path_label}.api.authority_resolution_policy.forbidden_placeholder_authorities")
    for required_placeholder in ("menu_projection", "scoreboard_tag", "display_name"):
        if required_placeholder not in forbidden_placeholders:
            fail(f"{path_label}.api.authority_resolution_policy.forbidden_placeholder_authorities must include {required_placeholder}")

    updates = require_mapping(document.get("state_vocabularies"), f"{path_label}.state_vocabularies")
    sources = require_non_empty_list(updates.get("delta_reason"), f"{path_label}.state_vocabularies.delta_reason")
    expected_sources = {
        "REPUTATION_QUEST",
        "FACTION_EVENT",
        "REGIONAL_ACTIVITY",
        "FACTION_DUNGEON",
        "DELIVERY",
        "ADMIN_MIGRATION",
    }
    if set(sources) != expected_sources:
        fail(f"{path_label}.state_vocabularies.delta_reason must match the agreed reputation source set")

    migration_requirements = require_mapping(document.get("migration_requirements"), f"{path_label}.migration_requirements")
    blockers = require_non_empty_list(migration_requirements.get("cutover_blockers"), f"{path_label}.migration_requirements.cutover_blockers")
    if not any("runtime_authority_not_implemented" in blocker for blocker in blockers):
        fail(f"{path_label}.migration_requirements.cutover_blockers must include a runtime authority not implemented blocker")
    if not any("legacy_reputation_sources_not_mapped_to_business_references" in blocker for blocker in blockers):
        fail(f"{path_label}.migration_requirements.cutover_blockers must include a legacy reputation sources mapping blocker")


def main(argv: list[str]) -> None:
    targets = argv or [
        "docs/contracts/reward-transaction-contract.yaml",
        "docs/contracts/economy-currency-contract.yaml",
        "docs/contracts/reputation-standing-contract.yaml",
    ]

    for target in targets:
        path = Path(target)
        resolved = path if path.is_absolute() else ROOT / path
        document = require_mapping(load(resolved), target)
        target_name = path.name
        if target_name == "contract.yaml" and document.get("schema_version") == 2:
            target_name = f"{document.get('contract_id', '')}.yaml"

        if target_name == "reward-transaction-contract.yaml":
            check_reward_transaction_shell(target, document)
        elif target_name == "economy-currency-contract.yaml":
            check_economy_currency_shell(target, document)
        elif target_name in {"reputation-standing-contract.yaml", "reputation-standing-contract"}:
            check_reputation_standing_shell(target, document)
        else:
            fail(f"unsupported shell contract target: {target}")
        print(f"{target}: ok")


if __name__ == "__main__":
    main(sys.argv[1:])
