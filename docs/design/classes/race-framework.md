---
title: Domain 04 Race Framework
status: proposed
updated_at: 2026-07-22
---

# Domain 04 Race Framework

This document governs the mechanical race contribution budget for Domain 04.
It defines what race may affect in a build, what it must not affect, and how
race-related choices interact with faction, religion, `Legacy Technique`, and
`Faith Scar` systems. It is not a final race roster, selector flow, passive
list, stat table, or runtime evidence document.

## Source Of Truth

Canonical authority:

- `docs/design/classes/race-framework.md` race contribution budget and build
  boundary.
- `docs/design/classes/class-framework.md` class and subclass identity
  contract.
- `docs/design/classes/domain-02-03-alignment.md` Domain 02 persistence and
  Domain 03 upstream boundaries.
- `docs/design/classes/skill-library-contract.yaml` allowed skill source
  references.

Relevant Domain 08 inputs:

- `docs/contracts/faction-definition-template.yaml`
- `docs/contracts/religion-definition-template.yaml`
- `docs/contracts/organization-relationship-contract.yaml`

Legacy design inputs:

- `docs/character-sector-mmo-gap-teardown-2026-07-04.md` identifies the missing
  live race utility, world response, and social consequence loops.
- `docs/reputation-faction-foundation-v0.0.1.md` remains faction philosophy and
  anti-BiS input only.

Legacy inputs are not source truth for final race lists, final percentage
bonuses, concrete passives, faction membership rules, religion conversion
rules, or runtime ids.

## Ownership

Domain 04 owns:

- stable `race_id` semantics;
- race mechanical budget and validation rules;
- race contribution categories;
- race eligibility references used by class, skill, loadout, weapon school, and
  build validation;
- boundaries for race-linked `Legacy Technique` and `Faith Scar` build impact.

Domain 04 does not own:

- persisted character storage, owned by Domain 02;
- base stat dictionary, formulas, coefficients, damage events, resources,
  cooldowns, or PvP modifier rules, owned by Domain 03;
- item instances, gear data, recipes, or reward payloads, owned by Domain 05;
- faction and religion doctrine, membership, conversion, exile, treaties, and
  relationship state, owned by Domain 08;
- selector narrative, menu copy, HUD, and presentation flows, owned by Domain
  14.

## Mechanical Definition

A race is a character identity layer with a limited mechanical budget. It may
tilt how a build feels in narrow contexts, but must not become the primary
source of endgame damage, healing, mitigation, economy power, or class identity.

Race may provide:

- small identity utility that is useful across multiple classes;
- narrow contextual preference in world, exploration, crafting, or social
  interactions;
- limited defensive, mobility, sustain, resource-comfort, or recovery rules that
  reference approved Domain 03 contracts;
- eligibility tags for race-sourced skills or passive effects in the Domain 04
  skill library;
- requirement or flavor hooks consumed by Domain 08 faction and religion
  systems.

Race must not provide:

- a best-in-slot endgame DPS, healing, tanking, PvP, or profession route;
- a required class, subclass, weapon school, faction, or religion pairing;
- local formulas, local stats, hidden multipliers, mitigation order, cooldown
  exceptions, or resource semantics;
- unrestricted stacking with class, item, religion, faction, Legacy Technique,
  or Faith Scar effects;
- narrative selector obligations or final race content.

## Contribution Budget

Every race must fit into one bounded contribution package. The package is
validated by category and stack behavior, not by final tuning numbers.

| Budget area | Allowed contribution | Forbidden contribution |
| --- | --- | --- |
| Combat identity | Minor conditional utility, comfort, or survivability that remains useful for more than one class. | Primary throughput scaling, permanent role superiority, or a mandatory combat rotation piece. |
| World utility | Contextual traversal, sensing, access hinting, language, region, or NPC response hooks. | Exclusive progression routes that make other races invalid for core content. |
| Economy and profession | Narrow preference, discount class, recipe hint, gathering comfort, or faction-service affinity. | Race-locked best-in-slot economy engine or required profession pairing. |
| Social systems | Eligibility, prejudice, trust, ritual, faction, or religion modifiers owned narratively by Domain 08. | Race as automatic faction membership, religion, rank, exile, or trust resolution. |
| Skills and passives | Race-sourced `skill_id` or passive references inside the Domain 04 skill contract. | Inline effects, local stat math, or plugin ids treated as semantic race mechanics. |

