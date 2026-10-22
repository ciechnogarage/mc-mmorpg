#!/usr/bin/env bash
# Cofa zaslanianie v2 + bron do lewej: przywraca 2 jary i 3 pliki z tej kopii, restart world1 (0 graczy, WYMUS=1 wymusza).
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE=/opt/docker/mc/data/world1; KONT=mc-world1-1
LISTA="$(docker exec "$KONT" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online"; exit 3; }
rm -f "$DANE"/plugins/zaklecia-*.jar; cp -a "$tu"/plugins/. "$DANE/plugins/"
docker restart "$KONT" >/dev/null; echo "== przywrocono z $tu, restart world1"
