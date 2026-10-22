#!/usr/bin/env bash
# Katalog calej paczki LittleRoom: strony po 40 modeli (bez efektow, czesci, telegrafow i modeli wykluczonych).
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
modele="/home/dev/Desktop/Przemek/MC/modele/OUTPUT — kopia (2)"
out=/opt/docker/mc/data/world1/output/lochy/modele/katalog; mkdir -p "$out"
cd "$modele"
find ModelEngine/blueprints -name '*.bbmodel' -printf '%f\t%p\n' | sort -u -t$'\t' -k1,1 \
 | grep -viE '(_fx|fx_|telegraph|splash|_parts|groundbreak|groundcrack|explosion|_shot|_beam|_damage|debris|_chunks|gibs|imposter|pomni|skibidi|luffy|steamboat|gman|headcrab|poring|angeling|deviling|chocobo|mudkip|wither|arcade|gokart|bumper|balloon|santa|easter|bbq|lawnmower|cranegame|jackpot|cashregister|portapotty|lockers|sofa|recordplayer|tank|nuke|^pc|caine|jax|rlgl)' \
 | cut -f2 | sed 's#^#/modele/#' > "$out/lista.txt"
split -l 40 -d "$out/lista.txt" "$out/strona_"
for f in "$out"/strona_[0-9][0-9]; do
  mapfile -t m < "$f"
  "$here/render.sh" "$f.png" "${m[@]}" --kolumny 5 --rozmiar 150 2>>"$out/bledy.txt"
done
wc -l < "$out/lista.txt"
