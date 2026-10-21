# Blockbench + ModelEngine Specialist Playbook

Status: repo playbook  
Scope: agent and human workflow for Blockbench art quality, ModelEngine v4 integration, MythicMobs hookup, Nexo/resource-pack delivery, and staging QA evidence  
Use with: `docs/assets/blockbench_model_standard.md`

## 1. Purpose

The repo standard defines the baseline. This playbook defines when an agent can
be treated as a real Blockbench + ModelEngine specialist rather than a file
generator.

A specialist must be able to:

- build a new model from zero instead of only patching an old `.bbmodel`;
- justify silhouette, scale, pivots, UV, and animation choices;
- reject a model that imports technically but fails art, gameplay, or
  performance review;
- produce evidence, not only files.

## 2. Source-of-Truth Order

Before any technical decision, use sources in this order:

1. `docs/assets/blockbench_model_standard.md`
2. local plugin docs mirrors, starting with `GOLDEN_RULES.md`, then
   `wiki/INDEX.md`, `wiki/md/`, generated config, and `local-notes/`:
   `.github/agent-knowledge/minecraft-server-docs/modelengine/`
   `.github/agent-knowledge/minecraft-server-docs/mythicmobs/`
   `.github/agent-knowledge/minecraft-server-docs/nexo/`
3. local project contracts such as:
   `content/dungeons/dungeon_ruins_001/modelengine/models.md`
   `content/dungeons/dungeon_ruins_001/modelengine/animation_requirements.md`
   `content/dungeons/dungeon_ruins_001/README_ASSETS.md`
4. official Blockbench wiki:
   `https://www.blockbench.net/wiki/`
   `https://www.blockbench.net/wiki/guides/minecraft-style-guide/`
   `https://www.blockbench.net/wiki/docs/bbmodel/`
5. Context7 or verified official docs for third-party behavior only if the local
   mirror is missing, stale, or ambiguous.

Do not guess ModelEngine, MythicMobs, or Blockbench behavior from memory when a
source is available locally.

## 3. Mastery Path

### Level 1: Blockbench Basic

The agent must understand:

- viewport, grid, and north-facing orientation;
- Edit, Paint, and Animate modes;
- outliner groups/bones versus cubes;
- pivots and parent-child hierarchy;
- local textures, UV layout, and `.bbmodel` save flow.

Pass bar:

- can explain why `root`, body bones, and hitbox helpers exist;
- can inspect a `.bbmodel` and spot missing texture links or random bone names.

### Level 2: Minecraft Art Quality

The agent must review:

- silhouette readability without texture;
- proportions from player combat distance;
- element count versus visible value;
- UV ratio and pixel density consistency;
- material separation and palette readability;
- Minecraft-style failure modes: mixels, banding, pillow shading, noise,
  overbuilt curves, and over-detailed micro-cubes.

Pass bar:

- can reject a model that is technically valid but visually weak;
- can explain why a texture or shape reads badly in Minecraft.

### Level 3: ModelEngine Technical Model

The agent must produce:

- Generic Model source with stable `model_id`;
- valid root hierarchy and named bones;
- `hitbox` setup and optional `shadow`;
- pivot placement that supports animation;
- reasonable cube/bone budget for expected runtime density.

Pass bar:

- can explain import-facing requirements from the local ModelEngine docs;
- can diagnose why a model would load with wrong eye height, bad hitbox, or
  broken forward direction.

### Level 4: Animation + Gameplay

The agent must define:

- required animation set for the asset type;
- loop, once, and hold intent;
- damage frame for each attack state;
- clear tells for slam, cleave, cast, enrage, stagger, or death.

Pass bar:

- can map encounter events to ModelEngine states and MythicMobs triggers;
- can explain how attack timing stays fair to the player.

### Level 5: Runtime Integration

The agent must connect:

- Blockbench source to ModelEngine blueprint/import flow;
- ModelEngine resource pack output to the project pack path;
- MythicMobs mob and skill config to the model states;
- Nexo/resource-pack delivery when relevant to the asset type.

Pass bar:

- can state which local files and runtime systems own the final asset;
- can diagnose import, pack, and state-machine failures separately.

