#!/usr/bin/env bash
# Sceneria wyboru postaci w creator_limbo (2026-09-29): nocna polana w tajdze z ogniskiem.
# Postacie staja w luku wokol ogniska (1501, 100, 270), przodem na poludnie/boki; kamera
# od poludnia patrzy na polnoc — za postacia ogien, lawy i namiot, dalej swierki.
# Kadr i luk: scene.points.wybor + scene.select w config.yml. Uzycie: ./scena-wyboru.sh <kontener>
# Pulapki jak w scenerie-ras.sh: fill max 32768 blokow, tp/time przechwytuje CMI.
set -euo pipefail
c="${1:?podaj kontener}"
r() { docker exec "$c" rcon-cli "execute in minecraft:creator_limbo run $1" >/dev/null; }
X=1501; Z=270
r "forceload add 1464 232 1543 311"
for x in 1472 1504; do for z in 240 272; do
  r "fill $x 90 $z $((x+31)) 98 $((z+31)) minecraft:dirt"
  r "fill $x 99 $z $((x+31)) 99 $((z+31)) minecraft:grass_block"
  r "fill $x 100 $z $((x+31)) 125 $((z+31)) minecraft:air"
  r "fillbiome $x 96 $z $((x+31)) 126 $((z+31)) minecraft:old_growth_spruce_taiga"
done; done
# plamy podsciolki i wydeptana ziemia przy ognisku
for p in "1490 258" "1512 256" "1486 282" "1516 284" "1497 292" "1508 246" "1480 268" "1522 270"; do set -- $p
  r "fill $(( $1-2 )) 99 $(( $2-1 )) $(( $1+2 )) 99 $(( $2+1 )) minecraft:podzol"
  r "fill $(( $1-1 )) 99 $(( $2-2 )) $(( $1+1 )) 99 $(( $2+2 )) minecraft:podzol"; done
r "fill $((X-4)) 99 $((Z-3)) $((X+4)) 99 $((Z+3)) minecraft:coarse_dirt"
r "fill $((X-3)) 99 $((Z-4)) $((X+3)) 99 $((Z+4)) minecraft:coarse_dirt"
r "fill $((X-1)) 99 $((Z-1)) $((X+1)) 99 $((Z+1)) minecraft:gravel"
# ognisko w kregu kamieni
r "setblock $X 100 $Z minecraft:campfire[lit=true]"
# krag kamieni rowno z ziemia (polplyty wygladaly z kamery jak kamienny blok)
for o in "-1 -1 cobblestone" "0 -1 mossy_cobblestone" "1 -1 andesite" "-1 0 mossy_cobblestone" "1 0 cobblestone" \
         "-1 1 andesite" "0 1 cobblestone" "1 1 mossy_cobblestone"; do set -- $o
  r "setblock $((X+$1)) 100 $((Z+$2)) minecraft:air"; r "setblock $((X+$1)) 99 $((Z+$2)) minecraft:$3"; done
# lawy z pni za ogniskiem (polnoc) — tlo kadru, poza lukiem postaci
r "fill $((X-2)) 100 $((Z-4)) $((X+2)) 100 $((Z-4)) minecraft:stripped_spruce_log[axis=x]"
# (boczne pnie X+-4 usuniete — kamera przy skrajnych postaciach miala je w twarz)
r "setblock $((X-4)) 100 $((Z-3)) minecraft:air"; r "setblock $((X+4)) 100 $((Z-3)) minecraft:air"
# namiot (polnocny zachod) i skrzynia, beczka
t=$((X-6)); u=$((Z-8))
r "fill $((t-2)) 100 $((u-1)) $((t-2)) 100 $((u+2)) minecraft:green_wool"
r "fill $((t+2)) 100 $((u-1)) $((t+2)) 100 $((u+2)) minecraft:green_wool"
r "fill $((t-1)) 101 $((u-1)) $((t-1)) 101 $((u+2)) minecraft:green_wool"
r "fill $((t+1)) 101 $((u-1)) $((t+1)) 101 $((u+2)) minecraft:green_wool"
r "fill $t 102 $((u-1)) $t 102 $((u+2)) minecraft:spruce_log[axis=z]"
r "setblock $((X+5)) 100 $((Z-7)) minecraft:barrel[facing=up]"
r "setblock $((X+6)) 100 $((Z-7)) minecraft:chest[facing=south]"
r "setblock $((X+6)) 101 $((Z-7)) minecraft:lantern"
# latarnie na palach po bokach
for l in "$((X-7)) $((Z-3))" "$((X+7)) $((Z-3))"; do set -- $l
  r "fill $1 100 $2 $1 101 $2 minecraft:spruce_fence"; r "setblock $1 102 $2 minecraft:lantern"; done
# swierki w pierscieniu, gesciej z tylu kadru
for f in "1489 258" "1496 252" "1504 250" "1512 254" "1519 260" "1485 266" "1480 256" "1522 250" \
         "1500 243" "1491 246" "1511 244" "1478 276" "1524 278" "1484 286" "1518 290" "1527 264" "1475 262" \
         "1494 298" "1508 299"; do set -- $f; r "place feature minecraft:spruce $1 100 $2"; done
for f in "1482 246" "1520 244" "1473 284" "1529 286"; do set -- $f; r "place feature minecraft:mega_spruce $1 100 $2"; done
# krzewy, paprocie, swietliki (wlasne czastki w nocy)
for b in "1493 262 firefly_bush" "1510 263 firefly_bush" "1488 272 firefly_bush" "1515 274 firefly_bush" \
         "1497 257 fern" "1506 258 fern" "1491 268 large_fern" "1513 267 sweet_berry_bush[age=3]" \
         "1486 262 fern" "1517 262 fern" "1495 280 firefly_bush" "1509 281 firefly_bush"; do set -- $b
  if [ "$3" = large_fern ]; then r "setblock $1 100 $2 minecraft:large_fern[half=lower]"; r "setblock $1 101 $2 minecraft:large_fern[half=upper]"
  else r "setblock $1 100 $2 minecraft:$3"; fi; done
# niewidzialne swiatlo w luku postaci — w nocy bez niego czarne sylwetki
for a in -90 -60 -30 0 30 60 90; do
  read lx lz < <(python3 -c "import math;a=math.radians($a);print(round($X+0.5-math.sin(a)*4.6-0.5), round($Z+0.5+math.cos(a)*4.6-0.5))")
  r "setblock $lx 102 $lz minecraft:light[level=11]"; done
# forceload tylko na czas budowy — w pamieci trzyma kadr sam plugin (3x3 chunki wokol kamery i postaci)
r "forceload remove 1464 232 1543 311"
echo "sceneria wyboru gotowa"
