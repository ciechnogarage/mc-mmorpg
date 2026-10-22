#!/usr/bin/env bash
# Zasłanianie strumienia + "obie dłonie gasną po puszczeniu PPM" (decyzje właściciela 17.10). Zaklecia = ui-zaklecia (world1 12:35)
# + zaklecia-zaslanianie (09aaecf, f9759e7), merge 20f2ed8. Zmienia TYLKO jar Zaklec i 5 linii w Zaklecia/config.yml.
# Przed podmiana sprawdza, ze world1 ma dokladnie te wersje, z ktorych wychodzi paczka (inaczej PRZERWIJ - ktos wdrozyl cos nowszego).
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE=/opt/docker/mc/data/world1; KONT=mc-world1-1; P=$DANE/plugins
OCZ_JAR=248eb3aa4ce133557e90a320a897802e; OCZ_CFG=bc1f3105c08cf982d6f9b2fd1be0b5ee
LISTA="$(docker exec "$KONT" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online; WYMUS=1 wymusza"; exit 3; }
g=$(df -BG --output=avail / | tail -1 | tr -dc 0-9); (( g >= 6 )) || { echo "PRZERWANE: dysk < 6 GB"; exit 5; }
J=$(ls "$P"/zaklecia-*.jar); [[ $(echo "$J" | wc -l) == 1 ]] || { echo "PRZERWANE: wiecej niz jeden jar Zaklec"; exit 4; }
[[ "$(md5sum < "$J" | cut -d' ' -f1)" == "$OCZ_JAR" ]] || { echo "PRZERWANE: jar Zaklec na world1 inny niz oczekiwany ($J) - ktos wdrozyl nowsza wersje; scal ja z zaklecia-zaslanianie i zbuduj paczke od nowa"; exit 4; }
[[ "$(md5sum < "$P/Zaklecia/config.yml" | cut -d' ' -f1)" == "$OCZ_CFG" ]] || { echo "PRZERWANE: Zaklecia/config.yml zmieniony od przygotowania paczki"; exit 4; }
KOPIA="$DANE/backups/zaklecia-zaslanianie-$(date +%Y%m%d-%H%M)"; mkdir -p "$KOPIA/plugins/Zaklecia"
cp -a "$J" "$KOPIA/plugins/"; cp -a "$P/Zaklecia/config.yml" "$KOPIA/plugins/Zaklecia/"; cp "$tu/cofnij.sh" "$KOPIA/"; echo "== kopia: $KOPIA"
rm -f "$P"/zaklecia-*.jar; cp "$tu"/jary/zaklecia-*.jar "$P/"; cp "$tu/pliki/Zaklecia/config.yml" "$P/Zaklecia/config.yml"
LOG="$DANE/logs/latest.log"; I0=$(stat -c %i "$LOG"); echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal - uruchom $KOPIA/cofnij.sh"; exit 6; }
grep -E 'Enabling Zaklecia' "$LOG"; echo "== ostrzezenia Zaklec przy starcie: $(grep -E '\[Zaklecia\]' "$LOG" | grep -cE 'WARN|ERROR' || true)"
echo "== gotowe. Cofniecie: $KOPIA/cofnij.sh"
