# MC Blockbench Review Gate

Use this gate for every creature/mob art pass before the art lane can claim `PASS`.

## Core rule

The real `.bbmodel` must be opened and iterated natively in Blockbench. Render scripts, JSON diffs, import success, and runtime visibility do not prove visual quality.

If a pass was not reviewed in Blockbench against accepted references, the strongest allowed verdict is `BLOCKED` or `UNVERIFIED`, never `PASS`.

## Required live review loop

While editing in Blockbench, review continuously in these views:

- front
- side
- three-quarter
- player-scale
- back / rear three-quarter when rear rhythm matters

For creature-reference-faithful passes, keep accepted concept and direct corpus references open side-by-side during review.

## Live Blockbench QA definition

For creature rescue work, “live Blockbench QA” means:

- the art worker edited the real `.bbmodel` in Blockbench
- the current capture set came from that live review session
- QA reviewed that current capture set between iterations
- QA critiqued appearance structurally, not just metadata or import state

## Required evidence per pass

Art lane must produce under the review directory:

- exported Blockbench captures:
  - front
  - side
  - three-quarter
  - player-scale
- one side-by-side sheet or note comparing current pass vs accepted reference
- one deviation / drift note:
  - what changed this pass
  - what stayed locked
  - what drift was caught and corrected in Blockbench
  - what still looks wrong
- one explicit verdict: `PASS`, `ITERATE`, `BLOCKED`, or `REJECT`

## Hard fail conditions

Do not allow `PASS` if any are true:

- the pass was mainly shaped by guessed numeric edits instead of live Blockbench review
- accepted concept/reference was not checked side-by-side during the pass
- current Blockbench captures are missing
- shell/family read fails in front, side, or player-scale
- the model relies on texture/noise/detail rescue for a weak shell
- the report claims improvement without naming drift corrections
- QA did not issue a structural appearance critique for rescue work

## Rescue-work QA rule

Between counted rescue iterations, QA must state:

- dominant visual failure
- exact structural reason the pass still looks wrong
- whether family read improved, premium read improved, both improved, or neither improved
- one required next axis only
- whether the current model would embarrass the server in current form

## Minimal per-pass checklist

- [ ] Real `.bbmodel` opened in Blockbench
- [ ] Accepted concept/reference open during review
- [ ] Front capture exported from Blockbench
- [ ] Side capture exported from Blockbench
- [ ] Three-quarter capture exported from Blockbench
- [ ] Player-scale capture exported from Blockbench
- [ ] Side-by-side comparison saved
- [ ] Drift/deviation note written
- [ ] Verdict explicit

## Prompt enforcement line

Use this sentence in art-lane prompts where applicable:

`Work Blockbench-first on the real .bbmodel. Keep accepted concept and direct references open side-by-side while editing. Export Blockbench front/side/three-quarter/player-scale captures for the pass. If the pass was not reviewed in Blockbench, you may not claim PASS. For rescue work, QA must critique the current live Blockbench capture set between iterations.`
