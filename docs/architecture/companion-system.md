# Companion system architecture

## Purpose

CompanionCore adds one low-cost combat companion per active MMORPG character. It owns companion state and lifecycle; MythicMobs owns physical entities and combat execution.

## Runtime boundary

```text
Nexus (DeluxeMenus + Nexo)
  -> CompanionCore (character profile, lifecycle, commands, limits)
      -> MythicMobs adapter (PDC/metadata, entity commands)
          -> MythicMobs (entity, vanilla pathfinding, goals, target selectors, skills)
```

`profileId` from the currently selected MMOProfiles character scopes persistent companion data. Player UUID scopes only the online owner and the active entity mapping.

## Non-negotiable constraints

- At most one active companion per player UUID.
- Runtime state never survives logout or server restart.
- No per-companion scheduler, global per-tick entity scan, custom pathfinder, or `~onTimer:1` skill.
- Only immutable DTOs cross an asynchronous persistence boundary.
- UI, display names, lore, and MythicMob IDs are not sources of truth.

## Delivery sequence

The initial release gate is Tank T1 summon → FOLLOW → dismiss → quit cleanup, verified for orphan entities. Roles, modes, AI pulses, and full performance profiles follow this gate.
