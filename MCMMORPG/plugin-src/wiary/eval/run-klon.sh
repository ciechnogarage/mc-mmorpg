#!/usr/bin/env bash
# Eval Wiar na klonie world1: start klona -> eval botem -> docker stop (ZAWSZE, zasada testowe-kontenery-limit).
# Uzycie: K=<katalog danych klona> run-klon.sh
# Klon: pluginy world1 bez Iris/danych graczy, kreator z galezi cc-testpostac, test-wejscie.enabled: true,
# velocity off, Wiary zainstalowane menu/instaluj.py. Env CC_TEST_WEJSCIE=1 tylko tutaj.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K=${K:?podaj K=<katalog danych klona>}
NAZWA=wiary-klon
SIEC=wiary-net
EVAL=/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/eval
RCON=/opt/docker/mc/data/world1/tools/klasy-rasowe/poligon

docker rm -f $NAZWA >/dev/null 2>&1 || true
docker network create $SIEC >/dev/null 2>&1 || true
trap 'docker stop $NAZWA >/dev/null 2>&1 || true; echo "klon zatrzymany"' EXIT
: > $K/logs/latest.log 2>/dev/null || true
docker run -d --name $NAZWA --network $SIEC --memory 3400m -v $K:/data -e CC_TEST_WEJSCIE=1 \
 -e TYPE=CUSTOM -e CUSTOM_SERVER=/data/paper-1.21.11-132.jar -e VERSION=1.21.11 -e EULA=true -e ONLINE_MODE=false \
 -e ENABLE_RCON=true -e RCON_PASSWORD=wiaryklon -e INIT_MEMORY=1G -e MAX_MEMORY=2G -e TZ=Europe/Warsaw \
 -e LEVEL_TYPE=minecraft:flat -e SPAWN_MONSTERS=false -e SPAWN_ANIMALS=false -e SPAWN_NPCS=false -e GENERATE_STRUCTURES=false \
 -e VIEW_DISTANCE=6 -e SIMULATION_DISTANCE=4 -e UID=1000 -e GID=1000 itzg/minecraft-server:latest >/dev/null
ok=0
for i in $(seq 1 300); do grep -q 'Done (' $K/logs/latest.log 2>/dev/null && { ok=1; break; }; sleep 2; done
[ $ok = 1 ] || { echo "klon nie wystartowal"; exit 1; }
echo "klon gotowy"
docker run --rm --network $SIEC -v "$EVAL:/e" -v "$RCON:/s" -v "$here:/h" -w /e \
  -e MC_HOST=$NAZWA -e RCON_PASS=wiaryklon node:20 node -e '
    const { Results } = require("/e/lib/harness");
    const r = new Results("wiary");
    require("/h/wiary-eval.js")(r)
      .catch(e => r.fail("scenariusz przerwany wyjątkiem", e.stack))
      .then(() => { r.print(); process.exit(r.failed ? 1 : 0) });
  '
