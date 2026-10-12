#!/bin/bash
# pozy.sh <TYP> <ID> <poza...> -> dla kazdej pozy PPM (brak = bez consumable) zrzut z przodu i zza plecow przy trzymanym PPM.
# Zmienia poze w configu KLONA (ab-test) i robi /animacjebroni reload. Zrzuty: /review/captures/poza-<poza>-{przod,tyl}.png
T=$1; I=$2; shift 2
X="docker exec -e DISPLAY=:99 mc-client-ab"; R="docker exec ab-test rcon-cli"; r(){ $R "$1" | sed 's/\x1b\[[0-9;]*m//g'; }
CFG=${CFG:?sciezka configu AnimacjeBroni klona}
for poza in "$@"; do
  python3 - "$CFG" "$T" "$poza" <<'PY'
import re, sys
p, typ, poza = sys.argv[1:]; s = open(p).read()
m = re.search(r'(?ms)^  %s:\n(?:    .*\n)*' % typ, s); b = m.group(0)
n = re.sub(r'(?m)^    poza: .*\n', '', b)
if poza != 'brak': n = n.replace('    czas:', '    poza: %s\n    czas:' % poza, 1)
open(p, 'w').write(s.replace(b, n))
PY
  r "animacjebroni reload" >/dev/null; r "minecraft:clear BronQA" >/dev/null; $X xdotool key 1
  r "mi give $T $I BronQA" >/dev/null; sleep 0.5; $X xdotool key 2; sleep 0.3; $X xdotool key 1; sleep 1.2
  r "minecraft:tp BronQA 0.5 -59 0.5 0 0" >/dev/null; sleep 0.4
  $X xdotool key F5; sleep 0.3; $X xdotool key F5; sleep 0.4; $X xdotool mousedown 3; sleep 1.0
  $X import -window root /review/captures/poza-$poza-przod.png; $X xdotool mouseup 3; sleep 0.3
  $X xdotool key F5; sleep 0.4; $X xdotool mousedown 3; sleep 1.0
  $X import -window root /review/captures/poza-$poza-tyl.png; $X xdotool mouseup 3; sleep 0.3
  $X xdotool key F5; sleep 0.2; $X xdotool key F5; sleep 0.2; $X xdotool key F5; sleep 0.3   # z powrotem 1. osoba
  echo "$poza: $(r 'animacjebroni sprawdz BronQA' | grep -oE 'garda=[a-z]+')"
done
