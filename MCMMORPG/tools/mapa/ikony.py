#!/usr/bin/env python3
"""Ikony znacznikow dla pelnej mapy: plugins/Mapa/ikony/<nazwa>.png (16x16 RGBA z konturem).

ikony.py <znaczniki.json> <wyjscie plugins/Mapa>   — nazwa pliku = czesc po ':' / ostatnim '/'
"""
import sys, os, json
from PIL import Image
import kafle

def nazwa_pliku(ik): return ik.split(":")[-1].split("/")[-1]

def main(zn, wyj, podkatalog="ikony", rozmiar=14):
    """rozmiar 14 -> 16x16 (pelna mapa), 9 -> 11x11 (minimapa NMinimap: plugins/NMinimap/markers)."""
    d = os.path.join(wyj, podkatalog); os.makedirs(d, exist_ok=True)
    for z in json.load(open(zn)):
        if z.get("ikona"):
            kafle.ikona(z["ikona"], rozmiar).save(os.path.join(d, nazwa_pliku(z["ikona"]) + ".png"))
    print("ikony:", sorted(os.listdir(d)))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], *(sys.argv[3:4]), *([int(sys.argv[4])] if len(sys.argv) > 4 else []))
