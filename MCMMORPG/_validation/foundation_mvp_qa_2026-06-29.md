# Foundation MVP QA Evidence - 2026-06-29

Scope: staging foundation MVP vertical slice.

## Proof-Level Verdict

- `M1 UI`: `SPEC_FIDELITY`, `RUNTIME_PROOF`, `INTEGRATION_PROOF` without `VISUAL_FIDELITY` -> BLOCKED
- `M2 RPG`: `SPEC_FIDELITY`, `RUNTIME_PROOF`, `INTEGRATION_PROOF` without direct starter combat/usability proof -> BLOCKED
- `M3 Dungeon`: `RUNTIME_PROOF` only -> FAIL for closure
- `M4 Mobs`: `RUNTIME_PROOF` only -> FAIL for closure
- `M5 Economy`: `RUNTIME_PROOF`, `INTEGRATION_PROOF` -> PASS
- `M6 Ops`: `RUNTIME_PROOF` -> PASS
- `M7 QA`: `SPEC_FIDELITY`, `RUNTIME_PROOF`, `INTEGRATION_PROOF` without `VISUAL_FIDELITY` and `PLAYER_PROOF` -> FAIL

Hard rule for this artifact: config existence and partial automation do not count as `VISUAL_FIDELITY` or `PLAYER_PROOF`.

## Implemented Surface

- Hub/menu entry: `MCMMORPG/plugins/DeluxeMenus/gui_menus/foundation_nexus.yml`
- Menu registration: `MCMMORPG/plugins/DeluxeMenus/config.yml`
- Runtime QA harness: `MCMMORPG/_validation/play_level1.js`
- Existing dungeon/mob/reward stack:
  - `MCMMORPG/plugins/MythicDungeons/maps/level_1/config.yml`
  - `MCMMORPG/plugins/MythicDungeons/maps/level_1/functions.yml`
  - `MCMMORPG/plugins/MythicMobs/mobs/level1_grove.yml`
  - `MCMMORPG/plugins/MythicMobs/skills/level1_grove.yml`
  - `MCMMORPG/plugins/MythicMobs/droptables/level1_grove.yml`

## Static Validation

Command: YAML/reference checks over DeluxeMenus, MMOItems, MythicMobs, MythicDungeons targets.

Result: PASS.

Evidence:
- `foundation_nexus` is registered in DeluxeMenus config.
- menu opens under `foundation`, `stolica`, `nexus`.
- starter references `SWORD.STEEL_SWORD` and `CONSUMABLE.RECALL_POTION`.
- `level_1` dungeon files parse.
- `GroveGuardian` mob, skills, and droptables exist.
- `FunctionSpawnMythicMob` and `FunctionFinishDungeon` remain wired in `level_1/functions.yml`.

## Host Runtime Validation

Command: `java -Xms2G -Xmx4G -jar paper.jar --nogui`

Result: PASS.

Evidence:
- server reaches `Done`
- RCON listens on `25575`
- DeluxeMenus reports `4 GUI menus loaded`
- MythicDungeons loads `level_1`

## Runtime Fixes Applied During QA

- Fixed `foundation_nexus.yml` so DeluxeMenus reloads cleanly.
- Corrected portal action from console-style `md play level_1 %player_name%` to player-style `md play level_1`.
- Set `MCMMORPG/plugins/Nexo/settings.yml`:
  - `Pack.dispatch.send_pre_join: false`
  - reason: pre-join Nexo resource-pack dispatch blocked offline bot spawn before runtime QA.
- Rewrote `_validation/play_level1.js` to:
  - use fresh nicknames via `MC_QA_USER`
  - accept resource packs when advertised
  - use `minecraft:generic` damage for boss-kill validation

## Host Runtime Results

Command: `dm reload`, `dm list`

Result: PASS.

Evidence:
- DeluxeMenus reload succeeds.
- `foundation_nexus` is present in the 4 loaded menus.

Command: `node MCMMORPG/_validation/play_m1_m2.js`

Result: PASS.

Evidence:
- host-side gate waits for `Done` + synchronous RCON before joining, avoiding false negatives during Paper boot
- static contract parity was proven against:
  - `docs/ai/foundation-mvp-spec-values.md`
  - `plugins/DeluxeMenus/gui_menus/foundation_nexus.yml`
  - `plugins/MMOCore/commands.yml`
  - MMOItems and LuckPerms plugin aliases from live plugin metadata
- fresh user `FQA30180522` joined and opened `/foundation`
- foundation menu layout was proven by title plus expected materials in slots `10/12/14/16`
- city orientation click produced the expected route/goal messages pointing through starter -> profile -> Portal Nexus
- starter click produced:
  - player-visible starter message,
  - menu state transition toward claimed state,
  - runtime inventory delta,
  - no permission denial,
  - but RCON echoes for direct money/permission reads remained blank in this stack
