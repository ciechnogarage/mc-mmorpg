# Vertical Slice E2E Runtime Gate

Status: planned runtime gate  
Backends: `hub`, `items`, `world` through Velocity

## Scenario

`Velocity login -> profile selection -> class selection -> region -> normal mob -> elite -> dungeon -> boss -> reward -> equip -> gameplay/stat change -> logout -> full restart -> reconnect -> verification`

## Test data

- disposable offline test account with stable UUID;
- one class and one known item definition;
- one mandatory region and one mandatory dungeon;
- one deterministic reward table;
- one explicit reward transaction ID;
- clean staging backup before the test.

## Steps and assertions

| Step | Assertion |
| --- | --- |
| Login through Velocity | Proxy accepts account and routes to the intended backend |
| Select profile/class | Authority state is created once and shown in UI/RCON |
| Enter region | Player reaches the designated content without hidden manual setup |
| Kill normal mob | Combat and baseline reward path produce expected evidence |
| Complete elite encounter | Encounter result identifies the player and source |
| Enter dungeon | New instance has a unique instance ID and clean state |
| Defeat boss | Boss result is attributable to the encounter instance |
| Grant reward | Reward contract fields are present and one transaction commits |
| Equip item | MMOItems definition and Nexo representation resolve correctly |
| Verify gameplay change | Stats or usable behavior changes, not only lore text |
| Logout and full restart | Backend exits and starts without critical exception |
| Reconnect | Profile, class, item, currency and progression match snapshot |
| Replay same reward transaction | No second reward; duplicate is audited |
| Leave/reset dungeon | No orphan instance or inherited player state remains |

## Verdict rules

- `PASS`: all assertions pass with complete evidence.
- `FAIL`: product behavior contradicts an assertion.
- `BLOCKED`: environment prevents the test before product behavior is observed.
- `INSUFFICIENT_EVIDENCE`: behavior may have occurred but required proof is
  missing.

Docker access failure must be reported as `BLOCKED` with
`VAL-RUNTIME-001`; it must not be converted into a product `FAIL`.
