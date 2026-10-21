#!/usr/bin/env python3
"""Generator zablokowanej mapy vanilli z wlasnym obrazem: world/data/map_<id>.dat.

mapa_dat.py <render dir> <znaczniki.json> <id> <xCenter> <zCenter> <scale> <wyjscie dir>

Obraz: teren_1x.bin z render.py zmniejszony do 128x128 (kazdy piksel = najczestszy kolor
w kwadracie 2^scale bloków, jak vanilla). locked=1 -> serwer nie rysuje terenu, ale strzalke
gracza i nazwane banery pokazuje normalnie. Plik wkladac do world/data przy zatrzymanym serwerze
albo pod id, ktorego serwer jeszcze nie wczytal (patrz README).
"""
import sys, os, json, gzip
from collections import Counter
from PIL import Image
from nbt import dumps, Byte, Int, IntArray
from paleta import rgb

DATA_VERSION = 4671   # 1.21.11, z world/level.dat

def obraz(render, xc, zc, scale):
    meta = json.load(open(os.path.join(render, "teren_1x.json")))
    src = open(os.path.join(render, "teren_1x.bin"), "rb").read()
    w, h, x0, z0 = meta["w"], meta["h"], meta["x0"], meta["z0"]
    k = 1 << scale
    lx, lz = xc - 64 * k, zc - 64 * k
    out = bytearray(128 * 128)
    for py in range(128):
        for px in range(128):
            c = Counter()
            for dz in range(k):
                z = lz + py * k + dz - z0
                if not 0 <= z < h: continue
                row = z * w
                for dx in range(k):
                    x = lx + px * k + dx - x0
                    if 0 <= x < w: c[src[row + x]] += 1
            c.pop(0, None)
            if c: out[py * 128 + px] = c.most_common(1)[0][0]
    return out

def main(render, znaczniki, mid, xc, zc, scale, wyj):
    mid, xc, zc, scale = int(mid), int(xc), int(zc), int(scale)
    kolory = obraz(render, xc, zc, scale)
    # Schemat 1.21.11 (MapBanner codec): pos = BlockPos (int[3]), color, name = komponent tekstu.
    # Wiki podaje stare klucze Pos/Color/Name - serwer je ODRZUCA ("No key pos").
    banery = [{"pos": IntArray([z["x"], z.get("y", 64), z["z"]]), "color": z.get("kolor", "white"),
               "name": z["nazwa"] if os.environ.get("NAZWY") == "tekst" else {"text": z["nazwa"]}}
              for z in json.load(open(znaczniki))]
    root = {"data": {"scale": Byte(scale), "dimension": "minecraft:overworld",
                     "trackingPosition": Byte(1), "unlimitedTracking": Byte(0), "locked": Byte(1),
                     "xCenter": Int(xc), "zCenter": Int(zc), "banners": banery, "frames": [],
                     "colors": bytes(kolory)},
            "DataVersion": Int(DATA_VERSION)}
    os.makedirs(wyj, exist_ok=True)
    with gzip.open(os.path.join(wyj, f"map_{mid}.dat"), "wb") as f: f.write(dumps(root))
    img = Image.new("RGB", (128, 128))
    img.putdata([rgb(b) if b else (0, 0, 0) for b in kolory])
    img.resize((512, 512), Image.NEAREST).save(os.path.join(wyj, f"map_{mid}_podglad.png"))
    print("map_%d.dat: srodek %d,%d skala %d (%d blokow), banery %d" % (mid, xc, zc, scale, 128 << scale, len(banery)))

if __name__ == "__main__":
    main(*sys.argv[1:8])
