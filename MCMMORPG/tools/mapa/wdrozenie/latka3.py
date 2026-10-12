#!/usr/bin/env python3
"""Latka v3: kafelek 'Mapa swiata' w Q -> Przygoda otwiera MAPE NA CALY EKRAN. latka3.py <plugins>. Idempotentna."""
import sys, os
P = sys.argv[1]; f = os.path.join(P, "DeluxeMenus/gui_menus/hub_przygoda.yml")
s = open(f, encoding="utf8").read(); zm = []
for stare, nowe in [
    ("      - '[console] mapa otworz %player_name%'", "      - '[console] mapa pelna %player_name%'"),
    ("      - '&7Gdzie jesteś, strefy Chowańców'\n      - '&7i legendarni przeciwnicy.'",
     "      - '&7Mapa na cały ekran: myszą celujesz,'\n      - '&7PPM przeciągasz, kółkiem przybliżasz.'\n      - '&7Shift — wyjście.'"),
]:
    if nowe in s: continue
    if stare not in s: raise SystemExit("BRAK KOTWICY: " + stare[:50])
    s = s.replace(stare, nowe); zm.append(nowe.strip()[:40])
open(f, "w", encoding="utf8").write(s); print("zmiany v3:", zm or "brak")
