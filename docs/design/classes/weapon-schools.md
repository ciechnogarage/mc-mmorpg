---
title: Domain 04 Weapon Schools Contract
status: proposed
updated_at: 2026-07-22
---

# Domain 04 Weapon Schools Contract

This document governs `weapon_school_id` for Domain 04 classes, subclasses, skills,
and loadout eligibility. It is a framework contract, not a concrete class roster,
itemization list, runtime adapter, or final MMOItems configuration.

## Source Of Truth

Canonical authority:

- `docs/design/classes/weapon-schools.md` owns `weapon_school_id` meaning.
- `docs/design/classes/class-framework.md` consumes weapon schools for class and
  subclass equipment compatibility.
- `docs/design/classes/skill-library-contract.yaml` consumes weapon schools for
  skill requirements.
- `docs/design/classes/combat-loadout.md` consumes weapon schools for loadout
  eligibility validation.
- `docs/design/classes/domain-02-03-alignment.md` defines ownership boundaries.

Domain 05 consumes weapon schools through item definition compatibility and
equipment-slot validation. Domain 05 does not define the gameplay meaning of a
weapon school.

Legacy design inputs:

- `docs/class-subclass-foundation-v0.0.1.md` keeps high-level weapon fantasy and
  role ideas only.
- `docs/class-skill-kits-foundation-v0.0.1.md` keeps high-level skill-loop and
  counterplay ideas only.

Legacy inputs are not source truth for final ids, item instances, concrete class
kits, item exceptions, formulas, cooldowns, or damage values.

## Definition

`weapon_school_id` is a stable Domain 04 semantic identifier for a gameplay
usage style. It describes how a weapon is used in combat: pressure pattern,
range band, timing profile, defensive posture, control affordance, and animation
expectations.

A weapon school is not:

- a raw list of allowed item ids;
- a Minecraft material name;
- an MMOItems item type;
- a display name, lore marker, model id, or plugin runtime id;
- a class-specific exception list;
- a complete combat formula, stat dictionary, or itemization budget.

The same physical item archetype may be compatible with one or more weapon
schools only when Domain 05 item definition compatibility explicitly says so.
The `weapon_school_id` meaning remains owned by Domain 04.

## Contract Shape

Minimum shape for a weapon school registry entry:

```yaml
weapon_school:
  id: weapon_school_id
  schema_version: 1
  semantic_owner_domain: "04"
  lifecycle:
    status: draft | validated | runtime_verified | released
  identity:
    name_key: string
    description_key: string
    usage_style_tags: [string]
  attack_profile:
    range_profile_id: string
    cadence_profile_id: string
    impact_profile_id: string
    cleave_profile_id: string | null
    guard_interaction_profile_id: string | null
    mobility_profile_id: string | null
  skill_constraints:
    allowed_skill_tags: [string]
    forbidden_skill_tags: [string]
    required_state_tags: [string]
    prohibited_state_tags: [string]
  animation_runtime:
    stance_profile_id: string
    swing_profile_id: string
    projectile_profile_id: string | null
    channel_profile_id: string | null
    offhand_profile_id: string | null
    runtime_adapter_refs: [string]
  domain_05_touchpoints:
    compatible_item_archetype_tags: [string]
    compatible_equipment_slot_ids: [string]
    dual_wield_policy_id: string | null
    shield_policy_id: string | null
    relic_policy_id: string | null
```

The registry may add descriptive tags, but validators must treat the fields above
as the minimum contract needed before classes, skills, or item definitions depend
on a weapon school.

## Attack Profile Semantics

Attack profile fields describe combat shape, not numeric balance:

| Field | Meaning | Boundary |
| --- | --- | --- |
| `range_profile_id` | Whether the style is melee, reach, thrown, projectile, area, or hybrid. | Domain 03 owns exact ranges and hit validation rules. |
| `cadence_profile_id` | Timing feel such as quick-chain, deliberate-heavy, charged, channeled, or burst-window. | Domain 03 owns cooldown, cast, channel, and global cooldown rules. |
| `impact_profile_id` | Main contact pattern such as single-target pressure, guard pressure, stagger pressure, or execution burst. | Domain 03 owns formulas, poise, tenacity, mitigation, and control taxonomy. |
| `cleave_profile_id` | Whether the style naturally supports nearby secondary targets. | Not a target cap, radius, or final AoE formula. |
| `guard_interaction_profile_id` | Whether the style blocks, parries, breaks guard, ignores guard, or has no guard interaction. | Must reference Domain 03 guard, block, poise, and control contracts when they exist. |
| `mobility_profile_id` | Whether attacks assume planted, step-in, lunge, retreating, or mobile execution. | Domain 14 owns input presentation; runtime adapters execute movement. |

