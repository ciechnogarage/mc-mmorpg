#!/usr/bin/env bash
# Generuje dzwieki kozlka (synteza.py) w kontenerze python:3.12-slim (numpy, scipy, soundfile, pillow instalowane na chwile).
# Uzycie: dzwieki.sh [katalog_podgladu]   -> ogg/kozlek/*.ogg obok skryptu; z podgladem: wav, png, pomiar.json
# Wzorzec glosnosci: assets klienta MC (MCMMORPG/scratchpad/mc-client/assets, tylko odczyt).
set -euo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; A=/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/assets
P=${1:-}; [[ -n "$P" ]] && mkdir -p "$P"
exec docker run --rm --name "dzwieki-zielarz-$$" --memory 1g -v "$H:/d" -v "$A:/assets:ro" ${P:+-v "$P:/podglad"} python:3.12-slim \
  sh -c "pip install -q --disable-pip-version-check --root-user-action=ignore numpy scipy soundfile pillow >/dev/null 2>&1 && python3 /d/synteza.py /d/ogg --vanilla /assets ${P:+--podglad /podglad} && chown -R $(id -u):$(id -g) /d/ogg ${P:+/podglad}"
