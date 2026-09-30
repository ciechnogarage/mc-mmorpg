#!/usr/bin/env bash
# Uruchom skrypt evalu wyboru postaci w kontenerze node na sieci klonu.
# Uzycie: KLON_DATA=<katalog danych klonu> NODE_MODULES=<katalog node_modules z mineflayer 4.39> ./run.sh scen.js
# Zmienne: OD/DO (setup.js — zakres postaci), QA (nick, domyslnie WyborQA).
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${KLON_DATA:?ustaw KLON_DATA (dane klonu, np. .../tmp/wybor/data)}"
: "${NODE_MODULES:?ustaw NODE_MODULES (mineflayer 4.39 + minecraft-protocol 1.68 + prismarine-nbt)}"
docker run --rm --network "${KLON_SIEC:-cc-nap-wybor-net}" -u 1000:1000 -e MC_HOST="${KLON_HOST:-cc-nap-wybor}" \
  -e CLONE_LOG=/clone/logs/latest.log -e OD -e DO -e QA -e SCEN \
  -v "$here":/e -v "$NODE_MODULES":/e/node_modules:ro \
  -v "$KLON_DATA/logs":/clone/logs:ro -v "$KLON_DATA":/clone-data:ro -w /e node:20 node "$@"
