#!/usr/bin/env python3
"""Lataka plikow world1 pod mape swiata (idempotentna). latka.py <katalog plugins> <map_id>

- MMOItems/item/miscellaneous.yml: dopisuje UI_MAPA (z ui_mapa.yml), jesli go nie ma,
- CoreTools/Scripts/stale-sloty.yml: slot 7 = UI_MAPA (wstawienie + map_id, klik/reka -> okno mapy,
  ochrona przed wyrzuceniem/druga reka/klawiszem 8/wypadnieciem),
- DeluxeMenus: config.yml (wpis mapa_swiata), hub_przygoda.yml (kafelek 'mapa', slot 15).
Kazda zmiana tylko raz - ponowne uruchomienie nic nie psuje.
"""
import sys, os, re
P, MID = sys.argv[1], int(sys.argv[2])
TU = os.path.dirname(os.path.abspath(__file__))
zm = []

def podmien(s, stare, nowe, opis):
    if nowe in s: return s
    if stare not in s: raise SystemExit("BRAK KOTWICY (%s): %r" % (opis, stare[:60]))
    zm.append(opis); return s.replace(stare, nowe, 1)

# MMOItems
f = os.path.join(P, "MMOItems/item/miscellaneous.yml"); s = open(f, encoding="utf8").read()
if not re.search(r"^UI_MAPA:", s, re.M):
    s = s.rstrip("\n") + "\n" + open(os.path.join(TU, "ui_mapa.yml"), encoding="utf8").read(); zm.append("UI_MAPA")
    open(f, "w", encoding="utf8").write(s)

# CoreTools
f = os.path.join(P, "CoreTools/Scripts/stale-sloty.yml"); s = open(f, encoding="utf8").read()
s = podmien(s, "    - script{s=stale_sloty_wstaw_sakwa} @Self\n",
            "    - script{s=stale_sloty_wstaw_sakwa} @Self\n    - script{s=stale_sloty_wstaw_mapa} @Self\n", "wstaw: mapa")
