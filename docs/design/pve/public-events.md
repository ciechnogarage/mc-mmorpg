---
title: Public Events Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-07-22
---

# Public Events Framework

## Purpose

This document defines the structural framework for public PvE content that is not ordinary Dungeon floor content:

- world bosses;
- public meta-events;
- open participation PvE chains;
- public PvE climax encounters tied to world state or timed activity.

It extends the shared language from:

- `docs/design/pve/encounter-framework.md`
- `docs/design/pve/mob-archetypes.yaml`
- `docs/design/pve/boss-framework.md`

This document does not define economic reward values, world unlock geography, or UI surfaces.

## System Boundary

Public events are distinct from:

- Dungeon floors
  Private or semi-private repeatable instance progression.
- Raids
  Structured group-instance content with stronger composition and assignment expectations.
- Islands / open world baseline
  Exploration and regional progression that can exist without active public participation pressure.

Public events should feel like shared world moments, not “Dungeon but outside”.

## Public Event Families

### World Boss

A single high-visibility public boss encounter with open participation and bounded spatial focus.

Primary characteristics:

- one dominant boss anchor;
- broad player aggregation;
- simpler contribution grammar than raids;
- clearer anti-leech and anti-chaos rules than ordinary open-world fights.

### Public Meta-Event

A multi-stage event chain with world-state progression.

Primary characteristics:

- several linked beats;
- escalating participation;
- shifting objectives or locations;
- one or more climax fights or defense states.

### Public Objective Event

A lighter shared event focused on objective handling rather than a named boss.

Primary characteristics:

- escort, defense, cleanse, holdout, or collection pressure;
- multiple player roles can contribute without perfect composition;
- shorter duration than a full meta-event.

## Core Goals

Public events should:

- create visible shared-world PvE moments;
- support drop-in participation without requiring pre-made group structure;
- reward active contribution, not presence only;
- avoid being the only mandatory source of core player power;
- remain readable under variable population.

Public events should not:

- require raid-like assignment discipline by default;
- become pure visual noise under large populations;
- reward AFK or tag-only participation;
- monopolize progression that Dungeon or Islands should also support.

## Participation Model

Public events use open participation with bounded eligibility.

### Participation Rules

- joining should be simple and readable;
- players must understand where the event is, what the objective is, and what counts as meaningful participation;
- participation may be individual, party-based, or guild-adjacent, but reward eligibility must resolve per player.

### Participation States

Recommended states:

- `DISCOVERED`
- `ELIGIBLE`
- `ACTIVE_CONTRIBUTOR`
- `LOW_CONTRIBUTOR`
- `INELIGIBLE`
- `LOCKED_OUT`

These states are event-facing, not combat-lifecycle replacements.

## Contribution Framework

Public events must use explicit contribution logic. “Was nearby” is not enough.

### Contribution Channels

An event may score contribution through one or more of:

- boss damage or add clear;
- objective interaction;
- defense or protection actions;
- room-state or structure handling;
- event-stage utility actions;
- event-specific support actions.

### Contribution Rules

- contribution must be active and measurable;
- contribution should recognize multiple role expressions, not only raw damage;
- contribution should decay or expire when the player stops participating;
- contribution should be stage-aware for multi-stage events.

### Contribution Guardrails

- contribution may not be granted for AFK presence only;
- contribution may not be granted for a trivial tag with no follow-through;
- event credit should prefer sustained useful participation over last-hit behavior;
- contribution thresholds should be forgiving enough for open-world scale, but still meaningful.

## Anti-Leech Contract

Every public event must define anti-leech behavior.

### Required Anti-Leech Questions

Every event should answer:

- what counts as meaningful participation?
- how long can a player be inactive before contribution degrades?
- what does a player need to do after joining late to become eligible?
- how is alt/AFK/tag-only abuse denied?

### Anti-Leech Rules

- late joiners may still earn partial or full eligibility, but not for zero effort;
- dead, disconnected, or permanently absent players should not retain full contributor status indefinitely;
- a player must contribute inside the active event band, not only before or after it.

## Population Bands

Public events must author for different attendance sizes.

### Population Bands

- `SMALL`
- `MEDIUM`
- `LARGE`
- `SURGE`

