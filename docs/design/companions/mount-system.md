---
title: Domain 12 Mount System
status: draft
updated_at: 2026-07-31
---
# Mount System

## Purpose
This document defines the Domain 12 contract for mount behavior inside the companion framework.

It covers:
- mount capability as a pet role;
- mount activation, riding, and dismount rules;
- world, combat, PvP, and encounter restrictions;
- visibility, passenger, and transition expectations;
- runtime and ModelEngine budget constraints.

It does not cover:
- pet capture, breeding, lineage, transfer, or pet-battle systems;
- Beastmaster or Warlock class summon rules;
- combat formulas, PvP coefficients, or dismount damage math;
- HUD, mount menu, or input presentation;
- model art direction, blueprint production, or animation authoring workflow.

## Core Definition
Within Domain 12, a mount is not a separate ownership system. A mount is:
- a capability some pets may unlock;
- an `active_mount` role that the current active pet identity may enter;
- a movement and travel function, not a combat power system.

Hard rules:
- a mounted pet is still the same `pet_id`;
- mounting never creates a second active pet slot;
- pets without `mount` capability can never be mounted;
- mount role does not turn the pet into a class summon or a separate vehicle system outside pet identity.

## Domain Boundary
Domain 12 owns:
- mount capability semantics;
- mount activation and role-switch rules;
- mount restrictions during combat, encounters, and world transitions;
- mount passenger rules and visibility expectations;
- mount runtime and technical budget guardrails.

Domain 12 does not own:
- PvP normalization formulas and rider damage rules: Domain 03;
- encounter-specific mount bans or forced dismount mechanics: Domain 07;
- menu, HUD, and player input presentation: Domain 14;
- mount models, animations, skin assets, and blueprint quality: Domain 15.

## Mount Capability Model
Mount access is hybrid and progression-gated.

| Layer | Meaning |
| --- | --- |
| `species mount envelope` | species may or may not support mount growth at all |
| `pet unlock` | specific pet has unlocked legal mount use |
| `character-ready mount use` | bonded character is currently allowed to ride that pet |

Framework rules:
- a species may support `mount` capability without every pet of that species automatically being mount-ready;
- mount use may depend on bond, training, evolution, equipment, quest progression, or equivalent pet-side unlocks;
- cosmetic skins never grant mount capability on their own.

## Mount Activation
Mounting is a role switch of the current active pet.

Allowed flows:
1. a mount-capable active assist pet switches into `active_mount`;
2. a non-mounted active pet is replaced by another pet that is both active and mount-capable, then enters `active_mount`;
3. dismount restores the last legal non-mount role, usually `active_assist`.

Hard rules:
- mounting moves the prior non-mount runtime presence into rollback-safe `transition` suppression or serialization after `reserve`; irreversible removal happens only after successful `commit`;
- a player cannot ride one pet while a second pet remains active as `assist`;
- if the prior active state is no longer legal after dismount, the pet returns as `recalled`, `stabled`, or another valid non-mounted state;
- mount activation must be idempotent across reconnect, restart, and transition handling.
- mount activation must use the shared `reserve -> transition -> commit / rollback` admission transaction from [companion-framework.md](./companion-framework.md).

## Movement-Only Contract
Mount role is a travel role, not a combat role.

Allowed mount value:
- movement speed;
- traversal convenience;
- travel identity and prestige;
- world access only where world progression explicitly permits it.

Mount role may not:
- act as a combat pet while mounted;
- serve as a tank or damage actor during riding;
- bypass encounter mechanics by default;
- smuggle pet progression into PvP combat power.

## Combat Restrictions
Mounted state must be stricter than assist-pet state.

Framework defaults:
- mounting is disallowed in contexts where combat lockout says it is illegal;
- entering combat while mounted may force dismount, suppress mount benefits, or block remounting for a cooldown window;
- active mount state must not preserve illegal movement advantage through combat lock, PvP lock, or encounter lock;
- mount role does not coexist with active assist behavior;
- entering `active_mount` suppresses or despawns active class summons by default, so mounted state does not preserve normal summon combat pressure.

Downstream documents may choose exact lock semantics, but the framework requires them to be explicit.

## World And Zone Restrictions
Mount use is subject to world progression and world-rule gates.

Possible gates include:
- safe-zone only;
- overworld allowed, dungeon disallowed;
- disallowed in tight encounter spaces;
- disallowed in contested or event-controlled areas;
- gated by world progression, region unlock, or narrative milestone.

Hard rules:
- mount use must not bypass intended exploration locks or progression gates;
- world or encounter rules may suppress mount activation even if the pet itself is legally mount-capable;
- zone restrictions must be deterministic and visible to the player through Domain 14 surfaces.

## Encounter Safety
Mounts must not break Domain 07 encounter assumptions.

