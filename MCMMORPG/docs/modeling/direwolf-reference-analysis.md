# Direwolf reference analysis and why briarwolf failed

## Direct reference corpus actually used

Primary source asset:
- `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`

Additional related assets in the same folder:
- `lrd_direwolf_ground_fx.bbmodel`
- `lrd_electric_beam.bbmodel`

Reference render pack used:
- `/home/przemek/projects/MC/MCMMORPG/_validation/reviews/ref_direwolf/front.png`
- `/home/przemek/projects/MC/MCMMORPG/_validation/reviews/ref_direwolf/side.png`
- `/home/przemek/projects/MC/MCMMORPG/_validation/reviews/ref_direwolf/three_quarter.png`
- `/home/przemek/projects/MC/MCMMORPG/_validation/reviews/ref_direwolf/player_scale.png`
- animation samples under the same directory, including:
  - `animation_idle_start.png`
  - `animation_run_start.png`
  - `animation_attack_1_impact.png`
  - `animation_leap_slam_impact.png`

Extracted texture payloads from the `.bbmodel`:
- main wolf texture: `/tmp/ref_direwolf_tex_0.png`
- slash effect textures: `/tmp/ref_direwolf_tex_1.png` .. `/tmp/ref_direwolf_tex_9.png`
- shadow tail texture: `/tmp/ref_direwolf_tex_10.png`

## What data exists in the reference and must be studied before building a wolf variant

Structural data from the `.bbmodel`:
- 136 elements
- 107 outliner groups
- max hierarchy depth 15
- 22 animations
- logical body tree centered around:
  - `root/chest`
  - `root/chest/lower_back`
  - `root/chest/lower_back/tail_base/.../tail_5`
  - `root/chest/neck/head`
  - `root/chest/neck/head/snoot`
  - `root/chest/neck/head/jaw`
  - `root/chest/left_front_bicep/.../left_front_foot`
  - `root/chest/right_front_bicep/.../right_front_foot`
  - `root/chest/lower_back/left_back_bicep/.../left_back_foot`
  - `root/chest/lower_back/right_back_bicep/.../right_back_foot`
- extra gameplay / VFX structures are also embedded:
  - sword subtree under jaw
  - slash_root with 9 slash pieces
  - shadow-tail segment chains on legs
  - separate `sword_death` subtree

Texture payload inside the reference:
- one main 256x256 wolf texture
- nine 256x256 slash textures
- one 128x128 shadow-tail texture

This matters because the reference is not just "a wolf shape". It is a full creature package with:
- premium silhouette
- layered materials
- rig-ready hierarchy
- attack/VFX attachments
- motion language

## What is actually good about the direwolf geometry

### 1. The massing is animal-first, not decoration-first
The model reads as a predator before any detail is read.

Core mass order:
1. head wedge
2. shoulder/chest block
3. rib/abdomen taper
4. hindquarter mass
5. tail extension

The briarwolf attempts failed because they started from decorative bark/root ideas and then tried to force wolfhood on top.
The direwolf does the opposite: wolf first, style second.

### 2. The body is low and long
From side view the direwolf sits low to the ground with a long forward-reaching head and a long tail counterweight.
That gives instant predator read.

Rules to preserve:
- body length should clearly exceed body height
- head should project forward, not be buried in the chest
- belly should have visible clearance from the ground
- tail should extend the silhouette, not terminate abruptly behind the hips

### 3. The backline is articulated, not a single brick
Readable flow:
- neck rise
- shoulder shelf
- back plane
- lower back transition
- tail launch

The direwolf never reads as one barrel. It has directional planes and rhythm changes.

### 4. The head is a wedge, not a box
Readable head landmarks:
- forward snoot
- separate jaw
- cheek fur
- brows
- ears
- glow eyes placed as a focal point, not random lights

This is the biggest failure in ugly briarwolf passes.
If the head becomes a flat box or plate mask, the whole creature stops being a wolf even if the rest is passable.

