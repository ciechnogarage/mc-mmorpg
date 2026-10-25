#!/bin/bash
# Podmiana samego jara Licha na WLASNYM klonie + klon.sh restart (bez ponownej budowy swiata).
set -euo pipefail
N=${1:-licho}; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rm -f $D/plugins/licho-*.jar; cp $A/build/libs/licho-*.jar $D/plugins/
/opt/docker/mc/narzedzia/klon.sh restart $N
docker exec klon-$N rcon-cli 'minecraft:difficulty peaceful' >/dev/null
grep -a "Enabling Licho" $D/logs/latest.log
