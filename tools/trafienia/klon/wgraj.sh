#!/usr/bin/env bash
# Wgrywa na klon z /opt/docker/mc/narzedzia/klon.sh (nazwa: trafienia) Kosiarza (model + MM) i jar Trafien,
# wyłącza pluginy walki gracza (AnimacjeBroni, KlasyBron, Zaklecia, ksiega, MenuKey), potem restart klonu przez klon.sh.
# Uzycie: wgraj.sh   (po: klon.sh start trafienia). Klon usuwa: klon.sh usun trafienia.
# PELNA_WALKA=1 wgraj.sh: NIE wylacza pluginow walki (test zgrania z produkcyjna walka: AnimacjeBroni, DwieBronie,
#   Zaklecia, KlasyBron, Ksiega, MenuKey) - klon wtedy z --z-kreatorem (postac z klasa).
set -euo pipefail
N=trafienia; D=/opt/docker/mc/klony/$N/data; P=$D/plugins
W=/opt/docker/mc/data/world1; TR=$W/tools/trafienia
[ -d "$P" ] || { echo "BLAD: brak klonu - najpierw /opt/docker/mc/narzedzia/klon.sh start $N"; exit 1; }
mkdir -p "$P/ModelEngine/blueprints/l004"
python3 "$W/tools/bagna/klon/normalizuj_bbmodel.py" "$W/output/trafienia/kosiarz/l004_kosiarz.bbmodel" "$P/ModelEngine/blueprints/l004/l004_kosiarz.bbmodel" >/dev/null
cp "$W/output/trafienia/mm/L004_kosiarz_mobs.yml" "$P/MythicMobs/mobs/"; cp "$W/output/trafienia/mm/L004_kosiarz_skills.yml" "$P/MythicMobs/skills/"
rm -f "$P"/trafienia-*.jar; cp "$(ls -t $TR/plugin/build/libs/trafienia-*.jar | head -1)" "$P/"; rm -rf "$P/Trafienia"
cp "$TR/klon/test_zaklecia.yml" "$P/MythicMobs/skills/trafienia_test_zaklecia.yml"   # tylko klon
# lacza z innymi pluginami (galezie NIEWDROZONE): PaskiMobow czyta tagi traf_plonie/traf_zmrozony, DwieBronie znaczy lewa reke
MC=/home/dev/Desktop/Przemek/MC/.claude/worktrees
PM=$(ls -t $MC/trafienia-paski/MCMMORPG/plugin-src/paski-mobow/build/libs/paski-mobow-*.jar 2>/dev/null | head -1)
DB=$(ls -t $MC/trafienia-dwie/MCMMORPG/plugin-src/dwie-bronie/build/libs/dwie-bronie-*.jar 2>/dev/null | head -1)
[ -n "$PM" ] && { rm -f "$P"/paski-mobow-*.jar; cp "$PM" "$P/"; }
[ -n "$DB" ] && { rm -f "$P"/dwie-bronie-*.jar*; cp "$DB" "$P/"; }
if [ "${PELNA_WALKA:-0}" = 1 ]; then
  for f in "$P"/*.jar.wylaczony-na-klonie; do [ -e "$f" ] && mv "$f" "${f%.wylaczony-na-klonie}"; done; echo "PELNA_WALKA: pluginy walki wlaczone"
else for j in animacje-broni klasy-bron zaklecia ksiega-postaci menu-key; do
  f=$(ls "$P" | grep "^$j.*\.jar$" || true); [ -n "$f" ] && mv "$P/$f" "$P/$f.wylaczony-na-klonie"; done; fi
echo "wgrane: model l004_kosiarz, MM L004_kosiarz_*, $(ls $P | grep '^trafienia-')"
# restart (bez kolejki, czeka na Done) zamiast stop+start - zasada hosta po podmianie jara
/opt/docker/mc/narzedzia/klon.sh restart $N
