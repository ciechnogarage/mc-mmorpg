---
title: Dungeon Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-08-12
---

# Dungeon Framework

## Domain 03 Combat Binding Inheritance

Every dungeon floor, room encounter, boss, hazard, or modifier that executes a
combat mechanic inherits the mandatory `combat_consumer_status` and
`d03_consumer_bindings` contract from `encounter-framework.md`. Dungeon
classification, package binding, floor pressure, difficulty, or Ascension data
cannot substitute for Domain 03 `RUNTIME_VERIFIED` mechanic readiness.

Missing, stale, mismatched, downgraded, or revoked binding suppresses/rejects
the affected mechanic and blocks runtime activation fail-closed. The dungeon
layer may not provide a local formula, generic effect, plugin default, boss
exception, or other combat fallback.

## Purpose

This document defines the 100-floor Dungeon as one coherent PvE system, not 100 unrelated dungeons.

It sits on top of:

- `docs/design/pve/encounter-framework.md`
- `docs/design/pve/mob-archetypes.yaml`
- `docs/design/pve/boss-framework.md`
- `docs/design/pve/endgame-framework.md`

This is a narrative planning artifact. It is strong enough to guide floor families, pacing, cadence, and endgame alignment, but it is not fully implementation-facing until the floor-failure model is closed.

## Closed Inputs

This framework assumes:

1. Dungeon contains 100 individually selectable floors.
2. Floors are divided into 10 sectors of 10 floors each.
3. A single floor should usually run for about `10-20` minutes.
4. Core party size is `1-4` players.
5. Floors are repeatable and are not mapped one-to-one to character levels.
6. Raid is separate content.
7. World Boss and Public Meta-Event are separate systems.
8. Islands and Dungeon support independent progression.
9. Raid may not be the only mandatory source of power.
10. Endgame may not collapse into daily/weekly chores only.

## Dungeon Identity

Dungeon is one evergreen PvE spine.

It should:

- teach and escalate PvE grammar across 100 floors;
- support solo through 4-player play without becoming four different games;
- remain target-farmable and replayable;
- remain distinct from raid, public event, and late-endgame specialization systems.

Dungeon uses `evergreen` as the default endgame entry layer. `Sigils / Maps` modify a Dungeon run only as explicit package content layered onto a selected floor or sector target.

## Sector Structure

Dungeon is divided into 10 sectors. Each sector should provide:

- one dominant environmental identity;
- one dominant lesson family;
- one miniboss-to-boss escalation lane;
- one internal pacing curve from onboarding to climax.

Recommended rhythm:

- Floors `x1-x2`: onboarding
- Floors `x3-x4`: normal grammar
- Floors `x5-x6`: first density increase
- Floors `x7-x8`: miniboss or elite-anchor climax
- Floor `x9`: pre-finale stress test
- Floor `x10`: sector-finale boss

## Floor Families

Core reusable families:

- `LINEAR_ASSAULT`
- `HUB_AND_BRANCH`
- `OBJECTIVE_LOOP`
- `ARENA_CHAIN`

## Floor Contract

Each floor should be specifiable through:

- `floor_id`
- `sector_id`
- `floor_family`
- `content_role`
- `expected_run_time_band`
- `encounter_density_band`
- `boss_presence`
- `optional_objective_slots`
- `difficulty_hooks`
- `repeatability_notes`

## Encounter Density

Dungeon controls pacing through density, not only labels.

Rules:

- a `10-20` minute floor should not feel uniformly high-density;
- a floor should alternate pressure beats and stabilization beats;
- sector finales may spike density only after the grammar has been taught.

## Boss and Miniboss Cadence

- most sectors should contain one explicit miniboss floor or miniboss-grade cluster;
- every sector should contain one sector-boss floor;
- high-pressure non-boss floors may act as pseudo-climaxes, but may not replace the finale.

## Scaling for 1-4 Players

Dungeon inherits the shared scaling model: `numbers + encounter composition`.

Implications:

- solo floors may reduce support load, add burden, and simultaneous asks, but may not delete floor identity;
- 4-player floors may widen composition pressure and concurrency, but may not become unreadable chaos.

## Difficulty Positioning

Dungeon evergreen remains the baseline difficulty lane. Higher-variation challenge belongs in package-bound runs or other downstream systems, not in a second hidden evergreen namespace.

## Dungeon Entry Model

Base rules:

- base floor or floor-variant selection is always the primary entry step;
- sigils/maps are secondary run modifiers, not the primary entry gate;
- Ascension is a broader overlay, not the primary entry gate.

## Evergreen vs Sigil/Map Classifier

A Dungeon run is `EVERGREEN_RUN` when:

- the player enters a directly selectable floor or authored evergreen floor variant from the evergreen catalog;
- no external `package_id` is required before entry.

A Dungeon run is `SIGIL_OR_MAP_RUN` when:

- the player must bind a canonical `package_id` and package namespace before the run starts;
- the package changes the encounter contract beyond the baseline evergreen floor variant.

Ascension never changes classifier by itself.

## Capability Test

- a floor variant remains evergreen only while it stays inside the evergreen capability envelope inherited from `endgame-framework.md`;
- if a floor variant becomes portable, package-like, multi-family, or parity-bearing beyond that envelope, it must move into package namespace even if it began as a floor variant.

## Package-Like Threshold in Dungeon

Dungeon does not define a second local threshold. The sole source of truth for package-like decision logic is `docs/design/pve/endgame-framework.md`.

Inherited rules:

- any Dungeon content that resolves to package content under the upstream enumerated decision test must carry canonical `package_id`;
- package-like content without canonical `package_id` is an invalid authored state, not a legal Dungeon classification;
- Dungeon inherits the upstream classifier decision table without narrowing, reinterpretation, or local exception path.

