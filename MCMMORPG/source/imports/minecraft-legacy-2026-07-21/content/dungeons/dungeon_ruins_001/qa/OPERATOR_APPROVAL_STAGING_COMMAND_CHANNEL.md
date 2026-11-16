# DR001 Staging Command Channel Approval

Status: RCON automation path prepared; staging restart and local password setup required
Target environment: `Server — kopia/1.21.11` staging only

## Purpose

Provide a safe approval record for the next DR001 staging runtime command while
preventing broad console access. This pass does not send commands to the server.
Visible console mode remains available for human/operator input only. Codex
automation must use the staging-only RCON allowlist path after a local password
is configured and staging is restarted.

## Allowed Commands

- `md create dungeon_ruins_001`
- `/md create dungeon_ruins_001`
- `mm reload`
- `/mm reload`
- `mm mobs info DR001_GatewardenVaros`
- `/mm mobs info DR001_GatewardenVaros`
- `mm mobs info DR001_RuinedKingAurelion`
- `/mm mobs info DR001_RuinedKingAurelion`
- `mm mobs info DR001_CisternOracleNhalruun`
- `/mm mobs info DR001_CisternOracleNhalruun`

## Forbidden Commands

Do not run spawn commands, `/stop`, `/reload`, `/minecraft:reload`, `/op`,
`/deop`, plugin-manager commands, economy commands, `/give`, arbitrary
Minecraft commands, dungeon play or flow commands, asset import commands,
production/live commands, command chains, pipes, semicolons, redirection, or
multi-line command input.

## RCON Setup

Staging `server.properties` is prepared with `enable-rcon=true` and
`rcon.port=25575`, but the actual `rcon.password` must be set locally by an
operator before staging is restarted. Do not commit or print the password.

Codex reads the same password from either:

- environment variable `STAGING_RCON_PASSWORD`; or
- ignored local file `tools/local/staging-rcon.env`.

Use `tools/local/staging-rcon.env.example` as the shape reference only.

## Dry-Run Examples

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-staging-command.ps1 -Command "md create dungeon_ruins_001"
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-staging-command-channel.ps1
```

## Actual Execution Example - Do Not Run Without Approval

Default mode is dry-run. Actual RCON send requires `-ApproveStagingCommand` and
must still pass the exact allowlist and staging guards:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-staging-command.ps1 -Command "md create dungeon_ruins_001" -ApproveStagingCommand
```

No command was sent in the RCON transport setup pass. The next command remains
pending:

```text
md create dungeon_ruins_001
```

## Required Preflight

- Confirm working directory is `C:\Codex\Minecraft`.
- Confirm target is exactly `Server — kopia/1.21.11`.
- Confirm this is staging, not production/live.
- If using console mode, treat it as human-only.
- If using automation, use only `tools/run-staging-command.ps1` with the RCON
  allowlist and explicit approval flag.
- Confirm Paper is already running for staging.
- Confirm `Server — kopia/1.21.11/logs/latest.log` exists.
- Run the dry-run command first and confirm it reports the command is
  allowlisted.
- Confirm `STAGING_RCON_PASSWORD` is configured locally and never printed.
- Confirm final asset gate is still expected to fail and is not bypassed.

## Required Post-Command Validation

After `md create dungeon_ruins_001` is run through the approved channel, inspect:

- `Server — kopia/1.21.11/logs/latest.log`
- `Server — kopia/1.21.11/plugins/MythicDungeons/maps/dungeon_ruins_001/`

Then run:

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_mythicdungeons_map_readiness.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_staging_preflight.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
```

Expected: map readiness can advance only after generated CLASSIC map files
exist. Final assets must remain `EXPECTED_FAIL` until final intake is approved.

## Rollback/Stop Procedure

If the command creates the wrong map or runtime errors appear, stop the staging
stack through the existing staging stop procedure only after approval:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\stop-staging-stack.ps1
```

Do not touch production/live. Do not delete staging map files unless a separate
rollback approval names the exact path.

## Secret Handling

Do not commit RCON passwords, tokens, or local secret files. Credentials must
come from `STAGING_RCON_PASSWORD` or ignored `tools/local/staging-rcon.env`,
must never be printed, and must never be logged.

## Why This Is Staging-Only

The target path is hardcoded to `Server — kopia/1.21.11`, and the guard refuses
paths that look like live or production. The allowlist contains only DR001
staging QA commands and no general-purpose server administration commands.
