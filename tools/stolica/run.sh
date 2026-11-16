#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
set -euo pipefail
base=/opt/docker/mc/data/world1
eval_dir=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
secret=$(grep -A4 'velocity:' "$base/config/paper-global.yml" | grep 'secret:' | awk '{print $2}')
rconpw=$(grep '^rcon.password=' "$base/server.properties" | cut -d= -f2-)
docker run --rm --network mc_mmorpg-net \
  -v "$eval_dir:/e:ro" -v "$base/tools/stolica:/t:ro" -v "$base/tools/handel/eval:/h:ro" \
  -w /e -e MC_FORWARDING_SECRET="$secret" -e MC_HOST=world1 \
  -e RCON_HOST=world1 -e RCON_PW="$rconpw" \
  node:22 node /t/eval.js
