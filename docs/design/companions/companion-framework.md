---
title: Domain 12 Companion Framework
status: draft
updated_at: 2026-07-31
---
# Companion Framework

## Purpose
Canonical framework for Domain 12 companion systems.

This document defines:
- the shared pet model;
- role and capability separation;
- activity and lifecycle rules;
- cross-system runtime budget and precedence rules;
- ownership and persistence boundaries;
- PvE, PvP, and pet-battle safety rules;
- boundaries to Domains 01-07, 14, and 15;
- integration expectations for PetCore, MCPets legacy migration context, ModelEngine, and runtime adapters.

This document does not define:
- damage, defense, threat, CC, or scaling formulas;
- Beastmaster or Warlock skill kits;
- item tier budgets or affix tables;
- encounter scripting or boss resist rules;
- HUD and menu implementation;
- asset production budgets or animation authoring rules.

## Domain Boundary
Domain 12 owns:
- pet taxonomy and capability model;
- owned pet records, active pet rules, and pet battle roster rules;
- pet progression loops: capture, breeding, training, evolution, pet skills, and pet equipment framework;
- appearance layer for pets and pet skins;
- pet lifecycle, death, respawn, recall, reconnect, and restart safety;
- mount-as-pet-role rules;
- runtime guardrails for AI, aggro, leash, teleport, and degradation;
- class summon boundary contract.

Domain 12 does not own:
- combat formulas and stat dictionaries: Domain 03;
- class-specific summon builds and rotations: Domain 04;
- pet equipment power budgets and affix economy: Domain 05;
- world progression gates and region progression locks: Domain 06;
- encounter safety exceptions and boss-side behavior: Domain 07;
- account and character persistence authority: Domains 01-02;
- UI, HUD, collection screens, and input surfaces: Domain 14;
- models, skins, animation asset production, and art budgets: Domain 15.

## Core Taxonomy
The framework uses one shared pet identity model and separates gameplay by role, not by unrelated entity types.

| Term | Meaning |
| --- | --- |
| `pet identity` | One concrete owned pet instance. |
| `species` | Canonical species family and its baseline rules. |
| `capability` | A role the pet is allowed to perform. |
| `active role` | The current role in runtime for the active pet. |
| `appearance` | Cosmetic presentation layer applied to a pet identity. |
| `battle roster` | Prepared set of pets eligible for consensual pet battles. |
| `character-ready` | A pet state that is bonded and usable by one specific character. |

## Identity And Runtime Fencing
Domain 12 runtime identity must bind to the accepted PetCore authority contract.

| Identifier | Meaning |
| --- | --- |
| `PetSubjectId` | canonical companion subject id, equal to active MMOProfiles `profileId` |
| `playerUuid` | live connected player transport identity, not durable companion subject |
| `sessionEpoch` | monotonic live-session fence for one `{playerUuid, PetSubjectId}` attachment |
| `runtimeEpoch` | monotonic runtime lease fence for one active actor reservation or executor |
| `petInstanceId` | canonical runtime instance id for one active pet or summon actor lease |

Hard rules:
- every occurrence of `character_id` in Domain 12 documents means `PetSubjectId`, not an undefined parallel identifier;
- `owner_scope` must resolve to `PetSubjectId` plus the current online `{playerUuid, sessionEpoch}` attachment when runtime is involved;
- changing active MMOProfiles character invalidates the prior `{playerUuid, PetSubjectId, sessionEpoch}` attachment immediately;
- any actor or reservation carrying an older `sessionEpoch` or `runtimeEpoch` is stale and must fail closed rather than resume;
- reconnect, transfer, restart recovery, and character swap must fence old executors by epoch before any restore attempt runs.

Hard rule:
- there are no `cosmetic-only` pets;
- all pets are gameplay pets;
- cosmetics are delivered through skins, appearance variants, summon FX, naming presentation, and similar presentation layers.

## Capability Model
Each pet identity may expose one or more capabilities.

| Capability | Function |
| --- | --- |
| `assist` | PvE support, helper behavior, follow, utility, or combat help in the main game. |
| `mount` | Travel role; the pet may be ridden when mount conditions are met. |
| `battle` | Eligibility for consensual pet-vs-pet battles. |

