# MythicDungeons editor checklist — dungeon_001

Human session with ops on staging (`Dungeon001QA_01` / `Dungeon001QA_02`). Codex cannot run `/md edit`.

**Before you start**

- [ ] Staging stack up: `.\tools\start-staging-qa.ps1` (2G heap; not 12G optimized)
- [ ] `cd tools/playtest-bot && npm run guard:dungeon-001` → `ok: true`
- [ ] Logs show `Iris]: Loaded dungeon_001!` and `MythicDungeons] Loading dungeon: dungeon_001`
- [ ] Read [DUNGEON_001_EDITOR_RUNBOOK.md](DUNGEON_001_EDITOR_RUNBOOK.md) and [LEVEL_BLUEPRINT.md](LEVEL_BLUEPRINT.md)

## Enter editor

- [ ] Join backend (Velocity `127.0.0.1:25566` or profile host/port)
- [ ] `/gamemode creative` (or survival if testing combat)
- [ ] `/md edit dungeon_001`
- [ ] `/md functiontool` open; confirm graph is empty (matches empty `functions.yml` on disk)

## Build V1 flow (mirror loch)

- [ ] **Dungeon start** → wire **start objective randomizer** (one variant)
- [ ] **Start objective complete** → announce/open mid; signal `dungeon_001_start_objective_complete`
- [ ] **Mid objective** complete → signal `dungeon_001_mid_objective_complete`
- [ ] Route to **end** arena; spawn mob `Dungeon001Warden`
- [ ] On boss death → signal `dungeon_001_end_boss_defeated` → **Finish Dungeon**

## World layout

- [ ] `/md setspawn` in **dunstart** (mossy stone entry), not map center
- [ ] Visually confirm **dunmid** (blue ice) and **dunend** (blackstone) match blueprint palette
- [ ] Outside PNG/layout: air/void only (no accidental solid fill)

## Save and reload

- [ ] `/md save`
- [ ] `/md leave`
- [ ] `/md reload dungeon_001`
- [ ] Re-run guard; no new `Iris Mantle.close` / `EngineMantle.close` in `paper-qa-stderr.log`

## Evidence (required for review)

- [ ] Screenshot or viewer capture: spawn, mid, end
- [ ] Note signal names and any placeholder instance id in [LEVEL_REVIEW_BOARD.md](LEVEL_REVIEW_BOARD.md)
- [ ] Optional automated instance smoke: `.\tools\run-dungeon-001-instance-smoke.ps1` — compare Iris **world names** in both `dungeon-001-instance-*-last-report.json` files

## Do not

- [ ] Do **not** hand-edit `functions.yml` while playbook marks format WIP
- [ ] Do **not** chain multiple `/md play dungeon_001` without guard after Mantle.close errors
