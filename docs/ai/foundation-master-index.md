# Foundation Master Index

Purpose: one canonical map of the current foundation v0.0.1 documents, the
staging MVP slice, and the milestone each document primarily supports.

Use this file as the first index before expanding scope or creating another
foundation document. Status and runtime truth still live in:

- `docs/ai/foundation-long-run-ledger.md`
- `docs/ai/foundation-implementation-status.md`

## Core Control Docs

| Document | Role | Primary milestone |
| --- | --- | --- |
| `docs/ai/foundation-mvp-spec-values.md` | hard values and explicit config contracts for the current slice | `A1`, `M1-M7` |
| `docs/ai/foundation-mvp-implementation.md` | staging-first implementation control plan | `M0-M7` |
| `docs/ai/foundation-mvp-task-board.md` | delegowalna tablica zadań | `A1-E` |
| `docs/ai/foundation-long-run-ledger.md` | runtime proof ledger and closure rules | `M1-M7` |
| `docs/ai/foundation-implementation-status.md` | owner/status/next action matrix | `M0-M7` |
| `docs/ai/foundation-agent-reports.md` | integration-grade report contract | all runtime-touching work |

## Architecture Pack

| Document | Role | Primary milestone |
| --- | --- | --- |
| `docs/ai/foundation-mvp-scope.md` | in-scope and out-of-scope boundaries for the current staging MVP | `M0-M7` |
| `docs/ai/foundation-network-architecture.md` | runtime topology, plugin boundaries, and validation path | `M2-M7` |
| `docs/ai/foundation-master-index.md` | index and routing map for the whole foundation doc set | integrator |
| `docs/ai/modelengine-boss-review-pipeline.md` | boss-first strict review queue, graph contract, and QA handoff target for ModelEngine bosses | `M4`, `M7` |

## Player-Path Design Docs

| Document | Role | Primary milestone |
| --- | --- | --- |
| `docs/player-journey-milestone-roadmap-v0.0.1.md` | full player journey and level milestones | long-range design |
| `docs/onboarding-tutorial-foundation-v0.0.1.md` | onboarding expectations and starter experience | `M1-M2` |
| `docs/stolica-wyspy-hub-foundation-v0.0.1.md` | canonical hub concept for `Stolica Wyspy` | `M1` |
| `docs/level-1-dungeon-island-foundation-v0.0.1.md` | first dungeon slice | `M3` |
| `docs/mob-boss-encounter-001-foundation-v0.0.1.md` | boss encounter contract | `M4` |
| `docs/loot-reward-table-001-foundation-v0.0.1.md` | first reward path | `M4-M5` |

## Supporting System Docs

These remain design inputs, not active implementation scope for this MVP cycle:

- `docs/guilds-foundation-v0.0.1.md`
- `docs/seasons-prestige-cosmetics-foundation-v0.0.1.md`
- `docs/loadout-bank-storage-foundation-v0.0.1.md`
- `docs/world-events-foundation-v0.0.1.md`
- `docs/world-content-loop-foundation-v0.0.1.md`
- `docs/reputation-faction-foundation-v0.0.1.md`
- `docs/pvp-foundation-v0.0.1.md`

## Default Routing

1. Need runtime truth or closure criteria: open `foundation-long-run-ledger.md`.
2. Need milestone owner/state: open `foundation-implementation-status.md`.
3. Need exact config contract: open `foundation-mvp-spec-values.md`.
4. Need scope boundary: open `foundation-mvp-scope.md`.
5. Need plugin/runtime topology: open `foundation-network-architecture.md`.

## Assumptions

- Canonical hub name is `Stolica Wyspy`.
- Current MVP closure target is the exact vertical slice, not later systems.
- New docs should link back to this index instead of creating another top-level map.
