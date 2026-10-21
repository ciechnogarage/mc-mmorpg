#!/usr/bin/env python3
"""Render 'blokowaty': kazdy blok = jego tekstura z gory (z client.jar), pomniejszona do PX px.

tekstury.py <teren.json.gz> <x0> <z0> <x1> <z1> <px na blok> <wyjscie.png>
Tinty (trawa/liscie/woda) stale dla calej mapy; cien z roznicy wysokosci do bloku na polnoc (jak mapa vanilli).
"""
import sys, json, gzip, io, zipfile, re
from PIL import Image, ImageEnhance

JAR = "/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar"
z = zipfile.ZipFile(JAR); NAZWY = set(z.namelist())
TINT = {"trawa": (124, 189, 76), "liscie": (89, 174, 48), "woda": (74, 136, 235), "brzoza": (128, 167, 85),
        "swierk": (97, 153, 97)}
TINTOWANE = {"grass_block_top": "trawa", "short_grass": "trawa", "tall_grass_top": "trawa", "fern": "trawa",
             "vine": "liscie", "water_still": "woda", "lily_pad": "trawa", "bush": "trawa", "large_fern_top": "trawa"}
ZAMIANY = [(r"_stairs$", ""), (r"_slab$", ""), (r"_wall$", ""), (r"_fence$", "_planks"), (r"_fence_gate$", "_planks"),
           (r"_pressure_plate$", ""), (r"_carpet$", "_wool"), (r"^waxed_", ""), (r"_wood$", "_log"),
           (r"brick$", "bricks"), (r"tile$", "tiles"), (r"^(\w+)_sign$|^(\w+)_wall_sign$", "")]
ALIASY = {"water": "water_still", "lava": "lava_still", "bamboo": "bamboo_stalk", "moss_carpet": "moss_block",
          "dark_oak_stairs": "dark_oak_planks", "campfire": "campfire_log_lit", "fire": "fire_0", "magma_block": "magma",
          "grindstone": "grindstone_side", "ender_chest": "obsidian"}
_cache = {}

def tekstura(blok):
    n = (blok or "").replace("minecraft:", "")
    if n in _cache: return _cache[n]
    kand = ([ALIASY[n]] if n in ALIASY else []) + [n + "_top", n]
    m = n
    for wzor, na in ZAMIANY:
        m2 = re.sub(wzor, na, m)
        if m2 != m: m = m2; kand += [m + "_top", m]
    for pl in ("_planks",):
        if n.startswith(("oak", "spruce", "birch", "dark_oak", "jungle", "acacia", "mangrove", "cherry")): kand.append(n.split("_")[0] + pl)
    kand += [n.replace("_stairs", "s"), n.replace("_slab", "s")]
    im = None; uzyta = None
    for k in kand:
        p = "assets/minecraft/textures/block/%s.png" % k
        if p in NAZWY:
            im = Image.open(io.BytesIO(z.read(p))).convert("RGBA"); uzyta = k; break
    if im is not None:
        im = im.crop((0, 0, 16, 16))                      # animowane: pierwsza klatka
        t = TINTOWANE.get(uzyta) or ("liscie" if uzyta.endswith("_leaves") else None)
        if uzyta and uzyta.startswith("birch_leaves"): t = "brzoza"
        if uzyta and uzyta.startswith("spruce_leaves"): t = "swierk"
        if t:
            r, g, b, a = im.split(); tr, tg, tb = TINT[t]
            im = Image.merge("RGBA", (r.point(lambda v: v * tr // 255), g.point(lambda v: v * tg // 255), b.point(lambda v: v * tb // 255), a))
        tlo = Image.new("RGBA", (16, 16), (90, 140, 60, 255) if t else (110, 110, 110, 255))
        tlo.alpha_composite(im); im = tlo.convert("RGB")
    _cache[n] = im
    return im

def main(wej, x0, z0, x1, z1, px, wyj):
    x0, z0, x1, z1, px = map(int, (x0, z0, x1, z1, px))
    kol = json.load(gzip.open(wej, "rt"))
    W, H = (x1 - x0) * px, (z1 - z0) * px
    out = Image.new("RGB", (W, H), (20, 26, 34))
    male = {}
    for zz in range(z0, z1):
        for xx in range(x0, x1):
            k = kol.get("%d,%d" % (xx, zz))
            if not k or not k[0]: continue
            nm, y, glab = k
            if glab: nm = "minecraft:water"
            t = tekstura(nm)
            if t is None: continue
            if (nm, px) not in male: male[(nm, px)] = t.resize((px, px), Image.BOX)
            kaf = male[(nm, px)]
            n = kol.get("%d,%d" % (xx, zz - 1))
            if glab:
                f = max(0.72, 1.08 - glab * 0.035)
            elif n and n[0] and not n[2]:
                f = 1.12 if y > n[1] else (0.82 if y < n[1] else 1.0)
            else:
                f = 1.0
            if f != 1.0: kaf = ImageEnhance.Brightness(kaf).enhance(f)
            out.paste(kaf, ((xx - x0) * px, (zz - z0) * px))
    out.save(wyj)
    brak = sorted(k for k, v in _cache.items() if v is None)
    print("zapisano", wyj, out.size, "| bez tekstury:", brak[:15])

if __name__ == "__main__":
    main(*sys.argv[1:8])
