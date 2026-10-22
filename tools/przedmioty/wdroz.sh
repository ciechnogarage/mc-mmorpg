#!/usr/bin/env bash
# Wdrozenie systemu przedmiotow na world1: kopia -> generator -> bramka -> przeladowanie (mi reload, mm reload).
# Linia "Poziom przedmiotu" (custom stat CUSTOM_ILVL) zadziala dopiero po restarcie serwera.
# Pierwsze wdrozenie przed restartem: BEZ_ILVL=1 wdroz.sh ; po restarcie zwykle wdroz.sh (dopisuje linie ilvl).
# Lup pod archetyp: SMART=1 (gdy %coretools_var_player_klasa_archetyp% dziala na serwerze).
# Wycofanie: tools/przedmioty/wycofaj.sh <katalog kopii>
set -euo pipefail
W=/opt/docker/mc/data/world1; T=$W/tools/przedmioty; P=$W/plugins
K=$W/backups/przedmioty-$(date +%Y%m%d-%H%M%S); mkdir -p "$K"
( cd "$P" && while read -r f; do if [ -e "$f" ]; then tar -cf - "$f" | tar -C "$K" -xf -; fi; done < "$T/pliki.txt" )
echo "kopia: $K"
PYTHONDONTWRITEBYTECODE=1 python3 "$T/generuj.py" ${SMART:+--smart} --plugins "$P" --katalog "$T/katalog.json"
docker run --rm -v "$P:/p:ro" -v "$T:/t:ro" -e PYTHONDONTWRITEBYTECODE=1 python:3.12-slim \
  sh -c 'pip -q install pyyaml >/dev/null 2>&1; python /t/sprawdz.py /p' || { echo "BRAMKA NIE PRZESZLA — przywracam"; "$T/wycofaj.sh" "$K"; exit 1; }
docker exec mc-world1-1 rcon-cli "mi reload" | tail -3
docker exec mc-world1-1 rcon-cli "mm reload" | tail -2
docker exec mc-world1-1 rcon-cli "mythiclib reload" | tail -2   # separator dziesietny
docker exec mc-world1-1 rcon-cli "nexo reload pack" | tail -2  # ramki tooltipu (przedmioty:<rzadkosc>) -> nowy pack do graczy
echo "wdrozone; kopia do wycofania: $K"
