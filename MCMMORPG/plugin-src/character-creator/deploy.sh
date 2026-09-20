#!/usr/bin/env bash
# Wdraza zbudowany jar kreatora na world1 i restartuje serwer.
#
# Istnieje jako OSOBNY skrypt, a nie ciag komend, zeby reguła uprawnien mogla wskazywac
# dokladnie te czynnosc, zamiast otwierac ogolne "kopiuj cokolwiek do plugins/".
#
# Zawsze robi kopie poprzedniego jara — wdrozenie ma byc odwracalne jednym `cp` z powrotem.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
plugins=/opt/docker/mc/data/world1/plugins
backups="$here/deploy-backups"
container=mc-world1-1

jar="$(ls -t "$here"/build/libs/character-creator-*.jar 2>/dev/null | head -1)"
[ -n "$jar" ] || { echo "BLAD: brak zbudowanego jara w build/libs — uruchom ./build.sh"; exit 1; }

mkdir -p "$backups"
stamp="$(date +%Y%m%d-%H%M%S)"
shopt -s nullglob
for old in "$plugins"/character-creator-*.jar; do
  cp -p "$old" "$backups/$(basename "$old" .jar)-$stamp.jar"
  echo "kopia: $backups/$(basename "$old" .jar)-$stamp.jar"
  rm -f "$old"
done
shopt -u nullglob

cp "$jar" "$plugins/$(basename "$jar")"
echo "wdrozony: $(basename "$jar")"

if [ "${1:-restart}" = "restart" ]; then
  log=/opt/docker/mc/data/world1/logs/latest.log

  # Numer i-wezla SPRZED restartu. Bez tego petla ponizej dopasowywala "Done (" z
  # POPRZEDNIEGO uruchomienia: Paper rotuje latest.log dopiero przy starcie, wiec przez
  # pierwsze sekundy po `docker restart` pod ta nazwa lezy nadal stary plik — i skrypt
  # meldowal "serwer wstal" natychmiast, zanim serwer w ogole zaczal wstawac.
  before="$(stat -c %i "$log" 2>/dev/null || echo none)"

  docker restart "$container" >/dev/null
  echo -n "restart $container"
  for _ in $(seq 1 60); do
    now="$(stat -c %i "$log" 2>/dev/null || echo none)"
    # Dopiero NOWY plik logu swiadczy o tym, ze to juz ta sesja.
    if [ "$now" != "none" ] && [ "$now" != "$before" ] \
       && grep -q 'Done (' "$log" 2>/dev/null; then
      echo " — serwer wstal"; exit 0
    fi
    echo -n "."; sleep 5
  done
  echo " — UWAGA: nie zobaczylem 'Done (' w NOWYM logu w 5 min"
  exit 1
fi
