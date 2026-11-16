#!/usr/bin/env bash
# Wgrywa paczkę „klasy: progresja” (serwer/plugins + generatory) do KATALOGU DANYCH serwera.
# Użycie: tools/klasy-rasowe/serwer/wgraj.sh <data-dir> [jar-zaklec]
#   <data-dir>  np. /opt/docker/mc/klony/klasy-progres/data (katalog z plugins/)
#   jar-zaklec  opcjonalnie zbudowany zaklecia-*.jar (MCMMORPG/plugin-src/zaklecia/build.sh) — podmienia stary
# world1 tylko z WGRAJ_WORLD1=tak (robi to agent integracji, po kopii i z cofnij.sh).
# Kolejność ma znaczenie: skille A (MythicLib/MythicMobs/CoreTools z tools/klasy-rasowe/<archetyp>/) → pliki B →
# generuj_klasy (czyta skille, szkielety i drzewka) → karta_kreatora → przedmioty → sprawdz.
set -euo pipefail
D="${1:?podaj katalog danych serwera}"; JAR="${2:-}"
[[ -d "$D/plugins/MMOCore" ]] || { echo "brak $D/plugins/MMOCore"; exit 1; }
if [[ "$(realpath "$D")" == /opt/docker/mc/data/world1* && "${WGRAJ_WORLD1:-}" != tak ]]; then
  echo "world1: odmowa (WGRAJ_WORLD1=tak tylko dla agenta integracji)"; exit 2; fi
R="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"   # korzeń repo
S="$R/tools/klasy-rasowe/serwer/plugins"
# skille klas (paczka A): katalogi mają układ plugins/ (MythicLib/skill, MythicMobs/{mobs,skills}, CoreTools)
for a in wojownik ninja mag akolita specjalizacje; do cp -r "$R/tools/klasy-rasowe/$a/." "$D/plugins/"; done
cp -r "$S/." "$D/plugins/"
# drzewka usunięte z paczki (B4/B14): combat, loop
rm -f "$D/plugins/MMOCore/skill-trees/combat.yml" "$D/plugins/MMOCore/skill-trees/loop.yml"
if [[ -n "$JAR" ]]; then
  rm -f "$D"/plugins/zaklecia-*.jar; cp "$JAR" "$D/plugins/"; fi
export KLASY_ROOT="$D" PYTHONDONTWRITEBYTECODE=1
[[ "$(realpath "$D")" == /opt/docker/mc/data/world1* ]] && export KLASY_WORLD1=tak
python3 "$R/tools/klasy-rasowe/generuj_klasy.py"
python3 "$R/tools/klasy-rasowe/karta_kreatora.py"
python3 "$R/tools/przedmioty/generuj.py" --plugins "$D/plugins"
echo "Teraz: tools/przedmioty/sprawdz.py (docker, pyyaml) i restart serwera (klon: klon.sh restart <n>)."
