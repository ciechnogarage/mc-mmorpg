---
title: Domain 05 Itemization Framework Draft
status: draft
updated_at: 2026-07-31
---

# Domain 05: itemization framework

## Purpose

This document consolidates the current domain-05 planning direction for itemization.
It is a planning and contract draft, not runtime truth.

## Companion semantic contract files

Settled areas are maintained as separate semantic contract scaffolds. This document stays the
narrative overview; the scaffolds carry the field-level and invariant-level detail.

| File | Covers | Closes |
| --- | --- | --- |
| `docs/design/items/equipment-slots.yaml` | v1 slot registry | slot scope |
| `docs/design/items/item-data-model.yaml` | definition / generation snapshot / instance boundaries, item invariants | `Domain 05: Item Data Model and Invariants` |
| `docs/design/items/modification-operation-semantics.yaml` | capacity consumption, operation record, workflow states, restart/retry | `Domain 05: Modification Transactions and Capacity` |
| `docs/design/items/relic-contract.yaml` | Relic field contract and combat integration | `Domain 05: Relic Contract and Combat Integration` |

Every scaffold marks foreign-domain durable fields as explicit placeholders. None of them
defines persistence shape, and none claims runtime implementation.

## Current repo inputs

Relevant source inputs already in the repository:

- `docs/itemization-foundation-v0.0.1.md`
- `docs/foundation-sloty-i-ekwipunek-v0.0.1.md`
- `docs/foundation-typy-ekwipunku-v0.0.1.md`
- `docs/economy-crafting-loot-foundation-v0.0.1.md`
- `docs/combat-foundation-v0.0.1.md`
- `docs/class-skill-kits-foundation-v0.0.1.md`
- `docs/foundation-index.md`

These remain design inputs. They are not yet a production contract.

## What survives from older foundations

- Equipment is a first-class RPG system, not only vanilla Minecraft armor plus weapon stats.
- Rare items should be the core of ordinary progression.
- Unique items should change build behavior with tradeoffs, not only add bigger numbers.
- Loot, crafting, and item sinks must remain connected.
- Itemization should interact with combat and skill tags, not only raw stats.

## Domain-05 gaps that still block production itemization

1. Affix taxonomy still depends on domain-03 combat stat authority.
2. Final item-instance durable contract still depends on domain 01.
3. Final slot budget math still depends on domain 03.
4. Relic integration still depends on domain 03 and domain 04.
5. Concrete uniques, sets, and broad loot pools must wait until the above are closed.

## Scope ownership

Domain 05 owns:

- item structure
- equipment slots
- item stat ceilings and item-budget semantics
- affix eligibility
- rarity
- item tier
- generation rules
- modification rules for an item instance
- item-side binding semantics
- Relic as an itemization subsystem

Domain 05 does not own:

- combat stat semantics and final coefficients
- class and weapon-school rules
- enemy or boss loot tables
- crafting costs and economy pricing
- market and tax rules
- GUI behavior
- the global durable item-instance contract shell

## Hard dependencies

### Domain 03: combat core

Must provide:

- canonical `stat_id` dictionary
- damage and defense type authority
- aggregation order
- caps and soft caps
- symbolic budget weights
- symbolic mapping from item power to combat output

### Domain 04: classes, skills, weapons

Must provide:

- weapon school tags
- class restrictions or proficiencies
- dual-wield rules
- loadout compatibility
- relic compatibility constraints

### Domain 01: data contracts

Must provide:

- `item_instance_id`
- owner identity envelope
- persistence boundary
- transaction semantics
- idempotency semantics
- revision/concurrency semantics
- event envelope semantics
- PDC versus durable storage authority split

## Governing contract direction

### 1. Four item axes stay separate

`item_tier`

- controls progression band and power ceiling
- gates max budget and affix access
- is not the same thing as a good or bad roll

`item_level`

- places the item on the internal power curve
- drives base budget and affix access
- participates in salvage and requirement scaling

`rarity`

- controls item construction, mainly affix structure
- is not a blanket multiplier for all item power

`required_character_level`

- controls equip eligibility only
- stays separate from crafting eligibility

### 2. Three conceptual layers of item data

#### Item definition

Static archetype metadata:

- `definition_id`
- slot
- item tags and archetype
- implicits
- allowed affix groups
- visuals or model hooks
- material identity
- default binding policy

#### Generation specification

