#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Evale HANDLU. Bot(y) bez OP jak gracz; RCON tylko do przygotowania danych i pomiarow.
#   ./run.sh sonda.js                      -> pojedynczy skrypt
#   EVAL_NICKS="HdA1 HdB1" ./run.sh regresja.js
set -uo pipefail
E=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
P=/opt/docker/mc/data/world1
secret="$(grep -A4 'velocity:' $P/config/paper-global.yml | grep 'secret:' | awk '{print $2}')"
rconpw="$(grep '^rcon.password=' $P/server.properties | cut -d= -f2-)"
JS="${1:-sonda.js}"
docker run --rm --network mc_mmorpg-net -v "$E:/e" -v "$here:/t" -v $P/plugins/CoreTools/plugindata:/db:ro -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST=world1 -e RCON_HOST=world1 -e RCON_PW="$rconpw" \
  -e EVAL_NICKS="${EVAL_NICKS:-}" -e EVAL_DEBUG="${EVAL_DEBUG:-}" -e EVAL_ARGS="${EVAL_ARGS:-}" \
  node:22 node --experimental-sqlite --no-warnings /t/$JS
