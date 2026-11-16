#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Eval menu-huba. Reuzywa harnessu evalow kreatora postaci (/e) — tam siedzi podpisywanie
# forwardingu Velocity i odpowiedz na resource pack, czyli dwie rzeczy, bez ktorych bot
# w ogole nie wejdzie na world1.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
eval_dir="/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval"
[ -d "$eval_dir/node_modules" ] || { echo "BLAD: brak $eval_dir/node_modules"; exit 2; }
secret="$(grep -A4 'velocity:' /opt/docker/mc/data/world1/config/paper-global.yml \
          | grep 'secret:' | awk '{print $2}')"
[ -n "$secret" ] || { echo "BLAD: nie znalazlem sekretu forwardingu"; exit 2; }
exec docker run --rm --network mc_mmorpg-net \
  -v "$eval_dir:/e" -v "$here:/h" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST="${MC_HOST:-world1}" \
  node:20 node -e '
    const { Results } = require("/e/lib/harness");
    const r = new Results("diag");
    require("/h/diag-komendy.js")(r)
      .catch(e => r.fail("scenariusz przerwany wyjatkiem", e.message))
      .then(() => { r.print(); process.exit(r.failed ? 1 : 0) });
  '
