#!/bin/bash
# Po klon-przygotuj.sh: klon jak produkcja dla klientow QA (kreator postaci, puste creator_limbo/stolica_odnowa, Q na gracza).
# Boty po tym nie przejda kreatora - testy botowe robic przed. Uzycie: klon-kreator.sh [nazwa]
set -euo pipefail
N=${1:-arena}; K=klon-$N; D=/opt/docker/mc/klony/$N/data
R() { docker exec -i $K rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
cp /opt/docker/mc/data/world1/plugins/character-creator-*.jar $D/plugins/
sed -i -e 's/^default-profile: true/default-profile: false/' -e 's/^profile-selection-invulnerability: false/profile-selection-invulnerability: true/' $D/plugins/MMOProfiles/config.yml
sed -i "s#^  zwykle: 'dm open hub_glowne %gracz%'#  zwykle: 'menugracza q %gracz%'#" $D/plugins/Zaklecia/config.yml
R 'mv remove creator_limbo' >/dev/null || true; R 'mv remove stolica_odnowa' >/dev/null || true
R 'mv create creator_limbo normal --generator VoidGen --world-type flat' >/dev/null; R 'mv create stolica_odnowa normal --generator VoidGen --world-type flat' >/dev/null
for w in "creator_limbo -8 99 -8 8 99 8" "stolica_odnowa -8 100 52 8 100 67"; do set -- $w
  R "minecraft:execute in minecraft:$1 run minecraft:forceload add $2 $4 $5 $7" >/dev/null
  R "minecraft:execute in minecraft:$1 run minecraft:fill $2 $3 $4 $5 $6 $7 minecraft:stone"
  R "minecraft:execute in minecraft:$1 run minecraft:forceload remove $2 $4 $5 $7" >/dev/null; done
R 'mv setspawn stolica_odnowa:0.5,101,59.5'
L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart $K >/dev/null
for i in $(seq 1 120); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
R 'minecraft:execute in minecraft:stolica_odnowa run minecraft:time set 6000' >/dev/null
grep -a -E 'Enabling (CharacterCreator|Arena)|CharacterCreator NIE|Q na gracza' $L | cut -c1-140