Framework rules:
- bosses, instances, and authored encounters may force dismount, recall, suppression, or mount denial;
- encounter reset must clear illegal mounted state;
- mount use must respect leash, actor budget, and high-fidelity model budget constraints;
- mount role must not allow players to skip telegraphs, traversal checks, or space-control mechanics unless the encounter explicitly allows it.

Domain 07 owns exact encounter exceptions, fail-state interactions, and per-encounter bans.

## PvP Boundary
Mounts require separate PvP handling and cannot inherit normal travel freedom into PvP.

Framework rules:
- PvP riding rules must be explicit, not assumed;
- mounted state is force-dismounted immediately in PvP contexts, and remount is denied while ordinary PvP mode persists unless an explicit downstream exception overrides it;
- pet progression behind a mount may not become direct PvP advantage;
- if PvP allows mount presence at all, dismount rules, cooldowns, vulnerability windows, and movement normalization must be defined by downstream PvP/combat contracts.

## Dismount Rules
Dismount behavior must be deterministic.

Triggers may include:
- manual dismount;
- entering blocked combat state;
- entering blocked zone;
- taking dismount-triggering damage;
- scripted encounter or world mechanic;
- invalid owner or runtime context.

Required outcomes:
- rider leaves mount role cleanly;
- active pet returns to last legal non-mount state if possible;
- if return is not legal, the pet moves into `recalled`, `transition_blocked`, `downed`, or equivalent valid state;
- dismount cannot duplicate pet runtime presence.

## Passenger Rules
Passenger behavior must be explicit and bounded.

Framework defaults:
- one rider seat is the baseline assumption;
- extra passenger seats are opt-in content, not a default mount property;
- passenger support, if any, must be explicit per mount family and must obey visibility, collision, and runtime safety limits;
- passenger support may be globally disabled until a later document enables it.

## Visibility And Presentation
Mount presentation remains cosmetic around a fixed semantic core.

Allowed:
- skins;
- color variants;
- summon or mount-up FX;
- approved model swaps within hitbox and budget rules;
- prestige presentation tied to quests, events, battle pass, or shop cosmetics.

Not allowed:
- cosmetic rarity changing movement legality;
- cosmetic rarity bypassing world gates;
- cosmetic unlocks rewriting mount capability if the pet is not progression-ready.

## Flight And Vertical Mobility
Flight is not assumed.

Framework default:
- no universal flight;
- any future flight-capable mount is an exceptional content path that requires explicit world, encounter, and PvP review.

Before any flying mount is allowed, downstream approval must define:
- zone whitelist/blacklist behavior;
- encounter bans;
- fall, dismount, and transition handling;
- vertical traversal exploit protections;
- runtime budget implications.

## Transition And Recovery Rules
Mount state must remain safe across runtime transitions.

Hard rules:
- mounted state is keyed to `PetSubjectId`;
- reconnect and restart recovery must be idempotent;
- world transfer, instance transfer, server transfer, or character swap must safely exit mount role;
- the system must not leave orphaned mounted pets after disconnect or owner invalidation;
- mount restoration after reconnect is allowed only if the target world and rules still permit it.
- stale `sessionEpoch` or `runtimeEpoch` may not restore a mount lease.

Lifecycle mapping:
- mount activation uses canonical `pending_spawn` followed by `active_mount`;
- forced mount shutdown uses canonical `suppressed`, `recalled`, or `transition_blocked` depending on cause;
- dismount is a role exit, not a second actor spawn.

## Technical Budgets
Mounts share the same runtime pressure as other high-fidelity companion actors.

Budget domains include:
- active mounted pets per player;
- active mount-capable high-fidelity models per chunk;
- animation update frequency;
- movement sync cadence;
- teleport cadence;
- packet pressure;
- pathfinding and follow fallback when leaving mount role.

Rules:
- low TPS conditions must degrade mount-side behavior safely;
- ModelEngine, MythicMobs, and custom runtime adapters must obey explicit hard budgets;
- mount visual fidelity must not override runtime safety.

## Integration Direction
| Concern | Expected owner |
| --- | --- |
| mount capability and role semantics | Domain 12 |
| formulas, PvP normalization, damage-triggered dismount math | Domain 03 |
| world progression gates | Domain 06 |
| encounter-specific bans and forced dismount logic | Domain 07 |
| HUD, menus, controls, player messaging | Domain 14 |
| model, animation, skin, and blueprint production | Domain 15 |
| runtime spawning, riding execution, animation, movement sync | runtime adapter layer |

## Explicit Non-Goals
This document does not define:
- exact mount speed tiers;
- exact dismount cooldown durations;
- exact PvP dismount triggers;
- exact saddle or equipment schema for mount unlock;
- exact passenger feature availability;
- any flying mount rollout.

Those require downstream balancing, world, encounter, and UI decisions.
