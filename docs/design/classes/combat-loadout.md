---
title: Domain 04 Combat Loadout Contract
status: proposed
updated_at: 2026-07-22
---

# Domain 04 Combat Loadout Contract

This document governs Domain 04 combat loadout shape: slot counts, slot types,
eligibility, edit restrictions, and persistence expectations. It is not a bank,
storage, stash, HUD, keybind layout, concrete skill, or class build document.

## Source Of Truth

Canonical authority:

- `docs/design/classes/combat-loadout.md` combat loadout slot model and edit rules.
- `docs/design/classes/class-framework.md` class/subclass loadout references.
- `docs/design/classes/skill-library-contract.yaml` skill identity and eligibility references.
- `docs/design/classes/domain-02-03-alignment.md` Domain 02 persistence and Domain 03 combat boundaries.

Legacy design inputs:

- `docs/loadout-bank-storage-foundation-v0.0.1.md` preparation philosophy and
  storage boundary.
- `docs/progression-respec-foundation-v0.0.1.md` loadout, respec, and combat
  lockout intuition only.

## Scope

Combat loadout is a preparation and eligibility contract. It determines which
learned, eligible Domain 04 skills and Relic reference are equipped before
combat begins.

Combat loadout must not:

- define individual skills, concrete class builds, damage formulas, cooldown
  values, resources, or stat behavior;
- provide bank, stash, storage, salvage, or inventory UX rules;
- define Domain 14 HUD placement, icons, hotbar rendering, or keybind UI;
- bypass stat, mastery, item, class, subclass, cooldown, or resource requirements;
- reset cooldowns, charges, resource state, or unlock state when swapped.

## Slot Model

V1 combat loadout exposes these semantic slots:

| Slot type | Count | Triggered by player | Purpose |
| --- | ---: | --- | --- |
| `active_skill` | 4 | Yes | Primary combat actions. |
| `mobility_skill` | 1 | Yes | Movement, repositioning, engage, disengage, or traversal action. |
| `utility_skill` | 1 | Yes | Non-primary action such as cleanse, ward, reveal, deploy, support, or setup. |
| `passive_skill` | 3 | No | Always-on, conditional, reactive, or rules-modifying choices. |
| `relic` | 1 | Yes | Dedicated Relic input and equipped Relic reference. |

`active_skill`, `mobility_skill`, and `utility_skill` are triggered combat skill
slots. Only `active_skill` consumes the four general active slots.
`mobility_skill` and `utility_skill` are dedicated triggered slots and may not be
used to exceed the active slot limit.

`passive_skill` slots do not consume active slots unless a future accepted
Domain 04 contract explicitly creates a hybrid slot type. A passive may reference
Domain 03 effects or conditions through the skill library contract, but this
loadout contract does not define those effects.

## Relic Slot

`relic` is separate from all skill slots.

Rules:

- `Relic` must not occupy `active_skill`, `mobility_skill`, `utility_skill`, or
  `passive_skill`.
- `Relic` has one dedicated equipped slot and one dedicated Domain 14 input.
- Ordinary accessory, artifact, bracelet, or armor slots must not silently act as
  Relic substitutes.
- Relic item slot identity and item data belong to Domain 05. Domain 04 only
  owns whether a class/subclass/loadout rule allows a Relic reference and which
  combat activation contract it may point at.
- Active Relic effects must resolve through approved Domain 03 combat, cooldown,
  resource, and effect contracts. This document does not define Relic effects.

## Eligibility Rules

A loadout entry is valid only when all referenced contracts agree:

- `skill_id` is learned or otherwise unlocked in Domain 02 persisted state.
- `skill_id` exists in the Domain 04 skill library contract.
- the slot type accepts the skill source/type tags;
- class and subclass requirements are satisfied;
- weapon school, item, state, mastery, and resource requirements are satisfied;
- Domain 03 formula, effect, cooldown, resource, and control references resolve;
- the loadout does not exceed any slot count.

Invalid entries must fail closed: unequip, disable, or reject the edit according
to the runtime adapter, but must not execute from the wrong slot type.

## Skill Swapping

