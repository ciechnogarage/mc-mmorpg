#!/bin/bash
# Cofniecie naprawy kreatora: poprzedni jar i config (kopie z wdroz.sh w pliki/), bez skryptu karty. Restart world1.
set -u
W=/opt/docker/mc/data/world1; B=/opt/docker/mc/backups/kreator-naprawa-2026-09-30/kreator; C=/opt/docker/mc/docker-compose.yml
[ -f $B/pliki/config.yml.przed ] || { echo "brak kopii — wdroz.sh nie byl uruchomiony"; exit 1; }
OLD=$(ls $B/pliki/character-creator-*.jar | head -1); [ -n "$OLD" ] || { echo "brak starego jara"; exit 1; }
docker compose -f $C stop world1
cp -a $B/pliki/config.yml.przed $W/plugins/CharacterCreator/config.yml
rm -f $W/tools/klasy-rasowe/karta_kreatora.py
rm -f $W/plugins/character-creator-*.jar && cp $OLD $W/plugins/
docker compose -f $C start world1
echo "wycofano do $(basename $OLD)"
