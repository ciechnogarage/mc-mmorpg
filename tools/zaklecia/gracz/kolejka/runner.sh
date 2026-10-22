#!/bin/bash
# Kolejka dla agentow Codex (sandbox bez dockera). Zgodna z /home/dev/.claude/CLAUDE.md: klon tylko z
# /opt/docker/mc/narzedzia/klon.sh (klon-zaklecia), bot tylko przez /opt/docker/mc/narzedzia/bot/run.sh.
# Plik kolejka/<nazwa>.req, pierwsza linia:
#   test <filtr>   -> harness bot/run.js <filtr> przez wspolny bot (kontener bot-zaklecia-*, 512 MB)
#   build          -> build Zaklecia (zaklecia-naprawy) + KsiegaPostaci (ksiega-naprawy-zaklecia), jary + nakladka na klon, restart
#   build bazowy   -> jary z world1 + oryginaly plikow nakladki, restart (porownania A/B)
# Wynik: <nazwa>.out (naglowek: md5 jarow), .serwer.log, .done. Odmowa przy <6 GB dysku / <5 GB RAM / bez klona.
D=$(dirname "$(readlink -f "$0")"); G=$(dirname "$D")
K=klon-zaklecia; C=/opt/docker/mc/klony/zaklecia/data; W=/opt/docker/mc/data/world1/plugins
N=/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-zaslanianie/MCMMORPG/plugin-src/zaklecia
KS=/home/dev/Desktop/Przemek/MC/.claude/worktrees/ksiega-zaklecia-lewa/MCMMORPG/plugin-src/ksiega-postaci
KONIEC=$(( $(date +%s) + 10*3600 ))
jar() { j=$(ls "$C"/plugins/zaklecia-*.jar 2>/dev/null | head -1); echo "JAR $(basename "$j") md5=$(md5sum "$j" | cut -c1-12) KSIEGA md5=$(md5sum "$C/plugins/ksiega-postaci-0.1.0.jar" | cut -c1-12)"; }
czekaj_done() { L=$C/logs/latest.log; for i in $(seq 1 90); do sleep 4; grep -q 'Done (' "$L" 2>/dev/null && [ "$(stat -c %i "$L")" != "$1" ] && return 0; done; return 1; }
restart() { local ino; ino=$(stat -c %i "$C/logs/latest.log" 2>/dev/null); docker stop "$K" >/dev/null; "$@"; docker start "$K" >/dev/null
  if czekaj_done "$ino"; then echo "KLON START OK"; return 0; else echo "KLON NIE WSTAL"; return 3; fi; }
wgraj_bazowy() { rm -f "$C"/plugins/zaklecia-*.jar; cp "$G"/naprawy/bazowy/zaklecia-*.jar "$C/plugins/"
  cp "$G"/naprawy/bazowy/ksiega-postaci-0.1.0.jar "$C/plugins/"; cp "$W/Zaklecia/config.yml" "$C/plugins/Zaklecia/config.yml"
  (cd "$G/naprawy/plugins-overlay" && find . -type f) | while read -r f; do
    if [ -f "$W/$f" ]; then cp "$W/$f" "$C/plugins/$f"; echo "przywrocony oryginal $f"; else rm -f "$C/plugins/$f"; fi; done; }
wgraj_nowy() { rm -f "$C"/plugins/zaklecia-*.jar; cp "$N"/build/libs/zaklecia-*.jar "$C/plugins/"
  cp "$KS/build/libs/ksiega-postaci-0.1.0.jar" "$C/plugins/ksiega-postaci-0.1.0.jar"
  [ -n "$(ls -A "$G/naprawy/plugins-overlay" 2>/dev/null)" ] && cp -r "$G/naprawy/plugins-overlay/." "$C/plugins/" && echo "nakladka skopiowana"; }
while [ "$(date +%s)" -lt "$KONIEC" ]; do
  for r in "$D"/*.req; do
    [ -e "$r" ] || continue
    n=$(basename "$r" .req); linia=$(head -1 "$r"); mv "$r" "$D/$n.wtoku"; out="$D/$n.out"; kod=9
    rodzaj=$(echo "$linia" | awk '{print $1}'); f=$(echo "$linia" | cut -s -d' ' -f2- | tr -cd 'A-Za-z0-9_,-' | head -c 300)
    dysk=$(df -BG --output=avail / | tail -1 | tr -dc 0-9); ram=$(free -g | awk '/Mem:/{print $7}')
    echo "$(date '+%F %T') start $n $rodzaj '$f' (dysk ${dysk}G ram ${ram}G load $(cut -d' ' -f1 /proc/loadavg))" >> "$D/runner.log"
    if [ "$dysk" -lt 6 ] || [ "$ram" -lt 5 ]; then echo "ODMOWA: dysk ${dysk} GB / RAM ${ram} GB ponizej progu (6/5) - zglos Claude" > "$out"; kod=8
    elif ! docker inspect -f '{{.State.Running}}' "$K" 2>/dev/null | grep -q true && [ "$rodzaj" = test ]; then echo "ODMOWA: klon $K nie dziala - zglos Claude" > "$out"; kod=7
    elif [ "$rodzaj" = build ] && [ "$f" = bazowy ]; then { echo "BUILD BAZOWY (jary world1 + oryginaly nakladki)"; restart wgraj_bazowy; kod=$?; jar; } > "$out" 2>&1
    elif [ "$rodzaj" = build ]; then
      { echo "BUILD Zaklecia $(git -C "$N" rev-parse --short HEAD) ($(git -C "$N" status --short -- src | wc -l) zmian) Ksiega $(git -C "$KS" rev-parse --short HEAD) ($(git -C "$KS" status --short -- src | wc -l) zmian)"
        bash "$N/build.sh" 2>&1 | grep -vE 'warning:|^\s+\^|^\s*return MythicLib'; b1=${PIPESTATUS[0]}
        bash "$KS/build.sh" 2>&1 | grep -vE 'warning:|^\s+\^'; b2=${PIPESTATUS[0]}
        if [ "$b1" = 0 ] && [ "$b2" = 0 ]; then restart wgraj_nowy; kod=$?
          grep -E '\[(Zaklecia|KsiegaPostaci)\]' "$C/logs/latest.log" | grep -E 'WARN|ERROR|Exception|Enabling' | head -20; jar
        else echo "BUILD FAIL (zaklecia=$b1 ksiega=$b2)"; kod=2; fi; } > "$out" 2>&1
    elif [ "$rodzaj" = test ]; then
      { jar; przed=$(wc -l < "$C/logs/latest.log")
        LIMIT_MS=1800000 timeout 1900 /opt/docker/mc/narzedzia/bot/run.sh zaklecia "$G/bot/run.js" "$f"; kod=$?
        echo "BLEDY-LOGU (Zaklecia/KsiegaPostaci, nowe od startu testu):"; tail -n +"$((przed+1))" "$C/logs/latest.log" \
          | grep -E 'zaklecia|Zaklecia|ksiega|KsiegaPostaci' | grep -E 'WARN|ERROR|Exception|at pl\.mcmmorpg' | head -40; } > "$out" 2>&1
    else echo "nieznany rodzaj '$rodzaj' (test|build)" > "$out"; fi
    tail -c 80000 "$C/logs/latest.log" > "$D/$n.serwer.log" 2>/dev/null
    echo $kod > "$D/$n.done"; rm -f "$D/$n.wtoku"
    echo "$(date '+%F %T') koniec $n kod=$kod" >> "$D/runner.log"
  done
  sleep 2
done
