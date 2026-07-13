# ModelEngine Learning Ledger

Generated from all 6771 files in `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)`. Regenerate after adding reference material.

## Evidence-Backed Lessons

- 450/542 blueprints have a direct model mechanic in a MythicMobs mob file; 451/542 appear anywhere in MythicMobs YAML. Supporting references are not runtime proof.
- 197/542 blueprints contain empty helper bones and 379/542 contain named hitbox or interaction bones; gameplay anchors are common but not universal.
- 36/542 blueprints contain timeline keyframes; explicit synchronization should be copied only from references that actually use it.
- Animation roles across the corpus: other=1022, idle=507, locomotion=405, attack=363, transition=211, death=181, reaction=88.
- Maturity tiers based on geometry, rig, animation, helpers, hitboxes, timelines, and YAML linkage: basic=238, production=134, developed=102, integrated=68.
- A strong reference must score across several systems. High cube or keyframe count alone is not evidence of better design.
- Select references by anatomy and encounter role before borrowing structure; quadruped, humanoid, multipart, flying, and prop rigs solve different problems.
- For every new model, record which reference supplied silhouette, rig, locomotion, attack timing, hit volume, VFX anchor, sound layering, and state-machine ideas.
- Every borrowed pattern needs a visible or runtime acceptance check; corpus similarity is not completion.

## Production Contract

- Start with a written silhouette target and choose references with matching anatomy and encounter role.
- Record reference provenance separately for silhouette, rig, locomotion, attacks, hit volumes, VFX, sound, and states.
- Require readable hierarchy, purposeful helper bones, local attack volumes, explicit impact timing, and complete state coverage where the encounter needs them.
- Compare the current render against selected references at the same camera distance before polishing details.
- Reject an iteration that adds complexity without improving silhouette, motion weight, combat readability, or runtime integration.
- Close only with a current render, validated model-skill-asset graph, compiled resource pack, and live spawn evidence.

## Selection Rules

- Use the machine-ranked references as candidates, not automatic templates.
- Inspect the full model and linked YAML before copying a pattern.
- Prefer several specialized references over one model used for every subsystem.
- Treat unresolved or unlinked assets as incomplete evidence.

## Top Reference Candidates

