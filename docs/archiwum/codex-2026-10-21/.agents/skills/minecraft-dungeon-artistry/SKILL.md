---
name: minecraft-dungeon-artistry
description: Craft knowledge for building jaw-dropping, content-dense MC dungeons - scene composition, organic building, palettes/lighting, procedural FAWE patterns, dungeon pacing, encounter design, FX direction, validation. Distilled from level_1 (Kwietna Polana) work + design docs quality bar.
---
# MC dungeon artistry

Use this skill BEFORE designing or building any dungeon scene, zone, or encounter.
It encodes what "feels like an event, not a plugin demo" means in practice
(quality bar: `docs/ai/mmo-excellence-playbook.md`).

## 1. Scene composition (the 5-second rule)

- **One scenic anchor per dungeon.** A single dominant landmark (e.g. Drzewo-Serce
  in level_1) visible from the entrance, framing the fantasy the player is buying.
  The player must understand the theme within 5 seconds of entering.
- **Close the horizon.** Never leave a bare skyline plate: ring of hills + double
  tree wall turns an island into a "cathedral". Mid-ground bumps kill flatness.
- **Sightlines are content.** Frame the anchor with gates/arches at the entrance;
  reveal it in stages along the route (glimpse -> partial -> full at the arena).
- **Contrast sells theme.** Pure zone vs corruption wedge (grove vs sculk);
  gradient transition bands between them, plus a "tongue" of the hostile theme
  reaching toward the play space to create tension.
- **Three depth planes.** Foreground detail (flowers, props), mid-ground shapes
  (hills, ruins), background silhouette (tree wall, anchor). Detail density is
  highest where the player walks, lowest at distance.

## 2. Organic building rules

- **<=7 blocks in a straight line** for anything organic; break lines with
  irregular offsets. Natural forms have partial symmetry only: regular height,
  irregular shape.
- **Branches are stepped 2x2 segments**, NEVER a single box from trunk to tip
  (boxes produce flat "pancake slabs"). Taper trunks in stages (r4 -> r3 -> r2 -> pillar)
  with root buttresses spilling outward (stepped, + mangrove_roots overflow).
- **Multi-lobe crowns**: central ellipsoid + per-branch lobes + top knob. Leaf
  palettes must include ~8% air and ~3% glow (shroomlight) for depth and life.
- **No lollipop trees** near the play space; simple ball trees are acceptable
  only as distant background silhouette (2nd wall layer).
- **Plants in typed clusters** (10-15 clumps of 1-3 species), never uniform %
  scatter (47% flower spam reads as noise, not a meadow).
- **Double-layer terrain** (surface + subsurface band) to avoid see-through holes.

## 3. Palettes, gradients, lighting

- **60-30-10**: dominant material 60%, secondary 30%, accent 10%. Value
  (light/dark) > hue > texture when choosing blocks; mix textures within one
  color family (stone/andesite/tuff/deepslate) for weathered depth.
- **Gradients are directional storytelling**: light->dark = age/corruption,
  warm->cool = safe->hostile. Use 3-5 step block gradients at zone boundaries.
- **Layered lighting**: mood sources at eye level (lanterns, candles), broad fill
  hidden (glowstone/sea_lantern buried under carpets/leaves/moss), accent glow
  inside the anchor (heart chamber). Match source to mood - lantern/candle =
  cozy camp, soul_fire = corruption, end_rod/amethyst = magic moment.
- **Hidden > visible**: light the scene, not the fixtures. Every reward/altar
  space gets a deliberate "spotlight" treatment.
- Test palettes in-place: build a small strip in the actual biome light before
  committing a //replace over the whole zone.

## 4. Procedural FAWE patterns (bot-driven build)

- ONE parameterized build script per dungeon (`_validation/build_level1_grove.js`
  pattern): PARAMS section on top, helper fns (sel/box/gen/cyl/ell/gmask/setb),
  commands accumulated in an array, phases selectable by argv. Extend sections,
  never fork new probe files.
