#!/usr/bin/env bash
# Rybak na KLONIE (klon.sh): jar Rzemiosla, przedmioty wyborne tylko-klon, restart, dwa stawy testowe (T1 i T2).
# Uzycie: przygotuj_klon.sh [nazwa=rybak] [--bez-terenu]   (klon: klon.sh start <nazwa> --z-kreatorem)
set -euo pipefail
N=${1:-rybak}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
M=$D/plugins/MMOItems/item/material.yml
sed -i '/^# >>> RYBAK wycinek/,/^# <<< RYBAK wycinek/d' "$M"; cat "$H/przedmioty-klon.yml" >> "$M"
/opt/docker/mc/narzedzia/klon.sh restart "$N" >/dev/null
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$D/logs/latest.log" | tail -2
R(){ docker exec "klon-$N" rcon-cli "$@"; }
V(){ docker exec "klon-$N" rcon-cli "execute in minecraft:overworld run minecraft:$*" >/dev/null; }
R "minecraft:difficulty peaceful" >/dev/null
[[ "${2:-}" == --bez-terenu ]] && exit 0
Y=-60   # wierzch trawy plaskiego swiata klonu (gracz stoi na Y+1)
V "forceload add 90 20 170 80"
R "minecraft:gamerule advance_time false" >/dev/null; R "minecraft:time set 4000" >/dev/null; R "minecraft:weather clear" >/dev/null
R "minecraft:gamerule advance_weather false" >/dev/null || true
V "fill 90 $((Y+1)) 20 170 $((Y+12)) 80 air"
# staw T1: zwykla woda (kazda woda = lowisko I), brzeg zachodni x=109
V "fill 110 $((Y-2)) 30 130 $Y 60 water"
# staw T2: woda + znacznik lowisko_t2 na dnie (Nexo NOTE_BLOCK, promien 6) — test: wedka I za slaba
V "fill 145 $((Y-2)) 30 160 $Y 50 water"
R "rzemioslo postaw lowisko_t2 world 152 $((Y-2)) 40"
V "setworldspawn 108 $((Y+1)) 45"
echo "TEREN GOTOWY"
