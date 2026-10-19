#!/usr/bin/env bash
# Material do chlodnej recenzji zielarza: 4 nagrania z klienta QA + klatki co 0,5 s + gesto (4/s) przejscia i kluczowych gestow.
# Uzycie (klon zielarz stoi, teren gotowy): material_recenzji.sh <katalog_wyjscia>
set -euo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; O=$1; mkdir -p "$O"
export NAGRANIA=$O
R(){ docker exec klon-zielarz rcon-cli "$@" >/dev/null; }
S(){ (cd "$H" && python3 -c "
import sys; sys.path.insert(0,'.'); import scena
for f in (scena.dziurawiec, scena.kozlek, scena.liscie): f.__globals__['start']=lambda x,y,z,tp: (scena.X('click','3'), scena.czekaj(2.2))
$1"); }
"$H/qa.sh" start zielarz | tail -1; sleep 8
R "minecraft:time set 1000"; R "rzemioslo narzedzie QaZielarz sierp 1"
R "minecraft:teleport QaZielarz 46.5 -59 36.5 0 15"; sleep 2; "$H/qa.sh" x key F3+d; "$H/qa.sh" foto laka
cp "$CLAUDE_JOB_DIR/tmp/qa/captures/laka.png" "$O/swiat_laka.png"
R "minecraft:teleport QaZielarz 45.5 -59 59.5 0 15"; sleep 2; "$H/qa.sh" foto zagajnik; cp "$CLAUDE_JOB_DIR/tmp/qa/captures/zagajnik.png" "$O/swiat_zagajnik.png"
R "minecraft:teleport QaZielarz 55.5 -59 37.5 30 15"; sleep 2; "$H/qa.sh" foto staw; cp "$CLAUDE_JOB_DIR/tmp/qa/captures/staw.png" "$O/swiat_staw.png"
R "minecraft:teleport QaZielarz 45.5 -59 41.6 0 31"; sleep 2; "$H/nagraj.sh" start 1_przejscie; sleep 1.5; "$H/qa.sh" x click 3; sleep 3; "$H/qa.sh" x keydown shift; sleep 0.2; "$H/qa.sh" x keyup shift; sleep 2.5; "$H/nagraj.sh" stop 1_przejscie
R "minecraft:teleport QaZielarz 48.5 -59 38.6 0 28"; sleep 1.5; "$H/nagraj.sh" start 2_dziurawiec; sleep 1.2; S "scena.dziurawiec()"; "$H/nagraj.sh" stop 2_dziurawiec
R "minecraft:teleport QaZielarz 54.3 -59 41.5 -90 30"; sleep 1.5; "$H/nagraj.sh" start 3_kozlek; sleep 1.2; S "scena.kozlek()"; R "minecraft:teleport QaZielarz 61.5 -59 49.7 180 30"; sleep 1.5; S "scena.kozlek(False)"; "$H/nagraj.sh" stop 3_kozlek
R "minecraft:teleport QaZielarz 41.5 -59 62.6 0 30"; sleep 1.5; "$H/nagraj.sh" start 4_liscie; sleep 1.2; S "scena.liscie()"; R "minecraft:teleport QaZielarz 43.5 -59 60.6 0 30"; sleep 1.5; S "scena.liscie()"; "$H/nagraj.sh" stop 4_liscie
# suszarnia i zielnik (statyczne)
R "rzemioslo doreki QaZielarz PROF_MEDICINAL_HERB 5"; R "minecraft:teleport QaZielarz 62.5 -59 57.3 0 -40"; sleep 1; "$H/qa.sh" x click 3; sleep 1; "$H/qa.sh" foto susz; cp "$CLAUDE_JOB_DIR/tmp/qa/captures/susz.png" "$O/suszarnia.png"
R "rzemioslo zielnik QaZielarz"; sleep 1.5; "$H/qa.sh" foto ziel; cp "$CLAUDE_JOB_DIR/tmp/qa/captures/ziel.png" "$O/zielnik.png"; "$H/qa.sh" x key Escape
docker run --rm --name "prof-zielarz-klatki-$$" --memory 1g -u 0 -v "$O:/o" alpine:3.20 sh -c "apk add -q ffmpeg >/dev/null 2>&1
for k in 1_przejscie 2_dziurawiec 3_kozlek 4_liscie; do mkdir -p /o/klatki_\$k; f=2; [ \$k = 1_przejscie ] && f=6
  ffmpeg -loglevel error -y -i /o/\$k.mp4 -vf \"fps=\$f,scale=960:-1\" /o/klatki_\$k/%03d.jpg; done; chown -R $(id -u):$(id -g) /o"
ls "$O"; du -sh "$O"
