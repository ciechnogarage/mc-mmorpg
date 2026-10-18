#!/bin/bash
# kierunki.sh <TYP> <ID> -> dla kazdego kierunku (brak, w, a, d, s): trzyma klawisz + PPM, zrzut z boku (F5 x2 = z przodu),
# puszcza, czyta z logu klona wariant MOCNY. Zrzuty: captures/<ID>-<kier>.png
T=$1; I=$2
X="docker exec -e DISPLAY=:99 ${KLIENT:-mc-client-bronie}"; r(){ docker exec ${KLON:-klon-bronie-atak} rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g'; }
L=/opt/docker/mc/klony/${KLON#klon-}/data/logs/latest.log
$X xdotool key F5; sleep 0.3; $X xdotool key F5; r "minecraft:clear BronQA" >/dev/null; $X xdotool key 1; r "mi give $T $I BronQA" >/dev/null; sleep 1.5
for k in brak w a d s; do
  r "minecraft:teleport BronQA 0.5 -59 0.5 0 0" >/dev/null; r "mmocore admin resource-stamina set BronQA 999" >/dev/null; sleep 1.2
  [[ $k != brak ]] && $X xdotool keydown $k; sleep 0.15
  $X xdotool mousedown 3; sleep 0.5
  [[ $k != brak ]] && $X xdotool keyup $k       # puszczony kierunek: wariant ma zostac z chwili wcisniecia PPM
  sleep 0.6; $X import -window root /review/captures/$I-$k.png
  n0=$(wc -l < $L); $X xdotool mouseup 3; sleep 0.8
  echo "$k: $(tail -n +$((n0+1)) $L | grep -oE 'MOCNY typ=[A-Z_]+ ogniwo=0 wariant=[a-z_]+' | head -1 | sed 's/.*wariant=//')"
done
