#!/usr/bin/env bash
# Scenerie ras w creator_limbo (2026-09-28). Najpierw tools/scena-prowizoryczna.sh (Czlowiek:
# wioska x~300). Tu: Elf x~600 (gaj wisniowy), Ork x~900 (obozowisko na sawannie),
# Nieumarly x~1200 (blada wioska-cmentarz). Kadry i klimat (pora dnia, czastki, ruch
# postaci) — sekcja scene.points w config.yml, przesuniecie x o 300/600/900 wzgledem wioski.
# Uzycie: ./scenerie-ras.sh <kontener-z-rcon-cli>
# Pulapki: fill w jednej komendzie max 32768 blokow; fillbiome wysokosc 96..126;
# 'tp'/'time' przechwytuje CMI -> minecraft:tp / minecraft:time.
set -euo pipefail
c="${1:?podaj kontener}"
r() { docker exec "$c" rcon-cli "execute in minecraft:creator_limbo run $1" >/dev/null; }
teren() { # x0 z0 biome
  for x in $1 $(( $1+32 )); do for z in $2 $(( $2+32 )); do
    r "fill $x 96 $z $((x+31)) 98 $((z+31)) minecraft:dirt"
    r "fill $x 99 $z $((x+31)) 99 $((z+31)) minecraft:grass_block"
    if [ -n "${3:-}" ]; then r "fillbiome $x 96 $z $((x+31)) 126 $((z+31)) minecraft:$3"; fi
  done; done; }
plama() { # x z blok
  r "fill $(( $1-1 )) 99 $(( $2-2 )) $(( $1+1 )) 99 $(( $2+2 )) minecraft:$3"
  r "fill $(( $1-2 )) 99 $(( $2-1 )) $(( $1+2 )) 99 $(( $2+1 )) minecraft:$3"; }

# --- ELF: gaj wisniowy ---------------------------------------------------------------
r "forceload add 576 232 639 303"; teren 576 240
for f in "cherry 596 100 266" "cherry 608 100 262" "cherry 588 100 276" "cherry 616 100 274" "birch 600 100 256" \
         "cherry 584 100 258" "cherry 620 100 252" "birch 612 100 280" "cherry 592 100 250" "cherry 598 100 244" \
         "cherry 606 100 246" "birch 603 100 240" "cherry 612 100 242" "cherry 590 100 242" "birch 596 100 237" \
         "cherry 626 100 262" "cherry 578 100 268"; do set -- $f; r "place feature minecraft:$1 $2 $3 $4"; done
r "fill 600 99 268 602 99 290 minecraft:moss_block"; r "fill 601 99 268 601 99 290 minecraft:mossy_cobblestone"
for p in "590 270" "612 268" "596 283" "607 286" "585 280" "618 283" "594 262" "609 258" "602 252"; do set -- $p
  r "fill $(( $1-2 )) 100 $(( $2-2 )) $(( $1+2 )) 100 $(( $2+2 )) minecraft:pink_petals[flower_amount=4] replace air"; done
for x in 599 604; do r "setblock $x 100 280 minecraft:mossy_cobblestone_wall"; r "setblock $x 101 280 minecraft:lantern"; done
r "fill 598 100 266 598 104 266 minecraft:mossy_stone_bricks"; r "fill 604 100 266 604 104 266 minecraft:mossy_stone_bricks"
r "fill 598 105 266 604 105 266 minecraft:mossy_stone_brick_slab"; r "fill 599 104 266 603 104 266 minecraft:cherry_leaves[persistent=true]"
r "setblock 601 103 266 minecraft:lantern[hanging=true]"

# --- ORK: obozowisko na sawannie -----------------------------------------------------
r "forceload add 872 232 935 303"; teren 872 232 savanna; teren 872 264 savanna
for p in "893 277" "897 280" "906 272" "909 276" "889 266" "913 266" "899 262" "903 260" "887 283" "915 285"; do set -- $p; plama $1 $2 coarse_dirt; done
r "fill 900 99 254 902 99 292 minecraft:coarse_dirt"; r "fill 901 99 254 901 99 292 minecraft:dirt_path"
for x in $(seq 878 924); do [ $x -ge 899 ] && [ $x -le 903 ] && continue; h=$(( 3 + (x*7 % 3) ))
  r "fill $x 100 252 $x $((99+h)) 252 minecraft:stripped_spruce_log"; r "setblock $x $((100+h)) 252 minecraft:spruce_fence"; done
