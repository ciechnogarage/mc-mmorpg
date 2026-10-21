---
title: Boss Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-08-12
---

# Boss Framework

## Purpose

This document defines the canonical boss-family specialization of
`docs/design/pve/encounter-framework.md`.

A valid boss-family spec must be:

- a valid parent encounter instance with `encounter_type: BOSS` or `encounter_type: MINIBOSS`;
- a valid boss extension using the required fields below.

This document closes:

- boss/miniboss typing;
- boss-specific lifecycle additions;
- boss-class and content-band metadata;
- canonical use of phase schema, runtime budget model, and scaling model;
- canonical actor-bearing add-package integration with `docs/design/pve/mob-archetypes.yaml`.

## Boss-Family Scope

This document covers two encounter types:

- `MINIBOSS`
- `BOSS`

This document also uses one separate content-band field:

- `STANDARD_FLOOR_BOSS`
- `SECTOR_FINALE_BOSS`
- `RAID_BOSS`
- `PUBLIC_EVENT_BOSS`
- `ENDGAME_BOSS`

`MINIBOSS` is an encounter type, not a content band.

## Boss Classes

Every boss-family spec must define:

- `boss_id`
- `boss_class`
- `boss_tags`

Allowed `boss_class` values:

- `DUELIST`
- `SIEGE`
- `COMMANDER`
- `HUNTER`
- `RITUALIST`
- `COLOSSUS`

Allowed `boss_tags` values:

- `ARENA_CONTROL`
- `ADD_DRIVEN`
- `OBJECTIVE_LINKED`
- `INTERRUPT_SENSITIVE`
- `MOBILITY_CHECK`
- `EXECUTION_CHECK`

## Boss Lifecycle Extension

Boss-family specs inherit the full parent lifecycle contract and must additionally define:

- `reset_clears_add_packages`
- `reset_clears_persistent_hazards`
- `reset_clears_fail_stacks`
- `reset_phase_progress_policy`
- `post_reset_state`
- `corpse_or_reward_rule`

Allowed values:

- `reset_phase_progress_policy`
  - `FULL_RESET`
  - `RETURN_TO_CHECKPOINT`
- `post_reset_state`
  - `IDLE`
  - `ARMED`

Checkpoint band rules:

- standard floor boss: `NO_CHECKPOINT` or `ROOM_CHECKPOINT`
- sector finale boss: `NO_CHECKPOINT`, `ROOM_CHECKPOINT`, or `PHASE_CHECKPOINT`
- raid boss: may use `PHASE_CHECKPOINT`
- public event boss: uses event recovery, not room checkpointing

## Boss-Family Scaling

Boss-family specs use exactly the same `scaling_profiles` shape as the parent encounter schema.

Allowed runtime-varying axes by party size:

- `body_count_delta`
- `support_presence_delta`
- `hazard_slot_delta`
- `periodic_skill_slot_delta`
- `projectile_source_slot_delta`
- `priority_target_slot_delta`
- `objective_concurrency_delta`
- `reinforcement_payload_delta`
- `add_cadence_delta`

Boss-family specs may not vary by party size without explicit exception:

- `boss_class`
- `boss_tags`
- lifecycle structure
- signature mechanic roster
- core mechanic grammar
- terminal fail class
- fight fantasy

Rules:

- `solo_profile` must reduce add or assignment burden before deleting the core lesson;
- `party4_profile` is the full intended non-raid composition;
- larger parties may intensify known grammar, but may not gain a new primary mechanic family that solo never learned;
- archetype-level scaling data is reusable authoring default only; the boss spec is the authoritative runtime scaling contract.

## Boss-Family Runtime Budget

Boss-family specs use the parent runtime budget equation without replacement:

`runtime_usage_now = baseline_runtime_usage + active_phase_budget_delta + active_reinforcement_delta + active_add_delta`

Boss-specific interpretation:

- `baseline_runtime_usage`
  boss steady-state usage before phase deltas and dynamic payloads;
- `active_phase_budget_delta`
  the currently active phase's `phase_budget_delta`;
- `active_reinforcement_delta`
  dynamic non-archetype burden only;
- `active_add_delta`
  actor-bearing burden from active add packages that reference archetypes.

