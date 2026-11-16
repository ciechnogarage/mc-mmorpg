#!/usr/bin/env bash
# Przywraca glod: Multiverse hunger=true teraz, pliki z kopii stare/ (dzialaja po restarcie world1).
set -euo pipefail
P=$(dirname "$(realpath "$0")"); W=/opt/docker/mc/data/world1/plugins
R(){ docker exec mc-world1-1 rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g'; }
[[ -d "$P/stare" ]] || { echo "brak $P/stare - wdroz.sh nie byl uruchomiony"; exit 1; }
for f in MythicHUD/layouts/hotbar.yml MythicHUD/config.yml MythicHUD/hud_assets/hud/l1-okno.yml MMOProfiles/config.yml CMI/Settings/EventCommands.yml; do cp -p "$P/stare/$f" "$W/$f"; done
rm -f "$W"/MythicHUD/source-pack/assets/mythichud/textures/assets/l1/zbroja_skrzydlo{,_p}.png
for s in arena creator_limbo licho stolica stolica_odnowa world world_nether world_the_end wyscigi; do
  R "mv modify $s set hunger true"
done
echo "Cofniete. Pliki HUD/MMOProfiles/CMI zadzialaja po restarcie world1."
