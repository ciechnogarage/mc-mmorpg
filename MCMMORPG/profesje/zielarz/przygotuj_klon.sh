#!/usr/bin/env bash
# Wycinek zielarza na KLONIE (klon.sh): jar Rzemiosla, przedmioty tylko-klon, restart, teren testowy, kepy, suszarnie.
# Uzycie: [WARIANT=v7|a|b|c] przygotuj_klon.sh [nazwa=zielarz] [--bez-terenu]
# Klon stawiaj z kreatorem: klon.sh start <nazwa> --z-kreatorem (bez niego Wiary/MMOProfiles nie wstaja i karta postaci
# pokazuje surowe %wiary_*% i klase "Human" — tak powstaly kadry v7 z 21.10)
set -euo pipefail
N=${1:-zielarz}; D=/opt/docker/mc/klony/$N/data; H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; P=$H/../../plugin-src/rzemioslo
[[ -d "$D/plugins" ]] || { echo "brak klonu $N"; exit 2; }
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' -A3 | head -20; echo "BUILD NIEUDANY"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1)
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
# grafika zielarza: bloki kep i karty zblizenia (Nexo zbuduje pack przy starcie)
G=$H/grafika; rm -rf "$G/tex" "$G/nexo"
"$G/px.sh" "$G/dziurawiec.py" "$G/tex" >/dev/null && "$G/px.sh" "$G/reszta.py" "$G/tex" >/dev/null && "$G/px.sh" "$G/wzory.py" "$G/tex" >/dev/null && "$G/px.sh" "$G/korzen3d.py" "$G/tex" "$G/nexo" >/dev/null && "$G/px.sh" "$G/nexo_klon.py" "$G/tex" "$G/nexo" >/dev/null
cp "$G/nexo/items/zielarz.yml" "$D/plugins/Nexo/items/zielarz.yml"
mkdir -p "$D/plugins/Nexo/pack/assets/nexo/textures/block/zielarz" "$D/plugins/Nexo/pack/assets/nexo/models/zielarz"
cp "$G"/nexo/pack/assets/nexo/textures/block/zielarz/*.png "$D/plugins/Nexo/pack/assets/nexo/textures/block/zielarz/"
cp "$G"/nexo/pack/assets/nexo/models/zielarz/*.json "$D/plugins/Nexo/pack/assets/nexo/models/zielarz/"
python3 "$H/wyglad/grafika_ui.py" "$D/plugins/Nexo/pack" >/dev/null   # font zielarz:ui: ikony klawiszy i tabliczka HUD (komunikaty A/B/C)
# wlasne dzwieki kozlka (feeling mmo/rytual; synteza CC0, wyglad/dzwieki/ZRODLA.md): wlasna przestrzen zielarz:, Nexo jej nie generuje
mkdir -p "$D/plugins/Nexo/pack/assets/zielarz/sounds/kozlek"
cp "$H"/wyglad/dzwieki/ogg/kozlek/*.ogg "$D/plugins/Nexo/pack/assets/zielarz/sounds/kozlek/"
cp "$H/wyglad/dzwieki/sounds.json" "$D/plugins/Nexo/pack/assets/zielarz/sounds.json"
# wariant komunikatow: WARIANT=v7|a|b|c przygotuj_klon.sh ... (bez zmiennej zostaje to, co w configu; domyslnie v7)
if [[ -n "${WARIANT:-}" ]]; then python3 "$H/wyglad/wariant.py" "$D/plugins/Rzemioslo/config.yml" "$WARIANT"; fi
# feeling chwili zbioru kozlka: FEELING=brak|subtelny|mmo|rytual (przelaczanie w biegu: feeling.py + rzemioslo reload)
if [[ -n "${FEELING:-}" ]]; then python3 "$H/wyglad/feeling.py" "$D/plugins/Rzemioslo/config.yml" "$FEELING"; fi
M=$D/plugins/MMOItems/item/material.yml
sed -i '/^# >>> ZIELARZ wycinek/,/^# <<< ZIELARZ wycinek/d' "$M"; cat "$H/przedmioty-klon.yml" >> "$M"
python3 "$H/klon_v7.py" "$D"   # v7: receptury z wybornymi + skup wybornych (tylko klon)
L=$D/logs/latest.log
# jak world1: klon dostaje domyslny paper-global (updates: false) -> nadepniety tripwire zmienia stan i kepa Nexo
# staje sie innym blokiem (25.10: dziurawiec 42,42 zamienil sie w wezel v2 "Dzikie przyprawy")
sed -i -E 's/^(\s*disable-(tripwire|noteblock)-updates:) false/\1 true/' "$D/config/paper-global.yml"
/opt/docker/mc/narzedzia/klon.sh restart "$N" >/dev/null   # restart wlasnego klonu bez kolejki (nie docker restart)
grep -E 'Rzemioslo\] (Kontrola|Profesje)' "$L" | tail -2
R0(){ docker exec "klon-$N" rcon-cli "$@" >/dev/null; }
R0 "minecraft:difficulty peaceful"; R0 "minecraft:time set 4000"   # 1000 = poranna rosa (mokry zbior); klon.sh daje DIFFICULTY=easy przy KAZDYM starcie (itzg nadpisuje server.properties) -> po klon.sh restart powtorz
[[ "${2:-}" == --bez-terenu ]] && exit 0
R(){ docker exec "klon-$N" rcon-cli "$@" >/dev/null; }
# komendy vanilli w overworld: na klonie --z-kreatorem goly fill/setblock z RCON nie trafia do overworldu
V(){ docker exec "klon-$N" rcon-cli "execute in minecraft:overworld run minecraft:$*" >/dev/null; }
Y=-60   # wierzch trawy w plaskim swiecie klonu; rosliny na Y+1
V "forceload add 30 30 80 80"
R "minecraft:gamerule advance_time false"; R "minecraft:gamerule advance_weather false"; R "minecraft:time set 4000"; R "minecraft:weather clear"
R "minecraft:difficulty peaceful"; V "fill 143 98 191 155 98 203 stone"   # spawn z configu world1 to na plaskim klonie powietrze
V "fill 30 $((Y+1)) 30 80 $((Y+8)) 80 air"
# laka w sloncu: dziurawiec
for xz in "42 42" "45 44" "48 41" "44 48" "50 46"; do set -- $xz; V "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_dziurawiec world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz dziurawiec world $1 $((Y+1)) $2"; done
# laka nie jak stol (ocena 24.10 pkt 7): kepy trawy i kwiatow wokol; kepy zielarza (tripwire) zostaja, feature sadzi tylko na powietrzu
for xz in "40 40" "47 39" "52 44" "41 47" "47 50" "53 50" "38 44"; do set -- $xz; V "place feature minecraft:patch_grass $1 $((Y+1)) $2"; done
for xz in "44 37" "51 39" "38 50"; do set -- $xz; V "place feature minecraft:flower_meadow $1 $((Y+1)) $2"; done
# staw z brzegiem: kozlek
V "fill 58 $((Y-1)) 40 64 $Y 46 water"; V "fill 57 $Y 39 65 $Y 39 mud"
# kozlek: 4 przy stawie + 4 w suchej ziemi (K2)
for xz in "57 41" "57 44" "61 47" "65 42" "71 50" "74 46" "72 54" "77 51"; do set -- $xz; V "setblock $1 $Y $2 grass_block"; V "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_kozlek world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz kozlek world $1 $((Y+1)) $2"; done
# zagajnik w cieniu: czosnek niedzwiedzi i konwalia wygladaja tak samo
for xz in "40 60" "46 58" "52 62" "44 68"; do set -- $xz; V "place feature minecraft:oak $1 $((Y+1)) $2"; done
V "fill 36 $((Y+1)) 54 56 $((Y+2)) 72 air replace minecraft:oak_leaves"   # niskie liscie zamykaly przejscie (2 bloki wysokosci dla gracza)
i=0; for xz in "42 61" "43 63" "47 60" "49 64" "45 66" "50 59" "41 65"; do set -- $xz
  g=$([[ $((i % 3)) == 1 ]] && echo konwalia || echo czosnek); i=$((i+1))
  V "setblock $1 $((Y+1)) $2 air"; R "rzemioslo postaw zielarz_szerokolistna world $1 $((Y+1)) $2"; R "rzemioslo kepa-xyz $g world $1 $((Y+1)) $2"; done
# szopa z suszarnia w cieniu + suszarnia na sloncu obok
V "fill 60 $Y 56 64 $((Y+5)) 60 spruce_planks hollow"; V "fill 60 $((Y+1)) 58 60 $((Y+2)) 58 air"   # wnetrze 4 bloki, wejscie od zachodu
V "fill 61 $((Y+4)) 58 63 $((Y+4)) 58 stripped_spruce_log[axis=x]"; R "rzemioslo suszarnia-xyz world 62 $((Y+4)) 58"
V "setblock 67 $((Y+1)) 58 spruce_fence"; V "setblock 67 $((Y+2)) 58 stripped_spruce_log[axis=x]"; R "rzemioslo suszarnia-xyz world 67 $((Y+2)) 58"
V "setworldspawn 50 $((Y+1)) 52"
echo "TEREN GOTOWY"
