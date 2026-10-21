---
title: Encounter Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-08-12
---

# Encounter Framework

## Purpose

This document is the canonical PvE encounter contract for domain 07.

It defines the shared structure for:

- trash packs;
- elite packs;
- minibosses;
- bosses;
- floor encounter rooms;
- raid encounters;
- public PvE event encounters.

This document is structural only. It does not own numeric combat formulas, item stat budgets,
class kits, or reward economy.

## Domain Boundary

Domain 07 owns:

- encounter roles and pack composition rules;
- encounter lifecycle and fail-state structure;
- encounter pressure model;
- runtime budget caps and overflow behavior;
- telegraph and interrupt standards;
- 1-4 player scaling contract;
- structural difficulty modifiers;
- MythicMobs and ModelEngine runtime budgets.

Domain 07 does not own:

- HP, damage, defense, threat, CC formulas, DR, burst limits;
- class kits, loadouts, and utility distribution;
- item power, affixes, and stat budgets;
- world geography, unlock flow, and milestones;
- reward economy;
- LFG, floor selection, or leaderboard UI.

## Mandatory Domain 03 Consumer Binding

Domain 07 owns encounter policy and authored intent, but it never owns combat
mechanic execution. Every encounter, mob, boss, phase, hazard, add package, or
signature mechanic that can execute a catalogued combat mechanic must bind that
mechanic to Domain 03 with this exact immutable shape:

```yaml
mechanic_id: namespaced_id
mechanic_contract_version: positive_integer
required_readiness: RUNTIME_VERIFIED
mechanic_contract_ref: immutable_ref
gate_transition_envelope_ref: path_at_sha256
contract_bundle_digest: sha256
build_digest: sha256
```

Every concrete consumer declares `combat_consumer_status` and
`d03_consumer_bindings`. `DESIGN_ONLY` and `VALIDATED` consumers may retain an
empty binding list, but they cannot receive executable provider mappings,
spawn/cast rights, or runtime activation. `RUNTIME_VERIFIED` is legal only when
every mechanic reachable from the consumer has exactly one binding and every
binding resolves the same non-revoked Domain 03 readiness lineage named by its
contract, gate-envelope, bundle, and build digests.

The following are intent only until bound: `control_immunity_class`,
`interruptibility_class`, `displacement_response_class`, damage/elemental
profiles, guard/stagger/backstab behavior, hazards, periodic effects, signature
mechanics, and any threat-generation hook. Telegraph, pressure, counterplay,
formula, tag, archetype, provider, or fallback prose never substitutes for a
binding.

Admission is fail-closed per mechanic. Missing, duplicate, stale,
digest-mismatched, downgraded, or revoked binding suppresses/rejects that
mechanic and blocks the consumer above `VALIDATED`; it never activates a local
formula, generic effect, plugin default, boss exception, or alternate damage/
control behavior. Noncombat presentation may degrade only when it cannot mutate
combat state or legality.

## Normative Status

This document is normative for:

- top-level encounter schema;
- canonical phase schema;
- canonical runtime budget equation;
- canonical scaling profile shape and execution order;
- capability assumptions for standard 1-4 floor content.

## Capability Baseline

Standard floor encounters may assume:

- basic movement and repositioning;
- dodge or short evasive movement;
- basic target swap;
- line-of-sight usage where space supports it;
- short burst focus on one target;
- reaction to visible telegraphs.

Standard floor encounters may not require by default:

- guaranteed interrupt access;
- guaranteed cleanse;
- guaranteed displacement;
- guaranteed taunt or threat-control tools;
- hard role trinity assumptions;
- perfect assignment discipline.

Conditional capability flags:

- `requires_interrupt_baseline`
- `requires_cleanse_baseline`
- `requires_displacement_baseline`
- `requires_split_assignment`
- `requires_objective_runner`

If one is `true`, the consuming spec must author an explicit fallback for its absence in standard floor content.
This fallback answers missing player capability only. It is not a fallback for
a missing, stale, or unready Domain 03 mechanic binding; contract failure still
suppresses/rejects the affected mechanic fail-closed.

## Canonical Terms

