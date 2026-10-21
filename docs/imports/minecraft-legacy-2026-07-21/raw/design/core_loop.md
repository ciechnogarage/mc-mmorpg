# Core Loop — MCMMORPG

Status: draft (R1). Owner: product_owner.

## Player promise

```text
Wchodzisz na serwer, wybierasz styl gry / klasę, rozwijasz postać, zdobywasz lepszy ekwipunek,
walczysz z mobami i bossami, przechodzisz dungeony, rozwijasz gildię, handlujesz,
eksplorujesz regiony i wracasz po progres sezonowy albo długoterminowy.
```

## MVP loop (vertical slice)

1. Spawn/hub (WorldGuard safe zone)
2. Onboarding — menu + hologramy (DeluxeMenus, DecentHolograms, PlaceholderAPI)
3. Class selection (MMOCore + menus)
4. PvE region — mobs, XP, loot (MythicMobs, MMOItems, MMOCore)
5. Optional mini-dungeon or boss (MythicDungeons + MythicMobs)
6. Simple economy sink/source (Vault/CMI — TO_CONFIRM owner plugin)

## Primary plugin owners

| Loop step | Owner plugin |
| --- | --- |
| Class/progression | MMOCore |
| Items/gear | MMOItems |
| Combat entities | MythicMobs |
| Dungeons | MythicDungeons |
| Regions | WorldGuard |
| Menus/UI | DeluxeMenus |

## Out of scope for first slice

- Full guild wars, auction house, season pass — see `tasks/backlog.md`
