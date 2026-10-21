---
title: Domain 12 Pet Core System
status: draft
updated_at: 2026-07-31
---
# Pet Core System

## Purpose
This document defines the core gameplay contract for the pet progression system described by [companion-framework.md](./companion-framework.md).

It covers:
- pet acquisition and collection;
- progression, bonding, training, evolution, and pet skills;
- active pet behavior in the main game;
- pet battle readiness and roster rules;
- pet equipment framework behavior;
- death, recall, resummon, and recovery rules;
- anti-P2W and anti-exploit boundaries.

It does not cover:
- Beastmaster and Warlock class summons;
- combat formulas or PvP math;
- UI screen layouts or control bindings;
- art production or ModelEngine asset standards.

## System Role
Pet Core is a full progression system, not a lightweight helper feature.

Players may use it for:
- capture and collection;
- breeding and lineage building;
- training and bonding;
- evolution and role growth;
- PvE assistance and utility;
- consensual pet-vs-pet battles.

Pet Core is allowed to matter in PvE. It is not allowed to create direct PvP player-power advantage.

## Pet Record Model
Each pet identity must carry a durable gameplay record.

| Field group | Purpose |
| --- | --- |
| `pet_id` | canonical identity of one concrete pet |
| `species_id` | species envelope and capability baseline |
| `lineage_id` | breeding line / ancestry trace |
| `owner_scope` | ownership record anchored to `PetSubjectId` and legal transfer scope |
| `bond_state` | relationship to the currently bonded character |
| `progression_state` | level, growth, training, evolution, unlocks |
| `capability_unlocks` | per-pet unlocked `assist`, `mount`, `battle` rights |
| `appearance_state` | selected skin and presentation cosmetics |
| `equipment_state` | attached pet equipment and attunement state |
| `battle_state` | pet-battle readiness, injuries, locks, and battle loadout |
| `runtime_state` | active role, recall state, cooldowns, and transfer locks |

`pet_id` stays stable across:
- character handoff;
- bonding changes;
- evolution;
- battle participation;
- appearance changes.

Runtime fencing:
- active runtime and battle reservation attach to `PetSubjectId = MMOProfiles.profileId`, not a free-form character key;
- live execution also carries `{playerUuid, sessionEpoch}` and a fresh `runtimeEpoch`;
- stale epochs must fail closed rather than restore or re-enter runtime.

## Acquisition And Collection
Pet acquisition may come from multiple sources.

Allowed acquisition families:
- capture in the world;
- quest rewards;
- breeding outcomes;
- progression rewards;
- event rewards;
- shop or pass rewards that grant appearance layers or bounded acquisition rights.

Hard rules:
- acquisition source does not bypass progression gates;
- monetized sources may not produce exclusive combat power that cannot be earned through gameplay;
- rare acquisition may affect prestige, collection rarity, or access path, but not create uncapped PvP advantage.

## Bonding And Character Readiness
A pet being owned is not the same as being ready for one character's use.

| State | Meaning |
| --- | --- |
| `unbonded` | owned but not practically ready for the current character |
| `partially_bonded` | usable in a limited state, pending readiness recovery |
| `bonded_ready` | fully ready for the bonded character under current gates |
| `bond_locked` | temporarily unavailable because of transfer, injury, or rule gate |

Rules:
- one pet may be `bonded_ready` for only one character at a time;
- rebonding is explicit and may impose cost, time, or progression requirements;
- transfer to another character never arrives as immediate `bonded_ready`;
- battle readiness is separate from basic assist readiness.

Atomicity rules:
- rebond, transfer, and battle reservation all require an exclusive per-`pet_id` lock;
- a `pet_id` in transfer handoff cannot simultaneously enter battle reservation or active runtime;
- a failed rebond or transfer attempt must roll back to the last valid bonded state.
- any bonded-state mutation must verify current `sessionEpoch`, `runtimeEpoch`, and `content_revision` before commit when runtime is affected.

## Progression Axes
Pet progression is multi-axis.

| Axis | Purpose |
| --- | --- |
| `growth` | broad pet development over time |
| `bond` | practical efficiency and readiness with a character |
| `training` | role specialization and capability maturation |
| `evolution` | major stage change or branch choice |
| `pet_skills` | active or passive pet-side abilities |
| `equipment` | pet-side role shaping and bounded stat or behavior adjustments |

Framework expectations:
- no single axis should fully replace all others;
- evolution is not the same as cosmetic skin swap;
- progression should support different pet fantasies without turning every pet into the same optimized template.

