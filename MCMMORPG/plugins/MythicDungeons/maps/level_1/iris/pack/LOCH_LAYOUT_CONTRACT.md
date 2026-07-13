# Loch Iris Layout Contract

This pack is the Iris side of the `loch` MythicDungeons pipeline.

## Image contract

- Source image: `images/Example.png`
- `images/loch_layout.png` is kept as a named alias for humans and should remain
  byte-for-byte compatible with `Example.png`.
- Current size: `200x500` pixels
- Scale: `1 pixel = 1 block`
- `dimensions/empty.json` reads the `BLUE` channel with `interpolationMethod: NONE`.
- `tiled: false` keeps the image from repeating infinitely.
- `centered: true` places the center of the image at world `0,0`.

## Zone contract

- `empty`: background outside the non-tiled image / no dungeon space.
- `dunstart`: start zone, BLUE channel value `#000040`.
- `dunmid`: mid zone, BLUE channel value `#000080`.
- `dunend`: end and boss zone, BLUE channel value `#0000C0`.

Gameplay progression is owned by MythicDungeons. Iris only owns the deterministic
terrain/zone mask for the dungeon map.