Skill swapping means editing any `active_skill`, `mobility_skill`,
`utility_skill`, `passive_skill`, or `relic` loadout entry.

Allowed edit context:

- player is out of combat;
- player is in an approved preparation context such as a safe zone, class/loadout
  NPC, or accepted preparation menu;
- all requirements are checked at edit time and again before execution.

Forbidden edit context:

- player is in combat;
- player is under PvP combat lockout after dealing or receiving damage;
- player is inside a dungeon, arena, battleground, encounter, or other content
  context that declares loadout editing closed;
- player attempts to swap to reset cooldowns, charges, resources, or temporary
  state.

Swapping must not create instant counter-pick behavior during combat. A failed
swap must leave the last valid loadout state intact or move to an explicit safe
empty slot state.

## Combat Lockout

Combat lockout blocks loadout editing, not skill execution from the already
equipped loadout.

Minimum triggers:

- dealing damage;
- receiving damage;
- applying or receiving a combat effect that the Domain 03 contract marks as a
  combat engagement;
- entering content whose rules explicitly close loadout edits.

The exact duration is runtime policy, not this document. The semantic rule is
that lockout must be long enough to prevent in-fight skill, passive, utility,
mobility, or Relic counter-swapping.

Combat lockout must apply equally to PvE and PvP where the active content rule
requires it. PvP must at minimum lock loadout edits briefly after dealing or
receiving damage.

## Persistence Expectations

Domain 02 persists selected character loadout state. Domain 04 owns meaning and
validation of the identifiers.

Minimum persisted shape:

```yaml
character.loadout:
  active_loadout_id: string
  equipped_skill_slots:
    active_skill:
      - slot_index: 0
        skill_id: string | null
      - slot_index: 1
        skill_id: string | null
      - slot_index: 2
        skill_id: string | null
      - slot_index: 3
        skill_id: string | null
    mobility_skill:
      skill_id: string | null
    utility_skill:
      skill_id: string | null
    passive_skill:
      - slot_index: 0
        skill_id: string | null
      - slot_index: 1
        skill_id: string | null
      - slot_index: 2
        skill_id: string | null
  equipped_relic_ref: string | null
```

Reconnect expectations:

- reconnect restores the last valid active loadout;
- reconnect must not duplicate learned skills, unlocks, Relics, or passive state;
- reconnect must not reset cooldowns or resource state through loadout restore;
- invalid now-ineligible entries must be disabled or unequipped until the
  requirement becomes valid again.

Restart expectations:

- server restart preserves selected slot identifiers and active loadout id;
- restart must not duplicate unlocks, create extra equipped entries, or bypass
  slot limits;
- runtime adapters must revalidate loadout entries before execution after boot.

Combat lockout timers may be runtime state, but reconnect/restart handling must
not allow a player to evade an active content lockout or immediate PvP edit
restriction.

## Domain Touchpoints

Domain 02:

- persists selected `class_id`, `subclass_id`, learned skills, equipped skill
  slots, active loadout id, and Relic reference;
- does not define loadout semantics, skill meaning, slot compatibility, or
  combat formulas.

Domain 03:

- owns formulas, effects, resources, cooldowns, controls, damage events, and PvP
  modifier rules referenced by equipped skills and Relic activation contracts.

Domain 05:

- owns item instances, equipment slot registry, Relic item data, and item
  compatibility rules consumed by loadout eligibility.

Domain 14:

- owns HUD, menu, input presentation, keybind UI, disabled-state messaging, and
  requirement/error display;
- consumes from Domain 04: slot counts, slot types, dedicated Relic input,
  edit-lock rules, and unmet requirement messages.

## Validation Requirements

Validators for loadout contracts must prove:

- no loadout can exceed slot counts;
- a skill cannot be equipped into a wrong slot type;
- passive skills do not consume active slots without an explicit accepted hybrid
  contract;
- combat lockout blocks all loadout edits;
- reconnect preserves the last valid loadout without duplicate unlocks;
- restart preserves identifiers without duplicate unlocks or slot entries;
- `Relic` does not consume any ordinary skill slot.
