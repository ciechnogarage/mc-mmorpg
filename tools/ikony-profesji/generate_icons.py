#!/usr/bin/env python3
"""Generator ikon surowcow profesyjnych (16x16 PNG) dla packa Nexo.

Rodzina: wspolny ciemny kontur, 3-tonowe cieniowanie (swiatlo z gory-lewej),
stonowana paleta w duchu tekstur block/pakiet3 (slate + zloto/miedz + ametyst).
Kazda ikona to jawna mapa pikseli - sylwetka ma byc czytelna w slocie 16x16.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tier as TIER  # noqa: E402
from classify_tiers import scan as scan_recipes, tier_of  # noqa: E402

OUT = os.environ.get("OUT", "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/profesje")

# --- wspolna paleta rodziny -------------------------------------------------
O = (24, 22, 30, 255)          # kontur, wspolny dla WSZYSTKICH ikon
T = (0, 0, 0, 0)               # przezroczystosc

PAL = {
    # kamien / slate (jak tlo pakiet3)
    "stone":   {"s": (74, 76, 86), "b": (108, 111, 124), "h": (146, 150, 163)},
    # zloto/miedz - akcent rodziny (#c9a227 z itemname)
    "gold":    {"s": (150, 112, 26), "b": (201, 162, 39), "h": (240, 208, 110)},
    "wood":    {"s": (82, 54, 32), "b": (120, 82, 48), "h": (158, 114, 70)},
    "oak":     {"s": (66, 42, 26), "b": (99, 64, 38), "h": (132, 90, 56)},
    "birch":   {"s": (150, 136, 108), "b": (196, 182, 152), "h": (228, 218, 195)},
    "resin":   {"s": (162, 104, 24), "b": (214, 146, 38), "h": (248, 196, 92)},
    "wheat":   {"s": (150, 110, 38), "b": (198, 156, 58), "h": (236, 202, 110)},
    "fish":    {"s": (52, 84, 110), "b": (84, 126, 158), "h": (132, 176, 205)},
    "fleece":  {"s": (168, 162, 150), "b": (214, 210, 200), "h": (242, 240, 234)},
    "leather": {"s": (92, 62, 42), "b": (134, 96, 64), "h": (172, 132, 92)},
    # glina celowo chlodniejsza/bardziej niebieska niz "stone" - inaczej obie
    # ikony czytaja sie jak ten sam szary kamien
    "clay":    {"s": (84, 92, 116), "b": (120, 130, 158), "h": (158, 168, 196)},
    "crystal": {"s": (94, 58, 140), "b": (138, 90, 196), "h": (184, 146, 232)},
    "green":   {"s": (58, 96, 46), "b": (86, 134, 62), "h": (124, 174, 88)},
}


def mk(tone, accent=None):
    """Buduje mape znak->kolor: s/b/h = ton bazowy, a/A = akcent."""
    m = {".": T, "o": O}
    p = PAL[tone]
    m["s"], m["b"], m["h"] = p["s"] + (255,), p["b"] + (255,), p["h"] + (255,)
    if accent:
        q = PAL[accent]
        m["a"], m["A"] = q["b"] + (255,), q["h"] + (255,)
    return m


ICONS = {}


def icon(name, tone, accent, rows):
    ICONS[name] = (mk(tone, accent), rows)


# 1. Bryla urobku - nieregularny odlamek skaly ze zlotymi zylkami
icon("ore_chunk", "stone", "gold", [
    "................",
    "................",
    "......oooo......",
    ".....ohhhbo.....",
    "...oohhbbbboo...",
    "..obhbbAAbbbbo..",
    "..obbbbaAbbbbo..",
    ".obsbbbbbbbbbbo.",
    ".obsbbbbbAAbbbo.",
    ".obssbbbbaAbbbo.",
    "..obssbbbbbbbo..",
    "..oobsssbbbbo...",
    "....oosssbbo....",
    "......oooooo....",
    "................",
    "................",
])

# 2. Surowy kamien - ciosany, fasetowany blok (jasna gora / sciana lewa / ciemna prawa).
# Trzy plaszczyzny + pionowy zalom, zeby NIE czytal sie jak okragla bryla gliny.
icon("rough_stone", "stone", None, [
    "................",
    "................",
    "....oooooo......",
    "...ohhhhhhoo....",
    "..ohhhhhhhhhho..",
    ".ohhhhhhhhhhhbo.",
    ".obbbshhhhsbsso.",
    ".obbbbbshsbssso.",
    ".obbbbbbsbsssso.",
    ".obbbbbbsbsssso.",
    ".obbbbbbsbsssso.",
    "..obbbbbsbssso..",
    "..oobbbbsbsso...",
    "....oooooooo....",
    "................",
    "................",
])

# 3. Surowe drewno - kloda z widocznym slojem na czole
icon("raw_timber", "wood", "birch", [
    "................",
    "................",
    "................",
    "...ooooooooooo..",
    "..oaAaahhhhhhbo.",
    ".oaAhAabbbbbbbo.",
    ".oaAaAabbbbbsbo.",
    ".oaAAAabbbbbsbo.",
    ".oaAaAabbbsbsbo.",
    ".oaAhAabbbsbbbo.",
    ".oaaaaabbbsbbbo.",
    "..obbbbsbbbbbbo.",
    "...ooooooooooo..",
    "................",
    "................",
    "................",
])

# 4. Twarde drewno debowe - ciosana belka, dwa bale
icon("oak_timber", "oak", None, [
    "................",
    "................",
    "..ooooooooooo...",
    ".ohhhhhhhhhhbo..",
    ".obbbbbbbbbbbo..",
    ".obbsbbbbsbbbo..",
    ".obbsbbbbsbbbo..",
    ".ossssssssssso..",
    "..ooooooooooooo.",
    "..ohhhhhhhhhhbo.",
    "..obbbbbbbbbbbo.",
    "..obbsbbbbsbbbo.",
    "..obbsbbbbsbbbo.",
    "..ossssssssssso.",
    "...ooooooooooo..",
    "................",
])

# 5. Zywiczne drewno swierkowe - kloda z kroplami bursztynu
icon("spruce_resin", "oak", "resin", [                    # kloda UKOSNA z kroplami zywicy.
    # Pozioma wersja zlewala sie z raw_timber/oak_timber/seasoned_timber - te same
    # brazowe prostokaty. Ukos daje inna sylwetke przy tym samym motywie.
    "................",
    ".............oo.",
    "...........oohho",
    "..........ohhbbo",
    ".........ohhbbo.",
    "........ohbaAo..",
    ".......ohbaAAo..",
    "......ohbbaAo...",
    ".....ohbbbbo....",
    "....ohbaAbo.....",
    "...ohbaAAo......",
    "..ohbbaAo.......",
    "..obbbbo........",
    "..obsso.........",
    "..osso..........",
    "..oo............",
])

# 6. Gietkie drewno brzozowe - snopek trzech jasnych listew zwiazanych rzemieniem,
# z czarnymi kreskami kory. Pionowy snopek != pozioma belka debowa.
icon("birch_lath", "birch", "leather", [
    "................",
    "....oooooooo....",
    "...ohbsohbso....",
    "...ohbsohbso....",
    "...oobsohboo....",
    "...ohbsohbso....",
    "...ohbsohbso....",
    "..oaaaaaaaaao...",
    "..oAAAAAAAAAo...",
    "..oaaaaaaaaao...",
    "...ohbsohbso....",
    "...ohbsohbso....",
    "...oobsoobso....",
    "...ohbsohbso....",
    "....oooooooo....",
    "................",
])

# 7. Swiezy zbior - snop zboza
icon("fresh_harvest", "wheat", "green", [
    "......o.o.......",
    ".....ohoho......",
    "....ohbhbho.....",
    "....obhbhbo.....",
    "...ohbhbhbho....",
    "...obhbhbhbo....",
    "...obbhbhbbo....",
    "....obbhbbo.....",
    "....oaAAAao.....",
    "....oaAAAao.....",
    "....obbhbbo.....",
    "...obbsbsbbo....",
    "...obsbsbsbo....",
    "..obsbsbsbsbo...",
    "..oso.oso.oso...",
    "...o...o...o....",
])

# 8. Swiezy polow - ryba
icon("raw_catch", "fish", "gold", [
    "................",
    "................",
    "...........oo...",
    "....oooo..ohho..",
    "..oohhhhoohbbo..",
    ".ohhbAbbbhbbbo..",
    ".obhbAbbbbhbbbo.",
    "ohbbbbbbbbbhbbo.",
    "obsbbbbbbbbbbbo.",
    ".obsbbbbbbbhbbo.",
    ".obssbbbbsbbbo..",
    "..oossbbsobbo...",
    "....ooossoosso..",
    "........ooooo...",
    "................",
    "................",
])

# 9. Surowe runo - zwoj welny
icon("raw_fleece", "fleece", None, [                      # runo: POSTRZEPIONE kosmyki.
    # Gladka bryla mylila sie z perla - stad zabkowany obrys i wyrazne zawijasy.
    "................",
    "..oo..oo..oo....",
    ".ohho.ohho.ohho.",
    ".ohbhoohbhoohbho",
    "obhbbhhbbhhbbbho",
    "obbsbbbsbbbsbbbo",
    "obbbbsbbbsbbbbbo",
    "obsbbbbsbbbbsbbo",
    "obbbsbbbbbsbbbbo",
    "obsbbbbsbbbbsbbo",
    ".obsbbbbbsbbbbo.",
    ".oobssbbbssbbbo.",
    "...oosssssssoo..",
    "..oo.oooooo.oo..",
    ".ohho......ohho.",
    "..oo........oo..",
])

# 10. Lup z bestii - platat skory: PROSTA krawedz ciecia u gory, dwie lapy u dolu,
# jasne przeszycia. Wersja z czterema lapami czytala sie w grze jak lepek zwierzecia
# (gorne wypustki wygladaly jak uszy) - stad scieta gora.
icon("beast_scrap", "leather", "fleece", [
    "................",
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    "obsbbbAbbbAbbbbo",
    "obsbbbbbbbbbbbbo",
    "obssbbbAbbbAbbbo",
    ".obssbbbbbbbbbo.",
    ".obsssbbbbbbbbo.",
    ".obsssbbbbbbbbo.",
    ".obhoossssoohbo.",
    ".ohho......ohho.",
    "..oo........oo..",
    "................",
    "................",
])

# 11. Gruda gliny - niska, rozplaszczona bryla z wglebieniami po palcach,
# chlodniejsza paleta. Szeroka i plaska, w odroznieniu od katowego kamienia.
icon("clay_lump", "clay", None, [
    "................",
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "...oohhhhhhoo...",
    "..ohhbbhhbbbho..",
    ".obhbsbbsbbbbho.",
    ".obbbbbbbbbbbbo.",
    "obsbbbbbbbbbbbbo",
    "obssbbbbbbbbbbbo",
    "obsssbbbbbbbbsbo",
    ".obsssbbbbbsssbo",
    ".oossssssssssoo.",
    "...oooooooooo...",
    "................",
])

# 12. Surowy krysztal - ostry odlamek ametystu
icon("rough_crystal", "crystal", "fleece", [
    "................",
    ".......oo.......",
    "......ohho......",
    "......ohbo......",
    ".....ohhbbo.....",
    ".....ohhbbo.....",
    "....oohhbbso....",
    "...oohAhbbso....",
    "..ohhoAhbbsoo...",
    "..ohbooohbsbho..",
    "..obbbsohbsbbo..",
    "..obbsssobsbbo..",
    "...obssso.ossbo.",
    "....ooso...oso..",
    "......o.....o...",
    "................",
])


def render(rows, cmap):
    im = Image.new("RGBA", (16, 16), T)
    px = im.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, f"wiersz {y} ma {len(row)} znakow, nie 16: {row!r}"
        for x, ch in enumerate(row):
            px[x, y] = cmap[ch]
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    assert len(ICONS) == 12, f"oczekiwano 12 ikon, jest {len(ICONS)}"
    outputs = scan_recipes()
    built = {}
    for name, (cmap, rows) in ICONS.items():
        assert len(rows) == 16, f"{name}: {len(rows)} wierszy"
        im = render(rows, cmap)
        t = tier_of("PROF_" + name.upper(), outputs)
        TIER.stamp(im, t)
        built[name] = (im, t)
        im.save(os.path.join(OUT, f"{name}.png"))
    print("zapisano", len(built), "ikon do", OUT)
    print("tiery:", {n: t for n, (_, t) in built.items()})

    # arkusz kontrolny do ogladania (6x NEAREST)
    cols, S = 6, 96
    rows_n = (len(built) + cols - 1) // cols
    sheet = Image.new("RGBA", (S * cols, S * rows_n), (38, 38, 46, 255))
    for i, (name, (im, t)) in enumerate(built.items()):
        sheet.alpha_composite(im.resize((S, S), Image.NEAREST), ((i % cols) * S, (i // cols) * S))
    sheet.save("/tmp/ikony-work/sheet.png")
    print("arkusz: /tmp/ikony-work/sheet.png")


if __name__ == "__main__":
    main()
