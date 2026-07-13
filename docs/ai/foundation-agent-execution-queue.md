# Foundation Agent Execution Queue

Cel: delegowalna, atomowa kolejka dla repo-local `mc:agent`, zgodna z nowym
standardem dowodu i z serializacją runtime.

Źródła prawdy:

- wartości: `docs/ai/foundation-mvp-spec-values.md`
- status i proof levels: `docs/ai/foundation-long-run-ledger.md`
- task board: `docs/ai/foundation-mvp-task-board.md`
- operational contract: `docs/ai/foundation-long-run-prompt-pack.md`

## Ownership

- `M1` -> `ui`
- `M2` -> `rpg`
- `M3` -> `dungeon`
- `M4` -> `mobs`
- `M5` -> `economy`
- `M6` -> `ops`
- `M7` -> `qa`

One integrator owns final shared-file writes, ledger truth, and conflict resolution.

## Dispatch Standard

Each task handed to an agent must name:

- allowed writable paths
- forbidden paths
- dependency milestone(s)
- required proof level
- expected evidence
- validation command
- done criteria
- rollback trigger
- next owner

If any of these are missing, the task is underspecified and should be tightened before dispatch.

## Runtime Serialization

- Any task that boots the server, joins with a bot, or validates dungeon/runtime state must run exclusively.
- Exclusive runtime work must own `MCMMORPG/_validation/.runtime.lock`.
- Static audits may run in parallel; runtime-touching tasks may not.

## Current Ready Tasks

1. `qa`: keep `play_m1_m2.js` green under the hardened reporter/lock contract.
2. `qa` + `dungeon` + `mobs`: repair exact `Portal Nexus -> level_1 -> boss -> finish -> reward -> return` proof path.
3. `rpg`: close or explicitly preserve the `STEEL_SWORD required-level 6.0` gameplay risk.
4. `qa`: after one exact full PASS, repeat for three fresh users before closing `M7`.

## Stop Conditions

Stop a subtask, not the whole run, only when:

- the work needs secrets, paid assets, DB contents, or private player data
- production changes are required
- exact plugin syntax or IDs cannot be verified from local source/config
- the same failure class repeats three times without new evidence
- rollback for the touched subsystem is unclear

Do not stop while an independent milestone can still move.
