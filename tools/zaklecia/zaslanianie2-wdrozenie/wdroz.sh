#!/usr/bin/env bash
# Zaslanianie v2 (celownik + krawedzie, wszystko zaslania) + bron do lewej odklada zaklecie (decyzje wlasciciela 17.10).
# Zaklecia a8b4c55 (galaz zaklecia-zaslanianie), KsiegaPostaci 76d0cec (galaz ksiega-zaklecia-lewa od hub-przywrocenie-1710),
# 3 pliki: Zaklecia/config.yml (wlasne-przywolania-zaslaniaja true), MythicMobs skille plomienie/odmrozenie (bez lineofsight).
# Tuz przed podmiana sprawdza md5 5 plikow world1 - jesli ktos wdrozyl cos nowszego, PRZERYWA.
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; DANE=/opt/docker/mc/data/world1; KONT=mc-world1-1; P=$DANE/plugins
declare -A OCZ=( [zaklecia-0.1.0-202610171337.jar]=482a8a1a543357fdb3f0c48662d1c298 [ksiega-postaci-0.1.0.jar]=3a8059f301a7b55543dabf32c0f0e09f
  [Zaklecia/config.yml]=61651e8ecf8cab449567a7d84eb40a0c [MythicMobs/skills/zaklecia/plomienie.yml]=766c6808bdb42fe2b02910e76282eb6c
  [MythicMobs/skills/zaklecia/odmrozenie.yml]=b48915dd7b896ef5a443ef6a9465622b )
LISTA="$(docker exec "$KONT" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: gracze online; WYMUS=1 wymusza"; exit 3; }
g=$(df -BG --output=avail / | tail -1 | tr -dc 0-9); (( g >= 6 )) || { echo "PRZERWANE: dysk < 6 GB"; exit 5; }
[[ $(ls "$P"/zaklecia-*.jar | wc -l) == 1 ]] || { echo "PRZERWANE: wiecej niz jeden jar Zaklec"; exit 4; }
for f in "${!OCZ[@]}"; do
  [[ -f "$P/$f" && "$(md5sum < "$P/$f" | cut -d' ' -f1)" == "${OCZ[$f]}" ]] || { echo "PRZERWANE: $f na world1 inny niz oczekiwany - ktos wdrozyl nowsza wersje; scal i zbuduj paczke od nowa"; exit 4; }
done
KOPIA="$DANE/backups/zaklecia-zaslanianie2-$(date +%Y%m%d-%H%M)"; mkdir -p "$KOPIA/plugins"
for f in "${!OCZ[@]}"; do mkdir -p "$KOPIA/plugins/$(dirname "$f")"; cp -a "$P/$f" "$KOPIA/plugins/$f"; done
cp "$tu/cofnij.sh" "$KOPIA/"; echo "== kopia: $KOPIA"
rm -f "$P"/zaklecia-*.jar; cp "$tu"/jary/zaklecia-*.jar "$P/"; cp "$tu/jary/ksiega-postaci-0.1.0.jar" "$P/"; cp -r "$tu/pliki/." "$P/"
LOG="$DANE/logs/latest.log"; I0=$(stat -c %i "$LOG"); echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal - uruchom $KOPIA/cofnij.sh"; exit 6; }
grep -E 'Enabling (Zaklecia|KsiegaPostaci)' "$LOG"; echo "== ostrzezenia Zaklecia/Ksiega/MythicMobs-zaklecia: $(grep -E '\[(Zaklecia|KsiegaPostaci)\]|Zak_' "$LOG" | grep -cE 'WARN|ERROR' || true)"
echo "== gotowe. Cofniecie: $KOPIA/cofnij.sh"
