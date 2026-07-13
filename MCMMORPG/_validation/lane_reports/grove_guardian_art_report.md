scope:
ART lane for `level_1_grove_guardian`. Source-of-truth audit covered the repo `.bbmodel`, the live docker `.bbmodel`, fresh `_validation` renders, and current model validators. Goal of this pass: diagnose shell/read honestly, restore missing repo source if absent, and decide whether the model deserves `ITERATE_LOCAL`, `REBUILD_SHELL`, or a pass bucket.

evidence:
- Repo blueprint was missing at the start; live docker blueprint existed at `docker/servers/world/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`.
- Repo blueprint was restored from the live docker copy and both files now share SHA-256 `ac28278e56e7aecf669b0b8849436798bf4108d51c57ad86c38ec5a59d51877f`.
- Blueprint structure after restore: `elements=73`, `groups=42`, `animations=11`, top outliner groups `body`, `hitbox`.
- Fresh render evidence was generated under `_validation/model_reviews/level_1_grove_guardian/`, including `front.png`, `side.png`, `back.png`, `three_quarter.png`, `silhouette.png`, `player_scale.png`, and multiple animation stills.
- Fresh visual read from those renders: the crown is distinctive, but the body under it still reads as a decorated humanoid idol/golem. Side view compresses into torso block + hanging arms + squat support legs. The identity depends too much on bark/stone clutter rescuing a familiar humanoid base.
- Autonomous self-critique result: named failure family is `generic_golem_drift` with visible `detail_rescuing_broken_shell` behavior. This is a shell/read failure, not a user-diagnosis request.
- Current validator state during the art audit:
  - render: PASS (`MODELENGINE_RENDER_PASS`)
  - quality: FAIL (`QUALITY_MANIFEST_MISSING`)
  - binding before repo resource-pack sync: FAIL on missing generated model assets in repo
  - strict review author script: unusable as a pass artifact in current state because its supporting manifest/runtime evidence chain is missing or stale.

files_or_areas:
- `plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
- `docker/servers/world/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
- `_validation/model_reviews/level_1_grove_guardian/`
- `_validation/render_bbmodel_review.js`
- `_validation/check_modelengine_quality.js`
- `_validation/check_modelengine_binding.js`
- `_validation/lane_reports/grove_guardian_art_report.md`

proposed_or_applied_changes:
- Applied: restored the missing repo blueprint from the live docker source.
- Applied: regenerated fresh render evidence for the restored blueprint hash.
- Not applied: no geometry micro-iteration was committed, because this is not a one-axis polish case.
- Required next repair path: rebuild the shell from a locked reference packet for rooted-grove-guardian massing while preserving only validated downstream contracts such as animation IDs and useful helper bones.
- Rollback note: the restored live blueprint copy is the current rollback baseline; do not overwrite it during rebuild without first saving a backup snapshot of the `.bbmodel` and the current review renders.

validation:
- Commands run:
  - `sha256sum plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel docker/servers/world/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
  - `npm --prefix /home/przemek/projects/MC/MCMMORPG/_validation run modelengine:render -- --model level_1_grove_guardian`
  - `node /home/przemek/projects/MC/MCMMORPG/_validation/check_modelengine_quality.js --model level_1_grove_guardian`
  - `node /home/przemek/projects/MC/MCMMORPG/_validation/check_modelengine_binding.js --model level_1_grove_guardian`
- Results:
  - render PASS
  - quality FAIL: `QUALITY_MANIFEST_MISSING`
  - binding was red during the art pass before repo resource-pack sync
- Art lane verdict: FAIL for current shell/read.

risks:
- Micro-polish would launder the same generic humanoid shell into a slightly different decorated golem instead of fixing the family read.
- Any numeric-only shell rewrite claimed as final would violate the Blockbench-first production rule for creature art.
- Missing quality-manifest evidence makes it too easy to overstate improvement.

blockers:
- Shell-level family failure remains visible: `generic_golem_drift`.
- Current production rule requires Blockbench-first final art on the real `.bbmodel`; that final rebuild review has not happened.
- `_validation/model_quality/level_1_grove_guardian.quality.json` is missing.

next_owner:
- ART shell rebuild owner first.
- MOBS and QA may only accept a later handoff once the art lane reaches `PASS_TO_RUNTIME` with current evidence.

failure_family:
generic_golem_drift: shell still reads as a decorated humanoid idol/golem; detail_rescuing_broken_shell remains visible.

rebuild_or_iterate:
REBUILD_SHELL

why_not_pass:
This cannot pass because the surviving diagnosis is shell-level: the model still reads as a generic decorated golem rather than a locked Grove Guardian family read. `ITERATE_LOCAL` would be process theater here because the failure is not one axis; it is a direct rebuild trigger tied to wrong-family massing plus an incomplete review artifact chain.

iteration_log:
baseline: restored the missing repo blueprint and regenerated fresh render evidence for the shipped hash.
iteration 1: not executed. Changed axis: none. Improvement: none. Still broken: generic_golem_drift survives in front/side/three-quarter/player-scale read, so local polish was rejected and escalated to REBUILD_SHELL immediately.