s = podmien(s, "stale_sloty_nic:\n",
"""# Slot 7 = MAPA SWIATA (2026-10-01, tools/mapa w repo MC). UI_MAPA to FILLED_MAP z MMOItems; obraz
# daje map_id=%d (plik world/data/map_%d.dat, zablokowany) dopinany vanillowym 'item modify'.
stale_sloty_wstaw_mapa:
  Conditions:
    - Checkitem{p=vanilla;slot=7;id=air} trueElse stale_sloty_wstaw_mapa_teraz
  Mechanics: []

stale_sloty_wstaw_mapa_teraz:
  Mechanics:
    - SetSlots{slots="7";source="mmoitems";type="MISCELLANEOUS";id="UI_MAPA"} @Self
    - command{c="minecraft:item modify entity {caster_name} hotbar.7 {function:'minecraft:set_components',components:{'minecraft:map_id':%d}}";runas=console;delay=1} @Self

stale_sloty_nic:
""" % (MID, MID, MID), "skrypty wstawienia mapy")
s = podmien(s, """    - Comparator{v1="{click_inventory}-{slot}";v2="PLAYER-18";op="=="} trueElse stale_sloty_klik_sakwa
""", """    - Comparator{v1="{click_inventory}-{slot}";v2="PLAYER-18";op="=="} trueElse stale_sloty_klik_sakwa
    - Comparator{v1="{click_inventory}-{slot}";v2="PLAYER-7";op="=="} trueElse stale_sloty_klik_mapa
    - Comparator{v1="{action}-{hotbar}";v2="HOTBAR_SWAP-7";op="=="} trueElse stale_sloty_klawisz_8
""", "klik: mapa")
s = podmien(s, "stale_sloty_anuluj:\n", """stale_sloty_klik_mapa:
  Conditions:
    - Checkitem{p=vanilla;slot=7;id=air} false
    - Comparator{v1="{item_first_id}";v2="UI_MAPA";op="=="} true
    - Comparator{v1="{top_inventory}";v2="CRAFTING";op="=="} falseElse stale_sloty_anuluj
  Mechanics:
    - cancelevent{}
    - command{c="dm open mapa_swiata %player_name%";runas=console;delay=1} @Self

stale_sloty_klawisz_8:
  Conditions:
    - Checkitem{p=vanilla;slot=7;id=air} false
    - Checkitem{p=mi;slot=7;type=MISCELLANEOUS;id=UI_MAPA} true
  Mechanics:
    - cancelevent{}

stale_sloty_anuluj:
""", "klik/klawisz: mapa")
# 'X trueElse S' w CoreTools = gdy X prawdziwe -> uruchom S i zakoncz; dlatego mapa to OSOBNA linia
# przed wymaganiem dziennika (zmiana linii dziennika na trueElse otwieralaby mape z dziennika).
s = podmien(s, """    - Comparator{v1="{item_first_id}";v2="UI_DZIENNIK";op="=="} true
  Mechanics:
    - cancelevent{}
    - command{c="dm open hub_glowne %player_name%";runas=console;delay=1} @Self

# --- nie""", """    - Comparator{v1="{item_first_id}";v2="UI_MAPA";op="=="} trueElse stale_sloty_reka_mapa
    - Comparator{v1="{item_first_id}";v2="UI_DZIENNIK";op="=="} true
  Mechanics:
    - cancelevent{}
    - command{c="dm open hub_glowne %player_name%";runas=console;delay=1} @Self

# mapa w rece: klik otwiera duza mape (ogladanie samej mapy w rece dziala bez klikania)
stale_sloty_reka_mapa:
  Conditions:
    - Comparator{v1="{item_first_id}";v2="UI_MAPA";op="=="} true
  Mechanics:
    - cancelevent{}
    - command{c="dm open mapa_swiata %player_name%";runas=console;delay=1} @Self

# --- nie""", "reka: mapa")
for zd in ("stale_sloty_drop", "stale_sloty_spawn_przedmiotu"):
    s = podmien(s, """%s:
  Conditions:
    - Comparator{v1="{item_first_id}";v2="UI_DZIENNIK";op="=="} trueElse stale_sloty_anuluj
""" % zd, """%s:
  Conditions:
    - Comparator{v1="{item_first_id}";v2="UI_MAPA";op="=="} trueElse stale_sloty_anuluj
    - Comparator{v1="{item_first_id}";v2="UI_DZIENNIK";op="=="} trueElse stale_sloty_anuluj
""" % zd, zd + ": mapa")
s = podmien(s, """stale_sloty_druga_reka:
  Conditions:
    - Comparator{v1="{item_second_id}";v2="UI_DZIENNIK";op="=="} trueElse stale_sloty_anuluj
""", """stale_sloty_druga_reka:
  Conditions:
    - Comparator{v1="{item_second_id}";v2="UI_MAPA";op="=="} trueElse stale_sloty_anuluj
    - Comparator{v1="{item_second_id}";v2="UI_DZIENNIK";op="=="} trueElse stale_sloty_anuluj
""", "druga reka: mapa")
open(f, "w", encoding="utf8").write(s)

# DeluxeMenus
f = os.path.join(P, "DeluxeMenus/config.yml"); s = open(f, encoding="utf8").read()
s = podmien(s, "  hub_przygoda:\n    file: hub_przygoda.yml\n",
            "  hub_przygoda:\n    file: hub_przygoda.yml\n  mapa_swiata:\n    file: mapa_swiata.yml\n", "dm config: mapa_swiata")
open(f, "w", encoding="utf8").write(s)
f = os.path.join(P, "DeluxeMenus/gui_menus/hub_przygoda.yml")
if os.path.exists(f):
    s = open(f, encoding="utf8").read()
    s = podmien(s, "  'wstecz':\n", """  'mapa':
    material: FILLED_MAP
    slot: 15
    display_name: '&e&lMapa świata'
    lore:
      - ''
      - '&7Gdzie jesteś, strefy Chowańców'
      - '&7i legendarni przeciwnicy.'
      - ''
      - '&eKliknij, aby otworzyć.'
    item_flags: [HIDE_ADDITIONAL_TOOLTIP]
    left_click_commands:
      - '[openguimenu] mapa_swiata'

  'wstecz':
""", "przygoda: kafelek mapa")
    open(f, "w", encoding="utf8").write(s)
print("zmiany:", zm or "brak (juz wdrozone)")
