| typ | parent | material | 2H flaga/itemy | AnimacjeBroni rodzaj | Trafienia rodzaj | DwieBronie lewa | Ksiega | grupa bieglosci | Kostur(Zaklecia) | MMOI disable-melee |
|---|---|---|---|---|---|---|---|---|---|---|
| SWORD | - | IRON_SWORD | 1/29 | ciete | ciecie | tak | B | miecz | - | - |
| DAGGER | - | STONE_SWORD | 0/15 | klute | pchniecie | tak | B | sztylet | - | - |
| SPEAR | - | TRIDENT | 0/10 | klute | pchniecie | - | B2H | wlocznia | - | - |
| HAMMER | - | IRON_AXE | 0/13 | obuchowe | obuch | tak | B | bulawa | - | - |
| GAUNTLET | - | IRON_SHOVEL | 0/1 | obuchowe | piesc | tak | B | sztylet | - | - |
| WHIP | - | LEAD | 0/9 | ciete | piesc(dom) | tak | B | lancuch | - | tak |
| STAFF | - | DIAMOND_HOE | 1/16 | bez rodzaju | obuch | - | B2H | kostur | K | tak |
| BOW | - | BOW | 0/13 | brak | piesc(dom) | - | B2H | luk | - | tak |
| CROSSBOW | - | WOODEN_PICKAXE | 0/10 | brak | piesc(dom) | - | B2H | kusza | - | tak |
| MUSKET | - | IRON_HORSE_ARMOR | 1/2 | brak | piesc(dom) | - | B2H | kusza | - | tak |
| LUTE | - | NAME_TAG | - | brak | piesc(dom) | - | B | - | - | tak |
| CATALYST | - | PRISMARINE_SHARD | 0/12 | brak | piesc(dom) | - | B | - | - | - |
| OFF_CATALYST | - | PRISMARINE_CRYSTALS | 0/3 | brak | piesc(dom) | - | BL | - | - | - |
| MAIN_CATALYST | - | PRISMARINE_CRYSTALS | - | brak | piesc(dom) | - | B | - | - | - |
| GREATSWORD | SWORD | DIAMOND_SWORD | 12/13 | ciete/ciezka | ciecie | - | B2H | miecz_dwureczny | - | - |
| LONG_SWORD | SWORD | STONE_SWORD | 0/1 | ciete | ciecie | tak | B | miecz | - | - |
| KATANA | SWORD | IRON_SWORD | 0/10 | ciete | ciecie | tak | B | klinga | - | - |
| THRUSTING_SWORD | DAGGER | GOLDEN_SWORD | 0/2 | klute | pchniecie | tak | B | miecz | - | - |
| AXE | SWORD | STONE_AXE | 3/15 | ciete | topor | tak | B | topor | - | - |
| GREATAXE | SWORD | DIAMOND_AXE | 0/1 | ciete/ciezka | topor | - | B2H | topor | - | - |
| HALBERD | SWORD | IRON_AXE | 8/10 | klute/ciezka | topor | - | B2H | drzewcowa | - | - |
| LANCE | SPEAR | STICK | 1/1 | klute/ciezka | pchniecie | - | B2H | drzewcowa | - | - |
| GREATHAMMER | HAMMER | DIAMOND_AXE | 8/9 | obuchowe/ciezka | obuch | - | B2H | mlot_dwureczny | - | - |
| GREATSTAFF | HAMMER | DIAMOND_HOE | 8/9 | obuchowe/ciezka | obuch | - | B2H | kostur_dwureczny | K | - |
| STAVE | HAMMER | IRON_HOE | - | brak | ciecie(mat) | - | B | kostur | - | - |
| TOME | CATALYST | BOOK | 0/15 | bez rodzaju | piesc(dom) | - | B | grimuar | - | - |
| TALISMAN | CATALYST | TOTEM_OF_UNDYING | 0/9 | bez rodzaju | piesc(dom) | - | B | totem | - | - |
| WAND | STAFF | STICK | 0/12 | bez rodzaju | piesc(dom) | - | B | kostur | - | tak |
| GREATBOW | BOW | BOW | 8/8 | brak | piesc(dom) | - | B2H | luk_dlugi | - | tak |
| SHIELD | CATALYST | SHIELD | 1/13 | obuchowe | piesc(dom) | - | B | tarcza | - | - |
| SIERP | SWORD | IRON_HOE | 0/8 | ciete | ciecie | tak | B | sierp | - | - |
| KOSA | SWORD | NETHERITE_HOE | 8/8 | ciete/ciezka | ciecie | - | B2H | kosa | - | - |
| FILAKTERIUM | CATALYST | ECHO_SHARD | 0/8 | bez rodzaju | piesc(dom) | - | B | filakterium | - | - |

Ksiega: B=typy-broni, 2H=typy-dwureczne, L=tylko-lewa. Trafienia (mat)=zgadywane z materialu, (dom)=bron-domyslna.

## Rozjazdy

### dwurecznosc: Anim waga>=1.4 vs Ksiega typy-dwureczne (2)
- SPEAR: Anim ciezka=False Ksiega 2H=True
- STAFF: Anim ciezka=False Ksiega 2H=True

### dwurecznosc: flaga MMOItems vs Ksiega (10)
- SPEAR: 0/10 z two-handed, Ksiega 2H=True
- STAFF: 1/16 z two-handed, Ksiega 2H=True
- BOW: 0/13 z two-handed, Ksiega 2H=True
- CROSSBOW: 0/10 z two-handed, Ksiega 2H=True
- MUSKET: 1/2 z two-handed, Ksiega 2H=True
- GREATSWORD: 12/13 z two-handed, Ksiega 2H=True
- GREATAXE: 0/1 z two-handed, Ksiega 2H=True
- HALBERD: 8/10 z two-handed, Ksiega 2H=True
- GREATHAMMER: 8/9 z two-handed, Ksiega 2H=True
- GREATSTAFF: 8/9 z two-handed, Ksiega 2H=True

### rodzaj Anim vs Trafienia (3)
- WHIP: ciete vs piesc(dom)
- STAFF: Anim bez rodzaju vs obuch
- HALBERD: klute vs topor

### brak w Trafienia bronie-mmoitems (zgaduje z materialu) (6)
- WHIP: piesc(dom)
- TOME: piesc(dom)
- TALISMAN: piesc(dom)
- WAND: piesc(dom)
- SHIELD: piesc(dom)
- FILAKTERIUM: piesc(dom)

### MMOItems disable-melee-attacks, a Anim ma rodzaj ciosu wrecz (1)
- WHIP: ciete

### typ broni wrecz bez AnimacjeBroni (1)
- STAVE: IRON_HOE
