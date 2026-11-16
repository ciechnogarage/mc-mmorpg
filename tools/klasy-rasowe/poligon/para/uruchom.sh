#!/usr/bin/env bash
# Uruchamia scenariusz para-botow.js na klonie i robi zrzuty kamery (mc-client-klasy) na liniach KADR.
# Wynik: qa/kadry/NNN.jpg + qa/kadry/kadry.jsonl (nr, opis, czas) + qa/para.log (pełny dziennik).
J=/home/dev/.claude/jobs/51c41ca8/tmp; Q=$J/qa; mkdir -p $Q/kadry; : > $Q/para.log
RP=$(docker inspect klasy-etap0 --format '{{range .Config.Env}}{{println .}}{{end}}' | grep RCON_PASSWORD | cut -d= -f2)
docker rm -f para >/dev/null 2>&1
docker run -d --name para --network klasy-net -e TYLKO="${TYLKO:-}" -e MC_HOST=10.99.41.2 -e RCON_PASS=$RP -v $J/sonda/para:/s -v $J/sonda/para:/p \
  -v /home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval:/e -v /opt/docker/mc/forge/gracz:/g node:20 node /s/para-botow.js >/dev/null
n=$(ls $Q/kadry/*.jpg 2>/dev/null | wc -l)
docker logs -f para 2>&1 | while IFS= read -r l; do
  echo "$l" >> $Q/para.log
  case "$l" in KADR*)
    n=$((n+1)); f=$(printf '%03d' $n); sleep 0.8
    docker exec -e DISPLAY=:99 mc-client-klasy import -window root /review/k$f.png 2>/dev/null && \
      docker exec mc-client-klasy convert /review/k$f.png -resize 800x -quality 72 /review/$f.jpg && mv $Q/captures/$f.jpg $Q/kadry/$f.jpg && rm -f $Q/captures/k$f.png && \
      python3 -c "import json,sys,time;print(json.dumps({'nr':'$f','opis':sys.argv[1][5:],'czas':time.strftime('%H:%M:%S')},ensure_ascii=False))" "$l" >> $Q/kadry/kadry.jsonl ;;
  esac
done
echo "KONIEC" >> $Q/para.log
