#!/usr/bin/env python3
"""Instaluje Wiary na serwerze (klon albo world1). Idempotentne: drugi przebieg niczego nie dubluje.

Uzycie: python3 instaluj.py <katalog serwera> <wiary.jar> <wiara.yml> <ikona wiara.png>

1. jar pluginu (stare wiary-*.jar usuwa),
2. menu DeluxeMenus gui_menus/wiara.yml + wpis w DeluxeMenus/config.yml,
3. kafelek 'wiara' w hub_postac.yml (slot 14 — dawniej zakomentowana 'klasa'),
4. przedmiot Nexo hub_wiara w items/hub_icons.yml + tekstura item/hub/wiara.png.
Kopie zapasowe robi skrypt wdrozenia PRZED uruchomieniem tego (nie tutaj).
"""
import glob
import os
import shutil
import sys

KAFELEK = """
  'wiara':
    material: nexo-hub_wiara
    slot: 14
    update: true
    display_name: '&6&lWiara'
    lore:
      - ''
      - '&7Patron twojej postaci i jego łaska.'
      - '&7Patron: %wiary_patron_imie%'
    left_click_commands:
      - '[openguimenu] wiara'
"""

NEXO = """
hub_wiara:
  itemname: <gray>Wiara
  material: PAPER
  excludeFromInventory: true
  excludeFromCommands: true
  Pack:
    texture: nexo:item/hub/wiara
"""


def main(srv, jar, menu, ikona):
    pl = os.path.join(srv, 'plugins')
    zmiany = []
    for stary in glob.glob(os.path.join(pl, 'wiary-*.jar')):
        if os.path.basename(stary) != os.path.basename(jar):
            os.remove(stary)
            zmiany.append('usunięty ' + os.path.basename(stary))
    shutil.copy2(jar, pl)
    zmiany.append('jar ' + os.path.basename(jar))

    dm = os.path.join(pl, 'DeluxeMenus')
    shutil.copy2(menu, os.path.join(dm, 'gui_menus', 'wiara.yml'))
    cfg = os.path.join(dm, 'config.yml')
    s = open(cfg, encoding='utf-8').read()
    if '\n  wiara:\n' not in s:
        kotwica = '\n  hub_postac:\n    file: hub_postac.yml\n'
        assert kotwica in s, 'brak wpisu hub_postac w DeluxeMenus/config.yml'
        s = s.replace(kotwica, kotwica + '  wiara:\n    file: wiara.yml\n')
        open(cfg, 'w', encoding='utf-8').write(s)
        zmiany.append('DeluxeMenus/config.yml: menu wiara')

    hub = os.path.join(dm, 'gui_menus', 'hub_postac.yml')
    s = open(hub, encoding='utf-8').read()
    if "\n  'wiara':\n" not in s:
        kotwica = "\n  'zmien_postac':\n"
        assert kotwica in s, "brak kafelka 'zmien_postac' w hub_postac.yml"
        s = s.replace(kotwica, KAFELEK + kotwica, 1)
        open(hub, 'w', encoding='utf-8').write(s)
        zmiany.append('hub_postac.yml: kafelek wiara (slot 14)')

    nexo = os.path.join(pl, 'Nexo', 'items', 'hub_icons.yml')
    s = open(nexo, encoding='utf-8').read()
    if '\nhub_wiara:\n' not in s:
        open(nexo, 'a', encoding='utf-8').write(NEXO)
        zmiany.append('Nexo hub_icons.yml: hub_wiara')
    tex = os.path.join(pl, 'Nexo', 'pack', 'assets', 'nexo', 'textures', 'item', 'hub')
    shutil.copy2(ikona, os.path.join(tex, 'wiara.png'))
    zmiany.append('tekstura hub/wiara.png')
    print('\n'.join(zmiany))


if __name__ == '__main__':
    main(*sys.argv[1:5])
