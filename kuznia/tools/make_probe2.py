#!/usr/bin/env python3
"""probe2 = MINIMALNA mutacja dzialajacego packa level_1.

Zmieniamy dokladnie trzy rzeczy, reszta zostaje bit w bit:
  1) blok wierzchni kazdego biomu -> inna welna (zeby odczyt bloku = odczyt regionu),
  2) dekoratory usuniete (kwiatki staja NA wierzchu i psulyby odczyt),
  3) obraz -> gradient BLUE 0..255 wzdluz X.
Dzieki temu roznica wyniku wzgledem level_1 moze pochodzic tylko z obrazu.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA  # noqa: E402
import json, os, shutil, struct, zlib

SRC = f"{DATA}/plugins/Iris/packs/level_1"
DST = f"{DATA}/plugins/Iris/packs/probe2"
# kolejnosc regionow z dimensions/level_1.json: void, dunstart, dunmid, dunend
WOOL = {"void": "white_wool", "dunstart": "orange_wool",
        "dunmid": "magenta_wool", "dunend": "light_blue_wool"}

shutil.rmtree(DST, ignore_errors=True)
shutil.copytree(SRC, DST)
shutil.rmtree(os.path.join(DST, ".iris"), ignore_errors=True)

# wymiar: tylko nazwa + wskazanie nowego obrazu
dp = f"{DST}/dimensions/level_1.json"
d = json.load(open(dp))
d["name"] = "Probe2"
d["regionStyle"]["imageMap"]["image"] = "probe_grad"
os.rename(dp, f"{DST}/dimensions/probe2.json")
json.dump(d, open(f"{DST}/dimensions/probe2.json", "w"), indent=1)
print("regiony w kolejnosci:", d["regions"])
print("imageMap:", d["regionStyle"]["imageMap"])

for key, wool in WOOL.items():
    p = f"{DST}/biomes/{key}.json"
    b = json.load(open(p))
    b["layers"][0]["palette"] = [{"block": f"minecraft:{wool}"}]
    b.pop("decorators", None)                      # czysty odczyt wierzchu
    json.dump(b, open(p, "w"), indent=1)
    print(f"  {key:9s} -> {wool}")

def png(path, w_, h_, rows):
    def ch(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag+data) & 0xffffffff)
    raw = b"".join(b"\x00" + r for r in rows)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(ch(b"IHDR", struct.pack(">IIBBBBB", w_, h_, 8, 2, 0, 0, 0)))
        f.write(ch(b"IDAT", zlib.compress(raw, 9)))
        f.write(ch(b"IEND", b""))

row = b"".join(bytes((0, 0, x)) for x in range(256))
png(f"{DST}/images/probe_grad.png", 256, 256, [row]*256)
print("obraz probe_grad.png: 256x256, BLUE=kolumna; centered => blok X = BLUE-128")
