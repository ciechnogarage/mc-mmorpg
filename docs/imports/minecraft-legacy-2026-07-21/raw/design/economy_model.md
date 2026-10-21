# Economy Model

Status: draft (R1). Owner: economy_balance_designer.

## MVP (simple)

- One primary currency — confirm implementation (CMI / Vault bridge / other): **TO_CONFIRM**
- Sources: mob drops (sell value), dungeon rewards, starter quests via NPC/menu
- Sinks: repairs, upgrades, teleport fees, cosmetic placeholders

## Plugins

| Role | Plugin |
| --- | --- |
| Bridge | Vault |
| Possible implementation | CMI |
| Item values | MMOItems sell/trade flags |
| Dungeon rewards | MythicDungeons + MMOItems |

## Live change policy

- Economy changes on production: **R4** — backup, approval, rollback, changelog

## Exploit review (required before release)

- Dupe paths, alt farming, dungeon reset abuse, sell-all loops
