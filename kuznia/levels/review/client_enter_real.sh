#!/usr/bin/env bash
# Kopia client_enter.sh BEZ przelaczania w tryb widza: gracz przechodzi wejscie jak prawdziwy gracz.
set -u
DUN=${1:-poziom_01}; K=/opt/docker/mc/forge/levels/klient
r(){ docker exec mc-forge rcon-cli "$1" 2>&1 | tr -d '\000' | sed 's/\x1b\[[0-9;]*m//g'; }
S=$(date -u +%Y-%m-%dT%H:%M:%SZ)
docker rm -f mc-client-wieza >/dev/null 2>&1
docker run -d --name mc-client-wieza --network forge_default -u 1000:1000 \
  -e MC_SERVER=mc-forge -e MC_PORT=25565 -e MC_USER=KlientQA \
  -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro \
  -v $K/instance:/mc/instance -v $K/captures:/review mc-client-test:latest bash /mc/launch.sh >/dev/null
for i in $(seq 1 60); do docker logs --since "$S" mc-forge 2>&1 | grep -q 'KlientQA joined' && break; sleep 3; done
sleep 3; r "op KlientQA" >/dev/null; r "gamemode adventure KlientQA" >/dev/null
r "execute in minecraft:overworld run tp KlientQA 0 -59 0" >/dev/null; sleep 2
r "md play $DUN KlientQA" >/dev/null
for i in $(seq 1 30); do W=$(docker logs --since "$S" mc-forge 2>&1 | grep -oE "Loading world into memory ${DUN}_[0-9]+" | tail -1 | awk '{print $NF}'); [ -n "$W" ] && break; sleep 2; done
echo "$W"
