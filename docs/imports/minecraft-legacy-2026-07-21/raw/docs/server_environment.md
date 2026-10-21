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
| STAGING | Compatibility and player tests | `Server — kopia/1.21.11`; required before production changes. |
| PRODUCTION | Live players | Backup, rollback, and approval are required for R3/R4. |

## Server Folders In Repo

| Folder | Role |
| --- | --- |
| `Server — kopia/1.21.11` | Local staging copy with Paper runtime and plugin inventory. |
| `Server` | Present in repo, but no plugin inventory was detected during the latest pass. |

## Evidence Used

1. `Server — kopia/1.21.11/version_history.json`
2. `Server — kopia/1.21.11/codex-start-last.out.log`
3. `Server — kopia/1.21.11/plugins/`
4. `Server — kopia/1.21.11/logs/latest.log`

## Remaining Verification Steps

1. Capture exact startup version lines for ProtocolLib and PixelLibs when they matter for an upcoming update.
2. Review staging boot warnings before the next player-facing QA session.
3. Keep this file and `docs/plugin_manifest.yaml` in sync with the staging copy after plugin changes.
