#!/bin/bash
# Przygotowanie klona `klon.sh start <nazwa> --z-kreatorem` do testow Areny (boty bez kreatora, ale z MMOProfiles/Chowance/Wiary).
set -euo pipefail
N=${1:-arena}; K=klon-$N; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
R() { docker exec -i $K rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
rm -f $D/plugins/character-creator-*.jar $D/plugins/arena-*.jar
sed -i 's/^default-profile: false/default-profile: true/' $D/plugins/MMOProfiles/config.yml
cp $A/build/libs/arena-*.jar $D/plugins/
cp -r $A/wdrozenie/pliki/. $D/plugins/
mkdir -p $D/plugins/WorldGuard/worlds/arena && cp $A/wdrozenie/swiat/regions.yml $D/plugins/WorldGuard/worlds/arena/regions.yml
L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart $K >/dev/null
for i in $(seq 1 120); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
grep -a -E 'Enabling (Arena|Chowance|Wiary|MMOProfiles)' $L
grep -v '^#' $A/wdrozenie/swiat/ustaw.txt | grep -v '^$' | R | grep -c -i 'success' || true
grep -v '^#' $A/wdrozenie/swiat/budowa.txt | grep -v '^$' | R | grep -c -E 'Changed|filled' || true
docker exec $K rcon-cli "minecraft:execute in minecraft:arena run time set 6000" >/dev/null
