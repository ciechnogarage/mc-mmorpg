#!/usr/bin/env bash
# Przywraca pliki z kopii zrobionej przez wdroz.sh i przeladowuje MMOItems + MythicMobs.
set -euo pipefail
K=${1:?katalog kopii backups/przedmioty-*}; P=/opt/docker/mc/data/world1/plugins
rm -f "$P/MythicMobs/droptables/Przedmioty.yml" "$P/MMOItems/modifiers/przedmioty.yml"
cp -a "$K"/. "$P"/
docker exec mc-world1-1 rcon-cli "mi reload" | tail -2
docker exec mc-world1-1 rcon-cli "mm reload" | tail -2
echo "przywrocono z $K"
