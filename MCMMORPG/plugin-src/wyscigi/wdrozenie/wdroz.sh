#!/usr/bin/env bash
# Wdrozenie Wyscigow Chowancow na world1 (Claude, 20.10, po zgodzie wlasciciela "wdróż").
#   wdroz.sh          -> kontrole + kopia swiata + restart + swiat 'wyscigi' + budowa toru
#   wdroz.sh --sucho  -> tylko kontrole, nic nie zmienia
# Restart world1 tylko przy 0 graczy (WYMUS=1 wymusza). Cofniecie: backups/wyscigi-<czas>/cofnij.sh
# Cudze pliki zmieniamy W MIEJSCU (latka.py), nie nadpisujemy - inne galezie moga je zmieniac.
set -euo pipefail
TU="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; W="$(dirname "$TU")"
DANE=/opt/docker/mc/data/world1; P=$DANE/plugins; KONT=mc-world1-1
R() { docker exec -i "$KONT" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
JAR=$(ls "$W"/build/libs/wyscigi-*.jar 2>/dev/null | head -1); [[ -f "$JAR" ]] || { echo "brak jaru (./build.sh)"; exit 1; }
EDYTOWANE=(DeluxeMenus/config.yml DeluxeMenus/gui_menus/hub_spolecznosc.yml Chowance/config.yml)

echo "== kontrole"
df -h / | tail -1; free -g | sed -n 2p
[[ $(df -BG --output=avail / | tail -1 | tr -dc 0-9) -ge 10 ]] || { echo "PRZERWANE: < 10 GB dysku (kopia swiata)"; exit 3; }
for f in "${EDYTOWANE[@]}"; do [[ -f $P/$f ]] || { echo "PRZERWANE: brak $f"; exit 4; }; done
ls $P/wyscigi-*.jar >/dev/null 2>&1 && { echo "PRZERWANE: jar wyscigi juz jest na world1"; exit 4; }
[[ -d $DANE/wyscigi ]] && { echo "PRZERWANE: swiat wyscigi juz istnieje"; exit 4; }
# proba latki na kopii (asercje: jedno gui_menus, wolne sloty kafla, swiaty_zabronione)
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
for f in "${EDYTOWANE[@]}"; do mkdir -p "$T/$(dirname $f)"; cp -a "$P/$f" "$T/$f"; done
python3 "$TU/latka.py" "$T" "$TU/hub-kafel.yml" >/dev/null || { echo "PRZERWANE: latka nie przechodzi na obecnych plikach"; exit 4; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online; WYMUS=1 wymusza"; exit 3; }
[[ "${1:-}" == --sucho ]] && { echo "== --sucho: kontrole OK, nic nie zmieniono"; exit 0; }

echo "== kopia swiata"; /opt/docker/mc/kopie/kopia-swiata.sh
STAMP=$(date +%Y%m%d-%H%M%S); B=$DANE/backups/wyscigi-$STAMP; mkdir -p "$B/stare"
cat > "$B/cofnij.sh" <<EOF
#!/usr/bin/env bash
# Cofa Wyscigi Chowancow ($STAMP): bez jaru i menu, 3 cudze pliki ze stanu sprzed wdrozenia.
# Swiat 'wyscigi' i dane pluginu (rekordy) zostaja na dysku; swiat znika z Multiverse.
set -euo pipefail
N=\$(docker exec $KONT rcon-cli 'minecraft:list uuids' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+' || true)
[ "\$N" = 0 ] || [ "\${1:-}" = --sila ] || { echo "graczy: \$N (--sila)"; exit 1; }
docker exec $KONT rcon-cli 'mv remove wyscigi' || true
docker stop -t 120 $KONT
rm -f $P/wyscigi-*.jar $P/DeluxeMenus/gui_menus/wyscigi.yml $P/DeluxeMenus/gui_menus/wyscigi_czasowka.yml $P/DeluxeMenus/gui_menus/wyscigi_wyscig.yml
cp -a $B/stare/. $P/
docker start $KONT; echo "== cofnieto wyscigi-$STAMP"
EOF
chmod +x "$B/cofnij.sh"

echo "== zatrzymanie world1"; docker stop -t 120 "$KONT" >/dev/null
# kopie cudzych plikow TUZ przed zmiana (serwer stoi, nikt ich teraz nie zapisze)
for f in "${EDYTOWANE[@]}"; do mkdir -p "$B/stare/$(dirname $f)"; cp -a "$P/$f" "$B/stare/$f"; done
cp -a "$JAR" "$P/"
cp -a "$TU/pliki/DeluxeMenus/gui_menus/." "$P/DeluxeMenus/gui_menus/"
mkdir -p "$P/Wyscigi"; [[ -f "$P/Wyscigi/config.yml" ]] || cp -a "$W/src/main/resources/config.yml" "$P/Wyscigi/config.yml"
grep -q '^debug: false' "$P/Wyscigi/config.yml" || echo "UWAGA: debug w Wyscigi/config.yml nie jest false"
python3 "$TU/latka.py" "$P" "$TU/hub-kafel.yml"

echo "== start world1"; L=$DANE/logs/latest.log; I=$(stat -c %i "$L"); docker start "$KONT" >/dev/null
for i in $(seq 1 120); do [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I" ]] && grep -q 'Done (' "$L" && break; sleep 5; done
grep -a -E 'Enabling Wyscigi|Tor Sadowy|Done \(' "$L" | cut -c1-140

echo "== swiat wyscigi"
grep -v '^#' "$TU/swiat/ustaw.txt" | grep -v '^$' | while read -r c; do R "$c" </dev/null | tail -1; done
R 'wyscigi zbuduj'
R 'minecraft:execute in minecraft:wyscigi run minecraft:time set 6000' >/dev/null
echo "== kontrola"
R 'mv list' | grep -i wyscigi
R 'mv gamerule list wyscigi' | grep -ciE 'keep_inventory: true|fall_damage: false' || true
R 'wyscigi stan'
grep -a -iE 'error|exception' "$L" | grep -iE 'wyscig' | head -5 || true
echo "== gotowe; cofniecie: $B/cofnij.sh"
