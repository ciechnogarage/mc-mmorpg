#!/usr/bin/env bash
# Prowizoryczna sceneria kreatora w scenie (etap 1): trawa + szablony wioski z vanilli
# w creator_limbo, x 208..399 / z 208..399. Docelowe scenerie ras to etap 4.
#
# Uzycie: ./scena-prowizoryczna.sh <kontener-z-rcon-cli>   (np. cc-scena, mc-world1-1)
# Kadr do config.yml (scene.points.default):
#   camera  { x: 301.5, y: 101.62, z: 285.5 }
#   subject { x: 301.5, y: 100.0,  z: 282.5 }
#   card    { x: 303.6, y: 100.45, z: 282.4 }
#   list    { x: 299.4, y: 100.95, z: 282.4 }
#   look    { x: 301.5, y: 101.0,  z: 282.5 }
#
# Pulapki (zmierzone): 'tp' i 'time' przechwytuje CMI — tylko minecraft:tp / minecraft:time;
# '/place structure village' w pustym swiecie stawia same sciezki, bez domow — dlatego
# pojedyncze szablony; szablony zostawiaja bloki jigsaw — usuwane nizej.
set -euo pipefail
c="${1:?podaj kontener}"
r() { docker exec "$c" rcon-cli "execute in minecraft:creator_limbo run $1"; }
r "forceload add 208 208 399 399"
for x in 208 272 336; do for z in 208 272 336; do
  r "fill $x 97 $z $((x+63)) 98 $((z+63)) minecraft:dirt"
  r "fill $x 99 $z $((x+63)) 99 $((z+63)) minecraft:grass_block"
done; done
r "place template minecraft:village/plains/town_centers/plains_fountain_01 295 100 262"
r "place template minecraft:village/plains/houses/plains_medium_house_1 282 100 250"
r "place template minecraft:village/plains/houses/plains_small_house_1 308 100 252"
r "fill 278 99 245 330 115 268 air replace minecraft:jigsaw"
r "fill 290 99 260 312 110 280 air replace minecraft:jigsaw"
r "fill 300 99 268 302 99 300 minecraft:dirt_path"
r "fill 298 99 273 304 99 279 minecraft:dirt_path"
r "place feature minecraft:fancy_oak 286 100 272"
r "place feature minecraft:oak 316 100 270"
r "place feature minecraft:birch 290 100 283"
r "place feature minecraft:fancy_oak 320 100 262"
r "setblock 304 100 279 minecraft:lantern"
r "minecraft:time set 6000"
r "minecraft:gamerule advance_time false"
