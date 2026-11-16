#!/usr/bin/env bash
# Profesje v2 na KLONIE z /opt/docker/mc/narzedzia/klon.sh: generator tresci + jary + paper-global (noteblock/tripwire),
# restart klonu. Uzycie: gen_klon.sh [nazwa=profesje]
set -euo pipefail
N=${1:-profesje}; D=/opt/docker/mc/klony/$N/data; R="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ -d "$D/plugins" ]] || { echo "brak klonu: /opt/docker/mc/narzedzia/klon.sh start $N"; exit 2; }
DZIALA=$(docker ps --format "{{.Names}}" | grep -cx "klon-$N" || true)   # restart wlasnego kontenera nie oddaje slotu klonu
docker run --rm --name "profesje-gen-$$" --memory 768m -u "$(id -u):$(id -g)" -w /tmp -e HOME=/tmp \
  -v "$D/plugins:/k/plugins" -v "$R:/w:ro" -v /home/dev/Desktop/Przemek/MC/tools:/tools:ro \
  -v /opt/docker/mc/backups/mistrzostwa-20260923/wdrozenie:/mst:ro python:3.12-slim sh -c \
  'pip -q install --target /tmp/pp pyyaml pillow >/dev/null 2>&1; PYTHONPATH=/tmp/pp python /w/profesje/generuj.py --plugins /k/plugins --katalog-v2 /tools/przedmioty/katalog.json --ikony-v1 /tools/ikony-profesji --mistrzostwa /mst'
for j in rzemioslo mistrzostwa; do
  J=$(ls "$R"/plugin-src/$j/build/libs/$j-*.jar 2>/dev/null | head -1)
  [[ -f "$J" ]] || { echo "brak jara $j — zbuduj: $R/plugin-src/$j/build.sh"; exit 1; }
  rm -f "$D"/plugins/$j-*.jar; cp "$J" "$D/plugins/"
done
G=$D/config/paper-global.yml
[[ -f "$G" ]] && sed -i 's/disable-noteblock-updates: false/disable-noteblock-updates: true/; s/disable-tripwire-updates: false/disable-tripwire-updates: true/' "$G"
if [[ "$DZIALA" == 1 ]]; then
  L=$D/logs/latest.log; I0=$(stat -c %i "$L"); docker restart "klon-$N" >/dev/null
  for i in $(seq 1 150); do sleep 4; [[ "$(stat -c %i "$L" 2>/dev/null)" != "$I0" ]] && grep -q 'Done (' "$L" && break; done
else
  /opt/docker/mc/narzedzia/klon.sh start "$N"
fi
grep 'Rzemioslo\] Kontrola' "$D/logs/latest.log" || echo "UWAGA: brak linii kontroli Rzemioslo"
