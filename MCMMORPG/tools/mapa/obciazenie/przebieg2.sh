#!/usr/bin/env bash
# Test obciazenia v2 na klonie mapa-proba. Uzycie: przebieg2.sh <nazwa> <N> <nic|reka|pelny>
# Przed pomiarem SPRAWDZA, ile botow trzyma OSOBISTA mape (map_id != 30000).
set -u
NAZWA=$1; N=$2; TRYB=$3; CZAS=${CZAS:-150}
TU="$(cd "$(dirname "$0")" && pwd)"; R="docker exec mapa-proba rcon-cli"; WYN=${WYN:?katalog wynikow}; mkdir -p $WYN
KLON=${KLON:?katalog danych klona}
docker rm -f mc-boty >/dev/null 2>&1
docker run -d --rm --name mc-boty --network mapa-net -e N=$N -e TRYB=$TRYB -e CZAS=$CZAS -e HOST=mapa-proba \
  -v /opt/docker/mc/forge/gracz/node_modules:/app/node_modules:ro -v $TU/boty2.js:/app/boty2.js:ro -w /app node:20 node boty2.js >/dev/null
sleep $(( N * 4 / 10 + 8 ))
for i in $(seq 0 $((N-1))); do
  $R "clear Bot$i minecraft:filled_map" >/dev/null 2>&1
  [ "$TRYB" != nic ] && $R "item replace entity Bot$i hotbar.7 with filled_map[map_id=30000]" >/dev/null 2>&1
done
sleep 15
OS=0; for i in $(seq 0 $((N-1))); do
  id=$($R "data get entity Bot$i SelectedItem.components.\"minecraft:map_id\"" | grep -o 'data: [0-9-]*' | grep -o '[0-9-]*$')
  [ -n "$id" ] && [ "$id" != 30000 ] && OS=$((OS+1))
done
$R "spark profiler --stop --save-to-file" >/dev/null 2>&1; sleep 3
PELNY=$(docker logs mc-boty 2>&1 | grep -c "ramki [1-9]")
TX0=$(docker exec mapa-proba cat /sys/class/net/eth0/statistics/tx_bytes); T0=$(date +%s); CPU=""; MS=""
for s in $(seq 1 9); do sleep 10; CPU="$CPU $(docker stats --no-stream --format '{{.CPUPerc}}' mapa-proba)"; MS="$MS | $($R mspt | sed 's/\x1b\[[0-9;]*m//g' | grep -o '◴.*' | cut -d, -f1)"; done
TX1=$(docker exec mapa-proba cat /sys/class/net/eth0/statistics/tx_bytes); T1=$(date +%s)
ONLINE=$($R "minecraft:list" | grep -o 'There are [0-9]*')
$R "spark profiler --stop --save-to-file" >/dev/null 2>&1; sleep 6
cp "$(ls -t $KLON/plugins/spark/*.sparkprofile | head -1)" $WYN/$NAZWA.sparkprofile
echo "$NAZWA N=$N tryb=$TRYB | $ONLINE | osobista mapa w rece: $OS/$N | pelny ekran otwarty: $PELNY | wysylka $(( (TX1-TX0)/(T1-T0)/1024/(N>0?N:1) )) KB/s na gracza | CPU$CPU | MSPT$MS" | tee -a $WYN/wyniki.txt
docker wait mc-boty >/dev/null 2>&1; sleep 3
