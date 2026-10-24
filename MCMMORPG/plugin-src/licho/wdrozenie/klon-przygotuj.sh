#!/bin/bash
# Przygotowanie klona `klon.sh start --z-kreatorem <nazwa>` do testow Licha: jar, menu, kafel w Q -> Druzyna,
# swiat 'licho' (VoidGen), zakaz Chowancow w tym swiecie, restart (klon.sh restart), budowa zagrody.
set -euo pipefail
N=${1:-licho}; K=klon-$N; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
R() { docker exec -i $K rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
[ -d "$D/plugins" ] || { echo "brak $D/plugins"; exit 1; }
[ "$(ls $A/build/libs/licho-*.jar | wc -l)" = 1 ] || { echo "w build/libs ma byc dokladnie jeden jar"; exit 1; }

rm -f $D/plugins/licho-*.jar
cp $A/build/libs/licho-*.jar $D/plugins/
cp $A/wdrozenie/menu/gui_menus/*.yml $D/plugins/DeluxeMenus/gui_menus/
cp $A/wdrozenie/menu/gui/*.png $D/plugins/Nexo/pack/assets/nexo/textures/gui/
cp $A/wdrozenie/menu/glify_licho.yml $D/plugins/Nexo/glyphs/licho.yml
python3 $A/wdrozenie/latka.py $D/plugins $A/wdrozenie

/opt/docker/mc/narzedzia/klon.sh restart $N
R 'minecraft:difficulty peaceful' | tail -1
grep -a -E 'Enabling Licho|\[Licho\]' $D/logs/latest.log | head -5

if [ ! -d $D/licho ]; then
  R 'mv remove licho' </dev/null >/dev/null || true
  grep -v '^#' $A/wdrozenie/swiat/ustaw.txt | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
fi
R 'licho zbuduj'
R 'minecraft:execute in minecraft:licho run minecraft:time set 12800' >/dev/null
R 'nexo reload all' | tail -1
echo gotowe
