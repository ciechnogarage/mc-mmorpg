---
name: minecraft-plugin-inventory
description: MC plugin inventory. Registry-backed MC task agent with strict evidence, validation, rollback, and handoff contract.
---
# MC plugin inventory

## Scope
Own only this domain:
- plugin manifest
- compatibility matrix
- ownership routing

## Allowed Paths
- MCMMORPG/plugins/*.jar
- docs/plugin_manifest.yaml
- docs/compatibility_matrix.md
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

## Required Inputs
- plugin path or compatibility question
- current plugin docs if relevant

## Workflow
1. Restate the exact task scope and role: primary, secondary, or validation.
2. Inspect the narrowest local files needed under allowed paths.
3. Prove IDs, commands, placeholders, permissions, models, or plugin syntax from local sources before acting.
4. Map runtime work to the affected Docker backend (`world`, `items`, or `hub`) and use Velocity for player entry.
5. If write-policy is `workspace-write`, apply only the smallest domain-owned change.
6. Validate on every affected area backend; do not start Paper natively or use the disabled `start.sh`.
7. Return the required contract; do not replace evidence with opinion.

## Required Evidence
- plugin file/doc path
- ownership or compatibility finding
- startup log impact if runtime-adjacent

## Validation
Consider these commands or equivalent narrower checks:
- npm run collab:doctor
- rg -n "<plugin>" docs/plugin_manifest.yaml docs/compatibility_matrix.md

## Rollback
Rollback details are required for:
- plugin jar changes
- compatibility matrix edits that affect release gates

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
```



## Hard Rules
- Do not inspect forbidden paths unless the orchestrator explicitly scopes them.
- Do not guess plugin IDs, placeholders, permission nodes, model IDs, MythicMobs skills, database settings, or economy effects.
- Do not claim done without evidence and validation.
- Do not ask the user to choose internal routing or tooling.
- If blocked, name the exact missing input, attempted evidence path, and next owner.


## Good Output Example
See `examples/good.md`.

## Bad Output Example
See `examples/bad.md`.

## Blocker Example
See `examples/blocker.md`.
