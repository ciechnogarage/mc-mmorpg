#!/usr/bin/env bash
# Wdrozenie MAPY v11 (minimapa: znaczniki wlasciwej postaci po jej wyborze; cofniecie NIE kasuje znacznikow graczy; v10: osobna mapa gracza omija numer NMinimap + NMinimap na map-id 1999000000 = koniec "mapa znika"; v9: osobne mapy graczy; v8: poprawki: PPM przeciaga, Shift+PPM pelny ekran, ruch zamyka, MapEngine poza watkiem glownym,
# NMinimap: prog RAM, render bez watku glownego; v7: panel mapy zamiast ekwipunku w atlasie; v6: wlasne znaczniki gracza per postac + prowadzenie; v5: Q/PPM otwiera mape na caly ekran, przycisk Atlas, ikony Mapy na minimapie NMinimap)
# Poprzednio: v4 (v3 + MAPA NA CALY EKRAN na pluginie MapEngine 1.8.12: kursor wzrokiem, PPM przeciaga,
# kolko = zoom, LPM = zakladki, Shift = wyjscie; przycisk w atlasie) na world1. Wymaga RESTARTU
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
( cd "$NARZ" && python3 ikony.py znaczniki.json "$ST/out/plugins/Mapa" >/dev/null )
( cd "$NARZ" && python3 ikony.py --modele "$ST/out/plugins/Mapa/ikony" "$ST/out/pack" )   # przedmioty mapa:ikona/* (panel atlasu)
( cd "$NARZ" && python3 blokidane.py "$ST/out/plugins/Mapa" world="$ST/t_world.json.gz":"$ST/r_world" stolica_odnowa="$ST/t_stolica.json.gz":"$ST/r_stolica" loch_001="$ST/t_loch.json.gz":"$ST/r_loch" 2>/dev/null )
"$WT/plugin-src/mapa/build.sh" >/dev/null 2>&1; JAR="$(ls "$WT"/plugin-src/mapa/build/libs/mapa-*.jar)"; echo "== jar: $(basename "$JAR")"

# 2. kopia
KOPIA="$DANE/backups/mapa11-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$KOPIA/plugins/CoreTools/Scripts" "$KOPIA/plugins/DeluxeMenus/gui_menus"
cp -a "$P/CoreTools/Scripts/stale-sloty.yml" "$KOPIA/plugins/CoreTools/Scripts/"
cp -a "$P/DeluxeMenus/gui_menus/hub_przygoda.yml" "$KOPIA/plugins/DeluxeMenus/gui_menus/"
mkdir -p "$KOPIA/nminimap" "$KOPIA/plugins/MMOItems/item"; cp -a "$P/NMinimap/config.yml" "$KOPIA/nminimap/"; cp -a "$P/MMOItems/item/miscellaneous.yml" "$KOPIA/plugins/MMOItems/item/"; cp -a "$P/NMinimap/markers" "$P/NMinimap/markers-font-cache.json" "$P/Nexo/pack/external_packs/NMinimap-zasoby.zip" "$KOPIA/nminimap/" 2>/dev/null || true
[[ -d "$P/Mapa" ]] && cp -a "$P/Mapa" "$KOPIA/plugins/Mapa"; cp -a "$P"/mapa-*.jar "$KOPIA/" 2>/dev/null || true
cp -a "$P"/MapEngine-*.jar "$KOPIA/" 2>/dev/null || true; [[ -d "$P/Nexo/pack/assets/mapa" ]] && { mkdir -p "$KOPIA/pack"; cp -a "$P/Nexo/pack/assets/mapa" "$KOPIA/pack/"; }
cat > "$KOPIA/cofnij.sh" <<COF
#!/usr/bin/env bash
# Cofniecie mapy do stanu sprzed tego wdrozenia (kod, MapEngine, grafiki packa, configi). Znaczniki graczy zostaja.
# Kazdy plik przywracany niezaleznie (brak jednego nie przerywa reszty); jary zdejmowane tylko gdy kopia je ma. Restart po.
set -uo pipefail; K="\$(cd "\$(dirname "\$0")" && pwd)"; BLEDY=0
p() { if [[ -e "\$1" ]]; then cp -a "\$1" "\$2" || { echo "BLAD kopiowania \$1"; BLEDY=1; }; else echo "brak w kopii: \$1 (pomijam)"; fi; }
p "\$K/plugins/CoreTools/Scripts/stale-sloty.yml" "$P/CoreTools/Scripts/"
p "\$K/plugins/DeluxeMenus/gui_menus/hub_przygoda.yml" "$P/DeluxeMenus/gui_menus/"
p "\$K/plugins/MMOItems/item/miscellaneous.yml" "$P/MMOItems/item/"
p "\$K/nminimap/markers/." "$P/NMinimap/markers/"; p "\$K/nminimap/markers-font-cache.json" "$P/NMinimap/"
p "\$K/nminimap/NMinimap-zasoby.zip" "$P/Nexo/pack/external_packs/"; p "\$K/nminimap/config.yml" "$P/NMinimap/"
if ls "\$K"/mapa-*.jar >/dev/null 2>&1; then rm -f $P/mapa-*.jar; p "\$(ls "\$K"/mapa-*.jar | head -1)" "$P/"; fi
if ls "\$K"/MapEngine-*.jar >/dev/null 2>&1; then rm -f $P/MapEngine-*.jar; p "\$(ls "\$K"/MapEngine-*.jar | head -1)" "$P/"; fi
[[ -d "\$K/pack/mapa" ]] && { rm -rf "$P/Nexo/pack/assets/mapa"; p "\$K/pack/mapa" "$P/Nexo/pack/assets/"; }
# dane graczy (wlasne znaczniki gracze/, rejestr osobistych map) zostaja AKTUALNE - cofamy kod i pliki, nie postep graczy
if [[ -d "\$K/plugins/Mapa" ]]; then
  Z="\$(mktemp -d)"; cp -a "$P/Mapa/gracze" "$P/Mapa/mapy-graczy.yml" "\$Z/" 2>/dev/null
  rm -rf "$P/Mapa"; p "\$K/plugins/Mapa" "$P/Mapa"
  [[ -d "\$Z/gracze" ]] && { rm -rf "$P/Mapa/gracze"; cp -a "\$Z/gracze" "$P/Mapa/"; }
  [[ -f "\$Z/mapy-graczy.yml" ]] && cp -a "\$Z/mapy-graczy.yml" "$P/Mapa/"; rm -rf "\$Z"
