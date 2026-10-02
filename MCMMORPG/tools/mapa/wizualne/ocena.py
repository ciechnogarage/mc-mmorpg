#!/usr/bin/env python3
import warnings; warnings.filterwarnings("ignore")
"""Ocena zrzutu 1280x720 z klienta QA: czy mapa w rece / minimapa / pelna mapa NAPRAWDE cos pokazuje.

ocena.py <png> <reka|minimapa|pelna>  -> wypisuje 'PASS ...' albo 'FAIL ...' (+ liczby)
reka: gracz patrzy w dol (pitch ~60), mapa w obu dloniach zajmuje srodek ekranu. Ramka mapy = pergamin
      (jasny bez); w srodku teren = duzo roznych kolorow. Pusta mapa = sam pergamin; brak mapy = trawa/podloze.
minimapa: kwadrat w lewym gornym rogu (x 62..318, y 62..318): teren zamiast nieba.
pelna: ekran mapy przed graczem: ciemny pasek zakladek u gory i zroznicowany obraz pod nim.
"""
import sys
from PIL import Image

import colorsys
def pergamin(p):  # ramka mapy vanilli: bez (odcien ~25-50 st., srednie nasycenie) - niezaleznie od jasnosci (noc)
    r, g, b = p
    h, sat, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.07 <= h <= 0.14 and 0.2 <= sat <= 0.65 and v >= 0.15 and r > g > b

def niebo(p):
    r, g, b = p
    return b > 200 and b > r + 40 and b > g + 10

def kolory(img):
    return len(set((r // 16, g // 16, b // 16) for r, g, b in img.getdata()))

def main(f, tryb):
    im = Image.open(f).convert("RGB")
    if tryb == "reka":
        # ramka: pasy, w ktorych przy pitch 60 ZAWSZE lezy brzeg mapy trzymanej oburacz (1280x720, FOV 70)
        pasy = [(338, 120, 372, 520), (910, 120, 946, 520), (420, 22, 860, 58)]
        rp = []
        for bx in pasy: rp += list(im.crop(bx).resize((12, 40) if bx[2] - bx[0] < 60 else (40, 12), Image.NEAREST).getdata())
        ramka = sum(map(pergamin, rp)) / len(rp)
        srodek = im.crop((480, 180, 800, 500)).resize((80, 80), Image.NEAREST)
        sp = list(srodek.getdata()); perg_s = sum(map(pergamin, sp)) / len(sp); k = kolory(srodek)
        ok = ramka >= 0.35 and perg_s < 0.6 and k >= 12   # pusta mapa: srodek = pergamin, ~4 kolory
        print(("PASS" if ok else "FAIL") + " reka: ramka=%.2f srodek_pergamin=%.2f kolory_srodka=%d" % (ramka, perg_s, k))
    elif tryb == "minimapa":
        m = im.crop((66, 66, 314, 314)).resize((62, 62), Image.NEAREST); px = list(m.getdata())
        nb = sum(map(niebo, px)) / len(px); k = kolory(m)
        ok = nb < 0.3 and k >= 3
        print(("PASS" if ok else "FAIL") + " minimapa: niebo=%.2f kolory=%d" % (nb, k))
    elif tryb == "pelna":
        srodek = im.crop((340, 120, 940, 520)).resize((120, 80), Image.NEAREST); k = kolory(srodek)
        px = list(srodek.getdata()); nb = sum(map(niebo, px)) / len(px)
        ok = k >= 30 and nb < 0.3
        print(("PASS" if ok else "FAIL") + " pelna: kolory=%d niebo=%.2f" % (k, nb))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
