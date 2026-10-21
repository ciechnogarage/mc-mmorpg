---
title: Domain 04 Class Framework
status: proposed
updated_at: 2026-07-22
---

# Domain 04 Class Framework

This document is the governing contract for Domain 04 classes and subclasses.
It defines the mechanical shape of a class, the minimum contract every class
must expose, and the boundaries a subclass may mutate.

It is not a concrete class roster, `CLS-*` design, skill kit, combat formula, or
runtime evidence document.

## Source Of Truth

Canonical authority:

- `docs/design/classes/class-framework.md` for class and subclass contract shape.
- `docs/design/classes/domain-02-03-alignment.md` for Domain 02 persistence and
  Domain 03 upstream ownership boundaries.

Legacy design inputs:

- `docs/class-subclass-foundation-v0.0.1.md` keeps fantasy, role, high-level
  roster, and counterplay ideas only.
- `docs/progression-respec-foundation-v0.0.1.md` keeps progression, respec,
  loadout, and combat-lockout intuitions only.

Legacy inputs are not source of truth for stat ids, damage types, formulas,
unlock levels, runtime ids, resource rules, or final class content.

## Mechanical Definition

A class is a Domain 04 semantic identity that binds a character to:

- a stable `class_id`;
- role and range tags used by UX, content requirements, and validators;
- allowed resource references owned by Domain 03 contracts;
- allowed weapon school and armor permission references consumed by Domain 05;
- progression, unlock, talent, and subclass eligibility references;
- loadout rules and starting skill references;
- runtime adapter mappings that do not define semantic identity;
- lifecycle state from design draft to release.

A class is not:

- a local combat formula;
- a replacement stat dictionary;
- an MMOCore class id;
- a complete itemization rule set;
- a global summon, pet, companion, HUD, or persistence system.

## Mandatory Class Components

Every class contract must define:

| Component | Required content | Authority boundary |
| --- | --- | --- |
| Identity | `class_id`, display keys, role tags, range tags, complexity | Domain 04 owns meaning; Domain 02 persists selected ids. |
| Resources | primary and optional secondary resource ids | Must reference Domain 03 resource contracts. |
| Equipment permissions | weapon school ids and armor permissions | Domain 04 defines allowed relationships; Domain 05 consumes item compatibility. |
| Progression | level curve, unlock table, talent trees, subclass unlock rule | Domain 04 owns unlock meaning; Domain 02 persists character state. |
| Loadout | loadout rule, defaults, required and forbidden skill tags | Domain 04 owns rules; Domain 14 owns input/HUD presentation. |
| Integration | runtime adapter refs and combat contract version | Adapter ids never become semantic ids. |
| Lifecycle | `draft`, `validated`, `runtime_verified`, or `released` | Validators and runtime evidence advance status. |

## Subclass Contract

A subclass is a mutation of one parent class. It narrows, redirects, or
specializes the parent class but does not become a separate full class.

A subclass may change:

- role emphasis within the parent identity;
- allowed or preferred weapon school subset;
- unlock table extensions;
- talent tree additions or branches;
- loadout requirements and forbidden skill tags;
- skill availability through approved `skill_id` references;
- resource usage only through approved Domain 03 resource contracts;
- runtime adapter mappings for subclass-specific implementation details.

A subclass must not replace:

- parent `class_id`;
- Domain 03 stat dictionary, damage formulas, resource semantics, cooldown
  contracts, CC taxonomy, or PvP modifier model;
- Domain 02 persistence ownership;
- Domain 05 item instance ownership;
- Domain 14 HUD/input ownership;
- class lifecycle rules;
- the requirement that all executable skills reference approved upstream
  `formula_id`, `coefficient_set_id`, `effect_id`, and cooldown/resource
  contracts where applicable.

## Contract Shape

This is the minimum shape for a Domain 04 class definition:

