# Foundation MVP Implementation

Purpose: turn foundation v0.0.1 design docs into a staging-first MVP plan with
clear agent ownership, plugin boundaries, validation gates, and rollback notes.

This is an implementation control document. It does not replace the foundation
docs and does not define final balance numbers, economy values, drop rates, or
production config.

## Default Target

- Environment: `MCMMORPG` staging.
- Runtime target: see `docs/server_environment.md`.
- Plugin source of truth: `docs/plugin_manifest.yaml`,
  `docs/compatibility_matrix.md`, `MCMMORPG/plugins/`, and startup logs.
- Agent source of truth: `docs/ai/plugin-agent-roster.md` and
  `docs/ai/mechanic-plugin-map.md`.
- Change posture: read-only first, then staging-only implementation, then QA.

## MVP Vertical Slice

The first shippable slice is:

1. Player enters `Stolica Wyspy` and orients by city landmarks.
2. Player explores city districts instead of a forced tutorial corridor.
3. Player gets starter skills and basic gear for money.
4. Player reads quest/info board and is pointed to Portal Nexus.
5. Player uses Portal Nexus dungeon selector for `level_1`.
6. Dungeon uses first mob set, first boss/elite encounter, and first loot table.
7. Player receives a readable reward and progression signal.
8. QA confirms no new critical startup/runtime errors.

Out of MVP:

- final class balance numbers;
- production launch;
- full guild wars, seasons, prestige, and endgame loops;
- destructive world resets or player-data changes;
- database migrations without explicit backup and approval.

## Milestones

| ID | Milestone | Primary agent | Plugins / systems | Done when |
| --- | --- | --- | --- | --- |
| M0 | Inventory and runtime baseline | `minecraft-plugin-inventory` | all installed plugins, manifest, compatibility matrix | plugin ownership and known runtime blockers are current |
| M1 | Hub and onboarding path | `minecraft-ui-hud-menus` | DeluxeMenus, MythicHUD, DecentHolograms, PlaceholderAPI, CMI | test player can identify next action without admin help |
| M2 | Starter RPG loop | `minecraft-rpg-systems` | MMOCore, MMOItems, MMOInventory, MythicLib, MythicCrucible | starter skill, first stat/progression signal, and first reward path are testable |
| M3 | Dungeon world slice | `minecraft-dungeon-world` | MythicDungeons, Multiverse-Core, WorldGuard, Chunky, FAWE | level 1 dungeon entry, exit, reset, and protected regions are testable |
| M4 | Mobs, boss, models | `minecraft-mobs-models` | MythicMobs, ModelEngine, MCPets, LibsDisguises, Nexo, PixelLibs | first dungeon mob set and encounter assets load without broken IDs |
| M5 | Economy and access gates | `minecraft-permissions-economy` | LuckPerms, Vault, CMI, Guilds, WorldGuard | minimal access, reward, and command permissions are explicit |
| M6 | Runtime/storage safety | `minecraft-ops-sync` | HuskSync, ProtocolLib, PacketEvents, voicechat, spark | known storage/protocol blockers are triaged and not worsened |
| M7 | Release gate | `minecraft-release-qa` | logs, RCON/in-game checks, regression notes | MVP is marked pass/fail with evidence and rollback notes |

## Agent Order

Run agents in this order for the first pass:

```bash
npm run mc:agent -- inventory "read-only: foundation MVP M0 plugin/runtime baseline"
npm run mc:agent -- ui "read-only: foundation MVP M1 hub onboarding path"
npm run mc:agent -- rpg "read-only: foundation MVP M2 starter RPG loop"
npm run mc:agent -- dungeon "read-only: foundation MVP M3 level 1 dungeon world slice"
npm run mc:agent -- mobs "read-only: foundation MVP M4 dungeon mob boss model slice"
npm run mc:agent -- economy "read-only: foundation MVP M5 economy permissions access gates"
npm run mc:agent -- ops "read-only: foundation MVP M6 runtime storage safety"
npm run mc:agent -- qa "read-only: foundation MVP M7 release gate evidence"
```

After read-only reports are accepted, repeat only the relevant agent task with
`staging implementation requested` in the task text.

## Cross-Agent Handoffs

- RPG to dungeon: class/skill assumptions, expected player power, reward hooks.
- Dungeon to mobs: encounter IDs, spawn points, boss phases, reset behavior.
- Mobs to UI: mob/boss display names, boss bars, placeholders, resource-pack
  requirements.
- UI to permissions/economy: commands exposed to players, menus that grant or
  spend currency, access groups.
- Ops to QA: startup errors, storage warnings, sync risks, performance evidence.

## Validation Gates

Every milestone needs:

- local evidence paths named;
- blast radius class from `docs/ai/agent-workflows.md`;
- startup log impact checked or explicitly deferred;
- smallest staging test described;
- rollback note for economy, permissions, world, storage, or player-data-adjacent
  changes;
- learning note only when the task exposes a reusable lesson.

MVP cannot pass release gate if:

- startup adds new critical errors;
- a test player cannot complete the vertical slice;
- a required plugin owner is unknown;
- a world/storage/economy change has no rollback note;
- a placeholder, permission node, MythicMobs skill, ModelEngine ID, or database
  setting was guessed without source evidence.
