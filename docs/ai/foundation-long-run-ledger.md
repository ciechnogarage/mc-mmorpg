# Foundation Long Run Ledger

Current phase: M1 visual fidelity + M2 starter combat usability proof
Current owner: integrator
Current standard: every milestone reports `SPEC_FIDELITY`, `VISUAL_FIDELITY`, `RUNTIME_PROOF`, `INTEGRATION_PROOF`, and `PLAYER_PROOF` separately; no milestone is "done" from config existence alone

## Status Rules

- `SPEC_FIDELITY`: repo docs, config, IDs, slots, commands, and declared behavior match the intended milestone contract.
- `VISUAL_FIDELITY`: player-visible layout, landmarks, district readability, and mood are proven against the hub concept/blockout references when applicable.
- `STATIC_CONTRACT`: spec, config, IDs, commands, slots, and values match the declared contract.
- `RUNTIME_PROOF`: server/plugin/runtime state demonstrates the subsystem is loaded and callable.
- `INTEGRATION_PROOF`: the subsystem hands off correctly into the next subsystem.
- `PLAYER_PROOF`: the exact player path is executed without admin shortcuts or non-equivalent substitutes.
- `PASS_WITH_RISK`: required proof level is satisfied for the milestone, but an explicit gameplay or operability risk remains open.
- `BLOCKED`: the current owner has concrete blocker evidence and the next independent owner/path is named.

## Milestone Matrix

| Milestone | Required proof | Achieved proof | Status | Owner | Evidence | Open risks / blockers | Next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| M1 UI | SPEC_FIDELITY, VISUAL_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF | SPEC_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF | BLOCKED | `ui` | `foundation_nexus` aliases/slots/actions match spec; menu opens; city-orientation messaging works; portal handoff observed through real `[player] md play level_1` path; runtime route checkpoints can be traversed | no screenshot/video evidence pack yet proving fidelity against concept references; current runtime route proof is not enough to claim visual fidelity | capture and compare player-view evidence against concept/blockout/images |
| M2 RPG | SPEC_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF | SPEC_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF | BLOCKED | `rpg` | starter backend delivery is now proven on the real menu path via claimed-state transition plus player-facing signal; `profile` permission path is wired; config/spec align on `STEEL_SWORD required-level 1.0` | fresh-player direct early-combat usability is still unproven because the real `md play` path currently loads `level_1_*` without any combat entities (`zombie`, `wolf`, `husk`) in the instance window | keep starter backend path stable and repair exact dungeon-start combat spawn before retrying fresh-player proof |
| M3 Dungeon | RUNTIME_PROOF, INTEGRATION_PROOF, PLAYER_PROOF | RUNTIME_PROOF | FAIL | `dungeon` | `level_1` loads on boot; portal dispatch path is visible in runtime | exact `Portal Nexus -> fresh dungeon start -> finish/return` still not proven end-to-end | repair the exact player-path driver or prove it with a real client |
| M4 Mobs | RUNTIME_PROOF, INTEGRATION_PROOF, PLAYER_PROOF | RUNTIME_PROOF | FAIL | `mobs` | `GroveGuardian` loads and can exist at arena runtime | no exact combat death -> finish proof in a full run yet | validate real combat death on the exact player path |
| M5 Economy | RUNTIME_PROOF, INTEGRATION_PROOF | RUNTIME_PROOF, INTEGRATION_PROOF | PASS | `economy` | default dungeon gate validated; admin deny-path validated; starter backend and console-money path proven | docs/runtime node naming drift must stay closed | keep spec/runtime parity on active permission nodes |
| M6 Ops | RUNTIME_PROOF | RUNTIME_PROOF | PASS | `ops` | host boot reaches `Done`; fresh-window log review shows no unexpected new critical regressions | accepted staging noise remains: offline-bot `Skin URL is null`, MCPets YAML fallback | keep runtime lock and scoped log review on every run |
| M7 QA | SPEC_FIDELITY, VISUAL_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF, PLAYER_PROOF | SPEC_FIDELITY, RUNTIME_PROOF, INTEGRATION_PROOF | FAIL | `qa` | host-side `play_m1_m2.js` proves M1/M2 spec/runtime/integration and route checkpoints; dungeon QA remains runtime-only | no exact end-to-end player proof for dungeon completion; no visual fidelity evidence pack for release closure | three consecutive exact-player-path passes plus visual evidence pack |

