# ModelEngine Reference Corpus

Generated from `$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)`. The corpus is read-only reference material.

## Inventory

- 6771 files, 232172760 bytes.
- 542 parsed blueprints and 542 MythicMobs YAML files.
- 914 PNG textures and 2818 OGG sounds.
- The root resource pack contains 2644 byte-identical copies of files already under `ModelEngine/resource pack`.

## Blueprint Benchmarks

| Metric | P50 | P75 | P90 | P95 | Maximum |
|---|---:|---:|---:|---:|---:|
| elements | 21 | 65 | 119 | 185 | 462 |
| bones | 7 | 19 | 37 | 57 | 200 |
| depth | 1 | 3 | 5 | 8 | 32 |
| animations | 3 | 7 | 14 | 20 | 27 |
| keyframes | 74 | 371 | 1761 | 2549 | 7858 |

## Corpus Coverage

- 450/542 blueprints have direct MythicMobs linkage.
- 197 use helper bones, 379 use named hitboxes, and 36 use timeline events.
- Maturity tiers: basic=238, production=134, developed=102, integrated=68.

## Strongest Multi-System References

| Model | Tier | Score | Elements | Bones | Animations | Helpers | Hitboxes | Timelines | YAML |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| piglin_mech_mount | integrated | 98.38 | 265 | 51 | 26 | 6 | 5 | 32 | 1 |
| piglin_mech | integrated | 97.13 | 274 | 57 | 19 | 4 | 5 | 32 | 1 |
| talus_knight | integrated | 95.14 | 168 | 37 | 17 | 5 | 12 | 20 | 1 |
| lr_hydra | integrated | 90.5 | 455 | 170 | 23 | 11 | 36 | 0 | 1 |
| lrd_yukio | integrated | 90.25 | 195 | 58 | 20 | 0 | 15 | 8 | 1 |
| lrd_witherstorm | integrated | 89 | 394 | 123 | 20 | 42 | 5 | 0 | 1 |
| lrd_direwolf | integrated | 84.07 | 136 | 107 | 22 | 39 | 1 | 4 | 1 |
| LR_minotaur | integrated | 83.22 | 112 | 34 | 23 | 1 | 1 | 55 | 1 |
| lrd_hollow_keeper | integrated | 81.93 | 243 | 59 | 22 | 23 | 1 | 4 | 1 |
| lrd_yukio_damage | production | 81.25 | 195 | 58 | 20 | 0 | 15 | 8 | 0 |
| lrd_beholder | integrated | 81 | 209 | 119 | 22 | 40 | 1 | 0 | 1 |
| littleroom | integrated | 79.2 | 376 | 64 | 19 | 3 | 1 | 7 | 2 |
| the_archivist | integrated | 78.98 | 169 | 86 | 15 | 6 | 6 | 6 | 1 |
| kazk_queen | integrated | 78.4 | 126 | 48 | 20 | 2 | 7 | 0 | 1 |
| ignevar | integrated | 76 | 440 | 128 | 24 | 2 | 0 | 6 | 1 |
| oog | integrated | 74.4 | 126 | 24 | 27 | 0 | 1 | 11 | 2 |
| lrd_kraken | integrated | 72.99 | 277 | 200 | 11 | 58 | 2 | 1 | 1 |
| medusa_2 | integrated | 72 | 174 | 101 | 24 | 2 | 1 | 0 | 1 |
| kobold_assassin_2 | production | 70.5 | 248 | 80 | 23 | 3 | 0 | 0 | 1 |
| voras | production | 69.63 | 215 | 37 | 21 | 0 | 1 | 0 | 2 |
| cog_sentinel | integrated | 69.31 | 279 | 21 | 16 | 5 | 0 | 43 | 1 |
| xarnoth | integrated | 68.87 | 305 | 97 | 15 | 15 | 1 | 0 | 1 |
| korrvag_8seg | integrated | 68.4 | 239 | 110 | 16 | 9 | 8 | 0 | 1 |
| lr_owlbear | integrated | 65.63 | 150 | 45 | 16 | 2 | 2 | 0 | 1 |
| lrd_piglin_king | integrated | 65.33 | 122 | 63 | 18 | 2 | 5 | 0 | 1 |

## Reusable Vocabulary

