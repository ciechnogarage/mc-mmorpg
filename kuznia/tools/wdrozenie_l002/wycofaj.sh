#!/usr/bin/env bash
# Cofa wdroz.sh: usuwa Loch 002 z world1 i przywraca menu z kopii. Potem restart world1.
set -eu
. "$(dirname "$(readlink -f "$0")")/../kuznia.sh"   # TOOLS DATA LEVELS KONTENER
P=/opt/docker/mc/data/world1/plugins
B=$(cat /opt/docker/mc/backups/loch002-ostatnia_kopia.txt)
rm -rf "$P/MythicDungeons/maps/loch_002"
rm -f "$P/MythicMobs/mobs/Loch_002.yml" "$P/MythicMobs/skills/Loch_002.yml"
cp -a "$B/hub_przygoda.yml" "$P/DeluxeMenus/gui_menus/hub_przygoda.yml"
echo "Cofniete z kopii $B. Restart world1."