Capability model is hybrid:
- `species` defines the baseline capability envelope and legal growth paths;
- a specific pet unlocks some capabilities through progression, training, evolution, equipment gates, bonding, or similar systems;
- not every pet of a species must automatically unlock every possible role.

Examples:
- one pet may be `assist` only;
- another may be `assist + battle`;
- another may be `assist + mount + battle`;
- another may be `battle` only until a later unlock path grants `assist` or `mount`.

## Active Pet Model
One character may have only one active pet identity at a time.

| Layer | Scope | Runtime entity in main world |
| --- | --- | --- |
| `owned pet collection` | account and character ownership records | no |
| `active pet` | one active pet identity for one character | yes, if role requires it |
| `pet battle roster` | prepared set of legal battle pets | no, outside pet-battle runtime |

Rules:
- activating a new pet always removes or serializes the previous active pet state first;
- a player cannot keep one pet active as `assist` and another active as `mount` at the same time;
- `pet battle roster` may contain pets that are not the current active pet;
- pet battles may use any legal pet from the active character's battle roster, not only the currently active overworld pet.

## Combined Runtime Budget
Domain 12 defines one cross-system companion runtime budget per character.

| Budget concept | Meaning |
| --- | --- |
| `pet_core_actor_slot` | one Pet Core world actor slot, occupied by `active_assist` or `active_mount` |
| `primary_summon_slot` | one class primary companion actor slot |
| `auxiliary_summon_slots` | bounded secondary summon actor slots |
| `temporary_overflow_slots` | explicitly granted short-lived overflow only |
| `total_companion_actor_cap` | authoritative total active companion actor ceiling per character across all Domain 12 systems |

Framework rules:
- every active Pet Core actor and every active class summon consumes the same shared per-character runtime budget;
- `active_mount` occupies the `pet_core_actor_slot` and still counts against `total_companion_actor_cap`;
- `temporary_overflow_slots` never bypass `total_companion_actor_cap`;
- if a new actor would exceed the cap, the outcome must be deterministic: reject, replace, suppress, or consume the lower-priority actor according to the precedence order below.

Precedence order:
1. valid owner context and transition safety
2. `active_mount`
3. `primary_summon_slot`
4. `active_assist`
5. `auxiliary_summon_slots`
6. `temporary_overflow_slots`

Mounted rule:
- entering `active_mount` suppresses or despawns all class summons by default;
- downstream documents may define narrow exceptions only through explicit runtime-safe override, not by silence.

## Admission Transaction
All Domain 12 actor admissions and role switches must use one shared lease transaction.

Required sequence:
1. `reserve`
2. `transition`
3. `commit`
4. `rollback` on failure

Transaction rules:
- one coordinator authority owns `total_companion_actor_cap` admission and lease issuance;
- `reserve` allocates the slot plan, target actor kind, fresh `petInstanceId`, live `playerUuid`, `PetSubjectId`, `sessionEpoch`, and fresh `runtimeEpoch` before any visible world mutation;
- `transition` may suppress or recall lower-priority actors, but must not destroy the previous legal actor irreversibly until the target admission can still commit;
- `commit` publishes the new active lease as `{petInstanceId, PetSubjectId, playerUuid, sessionEpoch, runtimeEpoch}` and invalidates older competing leases;
- `rollback` restores the last valid actor set if the target admission fails before commit;
- stale executors with older `runtimeEpoch` must be ignored or removed on contact, never re-committed.

## Active Role Semantics
The same active pet identity may move between roles if it has the required capability.

| Role | Runtime meaning |
| --- | --- |
| `assist` | Default overworld and PvE helper role. |
| `mount` | Riding and travel role for a mount-capable pet. |
| `battle` | Pet-battle role in consensual pet-vs-pet play. |

Role switching rules:
- if the player mounts a mount-capable pet, the prior active non-mount pet runtime enters rollback-safe `transition` suppression or serialization after `reserve`, and is removed irreversibly only after successful `commit`;
- if the player mounts the same active pet identity, it switches from `assist` to `mount`;
- when the player dismounts, the system restores the last legal non-mount active role, usually `assist`;
- if the prior role is no longer legal because of death, cooldown, world rule, or missing readiness, the pet returns in `stabled` or `recalled` state instead;
- `battle` role is separate from overworld presence and does not imply the pet remains spawned as an overworld assist entity during pet battles.

