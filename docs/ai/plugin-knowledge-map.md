# Plugin Knowledge Map

Purpose: shared source map for inspecting plugin syntax, runtime
behavior, and cross-plugin integrations.

## Source Ladder

Use sources in order and name the level used in reports:

1. Local installed config or nearby working example.
2. Local project docs and cached references.
3. Context7, if available.
4. Official plugin documentation or vendor wiki.
5. Trusted community/web source, only when higher-trust sources do not answer.

Do not guess syntax, placeholders, permission nodes, MythicMobs skills,
ModelEngine IDs, Nexo/resource-pack paths, database settings, or economy command
effects.

## Domain Starting Points

| Domain | Start | Do not expose |
| --- | --- | --- |
| Plugin inventory | `MCMMORPG/plugins/`, `docs/plugin_manifest.yaml`, `docs/compatibility_matrix.md` | JAR internals unless needed as evidence |
| Runtime operations | `MCMMORPG/logs/latest.log`, plugin runtime config names, service docs | DB credentials, hostnames, tokens, player data |
| RPG systems | `MMOCore/classes`, `MMOItems/item`, tiers, types, stats, drop tables | `userdata/`, live player inventories |
| Mobs and models | `MythicMobs/mobs`, `skills`, `droptables`, `ModelEngine/blueprints`, resource-pack paths | generated caches unless explicitly in scope |
| Dungeons and worlds | MythicDungeons maps/config, Iris packs/worlds, Multiverse worlds, WorldGuard regions | generated chunks, player data, destructive world edits |
| UI, HUD and menus | DeluxeMenus menus, MythicHUD layouts/listeners, PlaceholderAPI config, CMI UI settings | admin-only commands hidden in menus without review |
| Permissions and economy | LuckPerms, Vault, CMI, Guilds configs and local permission examples | security keys, DB contents, private player state |
| Release QA | `MCMMORPG/logs/latest.log`, `_validation/`, changed config paths, rollback notes | secrets, raw sensitive logs, private player state |

## Shared Cross-Checks

- Every command-executing UI action needs permissions/economy review.
- Every mob drop or reward path needs RPG/economy ownership.
- Every dungeon spawn/final trigger involving a MythicMob needs both dungeon and
  mobs ownership.
- Every storage or SQL warning needs ops ownership before release readiness.
- Every behavior change needs the cheapest validation that proves the change.
