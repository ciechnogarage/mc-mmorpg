#!/usr/bin/env bash
# Wycinek zielarza na KLONIE (klon.sh): jar Rzemiosla, przedmioty tylko-klon, restart, teren testowy, kepy, suszarnie.
# Uzycie: przygotuj_klon.sh [nazwa=zielarz] [--bez-terenu]
set -euo pipefail
N=${1:-zielarz}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
# grafika zielarza: bloki kep i karty zblizenia (Nexo zbuduje pack przy starcie)
G=$H/grafika; rm -rf "$G/tex" "$G/nexo"
"$G/px.sh" "$G/dziurawiec.py" "$G/tex" >/dev/null && "$G/px.sh" "$G/reszta.py" "$G/tex" >/dev/null && "$G/px.sh" "$G/nexo_klon.py" "$G/tex" "$G/nexo" >/dev/null
cp "$G/nexo/items/zielarz.yml" "$D/plugins/Nexo/items/zielarz.yml"
mkdir -p "$D/plugins/Nexo/pack/assets/nexo/textures/block/zielarz" "$D/plugins/Nexo/pack/assets/nexo/models/zielarz"
cp "$G"/nexo/pack/assets/nexo/textures/block/zielarz/*.png "$D/plugins/Nexo/pack/assets/nexo/textures/block/zielarz/"
cp "$G"/nexo/pack/assets/nexo/models/zielarz/*.json "$D/plugins/Nexo/pack/assets/nexo/models/zielarz/"
M=$D/plugins/MMOItems/item/material.yml
sed -i '/^# >>> ZIELARZ wycinek/,/^# <<< ZIELARZ wycinek/d' "$M"; cat "$H/przedmioty-klon.yml" >> "$M"
L=$D/logs/latest.log; I0=$(stat -c %i "$L"); docker restart "klon-$N" >/dev/null
for i in $(seq 1 150); do sleep 4; [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I0" ]] && grep -q 'Done (' "$L" && break; done
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$L" | tail -2
R0(){ docker exec "klon-$N" rcon-cli "$@" >/dev/null; }
R0 "minecraft:difficulty peaceful"; R0 "minecraft:time set 1000"
[[ "${2:-}" == --bez-terenu ]] && exit 0
R(){ docker exec "klon-$N" rcon-cli "$@" >/dev/null; }
Y=-60   # wierzch trawy w plaskim swiecie klonu; rosliny na Y+1
R "forceload add 30 30 80 80"
R "minecraft:gamerule advance_time false"; R "minecraft:gamerule advance_weather false"; R "minecraft:time set 1000"; R "minecraft:weather clear"
R "minecraft:difficulty peaceful"; R "fill 143 98 191 155 98 203 stone"   # spawn z configu world1 to na plaskim klonie powietrze
R "fill 30 $((Y+1)) 30 80 $((Y+8)) 80 air"
# laka w sloncu: dziurawiec
for xz in "42 42" "45 44" "48 41" "44 48" "50 46"; do set -- $xz; R "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_dziurawiec world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz dziurawiec world $1 $((Y+1)) $2"; done
# staw z brzegiem: kozlek
R "fill 58 $((Y-1)) 40 64 $Y 46 water"; R "fill 57 $Y 39 65 $Y 39 mud"
for xz in "57 41" "57 44" "61 47" "65 42"; do set -- $xz; R "setblock $1 $Y $2 grass_block"; R "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_kozlek world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz kozlek world $1 $((Y+1)) $2"; done
# zagajnik w cieniu: czosnek niedzwiedzi i konwalia wygladaja tak samo
for xz in "40 60" "46 58" "52 62" "44 68"; do set -- $xz; R "place feature minecraft:oak $1 $((Y+1)) $2"; done
i=0; for xz in "42 61" "43 63" "47 60" "49 64" "45 66" "50 59" "41 65"; do set -- $xz
  g=$([[ $((i % 3)) == 1 ]] && echo konwalia || echo czosnek); i=$((i+1))
  R "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_szerokolistna world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz $g world $1 $((Y+1)) $2"; done
# szopa z suszarnia w cieniu + suszarnia na sloncu obok
R "fill 60 $Y 56 64 $((Y+5)) 60 spruce_planks hollow"; R "fill 60 $((Y+1)) 58 60 $((Y+2)) 58 air"   # wnetrze 4 bloki, wejscie od zachodu
R "fill 61 $((Y+4)) 58 63 $((Y+4)) 58 stripped_spruce_log[axis=x]"; R "rzemioslo suszarnia-xyz world 62 $((Y+4)) 58"
R "setblock 67 $((Y+1)) 58 spruce_fence"; R "setblock 67 $((Y+2)) 58 stripped_spruce_log[axis=x]"; R "rzemioslo suszarnia-xyz world 67 $((Y+2)) 58"
R "setworldspawn 50 $((Y+1)) 52"
echo "TEREN GOTOWY"
