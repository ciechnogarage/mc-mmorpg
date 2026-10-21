# Level Review Board — dungeon_001

Parent task: **DUNGEON-001**

| Role | Status | Owner finding | Next action |
| --- | --- | --- | --- |
| Dungeon Level Designer Architect | pass | Brief + blueprint in `design/levels/dungeon_001/` | — |
| Iris Layout Architect | pass | Pack `dungeon_001`; `ChunkGenerator: iris:dungeon_001`; sync to MD map | — |
| MythicDungeons Flow Integrator | **pending-human** | `functions.yml` still empty (loch template also empty); MythicMobs signals ready | [`MD_EDITOR_CHECKLIST.md`](MD_EDITOR_CHECKLIST.md) — `/md edit` → `/md save` |
| Encounter Designer | pass | `Dungeon001DungeonMobs.yml` + `Dungeon001DungeonSkills.yml` | Tune after editor flow |
| Dungeon QA Architect | **mvp-pass** | `guard:dungeon-001` ok; `playtest:dungeon-001` → iris + mossy_stone_bricks | Instance smoke + zone walkthrough |

## Codex runs

| Packet | Role | Artifact |
| --- | --- | --- |
| WP-dungeon-001-design | dungeon_designer | `tasks/codex-team-runs/` (see team-board) |
| WP-dungeon-001-iris | config_codex_worker | |
| WP-dungeon-001-encounter | mob_boss_designer | |
| LangGraph | minecraft_server_orchestrator | `tasks/orchestrator-runs/` |

## QA evidence (2026-05-26)

| Gate | Result | Artifact |
| --- | --- | --- |
| Guard | pass | `tools/playtest-bot/reports/dungeon-001-qa-last-report.json` (`guard.ok: true`) |
| Layout playtest | pass | `status: playable but rough`, `afterCommand.world: iris:*`, `mossy_stone_bricks` |
| Ops / permissions | fixed | `tools/grant-dungeon-001-qa.py` uses `usercache.json` UUID (Velocity ≠ offline UUID3) |
| Instance smoke | partial | `.\tools\run-dungeon-001-instance-smoke.ps1` — confirm distinct `dungeon_001_N` in `latest.log` |

## Instance smoke (automated + log)

1. `.\tools\run-dungeon-001-instance-smoke.ps1` (A runs `/md leave` after play).
2. Compare `dungeon-001-instance-a-last-report.json` vs `dungeon-001-instance-b-last-report.json`.
3. **Pass:** different `dungeon_001_<n>` lines in `Server — kopia/1.21.11/logs/latest.log`.

## Blockers

| ID | Owner | Blocker | Exit |
| --- | --- | --- | --- |
| MD-FLOW-001 | human/live | Editor graph not saved (MVP+) | [`MD_EDITOR_CHECKLIST.md`](MD_EDITOR_CHECKLIST.md) → `/md save` → `/md reload dungeon_001` |
| QA-INSTANCE-001 | qa | Log may show single `dungeon_001_0` if prior instance not unloaded | A `/md leave`, restart backend, re-run instance smoke |
