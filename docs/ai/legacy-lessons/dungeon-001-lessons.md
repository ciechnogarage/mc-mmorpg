---
title: "Dungeon 001 Legacy Lessons"
source: "Curated from docs/imports/minecraft-legacy-2026-07-21/raw/design/levels/dungeon_001"
status: "active-curated"
---

# Dungeon 001 Legacy Lessons

This document preserves useful lessons from the legacy Dungeon 001 material. It intentionally excludes old local commands, old editor/runtime instructions, machine-specific paths, and historical QA scripts.

Current operational authority for implementation is:

- canonical project root: `/home/przemek/projects/MC`
- feature work: isolated `dev-<scope>` lease before write/runtime work
- integration: staging only after dev validation and explicit promotion

## Useful design lessons

### Keep the dungeon identity simple

Dungeon 001 had a clear useful fantasy: flooded ruin pacing from safe entry, through pressure, into a boss finale. That structure is worth preserving:

| Zone | Job | Player feeling |
| --- | --- | --- |
| Entry | orient player, show first objective | safe curiosity |
| Mid | apply encounter pressure | tension |
| End | boss/payoff and exit clarity | resolution |

The important lesson is not the old files; it is the readable three-act dungeon shape.

### Separate blueprint from editor/runbook state

Historical notes mixed design, editor state, generated files, QA commands, and environment assumptions. The active standard should separate:

- design brief: fantasy, pacing, encounter intent;
- level blueprint: zones, objectives, encounter order;
- implementation source: plugin config or generated assets;
- validation evidence: logs, client checks, screenshots, and test reports;
- operational runbook: only current, lease-compliant commands.

### Treat “file exists” as insufficient evidence

Several legacy notes recorded generated or staged files as if they proved completion. For the active process, a dungeon is not done just because maps, mobs, skills, or config files exist. Acceptance requires behavioral validation:

- player enters the intended start area;
- instance or session boundaries behave as designed;
- objective flow advances in order;
- mobs and boss appear at the intended points;
- completion and exit are clear;
- no obvious permission, log, or persistence errors appear.

### Instance behavior needs explicit proof

Dungeon work must define whether players share a run or receive separate instances. Sequential and simultaneous runs should be tested according to the desired mode. Instance identifiers, world/session naming, and cleanup behavior are validation evidence, not design decoration.

### Visual proof is useful, but not authority

Screenshots and previews help judge layout readability, spawn safety, palette, and mood. They are not canonical assets by themselves. Promote only source assets and configuration that can be regenerated or reviewed from the project root.

### GUI-only plugin state is a risk

When plugin editors store behavior in formats that are hard to hand-edit safely, agents should not pretend the source is fully automated. The standard response is:

- document the expected graph or behavior;
- keep generated/source files under review;
- require human or real-client validation when the editor is the source of truth;
- avoid overwriting editor output without diff review.

## Anti-lessons from the legacy material

- Do not promote old machine-specific runbooks into active docs.
- Do not treat historical QA metadata as proof of current behavior.
- Do not mix design decisions with environment startup instructions.
- Do not let dungeon work bypass the `dev-<scope>` lease system.
- Do not update staging directly as a substitute for isolated dev validation.

## Active acceptance checklist for a new dungeon slice

Before promotion out of dev, a dungeon slice should show:

- active design brief and level blueprint exist in source docs;
- owned plugin/source paths are declared in the lease;
- expected validation channels are declared before testing;
- entry, route, encounter, boss/payoff, and exit have been validated;
- screenshot or real-client evidence supports visual readability where relevant;
- rollback restores the prior dungeon source/config state;
- staging was touched only through explicit integration/promotion work.