| Model | Tier | Score | Main evidence |
|---|---|---:|---|
| piglin_mech_mount | integrated | 98.38 | 265 elements, 51 bones, 26 animations, 6 helpers, 5 hitboxes, 32 timeline events, 1 linked YAML |
| piglin_mech | integrated | 97.13 | 274 elements, 57 bones, 19 animations, 4 helpers, 5 hitboxes, 32 timeline events, 1 linked YAML |
| talus_knight | integrated | 95.14 | 168 elements, 37 bones, 17 animations, 5 helpers, 12 hitboxes, 20 timeline events, 1 linked YAML |
| lr_hydra | integrated | 90.5 | 455 elements, 170 bones, 23 animations, 11 helpers, 36 hitboxes, 0 timeline events, 1 linked YAML |
| lrd_yukio | integrated | 90.25 | 195 elements, 58 bones, 20 animations, 0 helpers, 15 hitboxes, 8 timeline events, 1 linked YAML |
| lrd_witherstorm | integrated | 89 | 394 elements, 123 bones, 20 animations, 42 helpers, 5 hitboxes, 0 timeline events, 1 linked YAML |
| lrd_direwolf | integrated | 84.07 | 136 elements, 107 bones, 22 animations, 39 helpers, 1 hitboxes, 4 timeline events, 1 linked YAML |
| LR_minotaur | integrated | 83.22 | 112 elements, 34 bones, 23 animations, 1 helpers, 1 hitboxes, 55 timeline events, 1 linked YAML |
| lrd_hollow_keeper | integrated | 81.93 | 243 elements, 59 bones, 22 animations, 23 helpers, 1 hitboxes, 4 timeline events, 1 linked YAML |
| lrd_yukio_damage | production | 81.25 | 195 elements, 58 bones, 20 animations, 0 helpers, 15 hitboxes, 8 timeline events, 0 linked YAML |
| lrd_beholder | integrated | 81 | 209 elements, 119 bones, 22 animations, 40 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| littleroom | integrated | 79.2 | 376 elements, 64 bones, 19 animations, 3 helpers, 1 hitboxes, 7 timeline events, 2 linked YAML |
| the_archivist | integrated | 78.98 | 169 elements, 86 bones, 15 animations, 6 helpers, 6 hitboxes, 6 timeline events, 1 linked YAML |
| kazk_queen | integrated | 78.4 | 126 elements, 48 bones, 20 animations, 2 helpers, 7 hitboxes, 0 timeline events, 1 linked YAML |
| ignevar | integrated | 76 | 440 elements, 128 bones, 24 animations, 2 helpers, 0 hitboxes, 6 timeline events, 1 linked YAML |
| oog | integrated | 74.4 | 126 elements, 24 bones, 27 animations, 0 helpers, 1 hitboxes, 11 timeline events, 2 linked YAML |
| lrd_kraken | integrated | 72.99 | 277 elements, 200 bones, 11 animations, 58 helpers, 2 hitboxes, 1 timeline events, 1 linked YAML |
| medusa_2 | integrated | 72 | 174 elements, 101 bones, 24 animations, 2 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| kobold_assassin_2 | production | 70.5 | 248 elements, 80 bones, 23 animations, 3 helpers, 0 hitboxes, 0 timeline events, 1 linked YAML |
| voras | production | 69.63 | 215 elements, 37 bones, 21 animations, 0 helpers, 1 hitboxes, 0 timeline events, 2 linked YAML |
| cog_sentinel | integrated | 69.31 | 279 elements, 21 bones, 16 animations, 5 helpers, 0 hitboxes, 43 timeline events, 1 linked YAML |
| xarnoth | integrated | 68.87 | 305 elements, 97 bones, 15 animations, 15 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| korrvag_8seg | integrated | 68.4 | 239 elements, 110 bones, 16 animations, 9 helpers, 8 hitboxes, 0 timeline events, 1 linked YAML |
| lr_owlbear | integrated | 65.63 | 150 elements, 45 bones, 16 animations, 2 helpers, 2 hitboxes, 0 timeline events, 1 linked YAML |
| lrd_piglin_king | integrated | 65.33 | 122 elements, 63 bones, 18 animations, 2 helpers, 5 hitboxes, 0 timeline events, 1 linked YAML |
| boreal_druid_mammoth | integrated | 64.77 | 115 elements, 52 bones, 11 animations, 2 helpers, 6 hitboxes, 4 timeline events, 1 linked YAML |
| eldric | integrated | 63.62 | 395 elements, 75 bones, 13 animations, 5 helpers, 1 hitboxes, 6 timeline events, 1 linked YAML |
| parasitic_abomination | integrated | 63.5 | 205 elements, 74 bones, 13 animations, 2 helpers, 1 hitboxes, 2 timeline events, 1 linked YAML |
| golem_king | production | 62.63 | 75 elements, 41 bones, 17 animations, 0 helpers, 7 hitboxes, 0 timeline events, 1 linked YAML |
| korrvag_7seg | integrated | 62.59 | 215 elements, 98 bones, 13 animations, 8 helpers, 7 hitboxes, 0 timeline events, 1 linked YAML |
| ork_nil | integrated | 61.26 | 122 elements, 41 bones, 13 animations, 0 helpers, 1 hitboxes, 4 timeline events, 1 linked YAML |
| dragon_sentinel_dragonform | production | 61.14 | 186 elements, 56 bones, 7 animations, 0 helpers, 11 hitboxes, 0 timeline events, 1 linked YAML |
| littleroom_gokart | integrated | 60.76 | 186 elements, 63 bones, 22 animations, 17 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| succubus | production | 60.42 | 72 elements, 37 bones, 21 animations, 0 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| korrvag_6seg | integrated | 59.85 | 191 elements, 86 bones, 12 animations, 7 helpers, 6 hitboxes, 0 timeline events, 2 linked YAML |
| kriger | production | 58.64 | 109 elements, 31 bones, 16 animations, 1 helpers, 1 hitboxes, 0 timeline events, 1 linked YAML |
| boreal_druid | integrated | 58.59 | 119 elements, 57 bones, 8 animations, 2 helpers, 1 hitboxes, 12 timeline events, 1 linked YAML |
| kobold_assassin | production | 58.38 | 131 elements, 33 bones, 19 animations, 0 helpers, 1 hitboxes, 0 timeline events, 2 linked YAML |
| lrd_horse | integrated | 57.32 | 45 elements, 34 bones, 19 animations, 6 helpers, 1 hitboxes, 7 timeline events, 1 linked YAML |
| azriel | production | 57.08 | 123 elements, 39 bones, 12 animations, 0 helpers, 1 hitboxes, 0 timeline events, 2 linked YAML |

The full per-model profiles and vocabularies are stored in `MCMMORPG/_validation/reference_corpus/modelengine-reference-corpus.json`.
