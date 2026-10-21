---
title: Raid Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-07-22
---

# Raid Framework

## Purpose

This document defines the structural framework for instanced raid PvE content as a separate format from the Dungeon.

It extends:

- `docs/design/pve/encounter-framework.md`
- `docs/design/pve/mob-archetypes.yaml`
- `docs/design/pve/boss-framework.md`

This document does not define numeric balance, reward values, or UI surfaces.

## System Boundary

Raid is separate from:

- Dungeon floors
  Repeatable 1-4 floor progression content.
- Public events
  Open participation world-facing PvE with softer coordination expectations.
- Standard Island progression
  Regional progression and exploration.

Raid should not be “Dungeon but longer”. It is a distinct coordination format.

## Raid Goals

Raids should:

- provide higher-structure cooperative PvE;
- allow stronger assignment and execution checks than Dungeon floors;
- deliver memorable multi-encounter or multi-boss progression spaces;
- support aspirational group mastery without becoming the sole mandatory power lane.

Raids should not:

- monopolize required player power;
- invalidate Dungeon or Island progression;
- depend on unreadable chaos or attendance inflation;
- assume every mechanic must be solved by perfect meta composition.

## Raid Identity

A raid should feel like:

- a structured group expedition or assault;
- a space with stronger encounter sequencing;
- higher discipline in pull, wipe, recovery, and progression logic;
- more exact role pressure than standard 1-4 Dungeon content.

## Raid Structure Model

### Core Structural Layers

A raid should define:

- `raid_id`
- `wing_structure`
- `boss_count`
- `intermediate_encounter_count`
- `checkpoint_structure`
- `reset_rules`
- `population_target_band`
- `difficulty_ladder`

### Encounter Mix

A raid may include:

- boss encounters;
- miniboss or elite-anchor gates;
- movement or objective sequences;
- interstitial trash that has a real pacing function.

Raid trash should exist to shape pacing, learning, or attrition, not just to pad time.

## Wing Model

Recommended raid decomposition:

- `ENTRY_ORIENTATION`
- `WING_1`
- `WING_2`
- `WING_3`
- `FINAL_WING`

Not every raid needs all five, but every raid should define macro progression chunks rather than one undifferentiated chain.

### Wing Rules

- each wing should have one dominant lesson family;
- each wing should advance raid grammar, not only aesthetics;
- checkpointing should align to wings or major boss gates;
- a wing should culminate in a recognisable boss or structural climax.

## Raid Boss Philosophy

Raids use the boss framework, but with stronger assumptions.

### Raid Boss Rules

- bosses may require more explicit assignment than Dungeon bosses;
- bosses may use more sustained multi-lane pressure;
- bosses may use tighter hard-fail conditions than standard floor content;
- bosses must still remain readable and fair.

### Raid Boss Guardrails

- a raid boss may be punishing, but must not be unintelligible;
- repeated wipes should teach something, not feel random;
- critical coordination checks must be visibly foreshadowed before the most punishing version appears.

## Group Coordination Model

Raid content may assume more structure than Dungeon content, but that assumption must be authored, not implied.

### Coordination Lanes

Allowed stronger raid asks:

- split assignments;
- role- or lane-based objective handling;
- add ownership;
- dedicated interrupt or cleanse responsibility;
- timed coordination windows;
- ordered mechanic passing or soak logic.

### Coordination Guardrails

- the raid spec must declare which capability baselines it assumes;
- if a mechanic assumes assignment, the encounter should teach that assignment grammar before its harshest expression;
- coordination load should scale through learning and encounter sequence, not only boss-by-boss surprise.

## Population and Composition

The exact final population target is still open, but the format assumes a larger group than standard 1-4 content.

### Structural Population Rules

- raids should author for a fixed or narrow population band;
- boss and objective logic should not stretch freely across wildly different attendance counts;
- if flexible population is later allowed, that flexibility must be intentionally authored, not inherited by accident from public-event logic.

### Composition Rules

- raid composition may ask for broader utility coverage than Dungeon;
- raid composition should still avoid requiring one exact narrow meta to function at all;
- composition checks should test preparation and discipline, not exploit hidden mandatory utility.

## Checkpoint and Wipe Model

Raid needs a stronger reset model than Dungeon.

### Checkpoint Rules

Allowed checkpoint shapes:

- wing checkpoint;
- boss checkpoint;
- phase checkpoint for exceptional encounters only.

### Wipe Rules

- full raid wipe should normally reset the current encounter;
- wipe recovery should be clear and fast enough to preserve iteration rhythm;
- partial survival should matter only if the encounter explicitly supports that recovery model.

## Difficulty Ladder

Raid may use its own difficulty structure rather than inheriting Dungeon floor semantics directly.

### Recommended Raid Difficulty Roles

- `story` or `learning`
  Understand layout, sequencing, and baseline boss grammar.
- `standard`
  Reference raid experience.
- `veteran`
  Higher execution and composition discipline.
- `mythic`
  Repeatable aspirational mastery tier.

### Raid Difficulty Guardrails

- higher difficulty must add structure, not only durability;
- higher difficulty may tighten assignments, add cadence, hazard overlap, or boss-side burden;
- raid difficulty must not become the only source of mandatory power progression.

## Reward Guardrails

This document does not define reward values, but it does define constraints.

### Reward Rules

- raids may reward prestige, progression support, collections, cosmetics, raid materials, or alternate advancement lanes;
- raids must not be the only mandatory source of core player power;
- players who prefer Dungeon or Island progression should still be able to complete a core build;
- raid rewards may be aspirational, but should not force every player into raid attendance for baseline viability.

## Relationship to Dungeon and Public Events

### Versus Dungeon

- Dungeon is repeatable 1-4 progression content;
- Raid is larger-group structured execution content.

### Versus Public Events

- public events are open and population-fluid;
- raids are instanced, bounded, and intentionally coordinated.

## Failure and Learning Model

Raids should support progression through learning.

### Learning Rules

- encounter sequencing should teach the raid's internal language;
- wipes should reveal which layer failed;
- interstitial encounters should prepare players for later boss grammar;
- final bosses should resolve earlier raid lessons, not abandon them.

## Raid Encounter Cadence

Recommended cadence:

- lower-complexity opener;
- one or more mid-raid coordination bosses;
- one deeper execution or arena-control boss;
- final boss that combines the raid's core lesson families.

Optional additions:

- miniboss checkpoints;
- wing-end structural events;
- optional challenge or side-objective encounters.

## Anti-Abuse and Participation Integrity

Raids should use stricter integrity rules than public events.

### Integrity Rules

- participation credit should prefer active encounter involvement;
- lockout or checkpoint exploitation should be prevented by explicit reset rules;
- AFK carry should not be treated as equivalent contribution;
- dead or disconnected players should not indefinitely retain full encounter status without the raid explicitly allowing it.

## Implementation-Facing Gaps

This framework is narrative-strong, but some inputs remain intentionally open:

- final target raid population and any flexible attendance policy;
- exact raid lockout, re-entry, and save-state policy;
- final reward-value mapping in domain 10;
- final UI/LFG/raid-browser surface in domain 14.

## Ready Uses

This document is already strong enough to guide:

- raid versus Dungeon/Public Event separation;
- raid wing structure;
- checkpoint and wipe philosophy;
- stronger coordination assumptions;
- reward guardrails versus mandatory power.
