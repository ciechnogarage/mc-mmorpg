# Level Blueprint — level_001 / dungeon_001 (Dungeon-001)

## Identity

- Level id: `level_001`
- Dungeon id: `dungeon_001`
- Player promise: cold flooded ruin; private instance per party
- Session target: 8–12 minutes V1 slice

## Zone Contract

- Start / `dunstart`: mossy stone entry, safe spawn, first objective
- Mid / `dunmid`: blue ice pressure, wave objective
- End / `dunend`: blackstone boss arena, `Dungeon001Warden`
- Empty/fallback: `minecraft:air`

## Visual Palette

- Start: `minecraft:mossy_stone_bricks`
- Mid: `minecraft:blue_ice`
- End: `minecraft:polished_blackstone_bricks`
- Void/background: `minecraft:air`

## Gameplay Flow

1. Spawn in start zone.
2. Run one start objective variant.
3. Send `dungeon_001_start_objective_complete`.
4. Open or announce route to mid.
5. Run one mid objective variant.
6. Send `dungeon_001_mid_objective_complete`.
7. Open or announce route to end.
8. Spawn `Dungeon001Warden`.
9. Boss death sends `dungeon_001_end_boss_defeated`.
10. MythicDungeons Finish Dungeon returns the player.

## Server integration

- `DungeonType: CLASSIC` — new world copy per party ([how-it-works](https://git.mythiccraft.io/mythiccraft/MythicDungeons/-/wikis/How-it-Works))
- `ChunkGenerator: iris:dungeon_001` (verified from loch template on staging)
- `regionStyle.imageMap.image: dungeon_001_layout` (layout file decoupled from loch `Example.png`)
- `fluidHeight: 62` for baseline flooded ambience (requires walkthrough validation)
- `MaxInstances: 5` (staging default)

## Acceptance Checklist

- [x] `/md play dungeon_001` starts instance via Velocity `25566` — `npm run playtest:dungeon-001` → `playable but rough`, `iris:*` (2026-05-26)
- [x] Spawn in `dunstart`, not map center — `firstSolidBelow: mossy_stone_bricks` at y≈122–124 (`dungeon-001-qa-last-report.json`)
- [~] Start/mid/end visually distinct; air outside PNG — image source switched to `dungeon_001_layout` (from `Example1`), still needs manual viewer confirmation
- [ ] Two parties → two different instance IDs in logs/PAPI — run `.\tools\run-dungeon-001-instance-smoke.ps1`; verify `dungeon_001_0` vs `dungeon_001_1` in `latest.log`
- [x] No `Iris Mantle.close` watchdog loop after single playtest — guard clean 2026-05-26
- [ ] Editor flow: start → mid → boss → Finish Dungeon — **human** [`MD_EDITOR_CHECKLIST.md`](MD_EDITOR_CHECKLIST.md) + `/md save`

## Review Board

See [`LEVEL_REVIEW_BOARD.md`](LEVEL_REVIEW_BOARD.md).
