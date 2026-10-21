# Hybrid Final Asset Report

Status: hybrid_review_required

- Generated 15 final-candidate Iris schematics in `intake/schematics`.
- Generated final-candidate Nexo model/texture assets in `intake/nexo`.
- Generated 11 final-candidate ModelEngine `.bbmodel` files with non-empty hitbox data.
- Generated 3 Ogg container audio candidates in `intake/sounds/dungeon_ruins_001`.
- Updated `intake/manifest.yml` to `hybrid_review_required`.

These files are not human-authored. They are suitable for staging asset QA only and require manual builder/art/audio review before production deployment.

## Staging Evidence - 2026-05-29

- `validate_final_assets.mjs --json` passed with 53 checked files,
  `approvalStatus: hybrid_review_required`, and `requiresHumanReview: true`.
- `import_assets_to_staging.ps1 -UseFinalIntake -Apply` copied or confirmed the
  final intake files in `Server — kopia/1.21.11`.
- Staging startup reached Paper `Done`.
- Iris detected the `dungeon_ruins_001` pack.
- Nexo completed resource-pack generation.
- ModelEngine imported all `dungeon_ruins_001_*.bbmodel` files without final
  `Missing hitbox` warnings after switching to template-derived blueprints.
- In-client particle/sound QA remains blocked until the MythicDungeons editor
  flow for `dungeon_ruins_001` is wired and playable in staging.
