# Compatibility Matrix

Status: staging inventory refreshed on 2026-06-24 from `MCMMORPG`.

## Server Target

| Field | Status |
| --- | --- |
| Minecraft | 1.21.11 |
| Runtime | Paper 1.21.11-127-main@bd74bf6 |
| Java | Temurin/OpenJDK 21.0.11+10 LTS |

## Core Stack Checks

| Plugin | Version | MC compat | Notes | Status |
| --- | --- | --- | --- | --- |
| MythicLib | 1.7.1-20260330.110510-95 | observed on 1.21.11 staging | Required by MMOCore/MMOItems | staged-observed |
| MMOCore | 1.13.1-20260330.110342-58 | observed on 1.21.11 staging | Classes/progression | staged-observed |
| MMOItems | 6.10.1-20260330.094756-58 | observed on 1.21.11 staging | Items | staged-observed |
| MythicMobs | 5.11.2 | observed on 1.21.11 staging | Mobs/bosses | staged-observed |
| MythicDungeons | 2.0.1-SNAPSHOT | observed on 1.21.11 staging | Dungeons | staged-observed |
| MythicHUD | 1.3.4-SNAPSHOT-all | observed on 1.21.11 staging | HUD/UI support plugin present in stack | staged-observed |
| LuckPerms | 5.5.36 | observed on 1.21.11 staging | Permissions | staged-observed |
| ProtocolLib | 5.4.1-SNAPSHOT-f606cc9 | observed on 1.21.11 staging | Packet deps | staged-observed |
| PacketEvents | 2.11.2 | observed on 1.21.11 staging | Packet deps | staged-observed |
| ModelEngine | 4.0.9 | observed on 1.21.11 staging | Models + performance | staged-observed |
| HuskSync | 3.8.8-0772f09+mc.1.21.11 | observed on 1.21.11 staging | Player sync | disabled for staging MVP until DB baseline exists |
| PixelLibs | 1.1.1 | observed on 1.21.11 staging | Library dependency | staged-observed |

## Observed Evidence

- `MCMMORPG/version_history.json` reports `1.21.11-127-bd74bf6 (MC: 1.21.11)`.
- `MCMMORPG/plugins/` inventory contains the full MMO/Mythic/Iris stack plus `MythicHUD`.
- `MCMMORPG/logs/latest.log` exists and must be reviewed for current startup warnings/errors before release.

## Known Gaps

- Dedicated quest plugin is still not present in the observed stack.
- Anti-cheat is still not present in the observed stack.
- Auction/shop path is still unresolved beyond Vault/CMI bridge assumptions.
- Latest checked startup log summary reported HuskSync/MCPets SQL errors and a large warning count; re-check current logs before release decisions.
- HuskSync has been moved out of active plugins for the staging MVP; MCPets YAML fallback is accepted only for this staging slice.
- CMI economy is enabled as the current staging Vault economy provider.
- `online-mode=false` is the requested staging baseline for local MVP work.

## Verification Procedure

1. Keep `docs/plugin_manifest.yaml` aligned with the actual `MCMMORPG/plugins` inventory.
2. Review `MCMMORPG/logs/latest.log` for plugin warnings before any plugin update or dungeon rollout.
3. Run the staging smoke checklist before treating this stack as release-ready.

## Docs Cache

Local syntax reference cache was not present at `.github/agent-knowledge/minecraft-server-docs/registry.yml` during the 2026-06-24 pass. Use installed configs first, then Context7 when available, then official plugin docs.
