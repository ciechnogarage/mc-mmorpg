# DR001 Staging Console Mode

Status: human-only fallback after separate approval
Target environment: `Server — kopia/1.21.11` staging only

## Purpose

Use console mode only when a DR001 staging command must be entered into the
Paper console by an approved human operator. This launcher starts Paper in the
foreground with normal console input available. It does not send any Minecraft
command, and Codex should not rely on it for automated command execution.

Use the background QA launcher when no console input is needed. Use the RCON
allowlist channel for Codex automation after staging RCON password setup and
restart.

## Validation Only

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-staging-console.ps1 -ValidateOnly
```

Expected result: `STAGING_CONSOLE_VALIDATION_OK`.

## Start Console Mode

Only after operator approval to start a visible staging console:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-staging-console.ps1 -HeapBudget 2G -StopStrayFirst
```

The script confirms the project root, exact staging path, Java, and `paper.jar`.
It refuses production/live-looking paths and refuses to start if Paper is
already listening on `25565`.

## Pending Command

No command was sent in the RCON transport setup pass. The next command remains
pending:

```text
md create dungeon_ruins_001
```

Do not run it until a separate approval explicitly names that exact command.

## Guard Rails

- Staging only: `Server — kopia/1.21.11`.
- No production/live action.
- No automatic command sending through visible console mode.
- RCON automation is separate and must use `tools/run-staging-command.ps1`.
- No asset import, resource-pack publishing, mob spawning, dungeon flow, or
  reload commands in this pass.
- Final asset gate remains expected fail.
- Full runtime smoke remains blocked until map creation succeeds.

## Evidence Source

MythicDungeons command syntax is sourced from the local docs mirror:

- `.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/commands-and-permissions.md`
- `.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/your-first-dungeon.md`
