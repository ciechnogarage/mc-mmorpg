# ModelEngine Binding Remediation

Status: pending runtime proof  
Scope: active MythicMobs model references reachable from the vertical slice

## Evidence chain

For every candidate model, verify in order:

`MythicMobs mob YAML -> Model.Id -> ModelEngine blueprint -> generated resource-pack asset -> cache model_id:* -> live spawn -> animation -> full restart/reload`

## Current static findings

| model_id | referenced_by_mob_id | referenced_by_spawn_or_dungeon | mandatory_for_vertical_slice | priority | blueprint_path | generated_asset_status | cache_status | live_spawn_status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `level_1_briarwolf` | `level_1_briarwolf` | to be confirmed from active spawn graph | unknown | P0/P1 pending graph | missing | missing | not_checked | not_tested |
| `level_1_glimmer_wisp` | `level_1_glimmer_wisp` | to be confirmed from active spawn graph | unknown | P0/P1 pending graph | present/verify exact path | incomplete | stale_or_incomplete | not_tested |
| `level_1_moss_stalker` | `level_1_moss_stalker` | to be confirmed from active spawn graph | unknown | P0/P1 pending graph | present/verify exact path | incomplete | stale_or_incomplete | not_tested |
| `level_1_corrupted_sprout` | `level_1_corrupted_sprout` | active content reference | unknown | P0/P1 pending graph | present | generated | not_checked | not_tested |
| `level_1_grove_ambience` | `level_1_grove_ambience` | active content reference | no/unknown | P1/P2 pending graph | present | generated | not_checked | not_tested |
| `level_1_grove_guardian` | `level_1_grove_guardian` | active content reference | unknown | P0/P1 pending graph | present | generated | not_checked | not_tested |

## Triage rule

Priority is derived from the dependency graph of the mandatory vertical slice,
not from the existence of a validator failure alone.

`briarwolf` is currently classified as an authored-source/blueprint gap. The
`glimmer_wisp` and `moss_stalker` findings are treated as generation/cache gaps
until their authored source and exact blueprint paths are checked. These are
not one shared "ModelEngine is broken" failure.

- P0: model is required to enter, complete, or understand the mandatory region,
  elite encounter, dungeon or boss.
- P1: model is active content but not required for the mandatory slice.
- P2: model is optional, archived, or outside the current MVP path.

The dependency classification must cite the dungeon map, mob spawn route or
quest/content entry that makes the model reachable.

## Remediation procedure

1. Confirm the model reference in the active mob YAML.
2. Confirm exact blueprint ID and `.bbmodel` location.
3. Determine whether the model belongs to tracked authored content or excluded
   vendor content.
4. Regenerate the model/resource-pack output using the supported ModelEngine
   pipeline; do not edit IDs merely to satisfy the checker.
5. Rebuild or refresh cache entries.
6. Run `check_modelengine_binding.js` and, where applicable,
   `check_modelengine_ecosystem.js`.
7. Spawn the mob on the designated `world` backend and verify model visibility,
   animation, damage hooks and despawn.
8. Reload and fully restart the backend, then repeat the spawn check.
9. Store logs, screenshots/render evidence where required, hashes and verdict in
   `runtime-evidence-register.yaml`.
