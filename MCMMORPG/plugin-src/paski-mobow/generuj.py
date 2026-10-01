#!/usr/bin/env python3
"""Generator grafik PaskiMobow: czcionka `paskimobow:pasek` (Nexo pack/assets/paskimobow).

Zrodlo prawdy dla znakow - Glify.java MUSI miec te same kody i szerokosci (sprawdza `--sprawdz` + test w Javie nie ma,
wiec stale sa tu i tam opisane tym samym komentarzem). Zasady czcionki bitmapowej MC:
- ascent <= height (inaczej klient odrzuca caly plik czcionki),
- szerokosc znaku = najdalsza NIEpusta kolumna + 1, odstep +1 -> znaki o stalej szerokosci dostaja w prawej kolumnie
  piksel alfa=1 (niewidoczny), zeby '1' nie bylo wezsze niz '8',
- kolor tekstu MNOZY piksele znaku: biale/szare tekstury barwimy kolorem rzadkosci z pluginu.

Wyjscie: wynik/pack/assets/paskimobow/{font/pasek.json,textures/font/*.png} + wynik/podglad.png (x8, do ogladania).
"""
import json, os, sys, shutil
from PIL import Image

H = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(H, 'wynik', 'pack', 'assets', 'paskimobow')
TEX = os.path.join(OUT, 'textures', 'font')

K = (28, 12, 24, 255)          # obrys z karty stylu
NIEWIDOCZNY = (0, 0, 0, 1)     # trzyma szerokosc znaku

# --- wymiary (Glify.java) ---
BAR_W = 50                     # wnetrze paska w px = 50 krokow (2%)
BAR_H = 7                      # ramka 1 + wnetrze 5 + ramka 1

# --- kody znakow (Glify.java) ---
C_CYFRA_PLAKIETKI = 0xE000     # +0..9
C_PLAKIETKA = 0xE010           # +1..3 (liczba cyfr)
C_RAMKA = 0xE020
C_RZADKOSC = 0xE030            # +0 klejnot, +1 korona, +2 gwiazda
C_DUCH = 0xE100                # +1..50
C_HP = 0xE200                  # +1..50
C_CYFRA_HP = 0xE300            # +0..9, +10 '/'
C_STATUS = 0xE400              # +indeks z STATUSY
C_SPACJA = 0xF000              # +dx+128, dx -128..128

CYFRY = {  # 3x5
    '0': ['###', '#.#', '#.#', '#.#', '###'],
    '1': ['.#.', '##.', '.#.', '.#.', '###'],
    '2': ['##.', '..#', '.#.', '#..', '###'],
    '3': ['##.', '..#', '.#.', '..#', '##.'],
    '4': ['#.#', '#.#', '###', '..#', '..#'],
    '5': ['###', '#..', '##.', '..#', '##.'],
    '6': ['.##', '#..', '###', '#.#', '###'],
    '7': ['###', '..#', '.#.', '.#.', '.#.'],
    '8': ['###', '#.#', '###', '#.#', '###'],
    '9': ['###', '#.#', '###', '..#', '##.'],
    '/': ['..#', '..#', '.#.', '#..', '#..'],
}

