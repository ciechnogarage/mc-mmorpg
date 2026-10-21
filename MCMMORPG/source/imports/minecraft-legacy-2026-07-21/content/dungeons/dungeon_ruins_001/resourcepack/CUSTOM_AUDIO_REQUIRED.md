# Custom Audio Required

No real custom `.ogg` files were generated in this environment.

Reason: the workspace exposes Node and Java but no Vorbis/Ogg encoder such as ffmpeg, oggenc, or an installed Node audio package. Writing fake `.ogg` files would break the resource pack, so this pack uses vanilla sound mappings in `assets/nexo/sounds.json` until audio is produced by an audio tool.

Required final files if custom audio is desired:

- `assets/nexo/sounds/dungeon_ruins_001/root_ambient.ogg`
- `assets/nexo/sounds/dungeon_ruins_001/crystal_chime.ogg`
- `assets/nexo/sounds/dungeon_ruins_001/ember_trial.ogg`

Acceptance: each file must be valid Ogg Vorbis, under 10 seconds for ambient stingers, and referenced from `assets/nexo/sounds.json`.
