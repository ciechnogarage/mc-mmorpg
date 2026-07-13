# MC Creature Reference Extraction Packet Template

Purpose: turn strong direct creature references into reusable execution rules so the next model can be built from packet + memory instead of vague recollection.

Use when:
- learning a new creature family
- a family packet/runbook does not exist yet
- a pass drifted and the existing packet was too weak

Do not use as a surface-copy sheet.
Use it to extract method, shell logic, landmark logic, motion logic, and material logic.

## Required inputs
- exact target creature / mob ID
- exact local active model path if one already exists
- at least 3 exact direct corpus references
- for each reference: exact `.bbmodel` path and any available render / animation evidence
- optional 1-2 external principle references only if local corpus is insufficient

## Output contract

### 0. Reference inventory
For every source file, record one row:
- exact path
- source type: `.bbmodel`, render, animation frame, texture sheet, external principle ref
- state: `GOTOWE`, `CZĘŚCIOWE`, or `PUSTE`
- why it is usable
- what it cannot tell you

This section must be file-by-file, not an aggregate blob.

### 1. Creature family and first-read promise
- What family must this read as immediately?
- What must a player understand at first glance?
- What is the one-sentence visual promise?

### 2. Wrong-family reads to reject
List the exact bad reads that fail this family.
Examples:
- appliance
- furniture
- armored humanoid
- generic golem
- cube pile
- rat / ferret / log-creature
- add family-specific failures

### 3. Per-reference extraction ledger
For each reference file, return:
- exact path
- role:
  - shell / silhouette anchor
  - landmark / head anchor
  - locomotion / joint-chain anchor
  - material / texture anchor
  - secondary style anchor
  - validation anchor
- state: `GOTOWE`, `CZĘŚCIOWE`, or `PUSTE`
- 2-4 concrete facts extracted
- 1-2 biggest gaps / limits
- what must be borrowed
- what must not be copied literally

Rule:
- no vague lines like "good silhouette" without naming what exactly is good
- prefer concrete extracted facts like counts, chain names, mass order, named landmarks, material/focal rules, and specific failure reads
- every later family rule should cite at least one source from this ledger

### 4. Structural data ledger
If the `.bbmodel` exposes objective data, record it explicitly:
- element count
- outliner group count
- hierarchy depth
- animation count
- texture payload count / sizes
- key subtree names
- obvious helper/VFX subtrees

If data is unavailable, say so explicitly instead of skipping it.

### 5. Family mass order
Name the primary-to-tertiary mass hierarchy.
Example shape:
1. primary mass
2. secondary mass
3. tertiary mass
4. extension / counterweight

State what happens if this order is lost.

### 6. Shell rules
Write explicit shell constraints.
Include:
- body axis
- long vs tall logic
- width vs height logic
- front-read priorities
- side-read priorities
- three-quarter-read priorities
- player-distance read

### 7. Landmark package
Name the non-negotiable landmarks.
Include as needed:
- skull / head package
- muzzle / beak / jaw package
- shoulder / chest package
- rib / abdomen / pelvis package
- limb landmarks
- tail / horn / fin / wing landmarks
- silhouette breaks that must survive simplification

### 8. Joint-chain and pivot logic
Record the movement structure, not just shape.
Include:
- chain order per limb or appendage
- major bend points
- planted-contact logic
- pivot sensitivity
- parts that must stay separated for animation

### 9. Motion constraints
Name the poses/actions the shell must survive.
Examples:
- idle tension
- walk/run
- bite / swipe / slam
- dodge / leap
- hurt / death / recovery

For each important action, say which body parts must visually carry it.

### 10. Texture / material rules
Extract material logic, not skin decoration.
Include:
- material hierarchy
- contrast placement
- focal accents
- plane-description rules
- what kind of noise is forbidden
- how texture supports shell instead of rescuing it

### 10A. Evidence -> inference -> execution-rule map
For at least 5 important rules, write one compact chain:
- Rule ID
- Criticality: `CRITICAL`, `IMPORTANT`, or `NICE_TO_HAVE`
- Evidence: exact source file(s) and the observed fact
- Inference: what that fact means structurally or behaviorally
- Execution rule: what the next model must do because of it

Format example:
- `R1` [`CRITICAL`] Evidence -> Inference -> Execution rule

Do not allow free-floating rules with no evidence lineage.

### 10B. Transfer map and confidence
For each major rule, mark whether it is:
- `GENERAL` = safe to transfer across many creature families
- `FAMILY` = safe inside this family only
- `REFERENCE-SPECIFIC` = tied to this exact package/vendor styling

