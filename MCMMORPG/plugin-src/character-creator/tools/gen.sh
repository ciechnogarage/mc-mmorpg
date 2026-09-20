#!/usr/bin/env bash
# Uruchamia generatory grafiki w kontenerze — na hoscie nie ma ani pyyaml, ani modulu venv.
#
# Domyslnie wszystko laduje w ./out. Wdrozenie do packa Nexo jest OSOBNA, swiadoma
# czynnoscia — generator nie pisze juz prosto do plikow dzialajacego serwera:
#   OUT=<pack>/assets/nexo/textures/gui GLYPHS=<pack-root>/glyphs/creator_ui.yml \
#     ./gen.sh gen_creator_art.py
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="${1:?podaj skrypt: gen_creator_art.py albo gen_creator_icons.py}"
shift || true

env_args=()
for name in CFG OUT GLYPHS PACK; do
  # Pusta zmienna to NIE to samo co nieustawiona: pusta nadpisalaby domyslna sciezke
  # w skrypcie pustym ciagiem.
  if [ -n "${!name:-}" ]; then
    env_args+=(-e "$name=${!name}")
  fi
done

exec docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "$(dirname "$here"):$(dirname "$here")" \
  -w "$here" \
  -e HOME=/tmp \
  "${env_args[@]}" \
  python:3.12-slim \
  sh -c "pip install --quiet --disable-pip-version-check pyyaml pillow >/dev/null 2>&1 && exec python '$script' $*"
