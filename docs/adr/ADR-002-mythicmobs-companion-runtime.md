# ADR-002: MythicMobs as companion entity runtime

- Status: Superseded by `ADR-003-petcore-companion-authority.md` (2026-07-20)
- Date: 2026-07-19
- Scope: CompanionCore MVP and its first extensions

> Superseded: PetCore, not CompanionCore, is now the durable companion
> authority for all four companion policies. MythicMobs' role as physical
> entity runtime (this ADR's core decision) is preserved and restated in
> ADR-003. Kept for historical record; do not extend `CompanionCore` as an
> authority.

## Context

Companions require bounded combat behaviour while preserving CPU headroom on Paper 1.21.11. The server already runs MythicMobs 5.11.2, MMOProfiles, DeluxeMenus, and Nexo. The system must not create a second combat engine or make an entity durable without its owner.

## Decision

Use MythicMobs for each companion's physical entity, vanilla pathfinding, goals, selectors, attacks, skills, and animations. Introduce a small CompanionCore plugin as the authority for character-scoped companion profiles, activation lifecycle, limits, mode, canonical IDs, and runtime ownership mapping.

CompanionCore calls MythicMobs only through `MythicCompanionAdapter`. The adapter can be replaced by a fake in tests. A shared, bounded coordinator handles Core-side periodic decisions; Mythic event triggers handle combat reactions.

Persistent companion state is owned by CompanionCore and keyed by MMOProfiles `profileId`. The active entity is keyed by the online player UUID and is always removed on quit, death handling, disable, or invalid ownership.

## Alternatives considered

### Citizens + Sentinel

Rejected. It adds another NPC and combat stack for behaviour already available in MythicMobs, increasing entity, plugin, and operational surface area.

### Custom Bukkit combat/pathfinding implementation

Rejected. It would duplicate vanilla/Mythic movement and targeting, violate the no-custom-pathfinder constraint, and create a high-risk hot path.

### MythicMobs-only state in mob configuration/name/lore

Rejected. Those are presentation/configuration surfaces, not canonical durable authority; they cannot reliably enforce lifecycle or character-scoped limits.

## Consequences

- MythicMobs availability is a controlled, explicit summon failure.
- The adapter must persist enough PDC/metadata to recognise managed entities, but Core data remains canonical.
- GUI uses DeluxeMenus/Nexo as presentation only.
- Every new role must pass the same lifecycle and low-frequency scheduling constraints before it is enabled.