### Level 6: QA / Specialist Review

The agent must be able to run or prepare:

- Blockbench visual review;
- staging smoke checklist;
- `latest.log` review;
- screenshot evidence collection;
- performance review and reject decision.

Pass bar:

- does not approve on technical import alone;
- classifies every failure as art, Blockbench format, ModelEngine import,
  MythicMobs/runtime, pack delivery, or gameplay/performance.

## 4. Art Review Rubric

Every non-trivial model should be reviewed with the rubric below.

| Area | Pass question | Fail examples |
| --- | --- | --- |
| Silhouette | Can a player identify the mob or prop from distance before reading texture detail? | Blob torso, weapon merges into body, limbs disappear into armor mass |
| Proportions | Do body mass, head size, weapon size, and stance sell the role? | Tiny boss head, unreadable hands, elite mob scaled like prop |
| Element count | Are cubes spent on visible form instead of noise? | Too many micro-cubes for trim nobody sees in combat |
| UV ratio | Is pixel density consistent and unstretched? | Squashed faces, accidental stretched trim |
| Material readability | Do stone, metal, cloth, bone, wood, void, moss read as separate materials? | One muddy value band for everything |
| Palette | Does color support biome, faction, and gameplay readability? | Accent color everywhere, no focal area |
| Shading | Does shading describe form instead of fighting it? | Banding, pillow shading, random noisy dithering |
| Minecraft style | Does the model stay blocky and economical? | Fake smooth curves made from many tiny rotated cubes |

Reject if the model only looks good in close-up orthographic view and not from
real combat distance.

## 5. Training Exercises

### Exercise 1: Static Prop

Build a one-block stone totem.

Goal:

- scale, UV, texture hookup, no mixels, clean import prep.

Deliverables:

- `.bbmodel`
- local texture
- README from template
- review report

Pass only if:

- the prop reads as stone from distance;
- no random cube names remain;
- evidence includes Blockbench screenshot.

### Exercise 2: Small Trash Mob

Build a 1.8-2.2 block mob with:

- `idle`
- `walk`
- `attack_01`
- `death`

Goal:

- pivots, parent hierarchy, basic hitbox, readable attack.

Pass only if:

- damage frame is documented;
- hitbox is not obviously unfair;
- in-game screenshot exists after spawn smoke.

### Exercise 3: Elite Mob

Build a 2.5-3 block elite mob with two attacks or one attack plus one cast.

Goal:

- readable silhouette, better material breakup, state sync with MythicMobs.

Pass only if:

- attack tells are distinct;
- the second state is not a copy-paste timing clone;
- README maps states to skills.

### Exercise 4: Boss Mini-Encounter

Build a new boss from zero. Do not use `grove_guardian` as a quality target.

Goal:

- bespoke silhouette, phase states, `enrage`, `cast`, and `special_01`;
- ModelEngine + MythicMobs + pack delivery path documented.

Pass only if:

- the agent can defend the visual direction;
- `spawn`, `death`, and at least one phase tell are clearly documented;
- staging smoke plan includes log review and reconnect check.

### Exercise 5: Failure Drill

Deliberately diagnose:

- missing texture;
- wrong `model_id`;
- missing or non-playing animation;
- oversized or misleading hitbox;
- bad pivot placement;
- pack not visible on client reconnect.

Pass only if:

- each issue is classified and paired with the next concrete debugging step.

## 6. Quality Gates

### Gate 1: Blockbench Visual Pass

Required:

- model visible;
- texture visible;
- animation list visible when relevant;
- no `cube1`, `bone2`, `test`;
- root and pivots reviewed.

Fail class:

- `blockbench_format_fail`

### Gate 2: Art Pass

Required:

- silhouette readable without texture;
- materials readable with texture;
- style fits dungeon/faction and Minecraft constraints.

Fail class:

- `art_fail`

### Gate 3: ModelEngine Pass

Required:

- import path documented from local docs, normally
  `plugins/ModelEngine/blueprints` or
  `plugins/MythicMobs/packs/<PackName>/models`;
