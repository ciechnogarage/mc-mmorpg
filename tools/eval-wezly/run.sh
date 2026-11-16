#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Eval wezlow zbierackich. Pozycza biblioteki wejscia z evalu kreatora (/e).
# Za kazdym razem SWIEZY nick: konto z istniejacym profilem nie przechodzi kreatora
# i zostaje uwiezione w limbo, przez co nie da sie dojsc do wezla.
set -euo pipefail
E=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
secret="$(grep -A4 'velocity:' /opt/docker/mc/data/world1/config/paper-global.yml | grep 'secret:' | awk '{print $2}')"
[ -n "$secret" ] || { echo "BLAD: brak sekretu forwardingu"; exit 2; }
NICK="${EVAL_NICK:-EvW$(( RANDOM % 90000 + 10000 ))}"
docker exec mc-world1-1 rcon-cli "op $NICK" >/dev/null 2>&1 || true
echo "nick testowy: $NICK"
set +e
docker run --rm --network mc_mmorpg-net -v "$E:/e" -v "$here:/w" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST="${MC_HOST:-world1}" \
  -e EVAL_NICK="$NICK" -e EVAL_ONLY="${EVAL_ONLY:-}" \
  -e EVAL_SABOTAGE="${EVAL_SABOTAGE:-}" -e EVAL_PROBY="${EVAL_PROBY:-}" \
  -e EVAL_IDX="${EVAL_IDX:-}" -e EVAL_KILLS="${EVAL_KILLS:-}" -e EVAL_TEMPO="${EVAL_TEMPO:-}" -e EVAL_TRYB="${EVAL_TRYB:-}" \
  node:20 node /w/"${EVAL_SCRIPT:-wezly.js}"
rc=$?
set -e
docker exec mc-world1-1 rcon-cli "deop $NICK" >/dev/null 2>&1 || true
exit $rc
