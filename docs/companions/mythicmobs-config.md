# MythicMobs companion configuration

`companion.tank.tier1` maps only to `Companion_Tank_T1` in the vertical slice.
It uses a tamed `WOLF`: CompanionCore assigns the live owner, allowing vanilla
Paper follow pathfinding rather than a custom navigator. Mythic configuration
keeps combat distance at 18 blocks and has only `~onSpawn`, `~onDamaged`, and
`~onDeath` skill triggers. No `~onTimer` trigger is present.

Do not use a mob display name or Mythic ID as durable ownership state. The
adapter writes owner/type metadata to the spawned entity; the Core profile is
canonical.
