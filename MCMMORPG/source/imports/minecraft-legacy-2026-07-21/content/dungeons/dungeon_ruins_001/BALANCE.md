# DR001 Balance Sheet

Status: `release_prep_baseline`. Numbers are conservative starting values and
must be tuned through staging player tests before production.

## Difficulty Bands

| Difficulty | Min level | Party | Mob HP | Mob damage | Loot multiplier | Notes |
| --- | ---: | --- | ---: | ---: | ---: | --- |
| Normal | 12 | 1-3 | x1.00 | x1.00 | x1.00 | No Mythic-only anchor respawn. |
| Veteran | 20 | 2-4 | x1.65 | x1.35 | x1.50 | Faster casts and larger add packs. |
| Mythic | 30 | 4-5 | x2.60 | x1.90 | x2.40 | Anchor respawn and 120s final enrage. |

## Party Scaling

| Party size | Boss HP | Trash density | Loot rolls |
| ---: | ---: | ---: | ---: |
| 1 | x0.65 | 70% | 1 |
| 2 | x1.00 | 100% | 2 |
| 3 | x1.35 | 120% | 3 |
| 4 | x1.70 | 140% | 4 |
| 5 | x2.10 | 160% | 5 |

## Active Combat Roster

| Mob ID | Role | Stage | Level | Base HP | Damage | Mechanics | Drops |
| --- | --- | --- | ---: | ---: | ---: | --- | --- |
| `DR001_RuinThrall` | melee trash | START | 12 | 85 | 8 | slow hit, crumble particles | `DR001_CommonMobDrops` |
| `DR001_MossboundArcher` | ranged trash | START | 13 | 70 | 7 | root arrow, kiting | `DR001_CommonMobDrops` |
| `DR001_RootlingSwarmer` | swarm add | START | 12 | 38 | 5 | leap, low HP pressure | `DR001_CommonMobDrops` |
| `DR001_AnimatedRubble` | elite add | START | 14 | 180 | 12 | armor, slam, Varos shield add | `DR001_EliteMobDrops` |
| `DR001_GatewardenVaros` | START mini-boss | START | 15 | 1850 | 18 | slam, shield at 60%, falling rocks at 25% | `DR001_VarosDrops` |
| `DR001_DrownedTombguard` | tank | MID | 20 | 260 | 16 | shield, knockback | `DR001_MidMobDrops` |
| `DR001_PlagueAcolyte` | caster/support | MID | 20 | 145 | 12 | poison cloud, ally heal | `DR001_MidMobDrops` |
| `DR001_CisternLeech` | disruptor | MID | 19 | 90 | 10 | lifesteal, water speed | `DR001_MidMobDrops` |
| `DR001_BoneMason` | summoner | MID | 21 | 180 | 12 | bone walls, minions | `DR001_EliteMobDrops` |
| `DR001_CisternOracleNhalruun` | MID boss | MID | 24 | 3200 | 22 | geysers, flood, mirror runes, drain interrupt | `DR001_NhalruunDrops` |
| `DR001_VoidboundKnight` | elite melee | END | 30 | 420 | 24 | cleave, enrage | `DR001_EndMobDrops` |
| `DR001_RuneSentinel` | turret | END | 29 | 260 | 20 | beam, interruptible cast | `DR001_EndMobDrops` |
| `DR001_CrownShade` | assassin add | END | 30 | 160 | 18 | blink behind player | `DR001_EndMobDrops` |
| `DR001_AnchorWarden` | anchor guard | END | 31 | 520 | 22 | shields active anchor | `DR001_EliteMobDrops` |
| `DR001_RuinedKingAurelion` | final boss | END | 32 | 6200 | 30 | anchors, shockwave, shades, void zones, enrage | `DR001_AurelionDrops` |

## Reward Baseline

| Reward | Normal | Veteran | Mythic | Notes |
| --- | ---: | ---: | ---: | --- |
| Ancient Coin | 8-14 | 16-26 | 30-48 | Completion/vault reward. |
| Rune Dust | 6-10 | 10-18 | 20-35 | Crafting and upgrade sink. |
| Gear chance | 6% | 12% | 22% | Per eligible loot roll. |
| Cosmetic chance | 1% | 2.5% | 5% | Requires later cosmetic source. |
| Weekly Crown Core | 0 | 0 | 1 | First Mythic weekly only. |

## Performance Budget

- Max active mobs: 35 Normal, 45 Veteran, 55 Mythic.
- Boss telegraphs may use burst particles; no every-tick decorative particle spam.
- ModelEngine P0 budget is reserved for Varos, Nhal'Ruun, and Aurelion.
- Loot shrine and furniture interactions require cooldowns before live release.
