# MC Model + Mob Anti-Pattern Catalog

Purpose: teach agents how to recognize recurring failure modes before they waste iterations or degrade a decent baseline.

This file is for model/mob-specific failure patterns. Put broader agent/process mistakes in `anti-patterns.md`.

## Severity Levels

- High: usually reject or revert
- Medium: blocks polish or downstream integration
- Low: acceptable temporarily during narrow exploration if explicitly tracked

## Visual Anti-Patterns

### 1. Generic armored humanoid drift
Severity: High

Symptom:
- creature, plant boss, beast, or colossus starts reading like a guy in armor

Why it happens:
- head too high and too clear
- shoulders, chest, waist, and legs all use familiar humanoid proportions
- symmetrical plate language dominates the read

What to do instead:
- bury or lower the face focus where appropriate
- enlarge the dominant trunk/body mass
- break humanoid proportion logic before adding detail

### 2. Box soup
Severity: High

Symptom:
- model is made of many boxes but no box matters more than the others

Why it happens:
- detail added before hierarchy
- every limb gets equal complexity
- no dominant outer contour

What to do instead:
- re-block the big masses only
- remove small forms until the silhouette reads in flat black

### 3. Noise masquerading as quality
Severity: High

Symptom:
- more cracks, bark, moss, straps, spikes, or cubes are used to fake premium quality

Why it happens:
- weak silhouette trying to be rescued by texture/detail
- agent equates complexity with craftsmanship

What to do instead:
- judge at distance first
- add detail only after hierarchy passes

### 4. Random asymmetry
Severity: Medium

Symptom:
- one side is different just to look fancy, but the difference means nothing

Why it happens:
- agent knows asymmetry is often good but not why

What to do instead:
- tie asymmetry to attack side, corruption side, shield side, weight side, or lore damage side

### 5. Focal spam
Severity: High

Symptom:
- several glows, color pops, corruption accents, or eye-catchers compete at once

Why it happens:
- no focal hierarchy
- fear of making the model feel "too plain"

What to do instead:
- choose one primary focal source
- demote the rest to support

### 6. Close-up-only craftsmanship
Severity: Medium

Symptom:
- model looks interesting in editor zoom but collapses in live gameplay distance

Why it happens:
- work judged from beauty shots only
- no player-distance review gate

What to do instead:
- require front/side/three-quarter/player-scale comparisons every pass

## Gameplay / Integration Anti-Patterns

### 7. Pretty but unbindable
Severity: High

Symptom:
- art is present but no clear ModelEngine / MythicMobs handoff exists

Why it happens:
- no helper-bone thinking
- no file-graph thinking
- no anchor strategy for effects, attacks, or hit volumes

What to do instead:
- define runtime needs during brief stage
- name expected anchors and IDs before polish

### 8. Animation-hostile geometry
Severity: Medium

Symptom:
- chosen forms look cool static but clip, jitter, or lock up once animated

Why it happens:
- silhouette built with no regard for motion arcs
- rigid clutter around joints or swing paths

What to do instead:
- critique with attack and locomotion paths in mind
- simplify collision-prone geometry near major joints

### 9. Role mismatch
Severity: High

Symptom:
- flanker looks tanky, caster looks bruiser, arena boss reads like trash mob

Why it happens:
- visual direction separated from encounter role
- references chosen by cool factor instead of combat function

What to do instead:
- define role in the brief
- choose references by anatomy plus encounter function

### 10. Fake completeness
Severity: High

Symptom:
- agent reports success because model exists, but there is no proof of spawn, bind, states, drops, or QA

Why it happens:
- output judged by asset existence only
- report language outruns evidence

What to do instead:
- require lane-specific artifact contract
- require explicit PASS / FAIL / BLOCKED / INSUFFICIENT_EVIDENCE

## Iteration Anti-Patterns

### 11. Blind “make it better” loop
Severity: High

Symptom:
- each pass changes multiple things and quality drifts downward

Why it happens:
- no frozen baseline
- no one-axis rule
- vague revision request

What to do instead:
- freeze first acceptable pass
- change one axis only
- compare side-by-side every time

### 12. Third random pass after two failures
Severity: High

Symptom:
- instead of regrouping, the agent keeps improvising new variants

Why it happens:
- no recovery rule
- optimism bias in self-review

What to do instead:
- stop after two failures
- return to silhouette studies or reference decomposition

### 13. Reference soup
Severity: Medium

Symptom:
- the output feels like three references smashed together with no clear ownership of ideas

Why it happens:
- too many references chosen without subsystem boundaries

What to do instead:
- assign references by purpose: silhouette, rig, locomotion, focal treatment, YAML integration

### 14. Baseline overwrite
Severity: High

Symptom:
- the only decent version is replaced before its strengths are recorded

Why it happens:
- agent treats each new pass as a total rewrite

What to do instead:
- preserve accepted baseline
- log what must not regress before iteration continues

### 15. Wrong object-family drift
Severity: High

Symptom:
- intended beast/guardian/treant/aberration reads like an appliance, piece of furniture, random industrial slab, or generic golem

