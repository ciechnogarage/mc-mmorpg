#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Test regresji sciezki profesji (dawny system slotow). Bot gra jak GRACZ BEZ OP:
# sam wpisuje /profesje i /menu, sam klika. Przygotowanie danych i POMIARY ida wylacznie
# przez RCON (konsola), nigdy przez czat bota — patrz memory/testy-z-perspektywy-gracza.
#   ./run.sh                 -> pelny test
#   EVAL_NICK=Xyz ./run.sh   -> wlasny (swiezy!) nick
set -uo pipefail
E=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
P=/opt/docker/mc/data/world1
secret="$(grep -A4 'velocity:' $P/config/paper-global.yml | grep 'secret:' | awk '{print $2}')"
rconpw="$(grep '^rcon.password=' $P/server.properties | cut -d= -f2-)"
NICK="${EVAL_NICK:-SlR$(( RANDOM % 90000 + 10000 ))}"
docker exec mc-world1-1 rcon-cli "deop $NICK" >/dev/null 2>&1 || true
echo "nick testowy: $NICK (bez OP)"
docker run --rm --network mc_mmorpg-net -v "$E:/e" -v "$here:/t" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST=world1 -e RCON_HOST=world1 -e RCON_PW="$rconpw" \
  -e EVAL_NICK="$NICK" -e EVAL_JS="${EVAL_JS:-}" -e EVAL_DEBUG="${EVAL_DEBUG:-}" \
  node:20 node /t/${EVAL_JS:-regresja.js}
rc=$?
echo "kod wyjscia: $rc   (0 = wszystko PASS)"
exit $rc
