#!/usr/bin/env bash
# Nagranie z klienta QA (qa.sh): zrzuty JPG w petli w tle, potem MP4 (ffmpeg w jednorazowym kontenerze alpine).
#   nagraj.sh start <klip>      zaczyna (dziala w tle az do stop)
#   nagraj.sh stop  <klip>      konczy, sklada <wyjscie>/<klip>.mp4, kasuje klatki
# Wyjscie: $NAGRANIA (domyslnie $CLAUDE_JOB_DIR/tmp/nagrania)
set -euo pipefail
Q=${QA_DIR:-$CLAUDE_JOB_DIR/tmp/qa}; C=mc-client-zielarz; K=$2; O=${NAGRANIA:-$CLAUDE_JOB_DIR/tmp/nagrania}
case "$1" in
start)
  rm -rf "$Q/captures/klip-$K"; mkdir -p "$Q/captures/klip-$K"
  docker exec -d -e DISPLAY=:99 $C sh -c "date +%s.%N > /review/klip-$K/start; i=0; while [ ! -f /review/klip-$K/stop ]; do import -window root -quality 72 /review/klip-$K/f\$(printf %05d \$i).jpg; i=\$((i+1)); done; date +%s.%N > /review/klip-$K/koniec" ;;
stop)
  touch "$Q/captures/klip-$K/stop"; for _ in $(seq 1 30); do [[ -f "$Q/captures/klip-$K/koniec" ]] && break; sleep 0.2; done
  n=$(ls "$Q/captures/klip-$K"/f*.jpg | wc -l)
  fps=$(python3 -c "print(round($n/($(cat "$Q/captures/klip-$K/koniec")-$(cat "$Q/captures/klip-$K/start")),2))")
  mkdir -p "$O"
  docker run --rm --name "prof-zielarz-ffmpeg-$$" --memory 1g -u 0 -v "$Q/captures/klip-$K:/in:ro" -v "$O:/out" alpine:3.20 sh -c \
    "apk add -q ffmpeg >/dev/null 2>&1 && ffmpeg -loglevel error -y -framerate $fps -i /in/f%05d.jpg -vf 'scale=960:-2' -c:v libx264 -pix_fmt yuv420p -crf 27 -movflags +faststart /out/$K.mp4 && chown $(id -u):$(id -g) /out/$K.mp4"
  rm -rf "$Q/captures/klip-$K"
  echo "$K.mp4: $n klatek, $fps fps, $(du -h "$O/$K.mp4" | cut -f1)" ;;
esac
