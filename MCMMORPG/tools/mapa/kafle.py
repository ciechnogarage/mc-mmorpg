#!/usr/bin/env python3
"""Kafelki atlasu + dane mapy w rece dla pluginu Mapa.

kafle.py <wyjscie> <swiat>=<render dir>:<bpp,bpp,bpp> [...]   (znaczniki z config.yml pluginu: --znaczniki plik.json)

Wyjscie:
  plugins/Mapa/swiaty/<swiat>.bin|.json   bajty kolorow map 1 blok = 1 bajt (mapa w rece)
  plugins/Mapa/kafle.json                 siatka kafelkow (kontrakt w plugin-src/mapa/SPEC.md)
  pack/assets/mapa/...                    tekstury 18x18, modele (gui scale 1.125 = 1 teksel na px GUI,
                                          kafelki stykaja sie bez szczelin), definicje z select na
                                          custom_model_data g0..g7 -> composite(kafelek, strzalka)
"""
import sys, os, json, math, shutil
from collections import Counter
from PIL import Image, ImageDraw
from paleta import rgb

K = 18                                   # px kafelka = rozstaw slotow
PERGAMIN = (196, 178, 136)               # brak danych
KOLOR_ZN = {"start": (245, 245, 245), "legendarny": (214, 52, 52), "strefa": (120, 214, 60),
            "miasto": (240, 200, 60), "loch": (170, 90, 220)}
KONTUR = (24, 20, 16)

def zapisz_json(p, d):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(d, open(p, "w"), separators=(",", ":"))

def model(p, tekstura, z=0.0):
    zapisz_json(p, {"parent": "minecraft:item/generated", "textures": {"layer0": tekstura},
                    "display": {"gui": {"rotation": [0, 0, 0], "translation": [0, 0, z], "scale": [1.125, 1.125, 1.125]}}})

def kafel_def(model_kafla, strzalki):
    cases = [{"when": "g%d" % d, "model": {"type": "minecraft:composite", "models": [
        {"type": "minecraft:model", "model": model_kafla},
        {"type": "minecraft:model", "model": "mapa:item/strzalka/g%d" % d}]}} for d in range(8)] if strzalki else []
    baza = {"type": "minecraft:model", "model": model_kafla}
    # oversized_in_gui: od 1.21.6 GUI przycina przedmiot do 16x16; bez tej flagi skala 1.125 = szczeliny
    if not cases: return {"model": baza, "oversized_in_gui": True}
    return {"oversized_in_gui": True, "model": {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
                      "cases": cases, "fallback": baza}}

def strzalki(pack):
    # strzalka gracza (jak kursor mapy vanilli: bialy grot z ciemnym konturem), 8 kierunkow
    # g0 = poludnie (+z, w dol ekranu), g2 = zachod (w lewo), g4 = polnoc, g6 = wschod
    for d in range(8):
        S = 18 * 8; im = Image.new("L", (S, S), 0); dr = ImageDraw.Draw(im)
        kat = math.radians(d * 45)                       # 0 = w dol
        def pkt(dx, dy):                                 # dy dodatnie = w strone grotu (jednostki = px kafla)
            x = dx * math.cos(kat) - dy * math.sin(kat); y = dx * math.sin(kat) + dy * math.cos(kat)
            return (S / 2 + x * 8, S / 2 + y * 8)
        dr.polygon([pkt(0, 7), pkt(-5, -5), pkt(0, -2.5), pkt(5, -5)], fill=255)
        maska = im.resize((K, K), Image.BOX).point(lambda v: 255 if v > 100 else 0)
        # kontur 1 px: maska rozszerzona o sasiadow
        kont = Image.new("L", (K, K), 0)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                kont.paste(255, (dx, dy), maska)
        im = Image.new("RGBA", (K, K), (0, 0, 0, 0))
        im.paste((24, 20, 16, 255), (0, 0), kont); im.paste((255, 255, 255, 255), (0, 0), maska)
        p = os.path.join(pack, "assets/mapa/textures/item/strzalka/g%d.png" % d); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
        model(os.path.join(pack, "assets/mapa/models/item/strzalka/g%d.json" % d), "mapa:item/strzalka/g%d" % d, z=0.5)


