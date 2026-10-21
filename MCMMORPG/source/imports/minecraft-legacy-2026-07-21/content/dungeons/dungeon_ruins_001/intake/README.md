# DR001 Final Asset Intake

This folder is the only accepted source for final builder, artist, and audio
assets for **Ruiny Złamanej Korony**.

Legacy `dungeon_ruins_001_*` generated files are reference scaffolds only. Final
assets must use DR001/dr001 names from `manifest.yml`.

## Required Intake

- `schematics/`: 10 builder-authored `.schem` files listed in `manifest.yml`.
- `nexo/`: final `models/item/dr001/...` and `textures/item/dr001/...` trees.
- `modelengine/`: 15 final `dr001_*.bbmodel` files with non-empty hitboxes.
- `sounds/`: final Ogg files under `sounds/dr001/`.

## Approval Gate

Normal final validation fails until:

```yaml
status: approved_final
review:
  approval_status: approved_final
```

All `sha256: TODO` placeholders and `pending` statuses must be replaced with
reviewed final values. `--allow-review-candidates` is only for review evidence
and must not be used for final staging import.

## Commands

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs --json
```