Hard rules:

- boss actor-bearing adds must never be counted in `active_reinforcement_delta`;
- boss hazard, projectile, periodic, or objective-concurrency burden that does not create actor-bearing bodies must never be counted in `active_add_delta`.

## Signature Mechanics

Every boss-family spec must declare `signature_mechanics`.

Each signature mechanic must define:

```yaml
mechanic_id: string
d03_consumer_binding_ref: mechanic_id
counterplay_family: DODGE_TIMING|SPACE_SWAP|TARGET_PRIORITY|INTERRUPT_IF_AVAILABLE|OBJECTIVE_HANDLE|BURST_WINDOW|BAIT_AND_PUNISH|ADD_CLEAR
telegraph_tier: TIER_1_MINOR|TIER_2_MAJOR|TIER_3_CRITICAL
telegraph_channel: visual|audio|mixed|worldspace
telegraph_window_ms: integer
readability_anchor: string
noise_constraints: string
failure_class: SOFT_FAIL|HARD_FAIL|TERMINAL_FAIL
introduced_in_phase: string
```

Rules:

- `d03_consumer_binding_ref` must resolve exactly one top-level inherited
  `d03_consumer_bindings` entry for the same `mechanic_id` before the signature
  mechanic can execute;
- missing, duplicate, stale, digest-mismatched, downgraded, or revoked binding
  rejects the signature mechanic fail-closed; boss phase, provider default,
  telegraph, or capability fallback cannot execute a substitute combat effect;
- every phase must reference at least one `mechanic_id`;
- `TIER_3_CRITICAL` mechanics require explicit readability anchors;
- multiple `TIER_3_CRITICAL` mechanics may not resolve on the same beat unless the phase is an explicit climax beat.

## Canonical Phase Usage

Boss-family specs must use the canonical parent `phase_blocks` shape.

Additional boss-family phase field:

- `phase_purpose`

Allowed `phase_purpose` values:

- `OPENING_LESSON`
- `STABLE_ROTATION`
- `ARENA_SHIFT`
- `ADD_CONTROL`
- `BURN_WINDOW`
- `RITUAL_OR_OBJECTIVE`
- `SOFT_ENRAGE`
- `FINAL_EXECUTION`

Rules:

- `MINIBOSS` should usually use 1-3 phases;
- standard floor `BOSS` should usually use 2-4 phases;
- sector finale and endgame bosses may use 3-5 phases;
- a new phase may intensify known grammar, but should not introduce unrelated primary grammar late in the encounter.

## Add Package Contract

Boss actor-bearing adds are canonical data, not prose.

Every boss-family spec that uses actor-bearing adds must define:

- `add_cadence_profile`
- `add_packages`

`add_cadence_profile` must use the parent canonical shape:

```yaml
add_cadence_profile:
  cadence_step_order: [slow, medium, fast]
  base_add_cadence_step: slow|medium|fast
```

Each add package must define:

```yaml
add_package_id: string
owning_phase_id: string
archetype_id: string
spawn_trigger: TIMER|HEALTH_GATE|OBJECTIVE_STATE|FAIL_ESCALATION|PHASE_TRANSITION
overflow_policy: QUEUE_UNTIL_SLOT_FREES|REPLACE_LOWEST_PRIORITY_SOURCE|DENY_NEW_SPAWN_OR_CAST|DESPAWN_EXPIRED_SOURCE_THEN_APPLY
priority_expectation: ignorable|should_clear|must_clear
ignored_failure_effect: string
stop_condition: string
base_count_by_party_size:
  solo: integer
  duo: integer
  trio: integer
  party4: integer
max_concurrent_instances_by_party_size:
  solo: integer
  duo: integer
  trio: integer
  party4: integer
compatibility_contract:
  allowed_role_pairs: []
  disallowed_role_pairs: []
  anchor_limit_override: integer|null
  capped_role_families: []
```

Canonical add math:

- `active_add_instances_now` is bounded by `max_concurrent_instances_by_party_size.<profile>`;
- `spawn_payload_now` starts from `base_count_by_party_size.<profile>`;
- `add_cadence_delta` changes only the cadence step of the shared encounter-level `add_cadence_profile`, not the package count directly;
- `reinforcement_payload_delta` never changes actor-bearing add count directly;
- add packages do not define a second competing scaling formula.