- `//pos1 x,y,z` absolute + `//generate` with expressions normalized to [-1,1]
  (x^2+z^2<1 = cylinder in selection). `//gmask` to constrain replacements.
- Throttle: heavy ops (`//set|//generate|//replace`) ~550ms, light ~160ms;
  watchdog on the whole run.
- NO `//brush` in MythicDungeons edit-mode (player position is frozen).
- 2-block plants (tall_grass, sunflower) BREAK in % patterns - place singles only.
- Build inside `/md edit` session; `/md leave` and MD autosave (300s) both write
  the template. Never leave live mobs before a save.

## 5. Dungeon pacing (route = curriculum)

Camp -> lesson zones -> elite gatekeeper -> boss -> reward scene. Rules:
- **1 zone = 1 mechanic lesson** (melee, LoS/dodge, interrupt, guard/parry,
  status), each introduced safely before it is combined.
- **Camp is narrative**: NPC/scout framing, the quest fantasy stated, the anchor
  visible in the distance.
- **Gatekeeper is a mini-exam** before the boss: uses lessons from the route.
- **Optional secrets** (side caches, poison side area) reward exploration with
  materials, never required for completion.
- **Checkpoint** after the gatekeeper; emergency exit always available.
- Ladder is thematic and deterministic - difficulty changes behavior, not layout
  ("klimatyczna drabinka, nie losowa lista map").

## 6. Encounter design (souls-like, anti-bullshit)

- **Telegraph grammar**: particle warning -> short pause -> hit. Every attack has
  a readable windup; damage timing must match the visual.
- **Phases change verbs, not numbers**: P1 teaches (slam, line, adds), P2 tests
  (AoE reposition, interruptable channel, projectile volley), enrage-lite
  compresses timing - never adds one-shots or counterplay-free chains.
- **Interrupt windows** are announced (glow + sound + title) and generous at low
  difficulty.
- **Anti-bullshit invariants**: no random one-shots; no tank/healer/class
  requirement; solo viable on Easy; difficulty scales behavior (add counts,
  telegraph speed, extra abilities/hazard on HARD) not just HP.
- Boss is a farming hook, not a BiS pinata - reward identity per design docs.

## 7. FX direction (moments, not particles)

- Intro / enrage / death are SCENES: layered particle + sound + title + world
  reaction (root burst, falling petals, light column). Budget one "wow" beat
  per scene, don't smear FX continuously.
- **Reward scene sequence**: death finale -> gate opens -> lit chamber ->
  named-loot moment (title with item family name) -> forward hint (forge/hub).
- Ambient life loop (fireflies, petals, heartbeat pulse near the anchor) at low
  frequency; corruption side gets its own pulse.

## 8. Validation loop (never ship on faith)

- **Screenshot pipeline**: bot + prismarine-viewer + puppeteer (`shoot_level_1.js`
  pattern; edit-mode = rotate frozen camera, play-mode = teleport zone shots).
  Viewer artifacts: moss_block renders as blue swirl, sculk family as white "?",
  particles/ModelEngine invisible; yaw/pitch are INVERTED vs bot.look.
- **Count server-side** (RCON + forceload), never via bot chunk visibility.
- **Death triggers only via real combat kill**, never `/kill`.
- Unique bot names per session owner (Fable* vs codex bots) to avoid
  duplicate_login kicks.
- Per-zone loop: build -> screenshot -> judge vs sections 1-3 -> max 2 iterations,
  then move on and batch remaining polish.
- Human eye validates FX and vibe in-game; PNGs validate geometry/palette only.

## Read first
- `docs/ai/mmo-excellence-playbook.md` (quality bar, content review checklist)
- `docs/mob-boss-encounter-001-foundation-v0.0.1.md` (encounter grammar example)
- `docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`
- `MCMMORPG/_validation/build_level1_grove.js` (reference implementation)

## Good/Bad/Blocker examples
See `examples/good.md`, `examples/bad.md`, `examples/blocker.md`.