## Mount-As-Pet Role
Mount is not a separate ownership system in this framework. It is a capability and active role that some pets may unlock.

This means:
- some pets can never be mounts;
- some pets can be mounts and assists;
- some pets can be mounts, assists, and battle pets;
- mount behavior does not make the pet a separate category of companion.

Hard rules:
- a mounted pet is still the same pet identity;
- mounting never creates a second active pet slot;
- mount role is for movement and travel, not for combat power;
- PvP handling for mounted state must use separate normalization, dismount, and lockout rules from normal pet-assist behavior.

## Appearance Layer
Appearance is a separate layer from power and legal role.

Appearance may include:
- model family presentation;
- skin;
- color variant;
- summon FX;
- cosmetic animation set;
- name presentation;
- quest, shop, or battle-pass cosmetic rewards.

Appearance rules:
- cosmetics do not define combat legality, role access, hitbox authority, or stat budget;
- cosmetic ownership may be account-wide where appropriate;
- cosmetic rewards may come from shop, quests, battle pass, or game progression;
- a rare skin does not grant combat advantage.

## Progression Model
Pet progression is a first-class gameplay system, not a minor utility track.

Framework scope includes:
- capture and acquisition;
- breeding and lineage;
- training and bonding;
- evolution;
- pet skills;
- pet equipment;
- pet battle participation.

Product direction:
- pet investment may create meaningful PvE value for players who develop the system seriously;
- pet progression must not grant direct PvP combat advantage;
- pet battles are separate consensual `pet vs pet` contests between players.

## Power Boundary
Domain 12 may define where pet power can apply, but not the formulas used to compute it.

Framework rules:
- pets may contribute meaningful value in PvE and general progression gameplay;
- pet-derived bonuses are nullified, normalized, or otherwise excluded from player-vs-player combat advantage;
- pet battles use their own battle-state and rule context;
- class summons do not inherit pet progression power by default.

Domain 12 must reference Domain 03 for:
- stat identifiers;
- formula identifiers;
- threat and aggro calculation;
- CC taxonomy;
- PvP normalization hooks.

## Ownership And Readiness
The framework separates collection ownership from character readiness.

| Layer | Default scope |
| --- | --- |
| `species discovery` | may be account-wide |
| `appearance collection` | may be account-wide |
| `pet identity ownership` | shared ownership record allowed |
| `bond`, `readiness`, `active loadout`, `equipment attunement`, `battle readiness` | character-bound |

Hard rules:
- a pet caught or raised on one character is not automatically fully ready on another character;
- only one character at a time may hold a pet in `character-ready` active state;
- changing the character that uses a pet must be an explicit operation, never an implicit login side effect;
- character swapping cannot bypass cooldowns, death penalties, fatigue, or other lock states;
- exact transfer workflow is downstream work, but the framework forbids instant unrestricted cross-character reuse of a fully prepared pet.

### Transfer Norms
Cross-character pet use follows `Soft Transfer`, not free reuse and not hard permanent character lock.

| Topic | Norm |
| --- | --- |
| transfer model | `Soft Transfer`: a pet may move between characters, but never as an instantly full-ready asset. |
| lineage | `lineage`, breeding history, evolution history, and core pet identity always travel with the pet. |
| active bond | only one character at a time may hold the pet as `bonded-ready`. |
| transfer trigger | transfer requires an explicit stable or system handoff action, never a passive relog or character swap. |
| runtime safety | an active pet must be `recalled` or safely despawned before transfer completes. |
| cooldown safety | transfer does not clear `downed`, injury, fatigue, summon lock, battle lock, or cooldown states. |
| equipment safety | equipment, loadout, and attunement do not transfer as guaranteed full-ready state; they require re-attunement, re-equip, or equivalent downstream validation. |
| readiness reset | after transfer, the pet keeps long-term identity and progression, but enters a partial readiness state on the new character instead of immediate full `character-ready`. |
| progression gate | after transfer, effective pet power and utility must be clamped by the receiving character's progression gates, so a late-game pet cannot trivialize early PvE or world progression. |
| battle-readiness gate | after transfer, pet-battle access and effective battle strength require the receiving character to satisfy battle-readiness rules; transfer alone must not unlock full pet-battle dominance on alts. |
| transfer lock | transfer must acquire an exclusive per-`pet_id` handoff lock before bond, readiness, equipment, or runtime state mutates. |
| rollback | failed transfer must roll back to the last valid owner, bond, and runtime state, never leave the pet in split ownership. |
| concurrent requests | if two transfer or rebond attempts race, only one request may commit; the loser must fail cleanly without mutating ownership or readiness. |

