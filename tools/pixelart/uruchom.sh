#!/usr/bin/env bash
# Uruchamia dowolny skrypt w obrazie 'pixelart' (Pillow juz w srodku).
# Uzycie: ./uruchom.sh <skrypt.py> [argumenty...]
#
# UWAGA: biblioteke montujemy pod /pxlib, NIE pod /lib — /lib w kontenerze to
# systemowy katalog bibliotek i jego nadpisanie zabija linker ("exec python3:
# no such file or directory").
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root=/opt/docker/mc/data/world1
exec docker run --rm -u "$(id -u):$(id -g)" \
  -v "$root:$root" -v "$here:/pxlib:ro" -w "$(pwd)" \
  -e PYTHONPATH=/pxlib pixelart:latest python3 "$@"