# --- ikony 7x7 (wypelnienie), obrys K dokladany automatycznie -> 9x9; swiatlo z lewej-gory ---
PAL = {
    'r': (150, 34, 18), 'o': (232, 112, 28), 'y': (255, 206, 72), 'w': (255, 246, 196),             # ogien
    'G': (76, 168, 58), 'g': (34, 98, 40), 'l': (166, 226, 104), 'v': (226, 255, 190),              # trucizna
    'R': (196, 30, 42), 'q': (112, 10, 26), 'p': (255, 128, 128),                                    # krew
    'S': (112, 104, 118), 's': (166, 158, 172), 'D': (62, 56, 70),                                   # czaszka
    'b': (126, 82, 44), 'u': (232, 196, 116), 'h': (156, 196, 222),                                  # klepsydra
    'z': (210, 212, 218), 'x': (140, 142, 152), 'Z': (88, 90, 100),                                  # stal
    'P': (110, 70, 150), 'E': (238, 234, 244), 'X': (40, 22, 60),                                    # oko
    'c': (170, 228, 255), 'C': (86, 164, 226), 'B': (44, 100, 176), 'n': (246, 252, 255),            # lod / tarcza
    'Y': (244, 194, 52), 'a': (130, 226, 255), 'A': (50, 140, 210),                                  # zloto / wiatr
    'H': (240, 96, 150), 'j': (255, 176, 206), 'J': (160, 40, 90),                                   # serce
    'f': (255, 238, 150), 'F': (206, 140, 30),                                                       # zlote serce
    'k': K[:3],
}
STATUSY = [  # kolejnosc = kolejnosc na pasku (najpierw szkodliwe) i indeks w Glify.STATUS_*
    ('ogien', ["...r...", "..ror..", ".royor.", ".royyor", "roywyor", "royyyor", ".rooor."]),
    ('trucizna', ["...l...", "...G...", "..lGg..", ".lGGGg.", "lvGGGgg", "GGGGGgg", ".ggggg."]),
    ('krwawienie', ["...p...", "...R...", "..pRq..", ".pRRRq.", "pwRRRqq", "RRRRRqq", ".qqqqq."]),
    ('obumarcie', [".sssss.", "sSSSSSS", "SkkSkkS", "SkkSkkD", "SSSkSSD", ".SDSDD.", ".D.D.D."]),
    ('spowolnienie', ["bbbbbbb", ".huuuh.", "..huh..", "...u...", "..huh..", ".huuuh.", "bbbbbbb"]),
    ('oslabienie', ["......z", ".....zx", "....zx.", ".......", "..zx...", "bbx....", "b......"]),
    ('oslepienie', ["......k", "..EEkE.", ".EPkPE.", "EPkXPPE", ".EkPPE.", "..kEE..", "k......"]),
    ('zamrozenie', ["ncccccC", "cncccCC", "ccccCCC", "cncCCCB", "ccCCCBB", "cCCCBBB", "CCCBBBB"]),
    ('sila', ["......p", ".....pR", "....pR.", "...pR..", "Y.pR...", ".Yq....", "Y.Y...."]),
    ('odpornosc', ["nnCCCCB", "nCCYCCB", "nCYYYCB", "CCCYCCB", ".CCYCB.", "..CCB..", "...B..."]),
    ('regeneracja', [".HH.HH.", "HjHHHHJ", "HHHHHHJ", ".HHHHJ.", "..HHJ..", "...J...", "......."]),
    ('szybkosc', None),  # liczone: dwa szewrony
    ('absorpcja', [".FF.FF.", "FfFFFFF", "FFFFFFF", ".FFFFF.", "..FFF..", "...F...", "......."]),
]
# ikony rzadkosci (szarosci, barwione kolorem rzadkosci w pluginie)
RZADKOSCI_IKONY = [
    ('klejnot', ["...w...", "..wW...", ".wWWm..", "wWWWmmd", ".WWmmd.", "..Wmd..", "...d..."]),
    ('korona', [".......", "w..W..m", "wW.W.mm", "wWWWWmm", "WWWWWmd", "WmmmmdD", "......."]),
    ('gwiazda', ["...w...", "...W...", "..wWm..", "wWWWWmd", "..Wmd..", "...m...", "...d..."]),
]
SZAROSCI = {'w': (255, 255, 255), 'W': (226, 226, 226), 'm': (176, 176, 176), 'd': (120, 120, 120),
            'D': (84, 84, 84), 'k': K[:3]}


def szewrony():
    g = [['.'] * 7 for _ in range(7)]
    for y in range(5):
        x0 = 2 - abs(y - 2)
        for dx, kol in ((0, 'a'), (1, 'A'), (3, 'a'), (4, 'A')):
            g[y + 1][x0 + dx] = kol
    return [''.join(r) for r in g]


