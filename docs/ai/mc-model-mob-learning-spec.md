# MC Model + Mob Learning Spec

Purpose: convert "look at good examples and learn" into explicit production knowledge that agents can reuse without drifting into imitation, slop, or random regression.

Use this together with:

- `$MC_ROOT/docs/ai/mc-mob-model-agent-pipeline.md`
- `$MC_ROOT/docs/ai/modelengine-learning-ledger.md`
- `$MC_ROOT/docs/ai/mc-model-mob-critique-rubric.md`
- `$MC_ROOT/docs/ai/mc-model-mob-anti-pattern-catalog.md`

## What "learning" means here

For agents in this repo, learning does NOT mean retraining model weights on your assets.

It means:

1. inspect strong local and external references
2. extract general making principles, not just specific block arrangements
3. record them in explicit files
4. apply them to one narrow task at a time
5. judge outputs against a rubric and anti-pattern list
6. preserve accepted baselines instead of mutating blindly

The important bit: the agent is supposed to learn HOW these models are built well in general — silhouette hierarchy, massing, focal control, creature-family read, asymmetry logic, material discipline, animation-friendly forms, and integration discipline — not just reuse the same cubes from the references.

If a lesson is not written into a brief, manifest, rubric, review note, playbook, or filled primary-read learning artifact, assume it will be lost or inconsistently applied.

## Learning Inputs

Priority order:

1. local shipped or active project assets in `MCMMORPG/`
2. local corpus in `$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)`
3. local derived analysis docs
4. external vendors as principle references only

External vendor principle map:

- Littleroom
  - what to learn: encounter completeness, multi-system packaging, readable boss/miniboss framing
  - what not to copy: exact silhouettes, named characters, direct kit structure
- SamusDev
  - what to learn: RPG role clarity, class fantasy readability, archetype consistency
  - what not to copy: class-specific branding, exact gear language, direct thematic lifts
- Toro Toro
  - what to learn: creature-first silhouette, focal hierarchy, memorable monster read
  - what not to copy: exact creature anatomy, iconic set pieces, distinct premium hero shots

## Four Layers Of Agent Knowledge

Every good mob/model task should carry all four layers.

### 1. Corpus knowledge

Questions the agent must answer:

- which local models are nearest in anatomy, role, and encounter scale?
- which references are integrated, not just pretty?
- which examples prove helper bones, hitboxes, timeline events, or YAML linkage?

Minimum output:

- 3 local references with exact paths
- 1-3 external principle references if useful
- exact reasons each was chosen
- for creature mobs: identify which reference supplies Layer A shell truth versus which references only contribute secondary Layer B translation rules

### 2. Design principle knowledge

The agent must convert examples into principles such as:

- distant read must be clear before details matter
- one dominant mass beats many equal masses
- gameplay role should be inferable from silhouette and stance
- asymmetry should reinforce identity, not randomize forms
- helper bones and hit volumes exist for a gameplay reason, not as decoration
- animation timing should support anticipation, impact, recovery, and vulnerability
- more cubes or more detail is not automatic quality
- creature family must read correctly before surface treatment begins
- materials and corruption should reinforce the main masses, not fragment them
- the model should feel designed as one organism/construct, not assembled from unrelated boxes

Minimum output:

- 5-10 principles for this creature class
- 3-5 explicit forbidden traits
- 3-5 build-method rules describing how to construct the forms, not only what the final result should resemble

### 3. Project house-style knowledge

The agent must learn what "good for this repo" means, not only what is generally impressive.

Current repo-level bar:

- quality beats raw complexity
- runtime integration matters as much as aesthetics
- level fantasy and encounter role must read quickly
- baseline freezing is mandatory after first acceptable pass
- local references outrank abstract internet inspiration
- evidence beats confident prose

For level_1 / grove-style natural enemies, bias toward:

- readable organic massing
- corruption used as a focal accent, not noise wallpaper
- creature or colossus read over generic armored humanoid read
- player-distance readability over close-up texture tricks

### 4. Task-specific knowledge

The agent must know what this exact creature is supposed to be.

Required fields:

- mob_id
- level or dungeon context
- combat role
- target distant read
- one dominant visual promise
- what it must not be confused with
- required runtime hooks
- exact next gate after this pass

