# Implementation Notes

## 2026-05-30 Final Asset Intake Planning Pass

- Ran a bounded staging-only final asset intake review for DR001 after the
  `DR001_RESTART_LOAD_SMOKE_OK` verdict.
- Confirmed runtime load smoke is OK by prior evidence and current validators:
  `validate_mythicdungeons_map_readiness.mjs` reports `LOAD_SAFE_READY`,
  `validate_staging_preflight.mjs` reports the config-only staging set is
  installed, and `validate_references.mjs` passes.
- Final asset gate remains the main blocker. The approved final intake source
  is `content/dungeons/dungeon_ruins_001/intake/`, but required final
  `dr001_*` schematics, ModelEngine blueprints, Nexo model/texture assets, and
  `sounds/dr001/*.ogg` files are missing.
- Existing intake and staging assets named `dungeon_ruins_001_*` are
  legacy/candidate assets only and were rejected as final DR001 evidence.
- `intake/manifest.yml` is still `pending_final_assets`, review approval is
  `pending_final_assets`, and schematic checksum entries still contain
  `sha256: TODO`.
- `tools/validate_final_assets.mjs` now emits machine-readable gate codes in
  JSON and text output, including `MISSING_INTAKE`,
  `INTAKE_PRESENT_NOT_APPROVED`, and `LEGACY_ASSET_REJECTED`.
- `tools/import_assets_to_staging.ps1 -UseFinalIntake` was run in dry-run mode
  only. It aborted before staging writes because final validation failed.
- Added `qa/FINAL_ASSET_INTAKE_STATUS.md` as the reviewable intake gap report
  and next-command record.
- No import apply, spawn, dungeon flow, HP phase test, manual resource-pack
  publish, production command, or live operation was run.

## 2026-05-29 Mineflayer QA Bot Dry-Run Workflow

- Added a staging-only Mineflayer QA bot workflow for DR001 player-context
  command execution. It is dry-run by default and must not connect or execute
  commands unless a later pass supplies `--approve-command`.
- The workflow uses the existing local `tools/playtest-bot` Mineflayer
  dependency; no package install was run.
- The initial command allowlist contains only `/md create dungeon_ruins_001`.
  The bot rejects command chaining, OP/deop, stop, reloads, give/economy,
  MythicMobs info, and mob spawn commands.
- Staging feasibility evidence: Paper 1.21.11 listens on port 25565,
  `online-mode=false`, whitelist is off, and staging `ops.json` contains the QA
  OP users including `Dungeon001QA_01`.
- No bot execution and no Minecraft command were sent in this pass. Map
  readiness remains `CONFIG_ONLY_NOT_LOAD_READY`; final assets remain an
  expected failure.

## 2026-05-29 Staging RCON Command Transport

- Transport decision: staging RCON is the approved automation path after local
  password setup and staging restart. Visible console mode remains available
  for human/operator input only.
- Updated staging-only `Server — kopia/1.21.11/server.properties` to set
  `enable-rcon=true` while leaving `rcon.password` unset. The operator must set
  the local staging password before restart; no secret was committed.
- Added ignored `tools/local/staging-rcon.env` handling with
  `tools/local/staging-rcon.env.example` as the safe template. The sender also
  accepts `STAGING_RCON_PASSWORD` from the environment and never prints it.
- Reworked `tools/run-staging-command.ps1` into a strict allowlisted RCON
  sender. It remains dry-run by default, requires `-ApproveStagingCommand` for
  actual sends, refuses production/live-looking targets, checks Paper runtime
  and RCON config, rejects command chaining/control characters, and logs only
  sanitized command metadata.
- Updated `tools/validate-staging-command-channel.ps1` to prove allowed
  dry-runs pass, forbidden commands fail, command chaining is rejected, missing
  password fails safely, and approved send is not exercised.
- No Minecraft command was sent. `md create dungeon_ruins_001` remains pending
  for a separate approval after RCON password setup and staging restart.
- Final asset gate remains an expected failure.

## 2026-05-29 Runtime Blocker Hardening

- `tools/import_assets_to_staging.ps1 -Apply` now fails fast unless
  `-UseFinalIntake` is present. `-AllowLegacyScaffoldApply` exists only for
  intentional legacy scaffold testing and must not be treated as DR001 runtime
  evidence.
