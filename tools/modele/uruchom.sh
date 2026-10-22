#!/usr/bin/env bash
# Generator modelu w obrazie pixelart (numpy+Pillow) z paczka LittleRoom pod /modele (tylko do odczytu).
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root=/opt/docker/mc/data/world1
modele="/home/dev/Desktop/Przemek/MC/modele/OUTPUT — kopia (2)"
exec docker run --rm -u "$(id -u):$(id -g)" -v "$root:$root" -v /tmp/claude-1000:/tmp/claude-1000 -v "$modele:/modele:ro" -w "$(pwd)" \
  -e PYTHONPATH="$here" -e WARIANT="${WARIANT:-0}" -e PYTHONDONTWRITEBYTECODE=1 pixelart:latest python3 "$@"
