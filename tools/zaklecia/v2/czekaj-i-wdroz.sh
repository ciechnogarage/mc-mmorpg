#!/usr/bin/env bash
# Czeka, az world1 bedzie pusty przez 2 kolejne minuty (max 12 h), i wtedy uruchamia wdroz.sh. Log: czekaj-i-wdroz.log
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; LOG="$tu/czekaj-i-wdroz.log"
echo "$(date '+%F %T') start strazy" >> "$LOG"; puste=0
for i in $(seq 1 720); do
  if docker exec mc-world1-1 rcon-cli 'minecraft:list uuids' 2>/dev/null | grep -q "There are 0 of"; then puste=$((puste+1)); else puste=0; fi
  if [[ $puste -ge 2 ]]; then echo "$(date '+%F %T') serwer pusty 2 min — wdrazam" >> "$LOG"; "$tu/wdroz.sh" >> "$LOG" 2>&1; echo "$(date '+%F %T') wdroz.sh exit=$?" >> "$LOG"; exit 0; fi
  sleep 60
done
echo "$(date '+%F %T') koniec 12 h bez pustego serwera — NIE wdrozono" >> "$LOG"
