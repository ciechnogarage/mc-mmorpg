#!/usr/bin/env bash
# Wdrozenie Lochu 002 na world1: mapa MD + moby/skille MM + kafelek w menu Przygoda (slot 12).
# WYMAGA RESTARTU world1 (nowa mapa MD nie wchodzi przez md reload - pamiec md-nowa-mapa-restart).
# Przed restartem: docker exec mc-world1-1 rcon-cli "minecraft:list uuids" (pusty wynik != zero graczy - pamiec rcon-list-pusty).
# Wycofanie: wycofaj.sh (ten sam katalog kopii).
set -eu
. "$(dirname "$(readlink -f "$0")")/../kuznia.sh"   # TOOLS DATA LEVELS KONTENER
SRC=$LEVELS/loch_002_md
P=/opt/docker/mc/data/world1/plugins
B=/opt/docker/mc/backups/loch002-$(date +%Y%m%d-%H%M)
mkdir -p "$B"
cp -a "$P/DeluxeMenus/gui_menus/hub_przygoda.yml" "$B/"
[ -e "$P/MythicDungeons/maps/loch_002" ] && cp -a "$P/MythicDungeons/maps/loch_002" "$B/" || true
rm -rf "$P/MythicDungeons/maps/loch_002"
cp -a "$SRC/md/loch_002" "$P/MythicDungeons/maps/loch_002"
cp "$SRC/mm/mobs/Loch_002.yml" "$P/MythicMobs/mobs/Loch_002.yml"
cp "$SRC/mm/skills/Loch_002.yml" "$P/MythicMobs/skills/Loch_002.yml"
python3 - "$P/DeluxeMenus/gui_menus/hub_przygoda.yml" <<'PY'
import sys
p = sys.argv[1]; s = open(p).read()
if "'loch_002':" not in s:
    kafel = """
  'loch_002':
    material: LILY_PAD
    slot: 12
    display_name: '&d&lLoch 002 &8» &2Bagna Zgniłych Korzeni'
    lore:
      - ''
      - '&7Topielica zamknęła śluzy rybaków'
      - '&7i zalała bagno zgniłą wodą.'
      - ''
      - '&8▸ &7Poziom: &f2–3   &8▸ &7Czas: &f10–15 min'
      - '&8▸ &7Solo albo drużyna do 4 osób'
      - '&8▸ &73 śluzy, 5 sekretów, Kokoniara, Topielica'
      - ''
      - '&aKliknij, aby wyruszyć.'
    left_click_commands:
      - '[console] md play loch_002 %player_name%'
      - '[close]'
"""
    i = s.index("\n  'zadania':")
    s = s[:i] + "\n" + kafel.rstrip("\n") + "\n" + s[i:]
    open(p, "w").write(s)
    print("kafelek dodany")
else:
    print("kafelek juz jest")
PY
echo "$B" > /opt/docker/mc/backups/loch002-ostatnia_kopia.txt
echo "Wgrane. Kopia: $B. Teraz restart world1 (za zgoda), potem sonda na world1."