## Last Successful Evidence

- `node MCMMORPG/_validation/play_m1_m2.js`: PASS
- `node MCMMORPG/_validation/validate_m5_m6.js`: PASS
- `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T10-49-40-522Z_FQA30180522.md`
- `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T10-40-02-028Z_M5M6602028.md`
- `MCMMORPG/_validation/foundation_mvp_qa_2026-06-29.md`

## Last Failed Evidence

- `MC_QA_USER=FoundationQA3 node MCMMORPG/_validation/play_level1.js`: FAIL
- `MC_QA_USER=M5GateQA node MCMMORPG/_validation/play_level1.js`: FAIL
- `node MCMMORPG/_validation/play_m1_m2.js`: FAIL
- `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T12-04-53-439Z_FQA34693439.md`
- `node MCMMORPG/_validation/play_m1_m2.js`: BLOCKED
- `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T17-19-15-097Z_FQA53555097.md`
- `node MCMMORPG/_validation/play_m1_m2.js`: BLOCKED
- `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T19-05-34-832Z_FQA59934832.md`
- failure seam: exact full player-path proof still diverges on dungeon execution/finish parity
- resolved seam: starter menu backend delivery is now proven through claimed-state transition on the real menu path
- hard gate now enforced: player-facing starter chat no longer upgrades the artifact verdict beyond `BLOCKED`

## Runtime Policy

- Runtime-touching validation is serialized. Any run that touches staging runtime must first own `MCMMORPG/_validation/.runtime.lock`.
- Scoped log review is mandatory. Proof applies only to the log window opened by the run itself.
- `VISUAL_FIDELITY` cannot be claimed from menu YAML, coordinate docs, or text-only runtime logs.
- `PLAYER_PROOF` cannot be claimed from config reads, RCON shortcuts, or approximate command paths unless the approximation is separately proven equivalent.
- Known accepted staging exceptions stay documented and must not be mixed with new regressions.

## Current Blockers

- The exact player-path proof `Portal Nexus -> md play -> fresh dungeon start -> boss kill -> finish/reward -> return` has not passed end-to-end.
- Mineflayer chat is not yet treated as equivalent to the DeluxeMenus click path when server logs show command-parity drift.
- Fresh-player starter combat usability remains blocked by a runtime seam: the exact `Portal Nexus -> md play` path is currently loading `level_1_*` without any combat entities to fight.
- Permission/economy hardening is not allowed to close M2 on its own; M2 still requires fresh-player combat proof after the starter/profile/dungeon gate contract is stable.

## Touched Surfaces

- `MCMMORPG/_validation/foundation_runtime.js`
- `MCMMORPG/_validation/foundation_bot.js`
- `MCMMORPG/_validation/validate_m5_m6.js`
- `MCMMORPG/plugins/DeluxeMenus/gui_menus/foundation_nexus.yml`
- `MCMMORPG/permissions.yml`
- `MCMMORPG/plugins/Nexo/settings.yml`
- `docs/ai/foundation-mvp-spec-values.md`
- `MCMMORPG/_validation/foundation_mvp_qa_2026-06-29.md`

## Integration Contract

- Active default-player gate source for foundation M1/M2 is `MCMMORPG/permissions.yml`.
- `plugins/CoreTools/core-perms.yml` is currently out of path because `core_perms.enabled: false`.
- Starter claim state is `foundation.starter.claimed` and should be treated as the only menu/backend marker for slot-12 claimed state.
- `/profile` is the MMOCore `player` alias and requires `mmocore.profile`.
- Portal Nexus handoff requires `dungeons.play`, `dungeons.play.send`, and `mythicdungeons.play.level_1`.
- Starter backend contract is console-driven: MMOItems give, CMI money give, then LuckPerms claim-node mutation.

## Next Action

1. Run `node --check MCMMORPG/_validation/foundation_bot.js` and `node --check MCMMORPG/_validation/validate_m5_m6.js`.
2. Add direct early-combat proof for a fresh starter player now that backend starter delivery is proven.
3. Capture player-view evidence for the hub against concept references.
4. Keep M7 blocked until one exact player-path run proves `M3+M4+M7` together.
