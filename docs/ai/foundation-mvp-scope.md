# Foundation MVP Scope

Purpose: make the current staging MVP boundary explicit so runtime work does not
expand into unrelated foundation systems.

This file describes the active MVP slice only. Milestone truth still lives in:

- `docs/ai/foundation-long-run-ledger.md`
- `docs/ai/foundation-implementation-status.md`

## In Scope

The current MVP is only this exact player path:

`Stolica Wyspy -> starter package -> profile signal -> Portal Nexus -> level_1 -> first boss -> reward -> return`

Included work:

- `M1`: hub/menu onboarding, orientation route, visual evidence for the active references
- `M2`: starter backend delivery, profile signal, fresh-player early-combat usability proof
- `M3`: dungeon entry, difficulty handoff, fresh instance arrival, finish, return
- `M4`: real combat target, boss identity, death-triggered completion, reward hook
- `M5`: minimal permissions and economy gates needed for the path
- `M6`: staging runtime safety and scoped log review
- `M7`: exact-player-path QA plus visual fidelity pack

## Out Of Scope

Do not treat these as part of the current MVP closure:

- guild implementation or guild wars
- seasons, prestige, cosmetics, or seasonal economy
- loadout, bank, or storage implementation beyond what is already required for the slice
- world-event implementation
- broader endgame loops, PvP progression, faction loops, or advanced crafting
- production launch hardening
- database migrations or destructive player/world resets

These may remain as design docs or architecture references in this cycle.

## Done Definition For This Cycle

The cycle is not done when config exists. It is done only when:

- `M1` has visual proof against current references
- `M2` has direct fresh-player combat usability proof
- `M3` has exact player-path entry -> finish -> return proof
- `M4` has real boss death -> reward proof
- `M7` has one clean exact-path pass first, then three consecutive fresh-user passes

## Default Decisions

- Runtime closure beats feature expansion.
- Architecture docs may describe later systems, but implementation work stays on the active slice.
- The canonical hub name is `Stolica Wyspy`; `miasto-kowale` is not valid.
