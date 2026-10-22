#!/usr/bin/env bash
[[ "${STARY_EVAL_E:-}" == "wiem-ze-world1" ]] || { echo "ODMOWA (audyt 20.10): stary eval /e celuje w world1 przez sekret Velocity. Przepisz na /opt/docker/mc/narzedzia/bot (require('/bot/lib')) i uruchom run.sh na klonie."; exit 3; }
# Evale klas rasowych (prototyp: Ork × Wojownik). Harness evalów kreatora postaci
# (/e) daje wejście przez forwarding Velocity i odpowiedź na resource pack.
# Konsola (RCON) służy wyłącznie do przygotowania i obserwacji — bot nie ma OP.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
eval_dir="/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval"
[ -d "$eval_dir/node_modules" ] || { echo "BLAD: brak $eval_dir/node_modules"; exit 2; }
secret="$(grep -A4 'velocity:' /opt/docker/mc/data/world1/config/paper-global.yml \
          | grep 'secret:' | awk '{print $2}')"
rcon_pass="$(docker inspect mc-world1-1 -f '{{range .Config.Env}}{{println .}}{{end}}' \
          | sed -n 's/^RCON_PASSWORD=//p')"
exec docker run --rm --network mc_mmorpg-net \
  -v "$eval_dir:/e" -v "$here:/h" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST="${MC_HOST:-world1}" \
  -e RCON_PASS="$rcon_pass" -e SCEN="${SCEN:-ork-wojownik}" -e DLUGO="${DLUGO:-0}" -e GDZIE="${GDZIE:-}" \
  -e RASA -e KLASA -e OP -e PARY \
  node:20 node -e '
    const { Results } = require("/e/lib/harness");
    const r = new Results("klasy-rasowe");
    require("/h/" + (process.env.SCEN || "ork-wojownik") + ".js")(r)
      .catch(e => r.fail("scenariusz przerwany wyjatkiem", e.stack))
      .then(() => { r.print(); process.exit(r.failed ? 1 : 0) });
  '
