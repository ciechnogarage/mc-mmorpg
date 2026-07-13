# City Hub Blockout v0.0.1

Purpose: concrete blockout blueprint for the `Stolica Wyspy` MVP city. This is
the handoff between visual concept and staging implementation. It defines
district placement, building roles, NPC ownership, and player flow. It does not
define final block palettes, exact coordinates, prices, NPC IDs, or production
plugin configs.

## Source Direction

Use these as source of truth:

- `docs/ai/city-hub-mvp-concept.md`
- `docs/stolica-wyspy-hub-foundation-v0.0.1.md`
- `docs/discovery-npc-board-loop-001-foundation-v0.0.1.md`
- `docs/world-content-loop-foundation-v0.0.1.md`
- generated visual concepts in
  `/home/przemek/.codex-alt/generated_images/019f0e34-3e9e-7c53-b32d-7c2c7ba76a4a/`

## Blockout Shape

Use a compact connected city with a central Portal Nexus and lightly separated
districts.

```text
                    [Temple District]
                           |
 [Skill/Class District] -- [Portal Nexus] -- [Quest/Info Board]
                           |
             [Market/Gear Spine + Profession/Crafting]
                           |
                       [Spawn Gate]
```

Rules:

- Spawn must face a readable city route, not a tutorial arena.
- Portal Nexus is the strongest central landmark.
- Districts are visible from each other through streets, arches, stairs, bridges,
  terraces, or plazas.
- NPCs are distributed by function and building, not grouped in one line.
- Player can walk the MVP loop without teleporting.

## Player Flow

MVP player flow:

```text
spawn -> city street -> gear/service area -> skill/class area -> quest/info
board -> Portal Nexus -> dungeon selector -> level_1
```

Optional player flow:

```text
spawn -> temple district -> profession/crafting preview -> market -> board ->
Portal Nexus
```

Board behavior:

- The board shows quests, city pointers, and dungeon notices.
- The board points to Portal Nexus.
- The board does not start a dungeon directly.

Portal Nexus behavior:

- Portal Nexus opens the dungeon selector.
- `level_1` is the first MVP dungeon option.
- Dungeon difficulty/status/reward preview belongs at Portal Nexus, not at the
  quest board.

## District Blueprint

### Spawn Gate

Role: first orientation point.

Build pieces:

- small arrival gate, dock, or plaza edge;
- visible road toward Portal Nexus;
- one non-intrusive welcome sign or hologram;
- no wall of text.

NPCs:

- optional `CityGuide` MythicMobs NPC for a short city pointer only.

Acceptance:

- Player can see Portal Nexus or the road leading to it immediately.
- Player can ignore guide and still find city services.

### Portal Nexus

Role: central dungeon selector and strongest landmark.

Build pieces:

- main portal structure;
- selector platform or altar;
- dungeon status boards;
- party/difficulty/reward preview area;
- space for future portals.

NPCs:

- `PortalKeeper` MythicMobs NPC.

Interactions:

- dungeon selector menu;
- `level_1` entry option;
- locked-state explanation for future dungeons.

Acceptance:

- Board points here.
- Portal Nexus does the dungeon selection.
- Portal Nexus remains readable from multiple city streets.

### Market And Gear Spine

Role: first equipment and return-loop services.

Build pieces:

- weapon/armor stall or shop;
- repair counter;
- salvage station;
- stash/bank service;
- basic upgrade station.

NPCs:

- `GearVendor` MythicMobs NPC;
- `RepairKeeper` MythicMobs NPC;
- `SalvageKeeper` MythicMobs NPC;
- `StashKeeper` MythicMobs NPC.

Interactions:

- buy or receive basic gear for money;
- preview repair/salvage/stash services;
- no final price tuning.

Acceptance:

- Player can identify how to get basic equipment before going to Portal Nexus.
- Services are not hidden behind one generic NPC.

### Skill And Class District

Role: starter skills now, class identity later.

Build pieces:

- starter skill hall;
- class mentor rooms or small buildings;
- locked class choice area for level 10;
- future subclass expansion markers.

NPCs:

- `StarterSkillTrainer` MythicMobs NPC;
- `WarriorMentor` MythicMobs NPC;
- `RogueMentor` MythicMobs NPC;
- `RangerMentor` MythicMobs NPC;
- `MageMentor` MythicMobs NPC;
- `AcolyteMentor` MythicMobs NPC.

Interactions:

- starter skill selection or claim;
- class mentors explain locked level 10 state;
- no final class balancing.

Acceptance:

- New player can find starter skills before the dungeon selector.
- Class mentors are visible as future goals, not all unlocked at level 1.

### Quest And Information Board

Role: organize activity and city orientation.

