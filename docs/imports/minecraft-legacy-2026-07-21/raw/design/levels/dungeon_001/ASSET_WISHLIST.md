# Asset Wishlist — dungeon_001

## Blocks

| Zone | Block |
| --- | --- |
| dunstart | `minecraft:mossy_stone_bricks` |
| dunmid | `minecraft:blue_ice` |
| dunend | `minecraft:polished_blackstone_bricks` |
| empty / fallback | `minecraft:air` |

## Mobs (MythicMobs)

| Role | Suggested id | Zone |
| --- | --- | --- |
| Mid sentinel | `Dungeon001DrownedSentinel` | dunmid |
| Mid caster | `Dungeon001Frostbinder` | dunmid |
| Boss | `Dungeon001Warden` | dunend |

## Signals

- `dungeon_001_start_objective_complete`
- `dungeon_001_mid_objective_complete`
- `dungeon_001_end_boss_defeated`

## Sounds / particles

- Start: subtle water drip (optional)
- Mid: frost crackle on wave start
- Boss: deep rumble on spawn
