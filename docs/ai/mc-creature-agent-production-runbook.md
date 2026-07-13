# MC Creature Agent Production Runbook

Purpose: turn family reference packets into a production operating system for execution agents. This runbook is the bridge between reference learning and actual art/mobs/dungeon/rpg/qa delivery.

Use this when:
- a creature family packet already exists
- a new mob should be built from packet rules instead of vague inspiration
- multiple agents need a hard handoff contract
- you need a Definition of Done that respects Layer A gates before runtime claims

Do not use this as a replacement for the family packet.
The packet teaches the family.
This runbook tells agents how to execute from that packet without drifting.

## 1. Required inputs before any write pass

A production pass is BLOCKED until all of these exist:

- exact mob/model ID
- exact repo root: `$MC_ROOT`
- exact current local model path if one exists
- exact family reference packet path
- exact evidence directory for the current pass
- exact lane owner: `art`, `mobs`, `dungeon`, `rpg`, or `qa`
- exact scope of allowed files
- exact previous accepted baseline or explicit note that none exists yet

Minimum acceptable family packet fields:
- reference inventory
- per-reference extraction ledger
- rule IDs with `Evidence -> Inference -> Execution rule`
- criticality / transfer / confidence
- dependency graph
- failure taxonomy
- rebuild triggers
- repair playbooks
- scoring / exit matrix
- downstream response contract
- exit-bucket templates

If the packet does not have these, the first owner is not allowed to improvise around the gap. The packet must be repaired first.

## 2. Lane order

Default execution order:
1. read-only brief refresh if the packet/baseline is stale
2. `art` Layer A shell pass
3. `art` Layer B style pass
4. `mobs` integration
5. optional `dungeon`
6. optional `rpg`
7. `qa`

Hard rule:
- `mobs`, `dungeon`, and `rpg` must never accept a creature handoff that is below `PASS_TO_STYLE`.
- runtime-facing lanes may consume `PASS_TO_RUNTIME` directly, or a `PASS_TO_STYLE` baseline plus explicitly in-scope implementation work, but never `ITERATE_LOCAL`, `REBUILD_SHELL`, or `INSUFFICIENT_EVIDENCE`.

## 3. What every execution agent must read first

Before writing, the agent must read:
- the family reference packet
- the current local work packet
- the current accepted baseline evidence paths
- the exact files it is allowed to edit

The agent must quote back in its own handoff:
- which rule IDs it is implementing
- which rule IDs are intentionally untouched
- which anti-patterns it is actively avoiding
- which exit bucket it is trying to earn
- for creature art, which visible failure family it ruled out or named in blunt self-critique before handoff

If the handoff does not name rule IDs, it is not a production handoff.

## 4. Art lane contract

### 4A. Art lane goal
Build or modify the creature from packet rules, not from memory and not from generic creature instincts.

