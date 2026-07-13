# City Hub MVP Concept

Purpose: implementation-facing concept for `Stolica Wyspy` MVP. This converts
the hub foundation into a buildable city layout direction without final block
palettes, NPC IDs, prices, or plugin configs.

## Core Decision

The MVP city is one connected MMO hub with lightly separated districts. It is
not a flat plaza with every NPC in one cluster, and it is not a forced tutorial
corridor.

The player flow is:

```text
spawn in city -> explore districts -> get starter skills -> get basic gear for
money -> read quest/info board -> walk to Portal Nexus -> choose dungeon
```

The board points players to the Portal Nexus. It does not start dungeons
directly.

## Layout Concept

Use a combined version of the three city ideas:

- central landmark from the Plaza concept;
- readable main movement from the Market Spine concept;
- functional separation from the District Ring concept.

Recommended shape:

- Portal Nexus in or near the center as the strongest landmark.
- Main street loop or broad ring around the portal.
- Short streets or terraces leading into districts.
- Each major system gets a building, stall cluster, shrine, workshop, or hall.
- Sightlines should let a new player see at least two next-interest points from
  spawn.

## Districts

### Portal Nexus

Role: dungeon selector and dungeon island access.

Contains:

- main portal structure;
- dungeon selector interaction;
- difficulty/status preview;
- party and reward preview;
- locked-state explanation.

Rules:

- Portal Nexus chooses dungeons.
- Quest/info board only points here.
- `level_1` is the first dungeon option for MVP.

### Market And Gear District

Role: first equipment, repair, salvage, stash, and basic upgrade loop.

Contains:

- basic gear vendor;
- repair/salvage service;
- stash or bank access;
- basic upgrade/crafting station;
- visible shopfronts or stalls, not one NPC line.

Rules:

- Player can get basic equipment for money.
- No final economy tuning in this layer.
- Services should be close enough to the Portal Nexus for dungeon return flow.

### Skill And Class District

Role: starter skills now, class identity later.

Contains:

- starter skill NPCs;
- class mentor buildings or halls;
- locked class choice explanation before level 10;
- future class/subclass expansion space.

Rules:

- NPCs live in buildings or clear service spots.
- Level 1 focuses on starter skills.
- Level 10 class mentors remain visible but locked/explained.

### Quest And Information Board

Role: organize activity and explain where systems are, without replacing
exploration.

Contains:

- quest board;
- city information board;
- dungeon notice pointing to Portal Nexus;
- service pointers to skills, gear, professions, and temple district.

Rules:

- Board is not a teleport-to-dungeon button.
- Board should make the city legible, not tell every step.

### Temple District

Role: religion, shrines, blessing preview, and future deity systems.

Contains:

- temple buildings;
- shrine keepers or priests;
- lore/service preview;
- locked or low-power blessing preview for MVP.

Rules:

- Religion is visible from the start as part of the city.
- No final deity bonuses in MVP.
- Shrines should feel like civic landmarks, not a hidden menu.

### Profession And Crafting District

Role: crafting, professions, materials, and return-loop services.

Contains:

- forge;
- alchemy/herbal station;
- enchanting/rune area;
- jeweler or artisan stall;
- profession board or orders later.

Rules:

- Professions are discoverable through buildings and materials.
- MVP may keep most profession depth locked or preview-only.

## NPC Rules

- NPCs are implemented as MythicMobs.
- Do not use Citizens/Civilians as the NPC foundation.
- Do not bake persistent NPC entities into world templates as the primary source
  of truth.
- Spawn and manage NPCs from explicit MythicMobs/config integration so they can
  be audited and reset.
- Each important NPC needs a world function and a place: shop, workshop, temple,
  hall, portal desk, or board area.
- Avoid a single line or pile of NPCs.

## Visual Concepts To Generate

Generate three images and use them as rough direction, not final map plans:

1. Top-down district layout: Portal Nexus center, market/gear, skills/classes,
   quest board, temple district, profession district.
2. Isometric city mood: fantasy island capital with central portal landmark and
   separated service districts.
3. Street-level player view: market street with visible route to portal, separate
   skill hall and temple buildings.

## Acceptance Criteria

- New player starts in the city and can orient by landmarks.
- Portal Nexus is visible as the primary dungeon access landmark.
- Board directs to Portal Nexus, not directly to a dungeon.
- Basic skills and basic gear are reachable by walking through city spaces.
- Religion/temple district is visible in the MVP layout.
- NPCs are MythicMobs-based and distributed by function.
- `online-mode=false` is the staging baseline.

