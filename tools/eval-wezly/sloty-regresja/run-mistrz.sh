#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Test mistrzostw na SERWERZE TESTOWYM (klon world1: kontener mc-mistrz-test, alias mistrztest).
# Bot BEZ OP. Przygotowanie danych i pomiary wylacznie przez RCON.
#   ./run-mistrz.sh                         -> pelny test
#   SABOTAZ=limit ./run-mistrz.sh           -> kontrola negatywna (plugin celowo psuje regule; test MA oblac)
#   EVAL_ONLY=zrodla,wybor ./run-mistrz.sh  -> wybrane scenariusze
#   CEL=world1 ...                          -> (po restarcie produkcji) ten sam test na world1
set -uo pipefail
E=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CEL="${CEL:-mistrztest}"
if [ "$CEL" = world1 ]; then CONT=mc-world1-1; DATA=/opt/docker/mc/data/world1; else CONT=mc-mistrz-test; DATA=/opt/docker/mc/backups/mistrzostwa-20260923/klon/data; fi
secret="$(docker inspect $CONT --format '{{range .Config.Env}}{{println .}}{{end}}' | grep ^CFG_VELOCITY_SECRET= | cut -d= -f2-)"
rconpw="$(grep '^rcon.password=' $DATA/server.properties | cut -d= -f2-)"
NICK="${EVAL_NICK:-Mst$(( RANDOM % 90000 + 10000 ))}"
SABOTAZ="${SABOTAZ:-}"
CFG=$DATA/plugins/Mistrzostwa/config.yml
if [ -n "$SABOTAZ" ]; then
  [ "$CEL" = world1 ] && { echo "SABOTAZ na produkcji zabroniony"; exit 3; }
  sed -i "s/^sabotaz-testowy:.*/sabotaz-testowy: [$SABOTAZ]/" $CFG
else
  sed -i "s/^sabotaz-testowy:.*/sabotaz-testowy: []/" $CFG
fi
docker exec $CONT rcon-cli "mistrzostwo reload" >/dev/null
docker exec $CONT rcon-cli "deop $NICK" >/dev/null 2>&1 || true
echo "cel: $CEL  nick: $NICK (bez OP)  sabotaz: ${SABOTAZ:-brak}"
docker run --rm --network mc_mmorpg-net -v "$E:/e" -v "$here:/t" -v "$DATA/plugins/MMOCore/exp-curves:/krzywe:ro" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST=$CEL -e RCON_HOST=$CEL -e RCON_PW="$rconpw" \
  -e EVAL_NICK="$NICK" -e EVAL_ONLY="${EVAL_ONLY:-}" -e EVAL_DEBUG="${EVAL_DEBUG:-}" -e SABOTAZ="$SABOTAZ" \
  node:20 node /t/${EVAL_JS:-mistrzostwa.js}
rc=$?
# sabotaz ZAWSZE zdejmowany po tescie
sed -i "s/^sabotaz-testowy:.*/sabotaz-testowy: []/" $CFG; docker exec $CONT rcon-cli "mistrzostwo reload" >/dev/null
echo "$NICK" >> "$here/.nicki-testowe"
echo "kod wyjscia: $rc   (0 = wszystko PASS)"
exit $rc
