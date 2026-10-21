---
title: "Minecraft Core Loop"
source: "Curated from docs/imports/minecraft-legacy-2026-07-21/raw/design/core_loop.md"
status: "active-curated"
---

# Minecraft Core Loop

This document is the active, cleaned design summary extracted from the legacy import. It carries design intent only. Operational procedures, historical local paths, old QA scripts, and legacy server instructions are intentionally excluded.

Current operational authority for implementation is:

- canonical project root: `/home/przemek/projects/MC`
- feature work: isolated `dev-<scope>` lease before write/runtime work
- integration: staging only after dev validation and explicit promotion

## Player promise

The player should understand the server as a coherent MMORPG loop:

1. Join into a safe onboarding area.
2. Choose a class or play style.
3. Fight PvE mobs and bosses.
4. Earn XP, loot, and currency.
5. Upgrade gear and skills.
6. Enter dungeons or boss encounters.
7. Return stronger, with persistent progress and clearer next goals.

The loop must stay readable before it becomes large. A small complete slice is better than many disconnected systems.

## MVP vertical slice

The first useful slice is:

1. Safe spawn/onboarding area.
2. Simple menu or NPC/hologram onboarding.
3. Class selection.
4. One PvE region with mobs, XP, and loot.
5. One small dungeon or boss encounter.
6. One economy source and one economy sink.
7. Rejoin persistence check for progression and rewards.

Anything outside this slice is backlog until the loop above is playable end-to-end.

## System ownership boundaries

Use plugin ownership to avoid agents overwriting each other:

| Loop area | Expected owner surface |
| --- | --- |
| Classes and player progression | RPG/progression docs and plugin config |
| Items and gear | item/equipment docs and plugin config |
| Combat mobs and bosses | mob design and MythicMobs source |
| Dungeon encounters | dungeon design, level docs, and dungeon plugin source |
| Protected regions | world/region configuration, gated by environment ticket |
| Menus and onboarding UI | UI/menu docs and plugin config |
| Economy values | economy docs and economy plugin config |

Before changing any of these surfaces, an agent must state the lease scope, allowed paths, validation channel, and rollback plan.

## Acceptance for the core loop

The core loop is not considered done until a real player flow can prove:

- class choice works from a clean player state;
- mobs grant intended XP/rewards;
- loot or currency can be spent on a meaningful sink;
- dungeon/boss entry, completion, and exit are understandable;
- progress persists after rejoin;
- logs show no obvious permission or plugin errors;
- all changes are reproducible from versioned source, not only from live runtime state.

## Design guardrails

- Keep the first loop small enough to test repeatedly on minimal resources.
- Do not promote a feature because a file exists; promote only after behavior is validated.
- Do not mix design docs with machine-specific runbooks.
- Treat imported legacy docs as reference material, not authority.
- Prefer explicit plugin ownership over “whoever touched the file last”.