Generation and provenance semantics:

- `source_type`
- `source_id`
- `item_tier`
- `item_level`
- `rarity`
- guaranteed affixes or constraints
- generation seed or equivalent provenance payload

In domain-05 v1 this remains a distinct conceptual layer, but not a separate durable entity.
Its approved v1 representation is an embedded generation snapshot inside `item_instance`.

The snapshot must carry a minimum provenance version stamp so an old instance is never
revalidated against current globals blindly:

- `definition_revision`
- `slot_registry_version`
- `affix_taxonomy_version`

#### Item instance

The only player-ownable durable item entity:

- `item_instance_id`
- `definition_id`
- owner reference or identity envelope
- rolled affixes
- generation snapshot
- capacity state
- binding state
- revision/version fields
- current durable operation pointers, for example `last_successful_operation_id`

The instance holds current state and current operation pointers only. Full modification
history belongs to the domain-01 durable operation ledger or event stream, not to the item.

### 3. Ownership boundary for `item_instance`

Domain 05 owns item semantics inside the instance:

- item payload meaning
- binding semantics
- capacity semantics
- generation semantics
- affix semantics

Domain 01 owns the durable shell for the instance contract:

- `contract_owner`
- `persistence_owner`
- `runtime_authority`
- `integration_coordinator`

for:

- identity
- owner envelope
- revision
- transaction envelope
- persistence-boundary rules

### 4. Approved v1 persistence direction

- `item_definition` exists as a separate definition layer
- `generation_specification` exists as a conceptual layer
- `generation_specification` is stored physically as snapshot/provenance data within `item_instance`
- no separate durable `generation_specification` object is introduced in v1
- embedded generation snapshot inside `item_instance` is the only runtime truth for provenance in v1
- any external recipe/template/spec material is non-authoritative after projection into the embedded snapshot
- provenance projection is one-way: `template/spec -> embedded snapshot`

### 5. Minimum v1 invariants for the item boundary

- `item_instance_id` is globally unique and governed by domain 01
- runtime item identity must not depend on display name, lore, or GUI slot
- `definition_id` must resolve to an existing `item_definition`
- generation snapshot must be semantically valid for the referenced definition
- binding state must be explicit, not inferred from placement
- owner identity must use domain-01 identity envelope rules
- revision and idempotent mutation semantics belong to domain 01, not local item logic
- PDC may be an adapter/cache layer, not the sole durable authority for valuable item instances

## Equipment-slot registry

Per slot the registry should capture:

- `slot_id`
- category
- allowed item tags
- budget multiplier
- offense allowance
- defense allowance
- allowed affix groups
- dual-wield behavior
- unique-equip behavior
- GUI visibility
- runtime source notes

Approved domain-05 v1 slot scope:

- `main_hand`
- `off_hand`
- `head`
- `chest`
- `legs`
- `feet`
- `ring_1`
- `ring_2`
- `amulet`
- `relic`

Explicitly outside v1:

- `gloves`
- `bracelet`
- `artifact1`
- `artifact2`
- `artifact3`

Excluded slots may continue to exist as runtime/plugin capabilities or legacy slots, but they are not allowed to define v1 domain-05 requirements.

Additional v1 assumptions:

- `Relic` is the only dedicated special-purpose slot in the domain-05 v1 contract.
- `artifact1-3` are legacy runtime slots outside the domain-05 v1 contract.
- `artifact1-3` are non-authoritative for v1 equip legality, affix eligibility, binding semantics, and all Relic behavior.
- `off_hand` may model `offhand_weapon` eligibility at the slot level, but build access control belongs to domain 04.

## Affix taxonomy

The taxonomy must be derived from the combat stat dictionary, not invented first.

Each affix needs:

- `affix_id`
- `affix_family`
- `affix_group`
- `affix_type`
- eligible slots
- eligible archetypes
- budget cost model
- tier table
- roll ranges
- conflict tags
- minimum item level
- allowed source types
- scaling rule
- version

## Modification capacity and transactions

Already settled:

- initial crafting does not spend capacity
- later modifications do spend capacity
- capacity does not regenerate
- no `keep old`
- no restore-to-previous-state affordance
- modification may worsen item

Still open:

- exact initial capacity formula
- per-operation costs
- allowed degradation patterns

