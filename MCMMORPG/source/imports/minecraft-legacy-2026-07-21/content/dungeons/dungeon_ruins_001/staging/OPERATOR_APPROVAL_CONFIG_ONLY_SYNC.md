# DR001 Config-Only Sync Approval Packet

Status: review-only, not applied
Target environment: `Server — kopia/1.21.11` staging only

## What Will Be Copied

- MythicMobs config:
  - `mythicmobs/mobs.yml` -> `plugins/MythicMobs/mobs/DR001_Mobs.yml`
  - `mythicmobs/bosses.yml` -> `plugins/MythicMobs/mobs/DR001_Bosses.yml`
  - `mythicmobs/skills.yml` -> `plugins/MythicMobs/skills/DR001_Skills.yml`
  - `mythicmobs/drops.yml` -> `plugins/MythicMobs/droptables/DR001_Drops.yml`
- MythicDungeons config:
  - `mythicdungeons/dungeon_ruins_001/config.yml`
  - `mythicdungeons/dungeon_ruins_001/functions.yml`

## What Will Not Be Copied

- ModelEngine `.bbmodel`
- Nexo models, textures, sounds, resource-pack files
- Iris schematics, images, packs, dimensions
- `.ogg`, `.png`, `.schem`, `.schematic`, `.zip`
- legacy `dungeon_ruins_001_*` scaffold assets
- MMOItems/MMOCore reward config; this needs a separate merge review

## Dry-Run Command

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\prepare_config_only_staging_sync.ps1
```

## Apply Command - Do Not Run Without Approval

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\prepare_config_only_staging_sync.ps1 -Apply -ApproveConfigOnlyStagingSync
```

## Rollback

Delete the copied config-only files from staging:

```powershell
Remove-Item -LiteralPath "Server — kopia\1.21.11\plugins\MythicMobs\mobs\DR001_Mobs.yml" -Force
Remove-Item -LiteralPath "Server — kopia\1.21.11\plugins\MythicMobs\mobs\DR001_Bosses.yml" -Force
Remove-Item -LiteralPath "Server — kopia\1.21.11\plugins\MythicMobs\skills\DR001_Skills.yml" -Force
Remove-Item -LiteralPath "Server — kopia\1.21.11\plugins\MythicMobs\droptables\DR001_Drops.yml" -Force
Remove-Item -LiteralPath "Server — kopia\1.21.11\plugins\MythicDungeons\maps\dungeon_ruins_001" -Recurse -Force
```

## Verification After Approved Apply

```text
/mm reload
/mm mobs info DR001_GatewardenVaros
/mm mobs info DR001_RuinedKingAurelion
/md reload dungeon_ruins_001
```

Then inspect `Server — kopia/1.21.11/logs/latest.log` for DR001 load errors.
Do not spawn bosses until reload is clean.

## Stop Conditions

- Target is not exactly staging.
- Any operation points outside `Server — kopia/1.21.11/plugins`.
- Any binary asset is listed for copy.
- Any legacy scaffold asset path is listed for copy.
- `validate_references.mjs` fails.
- `validate_staging_preflight.mjs` fails.
- MythicMobs reload reports DR001 condition, skill, mob, drop, or model errors.
- MythicDungeons cannot load `dungeon_ruins_001`.
