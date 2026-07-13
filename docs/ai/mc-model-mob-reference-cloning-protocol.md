# Reference cloning protocol (mandatory for every new Layer A shell)

Why this exists: `level_1_moss_stalker` (2026-07-08) had a correct `reference_shell_lock`
pointing at `lrd_direwolf.bbmodel`, but the actual shell was built by
`build_level_1_moss_stalker_baseline.js` typing cube coordinates from scratch —
zero references to the real file. Result: generic box-soup despite having the
right reference on file. Prose descriptions of a reference (atlas entries,
translation plans, shell locks) are not geometry. Copying real coordinates is.

## Tooling

- `MCMMORPG/_validation/lib/reference_clone.js` — library: load a `.bbmodel`,
  find a named outliner group, deep-clone its bone+cube subtree with fresh
  UUIDs, transform (scale/translate), remap textures into a target model,
  merge into a target `.bbmodel`, and record provenance.
- `npm run modelengine:clone-shell -- --ref <reference.bbmodel> --group <groupName> --target <target.bbmodel> [--into <parentGroup>] [--scale sx,sy,sz] [--translate tx,ty,tz] [--prefix name_]`
  Clones one bone subtree (e.g. `chest`, `left_leg_root`, `head`) out of a real
  reference file into the target model. Writes `<target>.provenance.json`
  mapping every cloned element's new uuid back to its source file + source uuid.
- `npm run modelengine:gate-shell -- --model <target.bbmodel> [--min-ratio 0.6]`
  Gate: fails if fewer than `min-ratio` of the model's elements have recorded
  provenance, or if `model_studies/<mob_id>_reference_shell_lock.md` doesn't
  have `overall_verdict: PASS`. Run this before any Layer B (texture/detail)
  pass and before any production handoff — a `FAIL` blocks the lane.
  A bare typed `PASS` is not enough: the shell_lock file must also carry a
  `verdict_evidence: <path(s) to comparison render(s)>` line, and every path
  must exist on disk with an mtime at or after the model file's mtime. This
  exists because a `PASS` with no evidence field is exactly how
  `level_1_moss_stalker`'s `BLOCKED` verdict got silently ignored the first
  time — the gate no longer accepts a verdict that isn't backed by a render
  file newer than the last model edit.
- `npm run modelengine:webgl-view -- --model <path.bbmodel> --out <scratch>.png [--yaw 35] [--pitch -15] [--size 720]`
  Real WebGL 2.0 render (puppeteer + SwiftShader + three.js) with actual
  lighting, shading and perspective — reuses the same bone-transform math as
  `modelengine:render`/`quick_view.js` (`collect`/`cubeVertices`/`animationPose`
  exported from `render_bbmodel_review.js`) so geometry is identical, but the
  image is good enough to actually judge silhouette, surface flow and whether
  an addition reads as rooted vs. floating. Use this for the sculpt loop below
  instead of `modelengine:quick-view`'s flat 2D render — the flat renderer has
  no real shading and hides exactly the defects the sculpt loop exists to catch.
  Note: SwiftShader intermittently fails program validation on first draw after
  a texture upload; the script already retries with a blank-frame pixel check,
  so a working render is guaranteed, just occasionally slower (2-5 attempts).
