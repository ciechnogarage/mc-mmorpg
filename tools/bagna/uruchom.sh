#!/usr/bin/env bash
# Generator MM Bagien w kontenerze (python:3.12-slim + pyyaml): kontrakt.md, dzwieki (staging), mm (staging) + bramki.
set -euo pipefail
D=/opt/docker/mc/data/world1
MCD=/opt/docker/mc/forge/gracz/node_modules/minecraft-data/minecraft-data/data/pc/1.21.11
LR="/opt/docker/mc/modele/OUTPUT — kopia (2)/resource pack/assets/minecraft/sounds/littleroom"
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp -v "$D:$D" -v "$MCD:$MCD:ro" -v "$LR:$LR:ro" -w "$D/tools/bagna" \
  -e DZWIEKI_LR="${DZWIEKI_LR:-1}" -e BAZA_WIDOCZNA="${BAZA_WIDOCZNA:-0}" -e OUT="${OUT:-/opt/docker/mc/data/world1/output/lochy/bagna/mm}" -e UDERZENIE="${UDERZENIE:-delay}" python:3.12-slim sh -c \
  'pip install -q --user --root-user-action=ignore --disable-pip-version-check pyyaml >/dev/null 2>&1; python kontrakt.py && python dzwieki.py && python mm_bagna.py'
