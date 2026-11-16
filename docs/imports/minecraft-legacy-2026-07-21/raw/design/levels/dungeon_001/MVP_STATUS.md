# MVP status — dungeon_001

## MVP scope (aligned with loch V1)

| Layer | MVP criterion | Status |
| --- | --- | --- |
| Iris layout | Pack listed in log (`[Iris]:   dungeon_001`); `ChunkGenerator: iris:dungeon_001` | **partial** — `dungeon_001_layout` now points to image distinct from loch (`Example1`), pending visual walkthrough |
| MD map | CLASSIC; `/md play dungeon_001` enters instance | **partial** — playtest 2026-05-26 layout only (`dungeon-001-qa-last-report.json`) |
| Encounters | Mobs + `dsignal` skills (loch parity) | files on staging; **not validated** via real Codex run |
| MD function graph | Generator + load | **partial** — `generate-dungeon-001-functions-yml.py` (5 funkcji, ~3.5 KB); dungeon ładuje się na staging; flow in-game **nie** potwierdzony w playteście — [`MD_FLOW_AUTOMATION_REPORT.md`](MD_FLOW_AUTOMATION_REPORT.md) |
| Instances | Two plays → different instance ids | **PASS** — `verify-dungeon-001-instances.py` (2026-05-27): ids 0–5 + foldery `dungeon_001_*` |

## Staging start (QA — 2 GB, not 12 GB)

**Do not** use `Server — kopia/1.21.11/start-optimized.ps1` from the agent or on a RAM-limited PC (12G + `AlwaysPreTouch` often prevents Paper from binding 25565).

```powershell
cd C:\Codex\Minecraft
.\tools\stop-staging-stack.ps1          # optional: clear stray java
.\tools\start-staging-qa.ps1 -HeapBudget 2G -StopStrayFirst
python tools\wait-dungeon-001-logs.py 300
```

Logs if JVM fails: `tools/playtest-bot/reports/paper-qa-stdout.log`, `paper-qa-stderr.log`, and `Server — kopia/1.21.11/logs/latest.log`.

Foreground console (recommended outside Cursor): `.\tools\start-staging-qa.ps1 -Foreground`.

## One-command verify (server must be running)

```powershell
cd C:\Codex\Minecraft
python tools\grant-dungeon-001-qa.py    # ops UUID from usercache — restart Paper after first QA join
.\tools\mvp-dungeon-001.ps1 -StartServer   # lite 2G + wait for log markers
cd tools\playtest-bot
npm run guard:dungeon-001
npm run playtest:dungeon-001
.\tools\run-dungeon-001-instance-smoke.ps1
```

**Ops note:** QA bots use Velocity UUIDs in `usercache.json`, not offline UUID3. Re-run `grant-dungeon-001-qa.py` after first join, then restart staging.

Cursor: repo `.cursorignore` and user `files.watcherExclude` exclude `Server — kopia/` to reduce RAM while QA runs.

## Flow (5 min, in-game) — full MVP+

Only needed if you want start→mid→boss→finish automation in MD (not required for first layout smoke).

1. `/md edit dungeon_001`
2. Wire signals from `Dungeon001DungeonSkills.yml` in function graph
3. `/md save` → `/md leave` → `/md reload dungeon_001`

See [`DUNGEON_001_EDITOR_RUNBOOK.md`](DUNGEON_001_EDITOR_RUNBOOK.md).

## 2026-05-26 visual hotfix

- `plugins/Iris/packs/dungeon_001/dimensions/dungeon_001.json`:
  - `regionStyle.imageMap.image` changed to `dungeon_001_layout`
  - `fluidHeight` changed `0 -> 62`
- `plugins/Iris/packs/dungeon_001/images/dungeon_001_layout.png` replaced from `Example1.png` to break 1:1 clone with loch.
- Full visual acceptance still requires in-game walkthrough + instance smoke.

## 2026-05-27 automation (spawn + QA)

- Root cause void death: luidHeight: 62 broke Iris spawn platform → staging uses luidHeight: 0 until walkthrough.
- Playtest bot: waitForDungeonGround, instance-active-at-spawn (no false 
o-teleport).
- Staging MD: PlayerLives: 0, Gamemode: CREATIVE for QA bots.
- Live preview: design/levels/dungeon_001/live-preview/latest.png (concept-B inset).
- **Still human (MVP+ blocker):** `/md edit` + functiontool GUI → `/md save` ([MD_EDITOR_CHECKLIST.md](MD_EDITOR_CHECKLIST.md)). Bot probe: `npm run probe:md-edit-dungeon-001` — chat commands alone nie budują grafu.
- **Auto raport:** [MD_FLOW_AUTOMATION_REPORT.md](MD_FLOW_AUTOMATION_REPORT.md) (2026-05-27).