## Capability Unlock Paths
Capabilities are not assumed to be instantly available.

| Capability | Default unlock expectation |
| --- | --- |
| `assist` | usually early, but may require minimum readiness or training |
| `mount` | gated by species support plus pet-specific unlock path |
| `battle` | gated by pet-battle readiness rules |

Unlock sources may include:
- species baseline;
- evolution branch;
- bond threshold;
- training milestone;
- equipment or saddle-style gating;
- quest or mastery unlock.

Capability loss rules:
- a pet may temporarily lose practical access to a capability if readiness, injury, transfer, or progression gates are not satisfied;
- cosmetic changes never grant or remove capabilities.

## Active Pet Commands
Minimum pet command surface:
- `follow`
- `stay`
- `passive`
- `assist`
- `recall`
- `dismiss`

Command semantics:

| Command | Effect |
| --- | --- |
| `follow` | pet follows owner and remains ready to help under role rules |
| `stay` | pet holds location under leash and safety limits |
| `passive` | pet avoids proactive combat contribution except where safety fallback requires movement |
| `assist` | pet may support the owner according to its allowed role and AI package |
| `recall` | controlled removal that preserves pet selection state |
| `dismiss` | deliberate removal that clears active presence until manually reactivated |

Domain 14 owns the final presentation of these controls. Domain 12 owns their semantics.

## PvE Assist Rules
Assist pets may provide meaningful value in the main game.

Allowed value families:
- bounded combat help;
- resource or collection utility;
- exploration support;
- target support or debuff support;
- owner support behaviors.

Hard rules:
- pets may help, but they must not become unattended boss tanks or full replacements for player action;
- pet power must respect Domain 03 combat contracts;
- assist pets may matter in PvE, but their effectiveness may be gated by character readiness and progression;
- AI and pathfinding behavior must degrade safely under runtime pressure.

## Pet Equipment
Pet equipment is part of Pet Core, but its final stat budgets belong outside Domain 12.

Pet equipment may:
- shape role focus;
- unlock or reinforce specialization paths;
- improve readiness or utility expression;
- contribute bounded combat or battle behavior within approved budgets.

Pet equipment may not:
- bypass progression gates;
- act as hidden unrestricted second player gearset;
- grant direct PvP player-power advantage.

Pet equipment after transfer:
- does not become instantly full-ready on the new character;
- may require re-attunement, re-equip, or equivalent readiness recovery.

## Evolution
Evolution is a major gameplay progression step.

Evolution may change:
- capability availability;
- role focus;
- battle options;
- training branches;
- appearance options;
- pet-skill pool.

Evolution may not:
- silently rewrite the pet into a different identity;
- bypass transfer or readiness gates;
- convert a purely cosmetic reward into combat authority.

## Pet Skills
Pet skills are pet-owned progression features, not class skills.

Pet skills may include:
- assist behaviors;
- passive pet traits;
- battle-pet actions;
- mount-supportive utility where relevant.

Rules:
- pet skills are governed by pet progression, not by Beastmaster or Warlock class kits;
- pet skills may have separate behavior between main-game assist and pet-battle contexts;
- pet skills must plug into Domain 03 formulas where they affect combat outcomes.

## Pet Battle System Boundary
Pet battles are consensual `pet vs pet` encounters between players.

Core rules:
- pet battles are opt-in;
- battle eligibility comes from `battle` capability plus battle-readiness rules;
- pet battles may use any legal pet in the active character's battle roster;
- the current overworld active pet is not the only legal battle entry;
- battle state is separate from general overworld readiness.

Pet battle roster rules:
- roster size is finite and explicit;
- roster changes may be limited outside safe contexts;
- injuries, battle fatigue, or battle lockouts may apply after pet battles;
- transfer between characters cannot bypass battle-readiness rules.

Reservation rules:
- battle entry atomically creates `battle_reserved` for the selected `pet_id` and the owning `character_id`;
- battle entry must also bind current `PetSubjectId`, `sessionEpoch`, and `runtimeEpoch`;
- a reserved pet cannot simultaneously be used as `active_assist`, `active_mount`, transfer source, or second battle entrant;
- if reservation setup fails mid-flight, the reservation must be fully rolled back;
- reconnect or restart recovery must idempotently release stale battle reservations that cannot be reattached to a live legal session.

## Death, Downed State, Recall, And Recovery
Pet Core uses the canonical lifecycle from [companion-framework.md](./companion-framework.md).