Canonical package identity in Dungeon:

- Dungeon may reference package identity, but may not mint, supersede, or retire `package_id`;
- the only authoritative registry surface for `package_id` is `docs/design/pve/package-registry.yaml`;
- Dungeon is a read-only consumer of canonical package identity defined upstream.

## Failure, Reset, and Repeatability

Dungeon inherits encounter-level lifecycle rules, but the exact floor-level failure model is still open.

Current assumptions:

- floor content should prefer recoverable pressure debt before immediate deletion;
- sector-finale floors may be less forgiving than ordinary floors;
- repeatability should preserve floor identity and target-farming value.

### Explicit Blocker

The exact floor-level failure model remains open:

- full floor reset;
- checkpointed retry;
- limited attempts;
- re-entry cost;
- or another hybrid.

### Temporary Decision Matrix: Recoverable Pressure vs Pseudo-Lifecycle Fail State

Until the floor-failure model is closed, use this matrix as the normative review rule.

| Pressure Axis | Still recoverable when | Treat as pseudo-lifecycle fail state when |
| --- | --- | --- |
| Hazard persistence | the player can realistically clear, route around, or outplay the hazard using baseline floor tools and without hidden forgiveness | hazard persistence accumulates into unavoidable floor denial for the rest of the run after one major mistake |
| Resource denial / attrition | the player can stabilize through normal play and recover a viable path to baseline clear | one debt spike functionally removes viable completion even though attempts, retries, or checkpoints did not formally change |
| Time pressure / decay | pressure narrows tolerance but still leaves a skill-based recovery path to baseline clear | timer or decay converts one major error into inevitable failure with no realistic recovery window |
| Route burden | route pressure increases danger or cost, but a recoverable route to baseline clear still exists | one route error permanently bricks baseline clear or forces effective restart without a formal reset |
| Objective burden | optional or supplemental pressure remains optional and baseline clear authority stays unchanged | the burden functionally turns optional objective state into required clear or reward-viability gate |
| Consequence severity | mistakes cause durable pressure that can still be played out and partially recovered | consequence severity creates unrecoverable depletion or denial that behaves like hidden wipe or hidden re-entry tax |

Review rule:

- if any package or overlay hits the right-hand column for any row, classify it as a pseudo-lifecycle fail state and reject it under the inherited lifecycle guardrail.
- if no single row fails but a composite pressure bundle removes the realistic skill-based path back to baseline completion, classify the bundle as a pseudo-lifecycle fail state and reject it.
- `baseline completion remains technically possible` is not sufficient if the combined pressure collapses practical recovery or baseline reward viability into de facto restart behavior.

`baseline reward viability` means the run still preserves the baseline reward lane attached to normal floor completion, without requiring hidden replays, pseudo-resets, or recovery states that behave like a failed run in practice.

### Failure Guardrail Until Blocker Is Closed

- sigil/map packages may increase encounter-level punishment, pressure debt, hazard persistence, and consequence severity inside the existing floor-failure model;
- sigil/map packages may not independently redefine attempt counts, retry counts, checkpoint structure, wipe-reset scope, or re-entry cost;
- sigil/map packages that modify pressure, attrition, hazard persistence, consequence severity, objective burden, or route burden must preserve meaningful recovery inside the inherited lifecycle model;
- sigil/map packages may not create pseudo-lifecycle fail states by removing meaningful recovery while lifecycle counters remain formally unchanged;
- Ascension overlays may not indirectly alter immutable lifecycle fields through pressure, attrition, or progression-state rules;
- Ascension overlays may not convert an optional objective into baseline clear authority or baseline reward-viability authority;
- Ascension attrition overlays may not create pseudo-lifecycle fail states by removing meaningful recovery while lifecycle counters remain formally unchanged.

In Dungeon, `meaningful recovery` means the floor still preserves a realistic, skill-based path back to baseline completion after a major mistake without granting extra attempts, retries, checkpoints, re-entry, or hidden objective forgiveness.

Immutable lifecycle fields for Dungeon:

- attempt count;
- retry count;
- checkpoint structure;
- wipe-reset scope;
- re-entry cost;
- floor completion authority;
- boss unlock state within floor;
- sector-finale clear ownership;
- failure-state transition graph.

## Optional Objectives

Optional objectives exist for mastery and replay variance, not to invalidate baseline clear.

Good uses:

- faster clear paths;
- alternate progression hooks;
- room-state manipulation for a better boss entry;
- mastery goals for repeat runners.

Bad uses:

- hiding mandatory baseline reward behind opaque side tasks;
- forcing objective overload into an already dense boss room;
- invalidating baseline floor completion when ignored.

## Repeatability Model

Dungeon repeatability should rely on:

- floor identity;
- difficulty hooks;
- optional-objective variation;
- sector mastery progression;
- composition variation across solo and party play.

Dungeon repeatability should not rely on:

- daily lockouts as the only retention lever;
- monopoly on mandatory power;
- purely inflated stat checks.

## Relationship to Other PvE Systems

### Raid

- separate content format;
- may have stronger assignment expectations;
- must not become the only mandatory power lane.

### Public Events

- world-facing and participation-fluid;
- should not replace authored `1-4` floor mastery;
- may complement Dungeon progression, not subsume it.

### Endgame Specializations

- sigils/maps are run-level package modifiers layered onto Dungeon, not the default access model;
- Ascension is a broader progression wrapper, not a floor selector;
- Dungeon evergreen remains the stable small-group PvE backbone.

## Readiness

This document is strong enough to guide:

- sector and floor-family planning;
- cadence planning for bosses and minibosses;
- floor-level content planning that depends on a stable evergreen-vs-package boundary.

It still depends on later documents for:

- exact floor-failure values;
- reward values;
- UI and entry surfacing.
