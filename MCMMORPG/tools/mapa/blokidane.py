#!/usr/bin/env python3
"""Dane mapy w rece 'z blokow' dla pluginu Mapa (kontrakt: plugin-src/mapa/SPEC-v3.md, czesc A).

blokidane.py <wyjscie plugins/Mapa> <swiat>=<teren.json.gz>:<render dir> [...]
Zapisuje swiaty/<swiat>.blk (u16 LE: indeks | odcien<<14, uklad jak <swiat>.bin z render.py)
oraz wspolny tekstury.bin (indeks*48: 3 odcienie x 4x4 bajtow koloru mapy).
"""
import sys, os, json, gzip, struct
from PIL import Image, ImageEnhance
from paleta import BAZA, rgb
from tekstury import tekstura

KOLORY = [(b * 4 + o, rgb(b * 4 + o)) for b in range(1, len(BAZA)) for o in range(4)]
_najb = {}
def najblizszy(c):
    if c not in _najb:
        _najb[c] = min(KOLORY, key=lambda k: (k[1][0]-c[0])**2*3 + (k[1][1]-c[1])**2*4 + (k[1][2]-c[2])**2*2)[0]
    return _najb[c]

ODCIENIE = (0.82, 1.0, 1.12)

def main():
    wyj = sys.argv[1]; os.makedirs(os.path.join(wyj, "swiaty"), exist_ok=True)
    indeks = {None: 0}; tab = [bytes(48)]
    for a in sys.argv[2:]:
        swiat, reszta = a.split("="); tj, rdir = reszta.split(":")
        meta = json.load(open(os.path.join(rdir, "teren_1x.json")))
        w, h, x0, z0 = meta["w"], meta["h"], meta["x0"], meta["z0"]
        kol = json.load(gzip.open(tj, "rt"))
        out = bytearray(w * h * 2)
        for k, (nm, y, glab) in kol.items():
            if not nm: continue
            x, z = map(int, k.split(","))
            if glab: nm = "minecraft:water"
            if nm not in indeks:
                t = tekstura(nm)
                if t is None: indeks[nm] = 0
                else:
                    m = t.resize((4, 4), Image.BOX); b = bytearray()
                    for f in ODCIENIE:
                        mm = ImageEnhance.Brightness(m).enhance(f) if f != 1.0 else m
                        b += bytes(najblizszy(px) for px in mm.getdata())
                    indeks[nm] = len(tab); tab.append(bytes(b))
            idx = indeks[nm]
            if not idx: continue
            if glab: od = 0 if glab > 4 else 1
            else:
                n = kol.get("%d,%d" % (x, z - 1))
                od = 1 if not n or not n[0] or n[2] else (2 if y > n[1] else (0 if y < n[1] else 1))
            i = ((z - z0) * w + (x - x0)) * 2
            struct.pack_into("<H", out, i, idx | (od << 14))
        open(os.path.join(wyj, "swiaty", swiat + ".blk"), "wb").write(out)
        print(swiat, "blokow:", w * h, "tekstur lacznie:", len(tab))
    assert len(tab) < 16384
    open(os.path.join(wyj, "tekstury.bin"), "wb").write(b"".join(tab))

if __name__ == "__main__":
    main()
