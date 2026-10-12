#!/usr/bin/env bash
# Build pluginu Rzemioslo i podmiana na klonie (klon.sh). Stary jar znika TYLKO gdy nowy sie zbudowal.
# Uzycie: wgraj_klon.sh [nazwa=profesje]
set -euo pipefail
N=${1:-profesje}; D=/opt/docker/mc/klony/$N/data; R="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; P=$R/plugin-src/rzemioslo
"$P/build.sh" >/dev/null 2>&1 || { "$P/build.sh" 2>&1 | grep -E 'error' | head; echo "BUILD NIEUDANY — klon bez zmian"; exit 1; }
J=$(ls "$P"/build/libs/rzemioslo-*.jar | head -1); [[ -f "$J" ]] || exit 1
/opt/docker/mc/narzedzia/klon.sh stop "$N"
rm -f "$D"/plugins/rzemioslo-*.jar; cp "$J" "$D/plugins/"
/opt/docker/mc/narzedzia/klon.sh start "$N"
grep 'Rzemioslo\] Kontrola' "$D/logs/latest.log" || echo "UWAGA: brak linii kontroli Rzemioslo"
