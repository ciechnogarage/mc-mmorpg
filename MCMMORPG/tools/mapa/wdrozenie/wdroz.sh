#!/usr/bin/env bash
# Wdrozenie MAPY SWIATA na world1 bez restartu i bez nowego pluginu:
#   world/data/map_30000.dat   zablokowana mapa vanilli z naszym obrazem (strzalka gracza + banery z nazwami)
#   Nexo glif mapa_swiata_bg   tlo okna "Mapa swiata" (obraz terenu)
#   DeluxeMenus mapa_swiata    okno pod Q -> Przygoda -> Mapa swiata (znaczniki = ikony gatunkow, "Tu jestes")
#   MMOItems UI_MAPA + CoreTools stale-sloty: mapa na stale w slocie 7 (klawisz 8), klik = duze okno
# Gracze juz zalogowani dostaja mape w slocie 7 przy nastepnym wejsciu / odrodzeniu / zmianie swiata.
# Cofniecie: <kopia>/cofnij.sh (sciezka wypisana na koncu).
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DANE="${DANE:-/opt/docker/mc/data/world1}"; KONT="${KONT:-mc-world1-1}"; MID=30000
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LOG="$DANE/logs/latest.log"; start=$(wc -l < "$LOG")
echo "== gracze: $(R 'minecraft:list uuids' | head -1)"

NOWE="world/data/map_$MID.dat plugins/Nexo/glyphs/mapa.yml plugins/Nexo/pack/assets/nexo/textures/gui/mapa_swiata_bg.png plugins/DeluxeMenus/gui_menus/mapa_swiata.yml"
ZMIENIANE="plugins/DeluxeMenus/config.yml plugins/DeluxeMenus/gui_menus/hub_przygoda.yml plugins/CoreTools/Scripts/stale-sloty.yml plugins/MMOItems/item/miscellaneous.yml"
KOPIA="$DANE/backups/mapa-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$KOPIA"
for f in $ZMIENIANE; do mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"; done
for f in $NOWE; do [[ -e "$DANE/$f" ]] && { mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"; } || echo "$f" >> "$KOPIA/NOWE.txt"; done
cat > "$KOPIA/cofnij.sh" <<COF
#!/usr/bin/env bash
# Cofniecie mapy swiata: przywraca pliki z tej kopii, kasuje NOWE, przeladowuje. Mapa w slotach graczy zostaje
# jako zwykly przedmiot - zdjac: minecraft:clear @a minecraft:filled_map[minecraft:custom_data~{MMOITEMS_ITEM_ID:"UI_MAPA"}]
set -euo pipefail; K="\$(cd "\$(dirname "\$0")" && pwd)"
for f in $ZMIENIANE; do cp -a "\$K/\$f" "$DANE/\$f"; done
[[ -f "\$K/NOWE.txt" ]] && while read -r f; do rm -f "$DANE/\$f"; done < "\$K/NOWE.txt"
for c in 'mi reload' 'ct reload' 'dm reload' 'nexo reload pack'; do docker exec $KONT rcon-cli "\$c" >/dev/null; done
echo cofniete
COF
chmod +x "$KOPIA/cofnij.sh"; echo "== kopia: $KOPIA"

# pliki
cp "$tu/pliki/world/data/map_$MID.dat" "$DANE/world/data/"
cp "$tu/pliki/plugins/Nexo/glyphs/mapa.yml" "$DANE/plugins/Nexo/glyphs/"
cp "$tu/pliki/plugins/Nexo/pack/assets/nexo/textures/gui/mapa_swiata_bg.png" "$DANE/plugins/Nexo/pack/assets/nexo/textures/gui/"
cp "$tu/pliki/plugins/DeluxeMenus/gui_menus/mapa_swiata.yml" "$DANE/plugins/DeluxeMenus/gui_menus/"
python3 "$tu/latka.py" "$DANE/plugins" "$MID"

# przeladowania (bez restartu)
R 'mi reload' >/dev/null; R 'ct reload' >/dev/null; R 'dm reload' | tail -2
R 'nexo reload pack' >/dev/null; sleep 20
glif() { python3 - "$DANE/plugins/Nexo/pack/pack.zip" <<'PY'
import sys, zipfile, json
z = zipfile.ZipFile(sys.argv[1]); n = z.namelist()
f = json.loads(z.read("assets/minecraft/font/default.json"))
ok = any("䆀" in "".join(p.get("chars", [])) for p in f["providers"])
print("ok" if ok and "assets/nexo/textures/gui/mapa_swiata_bg.png" in n else "brak")
PY
}
if [[ "$(glif)" != ok ]]; then echo "== glifu nie ma w packu po 'reload pack' -> pelne 'nexo reload'"; R 'nexo reload' >/dev/null; sleep 25; fi
echo "== glif w pack.zip: $(glif)"

echo "== kontrola logu (nowe linie WARN/ERROR z naszymi nazwami)"
tail -n +"$((start + 1))" "$LOG" | grep -E "WARN|ERROR" | grep -iE "mapa|UI_MAPA|stale|DeluxeMenus|CoreTools|Nexo" | head -20 || true
echo "== menu: $(R 'dm list' | tr ',' '\n' | grep -c mapa_swiata) x mapa_swiata"
echo "wdrozone; cofniecie: $KOPIA/cofnij.sh"
