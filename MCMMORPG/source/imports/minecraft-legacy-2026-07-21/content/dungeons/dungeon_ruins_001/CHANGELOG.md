# Changelog

## 0.2.0-dr001 - 2026-05-29

- Added a 2026-05-30 final asset intake planning pass. It documents the
  remaining `MISSING_INTAKE` gate in `qa/FINAL_ASSET_INTAKE_STATUS.md`, keeps
  legacy `dungeon_ruins_001_*` candidates rejected as final evidence, and
  confirms no import/apply, spawn, flow, HP phase test, manual resource-pack
  publish, or production/live operation was run.
- Updated `validate_final_assets.mjs` to emit explicit gate codes for final
  asset routing while preserving the expected failure until approved final
  `dr001_*` files, checksums, and manifest approval exist.
- Rebuilt the active source contract from "Ruins of the Fallen Root" to
  **Ruiny Złamanej Korony** using the DR001 concept brief.
- Adopted `DR001_` and `dr001_` as the active public namespaces while retaining
  older `dungeon_ruins_001_*` content as legacy scaffold/reference evidence.
- Added the three-stage release-prep flow: START Overgrown Ruins with Varos,
  MID Sunken Catacombs with Nhal'Ruun, and END Void Sanctum with Aurelion.
- Reworked active MythicMobs, MythicDungeons, Iris, Nexo, MMOItems, MMOCore,
  and ModelEngine source specs around the DR001 roster, objectives, rewards,
  and asset contracts.
- Updated final asset validation to require DR001/dr001 final intake assets and
  to block normal final import until `approved_final` manifest review and
  checksums exist.
- Added cross-reference validation for active source IDs.
- Added release-prep role, gate, live-runner, player QA, rollback, and lessons
  artifacts. Runtime testing remains `waiting-for-live-approval`.
- Runtime-blocker hardening: `import_assets_to_staging.ps1 -Apply` now blocks
  legacy scaffold apply unless final intake is used or an explicit legacy test
  override is provided; DR001 MythicMobs HP phase shorthand was removed from
  active mobs/bosses and replaced with runtime-verification wrappers.
- Added a review-only config-only staging sync packet for DR001 plugin-native
  MythicMobs and MythicDungeons configs. It does not copy assets, does not
  bypass final asset validation, and requires explicit operator approval before
  any apply/reload/spawn step.
- Fixed the diagnosis-confirmed DR001 MythicMobs startup targeter strings:
  bare `MobsInRadius{r=10}` now uses explicit DR001 non-boss mob types, and
  unsupported `RandomPlayersInRadius` now uses documented
  `PlayersInRadius{r=30;limit=3;sort=RANDOM}` syntax.
- Normalized the source MythicDungeons `functions.yml` placeholder to the local
  `Version: 1` plus `Functions: []` shape used by a working staging map, while
  keeping the editor-owned flow blocked.
- Added deterministic startup-error validators for DR001 targeters,
  MythicDungeons CLASSIC map readiness, and current `latest.log` scanning.
  The map readiness check reports `CONFIG_ONLY_NOT_LOAD_READY` until generated
  CLASSIC map files exist in staging.
- Added a staging command-channel safety pass. The dry-run allowlist guard and
  validator exist, but automated command send is blocked because no safe stdin
  or RCON transport is available. No server command was sent; `/md create
  dungeon_ruins_001` remains pending explicit operator approval.
- Added `tools/start-staging-console.ps1` for staging-only visible Paper console
  mode. It validates the exact staging target and keeps command entry manual;
  no command was sent in this pass.
- Prepared a staging-only RCON command transport in
  `tools/run-staging-command.ps1`. It is dry-run by default, requires
  `-ApproveStagingCommand` for actual sends, reads the password only from
  `STAGING_RCON_PASSWORD` or ignored `tools/local/staging-rcon.env`, and keeps
  the pending command as `md create dungeon_ruins_001`. No command was sent in
  this pass; staging restart/password setup is required before RCON can be
  active. Final asset validation remains an expected failure.
- Added a DR001 Mineflayer QA bot dry-run workflow for the player-context
  MythicDungeons bootstrap path. It uses the existing local Mineflayer
  dependency, allows only `/md create dungeon_ruins_001`, writes ignored
  sanitized result JSON, and requires a later explicit `--approve-command`
  approval before any bot joins staging or sends a command.

## 0.1.0 - 2026-05-28

- Added the initial modular `dungeon_ruins_001` scaffold and asset intake gate.
- Generated placeholder/scaffold assets for Iris, Nexo, ModelEngine, and QA.
- Recorded partial staging asset evidence and final-intake blockers.
