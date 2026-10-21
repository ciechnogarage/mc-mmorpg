# Visual redesign report — DUNGEON-001

Date: 2026-05-26

## Changes applied

1. `Server — kopia/1.21.11/plugins/Iris/packs/dungeon_001/images/dungeon_001_layout.png`
   - Replaced with non-loch source (`Example1.png` hash), so `dungeon_001_layout` no longer equals `Example.png`.
2. `Server — kopia/1.21.11/plugins/Iris/packs/dungeon_001/dimensions/dungeon_001.json`
   - `regionStyle.imageMap.image`: `Example` -> `dungeon_001_layout`
   - `fluidHeight`: `0` -> `62`
3. Docs updated:
   - `design/levels/dungeon_001/LEVEL_BLUEPRINT.md`
   - `design/levels/dungeon_001/MVP_STATUS.md`

## Verification snapshot

- `images/Example.png` md5: `d578b044f170d76334be9cb5e395fbee`
- `images/dungeon_001_layout.png` md5: `5b1329517e20e9ef962acd41e91a1bbb`
- `images/Example1.png` md5: `5b1329517e20e9ef962acd41e91a1bbb`
- `dimensions/dungeon_001.json`: `imageMap.image=dungeon_001_layout`, `fluidHeight=62`

## Still blocked

- MD editor flow remains human-only (`functions.yml` still `Functions: []`).
- CLASSIC instance proof (`dungeon_001_0` vs `dungeon_001_1`) remains pending.
- Visual pass/fail requires in-game walkthrough after server restart/reload.

## Rollback

1. Restore `dimensions/dungeon_001.json` values:
   - `imageMap.image=Example`
   - `fluidHeight=0`
2. Restore `images/dungeon_001_layout.png` from previous backup or copy `Example.png`.
