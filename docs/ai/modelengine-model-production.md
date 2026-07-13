# ModelEngine High-Quality Model Production

This runbook is mandatory for new or materially changed ModelEngine mobs. A
successful import is an integration result, not an art-quality result.

## Definition of Done

A model is complete only when all of the following are true:

1. The design brief, silhouette, construction, rig, texture, animation, hitbox,
   binding, and runtime gates pass.
2. Current visual evidence was rendered from the exact blueprint being shipped.
3. Every visual review category scores at least 4/5 and `knownIssues` is empty.
4. The model is spawned after a ModelEngine and MythicMobs reload and inspected
   at player distance.
5. The reviewer can name the creature, role, and dominant shape from its
   untextured silhouette.
6. Final art iteration happened Blockbench-first on the real `.bbmodel`, with live front/side/back/three-quarter/player-scale checks during editing rather than guessed numeric changes.

Never use cube count alone as a quality target. Complexity must serve the
creature's anatomy, silhouette, motion, or gameplay readability.

## 1. Design Brief

Write a quality manifest under
`MCMMORPG/_validation/model_quality/<model_id>.quality.json` before detailed
modeling. Never place review metadata beside `.bbmodel` files because
ModelEngine attempts to import JSON files from its blueprint tree.

- gameplay role, scale, locomotion, attacks, environment, and viewing distance;
- three form-language words, one dominant visual idea, and at least three
  forbidden traits;
- references chosen for construction, rig, animation, or texture technique;
- required animations derived from actual MythicMobs states and skills.

The dominant idea must be structural. Color, particles, or a texture motif do
not repair a weak silhouette.

## 2. Reference Deconstruction

Inspect nearby working `.bbmodel` files before creating geometry. Record:

- overall bounds and proportion ratios;
- primary, secondary, and tertiary masses;
- negative spaces separating head, limbs, weapons, tails, or crowns;
- bone hierarchy depth, joint pivots, cube distribution, and special bones;
- animation coverage, keyframe density, anticipation, impact, recovery, and
  secondary motion;
- texture resolution, palette hierarchy, UV use, and focal contrast.

Use the local Minotaur, Cerberus, and Chameleon as different construction
benchmarks. They are not appearance templates.

## 3. Silhouette Gate

Create three materially different blockout directions before detailing. Review
front, side, back, and three-quarter views plus a thumbnail representing
15-30 blocks of in-game distance.

Reject a blockout when any of these are true:

- the creature reads as a generic humanoid or unrelated archetype;
- head, torso, weapon, or locomotion cannot be identified in silhouette;
- limbs have uniform bamboo-like thickness;
- accidental asymmetry makes the pose look crooked;
- the center of mass is unsupported by feet, roots, paws, or other contacts;
- the design depends on texture to explain its shape.

Choose one direction and record why it wins. Do not texture or animate a failed
blockout.

## 4. Form Construction

Build in this order:

1. Primary masses establish weight, height, width, and center of gravity.
2. Secondary masses establish anatomy, armor, bark, muscle, plates, or growth.
3. Tertiary forms add accents without breaking the main read.

Every limb needs taper, at least one readable joint where anatomy requires it,
and a believable load path. Feet must contact the ground. The head must carry
character and gaze direction. Weapons and oversized growths must connect
structurally to the body. Asymmetry must be intentional and compositionally
balanced.

Review intersections in the bind pose and in every extreme animation pose.
Remove floating cubes, paper-thin accidental surfaces, hidden duplicate cubes,
and details invisible at gameplay distance.

## 5. Rig and Pivots

The outliner follows the creature's anatomy, not a fixed humanoid template.

- Put each pivot at the real rotation center of its joint.
- Use articulated chains for shins, forearms, jaws, necks, tails, roots,
  chains, wings, and other bending structures.
- Keep visual cubes assigned to exactly one intended bone.
- Keep hitbox and helper bones separate from rendered anatomy.
- Use `h_`, `b_`, `ob_`, item, mount, leash, and other ModelEngine prefixes
  only for their documented behavior.
- Balance bone count against runtime cost. A virtual helper is preferable when
  it improves motion without creating another rendered part.

Mirror bilateral anatomy mechanically first, then introduce deliberate
asymmetry. Unexplained pivot or length differences are defects.

## 6. Hitbox and Eye Height

The main `hitbox` represents navigable collision around the body, not the
maximum visual bounds of horns, crowns, tails, or weapons.

- Keep the hitbox as a root bone with exactly one cube.
- Keep X and Z square as required by ModelEngine.
- Put eye height at the hitbox bone pivot Y.
- Keep the hitbox hidden in authored and review renders.
- Use documented `b_` or `ob_` sub-hitboxes when large attackable parts need
  their own target volume.
- Compare collision width, body width, total height, eye height, and ground
  contact in a dedicated debug render.

## 7. Texture and UV

Define a limited palette and contrast hierarchy before painting detail.

- Reserve strongest contrast for face, weak point, weapon, or signature feature.
- Keep texel density coherent between neighboring parts.
- Keep UV rectangles inside the texture and avoid unintended overlap.
- Use material direction to reinforce wood grain, fur, scales, stone, or metal.
- Avoid random noise, flat single-color surfaces, and texture detail that
  disguises poor geometry.
- Review textured and untextured renders. Both must remain readable.

## 8. Animation

ModelEngine evaluates Minecraft animation at 20 ticks per second. Author and
review on 0.05-second boundaries.

Required states come from the mob's real behavior. A boss normally needs idle,
locomotion, hit reaction, death, phase transition, casts, and distinct attack
animations rather than one generic `attack`.

Each action animation contains:

