# MCMMORPG Development Plan

## Overview

Bring the full Paper MMORPG workspace to a coherent, parallel-development state
for persistent PvE/PvP alpha work. Player characters and cosmetics persist;
seasonal competitive state resets. Plugin configuration remains the adapter
layer, while shared contracts define ownership, persistence, IDs, and runtime
verification.

## Architecture Decisions

- Keep the existing Paper/plugin stack and add contracts around it instead of
  replacing plugins prematurely.
- Define one authority for every shared concept: player profile, character,
  class progression, item, currency, pet, guild, dungeon, and PvP season.
- Use MariaDB/MySQL as the durable authority for cross-plugin MMO state; local
  plugin stores are adapters or caches unless explicitly approved otherwise.
- Develop all nine domains in parallel after the shared contract checkpoint;
  one integrator owns shared files and conflict resolution.
- Validate each affected backend independently, then run cross-area checks
  through Velocity.

## Task List

### Phase 1: Baseline and Contracts

- [ ] Inventory the 1095 remaining source candidates and assign each to a
  baseline group: runtime config, authored content, validation, documentation,
  agent tooling, or custom plugin source.
- [ ] Create the configuration authority registry: owner, consumers, source
  path, persistence mode, reload policy, and verification command.
- [ ] Define shared IDs and contracts for profiles, characters, classes,
  progression, items, currencies, pets, guilds, dungeons, and PvP seasons.
- [ ] Add coherence checks for plugin dependencies, missing IDs, duplicate
  authorities, disabled-runtime assumptions, and manifest drift.

### Checkpoint: Foundation

- [ ] Repository hygiene and plugin inventory checks pass.
- [ ] Every cross-domain concept has exactly one declared authority.
- [ ] No agent edits a shared contract or registry without integrator review.

### Phase 2: Parallel Domain Work

- [ ] Combat and RPG: classes, stats, skills, itemization, death and recovery.
- [ ] World and PvE: regions, exploration, mobs, bosses, dungeons, rewards.
- [ ] Competitive PvP: queues, arenas, matchmaking, MMR, seasons, anti-abuse.
- [ ] Pets: acquisition, training, daily/gathering/combat utility, limits.
- [ ] Professions and economy: gathering, crafting, vendors, sinks, inflation.
- [ ] Social MMO: parties, guilds, chat, friends, group discovery.
- [ ] Meta and retention: achievements, collections, cosmetics, milestones.
- [ ] UX and discovery: character stage, HUD, menus, onboarding, navigation.
- [ ] Operations: persistence, backups, permissions, observability, rollback.

Each domain must return an inventory, gap matrix, scoped implementation,
config references, and backend evidence. Domain agents may not modify another
domain's authority or shared contract directly.

### Checkpoint: Integration Trains

- [ ] Integrator merges one domain batch at a time after coherence checks.
- [ ] `world`, `items`, and `hub` backends boot cleanly for affected changes.
- [ ] Cross-area flows are verified through Velocity, not direct backend joins.
- [ ] Runtime warnings and persistence behavior are recorded as evidence.

### Phase 3: Alpha Hardening

- [ ] Closed persistent-alpha onboarding is reproducible.
- [ ] PvP season reset affects only seasonal state, never characters/cosmetics.
- [ ] PvE and PvP rewards do not create mandatory pet or class multipliers.
- [ ] Backup, restore, rollback, and data migration paths are tested.
- [ ] Release gate includes security, performance, economy, and player-flow QA.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Two plugins own the same player state | Data loss or divergence | Registry plus runtime coherence checks |
| Generated server data enters source control | Secrets and player-data leak | Ignore policy plus staged-file security scan |
| Parallel agents touch shared configs | Integration conflicts | Contract checkpoint and single integrator |
| Plugin snapshot incompatibility | Boot/runtime failure | Backend-specific staging validation |
| Pet progression becomes mandatory | PvE/PvP balance collapse | Explicit utility and multiplier caps in contracts |

## Immediate Next Task

Build the authority registry and coherence validator from the current manifest,
plugin configs, custom plugin source, and existing validation scripts. This is
the first implementation step after repository hygiene.
