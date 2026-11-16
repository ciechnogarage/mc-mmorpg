# Visual check — dungeon_001 live preview

Repo-visible capture for Cursor meta: PNG frames + `latest.json` under [`live-preview/`](live-preview/) (no browser required).

## Staging up (once per session)

From `C:\Codex\Minecraft`:

```powershell
.\tools\start-staging-qa.ps1 -HeapBudget 2G -StopStrayFirst
python tools\wait-dungeon-001-logs.py 300
python tools\grant-dungeon-001-qa.py   # first-time QA UUID only; restart if prompted
```

Optional full MVP gate (sync + guard + standard playtest):

```powershell
.\tools\mvp-dungeon-001.ps1 -StartServer
```

## Capture preview (after layout / Iris / MD changes)

```powershell
$env:Path = "C:\Codex\Minecraft\.tools\node-v22.11.0-win-x64;" + $env:Path
cd C:\Codex\Minecraft\tools\playtest-bot
npm run capture:dungeon-001-preview
```

What it does:

1. **Guard** — Paper `25565`, Velocity `25566`, log markers `dungeon_001`, no known startup errors.
2. **Playtest** — profile `dungeon-001-visual-preview`: `/md play dungeon_001`, 12s settle, mossy start check.
3. **Viewer hold 60s** on `http://127.0.0.1:3009` (first-person).
4. **Frames every ~8s** → `design/levels/dungeon_001/live-preview/frame-<timestamp>.png`.
5. **`latest.json`** — world, position, block under feet, viewer URL, frame list; **`latest.png`** mirrors newest frame.

Meta workflow: open `live-preview/latest.png` + `latest.json` in Cursor after each capture. Optional live browser: URL in `latest.json`.

## When to re-run

- After changing Iris layout PNG (`concepts/` → staging sync).
- After `fluidHeight`, biome, or MD spawn edits.
- Before closing a layout iteration in review board.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Guard fails | `start-staging-qa.ps1`; check `tools/playtest-bot/reports/paper-qa-stdout.log` |
| Viewer port busy | Stop prior bot; profile uses port `3009` |
| Empty/black frames | Chunks still loading — increase `settleMs` in profile |
| Wrong start block | `/md setspawn` in `dunstart`; see `LEVEL_BLUEPRINT.md` |
