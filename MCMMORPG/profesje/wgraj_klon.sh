#!/usr/bin/env bash
# Build pluginu Rzemioslo i podmiana na klonie (klon.sh). Stary jar znika TYLKO gdy nowy sie zbudowal.
# Uzycie: wgraj_klon.sh [nazwa=profesje]
set -euo pipefail
N=${1:-profesje}; D=/opt/docker/mc/klony/$N/data; R="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; P=$R/plugin-src/rzemioslo
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' | head; echo "BUILD NIEUDANY — klon bez zmian"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1); [[ -f "$J" ]] || exit 1
DZIALA=$(docker ps --format "{{.Names}}" | grep -cx "klon-$N" || true)   # restart wlasnego kontenera nie oddaje slotu klonu
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
if [[ "$DZIALA" == 1 ]]; then
  L=$D/logs/latest.log; I0=$(stat -c %i "$L"); docker restart "klon-$N" >/dev/null
  for i in $(seq 1 150); do sleep 4; [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I0" ]] && grep -q 'Done (' "$L" && break; done
else
  /opt/docker/mc/narzedzia/klon.sh start "$N"
fi
grep 'Rzemioslo\] Kontrola' "$D/logs/latest.log" || echo "UWAGA: brak linii kontroli Rzemioslo"
