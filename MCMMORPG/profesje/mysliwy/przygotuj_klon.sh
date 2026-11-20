#!/usr/bin/env bash
# Mysliwy na KLONIE (klon.sh): jar Rzemiosla, przedmioty tylko-klon, restart, plac lowiecki z rewirami i ogniskiem.
# Uzycie: przygotuj_klon.sh [nazwa=mysliwy] [--bez-terenu]   (klon: klon.sh start <nazwa> --z-kreatorem)
set -euo pipefail
N=${1:-mysliwy}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
for f in material:przedmioty-klon.yml consumable:przedmioty-klon-consumable.yml; do
  M=$D/plugins/MMOItems/item/${f%%:*}.yml
  sed -i '/^# >>> MYSLIWY wycinek/,/^# <<< MYSLIWY wycinek/d' "$M"; cat "$H/${f#*:}" >> "$M"
done
/opt/docker/mc/narzedzia/klon.sh restart "$N" >/dev/null
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$D/logs/latest.log" | tail -2
R(){ docker exec "klon-$N" rcon-cli "$@"; }
V(){ docker exec "klon-$N" rcon-cli "execute in minecraft:overworld run minecraft:$*" >/dev/null; }
R "minecraft:difficulty peaceful" >/dev/null
[[ "${2:-}" == --bez-terenu ]] && exit 0
Y=-60   # wierzch trawy plaskiego swiata klonu (gracz stoi na Y+1)
V "forceload add 180 60 300 200"
R "minecraft:gamerule advance_time false" >/dev/null; R "minecraft:time set 4000" >/dev/null; R "minecraft:weather clear" >/dev/null
R "minecraft:gamerule advance_weather false" >/dev/null || true
V "fill 180 $((Y+1)) 60 300 $((Y+12)) 200 air"
V "kill @e[type=!minecraft:player,x=180,y=-64,z=60,dx=120,dy=30,dz=140]"
# ogniska wedzarni (zapalone x2, zgaszone x1)
V "setblock 200 $((Y+1)) 190 minecraft:campfire[lit=true]"
V "setblock 204 $((Y+1)) 190 minecraft:campfire[lit=true]"
V "setblock 208 $((Y+1)) 190 minecraft:campfire[lit=false]"
V "setworldspawn 200 $((Y+1)) 180"
echo "TEREN GOTOWY (rewiry stawia test: rzemioslo rewir world <x> -59 <z> <mob>)"