fi
[[ \$BLEDY == 0 ]] && echo "pliki przywrocone; teraz restart: docker restart $KONT" || echo "UWAGA: byly bledy kopiowania - sprawdz powyzej"
COF
chmod +x "$KOPIA/cofnij.sh"; echo "== kopia: $KOPIA"

# 3. pliki
rm -f "$P"/mapa-*.jar; cp "$JAR" "$P/"
[[ -f "$P/MapEngine-1.8.12.jar" ]] || cp "$tu/pliki-pelna/MapEngine-1.8.12.jar" "$P/"
mkdir -p "$P/Mapa"; rm -rf "$P/Mapa/swiaty"; cp -r "$ST/out/plugins/Mapa/swiaty" "$ST/out/plugins/Mapa/kafle.json" "$ST/out/plugins/Mapa/tekstury.bin" "$P/Mapa/"
rm -rf "$P/Mapa/ikony"; cp -r "$ST/out/plugins/Mapa/ikony" "$P/Mapa/"
rm -f "$P/Mapa/config.yml"   # config z jara (zakladki, zoom z blokow); poprzedni w kopii
rm -rf "$P/Nexo/pack/assets/mapa"; cp -r "$ST/out/pack/assets/mapa" "$P/Nexo/pack/assets/"
python3 "$tu/latka2.py" "$P"
python3 "$tu/latka3.py" "$P"
python3 "$tu/latka4.py" "$P"   # opis UI_MAPA (nowe sterowanie)
python3 "$tu/latka5.py" "$P"   # NMinimap: wydajnosc + prog RAM
python3 "$tu/latka6.py" "$P"   # NMinimap: wysoki numer mapy (konflikt z mapa gracza 0)
# ikony Mapy na minimapie: markers + mapowanie znakow + pack zasobow z TEGO SAMEGO przebiegu (klon), inaczej zle ikony
cp "$tu"/pliki-minimapa/markers/*.png "$P/NMinimap/markers/"
cp "$tu/pliki-minimapa/markers-font-cache.json" "$P/NMinimap/"
cp "$tu/pliki-minimapa/NMinimap-zasoby.zip" "$P/Nexo/pack/external_packs/"

# 4. restart (Nexo buduje pack przy starcie)
LOG="$DANE/logs/latest.log"; I0="$(stat -c %i "$LOG")"
echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal w 10 min"; exit 6; }
echo "== Mapa: $(grep -E '\[Mapa\] (Enabling|.*WARN|.*Brak)' "$LOG" | tail -3)"
grep -E "\[Mapa\].*" "$LOG" | grep -iE "warn|error|brak|niepoprawn" || echo "== Mapa bez ostrzezen"
grep -E "Enabling MapEngine|Color palette is valid" "$LOG" | sed 's/^/== /' || echo "== UWAGA: brak MapEngine w logu"
for i in $(seq 1 30); do grep -q "Finished generating resourcepack" "$LOG" && break; sleep 2; done; sleep 5   # zip dopisywany chwile po komunikacie
unzip -l "$P/Nexo/pack/pack.zip" | grep -c "assets/mapa/items/" | sed 's/^/== kafelki w pack.zip: /'
echo "wdrozone; cofniecie: $KOPIA/cofnij.sh"