| Term | Meaning |
| --- | --- |
| Encounter | One bounded combat challenge with a clear start, active phase, and resolution condition. |
| Threat | Any active source of pressure: hostile actor, persistent hazard, critical cast, objective timer, or arena event. |
| Pressure Slot | One simultaneous player answer demanded at a given moment. |
| Anchor Unit | The unit or mechanic that defines the pack or room center of gravity. |
| Reinforcement | Dynamic non-archetype burden such as extra hazard lanes, projectile emitters, objective-state burden, or periodic-skill pressure. |
| Add Package | Dynamic actor-bearing burden that references mob archetypes. |
| Recovery Window | A bounded interval in which no new mandatory primary answer is introduced. |
| Fail State | A named escalation outcome caused by missed counterplay or loss of control. |

## Encounter Lifecycle

Every encounter must use:

- `IDLE`
- `ARMED`
- `ENGAGED`
- `ESCALATED`
- `RECOVERY`
- `RESOLVED_SUCCESS`
- `RESOLVED_FAILURE`
- `RESETTING`

Every encounter spec must declare:

- `start_condition`
- `win_condition`
- `terminal_fail_condition`
- `reset_condition`
- `leash_rule`
- `death_policy`
- `disconnect_policy`
- `cleanup_policy`
- `checkpoint_policy`
- `soft_fail_effect`
- `hard_fail_effect`
- `fail_stack_limit`
- `fail_stack_decay_rule`
- `recovery_entry_rule`

Allowed lifecycle values:

- `death_policy`
  - `ALLOW_CONTINUE`
  - `RESPAWN_AND_REJOIN`
  - `DEATH_COUNTS_AS_FAIL_STACK`
  - `DEATH_ENDS_ATTEMPT`
- `disconnect_policy`
  - `PAUSE_NOT_ALLOWED_CONTINUE`
  - `ACTOR_PERSISTS_AND_PLAYER_REJOINS`
  - `DISCONNECT_COUNTS_AS_DEATH`
  - `DISCONNECT_FORCES_RESET`
- `checkpoint_policy`
  - `NO_CHECKPOINT`
  - `ROOM_CHECKPOINT`
  - `PHASE_CHECKPOINT`
  - `FLOOR_CHECKPOINT`
- `cleanup_policy`
  - `DESPAWN_ALL_AND_RESET`
  - `DESPAWN_FAIL_SPAWNS_ONLY`
  - `PERSIST_REWARD_STATE_RESET_COMBAT`

## Pressure Model

`PRESSURE_SLOT_COUNT` means peak simultaneous player answers in the steady-state window.

Allowed values:

- `LOW` = 1
- `MEDIUM` = 2
- `HIGH` = 3
- `EXTREME` = 4

Allowed pressure types:

- `POSITIONING`
- `INTERRUPT`
- `TARGET_PRIORITY`
- `MOVEMENT_DODGE`
- `SPACE_DENIAL`
- `ATTRITION`
- `BURST_RESPONSE`
- `OBJECTIVE_HANDLING`
- `ADD_CONTROL`

`expected_resolution_window_seconds` is authored as a min/max range:

- trash and elite: engagement to room control;
- miniboss: engagement to victory or fail;
- boss: whole encounter window unless phase-local profile overrides it.

## Runtime Budget Model

### Ledger Caps

Every encounter must declare:

- `actor_budget.max_actor_slots`
- `actor_budget.max_priority_target_slots`
- `actor_budget.max_hazard_slots`
- `actor_budget.max_periodic_skill_slots`
- `actor_budget.max_projectile_source_slots`
- `actor_budget.max_high_fidelity_model_slots`
- `actor_budget.overflow_policy`

Allowed `overflow_policy` values:

- `QUEUE_UNTIL_SLOT_FREES`
- `REPLACE_LOWEST_PRIORITY_SOURCE`
- `DENY_NEW_SPAWN_OR_CAST`
- `DESPAWN_EXPIRED_SOURCE_THEN_APPLY`

### Baseline Usage

Every encounter must declare:

- `baseline_runtime_usage.actor_slots`
- `baseline_runtime_usage.priority_target_slots`
- `baseline_runtime_usage.hazard_slots`
- `baseline_runtime_usage.periodic_skill_slots`
- `baseline_runtime_usage.projectile_source_slots`
- `baseline_runtime_usage.high_fidelity_model_slots`

### Canonical Budget Equation

At any active moment:

`runtime_usage_now = baseline_runtime_usage + active_phase_budget_delta + active_reinforcement_delta + active_add_delta`

For every ledger:

