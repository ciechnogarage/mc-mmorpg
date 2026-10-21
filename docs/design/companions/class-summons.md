---
title: Domain 12 Class Summons Contract
status: draft
updated_at: 2026-07-31
---
# Class Summons

## Purpose
This document defines the Domain 12 framework contract for class summons, with Beastmaster and Warlock as the first required consumers.

It covers:
- the shared lifecycle and runtime boundary for class summons;
- separation from Pet Core progression;
- summon slots, activity limits, and runtime safety;
- scaling, threat, CC, despawn, reset, and PvP boundary expectations;
- integration direction with Domains 03, 04, 07, and runtime adapters.

It does not cover:
- concrete Beastmaster builds, talents, or skill kits;
- concrete Warlock spell rotations, minion rosters, or balance numbers;
- combat formulas, coefficient tables, or PvP math;
- pet capture, breeding, transfer, bonding, evolution, pet battles, or pet cosmetics economy.

## Domain Boundary
Domain 12 owns:
- the framework distinction between class summons and Pet Core pets;
- the minimum summon runtime contract;
- shared summon lifecycle states;
- summon activity and slot semantics;
- encounter safety and despawn/reset rules expected from summons.

Domain 12 does not own:
- class fantasy, build identities, and skill definitions: Domain 04;
- formulas, threat values, CC durations, resource math, and PvP normalization: Domain 03;
- encounter-specific enforcement and boss-side exception logic: Domain 07;
- summon UI or HUD surfaces: Domain 14;
- summon model production and animation asset budgets: Domain 15.

## Core Separation Rule
Class summons are not Pet Core pets.

Hard separation:
- class summons do not use pet capture, breeding, lineage, bonding, evolution, transfer, or pet-battle systems;
- class summons do not inherit account-wide pet ownership or `character-ready` pet states;
- class summons do not become stronger because a player has a stronger Pet Core pet;
- class summons do not consume Pet Core battle roster slots;
- class summons do not use pet `Soft Transfer` rules.

Allowed overlap:
- runtime infrastructure may be shared;
- appearance attachment may be shared only by explicit contract;
- class-facing presentation may borrow cosmetic layers if Domain 04 and Domain 15 explicitly allow it.

## Summon Taxonomy
This framework recognizes two primary summon families.

| Family | Role |
| --- | --- |
| `beast_companion` | long-lived class companion central to Beastmaster gameplay |
| `skill_minion` | summon created from class skills, resources, rituals, corpses, curses, or other class mechanics |

The framework also recognizes activity behavior classes:

| Behavior class | Meaning |
| --- | --- |
| `persistent` | expected to remain active until death, dismissal, or invalid owner context |
| `timed` | expires after duration |
| `conditional` | exists while a class condition remains true |
| `rotational` | frequently enters and leaves play as part of skill flow |

## Beastmaster Contract
Beastmaster's beast companion is a class summon, not a Pet Core pet.

Framework rules:
- Beastmaster may rely on a primary beast as a core part of class identity;
- the beast may contribute meaningful class power in PvE and other allowed modes;
- the beast's progression is character-facing through class systems, not through pet capture, breeding, transfer, or pet-battle loops;
- Beastmaster appearance may optionally consume approved cosmetic layers without inheriting pet progression power;
- the beast must remain subject to summon slot, threat, CC, death, and reset rules from this document and Domain 03.

## Warlock Contract
Warlock minions are class summons derived from class execution, not Pet Core ownership.

Framework rules:
- Warlock summons may be timed, persistent, rotational, or conditional depending on class kit;
- Warlock summon count must obey explicit active slot limits;
- Warlock minions may not bypass the class's legal combat loadout or resource boundaries;
- Warlock minions must obtain combat meaning through Domain 03 contracts and Domain 04 class definitions;
- Warlock minions are never treated as catchable, breedable, transferable pets.

## Summon Slot Model
Class summons must use explicit slot semantics.

| Slot concept | Meaning |
| --- | --- |
| `primary_summon_slot` | one long-lived central summon slot, mainly for Beastmaster-style companions |
| `auxiliary_summon_slots` | bounded secondary slots for minions or support summons |
| `temporary_overflow_slots` | optional short-lived override allowed only when Domain 04 explicitly grants it |

