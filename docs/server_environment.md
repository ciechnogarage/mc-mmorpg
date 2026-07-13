# Server Environment

Status: staging verified from local server copy and observed startup logs.

## Target

| Field | Value |
| --- | --- |
| Minecraft version | 1.21.11 |
| Runtime | Paper 1.21.11-127-main@bd74bf6 |
| Java | Temurin/OpenJDK 21.0.11+10 LTS |

## Environments

| Environment | Purpose | Notes |
| --- | --- | --- |
| DEV | Fast config experiments | May reset. |
| STAGING | Compatibility and player tests | `MCMMORPG`; required before production changes. |
| PRODUCTION | Live players | Backup, rollback, and approval are required for R3/R4. |

## Server Folders In Repo

| Folder | Role |
| --- | --- |
| `MCMMORPG` | Local staging copy with Paper runtime and plugin inventory. |

## Evidence Used

1. `MCMMORPG/version_history.json`
2. `MCMMORPG/plugins/`
3. `MCMMORPG/logs/latest.log`

## Remaining Verification Steps

1. Capture exact startup version lines for ProtocolLib and PixelLibs when they matter for an upcoming update.
2. Review staging boot warnings before the next player-facing QA session; latest checked summary reported HuskSync/MCPets SQL errors.
3. Keep this file and `docs/plugin_manifest.yaml` in sync with the staging copy after plugin changes.
