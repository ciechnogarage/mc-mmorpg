#!/usr/bin/env bash
# Klient QA (prawdziwy MC 1.21.11, Xvfb) na klonie zielarza. Uzycie:
#   qa.sh start [klon=zielarz]       klient wchodzi jako QaZielarz (akceptuje pack)
#   qa.sh x <xdotool args...>         wejscie (mousemove_relative, click, mousedown, key ...)
#   qa.sh foto <nazwa>                zrzut ekranu do captures/<nazwa>.png
#   qa.sh stop                        usuwa kontener klienta
set -euo pipefail
Q=${QA_DIR:-$CLAUDE_JOB_DIR/tmp/qa}; C=mc-client-zielarz; N=${2:-zielarz}
case "$1" in
start)
  docker rm -f $C >/dev/null 2>&1 || true
  docker run -d --name $C --memory 2g --network klon-$N-net -u 1000:1000 -e MC_SERVER=klon-$N -e MC_PORT=25565 -e MC_USER=QaZielarz \
    -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro -v "$Q/instance:/mc/instance" -v "$Q/captures:/review" \
    mc-client-test:latest bash /mc/launch.sh >/dev/null
  L=/opt/docker/mc/klony/$N/data/logs/latest.log
  for i in $(seq 1 60); do grep -q "QaZielarz joined" "$L" && break
    docker exec -e DISPLAY=:99 $C sh -c 'xdotool mousemove 483 435; xdotool click --delay 200 1' 2>/dev/null || true; sleep 4; done
  grep -q "QaZielarz joined" "$L" && echo "WSZEDL" || { echo "NIE WSZEDL"; exit 1; } ;;
x) shift; docker exec -e DISPLAY=:99 $C xdotool "$@" ;;
foto) docker exec -e DISPLAY=:99 $C import -window root "/review/$2.png" ;;
stop) docker rm -f $C >/dev/null 2>&1 || true ;;
esac
