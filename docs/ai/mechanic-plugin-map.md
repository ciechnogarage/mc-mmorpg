# Mechanic Plugin Map

Purpose: route MC design and implementation work from planned mechanics to the
right plugin stack, agent, handoffs, and validation path.

## Core RPG Loop

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Classes, subclasses, skill kits | MMOCore, MMOItems, MythicLib | `minecraft-rpg-systems` | UI for menus/HUD, permissions for gated commands | Config references, class selection, skill behavior, startup log |
| Stats, damage, defense, scaling | MMOCore, MMOItems, MythicLib | `minecraft-rpg-systems` | Mobs for enemy tuning, QA for combat checks | Local examples, controlled combat, log review |
| Progression, respec, milestones | MMOCore, MMOItems, CMI where command driven | `minecraft-rpg-systems` | Permissions/economy, UI | New-player path, command gates, rollback |
| Inventory, loadouts, storage | MMOInventory, MMOItems, CMI, HuskSync for persistence | `minecraft-rpg-systems` | Ops for storage/sync, permissions for access | Do not inspect private player data; use staging-safe checks |

## Items, Loot, Crafting

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Item types, tiers, affixes | MMOItems, MythicLib, MythicCrucible | `minecraft-rpg-systems` | Mobs for drops, UI for display | YAML schema, `/mi` reload/give when approved, log review |
| Loot tables, rewards | MMOItems, MythicMobs droptables, MythicCrucible | `minecraft-rpg-systems` | Mobs, dungeon, economy | Drop path trace, reward economy check |
| Crafting/professions | MMOItems, MythicCrucible, MMOCore, CMI if command-backed | `minecraft-rpg-systems` | Permissions/economy, UI | Recipe visibility, cost/reward path, startup log |
| Prefixes/suffixes | MMOItems, MythicLib | `minecraft-rpg-systems` | QA for balance regression | Local examples, item generation check |

## Encounters, Mobs, Models

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Mobs, elites, bosses | MythicMobs, ModelEngine | `minecraft-mobs-models` | RPG for drops/stats, dungeon for spawn points | Mob ID, model ID, skill ID, spawn test, log review |
| Boss phases and skills | MythicMobs, ModelEngine, MythicLib | `minecraft-mobs-models` | RPG for damage scaling, QA for playtest | Local skill examples, controlled fight, smallest trigger test |
| Pets, companions, minions | MCPets, ModelEngine, MythicMobs | `minecraft-mobs-models` | Ops for storage errors, permissions for access | Pet config, persistence mode, spawn/summon check |
| Resource-pack assets | ModelEngine resource pack, Nexo, PixelLibs, MEG-Molang | `minecraft-mobs-models` | UI if visible names/text change | Asset path trace, cache awareness, no generated-cache edits unless approved |

## World, Dungeons, Regions

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Dungeon instances and finish logic | MythicDungeons, MythicMobs | `minecraft-dungeon-world` | Mobs for boss IDs, QA for end-to-end run | Start -> boss -> death trigger -> finish, latest log |
| Dungeon ladder and difficulty | MythicDungeons, MMOCore/MMOItems for rewards | `minecraft-dungeon-world` | RPG, mobs, QA | Config trace, reward gate, staging run |
| Worlds and map delivery | Multiverse-Core, Iris, VoidGen, Chunky, FAWE | `minecraft-dungeon-world` | QA for rollback/readiness | Backup/rollback, no generated chunk edits without approval |
| Regions and protection | WorldGuard, Multiverse-Core | `minecraft-dungeon-world` | Permissions for access gates | Region flags, allow/deny path, rollback |

## Player-Facing UI And Social Systems

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Menus, navigation, onboarding UI | DeluxeMenus, CMI, PlaceholderAPI | `minecraft-ui-hud-menus` | Permissions/economy for commands/costs | Menu action trace, placeholder source, deny path |
| HUD and combat/status display | MythicHUD, PlaceholderAPI, MMOCore placeholders | `minecraft-ui-hud-menus` | RPG for source stats | Placeholder validation, visual/staging check |
| Holograms and world info | DecentHolograms, PlaceholderAPI | `minecraft-ui-hud-menus` | Dungeon/world for placement | Text/path check, no admin exposure |
| Guilds, ranks, reputation access | Guilds, LuckPerms, Vault, CMI | `minecraft-permissions-economy` | UI for menus, QA for release gate | Permission nodes, default player path, economy effects |
| PvP access and command gates | LuckPerms, WorldGuard, CMI, PvP-related plugin config if present | `minecraft-permissions-economy` | Dungeon/world for regions, QA | Allow/deny path, region flag, rollback |

## Runtime And Release

| Mechanic | Primary plugins | Agent | Handoffs | Validation |
| --- | --- | --- | --- | --- |
| Sync/storage/database health | HuskSync and storage-backed plugins | `minecraft-ops-sync` | QA, owning gameplay agent | Redacted log triage, connectivity classification |
| Packet/protocol dependencies | ProtocolLib, PacketEvents | `minecraft-ops-sync` | QA | Startup enable order, compatibility warnings |
| Voice/performance/metrics | voicechat, spark, bStats | `minecraft-ops-sync` | QA | Log review, config sanity, no secret exposure |
| Release readiness | All affected plugins, logs, RCON, mineflayer | `minecraft-release-qa` | Owning domain agent | Evidence summary, rollback, smallest proving test |