- Final intake remains the only normal staging asset path for DR001. The final
  asset gate still intentionally fails until real approved `dr001_*` assets and
  checksums exist.
- Unverified MythicMobs inline HP shorthand such as `@self <60% 0.45` was
  removed from active DR001 mobs/bosses. Phase transitions now call named
  wrapper skills with `RUNTIME_VERIFY_MYTHICMOBS_HP_CONDITION` comments and
  must be confirmed by MythicMobs reload/runtime testing before release.
- `tools/validate_references.mjs` now fails on missing import safety guards,
  active legacy `dungeon_ruins_001_*` contract tokens, and suspicious inline HP
  shorthand.

## 2026-05-29 Staging Command Channel Safety Pass

- Transport decision: manual fallback. `tools/start-staging-qa.ps1` and
  `tools/start-staging-stack.ps1` launch hidden/background Java processes
  without a retained safe stdin channel, staging RCON is disabled, and no local
  repo RCON sender was found.
- Added `tools/run-staging-command.ps1` as a dry-run allowlist guard. It checks
  project root, exact staging target `Server — kopia/1.21.11`, command shape,
  forbidden chaining/control characters, strict allowlist membership, running
  Paper/latest.log preflight, and writes credential-free audit lines.
- Added `tools/validate-staging-command-channel.ps1` to validate dry-run
  acceptance and forbidden-command rejection without approved send.
- Added `qa/OPERATOR_APPROVAL_STAGING_COMMAND_CHANNEL.md` and
  `qa/RUN_STAGING_COMMAND_MANUAL.md` for the approved manual path.
- No command was sent to staging. `/md create dungeon_ruins_001` remains
  pending until explicit one-command operator approval.
- Full runtime smoke remains blocked until MythicDungeons map creation succeeds.
- Final asset gate remains an expected failure and was not bypassed.

## 2026-05-29 Staging Console Mode

- Transport decision: interactive console launcher. A managed stdin sender was
  not added because a PID/runtime file cannot safely recover a redirected
  `StandardInput` handle from a separate PowerShell session without a durable
  broker process, and this repo has no such command broker.
- Added `tools/start-staging-console.ps1`. It confirms the project root, exact
  staging target `Server — kopia/1.21.11`, Java, and `paper.jar`; refuses
  production/live-looking paths; uses the same 2G default heap convention as
  `start-staging-qa.ps1`; and starts Paper in the foreground console.
- Added `qa/RUN_STAGING_CONSOLE_MODE.md` and updated the DR001 command-channel
  docs to distinguish background QA launch from operator console mode.
- No command was sent to staging. `/md create dungeon_ruins_001` remains
  pending until explicit one-command operator approval.
- Final asset gate remains an expected failure. Full runtime smoke remains
  blocked until MythicDungeons map creation succeeds.

## 2026-05-29 Config-Only Staging Sync Packet

- Added `staging/DR001_CONFIG_ONLY_SYNC_MANIFEST.json` for review-only,
  plugin-native config sync planning.
- Added `tools/prepare_config_only_staging_sync.ps1`; it is dry-run by default
  and requires both `-Apply` and `-ApproveConfigOnlyStagingSync` before it can
  copy config files into staging.
- Added `tools/validate_staging_preflight.mjs` to verify the manifest, staging
  scope, active namespace, excluded binary assets, and current installed state.
- DR001 remains source-only until the config-only sync is reviewed and applied.
  Runtime smoke cannot run before plugin-native staging config files exist.
- Existing staging legacy residue must not be used as DR001 runtime proof.
- Final `dr001_*` assets are still missing; the final asset gate remains an
  expected failure and was not bypassed.

## 2026-05-29 DR001 Release-Prep Migration

- Formal invoke packet
  `tasks/codex-team-packets/WP-dungeon-ruins-001-dr001-release-prep.yaml`
  produced
  `tasks/codex-team-runs/WP-dungeon-ruins-001-dr001-release-prep.json`, but the
  invoked worker could not access the workspace because its sandbox bootstrap
  failed. The repair continued directly in this session with the same accepted
  plan.
- Active player-facing identity is now **Ruiny Złamanej Korony**.
- Active public IDs now use `DR001_` for MythicMobs, drop tables, and MMOItems,
  and `dr001_` for Iris, Nexo, ModelEngine, and objective state.