Minimum budget rules:

- A race may not occupy more than one primary build niche. If it grants combat
  comfort, its world, economy, and social hooks must stay horizontal.
- Race benefits must have at least one meaningful non-combat use or identity
  expression so race is not selected only for damage math.
- Race effects must remain legible without external calculators. If a race
  requires hidden cap or breakpoint math to justify itself, it is too dominant
  for this framework.
- Race drawbacks may create trade-offs, but must not punish a class so hard that
  the pairing becomes fake or unusable.

## Allowed Effect Categories

Race mechanics may use these categories when backed by approved upstream
contracts:

- `race_passive`: always-on or conditional identity effect.
- `race_active`: limited active utility, never a class rotation requirement.
- `race_reactive`: response to a narrow condition, such as damage type, terrain,
  status, reputation event, or ritual state.
- `race_world_affinity`: exploration, NPC, region, language, detection, travel,
  or access preference.
- `race_profession_affinity`: narrow profession comfort or unlock preference.
- `race_social_affinity`: Domain 08-consumed relationship, trust, obligation, or
  prejudice hook.

All categories must reference approved Domain 03 `effect_id`, `formula_id`,
`coefficient_set_id`, `resource`, `cooldown`, and control contracts where combat
execution exists. Non-combat categories must still declare owner domain and
persistence owner.

## Anti-Mandatory Pairing Rules

Race-class pairing is valid only if multiple races remain plausible for every
class and every race remains plausible for multiple class roles.

Rules:

- No class may require a specific `race_id` for its baseline loop, subclass
  unlock, required skill, required weapon school, or mandatory resource model.
- No race may be tuned around one class as its primary intended solution.
- Race bonuses must not multiply only one class's best throughput path unless an
  equivalent horizontal benefit exists for other roles.
- Race drawbacks must not disable core class mechanics, required weapon schools,
  or baseline loadout eligibility.
- Race-specific skill references must be optional sidegrade or utility
  candidates, not mandatory loadout slots.
- PvP validation must check that race does not become an unavoidable counterpick
  for a class, arena role, or faction conflict mode.

Invalid examples:

- `race_id` grants the only viable cooldown reduction for one burst class.
- `race_id` is the only source of a resource comfort needed by one healer.
- `race_id` drawback makes a shield, bow, summon, stealth, or cast class fail its
  baseline loop.
- `race_id` plus one faction or religion creates the only optimal endgame path.

## Faction And Religion Boundary

Faction and religion are Domain 08 identity and relationship systems. Race may
influence how those systems initially respond, but race must not decide their
mechanical outcome.

Rules:

- Race may seed contextual modifiers such as affinity, suspicion, taboo, ritual
  access, or dialogue/world response.
- Race must not automatically grant faction membership, religion membership,
  rank, trust, exile, conversion, or reconciliation.
- Faction and religion build impacts must stay inside the Domain 04 contribution
  budget and must be declared as faction-sourced or religion-sourced effects,
  not hidden race effects.
- Religion is not a faction alias. Race hooks must preserve the distinction from
  `docs/contracts/religion-definition-template.yaml`.
- Faction reputation, membership, trust, trust debt, allegiance, and exile are
  separate axes under `docs/contracts/organization-relationship-contract.yaml`;
  race must not collapse them into one hidden score.
- Race/faction/religion stacks must be validated together so no combination
  becomes the required class path.

## Legacy Technique Boundary

`Legacy Technique` is permanent character progression state persisted by Domain
02 and mechanically bounded by Domain 04. A legacy technique may carry ancestry,
school, faction, or religion fiction, but it must not backdoor a second class.

Rules:

- `Legacy Technique` may reference race as eligibility, flavor, discovery, or
  quest context.