Build pieces:

- physical board or notice wall;
- nearby small clerk desk or city notice booth;
- signs pointing to Portal Nexus, skills, gear, temple, professions.

NPCs:

- optional `BoardClerk` MythicMobs NPC.

Interactions:

- board menu or hologram with city pointers;
- first dungeon notice pointing to Portal Nexus;
- quest/zlecenie preview.

Acceptance:

- Board points to Portal Nexus.
- Board does not start `level_1` directly.
- Board does not replace city exploration.

### Temple District

Role: religions, shrines, blessing preview, future deity system.

Build pieces:

- temple hall or shrine court;
- multiple small shrines for future deity identities;
- prayer/offering space;
- visible civic landmark.

NPCs:

- `ShrineKeeper` MythicMobs NPC;
- optional future deity-specific priests as separate MythicMobs NPCs.

Interactions:

- lore/service preview;
- locked or low-power blessing preview;
- no final deity bonuses.

Acceptance:

- Religion is visible in MVP city.
- Shrine NPCs are not hidden in a generic services pile.

### Profession And Crafting District

Role: crafting, professions, materials, and later profession orders.

Build pieces:

- forge;
- alchemy/herbal station;
- enchanting/rune table area;
- jeweler/artisan stall;
- material storage or work order board.

NPCs:

- `ForgeKeeper` MythicMobs NPC;
- `Alchemist` MythicMobs NPC;
- `Runesmith` MythicMobs NPC;
- `Artisan` MythicMobs NPC.

Interactions:

- profession/crafting preview;
- optional locked services;
- no full profession progression in MVP.

Acceptance:

- Player understands professions exist from buildings and stations.
- Crafting is a city district, not just a menu.

## MythicMobs NPC Implementation Rules

- All city NPCs use MythicMobs definitions.
- Do not use Citizens/Civilians as the baseline NPC system.
- Do not rely on persistent entities baked into map templates.
- Use explicit spawn/reset ownership so NPCs can be audited and restored.
- Keep NPC definitions separate by district where practical.
- NPC display names should be readable, but exact names can be finalized during
  implementation.

Initial NPC roster:

| NPC | District | Purpose |
| --- | --- | --- |
| `PortalKeeper` | Portal Nexus | Dungeon selector / lock explanation |
| `GearVendor` | Market/Gear | Basic equipment |
| `RepairKeeper` | Market/Gear | Repair service |
| `SalvageKeeper` | Market/Gear | Salvage service |
| `StashKeeper` | Market/Gear | Bank/stash |
| `StarterSkillTrainer` | Skill/Class | Starter skills |
| `WarriorMentor` | Skill/Class | Level 10 class preview |
| `RogueMentor` | Skill/Class | Level 10 class preview |
| `RangerMentor` | Skill/Class | Level 10 class preview |
| `MageMentor` | Skill/Class | Level 10 class preview |
| `AcolyteMentor` | Skill/Class | Level 10 class preview |
| `BoardClerk` | Quest/Info | Board interaction / pointers |
| `ShrineKeeper` | Temple | Religion preview |
| `ForgeKeeper` | Profession/Crafting | Forge/crafting preview |
| `Alchemist` | Profession/Crafting | Alchemy preview |
| `Runesmith` | Profession/Crafting | Enchant/rune preview |
| `Artisan` | Profession/Crafting | Artisan/jewel preview |

## Agent Handoff

`minecraft-dungeon-world`:

- propose Portal Nexus world placement and dungeon selector access;
- map `level_1` entry requirements;
- avoid direct board-to-dungeon start.

`minecraft-mobs-models`:

- define MythicMobs city NPC ownership model;
- keep NPCs resettable and not baked into world templates;
- prepare NPC definitions by district.

`minecraft-ui-hud-menus`:

- board menu / city pointers;
- Portal Nexus selector UI handoff;
- holograms or short signs for districts.

`minecraft-rpg-systems`:

- starter skill claim/selection;
- basic equipment handoff;
- no final balance values.

`minecraft-permissions-economy`:

- money-based basic gear access;
- minimal permissions for city services;
- CMI/Vault integration sanity.

`minecraft-release-qa`:

- validate new player path from spawn to Portal Nexus selector;
- confirm board does not directly start dungeon;
- confirm no new startup blockers.

## Validation Checklist

- `online-mode=false` remains staging baseline.
- New player can identify Portal Nexus from spawn route.
- Districts are lightly separated and readable.
- NPCs are not clustered in one place.
- Temple district is visible.
- Board points to Portal Nexus.
- Portal Nexus opens dungeon selection.
- Basic gear and starter skill services are discoverable before dungeon entry.
- MythicMobs is the NPC implementation path.

