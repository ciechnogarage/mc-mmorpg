#!/bin/bash
# Straznik wdrozenia: czeka (max 12 h) na pusty world1 (2 odczyty pod rzad co 60 s), uruchamia wdroz.sh i sprawdza start.
# Uruchamiany przez setsid nohup, niezalezny od sesji. Log: czekaj-i-wdroz.log w katalogu pluginu.
H="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; L=$H/czekaj-i-wdroz.log; koniec=$(( $(date +%s) + 43200 ))
echo "start $(date) (straznik, max 12 h)" > $L
puste=0
until [ $puste -ge 2 ]; do
  [ $(date +%s) -gt $koniec ] && { echo "PODDAJE SIE $(date): serwer nie byl pusty przez 12 h" >> $L; exit 1; }
  if docker exec mc-world1-1 rcon-cli "minecraft:list uuids" 2>/dev/null | grep -q "There are 0 of"; then puste=$((puste+1)); else puste=0; fi
  [ $puste -ge 2 ] || sleep 60
done
echo "pusto $(date)" >> $L
$H/wdroz.sh >> $L 2>&1; echo "wdroz exit $? $(date)" >> $L
J=$(ls -t $H/build/libs/*.jar | head -1 | sed 's/.*animacje-broni-//; s/\.jar//'); W=/opt/docker/mc/data/world1/logs/latest.log
for i in $(seq 1 120); do grep -q "Enabling AnimacjeBroni v$J" $W 2>/dev/null && grep -q "Done (" $W && break; sleep 5; done
grep -E "Enabling AnimacjeBroni|AnimacjeBroni\] Typy|Done \(|Brak MMOCore" $W | tail -4 >> $L
echo "bledy pluginu: $(grep -ciE 'animacjebroni.*(exception|error)' $W)" >> $L; echo "KONIEC $(date)" >> $L
