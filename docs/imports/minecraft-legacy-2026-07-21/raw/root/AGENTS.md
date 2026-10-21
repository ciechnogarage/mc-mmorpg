# Minecraft Codex Guide

## Workspace Standard Inheritance

This repo inherits the workspace AI baseline from `C:\Codex\AGENTS.md` and
`C:\Codex\STANDARD.md`. The rules below are repo-specific additions for domain
workflow, routing, validation, and source-of-truth documents.

## Workspace Orchestration

This repo follows the current workspace `single_account` standard.

| Tier | Use in this repo |
|------|------------------|
| Cursor meta | Plan, create work packets, update `tasks/team-board.md`, and review run artifacts. No feature implementation in Cursor meta. |
| Active Codex | `primary` / `~/.codex`; normal implementation and QA dispatch through `invoke-codex-team.ps1`. |
| Legacy fallback | `secondary` / `C:\Codex\.codex-homes\codex2`; manual fallback only, not the default hook path. |

Forward work with:

```powershell
.\tools\invoke-codex-team.ps1 -WorkPacket tasks\codex-team-packets\WP-....yaml
C:\Codex\ops\codex-team\invoke-codex-team.ps1 -ProjectRoot C:\Codex\Minecraft -WorkPacket tasks\codex-team-packets\WP-....yaml
```

**Rule 1:** feature implementation goes through invoke. Cursor meta plans and
reviews; it does not report implementation success from `cursor_meta_implementation`.
See [`ops/codex-team/dispatch-contract.md`](ops/codex-team/dispatch-contract.md)
and `C:\Codex\ops\codex-team\ORCHESTRATION.md`.

## Standard Stack

| Layer | Master doc |
|-------|------------|
| 0-1 | `C:\Codex\AGENTS.md` / `C:\Codex\STANDARD.md`, plus `../../LangChain/docs/wiki/cross-project-standards.md` |
| 2 Operating model A - MCMMORPG | This file, `docs/ai/standard-map.md`, `.agents/skills/`, `ops/org/roles_raci.yaml` |
| 3 Product | `.github/agent-knowledge/mcmmorpg-orchestrator/SPEC.md`, `.github/agent-knowledge/server-delivery-pipeline/` |

Do not import operating rules from NigaiTale, Hude, or Nigaigames; they use a
different model.

## Repo Layout

- Agent instructions: `AGENTS.md`
- Repo standard map: `docs/ai/standard-map.md`
- Repo-local skills: `.agents/skills/`
- Local docs cache: `.github/agent-knowledge/minecraft-server-docs/`
- Mirror tools: `.github/agent-knowledge/minecraft-server-docs/_tools/`
- Server files: `Server/` or server copy folders such as `Server — kopia/`
- Playbooks: `MajnKarto/.github/agent-knowledge/agent-playbooks/`
- MCMMORPG orchestrator spec: `.github/agent-knowledge/mcmmorpg-orchestrator/`
- Project docs and ops: `docs/`, `design/`, `tasks/`, `qa/`, `builds/`, `decisions/`, `ops/`

## Work Routing

Use the smallest workflow that matches the request:

1. Repo orientation and shared safety rules: this `AGENTS.md`.
2. Docs maintenance: use `minecraft-docs-mirror`.
3. Plugin admin/config work: use `minecraft-plugin-admin`.
4. Sprint/release/product loop: use `minecraft-server-delivery`.
5. MCMMORPG orchestration: use `minecraft-mcmmorpg-orchestrator` when the request
   spans multiple plugins, defines a new RPG feature, plans a release or plugin
   update, changes live permissions or economy, or needs explicit workflow/risk
   classification before config work. Then route execution through
   `minecraft-plugin-admin` and/or `minecraft-server-delivery` as appropriate.

Docs maintenance includes adding, refreshing, validating, or repairing local
documentation mirrors, mirror scripts, mirror metadata, `mirror-status.yml`,
`FETCH_STATUS.md`, broken docs links, or `*.error.md` cleanup.