- reload path documented from local docs, normally `/meg reload models`;
- smoke spawn documented from local docs, normally `/meg summon [model-id]`;
- hitbox and eye height make sense;
- shadow/helper bones behave as intended.

Fail class:

- `modelengine_import_fail`

### Gate 4: MythicMobs Pass

Required:

- mob/boss spawns with the model;
- model attachment and state mechanics are checked against local ModelEngine and
  MythicMobs docs;
- correct states trigger from the mapped skill events;
- damage timing matches the attack tell.

Fail class:

- `mythicmobs_fail`

### Gate 5: Resource Pack Pass

Required:

- generated or merged pack contains needed assets;
- ModelEngine pack output is checked, normally
  `plugins/ModelEngine/resource pack.zip`;
- Nexo ModelEngine auto-import or external-pack merge behavior is checked when
  Nexo delivers the pack;
- client sees the right textures after reload/reconnect;
- no namespace conflict or stale pack issue.

Fail class:

- `pack_fail`

### Gate 6: Gameplay / Performance Pass

Required:

- hitbox is fair;
- attack readability is acceptable;
- death/despawn cleanup works;
- no noticeable runtime regression for intended encounter density.

Fail class:

- `gameplay_fail`
- `performance_fail`

## 7. Evidence Loop

Every candidate asset needs:

1. Blockbench screenshot with model visible.
2. In-game screenshot or equivalent visual runtime evidence.
3. Review markdown with pass/fail verdict.
4. Relevant log review notes.
5. README with runtime IDs, animations, and damage frames.

Do not mark a final asset as approved when any of the following is missing:

- Blockbench screenshot;
- runtime visual evidence;
- failure classification for open issues;
- README;
- review report.

## 8. Failure Diagnosis Matrix

| Symptom | Likely class | What to inspect first |
| --- | --- | --- |
| `.bbmodel` opens but model is invisible or malformed | `blockbench_format_fail` | format choice, outliner/groups, texture path, broken save |
| Model file does not import | `modelengine_import_fail` | `.bbmodel` location under `plugins/ModelEngine/blueprints` or `plugins/MythicMobs/packs/<PackName>/models`, then `/meg reload models` output |
| `/meg summon [model-id]` fails | `modelengine_import_fail` | exact `model_id`, import log, ModelEngine command docs, blueprint path |
| Texture missing or vanilla-item look in game | `pack_fail` | generated `plugins/ModelEngine/resource pack.zip`, Nexo merge/import behavior, stale client pack, missing texture refs |
| Model takes damage oddly or looks at wrong height | `modelengine_import_fail` | `hitbox` cube, hitbox pivot Y / eye height |
| Animation exists but damage lands too early | `mythicmobs_fail` or `gameplay_fail` | skill delay, state trigger, attack tell timing |
| Model seems larger than the real combat space | `gameplay_fail` | hitbox fairness, weapon reach messaging, encounter range |
| Animating limb rotates from nonsense point | `blockbench_format_fail` | pivot placement on the bone, parent hierarchy |
| Client sees old model after changes | `pack_fail` | pack regeneration, zip refresh, reconnect, Nexo external-pack path or ModelEngine pack auto-import |
| Performance drops in mob-heavy area | `performance_fail` | cube/bone count, texture size, encounter density |

## 9. Required Asset Files

Every new model should ship with:

- source `.bbmodel`
- local texture folder or explicit texture path note
- model README from the template
- review report from the template
- evidence paths for Blockbench and runtime visuals

Use these templates:

- `docs/assets/templates/blockbench_model_readme_template.md`
- `docs/assets/reviews/TEMPLATE.md`

## 10. Specialist Acceptance Criteria

An agent qualifies as a repo-ready specialist only when it can independently:

- create a new mob, prop, or boss from zero;
- explain silhouette, scale, pivot, UV, and animation decisions;
- connect the asset to ModelEngine;
- map states into MythicMobs;
- account for resource-pack/Nexo delivery where relevant;
- prepare or execute a staging smoke path with evidence;
- reject its own output when art, gameplay, or performance bar is not met.

Anything below that is pipeline assistance, not specialist-level ownership.
