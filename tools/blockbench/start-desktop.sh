#!/usr/bin/env bash
set -euo pipefail
export DISPLAY="${DISPLAY:-:1}"
export XAUTHORITY="${XAUTHORITY:-/home/dev/.Xauthority}"
exec /home/dev/.local/opt/blockbench-5.1.6/blockbench \
  --userData /home/dev/.local/share/blockbench-codex/profile "$@"