Plugin admin/config work includes answering or preparing changes for plugin
commands, permissions, placeholders, YAML keys, generated config behavior,
server config, reload or restart guidance, validation steps, and rollback notes.

Sprint/release/product loop work includes turning ideas, bug reports, balancing
requests, player feedback, and operational findings into sprint backlog items,
delivery gates, test plans, player QA scripts, release notes, retro notes, and
next-sprint improvement proposals. It routes implementation details back through
`minecraft-plugin-admin` and documentation gaps through `minecraft-docs-mirror`.
For larger, high-risk, player-facing, economy, worldgen, dungeon, progression,
or release work, `minecraft-server-delivery` is mandatory and must run with
collaborating roles, a shared board, handoffs, blockers, live review loop, QA
gate, and retro.

Keep docs maintenance separate from plugin development unless the user explicitly
asks for development work.

## Task Contract

For larger, unclear, or high-risk tasks, establish these before editing:

- Goal: what should change.
- Context: which files, docs, configs, logs, or examples matter.
- Constraints: syntax sources, safety rules, scope limits, and user preferences.
- Done when: validation steps, expected behavior, and review checks.

For larger/high-risk/player-facing work, also establish the role collaboration
contract before calling the task done:

- relevant roles and owners, even if one person or Codex performs several roles;
- role report for checked work, sources, blockers, handoff, feedback, evidence,
  and lesson learned;
- blocker owner, exit condition, and accepting role;
- QA/Ops release gate with logs, rollback, and player-test evidence;
- live review loop with preview/live evidence, real-time role observations,
  proposed fixes, owner decisions, and retest results;
- live runner authorization when runtime evidence is required: exact command
  list, approval status, log watch, assisted/manual command owner, and next
  state;
- retro item feeding the next sprint.

For admin/config changes, always name the source used for exact syntax and the
validation path the user should run. For long or ambiguous work, prefer a plan
before changing files.

## Core Constraints

- Do not guess exact plugin syntax.
- Exact commands, permissions, placeholders, YAML keys, IDs, and config values must come from local docs, generated config, local plugin files, or verified official sources.
- Broken local links or missing mirrored pages are docs-maintenance issues, not permission to invent placeholders.
- Do not broaden docs scope to unrelated plugins, downloads, or asset packs unless the user explicitly asks.
- Preserve existing user edits and unrelated config.
- Ask before destructive file operations or live server actions.
- Do not run live server commands unless the user explicitly asks and the environment supports it.
- Do not mark larger/high-risk/player-facing work done if required role board,
  handoffs, live review evidence, gate review, or retro evidence is missing.
- Do not leave `blocked-live-runtime` as a final state. Convert it to
  `waiting-for-live-approval`, `live-running`, `blocked` with evidence, or a
  completed live verdict.

## Plugin Admin Lookup Order

For admin/config work, use this order:

1. Read `.github/agent-knowledge/minecraft-server-docs/README.md`.
2. Pick the right source from `.github/agent-knowledge/minecraft-server-docs/registry.yml`.
3. Read `<source>/GOLDEN_RULES.md`.
4. Open `<source>/wiki/INDEX.md`.
5. Search `<source>/wiki/md/` with `rg`.
6. Check `<source>/generated-config/` when installed behavior matters.
7. Check `<source>/local-notes/` for curated server-specific notes.
8. Browse official docs only when local sources are missing, stale, or ambiguous.

Source selection defaults:

- Plugin commands/config/permissions/placeholders/integrations: matching plugin folder.
- Vanilla IDs, NBT, selectors, datapacks, resource packs: `minecraft-wiki/`.
- Paper behavior/config: `paper/`.
- Purpur behavior/config: `purpur/`.
- Velocity proxy/forwarding: `velocity/`.
- Bukkit or Spigot enums, materials, events, API names: `bukkit-api` or `spigot-api`.

## Plugin Admin Completion

For plugin admin/config work, finish with:

- affected plugin or server layer;
- files changed or inspected;
- exact local or verified official source used;
- reload command or restart requirement;
- logs to inspect;
- rollback note when relevant;
- docs-cache gaps that should be handled later with `minecraft-docs-mirror`.

