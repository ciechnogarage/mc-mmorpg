#!/usr/bin/env bash
# Wgrywa zbudowany loch (kuznia/levels/<pid>_md: md/<pid> + mm/{mobs,skills}) na klon $KUZNIA.
# Uzycie: wgraj_klon.sh loch_002   -> potem klon.sh restart $KUZNIA (nowa mapa MD wchodzi tylko po restarcie).
set -eu
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
PID=${1:?uzycie: wgraj_klon.sh <loch_NNN>}
SRC=$LEVELS/${PID}_md
P=$DATA/plugins
[ -d "$SRC/md/$PID" ] || { echo "brak $SRC/md/$PID - najpierw build_${PID/_/}_md.py"; exit 2; }
case "$P" in /opt/docker/mc/klony/*) ;; *) echo "ODMOWA: $P nie jest klonem"; exit 3;; esac
rm -rf "$P/MythicDungeons/maps/$PID"
cp -a "$SRC/md/$PID" "$P/MythicDungeons/maps/$PID"
for k in mobs skills; do for f in "$SRC"/mm/$k/*.yml; do [ -e "$f" ] && cp "$f" "$P/MythicMobs/$k/"; done; done
ls "$P/MythicDungeons/maps/$PID" "$SRC"/mm/*/
echo "Wgrane na $KONTENER. Teraz: /opt/docker/mc/narzedzia/klon.sh restart $KUZNIA"
