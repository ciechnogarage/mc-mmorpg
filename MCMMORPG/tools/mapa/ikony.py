#!/usr/bin/env python3
"""Ikony znacznikow dla pelnej mapy: plugins/Mapa/ikony/<nazwa>.png (16x16 RGBA z konturem).

ikony.py <znaczniki.json> <wyjscie plugins/Mapa>   — nazwa pliku = czesc po ':' / ostatnim '/'
"""
import sys, os, json
from PIL import Image
import kafle

def nazwa_pliku(ik): return ik.split(":")[-1].split("/")[-1]

def main(zn, wyj, podkatalog="ikony", rozmiar=14):
    """rozmiar 14 -> 16x16 (pelna mapa), 9 -> 11x11 (minimapa NMinimap: plugins/NMinimap/markers)."""
    d = os.path.join(wyj, podkatalog); os.makedirs(d, exist_ok=True)
    for z in json.load(open(zn)):
        if z.get("ikona"):
            kafle.ikona(z["ikona"], rozmiar).save(os.path.join(d, nazwa_pliku(z["ikona"]) + ".png"))
    ikony_gracza(wyj, podkatalog, rozmiar)
    print("ikony:", sorted(os.listdir(d)))


# --- ikony do wyboru dla wlasnych znacznikow gracza (SPEC-v6) + cel prowadzenia ----------------------
PINEZKI = {"pin_czerwona": (214, 52, 52), "pin_niebieska": (60, 110, 220), "pin_zielona": (90, 190, 60),
           "pin_zolta": (240, 200, 50), "pin_fioletowa": (160, 80, 210), "pin_biala": (235, 235, 235)}
Z_GRY = {"nether_star": "mc:item/nether_star", "gold_ingot": "mc:item/gold_ingot", "oak_door": "mc:item/oak_door",
         "iron_sword": "mc:item/iron_sword"}

def pinezka(kolor, rozmiar):
    from PIL import ImageDraw
    S = 64; im = Image.new("RGBA", (S, S)); d = ImageDraw.Draw(im)
    d.ellipse([12, 2, 52, 42], fill=kolor + (255,)); d.polygon([(16, 32), (48, 32), (32, 62)], fill=kolor + (255,))
    d.ellipse([24, 12, 40, 28], fill=(255, 255, 255, 230))
    m = im.resize((rozmiar, rozmiar), Image.BOX); a = m.getchannel("A").point(lambda v: 255 if v > 110 else 0); m.putalpha(a)
    wyn = Image.new("RGBA", (rozmiar + 2, rozmiar + 2))
    for dx, dy in ((0, 1), (2, 1), (1, 0), (1, 2)): wyn.paste(Image.new("RGBA", m.size, (24, 20, 16, 255)), (dx, dy), a)
    wyn.alpha_composite(m, (1, 1)); return wyn

def cel(rozmiar):
    from PIL import ImageDraw
    im = Image.new("RGBA", (rozmiar + 2, rozmiar + 2)); d = ImageDraw.Draw(im); n = rozmiar + 1
    for w, k in ((3, (24, 20, 16, 255)), (1, (230, 40, 40, 255))):
        d.line([(1, 1), (n - 1, n - 1)], fill=k, width=w); d.line([(1, n - 1), (n - 1, 1)], fill=k, width=w)
    return im

def modele(ikony_dir, pack):
    """Przedmioty mapa:ikona/<nazwa> (panel atlasu, SPEC-v7) z ikon 16x16; uruchamiac PO kafle.py (czysci assets/mapa)."""
    for f in sorted(os.listdir(ikony_dir)):
        if not f.endswith(".png"): continue
        n = f[:-4]
        t = os.path.join(pack, "assets/mapa/textures/item/ikona", f); os.makedirs(os.path.dirname(t), exist_ok=True)
        Image.open(os.path.join(ikony_dir, f)).save(t)
        kafle.zapisz_json(os.path.join(pack, "assets/mapa/models/item/ikona", n + ".json"),
                          {"parent": "minecraft:item/generated", "textures": {"layer0": "mapa:item/ikona/" + n}})
        kafle.zapisz_json(os.path.join(pack, "assets/mapa/items/ikona", n + ".json"),
                          {"model": {"type": "minecraft:model", "model": "mapa:item/ikona/" + n}})

def ikony_gracza(wyj, podkatalog, rozmiar):
    d = os.path.join(wyj, podkatalog); os.makedirs(d, exist_ok=True)
    for n, k in PINEZKI.items(): pinezka(k, rozmiar).save(os.path.join(d, n + ".png"))
    for n, src in Z_GRY.items(): kafle.ikona(src, rozmiar).save(os.path.join(d, n + ".png"))
    cel(rozmiar).save(os.path.join(d, "cel.png"))

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--modele":     # ikony.py --modele <ikony dir> <pack dir>
        modele(sys.argv[2], sys.argv[3])
    else:
        main(sys.argv[1], sys.argv[2], *(sys.argv[3:4]), *([int(sys.argv[4])] if len(sys.argv) > 4 else []))
