# Loch Iris Layout Contract

This pack is the Iris side of the `level_1` / `Kwietna Polana` MythicDungeons pipeline.

## Live image contract

- Source image: `images/level1_map.png`
- Current size: `160x160` pixels
- Scale: `1 pixel = 1 block`
- `dimensions/level_1.json` reads the `BLUE` channel with `interpolationMethod: NONE`
- `tiled: false` keeps the image from repeating infinitely
- `centered: true` places the image center at world `0,0`

## Zone contract

- `void`: background / outside dungeon footprint, current BLUE value `#00001E`
- `dunstart`: outer approach meadow, current BLUE value `#000040`
- `dunmid`: traversal corridor / transition meadow, current BLUE value `#000080`
- `dunend`: boss meadow core around the arena, current BLUE value `#0000C8`

Gameplay progression is owned by MythicDungeons. Iris only owns the deterministic
terrain shell, biome dressing, and zone mask for the dungeon map.
