#!/usr/bin/env bash
# Probkuje mspt co 10 s przez N probek; wypisuje avg/min/max z okna 10 s.
N=${1:-18}
for i in $(seq 1 $N); do
  o=$(docker exec mc-world1-1 rcon-cli "mspt" | sed 's/\x1b\[[0-9;]*m//g' | tr '\n' ' ')
  echo "$(date +%T) $(echo "$o" | grep -o '◴ [^,]*, [^,]*' | awk -F', ' '{print $2}')"
  sleep 10
done
