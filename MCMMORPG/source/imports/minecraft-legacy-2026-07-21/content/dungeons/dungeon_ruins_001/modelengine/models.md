# DR001 ModelEngine Contract

All active ModelEngine IDs use the `dr001_` namespace. Existing
`dungeon_ruins_001_*.bbmodel` files are legacy scaffold references only and do
not satisfy final release requirements.

| Model ID | Entity base | Required animations | Priority |
| --- | --- | --- | --- |
| `dr001_ruin_thrall` | zombie/husk | idle, walk, attack, hit, death | P1 |
| `dr001_mossbound_archer` | skeleton | idle, walk, shoot, root_cast, death | P2 |
| `dr001_rootling` | silverfish | idle, leap, attack, death | P2 |
| `dr001_animated_rubble` | iron golem | idle, walk, slam, crumble, death | P1 |
| `dr001_gatewarden_varos` | iron golem | idle, walk, attack_slam, shield, roar, death | P0 |
| `dr001_drowned_tombguard` | drowned | idle, walk, shield_bash, death | P2 |
| `dr001_plague_acolyte` | witch | idle, walk, cast, heal, death | P2 |
| `dr001_cistern_leech` | silverfish | idle, swim, bite, death | P2 |
| `dr001_bone_mason` | stray | idle, walk, summon, cast, death | P2 |
| `dr001_nhalruun` | drowned/custom | idle, swim_idle, cast, flood, mirror, death | P0 |
| `dr001_voidbound_knight` | wither skeleton | idle, walk, cleave, enrage, death | P1 |
| `dr001_rune_sentinel` | shulker/custom | idle, beam_cast, overload, death | P1 |
| `dr001_crown_shade` | enderman | idle, blink, stab, death | P2 |
| `dr001_anchor_warden` | piglin brute | idle, walk, shield, slam, death | P1 |
| `dr001_ruined_king` | wither skeleton/custom | idle, walk, cleave, shockwave, summon, phase, death | P0 |

## Boss Prompts

Varos: ancient stone gatewarden golem, broken chains, moss and amber runes,
bulky silhouette, 3.2 blocks tall, heavy slam and shield phase animations.

Nhal'Ruun: drowned oracle, cracked bone mask, turquoise cursed water aura, four
floating rune tablets, flood ritual and mirror phase animations.

Aurelion: ruined king in blackstone armor, broken golden crown, violet void core,
long cracked sword, regal undead silhouette, throne collapse death animation.
