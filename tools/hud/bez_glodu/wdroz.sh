#!/usr/bin/env bash
# Wylacza glod na world1. Uruchamiac TYLKO za zgoda wlasciciela.
#  - teraz: Multiverse hunger=false w 9 swiatach (mv modify, dziala od razu, bez restartu)
#  - pliki: kafel glodu z MythicHUD, pancerz z hotbara do okna postaci (+2 tekstury), MMOProfiles bez zapisu jedzenia per postac, CMI 'cmi feed' przy wejsciu
#    -> zadzialaja po najblizszym restarcie world1 (restart robi wlasciciel albo za jego zgoda).
set -euo pipefail
P=$(dirname "$(realpath "$0")"); W=/opt/docker/mc/data/world1/plugins
R(){ docker exec mc-world1-1 rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
PLIKI="MythicHUD/layouts/hotbar.yml MythicHUD/config.yml MythicHUD/hud_assets/hud/l1-okno.yml MMOProfiles/config.yml CMI/Settings/EventCommands.yml Multiverse-Core/worlds.yml"
mkdir -p "$P/stare"
for f in $PLIKI; do mkdir -p "$P/stare/$(dirname $f)"; cp -p "$W/$f" "$P/stare/$f"; done
python3 "$P/zmien.py" "$W"
for s in arena creator_limbo licho stolica stolica_odnowa world world_nether world_the_end wyscigi; do
  R "mv modify $s set hunger false"
done
echo "hunger: false w worlds.yml: $(grep -c 'hunger: false' $W/Multiverse-Core/worlds.yml)/9"
echo "Gotowe. HUD/MMOProfiles/CMI po restarcie world1. Cofniecie: $P/cofnij.sh"
