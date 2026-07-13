# ModelEngine Boss-First Review Pipeline

Purpose: define the boss-side source of truth for strict ModelEngine review work
before QA extends `MCMMORPG/_validation`.

This document is limited to evidence proved from local MythicMobs,
ModelEngine, current review artifacts, and compiled cache data. It does not
promote any boss family into the queue without a current quality manifest,
render evidence, ecosystem proof, and runtime spawn proof.

## Current Local Queue

### Tier A: strict-ready local boss families

1. `level_1_grove_guardian`
   - mob config: `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml`
   - model blueprint: `MCMMORPG/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
   - quality manifest: `MCMMORPG/_validation/model_quality/level_1_grove_guardian.quality.json`
   - current review renders: `MCMMORPG/_validation/model_reviews/level_1_grove_guardian/`
   - compiled cache proof: `MCMMORPG/plugins/ModelEngine/.data/cache.json`
   - runtime proof: `MCMMORPG/_validation/model_reviews/level_1_grove_guardian/runtime-probe.md`
   - queue status: `strict-ready`

### Tier B: bound local support models, not boss queue members

1. `level_1_corrupted_sprout`
   - mob config: `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/corrupted_sprout.mob.yml`
   - model blueprint: `MCMMORPG/plugins/ModelEngine/blueprints/level_1_corrupted_sprout/level_1_corrupted_sprout.bbmodel`
   - compiled cache proof: `MCMMORPG/plugins/ModelEngine/.data/cache.json`
   - queue status: `support-only`
   - blocker for boss queue: no boss-quality manifest or current boss review evidence

2. `level_1_grove_ambience`
   - mob config: `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_ambience.mob.yml`
   - model blueprint: `MCMMORPG/plugins/ModelEngine/blueprints/level_1_grove_ambience/level_1_grove_ambience.bbmodel`
   - compiled cache proof: `MCMMORPG/plugins/ModelEngine/.data/cache.json`
   - queue status: `support-only`
   - blocker for boss queue: ambience helper, not a boss family

### Tier C: compiled blueprints with no proved local boss binding

1. `LR_minotaur`
2. `cerberus`
3. `chameleon`

These families are present in `MCMMORPG/plugins/ModelEngine/blueprints/` and in
`MCMMORPG/plugins/ModelEngine/.data/cache.json`, but this repo-local pass did
not find a proved local MythicMobs boss binding for them under
`MCMMORPG/plugins/MythicMobs/**`. They stay out of the active boss queue until
their local mob, skill, and reward graph is proved from current source.

## Boss Graph Contract

Strict review promotes a boss family only when this graph resolves from local
files:

1. MythicMobs mob ID -> exact `Model.Id`
2. `Model.Id` -> shipped `.bbmodel`
3. shipped `.bbmodel` -> current quality manifest hash
4. manifest `skillBindings` -> exact MythicMobs skill files
5. manifest `integrationFiles` -> exact local mob and skill files
6. compiled cache -> `model_id:*` entries for the shipped blueprint
7. runtime probe -> reload, spawn, and hidden-base-entity proof
8. death path -> on-death skills and drop table path

### Grove Guardian resolved graph

- mob ID: `level_1_grove_guardian`
- model ID: `level_1_grove_guardian`
- blueprint:
  `MCMMORPG/plugins/ModelEngine/blueprints/level_1_grove_guardian/level_1_grove_guardian.bbmodel`
- blueprint SHA-256:
  `4aa75097652c66ae02b95a470ce2ed6352f7b87bf21b285a35268004ebb3f36e`
- mob file:
  `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml`
- primary skill files:
  `MCMMORPG/plugins/MythicMobs/Packs/level_1/skills/grove_guardian.skill.yml`
  `MCMMORPG/plugins/MythicMobs/Packs/level_1/skills/grove_guardian_p2.skill.yml`
- reward file:
  `MCMMORPG/plugins/MythicMobs/Packs/level_1/droptables/grove_guardian.droptable.yml`
- state-modelpart-skill anchors proved in manifest:
  `right_impact`, `left_impact`, `thorn_origin`, `summon_origin`, `core_vfx`
- current runtime proof:
  `mm mobs spawn level_1_grove_guardian 1 l1view,0,64,0`

## Strict Review Scaffolding Rules

1. A boss family enters the queue only with `profile: "boss"` in a schema v2
   quality manifest.
2. Manifest `blueprintSha256` must match the shipped `.bbmodel`.
3. Render evidence must contain current front, side, back, three-quarter,
   silhouette, player-scale, hitbox, helpers, and representative combat-state
   renders.
4. `knownIssues` must be empty and every review score must be at least `4`.
5. Ecosystem bindings must resolve every referenced animation, bone, skill, and
   integration file.
6. Runtime proof must show reload success, spawn success, and proof that the
   base mob is visually hidden under the rendered model.
7. Reward review is not closed here; reward ownership hands off to
   `minecraft-rpg-systems` when loot contents or progression effects change.

## Deterministic 10 Percent QA Sampling Contract

The QA harness should implement sampling against the active boss queue, not the
entire corpus.

1. Build the candidate set from queue entries with status `strict-ready`.
2. Sort by `familyId` ascending.
3. Compute `sampleSize = max(1, floor(queueSize * 0.10))`.
4. Use a stable seed string:
   `<familyId>|<blueprintSha256>|<manifestPath>`
5. Hash each seed and sort by hash ascending.
6. Take the first `sampleSize` entries as the deterministic QA sample.
7. Fail the dashboard if any sampled boss loses:
   - manifest hash parity
   - render evidence existence
   - ecosystem pass
   - runtime evidence file

With the current queue size of `1`, the deterministic sample is exactly
`level_1_grove_guardian`.

## QA Dashboard And Regression Handoff

These items belong in `MCMMORPG/_validation` and should be owned by
`minecraft-release-qa`:

1. dashboard report for boss queue coverage, strict-ready count, sampled count,
   stale manifests, and missing runtime evidence
2. regression test that a strict-ready boss has a resolved local graph from mob
   file through rewards
3. regression test that the 10 percent sampling selection is deterministic for
   unchanged queue inputs
4. regression test that non-boss support models never enter the boss queue
5. regression test that compiled-but-unbound blueprints stay excluded until a
   local MythicMobs boss binding exists

## Ownership And Handoff

- `minecraft-mobs-models`
  owns queue admission, local boss/model graph truth, boss skill/model anchors,
  and blueprint/manifests under proved evidence.
- `minecraft-release-qa`
  owns `_validation` dashboarding, regression tests, sampling implementation,
  and runtime proof aggregation.
- `minecraft-rpg-systems`
  owns reward-content review when boss drop contents, progression, or balance
  effects change.
