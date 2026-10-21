# DR001 Boss Spawn Smoke Status

Date: 2026-05-30
Verdict: `DEATH_SIGNAL_FIX_AND_BOSS_SMOKE_OK_HP_SKIPPED`
Target: `Server - kopia/1.21.11` staging only

## Summary

DR001 boss standalone death behavior was hardened for staging smoke. Direct
MythicDungeons `dsignal` calls are no longer reachable from standalone boss
`~onDeath` hooks; dungeon-only signal skills remain preserved for later full
MythicDungeons instance verification.

After config-only sync and MythicMobs reload, all three DR001 bosses spawned,
persisted by entity data, and were cleaned up in the approved QA arena:
`minecraft:overworld` / `world` at `0.5 128 64.5`, radius `32`.

## Runtime Artifact

- `qa/dr001-runtime-smoke/boss-spawn-smoke-20260530T144955Z.json`
- Status: `DR001_BOSS_SPAWN_SMOKE_PARTIAL_HP_SKIPPED`
- HP phase: skipped because no safe HP trigger command is approved/proven.

## Boss Spawn Smoke

| Boss ID | Info | Spawn | Persistence | Death/cleanup | DungeonSignal NPE | Runtime notes | HP phase | Result |
| ------- | ---: | ----: | ----------: | ------------: | ----------------: | ------------: | -------: | -----: |
| `DR001_GatewardenVaros` | PASS | PASS | PASS | PASS | NONE | Missing MMOItems material warning for `DR001_RELIC_SHARD` | SKIPPED | PASS |
| `DR001_CisternOracleNhalruun` | PASS | PASS | PASS | PASS | NONE | Missing MMOItems material warning for `DR001_RUNE_DUST` | SKIPPED | PASS |
| `DR001_RuinedKingAurelion` | PASS | PASS | PASS | PASS | NONE | Missing MMOItems material warnings for `DR001_ANCIENT_COIN`, `DR001_CROWN_CORE` | SKIPPED | PASS |

## Fix-Forward Notes

- First smoke pass spawned all bosses but vanilla selector persistence evidence
  was missing because the QA chunk was not loaded.
- The tiny allowlisted barrier platform fill initially reported
  `That position is not loaded`.
- Mineflayer arena hold loaded the staging QA chunk; platform fill then
  succeeded and the retest produced entity data for all three bosses.
- Temporary barrier platform was removed after the smoke.
- RCON returned a read timeout on the `mm reload` call, but server logs showed
  MythicMobs completed reload successfully.

## Remaining Non-Blocking Gates

- `validate_final_assets.mjs` remains expected fail until approved final intake
  exists.
- MMOItems material warnings are expected for reward/drop smoke and do not
  block this boss death-signal hardening pass.
- ModelEngine offline skin warning occurred when the QA bot joined; it is a
  visual/runtime warning and not a DR001 death-signal failure.
- Full MythicDungeons flow, rewards/economy, final asset import, and resource
  pack publish were not run.
