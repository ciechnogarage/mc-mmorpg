# DR001 Asset Requirements

Final production QA for `dungeon_ruins_001` requires approved DR001/dr001
assets. Generated or legacy `dungeon_ruins_001_*` files may be used as visual
reference only.

## Required Schematic Rooms

- `gatehouse_broken_01.schem`
- `rooted_courtyard_01.schem`
- `varos_gate_arena_01.schem`
- `collapsed_stairwell_01.schem`
- `cistern_puzzle_01.schem`
- `nhalruun_oracle_chamber_01.schem`
- `crown_bridge_01.schem`
- `void_anchor_hall_01.schem`
- `void_sanctum_arena_01.schem`
- `loot_shrine_exit_01.schem`

## Required Nexo Assets

Blocks: `dr001_rune_cracked_stone`, `dr001_ancient_moss_panel`,
`dr001_cursed_water_grate`, `dr001_void_rune_block`,
`dr001_broken_crown_ornament`.

Furniture: `dr001_void_anchor`, `dr001_rune_pedestal`,
`dr001_broken_king_statue`, `dr001_soul_censer`, `dr001_loot_shrine`.

Items: `dr001_ruined_gate_key`, `dr001_relic_shard`,
`dr001_cleansing_draught`.

## Required ModelEngine Assets

Final `.bbmodel` files must match `modelengine/models.md`, include hitboxes, and
include encounter animations used by MythicMobs skills.

## Required Sounds

- `intake/sounds/dr001/roots_whisper.ogg`
- `intake/sounds/dr001/cistern_geyser.ogg`
- `intake/sounds/dr001/void_crown_pulse.ogg`
- `intake/sounds/dr001/aurelion_enrage.ogg`

## Validation

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
```

Expected pre-release result: fail with missing/pending final assets. Passing
normal mode requires `approved_final` manifest status and filled checksums.