Pet Core-specific interpretation:
- `active_assist` is the default world-active pet state;
- `active_mount` is the mounted role and excludes `active_assist`;
- `battle_reserved` is the only legal battle-entry state;
- `dismiss` is the action that returns the pet to `stabled`;
- `recovering` is a substatus layered on `downed`, `suppressed`, or `transition_blocked`.
- restore from `suppressed` follows the canonical `resume_policy` contract from [companion-framework.md](./companion-framework.md).

Hard rules:
- no duplicate respawn on reconnect or restart;
- no automatic full-power in-combat respawn after defeat;
- recovery may require cooldown, resource, safe context, or explicit action;
- outside combat, the system may restore an eligible last active pet in an idempotent way;
- transfer does not wipe downed or recovery states.

## Transfer And Alt Safety
Pet Core must preserve the shared-pet fantasy without enabling alt abuse.

Transfer is governed by the framework's `Soft Transfer` rules.

Operational consequences:
- the pet remains the same pet identity with the same lineage;
- the new character receives only partial readiness by default;
- full practical use must be rebuilt through bond/readiness recovery;
- character progression gates clamp effectiveness after transfer;
- pet-battle gates separately clamp battle access and battle effectiveness.

This is required so that:
- a late-game pet does not trivialize early PvE on a low-level alt;
- a strong battle pet does not dominate alt-side pet battles just because ownership exists;
- transfer remains meaningful without becoming unrestricted copy-paste power.

## Character Progression Gate
After transfer, pet effectiveness must respect the receiving character's progression stage.

This gate may limit:
- effective combat contribution;
- available utility features;
- accessible equipment effects;
- legal zones or activities for the pet;
- active pet-skill effectiveness.

Hard rule:
- a pet may preserve its own long-term progression while still being temporarily clamped in practical output for the receiving character.

## Battle-Readiness Gate
Battle eligibility is not ownership-only.

Battle-readiness may depend on:
- current bond state;
- battle injuries or fatigue;
- battle-specific training or loadout readiness;
- receiving character unlock state after transfer.

Hard rule:
- ownership plus `battle` capability is insufficient for full-strength pet-battle use if the receiving character has not re-established legal battle readiness.

## PvP Boundary
Pet Core must not give direct PvP player-power advantage.

Allowed:
- cosmetic presence;
- role identity and presentation;
- separate opt-in pet battles.

Not allowed:
- direct player-vs-player combat stat advantage from pet progression;
- transfer-based PvP advantage;
- monetization-based PvP pet power.

Transition rule:
- on entering PvP mode, `active_assist` moves to `suppressed` by default, and Pet Core skills or utilities lose illegal PvP effect unless a later contract explicitly re-enables a bounded subset.
- pet-battle reservation may not overlap ordinary PvP combat on the same character.

## Anti-P2W Rules
The system may monetize cosmetics, collection fantasy, and presentation, but not mandatory combat advantage.

Allowed monetization surfaces:
- skins;
- summon FX;
- cosmetic naming or presentation layers;
- account-wide appearance collection;
- bounded non-exclusive acquisition conveniences, if later approved.

Not allowed:
- shop-exclusive uncapped pet power;
- pay-only battle dominance;
- cosmetic rarity translating into stronger combat legality;
- monetized bypass of transfer/readiness/progression gates.

## Runtime And Recovery Rules
Pet runtime must remain resilient across server operations.

Hard rules:
- active runtime is keyed to `PetSubjectId`;
- restart recovery must be idempotent;
- owner disconnect, world transfer, instance transfer, profile swap, and server transfer require controlled recall or serialization;
- pets must not remain active after character swap;
- AI update loops, teleportation, animation sync, and pathfinding operate under explicit budgets;
- `/reload` is unsupported.

Fail-closed rules:
- if PetCore authority or required provider state is unavailable, new runtime and transfer actions fail closed immediately;
- existing active assist or battle reservations must be recalled, suppressed, or fenced within five ticks unless a stricter downstream contract applies.

## Integration Direction
| Concern | Expected owner |
| --- | --- |
| pet semantic authority | PetCore |
| formulas, threat, and PvP normalization | Domain 03 |
| class summons | Domain 04 |
| pet equipment numeric budgets | Domain 05 |
| encounter and boss-side safety | Domain 07 |
| player-facing menus and HUD | Domain 14 |
| model and skin asset production | Domain 15 |

## Open Implementation Follow-Ups
This document intentionally leaves the following to downstream tickets or documents:
- exact progression curves and caps;
- exact equipment slot schema;
- exact battle roster size;
- exact rebond cost or time model;
- exact downed and revival timings;
- exact transfer UX and stable flow;
- exact mount gating details for mount-capable pets.