### 5. Legs have true segment rhythm
The direwolf has explicit upper and lower leg segments with feet at the end of a readable joint chain.
It is not four vertical posts.

The important thing is not raw poly count. The important thing is the chain logic:
- front bicep -> front forearm -> foot
- back bicep -> back forearm -> ankle -> foot

That chain creates believable posing and motion.

### 6. Tail is a silhouette system, not a stub
The tail is segmented deeply:
- `tail_base`
- multiple tail links through `tail_5`

That matters because the tail contributes to:
- balance
- aggression
- motion arcs
- side-view premium feel

## What is actually good about the direwolf texture

### 1. Material hierarchy is disciplined
The main texture does not spam equal noise everywhere.
It has readable priorities:
- dark core planes
- controlled lighter edge highlights
- select cool-blue accents
- focal lights at the eyes

### 2. Palette is narrow and coherent
The reference uses a controlled cool palette rather than random local color changes.
That makes the model feel expensive.

### 3. Highlights describe planes
The texture is not random dirt.
Lighter strokes and trims help tell the viewer where:
- brow planes turn
- snoot edges catch light
- limb faces separate
- chest and back volumes roll

### 4. Contrast is intentionally placed
Contrast is saved for important reads:
- head / face
- eye focal area
- some structural edges
- paws / claws / selected plane breaks

The failed briarwolf textures used bark-like noise almost everywhere, flattening the form.

### 5. UV/mapping logic supports anatomy
The direwolf texture atlas contains many dedicated islands and directional strokes.
It is clearly mapped with body-region intent, not a lazy all-over procedural fill.

## What is actually good about the direwolf animation package

Reference animations present:
- sleep
- awaken
- idle
- run
- start_run
- end_run
- lean_right
- lean_left
- turn_180
- running_swipe_ready
- running_swipe
- combo_1
- attack_1
- attack_2
- jump_back
- dodge_clockwise
- dodge_counter_clockwise
- take_damage_1
- take_damage_2
- leap_slam
- howl
- death

### What this tells us
The reference was designed as a full body-action creature, not as a static decorative mesh.
The geometry and hierarchy support:
- crouch/compression
- forward lunges
- side dodges
- leap impacts
- head-led expression
- tail-led balance

Observed motion principles from sampled frames:
- even in idle the wolf holds tension; it is crouched, not neutral
- run pose depends on a long body axis and clear leg segmentation
- attack impact depends on head/neck/jaw separation and strong forequarter mass
- leap slam depends on tail counterbalance and whole-body compression/extension

This means that if we simplify a wolf variant too aggressively, we are not just losing detail. We are destroying the motion language the reference depends on.

## What was wrong in our process

### 1. We did not fully ingest the reference package before designing
We looked at the wolf generally instead of extracting the exact package:
- hierarchy
- mass order
- head landmarks
- leg chain logic
- tail segmentation
- animation demands
- texture discipline

That is the core process failure.

Missing extraction discipline in the old process:
- we did not log source-by-source state (`GOTOWE` / `CZĘŚCIOWE` / `PUSTE`)
- we did not return 2-4 concrete facts plus 1-2 gaps per reference file
- we mixed shell, texture, animation, and style lessons into one blob instead of a file-by-file ledger

