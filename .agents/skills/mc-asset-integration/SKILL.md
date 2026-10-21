---
name: mc-asset-integration
description: Integrate and statically validate approved Minecraft visual assets across ModelEngine, MythicMobs, Nexo, MMOItems, MythicCrucible, and repository evidence contracts. Use after an exact source revision has `ART_PASS`, or when diagnosing asset delivery and binding without claiming art or runtime success.
---

# MC asset integration

Connect an art-approved source to its declared delivery profile and prove the strongest state supported by repository evidence.

## Entry and ownership gates

Require an evidence-bound `ART_PASS` for the exact source revision before promotion. Do not override `ITERATE`, `REBUILD`, `BLOCKED`, `INSUFFICIENT_EVIDENCE`, or stale-hash evidence. For diagnosis without `ART_PASS`, remain read-only and report the blocked promotion.

Treat Nexo as the owner of the merged resource pack while `MCMMORPG/plugins/Nexo/settings.yml` enables ModelEngine import. Treat generated packs, exports, and caches as outputs, never authoring sources. Do not create a second pack owner or hand-merge ModelEngine output around Nexo.

## Integrate by delivery profile

1. Resolve the declared asset ID and source revision exactly.
2. For animated entities, verify `.bbmodel` identity, ModelEngine blueprint placement, MythicMobs model/state/skill bindings, bones, animation names, and timing contracts.
3. For static items, weapons, armor, furniture, or props, keep presentation and pack merge in Nexo. Add MMOItems or MythicCrucible only when the gameplay semantics require them.
4. Run the applicable commands from `MCMMORPG/_validation/package.json` and record exact outputs. A parser, schema check, or successful build proves only static integration.
5. Report changed files, resolved IDs, validator evidence, rollback inputs, and the strongest justified handoff status: `READY_FOR_INTEGRATION_GATE`, `FAIL`, or `BLOCKED`. Domain 17, not the integrator, awards `INTEGRATION_PASS`.

## Load references selectively

- Read `docs/ai/plugin-knowledge-map.md` only for the plugin surfaces involved; do not guess commands, keys, placeholders, or ownership.
- Read the deployment, naming, and validation portions of `docs/ai/blockbench-mcp-mob-workflow.md` for ModelEngine and MythicMobs delivery.
- Read sections 10-14 of `docs/ai/modelengine-model-production.md` only when runtime bindings, combat synchronization, states, or sensory hooks are in scope.
- Read the mobs and QA lane portions of `docs/ai/mc-creature-agent-production-runbook.md` only for creature handoffs.

## Runtime boundary

Do not directly edit `.bbmodel` or issue an art verdict. Do not start or change staging/live, install plugins, regenerate or distribute packs, reload or restart servers, spawn entities, change worlds, or mutate player data without explicit approval. Without approved runtime evidence, never claim `RUNTIME_PASS`, `RELEASED`, or player-visible success.
