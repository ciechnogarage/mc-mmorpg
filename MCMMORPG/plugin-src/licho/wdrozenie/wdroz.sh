#!/usr/bin/env bash
# Wdrozenie „Licho nie spi” na world1 (Claude, wzor: wyscigi/wdrozenie/wdroz.sh). TYLKO za zgoda wlasciciela.
#   wdroz.sh          -> kontrole + kopia swiata + restart + (swiat 'licho' jesli brak) + budowa zagrody
#   wdroz.sh --sucho  -> tylko kontrole, nic nie zmienia
# Restart world1 tylko przy 0 graczy (WYMUS=1 wymusza). Cofniecie: backups/licho-<czas>/cofnij.sh
# Cudze pliki zmieniamy W MIEJSCU (latka.py); tlo Druzyny (hub_spolecznosc_*.png) podmieniamy tylko, gdy ma md5 z generowania.
set -euo pipefail
TU="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; W="$(dirname "$TU")"
DANE=/opt/docker/mc/data/world1; P=$DANE/plugins; KONT=mc-world1-1; GUI=$P/Nexo/pack/assets/nexo/textures/gui
R() { docker exec -i "$KONT" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
[[ "$(ls "$W"/build/libs/licho-*.jar 2>/dev/null | wc -l)" == 1 ]] || { echo "w build/libs ma byc dokladnie jeden jar (./build.sh)"; exit 1; }
JAR=$(ls "$W"/build/libs/licho-*.jar)
EDYTOWANE=(DeluxeMenus/config.yml DeluxeMenus/gui_menus/hub_spolecznosc.yml KsiegaPostaci/config.yml MenuKey/config.yml Chowance/config.yml)

echo "== kontrole"
df -h / | tail -1; free -g | sed -n 2p
[[ $(df -BG --output=avail / | tail -1 | tr -dc 0-9) -ge 10 ]] || { echo "PRZERWANE: < 10 GB dysku (kopia swiata)"; exit 3; }
for f in "${EDYTOWANE[@]}"; do [[ -f $P/$f ]] || { echo "PRZERWANE: brak $f"; exit 4; }; done
(cd "$P" && md5sum -c --quiet "$TU/menu/baza-hub-md5.txt") || { echo "PRZERWANE: tlo hub_spolecznosc zmienione od generowania - uruchom narzedzia/ksiega/rysuj_menu.py wdrozenie/menu"; exit 4; }
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
for f in "${EDYTOWANE[@]}"; do mkdir -p "$T/$(dirname $f)"; cp -a "$P/$f" "$T/$f"; done
python3 "$TU/latka.py" "$T" "$TU" >/dev/null || { echo "PRZERWANE: latka nie przechodzi na obecnych plikach"; exit 4; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online; WYMUS=1 wymusza"; exit 3; }
[[ "${1:-}" == --sucho ]] && { echo "== --sucho: kontrole OK, nic nie zmieniono"; exit 0; }

echo "== kopia swiata"; /opt/docker/mc/kopie/kopia-swiata.sh
STAMP=$(date +%Y%m%d-%H%M%S); B=$DANE/backups/licho-$STAMP; mkdir -p "$B/stare" "$B/stare-jar"
cat > "$B/cofnij.sh" <<EOF
#!/usr/bin/env bash
# Cofa wdrozenie Licha ($STAMP): poprzedni jar (jesli byl), stare menu/glify/tlo Druzyny, cudze pliki sprzed zmian.
# Swiat 'licho' zostaje (pusty VoidGen + zagroda); usuniecie: 'mv delete licho' + 'mv confirm' recznie.
set -euo pipefail
N=\$(docker exec $KONT rcon-cli 'minecraft:list uuids' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+' || true)
[ "\$N" = 0 ] || [ "\${1:-}" = --sila ] || { echo "graczy: \$N (--sila)"; exit 1; }
docker stop -t 120 $KONT
rm -f $P/licho-*.jar $P/DeluxeMenus/gui_menus/licho*.yml $P/Nexo/glyphs/licho.yml $GUI/licho*_[1-4].png
cp -a $B/stare-jar/. $P/ 2>/dev/null || true
cp -a $B/stare/. $P/
docker start $KONT; echo "== cofnieto licho-$STAMP"
EOF
chmod +x "$B/cofnij.sh"

echo "== zatrzymanie world1"; docker stop -t 120 "$KONT" >/dev/null
# kopie TUZ przed zmiana (serwer stoi, nikt ich teraz nie zapisze)
for f in "${EDYTOWANE[@]}" Licho/config.yml; do
  if [[ -f $P/$f ]]; then mkdir -p "$B/stare/$(dirname $f)"; cp -a "$P/$f" "$B/stare/$f"; fi
done
for f in $P/DeluxeMenus/gui_menus/licho*.yml $GUI/hub_spolecznosc_[1-4].png $GUI/licho*_[1-4].png $P/Nexo/glyphs/licho.yml; do
  if [[ -f $f ]]; then r=${f#$P/}; mkdir -p "$B/stare/$(dirname $r)"; cp -a "$f" "$B/stare/$r"; fi
done
cp -a $P/licho-*.jar "$B/stare-jar/" 2>/dev/null || true
rm -f $P/licho-*.jar; cp -a "$JAR" "$P/"
rm -f $P/DeluxeMenus/gui_menus/licho*.yml; cp -a "$TU/menu/gui_menus/." "$P/DeluxeMenus/gui_menus/"
cp -a "$TU/menu/gui/." "$GUI/"
cp -a "$TU/menu/glify_licho.yml" "$P/Nexo/glyphs/licho.yml"
mkdir -p "$P/Licho"; cp -a "$W/src/main/resources/config.yml" "$P/Licho/config.yml"
grep -q '^debug: false' "$P/Licho/config.yml" || echo "UWAGA: debug w Licho/config.yml nie jest false"
python3 "$TU/latka.py" "$P" "$TU"

echo "== start world1"; L=$DANE/logs/latest.log; I=$(stat -c %i "$L"); docker start "$KONT" >/dev/null
for i in $(seq 1 120); do [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I" ]] && grep -q 'Done (' "$L" && break; sleep 5; done
grep -a -E 'Enabling Licho|\[Licho\]|Done \(' "$L" | cut -c1-140 | head -6

echo "== swiat i zagroda"
if ! R 'mv list' | grep -q 'licho - NORMAL'; then
  grep -v '^#' "$TU/swiat/ustaw.txt" | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
fi
R 'licho zbuduj'
R 'minecraft:execute in minecraft:licho run minecraft:time set 12800' >/dev/null
R 'nexo reload all' | tail -1
echo "== kontrola"
R 'mv list' | grep -i licho
R 'licho stan'
grep -a -E 'ERROR|SEVERE' "$L" | grep -iE 'licho|deluxe|ksiega|menukey|nexo' | grep -v 'No key layers' | head -5 || true
echo "== gotowe; cofniecie: $B/cofnij.sh"