Transfer consequences:
- the pet remains the same pet;
- the new character does not inherit instant full practical use of that pet;
- long-term pet progression is preserved;
- short-term combat and battle readiness must be re-established on the receiving character.

## Canonical Lifecycle
Domain 12 uses one canonical lifecycle vocabulary across Pet Core, mounts, transfers, pet battles, and class summons.

| State | Meaning |
| --- | --- |
| `stabled` | Owned, inactive, and not occupying runtime. |
| `pending_spawn` | Admission has been accepted and runtime creation is in progress. |
| `active_assist` | Spawned as assist role. |
| `active_mount` | Active as mount role. |
| `active_summon` | Active as a class summon actor. |
| `battle_reserved` | Reserved for one legal pet-battle session and excluded from conflicting use. |
| `suppressed` | Temporarily inactive because of PvP, mount, encounter, budget, or rule gate. |
| `downed` | Defeated or disabled and not currently usable. |
| `recalled` | Explicitly removed by player or system. |
| `transition_blocked` | Temporarily blocked during world, instance, mount, or server transition. |

Derived meanings:
- `dismissed` is not a separate canonical runtime state; it is the action that returns the entity to `stabled`;
- `recovering` is a substatus layered on `downed`, `suppressed`, or `transition_blocked`, not a competing top-level state;
- `expiring` is a timed-summon substatus layered on `active_summon`, not a competing top-level state.

Required mappings:
- Pet Core documents must express active presence as `active_assist`, `active_mount`, or `battle_reserved`;
- class summon documents must express active summon presence as `active_summon`;
- mount documents must use `active_mount`, not local substitutes.

Lifecycle invariants:
- runtime admission is idempotent;
- reconnect or restart must not duplicate active pets;
- active runtime state is always bound to `character_id`, not only player UUID;
- character change forces controlled recall or serialization;
- owner despawn, disconnect, world transfer, instance transfer, or server transfer must safely remove or serialize the pet;
- AI must support degraded behavior under low TPS.

## Pet Battle Reservation
Pet battles require atomic reservation.

Framework rules:
- one `pet_id` may hold only one live `battle_reserved` state at a time;
- one `character_id` may hold only one live pet-battle session at a time unless a later document explicitly permits more;
- entering pet battle atomically reserves the pet and the character together;
- reservation must release idempotently on battle completion, disconnect, restart recovery, transfer failure, or session invalidation;
- `battle_reserved` excludes conflicting use of that `pet_id` as `active_assist`, `active_mount`, transfer source, or concurrent battle entry.

## Content Run And Fail-Closed Rules
Domain 12 must fence companion behavior by active content mode and fail closed on authority loss.

| Term | Meaning |
| --- | --- |
| `ContentRunId` | canonical active content or social-run identity used to validate companion eligibility in the current mode |
| `content_revision` | monotonic revision of provider-backed eligibility and mode state |

Framework rules:
- companion admission may depend on `ContentRunId` and current `content_revision` when the player is in group, social, or other content-run scoped modes;
- Adventurer and other non-pet companion policies must be validated through the same content-mode fence, not inferred ad hoc;
- if companion authority or provider state is unavailable, new actions fail closed immediately;
- when fail-closed is active, existing actors and reservations must be recalled, suppressed, or fenced within five ticks unless a stricter downstream contract applies;
- a stale `content_revision` must not authorize new spawn, restore, transfer, or battle entry.

## PvP Mode Transition
Domain 12 uses one canonical PvP transition boundary.

Transition order:
1. detect PvP mode entry
2. deny new Pet Core assist actions
3. disable or clear illegal pet targets and autonomous AI actions
4. resolve mount state
5. resolve class summon legality
6. enforce pet-battle overlap rules

Capability matrix:

