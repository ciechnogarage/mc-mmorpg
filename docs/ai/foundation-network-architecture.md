# Foundation Network Architecture

Purpose: describe the current runtime topology for the foundation MVP slice,
including player-path handoff, plugin boundaries, validation flow, and staging
assumptions.

This is an architecture map of the existing stack. It is not a proposal for a
new service architecture.

## Runtime Topology

1. Player joins local staging `Paper` runtime in `MCMMORPG/`.
2. Hub/menu interaction starts in DeluxeMenus through `foundation_nexus.yml`.
3. Starter backend mutates item/currency/claim state through console-dispatched commands.
4. Profile signal routes into MMOCore.
5. Portal Nexus routes into MythicDungeons `level_1`.
6. Dungeon runtime depends on MythicDungeons instance flow plus MythicMobs encounter hooks.
7. Reward/finish path returns through MythicDungeons and economy/permission state already granted in the slice.

## Primary Boundaries

| Boundary | Active component | Responsibility |
| --- | --- | --- |
| Hub/menu UI | `DeluxeMenus` | player-visible onboarding, starter claim button, Portal Nexus handoff |
| RPG/profile/items | `MMOCore`, `MMOItems` | profile UI, starter item contract, level gate |
| Economy/claims | `CMI`, `LuckPerms`, `Vault` | starter money, claim node, default access gates |
| Dungeon runtime | `MythicDungeons` | instance creation, difficulty menu, return path |
| Encounter runtime | `MythicMobs` | boss identity, spawn/death hooks, reward chain |
| Runtime evidence | `_validation/` harness + `latest.log` | proof collection, scoped log review, run artifacts |

## Validation Path

The proof path for this MVP is:

1. static contract check against menu/spec/plugin config
2. live Paper + RCON readiness
3. fresh-player menu path
4. exact Portal Nexus handoff
5. confirmed `level_1_*` instance arrival
6. confirmed combat target / boss identity
7. real death -> finish -> reward -> return
8. scoped `latest.log` review with no new critical exceptions

Admin shortcuts, raw RCON substitutions, or config-only checks do not replace
player-path proof.

## Staging Assumptions

- `online-mode=false` is accepted for local MVP validation.
- runtime-touching validation is serialized by `_validation/.runtime.lock`
- Nexo pre-join pack dispatch is non-blocking for bot QA
- HuskSync stays out of the active MVP path
- MCPets YAML fallback is accepted staging noise while pets remain out of scope

## Current Known Sensitive Seams

- starter backend contract can drift if the menu commands change without harness updates
- dungeon arrival proof depends on the exact post-Portal Nexus player path, not a console approximation
- encounter proof depends on a real combat entity in the freshly created instance
- finish proof depends on boss death hooks and reward visibility, not only chat success

## Source Of Truth

- closure rules: `docs/ai/foundation-long-run-ledger.md`
- milestone state: `docs/ai/foundation-implementation-status.md`
- hard config values: `docs/ai/foundation-mvp-spec-values.md`
- document routing: `docs/ai/foundation-master-index.md`
