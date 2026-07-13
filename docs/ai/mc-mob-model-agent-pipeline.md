# MC Mob + Model Agent Pipeline

Purpose: stop random quality drift while building or repairing ModelEngine / MythicMobs creatures in MCMMORPG.

Use with:
- `docs/ai/mc-model-mob-learning-spec.md`
- `docs/ai/modelengine-learning-ledger.md`
- `docs/ai/mc-model-mob-critique-rubric.md`
- `docs/ai/mc-model-mob-anti-pattern-catalog.md`
- `docs/ai/mc-mob-work-packet-template.md`
- `docs/ai/mc-creature-agent-production-runbook.md`

## Core rule

Do not give one agent one giant task spanning model, mob, rewards, placement, and validation. Split work into lanes and force evidence gates.

## Roles

- `art`: silhouette, blockout, Blockbench geometry, texture, preview evidence, source-of-truth visual brief
- `mobs`: MythicMobs + ModelEngine binding, animation/state linkage, helper bones, combat hooks, spawn/death proof
- `dungeon`: level flow, spawn placement, instance/reset/region concerns
- `rpg`: drops, reward tables, progression, MMOItems/MMOCore consequences
- `qa`: PASS/FAIL/BLOCKED/INSUFFICIENT_EVIDENCE gate

## Non-negotiable rules

1. One iteration changes one primary axis only.
2. Freeze the first acceptable baseline.
3. Local references outrank internet inspiration.
4. External vendor references are principle sources, not copying permission.
5. No task is done without evidence validation.
6. Derived docs and ledgers are not a substitute for direct `.bbmodel` inspection when the corpus is available.
7. If the user says the result is ugly, unworthy, or unlike references, treat it as a primary-read failure first, not a detail-polish request.
8. If a pass reads like the wrong object family, stop polishing and switch to shell/blockout rebuild over the preserved rig contract.
9. Blockbench-first is mandatory for creature quality. The real `.bbmodel` must be reviewed natively in Blockbench.
10. Numeric/script-generated edits may bootstrap or validate, but are not an acceptable primary workflow for final creature quality.
11. No visual pass may overwrite the current best baseline without a backup and a “must not regress” note.
12. QA must give real PASS/FAIL evidence, not soft opinion.

## Wrong-family rescue override

When a creature pass is judged ugly, unworthy, confusing, or wrong-family, do not enter a tiny-polish loop.

Required rescue sequence:
1. full direct `.bbmodel` reference autopsy
2. shell-lock / Layer A freeze
3. 5 large structural iterations with QA critique between each
4. only after art approval: `mobs` / runtime handoff

Large rescue iterations for predator quadrupeds must follow this exact order:
1. family shell rebuild
2. forequarter thrust / threat origin
3. backline + hip rhythm
4. asymmetry + focal hierarchy
5. planted-foot tension / spring-loaded stance

Rules for this override:
- each pass still changes one primary axis, but it must be a large structural axis, not tiny polish
- each pass must record what improved, what got worse, and what still looks wrong
- if family read still fails after iteration 3, restart shell instead of polishing forward
- if premium front read still fails after iteration 5, remain blocked in `art`
- no `mobs` handoff before final art approval

## Artifact contract per mob

Use `<mob_id>` consistently across files. Minimum set:
- `MCMMORPG/_validation/model_studies/<mob_id>_reference_study.md`
- `MCMMORPG/_validation/model_studies/<mob_id>_reference_parity.md`
- `MCMMORPG/_validation/model_studies/<mob_id>_reference_shell_lock.md`
- `MCMMORPG/_validation/model_studies/<mob_id>_translation_plan.md`
- `MCMMORPG/_validation/model_reviews/<mob_id>/`
- `MCMMORPG/plugins/ModelEngine/blueprints/<mob_id>/<mob_id>.bbmodel`
- `MCMMORPG/plugins/MythicMobs/Packs/<pack>/mobs/<mob_id>.mob.yml` when integration starts
- `MCMMORPG/plugins/MythicMobs/Packs/<pack>/skills/<mob_id>.skill.yml` when integration starts
- `MCMMORPG/plugins/MythicMobs/Packs/<pack>/droptables/<mob_id>.droptable.yml` when rewards are in scope

For rescue work, add:
- one full reference autopsy
- one 5-iteration rescue packet
- one per-iteration QA critique trail

## Blockbench and QA gate

For creature rescue work:
- art must review the real `.bbmodel` in Blockbench
- art must export front, side, three-quarter, and player-scale captures after every iteration
- QA must review the current live Blockbench-derived capture set between iterations
- QA must describe appearance failures brutally and specifically, not just say “still weak”

## Default route

1. read-only brief / reference study
2. art implementation
3. mobs integration
4. optional dungeon and/or rpg
5. qa validation

Rescue route:
1. reference autopsy
2. shell-lock
3. 5 large art iterations with QA critique between each
4. final art verdict
5. only then mobs/runtime handoff
