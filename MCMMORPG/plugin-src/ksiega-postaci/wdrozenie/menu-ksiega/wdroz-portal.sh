#!/bin/bash
# Wdrozenie MENU + Portal (commit e24a441) na world1. Kopia i cofnij.sh w backups/menu-portal-<czas>.
# Uruchom przy 0 graczach; na koncu restart world1 (jar ksiega-postaci).
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins
KP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"; K=$KP/wdrozenie/menu-ksiega; S="$(cd "$KP/../.." && pwd)"
B=$W/backups/menu-portal-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
F="DeluxeMenus/gui_menus/hub_glowne.yml MMOCore/waypoints/default_waypoints.yml MMOCore/messages.yml
Nexo/glyphs/ksiega_menu.yml KsiegaPostaci/config.yml ksiega-postaci-0.1.0.jar
Nexo/pack/assets/nexo/textures/gui/kb_1.png Nexo/pack/assets/nexo/textures/gui/kb_2.png
Nexo/pack/assets/nexo/textures/gui/kb_3.png Nexo/pack/assets/nexo/textures/gui/kb_4.png"
for f in $F; do mkdir -p "$B/$(dirname "$f")"; cp -a "$P/$f" "$B/$f"; done
cat > "$B/cofnij.sh" <<EOF
#!/bin/bash
set -e
for f in $(echo $F); do cp -a "$B/\$f" "$P/\$f"; done
rm -f "$P/Nexo/items/portal_waypoints.yml" "$P"/Nexo/pack/assets/nexo/textures/gui/portal_*.png
rm -rf "$P/Nexo/pack/assets/nexo/textures/item/portal"
echo "przywrocone - teraz: docker restart mc-world1-1"
EOF
chmod +x "$B/cofnij.sh"

# messages.yml: podmiana TYLKO sekcji waypoint: (reszta produkcyjnego pliku zostaje)
python3 - "$P/MMOCore/messages.yml" "$S/plugins/MMOCore/messages.yml" <<'PY'
import sys
prod, repo = (open(p, encoding='utf-8').read().split('\n') for p in sys.argv[1:3])
def blk(L):
    a = L.index('waypoint:'); b = a + 1
    while b < len(L) and (L[b].startswith(' ') or L[b] == ''): b += 1
    return a, b
pa, pb = blk(prod); ra, rb = blk(repo)
open(sys.argv[1], 'w', encoding='utf-8').write('\n'.join(prod[:pa] + repo[ra:rb] + prod[pb:]))
PY

cp "$K/hub_glowne.yml" "$P/DeluxeMenus/gui_menus/"
cp "$S/plugins/MMOCore/waypoints/default_waypoints.yml" "$P/MMOCore/waypoints/"
cp "$K/glify_ksiega.yml" "$P/Nexo/glyphs/ksiega_menu.yml"
cp "$K"/k12/kb_?.png "$K"/k12/portal_?.png "$K"/k12/portal_4_count_?.png "$P/Nexo/pack/assets/nexo/textures/gui/"
cp "$K/portal_waypoints.yml" "$P/Nexo/items/"
mkdir -p "$P/Nexo/pack/assets/nexo/textures/item/portal"
cp "$K"/portal-ikony/*.png "$P/Nexo/pack/assets/nexo/textures/item/portal/"
cp "$KP/build/libs/ksiega-postaci-0.1.0.jar" "$P/"
sed -i "s/^  znaki-tytulu: \['䅨'\]/  znaki-tytulu: ['䅨', '䅬']/" "$P/KsiegaPostaci/config.yml"
grep -q "'䅬'" "$P/KsiegaPostaci/config.yml" || echo "UWAGA: dopisz '䅬' do ukryj-ekwipunek.znaki-tytulu w KsiegaPostaci/config.yml"
echo "Wgrane. Kopia: $B  (cofnij: $B/cofnij.sh). Teraz: docker restart mc-world1-1"
