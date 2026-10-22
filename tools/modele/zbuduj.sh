#!/usr/bin/env bash
# Buduje model z generatora i sklada arkusze oceny. Wynik (tylko to):
#   output/lochy/modele-wlasne/<model>/<model>.bbmodel  - plik dla ModelEngine
#   .../podglad.png  - przod | tyl | klatka ataku (kamera 3/4 z gory) + widok z oczu gracza (2 i 4 bloki)
#   .../klatki.png   - wszystkie animacje po 5 klatek, wspolny kadr
# Zrzuty posrednie ida do katalogu tymczasowego i sa kasowane; w Blockbenchu na koniec otwarte tylko gotowe modele. Wymaga Blockbencha z MCP (tools/blockbench/start-desktop.sh).
# Uzycie: tools/modele/zbuduj.sh korzeniak
set -euo pipefail
m=$1
W=/opt/docker/mc/data/world1; T=$W/tools; D=$W/output/lochy/modele-wlasne/$m
tmp=$(mktemp -d); trap 'rm -rf "$tmp"; python3 "$T/bbrender/bb_sesja.py" >/dev/null 2>&1 || true' EXIT   # Blockbench zawsze posprzatany (POKAZ.txt)
if [ -f "$D/PRZYWROCONE.txt" ] && [ "${NADPISZ:-}" != 1 ]; then
  cat "$D/PRZYWROCONE.txt"; echo "Nie buduje (wlasciciel wybral przywrocona wersje). Arkusze: tools/modele/podglad.sh $m; swiadome nadpisanie: NADPISZ=1"; exit 1
fi
mkdir -p "$D"
cd "$D"
"$T/modele/uruchom.sh" "$T/modele/$m.py" "$D/$m.bbmodel"
dozw=$(sed -n "s/^POD_ZIEMIA = {\(.*\)}.*/\1/p" "$T/modele/$m.py" | tr -d "' ")   # np. spawn:8,death:6
python3 "$T/bbrender/bb_ziemia.py" "$D/$m.bbmodel" --dozwolone "${dozw:-spawn}" || { echo "BRAMKA ZIEMI: cos tonie - popraw animacje"; python3 "$T/bbrender/bb_sesja.py" >/dev/null; exit 1; }
python3 "$T/bbrender/bb_mcp_arkusz.py" "$tmp/widoki" "$D/$m.bbmodel" >/dev/null
python3 "$T/bbrender/bb_oczy_gracza.py" "$tmp/widoki" "$D/$m.bbmodel" >/dev/null
python3 "$T/bbrender/bb_klatki.py" "$tmp/klatki" "$D/$m.bbmodel" >/dev/null
docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 \
  "$T/bbrender/arkusz_podgladu.py" "$tmp/widoki" "$D/podglad.png"
if ls "$tmp/klatki"/*.png >/dev/null 2>&1; then   # model bez animacji (etap koloru) - bez arkusza klatek
  docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 \
    "$T/bbrender/arkusz_klatek.py" "$tmp/klatki" "$D/klatki.png"
fi
ls "$D"