- Race may not make a `Legacy Technique` uniquely mandatory for a class build.
- Legacy effects must use approved Domain 03 contracts and Domain 04 skill or
  effect references.
- A race-linked legacy technique must be a sidegrade, specialization accent, or
  long-term identity hook, not a required throughput source.
- Changing race policy must not rewrite already persisted
  `character.progression.legacy_technique_ids` without an explicit migration
  contract.

## Faith Scar Boundary

`Faith Scar` is permanent or semi-permanent faith consequence state persisted by
Domain 02 and narrated through Domain 08 religion systems. It may affect build
identity, but only inside the Domain 04 race and religion contribution budgets.

Rules:

- `Faith Scar` may interact with race through ritual consequence, broken oath,
  taboo, reconciliation, or community response.
- A Faith Scar must not erase baseline class viability or lock a race into one
  religion or faction as the only practical path.
- Scar effects must not bypass class, loadout, resource, cooldown, PvP, or
  effect validation.
- Scar removal, conversion, and social consequence rules belong to Domain 08;
  their mechanical impacts must remain declared and validated by Domain 04.
- Changing race policy must not rewrite already persisted
  `character.progression.faith_scar_ids` without an explicit migration contract.

## Contract Shape

Minimum race definition shape:

```yaml
race:
  id: race_example
  schema_version: 1
  display:
    name_key: string
    description_key: string
    icon_ref: string
  identity:
    tags: [string]
    region_affinity_tags: [string]
    social_affinity_tags: [string]
  contribution_budget:
    package_id: race_budget_minor_identity
    combat_role: utility | comfort | survivability | none
    world_role: affinity | access_hint | traversal | sensing | none
    economy_role: profession_affinity | service_affinity | none
    social_role: faction_hook | religion_hook | npc_response | none
  mechanics:
    skill_ids: [string]
    passive_effect_refs: [string]
    active_effect_refs: [string]
    reactive_effect_refs: [string]
  boundaries:
    forbidden_class_lock_ids: [string]
    forbidden_subclass_lock_ids: [string]
    forbidden_mandatory_pairing_tags: [string]
    stack_group_id: string
  integration:
    persistence_owner: character_domain
    runtime_adapter_ref: string | null
  lifecycle:
    status: draft | validated | runtime_verified | released
```

This shape defines required fields only. It does not approve final race ids,
race names, effect values, final passives, or selector content.

## Domain Touchpoints

Domain 02:

- persists selected `character.identity.race_id`;
- persists learned or equipped race-sourced skill references where applicable;
- persists `character.progression.legacy_technique_ids` and
  `character.progression.faith_scar_ids`;
- does not define race meaning, race mechanics, or contribution budget.

Domain 03:

- owns stat dictionary, damage events, formulas, coefficients, resources,
  cooldowns, control taxonomy, and PvP modifier behavior referenced by race
  mechanics;
- must reject race mechanics that inline new combat math.

Domain 05:

- owns item instances, recipes, profession item payloads, equipment data, and
  economy reward objects consumed by race preferences.

Domain 08:

- owns faction, religion, organization, doctrine, membership, rank, conversion,
  exile, trust, oath, taboo, and world response semantics;
- consumes race affinity hooks but cannot use them to exceed Domain 04 build
  budget.

Domain 14:

- owns selector, HUD, menu, disabled-state messaging, and explanation of race
  requirements or warnings.

## Validation Requirements

Race framework validators must prove:

- every `race_id` is a stable Domain 04 semantic id persisted by Domain 02;
- every race has exactly one declared bounded contribution package;
- every combat-affecting race mechanic references approved Domain 03 contracts;
- every race-sourced skill reference exists in the Domain 04 skill library;
- no class or subclass requires one race for baseline viability;
- no race requires one class, faction, religion, Legacy Technique, or Faith Scar
  for practical viability;
- race, faction, religion, Legacy Technique, and Faith Scar stacking stays inside
  the declared budget;
- Domain 08 relationship state is not silently inferred from race alone;
- no final percentage bonuses, concrete race list, or selector narrative is
  required to validate this framework.
