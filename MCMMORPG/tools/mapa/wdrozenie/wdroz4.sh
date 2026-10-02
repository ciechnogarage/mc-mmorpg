#!/usr/bin/env bash
# Wdrozenie MAPY (plugin Mapa v3: mapa w rece z blokow, atlas z ikonami i zakladkami Kraina/Stolica/Loch 001) na world1. Wymaga RESTARTU
# (nowy jar). Bez graczy na serwerze albo WYMUS=1. Dane generowane na swiezo z plikow swiata (tylko odczyt).
# Wymaga wczesniejszego wdroz.sh (v1: UI_MAPA, slot 7, map_30000.dat). Cofniecie: <kopia>/cofnij.sh
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; NARZ="$(dirname "$tu")"; WT="$(cd "$NARZ/../.." && pwd)"
DANE="${DANE:-/opt/docker/mc/data/world1}"; KONT="${KONT:-mc-world1-1}"; P="$DANE/plugins"
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: sa gracze (restart ich wyrzuci); WYMUS=1 wymusza"; exit 3; }
[[ -f "$DANE/world/data/map_30000.dat" ]] || { echo "BRAK v1 (map_30000.dat) - najpierw wdroz.sh"; exit 4; }

# 1. dane i jar (staging w katalogu tymczasowym)
ST="$(mktemp -d)"; trap 'rm -rf "$ST"' EXIT
python3 "$NARZ/teren.py" "$DANE/world/region" "$ST/t_world.json.gz" >/dev/null
python3 "$NARZ/teren.py" "$DANE/stolica_odnowa/region" "$ST/t_stolica.json.gz" >/dev/null
python3 "$NARZ/render.py" "$ST/t_world.json.gz" "$ST/r_world" >/dev/null
python3 "$NARZ/render.py" "$ST/t_stolica.json.gz" "$ST/r_stolica" >/dev/null
python3 "$NARZ/teren.py" "$P/MythicDungeons/maps/loch_001/region" "$ST/t_loch.json.gz" >/dev/null
python3 "$NARZ/render.py" "$ST/t_loch.json.gz" "$ST/r_loch" >/dev/null
( cd "$NARZ" && python3 kafle.py "$ST/out" world="$ST/r_world":3,6,12 stolica_odnowa="$ST/r_stolica":1,2,4 loch_001="$ST/r_loch":1,2,4 --znaczniki znaczniki.json )
( cd "$NARZ" && python3 blokidane.py "$ST/out/plugins/Mapa" world="$ST/t_world.json.gz":"$ST/r_world" stolica_odnowa="$ST/t_stolica.json.gz":"$ST/r_stolica" loch_001="$ST/t_loch.json.gz":"$ST/r_loch" 2>/dev/null )
"$WT/plugin-src/mapa/build.sh" >/dev/null 2>&1; JAR="$(ls "$WT"/plugin-src/mapa/build/libs/mapa-*.jar)"; echo "== jar: $(basename "$JAR")"

# 2. kopia
KOPIA="$DANE/backups/mapa3-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$KOPIA/plugins/CoreTools/Scripts" "$KOPIA/plugins/DeluxeMenus/gui_menus"
cp -a "$P/CoreTools/Scripts/stale-sloty.yml" "$KOPIA/plugins/CoreTools/Scripts/"
cp -a "$P/DeluxeMenus/gui_menus/hub_przygoda.yml" "$KOPIA/plugins/DeluxeMenus/gui_menus/"
[[ -d "$P/Mapa" ]] && cp -a "$P/Mapa" "$KOPIA/plugins/Mapa"; cp -a "$P"/mapa-*.jar "$KOPIA/" 2>/dev/null || true
cat > "$KOPIA/cofnij.sh" <<COF
#!/usr/bin/env bash
# Cofniecie v2 mapy (wraca v1: statyczna mapa + menu DM). Wymaga restartu (zdjecie jara).
set -euo pipefail; K="\$(cd "\$(dirname "\$0")" && pwd)"
cp -a "\$K/plugins/CoreTools/Scripts/stale-sloty.yml" "$P/CoreTools/Scripts/"
cp -a "\$K/plugins/DeluxeMenus/gui_menus/hub_przygoda.yml" "$P/DeluxeMenus/gui_menus/"
rm -f $P/mapa-*.jar; rm -rf "$P/Nexo/pack/assets/mapa"
[[ -d "\$K/plugins/Mapa" ]] && { rm -rf "$P/Mapa"; cp -a "\$K/plugins/Mapa" "$P/Mapa"; cp -a "\$K"/mapa-*.jar "$P/" 2>/dev/null || true; }
echo "pliki przywrocone; teraz restart: docker restart $KONT (plugins/Mapa zostaje jako dane)"
COF
chmod +x "$KOPIA/cofnij.sh"; echo "== kopia: $KOPIA"

# 3. pliki
rm -f "$P"/mapa-*.jar; cp "$JAR" "$P/"
mkdir -p "$P/Mapa"; rm -rf "$P/Mapa/swiaty"; cp -r "$ST/out/plugins/Mapa/swiaty" "$ST/out/plugins/Mapa/kafle.json" "$ST/out/plugins/Mapa/tekstury.bin" "$P/Mapa/"
rm -f "$P/Mapa/config.yml"   # config z jara (zakladki, zoom z blokow); poprzedni w kopii
rm -rf "$P/Nexo/pack/assets/mapa"; cp -r "$ST/out/pack/assets/mapa" "$P/Nexo/pack/assets/"
python3 "$tu/latka2.py" "$P"

# 4. restart (Nexo buduje pack przy starcie)
LOG="$DANE/logs/latest.log"; I0="$(stat -c %i "$LOG")"
echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal w 10 min"; exit 6; }
echo "== Mapa: $(grep -E '\[Mapa\] (Enabling|.*WARN|.*Brak)' "$LOG" | tail -3)"
grep -E "\[Mapa\].*" "$LOG" | grep -iE "warn|error|brak|niepoprawn" || echo "== Mapa bez ostrzezen"
for i in $(seq 1 30); do grep -q "Finished generating resourcepack" "$LOG" && break; sleep 2; done; sleep 5   # zip dopisywany chwile po komunikacie
unzip -l "$P/Nexo/pack/pack.zip" | grep -c "assets/mapa/items/" | sed 's/^/== kafelki w pack.zip: /'
echo "wdrozone; cofniecie: $KOPIA/cofnij.sh"
