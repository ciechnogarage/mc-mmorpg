# Iris imagemap — `level_1` authoring notes

This pack uses the live `level_1` setup, not the older `empty` / `Example.png`
scaffold.

## Current live wiring

- Dimension file: `dimensions/level_1.json`
- Imagemap source: `images/level1_map.png`
- Image size: `160x160`
- Scale: `1 pixel = 1 block`
- Mapping: `regionStyle.imageMap.channel = BLUE`
- Placement: `centered: true`, `tiled: false`

## BLUE values in the current image

| BLUE value | Region | Biome | Purpose |
|------------|--------|-------|---------|
| `#00001E` | `void` | `void` | outside footprint |
| `#000040` | `dunstart` | `dunstart` | outer approach meadow |
| `#000080` | `dunmid` | `dunmid` | transition / corridor |
| `#0000C8` | `dunend` | `dunend` | boss meadow core |

## Terrain intent

- Keep the boss core around world `(0,63,0)` stable and flat enough for the runtime arena blockout.
- Keep the immediate corridor through the center close to Y `63-64`.
- Push height variation into the outer meadow where small rolling undulations are safe.
- Prefer lightweight biome decorators over heavy object placement.