1. anticipation;
2. readable action path;
3. exact impact or event frame;
4. follow-through;
5. recovery to a stable pose.

Animate the complete kinetic chain. A weapon strike should involve feet, hips
or body, shoulder, arm, forearm, hand, and weapon where applicable. Idle must
show weight and character. Walk cycles must transfer weight without foot
sliding. Loops must close without a pop. Death must settle into a stable final
pose. Use timeline events for gameplay synchronization when appropriate.

## 9. Iterative Visual Review

Every iteration produces:

- front, side, back, and three-quarter bind-pose renders;
- black silhouette;
- player scale comparison;
- hitbox debug view;
- representative frames for idle, walk, every attack/cast, transition, hit,
  and death;
- comparison with the previous iteration and a relevant local reference.

Review these categories from 1-5: silhouette, proportions, construction,
character, readability, rig, animation, texture, and hitbox. A score below 4
blocks completion. Record concrete defects, revise, rerender, and repeat until
all categories pass and no known issue remains.

## 10. Integration and Runtime Gate

Run gates in this order:

1. static blueprint quality;
2. fresh evidence and quality-manifest hash;
3. MythicMobs YAML to blueprint binding;
4. ModelEngine reload and cache generation;
5. MythicMobs reload;
6. live spawn;
7. state and skill animation checks;
8. collision, eye height, scale, orientation, ground contact, and player-distance
   visual inspection.

The required loop is:

`analyze -> model -> render -> critique -> revise -> rerender -> import -> spawn -> inspect -> revise`

Do not report completion after a failed or missing visual/runtime gate.

## 11. Ecosystem Contract

The model, mob, and skills are one authored system. A boss quality manifest
uses schema version 2 and declares:

- `interactionBones`: moving hit origins, projectile/cast targets, VFX anchors,
  detachable parts, and other gameplay-facing bones;
- `animationContract`: purpose, impact ticks, and primary kinetic-chain bones
  for every gameplay animation;
- `skillBindings`: the exact skill definition, state animation, model parts,
  impact ticks, timing tolerance, and required GCD/model lock;
- `integrationFiles`: mob, skill, item, sound, or other files required by the
  model.

Run `npm run modelengine:ecosystem`. It must prove that referenced files,
animations, bones, states, and impact timing resolve against the shipped
blueprint.

## 12. Combat Synchronization

Do not implement boss melee as owner-centered radius damage when the damaging
limb has a visible path. Attach a short-lived totem or equivalent hit volume to
the animated hand, foot, jaw, weapon, tail, or root. Start it at the declared
impact tick and keep its duration no longer than the visible contact window.

Every committed attack must define:

1. an animation and anticipation interval;
2. GCD or equivalent overlap prevention;
3. model rotation/AI policy during the committed motion;
4. one or more moving impact origins;
5. synchronized sound and VFX;
6. recovery and restoration of normal control.

Use `BodyClamp` for believable head/body tracking limits. Use `lockmodel`
during attacks whose animation would be broken by owner yaw changes. Disable AI
only when pathfinding would visibly fight the authored action.

## 13. State, Damage, and Transformation

Use `defaultstate` replacement for passive, awakened, locomotion, stun, flight,
or phase modes. State transitions must be explicit and reversible.

Use `changepart`, part visibility, alternate models, and detached helper mobs
only when the design calls for equipment changes, damage progression,
dismemberment, or transformation. Validate both source and replacement model
parts. Decorative complexity without gameplay or silhouette value is not a
quality improvement.

## 14. Sensory Layer

Layered sound, particles, emissive textures, brightness, tint, recoil, and
camera feedback reinforce an already readable motion:

- movement and vocal layers begin during anticipation;
- whoosh or strain follows the moving limb;
- impact sound, debris, recoil, and damage share the impact tick;
- ambient particles use stable helper bones rather than owner position;
- emissive and brightness changes identify charge, weak points, or phase state;
- death effects follow the actual collapsing or detaching parts.

Sound duration must fit the animation phase. Do not truncate a long cue with a
short state or delay an impact transient beyond visible contact.

## 15. Corpus-Derived Benchmarks

The current reference corpus is documented in
`docs/ai/modelengine-reference-corpus.md`. It contains 542 blueprints spanning
lightweight props, ordinary mobs, vehicles, elites, and large bosses.

Use corpus percentiles as diagnostics:

- compare a model only with a similar archetype and gameplay role;
- investigate unexpectedly shallow hierarchy, low animation coverage, or
  missing interaction bones;
- do not inflate cube, bone, texture, or animation counts to reach a percentile;
- inspect complete mob/skill/resource-pack relationships, not isolated source
  files.

## 16. Original Synthesis And Continuous Learning

The corpus is evidence and design vocabulary, not a style template. A new model
must begin with its own intended distant read, encounter role, mood, scale,
dominant shape, and player-facing mechanic.

For every relevant subsystem:

- select at least two references matched to anatomy and encounter role;
- state the principle being studied and the limitations of each reference;
- record a deliberate difference that protects the new model's identity;
- write a falsifiable visual or runtime hypothesis;
- validate that hypothesis with render, editor, or runtime evidence.

Use `MCMMORPG/_validation/reference_corpus/modelengine-design-router.json` to
find specialized candidates and `modelengine-negative-patterns.json` to check
known risks. Neither file authorizes copying geometry, proportions, bones, or
keyframes.

Record outcomes in
`MCMMORPG/_validation/active_runtime_reviews/model_design_learning.json`.
Confirmed, rejected, and inconclusive hypotheses are all useful. Promote a
lesson into this runbook or the ModelEngine skill only after strong or repeated
evidence; never convert one subjective observation into a universal rule.
