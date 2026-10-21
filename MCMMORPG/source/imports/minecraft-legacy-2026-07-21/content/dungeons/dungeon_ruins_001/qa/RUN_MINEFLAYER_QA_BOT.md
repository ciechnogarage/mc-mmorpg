# DR001 Mineflayer QA Bot

Status: dry-run workflow only until a separate execution approval is granted.

This workflow is staging-only for `Server — kopia/1.21.11`. It is not
production automation and must not be used against live/prod targets.

## Purpose

RCON command delivery works technically, but MythicDungeons did not generate
CLASSIC map files when `md create dungeon_ruins_001` was sent through RCON.
The current hypothesis is that `/md create dungeon_ruins_001` needs a real
in-game player/editor context. The Mineflayer QA bot exists only to provide
that staging player context for allowlisted commands.

## Safety

- The bot defaults to dry-run and does not connect or execute commands.
- Actual command execution requires `--approve-command`.
- The only initial allowlisted command is `/md create dungeon_ruins_001`.
- The bot rejects command chaining and broad admin commands such as `/op`,
  `/deop`, `/stop`, reloads, `/give`, economy commands, MythicMobs info, and
  mob spawn commands.
- The bot writes sanitized runtime output to ignored
  `qa/mineflayer-last-result.json`.
- No Microsoft credentials are used by default. Staging currently has
  `online-mode=false`; if auth is changed or external credentials become
  required, bot execution is blocked until credentials are configured outside
  the repo.

## Target

Default target:

```powershell
node tools/minecraft/qa-bot-mineflayer.js --dry-run --command "/md create dungeon_ruins_001"
```

Future approved execution command:

```powershell
node tools/minecraft/qa-bot-mineflayer.js --host 127.0.0.1 --port 25565 --username Dungeon001QA_01 --command "/md create dungeon_ruins_001" --approve-command
```

Paper listens on `127.0.0.1:25565`. A local Velocity entrypoint may also be
available on `127.0.0.1:25566`, but this DR001 bootstrap defaults to Paper
because staging `server.properties` has `online-mode=false`, whitelist off, and
the QA usernames are OPs in staging `ops.json`.

## After Approved Execution

Run:

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_mythicdungeons_map_readiness.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_staging_preflight.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
```

Expected before execution: `CONFIG_ONLY_NOT_LOAD_READY`.
Expected final asset state: `EXPECTED_FAIL` until final intake is approved.
