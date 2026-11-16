#!/usr/bin/env bash
# Katalog obiektow Iris z packa overworld: strony po 48 (clutter, structures, jigsaw bez podwodnych/netheru).
set -euo pipefail
. "$(dirname "$(readlink -f "$0")")/../kuznia.sh"   # TOOLS DATA LEVELS KONTENER
O=/opt/docker/mc/data/world1/plugins/Iris/packs/overworld/objects
OUT=/opt/docker/mc/data/world1/output/lochy/budowle/katalog; mkdir -p "$OUT"; rm -f "$OUT"/*
JAR=/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client
for kat in clutter structures jigsaw; do
  find "$O/$kat" -name '*.iob' | grep -viE 'underwater|ocean|nether|bastion|end_|coral|ice|snow|desert|badlands|mesa|jungle|bamboo|mushroom' | sort > "$OUT/$kat.txt"
  split -l 48 -d "$OUT/$kat.txt" "$OUT/${kat}_"
done
for f in "$OUT"/*_[0-9][0-9]; do
  mapfile -t m < "$f"
  docker run --rm -u "$(id -u):$(id -g)" -v /opt/docker/mc:/opt/docker/mc -v "$JAR:$JAR:ro" -v "$TOOLS:$TOOLS:ro" -w "$TOOLS/budowle" \
    pixelart:latest python3 -W ignore podglad.py "$f.png" "${m[@]}" --kolumny 8 --kafel 180 2>>"$OUT/bledy.txt"
done
wc -l "$OUT"/*.txt
