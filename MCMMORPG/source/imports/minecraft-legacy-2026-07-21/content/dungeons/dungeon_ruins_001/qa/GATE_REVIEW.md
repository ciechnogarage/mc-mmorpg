# DR001 Gate Review

Gate: release-prep source gate
Verdict: waiting-for-live-approval

Latest staging asset visual/build-review verdict: `human_review_required`
(`FINAL_VISUAL_REVIEW_PARTIAL_HUMAN_REVIEW_REQUIRED`, 2026-05-30).

## Passed

- DR001 final asset intake planning pass completed on 2026-05-30 without
  applying imports or touching production/live.
- Runtime load smoke remains OK by current validator evidence:
  `validate_mythicdungeons_map_readiness.mjs` reports `LOAD_SAFE_READY`.
- Final asset validator now reports explicit gate codes for routing:
  `MISSING_INTAKE`, `INTAKE_PRESENT_NOT_APPROVED`, and
  `LEGACY_ASSET_REJECTED`.
- `qa/FINAL_ASSET_INTAKE_STATUS.md` records required assets, found legacy
  candidates, missing final `dr001_*` assets, target staging paths, manifest
  status, and the next dry-run command.
- Active namespace migrated to `DR001_` / `dr001_`.
- Cross-reference validator passes for active source specs.
- MythicDungeons command syntax was sourced from the local docs mirror.
- No production or live server command was run.
- Legacy scaffold asset apply is blocked by default; normal staging path is
  final intake only.
- Suspicious inline MythicMobs HP shorthand was removed from active DR001
  mobs/bosses.
- Config-only staging sync packet exists for review, with dry-run tooling and
  preflight validation. It has not been applied.
- Diagnosis-confirmed DR001 MythicMobs targeter strings were fixed in source:
  unsupported `RandomPlayersInRadius` is gone, and `MobsInRadius` now includes
  an explicit DR001 non-boss type list.
- Deterministic validators now cover DR001 targeters, MythicDungeons CLASSIC
  map readiness, and current startup log scanning.
- Staging command-channel safety artifacts exist:
  `tools/run-staging-command.ps1`,
  `tools/start-staging-console.ps1`,
  `tools/validate-staging-command-channel.ps1`, and
  `qa/OPERATOR_APPROVAL_STAGING_COMMAND_CHANNEL.md`.
- The automated channel is dry-run/manual-fallback only; visible console mode
  is available for later approved operator input. No server command was sent.
- Staging RCON command transport is prepared as the approved automation path:
  dry-run by default, strict allowlist, local ignored password source, and no
  broad command shell. No command was sent during setup.
- A DR001 Mineflayer QA bot workflow now exists as a dry-run-only staging
  player-context path. It uses the existing local Mineflayer dependency, allows
  only `/md create dungeon_ruins_001`, writes ignored sanitized runtime JSON,
  and requires a separate `--approve-command` execution approval before joining
  staging or sending the command.
- DR001 standalone boss death-signal hardening passed staging smoke on
  2026-05-30: all three bosses spawned, produced persistence evidence, and
  cleaned up without a MythicDungeons `DungeonSignalMechanic` NPE.
- The DR001 command channel now has exact narrow staging commands for boss info,
  boss spawn, entity checks, scoped cleanup, temporary arena platform setup, and
  `mm reload`.

## Blocked

- Final asset intake is blocked as `MISSING_INTAKE`: approved final `dr001_*`
  files are absent, manifest status is `pending_final_assets`, review approval
  is `pending_final_assets`, and schematic checksums still contain
  `sha256: TODO`.
- Existing `dungeon_ruins_001_*` intake/staging assets are legacy/candidate
  evidence only and must not be copied or accepted as final DR001 assets.
- Final intake dry-run with `-UseFinalIntake` aborts before copy planning until
  `validate_final_assets.mjs` passes normal mode.
- Final builder/artist/audio assets are pending.
- `validate_final_assets.mjs` must fail until intake is `approved_final`.
- Runtime staging import and `/md reload` / `/md play` require explicit operator approval.
- MMOCore reward command hooks and weekly lockout must be proven in staging.
- MythicMobs HP phase wrapper conditions are explicitly marked for runtime
  verification and are not release-proven until plugin reload/playtest passes.
- Full MythicDungeons flow remains unproven; the current pass tested standalone
  boss smoke only.
- HP phase wrappers remain unproven because no safe HP trigger command is
  approved/proven.
- Reward/economy smoke remains blocked by missing MMOItems material definitions
  and separate reward import/review requirements.
- Generated final asset candidates are not approved final. The 2026-05-30
  staging visual/build-review pass proved Nexo registry/give and boss spawn
  command success, but Nexo placement failed, schematics were not pasted, and
  no screenshot/human visual acceptance was captured.
- Nexo block candidate syntax required a source fix: `custom_block.type` must
  use documented `NOTEBLOCK`, not `note_block`.
- The 2026-05-30 Prism real-client pass is blocked as
  `PRISM_CLIENT_BLOCKED_NO_INSTANCE`: Prism/auth worked and the client attempted
  Velocity `127.0.0.1:25566`, but the only local instance is `26.1.2` and
  staging rejected it as `Outdated server! I'm still on 1.21.11`. No compatible
  local `1.21.11` Prism instance or cache was found.

## Rollback

Remove DR001/dr001 imports from staging plugin folders and restore backups from
`Server — kopia/1.21.11/codex-backups/dungeon_ruins_001/`. Source rollback is a
normal git revert of this release-prep change set.
