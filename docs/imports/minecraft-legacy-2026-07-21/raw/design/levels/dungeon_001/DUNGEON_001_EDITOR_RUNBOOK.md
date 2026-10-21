# MythicDungeons editor runbook — dungeon_001

**Owner:** MythicDungeons Flow Integrator (live). Codex cannot run `/md edit`.


## Staging state (functions + MythicMobs)

- On staging, `plugins/MythicDungeons/maps/loch/functions.yml` and `plugins/MythicDungeons/maps/dungeon_001/functions.yml` are **empty** (`Functions: []`). **An in-game `/md edit` session is mandatory for MVP+**; playtests only prove load/spawn until the flow graph exists.
- MythicMobs slice on staging:
  - [Dungeon001DungeonSkills.yml](../../../Server%20%E2%80%94%20kopia/1.21.11/plugins/MythicMobs/skills/Dungeon001DungeonSkills.yml)
  - [Dungeon001DungeonMobs.yml](../../../Server%20%E2%80%94%20kopia/1.21.11/plugins/MythicMobs/mobs/Dungeon001DungeonMobs.yml)

## Preconditions

- Backend loaded: `Iris]: Loaded dungeon_001!` and `MythicDungeons] Loading dungeon: dungeon_001`
- Guard passed: `cd tools/playtest-bot && npm run guard:dungeon-001`

## V1 flow (mirror loch)

1. `/md edit dungeon_001`
2. `/md functiontool` — build graph:
   - Dungeon start → start objective randomizer
   - Start complete → announce/open mid (`dungeon_001_start_objective_complete`)
   - Mid objective → `dungeon_001_mid_objective_complete`
   - End spawn `Dungeon001Warden`
   - Signal `dungeon_001_end_boss_defeated` → **Finish Dungeon**
3. `/md setspawn` in dunstart (if spawn wrong)
4. `/md save`
5. `/md leave`
6. `/md reload dungeon_001`

## Do not

- Hand-edit `functions.yml` while playbook marks format WIP
- Run multiple `/md play dungeon_001` without guard after Mantle.close errors

## Evidence

- Screenshot or viewer capture at spawn + mid + end
- Note signal names in `LEVEL_REVIEW_BOARD.md`
- Update Flow Integrator row to **pass**
