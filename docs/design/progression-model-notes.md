---
title: "Progression Model Notes"
source: "Curated from docs/imports/minecraft-legacy-2026-07-21/raw/design/progression_model.md"
status: "active-curated"
---

# Progression Model Notes

This document extracts active progression design guidance from the legacy import. It is not a runtime runbook and does not carry old machine-specific instructions.

Current operational authority for implementation is:

- canonical project root: `/home/przemek/projects/MC`
- feature work: isolated `dev-<scope>` lease before write/runtime work
- integration: staging only after dev validation and explicit promotion

## MVP progression slice

The first progression slice should prove:

1. A player can choose a class or role.
2. The player can gain levels through the core PvE loop.
3. Skill unlocks happen at intended thresholds.
4. Gear or rewards reinforce the chosen role.
5. Progress persists across reconnect.

The level range can stay small. A reliable 1–3 or 1–5 slice is more valuable than an unvalidated 1–30 table.

## Acceptance criteria

Progression work is not accepted until validation shows:

- class selection works from a clean account state;
- XP gain is tied to intended mobs or activities;
- skill unlocks match the spec;
- locked skills cannot be used through permission mistakes;
- reconnect keeps level, class, and unlock state;
- rollback can restore the prior progression config.

## Design guardrails

- Do not add progression systems faster than they can be tested.
- Keep early-level feedback frequent and clear.
- Avoid reward spikes that invalidate economy balance.
- Keep class identity visible in skills, gear, and encounter roles.
- Treat persistence as part of progression, not as a later infrastructure concern.

## Agent implementation standard

Any agent touching progression must state:

- `dev-<scope>` lease id;
- exact source paths it may edit;
- validation channel for class choice, XP gain, unlocks, and persistence;
- rollback method;
- whether staging was touched.

No lease means read-only or documentation-only work.
