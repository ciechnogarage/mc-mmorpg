#!/usr/bin/env bash
# Generator -> pliki MM na KLON bagna-test -> mm reload (+ parser logu: nowe bledy wzgledem bazy)
set -euo pipefail
K=/opt/docker/mc/bagna-test/data; B=/opt/docker/mc/data/world1/output/lochy/bagna/mm
/home/dev/Desktop/Przemek/MC/tools/bagna/uruchom.sh | tail -1
# arena: wersja z widoczna baza (bot bije encje bazowa, nie hitbox ME) - patrz BAZA w mm_bagna.py
B2=$B/arena_baza_widoczna; mkdir -p $B2; BAZA_WIDOCZNA=1 OUT=$B2 /home/dev/Desktop/Przemek/MC/tools/bagna/uruchom.sh | tail -1; B=$B2
cp "$B/Bagna_mobs.yml" "$K/plugins/MythicMobs/mobs/"; cp "$B/Bagna_skills.yml" "$K/plugins/MythicMobs/skills/"
L=$K/logs/latest.log; n0=$(wc -l < "$L")
docker exec bagna-test rcon-cli "mm reload" >/dev/null; sleep 6
tail -n +$n0 "$L" | grep -E "WARN|ERROR|Failed to load|Could not find|Configuration Error" | grep -v "BTEST" | grep -i -B1 -A3 "bagna" | head -20 || true   # Zaklecia wylaczone na klonie -> ich bledy ignorujemy
