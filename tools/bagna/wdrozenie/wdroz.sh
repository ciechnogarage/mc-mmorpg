#!/usr/bin/env bash
# WDROZENIE Bagien (10 mobow: definicje MM + modele ME + dzwieki + paski) na serwer. BEZ RESTARTU (reloady).
# Uzycie: wdroz.sh [--wykonaj]          (bez flagi = na sucho: tylko sprawdza i wypisuje kroki)
# Zmienne: CEL=/opt/docker/mc/data/world1  KONTENER=mc-world1-1  CZEKAJ_NA_ZERO=1 (czeka az 0 graczy - reload packa
#          Nexo wysyla graczom nowy pack)  -> na klonie: CEL=/opt/docker/mc/bagna-test/data KONTENER=bagna-test
# Uwaga: Loch 002 i wyspa Mokradel NIE istnieja na world1 - wdrazamy same definicje mobow (nic sie samo nie spawnuje).
set -euo pipefail
TRYB="${1:---sucho}"
CEL=${CEL:-/opt/docker/mc/data/world1}; KONTENER=${KONTENER:-mc-world1-1}
# zakres 03.10: piatka Lochu 002 + Kokoniatko (Wyleg). Mokradla (Rechotnik, Slimak, Ognik, Krol) odlozone: mm/odlozone/
MOBY="pijawka mulowiec kosciej szeptucha kokoniara kokoniatko"
B=/opt/docker/mc/data/world1/output/lochy/bagna; T=/home/dev/Desktop/Przemek/MC/tools/bagna; H=$(dirname "$(readlink -f "$0")")
P=$CEL/plugins; BK=${BK:-/opt/docker/mc/backups/bagna-$(date +%Y%m%d-%H%M)}
RC() { docker exec "$KONTENER" rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
run() { echo "+ $*"; [ "$TRYB" = "--wykonaj" ] && eval "$@" || true; }
L=$CEL/logs/latest.log
czekaj_log() { local wz="$1" od="$2" s="${3:-120}"; for i in $(seq 1 $s); do tail -n +"$od" "$L" | grep -q "$wz" && return 0; sleep 1; done; echo "BLAD: brak w logu: $wz"; return 1; }

# ---- 0. bramki (generator + brak wersji areny + komplet plikow)
"$T/uruchom.sh" | tail -1 | grep -q "BRAMKI: OK" || { echo "BLAD: bramki generatora"; exit 2; }
grep -q "i=false" "$B/mm/Bagna_mobs.yml" && { echo "BLAD: Bagna_mobs.yml to wersja ARENY (i=false)"; exit 2; }
for m in $MOBY; do
  [ -f "$B/modele/$m/bagna_$m.bbmodel" ] || { echo "BLAD: brak modelu $m"; exit 2; }; done
for f in "$P/MythicMobs/mobs/Bagna_mobs.yml" "$P/ModelEngine/blueprints/bagna" "$P/Nexo/pack/assets/bagna"; do
  [ -e "$f" ] && { echo "BLAD: juz istnieje $f (najpierw wycofaj.sh)"; exit 2; }; done
docker ps --format '{{.Names}}' | grep -qx "$KONTENER" || { echo "BLAD: kontener $KONTENER nie dziala"; exit 2; }
echo "bramki OK; cel $CEL ($KONTENER); kopia -> $BK"
if [ "$TRYB" != "--wykonaj" ]; then echo "(na sucho - nic nie zmieniono; uruchom z --wykonaj)"; fi

# ---- 1. zero graczy (pamiec rcon-list-pusty: pusta odpowiedz != 0)
if [ "${CZEKAJ_NA_ZERO:-1}" = 1 ] && [ "$TRYB" = "--wykonaj" ]; then
  while :; do n=$(RC "minecraft:list uuids" | sed -n 's/.*There are \([0-9]*\) of a max.*/\1/p')
    [ -n "$n" ] || { echo "BLAD: pusta odpowiedz list"; exit 4; }; [ "$n" = 0 ] && break; echo "graczy: $n - czekam 3 min"; sleep 180; done
fi

# ---- 2. kopia + manifest + log bazowy
run "mkdir -p '$BK' && cp -p '$P/PaskiMobow/config.yml' '$BK/PaskiMobow-config.yml' && cp -p '$P/Nexo/sounds.yml' '$BK/Nexo-sounds.yml'"
run "printf '%s\n' plugins/MythicMobs/mobs/Bagna_mobs.yml plugins/MythicMobs/skills/Bagna_skills.yml plugins/ModelEngine/blueprints/bagna plugins/Nexo/pack/assets/bagna > '$BK/MANIFEST-nowe.txt'"
run "echo '$CEL $KONTENER' > '$BK/CEL.txt'; cp -p '$L' '$BK/log-przed.txt'; python3 '$T/klon/log.py' '$BK/log-przed.txt' >/dev/null"

# ---- 3. pliki
run "mkdir -p '$P/ModelEngine/blueprints/bagna'"
for m in $MOBY; do
  run "python3 '$T/klon/normalizuj_bbmodel.py' '$B/modele/$m/bagna_$m.bbmodel' '$P/ModelEngine/blueprints/bagna/bagna_$m.bbmodel' >/dev/null"; done
run "cp '$B/mm/Bagna_mobs.yml' '$P/MythicMobs/mobs/' && cp '$B/mm/Bagna_skills.yml' '$P/MythicMobs/skills/'"
run "cp -r '$B/dzwieki/pack/assets/bagna' '$P/Nexo/pack/assets/'"
run "python3 '$H/dzwieki_yml.py' dodaj '$P/Nexo/sounds.yml' '$B/dzwieki/sounds.yml.bagna'"
run "python3 '$H/paski.py' dodaj '$P/PaskiMobow/config.yml' '$B/mm/paskimobow_bagna.yml'"

# ---- 4. reloady: meg -> (Nexo sam importuje pack ME) -> nexo reload all (sounds.yml) -> mm -> paski
if [ "$TRYB" = "--wykonaj" ]; then
  n0=$(wc -l < "$L"); RC "meg reload" >/dev/null; czekaj_log "ModelEngine ResourcePack is ready" "$n0" 180   # swiezy serwer: Nexo pisze tylko "ready", import robi nexo reload all
  n0=$(wc -l < "$L"); RC "nexo reload all" >/dev/null; czekaj_log "Finished generating resourcepack" "$n0" 180
  RC "mm reload" >/dev/null; sleep 8; RC "paskimobow przeladuj"
  sleep 3; tail -n +"$(wc -l < "$BK/log-przed.txt")" "$L" > "$BK/log-wdrozenie.txt"
  python3 "$T/klon/log.py" "$BK/log-wdrozenie.txt" "$BK/log-przed.txt.rodzaje.json" | tee "$BK/kontrola.txt" | head -20
  unzip -l "$P/Nexo/pack/pack.zip" | grep -cE "modelengine/models/bagna_" | sed 's/^/plikow modeli bagna w pack.zip: /'
  unzip -p "$P/Nexo/pack/pack.zip" assets/bagna/sounds.json >/dev/null 2>&1 && echo "assets/bagna/sounds.json w pack.zip: TAK" || echo "UWAGA: brak assets/bagna/sounds.json w pack.zip"
  echo "$BK" > "$H/ostatnia_kopia.txt"; echo "WDROZONE. Kopia: $BK. Wycofanie: $H/wycofaj.sh --wykonaj"
fi
