# MC Model + Mob Critique Rubric

Purpose: give agents and reviewers a shared scoring language so they can judge whether a mob/model pass is actually getting better.

Use after any brief, silhouette study, baseline art pass, or integration handoff.

## Scoring Rule

Score each category from 1 to 5.

- 1 = broken or absent
- 2 = weak / confused
- 3 = serviceable but mid
- 4 = strong and production-usable
- 5 = excellent and reference-worthy for this repo

A pass should not move forward if any critical category scores 1 or 2.

Before scoring, the agent must write a blunt self-critique in failure language if the pass is weak. Do not wait for the user to point out obvious failure. Name the failure family yourself: box soup, wrong object-family drift, generic armored humanoid drift, theme-first massing, landmark migration, legs as posts/supports, detail rescuing a broken shell, no threat origin, dead forequarter, or similar exact diagnosis.

Critical categories:

- silhouette and distant read
- reference shell parity for creature mobs
- role clarity
- integration readiness
- evidence quality
- autonomous self-critique quality

## Categories

### 1. Silhouette and distant read

Question: can the player understand the creature class and dominant idea at gameplay distance?

Look for:

- one dominant outer read
- visible hierarchy of big > medium > small forms
- readable gaps / negative space
- identifiable shape in flat black silhouette

Red flags:

- box soup
- equal-width everything
- detail only visible in close-up

### 2. Mass hierarchy

Question: do the largest forms organize the creature clearly before surface detail appears?

Look for:

- dominant torso/trunk/body read
- limbs sized according to role
- support masses that reinforce, not compete

Red flags:

- no dominant mass
- random asymmetry
- noise replacing structure

### 2b. Reference shell parity for creature mobs

Question: does this pass preserve the approved creature-family shell from the reference package before styling is considered?

Look for:

- body axis and long-vs-tall proportion logic preserved
- head landmarks still readable
- limb chain and planted-foot logic preserved
- tail system and backline rhythm preserved
- styling layered on top of shell instead of replacing it

Red flags:

- theme accents erase the head wedge or muzzle read
- legs collapse into posts or furniture supports
- the model only reads correctly because of surface noise or decorative spikes
- hiding theme accents mentally would no longer leave the intended creature family
- any landmark migration or joint-chain substitution relative to the approved reference shell
- any proportion drift that requires prose justification instead of visible parity

Auto-fail conditions:

- landmark migration
- joint-chain substitution or laundering
- proportion drift introduced by theme pass
- decorative rescue of a broken shell

### 3. Role clarity

Question: can the player infer what this mob does from shape, stance, and focus?

Look for:

- tank / bruiser / caster / flanker / summon / beast cues
- attack-side or focus-side readability
- stance consistent with behavior
- correct creature family read before details (beast, guardian, treant, aberration, construct, etc.)

Red flags:

- visual role contradicts gameplay role
- reads like generic armored humanoid when it should read creature/colossus
- no danger focal point
- reads like the wrong object family entirely: appliance, furniture, random cube pile, generic golem

### 4. Originality within references

Question: does it feel informed by references without reading like a kitbash copy?

Look for:

- borrowed principle, original arrangement
- project-specific identity
- coherent local fantasy fit

Red flags:

- obvious vendor mimicry
- stitched-together reference theft
- style mismatch with repo world

### 5. Material and focal discipline

Question: are value, color, corruption, glow, and accents controlled?

Look for:

- one primary focal band or region
- corruption used as signal, not wallpaper
- bark/stone/flesh/material grouping that supports forms

Red flags:

- glow spam
- moss/corruption everywhere
- textures trying to save weak forms

### 6. Motion and animation support

Question: does the model support believable anticipation, impact, recovery, and idle life?

Look for:

- clear limb ownership
- weighted major actions
- space for delayed or trailing forms
- no geometry choices that sabotage animation arcs

Red flags:

- stiff blockout with no motion logic
- crown/limb clutter that will clip constantly
- fake dynamism with no rig support

### 7. Gameplay readability

Question: can the player read hit threat, weak points, and encounter logic?

Look for:

- attack side or threat side legible
- important focal point readable during combat
- shape supports phase or behavior signaling

Red flags:

- combat cues buried in noise
- same read from all states
- all danger areas visually equal

### 8. Integration readiness

Question: is the pass ready for ModelEngine + MythicMobs binding without guesswork?

Look for:

- stable model ID plan
- helper-bone intent where needed
- plausible hitbox / interaction anchor plan
- file graph and handoff clarity

Red flags:

- pretty art with no runtime thought
- no anchor strategy
- state or animation naming chaos

### 9. Evidence quality

Question: does the report prove what it claims?

Look for:

- exact reference paths
- exact changed paths
- preview or runtime evidence
- blocker honesty when uncertain
- front, side, three-quarter, and player-scale evidence for the current art pass
- side-by-side comparison against baseline or last accepted pass
- explicit shell-parity comparison against the approved reference artifact for creature mobs

Red flags:

- confident prose with no proof
- vague “inspired by references” claims
- no side-by-side comparison against baseline
- no screenshot/render evidence while claiming visual improvement
- no shell-parity comparison despite claiming creature-family fidelity

## Decision Thresholds

### 10. Autonomous self-critique quality

Question: did the agent correctly recognize and name what is bad without outsourcing judgment to the user?

Look for:

- direct naming of the failure family before proposing fixes
- clear statement of what makes the creature read wrong
- repair priorities ordered by shell importance, not decoration
- honest stop/go judgment when the pass is not good enough

Red flags:

- asking the user to keep doing the diagnosis
- vague language like "something feels off"
- treating visible shell failure as a small polish issue
- defending a technically changed but visually weak pass

Auto-fail conditions:

- the pass is obviously wrong-family / boxy / dead-read and the agent does not name it
- the user has to teach the agent what is visibly broken after the agent already saw the same corpus
- the report hides behind metadata, JSON, or file existence instead of visible quality judgment

### Advance to next lane
Allowed only when:

- all critical categories >= 3
- for creature mobs, reference shell parity >= 4
- autonomous self-critique quality >= 4
- average across categories >= 3.5
- no unresolved blocker in evidence or integration readiness
- no auto-fail condition triggered in reference shell parity

### Needs another focused iteration

Use when:

- one critical category = 2 but baseline is promising
- the failure is narrow and one-axis-only correction is realistic

### BLOCKED

Use when:

- references are insufficient
- baseline is unstable
- no clear next axis exists
- runtime contract is unknown

### Reject and revert

Use when:

- new pass regresses accepted baseline
- anti-pattern count increased without a compensating gain
- visual complexity rose while readability fell

## Short Scorecard Template

```md
mob_id:
pass_type:
baseline_compared_against:

scores:
- silhouette_and_distant_read:
- mass_hierarchy:
- role_clarity:
- originality_within_references:
- material_and_focal_discipline:
- motion_and_animation_support:
- gameplay_readability:
- integration_readiness:
- evidence_quality:

anti_pattern_hits:
- 

decision:
- ADVANCE | ITERATE | BLOCKED | REJECT_AND_REVERT

one_axis_next_step:
- 
```

## Mandatory Reviewer Habit

Always answer both:

1. what is working and must not regress?
2. what single thing should change next?

If the reviewer cannot answer both, the critique is not actionable.
