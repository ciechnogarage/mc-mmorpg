# Compatibility Matrix

Status: staging inventory captured on 2026-05-26 from `Server — kopia/1.21.11`.

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
| HuskSync | 3.8.8-0772f09+mc.1.21.11 | observed on 1.21.11 staging | Player sync | staged-observed |
| PixelLibs | 1.1.1 | observed on 1.21.11 staging | Library dependency | staged-observed |

## Observed Evidence

- `version_history.json` reports `1.21.11-127-bd74bf6 (MC: 1.21.11)`.
- `codex-start-last.out.log` reports Paper boot on Java 21.
- `plugins/` inventory contains the full MMO/Mythic/Iris stack plus `MythicHUD`.

## Known Gaps

- Dedicated quest plugin is still not present in the observed stack.
- Anti-cheat is still not present in the observed stack.
- Auction/shop path is still unresolved beyond Vault/CMI bridge assumptions.
- Startup log review is still needed for plugin enable warnings/errors beyond presence and version detection.

## Verification Procedure

1. Keep `docs/plugin_manifest.yaml` aligned with the actual `Server — kopia/1.21.11/plugins` inventory.
2. Review `codex-start-last.out.log` for plugin warnings before any plugin update or dungeon rollout.
3. Run the staging smoke checklist before treating this stack as release-ready.

## Docs Cache

Local syntax reference: `.github/agent-knowledge/minecraft-server-docs/registry.yml`.