- Older `dungeon_ruins_001_*` generated assets are retained as legacy scaffold
  evidence only; they are not final asset intake.
- Local MythicDungeons command syntax source used for QA docs:
  `.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/commands-and-permissions.md`.
- Added `tools/validate_references.mjs`; it passed during implementation with
  active counts: 15 mobs/bosses, 32 skills, 7 drop tables, 17 MMOItems IDs, and
  13 Nexo IDs.
- Updated `tools/validate_final_assets.mjs` to require DR001/dr001 final intake
  names and to block normal mode until intake is `approved_final` with filled
  checksums.
- Validation evidence after migration:
  - `node content\dungeons\dungeon_ruins_001\tools\validate_references.mjs`
    passed.
  - `python -c "... yaml.safe_load ..."` parsed 25 YAML files under this pack.
  - `Get-ChildItem ... *.json | ConvertFrom-Json` parsed JSON files under this
    pack.
  - `node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs`
    failed intentionally because final DR001 intake assets are pending.
  - `import_assets_to_staging.ps1 -UseFinalIntake` aborted before staging writes
    because final validation failed.
  - `import_assets_to_staging.ps1` dry-run completed and now labels old source
    assets as legacy scaffold reference, not DR001 release evidence.

## Assumptions

- `dungeon_ruins_001` is a new modular content pack and does not replace
  `dungeon_ruins_01`.
- The implementation intentionally writes only under
  `content/dungeons/dungeon_ruins_001/`.
- MythicDungeons runtime flow must be wired with the in-game editor because the
  local working examples use `Functions: []`.
- Iris specs use JSON because local Iris packs use JSON.
- MMOCore files are hook/reference placeholders until exact command/objective
  integration is verified in staging.

## Local Files Inspected

- `AGENTS.md`
- `.github/agent-knowledge/minecraft-server-docs/README.md`
- `.github/agent-knowledge/minecraft-server-docs/registry.yml`
- `Server - kopia/1.21.11/plugins/MythicDungeons/maps/dungeon_ruins_01/config.yml`
- `Server - kopia/1.21.11/plugins/MythicDungeons/maps/dungeon_ruins_01/functions.yml`
- `Server - kopia/1.21.11/plugins/MythicDungeons/maps/dungeon_ruins_01/PROGRESSION_CONTRACT.md`
- `Server - kopia/1.21.11/plugins/MythicDungeons/maps/dungeon_ruins_01/iris/pack/...`
- `Server - kopia/1.21.11/plugins/MythicMobs/mobs/DungeonRuins01Mobs.yml`
- `Server - kopia/1.21.11/plugins/MythicMobs/skills/DungeonRuins01Skills.yml`
- `Server - kopia/1.21.11/plugins/MythicMobs/droptables/DungeonRuins01Drops.yml`
- `Server - kopia/1.21.11/plugins/MMOItems/item/material.yml`
- `Server - kopia/1.21.11/plugins/MMOItems/item/axe.yml`
- `Server - kopia/1.21.11/plugins/MMOCore/exp-sources.yml`
- `Server - kopia/1.21.11/plugins/Nexo/items/nexo_defaults/nexo_furniture.yml`
- `Server - kopia/1.21.11/plugins/MythicMobs/mobs/ModelEngine_RustRaider.yml`
- `Server - kopia/1.21.11/plugins/MythicMobs/skills/ModelEngine_RustRaider_Skills.yml`

## Syntax Uncertainties

- MythicDungeons objective graph is not authored here because local examples keep
  `functions.yml` editor-owned.
- MMOCore completion and bonus reward hooks need command/event verification.
- Nexo block mechanics differ by setup; these files use local `Pack` and
  `Mechanics` casing but require staging import review.
- MMOItems custom item categories are split into source files here and must be
  imported into plugin-native `item/<type>.yml` targets.

## Validation Commands Run

- `git status --short` initially failed because the repo is blocked by Git
  dubious ownership for the `codex2` user.
- `git -c safe.directory=C:/Codex/Minecraft status --short -- content/dungeons/dungeon_ruins_001`
  reported the new pack as untracked.
- Iris JSON parse with `ConvertFrom-Json` passed for all files under
  `iris/pack`.