def ikona(wiersze, pal):
    """7x7 wypelnienia -> 9x9 z obrysem K w pustych polach sasiadujacych (8 kierunkow)."""
    im = Image.new('RGBA', (9, 9), (0, 0, 0, 0))
    pole = {}
    for y, w in enumerate(wiersze):
        assert len(w) == 7, (wiersze, w)
        for x, ch in enumerate(w):
            if ch != '.':
                pole[(x + 1, y + 1)] = ch
    for (x, y), ch in pole.items():
        im.putpixel((x, y), pal[ch] + (255,) if len(pal[ch]) == 3 else pal[ch])
    for y in range(9):
        for x in range(9):
            if (x, y) in pole:
                continue
            if any((x + dx, y + dy) in pole and pole[(x + dx, y + dy)] != 'k'
                   for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                im.putpixel((x, y), K)
    return im


def cyfra_plakietki(ch):
    im = Image.new('RGBA', (3, 9), (0, 0, 0, 0))
    for y, w in enumerate(CYFRY[ch]):
        for x, c in enumerate(w):
            if c == '#':
                im.putpixel((x, y + 2), (255, 255, 255, 255))
    if im.getpixel((2, 0))[3] == 0:
        im.putpixel((2, 0), NIEWIDOCZNY)
    return im


def cyfra_hp(ch):
    """3x5 biala + cien 1 px w prawo-dol; wiersze 1..6 paska (0 i 6 to ramka)."""
    im = Image.new('RGBA', (4, 7), (0, 0, 0, 0))
    for y, w in enumerate(CYFRY[ch]):
        for x, c in enumerate(w):
            if c == '#' and im.getpixel((x + 1, y + 2))[3] == 0:
                im.putpixel((x + 1, y + 2), (28, 12, 24, 210))
    for y, w in enumerate(CYFRY[ch]):
        for x, c in enumerate(w):
            if c == '#':
                im.putpixel((x, y + 1), (255, 255, 255, 255))
    if im.getpixel((3, 0))[3] == 0:
        im.putpixel((3, 0), NIEWIDOCZNY)
    return im


def plakietka(n):
    w = 4 * n + 3
    im = Image.new('RGBA', (w, 9), (0, 0, 0, 0))
    for y in range(9):
        for x in range(w):
            rog = (x in (0, w - 1)) and (y in (0, 8))
            if rog:
                continue
            if x in (0, w - 1) or y in (0, 8):
                im.putpixel((x, y), K)
            else:
                v = 255 if y <= 2 else (236 if y <= 6 else 196)
                if x == 1:
                    v = min(255, v + 10)
                im.putpixel((x, y), (v, v, v, 255))
    return im


def ramka():
    w = BAR_W + 2
    im = Image.new('RGBA', (w, BAR_H), (0, 0, 0, 0))
    for y in range(BAR_H):
        for x in range(w):
            if (x in (0, w - 1)) and (y in (0, BAR_H - 1)):
                continue
            if x in (0, w - 1) or y in (0, BAR_H - 1):
                im.putpixel((x, y), K)
            else:
                im.putpixel((x, y), (52, 30, 38, 235) if y > 1 else (40, 22, 30, 235))
    return im


def wypelnienie(n, tony):
    """Kolumny 1..n (kolumna 0 = ramka, przezroczysta), wiersze 1..5."""
    im = Image.new('RGBA', (n + 1, BAR_H), (0, 0, 0, 0))
    for x in range(1, n + 1):
        for y in range(1, BAR_H - 1):
            im.putpixel((x, y), tony[y - 1])
    return im


HP_TONY = [(238, 92, 80, 255), (204, 44, 44, 255), (186, 34, 40, 255), (160, 26, 36, 255), (122, 18, 32, 255)]
DUCH_TONY = [(255, 238, 200, 255), (240, 214, 170, 255), (232, 200, 150, 255), (214, 180, 130, 255), (186, 150, 108, 255)]


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(TEX)
    prov = []

    def dodaj(nazwa, im, kod, ascent):
        im.save(os.path.join(TEX, nazwa + '.png'))
        prov.append({'type': 'bitmap', 'file': f'paskimobow:font/{nazwa}.png', 'ascent': ascent,
                     'height': im.height, 'chars': [chr(kod)]})
        assert ascent <= im.height, nazwa

    for d in range(10):
        dodaj(f'pc{d}', cyfra_plakietki(str(d)), C_CYFRA_PLAKIETKI + d, 8)
        dodaj(f'hc{d}', cyfra_hp(str(d)), C_CYFRA_HP + d, 7)
    dodaj('hc_ukosnik', cyfra_hp('/'), C_CYFRA_HP + 10, 7)
    for n in (1, 2, 3):
        dodaj(f'plakietka{n}', plakietka(n), C_PLAKIETKA + n, 8)
    dodaj('ramka', ramka(), C_RAMKA, 7)
    for i in range(1, BAR_W + 1):
        dodaj(f'duch{i}', wypelnienie(i, DUCH_TONY), C_DUCH + i, 7)
        dodaj(f'hp{i}', wypelnienie(i, HP_TONY), C_HP + i, 7)
    for i, (nazwa, w) in enumerate(RZADKOSCI_IKONY):
        dodaj(f'rz_{nazwa}', ikona(w, SZAROSCI), C_RZADKOSC + i, 8)
    for i, (nazwa, w) in enumerate(STATUSY):
        dodaj(f'st_{nazwa}', ikona(w or szewrony(), PAL), C_STATUS + i, 8)
    prov.append({'type': 'space', 'advances': {chr(C_SPACJA + dx + 128): dx for dx in range(-128, 129) if dx}})
    os.makedirs(os.path.join(OUT, 'font'))
    with open(os.path.join(OUT, 'font', 'pasek.json'), 'w', encoding='utf-8') as f:
        json.dump({'providers': prov}, f, ensure_ascii=True, indent=1)
    podglad()
    print(f'{len(prov)} dostawcow, {len(os.listdir(TEX))} tekstur -> {OUT}')


def podglad():
    """Arkusz x8: ikony statusow, rzadkosci (zabarwione), plakietki, paski w kilku stanach."""
    kolory = [(200, 200, 200), (90, 150, 255), (255, 220, 80), (210, 110, 255), (255, 150, 40)]
    S = 8
    tlo = Image.new('RGBA', (140, 70), (110, 140, 90, 255))

    def wklej(im, x, y, kolor=None):
        if kolor:
            px = im.load()
            im = im.copy(); q = im.load()
            for yy in range(im.height):
                for xx in range(im.width):
                    r, g, b, a = px[xx, yy]
                    q[xx, yy] = (r * kolor[0] // 255, g * kolor[1] // 255, b * kolor[2] // 255, a)
        tlo.alpha_composite(im, (x, y))

    x = 2
    for nazwa, _ in STATUSY:
        wklej(Image.open(os.path.join(TEX, f'st_{nazwa}.png')), x, 2); x += 10
    x = 2
    for i, kol in enumerate(kolory):
        nazwa = RZADKOSCI_IKONY[min(2, max(0, i - 2))][0] if i >= 1 else 'klejnot'
        wklej(Image.open(os.path.join(TEX, f'rz_{nazwa}.png')), x, 14, kol); x += 10
    y = 26
    for i, (proc, duch, poz) in enumerate([(100, 0, 1), (64, 80, 12), (18, 18, 100)]):
        kol = kolory[i * 2]
        n = len(str(poz))
        wklej(Image.open(os.path.join(TEX, f'plakietka{n}.png')), 2, y, kol)
        for j, ch in enumerate(str(poz)):
            wklej(Image.open(os.path.join(TEX, f'pc{ch}.png')), 4 + 4 * j, y, (28, 12, 24))
        bx = 4 * n + 3 + 4
        wklej(Image.open(os.path.join(TEX, 'ramka.png')), bx, y + 1)
        if duch:
            wklej(Image.open(os.path.join(TEX, f'duch{duch * BAR_W // 100}.png')), bx, y + 1)
        if proc:
            wklej(Image.open(os.path.join(TEX, f'hp{max(1, proc * BAR_W // 100)}.png')), bx, y + 1)
        tekst = f'{proc * 14 // 10}/140'
        tx = bx + 1 + (BAR_W - (4 * len(tekst) - 1)) // 2
        for j, ch in enumerate(tekst):
            wklej(Image.open(os.path.join(TEX, 'hc_ukosnik.png' if ch == '/' else f'hc{ch}.png')), tx + 4 * j, y + 1)
        y += 12
    tlo.resize((tlo.width * S, tlo.height * S), Image.NEAREST).save(os.path.join(H, 'wynik', 'podglad.png'))


if __name__ == '__main__':
    main()