def ikony(pack):
    zl, ko = (226, 190, 110, 255), (30, 24, 16, 255)
    def rysuj(nazwa, f):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); dr = ImageDraw.Draw(im); f(dr)
        # kontur: rozmyj alfe o 1 px
        al = im.getchannel("A"); kont = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            kont.paste(Image.new("RGBA", (16, 16), ko), (dx, dy), al)
        kont.alpha_composite(im); im = kont
        p = os.path.join(pack, "assets/mapa/textures/item/ui/%s.png" % nazwa); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
        zapisz_json(os.path.join(pack, "assets/mapa/models/item/ui/%s.json" % nazwa),
                    {"parent": "minecraft:item/generated", "textures": {"layer0": "mapa:item/ui/%s" % nazwa}})
        zapisz_json(os.path.join(pack, "assets/mapa/items/ui/%s.json" % nazwa),
                    {"model": {"type": "minecraft:model", "model": "mapa:item/ui/%s" % nazwa}})
    rysuj("lewo",  lambda d: (d.polygon([(3,8),(9,2),(9,6),(13,6),(13,10),(9,10),(9,14)], fill=zl)))
    rysuj("prawo", lambda d: (d.polygon([(13,8),(7,2),(7,6),(3,6),(3,10),(7,10),(7,14)], fill=zl)))
    rysuj("gora",  lambda d: (d.polygon([(8,3),(2,9),(6,9),(6,13),(10,13),(10,9),(14,9)], fill=zl)))
    rysuj("dol",   lambda d: (d.polygon([(8,13),(2,7),(6,7),(6,3),(10,3),(10,7),(14,7)], fill=zl)))
    rysuj("plus",  lambda d: (d.rectangle([7,3,9,13], fill=zl), d.rectangle([3,7,13,9], fill=zl)))
    rysuj("minus", lambda d: d.rectangle([3,7,13,9], fill=zl))
    rysuj("srodek",lambda d: (d.ellipse([3,3,13,13], outline=zl, width=2), d.rectangle([7,7,9,9], fill=zl)))
    rysuj("pelny", lambda d: [d.rectangle(r, fill=zl) for r in ([2,2,6,3],[2,2,3,6],[10,2,14,3],[13,2,14,6],[2,13,6,14],[2,10,3,14],[10,13,14,14],[13,10,14,14])])

