#!/bin/bash
# Przygotowanie klona `klon.sh start <nazwa>` do testow Wyscigow: jar, menu, kafel w Q -> Spolecznosc,
# swiat 'wyscigi' (VoidGen), zakaz Chowancow w tym swiecie, restart, budowa toru.
set -euo pipefail
N=${1:-wyscigi}; K=klon-$N; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
R() { docker exec -i $K rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
[ -d "$D/plugins" ] || { echo "brak $D/plugins"; exit 1; }
ls $A/build/libs/wyscigi-*.jar >/dev/null

rm -f $D/plugins/wyscigi-*.jar
cp $A/build/libs/wyscigi-*.jar $D/plugins/
cp $A/wdrozenie/pliki/DeluxeMenus/gui_menus/wyscigi*.yml $D/plugins/DeluxeMenus/gui_menus/
python3 $A/wdrozenie/latka.py $D/plugins $A/wdrozenie/hub-kafel.yml

L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart -t 60 $K >/dev/null
for i in $(seq 1 150); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
grep -a -E 'Enabling Wyscigi|\[Wyscigi\]' $L | head -5

if ! R 'mv list' | grep -q wyscigi; then
  grep -v '^#' $A/wdrozenie/swiat/ustaw.txt | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
fi
R 'wyscigi zbuduj'
docker exec $K rcon-cli "minecraft:execute in minecraft:wyscigi run time set 6000" >/dev/null
echo gotowe