Correct extraction recipe from now on:
1. inventory every source file separately
2. assign each source a role
3. mark each source `GOTOWE` / `CZĘŚCIOWE` / `PUSTE`
4. write 2-4 facts and 1-2 limits per source
5. derive explicit rule IDs from evidence (`R1`, `R2`, ...)
6. assign criticality (`CRITICAL` / `IMPORTANT` / `NICE_TO_HAVE`)
7. build a file/view/frame evidence matrix saying which rules each source strongly supports, weakly supports, or cannot justify
8. if sources conflict, resolve them explicitly instead of blending them by vibe
9. mark each rule as `GENERAL`, `FAMILY`, or `REFERENCE-SPECIFIC`
10. attach confidence (`HIGH` / `MEDIUM` / `LOW`)
11. only then merge those facts into family rules and a downstream execution brief
12. require the downstream pass to answer in a fixed response contract instead of free-form prose
13. add a dependency graph so downstream rules do not get evaluated as if they were independent of broken shell rules
14. classify failures with a fixed taxonomy instead of vague disappointment prose
15. define rebuild triggers so repeated `CRITICAL` or family-read failure forces shell rebuild instead of decorative thrashing
16. attach short repair playbooks per major failure type so the next pass has a concrete recovery path
17. classify the pass into an exit matrix (`PASS_TO_STYLE`, `PASS_TO_RUNTIME`, `ITERATE_LOCAL`, `REBUILD_SHELL`, `INSUFFICIENT_EVIDENCE`) instead of vague momentum language
18. force the final response shape to match the chosen exit bucket so downstream handoff cannot slip back into loose prose

### 2. We optimized too early for validator-green and lightweight generation
This pushed us toward:
- too few elements
- too shallow hierarchy
- too little head construction
- too little leg articulation
- too low texture ambition

Validator-green is necessary but completely insufficient for creature quality.

### 3. We treated briar/root theme as a base shape instead of a secondary language
Correct order:
1. premium wolf shell
2. accepted wolf read from front/side/three-quarter
3. selective briar/root accents
4. final texture polish

Wrong order:
1. bark cubes and thorns
2. then try to make that look like a wolf

### 4. We did not use the animation pack as a modeling constraint
A wolf should be modeled as if it must survive:
- crouch
- run
- bite attack
- leap
- recovery

If the shell cannot clearly support those poses, the shell is wrong.

### 5. We underspecified texture rules
Without explicit texture rules, the generator defaulted to noisy bark spam.
That produced:
- flattened forms
- weak focal hierarchy
- ugly material read
- furniture/log feel instead of beast feel

## Concrete rules that must be followed for the next wolf-like creature

## Shell / silhouette rules
- Start from a wolf blockout, not a bark concept.
- Front, side, and three-quarter views must read correctly before decorative accents.
- Head must project clearly forward from the chest.
- Backline must show neck -> shoulder -> back -> lower back -> tail launch.
- Torso must read as shoulder mass + rib/abdomen + hindquarters, not one barrel.
- Front and hind legs must have explicit upper/lower segmentation and planted paws.
- Tail must be long enough to finish the silhouette and support action poses.

## Head rules
- Separate skull, snoot/muzzle, jaw, brows/cheeks, and ears.
- The face must form a wedge, not a flat rectangular mask.
- Eyes are focal accents, not the whole face.
- Bark/root additions cannot erase wolf cranial landmarks.

## Texture rules
- Use a limited palette with material roles defined up front.
- Put contrast where it helps read form, not everywhere.
- Use lighter value shifts to describe planes and edges.
- Do not stripe bark noise across all major masses.
- Fur/body base must stay dominant; briar/root accents are secondary and selective.
- Texture should make the model read clearer from distance, not busier up close.

## Rig and motion rules
- Model as if idle, run, bite, dodge, and leap must all look good.
- Head, jaw, chest, lower back, and tail need enough structural separation to animate.
- Legs must be built for readable bend points.
- If a body part cannot contribute to an attack or recovery pose, it is probably over-merged.

## Process rules
1. Open the direct `.bbmodel` first.
2. Open reference renders for front / side / three-quarter / player scale.
3. Open representative animation frames for idle, run, attack, and leap.
4. Extract geometry rules before touching the generator.
5. Build an unthemed wolf shell that passes visual review.
6. Only then add briar/root language.
7. Only then texture it.
8. Only after that run integration/binding passes.

