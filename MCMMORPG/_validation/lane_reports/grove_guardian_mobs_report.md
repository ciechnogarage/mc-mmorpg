scope:
MOBS lane for `level_1_grove_guardian`. Audited the runtime contract as one chain: MythicMobs mob YAML -> skill/droptable YAML -> ModelEngine blueprint source -> compiled cache/resource-pack assets -> safe reload proof on the live `world` docker backend. No restart or kill was used.

evidence:
- `plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml` declares `Model.Id: level_1_grove_guardian` and uses the expected guardian skill hooks.
- Repo and live blueprint now both exist and match SHA-256 `ac28278e56e7aecf669b0b8849436798bf4108d51c57ad86c38ec5a59d51877f`.
- Repo-side generated ModelEngine assets were missing for Grove Guardian before repair.
- After syncing generated assets from the live world tree back into the repo tree, targeted guardian proof passes: `rendered_bones=36`, `cache_entries=68`, `json_assets=68`.
- `modelengine:check` now reports PASS for Grove Guardian specifically, even though the whole command still exits non-zero because unrelated `level_1_briarwolf` is broken.
- Safe runtime reload proof succeeded:
  - `./docker/mc rcon world "mm reload"` -> Mythic reload finished
  - `./docker/mc rcon world "meg reload"` -> completed successfully
  - latest live log shows fresh import of `level_1_grove_guardian.bbmodel`
  - `mm mobs list` includes `level_1_grove_guardian`
- Art handoff did show autonomous self-critique: the art lane explicitly named `generic_golem_drift`, rejected outsourced judgment, and escalated to `REBUILD_SHELL`.
- Reward path remains broken after live reload:
  - docker `latest.log` still emits repeated `Drop type not found` warnings for `grove_guardian.droptable.yml` and `shared.droptable.yml`
  - warnings reproduced with both `mmoitems{...}` and `mmoitem{...}` syntax attempts
- MMOItems source IDs do exist in repo item files, so the loot failure points at MythicMobs<->MMOItems drop registration/integration rather than missing Grove Guardian item definitions.
- `modelengine:quality -- --model level_1_grove_guardian` still fails `QUALITY_MANIFEST_MISSING`.

files_or_areas:
- `plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml`
- `plugins/MythicMobs/Packs/level_1/skills/grove_guardian.skill.yml`
- `plugins/MythicMobs/Packs/level_1/droptables/grove_guardian.droptable.yml`
- `plugins/MythicMobs/Packs/level_1/droptables/shared.droptable.yml`
- `plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
- `plugins/ModelEngine/resource pack/assets/modelengine/models/level_1_grove_guardian/`
- `docker/servers/world/logs/latest.log`

proposed_or_applied_changes:
- Applied: synced repo-side generated ModelEngine model JSON assets for `level_1_grove_guardian` so the repo binding/resource-pack chain matches the live world output.
- Applied: attempted drop syntax repair from `mmoitems{...}` to `mmoitem{...}` in Grove Guardian and shared level-1 droptables, synced those files into the live world tree, and reloaded MythicMobs.
- Result of the loot patch attempt: still blocked. Runtime keeps logging `Drop type not found`, so this is not a Grove Guardian item-ID typo.
- Rollback note: if the MMOItems drop integration owner wants a clean rollback point, revert `grove_guardian.droptable.yml` and `shared.droptable.yml` to their pre-`mmoitem` syntax baseline before the next compatibility experiment.

validation:
- Commands run:
  - `npm --prefix $MC_ROOT/MCMMORPG/_validation run modelengine:check -- --model level_1_grove_guardian`
  - `npm --prefix $MC_ROOT/MCMMORPG/_validation run modelengine:ecosystem -- --model level_1_grove_guardian`
  - `npm --prefix $MC_ROOT/MCMMORPG/_validation run modelengine:quality -- --model level_1_grove_guardian`
  - `sha256sum plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel docker/servers/world/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
  - `./docker/mc rcon world "mm reload"`
  - `./docker/mc rcon world "meg reload"`
  - `./docker/mc rcon world "mm mobs list" | grep -i 'level_1_grove_guardian\|grove_guardian' || true`
- Results:
  - `modelengine:ecosystem` PASS
  - Grove Guardian-specific binding/resource-pack proof PASS
  - live reload/import proof PASS
  - reward/drop runtime proof FAIL because `Drop type not found` persists
  - quality FAIL because manifest is missing
- MOBS lane verdict: BLOCKED.

risks:
- Boss reward/economy path is untrustworthy while live drop warnings persist.
- Global `modelengine:check` remains noisy because of unrelated `level_1_briarwolf`; do not misread that as a Grove Guardian regression.
- Accepting downstream release with a red reward path would ship a broken death/reward contract.

blockers:
- Runtime loot integration blocker between MythicMobs and MMOItems drop registration on the live `world` backend.
- Missing quality manifest for Grove Guardian keeps quality validation red.
- Art lane is still `REBUILD_SHELL`, so the model is not in a valid downstream art handoff state anyway.

next_owner:
- Primary: plugin/integration owner for MythicMobs <-> MMOItems drop registration on the `world` backend.
- Secondary: art rebuild owner, then QA after a clean art handoff and restored quality artifact.

failure_family:
not_applicable: mobs lane repaired binding/resource-pack parity, but the remaining blocker is runtime drop integration plus upstream art/quality gating.

rebuild_or_iterate:
BLOCKED

why_not_pass:
I cannot pass this lane because the reward path still fails live with `Drop type not found` after safe reload, and the accepted art handoff is not production-ready. Binding/resource-pack parity was repaired, but the encounter's death/reward contract and upstream quality/art gates are still broken.