Also mark confidence:
- `HIGH` = supported by multiple direct sources
- `MEDIUM` = supported by one strong source
- `LOW` = plausible but still under-tested

### 10C. Evidence matrix by file/view/frame
Build a compact matrix where each important source says:
- exact file/view/frame
- which rule IDs it strongly supports
- which rule IDs it weakly supports
- which claims it cannot justify

Example columns:
- Source
- Strong support
- Weak support
- Cannot justify

This prevents one source from being over-credited for claims it cannot actually prove.

### 10D. Contradiction-resolution policy
If sources appear to disagree, resolve in this order unless a stronger reason is documented:
1. direct `.bbmodel` structure / hierarchy
2. neutral review renders (front/side/three-quarter/player-scale)
3. representative animation frames
4. style / VFX references
5. derived prose/docs

Required output when conflict exists:
- conflicting sources
- what each one suggests
- chosen winner
- why that source wins
- whether confidence dropped because of the conflict

### 10E. Blind spots / unresolved questions
Add an explicit list of what remains under-proven.
Typical examples:
- only sampled frames, not full curve/timing analysis
- hierarchy known but pivots not fully curated
- packaging/VFX lesson may be reference-specific
- family transfer still unproven outside one case study

### 11. Layer A lock vs Layer B freedom
List explicitly:
- Layer A sacred constraints that later styling cannot break
- Layer B freedoms allowed after Layer A is proven

### 12. Build-order rules
Write the order the next model must be built in.
Minimum structure:
1. family shell
2. landmark lock
3. joint-chain / motion viability
4. selective secondary style language
5. texture pass
6. integration / runtime

### 13. Forbidden traits and anti-patterns
List 5-10 hard fails.
Examples:
- equal-importance box soup
- decoration-first shell
- detail trying to rescue weak massing
- landmark drift
- joint-chain laundering
- texture noise flattening form
- wrong-family read surviving after polish

### 14. Acceptance bar for the next geometry pass
State exactly what the next pass must prove.
Include:
- what must read at distance
- what would instantly fail
- required evidence views
- what is still blocked until later

### 14A. Rule-verification gate
Before any downstream pass is accepted, add a compact verification table:
- rule ID
- status: `PASS` / `FAIL` / `UNTESTED`
- evidence path for that verdict
- short note

Minimum rule:
- if any critical Layer A rule is `FAIL`, verdict cannot be PASS
- if too many critical rules are `UNTESTED`, verdict is `INSUFFICIENT_EVIDENCE`

### 14B. Rule dependency graph
For the major rule IDs, state which rules depend on which others.
Purpose:
- stop the agent from polishing downstream rules while upstream structure is still broken
- expose when one FAIL makes several later checks meaningless

Format example:
- `R4` depends on `R1` + `R3`
- `R5` depends on `R1` + `R2`

### 14C. Failure taxonomy
When a pass fails, classify the failure instead of writing vague prose.
Use one or more labels:
- `SHAPE_FAILURE`
- `FAMILY_FAILURE`
- `MOTION_FAILURE`
- `LANDMARK_FAILURE`
- `TEXTURE_HIERARCHY_FAILURE`
- `EVIDENCE_FAILURE`
- `STYLE_OVERWRITE_FAILURE`

For each label, add:
- which rule IDs failed
- visible symptom
- likely cause
- next repair direction

### 14D. Rebuild triggers
Define when local repair is allowed and when full shell rebuild is mandatory.
Minimum policy:
- 1 `CRITICAL` fail may allow a focused rebuild if the rest of Layer A is stable
- 2 or more `CRITICAL` fails = full shell rebuild, not polish
- repeated `FAMILY_FAILURE` after 2 passes = rebuild from neutral family shell
- `TEXTURE_HIERARCHY_FAILURE` alone may stay local if shell/family rules already pass
- `EVIDENCE_FAILURE` blocks verdict escalation until required captures exist

### 14E. Failure-specific repair playbooks
For each common failure label, define a short repair recipe.
Minimum format:
- trigger condition
- immediate rollback or preserve decision
- exact repair scope
- forbidden next step
- proof needed before the next verdict

Recommended playbooks:
- `FAMILY_FAILURE`: strip style language, restore neutral family shell, re-lock `R1` + `R2`, then re-capture front/side/three-quarter before any texture work
- `LANDMARK_FAILURE`: isolate head/forequarter package, rebuild jaw/cheek/brow/ear structure, forbid texture polish until landmark read returns
- `MOTION_FAILURE`: restore structural separations and test crouch/run/bite/leap poses before surface changes
- `TEXTURE_HIERARCHY_FAILURE`: freeze geometry, repaint focal/material hierarchy only, forbid geometry thrash unless structural rules also fail
- `EVIDENCE_FAILURE`: no quality verdict upgrade until required captures and packet citations exist