| System state | Default PvP outcome |
| --- | --- |
| `active_assist` | moves to `suppressed` |
| Pet Core assist skills, debuffs, utility | disabled unless an explicit bounded PvP contract re-enables them |
| `active_mount` | force-dismounted immediately; remount denied while ordinary PvP mode persists unless an explicit bounded exception overrides it |
| class summons | legal only if Domain 03 and Domain 04 explicitly authorize that summon family in PvP; otherwise `suppressed` |
| `battle_reserved` | allowed only for explicit consensual pet battle, never as overlap with ordinary PvP combat |

Hard rules:
- ordinary PvP and consensual pet battle may not share the same active reservation on the same character at the same time;
- target acquisition and autonomous assist AI must be cleared before PvP enforcement commits;
- downstream documents may specialize bounded exceptions, but they must start from this matrix rather than replace it.

## Suppression Recovery
`suppressed` is a fenced temporary state, not an implicit promise to restore.

Every suppression must record:
- `suppression_reason`
- `suppression_epoch`
- `suppression_source_state`
- `resume_policy`

Allowed `resume_policy` values:
- `resume_if_context_valid`
- `expire_on_exit`
- `require_fresh_action`

Hard rules:
- restoration from `suppressed` must pass the same admission, epoch, and budget checks as a fresh runtime action;
- a stale suppression token may not restore an actor;
- if suppression reason changed since entry, the newer reason wins and prior resume intent is discarded.

## Aggro, Safety, And Runtime Guardrails
Companion runtime must not break encounter safety or server stability.

Framework guardrails:
- pets cannot remain active without a valid owning character context;
- pets require leash, teleport, and owner-distance rules;
- boss and encounter flows may force recall, passivity, or runtime degradation;
- pets must not create duplicate pulls or broken reset states through orphaned runtime;
- pathfinding, animation, packet, and AI update frequencies require hard budgets;
- ModelEngine and related adapters must respect configured model and animation budgets;
- `/reload` is not a supported lifecycle path.

Domain 12 defines the guardrails. Domain 07 defines encounter-specific exceptions and enforcement rules.

## Class Summons Boundary
Class summons are not pets from this progression loop.

Beastmaster and Warlock summons:
- do not inherit breeding, evolution, pet battle roster, or pet progression loops by default;
- do not become stronger because a cosmetic skin is rare;
- must use Domain 03 combat contracts and Domain 04 class design;
- may share runtime infrastructure or appearance attachment only through explicit integration contracts.

Framework intent:
- full separation between pet progression and class summon progression;
- shared adapters are allowed;
- shared semantic power systems are not assumed.

## Integration Contract Direction
Runtime integration should remain split by responsibility.

| Concern | Intended owner |
| --- | --- |
| pet semantic authority | PetCore |
| character identity and persistent character registry | Domains 01-02 authority |
| combat formula and PvP normalization hooks | Domain 03 |
| class summon semantics | Domain 04 |
| item budget and pet equipment economy | Domain 05 |
| world progression and region gating | Domain 06 |
| encounter enforcement and boss safety hooks | Domain 07 |
| presentation surfaces | Domain 14 |
| model, animation, and skin production | Domain 15 |
| physical entity, model, and animation runtime adapters | runtime adapter layer, including MythicMobs and ModelEngine integrations |

Legacy note:
- MCPets may inform migration or content import strategy;
- MCPets is not the durable semantic authority for this framework.

## Downstream Documents
This framework is the prerequisite for:
- `docs/design/companions/pet-core-system.md`
- `docs/design/companions/class-summons.md`
- `docs/design/companions/mount-system.md`

Expected follow-up scope:
- `pet-core-system.md`: progression, commands, AI, equipment behavior, death, anti-P2W, restart recovery;
- `class-summons.md`: Beastmaster and Warlock contract boundary;
- `mount-system.md`: mount-only restrictions, terrain, dismount, visibility, passenger and travel rules for mount-capable pets.

## Current Settled Defaults
| Topic | Settled default |
| --- | --- |
| pet system depth | full progression system with capture, breeding, training, evolution, pet skills, and pet battles |
| PvE value | meaningful and investment-sensitive |
| PvP value | normalized out or excluded |
| pet battles | consensual `pet vs pet` mode |
| capability model | hybrid `species envelope + per-pet unlocks` |
| active pet limit | one active pet identity per character |
| mount model | same pet identity may switch into mount role if capable |
| cosmetic model | all pets are gameplay pets; cosmetics come from skins and presentation layers |
| summon boundary | class summons remain separate from pet progression |