Population band affects:

- boss and add readability;
- hazard saturation;
- objective concurrency;
- contribution thresholds;
- anti-chaos constraints.

### Population Rules

- more players should not merely mean more HP;
- event scaling should first widen role capacity and contribution lanes before inflating durability;
- if `SURGE` population would destroy clarity, the event must clamp or simplify local asks.

## World Boss Framework

World bosses are the most legible public boss expression.

### World Boss Rules

- one dominant boss identity;
- readable arena or zone boundary;
- limited simultaneous critical asks;
- simpler add logic than raids;
- explicit contribution and anti-leech contract;
- strong recovery/readability even at larger populations.

### World Boss Anti-Chaos Guardrails

- critical mechanics should not stack on too many players at once;
- add cadence should not scale endlessly with attendance;
- arena hazard density should clamp before visual noise makes the fight unreadable;
- boss signatures should remain visible under large population spell clutter.

## Meta-Event Framework

Meta-events are multi-stage shared PvE sequences.

### Stage Structure

A meta-event should define:

- stage order;
- stage goals;
- stage success and failure rules;
- stage transition logic;
- whether failure degrades, resets, branches, or ends the chain.

### Meta-Event Stage Types

- `ASSEMBLY`
- `ESCORT`
- `DEFENSE`
- `POINT_CONTROL`
- `RESOURCE_PUSH`
- `BOSS_CLIMAX`
- `RECOVERY_OR_CLEANUP`

### Meta-Event Rules

- every stage should have one clear public objective;
- the chain should escalate, not wander;
- later stages may assume event participation momentum, but should not become raid-grade by surprise;
- failure may reduce rewards or branch outcomes, but should be explicit.

## Event Lifecycle

Public events use the shared encounter lifecycle where relevant, plus event-level state.

### Event-Level States

Recommended event layer:

- `SCHEDULED` or `AVAILABLE`
- `FORMING`
- `ACTIVE_STAGE`
- `CLIMAX`
- `RESOLVED_SUCCESS`
- `RESOLVED_FAILURE`
- `COOLDOWN`

This layer complements, not replaces, the encounter lifecycle of boss or objective beats inside the event.

## Public Recovery Model

Public events need softer recovery than raids.

### Recovery Rules

- late join and rejoin should usually be possible;
- individual death should rarely be equivalent to total event failure;
- event failure should usually be stage-based, objective-based, or timer-based, not one-player based;
- boss or stage recovery windows must remain visible even under crowd conditions.

## Difficulty and Variant Logic

Public events may have variants, but should not use the same assumption set as Dungeon difficulty.

Good variant levers:

- stage count or stage intensity;
- add cadence clamps;
- objective concurrency;
- boss-side support burden;
- world-state modifiers.

Bad variant levers:

- raw HP scaling only;
- unreadable mechanic stacking under high population;
- mandatory raid-style utility assumptions without a raid format.

## Reward Guardrails

This document does not set reward values, but it does set reward shape guardrails.

### Reward Rules

- public events may reward progress materials, collections, prestige, event tokens, cosmetics, or alternate progression support;
- public events must not be the only mandatory source of core player power;
- world boss and meta-event rewards should feel meaningful without making Dungeon or Islands obsolete;
- contribution level may affect reward tier or bonus eligibility, but should not create completely invisible logic.

## Relationship to Dungeon and Raid

### Versus Dungeon

- public events are open participation and world-facing;
- dungeon content is repeatable floor progression with tighter authored pacing.

### Versus Raid

- public events tolerate looser composition and weaker assignment discipline;
- raids should remain the place for more exact structured group execution.

## Implementation-Facing Gaps

This framework is narrative-strong, but some source contracts still need final hardening before every field becomes production-authorable without interpretation.

Notable explicit dependencies:

- final reward-value ownership and contribution-to-reward mapping in domain 10;
- final participation surfaces in domain 14;
- last hardening pass of the underlying encounter trio if zero ambiguity is required.

## Ready Uses

This document is already strong enough to guide:

- world boss vs meta-event separation;
- contribution and anti-leech rules;
- population-band planning;
- event-stage planning;
- public-event boundaries versus Dungeon and Raid.