`runtime_usage_now.<ledger> <= actor_budget.max_<ledger>`

### Canonical Budget Decomposition

Every runtime contribution must belong to exactly one bucket:

- `baseline_runtime_usage`
  Steady-state authored usage before dynamic changes.
- `active_phase_budget_delta`
  Phase-local authored delta.
- `active_reinforcement_delta`
  Dynamic non-archetype burden such as extra hazards, projectile emitters, objective-state pressure, periodic-skill pressure, or objective concurrency burden.
- `active_add_delta`
  Dynamic actor-bearing burden created through explicit add or spawned-actor packages that reference mob archetypes.

Hard rules:

- actor-bearing spawned bodies must never be counted in `active_reinforcement_delta`;
- non-archetype hazard, projectile, periodic, or objective-concurrency burden must never be counted in `active_add_delta`.

### Canonical Ledger Mapping

Scaling and dynamic burden map to ledgers in exactly this way:

- `body_count_delta`
  changes actor-bearing bodies and therefore contributes to `actor_slots`; if the body is a priority unit, it may also raise `priority_target_slots`.
- `support_presence_delta`
  changes optional shell/support bodies and therefore contributes to `actor_slots`; if those bodies are priority units, it may also raise `priority_target_slots`.
- `hazard_slot_delta`
  contributes only to `hazard_slots`.
- `periodic_skill_slot_delta`
  contributes only to `periodic_skill_slots`.
- `projectile_source_slot_delta`
  contributes only to `projectile_source_slots`.
- `priority_target_slot_delta`
  contributes only to `priority_target_slots`.
- `objective_concurrency_delta`
  contributes to `active_reinforcement_delta.priority_target_slots` by default; if the concurrent objective also creates non-actor area denial or ticking objective pressure, the consuming spec must explicitly add the corresponding `hazard_slots` and/or `periodic_skill_slots` in `active_reinforcement_delta`; if the objective handler is an actor-bearing unit, it must be authored as an add package instead.
- `reinforcement_payload_delta`
  contributes only to `active_reinforcement_delta` ledgers.
- `add_cadence_delta`
  changes the cadence step of authored add packages; it does not itself consume a ledger until a package actually spawns actors.

## Reinforcement Contract

Allowed reinforcement triggers:

- `TIMER`
- `HEALTH_GATE`
- `OBJECTIVE_STATE`
- `FAIL_ESCALATION`
- `PHASE_TRANSITION`

Every reinforcement-capable encounter must declare:

- `reinforcement_trigger`
- `reinforcement_payload`
- `reinforcement_budget_source`
- `reinforcement_overflow_behavior`
- `reinforcement_stop_condition`

Rules:

- reinforcement payload is for non-archetype dynamic burden only;
- if a payload creates actor-bearing bodies, it must be authored as an add or spawned-actor package instead;
- reinforcements must never be prose-only.

## Control and Interrupt Contract

Allowed values:

- `control_immunity_class`
  - `CONTROL_VULNERABLE`
  - `CONTROL_RESISTANT`
  - `CONTROL_LIMITED`
  - `CONTROL_IMMUNE`
- `interruptibility_class`
  - `UNINTERRUPTIBLE`
  - `INTERRUPTIBLE_OPTIONAL`
  - `INTERRUPTIBLE_RECOMMENDED`
  - `INTERRUPTIBLE_REQUIRED`
- `displacement_response_class`
  - `DISPLACEMENT_NONE`
  - `DISPLACEMENT_LIMITED`
  - `DISPLACEMENT_ALLOWED`

Rules:

- `INTERRUPTIBLE_REQUIRED` may not be functionally fake-immune;
- if a standard floor encounter depends on interrupt as the only non-fail answer, it must set `requires_interrupt_baseline: true`;
- fake casts are allowed only after the honest version has been taught.

## Telegraph Contract

Allowed telegraph tiers:

- `TIER_1_MINOR`
- `TIER_2_MAJOR`
- `TIER_3_CRITICAL`

Every encounter spec must declare:

- `telegraph_contract.telegraph_tier`
- `telegraph_contract.telegraph_channel`
- `telegraph_contract.telegraph_window_ms`
- `telegraph_contract.telegraph_clarity_notes`

Allowed `telegraph_channel` values:

- `visual`
- `audio`
- `mixed`
- `worldspace`

Rules:

- higher consequence requires stronger contrast and clearer signaling;
- telegraph shortening is allowed by difficulty, telegraph deletion is not;
- `TIER_3_CRITICAL` mechanics require explicit readability anchors in consuming specs.

## Scaling Contract: 1-4 Players

Closed decision: scaling changes `numbers + encounter composition`.

Scaling is discrete by party size:

- `solo_profile`
- `duo_profile`
- `trio_profile`
- `party4_profile`

Each profile must use this exact shape:

```yaml
body_count_delta: integer
support_presence_delta: integer
hazard_slot_delta: integer
periodic_skill_slot_delta: integer
projectile_source_slot_delta: integer
priority_target_slot_delta: integer
objective_concurrency_delta: integer
reinforcement_payload_delta: integer
add_cadence_delta: integer
notes: string
```

Allowed scaling axes:

- body count;
- support presence;
- hazard concurrency;
- periodic-skill burden;
- projectile burden;
- priority-target concurrency;
- objective concurrency;
- reinforcement payload;
- add cadence.

Disallowed scaling axes without explicit exception:

- core fantasy and anchor identity;
- primary mechanic grammar;
- lifecycle structure;
- terminal fail class;
- baseline counterplay family.

### Canonical Scaling Operations

Apply scaling deltas in this order:

1. Start from authored baseline composition and `baseline_runtime_usage`.
2. Apply `body_count_delta` to repeatable non-anchor body count.
3. Apply `support_presence_delta` to optional shell/support bodies.
4. Apply `hazard_slot_delta`, `periodic_skill_slot_delta`, `projectile_source_slot_delta`, and `priority_target_slot_delta` to runtime burden ledgers.
5. Apply `objective_concurrency_delta`.
6. Apply `reinforcement_payload_delta` to non-archetype reinforcement payload count or intensity.
7. Apply `add_cadence_delta` to authored add cadence steps.

Clamp rules:

- all counts and slot deltas clamp at minimum `0`;
- anchor bodies may not be removed by `body_count_delta`;
- support removal happens before anchor removal in solo-facing profiles;
- if a delta would violate encounter grammar, the spec is invalid rather than silently coerced.

Add cadence semantics:

- `add_cadence_delta` operates on authored cadence steps, not raw milliseconds;
- `-1` means one step slower or less frequent;
- `0` means unchanged;
- `+1` means one step faster or more frequent;
- if no authored add cadence source exists, non-zero `add_cadence_delta` is invalid.

### Canonical Add Cadence Source

Any encounter that uses actor-bearing adds must author:

```yaml
add_cadence_profile:
  cadence_step_order: [slow, medium, fast]
  base_add_cadence_step: slow|medium|fast
```

Rules:

- `base_add_cadence_step` is the source that `add_cadence_delta` operates on;
- `add_cadence_delta` moves one position at a time within `cadence_step_order`;
- the resulting step clamps to the nearest valid edge of the authored order.
- within one encounter spec, `add_cadence_profile` is the shared cadence source for every add or spawned-actor package unless that future exception is added explicitly to this parent contract.

## Canonical Phase Schema

Any `BOSS` or `MINIBOSS` encounter must define `phase_blocks`.

Each phase block must define:

- `phase_id`
- `phase_entry_rule`
- `phase_exit_rule`
- `phase_pressure_profile`
- `phase_budget_delta`
- `phase_recovery_window`
- `phase_fail_behavior`
- `phase_reinforcement_rules`
- `phase_telegraph_rules`

Child specs may add extension fields to a phase block, but may not rename or replace these canonical fields.

### `phase_pressure_profile`

```yaml
phase_pressure_profile:
  phase_pressure_slot_count: LOW|MEDIUM|HIGH|EXTREME
  phase_primary_pressure_type: POSITIONING|INTERRUPT|TARGET_PRIORITY|MOVEMENT_DODGE|SPACE_DENIAL|ATTRITION|BURST_RESPONSE|OBJECTIVE_HANDLING|ADD_CONTROL
  phase_secondary_pressure_types: []
  phase_counterplay: []
  phase_expected_resolution_window_seconds:
    min: integer
    max: integer
```

### `phase_budget_delta`

```yaml
phase_budget_delta:
  actor_slots_delta: integer
  priority_target_slots_delta: integer
  hazard_slots_delta: integer
  periodic_skill_slots_delta: integer
  projectile_source_slots_delta: integer
  high_fidelity_model_slots_delta: integer
```

