# Plugin Agent Roster

Purpose: route MC server plugin work to one primary agent, with clear handoffs
when a change crosses domains.

## Inventory Snapshot

- Source of truth: `MCMMORPG/plugins/`, `docs/plugin_manifest.yaml`,
  `docs/compatibility_matrix.md`, and `MCMMORPG/logs/latest.log`.
- Current server path: `MCMMORPG`.
- Re-check runtime logs before treating older HuskSync, MCPets, MythicMobs, or
  MythicDungeons issues as still active.
- Use `docs/ai/mechanic-plugin-map.md` for mechanic-level routing.

## Agents

| Agent | Primary plugins/areas | Responsibility |
| --- | --- | --- |
| `minecraft-plugin-inventory` | All plugin JARs, manifest, compatibility docs | Versions, dependency map, docs drift, ownership routing |
| `minecraft-rpg-systems` | MMOCore, MMOItems, MMOInventory, MythicLib, MythicCrucible | Classes, stats, itemization, progression, loot, crafting, balance |
| `minecraft-mobs-models` | MythicMobs, ModelEngine, MCPets, LibsDisguises, MEG-Molang, PixelLibs, Nexo | Mobs, bosses, pets, models, animations, resource-pack assets |
| `minecraft-dungeon-world` | MythicDungeons, Iris, Multiverse-Core, VoidGen, Chunky, FAWE, WorldGuard | Dungeons, worlds, instances, region flags, pregeneration, map rollback |
| `minecraft-ui-hud-menus` | DeluxeMenus, MythicHUD, DecentHolograms, PlaceholderAPI, CMI UI config | Menus, HUD, holograms, placeholders, player-facing text |
| `minecraft-visual-assets` | Blockbench, Nexo, ModelEngine source, resource pack | Art direction, weapons/items, blocks, furniture, skins, cosmetics, icons, textures and asset integration |
| `minecraft-permissions-economy` | LuckPerms, Vault, CMI, Guilds | Permission nodes, ranks, economy effects, command exposure, social gates |
| `minecraft-ops-sync` | HuskSync, voicechat, ProtocolLib, PacketEvents, spark, bStats, logs | Startup health, SQL/storage, packet dependencies, voice, performance |
| `minecraft-release-qa` | Logs, RCON, mineflayer, validation scripts, changed plugin configs | Final readiness gate, rollback evidence, validation completeness |

## Handoff Rules

- UI work hands off to `minecraft-permissions-economy` when menus execute
  commands, charge currency, grant items, teleport, or expose rank-gated actions.
- Visual asset work hands gameplay ownership to `minecraft-rpg-systems`,
  `minecraft-mobs-models`, `minecraft-dungeon-world`, or
  `minecraft-ui-hud-menus`; `minecraft-visual-assets` keeps ownership of the
  editable source, visual quality, stable IDs, and resource-pack integration.
- Mobs/models work hands off to `minecraft-rpg-systems` when drops, stats, loot
  tables, or progression gates change.
- Dungeon/world work hands off to `minecraft-release-qa` when worlds, instances,
  regions, backups, or rollback are involved.
- Ops/runtime work hands off to `minecraft-release-qa` when startup errors block
  release readiness or require staging config changes.
- Inventory work hands off to the owning domain agent when drift is behavioral,
  not just documentation.

## Daily Usage

- Generate a prompt from `$HOME`:
  `npm run mc:agent -- <alias> "<task>"`
- Start read-only unless implementation was explicitly requested.
- Ask for a plan before risky edits to world, economy, permissions, DB/storage,
  or player-data-adjacent state.
- Use `docs/ai/mc-agent-task-cheatsheet.md` when the first agent is unclear.

## Current Known Issue Owners

| Issue | Primary agent | Secondary agent |
| --- | --- | --- |
| HuskSync MySQL failure | `minecraft-ops-sync` | `minecraft-release-qa` |
| MCPets SQL fallback | `minecraft-ops-sync` | `minecraft-mobs-models` |
| MythicMobs duplicate model warning | `minecraft-mobs-models` | `minecraft-release-qa` |
| MythicDungeons PlayerQuitEvent cleanup error | `minecraft-dungeon-world` | `minecraft-release-qa` |

See `docs/ai/known-runtime-issues.md` for read-only triage runbooks.

## Minimum Done Criteria

- Responsible domain skill used or explicitly ruled out.
- Local source files and assumptions named.
- Startup log impact checked or deferral reason recorded.
- Staging, RCON, or in-game validation recorded when behavior changes.
- Rollback documented for high-risk changes.
- Learning note written or explicitly marked unnecessary after failures.
- Report follows `docs/ai/agent-workflows.md`.
- Source lookup follows `docs/ai/plugin-knowledge-map.md`.
