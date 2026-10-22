#!/usr/bin/env bash
# Pomiar na arenie klonu bagna-test: uruchom.sh <MOB> [nieuwazny|uwazny] [POWT] [ILE]
# Wymaga: dzialajacy bagna-test (klon/start.sh), arena_v1.nbt postawiona (forge/tools/przeciwnicy/arena.py -> place template), staging wdrozony (klon/wdroz.sh).
set -euo pipefail
B=/opt/docker/mc/data/world1/output/lochy/bagna; H=$(dirname "$(readlink -f "$0")")
docker run --rm --network bagna-net -v /opt/docker/mc/forge/gracz:/g:ro -v "$H:/h:ro" -v "$B:/b:ro" \
  -e MOB="$1" -e TRYB="${2:-nieuwazny}" -e POWT="${3:-3}" -e ILE="${4:-1}" -e MC_HOST=bagna-test -e RCON_PASS=bagnatest \
  -e ARENA="${ARENA:-0.5,-60,0.5}" -e START="${START:-}" -e SPAWN="${SPAWN:-}" -e LIMIT_S="${LIMIT_S:-60}" -e BRON="${BRON:-stone_sword}" -e TYP="${TYP:-}" -e STOJ_S="${STOJ_S:-0}" -e DEBUG="${DEBUG:-}" node:20 node /h/arena_bot.js
