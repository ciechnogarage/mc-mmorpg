#!/usr/bin/env bash
# Evale systemu przedmiotow na klonie itm-test (tools/przedmioty/klon.sh). Bot bez OP; konsola (RCON)
# tylko przygotowuje (klasa, poziom, wygenerowanie przedmiotu) i obserwuje.
# Uzycie: SCEN=sonda tools/przedmioty/run.sh
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
eval_dir="/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval"
exec docker run --rm --network "${SIEC:-itm-net}" \
  -v "$eval_dir:/e:ro" -v "$here:/h:ro" -v /home/dev/Desktop/Przemek/MC/tools/klasy-rasowe/rcon.js:/k/rcon.js:ro \
  -v "${WYNIKI:-/tmp}:/w" -w /e \
  -e MC_HOST="${MC_HOST:-itm-test}" -e RCON_PASS="${RCON_PASS:-itmtest}" -e SCEN="${SCEN:-sonda}" -e ARG -e N \
  node:20 node -e '
    const { Results } = require("/e/lib/harness");
    const r = new Results("przedmioty");
    require("/h/evale/" + process.env.SCEN + ".js")(r)
      .catch(e => r.fail("scenariusz przerwany wyjatkiem", e.stack))
      .then(() => { r.print(); process.exit(r.failed ? 1 : 0) });
  '
