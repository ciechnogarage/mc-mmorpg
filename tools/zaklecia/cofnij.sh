#!/usr/bin/env bash
# Cofniecie zaklec: przywraca pliki z tej kopii, a NOWE.txt kasuje. Potem mm reload + nexo reload pack.
set -euo pipefail
kopia="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE="${1:-/opt/docker/mc/data/world1}"; KONT="${2:-mc-world1-1}"
[[ -f "$kopia/NOWE.txt" ]] && while read -r f; do rm -rf "${DANE:?}/$f"; echo "usuniete: $f"; done < "$kopia/NOWE.txt"
(cd "$kopia" && find plugins -type f 2>/dev/null) | while read -r f; do mkdir -p "$DANE/$(dirname "$f")"; cp -a "$kopia/$f" "$DANE/$f"; echo "przywrocone: $f"; done
rm -f "$DANE"/plugins/zaklecia-*.jar
docker exec "$KONT" rcon-cli 'mm reload' >/dev/null; docker exec "$KONT" rcon-cli 'nexo reload pack' >/dev/null
echo "cofniete (plugin Zaklecia usuniety z plugins/, zniknie po restarcie)"
