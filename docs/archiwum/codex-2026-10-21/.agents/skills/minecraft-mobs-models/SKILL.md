---
name: minecraft-mobs-models
description: MC mobs, bosses and models. Registry-backed MC task agent with strict evidence, validation, rollback, and handoff contract.
---
# MC mobs, bosses and models

## Scope
Own only this domain:
- MythicMobs
- ModelEngine
- bosses
- spawn hooks
- reward hooks

## Allowed Paths
- MCMMORPG/plugins/MythicMobs/**
- MCMMORPG/plugins/ModelEngine/**
- MCMMORPG/plugins/Nexo/**
- docs/ai/**

## Forbidden Paths
- **/.env*
- **/*secret*
- **/*token*
- **/DatabaseBackups/**
- **/playerdata/**
- **/userdata/**
- **/*.db
- **/*.sqlite
- **/*.sqlite3
- MCMMORPG/world*/**

## Read First
- `docs/ai/how-to-use-mc-agents.md`
- `docs/ai/plugin-agent-roster.md`
- `docs/ai/agent-workflows.md`
- `docs/ai/plugin-knowledge-map.md` when plugin syntax or IDs are uncertain
- Nearby local working examples before external docs
- `docs/ai/modelengine-model-production.md` (mandatory domain workflow)
- `docs/ai/mc-mob-model-agent-pipeline.md` (mandatory domain workflow)
- `docs/ai/mc-model-mob-learning-spec.md` (mandatory domain workflow)
- `docs/ai/mc-model-mob-critique-rubric.md` (mandatory domain workflow)
- `docs/ai/mc-model-mob-anti-pattern-catalog.md` (mandatory domain workflow)
- `docs/ai/mc-model-mob-reference-cloning-protocol.md` (mandatory domain workflow — read this before building any new Layer A shell)
- `docs/ai/blockbench-mcp-mob-workflow.md` (mandatory domain workflow — Blockbench MCP is the required execution layer for geometry/texture/animation edits; scripted `.bbmodel` edits are bootstrap/validation only, never the primary workflow)

## Required Inputs
- mob/boss/model ID
- spawn/death/reward expectation

## Workflow
1. Restate the exact task scope and role: primary, secondary, or validation.
2. Inspect the narrowest local files needed under allowed paths.
3. Prove IDs, commands, placeholders, permissions, models, or plugin syntax from local sources before acting.
4. Map runtime work to the affected Docker backend (`world`, `items`, or `hub`) and use Velocity for player entry.
5. If write-policy is `workspace-write`, apply only the smallest domain-owned change.
6. Validate on every affected area backend; do not start Paper natively or use the disabled `start.sh`.
7. Return the required contract; do not replace evidence with opinion.

## Required Evidence
- exact mob/model config path
- ID reference proof
- current model renders and quality manifest
- accepted reference-shell parity artifact and translation-plan proof from art handoff
- art handoff autonomous self-critique proving the worker recognized or ruled out obvious shell/read failure
- resolved model-state-modelpart-skill ecosystem contract
- spawn/death/reward validation

## Validation
Consider these commands or equivalent narrower checks:
- npm --prefix MCMMORPG/_validation run modelengine:quality
- npm --prefix MCMMORPG/_validation run modelengine:render -- --model <model>
- npm --prefix MCMMORPG/_validation run modelengine:ecosystem -- --model <model>
- npm --prefix MCMMORPG/_validation run modelengine:check
- npm --prefix MCMMORPG/_validation run modelengine:gate-shell -- --model <model.bbmodel>
  (required before any Layer B pass or production handoff on a new/rebuilt shell; FAIL means the shell wasn't cloned from real reference geometry via modelengine:clone-shell — see mc-model-mob-reference-cloning-protocol.md)
- npm --prefix MCMMORPG/_validation run modelengine:check-render-discipline -- --model <model.bbmodel>
  (required before claiming a freehand/sculpt pass is done; FAIL means elements were added without an in-between render — see the sculpt loop in mc-model-mob-reference-cloning-protocol.md)
- npm --prefix MCMMORPG/_validation run modelengine:check-content-completeness -- --model <model.bbmodel>
  (required before any PASS_TO_RUNTIME claim; FAIL means the texture is unpainted/placeholder UV reuse or the model has no real animation — see mc-model-mob-reference-cloning-protocol.md)

## Rollback
Rollback details are required for:
- boss skills
- drop/reward hooks
- model/resource references

## Output Contract
Return exactly these non-empty sections:

```md
scope:
evidence:
files_or_areas:
proposed_or_applied_changes:
validation:
risks:
blockers:
next_owner:
failure_family:
rebuild_or_iterate:
why_not_pass:
```

For creature-art / mobs / QA lanes, `failure_family`, `rebuild_or_iterate`, and `why_not_pass` are mandatory and are validated automatically. `failure_family` must use: box_soup, wrong_family_drift, wrong_object_family_drift, dead_forequarter, no_threat_origin, legs_as_posts, detail_rescuing_broken_shell, theme_first_massing, landmark_migration, rat_log_read, root_corridor, snout_stick, rodent_drift, furniture_like_read, appliance_like_read, generic_humanoid_drift, generic_golem_drift, no_visible_failure_family_remaining, not_applicable. `rebuild_or_iterate` must use: REBUILD_SHELL, ITERATE_LOCAL, PASS_TO_STYLE, PASS_TO_RUNTIME, BLOCKED, INSUFFICIENT_EVIDENCE, NOT_APPLICABLE.

## Hard Rules
- Do not inspect forbidden paths unless the orchestrator explicitly scopes them.
- Do not guess plugin IDs, placeholders, permission nodes, model IDs, MythicMobs skills, database settings, or economy effects.
- Do not claim done without evidence and validation.
- A successful ModelEngine import, cache entry, or spawn does not prove visual quality.
- Do not accept stale renders: evidence must match the current blueprint SHA-256.
- Return `INSUFFICIENT_EVIDENCE` when a changed model lacks current multi-view and runtime proof.
- Reject an art handoff that lacks autonomous self-critique of obvious visual failure or absence thereof.
- Reject art handoff if `reference_shell_lock` or equivalent reference-shell artifact is missing or any Layer A item is unresolved.
- Do not accept runtime-ready mobs when shell parity is unproven or only described in prose.
- Require exact opened reference `.bbmodel` paths, locked shell ratios, locked landmark list, locked joint-chain list, neutral-shell side-by-side proof, and an explicit statement that Layer B did not modify Layer A.
- Do not ask the user to choose internal routing or tooling.
- If blocked, name the exact missing input, attempted evidence path, and next owner.


## Good Output Example
See `examples/good.md`.

## Bad Output Example
See `examples/bad.md`.

## Blocker Example
See `examples/blocker.md`.
