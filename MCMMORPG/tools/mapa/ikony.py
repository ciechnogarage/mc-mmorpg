#!/usr/bin/env python3
"""Ikony znacznikow dla pelnej mapy: plugins/Mapa/ikony/<nazwa>.png (16x16 RGBA z konturem).

ikony.py <znaczniki.json> <wyjscie plugins/Mapa>   — nazwa pliku = czesc po ':' / ostatnim '/'
"""
import sys, os, json
from PIL import Image
import kafle

def nazwa_pliku(ik): return ik.split(":")[-1].split("/")[-1]

def main(zn, wyj):
    d = os.path.join(wyj, "ikony"); os.makedirs(d, exist_ok=True)
    for z in json.load(open(zn)):
        if z.get("ikona"):
            kafle.ikona(z["ikona"], 14).save(os.path.join(d, nazwa_pliku(z["ikona"]) + ".png"))
    print("ikony:", sorted(os.listdir(d)))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