## Docs Maintenance Completion

For docs maintenance, follow `minecraft-docs-mirror` and the docs-cache
`AGENTS.md`. Do not call docs work complete unless:

- mirrored content stays in `wiki/md/` and `wiki/html/`;
- curated notes stay in `local-notes/`;
- `mirror-status.yml` and `FETCH_STATUS.md` are current when status changed;
- local link normalization has been run after mirror content changes;
- `validate-docs.ps1` reports `Errors: 0` and `Warnings: 0`;
- no `*.error.md` files remain.

Standard validation commands from repo root:

```powershell
.\.github\agent-knowledge\minecraft-server-docs\_tools\normalize-local-links.ps1
.\.github\agent-knowledge\minecraft-server-docs\_tools\validate-docs.ps1
.\.github\agent-knowledge\minecraft-server-docs\_tools\update-fetch-status.ps1
Get-ChildItem .github\agent-knowledge\minecraft-server-docs -Recurse -Filter *.error.md
```

Expected final state:

- Local wiki link scan reports `BAD_COUNT=0` when the normalizer reports it.
- `validate-docs.ps1` reports `Errors: 0` and `Warnings: 0`.
- The recursive `*.error.md` scan returns `0`.

## Docs Cache Role

The local docs cache is a static, local knowledge base for Codex:

- deterministic lookup from versioned files;
- fast search with `rg`;
- source navigation through `README.md`, `registry.yml`, `wiki/INDEX.md`, and `wiki/MENU.md`;
- no live fetch dependency for common admin tasks.

It is not an MCP server. Treat it as a repo-local source of truth, and browse
official docs only when the local cache is missing, stale, or ambiguous. Do not
add MCP servers, skills, or automations unless they remove a real repeated
manual loop.

## Learned Workspace Facts

- Global orchestration uses `C:\Codex\ops\codex-team\global-lane-policy.yaml` with `dispatch_strategy: single_account`, `active_login_id: primary`, and `forbid_ack_only: true`.
- Project repos keep `ops/codex-team/global-lane-policy.pointer.yaml`; do not fork the global policy locally.
- Cursor hooks and state live on the hub: `C:\Codex\.cursor\hooks\run-hook.cmd` and `C:\Codex\.cursor\hooks\state\orchestration.json`.
- After hook or rule changes, run `C:\Codex\ops\codex-team\sync-cursor-orchestration-rules.ps1` and restart Cursor.
- Normal implementation in this repo dispatches through `tools/invoke-codex-team.ps1` on `primary`; `secondary` is legacy/manual fallback only.
- ACK-only success is blocked. A satisfying implementation run must have a real artifact under `tasks/codex-team-runs/` with `dispatch_source: langchain_codex.invoke` and a concrete deliverable.
- `langgraph_minecraft` is available for explicit MCMMORPG graph work; do not run it if Cursor meta already produced the full plan for the same task.
- Domain truth lives in `AGENTS.md`, `.github/agent-knowledge/mcmmorpg-orchestrator/SPEC.md`, and `.github/agent-knowledge/minecraft-server-docs/`.
- Work packets live in `tasks/codex-team-packets/`; role routing lives in `ops/codex-team/registry.yaml` and `ops/org/roles_raci.yaml`.
- Dungeon/Iris/MythicDungeons work uses `dungeon_content_pipeline` plus `MajnKarto/.../agent-playbooks/`; dungeon IDs use `lowercase_underscore`.
- Staging QA targets `Server — kopia/1.21.11` unless a task explicitly names another environment; live server commands still require explicit user approval.
- Dungeon QA should compare player-visible screenshots against `design/levels/<dungeon_id>/`, not only run JSON.
- The canonical first production dungeon is `dungeon_ruins_01`; `loch` is a historical prototype and `dungeon_001` is a legacy vertical slice. Release/runtime evidence for the first dungeon must use `dungeon_ruins_01` and its `LIVE_RUNNER.md`.
