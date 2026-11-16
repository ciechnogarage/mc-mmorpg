#!/usr/bin/env python3
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import TOOLS, DATA  # noqa: E402
import sys; sys.path.insert(0,TOOLS)
from mca import World
W = World(f'{DATA}/pr3/region')
NAME = {"minecraft:white_wool":"void[0]","minecraft:orange_wool":"dunstart[1]",
        "minecraft:magenta_wool":"dunmid[2]","minecraft:light_blue_wool":"dunend[3]"}
Z = 0
rows = []
for x in range(-320, 321):
    y, b = W.top_block(x, Z)
    rows.append((x, y, NAME.get(b, b)))
# progi = miejsca zmiany
print(f"{'blok X':>8} {'BLUE=X+128':>11} {'Y':>4}  region")
prev = None
for x, y, r in rows:
    if r != prev:
        blue = x + 128
        inside = 0 <= blue <= 255
        print(f"{x:8d} {blue if inside else '-':>11} {str(y):>4}  {r}   <-- ZMIANA {'(w obrazie)' if inside else '(POZA obrazem)'}")
        prev = r
print()
from collections import Counter
c = Counter(r for _, _, r in rows)
print("udzial na linii Z=0 (X -320..320):")
for k, v in c.most_common(): print(f"   {k:>14}: {v} blokow")
print()
print("kontrola brzegow obrazu (obraz pokrywa X -128..127):")
for x in (-131,-130,-129,-128,-127,126,127,128,129,130,200,319):
    y,b = W.top_block(x,0); print(f"   X={x:5d} BLUE={x+128 if 0<=x+128<=255 else '-':>4} -> {NAME.get(b,b)} (y={y})")
