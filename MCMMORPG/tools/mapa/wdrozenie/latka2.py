#!/usr/bin/env python3
"""Latka v2 (plugin Mapa przejmuje mape): latka2.py <katalog plugins>. Idempotentna, wymaga latki v1.
- CoreTools: mapa w rece -> NIC (klik i Shift+klik obsluguje plugin); klik w slot 7 -> 'mapa otworz'.
- DeluxeMenus hub_przygoda: kafelek 'mapa' -> '[console] mapa otworz %player_name%'.
Stare menu DM 'mapa_swiata' zostaje zarejestrowane (nikt go nie otwiera) - do skasowania po akceptacji.
"""
import sys, os
P = sys.argv[1]; zm = []
def zamien(f, stare, nowe, opis):
    s = open(f, encoding="utf8").read()
    if nowe and nowe in s and stare not in s: return
    if stare not in s:
        if not nowe: return
        raise SystemExit("BRAK KOTWICY (%s)" % opis)
    open(f, "w", encoding="utf8").write(s.replace(stare, nowe)); zm.append(opis)
f = os.path.join(P, "CoreTools/Scripts/stale-sloty.yml")
zamien(f, '    - Comparator{v1="{item_first_id}";v2="UI_MAPA";op="=="} trueElse stale_sloty_reka_mapa\n', "", "reka: mapa -> plugin")
zamien(f, 'command{c="dm open mapa_swiata %player_name%";runas=console;delay=1}', 'command{c="mapa otworz %player_name%";runas=console;delay=1}', "klik slot 7 -> mapa otworz")
f = os.path.join(P, "DeluxeMenus/gui_menus/hub_przygoda.yml")
zamien(f, "      - '[openguimenu] mapa_swiata'", "      - '[console] mapa otworz %player_name%'", "przygoda -> mapa otworz")
print("zmiany v2:", zm or "brak")
