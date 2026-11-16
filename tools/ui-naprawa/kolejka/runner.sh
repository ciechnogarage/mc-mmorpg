#!/bin/bash
# Runner kolejki ui-naprawa: wykonuje kolejka/<nazwa>.req (jedna akcja na linie) na KLONIE klon-ui-naprawa (klon.sh) i kliencie ui-naprawa-klient.
# Wynik: <nazwa>.out + <nazwa>.done (kod). Produkcji NIE dotyka (jedyny wyjatek: odczyt "prod-czytaj <sciezka>" = cat pliku world1).
U=/home/dev/Desktop/Przemek/MC/tools/ui-naprawa; Q=$U/kolejka; D=/opt/docker/mc/klony/ui-naprawa/data
log(){ echo "$(date +%H:%M:%S) $*" >> $Q/runner.log; }
czekaj_start(){ # $1 = inode sprzed restartu
  for i in $(seq 1 180); do sleep 2; n=$(stat -c %i $D/logs/latest.log 2>/dev/null || echo x)
    [ "$n" != "$1" ] && grep -q 'Done (' $D/logs/latest.log 2>/dev/null && return 0; done; return 1; }
bledy(){ grep -E '^\[[0-9:]+\] \[[^]]*/(WARN|ERROR)\]' $D/logs/latest.log | sed -E 's/^\[[0-9:]+\] //; s/[0-9]+(\.[0-9]+)?/N/g' | cut -c1-300 | sort -u; }
akcja(){ set -- $1; local a=$1; shift; local r="$*"
  local wolne=$(df --output=avail -k / | tail -1)
  if [ "$wolne" -lt 2621440 ]; then echo "STOP: na dysku tylko $((wolne/1024)) MB wolnego (<2,5 GB) - produkcja dzieli dysk. Zakoncz prace i zglos to nadzorcy."; return 9; fi
  case "$a" in
    wgraj) # staging/plugins -> klon, jary z staging/jary podmieniaja stare o tym samym prefiksie, restart klonu
      for j in $U/staging/jary/*.jar; do [ -e "$j" ] || continue; p=$(basename "$j" | sed -E 's/-[0-9][0-9.]*(-[0-9]+)?\.jar$//'); rm -f $D/plugins/$p-[0-9]*.jar; cp "$j" $D/plugins/; echo "jar $(basename $j)"; done
      (cd $U/staging/plugins && find . -type f | while read f; do mkdir -p "$D/plugins/$(dirname "$f")"; cp "$f" "$D/plugins/$f"; echo "plik $f"; done)
      I0=$(stat -c %i $D/logs/latest.log 2>/dev/null || echo x); docker restart klon-ui-naprawa >/dev/null
      if czekaj_start "$I0"; then echo "START OK"; bledy > $Q/.bledy-teraz; echo "== NOWE WARN/ERROR vs baza:"; comm -13 $U/log-bazowy.txt $Q/.bledy-teraz | head -80; else echo "START NIEUDANY"; tail -40 $D/logs/latest.log; return 1; fi ;;
    buduj) # zaklecia|ksiega|paski|chowance
      case "$r" in zaklecia) S=/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-zaklecia/MCMMORPG/plugin-src/zaklecia;;
        ksiega) S=/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-ksiega/MCMMORPG/plugin-src/ksiega-postaci;;
        paski) S=/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-paski/MCMMORPG/plugin-src/paski-mobow;;
        chowance) S=$U/src/chowance;; *) echo "nieznany plugin"; return 1;; esac
      (cd $S && ./build.sh 2>&1 | tail -60) || return 1
      j=$(ls $S/build/libs/*.jar | grep -v -E 'sources|javadoc|plain' | head -1); [ -f "$j" ] || { echo "brak jara"; return 1; }
      p=$(basename "$j" | sed -E 's/-[0-9][0-9.]*(-[0-9]+)?\.jar$//'); mkdir -p $U/staging/jary; rm -f $U/staging/jary/$p-[0-9]*.jar; cp "$j" $U/staging/jary/; echo "OK $(basename $j)" ;;
    rcon) docker exec klon-ui-naprawa rcon-cli "$r" 2>&1 | sed 's/\x1b\[[0-9;]*m//g' ;;
    klient) case "$r" in start) $U/klient/start.sh && echo "klient start";; stop) docker rm -f ui-naprawa-klient >/dev/null && echo stop;; log) tail -40 $U/klient/instance/client.log;; esac ;;
    x) docker exec -e DISPLAY=:99 ui-naprawa-klient xdotool $r 2>&1 ;;
    zrzut) docker exec -e DISPLAY=:99 ui-naprawa-klient import -window root "/review/zrzuty/$r.png" && cp $U/klient/zrzuty/$r.png $U/zrzuty/ && echo "zrzuty/$r.png" ;;
    czekaj) sleep "$r" ;;
    log) tail -n "${r:-60}" $D/logs/latest.log ;;
    grep) grep -E -- "$r" $D/logs/latest.log | tail -80 ;;
    bot) # bot <plik.js w bot/> : node + mineflayer w kontenerze na sieci klona, limit 10 min
      docker run --rm --name ui-naprawa-bot --memory 1g --network klon-ui-naprawa-net -u 1000:1000 -v $U/bot:/bot -v /opt/docker/mc/narzedzia/bot/node_modules:/bot/node_modules:ro -w /bot -e HOME=/tmp node:22 timeout 600 node "$r" 2>&1 | tail -150 ;;
    prod-czytaj) case "$r" in /opt/docker/mc/data/world1/*) cat "$r" | head -400;; *) echo "tylko pliki world1";; esac ;;
    *) echo "nieznana akcja: $a"; return 1 ;;
  esac; }
mkdir -p $U/klient/zrzuty; log "runner start"
while true; do
  for f in $(ls $Q/*.req 2>/dev/null | sort); do n=${f%.req}; [ -e $n.done ] && continue
    log "start $(basename $n)"; : > $n.out; kod=0
    while IFS= read -r linia || [ -n "$linia" ]; do [ -z "${linia// }" ] && continue; [[ "$linia" == \#* ]] && continue
      echo ">> $linia" >> $n.out; timeout 1500 bash -c "$(declare -f akcja czekaj_start bledy log); U=$U; Q=$Q; D=$D; akcja \"\$1\"" _ "$linia" >> $n.out 2>&1 || { kod=$?; echo "!! kod $kod" >> $n.out; break; }
    done < $f; echo $kod > $n.done; log "koniec $(basename $n) kod=$kod"; done; sleep 2; done