### 14F. Scoring / exit matrix
Define a compact exit gate for the current pass.
Minimum matrix should say:
- `PASS_TO_STYLE`: all `CRITICAL` rules PASS, no `FAMILY_FAILURE`, evidence complete
- `PASS_TO_RUNTIME`: all `CRITICAL` + all relevant `IMPORTANT` rules PASS, evidence complete, no unresolved drift
- `ITERATE_LOCAL`: at most 1 `CRITICAL` fail and no repeated family-read collapse
- `REBUILD_SHELL`: 2+ `CRITICAL` fails, repeated `FAMILY_FAILURE`, or dependency collapse on `R1`/`R2`/`R3`
- `INSUFFICIENT_EVIDENCE`: required views/files missing regardless of apparent quality

### 15. Short execution brief
End with 5-10 lines that a downstream art agent can use directly.
This is the compressed handoff from the packet.

### 15A. Downstream accountability check
Before a downstream geometry pass starts, it must cite:
- which packet rule IDs it is implementing
- which anti-patterns it is explicitly avoiding
- which constraints remain blocked until later

If the pass cannot cite those, it is not actually using the packet.

### 15B. Downstream art-pass response contract
Every downstream art pass should answer in this compact shape:
- `Implemented rules:` rule IDs + one-line action per rule
- `Blocked rules:` rule IDs not attempted yet and why
- `Known deviations:` any intentional deviation from the packet and why
- `Anti-patterns avoided:` named failures actively checked
- `Evidence captured:` exact view/file paths produced for this pass
- `Verdict requested:` `PASS`, `ITERATE`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE`

If the pass does not return this shape, the handoff is incomplete.

### 15C. Exit-bucket output templates
Use a different final response shape depending on the exit bucket.

`PASS_TO_STYLE`
- `Exit bucket:` `PASS_TO_STYLE`
- `Structural rules passed:` list the `CRITICAL` rules that passed
- `Style now allowed:` list the Layer B freedoms now unlocked
- `Still forbidden:` anything still blocked before runtime/integration
- `Evidence:` exact front/side/three-quarter/player-scale paths
- `Next lane:` expected next owner or next pass type

`PASS_TO_RUNTIME`
- `Exit bucket:` `PASS_TO_RUNTIME`
- `Rules passed:` all relevant `CRITICAL` + `IMPORTANT` rules
- `Approved artifact baseline:` exact model/review artifact paths
- `Residual risks:` anything still worth watching but not blocking
- `Evidence:` exact approval paths
- `Next lane:` runtime/integration owner

`ITERATE_LOCAL`
- `Exit bucket:` `ITERATE_LOCAL`
- `Failed rules:` exact rule IDs still failing
- `Failure taxonomy:` one or more labels
- `Local repair plan:` the one axis to fix next
- `Evidence to recapture:` exact required views/files
- `Do not touch:` what must remain frozen during this iteration

`REBUILD_SHELL`
- `Exit bucket:` `REBUILD_SHELL`
- `Why rebuild:` failed `CRITICAL` rules + dependency collapse or repeated family failure
- `Rollback point:` last acceptable shell or state to return to
- `Rebuild scope:` exact Layer A parts to restart
- `Forbidden thrash:` what not to keep tweaking
- `Required proof for re-entry:` exact captures needed before style work may resume

`INSUFFICIENT_EVIDENCE`
- `Exit bucket:` `INSUFFICIENT_EVIDENCE`
- `Missing evidence:` exact paths/views/citations still absent
- `What cannot be claimed yet:` blocked verdicts or stages
- `Capture plan:` exact missing evidence to produce next
- `Current best provisional read:` optional, clearly marked as provisional only

## Rule
If this packet is not filled, the next creature-family geometry pass is not ready.

## Extraction style rule
For each reference file, prefer:
- 2-4 most concrete facts
- 1-2 biggest gaps
- exact paths and named structures
- short factual bullets instead of long quotes

## Short prompt line
Use this in packets/prompts:

`Open direct creature references first. Extract family rules into a reusable packet: file-by-file facts, structural data, evidence matrix, contradiction handling, mass order, shell constraints, landmark package, joint-chain logic, motion constraints, material hierarchy, rule dependencies, failure taxonomy, rebuild triggers, repair playbooks, exit buckets, and a rule-verification gate. Build the next pass from those written rules, not from vague inspiration.`