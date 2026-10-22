#!/usr/bin/env bash
# Wdrozenie zaklec (Plomienie, Odmrozenie) na serwer: pliki MM/Crucible + assety packa Nexo + przeladowania.
# Uzycie: wdroz.sh [katalog_danych=/opt/docker/mc/data/world1] [kontener=mc-world1-1] [--bez-packa]
# Plugin Zaklecia (ciche zasoby MMOCore) wchodzi dopiero z restartem: skrypt sam wybiera wariant skilli.
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DANE="${1:-/opt/docker/mc/data/world1}"; KONT="${2:-mc-world1-1}"; PACK=1; [[ "${3:-}" == "--bez-packa" ]] && PACK=0
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LOG="$DANE/logs/latest.log"; start=$(wc -l < "$LOG")

echo "== gracze: $(R 'minecraft:list uuids' | head -1)"
# kopia zapasowa tylko tego, co nadpisujemy (za pierwszym razem dziennie)
KOPIA="$DANE/backups/zaklecia-$(date +%Y%m%d)"
if [[ ! -d "$KOPIA" ]]; then
  mkdir -p "$KOPIA"
  for f in plugins/MythicMobs/items/Zaklecia.yml plugins/MythicMobs/skills/Zaklecia.yml plugins/Nexo/pack/assets/zaklecia; do
    [[ -e "$DANE/$f" ]] && { mkdir -p "$KOPIA/$(dirname "$f")"; cp -a "$DANE/$f" "$KOPIA/$f"; } || echo "$f" >> "$KOPIA/NOWE.txt"
  done
  cp "$tu/cofnij.sh" "$KOPIA/"; echo "== kopia: $KOPIA"
fi

if R 'plugins' | grep -q 'Zaklecia'; then
  echo "== plugin Zaklecia zaladowany -> wariant cichy"
  cp "$tu/src/MythicMobs/skills/Zaklecia.yml" "$DANE/plugins/MythicMobs/skills/Zaklecia.yml"
else
  echo "== brak pluginu Zaklecia -> wariant awaryjny (komenda konsoli; linia w logu co tik rzucania)"
  python3 "$tu/wariant_awaryjny.py" "$tu/src/MythicMobs/skills/Zaklecia.yml" "$DANE/plugins/MythicMobs/skills/Zaklecia.yml"
fi
cp "$tu/src/MythicMobs/items/Zaklecia.yml" "$DANE/plugins/MythicMobs/items/Zaklecia.yml"
rm -rf "$DANE/plugins/Nexo/pack/assets/zaklecia"; cp -r "$tu/src/Nexo/pack/assets/zaklecia" "$DANE/plugins/Nexo/pack/assets/"

R 'mm reload' >/dev/null; sleep 8
[[ $PACK == 1 ]] && { R 'nexo reload pack' >/dev/null; sleep 20; }

echo "== kontrola"
tail -n +"$((start + 1))" "$LOG" | python3 "$tu/kontrola_logu.py"
R 'mm items list' | tr ',' '\n' | grep -c ZAKLECIE_ | sed 's/^/przedmioty ZAKLECIE_*: /'
