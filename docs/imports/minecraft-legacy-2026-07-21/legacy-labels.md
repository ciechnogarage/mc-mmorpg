# Legacy Labels — Minecraft docs import 2026-07-21

This file classifies old operational material imported from `/home/przemek/projects/Minecraft`.

Current standard:

- Active root: `/home/przemek/projects/MC`
- Write work: requires `dev-<scope>` lease
- Staging: integration/promotion only
- Legacy paths/commands from Windows, Cursor meta, `Server — kopia`, `.ps1`, playtest-bot, usercache, old staging stacks: not active operating guidance

## Status meanings

| Status | Meaning |
| --- | --- |
| `legacy-only` | Keep for historical/recovery context only. Do not execute as current procedure. |
| `archive-reference` | Useful as audit/reference material, not source of truth. |
| `extract-only` | May contain useful ideas, but only small reviewed snippets should be promoted. |
| `do-not-promote` | Must not be copied into active docs as-is. |

## Labels

| Path/group | Status | Reason | Allowed use |
| --- | --- | --- | --- |
| `raw/root/AGENTS.md` | `legacy-only`, `do-not-promote` | Old Windows/Cursor/Codex dispatch model; references `C:\Codex`, `invoke-codex-team.ps1`, `Server/`, `Server — kopia`, legacy agent knowledge paths. | Historical audit only; extract no active procedure without rewrite to current lease standard. |
| `raw/docs/AGENTS.md` | `archive-reference` | Docs rules are mostly generic, but inherit old `C:\Codex` baseline. | Compare against current docs rules only. |
| `raw/docs/plugin_manifest.yaml` | `legacy-only`, `archive-reference`, `do-not-promote` | Describes old `Server — kopia/1.21.11` staging inventory. | Historical plugin inventory reference; do not use as current stack truth. |
| `raw/docs/compatibility_matrix.md` | `legacy-only`, `archive-reference`, `do-not-promote` | Same old staging/server-copy assumptions as plugin manifest. | Historical compatibility reference only. |
| `raw/docs/server_environment.md` | `legacy-only`, `do-not-promote` | Collides with current server/environment docs and may encode old staging assumptions. | Manual diff only; keep current docs as authority unless reviewed. |
| `raw/tasks/*` | `legacy-only`, `archive-reference`, `do-not-promote` | Old board/backlog/orchestrator state; contains obsolete Cursor/Codex workflow state. | Audit history only. |
| `raw/tasks/orchestrator-runs/*` | `legacy-only`, `archive-reference`, `do-not-promote` | Historical run artifacts, not current task source. | Audit history only. |
| `raw/decisions/decision_log.md` | `archive-reference`, `extract-only` | May preserve old decisions, but includes decisions about obsolete tooling and legacy deletion. | Extract only still-valid decisions after review. |
| `raw/design/core_loop.md` | `extract-only` | Useful product loop ideas with minimal operational risk. | Extract curated current product summary. |
| `raw/design/economy_model.md` | `extract-only` | Useful economy/exploit checklist, but plugin assumptions need current validation. | Extract curated economy notes after validation. |
| `raw/design/progression_model.md` | `extract-only` | Useful progression acceptance notes, but staging wording is stale. | Extract curated progression notes after cleanup. |
| `raw/design/levels/dungeon_001/*` | `legacy-only`, `extract-only` | Useful dungeon lessons mixed with old staging, Windows `.ps1`, `Server — kopia`, playtest-bot, usercache references. | Extract lessons only; do not execute commands. |
| `raw/wiki/*` | `archive-reference` | Imported wiki/cache material; usefulness not yet reviewed. | Reference only until separate usefulness pass. |

## Explicit non-authority

These imported patterns are not active instructions:

- `C:\Codex` paths
- Cursor meta implementation/dispatch rules
- `invoke-codex-team.ps1`
- `start-staging-qa.ps1`
- `start-optimized.ps1`
- `tools/playtest-bot`
- `Server — kopia/1.21.11`
- direct `usercache`/`ops` manipulation
- old staging readiness claims

Any future promotion must rewrite content to the current `/home/przemek/projects/MC` + `dev-<scope>` lease standard.
