#!/usr/bin/env bash
# Wycofanie Bagien: usuwa nowe sciezki z MANIFEST, przywraca PaskiMobow/config.yml i Nexo/sounds.yml z kopii, reloady.
# Uzycie: wycofaj.sh [--wykonaj]   (kopia: ostatnia_kopia.txt albo BK=...)
set -euo pipefail
TRYB="${1:---sucho}"; H=$(dirname "$(readlink -f "$0")"); T=/home/dev/Desktop/Przemek/MC/tools/bagna
BK=${BK:-$(cat "$H/ostatnia_kopia.txt")}; read CEL KONTENER < "$BK/CEL.txt"; P=$CEL/plugins; L=$CEL/logs/latest.log
RC() { docker exec "$KONTENER" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
run() { echo "+ $*"; [ "$TRYB" = "--wykonaj" ] && eval "$@" || true; }
echo "wycofanie z kopii $BK na $CEL ($KONTENER)"
while read -r f; do [ -n "$f" ] && run "rm -rf '$CEL/$f'"; done < "$BK/MANIFEST-nowe.txt"
run "cp -p '$BK/PaskiMobow-config.yml' '$P/PaskiMobow/config.yml' && cp -p '$BK/Nexo-sounds.yml' '$P/Nexo/sounds.yml'"
if [ "$TRYB" = "--wykonaj" ]; then
  n0=$(wc -l < "$L"); RC "meg reload" >/dev/null
  for i in $(seq 1 180); do tail -n +$n0 "$L" | grep -q "ModelEngine ResourcePack is ready" && break; sleep 1; done
  RC "nexo reload all" >/dev/null; sleep 25; RC "mm reload" >/dev/null; sleep 8; RC "paskimobow przeladuj"
  tail -n +$n0 "$L" > "$BK/log-wycofanie.txt"; python3 "$T/klon/log.py" "$BK/log-wycofanie.txt" "$BK/log-przed.txt.rodzaje.json" | head -12
  echo "WYCOFANE."
fi
