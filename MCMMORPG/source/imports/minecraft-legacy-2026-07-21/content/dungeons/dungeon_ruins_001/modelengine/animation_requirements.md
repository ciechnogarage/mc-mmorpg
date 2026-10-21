# DR001 Animation Requirements

Final `.bbmodel` files must contain non-empty hitbox data and the animations
listed in `modelengine/models.md`.

Event mapping:

| Encounter event | ModelEngine state | MythicMobs trigger |
| --- | --- | --- |
| Boss spawn | `roar` / `rise` | `~onSpawn` |
| Varos slam | `attack_slam` | `DR001_GateSlam` |
| Varos shield | `shield` | `DR001_SummonRubbleAdds` |
| Nhal'Ruun flood | `flood` | `DR001_FloodPhase` |
| Nhal'Ruun mirror | `mirror` | `DR001_MirrorRunes` |
| Aurelion shockwave | `shockwave` | `DR001_CrownShockwave` |
| Aurelion summon | `summon` | `DR001_SummonCrownShades` |
| Aurelion phase | `phase` | `DR001_ThroneCollapse` |
| Death | `death` | `~onDeath` |

The final asset validator treats legacy `dungeon_ruins_001_*` model files as
non-final for this DR001 release-prep pass.
