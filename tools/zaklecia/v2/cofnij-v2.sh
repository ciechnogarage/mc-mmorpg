#!/usr/bin/env bash
# Cofniecie v2: przywraca pliki i stary jar z tej kopii, kasuje NOWE.txt, restartuje (tylko przy 0 graczy).
set -euo pipefail
kopia="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE="${1:-/opt/docker/mc/data/world1}"; KONT="${2:-mc-world1-1}"
docker exec "$KONT" rcon-cli 'minecraft:list uuids' | grep -q "There are 0 of" || { echo "PRZERWANE: gracze online"; exit 3; }
while read -r f; do rm -f "${DANE:?}/$f"; echo "usuniete: $f"; done < "$kopia/NOWE.txt"
rm -f "$DANE"/plugins/zaklecia-*.jar
(cd "$kopia" && find plugins -type f) | while read -r f; do mkdir -p "$DANE/$(dirname "$f")"; cp -a "$kopia/$f" "$DANE/$f"; echo "przywrocone: $f"; done
docker restart "$KONT" >/dev/null; echo "restart zlecony — v1 wraca po starcie"
