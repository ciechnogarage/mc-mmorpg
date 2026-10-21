#!/usr/bin/env bash
# Pieczenie lochu w kuzni: swiat Irisa z packa -> forceload calego obrazu -> czekaj az chunki staną -> metryki.
# Uzycie: piec.sh <pack> <ziarno> [polowa_rozmiaru=176]   ($KONTENER musi stac). Wypisuje nazwe swiata na koncu.
set -u
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
PACK=$1; SEED=$2; H=${3:-176}
T=$TOOLS; D=$DATA
W="${PACK}_$(date +%H%M%S)_${SEED}"          # swiezy prefiks: Iris cicho odmawia create na nazwie usunietego swiata
crash_before=$(ls $D/crash-reports 2>/dev/null | wc -l)
t0=$(date +%s)
docker exec $KONTENER rcon-cli "iris create $W dimension=$PACK seed=$SEED" >/dev/null 2>&1
for i in $(seq 1 60); do docker logs --since 3m $KONTENER 2>&1 | grep -q "Engine: $W/" && break; sleep 2; done
# vanilla: forceload add przyjmuje max 256 chunkow na komende - wiekszy obszar cicho odrzuca. Cwiartki.
for qx in "-$H -1" "0 $((H-1))"; do for qz in "-$H -1" "0 $((H-1))"; do
  set -- $qx; x0=$1; x1=$2; set -- $qz; z0=$1; z1=$2
  docker exec $KONTENER rcon-cli "execute in minecraft:${W,,} run forceload add $x0 $z0 $x1 $z1" 2>&1 | tr -d '\000' | sed 's/\x1b\[[0-9;]*m//g' | grep -viE 'marked|oznaczon' | head -1
done; done
need=$(( (2*H/16) * (2*H/16) )); prev=-1; n=0
for i in $(seq 1 90); do
  docker exec $KONTENER rcon-cli "save-all flush" >/dev/null 2>&1
  # licz tylko chunki w stanie full (w pliku leza tez niedokonczone proto-chunki)
  n=$(python3 -c "
import sys,glob;sys.path.insert(0,'$T');import mca
n=0
for p in glob.glob('$D/$W/region/*.mca'):
    r=mca.Region(p)
    for cx,cz in r.chunks():
        c=r.chunk(cx & 31,cz & 31); n+= bool(c) and c.get('Status')=='minecraft:full'
print(n)" 2>/dev/null || echo 0)
  [ "$n" = "$prev" ] && [ "$n" -ge "$need" ] && break; prev=$n; sleep 3
done
dt=$(( $(date +%s)-t0 ))
crash_after=$(ls $D/crash-reports 2>/dev/null | wc -l)
echo "swiat=$W | ${dt}s | chunki=$n/$need | serwer=$([ "$crash_after" -gt "$crash_before" ] && echo CRASH || echo ok) ($(docker ps --filter name=$KONTENER --format '{{.Status}}'))"
python3 "$T/loch_metrics.py" "$LEVELS/${PACK}_spec.json" "$D/$W/region"
echo "SWIAT=$W"