- Local Node was available, but `yaml` and `js-yaml` modules were not installed,
  so no full YAML parser was available.
- `rg -n "[ \t]+$" content\dungeons\dungeon_ruins_001` found no trailing
  whitespace.
- `rg -n "\t" content\dungeons\dungeon_ruins_001` found no tabs.
- `rg -n "dungeon_ruins_001_" -g "!*content/dungeons/dungeon_ruins_001/**"`
  found no existing prefixed IDs outside this pack.
- Local PowerShell cross-reference check reported no missing MythicMobs skill,
  drop, or ModelEngine model references.
- Unscoped `git diff --check` was not a valid verdict for this task because the
  repo already contains many unrelated docs-cache whitespace findings.
- Asset generation produced 15 `.schem`, 13 `.png`, 11 `.bbmodel`, 25 `.json`,
  1 `.mcmeta`, and the staging import helper.
- Node validation passed for gzip `.schem`, PNG signatures, JSON/`.mcmeta`
  parse, and required `.bbmodel` animations including boss `enrage`.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1`
  passed in dry-run mode and reported planned copies for 25 Nexo assets, 11
  ModelEngine blueprints, 1 Iris layout image, and 15 Iris schematics.
- `node content\dungeons\dungeon_ruins_001\tools\polish_assets.mjs` replaced the
  first-pass scaffold with a more detailed authored procedural asset pass.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1 -Apply`
  imported 25 Nexo resource-pack assets, 11 ModelEngine blueprints, 1 Iris layout
  image, and 15 Iris schematics into `Server - kopia/1.21.11`.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-staging-qa.ps1 -HeapBudget 2G -StopStrayFirst`
  started staging; Paper reached `Done`, Nexo generated the resource pack, and
  ModelEngine imported the `dungeon_ruins_001_*.bbmodel` files.
- Staging log review found ModelEngine `Missing hitbox` warnings for generated
  `dungeon_ruins_001_*.bbmodel` files and unrelated Iris `dungeon_ruins_01`
  dimension warnings.
- Staging file checks confirmed imported Iris schematics/layout image,
  ModelEngine blueprints, and Nexo texture/model assets.
- Full in-client particle/sound verification is documented as blocked in
  `qa/STAGING_ASSET_PLAYTEST.md` until the dungeon has a playable
  MythicDungeons editor flow and human-reviewed assets.
- Added final asset intake pipeline under `intake/`, with
  `tools/validate_final_assets.mjs` and `import_assets_to_staging.ps1
  -UseFinalIntake`. This intentionally blocks final staging import until real
  builder schematics, final Nexo assets, `.bbmodel` hitboxes, and custom `.ogg`
  files are supplied.
- Repaired the final intake gate after the invoked worker reported success
  without implementation. The validator now fails normal final mode unless
  `intake/manifest.yml` is marked `approved_final`, checksum placeholders are
  removed, and no asset remains in a candidate or review-required state.
- `node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs`
  now fails cleanly while the manifest is `hybrid_review_required`.
- `node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
  --allow-review-candidates` passes as review evidence only and warns that it
  is not valid for production or final staging import.
- `powershell -NoProfile -ExecutionPolicy Bypass -File
  .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1
  -UseFinalIntake` aborts before staging writes because normal final validation
  fails.
- `powershell -NoProfile -ExecutionPolicy Bypass -File
  .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1`
  still completes dry-run mode against scaffold source assets.
- `tools/invoke-codex-team.ps1 -WorkPacket
  tasks/codex-team-packets/WP-dungeon-ruins-001-final-assets.yaml` produced a
  run artifact at `tasks/codex-team-runs/WP-dungeon-ruins-001-final-assets.json`,
  but the invoked worker could not modify files because its local sandbox setup
  failed. The implementation was completed in this session using the same
  accepted work packet scope.

## Commands Not Run

- No production server start, stop, restart, or reload.
- No `/md create`, `/md play`, `/md reload`, `/md edit`, `/mm reload`, `/mm
  mobs info`, spawn, dungeon-flow, asset import, or live console commands.
- No `/md play`, `/md reload`, or `/md edit` staging commands were run because
  `dungeon_ruins_001` is not yet a verified playable MythicDungeons editor flow.
- No real custom `.ogg` audio was generated because only Node and Java were
  available locally; no audio encoder such as `ffmpeg` or `oggenc` was found.
- No final intake import was applied because `intake/` intentionally contains
  no real final binary assets yet.

## Hybrid Final Candidate Pass - 2026-05-29

- `node content\dungeons\dungeon_ruins_001\tools\generate_hybrid_final_assets.mjs`
  generated technical final-candidate assets under `intake/`.
- `node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
  --json` passed with 53 checked files, `approvalStatus:
  hybrid_review_required`, and `requiresHumanReview: true`.
- `powershell -NoProfile -ExecutionPolicy Bypass -File
  .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1
  -UseFinalIntake -Apply` completed after validation.
- ModelEngine originally logged `Missing hitbox` for generated blueprints. The
  generator was revised to derive `.bbmodel` files from the local
  `rust_raider.bbmodel` blueprint template while adding the required
  dungeon-specific animation aliases. Final staging logs imported all
  `dungeon_ruins_001_*.bbmodel` files without `Missing hitbox`.
- Import backups are now written under
  `Server — kopia/1.21.11/codex-backups/dungeon_ruins_001/` instead of beside
  plugin assets, preventing ModelEngine from importing backup `.bbmodel` files.
- 11 old beside-file `.bbmodel.bak-dungeon_ruins_001-*` backups created by the
  earlier import pass were removed from staging after they caused ModelEngine
  `Unknown format` warnings.
- Staging startup reached Paper `Done`; Nexo completed resource-pack
  generation; Iris detected `dungeon_ruins_001`.
- The generated `.ogg` files have valid Ogg signatures and passed repository
  validation, but they were generated without `ffmpeg`/`oggenc`; in-client
  playback must still be treated as unproven.
- No production server command was run.
- No in-client `/md play dungeon_ruins_001` QA was run because the
  MythicDungeons editor flow is not yet verified as playable in staging.

## Remaining Risks

- Runtime correctness depends on staging import and editor wiring.
- Asset placeholders must be replaced before player-visible QA.
- Balance values are placeholders and may affect economy if imported without
  tuning.

## DR001 Startup Error Fix-Only Pass - 2026-05-29

- Fixed the three diagnosed MythicMobs targeter startup errors in source:
  `RandomPlayersInRadius{r=30;amount=3}` was replaced with
  `PlayersInRadius{r=30;limit=3;sort=RANDOM}`, and both bare
  `MobsInRadius{r=10}` targeters now include explicit DR001 non-boss mob IDs.
- MythicMobs syntax source: local MythicMobs mirror
  `skills-targeters-mobsinradius.md` and `skills-targeters.md`.
- MythicDungeons `functions.yml` was normalized to `Version: 1` plus
  `Functions: []`, matching a local loaded map pattern. This is still only an
  editor-owned placeholder, not a playable dungeon graph.
- MythicDungeons load-safe state remains blocked by missing generated CLASSIC
  map files in staging: `data/`, `region/`, `level.dat`, `uid.dat`, and
  `session.lock`. Those files are generated/binary world state and were not
  faked, manually created, or copied from unrelated maps in this pass.
- DR001 difficulty levels remain review-risk because the local docs schema
  shows full level entries with `Icon`, `MobHealth`, `MobAmounts`,
  `MobDamage`, `BonusMythicLevels`, and `BonusLoot`; no difficulty rebalance or
  schema expansion was done in this startup-error fix-only pass.
- Added validators:
  `tools/validate_mythicmobs_targeters.mjs`,
  `tools/validate_mythicdungeons_map_readiness.mjs`, and
  `tools/scan_startup_log_dr001.mjs`.
- Full dungeon flow, HP phase behavior, reward integration, final assets,
  reload/restart verification, and player QA remain blocked pending separate
  approval.

## Manual Review Checklist

- Confirm every generated ID starts with `dungeon_ruins_001_`.
- Confirm no duplicate IDs in staging plugin-native files.
- Confirm MythicMobs model IDs match ModelEngine `.bbmodel` names.
- Confirm Nexo placeholder paths match the resource-pack build process.
- Confirm MMOCore reward hooks do not bypass cooldown or party rules.
- Confirm rollback is simply removing imported `dungeon_ruins_001` files from
  staging plugin folders before any production operation.
