#!/usr/bin/env python3
"""Buduje pack Irisa 'probe' do zmierzenia DWOCH rzeczy naraz:

  1) Jak wartosc kanalu obrazu przeklada sie na wybor regionu (progi, wplyw `rarity`,
     normalizacja /255 vs /256).
  2) Co Iris generuje POZA zasiegiem obrazu przy tiled:false - to rozstrzyga, czy
     "wyspa w pustce" da sie zrobic sama maska, czy trzeba czyscic teren recznie.

Obraz: 256x256 px, BLUE = indeks kolumny (0..255), RED/GREEN = 0.
centered:true => piksel x odpowiada blokowi X = x-128, czyli BLUE = X+128.
Kazdy region ma inny blok wierzchni, wiec odczyt bloku = odczyt regionu.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA  # noqa: E402
import json, os, sys, struct, zlib

OUT = sys.argv[1] if len(sys.argv) > 1 else f"{DATA}/plugins/Iris/packs/probe"
RARITY = None
if len(sys.argv) > 2:                      # np. "1,1,1,4,1,1,1,1"
    RARITY = [int(v) for v in sys.argv[2].split(",")]

BLOCKS = ["white_wool","orange_wool","magenta_wool","light_blue_wool",
          "yellow_wool","lime_wool","pink_wool","gray_wool"]
N = len(BLOCKS)

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)

# --- wymiar ---
w(f"{OUT}/dimensions/probe.json", {
    "name": "Probe",
    "fluidHeight": 0,
    "bedrock": False,
    "landChance": 1,
    "regions": [f"r{i}" for i in range(N)],
    "continentalStyle": {"style": "SIMPLEX"},
    "regionStyle": {"imageMap": {
        "image": "probe_map",
        "coordinateScale": 1,
        "interpolationMethod": "NONE",
        "channel": "BLUE",
        "tiled": False,
        "centered": True}},
    "rockPalette": {"palette": [{"block": "minecraft:stone"}]},
})

# --- generator plaski (deterministyczny, bez szumu) ---
w(f"{OUT}/generators/flat.json", {
    "interpolator": {"function": "NONE", "horizontalScale": 1},
    "seed": 310,
    "composite": [{"seed": 310, "style": {"style": "FLAT"}}],
})

# --- regiony + biomy: jeden blok wierzchni na region ---
for i, blk in enumerate(BLOCKS):
    region = {"name": f"R{i}", "landBiomes": [f"b{i}"]}
    if RARITY:
        region["rarity"] = RARITY[i]
    w(f"{OUT}/regions/r{i}.json", region)
    w(f"{OUT}/biomes/b{i}.json", {
        "name": f"B{i}",
        "derivative": "PLAINS",
        "vanillaDerivative": "PLAINS",
        "layers": [{"palette": [{"block": f"minecraft:{blk}"}],
                    "minHeight": 1, "maxHeight": 1}],
        "generators": [{"generator": "flat", "min": 1, "max": 1}],
    })

# --- obraz: PNG 256x256, BLUE = numer kolumny (bez zaleznosci od PIL) ---
def png(path, w_, h_, rows):
    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff))
    raw = b"".join(b"\x00" + r for r in rows)          # filter 0 na kazdy wiersz
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", w_, h_, 8, 2, 0, 0, 0)))  # 8-bit RGB
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))

row = b"".join(bytes((0, 0, x)) for x in range(256))    # RED=0, GREEN=0, BLUE=x
png(f"{OUT}/images/probe_map.png", 256, 256, [row] * 256)

print(f"pack: {OUT}")
print(f"regionow: {N}, rarity: {RARITY or 'brak (domyslne 1)'}")
print("obraz: 256x256, BLUE=kolumna 0..255, centered => blok X = BLUE-128")
