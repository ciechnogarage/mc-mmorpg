# Instance smoke — dungeon_001

Prove **CLASSIC** dungeons create a **new instance per party** (not shared world).

## Steps

1. Start staging stack; run `npm run guard:dungeon-001` until `ok: true`.
2. Player A (fresh name): `/md play dungeon_001` — record instance id from PlaceholderAPI `%mythicdungeons_instance_id%` or server log.
3. Player A leaves (`/md leave` or finish).
4. Player B: `/md play dungeon_001` — instance id **must differ** from step 2.
5. Optional: two players same time — both in dungeon but separate instances if not party-shared.

## Pass criteria

- Different instance identifiers for sequential runs.
- Spawn in `dunstart` zone (not map center).
- No immediate `Iris Mantle.close` after leave.

## Fail actions

- See `loch-dungeon-qa-architect.md` preflight; restart backend before retry.
