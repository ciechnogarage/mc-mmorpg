#!/usr/bin/env bash
# Wdrozenie v2 (oburacz + Ksiega zaklec) na world1. Wymaga RESTARTU (nowy jar pluginu Zaklecia), wiec
# rusza tylko przy 0 graczy. Kopia i cofniecie: backups/zaklecia-v2-<data>/cofnij-v2.sh
# Uzycie: wdroz.sh [katalog_danych=/opt/docker/mc/data/world1] [kontener=mc-world1-1]
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DANE="${1:-/opt/docker/mc/data/world1}"; KONT="${2:-mc-world1-1}"
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: na serwerze sa gracze (restart by ich wyrzucil); WYMUS=1 wymusza"; exit 3; }
JAR="$(ls "$tu"/zaklecia-*.jar | tail -1)"

KOPIA="$DANE/backups/zaklecia-v2-$(date +%Y%m%d-%H%M)"; mkdir -p "$KOPIA"
for f in plugins/MythicMobs/items/Zaklecia.yml plugins/MythicMobs/skills/Zaklecia.yml plugins/MenuKey/config.yml \
         plugins/DeluxeMenus/config.yml plugins/DeluxeMenus/gui_menus/hub_umiejetnosci.yml; do
  mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"
done
mkdir -p "$KOPIA/plugins"; cp -a "$DANE"/plugins/zaklecia-*.jar "$KOPIA/plugins/" 2>/dev/null || true
mkdir -p "$KOPIA/plugins/Nexo/pack/assets"; cp -a "$DANE/plugins/Nexo/pack/assets/zaklecia" "$KOPIA/plugins/Nexo/pack/assets/"
echo "plugins/DeluxeMenus/gui_menus/ksiega_zaklec.yml" > "$KOPIA/NOWE.txt"
[[ -f "$DANE/plugins/DeluxeMenus/gui_menus/ulubione_zaklec.yml" ]] || echo "plugins/DeluxeMenus/gui_menus/ulubione_zaklec.yml" >> "$KOPIA/NOWE.txt"
if [[ -f "$DANE/plugins/Zaklecia/config.yml" ]]; then mkdir -p "$KOPIA/plugins/Zaklecia"; cp -a "$DANE/plugins/Zaklecia/config.yml" "$KOPIA/plugins/Zaklecia/"; else echo "plugins/Zaklecia/config.yml" >> "$KOPIA/NOWE.txt"; fi
cp "$tu/cofnij-v2.sh" "$KOPIA/"; echo "== kopia: $KOPIA"

rm -f "$DANE"/plugins/zaklecia-*.jar; cp "$JAR" "$DANE/plugins/"
cp "$tu/src/MythicMobs/items/Zaklecia.yml" "$DANE/plugins/MythicMobs/items/Zaklecia.yml"
cp "$tu/src/MythicMobs/skills/Zaklecia.yml" "$DANE/plugins/MythicMobs/skills/Zaklecia.yml"
cp "$tu/src/DeluxeMenus/gui_menus/ksiega_zaklec.yml" "$tu/src/DeluxeMenus/gui_menus/ulubione_zaklec.yml" "$DANE/plugins/DeluxeMenus/gui_menus/"
# E: Q przez plugin Zaklecia (Shift+Q = ulubione, samo Q = hub jak dotad)
sed -i "s|^command: .*|command: 'zaklecia q %player%'|" "$DANE/plugins/MenuKey/config.yml"
python3 "$tu/wpnij_menu.py" "$DANE/plugins/DeluxeMenus"
mkdir -p "$DANE/plugins/Zaklecia"; cp "$tu/src/Zaklecia/config.yml" "$DANE/plugins/Zaklecia/config.yml"
if [[ -d "$tu/src/Nexo/pack/assets/zaklecia" ]]; then   # modele/tekstury dloni; Nexo generuje pack przy starcie
  rm -rf "$DANE/plugins/Nexo/pack/assets/zaklecia"; cp -r "$tu/src/Nexo/pack/assets/zaklecia" "$DANE/plugins/Nexo/pack/assets/"
fi

LOG="$DANE/logs/latest.log"; I0=$(stat -c %i "$LOG")
echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal w 10 min"; exit 6; }
echo "== kontrola"
grep -h "\[Zaklecia\] zaklecia:" "$LOG" || { echo "BLAD: plugin Zaklecia nie wystartowal"; exit 5; }
python3 "$tu/../kontrola_logu.py" < "$LOG" | grep -v "przeladowany" || true
R 'mm items list' | tr ',' '\n' | grep -c ZAKLECIE_ | sed 's/^/przedmioty ZAKLECIE_*: /'
grep -h "GUI menus loaded" "$LOG" | tail -1 | sed 's/.*INFO\]: /menu: /'
grep -q "Could not load menu.*ksiega_zaklec\|ksiega_zaklec.*error" "$LOG" && { echo "BLAD: Ksiega zaklec nie wczytana"; exit 7; } || true