Framework rules:
- one character may have at most one `primary_summon_slot` occupied at a time;
- auxiliary summon count must be hard-capped;
- temporary overflow slots must be explicit, bounded, and easy to reason about;
- slot count is a class contract concern, but slot categories are standardized here;
- every summon slot also consumes the shared `total_companion_actor_cap` defined in [companion-framework.md](./companion-framework.md).

## Activity And Runtime Presence
Class summons are separate from the active Pet Core pet identity.

This means:
- a character may have a legal Pet Core active pet and legal class summons at the same time if downstream balance and runtime limits allow it;
- summon runtime still counts toward global actor and AI budgets;
- Pet Core's one-active-pet rule does not cap class summons;
- class summons must not be modeled as alternative roles of the player's active Pet Core pet.

Mounted-state rule:
- entering `active_mount` suppresses or despawns class summons by default;
- a mounted player may not retain normal class-summon combat pressure unless a downstream class rule explicitly re-enables a bounded exception;
- remount and dismount flows must not duplicate or resurrect suppressed summons outside legal class recovery rules.

Suppression recovery contract:
- each suppressed summon must record `suppression_reason`, `suppression_epoch`, `suppression_source_state`, and `resume_policy`;
- `resume_if_context_valid` may restore only through fresh admission checks and current epoch validation;
- `expire_on_exit` discards the summon when the blocking context ends;
- `require_fresh_action` never auto-restores and waits for a new legal class action.

## Lifecycle States
Class summons use the canonical lifecycle from [companion-framework.md](./companion-framework.md).

Class-summon mapping:
- `pending_spawn` maps to canonical `pending_spawn`;
- active legal presence maps to canonical `active_summon`;
- timed expiry is a substatus layered on `active_summon`, not a separate top-level state;
- encounter, mount, PvP, or rule shutdown maps to canonical `suppressed`;
- explicit removal maps to canonical `recalled` or `stabled` depending on whether the class retains selected summon identity;
- invalid owner or session context maps to canonical `transition_blocked` followed by cleanup or rollback.

Lifecycle invariants:
- summon spawn must be idempotent at the framework level;
- owner disconnect, death, profile swap, world transfer, instance transfer, or server transfer must not leave orphaned summons;
- summons may not remain active after character swap;
- startup or reconnect must not duplicate legal summon state.
- active summon leases must fence on `PetSubjectId`, `sessionEpoch`, and `runtimeEpoch`, not an undefined local owner key.

## Spawn And Despawn Rules
Summon creation must be class-authorized and runtime-safe.

Hard rules:
- every summon spawn must resolve to a valid owning `PetSubjectId = MMOProfiles.profileId`;
- if slot budget is full, new summon behavior must be explicit: reject, replace, consume, or rotate;
- despawn policy must be explicit for quit, death, disconnect, instance reset, encounter reset, and class unload cases;
- a summon that loses its valid owner context must safely despawn or serialize according to downstream class rules.
- summon admission must use the shared `reserve -> transition -> commit / rollback` transaction from [companion-framework.md](./companion-framework.md).

## Scaling Contract
Class summons must scale through Domain 03 combat contracts, not local ad hoc math.

Framework rules:
- summon stats must reference approved Domain 03 contracts;
- scaling may be snapshot-based, dynamic, or hybrid, but the chosen behavior must be explicit in class-facing downstream documents;
- class summon scaling may depend on class loadout, skill rank, resource state, equipment, or approved modifiers;
- class summon scaling may not depend on Pet Core growth, pet lineage, pet transfer state, or pet-battle readiness.

## Threat And Aggro
Threat behavior is part of summon safety and combat integration.

Framework requirements:
- summon threat behavior must be mediated by Domain 03 threat contracts;
- summons must not create encounter-breaking unattended tank states without explicit class intent and encounter approval;
- summons must obey leash, owner-distance, and reset-safe behavior;
- summons must not keep bosses or encounters alive after the owner is no longer in a valid combat context.

Beastmaster note:
- a central beast may legitimately hold meaningful aggro if the class is designed around it, but still only within Domain 03 threat rules and Domain 07 encounter safety constraints.

