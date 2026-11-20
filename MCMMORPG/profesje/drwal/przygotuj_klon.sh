#!/usr/bin/env bash
# Wycinek drwala na KLONIE (klon.sh): jar Rzemiosla, przedmioty wyborne tylko-klon, restart, las testowy.
# Uzycie: przygotuj_klon.sh [nazwa=drwal] [--bez-terenu]   (klon: klon.sh start <nazwa> --z-kreatorem)
set -euo pipefail
N=${1:-drwal}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
M=$D/plugins/MMOItems/item/material.yml
sed -i '/^# >>> DRWAL wycinek/,/^# <<< DRWAL wycinek/d' "$M"; cat "$H/przedmioty-klon.yml" >> "$M"
/opt/docker/mc/narzedzia/klon.sh restart "$N" >/dev/null
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$D/logs/latest.log" | tail -2
R(){ docker exec "klon-$N" rcon-cli "$@"; }
V(){ docker exec "klon-$N" rcon-cli "execute in minecraft:overworld run minecraft:$*" >/dev/null; }
R "minecraft:difficulty peaceful" >/dev/null
[[ "${2:-}" == --bez-terenu ]] && exit 0
Y=-60   # wierzch trawy plaskiego swiata klonu
V "forceload add 96 26 156 86"
R "minecraft:gamerule advance_time false" >/dev/null; R "minecraft:time set 4000" >/dev/null; R "minecraft:weather clear" >/dev/null
V "fill 143 98 191 155 98 203 stone"
V "fill 96 $((Y+1)) 26 156 $((Y+20)) 86 air"
# pojedyncze deby i swierki z odstepem (czysta scinka w kazda strone)
for xz in "110 40" "125 40" "140 40" "110 52" "125 52"; do set -- $xz; V "place feature minecraft:oak $1 $((Y+1)) $2"; done
for xz in "110 64" "125 64" "140 64"; do set -- $xz; V "place feature minecraft:spruce $1 $((Y+1)) $2"; done
# para blisko siebie: dab 140,52 zawisa na sasiednim przy upadku na wschod
V "place feature minecraft:oak 140 $((Y+1)) 52"; V "place feature minecraft:oak 144 $((Y+1)) 52"
# rozlozysty dab z galeziami
V "place feature minecraft:fancy_oak 110 $((Y+1)) 78"
# kamienny mur obok: przeszkoda nie-drzewna
V "fill 125 $((Y+1)) 76 125 $((Y+4)) 82 cobblestone"; V "place feature minecraft:oak 121 $((Y+1)) 79"
# losowy ksztalt featurow: nisko zwisajace liscie zaslaniaja pien (powtarzalnosc testu) — czyscimy 2 dolne warstwy
for l in oak_leaves spruce_leaves; do V "fill 96 $((Y+1)) 26 156 $((Y+2)) 86 air replace $l"; done
R "rzemioslo las world 96 26 156 86"
V "setworldspawn 125 $((Y+1)) 46"
R "rzemioslo drzewa-skan world 96 26 156 86"
echo "TEREN GOTOWY"
