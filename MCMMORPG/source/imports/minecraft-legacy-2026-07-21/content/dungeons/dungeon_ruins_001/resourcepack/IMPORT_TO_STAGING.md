# DR001 Import To Staging

Status: waiting-for-live-approval

The default import helper still supports a legacy scaffold dry-run for audit
visibility, but DR001 release QA must use `-UseFinalIntake` after final assets
are approved.

## Reference Dry-Run

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1
```

Expected: lists legacy scaffold files only. This is not release evidence.

## Final Intake Dry-Run

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -UseFinalIntake
```

Expected before assets: validator fails and import aborts before writes.

## Final Apply

Only after explicit staging approval and `approved_final` intake:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -UseFinalIntake -Apply
```