Restart and rollback behavior is no longer open. It is settled in
`docs/design/items/modification-operation-semantics.yaml`: before `COMMITTED` no partial
business mutation may be exposed and capacity stays unspent; after `COMMITTED` a replay of
the same `idempotency_key` returns the committed result; unresolvable state goes to
`MANUAL_REVIEW`.

Approved v1 transaction direction:

- every logical modification attempt creates one durable modification operation
- retry identity is based on `idempotency_key`, not lore, display text, or payload comparison
- `RESERVED` acts as an exclusivity fence on `item_instance_id + expected_revision`
- `RESERVED` does not consume capacity and does not change the business state of the item
- `RESERVED` is also a lease, with reservation owner, expiry, explicit release, and reaper/timeout recovery
- capacity is consumed only at `COMMITTED`
- item result, `remaining_capacity`, revision increment, and successful operation linkage must commit atomically

Minimum instance-side fields the modification contract needs:

- `initial_capacity`
- `remaining_capacity`
- `modification_count`
- `revision` (domain-01 shell)
- `last_successful_operation_id` or equivalent durable linkage (domain-01 shell)

Recommended v1 operation states, aligned with domain 01:

- `RECEIVED`
- `VALIDATED`
- `RESERVED`
- `APPLYING`
- `COMMITTED`
- `COMPENSATED`
- `MANUAL_REVIEW`

Minimum v1 modification invariants:

- one logical operation must not consume capacity twice
- restart/retry must resolve by `idempotency_key`
- duplicate operations on the same instance/revision must be fenced
- failure before `COMMITTED` must not produce partial item-state mutation
- item mutation must not depend on lore parsing or GUI transient state

## Relic boundary

Current repo inputs mix `Relic` with `Artifact` or generic utility slots. Domain 05 narrows that.

Working direction:

- `Relic` is one dedicated itemization subsystem and one dedicated equipment slot
- relic active effects route through shared combat and cooldown contracts
- Relic must not create a parallel combat engine
- generic artifact, trinket, or accessory slots must not substitute the Relic contract

Approved v1 Relic direction:

- `Relic` is a build modifier, not a second class system
- one equipped `Relic` maximum
- one `active_effect_ref` maximum
- passive effects are allowed through referenced effect contracts
- `active_effect_ref` must resolve through the shared domain-03 combat/effect activation contract
- Relic uses the shared cooldown contract, not a separate relic-only cooldown engine
- restrictions are tag-based, not custom per-relic rule engines

Recommended Relic contract shape:

- `relic_mode`: `passive_only | active_only | hybrid`
- `passive_effect_refs`
- `active_effect_ref`
- `cooldown_group`
- `restriction_tags`
- `unique_equipped_group`

Relic guardrails:

- no separate Relic resource system in v1
- no separate Relic damage engine
- no multi-skill Relic rotation model
- no use of ordinary accessory or legacy artifact slots as Relic substitutes

Published contract artifact:

- `docs/design/items/relic-contract.yaml` carries the field-level Relic contract, the
  `relic_mode` cardinality matrix, the v1 invariants, and the validation expectations
- that file also lists every unresolved domain-03 and domain-04 reference as an explicit
  placeholder token, so no effect, cooldown, or tag identifier is invented by domain 05
- slot identity itself stays in `docs/design/items/equipment-slots.yaml`

## Recommended next deliverables

Already in the repository:

1. `docs/design/items/equipment-slots.yaml`
2. `docs/design/items/itemization-framework.md`
3. `docs/design/items/item-data-model.yaml`
4. `docs/design/items/modification-operation-semantics.yaml`
5. `docs/design/items/relic-contract.yaml`

Still missing, and blocking:

6. `docs/design/combat/stat-dictionary.yaml` or equivalent domain-03 authority
7. `docs/design/items/affix-taxonomy.yaml` skeleton, only after the stat dictionary exists
8. the domain-01 durable item-instance and operation contract shells, which every
   `placeholder_pending_domain_01` entry in the two scaffolds waits on

## Suggested sequencing

1. Freeze expansion of concrete affix lists until the combat stat dictionary exists.
2. Approve four-axis separation.
3. Approve slot registry.
4. Approve three-layer item data model.
5. Draft affix taxonomy skeleton.
6. Align symbolic budget formulas with domain 03.
7. Define modification transactions and restart behavior.
8. Define Relic as a combat-core client.
9. Only then design uniques, legendary items, sets, and broad loot pools.
