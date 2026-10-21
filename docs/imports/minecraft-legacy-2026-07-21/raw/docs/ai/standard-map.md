# Minecraft Repo Standard Map

This file maps `C:\Codex\STANDARD.md` to the Minecraft repo. It is the quick
operational checklist for agents and human reviewers.

## Classification And Routing

| Standard tier | Minecraft mapping | Required route |
| --- | --- | --- |
| T0 read-only | status, explanation, source lookup, log summary | Cursor/meta answer with file evidence. |
| T1 low-risk edit | small docs/task cleanup | Direct edit is allowed; run the smallest relevant check. |
| T2 normal work | repo docs, staging config, dungeon/content files | Use task contract and route large implementation through `invoke-codex-team.ps1`. |
| T3 high-risk | permissions, economy, production config, plugin updates, security/data | Plan first, record rollback, require human approval before live action. |
| T4 orchestrated | LangGraph, multi-role delivery, live QA, release | Use `minecraft-server-delivery`, board/handoffs/gate/retro, and explicit approval gates. |

## Source Of Truth

- AI/project rules: `AGENTS.md`, `.cursor/rules/`, `.agents/skills/`.
- Product/orchestration: `.github/agent-knowledge/mcmmorpg-orchestrator/SPEC.md`.
- Plugin syntax: `.github/agent-knowledge/minecraft-server-docs/registry.yml` and source folders.
- Plugin inventory: `docs/plugin_manifest.yaml`, `docs/compatibility_matrix.md`, `docs/server_environment.md`.
- Decisions and rollback: `decisions/decision_log.md`, `builds/rollback_plans.md`.
- Work tracking: `tasks/team-board.md`, `tasks/codex-team-packets/`, `tasks/codex-team-runs/`.

## Verification Commands

Run the smallest relevant set:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\Codex\ops\codex-team\verify-ssot.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File C:\Codex\ops\codex-team\sync-cursor-orchestration-rules.ps1 -VerifyOnly
powershell -NoProfile -ExecutionPolicy Bypass -File C:\Codex\ops\codex-team\validate-codex-runs.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.github\agent-knowledge\minecraft-server-docs\_tools\validate-docs.ps1
Get-ChildItem .github\agent-knowledge\minecraft-server-docs -Recurse -Filter *.error.md
git -c safe.directory=C:/Codex/Minecraft diff --check -- . ":(exclude).github/agent-knowledge/minecraft-server-docs"
```

Docs mirror work additionally runs:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.github\agent-knowledge\minecraft-server-docs\_tools\normalize-local-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.github\agent-knowledge\minecraft-server-docs\_tools\update-fetch-status.ps1
```

## Security, Data, And Production Boundaries

- Do not commit secrets, tokens, player data, logs with private data, or local auth files.
- Do not run live server commands without explicit user approval.
- Do not treat `Server — kopia/1.21.11` as production; it is staging/local evidence.
- R3/R4 changes require approval, rollback steps, log checks, and QA/Ops gate evidence.
- Plugin syntax must come from local docs cache, generated config, installed config, or verified official docs.

## Human Review Gates

Human review is required before:

- production deploys or restarts;
- permission, staff/admin, economy, or player-data changes;
- plugin updates in production;
- destructive filesystem/database operations;
- publishing release notes that imply live readiness.

## Definition Of Done

A repo task is done when:

- the task contract is satisfied;
- sources and assumptions are named;
- relevant deterministic checks passed or blocked reasons are recorded;
- changed files are reviewable and scoped;
- `tasks/team-board.md` reflects the current audit verdict for dispatched work;
- rollback and player QA notes exist for player-facing or high-risk work.
