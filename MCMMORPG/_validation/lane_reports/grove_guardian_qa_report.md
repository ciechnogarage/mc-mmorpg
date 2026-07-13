scope:
QA lane for `level_1_grove_guardian`. Final-gate audit covered current repo files, current docker runtime evidence, current validator outputs, and whether upstream lanes named the real failure family instead of outsourcing judgment.

evidence:
- Current QA verdict: FAIL.
- Art lane did name the real failure family with autonomous self-critique: `generic_golem_drift`, escalated to `REBUILD_SHELL`, and did not outsource diagnosis to the user.
- MOBS lane repaired repo binding/resource-pack parity for Grove Guardian and proved safe reload/import, but still ended BLOCKED on live reward-path warnings.
- Current command results for Grove Guardian state:
  - `modelengine:ecosystem` PASS
  - Grove Guardian-specific binding/resource-pack proof PASS
  - `modelengine:quality` FAIL: `QUALITY_MANIFEST_MISSING`
  - report validator status was initially FAIL for all three lane reports until they were rewritten into the required contract format
- Current runtime evidence remains negative for loot/reward path:
  - docker `latest.log` still shows repeated `Drop type not found` warnings for `grove_guardian.droptable.yml` and `shared.droptable.yml`
- Current art evidence remains negative for shell readiness:
  - fresh `_validation/model_reviews/level_1_grove_guardian/` renders still support the art-lane diagnosis that the model reads as a decorated humanoid/golem rather than a locked Grove Guardian shell
- There is no honest production PASS while art is `REBUILD_SHELL` and reward runtime is still broken.

files_or_areas:
- `_validation/lane_reports/grove_guardian_art_report.md`
- `_validation/lane_reports/grove_guardian_mobs_report.md`
- `_validation/lane_reports/grove_guardian_qa_report.md`
- `_validation/model_reviews/level_1_grove_guardian/`
- `plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
- `plugins/ModelEngine/resource pack/assets/modelengine/models/level_1_grove_guardian/`
- `plugins/MythicMobs/Packs/level_1/droptables/grove_guardian.droptable.yml`
- `plugins/MythicMobs/Packs/level_1/droptables/shared.droptable.yml`
- `docker/servers/world/logs/latest.log`

proposed_or_applied_changes:
- Applied: QA rewrote the lane reports into the validator contract format after the initial `collab:validate-output` failures.
- Applied: QA accepted the updated evidence that repo blueprint parity and repo-side generated model assets are now restored for Grove Guardian.
- Proposed: do not attempt release signoff until both conditions are true:
  1. art handoff reaches `PASS_TO_RUNTIME` from a real Blockbench-first rebuild on the `.bbmodel`
  2. live reward/drop warnings are cleared on the `world` backend
- Rollback note: if later experiments on droptables or blueprint rebuild regress state, roll back to the current restored blueprint hash and the current synced repo resource-pack asset directory before re-testing.

validation:
- Commands run by QA/orchestrator in the current evidence pass:
  - `npm --prefix /home/przemek/projects/MC/MCMMORPG/_validation run modelengine:render -- --model level_1_grove_guardian`
  - `npm --prefix /home/przemek/projects/MC/MCMMORPG/_validation run modelengine:check -- --model level_1_grove_guardian`
  - `npm --prefix /home/przemek/projects/MC/MCMMORPG/_validation run modelengine:quality -- --model level_1_grove_guardian`
  - `npm --prefix /home/przemek/projects/MC/MCMMORPG/_validation run modelengine:ecosystem -- --model level_1_grove_guardian`
  - `./docker/mc rcon world "mm reload"`
  - `./docker/mc rcon world "meg reload"`
  - `npm run collab:validate-output -- --agent art --file /home/przemek/projects/MC/MCMMORPG/_validation/lane_reports/grove_guardian_art_report.md --json`
  - `npm run collab:validate-output -- --agent mobs --file /home/przemek/projects/MC/MCMMORPG/_validation/lane_reports/grove_guardian_mobs_report.md`
  - `npm run collab:validate-output -- --agent qa --file /home/przemek/projects/MC/MCMMORPG/_validation/lane_reports/grove_guardian_qa_report.md`
- QA statement on autonomous self-critique: the art worker named the real failure family; this was not outsourced judgment.
- Current QA verdict: FAIL.

risks:
- False-positive release signoff risk if anyone treats repaired binding alone as sufficient while art remains `REBUILD_SHELL`.
- Economy/reward risk if Grove Guardian ships while `Drop type not found` still fires in live logs.
- Process drift risk if reports are not kept in validator contract format.

blockers:
- Primary blocker: art handoff is still `REBUILD_SHELL` because the shell remains in `generic_golem_drift`.
- Secondary blocker: runtime reward/drop integration remains broken after safe reload.
- Additional blocker: no quality manifest exists yet for Grove Guardian, so quality validation remains red.

next_owner:
- ART rebuild owner first.
- Plugin/integration owner for MythicMobs <-> MMOItems drop registration second.
- QA returns only after those two blockers produce fresh evidence.

failure_family:
generic_golem_drift: final gate is blocked first by shell-level wrong-family read; runtime reward integration failure remains a second blocker.

rebuild_or_iterate:
REBUILD_SHELL

why_not_pass:
QA cannot grant PASS because the worker did name the real failure family and that family is still active: `generic_golem_drift`. On top of that, the live reward path is still broken. Passing this now would be a false positive.
