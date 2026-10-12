#!/usr/bin/env bash
# Klon world1 do testow wyboru postaci (agent WYBOR). Bez portow na hoscie, siec cc-nap-wybor-net.
set -euo pipefail
K=/home/dev/.claude/jobs/2f21747c/tmp/wybor/data
docker rm -f cc-nap-wybor >/dev/null 2>&1 || true
docker network create cc-nap-wybor-net >/dev/null 2>&1 || true
mkdir -p $K/logs; : > $K/logs/latest.log
docker run -d --name cc-nap-wybor --network cc-nap-wybor-net --memory 3400m -v $K:/data -e CC_TEST_WEJSCIE=1 \
 -e TYPE=CUSTOM -e CUSTOM_SERVER=/data/paper-1.21.11-132.jar -e VERSION=1.21.11 -e EULA=true -e ONLINE_MODE=false \
 -e ENABLE_RCON=true -e RCON_PASSWORD=wybor -e INIT_MEMORY=1G -e MAX_MEMORY=2G -e TZ=Europe/Warsaw \
 -e VIEW_DISTANCE=6 -e SIMULATION_DISTANCE=4 -e UID=1000 -e GID=1000 -e SKIP_SERVER_PROPERTIES=true itzg/minecraft-server:latest >/dev/null
for i in $(seq 1 240); do grep -q 'Done (' $K/logs/latest.log 2>/dev/null && { echo DONE; exit 0; }; sleep 2; done; echo TIMEOUT; exit 1