- `npm run modelengine:mirror -- --target <path.bbmodel> --group <groupName> [--axis x] [--center 0] [--prefix mirror_] [--into <parentGroupName>]`
  Mirrors a bone/group subtree across an axis plane instead of hand-retyping a
  symmetric limb's coordinates (which is the same blind-coordinate-authoring
  failure this whole protocol exists to prevent, just applied to "the other
  leg" instead of a whole shell). **Critical usage note**: always pass `--into
  <parentGroupName>` naming the correct mirrored-side parent bone (e.g.
  mirroring `left_front_forearm` under `right_front_bicep`). Without `--into`
  the mirrored subtree is reattached as a top-level sibling at the model root,
  which silently drops the real ancestor transform chain (shoulder/chest
  rotations) and produces wrong world-space placement with no error thrown.
  Verify correctness by comparing world-space bounding boxes (via
  `collect`/`cubeVertices` from `render_bbmodel_review.js`) against the real
  counterpart limb, or just render it — a detached mirrored limb reads as
  visibly wrong immediately.
- `npm run modelengine:snap -- --target <path.bbmodel> --element <name> [--corner 0-7] [--reference <other.bbmodel>]`
  Snaps one corner of a freehand element onto the nearest real surface point
  among the model's other elements (or an external reference file's
  elements), instead of eyeballing a coordinate and hoping it touches the
  body. Uses raw element `from`/`to` corners as candidates, not full
  bone-transform-resolved world coordinates — exact for elements near the
  root or with identity ancestor rotations, unreliable for deeply nested/
  rotated bones (a large reported snap distance is the tell that this
  simplification doesn't hold; confirm visually with `modelengine:webgl-view`
  before trusting it in that case).

## Steps for a new creature shell

1. Pick reference file(s) from the corpus (`docs/ai/modelengine-reference-corpus.md`,
   `modelengine-creation-atlas.md`) and write `model_studies/<mob_id>_reference_shell_lock.md`
   as today, but do not treat it as done until step 4 passes.
2. For each structural chain the target needs (spine/chest, head, each leg,
   tail, wings, etc.), run `modelengine:clone-shell` against the chosen
   reference group. Prefer one clone call per bone subtree over one giant
   clone of the whole model — this is what lets you mix chains from multiple
   references (e.g. direwolf legs + a plant-mob's bark texture set).
3. Only after the cloned shell exists may you hand-author *original* elements
   (accents, silhouette breaks, thematic replacements) — cap these at roughly
   20% of total elements. If you're inventing more than that, you're rebuilding
   from scratch again; clone more source material instead.
4. Run `modelengine:gate-shell`. If it fails, either clone more real geometry
   or go fix the shell_lock — do not proceed to texture/detail passes on a
   FAIL. Update `reference_shell_lock.md`'s `overall_verdict` to `PASS` only
   once the gate itself passes.
5. Render-check (`modelengine:render`) after every clone+transform step, not
   only at the end of a work session — catch silhouette/proportion problems
   while the cause is still the last thing you did.

## Corpus tooling: deliberately not swapped to WebGL

`render_model_reference_corpus.js` + `build_model_review_sheets.js` render the
*entire* reference corpus (every model, every animation phase, every static
view — potentially thousands of frames) through the flat 2D renderer, with
resume/fallback/memory-pressure tracking tuned to that renderer specifically.
This was NOT swapped to WebGL: puppeteer's per-frame cost (browser nav + up
to 5 SwiftShader-validation retries) would multiply full-corpus render time
by roughly an order of magnitude for a bulk thumbnail/fallback-rate job that
doesn't need per-frame shading quality — that would be a regression, not an
upgrade, for what that pipeline is actually for.

What did need the real renderer was judging *one* model at a time — the
sculpt loop, or picking/confirming a reference before cloning it — and
`modelengine:webgl-view` already covers that for any `.bbmodel` path. The one
gap this left was resolving a corpus model by short id instead of typing its
full nested blueprint path by hand:

`npm run modelengine:corpus-spotcheck -- --id <blueprint-basename-or-relative-path> --out <path.png> [--corpus <dir>] [--yaw 35] [--pitch -10]`
Resolves the id against the corpus's blueprint tree and renders it through
the real `modelengine:webgl-view` pipeline. Fails loudly if the id is
ambiguous (lists all matches) instead of silently picking one.

## Content completeness (beyond shell geometry)

Passing `gate-shell` and `check-render-discipline` only proves the *geometry*
is real and was visually checked while being built. It says nothing about
whether the texture is actually painted or the model has any animation —
both of those gaps are invisible to the gate above and easy to ship
by accident.

`npm run modelengine:check-content-completeness -- --model <path.bbmodel> [--min-uv-ratio 0.15] [--require-animation true|false]`
FAILs if:
- fewer than `min-uv-ratio` of textured faces use a distinct UV rect (most
  faces reusing the same tiny UV stamp means the texture is placeholder/
  unpainted, not mapped per-surface — this caught `level_1_moss_stalker`'s
  actual UV ratio of 0.04 on first run)
- the model has zero animations with real keyframes (a skeleton with no
  authored motion at all)

Run this before any `PASS_TO_RUNTIME` claim, alongside `gate-shell` and
`check-render-discipline` — none of the three substitute for the others.

## Hard rule

A shell element with no provenance entry and no explicit "intentional original
addition" note in the mob's work packet is an anti-pattern
(`blind_coordinate_authoring`, see `mc-model-mob-anti-pattern-catalog.md`).
`check_shell_provenance.js` is the enforcement mechanism, not a suggestion.

## The sculpt loop (for the ~20% you build freehand)

Cloning solves shell topology. It does not teach judgment about a specific
addition — a spike, a scar, an asymmetric plate. Placing that from typed
coordinates alone with no visual check in between is the same blind-authoring
failure at a smaller scale, and it's how a "corruption seam" or "silhouette
break" ends up floating disconnected from the body instead of rooted in it.

For any freehand element:

1. Add it with a first-guess coordinate (it will usually be wrong — that's fine).
2. `npm run modelengine:webgl-view -- --model <path> --out <scratch>.png --yaw 35 --pitch -10`
   (also try `--yaw -35`, `--pitch 10`, a top-down `--pitch -80` — one angle hides
   most placement errors).
3. Read the PNG. Name the specific defect you see (floating above the surface,
   wrong scale relative to the neighboring mass, breaks the silhouette in the
   wrong place, doesn't follow the surface curve) — not "looks fine."
4. Adjust the coordinates to fix that specific defect. Re-render. Repeat until
   the defect is gone — usually 2-3 passes, not one.

Do not batch ten freehand additions and render once at the end. One render
covering ten new elements can't tell you which one is wrong. Render after
each element or tight cluster of elements while the cause is still the last
edit you made.

This is enforced, not just advised: `npm run modelengine:check-render-discipline -- --model <path> [--max-new-elements 3]`
reads `<model>.render_log.json` (appended automatically by `modelengine:webgl-view`
and `modelengine:quick-view` on every successful render) and FAILs if the
model has gained more than `max-new-elements` since the last logged render, or
if no render has ever been logged for it. Run it before claiming a freehand
pass is done.
