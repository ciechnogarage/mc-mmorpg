---
title: "Economy Model Notes"
source: "Curated from docs/imports/minecraft-legacy-2026-07-21/raw/design/economy_model.md"
status: "active-curated"
---

# Economy Model Notes

This document preserves useful economy design lessons from the legacy import. It does not authorize historical operational commands, old workspace paths, or direct production changes.

Current operational authority for implementation is:

- canonical project root: `/home/przemek/projects/MC`
- feature work: isolated `dev-<scope>` lease before write/runtime work
- integration: staging only after dev validation and explicit promotion

## MVP economy

Start with one currency and a small number of clear transactions:

| Type | Initial examples | Design intent |
| --- | --- | --- |
| Sources | mob drops, dungeon rewards, starter quests | let players feel progress quickly |
| Sinks | repairs, upgrades, travel fees, cosmetics | prevent reward inflation |
| Value definitions | item sell/trade flags, reward tables | keep balancing reviewable in source |

The first economy target is “useful and testable”, not “complete marketplace”.

## Release risk

Economy changes are high-risk because they can permanently distort player inventories and progression. Any non-trivial economy change must have:

- source diff showing exact changed rewards/prices;
- rollback method;
- exploit review;
- test evidence from the relevant `dev-<scope>` environment;
- staging promotion only after review.

## Required exploit review

Before promotion, check at least:

- duplication paths;
- repeatable farm loops;
- dungeon reset reward abuse;
- sell/buy arbitrage;
- reward inflation from stacked systems;
- permission leakage that gives unintended sell, shop, or admin access;
- persistence mismatch after reconnect.

## Balancing rules

- Prefer fewer reward paths until telemetry or playtest evidence exists.
- Every source should have a corresponding sink or pacing reason.
- Dungeon rewards should not bypass the intended item/progression ladder.
- Starter rewards should accelerate onboarding, not invalidate early gameplay.
- Economy configuration must be versioned and reviewable; live-only edits are not a valid final state.

## Plugin assumptions

Legacy docs mention possible plugin roles, but current implementation must verify the actual installed stack before binding the design to a plugin-specific syntax. Treat plugin names as candidates until the active environment confirms ownership.
