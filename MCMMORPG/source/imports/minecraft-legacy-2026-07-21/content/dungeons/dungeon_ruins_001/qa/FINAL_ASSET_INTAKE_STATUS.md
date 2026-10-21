# DR001 Final Asset Intake Status

Date: 2026-05-30
Scope: staging-only final asset intake planning for `Server — kopia/1.21.11`
Verdict: `FINAL_ASSET_BUNDLE_PARTIAL_AUTHORING_READY`

## Summary

The active final asset gate is still blocked, but the intake has advanced from
missing-only to a partial authoring bundle. The approved source remains
`content/dungeons/dungeon_ruins_001/intake/`. It now contains DR001-named
schematic, ModelEngine, and Nexo technical candidates with SHA-256 checksums,
but it is not `approved_final` because four authored Ogg files are missing and
the generated/model-derived art requires human builder/art review.

Legacy/candidate `dungeon_ruins_001_*` intake files were archived under
`content/dungeons/dungeon_ruins_001/legacy_candidate_archive/` and remain
reference-only.

No import/apply, spawn, dungeon flow, HP phase test, manual resource-pack
publish, production command, or live operation was run.

## Approval And Manifest Status

- Manifest: `content/dungeons/dungeon_ruins_001/intake/manifest.yml`
- Manifest status: `partial_authoring_ready`
- Review approval status: `partial_authoring_ready`
- Checksum policy: `sha256`, required before staging import
- Checksum status: created files have SHA-256 values; sound entries are
  `missing`
- Non-final statuses: manifest intentionally remains non-final
- Validator gate codes: `MISSING_INTAKE`, `INTAKE_PRESENT_NOT_APPROVED`

## Required Source And Target Paths

- Source root: `content/dungeons/dungeon_ruins_001/intake/`
- Nexo source: `intake/nexo/`
- Nexo staging target: `Server — kopia/1.21.11/plugins/Nexo/pack/assets/nexo/`
- ModelEngine source: `intake/modelengine/`
- ModelEngine staging target:
  `Server — kopia/1.21.11/plugins/ModelEngine/blueprints/`
- Iris schematic source: `intake/schematics/`
- Iris staging target:
  `Server — kopia/1.21.11/plugins/Iris/packs/dungeon_ruins_001/schematics/`
- Sound source: `intake/sounds/`
- Sound staging target:
  `Server — kopia/1.21.11/plugins/Nexo/pack/assets/nexo/sounds/`

## Required Assets

Schematics:

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

ModelEngine:

- `dr001_ruin_thrall.bbmodel`
- `dr001_mossbound_archer.bbmodel`
- `dr001_rootling.bbmodel`
- `dr001_animated_rubble.bbmodel`
- `dr001_gatewarden_varos.bbmodel`
- `dr001_drowned_tombguard.bbmodel`
- `dr001_plague_acolyte.bbmodel`
- `dr001_cistern_leech.bbmodel`
- `dr001_bone_mason.bbmodel`
- `dr001_nhalruun.bbmodel`
- `dr001_voidbound_knight.bbmodel`
- `dr001_rune_sentinel.bbmodel`
- `dr001_crown_shade.bbmodel`
- `dr001_anchor_warden.bbmodel`
- `dr001_ruined_king.bbmodel`

Nexo:

- Blocks: `dr001_rune_cracked_stone`, `dr001_ancient_moss_panel`,
  `dr001_cursed_water_grate`, `dr001_void_rune_block`,
  `dr001_broken_crown_ornament`
- Furniture: `dr001_void_anchor`, `dr001_rune_pedestal`,
  `dr001_broken_king_statue`, `dr001_soul_censer`, `dr001_loot_shrine`
- Items: `dr001_ruined_gate_key`, `dr001_relic_shard`,
  `dr001_cleansing_draught`
- Each Nexo entry requires matching final model JSON under
  `intake/nexo/models/item/dr001/...` and texture PNG under
  `intake/nexo/textures/item/dr001/...`

Sounds:

- `sounds/dr001/roots_whisper.ogg`
- `sounds/dr001/cistern_geyser.ogg`
- `sounds/dr001/void_crown_pulse.ogg`
- `sounds/dr001/aurelion_enrage.ogg`

## Found Assets

Approved final `dr001_*` assets found:

- Schematics: none
- ModelEngine: none
- Nexo models/textures: none
- Sounds: none

Rejected legacy/candidate intake assets found:

- 15 legacy/candidate schematics under `intake/schematics/`
- 11 legacy/candidate ModelEngine blueprints named
  `dungeon_ruins_001_*.bbmodel`
- 12 legacy/candidate Nexo model JSON files under
  `intake/nexo/models/item/dungeon_ruins_001/`
- 12 legacy/candidate Nexo texture PNG files under
  `intake/nexo/textures/item/dungeon_ruins_001/`
- 3 legacy/candidate Ogg files under `intake/sounds/dungeon_ruins_001/`

Staging also contains legacy/candidate `dungeon_ruins_001_*` assets from earlier
approved staging work. They are not final DR001 evidence and are not a safe
source for this gate.

## Missing Assets

- 10 final builder schematics listed above
- 15 final ModelEngine `dr001_*.bbmodel` files listed above
- 13 final Nexo model JSON files under `intake/nexo/models/item/dr001/...`
- 13 final Nexo texture PNG files under `intake/nexo/textures/item/dr001/...`
- 4 final Ogg files under `intake/sounds/dr001/`
- Completed manifest review fields:
  `builder_author`, `art_author`, `audio_author`, `reviewer`, `reviewed_at`
- `status: approved_final`
- `review.approval_status: approved_final`
- Real SHA-256 checksums replacing every `sha256: TODO`

## Dry-Run Import

Command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -UseFinalIntake
```

Result: blocked before staging writes. The script ran
`validate_final_assets.mjs`, received the expected final asset validation
failure, and aborted before planning copy operations.

No legacy scaffold dry-run was used as final import evidence.

## Next Import Command If Applicable

Only after the required `dr001_*` files exist, manifest status is
`approved_final`, review approval is `approved_final`, and checksums are filled:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -UseFinalIntake
```

That command is still dry-run by default. Applying the import would require a
separate explicit approval and `-Apply`.

## Role Evidence

- Product/Delivery: final asset gate remains the main blocker; runtime load
  smoke status is documented as OK from prior evidence.
- Plugin/Admin: affected layers are Nexo resource-pack assets, ModelEngine
  blueprints, Iris schematics, and Nexo custom sounds in staging only.
- World/Art/Audio: final builder, model, texture, and sound assets are absent.
- QA/Ops: validators and import dry-run were used only in read/dry-run mode;
  no server commands or runtime flows were run.

## Blocker

Owner: Builder/art/audio intake owner plus human reviewer
Exit condition: approved final `dr001_*` intake files, completed checksums, and
manifest approval allow the dry-run import to plan only staging target paths.
Accepting role: QA/Ops final asset gate reviewer