### 4B. Art lane non-negotiables
- Blockbench-first on the real `.bbmodel`
- one-axis-only per iteration
- no overwriting best baseline without a backup
- no style polish on top of broken Layer A
- no FX used to rescue shell failures
- Layer A shell elements must come from `modelengine:clone-shell` against the
  reference file(s) named in `reference_shell_lock.md` — hand-typed cube
  coordinates are not a valid Layer A pass (see
  `mc-model-mob-reference-cloning-protocol.md`, anti-pattern #21). Run
  `modelengine:gate-shell` before claiming `PASS_TO_STYLE` or `PASS_TO_RUNTIME`;
  a FAIL forces `ITERATE_LOCAL` or `REBUILD_SHELL`, never a pass. Also run
  `modelengine:check-render-discipline` before claiming any freehand/detail
  pass done — a FAIL means elements were batched without an in-between render.
  Also run `modelengine:check-content-completeness` before claiming
  `PASS_TO_RUNTIME` — a FAIL means the texture is unpainted placeholder UV
  reuse or the model has no real animation.

### 4C. Art lane minimum output
- updated `.bbmodel` or explicit BLOCKED reason
- front / side / three-quarter / player-scale evidence paths
- autonomous self-critique naming any obvious failure family still present or stating that none remain with evidence
- any required action-pose evidence paths named in the packet
- downstream response contract fully filled
- exit bucket selected from:
  - `PASS_TO_STYLE`
  - `PASS_TO_RUNTIME`
  - `ITERATE_LOCAL`
  - `REBUILD_SHELL`
  - `INSUFFICIENT_EVIDENCE`

### 4D. Art lane Definition of Done
Art is done for this pass only when:
- requested scope files exist
- exact rule IDs implemented are named
- required evidence paths exist
- no forbidden anti-pattern is ignored
- chosen exit bucket is justified by the packet's scoring matrix
- if `PASS_TO_STYLE` or `PASS_TO_RUNTIME` is claimed, all blocking `CRITICAL` rules are PASS
- obvious shell/read failure was either named and routed to the correct non-pass bucket or proven absent in current evidence

If any `CRITICAL` rule is FAIL, art cannot claim success.

## 5. Mobs lane contract

### 5A. Mobs lane entry gate
Mobs lane may start only if the incoming art handoff is:
- `PASS_TO_STYLE` with exact evidence paths and a stable model baseline
or
- `PASS_TO_RUNTIME`

Rejected handoff states:
- `ITERATE_LOCAL`
- `REBUILD_SHELL`
- `INSUFFICIENT_EVIDENCE`
- missing rule citations
- missing evidence paths

### 5B. Mobs lane minimum output
- blueprint binding proof
- MythicMobs state/skill mapping
- helper-bone / interaction-bone notes if relevant
- reload/runtime validation attempted
- exact changed file paths
- rollback note

### 5C. Mobs lane Definition of Done
Mobs is done only when:
- model path and mob IDs resolve exactly
- bindings point at the intended shipped blueprint
- runtime/reload proof is captured or the report is explicitly BLOCKED
- no art-lane shell failure is being silently laundered as a runtime issue

## 6. Dungeon lane contract

Dungeon lane may start only after model identity, mob ID, spawn hooks, and rollback notes are explicit.

Dungeon done means:
- exact region/instance/spawn files changed are named
- encounter entry/exit/reset assumptions are explicit
- creature placement respects the approved player-distance read
- blockers are explicit if runtime validation is unavailable

## 7. RPG lane contract

RPG lane may start only after creature identity and role are stable.

RPG done means:
- exact reward/drop/progression files changed are named
- role fantasy matches the approved family packet and art read
- no reward language contradicts the creature read
- rollback note exists for player-facing economy/progression changes

## 8. QA lane contract

### 8A. QA must verify the lane chain, not just one file
QA checks:
- packet completeness
- art exit bucket correctness
- evidence path existence
- baseline preservation
- integration/runtime proof where claimed
- that downstream lanes did not accept illegal handoffs
- that creature-art workers did not outsource obvious visual judgment back to the user

### 8B. QA allowed verdicts
- `PASS`
- `FAIL`
- `BLOCKED`
- `INSUFFICIENT_EVIDENCE`

### 8C. QA Definition of Done
QA is done only when:
- every required artifact path is named
- every required evidence path is named
- every lane handoff respected the packet contract
- the final verdict matches the strongest proven state, not the most optimistic prose

If proof is partial, QA must prefer `BLOCKED` or `INSUFFICIENT_EVIDENCE` over fake green.

## 9. Exit-bucket routing rules

### `PASS_TO_STYLE`
Meaning:
- Layer A is good enough for style work
- downstream destination: `art` style pass only
- not yet permission for runtime/integration success claims

### `PASS_TO_RUNTIME`
Meaning:
- all relevant structural and important quality rules passed
- downstream destination: `mobs`, then optional `dungeon`/`rpg`, then `qa`

### `ITERATE_LOCAL`
Meaning:
- the same lane may perform one narrow repair pass
- downstream destination: same owner only
- all non-target axes stay frozen

### `REBUILD_SHELL`
Meaning:
- stop polishing
- reset to Layer A rebuild
- downstream destination: `art` only
- all non-shell work is blocked

### `INSUFFICIENT_EVIDENCE`
Meaning:
- capture first, claims later
- downstream destination: same owner only until proof gap is closed

## 10. Universal handoff template

Every lane should hand off in this shape:

```md
Lane:
Mob ID:
Family packet:
Allowed files touched:
Implemented rules:
Blocked rules:
Known deviations:
Anti-patterns avoided:
Evidence captured:
Validation attempted:
Exit bucket:
Next owner:
Rollback note:
```

## 11. Global Definition of Done for a creature slice

A creature slice is actually done only when all of the following are true:
- the family packet exists and is strong enough to execute from
- the current best model baseline is backed by exact evidence paths
- the final art pass happened Blockbench-first on the real `.bbmodel`
- the last accepted art handoff is `PASS_TO_RUNTIME`
- integration lanes changed only in-scope files and recorded rollback notes
- QA ends with `PASS` based on real evidence rather than inference

A successful import alone is not DoD.
A pretty screenshot alone is not DoD.
A packet without a valid execution chain is not DoD.

## 12. Fast reject conditions

Reject or bounce the pass immediately when any of these happen:
- the worker cites vibes instead of rule IDs
- the worker uses FX to defend a bad shell
- the worker reports completion without exact evidence paths
- a downstream lane accepts `ITERATE_LOCAL`, `REBUILD_SHELL`, or `INSUFFICIENT_EVIDENCE` as if it were production-ready
- QA says `PASS` while any required proof path is missing
- the pass overwrote the best known baseline with no backup or rollback note

## 13. Short operating line

Reference packet first, lane contract second, evidence third, runtime last. If the packet says the shell is broken, nobody downstream is allowed to pretend otherwise.