- profile signal click opened the MMOCore character GUI (`Your Character`)
- portal selector click was proven through the real player command handoff `[player] md play level_1`, recorded in `latest.log`
- no fresh command/event exception appeared in the run log window

Known gaps / risks kept explicit:
- Starter bundle still lacks direct early-combat/player-proof even after config hardening; current evidence remains runtime/integration-focused.
- Runtime inventory probes serialize the granted MMOItems starter back as generic `minecraft:stone` data with components/lore, so proof is based on inventory delta + player-facing signal + menu state, not clean semantic item IDs.
- This pass proves M1/M2 contract and integration quality, not end-to-end dungeon completion.

Command: LuckPerms runtime gate check for `default`.

Result: PASS.

Evidence:
- Real player-path denial exposed the actual MythicDungeons command nodes required for the slice: `dungeons.play` and `dungeons.play.send`.
- Durable defaults were added in `MCMMORPG/permissions.yml`.
- Active-provider runtime storage was updated so `default` can execute the `md play level_1` path without permission denial.
- Final M5/M6 validation run `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T10-40-02-028Z_M5M6602028.md` passed the default-player dungeon command gate.
- This closes the minimal M5 dungeon-access gate in staging without broadening admin access.

Command: direct server-console starter economy probe.

Result: PASS.

Evidence:
- Exact console path `cmi money give MenuConsole5932 25` was executed through the live server console, matching the executor used by DeluxeMenus `[console]`.
- `latest.log` records `MenuConsole5932 new balance: 125.00€`.
- This proves the starter cash grant works on the real console execution path even though RCON echoes for `cmi money` remain blank in this stack.

Command: probe joins with offline bots after Nexo change.

Result: PASS.

Evidence:
- bots reach `login` and `spawn`
- previous no-spawn blocker was removed

Command: automated E2E with fresh users, e.g. `MC_QA_USER=FoundationQA3 node play_level1.js`

Result: FAIL.

Evidence:
- automated bot can join the server
- server log records `M5GateQA issued server command: /foundation` and `M5GateQA issued server command: /dm open foundation_nexus`
- there is no permission-denied evidence for the `foundation` or `dm open foundation_nexus` path in `latest.log`
- bot-driven `/md play level_1` path is not a faithful proxy here; server log records the bot command as `/md level_1`
- RCON/console variants did not provide a reliable substitute for the exact player-click path
- no confirmed boss spawn / finish / return chain was proven end-to-end by automation in this run

Command: dedicated M5/M6 validation harness.

Result: PASS.

Evidence:
- `node MCMMORPG/_validation/validate_m5_m6.js`
- report: `MCMMORPG/_validation/runs/foundation_bot_2026-06-29T10-40-02-028Z_M5M6602028.md`
- PASS for:
  - default gate file proof
  - menu command acceptance
  - dungeon command gate without permission denial
  - admin mutate path without LuckPerms storage mutation
  - starter item + claim backend
  - fresh-window runtime safety review with no unexpected new regressions

## Additional Runtime Findings

- `ModelEngine` logs `Skin URL is null` for offline bot users. This is noisy but did not prevent post-login bot spawn once Nexo pre-join dispatch was disabled.
- `M5GateQA` command probes show the server accepts the menu-entry commands, but mineflayer still times out waiting for the GUI window. Current evidence points at QA-driver/menu-window parity, not a `default` permission denial.
- Fresh `latest.log` review after the M5 permission change did not introduce new critical boot/runtime errors beyond the already accepted staging exceptions.
- Boss validation memory remains relevant: if the final finish-chain test is retried manually or with a better driver, use `minecraft:generic` damage on the real boss base to trigger death handlers reliably.
- Runtime state for prior bot joins can leave fresh bots spawning high in `world`, so position-only heuristics are not enough proof of a new dungeon start.

## Verdict

FINAL: FAIL

Reason:
- implementation exists and loads on host runtime
- `M1 UI` and `M2 RPG` now have host-side config/runtime proof
- `M1 UI` and `M2 RPG` now also have host-side contract proof against docs and plugin command surfaces
- menu registration/runtime load are proven
- full playable slice is still not proven because the exact player-path `Portal Nexus -> md play -> boss kill -> finish/return` has not passed end-to-end

## Next Owner Step

Validate with one exact player-path run from a real client:
1. join a fresh player
2. run `/foundation`
3. claim starter
4. click `Portal Nexus: level_1`
5. confirm boss spawn
6. kill boss
7. confirm finish, rewards, and return

If automation must continue, fix the QA driver so it can execute the exact player command path instead of bot chat or console approximations.
