#!/usr/bin/env bash
# Wszystkie scenariusze profesji v2 na klonie (klon.sh), KAZDY osobnym botem (wspolny /opt/docker/mc/narzedzia/bot).
# Uzycie: run_all.sh [nazwa-klonu=profesje]
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; K=${1:-profesje}
pass=0; fail=0
for s in wezel zielarstwo buff zlecen warsztat oprawianie lowienie lowisko2 naprawa gniazdo menu oboz mistrz; do
  out=$(LIMIT_MS=400000 /opt/docker/mc/narzedzia/bot/run.sh "$K" "$here/gra.js" "$s" 2>&1 | grep -E '^(PASS|FAIL)')
  echo "$out" | sed "s/^/[$s] /"
  pass=$((pass + $(echo "$out" | grep -c '^PASS'))); fail=$((fail + $(echo "$out" | grep -c '^FAIL')))
  [ -z "$out" ] && { echo "[$s] FAIL brak wyniku (bot nie wszedl / limit)"; fail=$((fail+1)); }
done
echo; echo "RAZEM: $pass PASS, $fail FAIL"
[ "$fail" = 0 ]
