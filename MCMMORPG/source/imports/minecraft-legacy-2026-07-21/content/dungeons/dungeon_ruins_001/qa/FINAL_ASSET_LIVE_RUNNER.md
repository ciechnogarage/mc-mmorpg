# DR001 Final Asset Live Runner

Status: waiting-for-live-approval
Approval: needs-approval before staging runtime commands
Dungeon: `dungeon_ruins_001` / `Ruiny Złamanej Korony`

## Non-Runtime Checks

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_references.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1
```

Expected before final assets: reference validator passes, final asset validator
fails, import helper is dry-run only.

## Approved Staging Commands

Only after explicit operator approval:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -UseFinalIntake -Apply
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-staging-qa.ps1 -HeapBudget 2G -StopStrayFirst
```

Manual MythicDungeons commands after editor wiring:

```text
/md reload dungeon_ruins_001
/md play dungeon_ruins_001
```

Command syntax source: local MythicDungeons docs mirror
`.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/commands-and-permissions.md`.

Stop staging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\stop-staging-stack.ps1
```

## Required Log Markers

- `Done (`
- Nexo resource-pack generation completed.
- ModelEngine imports `dr001_*.bbmodel`.
- MythicDungeons reloads `dungeon_ruins_001`.
- No unresolved DR001 cross-reference warnings from the source validators.

## Forbidden Markers

- Missing `dr001_` model, texture, sound, or hitbox warnings.
- Missing MythicDungeons map/config for `dungeon_ruins_001`.
- Missing `DR001_` mob, skill, drop table, or MMOItems IDs.
- `NoClassDefFoundError`, `ClassNotFoundException`, or plugin disable.

## Observer Roles

- QA Lead: run player scripts and timestamp findings.
- Ops/Admin: log watch, restart/rollback triggers, stop command ownership.
- Combat: boss phases, telegraphs, add pressure.
- Economy: loot once-only, weekly reward, currency rates.
- World/Iris/Nexo/ModelEngine: room visuals, resource pack, hitboxes, sounds.

Next state remains `waiting-for-live-approval` until an operator approves the
runtime command list.