### `phase_recovery_window`

```yaml
phase_recovery_window:
  recovery_window_exists: boolean
  recovery_window_trigger: string
  recovery_window_length_band: short|medium|long
  recovery_window_restrictions: string
```

### `phase_fail_behavior`

```yaml
phase_fail_behavior:
  soft_fail_effect: string
  hard_fail_effect: string
  hard_fail_reversible: boolean
  recovery_entry_rule: string
```

### `phase_reinforcement_rules`

```yaml
phase_reinforcement_rules:
  add_package_refs: []
  reinforcement_trigger: TIMER|HEALTH_GATE|OBJECTIVE_STATE|FAIL_ESCALATION|PHASE_TRANSITION|NONE
  reinforcement_stop_condition: string
```

### `phase_telegraph_rules`

```yaml
phase_telegraph_rules:
  signature_mechanic_refs: []
  telegraph_notes: string
```

## Encounter Schema Contract

Required top-level fields:

```yaml
encounter_id: string
encounter_type: TRASH|ELITE|MINIBOSS|BOSS|EVENT
combat_consumer_status: DESIGN_ONLY|VALIDATED|RUNTIME_VERIFIED
d03_consumer_bindings: []
space_profile: string
start_condition: string
win_condition: string
terminal_fail_condition: string
reset_condition: string
leash_rule: string
death_policy: enum
disconnect_policy: enum
cleanup_policy: enum
checkpoint_policy: enum
primary_pressure_type: enum
secondary_pressure_types: [enum]
pressure_slot_count: LOW|MEDIUM|HIGH|EXTREME
expected_resolution_window_seconds:
  min: integer
  max: integer
actor_budget:
  max_actor_slots: integer
  max_priority_target_slots: integer
  max_hazard_slots: integer
  max_periodic_skill_slots: integer
  max_projectile_source_slots: integer
  max_high_fidelity_model_slots: integer
  overflow_policy: enum
baseline_runtime_usage:
  actor_slots: integer
  priority_target_slots: integer
  hazard_slots: integer
  periodic_skill_slots: integer
  projectile_source_slots: integer
  high_fidelity_model_slots: integer
control_contract:
  control_immunity_class: enum
  interruptibility_class: enum
  displacement_response_class: enum
telegraph_contract:
  telegraph_tier: enum
  telegraph_channel: visual|audio|mixed|worldspace
  telegraph_window_ms: integer
  telegraph_clarity_notes: string
scaling_profiles:
  solo_profile: {body_count_delta: integer, support_presence_delta: integer, hazard_slot_delta: integer, periodic_skill_slot_delta: integer, projectile_source_slot_delta: integer, priority_target_slot_delta: integer, objective_concurrency_delta: integer, reinforcement_payload_delta: integer, add_cadence_delta: integer, notes: string}
  duo_profile: {same_shape_as_solo_profile}
  trio_profile: {same_shape_as_solo_profile}
  party4_profile: {same_shape_as_solo_profile}
counterplay: [string]
soft_fail_effect: string
hard_fail_effect: string
fail_stack_limit: integer
fail_stack_decay_rule: string
recovery_entry_rule: string
```

Optional fields:

```yaml
requires_interrupt_baseline: boolean
requires_cleanse_baseline: boolean
requires_displacement_baseline: boolean
requires_split_assignment: boolean
requires_objective_runner: boolean
reinforcements: []
add_cadence_profile: {}
phase_blocks: []
difficulty_overrides: {}
event_overlay: {}
```

## Integration Notes

This document is the parent contract for:

- `docs/design/pve/mob-archetypes.yaml`
- `docs/design/pve/boss-framework.md`
- `docs/design/pve/dungeon-framework.md`
- `docs/design/pve/public-events.md`
- `docs/design/pve/raid-framework.md`
- `docs/design/pve/endgame-framework.md`

All child contracts inherit the Domain 03 binding shape, lifecycle ceiling, and
fail-closed admission rule without narrowing. A child may add bindings but may
not replace them with a local readiness flag or provider-specific proof.

## Deferred

This document intentionally still does not close:

- numeric combat balance;
- final floor difficulty entry model;
- exact floor layout generation method;
- endgame separation between Ascension, sigils, raids, and other PvE loops.
