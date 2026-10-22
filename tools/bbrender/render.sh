#!/usr/bin/env bash
# Uruchamia render_bbmodel.py w obrazie pixelart z paczka modeli zamontowana tylko do odczytu pod /modele.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root=/opt/docker/mc/data/world1
modele="/home/dev/Desktop/Przemek/MC/modele/OUTPUT — kopia (2)"
exec docker run --rm -u "$(id -u):$(id -g)" -v "$root:$root" -v "$modele:/modele:ro" -w "$(pwd)" -e WIDOKI="${WIDOKI:-}" \
  pixelart:latest python3 "$here/render_bbmodel.py" "$@"
