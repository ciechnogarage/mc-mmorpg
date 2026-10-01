#!/usr/bin/env python3
"""Zrzut powierzchni swiata z plikow regionow (tylko odczyt) -> teren.json.gz.

Dla kazdej kolumny: wierzchni blok (z heightmapy WORLD_SURFACE), jego y i glebokosc wody.
Uzycie: teren.py <katalog region> <wyjscie.json.gz>
"""
import sys, os, json, gzip
sys.path.insert(0, "/opt/docker/mc/forge/tools")
from mca import Region, _section_block

WODY = {"minecraft:water", "minecraft:bubble_column", "minecraft:kelp", "minecraft:kelp_plant",
        "minecraft:seagrass", "minecraft:tall_seagrass"}
PRZEZROCZYSTE = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:glass",
                 "minecraft:barrier", "minecraft:light"}

def heightmap(longs, min_y):
    out, bits, per = [], 9, 7
    for i in range(256):
        lo, sh = divmod(i, per)
        out.append(((longs[lo] >> (sh * bits)) & 0x1FF) + min_y)
    return out

def main(regdir, wyj):
    kol = {}
    for fn in sorted(os.listdir(regdir)):
        if not fn.endswith(".mca"): continue
        r = Region(os.path.join(regdir, fn))
        for cx, cz in r.chunks():
            ch = r.chunk(cx & 31, cz & 31)
            if not ch or ch.get("Status") not in ("minecraft:full", "full"): continue
            hm = (ch.get("Heightmaps") or {}).get("WORLD_SURFACE")
            if not hm: continue
            min_y = ch.get("yPos", -4) * 16
            secs = {s.get("Y"): s for s in ch.get("sections", [])}
            hs = heightmap(hm, min_y)
            def blok(lx, y, lz):
                s = secs.get(y >> 4)
                return _section_block(s, lx, y & 15, lz) if s else None
            for lz in range(16):
                for lx in range(16):
                    y = hs[lz * 16 + lx] - 1
                    nm = blok(lx, y, lz)
                    while nm in PRZEZROCZYSTE and y > min_y:
                        y -= 1; nm = blok(lx, y, lz)
                    glab = 0
                    if nm in WODY:
                        yy = y
                        while nm in WODY and yy > min_y and glab < 32:
                            yy -= 1; glab += 1; nm2 = blok(lx, yy, lz); nm = nm2
                        nm = "minecraft:water"
                    kol[f"{cx*16+lx},{cz*16+lz}"] = [nm, y, glab]
    with gzip.open(wyj, "wt") as f: json.dump(kol, f)
    from collections import Counter
    c = Counter(v[0] for v in kol.values())
    print(len(kol), "kolumn")
    for k, n in c.most_common(60): print(n, k)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
