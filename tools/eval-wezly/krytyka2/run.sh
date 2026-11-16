#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Runner krytyki 2: wiele botow w jednym procesie. EVAL_NICKS="A B" (swieze nicki), EVAL_SCRIPT=plik.js
set -uo pipefail
E=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
W=/home/dev/Desktop/Przemek/MC/tools/eval-wezly
secret="$(grep -A4 'velocity:' /opt/docker/mc/data/world1/config/paper-global.yml | grep 'secret:' | awk '{print $2}')"
NICKS="${EVAL_NICKS:-K2a$((RANDOM%9000+1000)) K2b$((RANDOM%9000+1000))}"
for n in $NICKS; do docker exec mc-world1-1 rcon-cli "op $n" >/dev/null 2>&1; done
echo "nicki: $NICKS"
docker run --rm --network mc_mmorpg-net -v "$E:/e" -v "$W:/w" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST=world1 -e EVAL_NICKS="$NICKS" \
  -e EVAL_TRYB="${EVAL_TRYB:-}" -e EVAL_N="${EVAL_N:-}" -e EVAL_DEBUG="${EVAL_DEBUG:-}" -e EVAL_GM="${EVAL_GM:-}" \
  node:20 node /w/krytyka2/"${EVAL_SCRIPT}"
rc=$?
for n in $NICKS; do docker exec mc-world1-1 rcon-cli "deop $n" >/dev/null 2>&1; done
exit $rc
