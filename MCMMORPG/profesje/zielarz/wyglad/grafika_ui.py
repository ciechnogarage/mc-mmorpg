#!/usr/bin/env python3
"""Font zielarz:ui dla komunikatow zielarza (warianty A/B/C): ikony klawiszy, tabliczka HUD, przesuniecia.
Uzycie: grafika_ui.py <katalog_pack>   (np. plugins/Nexo/pack) -> assets/zielarz/font/ui.json + textures/font/*.png
Paleta zmierzona z HUD (okno_a.png, l1-okno): obrys #251C2B, panel #684338, zloto #8A6A30/#D5A96B, jasne #FFE7AE.
Kody znakow musza zgadzac sie z ZKomunikaty.java (LPM=E000, PPM=E001, SHIFT=E002, ...)."""
import json, os, sys
from PIL import Image

OBRYS, CIEMNE, PANEL, CIEN = (0x25, 0x1C, 0x2B), (0x39, 0x23, 0x2E), (0x68, 0x43, 0x38), (0x4A, 0x2F, 0x2A)
ZLOTO, ZLOTO_J, JASNE = (0x8A, 0x6A, 0x30), (0xD5, 0xA9, 0x6B), (0xFF, 0xE7, 0xAE)
TON = {"info": ZLOTO_J, "dobrze": (0x9C, 0xC0, 0x62), "uwaga": (0xE0, 0x8A, 0x3C), "zle": (0xD0, 0x4E, 0x3A)}

def img(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))

def px(im, x, y, c):
    im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def mysz(lewy):
    """Myszka 9x11: obrys, swiecacy klikany przycisk (lewy albo prawy), korpus w kolorze panelu HUD."""
    w, h = 9, 11
    im = img(w, h)
    for y in range(h):
        for x in range(w):
            rog = (x in (0, w - 1) and y in (0, 1, h - 2, h - 1)) or (x in (1, w - 2) and y in (0, h - 1))
            if rog:
                continue
            brzeg = x == 0 or x == w - 1 or y == 0 or y == h - 1 or (x in (1, w - 2) and y in (1, h - 2))
            if brzeg:
                px(im, x, y, OBRYS); continue
            if y <= 4:   # przyciski
                if x == 4: px(im, x, y, OBRYS); continue
                klik = (x < 4) == lewy
                if klik: px(im, x, y, JASNE if y <= 2 else ZLOTO_J)
                else: px(im, x, y, PANEL)
            elif y == 5: px(im, x, y, OBRYS)
            else: px(im, x, y, CIEN if y >= h - 3 else PANEL)
    return im

LIT = {  # 3x5
    "S": ["###", "#..", "###", "..#", "###"], "H": ["#.#", "#.#", "###", "#.#", "#.#"], "I": ["#", "#", "#", "#", "#"],
    "F": ["###", "#..", "##.", "#..", "#.."], "T": ["###", ".#.", ".#.", ".#.", ".#."],
}

def klawisz(napis):
    sz = sum(len(LIT[c][0]) for c in napis) + len(napis) - 1
    w, h = sz + 6, 11
    im = img(w, h)
    for y in range(h):
        for x in range(w):
            if (x in (0, w - 1)) and y in (0, h - 1): continue
            if x in (0, w - 1) or y in (0, h - 1): px(im, x, y, OBRYS)
            elif y == 1: px(im, x, y, ZLOTO)
            elif y >= h - 3: px(im, x, y, CIEN)
            else: px(im, x, y, PANEL)
    x0 = 3
    for c in napis:
        for dy, row in enumerate(LIT[c]):
            for dx, ch in enumerate(row):
                if ch == "#": px(im, x0 + dx, 2 + dy, JASNE)
        x0 += len(LIT[c][0]) + 1
    return im

PH = 17   # wysokosc tabliczki (ikona 11 px + ramka)

