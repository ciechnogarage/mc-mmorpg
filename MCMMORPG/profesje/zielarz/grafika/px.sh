#!/usr/bin/env bash
# Pillow w obrazie pixelart (warsztat world1/tools/pixelart pod /pxlib); nazwa + limit pamieci (zasady hosta)
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec docker run --rm --name "pixel-zielarz-$$" --memory 512m -u "$(id -u):$(id -g)" -v "$H:$H" -v /opt/docker/mc/data/world1/tools/pixelart:/pxlib:ro -w "$H" -e PYTHONPATH=/pxlib pixelart:latest python3 "$@"
