# DR001 Staging Player QA Script

Status: waiting-for-live-approval
Environment: `Server — kopia/1.21.11`

## Solo Normal

- Level 12 player with `DR001_RUINED_GATE_KEY` can enter.
- Player without key cannot enter.
- Player below level requirement is blocked when MMOCore gate is wired.
- START relic counter reaches 3/3.
- START courtyard kill counter reaches 18/18.
- `DR001_GatewardenVaros` death opens collapsed stairwell checkpoint.
- MID rune pedestals reach 4/4 and wrong interaction spawns pressure.
- `DR001_CisternOracleNhalruun` flood, cleanse, and interrupt checks are visible.
- END `dr001_void_anchor` count reaches 3/3.
- `DR001_RuinedKingAurelion` loses shield after anchors.
- `dr001_loot_shrine` claims once and exit portal returns player safely.

## Party 2-5

- Party enters same instance.
- Boss HP scaling follows `BALANCE.md`.
- Wipe respawns at latest checkpoint.
- Disconnect/reconnect does not duplicate loot or reset objectives.
- Loot roll policy is observed and recorded.

## Exploit Checks

- Loot shrine cannot be claimed twice.
- Objective items cannot bypass intended dungeon state.
- Teleport/ender pearl bypass stays blocked by dungeon rules.
- DR001 mobs despawn after party leaves.
- Daily cooldown and Mythic weekly Crown Core lockout work.

## Current Verdict

Blocked for runtime evidence until:

- final intake assets are approved;
- staging import is authorized;
- MythicDungeons editor flow is wired;
- live runner command list is approved.