## What The Agent Must Learn To Judge

The agent should not only ask "can I generate something?"
It must ask:

- does this read clearly at player distance?
- is the role readable from shape, stance, and focal area?
- did I improve the chosen axis or just add noise?
- did I preserve what was already accepted?
- is this ready for the next lane, or am I hiding uncertainty with prose?

If those questions are unanswered, the work is not ready.

## Learning Loop

### Phase A — study
Output:

- reference set
- extracted principles
- anti-copy notes
- anti-pattern watchlist
- build-method notes: how the good references construct silhouette, mass transitions, focal point, limb emphasis, and material hierarchy

### Phase B — propose

Output:

- 2-3 directions only
- one recommendation
- one sentence on what each direction optimizes

### Phase C — baseline

Output:

- one accepted visual baseline
- quality manifest
- explicit note on what must not regress

### Phase D — critique

Output:

- score with the rubric
- anti-pattern hits
- go/no-go for next lane

### Phase E — integrate

Output:

- exact IDs and file graph
- runtime proof or blocker
- next owner handoff

## Baseline Discipline

When the first pass is mid but usable:

1. freeze it
2. note what works
3. request exactly one change axis
4. compare new pass against the frozen baseline

Never tell the agent only:

- "make it better"
- "learn from this"
- "improve the vibe"

That wording causes drift because the agent has no stable acceptance anchor.

## Two-Failure Recovery Rule

After two rejected visual iterations:

1. return to the best accepted baseline
2. produce side-by-side silhouette or massing studies
3. choose one direction explicitly
4. resume from the chosen direction only

No third random mutation pass.

## Definition Of Learning Success

A learning pass is successful only if it produces reusable explicit knowledge in at least one of these forms:

- reference study
- silhouette candidate review
- quality manifest
- critique rubric scorecard
- anti-pattern notes
- improved prompt packet
- clearer handoff contract

If the pass only produces a new model but no durable judgment framework, the agent did not truly learn; it only generated.

## Required Prompt Addendum For Learning Tasks

Add this block to any read-only study or first-pass art task:

```text
Do not imitate references directly at the surface level. First reconstruct Layer A 1:1 from the chosen reference package before any project styling.
Layer A means preserved body axis, proportion ratios, head landmarks, limb segmentation order, joint pivots/chain logic, planted-foot logic, tail/backline rhythm, and motion-critical separations.
Then state what to borrow and what not to copy: do not copy vendor textures, hero details, ornaments, or branded motifs; do preserve Layer A structure as explicit constraint.
Explain how the strong references achieve silhouette hierarchy, massing, focal control, creature-family readability, and motion support so the next pass can translate the method without destroying the locked shell.
For creature mobs, split the work into two layers: Layer A = sacred reference shell constraints; Layer B = original project translation (theme, corruption, material accents, local flavor). If any Layer B choice breaks Layer A, the pass fails.
```

## Required Prompt Addendum For Critique Tasks

```text
Judge this pass against the local baseline, the critique rubric, and the anti-pattern catalog. If quality is unclear, return BLOCKED or INSUFFICIENT_EVIDENCE instead of optimistic prose.
```

## Minimal Work Packet For "teach from references"

A valid task is:

```text
read-only brief for <mob_id> in $MC_ROOT; backend world; inspect local corpus refs plus selected vendor principles; open direct `.bbmodel`, render pack, and representative idle/run/attack/leap frames; fill `$MC_ROOT/docs/ai/mc-primary-read-learning-template.md` or a creature-specific equivalent; return exact references, 5-10 extracted principles, 3-5 build-method rules, 3-5 forbidden traits, 2-3 silhouette directions, one recommended baseline, a reference parity artifact, a translation plan, and the acceptance criteria for the next art pass; do not edit gameplay files
```

An invalid task is:

```text
ucz sie z tych modeli i potem zrob dobrego moba
```

## Maintenance Rule

When a successful mob teaches a new durable lesson, add it to one of:

- `modelengine-learning-ledger.md` for corpus-wide evidence-backed lessons
- `mc-model-mob-anti-pattern-catalog.md` for repeated failure modes
- `mc-model-mob-critique-rubric.md` if the scoring bar was missing something
- creature-specific `_validation` docs if the lesson is local to one mob
