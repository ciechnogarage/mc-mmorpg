#!/usr/bin/env python3
"""Validate seeded Domain 00 product-governance YAML artifacts."""

from __future__ import annotations

from pathlib import Path
import sys
from typing import Any

import yaml


ROOT = Path(__file__).resolve().parents[1]
PRODUCT_DIR = ROOT / "docs" / "product"
FILES = {
    "decisions": PRODUCT_DIR / "00-decision-registry.yaml",
    "ownership": PRODUCT_DIR / "00-domain-ownership.yaml",
    "questions": PRODUCT_DIR / "00-open-questions.yaml",
    "intake": PRODUCT_DIR / "00-current-state-intake.yaml",
    "conflicts": PRODUCT_DIR / "00-conflict-registry.yaml",
    "path_registry": PRODUCT_DIR / "00-first-class-path-registry.yaml",
    "profession_catalog": ROOT / "docs" / "contracts" / "profession-list.yaml",
}
OPEN_CONFLICT_STATUSES = {
    "open",
    "under_review",
    "awaiting_owner",
    "awaiting_evidence",
}
EXPECTED_FIRST_CLASS_PATH_IDS = {
    "path.islands",
    "path.dungeons",
    "path.pvp",
    "path.guild",
    "path.pets",
    "path.profession.miner",
    "path.profession.lumberjack",
    "path.profession.harvester",
    "path.profession.blacksmith",
    "path.profession.armorer",
    "path.profession.alchemist",
    "path.profession.artificer",
    "path.profession.cook",
}
EXPECTED_FAMILY_IDS = {
    "family.pve",
    "family.competitive",
    "family.social",
    "family.companions",
    "family.professions",
}
EXPECTED_CANDIDATE_PATH_IDS = {"candidate.builder_architect"}
EXPECTED_SUPPORTING_CAPABILITY_IDS = {
    "support.trade_market",
    "support.collection",
    "support.hybrid_building",
    "support.quests_narrative",
    "support.classes_builds",
    "support.itemization",
    "support.economy",
    "support.social_basics",
    "support.public_events",
    "support.raids",
}
PROGRESSION_FANTASY_FIELDS = {
    "player_promise",
    "first_session_entry",
    "mastery_horizon",
    "lasting_accomplishment",
    "ecosystem_role",
    "minecraft_world_expression",
}


