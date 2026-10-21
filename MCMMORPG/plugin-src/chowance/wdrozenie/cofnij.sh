#!/bin/bash
# Cofniecie wdrozenia zagrody: stary jar + config + menu z przed/. Czeka na zero graczy.
set -u
W=/opt/docker/mc/data/world1; B=/opt/docker/mc/backups/chowance-zagroda-2026-10-18; C=/opt/docker/mc/docker-compose.yml
log() { echo "$(date '+%F %T') $*"; }
OLD=$(ls $B/przed/chowance-0.1.0-*.jar 2>/dev/null | head -1); [ -f "$OLD" ] || { log "BLAD: brak kopii w przed/"; exit 1; }
for i in $(seq 1 1440); do
  L=$(docker exec mc-world1-1 rcon-cli "minecraft:list uuids" 2>/dev/null); echo "$L" | grep -q "There are 0 of" && break; sleep 30
done
echo "$L" | grep -q "There are 0 of" || { log "BLAD: serwer nie opustoszal"; exit 2; }
docker compose -f $C stop world1 || exit 3
rm -f $W/plugins/chowance-0.1.0-*.jar $W/plugins/.paper-remapped/chowance-0.1.0-*.jar && cp "$OLD" $W/plugins/
cp $B/przed/config.yml $W/plugins/Chowance/config.yml
cp $B/przed/chowance_menazeria.yml $W/plugins/DeluxeMenus/gui_menus/chowance_menazeria.yml
docker compose -f $C start world1
log "cofnieto (mlode zniknely same — ciala nieprzetrwale)"
