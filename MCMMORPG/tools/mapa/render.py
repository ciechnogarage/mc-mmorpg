#!/usr/bin/env python3
"""Render terenu w kolorach map vanilli z teren.json.gz.

render.py <teren.json.gz> <katalog wyjscia>
Wyjscie: teren_1x.png (1 blok = 1 px, caly wygenerowany obszar) + teren_1x.bin (bajty kolorow map).
"""
import sys, json, gzip, os
from PIL import Image
from paleta import baza, rgb

def main(wej, wyj):
    kol = json.load(gzip.open(wej, "rt"))
    xs = [int(k.split(",")[0]) for k in kol]; zs = [int(k.split(",")[1]) for k in kol]
    x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
    w, h = x1 - x0 + 1, z1 - z0 + 1
    bajty = bytearray(w * h)          # 0 = brak danych (przezroczyste na mapie)
    for k, (nm, y, glab) in kol.items():
        x, z = map(int, k.split(","))
        b = baza(nm)
        if b == 12:                   # woda: odcien z glebokosci + szachownica (jak vanilla)
            d = glab * 0.1 + ((x + z) & 1) * 0.2
            o = 2 if d < 0.5 else (0 if d > 0.9 else 1)
        else:                         # lad: porownanie z blokiem na polnoc (z-1)
            n = kol.get(f"{x},{z-1}")
            if not n or n[0] == "minecraft:water": o = 1
            else: o = 2 if y > n[1] else (1 if y == n[1] else 0)
        bajty[(z - z0) * w + (x - x0)] = b * 4 + o
    img = Image.new("RGB", (w, h), (0, 0, 0))
    px = img.load()
    for i, b in enumerate(bajty):
        if b: px[i % w, i // w] = rgb(b)
    os.makedirs(wyj, exist_ok=True)
    img.save(os.path.join(wyj, "teren_1x.png"))
    with open(os.path.join(wyj, "teren_1x.bin"), "wb") as f: f.write(bajty)
    json.dump({"x0": x0, "z0": z0, "w": w, "h": h}, open(os.path.join(wyj, "teren_1x.json"), "w"))
    print("obszar x", x0, x1, "z", z0, z1, "rozmiar", w, h)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
