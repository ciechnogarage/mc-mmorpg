# MCPets and CompanionCore cutover evidence (ADR-003)

Status: cutover evidence, 2026-07-20. Backend: `world` (only backend that
carried the MCPets jar; `items` and `hub` never had it).

## Database check

`mcmmorpg-mariadb` (`SHOW DATABASES;`) lists only `information_schema`,
`mcmmorpg`, `mysql`, `performance_schema`, `sys`. No `mcpets_db` (or any
MCPets-named database) exists on staging MariaDB. MCPets' `config.yml` points
`MySQL.Host` at `localhost` on port `2560`, unreachable from inside the
Docker network, so it has never had live SQL state to migrate — confirmed by
the boot log line `[MCPets] Could not reach SQL database` /
`Will be using YAML support instead`, and by `0 pets registered` /
`0 categories registered` at every boot. There is no legacy player-pet data
to migrate; only the plugin's own config/state files existed.

## Backup

`node scripts/backup-mcpets-legacy.mjs` copied both MCPets directories
(source template and the `world` backend's deployed copy — 5 files each:
`blacklist.yml`, `config.yml`, `language.yml`, `menuIcons.yml`,
`petfoods.yml`) with a SHA-256 manifest to
`MCMMORPG/backups/mcpets-legacy/2026-07-20T07-30-58-293Z/manifest.json`
(gitignored, local — `MCMMORPG/backups/` is a pre-existing hygiene-ignored
path). No player-owned data existed to lose; this backup preserves the
plugin's tuning/config in case any of it is wanted as legacy-claim seed data.

### CompanionCore amend backup

Before disabling the remaining live legacy authority, the complete
`plugin-src/companion-core/` source/build tree, the deployed world-backend
`plugins/CompanionCore/` config directory, the top-level plugin JAR and its
Paper remap-cache JAR were copied to the local, gitignored
`MCMMORPG/backups/companioncore-legacy/2026-07-20T08-07-00Z/` directory. Its
`manifest.md` records SHA-256 values. In particular, the deployed JAR and
source build artifact match
`9166f06188ec25a92f73bd1f393d6c9c3485f2a9dcfe9453211efd6b0b65070f`;
the remap-cache artifact is preserved separately as
`4a45a0003441a3f9f8d3c68b0dc98193a5892420ffa7de805f2da17ff3277340`.

The source, deployed config and build artifact remain at their original paths
as well. The backup is a rollback/reference snapshot, not a source deletion or
PetCore conversion.

## Boot evidence — before cutover (MCPets enabled)

`./mc up world`, then `docker logs srv-world`:

```
[07:31:26 INFO]: [PluginInitializerManager] Initialized 23 plugins
...
[07:31:55 INFO]: [MCPets] Enabling MCPets v4.1.5-SNAPSHOT
[07:31:55 INFO]: [MCPets] : 0 pets registered successfully !
[07:31:55 INFO]: [MCPets] : 0 categories registered successfully !
[07:31:55 ERROR]: [MCPets] Could not reach SQL database. Please configure your database parameters.
[07:31:55 INFO]: [MCPets] [Database] Can't initialize MySQL.
[07:31:55 INFO]: [MCPets] [Database] Will be using YAML support instead (no worry it's not a bug).
[07:31:55 INFO]: -=-=-=-= MCPets loaded =-=-=-=-
```

Only pre-existing MythicMobs drop-type and ModelEngine hitbox warnings
observed, matching the prior `RUNTIME-001` baseline in
`docs/audits/2026-07-13/runtime-evidence-register.yaml`. No MCPets-specific
errors beyond the known SQL-unreachable line.

## Cutover action

- Moved `MCMMORPG/plugins/MCPets 4.1.6 (1.21.+).jar` to
  `MCMMORPG/plugins/_disabled/` (rollback: move it back).
- Moved `MCMMORPG/docker/servers/world/plugins/MCPets 4.1.6 (1.21.+).jar` to
  `MCMMORPG/docker/servers/world/plugins/_disabled/` (rollback: move it
  back).
- Neutralized `MCMMORPG/plugins/MCPets/config.yml` (tracked file):
  `SpawnPetOnReconnect: false`, `Mountable: false`, `DistanceTeleport: 0`, so
  a jar accidentally restored to a plugins directory still cannot
  auto-spawn, mount or teleport an entity.
