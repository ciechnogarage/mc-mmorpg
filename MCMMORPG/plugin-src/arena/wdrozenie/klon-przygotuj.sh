#!/bin/bash
# Przygotowanie klona `klon.sh start <nazwa> --z-kreatorem` do testow Areny (boty bez kreatora, ale z MMOProfiles/Chowance/Wiary).
set -euo pipefail
N=${1:-arena}; K=klon-$N; D=/opt/docker/mc/klony/$N/data; A="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
R() { docker exec -i $K rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
rm -f $D/plugins/character-creator-*.jar $D/plugins/arena-*.jar
sed -i -e 's/^default-profile: false/default-profile: true/' -e 's/^profile-selection-invulnerability: true/profile-selection-invulnerability: false/' $D/plugins/MMOProfiles/config.yml  # boty bez kreatora: inaczej MMOProfiles robi je niesmiertelnymi
cp $A/build/libs/arena-*.jar $D/plugins/
cp -r $A/wdrozenie/pliki/. $D/plugins/
rm -f $D/plugins/MythicLib/config.yml.PATCH.md
python3 - "$D/plugins/MythicLib/config.yml" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
lines = path.read_text().splitlines(keepends=True)
pvp = next(i for i, line in enumerate(lines) if line.strip() == 'pvp_on:')
offense = next(i for i in range(pvp + 1, len(lines)) if lines[i].strip() == 'offense:')
indent = len(lines[offense]) - len(lines[offense].lstrip())
end = next((i for i in range(offense + 1, len(lines))
            if lines[i].strip() and not lines[i].lstrip().startswith('#')
            and len(lines[i]) - len(lines[i].lstrip()) <= indent), len(lines))
for key in ('party_member', 'guild_ally', 'party_other', 'guild_enemy'):  # PvP tylko w ringu (WG+plugin), wiec przyjaciele tez moga sie bic
    entry = next((i for i in range(offense + 1, end) if lines[i].lstrip().startswith(key + ':')), None)
    if entry is None:
        lines.insert(end, ' ' * (indent + 2) + key + ': true\n')
        end += 1
    else:
        lines[entry] = ' ' * (indent + 2) + key + ': true\n'
path.write_text(''.join(lines))
PY
mkdir -p $D/plugins/WorldGuard/worlds/arena && cp $A/wdrozenie/swiat/regions.yml $D/plugins/WorldGuard/worlds/arena/regions.yml
L=$D/logs/latest.log; I=$(stat -c %i $L); docker restart $K >/dev/null
for i in $(seq 1 120); do [ "$(stat -c %i $L 2>/dev/null)" != "$I" ] && grep -q 'Done (' $L && break; sleep 5; done
grep -a -E 'Enabling (Arena|Chowance|Wiary|MMOProfiles)' $L
grep -v '^#' $A/wdrozenie/swiat/ustaw.txt | grep -v '^$' | grep -v '^mv setspawn ' | R | grep -c -i 'success' || true
grep -v '^#' $A/wdrozenie/swiat/wg-pvp-deny.txt | grep -v '^$' | R | grep -c -i 'success' || true
grep -v '^#' $A/wdrozenie/swiat/budowa.txt | grep -v '^$' | R | grep -c -E 'Changed|filled' || true
grep '^mv setspawn ' $A/wdrozenie/swiat/ustaw.txt | R | grep -c -i 'success' || true
docker exec $K rcon-cli "minecraft:execute in minecraft:arena run time set 6000" >/dev/null