def tabliczka_lewa(ton):
    im = img(4, PH)
    for y in range(PH):
        for x in range(4):
            if x == 0 and y in (0, PH - 1): continue
            if x == 0 or y in (0, PH - 1): px(im, x, y, OBRYS)
            elif x in (1, 2): px(im, x, y, TON[ton])
            else: px(im, x, y, OBRYS)
    return im

def tabliczka_prawa():
    im = img(3, PH)
    for y in range(PH):
        for x in range(3):
            if x == 2 and y in (0, PH - 1): continue
            if x == 2 or y in (0, PH - 1): px(im, x, y, OBRYS)
            elif y == 1: px(im, x, y, ZLOTO)
            elif y == PH - 2: px(im, x, y, OBRYS)
            else: px(im, x, y, CIEMNE)
    return im

def tabliczka_srodek(w):
    im = img(w, PH)
    for y in range(PH):
        for x in range(w):
            if y in (0, PH - 1): px(im, x, y, OBRYS)
            elif y == 1: px(im, x, y, ZLOTO)
            elif y == PH - 2: px(im, x, y, OBRYS)
            else: px(im, x, y, CIEMNE)
    return im

def winieta(alfa_max, w=256, h=144):
    """Nakladka kamery (equippable.camera_overlay) dla feelingu zielarza: czarne brzegi, przezroczysty srodek.
    Alfa rosnie od ~0.55 promienia elipsy do brzegu (wygladzona krzywa), srodek ekranu czysty."""
    im = img(w, h)
    for y in range(h):
        for x in range(w):
            dx, dy = (x + 0.5 - w / 2) / (w / 2), (y + 0.5 - h / 2) / (h / 2)
            r = (dx * dx + dy * dy) ** 0.5
            t = max(0.0, min(1.0, (r - 0.55) / 0.75))
            a = int(alfa_max * t * t * (3 - 2 * t))
            if a: im.putpixel((x, y), (8, 6, 4, a))
    return im

def main(pack):
    tex = os.path.join(pack, "assets/zielarz/textures/font"); os.makedirs(tex, exist_ok=True)
    fnt = os.path.join(pack, "assets/zielarz/font"); os.makedirs(fnt, exist_ok=True)
    prov = []
    def dodaj(nazwa, im, znak, ascent):
        im.save(os.path.join(tex, nazwa + ".png"))
        prov.append({"type": "bitmap", "file": f"zielarz:font/{nazwa}.png", "height": im.height, "ascent": ascent, "chars": [znak]})
    dodaj("lpm", mysz(True), chr(0xE000), 9)
    dodaj("ppm", mysz(False), chr(0xE001), 9)
    dodaj("shift", klawisz("SHIFT"), chr(0xE002), 9)
    for i, t in enumerate(["info", "dobrze", "uwaga", "zle"]):
        dodaj("tab_l_" + t, tabliczka_lewa(t), chr(0xE010 + i), 11)
    dodaj("tab_p", tabliczka_prawa(), chr(0xE014), 11)
    for i in range(8):   # srodek o szerokosci 1,2,4,...,128 (skladany binarnie)
        dodaj(f"tab_s{1 << i}", tabliczka_srodek(1 << i), chr(0xE020 + i), 11)
    adv = {}
    for i in range(9):
        adv[chr(0xE100 + i)] = -(1 << i)   # -1..-256
        adv[chr(0xE110 + i)] = 1 << i      # 1..256
    prov.append({"type": "space", "advances": adv})
    misc = os.path.join(pack, "assets/zielarz/textures/misc"); os.makedirs(misc, exist_ok=True)
    winieta(110).save(os.path.join(misc, "winieta.png"))         # feeling: rytual (lekka, cala sesja)
    winieta(190).save(os.path.join(misc, "winieta_mocna.png"))   # feeling: rytual (chwila wyciagniecia)
    with open(os.path.join(fnt, "ui.json"), "w") as f:
        json.dump({"providers": prov}, f, ensure_ascii=True, indent=1)
    print("zielarz:ui", len(prov), "dostawcow ->", fnt)

if __name__ == "__main__":
    main(sys.argv[1])
