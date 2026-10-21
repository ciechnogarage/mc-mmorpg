# DR001 Real Client Visual Review

Date: 2026-05-30
Target: `Server - kopia/1.21.11` staging only via Velocity `127.0.0.1:25566`
Decision: `PRISM_CLIENT_BLOCKED_NO_INSTANCE`
Verdict: `REAL_CLIENT_VISUAL_REVIEW_BLOCKED_NO_INSTANCE`

## Summary

Prism Launcher was found and used from `C:\Codex\PrismLauncher-Windows-MinGW-w64-Portable-11.0.2\prismlauncher.exe`.
The local Prism account/session refreshed successfully and launched the only local instance, `26.1.2`, as a real Minecraft client. The client was driven through Multiplayer and Direct Connection to `127.0.0.1:25566`.

The staging server rejected the connection before resource-pack acceptance or in-world review:

`Unable to connect to loch: Outdated server! I'm still on 1.21.11`

No compatible local Prism instance or cached `1.21.11` client was present under the Prism tree, so boss, Nexo, schematic, and sound visual review could not start.

## Prism / Client

| Item | Result |
| --- | --- |
| Prism path | `C:\Codex\PrismLauncher-Windows-MinGW-w64-Portable-11.0.2\prismlauncher.exe` |
| Instance used | `26.1.2` |
| Compatible `1.21.11` instance | Not present |
| Connected server | Attempted `127.0.0.1:25566` through Velocity |
| Resource pack accepted | No; blocked before join/resource-pack prompt |
| Screenshot folder | `C:\Codex\PrismLauncher-Windows-MinGW-w64-Portable-11.0.2\instances\26.1.2\minecraft\screenshots` |
| Evidence folder | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/` |

## Evidence Screenshots

| Evidence | Screenshot path | Result | Notes |
| --- | --- | ---: | --- |
| Prism-launched Minecraft main menu | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/screen_discovery.png` | PASS | Real `Minecraft 26.1.2` window visible. |
| Multiplayer warning / server list path | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/screen_after_scaled_multiplayer.png` | PASS | Real client entered Multiplayer flow. |
| Server list | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/screen_server_list.png` | PASS | Real client server-list UI. |
| Velocity direct-connect rejection | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/screen_after_direct_connect.png` | BLOCKED | Shows exact incompatible-version rejection. |

## Runtime Evidence

| Source | Evidence |
| --- | --- |
| Client log | `Connecting to 127.0.0.1, 25566` followed by `Unable to connect to loch: Outdated server! I'm still on 1.21.11`. |
| Prism local instance scan | Only `instances\26.1.2`, `meta\net.minecraft\26.1.2.json`, and `libraries\com\mojang\minecraft\26.1.2` were present. |
| Staging network state | Paper/RCON listened on `25565/25575`; Velocity listened on `127.0.0.1:25566`. |

## Category Results

| Category | Result | Notes |
| --- | ---: | --- |
| Boss review | BLOCKED_NO_INSTANCE | No compatible client joined staging, so DR001 bosses were not viewed. |
| Nexo review | BLOCKED_NO_INSTANCE | Resource pack was not offered or accepted because the client could not join. |
| Schematic review | BLOCKED_NO_INSTANCE | No in-world client scene was available. |
| Sound review | BLOCKED_NO_INSTANCE | No real client target was online after the version rejection. |

## Cleanup Status

- No staging world edits were made in this real-client pass.
- No bosses were spawned in this real-client pass.
- No Nexo items/furniture were placed in this real-client pass.
- No schematic was pasted in this real-client pass.
- Temporary Prism `JoinServerOnLaunch` settings were not left enabled; `instance.cfg` now has `JoinServerOnLaunch=false` and empty `JoinServerOnLaunchAddress`.
- Prism/Minecraft client processes were stopped after evidence capture.

## Decision

`PRISM_CLIENT_BLOCKED_NO_INSTANCE`

Prism and authentication worked, but the only local Minecraft instance was incompatible with staging. Because no local compatible `1.21.11` Prism instance or cached `1.21.11` client existed, this pass cannot honestly produce DR001 in-game asset screenshots without creating/downloading a compatible client instance.

## Next Approval Request

Approve creating a Prism staging QA instance compatible with `Server - kopia/1.21.11`, using official Minecraft client assets only, then rerun the real-client DR001 visual screenshot pass.