Weapon schools may support later validators that answer whether a skill's stated
usage and animation contract are plausible for the equipped weapon school. They
must not inline skill math.

## Skill Constraints

Skills may reference `weapon_school_ids` in
`docs/design/classes/skill-library-contract.yaml` requirements.

Rules:

- A skill requiring a weapon school must describe a usage dependency, not an item
  id dependency.
- Weapon school requirements must compose with class, subclass, loadout, state,
  resource, cooldown, formula, and effect contracts.
- Skill tags remain classification labels. They do not create new stats, damage
  types, formulas, mitigation rules, cooldown rules, or PvP modifiers.
- A class or subclass may allow a broad weapon school while a specific skill
  requires a narrower school.
- A subclass may add or remove allowed weapon schools only through the subclass
  delta contract in `class-framework.md`.
- No skill may create an item-instance exception such as "this exact item can
  bypass the weapon school requirement." Such exceptions belong outside this
  framework-level contract and are not approved for Domain 04 v1.

## Animation And Runtime Expectations

Weapon schools must carry enough animation/runtime metadata for adapters and QA
to verify that execution matches the intended usage style.

Minimum expectations:

- `stance_profile_id` defines the held posture family expected by the style.
- `swing_profile_id` defines the primary attack motion family.
- `projectile_profile_id` is present only when the style launches or controls a
  projectile-like attack.
- `channel_profile_id` is present only when the style supports channeled or held
  execution.
- `offhand_profile_id` is present only when the style has explicit offhand,
  shield, or dual-wield semantics.
- `runtime_adapter_refs` may point to plugin or animation implementation refs,
  but those refs never define the semantic `weapon_school_id`.

Animation profiles do not prove final art quality, hit timing, damage timing, or
visual approval. Runtime verification must still prove the adapter uses the
correct semantic id and does not execute in incompatible loadout or item states.

## Domain 05 Touchpoints

Domain 04 provides stable weapon school identifiers and gameplay meaning. Domain
05 itemization consumes those identifiers in item definitions and equipment
validation.

Domain 05 should reference weapon schools from:

- item definition compatibility metadata;
- equipment slot eligibility;
- item archetype tags;
- affix eligibility where affixes depend on usage style;
- dual-wield, shield, offhand, and relic compatibility policies.

Rules:

- Domain 05 may say an item definition is compatible with a weapon school.
- Domain 05 may not redefine the gameplay semantics of a weapon school.
- `item_instance_id` and item-instance state remain Domain 05/Domain 01 concerns,
  not Domain 04 weapon school concerns.
- Weapon school compatibility must be expressed at the item definition or
  approved archetype-policy level, not as ad hoc instance exceptions.
- Relic item data remain Domain 05-owned; any combat activation still references
  Domain 03 contracts and Domain 04 loadout eligibility.

## Validation Requirements

Minimum validators for weapon school contracts must prove:

- every referenced `weapon_school_id` resolves to one registry entry;
- each weapon school has gameplay semantics beyond allowed item ids;
- class and subclass `allowed_weapon_schools` reference existing ids;
- skill `weapon_school_ids` reference existing ids and do not bypass class,
  subclass, loadout, resource, cooldown, formula, or effect requirements;
- Domain 05 item definitions reference weapon schools only as compatibility
  identifiers, not semantic definitions;
- runtime adapter ids, plugin ids, display text, and item ids do not define or
  rewrite `weapon_school_id`;
- no concrete item-instance exception is required for a class or skill to pass
  compatibility.

## Lifecycle

| Status | Meaning |
| --- | --- |
| `draft` | Weapon school design exists but references or validation may be incomplete. |
| `validated` | Schema, ownership, and cross-domain references pass static validation. |
| `runtime_verified` | Runtime adapters execute only valid class, skill, loadout, and item combinations. |
| `released` | Runtime verification and dependent Domain 04/05 contracts are accepted for production use. |

Class-specific `CLS-*` work may reference weapon schools after the relevant
entries are at least `validated`. Final class kits, final itemization, concrete
items, and item-instance exceptions remain outside this document.
