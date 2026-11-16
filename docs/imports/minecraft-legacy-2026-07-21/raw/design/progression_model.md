# Progression Model

Status: draft (R1). Owner: rpg_systems_designer.

## MVP scope

- 1–3 starter classes (vertical slice target)
- Level cap MVP: TO_DEFINE (suggest 20–30 for first test)
- XP sources: mob kills, dungeon completion (no unbounded AFK farm without caps)

## Systems

| System | Plugin | Notes |
| --- | --- | --- |
| Classes & skills | MMOCore | Verify skill config format against docs cache |
| Stats | MMOCore + MMOItems | Item stats stack rules — test on staging |
| Professions | MMOCore | Optional post-MVP |

## Balance checkpoints

- New player vs elite mob TTK
- Solo vs party dungeon (if applicable)
- XP/hour vs target session length

## Acceptance (vertical slice)

- Player can pick a class and gain levels on staging
- Skills unlock per spec without permission exploits
- Progress persists across rejoin (HuskSync — verify)