## CC And Status Interaction
Class summons must participate in crowd control and status rules through Domain 03.

Framework rules:
- summons may be targetable, disabled, interrupted, displaced, or otherwise controlled only through legal Domain 03 taxonomies;
- immunity exceptions must be explicit, not implied from summon type;
- CC interaction must remain understandable in PvE and PvP contexts.

## Target Limits And Control
Summons may not act as unrestricted extra players.

Hard rules:
- class summons must use bounded targeting and control logic;
- multi-target or swarm behaviors require explicit runtime budgeting;
- summons must not bypass player loadout intent by carrying uncapped autonomous utility or damage packages;
- any auto-targeting or retargeting logic must respect owner validity, leash rules, and encounter safety.

## Death And Recovery
Summon death handling must be explicit and class-safe.

Allowed downstream patterns:
- cooldown-based resummon;
- resource-cost resummon;
- class-specific revive or rebuild flow;
- out-of-combat recovery.

Framework prohibitions:
- no uncontrolled immediate infinite combat respawn;
- no summon death reset by changing character;
- no summon persistence across invalid owner context.

## Encounter Reset And Boss Safety
Class summons must not break encounter lifecycle.

Hard rules:
- encounter reset may force summon despawn, suppression, or target clear;
- boss mechanics may force recall, passivity, suppression, or degradation;
- summons must not preserve illegal combat state after wipe, reset, or owner invalidation;
- class summons count toward encounter runtime budgets and actor budgets.

Domain 07 owns exact encounter exceptions and enforcement specifics.

## PvP Boundary
Class summons may exist in PvP only through Domain 03 and Domain 04 approved rules.

Framework rules:
- PvP summon behavior must use the same normalization authority as player combat;
- summon presence in PvP does not license pet progression power to enter PvP;
- summon count, uptime, survivability, and CC relevance in PvP must be explicitly bounded;
- no class summon may smuggle Pet Core progression advantage into PvP.

Transition rule:
- on entering PvP mode, class summons remain legal only if Domain 03 and Domain 04 explicitly authorize that summon family in PvP;
- otherwise they move to `suppressed`.
- if PvP keeps a summon legal, autonomous targeting and AI behavior must still obey the canonical PvP matrix and any explicit bounded PvP summon contract.

## Appearance And Cosmetics
Class summons may optionally use appearance layers, but cosmetics remain cosmetic.

Allowed:
- summon skins or presentation variants;
- summon FX or thematic presentation;
- approved appearance mapping from Domain 15 content pipelines.

Not allowed:
- cosmetic rarity defining summon power;
- cosmetic ownership granting class summon capability;
- cosmetic overlap causing class summons to enter Pet Core transfer or pet-battle systems.

## Runtime Budgets
Class summons must stay within explicit technical budgets.

Budget domains include:
- active summon count per player;
- active high-fidelity models per chunk or encounter space;
- AI tick frequency;
- pathfinding cadence;
- teleport cadence;
- animation sync frequency;
- packet pressure.

Rules:
- class summons and Pet Core pets share world runtime budget pressure even though they are different systems;
- admission, suppression, and overflow resolution must respect the shared precedence order from [companion-framework.md](./companion-framework.md);
- low TPS conditions must degrade summon behavior safely;
- ModelEngine, MythicMobs, and any custom runtime adapter must respect hard budgets.

## Integration Direction
| Concern | Expected owner |
| --- | --- |
| class summon framework boundary | Domain 12 |
| class identity, skill triggers, summon kit meaning | Domain 04 |
| formulas, resources, threat, CC, PvP normalization | Domain 03 |
| encounter reset and boss safety specifics | Domain 07 |
| summon UI/HUD surfaces | Domain 14 |
| summon models, skins, animations | Domain 15 |
| runtime spawning, pathfinding, animation execution | runtime adapter layer |

## Explicit Non-Goals
This document does not define:
- Beastmaster talents;
- Warlock minion list;
- exact summon slot counts per class;
- exact snapshot vs dynamic scaling choice per summon;
- exact PvP coefficients;
- exact summon UI commands or HUD layouts.

Those belong to downstream class or combat documents once upstream contracts are ready.
