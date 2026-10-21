---
title: Endgame Framework
domain: "07-pve-encounters"
status: draft
last_updated: 2026-08-12
---

# Endgame Framework

## Domain 03 Combat Binding Inheritance

Evergreen variants, sigil/map packages and Ascension overlays remain Domain 07
policy layers. Any combat mechanic they introduce or intensify must resolve an
exact mandatory `d03_consumer_bindings` entry inherited from
`encounter-framework.md`, with `required_readiness: RUNTIME_VERIFIED` and one
immutable contract/gate-envelope/bundle/build lineage.

Classifier outcome, `package_id`, parity, disclosure, package status, or
Ascension eligibility is never combat readiness. Missing, stale, mismatched,
downgraded, or revoked binding suppresses/rejects only the affected mechanic and
blocks the consuming run contract above validation fail-closed; no package or
Ascension fallback may execute local combat behavior.

## Purpose

This document defines the structural split of PvE endgame once baseline Island and 100-floor Dungeon progression already exist.

It prevents endgame from collapsing into:

- the same content with more HP;
- calendar-only chores;
- one mandatory raid lane;
- three differently named versions of the same challenge loop.

It builds on:

- `docs/design/pve/encounter-framework.md`
- `docs/design/pve/mob-archetypes.yaml`
- `docs/design/pve/boss-framework.md`
- `docs/design/pve/dungeon-framework.md`
- `docs/design/pve/public-events.md`
- `docs/design/pve/raid-framework.md`

This document defines structural roles, boundaries, classifier rules, and progression functions. It does not define reward values, power curves, or UI surfaces.

## Closed Inputs

This framework assumes:

1. Islands and Dungeon must each support an independent progression path.
2. Raid is separate content and may not be the only mandatory source of power.
3. World Boss and Public Meta-Event are separate systems.
4. Endgame may not reduce to daily/weekly attendance chores.
5. Dungeon floors remain individually selectable, repeatable, and party-scaled for `1-4` players.

## Endgame Pillars

PvE endgame is split into four distinct pillars:

1. `Evergreen Dungeon/Island Mastery`
2. `Sigils / Maps`
3. `Ascension`
4. `Raid / Public Prestige`

If two pillars produce the same practical session loop, the split has failed.

## Pillar Roles

| Pillar | Primary function | Session shape | Player motive |
| --- | --- | --- | --- |
| `Evergreen Dungeon/Island Mastery` | Baseline repeatable PvE backbone | Direct target selection | Reliable replayable progression |
| `Sigils / Maps` | Explicit package-bound challenge runs | Bound package on top of evergreen target | Opt-in high-variation challenge |
| `Ascension` | Long-cycle prestige overlay | Persistent progression-state overlay | Mastery extension without replacing baseline |
| `Raid / Public Prestige` | Large-group or public prestige lane | Separate participation model | Social coordination and prestige |

## Core Separation Rules

1. `Evergreen Dungeon/Island Mastery` is the baseline content namespace for repeatable Dungeon and Island endgame participation.
2. `Sigils / Maps` are external package-namespace modifiers applied onto an evergreen target.
3. `Ascension` is a progression-state overlay applied after run-classifier resolution.
4. `Raid / Public Prestige` lives outside the Dungeon/Island endgame run classifier and defines its own participation and parity rules.
5. Namespace alone never decides classification. Classification must satisfy namespace and capability rules together.
6. Any content whose contract crosses the package-like threshold must move into package namespace even if it began life as a selectable evergreen variant.
7. Package-like content without a canonical `package_id` is an invalid authored state.

## Precedence Rules

Dungeon/Island endgame uses one canonical order:

1. Resolve evergreen target.
2. Resolve run classifier.
3. If package-bound, bind package contract.
4. Apply Ascension overlay.
5. Resolve parity and reward lanes for the final combined contract.

Normative rules:

- `Evergreen` must remain directly selectable without requiring package state or Ascension state.
- `Sigils / Maps` must never replace evergreen as the default Dungeon or Island entry model.
- `Ascension` must never replace evergreen as the default Dungeon or Island entry model.
- `Ascension` may change how a player approaches evergreen progression, but may not remove the underlying evergreen layer.

## Run Classifier

Every Dungeon or Island endgame run must classify as exactly one of:

- `EVERGREEN_RUN`
- `SIGIL_OR_MAP_RUN`

Base rules:

- `EVERGREEN_RUN` selects a target from the baseline evergreen catalog and does not require an external `package_id` before entry.
- `SIGIL_OR_MAP_RUN` requires an external `package_id` and package namespace before entry, then binds that package onto a baseline evergreen target.
- `Ascension` is not a run classifier and may not change the classifier result after resolution.

## Capability-Based Classifier

Namespace and entry shape are necessary but not sufficient.

An authored target may remain `EVERGREEN_RUN` only if all of the following are false:

- it requires a `package_id` at entry;
- it declares package-specific parity;
- it applies a contract bundle intended to be portable across multiple evergreen targets;
- it changes the run contract beyond the evergreen capability envelope;
- it crosses the package-like threshold.

## Classifier Decision Table

Use this table after namespace shape has been identified:

| Case | Baseline catalog target? | External `package_id`? | Crosses capability envelope? | Crosses package-like threshold? | Result |
| --- | --- | --- | --- | --- | --- |
| Floor or region variant attached to one target, no extra contract layer | Yes | No | No | No | `EVERGREEN_RUN` |
| Directly selectable variant with separate parity lane but no package id | Yes | No | Yes | Yes | Invalid authored state. Must be re-authored with canonical `package_id` and ship as `SIGIL_OR_MAP_RUN`. |
| Portable modifier shell reused across multiple targets without package id | Maybe | No | Yes | Yes | Invalid authored state. Must be re-authored with canonical `package_id` and ship as `SIGIL_OR_MAP_RUN`. |
| Package declared before entry with canonical `package_id` | Yes | Yes | Yes | Yes | `SIGIL_OR_MAP_RUN` |
| Harder baseline variant on one target, no portable shell, no extra parity | Yes | No | No | No | `EVERGREEN_RUN` |
| Variant introduces second explicit challenge-contract layer but no package id | Yes | No | Yes | Yes | Invalid authored state. Must be re-authored with canonical `package_id` and ship as `SIGIL_OR_MAP_RUN`. |
| Variant introduces second explicit challenge-contract layer with canonical `package_id` | Yes | Yes | Yes | Yes | `SIGIL_OR_MAP_RUN` |

Tie-break rules:

- if content can be argued both ways, the stricter result wins;
- any stricter result lands on `SIGIL_OR_MAP_RUN` and requires canonical `package_id` before content may ship.

## Evergreen Capability Envelope

Evergreen variants may:

- remain attached to one named target;
- increase or decrease density, support load, or boss-side pressure inside that target's baseline identity;
- reuse the shared floor-failure model without altering lifecycle semantics.

Evergreen variants may not:

- bind external package identity or package-specific parity;
- declare portable modifier bundles intended for reuse across unrelated targets;
- add a second challenge-contract layer on top of target selection;
- override lifecycle semantics;
- combine modifier families into a reusable packaged challenge shell.

## Package-Like Decision Test

Apply the following test in order:

1. If content declares its own `package_id`, `package_namespace`, or package-specific parity key, it is package content.
2. If content is authored for reuse across multiple floors, sectors, or regions without being inseparable from one baseline target, it is package content.
3. If content requires a second authored challenge ruleset beyond choosing the evergreen target itself, it is package content and must carry canonical `package_id`.
4. If content introduces a challenge shell whose primary identity is the modifier bundle rather than the baseline target, it is package content and must carry canonical `package_id`.
5. If content combines two or more modifier families from `pressure`, `route`, `boss`, `objective`, or `attrition`, continue to rule 6.
6. If that multi-family combination is inseparable from one baseline target, has no package-specific parity, adds no second authored challenge ruleset, and is not portable, it may remain evergreen. Otherwise it is package content and must carry canonical `package_id`.
7. If content changes encounter contract across more than one encounter phase or room family in a way intended to be portable, it is package content and must carry canonical `package_id`.

Result rule:

- package-like content without canonical `package_id` is an invalid authored state, not a third classification.

## Dispositive Proxy Threshold

Operational proxies are not advisory hints. They are classifier inputs with a closed decision rule.

### `primary identity`

Classify content as package-like under `primary identity` when either:

- any one dispositive proxy is true; or
- any two supporting proxies are true.

Dispositive proxies:

- standalone parity lane exists for the modifier bundle;
- reusable metadata lineage exists independently of one baseline target;
- the modifier bundle is named as a reusable run identity across multiple targets.

Supporting proxies:

- authoring history is tracked independently of one baseline target;
- target name is routinely secondary in design or review language;
- modifier naming survives target swap without rewriting the core contract.

Precedence rule:

