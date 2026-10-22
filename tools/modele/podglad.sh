#!/usr/bin/env bash
# Arkusze oceny (podglad.png, klatki.png) dla GOTOWEGO pliku .bbmodel, bez uruchamiania generatora - np. dla modeli
# przywroconych z archiwum (PRZYWROCONE.txt). Bramka ziemi tylko informuje. Uzycie: tools/modele/podglad.sh pniaczek
set -euo pipefail
m=$1
W=/opt/docker/mc/data/world1; T=$W/tools; D=$W/output/lochy/modele-wlasne/$m
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
python3 "$T/bbrender/bb_ziemia.py" "$D/$m.bbmodel" --dozwolone spawn,death || echo "(bramka ziemi: uwaga powyzej, model nie zmieniony)"
python3 "$T/bbrender/bb_mcp_arkusz.py" "$tmp/widoki" "$D/$m.bbmodel" >/dev/null
python3 "$T/bbrender/bb_oczy_gracza.py" "$tmp/widoki" "$D/$m.bbmodel" >/dev/null
python3 "$T/bbrender/bb_klatki.py" "$tmp/klatki" "$D/$m.bbmodel" >/dev/null
docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 \
  "$T/bbrender/arkusz_podgladu.py" "$tmp/widoki" "$D/podglad.png"
docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 \
  "$T/bbrender/arkusz_klatek.py" "$tmp/klatki" "$D/klatki.png"
python3 "$T/bbrender/bb_sesja.py"
ls "$D"
