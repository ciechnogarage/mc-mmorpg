#!/bin/bash
# Uruchomienie harnessu przez WSPOLNEGO bota (CLAUDE.md): tylko na klonie klon-zaklecia.
exec env LIMIT_MS=${LIMIT_MS:-1800000} /opt/docker/mc/narzedzia/bot/run.sh zaklecia "$(dirname "$(readlink -f "$0")")/run.js" "$@"
