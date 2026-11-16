#!/usr/bin/env bash
# Wdrozenie zaklec na world1. Wymaga RESTARTU (nowy jar pluginu Zaklecia), wiec
# rusza tylko przy 0 graczy. Kopia i cofniecie: backups/zaklecia-v2-<data>/cofnij-v2.sh
# Uzycie: wdroz.sh [katalog_danych=/opt/docker/mc/data/world1] [kontener=mc-world1-1]
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[[ -d "$tu/src" ]] || { echo "PRZERWANE: skopiuj ten plik do katalogu v2/"; exit 2; }
DANE="${1:-/opt/docker/mc/data/world1}"; KONT="${2:-mc-world1-1}"
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: na serwerze sa gracze (restart by ich wyrzucil); WYMUS=1 wymusza"; exit 3; }
JAR="$(ls -t "$tu"/zaklecia-*.jar | head -1)"
# Konfiguracja nowych typow zaklec wymaga jaru z odpowiadajacego jej zrodla.
# Brak swiezego jaru zatrzymuje wdrozenie PRZED kopia i restartem.
if [[ -n "$(find "$tu/src" -type f -newer "$JAR" -print -quit)" ]]; then
  echo "PRZERWANE: pliki v2/src sa nowsze niz $JAR; przygotuj aktualny jar poza tym skryptem"
  exit 4
fi

KOPIA="$DANE/backups/zaklecia-v2-$(date +%Y%m%d-%H%M)"; mkdir -p "$KOPIA"
for f in plugins/MythicMobs/items/Zaklecia.yml plugins/MythicMobs/skills/Zaklecia.yml plugins/MenuKey/config.yml \
         plugins/DeluxeMenus/config.yml plugins/DeluxeMenus/gui_menus/hub_umiejetnosci.yml \
         plugins/MythicHUD/config.yml; do
  if [[ -f "$DANE/$f" ]]; then mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"; fi
done
mkdir -p "$KOPIA/plugins"; cp -a "$DANE"/plugins/zaklecia-*.jar "$KOPIA/plugins/" 2>/dev/null || true
mkdir -p "$KOPIA/plugins/Nexo/pack/assets"; cp -a "$DANE/plugins/Nexo/pack/assets/zaklecia" "$KOPIA/plugins/Nexo/pack/assets/"
> "$KOPIA/NOWE.txt"
for f in plugins/DeluxeMenus/gui_menus/ksiega_zaklec.yml plugins/DeluxeMenus/gui_menus/ulubione_zaklec.yml; do
  if [[ -f "$DANE/$f" ]]; then
    mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"
  else
    echo "$f" >> "$KOPIA/NOWE.txt"
  fi
done
if [[ -f "$DANE/plugins/Zaklecia/config.yml" ]]; then mkdir -p "$KOPIA/plugins/Zaklecia"; cp -a "$DANE/plugins/Zaklecia/config.yml" "$KOPIA/plugins/Zaklecia/"; else echo "plugins/Zaklecia/config.yml" >> "$KOPIA/NOWE.txt"; fi
# Zapisz stan kazdego nowego pliku; cofniecie usunie tylko te, ktorych wczesniej nie bylo.
for src in "$tu"/src/MythicMobs/items/zaklecia/*.yml "$tu"/src/MythicMobs/skills/zaklecia/*.yml \
           "$tu"/src/MythicMobs/mobs/zaklecia/*.yml \
           "$tu"/src/MythicHUD/layouts/zaklecia.yml "$tu"/src/MythicHUD/hud_assets/hud/zaklecia.yml; do
  rel="plugins/${src#"$tu/src/"}"
  if [[ -f "$DANE/$rel" ]]; then
    mkdir -p "$KOPIA/$(dirname "$rel")"; cp -a "$DANE/$rel" "$KOPIA/$rel"
  else
    echo "$rel" >> "$KOPIA/NOWE.txt"
  fi
done
cp "$tu/cofnij-v2.sh" "$KOPIA/"; echo "== kopia: $KOPIA"

rm -f "$DANE"/plugins/zaklecia-*.jar; cp "$JAR" "$DANE/plugins/"
mkdir -p "$DANE/plugins/MythicMobs/items/zaklecia" "$DANE/plugins/MythicMobs/skills/zaklecia" "$DANE/plugins/MythicMobs/mobs/zaklecia"
cp "$tu"/src/MythicMobs/items/zaklecia/*.yml "$DANE/plugins/MythicMobs/items/zaklecia/"
cp "$tu"/src/MythicMobs/skills/zaklecia/*.yml "$DANE/plugins/MythicMobs/skills/zaklecia/"
cp "$tu"/src/MythicMobs/mobs/zaklecia/*.yml "$DANE/plugins/MythicMobs/mobs/zaklecia/"
rm -f "$DANE/plugins/MythicMobs/items/Zaklecia.yml" "$DANE/plugins/MythicMobs/skills/Zaklecia.yml"
mkdir -p "$DANE/plugins/MythicHUD/layouts" "$DANE/plugins/MythicHUD/hud_assets/hud"
cp "$tu/src/MythicHUD/layouts/zaklecia.yml" "$DANE/plugins/MythicHUD/layouts/zaklecia.yml"
cp "$tu/src/MythicHUD/hud_assets/hud/zaklecia.yml" "$DANE/plugins/MythicHUD/hud_assets/hud/zaklecia.yml"
python3 - "$DANE/plugins/MythicHUD/config.yml" <<'PYHUD'
from pathlib import Path
import sys
p = Path(sys.argv[1]); s = p.read_text()
if '  - zaklecia-layout' not in s:
    needle = '  default:\n'
    if needle not in s: raise SystemExit('BLAD: brak layout.default w MythicHUD/config.yml')
    p.write_text(s.replace(needle, needle + '  - zaklecia-layout\n', 1))
PYHUD
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
