#!/usr/bin/env bash
# Gornik na KLONIE (klon.sh): jar Rzemiosla, przedmioty tylko-klon, restart, chodnik probny z dwoma przodkami.
# Uzycie: przygotuj_klon.sh [nazwa=gornik] [--bez-terenu]   (klon: klon.sh start <nazwa> --z-kreatorem)
# Chodnik: wzdluz X 200..230, Z 100..102 (podloga Y-60, sufit Y-56, 3 bloki wolne), sciana polnocna Z=99.
#  miekka skala X 198..213 (STONE), twarda X 214..232 (DEEPSLATE), sciany Z=99 i Z=105; przodki stawia test (urobek.js)
set -euo pipefail
N=${1:-gornik}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
M=$D/plugins/MMOItems/item/material.yml
sed -i '/^# >>> GORNIK wycinek/,/^# <<< GORNIK wycinek/d' "$M"; cat "$H/przedmioty-klon.yml" >> "$M"
/opt/docker/mc/narzedzia/klon.sh restart "$N" >/dev/null
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$D/logs/latest.log" | tail -2
R(){ docker exec "klon-$N" rcon-cli "$@"; }
V(){ docker exec "klon-$N" rcon-cli "execute in minecraft:overworld run minecraft:$*" >/dev/null; }
R "minecraft:difficulty ${TRUDNOSC:-easy}" >/dev/null   # easy: zawal ma ranic (peaceful leczy)
[[ "${2:-}" == --bez-terenu ]] && exit 0
V "forceload add 190 90 240 112"
R "minecraft:gamerule advance_time false" >/dev/null; R "minecraft:time set 6000" >/dev/null; R "minecraft:weather clear" >/dev/null
R "minecraft:gamerule spawn_monsters false" >/dev/null || R "minecraft:gamerule doMobSpawning false" >/dev/null || true
V "kill @e[type=!minecraft:player,x=190,y=-64,z=90,dx=50,dy=20,dz=22]"
# bryla skaly i wydrazony chodnik
V "fill 196 -61 95 234 -52 106 minecraft:stone"
V "fill 214 -61 95 234 -52 106 minecraft:deepslate"
V "fill 198 -59 100 232 -57 103 minecraft:air"
V "fill 198 -60 100 232 -60 103 minecraft:cobblestone"
V "fill 198 -59 104 232 -57 104 minecraft:air"
for x in 200 210 220 230; do V "setblock $x -57 103 minecraft:lantern[hanging=false]"; done
V "setworldspawn 205 -59 102"
echo "TEREN GOTOWY"