Rules:

- `archetype_id` must exist in `mob-archetypes.yaml`;
- `owning_phase_id` must match an authored phase;
- prose-only add counts are invalid;
- packages consume actor budget through referenced archetype `runtime_contribution`;
- packages must not violate encounter pack constraints unless `compatibility_contract` explicitly overrides them;
- inside `compatibility_contract`, explicit `disallowed_role_pairs` and `anchor_limit_override` take precedence over mob-archetype defaults and composition aliases; otherwise the mob-archetype defaults remain binding;
- if `priority_expectation: must_clear`, the fight must teach that expectation before the most punishing version appears.

## Capability Fallbacks

Boss-family specs inherit the parent capability flags.

If any of these is `true`:

- `requires_interrupt_baseline`
- `requires_cleanse_baseline`
- `requires_displacement_baseline`
- `requires_split_assignment`
- `requires_objective_runner`

then the boss spec must define:

```yaml
capability_fallbacks:
  interrupt_absent: string
  cleanse_absent: string
  displacement_absent: string
  split_assignment_absent: string
  objective_runner_absent: string
```

Standard 1-4 floor bosses may not leave these fallbacks implicit.
Capability fallbacks provide alternate authored counterplay when a party lacks
a tool; they never authorize execution when a Domain 03 consumer binding is
missing, stale, mismatched, downgraded, or revoked.

## Boss-Family Template

The template below is a valid parent encounter instance plus boss-family extension.