JAR = "/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar"
NEXO_TEX = "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/chowance"
_ikony = {}
def ikona(nazwa, rozmiar):
    """Ikona znacznika: nexo:chw_* (pack Nexo) albo mc:item/* (client.jar), przeskalowana z konturem."""
    klucz = (nazwa, rozmiar)
    if klucz in _ikony: return _ikony[klucz]
    import zipfile, io
    if nazwa.startswith("nexo:"):
        im = Image.open(os.path.join(NEXO_TEX, nazwa[5:] + ".png")).convert("RGBA")
    else:
        im = Image.open(io.BytesIO(zipfile.ZipFile(JAR).read("assets/minecraft/textures/%s.png" % nazwa[3:]))).convert("RGBA")
    im = im.crop(im.getbbox()); bok = max(im.size); kw = Image.new("RGBA", (bok, bok)); kw.paste(im, ((bok - im.width) // 2, (bok - im.height) // 2))
    im = kw.resize((rozmiar, rozmiar), Image.NEAREST if bok <= rozmiar * 2 else Image.BOX)
    a = im.getchannel("A").point(lambda v: 255 if v > 100 else 0); im.putalpha(a)
    wyn = Image.new("RGBA", (rozmiar + 2, rozmiar + 2))
    for dx, dy in ((0,1),(2,1),(1,0),(1,2)): wyn.paste(Image.new("RGBA", im.size, KONTUR + (255,)), (dx, dy), a)
    wyn.alpha_composite(im, (1, 1)); _ikony[klucz] = wyn
    return wyn

def main():
    wyj = sys.argv[1]; argi = sys.argv[2:]
    zn = []
    if "--znaczniki" in argi:
        i = argi.index("--znaczniki"); zn = json.load(open(argi[i + 1])); del argi[i:i + 2]
    pack = os.path.join(wyj, "pack"); dane = os.path.join(wyj, "plugins/Mapa")
    shutil.rmtree(os.path.join(pack, "assets/mapa"), ignore_errors=True)
    strzalki(pack); ikony(pack)
    # pusty kafel (poza swiatem)
    im = Image.new("RGB", (K, K), PERGAMIN); dr = ImageDraw.Draw(im)
    p = os.path.join(pack, "assets/mapa/textures/item/pusty.png"); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
    model(os.path.join(pack, "assets/mapa/models/item/pusty.json"), "mapa:item/pusty")
    zapisz_json(os.path.join(pack, "assets/mapa/items/pusty.json"), {"model": {"type": "minecraft:model", "model": "mapa:item/pusty"}, "oversized_in_gui": True})
    indeks = {"swiaty": {}}; licz = 0
    for a in argi:
        swiat, reszta = a.split("="); rdir, bpps = reszta.split(":")
        meta = json.load(open(os.path.join(rdir, "teren_1x.json"))); src = open(os.path.join(rdir, "teren_1x.bin"), "rb").read()
        w, h, x0, z0 = meta["w"], meta["h"], meta["x0"], meta["z0"]
        os.makedirs(os.path.join(dane, "swiaty"), exist_ok=True)
        shutil.copy(os.path.join(rdir, "teren_1x.bin"), os.path.join(dane, "swiaty", swiat + ".bin"))
        json.dump(meta, open(os.path.join(dane, "swiaty", swiat + ".json"), "w"))
        # przytnij do danych (bez pustych brzegow)
        xs = [i % w for i, b in enumerate(src) if b]; zs = [i // w for i, b in enumerate(src) if b]
        bx0, bx1, bz0, bz1 = min(xs), max(xs), min(zs), max(zs)
        gx0, gz0 = x0 + bx0, z0 + bz0
        poz = []
        for bpp in map(int, bpps.split(",")):
            bl = K * bpp
            kol = math.ceil((bx1 - bx0 + 1) / bl); wier = math.ceil((bz1 - bz0 + 1) / bl)
            for tz in range(wier):
                for tx in range(kol):
                    im = Image.new("RGB", (K, K), PERGAMIN); px = im.load()
                    for py in range(K):
                        for pxx in range(K):
                            c = Counter()
                            for dz in range(0, bpp, max(1, bpp // 3)):
                                zz = gz0 - z0 + tz * bl + py * bpp + dz
                                if not 0 <= zz < h: continue
                                for dx in range(0, bpp, max(1, bpp // 3)):
                                    xx = gx0 - x0 + tx * bl + pxx * bpp + dx
                                    if 0 <= xx < w: c[src[zz * w + xx]] += 1
                            c.pop(0, None)
                            if c: px[pxx, py] = rgb(c.most_common(1)[0][0])
                    dr = ImageDraw.Draw(im)
                    for z in zn:
                        if z.get("swiat", "world") != swiat: continue
                        mx = (z["x"] - gx0 - tx * bl) // bpp; mz = (z["z"] - gz0 - tz * bl) // bpp
                        if 0 <= mx < K and 0 <= mz < K:
                            if z.get("ikona"):
                                ic = ikona(z["ikona"], 12); rgba = im.convert("RGBA")
                                rgba.alpha_composite(ic, (max(0, min(K - ic.width, mx - ic.width // 2)), max(0, min(K - ic.height, mz - ic.height // 2))))
                                im = rgba.convert("RGB"); dr = ImageDraw.Draw(im)
                            else:
                                dr.rectangle([mx - 2, mz - 2, mx + 2, mz + 2], fill=KONTUR)
                                dr.rectangle([mx - 1, mz - 1, mx + 1, mz + 1], fill=KOLOR_ZN.get(z.get("typ"), (255, 255, 255)))
                    nazwa = "%s/b%d/%d_%d" % (swiat, bpp, tx, tz)
                    p = os.path.join(pack, "assets/mapa/textures/item", nazwa + ".png"); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p, optimize=True)
                    model(os.path.join(pack, "assets/mapa/models/item", nazwa + ".json"), "mapa:item/" + nazwa)
                    zapisz_json(os.path.join(pack, "assets/mapa/items", nazwa + ".json"), kafel_def("mapa:item/" + nazwa, True))
                    licz += 1
            poz.append({"bpp": bpp, "kol": kol, "wier": wier})
        indeks["swiaty"][swiat] = {"x0": gx0, "z0": gz0, "poziomy": poz}
    zapisz_json(os.path.join(dane, "kafle.json"), indeks)
    print("kafelki: %d, swiaty: %s" % (licz, {k: [(p["bpp"], p["kol"], p["wier"]) for p in v["poziomy"]] for k, v in indeks["swiaty"].items()}))

if __name__ == "__main__":
    main()
