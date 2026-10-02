#!/usr/bin/env python3
"""Latka v4: nowy opis UI_MAPA (PPM = przyblizenie, Shift+PPM = mapa na caly ekran). latka4.py <plugins>. Idempotentna.
BEZ revision-id: aktualizacja MMOItems przebudowalaby przedmiot i zgubila map_id. Stare mapy w eq poprawia plugin Mapa."""
import sys, os
P = sys.argv[1]; f = os.path.join(P, "MMOItems/item/miscellaneous.yml")
s = open(f, encoding="utf8").read()
stare = "      - '&fKlik &7— duża mapa ze znacznikami'\n"
nowe = "      - '&fPPM &7— przybliż / oddal'\n      - '&fShift+PPM &7— mapa na cały ekran'\n"
if nowe in s: print("zmiany v4: brak")
elif stare in s: open(f, "w", encoding="utf8").write(s.replace(stare, nowe)); print("zmiany v4: opis UI_MAPA")
else: raise SystemExit("BRAK KOTWICY: opis UI_MAPA")
