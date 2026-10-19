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
python3 - "$D/plugins/DeluxeMenus" "$A/wdrozenie/hub-kafel.yml" <<'PY'
import sys, pathlib
dm = pathlib.Path(sys.argv[1]); kafel = pathlib.Path(sys.argv[2]).read_text()
cfg = dm / 'config.yml'; t = cfg.read_text()
if 'wyscigi.yml' not in t:
    wpis = ''.join(f"  {n}:\n    file: {n}.yml\n" for n in ('wyscigi', 'wyscigi_czasowka', 'wyscigi_wyscig'))
    t = t.replace('gui_menus:\n', 'gui_menus:\n' + wpis, 1)
    cfg.write_text(t)
hub = dm / 'gui_menus/hub_spolecznosc.yml'; h = hub.read_text()
if 'wyscigi_kafel' not in h:
    hub.write_text(h.rstrip('\n') + '\n' + kafel)
print('menu: ok')
PY
# Chowaniec towarzysz nie wchodzi do swiata wyscigow (despawn przy zmianie swiata juz jest).
python3 - "$D/plugins/Chowance/config.yml" <<'PY'
import sys, pathlib, re
p = pathlib.Path(sys.argv[1]); t = p.read_text()
m = re.search(r'^(\s*swiaty_zabronione: \[)([^\]]*)\]', t, re.M)
if m and 'wyscigi' not in m.group(2):
    t = t[:m.start()] + m.group(1) + (m.group(2) + ', ' if m.group(2).strip() else '') + 'wyscigi]' + t[m.end():]
    p.write_text(t)
print('chowance:', re.search(r'swiaty_zabronione: \[[^\]]*\]', t).group(0))
PY

L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart -t 60 $K >/dev/null
for i in $(seq 1 150); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
grep -a -E 'Enabling Wyscigi|\[Wyscigi\]' $L | head -5

if ! R 'mv list' | grep -q wyscigi; then
  grep -v '^#' $A/wdrozenie/swiat/ustaw.txt | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
fi
R 'wyscigi zbuduj'
docker exec $K rcon-cli "minecraft:execute in minecraft:wyscigi run time set 6000" >/dev/null
echo gotowe