- Animation roles: other=1022, idle=507, locomotion=405, attack=363, transition=211, death=181, reaction=88.
- Frequent animation names: idle (401), walk (247), death (161), spawn (114), attack_# (51), land (43), attack# (40), run (31), hit_seg_# (28), stun (27), attack (23), stun_hit (19), jump (19), bite (18), cast_# (17), punch_# (16), shoot (14), swing_# (14), hit_# (14), awaken (13), feed (13), stab (13), leap (12), idle_surface (12), turn_left (12).
- Frequent bone names: hitbox (376),  (309), h_head (142), tail# (109), tentacle_# (108), tentacle_#_# (95), body (92), left_leg (88), right_leg (87), left_arm (84), tl_tentacle_# (84), right_arm (82), left_forearm (67), leg# (67), right_forearm (65), segment_#_left_leg_# (63), segment_#_left_leg_#_# (63), segment_#_right_leg_# (63), segment_#_right_leg_#_# (63), left_foot (62), right_foot (62), torso (62), tail_# (61), tentacle_#_#_cube# (58), left_hand (55).
- Frequent MythicMobs mechanics: skill (4620), delay (3126), sound (2246), changepart (1871), state (1793), effect:particles (1624), model (1174), varequals (973), gcd (933), offgcd (839), setvar (824), setspeed (700), summon (596), totem (559), partvis (553), remove (511), throw (484), stance (480), setstance (461), lockmodel (439), e:p (438), recoil (431), brightness (392), setai (366), damage (357).
- Frequent triggers: onspawn (2026), ontimer (1610), onload (733), ondamaged (322), oninteract (275), ondeath (222), onattack (163), onuse (74), onsignal (65), onpickup (23), onentercombat (13), onshoot (9), ondropcombat (9), onunheld (6), onswing (5), onhold (5), onpressq (3), onkill (2), onheld (2), ondespawn (1).

## Ecosystem Patterns

- Treat a mob as a synchronized model, behavior, collision, sound, VFX, item, and resource-pack system.
- Design gameplay helper bones while designing the rig: moving hit origins, projectile targets, VFX anchors, sub-hitboxes, detachable parts, and gaze targets.
- Attach melee damage windows to animated model parts; a static radius around the owner does not prove contact.
- Align damage, sound, particles, recoil, brightness, and part changes to explicit animation ticks.
- Use GCD, AI control, model rotation locks, body clamps, stances, and variables to preserve anticipation, impact, recovery, and phase state.
- Use default-state replacement for passive, awakened, locomotion, stun, flight, and phase modes instead of one permanent idle.
- Use changepart and part visibility only when damage, transformation, equipment, or dismemberment is part of the design.
- Use emissive maps, brightness, and tint as readability layers after silhouette and material separation are already strong.
- Set complexity budgets by archetype and gameplay role; corpus percentiles are diagnostics, not universal cube quotas.
- Validate the complete reference graph and current visual/runtime evidence before accepting a mob.

## High-Value References

- **dreadroot**: 71 elements, 34 bones, depth 5, 8 animations, 1176 keyframes. Segmented roots and three long articulated arm chains create identity; passive-to-awaken state replacement, moving model-part hit volumes, and layered sound sell scale.
- **golem_king**: 75 elements, 41 bones, depth 6, 17 animations, 1875 keyframes. Purposeful asymmetry, articulated fingers, OBB interaction bones, stun/run state replacement, and destruction effects turn geometry into gameplay.
- **lr_hydra**: 455 elements, 170 bones, depth 10, 23 animations, 3963 keyframes. Repeated neck chains, per-head target bones, local bite volumes, dismemberment through part replacement, and head-specific attacks support a multi-part boss.
- **ignevar**: 440 elements, 128 bones, depth 32, 24 animations, 4696 keyframes. Deep chains, target helpers, local stomp and breath origins, brightness, tint, and layered audio create readable elemental attacks.
- **lrd_hollow_keeper**: 243 elements, 59 bones, depth 8, 22 animations, 2933 keyframes. Weapon, foot, projectile, and gib bones are gameplay anchors; sleep/wake transitions and attack-specific moving volumes synchronize the encounter.
- **lr_minotaur**: 112 elements, 34 bones, depth 4, 23 animations, 5052 keyframes. Dense full-body kinetic chains, timeline events, alternate locomotion states, and anticipation/recovery produce weight.
- **lrd_direwolf**: 136 elements, 107 bones, depth 13, 22 animations, 3021 keyframes. Quadruped hierarchy, secondary chains, multiple attack families, and variant textures demonstrate reusable anatomy without a humanoid template.
- **boreal_druid_mammoth**: 115 elements, 52 bones, depth 7, 11 animations, 2041 keyframes. Large quadruped mass, trunk and accessory chains, helper effects, and transformation support show how silhouette and phase mechanics cooperate.

## Reference Integrity

- 457 distinct model IDs are referenced by MythicMobs.
- 8 references do not resolve directly to a blueprint basename; these include dynamic placeholders or incomplete packs and must not become project assumptions.
- 5 model-part references could not be resolved by the conservative cross-pack mapper.
- 43 state references could not be resolved to an animation by the conservative mapper.

The machine-readable full inventory and relationship graph is stored in `MCMMORPG/_validation/reference_corpus/modelengine-reference-corpus.json`.