def fail(message: str) -> "NoReturn":
    print(f"domain 00 product YAML check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_yaml(path: Path) -> dict[str, Any]:
    try:
        document = yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as error:  # pragma: no cover - surfaced to CLI
        fail(f"cannot parse {path.relative_to(ROOT)}: {error}")
    if not isinstance(document, dict):
        fail(f"{path.relative_to(ROOT)} must contain a top-level mapping")
    return document


def expect_mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        fail(f"{label} must be a mapping")
    return value


def expect_list(value: Any, label: str) -> list[Any]:
    if not isinstance(value, list):
        fail(f"{label} must be a list")
    return value


def require_ids(records: list[dict[str, Any]], key: str, label: str) -> dict[str, dict[str, Any]]:
    indexed: dict[str, dict[str, Any]] = {}
    for index, record in enumerate(records, start=1):
        if not isinstance(record, dict):
            fail(f"{label} record {index} must be a mapping")
        record_id = record.get(key)
        if not isinstance(record_id, str) or not record_id:
            fail(f"{label} record {index} must include non-empty {key}")
        if record_id in indexed:
            fail(f"duplicate {key}: {record_id}")
        indexed[record_id] = record
    return indexed


def ensure_string_list(value: Any, label: str) -> list[str]:
    items = expect_list(value, label)
    for item in items:
        if not isinstance(item, str) or not item:
            fail(f"{label} must contain only non-empty strings")
    return items


def ensure_unique_strings(value: Any, label: str) -> list[str]:
    items = ensure_string_list(value, label)
    if len(items) != len(set(items)):
        fail(f"{label} must not contain duplicates")
    return items


def ensure_refs_exist(refs: list[str], known: set[str], label: str) -> None:
    for ref in refs:
        if ref not in known:
            fail(f"{label} references unknown id {ref}")


def ensure_non_empty_string(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        fail(f"{label} must be a non-empty string")
    return value


def keeps_claims_quarantined(record: dict[str, Any]) -> bool:
    return (
        record.get("status") in OPEN_CONFLICT_STATUSES
        and record.get("blocking") != "none"
    )


def validate_path_registry(
    registry: dict[str, Any],
    profession_catalog: dict[str, Any],
    decision_ids: set[str],
    domain_ids: set[str],
) -> None:
    meta = expect_mapping(registry.get("meta"), "path registry meta")
    if meta.get("status") != "active":
        fail("path registry must be active")
    if meta.get("registry_revision") != "path-registry-r1":
        fail("path registry must declare registry_revision path-registry-r1")
    if meta.get("tier_0_norm_revision") != "tier0-r2":
        fail("path registry must bind to tier0-r2")
    authority_decision_id = ensure_non_empty_string(
        meta.get("authority_decision_id"),
        "path registry authority_decision_id",
    )
    ensure_refs_exist(
        [authority_decision_id],
        decision_ids,
        "path registry authority_decision_id",
    )

    primary_segment = expect_mapping(
        registry.get("primary_segment"),
        "path registry primary_segment",
    )
    primary_segment_id = ensure_non_empty_string(
        primary_segment.get("segment_id"),
        "path registry primary_segment.segment_id",
    )

    vocabulary = expect_mapping(
        registry.get("status_vocabulary"),
        "path registry status_vocabulary",
    )
    scope_statuses = set(
        ensure_unique_strings(
            vocabulary.get("scope_status"),
            "path registry scope_status vocabulary",
        )
    )
    delivery_statuses = set(
        ensure_unique_strings(
            vocabulary.get("delivery_status"),
            "path registry delivery_status vocabulary",
        )
    )
    if scope_statuses != {"approved_first_class", "candidate", "supporting"}:
        fail("path registry scope_status vocabulary is not canonical")
    if "cannot_judge" not in delivery_statuses or "release_ready" not in delivery_statuses:
        fail("path registry delivery_status vocabulary must preserve cannot_judge and release_ready")

    claim_rules = ensure_string_list(
        registry.get("claim_rules"),
        "path registry claim_rules",
    )
    if not any("thirteen" in rule for rule in claim_rules):
        fail("path registry claim rules must define the thirteen-path full-scope claim")
    if not any("cannot_judge" in rule.lower() for rule in claim_rules):
        fail("path registry claim rules must preserve cannot_judge semantics")

    families = require_ids(
        expect_list(registry.get("families"), "path registry families"),
        "family_id",
        "path family",
    )
    if set(families) != EXPECTED_FAMILY_IDS:
        fail("path registry family set is not canonical")
    paths = require_ids(
        expect_list(registry.get("paths"), "path registry paths"),
        "path_id",
        "first-class path",
    )
    if set(paths) != EXPECTED_FIRST_CLASS_PATH_IDS:
        missing = sorted(EXPECTED_FIRST_CLASS_PATH_IDS - set(paths))
        extra = sorted(set(paths) - EXPECTED_FIRST_CLASS_PATH_IDS)
        fail(f"path registry first-class set mismatch; missing={missing}, extra={extra}")

    family_children: list[str] = []
    for family_id, family in families.items():
        if family.get("scope_status") != "approved_first_class":
            fail(f"path family {family_id} must be approved_first_class")
        if family.get("delivery_status") not in delivery_statuses:
            fail(f"path family {family_id} has unknown delivery_status")
        child_ids = ensure_unique_strings(
            family.get("child_path_ids"),
            f"path family {family_id} child_path_ids",
        )
        ensure_refs_exist(child_ids, set(paths), f"path family {family_id} child_path_ids")
        ensure_non_empty_string(family.get("rollup_rule"), f"path family {family_id} rollup_rule")
        family_children.extend(child_ids)
    if len(family_children) != len(set(family_children)):
        fail("a first-class path may belong to only one family")
    if set(family_children) != set(paths):
        fail("every first-class path must be assigned to exactly one family")

    catalog_professions = require_ids(
        expect_list(profession_catalog.get("professions"), "profession catalog professions"),
        "profession_id",
        "profession catalog",
    )
    registry_professions: dict[str, str] = {}
    for path_id, path in paths.items():
        if path.get("scope_status") != "approved_first_class":
            fail(f"first-class path {path_id} must be approved_first_class")
        delivery_status = path.get("delivery_status")
        if delivery_status not in delivery_statuses:
            fail(f"first-class path {path_id} has unknown delivery_status")
        owner_domain = ensure_non_empty_string(
            path.get("product_owner_domain"),
            f"first-class path {path_id} product_owner_domain",
        )
        ensure_refs_exist([owner_domain], domain_ids, f"first-class path {path_id} product_owner_domain")
        ensure_refs_exist(
            ensure_unique_strings(
                path.get("contributing_domains"),
                f"first-class path {path_id} contributing_domains",
            ),
            domain_ids,
            f"first-class path {path_id} contributing_domains",
        )
        segment_ids = ensure_unique_strings(
            path.get("primary_segment_ids"),
            f"first-class path {path_id} primary_segment_ids",
        )
        if segment_ids != [primary_segment_id]:
            fail(f"first-class path {path_id} must serve the canonical primary segment")
        fantasy = expect_mapping(
            path.get("progression_fantasy"),
            f"first-class path {path_id} progression_fantasy",
        )
        missing_fantasy_fields = PROGRESSION_FANTASY_FIELDS - set(fantasy)
        if missing_fantasy_fields:
            fail(
                f"first-class path {path_id} progression_fantasy missing fields: "
                + ", ".join(sorted(missing_fantasy_fields))
            )
        for field in PROGRESSION_FANTASY_FIELDS:
            ensure_non_empty_string(
                fantasy.get(field),
                f"first-class path {path_id} progression_fantasy.{field}",
            )
        ensure_unique_strings(
            path.get("required_surfaces"),
            f"first-class path {path_id} required_surfaces",
        )
        scope_authority_refs = ensure_unique_strings(
            path.get("scope_authority_refs"),
            f"first-class path {path_id} scope_authority_refs",
        )
        if authority_decision_id not in scope_authority_refs:
            fail(f"first-class path {path_id} must link {authority_decision_id}")
        ensure_refs_exist(
            scope_authority_refs,
            decision_ids,
            f"first-class path {path_id} scope_authority_refs",
        )
        downstream_context_refs = ensure_unique_strings(
            path.get("downstream_context_refs"),
            f"first-class path {path_id} downstream_context_refs",
        )
        for context_ref in downstream_context_refs:
            if not context_ref.startswith("docs/") or not (ROOT / context_ref).is_file():
                fail(f"first-class path {path_id} references missing context file {context_ref}")
        blocking_refs = ensure_unique_strings(
            path.get("blocking_refs"),
            f"first-class path {path_id} blocking_refs",
        )
        missing_delivery_evidence = ensure_unique_strings(
            path.get("missing_delivery_evidence"),
            f"first-class path {path_id} missing_delivery_evidence",
        )
        if delivery_status == "blocked" and not blocking_refs:
            fail(f"blocked first-class path {path_id} must declare blocking_refs")
        if delivery_status == "cannot_judge" and not missing_delivery_evidence:
            fail(f"cannot_judge first-class path {path_id} must declare missing delivery evidence")
        if delivery_status == "release_ready" and (blocking_refs or missing_delivery_evidence):
            fail(f"release_ready first-class path {path_id} cannot retain blockers or missing evidence")
        profession_id = path.get("profession_id")
        if path_id.startswith("path.profession."):
            if not isinstance(profession_id, str) or not profession_id:
                fail(f"profession path {path_id} must declare profession_id")
            registry_professions[profession_id] = path.get("display_name")
        elif profession_id is not None:
            fail(f"non-profession path {path_id} must not declare profession_id")
        expected_family_id = path.get("family_id")
        if expected_family_id not in families:
            fail(f"first-class path {path_id} references unknown family_id {expected_family_id}")
        if path_id not in families[expected_family_id].get("child_path_ids", []):
            fail(f"first-class path {path_id} is missing from its declared family")

    if set(registry_professions) != set(catalog_professions):
        fail("profession first-class paths must match docs/contracts/profession-list.yaml exactly")
    for profession_id, display_name in registry_professions.items():
        if display_name != catalog_professions[profession_id].get("display_name"):
            fail(f"profession path {profession_id} display_name differs from profession catalog")

    candidates = require_ids(
        expect_list(registry.get("candidate_paths"), "path registry candidate_paths"),
        "candidate_id",
        "candidate path",
    )
    if set(candidates) != EXPECTED_CANDIDATE_PATH_IDS:
        fail("path registry candidate set is not canonical")
    for candidate_id, candidate in candidates.items():
        if candidate.get("scope_status") != "candidate":
            fail(f"candidate path {candidate_id} must have candidate scope_status")
        owner_domain = ensure_non_empty_string(
            candidate.get("coordinating_owner_domain"),
            f"candidate path {candidate_id} coordinating_owner_domain",
        )
        ensure_refs_exist([owner_domain], domain_ids, f"candidate path {candidate_id} owner")
        ensure_refs_exist(
            ensure_unique_strings(
                candidate.get("contributing_domains", []),
                f"candidate path {candidate_id} contributing_domains",
            ),
            domain_ids,
            f"candidate path {candidate_id} contributing_domains",
        )
        ensure_string_list(
            candidate.get("promotion_requirements"),
            f"candidate path {candidate_id} promotion_requirements",
        )

    supporting = require_ids(
        expect_list(
            registry.get("supporting_capabilities"),
            "path registry supporting_capabilities",
        ),
        "capability_id",
        "supporting capability",
    )
    if set(supporting) != EXPECTED_SUPPORTING_CAPABILITY_IDS:
        fail("path registry supporting-capability set is not canonical")
    for capability_id, capability in supporting.items():
        if capability.get("scope_status") != "supporting":
            fail(f"supporting capability {capability_id} must have supporting scope_status")
        owner_domain = ensure_non_empty_string(
            capability.get("coordinating_owner_domain"),
            f"supporting capability {capability_id} coordinating_owner_domain",
        )
        ensure_refs_exist([owner_domain], domain_ids, f"supporting capability {capability_id} owner")
        ensure_refs_exist(
            ensure_unique_strings(
                capability.get("contributing_domains", []),
                f"supporting capability {capability_id} contributing_domains",
            ),
            domain_ids,
            f"supporting capability {capability_id} contributing_domains",
        )
        ensure_non_empty_string(
            capability.get("boundary"),
            f"supporting capability {capability_id} boundary",
        )


def validate() -> None:
    decisions_doc = load_yaml(FILES["decisions"])
    ownership_doc = load_yaml(FILES["ownership"])
    questions_doc = load_yaml(FILES["questions"])
    intake_doc = load_yaml(FILES["intake"])
    conflicts_doc = load_yaml(FILES["conflicts"])
    path_registry_doc = load_yaml(FILES["path_registry"])
    profession_catalog_doc = load_yaml(FILES["profession_catalog"])

    decisions = require_ids(
        expect_list(decisions_doc.get("decisions"), "decisions"),
        "decision_id",
        "decision",
    )
    ownership = require_ids(
        expect_list(ownership_doc.get("ownership"), "ownership"),
        "domain_id",
        "ownership",
    )
    questions = require_ids(
        expect_list(questions_doc.get("questions"), "questions"),
        "question_id",
        "question",
    )
    intake = require_ids(
        expect_list(expect_mapping(intake_doc.get("intake"), "intake").get("records"), "intake.records"),
        "intake_id",
        "intake",
    )
    conflicts = require_ids(
        expect_list(
            expect_mapping(conflicts_doc.get("conflicts"), "conflicts").get("records"),
            "conflicts.records",
        ),
        "conflict_id",
        "conflict",
    )

    decision_ids = set(decisions)
    domain_ids = set(ownership)
    question_ids = set(questions)
    intake_ids = set(intake)
    conflict_ids = set(conflicts)

    validate_path_registry(
        path_registry_doc,
        profession_catalog_doc,
        decision_ids,
        domain_ids,
    )

    for domain_id, record in ownership.items():
        ensure_string_list(record.get("allowed_consumers", []), f"ownership {domain_id} allowed_consumers")
        ensure_refs_exist(
            ensure_string_list(record.get("open_question_ids", []), f"ownership {domain_id} open_question_ids"),
            question_ids,
            f"ownership {domain_id} open_question_ids",
        )
        placeholder_ids = ensure_string_list(
            record.get("placeholder_ids", []),
            f"ownership {domain_id} placeholder_ids",
        )
        if placeholder_ids and not record.get("open_question_ids"):
            fail(
                f"ownership {domain_id} uses placeholders but does not link any open_question_ids"
            )

    for question_id, record in questions.items():
        ensure_refs_exist(
            ensure_string_list(record.get("affected_domains", []), f"question {question_id} affected_domains"),
            domain_ids,
            f"question {question_id} affected_domains",
        )
        ensure_refs_exist(
            ensure_string_list(
                record.get("related_conflict_ids", []),
                f"question {question_id} related_conflict_ids",
            ),
            conflict_ids,
            f"question {question_id} related_conflict_ids",
        )
        ensure_refs_exist(
            ensure_string_list(
                record.get("related_decision_ids", []),
                f"question {question_id} related_decision_ids",
            ),
            decision_ids,
            f"question {question_id} related_decision_ids",
        )
        if record.get("blocking") and not record.get("related_conflict_ids"):
            fail(f"question {question_id} is blocking but has no related_conflict_ids")

    for intake_id, record in intake.items():
        linked_conflict_ids = ensure_string_list(
            record.get("conflict_ids", []),
            f"intake {intake_id} conflict_ids",
        )
        ensure_refs_exist(
            linked_conflict_ids,
            conflict_ids,
            f"intake {intake_id} conflict_ids",
        )
        quarantining_conflict_ids = [
            conflict_id
            for conflict_id in linked_conflict_ids
            if keeps_claims_quarantined(conflicts[conflict_id])
        ]
        source_truth_mode = record.get("source_truth_mode")
        intake_state = record.get("intake_state")
        if source_truth_mode == "approved_target_state" and intake_state == "quarantined":
            fail(
                f"intake {intake_id} cannot be quarantined with source_truth_mode approved_target_state"
            )
        if quarantining_conflict_ids:
            if intake_state == "promoted":
                fail(
                    f"intake {intake_id} cannot be promoted while conflicts remain open: "
                    + ", ".join(quarantining_conflict_ids)
                )
            if source_truth_mode == "approved_target_state":
                fail(
                    f"intake {intake_id} cannot use approved_target_state while conflicts remain open: "
                    + ", ".join(quarantining_conflict_ids)
                )
            if record.get("review_state") == "ready_for_promotion":
                fail(
                    f"intake {intake_id} cannot be ready_for_promotion while conflicts remain open: "
                    + ", ".join(quarantining_conflict_ids)
                )
            if record.get("disposition") == "carry_as_approved":
                fail(
                    f"intake {intake_id} cannot carry_as_approved while conflicts remain open: "
                    + ", ".join(quarantining_conflict_ids)
                )
        if record.get("disposition") == "carry_as_approved":
            expected_review_state = (
                "closed" if record.get("intake_state") == "promoted" else "ready_for_promotion"
            )
            if record.get("review_state") != expected_review_state:
                fail(
                    f"intake {intake_id} carry_as_approved with intake_state "
                    f"{record.get('intake_state')} requires review_state {expected_review_state}"
                )
            ensure_non_empty_string(
                record.get("target_artifact", ""),
                f"intake {intake_id} target_artifact",
            )
        if record.get("classification") == "runtime_contradicted" and not record.get("conflict_ids"):
            fail(f"intake {intake_id} runtime_contradicted requires at least one conflict_id")

    for conflict_id, record in conflicts.items():
        ensure_refs_exist(
            ensure_string_list(record.get("claims", []), f"conflict {conflict_id} claims"),
            intake_ids,
            f"conflict {conflict_id} claims",
        )
        ensure_refs_exist(
            ensure_string_list(
                record.get("affected_domains", []),
                f"conflict {conflict_id} affected_domains",
            ),
            domain_ids,
            f"conflict {conflict_id} affected_domains",
        )
        blocking = record.get("blocking")
        status = record.get("status")
        if status in OPEN_CONFLICT_STATUSES and blocking == "none":
            ensure_non_empty_string(
                record.get("non_blocking_justification", ""),
                f"conflict {conflict_id} non_blocking_justification",
            )

    for decision_id, record in decisions.items():
        source_intake_ids = ensure_string_list(
            record.get("source_intake_ids", []),
            f"decision {decision_id} source_intake_ids",
        )
        ensure_refs_exist(
            source_intake_ids,
            intake_ids,
            f"decision {decision_id} source_intake_ids",
        )
        if record.get("status") == "approved" and not source_intake_ids:
            fail(f"decision {decision_id} approved decisions require source_intake_ids")
        ensure_refs_exist(
            ensure_string_list(record.get("conflict_ids", []), f"decision {decision_id} conflict_ids"),
            conflict_ids,
            f"decision {decision_id} conflict_ids",
        )
        ensure_refs_exist(
            ensure_string_list(record.get("open_questions", []), f"decision {decision_id} open_questions"),
            question_ids,
            f"decision {decision_id} open_questions",
        )
        ensure_refs_exist(
            ensure_string_list(record.get("dependencies", []), f"decision {decision_id} dependencies"),
            decision_ids,
            f"decision {decision_id} dependencies",
        )


def main() -> None:
    validate()
    print("domain 00 product YAML check passed")


if __name__ == "__main__":
    main()