```yaml
class:
  id: string
  schema_version: integer

  display:
    name_key: string
    description_key: string
    icon_ref: string

  identity:
    role_tags: [string]
    combat_range_tags: [string]
    complexity: low | medium | high
    primary_resource_id: string
    secondary_resource_id: string | null
    allowed_weapon_schools: [string]
    armor_permissions: [string]

  progression:
    level_curve_id: character_level_1_100
    unlock_table_id: string
    talent_tree_ids: [string]
    subclass_unlock_rule_id: string | null

  loadout:
    loadout_rule_id: string
    default_skill_ids: [string]
    required_skill_tags: [string]
    forbidden_skill_tags: [string]

  integration:
    mmocore_class_id: string | null
    persistence_owner: character_domain
    combat_contract_version: string

  lifecycle:
    status: draft | validated | runtime_verified | released
```

Subclass definitions must reference the parent class and express only deltas:

```yaml
subclass:
  id: string
  schema_version: integer
  parent_class_id: string

  identity_delta:
    role_tags_added: [string]
    role_tags_removed: [string]
    combat_range_tags_added: [string]
    combat_range_tags_removed: [string]

  equipment_delta:
    allowed_weapon_schools_added: [string]
    allowed_weapon_schools_removed: [string]
    armor_permissions_added: [string]
    armor_permissions_removed: [string]

  progression_delta:
    unlock_table_extension_id: string | null
    talent_tree_ids_added: [string]

  loadout_delta:
    required_skill_tags_added: [string]
    forbidden_skill_tags_added: [string]
    skill_ids_added: [string]
    skill_ids_removed: [string]

  integration:
    runtime_adapter_ref: string | null

  lifecycle:
    status: draft | validated | runtime_verified | released
```

## Identity Contract

`class_id` and `subclass_id` are stable semantic identifiers owned by Domain 04.
They must not be inferred from plugin names, display names, MMOCore ids, menu
labels, or item ids.

Domain 02 persists selected and unlocked ids but does not define their meaning.
Changing runtime plugin mappings must not rewrite the semantic ids.

## Progression Contract

A class may reference progression artifacts but must not inline final unlock
levels or talent math before the upstream contracts are accepted.

Required references:

- `level_curve_id`;
- `unlock_table_id`;
- `talent_tree_ids`;
- optional `subclass_unlock_rule_id`.

Progression state is persisted by Domain 02. Domain 04 owns the meaning of the
unlock and talent definitions.

## Loadout Contract

A class must point to a loadout rule rather than defining ad hoc slot behavior.
The loadout contract must resolve:

- active skill eligibility;
- passive and utility requirements;
- default starting skills;
- required and forbidden skill tags;
- combat edit lockout;
- reconnect and restart persistence semantics;
- Domain 14 presentation and input requirements.

The class framework does not set final slot counts. Those belong in the combat
loadout contract.

## Integration Contract

Runtime providers are adapters. `mmocore_class_id` and other implementation refs
may execute or store plugin-specific behavior, but they do not define
`class_id`, `subclass_id`, `skill_id`, `race_id`, or `weapon_school_id`.

Class and subclass definitions may reference:

- Domain 03 combat contract versions;
- approved resource ids;
- approved formula/effect/coefficient ids through skills;
- Domain 05 weapon school and armor compatibility ids;
- Domain 14 loadout and HUD presentation requirements.

They must not define local combat formulas, new stats, mitigation order, crit
rules, global cooldown caps, CC diminishing returns, or PvP scaling models.

## Lifecycle Contract

Class and subclass lifecycle status has these meanings:

| Status | Meaning |
| --- | --- |
| `draft` | Design exists but upstream references or validators may be incomplete. |
| `validated` | Schema and cross-domain references pass validators. |
| `runtime_verified` | Runtime adapter mappings were exercised on the correct backend. |
| `released` | Runtime evidence and release QA are accepted for player-facing use. |

`CLS-*` class-specific production work may not start until the Domain 02 persisted
state map and required Domain 03 upstream gates in
`docs/design/classes/domain-02-03-alignment.md` are accepted.
