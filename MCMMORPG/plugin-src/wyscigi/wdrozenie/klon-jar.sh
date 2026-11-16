#!/bin/bash
# Podmiana samego jara Wyscigow na WLASNYM klonie + restart (bez ponownej budowy swiata).
set -euo pipefail
N=${1:-wyscigi}; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rm -f $D/plugins/wyscigi-*.jar; cp $A/build/libs/wyscigi-*.jar $D/plugins/
L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart -t 60 klon-$N >/dev/null
for i in $(seq 1 150); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
grep -a "Enabling Wyscigi" $L
