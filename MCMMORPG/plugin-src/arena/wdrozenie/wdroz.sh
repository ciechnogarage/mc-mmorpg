#!/usr/bin/env bash
# Wdrozenie Otwartej Areny na world1 (Claude, 17.10). URUCHAMIAC TYLKO PO ZGODZIE WLASCICIELA.
#   wdroz.sh            -> kontrole + kopia + restart + budowa areny
#   wdroz.sh --sucho    -> tylko kontrole (dysk, gracze, dryf plikow), nic nie zmienia
# Restart world1 tylko przy 0 graczy (WYMUS=1 wymusza). Cofniecie: backups/arena-<czas>/cofnij.sh
set -euo pipefail
TU="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; ARENA="$(dirname "$TU")"
DANE=/opt/docker/mc/data/world1; P=$DANE/plugins; KONT=mc-world1-1
R() { docker exec -i "$KONT" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
JAR=$(ls "$ARENA"/build/libs/arena-*.jar 2>/dev/null | head -1); [[ -f "$JAR" ]] || { echo "brak jaru (zbuduj)"; exit 1; }
PNG=$P/Nexo/pack/assets/nexo/textures/gui

echo "== kontrole"
df -h / | tail -1; free -g | sed -n 2p
[[ $(df -BG --output=avail / | tail -1 | tr -dc 0-9) -ge 6 ]] || { echo "PRZERWANE: < 6 GB dysku"; exit 3; }
# dryf: pliki, ktore nadpisujemy, musza byc takie jak w chwili przygotowania paczki (inna sesja mogla je zmienic)
DRYF=0
while read -r sum plik; do
  teraz=$(md5sum "$P/${plik#./}" 2>/dev/null | cut -d' ' -f1)
  [[ "$teraz" == "$sum" ]] || { echo "DRYF: $plik (paczka $sum, serwer ${teraz:-brak})"; DRYF=1; }
done < "$TU/baza/md5.txt"
[[ $DRYF == 0 ]] || { echo "PRZERWANE: pliki na serwerze zmienione od przygotowania paczki - scal zmiany i odswiez baza/"; exit 4; }
[[ ! -e $P/WorldGuard/worlds/arena/regions.yml && -z "$(ls -d $DANE/arena 2>/dev/null)" ]] || { echo "PRZERWANE: swiat/regiony arena juz istnieja"; exit 4; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online; WYMUS=1 wymusza"; exit 3; }
[[ "${1:-}" == --sucho ]] && { echo "== --sucho: kontrole OK, nic nie zmieniono"; exit 0; }

echo "== kopia swiata"; /opt/docker/mc/kopie/kopia-swiata.sh
STAMP=$(date +%Y%m%d-%H%M%S); B=$DANE/backups/arena-$STAMP; mkdir -p "$B/stare"
(cd "$TU/baza" && find . -type f ! -name md5.txt) | while read -r f; do mkdir -p "$B/stare/$(dirname "$f")"; cp -a "$P/$f" "$B/stare/$f"; done
mkdir -p "$B/stare/MythicLib" "$B/stare/Nexo/pack/assets/nexo/textures/gui"
mkdir -p "$B/stare/Zaklecia"; cp -a "$P/Zaklecia/config.yml" "$B/stare/Zaklecia/"; cp -a "$P/MythicLib/config.yml" "$B/stare/MythicLib/"; cp -a "$PNG"/hub_spolecznosc_{1,2,3,4}.png "$B/stare/Nexo/pack/assets/nexo/textures/gui/"
cat > "$B/cofnij.sh" <<EOF
#!/usr/bin/env bash
# Cofa Otwarta Arene ($STAMP): pliki sprzed wdrozenia, bez jaru areny i moba manekina; flagi WG pvp w swiatach glownych
# zostaja (deny i tak bylo stanem faktycznym - PvP nie dzialalo nigdzie). Swiat arena zostaje na dysku, niezaladowany.
set -euo pipefail
N=\$(docker exec $KONT rcon-cli 'minecraft:list uuids' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+' || true)
[ "\$N" = 0 ] || [ "\${1:-}" = --sila ] || { echo "graczy: \$N (--sila)"; exit 1; }
docker exec $KONT rcon-cli 'mv unload arena' || true
docker stop -t 120 $KONT
rm -f $P/arena-*.jar $P/MythicMobs/mobs/Arena.yml
cp -a $B/stare/. $P/
docker start $KONT; echo "== cofnieto arena-$STAMP"
EOF
chmod +x "$B/cofnij.sh"

echo "== zatrzymanie world1"; docker stop -t 120 "$KONT" >/dev/null
# ponowna kontrola dryfu TUZ przed kopiowaniem (lekcja 13.10)
while read -r sum plik; do [[ $(md5sum "$P/${plik#./}" | cut -d' ' -f1) == "$sum" ]] || { echo "DRYF w ostatniej chwili: $plik - start bez zmian"; docker start "$KONT"; exit 4; }; done < "$TU/baza/md5.txt"
cp -a "$JAR" "$P/"
cp -a "$TU/pliki/DeluxeMenus/gui_menus/." "$P/DeluxeMenus/gui_menus/"
cp -a "$TU/pliki/MythicMobs/mobs/Arena.yml" "$P/MythicMobs/mobs/"
cp -a "$TU/pliki/CoreTools/Scripts/kara-smierci.yml" "$P/CoreTools/Scripts/"
cp -a "$TU/pack/gui"/hub_spolecznosc_{1,2,3,4}.png "$PNG/"
mkdir -p "$P/WorldGuard/worlds/arena"; cp -a "$TU/swiat/regions.yml" "$P/WorldGuard/worlds/arena/regions.yml"
# Q na gracza: tylko klucz klawisz-q.zwykle w Zaklecia (inne klucze bez zmian; brak trafienia = stare menu)
sed -i "s#^  zwykle: 'dm open hub_glowne %gracz%'#  zwykle: 'menugracza q %gracz%'#" "$P/Zaklecia/config.yml"
grep -q "zwykle: 'menugracza q %gracz%'" "$P/Zaklecia/config.yml" || echo "UWAGA: klawisz-q w Zaklecia nie zmieniony (inny format) - Q dziala jak dotad"
python3 - "$P/MythicLib/config.yml" <<'PY'
from pathlib import Path
import sys
path = Path(sys.argv[1]); lines = path.read_text().splitlines(keepends=True)
pvp = next(i for i, l in enumerate(lines) if l.strip() == 'pvp_on:')
off = next(i for i in range(pvp + 1, len(lines)) if lines[i].strip() == 'offense:')
ind = len(lines[off]) - len(lines[off].lstrip())
end = next((i for i in range(off + 1, len(lines)) if lines[i].strip() and not lines[i].lstrip().startswith('#')
            and len(lines[i]) - len(lines[i].lstrip()) <= ind), len(lines))
for key in ('party_member', 'guild_ally', 'party_other', 'guild_enemy'):  # PvP tylko w ringu (WG+plugin), wiec przyjaciele tez moga sie bic
    e = next((i for i in range(off + 1, end) if lines[i].lstrip().startswith(key + ':')), None)
    if e is None: lines.insert(end, ' ' * (ind + 2) + key + ': true\n'); end += 1
    else: lines[e] = ' ' * (ind + 2) + key + ': true\n'
path.write_text(''.join(lines))
PY
sed -n '/pvp_on:/,/builtin_mana/p' "$P/MythicLib/config.yml" | sed -n '/offense:/,$p' | grep -E 'party|guild'

echo "== start world1"; L=$DANE/logs/latest.log; I=$(stat -c %i "$L"); docker start "$KONT" >/dev/null
for i in $(seq 1 120); do [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I" ]] && grep -q 'Done (' "$L" && break; sleep 5; done
grep -a -E 'Enabling Arena|Done \(' "$L" | cut -c1-120

echo "== swiat areny"
grep -v '^#' "$TU/swiat/ustaw.txt" | grep -v '^$' | grep -v '^mv setspawn ' | R | grep -c -i -E 'success|created|set' || true
grep -v '^#' "$TU/swiat/budowa.txt" | grep -v '^$' | R | grep -c -E 'Changed|filled' || true
grep '^mv setspawn ' "$TU/swiat/ustaw.txt" | R
grep -v '^#' "$TU/swiat/wg-pvp-deny.txt" | grep -v '^$' | R
R 'minecraft:execute in minecraft:arena run minecraft:time set 6000' >/dev/null
echo "== kontrola"; R 'rg info arena_trening -w arena' | grep -i flags; R 'rg info arena_ring -w arena' | grep -i flags
R 'minecraft:execute in minecraft:arena run minecraft:execute if entity @e[type=minecraft:iron_golem]'
R 'nexo reload all' | tail -1
echo "== gotowe; cofniecie: $B/cofnij.sh"
