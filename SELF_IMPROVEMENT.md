# Foundation MVP QA Runbook

## Goal

Close `M7` with repeatable exact-player-path proof for:

`foundation menu -> starter -> Portal Nexus -> level_1 -> boss -> reward -> return`

This runbook is operational only. The source of truth for milestone status is
`docs/ai/foundation-long-run-ledger.md`.

## Quality Gate

- `M7` is not complete from docs, config, or one subsystem PASS.
- Player-facing success chat is never equivalent to backend delivery proof.
- Every run must report proof levels separately:
  - `SPEC_FIDELITY`
  - `VISUAL_FIDELITY`
  - `STATIC_CONTRACT`
  - `RUNTIME_PROOF`
  - `INTEGRATION_PROOF`
  - `PLAYER_PROOF`
- Full closure requires `PLAYER_PROOF` plus three consecutive passes for three fresh users.

## Runtime Safety Rules

- Staging only.
- One runtime-touching run at a time. The runner must own `MCMMORPG/_validation/.runtime.lock`.
- Use a unique offline username for every run.
- Do not use RCON `md kick`.
- Do not use `/kill`, console-kill, or other admin shortcuts as finish proof.
- Every run must scope its own `latest.log` window and fail on new command/event exceptions.

## Canonical Commands

```bash
cd /home/przemek/projects/MC
node --check MCMMORPG/_validation/validate_character_sector.js
node MCMMORPG/_validation/validate_character_sector.js
node --check MCMMORPG/_validation/foundation_bot.js
node --check MCMMORPG/_validation/foundation_runtime.js
node --check MCMMORPG/_validation/validate_m5_m6.js
node MCMMORPG/_validation/play_m1_m2.js
MC_QA_USER=FQA<unique> MC_QA_DIFFICULTY=NORMAL node MCMMORPG/_validation/play_level1.js
```

## Character Sector Gate

When the active task is `itemy/menu/klasy/rasy/inventory`, keep it separate
from dungeon, boss, reward, and full M7 proof work.

- Start from `CoreTools/Scripts/foundation_character_flow.yml`, then inspect
  race/class menus, MMOCore classes, MMOItems starter IDs, and MMOInventory.
- Do not claim the sector is clean from YAML presence alone.
- Starter loadouts must be usable by a fresh postać: no level or stat gate above
  the first playable state.
- Race sigils are identity items until PvP-safe racial passives are explicitly
  designed and validated.
- Hidden subclasses still need project-specific names and slot labels; default
  plugin labels are a quality failure.
- Validate this sector with `node MCMMORPG/_validation/validate_character_sector.js`.

## Acceptance

- `M1` and `M2` stay green under the hardened reporter.
- `M1` additionally requires a player-view visual evidence pack against the hub concept references before closure.
- `M2` additionally requires direct starter combat usability proof or a removed level gate.
- If starter/profile/menu runtime gives only a visible signal without backend proof, report `BLOCKED` or `INSUFFICIENT_EVIDENCE`, never `PASS`.
- `M3`, `M4`, and `M7` require one exact player-path PASS before any closure claim.
- Final `M7` acceptance requires:
  - three consecutive `FOUNDATION_QA_PASS` runs,
  - three fresh users,
  - no new critical command/event exception in each run window,
  - evidence packs written under `MCMMORPG/_validation/runs/`.

## Known Open Risks

- The old `STEEL_SWORD` starter risk is superseded for the character sector by
  the CoreTools class finalizer loadouts; keep `validate_character_sector.js`
  green before changing starter items again.
- Mineflayer chat / RCON parity is not assumed equivalent to a DeluxeMenus click path unless separately proven.
- Offline-bot `ModelEngine` skin warnings remain accepted staging noise only.

## Wolf-like creature modeling rule

For wolf-like MC creatures in this workspace, direct corpus deconstruction is mandatory before any generator edits.

Required direct sources:
- `projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`
- `MCMMORPG/_validation/reviews/ref_direwolf/front.png`
- `MCMMORPG/_validation/reviews/ref_direwolf/side.png`
- `MCMMORPG/_validation/reviews/ref_direwolf/three_quarter.png`
- representative animation frames such as idle/run/attack/leap

Required order:
1. extract hierarchy, massing, head landmarks, leg chain logic, tail logic, texture rules, and motion constraints from the direct reference package
2. build a plain wolf shell that works in front/side/three-quarter before decorative theming
3. add briar/root language only after the wolf read survives distance review
4. texture last; contrast and noise must reinforce form instead of flattening it
5. only then run integration/binding/runtime checks

Failure trigger:
- If validators pass but the beast still looks ugly or reads like furniture/golem/log instead of predator, stop polishing and go back to direct side-by-side reference deconstruction. Do not defend the model with technical passes.

## Immediate Next Loop

1. Keep `M7` blocked until the dungeon path is proven end-to-end.
2. If the next full run fails, record the exact seam: dispatch, dungeon start, boss identity, finish trigger, reward, or return.
3. After two failed attempts in the same seam, stop polishing heuristics and isolate the seam directly.
4. Repeat only after the ledger is updated.
