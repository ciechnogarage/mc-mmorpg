#!/usr/bin/env bash
# Wdrozenie Wyscigow Chowancow na world1 (Claude). v1 20.10 14:09, v2 20.10 (A/D, bonusy, pauza Q, menu Ksiegi).
#   wdroz.sh          -> kontrole + kopia swiata + restart + (swiat 'wyscigi' jesli brak) + budowa toru
#   wdroz.sh --sucho  -> tylko kontrole, nic nie zmienia
# Restart world1 tylko przy 0 graczy (WYMUS=1 wymusza). Cofniecie: backups/wyscigi-<czas>/cofnij.sh
# Cudze pliki zmieniamy W MIEJSCU (latka.py); tlo Druzyny (hub_spolecznosc_*.png) podmieniamy tylko, gdy ma md5 z generowania.
set -euo pipefail
TU="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; W="$(dirname "$TU")"
DANE=/opt/docker/mc/data/world1; P=$DANE/plugins; KONT=mc-world1-1; GUI=$P/Nexo/pack/assets/nexo/textures/gui
R() { docker exec -i "$KONT" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
JAR=$(ls "$W"/build/libs/wyscigi-*.jar 2>/dev/null | head -1); [[ -f "$JAR" ]] || { echo "brak jaru (./build.sh)"; exit 1; }
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
STAMP=$(date +%Y%m%d-%H%M%S); B=$DANE/backups/wyscigi-$STAMP; mkdir -p "$B/stare" "$B/stare-jar"
cat > "$B/cofnij.sh" <<EOF
#!/usr/bin/env bash
# Cofa wdrozenie Wyscigow ($STAMP): poprzedni jar (jesli byl), stare menu/glify/tlo Druzyny, cudze pliki sprzed zmian.
set -euo pipefail
N=\$(docker exec $KONT rcon-cli 'minecraft:list uuids' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+' || true)
[ "\$N" = 0 ] || [ "\${1:-}" = --sila ] || { echo "graczy: \$N (--sila)"; exit 1; }
docker stop -t 120 $KONT
rm -f $P/wyscigi-*.jar $P/DeluxeMenus/gui_menus/wyscigi*.yml $P/Nexo/glyphs/wyscigi.yml $GUI/wyscigi*_[1-4].png
cp -a $B/stare-jar/. $P/ 2>/dev/null || true
cp -a $B/stare/. $P/
docker start $KONT; echo "== cofnieto wyscigi-$STAMP"
EOF
chmod +x "$B/cofnij.sh"

echo "== zatrzymanie world1"; docker stop -t 120 "$KONT" >/dev/null
# kopie TUZ przed zmiana (serwer stoi, nikt ich teraz nie zapisze)
for f in "${EDYTOWANE[@]}" Wyscigi/config.yml; do
  if [[ -f $P/$f ]]; then mkdir -p "$B/stare/$(dirname $f)"; cp -a "$P/$f" "$B/stare/$f"; fi
done
for f in $P/DeluxeMenus/gui_menus/wyscigi*.yml $GUI/hub_spolecznosc_[1-4].png; do
  if [[ -f $f ]]; then r=${f#$P/}; mkdir -p "$B/stare/$(dirname $r)"; cp -a "$f" "$B/stare/$r"; fi
done
cp -a $P/wyscigi-*.jar "$B/stare-jar/" 2>/dev/null || true
rm -f $P/wyscigi-*.jar; cp -a "$JAR" "$P/"
rm -f $P/DeluxeMenus/gui_menus/wyscigi*.yml; cp -a "$TU/menu/gui_menus/." "$P/DeluxeMenus/gui_menus/"
cp -a "$TU/menu/gui/." "$GUI/"
cp -a "$TU/menu/glify_wyscigi.yml" "$P/Nexo/glyphs/wyscigi.yml"
mkdir -p "$P/Wyscigi"; cp -a "$W/src/main/resources/config.yml" "$P/Wyscigi/config.yml"
grep -q '^debug: false' "$P/Wyscigi/config.yml" || echo "UWAGA: debug w Wyscigi/config.yml nie jest false"
python3 "$TU/latka.py" "$P" "$TU"

echo "== start world1"; L=$DANE/logs/latest.log; I=$(stat -c %i "$L"); docker start "$KONT" >/dev/null
for i in $(seq 1 120); do [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I" ]] && grep -q 'Done (' "$L" && break; sleep 5; done
grep -a -E 'Enabling Wyscigi|Tor Sadowy|Done \(' "$L" | cut -c1-140

echo "== swiat i tor"
if ! R 'mv list' | grep -q 'wyscigi - NORMAL'; then
  grep -v '^#' "$TU/swiat/ustaw.txt" | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
fi
R 'wyscigi zbuduj'
R 'minecraft:execute in minecraft:wyscigi run minecraft:time set 6000' >/dev/null
R 'nexo reload all' | tail -1
echo "== kontrola"
R 'mv list' | grep -i wyscigi
R 'wyscigi stan'
R 'minecraft:execute in minecraft:wyscigi run minecraft:forceload query'
grep -a -E 'ERROR|SEVERE' "$L" | grep -iE 'wyscig|deluxe|ksiega|menukey|nexo' | head -5 || true
echo "== gotowe; cofniecie: $B/cofnij.sh"
