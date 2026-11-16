#!/usr/bin/env bash
# Arkusze dla wersji do porownania: output/lochy/modele-wlasne/<model>/wersje/<model>_w<N>.bbmodel ->
#   <model>_w<N>_podglad.png, <model>_w<N>_klatki.png, porownanie.png (wersje obok siebie). Na koniec w Blockbenchu
#   otwarte sa wszystkie wersje (pokaz_gotowe). Uzycie: tools/modele/wersje.sh <model>
set -euo pipefail
m=$1
W=/opt/docker/mc/data/world1; T=$W/tools; D=$W/output/lochy/modele-wlasne/$m/wersje
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
nr=()
for f in "$D"/${m}_w*.bbmodel; do
  n=$(basename "$f" .bbmodel); n=${n##*_w}; nr+=("$n")
  python3 "$T/bbrender/bb_mcp_arkusz.py" "$tmp/w$n" "$f" >/dev/null
  python3 "$T/bbrender/bb_oczy_gracza.py" "$tmp/w$n" "$f" >/dev/null
  python3 "$T/bbrender/bb_klatki.py" "$tmp/k$n" "$f" >/dev/null
  docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 "$T/bbrender/arkusz_podgladu.py" "$tmp/w$n" "$D/${m}_w${n}_podglad.png"
  docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 "$T/bbrender/arkusz_klatek.py" "$tmp/k$n" "$D/${m}_w${n}_klatki.png"
done
docker run --rm -u "$(id -u):$(id -g)" -v "$W:$W" -v "$tmp:$tmp" -w "$tmp" pixelart:latest python3 "$T/bbrender/arkusz_wariantow.py" "$tmp" "$D/porownanie.png" "${nr[@]}"
python3 "$T/bbrender/bb_sesja.py"
ls "$D"