Why it happens:
- primary mass hierarchy was never solved
- focal point buried inside framing clutter
- geometry inherited from an old scaffold instead of rebuilt for the new family
- detail and material patches try to rescue the bad family read

What to do instead:
- name the intended creature family and the wrong-family failure explicitly in the brief
- judge the flat silhouette and distant read before detail
- if wrong-family drift is present after a pass, stop polish and do shell/blockout rebuild over the preserved rig contract

### 16. Theme-first massing
Severity: High

Symptom:
- model starts from bark, corruption, armor motifs, biome props, or other style language before the creature shell reads correctly

Why it happens:
- reference package was treated like a moodboard instead of a form contract
- the agent tries to make the theme carry the creature read
- the shell never got its own acceptance gate before detailing

What to do instead:
- rebuild the neutral shell first
- freeze Layer A reference-shell constraints before styling
- allow theme only as Layer B surface translation that cannot overwrite creature-family landmarks

### 17. Landmark migration
Severity: High

Symptom:
- muzzle, brow ridge, shoulder break, hip break, spine crest, paw placement, or other named landmarks drift away from the approved shell under a later pass

Why it happens:
- the team tracked vibes and silhouette only, not landmark lock points
- a styling pass was allowed to reshape the shell

What to do instead:
- lock named landmarks in a reference shell artifact
- reject any pass that moves them without rebuilding and re-approving Layer A

### 18. Outsourced judgment
Severity: High

Symptom:
- the agent waits for the user to explain that the model is boxy, dead, wrong-family, plank-like, furniture-like, or otherwise obviously bad

Why it happens:
- the agent treated references as mood only, not as learned shell truth
- there is no internal failure vocabulary for creature read
- the process values asset existence over visible quality

What to do instead:
- require autonomous self-critique before handoff
- name the failure family in blunt language as soon as it appears
- stop the pass at shell stage and rebuild before polish when the read is obviously wrong
- derive reusable rules from prior corpus study so the user does not have to keep reteaching the same visual lesson

### 19. Joint-chain laundering
Severity: High

Symptom:
- the model keeps a superficial silhouette but changes limb segmentation order, pivot logic, planted-foot logic, or tail-chain behavior relative to the approved shell

Why it happens:
- animation/integration was treated as a downstream concern
- shell review ignored motion-critical structure

What to do instead:
- record joint-chain order and pivots in the shell lock
- auto-fail when a later pass simplifies or swaps chain logic without explicit re-approval

### 19. Proportion drift under theme pass
Severity: High

Symptom:
- once corruption, bark, armor, or decorative mass is added, the body ratios silently stop matching the approved shell

Why it happens:
- Layer B styling was allowed to change Layer A structure
- reviewers accepted premium-looking noise instead of checking ratios

What to do instead:
- keep ratio checks inside the shell lock artifact
- compare current front/side/player-scale captures against the accepted shell before PASS

### 20. Decorative rescue of broken shell
Severity: High

Symptom:
- spikes, glow, straps, moss, corruption, or material breakup make a weak shell look temporarily impressive in close-up shots

Why it happens:
- the agent tried to rescue a broken body plan instead of rebuilding it
- reviewers judged beauty shots instead of shell parity

What to do instead:
- strip the read mentally back to the neutral shell during review
- if the creature family no longer reads without decoration, revert and rebuild the shell


Bad prompt smells:

- "ucz sie z tego i zrob dobre"
- "ulepsz to"
- "zrob bardziej premium"
- "inspiruj sie Toro"
- "dodaj detali"
- "napraw to na oko"

Better replacements:

- "extract 5-10 principles and 3-5 forbidden traits before proposing variants"
- "improve only silhouette hierarchy; do not change texture, rig, or YAML"
- "borrow Toro-grade silhouette pressure without copying anatomy or hero composition"
- "report BLOCKED if the shape still reads as generic armored humanoid"

### 21. Blind coordinate authoring
Severity: High

Symptom:
- a `reference_shell_lock.md` correctly names a real reference `.bbmodel`, but the
  actual shell-building script never reads that file — every `from`/`to`/`origin`
  is a number typed by the agent from a prose description of the reference
- output reads as box soup even though "a reference was used" per the paper trail

Why it happens:
- reference material was consumed as English description (atlas entries,
  translation plans) instead of as geometry to copy and transform
- nothing in the pipeline forced the agent to touch the real file's coordinates

What to do instead:
- see `mc-model-mob-reference-cloning-protocol.md`: build the shell with
  `modelengine:clone-shell` against the locked reference file(s), then run
  `modelengine:gate-shell` before any texture/detail pass. A model whose
  `<name>.bbmodel.provenance.json` covers less than ~60% of its elements was
  hand-authored, not cloned — treat that as this anti-pattern, not as done.

## Triage Rule

When reviewing a weak pass, identify:

1. the dominant anti-pattern
2. whether it is visual, gameplay, or iteration-related
3. the single next change most likely to remove it

If more than two high-severity anti-patterns are active at once, revert to a simpler baseline instead of polishing forward.

## Maintenance Rule

Add a new anti-pattern only when:

- it has happened more than once, or
- it is general enough to save future iterations across several mobs