- `inseparable from one baseline target` overrides supporting `primary identity` proxies by default;
- supporting proxies alone may not classify inseparable target-bound content as package-like;
- inseparable target-bound content becomes package-like only when a hard package signal also exists:
  - canonical `package_id`;
  - package-specific parity lane;
  - reusable metadata lineage independent of the target;
  - reusable run identity naming across multiple targets.

### `extra contract layer`

Classify content as package-like under `extra contract layer` when either:

- any one dispositive proxy is true; or
- any two supporting proxies are true.

Dispositive proxies:

- dedicated modifier payload with its own stable behavioral semantics exists beyond base target selection;
- separate parity lane exists for the added ruleset;
- package-specific constraints exist that remain meaningful if copied onto another target;
- second authored challenge ruleset exists that remains meaningful if copied onto another target.

Supporting proxies:

- the added ruleset can be documented as a standalone challenge bundle;
- the added ruleset stays semantically intact after target swap;
- the added ruleset changes encounter obligations in a way that is not fully described by target selection alone.

Non-proxy clarification:

- disclosure burden alone is not an extra contract layer;
- extra explanation, UI copy, tutorialization, or warning text for a target-bound evergreen variant does not by itself create package content;
- package classification requires a second authored ruleset with its own stable behavioral semantics, not merely clearer presentation of one target-bound variant.

Tie-break rule:

- if one reviewer finds the dispositive threshold met and another does not, the stricter result wins and the content must be authored as package content.

### Package-Like Glossary

- `portable`
  A modifier contract is portable when it can be rebound onto more than one baseline floor, sector, or region target without rewriting its core rule payload.
- `inseparable from one baseline target`
  A modifier is inseparable when its logic only makes sense for one named baseline target and cannot be rebound elsewhere without redesigning the contract itself.
- `primary identity`
  Primary identity is the element a player or author would use to name the run first.
- `extra contract layer`
  An extra contract layer exists when a player must understand or accept a second authored challenge ruleset beyond selecting the baseline evergreen target.
- `meaningful recovery`
  Meaningful recovery means the run still preserves a realistic, skill-based path back to successful completion after a major mistake without requiring the system to grant extra attempts, retries, checkpoints, or re-entry.

## Evergreen Mastery

Evergreen is the default non-raid endgame backbone.

Dungeon evergreen:

- stays directly selectable;
- remains understandable without rare entry items;
- remains valid for solo and party play;
- may include baseline floor variants and target-specific mastery variants;
- may not require package identity or Ascension state to instantiate the basic loop.

Island evergreen:

- stays directly selectable;
- remains independent from Dungeon participation;
- may include regional pressure variants only while they remain target-bound and package-free.

## Sigils / Maps

Sigils and maps are explicit run packages layered onto evergreen targets.

They may modify one or more of:

- encounter composition;
- support presence;
- reinforcement behavior;
- add cadence;
- telegraph compression;
- hazard persistence;
- route obligations;
- objective obligations;
- boss-side pressure packages.

They should not mainly modify:

- raw HP only;
- raw damage only;
- one invisible numeric multiplier with no change in play pattern.

### Authoring Rules

Every sigil/map package must declare:

- `package_id`
- `package_namespace`
- `run_target`
- `modifier_family`
- `play_pattern_delta`
- `encounter_scope`
- `failure_pressure_delta`
- `clarity_guardrails`
- `parity_key`

Canonical identity rules:

- `package_id` is the sole canonical run-contract identifier for sigil/map package content.
- `package_id` must be globally unique within the sigil/map package registry.
- `package_id` is immutable after shipping; replacement requires a new `package_id`.
- superseding a package must create explicit lineage from one `package_id` to another and may not reuse the prior identifier.
- `parity_key` is secondary comparison metadata only and may not serve as package identity.
- `run_target` scopes where a package may bind, but does not replace or redefine package identity.
- the authoritative registry surface for `package_id` issuance, supersession, retirement, and uniqueness enforcement is `docs/design/pve/package-registry.yaml`, owned by domain `07`.
- only `docs/design/pve/package-registry.yaml` may mint, supersede, or retire `package_id`; all other planning documents are read-only consumers of canonical package identity.

### Structural Guardrails

- packages exist specifically for contracts that cross the evergreen capability envelope and package-like threshold;
- packages may not redefine floor-level attempt, retry, or reset semantics by default;
- sigil/map packages that modify pressure, attrition, hazard persistence, consequence severity, objective burden, or route burden must preserve meaningful recovery inside the inherited lifecycle model;
- sigil/map packages may not create pseudo-lifecycle fail states by removing meaningful recovery while lifecycle counters remain formally unchanged;
- package content may not be backported into evergreen merely by changing registration location.

