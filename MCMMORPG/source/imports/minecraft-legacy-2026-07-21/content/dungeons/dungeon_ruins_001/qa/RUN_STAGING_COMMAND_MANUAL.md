# Manual DR001 Staging Command Runbook

Status: human-only fallback; RCON is the approved automation path after setup
Target environment: `Server — kopia/1.21.11` staging only

## Why Manual

The background staging launchers start Paper hidden or backgrounded without
retaining a safe stdin handle. Visible console mode exists, but Codex cannot
reliably type into or read that console. It is human/operator-only.

RCON is now the approved automation path if staging is configured with a local
password and restarted. No command was sent while preparing this path.

For future approved human operator command work, use the visible console
launcher:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-staging-console.ps1 -HeapBudget 2G -StopStrayFirst
```

That launcher still does not send commands automatically.

## Next Approved Command

After explicit operator approval, the next pending command is:

```text
md create dungeon_ruins_001
```

Do not run `/mm reload`, `/md reload`, spawn commands, dungeon flow commands,
asset import, resource-pack publishing, or production/live commands in the same
approval.

## Preflight

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-staging-command.ps1 -Command "md create dungeon_ruins_001"
```

Proceed only if the dry-run confirms the command is allowlisted and the
operator has explicit approval for this exact command. Actual automation uses:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-staging-command.ps1 -Command "md create dungeon_ruins_001" -ApproveStagingCommand
```

That apply form requires staging RCON to be restarted with a local
`rcon.password` and the same value supplied through `STAGING_RCON_PASSWORD` or
ignored `tools/local/staging-rcon.env`.

## Validation After Execution

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_mythicdungeons_map_readiness.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_staging_preflight.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
```

Review `Server — kopia/1.21.11/logs/latest.log` for MythicDungeons errors.

## Rollback Guidance

If map creation fails or creates unintended state, preserve logs and request a
separate rollback approval naming the exact staging path. Do not remove map
files, stop production/live, import assets, or start dungeon flow as part of
this approval.