## Hard art gate standard
- First read veto: if the immediate visual impression is weak or ugly, the shell fails even if it is technically improved.
- Front/head/paw gate is blocking: bad front read, crate-head, plank muzzle, slab paws, or support-post legs stop handoff regardless of side-view progress.
- Relative improvement does not count as success; only absolute visual quality matters.
- Validator-green has zero weight in art signoff.
- Mobs lane stays frozen until art lane produces a strong wolf read.
- Decoration never rescues a weak blockout; if theme detail is doing rescue work, the shell is still wrong.
- Rat/log read is a named hard fail. If the creature reads like a rodent, ferret, root-stick, or log-creature, stop all decorative work and rebuild animal architecture.
- Cranial dominance and forequarter dominance are mandatory. Skull + cheeks + jaw must visually beat the snout, and shoulder/chest mass must lead the creature before abdomen taper and hindquarter push.
- `root corridor` / `snout stick` is a banned anti-pattern: a long narrow muzzle projecting from an underbuilt skull is not predator reach.

## Anti-patterns to ban
- "looks roughly like a wolf so continue"
- "validators are green so art is good enough"
- "add more bark detail to fix weak silhouette"
- "make it simpler by deleting the head/leg hierarchy"
- "treat reference as moodboard instead of direct source package"

## Minimum acceptance bar for briarwolf redo
Before saying the redo is good:
- it must beat the current briarwolf on front read
- it must keep a clean predator silhouette in side view
- it must preserve wolf head landmarks under briar styling

## 2026-07-07 Blockbench process lock
- direct Blockbench session note: `/home/przemek/projects/MC/MCMMORPG/docs/modeling/direwolf-blockbench-session-2026-07-07.md`
- direct source used for this learning case: `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`
- the durable lesson is NOT "keep checking this one direwolf forever"; the durable lesson is: open strong direct references once, extract the structural truth into a reusable packet/runbook, then build from those written constraints and memory
- reopen the direct source package when:
  - a new creature family is being learned
  - the packet is missing or ambiguous
  - a pass drifts/fails and the written constraints were not enough
- future wolf passes may not claim `PASS` without Blockbench-side review against the accepted concept plus the distilled shell rules from this analysis
- the 2026-07-07 CLI launch attempt found a real environment blocker: the current AppImage run hit a fatal GPU/Wayland failure, so a later run must first stabilize Blockbench itself before claiming native review evidence
- texture must have readable material hierarchy instead of bark spam
- the briar additions must feel intentionally placed, not glued on

## General workflow lesson extracted from direwolf
1. Learn from direct references first, not from derived prose.
2. Distill that learning into explicit reusable rules, preferably via `/home/przemek/projects/MC/docs/ai/mc-creature-reference-extraction-packet-template.md` and a filled family packet such as `/home/przemek/projects/MC/MCMMORPG/_validation/model_studies/direwolf_family_reference_extraction_packet.md`.
3. Every strong rule should have lineage: source file -> observed fact -> inference -> execution rule ID.
4. Mark what is general, what is family-only, and what is reference-specific.
5. Attach confidence so weak assumptions do not masquerade as hard law.
6. Only after that should the packet collapse into a short downstream execution brief.


## Short diagnosis: what is not wrong with the tools, and what is wrong with us
Not wrong:
- reference files exist
- renders exist
- animations exist
- texture data exists
- the corpus is sufficient

Wrong:
- we used the corpus too shallowly
- we designed from idea-first instead of reference-deconstruction-first
- we tried to compress a premium animated creature package into a cheap procedural shortcut
- we let technical pass criteria lead the art process
- we were not logging what each source could and could not prove
- we were not forcing contradiction resolution when different source types pulled in different directions
- we were not verifying rule-by-rule before handing the learning downstream
- we were treating all failed passes too similarly instead of naming whether the failure was shell, family, motion, landmark, texture, or evidence
- we were allowing tweak loops when rebuild should have been called early
- we were not handing the next pass a concrete recovery playbook after each failure class
- we were not distinguishing "can continue locally" from "must rebuild" with a hard exit matrix

This file exists to stop that failure from repeating.