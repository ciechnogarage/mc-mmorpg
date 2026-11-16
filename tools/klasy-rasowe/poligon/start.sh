#!/usr/bin/env bash
# Klon world1 do testow klas (etap 0/1). Uzycie: start.sh [env-testowe: 1|0]
set -euo pipefail
K=${K:-/home/dev/.claude/jobs/51c41ca8/tmp/klon/data}  # katalog danych klonu (nadpisz K=...)
docker rm -f klasy-etap0 >/dev/null 2>&1 || true
docker network create --subnet 10.99.41.0/24 klasy-net >/dev/null 2>&1 || true  # nigdy bez --subnet: domyslna pula zjada LAN 192.168.64/20 (01.10)
EXTRA=(); [ "${1:-0}" = 1 ] && EXTRA=(-e CC_TEST_WEJSCIE=1)
: > $K/logs/latest.log 2>/dev/null || true
docker run -d --name klasy-etap0 --network klasy-net --memory 3400m -v $K:/data "${EXTRA[@]}" \
 -e TYPE=CUSTOM -e CUSTOM_SERVER=/data/paper-1.21.11-132.jar -e VERSION=1.21.11 -e EULA=true -e ONLINE_MODE=false \
 -e ENABLE_RCON=true -e RCON_PASSWORD=etap0klon -e INIT_MEMORY=1G -e MAX_MEMORY=2G -e TZ=Europe/Warsaw \
 -e LEVEL_TYPE=minecraft:flat -e SPAWN_MONSTERS=false -e SPAWN_ANIMALS=false -e SPAWN_NPCS=false -e GENERATE_STRUCTURES=false \
 -e VIEW_DISTANCE=6 -e SIMULATION_DISTANCE=4 -e UID=1000 -e GID=1000 itzg/minecraft-server:latest >/dev/null
for i in $(seq 1 200); do grep -q 'Done (' $K/logs/latest.log 2>/dev/null && { echo DONE; exit 0; }; sleep 2; done; echo TIMEOUT; exit 1
