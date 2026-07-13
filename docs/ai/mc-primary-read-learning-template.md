# MC Primary-Read Learning Template

Purpose: force the agent to learn the method of making a good creature, not merely to copy blocks from references.

Use before any new mob/model geometry pass.

## Inputs
- exact local active model path
- at least 3 exact direct corpus `.bbmodel` reference paths
- optional 1-2 external principle references

## Output contract

### 1. Intended family
- What is this supposed to read as at first glance?
- beast / guardian / treant / aberration / construct / etc.

### 2. Wrong-family failures to avoid
- appliance
- furniture
- armored humanoid
- generic golem
- cube pile
- add task-specific variants

### 3. Reference role map
For each reference, say what it teaches:
- silhouette anchor
- focal/head anchor
- strike-side / locomotion anchor
- massing anchor
- material hierarchy anchor

### 4. General principles learned
Write 5-10 principles.
These must be general making rules, not descriptions of one model.
Example categories:
- silhouette hierarchy
- big/medium/small organization
- trunk-to-head flow
- focal point placement
- asymmetry purpose
- corruption/material discipline
- stance and gameplay role read
- animation-friendly geometry

### 5. Build-method rules
Write 3-5 rules answering: how should this kind of model be constructed?
Examples:
- solve the dominant body mass before adding surface cubes
- place the focal point inside the main mass transition, not as a detached box
- assign asymmetry to attack-side or weight-side only
- simplify the top silhouette to 1-3 strong gestures before detail
- if flat silhouette fails, remove pieces instead of adding detail

### 6. Forbidden traits
Write 3-5 things that must not appear.
Examples:
- equal-importance box soup
- random side cubes with no anatomical role
- furniture collar around the head
- detail trying to rescue weak massing
- wrong creature family drift

### 7. Acceptance criteria for the next geometry pass
- what must read at player distance?
- what is the single most important visual promise?
- what evidence views are required?
- what score would fail immediately?

## Rule
If this template is not filled, the next geometry pass is not ready.
