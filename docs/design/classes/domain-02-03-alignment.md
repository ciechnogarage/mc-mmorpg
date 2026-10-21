---
title: Domain 04 Alignment Domains 02 03
status: proposed
updated_at: 2026-08-12
---

# Domain 04 Alignment Domains 02 03

This document closes the ownership boundary between Domain 04 classes,
subclasses, races, skills, and weapon schools, Domain 02 character persistence,
and Domain 03 combat core contracts.

It is not a class roster, skill kit, formula sheet, or runtime evidence
document.

## Ownership

| Identifier | Semantic owner | Persistence owner | Runtime adapter note |
| --- | --- | --- | --- |
| `class_id` | Domain 04 | Domain 02 character record | `mmocore_class_id` may map runtime only. |
| `subclass_id` | Domain 04 | Domain 02 character record | Nullable until subclass unlock contract grants it. |
| `race_id` | Domain 04 | Domain 02 character record | Race meaning/budget stays D04; identity storage stays D02. |
| `skill_id` | Domain 04 | Domain 02 character learned/equipped state | Skill execution references Domain 03 formulas/effects/contracts. |
| `weapon_school_id` | Domain 04 | Domain 02 unlock/proficiency state | Not a raw item-id list; Domain 05 consumes compatibility only. |

Domain 04 owns the meaning, lifecycle, validation, and allowed relationships of
these identifiers. Domain 02 owns persisted character state storing which
identifiers are selected, learned, unlocked, and equipped.

## Domain 02 Persistence Fields

Domain 02 persists Domain 04 state without becoming the semantic authority for
that state.

| Character registry key | Status | Purpose |
| --- | --- | --- |
| `character.identity.race_id` | existing, annotated | Selected race identity. |
| `character.identity.class_id` | existing, annotated | Selected class identity. |
| `character.identity.subclass_id` | existing, annotated | Selected subclass identity when unlocked. |
| `character.progression.learned_skill_ids` | existing, annotated | Durable learned skills. |
| `character.progression.weapon_school_unlock_ids` | existing, annotated | Durable weapon school unlock/proficiency ids. |
| `character.loadout.active_loadout_id` | existing, annotated | Active loadout selector. |
| `character.loadout.equipped_skill_ids` | existing, annotated | Equipped skill slots for the active loadout. |
| `character.progression.talent_allocations` | existing, annotated | Durable talent choices. |
| `character.progression.respec_state` | existing, annotated | Respec state and refund gates. |
| `character.progression.legacy_technique_ids` | existing, annotated | Permanent Legacy Technique state. |
| `character.progression.faith_scar_ids` | existing, annotated | Permanent Faith Scar state. |

Every Domain 04-owned key persisted by Domain 02 carries:

- `semantic_owner_domain: "04"`
- `persistence_owner_domain: "02"`
- `canonical_definition_contract: docs/design/classes/domain-02-03-alignment.md`

## Domain 03 Upstream Gates

Domain 04 cannot release class-specific `CLS-*` work until the Domain 03
contract families below exist and are approved enough for every reference path
already declared in Domain 04 classes, skills, loadout, race, and
Relic-adjacent integrations.

| Contract id | Required by D04 |
| --- | --- |
| `stat-dictionary` | Canonical stat ids referenced by classes, talents, skills, and restrictions. |
| `combat-stat-contract` | Stat ownership, aggregation, caps, and runtime owner boundaries. |
| `damage-event-contract` | Legal damage classification vocabulary consumed by D04, including top-level damage family and any accepted school / elemental taxonomy references. |
| `stat-contribution-contract` | How class, item, buff, and runtime contributions stack without double counting. |
| `damage-formula-contract` | Approved `formula_id` and mitigation order. |
| `coefficient-set-contract` | Approved `coefficient_set_id` references. |
| `effect-contract` | Approved `effect_ids`, race effect refs, and Relic effect refs. |
| `activation-contract` | Approved activation refs for active Relic / combat execution hooks. |
| `range-profile-contract` | Approved `range_profile_id` references. |
| `cast-time-rule-contract` | Approved `cast_time_rule_id` references. |
| `channel-time-rule-contract` | Approved `channel_time_rule_id` references. |
| `state-requirement-contract` | Approved `state_requirement_ids` references. |
| `resource-requirement-contract` | Approved `resource_requirement_ids` references. |
| `resource-contract` | Resource state, costs, regeneration, reset, and persistence semantics. |
| `cooldown-contract` | Cooldown, global cooldown, and charge-rule references. |
| `cc-taxonomy-contract` | Approved control categories, diminishing returns, and PvP CC policy hooks. |
| `pvp-mode-modifier-contract` | Mode parameters/caps that must not fork the base combat pipeline. |

Until these contracts exist, Domain 04 may describe mechanics only as gameplay
concepts and design tags. Domain 04 must not define a stat dictionary, damage
formula, effect semantics, activation semantics, resource regeneration rules,
cooldown recovery rules, CC taxonomy, global PvP modifiers, or an alternate
combat pipeline.

## Mandatory Domain 03 Consumer Binding

Every Domain 04 record that executes a catalogued combat mechanic must carry
one binding for each mechanic it can execute:

```yaml
mechanic_id: namespaced_id
mechanic_contract_version: positive_integer
required_readiness: RUNTIME_VERIFIED
mechanic_contract_ref: immutable_ref
gate_transition_envelope_ref: path_at_sha256
contract_bundle_digest: sha256
build_digest: sha256
```

The binding is additional to `formula_id`, `coefficient_set_id`, `effect_ids`,
control category, resource/cooldown refs, and runtime provider refs. None of
those generic references substitutes for mechanic readiness.

Rules:

- every executable mechanic has exactly one binding for its `mechanic_id`;
- `required_readiness` is the literal `RUNTIME_VERIFIED`;
- the immutable mechanic contract, gate transition envelope, contract bundle,
  and build digests must resolve to one Domain 03 readiness lineage;
- `draft` and design-only records may use an empty binding list, but they may
  not receive an executable provider mapping or advance above `validated`;
- missing, duplicate, stale, digest-mismatched, downgraded, or revoked bindings
  suppress/reject the mechanic fail-closed at admission;
- no formula, effect, tag, provider behavior, or Domain 04 local rule may act as
  a fallback for a rejected binding.

## Adapter Rule

`mmocore_class_id` is an adapter/runtime mapping only. It never becomes the
semantic class identity.

Valid use:

```yaml
class_id: cls_example
runtime_mapping:
  mmocore_class_id: EXAMPLE
```

Invalid use:

```yaml
class_id: EXAMPLE # inferred from MMOCore id
```

Changing MMOCore class naming, plugin storage, or adapter implementation must
not silently rewrite `class_id`, `subclass_id`, `race_id`, `skill_id`, or
`weapon_school_id`.

## CLS Gate

Class-specific design may start only after the Domain 02 persisted-state map
above is accepted and the Domain 03 gates needed by the class are present.
Before that point, Domain 04 work is limited to ownership, framework, loadout,
race, weapon school, and validation contracts.
