# Foundation Agent Reports

Purpose: short integration-grade reports from domain agents and the integrator.
This file stores conclusions, not raw dumps. Do not place secrets, private
player data, DB contents, or unscoped runtime logs here.

## Required Report Contract

Every agent report must include:

```md
Date:
Milestone:
Agent:
Scope:
Required proof:
Achieved proof:
Evidence:
Validation attempted:
Recommendation:
Why:
Risks:
Blockers:
Rejected assumptions:
Rollback trigger:
Next owner:
Integrator status:
```

Hard rule: file existence or config presence is not valid evidence for
`PLAYER_PROOF`, and is not sufficient by itself for `INTEGRATION_PROOF`.

## Proof Vocabulary

- `STATIC_CONTRACT`: spec/config/IDs/commands/values match.
- `SPEC_FIDELITY`: repo docs, config, IDs, and declared milestone behavior match.
- `VISUAL_FIDELITY`: player-visible layout and landmark quality are proven against the active references.
- `RUNTIME_PROOF`: runtime state shows the subsystem is loaded and callable.
- `INTEGRATION_PROOF`: the subsystem hands off correctly to the next seam.
- `PLAYER_PROOF`: the exact player path ran without non-equivalent shortcuts.

## Runtime Reporting Rules

- Runtime-touching reports must identify the scoped `latest.log` window or run artifact.
- If a runtime run happened, the report must name the lock owner or evidence pack under `MCMMORPG/_validation/runs/`.
- Graczowidoczne milestone'y muszą mieć `visual_evidence` albo jawny `BLOCKED` z powodem braku visual proof.
- QA reports must end with one of: `PASS`, `FAIL`, `BLOCKED`, `INSUFFICIENT_EVIDENCE`.
- Player-facing success chat without backend delivery proof must be reported as `BLOCKED` or `INSUFFICIENT_EVIDENCE`.
- A report verdict cannot be `PASS` if any milestone in the same artifact is `BLOCKED`, `FAIL`, or missing required proof.

## Current Integration Notes

- 2026-06-29: foundation validation now requires explicit proof levels and serialized runtime runs through `_validation/.runtime.lock`.
- 2026-06-29: `M1` remains blocked without visual evidence; `M2` remains blocked until starter backend delivery is proven, not just announced in chat.
- 2026-06-29: `M7` remains blocked until the exact dungeon player path is proven end-to-end.
