#!/usr/bin/env bash
# Wdrozenie napraw zaklec (test gracza 09-12.10) na world1: Zaklecia 428760e + KsiegaPostaci 264ee80 (107b60a reguly rak + filtr broni + bez refresh przy anulowanym slocie) + 6 plikow configu.
echo "PRZESTARZALE (17.10): te naprawy sa na world1 od 13.10 20:01 (paczka tools/zaklecia/kostur/wdrozenie). Ten skrypt cofnalby Kostur i Ksiege z 17.10 - NIE URUCHAMIAC."; exit 9
# Wymaga RESTARTU, wiec rusza tylko przy 0 graczy (WYMUS=1 wymusza). Kopia + cofnij.sh w backups/zaklecia-naprawy-<data>.
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE=/opt/docker/mc/data/world1; KONT=mc-world1-1
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: na serwerze sa gracze; WYMUS=1 wymusza"; exit 3; }
g=$(df -BG --output=avail / | tail -1 | tr -dc 0-9); (( g >= 6 )) || { echo "PRZERWANE: dysk < 6 GB"; exit 5; }; echo "== dysk ${g} GB"
KOPIA="$DANE/backups/zaklecia-naprawy-$(date +%Y%m%d-%H%M)"; mkdir -p "$KOPIA/plugins"
cp -a "$DANE"/plugins/zaklecia-*.jar "$DANE/plugins/ksiega-postaci-0.1.0.jar" "$KOPIA/plugins/"
(cd "$tu/pliki" && find . -type f) | while read -r f; do mkdir -p "$KOPIA/plugins/$(dirname "$f")"; cp -a "$DANE/plugins/$f" "$KOPIA/plugins/$f"; done
cp "$tu/cofnij.sh" "$KOPIA/"; echo "== kopia: $KOPIA"
rm -f "$DANE"/plugins/zaklecia-*.jar; cp "$tu"/jary/zaklecia-*.jar "$DANE/plugins/"; cp "$tu/jary/ksiega-postaci-0.1.0.jar" "$DANE/plugins/"
cp -r "$tu/pliki/." "$DANE/plugins/"
LOG="$DANE/logs/latest.log"; I0=$(stat -c %i "$LOG")
echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal w 10 min - uruchom $KOPIA/cofnij.sh"; exit 6; }
grep -E 'Enabling (Zaklecia|KsiegaPostaci)' "$LOG"
B=$(grep -E '\[(Zaklecia|KsiegaPostaci)\]' "$LOG" | grep -cE 'WARN|ERROR' || true); echo "== ostrzezenia Zaklecia/Ksiega przy starcie: $B"
grep -E '\[(Zaklecia|KsiegaPostaci)\]' "$LOG" | grep -E 'WARN|ERROR' | head -10 || true
echo "== gotowe. Cofniecie: $KOPIA/cofnij.sh"