```yaml
encounter_id: string
encounter_type: BOSS|MINIBOSS
combat_consumer_status: DESIGN_ONLY|VALIDATED|RUNTIME_VERIFIED
d03_consumer_bindings: []
boss_id: string
boss_class: DUELIST|SIEGE|COMMANDER|HUNTER|RITUALIST|COLOSSUS
boss_tags: []
content_band: STANDARD_FLOOR_BOSS|SECTOR_FINALE_BOSS|RAID_BOSS|PUBLIC_EVENT_BOSS|ENDGAME_BOSS
space_profile: string
start_condition: string
win_condition: string
terminal_fail_condition: string
reset_condition: string
leash_rule: string
death_policy: ALLOW_CONTINUE|RESPAWN_AND_REJOIN|DEATH_COUNTS_AS_FAIL_STACK|DEATH_ENDS_ATTEMPT
disconnect_policy: PAUSE_NOT_ALLOWED_CONTINUE|ACTOR_PERSISTS_AND_PLAYER_REJOINS|DISCONNECT_COUNTS_AS_DEATH|DISCONNECT_FORCES_RESET
cleanup_policy: DESPAWN_ALL_AND_RESET|DESPAWN_FAIL_SPAWNS_ONLY|PERSIST_REWARD_STATE_RESET_COMBAT
checkpoint_policy: NO_CHECKPOINT|ROOM_CHECKPOINT|PHASE_CHECKPOINT|FLOOR_CHECKPOINT
primary_pressure_type: POSITIONING|INTERRUPT|TARGET_PRIORITY|MOVEMENT_DODGE|SPACE_DENIAL|ATTRITION|BURST_RESPONSE|OBJECTIVE_HANDLING|ADD_CONTROL
secondary_pressure_types: []
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
  overflow_policy: QUEUE_UNTIL_SLOT_FREES|REPLACE_LOWEST_PRIORITY_SOURCE|DENY_NEW_SPAWN_OR_CAST|DESPAWN_EXPIRED_SOURCE_THEN_APPLY
baseline_runtime_usage:
  actor_slots: integer
  priority_target_slots: integer
  hazard_slots: integer
  periodic_skill_slots: integer
  projectile_source_slots: integer
  high_fidelity_model_slots: integer
control_contract:
  control_immunity_class: CONTROL_VULNERABLE|CONTROL_RESISTANT|CONTROL_LIMITED|CONTROL_IMMUNE
  interruptibility_class: UNINTERRUPTIBLE|INTERRUPTIBLE_OPTIONAL|INTERRUPTIBLE_RECOMMENDED|INTERRUPTIBLE_REQUIRED
  displacement_response_class: DISPLACEMENT_NONE|DISPLACEMENT_LIMITED|DISPLACEMENT_ALLOWED
telegraph_contract:
  telegraph_tier: TIER_1_MINOR|TIER_2_MAJOR|TIER_3_CRITICAL
  telegraph_channel: visual|audio|mixed|worldspace
  telegraph_window_ms: integer
  telegraph_clarity_notes: string
corpse_or_reward_rule: string
reset_clears_add_packages: boolean
reset_clears_persistent_hazards: boolean
reset_clears_fail_stacks: boolean
reset_phase_progress_policy: FULL_RESET|RETURN_TO_CHECKPOINT
post_reset_state: IDLE|ARMED
scaling_profiles:
  solo_profile: {body_count_delta: integer, support_presence_delta: integer, hazard_slot_delta: integer, periodic_skill_slot_delta: integer, projectile_source_slot_delta: integer, priority_target_slot_delta: integer, objective_concurrency_delta: integer, reinforcement_payload_delta: integer, add_cadence_delta: integer, notes: string}
  duo_profile: {same_shape_as_solo_profile}
  trio_profile: {same_shape_as_solo_profile}
  party4_profile: {same_shape_as_solo_profile}
counterplay: []
soft_fail_effect: string
hard_fail_effect: string
fail_stack_limit: integer
fail_stack_decay_rule: string
recovery_entry_rule: string
requires_interrupt_baseline: boolean
requires_cleanse_baseline: boolean
requires_displacement_baseline: boolean
requires_split_assignment: boolean
requires_objective_runner: boolean
capability_fallbacks:
  interrupt_absent: string
  cleanse_absent: string
  displacement_absent: string
  split_assignment_absent: string
  objective_runner_absent: string
add_cadence_profile:
  cadence_step_order: [slow, medium, fast]
  base_add_cadence_step: slow|medium|fast
signature_mechanics: [] # each entry carries d03_consumer_binding_ref
add_packages: []
phase_blocks:
  - phase_id: string
    phase_purpose: OPENING_LESSON|STABLE_ROTATION|ARENA_SHIFT|ADD_CONTROL|BURN_WINDOW|RITUAL_OR_OBJECTIVE|SOFT_ENRAGE|FINAL_EXECUTION
    phase_entry_rule: string
    phase_exit_rule: string
    phase_pressure_profile:
      phase_pressure_slot_count: LOW|MEDIUM|HIGH|EXTREME
      phase_primary_pressure_type: POSITIONING|INTERRUPT|TARGET_PRIORITY|MOVEMENT_DODGE|SPACE_DENIAL|ATTRITION|BURST_RESPONSE|OBJECTIVE_HANDLING|ADD_CONTROL
      phase_secondary_pressure_types: []
      phase_counterplay: []
      phase_expected_resolution_window_seconds:
        min: integer
        max: integer
    phase_budget_delta:
      actor_slots_delta: integer
      priority_target_slots_delta: integer
      hazard_slots_delta: integer
      periodic_skill_slots_delta: integer
      projectile_source_slots_delta: integer
      high_fidelity_model_slots_delta: integer
    phase_recovery_window:
      recovery_window_exists: boolean
      recovery_window_trigger: string
      recovery_window_length_band: short|medium|long
      recovery_window_restrictions: string
    phase_fail_behavior:
      soft_fail_effect: string
      hard_fail_effect: string
      hard_fail_reversible: boolean
      recovery_entry_rule: string
    phase_reinforcement_rules:
      add_package_refs: []
      reinforcement_trigger: TIMER|HEALTH_GATE|OBJECTIVE_STATE|FAIL_ESCALATION|PHASE_TRANSITION|NONE
      reinforcement_stop_condition: string
    phase_telegraph_rules:
      signature_mechanic_refs: []
      telegraph_notes: string
```

## Integration Notes

- `dungeon-framework.md` should consume this file by selecting cadence, placement, checkpoint expectations, and content-band rules, not by redefining schema.
- Boss actor-bearing adds must reference `mob-archetypes.yaml`; prose-only role names are invalid.
- If a boss needs a bespoke add outside the archetype library, that exception must be documented explicitly in the consuming spec.