for x in 898 904; do r "fill $x 100 252 $x 106 252 minecraft:dark_oak_log"; r "setblock $x 107 252 minecraft:skeleton_skull"; done
r "fill 898 106 252 904 106 252 minecraft:dark_oak_log[axis=x]"
for t in "886 262" "914 262"; do set -- $t; x=$1; z=$2
  r "fill $((x-2)) 100 $((z-1)) $((x-2)) 100 $((z+2)) minecraft:brown_wool"; r "fill $((x+2)) 100 $((z-1)) $((x+2)) 100 $((z+2)) minecraft:brown_wool"
  r "fill $((x-1)) 101 $((z-1)) $((x-1)) 101 $((z+2)) minecraft:brown_wool"; r "fill $((x+1)) 101 $((z-1)) $((x+1)) 101 $((z+2)) minecraft:brown_wool"
  r "fill $x 102 $((z-1)) $x 102 $((z+2)) minecraft:dark_oak_log[axis=z]"; done
for f in "894 274" "908 272"; do set -- $f; r "setblock $1 100 $2 minecraft:campfire[lit=true]"
  r "setblock $(( $1+2 )) 100 $2 minecraft:stripped_acacia_log[axis=z]"; r "setblock $(( $1-2 )) 100 $(( $2+1 )) minecraft:stripped_acacia_log[axis=x]"; done
for s in "896 282 skeleton_skull" "906 283 wither_skeleton_skull" "890 272 skeleton_skull"; do set -- $s
  r "fill $1 100 $2 $1 101 $2 minecraft:spruce_fence"; r "setblock $1 102 $2 minecraft:$3"; done
r "setblock 911 100 279 minecraft:bone_block"; r "setblock 887 100 276 minecraft:hay_block"
for f in "acacia 882 100 280" "acacia 921 100 278" "acacia 896 100 238" "acacia 912 100 240" "acacia 876 100 258" "acacia 928 100 262"; do
  set -- $f; r "place feature minecraft:$1 $2 $3 $4"; done

# --- NIEUMARLY: blada wioska-cmentarz ------------------------------------------------
r "forceload add 1168 224 1231 303"; teren 1168 232 pale_garden; teren 1168 264 pale_garden
for p in "1193 276" "1208 270" "1188 262" "1213 280" "1199 258" "1186 284"; do set -- $p; plama $1 $2 pale_moss_block; done
r "fill 1200 99 256 1202 99 292 minecraft:gravel"; r "fill 1201 99 256 1201 99 292 minecraft:tuff"
for t in "plains/zombie/houses/plains_small_house_1 1184 100 246" "plains/zombie/houses/plains_small_house_2 1210 100 244" \
         "plains/zombie/houses/plains_medium_house_1 1193 100 234"; do set -- $t; r "place template minecraft:village/$1 $2 $3 $4"; done
for z in 232 248; do r "fill 1168 99 $z 1231 115 $((z+15)) air replace minecraft:jigsaw"; done
A="1176 100 228 1226 114 260"
for rep in "pale_oak_planks oak_planks" "dark_oak_log oak_log" "stripped_dark_oak_log stripped_oak_log" "mossy_cobblestone cobblestone" \
           "air glass_pane" "air white_bed" "dark_oak_stairs oak_stairs" "dark_oak_fence oak_fence"; do set -- $rep; r "fill $A minecraft:$1 replace minecraft:$2"; done
for f in "pale_oak 1182 100 272" "pale_oak 1218 100 270" "pale_oak 1178 100 256" "pale_oak 1224 100 256" "pale_oak 1206 100 226"; do
  set -- $f; r "place feature minecraft:$1 $2 $3 $4"; done
for g in "1193 270" "1196 272" "1206 270" "1209 272" "1194 262" "1208 262"; do set -- $g
  r "setblock $1 100 $2 minecraft:cobbled_deepslate_wall"; r "setblock $1 101 $2 minecraft:chiseled_stone_bricks"
  r "setblock $1 99 $(( $2+1 )) minecraft:coarse_dirt"; r "setblock $1 99 $(( $2+2 )) minecraft:coarse_dirt"; done
for l in "1198 280" "1204 280"; do set -- $l; r "setblock $1 100 $2 minecraft:cobbled_deepslate_wall"; r "setblock $1 101 $2 minecraft:soul_lantern"; done
r "setblock 1189 100 280 minecraft:cobweb"; r "setblock 1213 100 266 minecraft:soul_campfire"
# noc: niewidzialne swiatlo przy postaci, inaczej w nocnym kadrze jest czarna sylwetka
for p in "1200 101 283" "1203 101 283" "1201 103 281" "1201 101 280"; do r "setblock $p minecraft:light[level=13]"; done
echo "scenerie ras gotowe"
