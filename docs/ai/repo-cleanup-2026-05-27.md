# Repo Cleanup Report - 2026-05-27

## Scope

Safe repo-standard cleanup only. No live server commands, no destructive file
operations, and no deletion of historical dungeon or run artifacts.

## Decisions Applied

- `dungeon_ruins_01` remains the canonical first production dungeon.
- `loch` is retained as a historical prototype.
- `dungeon_001` is retained as legacy vertical-slice evidence.
- `dungeon_1` remains a wrong-engine artifact and cannot be QA evidence.

## Files Cleaned

- `AGENTS.md`: added the canonical dungeon rule.
- `ops/tools/registry.yaml`: added `live_runner_dungeon_ruins_01` and renamed
  legacy loch/dungeon_001 tools.
- `qa/smoke_tests.md`: moved release QA language to the `dungeon_ruins_01`
  live runner.
- `tools/playtest-bot/README.md`: marked the bot as a legacy harness.
- `tasks/codex-team-packets/WP-mc-graph-smoke.yaml`: removed stale legacy-dungeon smoke
  wording.
- `tools/start-staging-stack.ps1` and `tools/start-staging-qa.ps1`: background
  helpers now use hidden windows.
- `tools/stop-staging-stack.ps1`: generalized the local Velocity wording.
- `ops/README.md`: clarified canonical runtime QA routing.
- `tasks/backlog.md`: added cleanup follow-ups.
- `decisions/decision_log.md`: added `DEC-005`.
- `.github/agent-knowledge/server-delivery-pipeline/_tools/new-sprint-cycle.ps1`:
  excluded vendor/runtime folders and generated delivery artifacts from marker
  scans.
- `.github/agent-knowledge/server-delivery-pipeline/sprints/sprint-2026-05-17-expert-gates/00-intake.md`:
  replaced stale `node_modules` marker noise with a cleanup note.
- `.github/agent-knowledge/server-delivery-pipeline/sprints/sprint-2026-05-16-bootstrap/00-intake.md`:
  replaced self-referential marker wording with neutral scoped wording.

## Not Cleaned

- Historical run JSON files under `tasks/codex-team-runs/` are preserved as
  audit evidence.
- Legacy `design/levels/dungeon_001/` and staging map files are preserved until
  `STD-003` decides archive vs retained evidence.
- Runtime files under ignored local folders such as `.tools/`, `Server — kopia/`,
  and `tools/playtest-bot/reports/` were inventoried only, not deleted.
- Docs mirror cache churn is outside this cleanup pass.

## Validation

- `verify-ssot.ps1`: OK.
- `sync-cursor-orchestration-rules.ps1 -VerifyOnly`: OK.
- `validate-codex-runs.ps1`: OK with pre-existing warnings.
- `validate-docs.ps1`: `Errors: 0`, `Warnings: 0`.
- `*.error.md` scan: 0.
- `git diff --check` scoped to cleanup files: OK.

## Standard Sweep Run

Run request: "Uruchom sprzątanie repo na bazie standardu".

Actions run:

- Searched for repo cleanup and validation tools.
- Checked full git status before changing anything.
- Scanned active repo files for standard signals: work markers, transitional
  live-runtime states, stale staging path spellings, visible helper windows,
  stale legacy-dungeon smoke wording, and forbidden Cursor-meta implementation
  markers.
- Excluded historical run reports from stale-reference cleanup so audit
  evidence stays intact.

Sweep result:

- No destructive cleanup was run.
- No live server commands were run.
- No active stale `loch`/`dungeon_001` release-gate wording remains outside
  legacy/historical contexts.
- Remaining live-runtime and Cursor-meta implementation matches are standard
  guard rules, not bad final states.
- Docs-cache mirror churn remains intentionally out of scope for this standard
  sweep and should use `minecraft-docs-mirror` if cleanup is requested there.
- Sprint intake marker scans now ignore `node_modules`, `reports`, `.tools`,
  `.git`, and generated server-delivery artifacts, so future sprint generation
  does not promote vendor/runtime noise or its own reports into actionable
  backlog signals.
