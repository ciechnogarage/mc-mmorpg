#!/usr/bin/env bash
# Bramka przed wdrozeniem packa: N ziaren, kazde musi sie wygenerowac bez crasha serwera.
# Uzycie: soak.sh <pack> <ziarno1> [ziarno2 ...]   (serwer $KONTENER musi stac)
set -u
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
PACK=$1; shift
RUN=$(date +%H%M%S)   # swiezy prefiks: Iris pamieta nazwy usunietych swiatow i cicho odmawia create
T=$TOOLS
for SEED in "$@"; do
  W="s${RUN}_${SEED}"
  crash_before=$(ls $DATA/crash-reports 2>/dev/null | wc -l)
  t0=$(date +%s)
  resp=$(docker exec $KONTENER rcon-cli "iris create $W dimension=$PACK seed=$SEED" 2>&1 | tr -d '\000' | sed 's/\x1b\[[0-9;]*m//g' | tr '\n' ' ')
  for i in $(seq 1 60); do docker logs --since 3m $KONTENER 2>&1 | grep -q "Engine: $W/" && break; sleep 2; done
  docker exec $KONTENER rcon-cli "execute in minecraft:${W,,} run forceload add -128 -128 127 127" >/dev/null 2>&1
  prev=-1; n=0
  for i in $(seq 1 45); do
    docker exec $KONTENER rcon-cli "save-all flush" >/dev/null 2>&1
    n=$(python3 -c "import sys,glob;sys.path.insert(0,'$T');import mca;print(sum(len(mca.Region(p).chunks()) for p in glob.glob('$DATA/$W/region/*.mca')))" 2>/dev/null || echo 0)
    [ "$n" = "$prev" ] && [ "$n" -ge 256 ] && break; prev=$n; sleep 3
  done
  dt=$(( $(date +%s)-t0 ))
  up=$(docker ps --filter name=$KONTENER --format '{{.Status}}')
  crash_after=$(ls $DATA/crash-reports 2>/dev/null | wc -l)
  seed_log=$(docker logs --since 5m $KONTENER 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -oE "Engine: $W/[^ ]+ .*Seed: -?[0-9]+" | tail -1 | grep -oE 'Seed: -?[0-9]+')
  metrics=$(python3 "$T/level_metrics.py" "$PACK" "$DATA/$W/region" 2>&1)
  docker exec $KONTENER rcon-cli "execute in minecraft:${W,,} run forceload remove all" >/dev/null 2>&1
  crashed=$([ "$crash_after" -gt "$crash_before" ] && echo "CRASH" || echo "ok")
  echo "$SEED | ${seed_log:-BRAK SILNIKA rcon=[$resp]} | ${dt}s | chunki=$n | serwer=$crashed ($up) | $metrics"
done