- Moved the live world-backend
  `MCMMORPG/docker/servers/world/plugins/CompanionCore-0.1.0-SNAPSHOT.jar` to
  `MCMMORPG/docker/servers/world/plugins/_disabled/CompanionCore/`.
- Moved the corresponding Paper remap-cache JAR from
  `plugins/.paper-remapped/` to the matching nested `_disabled/CompanionCore/`
  path. No source, deployed configuration, build artifact, MythicMobs content
  or other companion/minion work was changed.

## Boot evidence — after authority cutover (MCPets and CompanionCore disabled)

`MCMMORPG/docker ./mc up world`, then current-boot `docker logs` / `rcon-cli`:

```
[07:53:21 INFO]: [PluginInitializerManager] Initialized 22 plugins
[07:53:58 INFO]: Done (38.704s)! For help, type "help"
```

- `./mc rcon world plugins` returned 22 plugins: `Chunky`, `CMILib`,
  `CoreTools`, `FastAsyncWorldEdit`, `Iris`, `LibsDisguises`, `LuckPerms`,
  `MEGMoLangExtension`, `MinionCore`, `ModelEngine`, `Multiverse-Core`,
  `MythicCrucible`, `MythicDungeons`, `MythicLib`, `MythicMobs`,
  `packetevents`, `PixelLibs`, `PlaceholderAPI`, `ProtocolLib`, `Vault`,
  `VoidGen`, `WorldGuard`. Neither `MCPets` nor `CompanionCore` is listed.
- A log scan bounded to this boot (`docker logs --since
  2026-07-20T07:53:15.32269055Z srv-world`) found zero
  `CompanionCore|MCPets` matches.
- `./mc rcon world companion` returned Paper's `Unknown or incomplete command`
  response, proving `/companion` is not registered. `./mc rcon world list`
  reported `0 of a max of 20 players online`.
- The world container reached Docker health `healthy`. Observed warning output
  was in existing MythicMobs drop/ModelEngine asset categories and contained no
  MCPets or CompanionCore load, enable or command-registration event.

Backend was brought back down (`./mc down world`) after evidence capture to
restore the environment to its pre-session state (only `mariadb` and
`velocity` running).

## Exact CompanionCore rollback

Run these commands from the repository root while `srv-world` is stopped:

```bash
mv MCMMORPG/docker/servers/world/plugins/_disabled/CompanionCore/CompanionCore-0.1.0-SNAPSHOT.jar \
  MCMMORPG/docker/servers/world/plugins/
mv MCMMORPG/docker/servers/world/plugins/_disabled/CompanionCore/.paper-remapped/CompanionCore-0.1.0-SNAPSHOT.jar \
  MCMMORPG/docker/servers/world/plugins/.paper-remapped/
cd MCMMORPG/docker && ./mc up world
./mc rcon world plugins
./mc rcon world companion
```

This restores the exact pre-cutover world runtime JAR and cache (the manifest
paths and SHA-256 values above provide a recovery source if the disabled copies
are lost). The source, build output and `plugins/CompanionCore/` configuration
were never moved or modified, so no data/config restore is required. Do not
run this rollback alongside a PetCore runtime; it deliberately restores the
parallel legacy authority that ADR-003 forbids.

## Registry/status validation

```
python3 scripts/check-contract-registry.py   # 16 contracts validated; 4 unresolved blocker(s)
python3 scripts/check-contract-statuses.py   # contract status vocabulary and release gate validated
node --test tests/contract-statuses.test.mjs # pass
```

## Residual/unresolved

- No in-game bot/player pass was run to positively exercise "join server,
  confirm no legacy pet/companion auto-spawns" end-to-end. The authoritative
  runtime evidence is jar absence, the 22-plugin loaded list and the absent
  `/companion` registration. A future PetCore vertical-slice ticket should add
  that scripted player-flow acceptance check.
- `plugin-src/companion-core`, `plugin-src/companion-bridge`,
  `plugin-src/minion-core` and all MythicMobs companion/minion content were
  left untouched. This amend removes only the live CompanionCore authority;
  it does not implement, configure or activate PetCore.
