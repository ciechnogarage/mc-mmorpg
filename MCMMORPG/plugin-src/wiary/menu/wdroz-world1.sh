#!/usr/bin/env bash
# Wdrozenie Wiar na world1: kopia -> stop (tylko przy 0 graczy) -> instaluj.py -> start -> kontrola logu.
# Cofniecie: backups/wiary-<czas>/cofnij.sh (tworzony tutaj).
# Nowy plugin + nowy przedmiot Nexo = pelny restart; pliki podmieniane przy ZATRZYMANYM serwerze.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
W="$here/.."
DANE=/opt/docker/mc/data/world1
KONT=mc-world1-1
JAR=$(ls "$W"/build/libs/wiary-*.jar | head -1)
[ -f "$JAR" ] || { echo "brak jara — ./build.sh"; exit 2; }

echo "== gracze"
lista=$(docker exec "$KONT" rcon-cli 'minecraft:list uuids' || true)
echo "$lista"
echo "$lista" | grep -q "There are 0 of" || { echo "PRZERWANE: gracze online albo brak odpowiedzi (pusty wynik to NIE zero)"; exit 3; }

KOPIA="$DANE/backups/wiary-$(date +%Y%m%d-%H%M)"
echo "== kopia -> $KOPIA"
for f in plugins/DeluxeMenus/config.yml plugins/DeluxeMenus/gui_menus/hub_postac.yml plugins/Nexo/items/hub_icons.yml; do
  mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"
done
cat > "$KOPIA/NOWE.txt" <<EOF
plugins/$(basename "$JAR")
plugins/DeluxeMenus/gui_menus/wiara.yml
plugins/Nexo/pack/assets/nexo/textures/item/hub/wiara.png
docs/wiary/panteon.md
EOF
cat > "$KOPIA/cofnij.sh" <<'EOF'
#!/usr/bin/env bash
# Cofniecie Wiar: tylko przy 0 graczy. Dane postaci (plugins/Wiary/profile) ZOSTAJA — plugin znika, pliki nie.
set -euo pipefail
kopia="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE=/opt/docker/mc/data/world1; KONT=mc-world1-1
docker exec "$KONT" rcon-cli 'minecraft:list uuids' | grep -q "There are 0 of" || { echo "PRZERWANE: gracze online"; exit 3; }
docker stop "$KONT" >/dev/null
rm -f "$DANE"/plugins/wiary-*.jar
while read -r f; do
  if [ "${f#plugins/wiary-}" = "$f" ]; then rm -f "${DANE:?}/$f"; echo "usuniete: $f"; fi
done < "$kopia/NOWE.txt"
(cd "$kopia" && find plugins -type f) | while read -r f; do cp -a "$kopia/$f" "$DANE/$f"; echo "przywrocone: $f"; done
docker start "$KONT" >/dev/null; echo "start zlecony — world1 bez Wiar"
EOF
chmod +x "$KOPIA/cofnij.sh"

I0=$(stat -c %i "$DANE/logs/latest.log")
echo "== stop"; docker stop "$KONT" >/dev/null
echo "== instalacja"
python3 "$here/instaluj.py" "$DANE" "$JAR" "$here/wiara.yml" "$here/wiara.png"
mkdir -p "$DANE/docs/wiary"; cp "$W/PANTEON.md" "$DANE/docs/wiary/panteon.md"
echo "== start"; docker start "$KONT" >/dev/null
for i in $(seq 1 240); do
  I1=$(stat -c %i "$DANE/logs/latest.log" 2>/dev/null || echo "$I0")
  [ "$I1" != "$I0" ] && grep -q 'Done (' "$DANE/logs/latest.log" && break
  sleep 3
done
grep -q 'Done (' "$DANE/logs/latest.log" && [ "$(stat -c %i "$DANE/logs/latest.log")" != "$I0" ] || { echo "SERWER NIE WSTAL w 12 min — sprawdz log; cofniecie: $KOPIA/cofnij.sh"; exit 4; }
echo "== log"
grep -E "\[Wiary\]|wiary" "$DANE/logs/latest.log" | grep -vi remap || true
echo "kopia: $KOPIA"