## Ascension

Ascension is a prestige mastery reset lane layered over baseline progression.

It must:

- remain persistent across more than one run;
- operate as a long-cycle progression-state choice rather than a one-session package;
- preserve a recognizable underlying evergreen content lane.

It must not:

- function as the default floor-difficulty selector;
- function as the only gateway into baseline repeatable endgame PvE;
- collapse into renamed sigil/map package content;
- invalidate non-Ascension baseline endgame viability.

## Ascension Override Conflict Matrix

Ascension applies after classifier resolution and package bind. It does not have unlimited override authority.

| Contract Area | Ascension may override? | Rule |
| --- | --- | --- |
| Run classifier | No | Ascension may not change `EVERGREEN_RUN` into `SIGIL_OR_MAP_RUN` or reverse. |
| Namespace ownership | No | Ascension may not move content between evergreen catalog and package namespace. |
| Target identity | No | Ascension may not replace selected floor, sector band, or region target at entry resolution. |
| Package identity | No | Ascension may not delete, swap, or spoof bound `package_id` or `package_namespace`. |
| Lifecycle fields | No | Ascension may not alter immutable lifecycle fields. |
| Parity lane family | Limited | Ascension may add Ascension-specific parity, but may not replace package or evergreen identity. |
| Encounter pressure | Yes | Ascension may scale pressure only inside the allowed overlay envelope. |
| Clarity/readability guardrails | No | Ascension may not override readability floors guaranteed by parent contracts. |
| `route` family | No | Ascension may not introduce or replace package-like route obligations after classifier resolution. |
| `objective` family | No | Ascension may not introduce package-like objective shells or swap the primary objective contract of the baseline or bound package. |
| `boss` family | Limited | Ascension may scale boss pressure, but may not replace the boss mechanic family or create a boss-side challenge shell. |
| `attrition` family | Limited | Ascension may scale attrition pressure only inside immutable lifecycle fields and without creating a second shell. |

### Allowed Ascension Objective Operations

- adjust success pressure on an already existing objective;
- tighten tolerance windows on an already existing objective without replacing its objective family;
- scale consequence severity inside immutable lifecycle guardrails.

### Denied Ascension Objective Operations

- adding a new primary objective family;
- replacing the primary objective contract of the baseline target or its bound package;
- injecting a reusable objective shell that could stand alone as package content;
- creating pre-entry objective disclosure that would become a second challenge-contract layer;
- converting an optional objective into baseline clear authority;
- converting an optional objective into baseline reward-viability authority.

### Allowed Ascension Attrition Operations

- scale long-cycle resource pressure bands;
- scale ongoing punishment intensity within a run;
- add prestige-state burden that does not alter immutable lifecycle fields.

### Denied Ascension Attrition Operations

- resource-starvation wrappers that create a new package-like challenge shell;
- run-wide depletion contracts that replace the baseline or bound package mechanic family;
- timer, starvation, or decay rules that effectively redefine attempt, retry, checkpoint, wipe-reset, or re-entry behavior;
- portable attrition bundles that should instead live as sigil/map package content;
- attrition overlays that create pseudo-lifecycle fail states by removing meaningful recovery while lifecycle counters remain formally unchanged.

### Ascension Conflict Rules

- if a proposed Ascension rule adds a new `route`, `objective`, reusable `boss`, or reusable `attrition` shell after package bind, it is invalid and must be re-authored as package content;
- Ascension may intensify an already bound package, but may not become a third challenge-contract layer;
- when Ascension and a bound package touch the same allowed pressure axis, the package defines the mechanic family and Ascension may only scale within that family's guardrails.

## Immutable Lifecycle Fields

Until a future dedicated floor-failure contract explicitly supersedes this section, the following fields are immutable to packages and Ascension overlays:

- attempt count;
- retry count;
- checkpoint structure;
- wipe-reset scope;
- re-entry cost;
- floor completion authority;
- boss unlock state within floor;
- sector-finale clear ownership;
- failure-state transition graph.

## Readiness

This document is strong enough to support:

- downstream Dungeon and Island endgame planning;
- future sigil/map detailed spec work;
- future Ascension detailed spec work.

It still depends on later documents for:

- reward values;
- UI surfaces;
- final floor-failure values.
