#!/usr/bin/env python3
"""Generator grafik PaskiMobow: czcionka `paskimobow:pasek` (Nexo pack/assets/paskimobow).

Zrodlo prawdy dla znakow - Wyglad.java MUSI miec te same kody i szerokosci.
Zasady czcionki bitmapowej MC:
- NIC SIE NIE NAKLADA: klient nie gwarantuje kolejnosci rysowania nakladajacych sie znakow (zmierzone 01.10:
  cyfra HP na tle ramki wychodzila przyciemniona) -> pasek i plakietka to kafle stykajace sie krawedziami,
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

# --- wymiary (Wyglad.java) ---
BAR_W = 50                     # wnetrze paska w px = 50 krokow (2%)
BAR_H = 7                      # ramka 1 + wnetrze 5 + ramka 1

# --- kody znakow (Wyglad.java) ---
C_CYFRA_PLAKIETKI = 0xE000     # +0..9: kafel 4x9 (cyfra + tlo plakietki), barwiony kolorem rzadkosci
C_PLAKIETKA = 0xE010           # +0 lewy brzeg (2 kol.), +1 prawy brzeg (1 kol.)
C_RAMKA = 0xE020               # +0 lewy koniec paska, +1 prawy koniec (po 1 kol.)
C_TLO = 0xE500                 # +1..50 puste wnetrze paska z ramka gora/dol
C_RZADKOSC = 0xE030            # +0 klejnot, +1 korona, +2 gwiazda
C_DUCH = 0xE100                # +1..50
C_HP = 0xE200                  # +stopien*64+1..50: zielony, zolty, pomaranczowy, czerwony
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
STATUSY = [  # kolejnosc = kolejnosc na pasku (najpierw szkodliwe) i indeks w Wyglad.Status
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


def ton_plakietki(y):
    return 255 if y <= 2 else (236 if y <= 6 else 196)


def kafel_plakietki(ch):
    """4x9: tlo (biale, barwione rzadkoscia) + cyfra w kolorze obrysu w kol. 0..2, wiersze 2..6; kol. 3 = odstep."""
    im = Image.new('RGBA', (4, 9), (0, 0, 0, 0))
    for y in range(9):
        for x in range(4):
            if y in (0, 8):
                im.putpixel((x, y), K)
            else:
                v = ton_plakietki(y); im.putpixel((x, y), (v, v, v, 255))
    for y, w in enumerate(CYFRY[ch]):
        for x, c in enumerate(w):
            if c == '#':
                im.putpixel((x, y + 2), K)
    return im


def brzeg_plakietki(lewy):
    w = 2 if lewy else 1
    im = Image.new('RGBA', (w, 9), (0, 0, 0, 0))
    for y in range(9):
        if lewy:
            if y not in (0, 8):
                im.putpixel((0, y), K)
            if y in (0, 8):
                im.putpixel((1, y), K)
            else:
                v = min(255, ton_plakietki(y) + 10); im.putpixel((1, y), (v, v, v, 255))
        elif y not in (0, 8):
            im.putpixel((0, y), K)
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


TLO_TONY = [(40, 22, 30, 255), (52, 30, 38, 255), (52, 30, 38, 255), (52, 30, 38, 255), (52, 30, 38, 255)]


def koniec_paska():
    im = Image.new('RGBA', (1, BAR_H), (0, 0, 0, 0))
    for y in range(1, BAR_H - 1):
        im.putpixel((0, y), K)
    return im


def wypelnienie(n, tony):
    """Kafel n kolumn: wiersz 0 i 6 = ramka, 1..5 = tony."""
    im = Image.new('RGBA', (n, BAR_H), (0, 0, 0, 0))
    for x in range(n):
        im.putpixel((x, 0), K); im.putpixel((x, BAR_H - 1), K)
        for y in range(1, BAR_H - 1):
            im.putpixel((x, y), tony[y - 1])
    return im


HP_TONY = [
    [(156, 242, 100, 255), (100, 214, 60, 255), (70, 180, 48, 255), (48, 146, 42, 255), (32, 108, 36, 255)],
    [(255, 240, 112, 255), (250, 214, 62, 255), (226, 178, 40, 255), (192, 142, 30, 255), (150, 102, 26, 255)],
    [(255, 188, 92, 255), (250, 144, 48, 255), (224, 110, 30, 255), (190, 78, 28, 255), (148, 52, 28, 255)],
    [(238, 92, 80, 255), (204, 44, 44, 255), (186, 34, 40, 255), (160, 26, 36, 255), (122, 18, 32, 255)],
]
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
        dodaj(f'pc{d}', kafel_plakietki(str(d)), C_CYFRA_PLAKIETKI + d, 8)
        dodaj(f'hc{d}', cyfra_hp(str(d)), C_CYFRA_HP + d, 7)
    dodaj('hc_ukosnik', cyfra_hp('/'), C_CYFRA_HP + 10, 7)
    dodaj('plakietka_l', brzeg_plakietki(True), C_PLAKIETKA, 8)
    dodaj('plakietka_p', brzeg_plakietki(False), C_PLAKIETKA + 1, 8)
    dodaj('ramka_l', koniec_paska(), C_RAMKA, 7)
    dodaj('ramka_p', koniec_paska(), C_RAMKA + 1, 7)
    for i in range(1, BAR_W + 1):
        dodaj(f'duch{i}', wypelnienie(i, DUCH_TONY), C_DUCH + i, 7)
        for stopien, tony in enumerate(HP_TONY):
            dodaj(f'hp{stopien}_{i}', wypelnienie(i, tony), C_HP + stopien * 64 + i, 7)
        dodaj(f'tlo{i}', wypelnienie(i, TLO_TONY), C_TLO + i, 7)
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
    tlo = Image.new('RGBA', (140, 80), (110, 140, 90, 255))

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
    for i, (proc, duch, poz) in enumerate([(80, 0, 1), (50, 70, 12), (25, 25, 25), (10, 10, 100)]):
        kol = kolory[min(i, len(kolory) - 1)]
        x = 2
        def kafel(nazwa, k=None):
            nonlocal x
            im = Image.open(os.path.join(TEX, nazwa + '.png')); wklej(im, x, y, k); x += im.width
        kafel('plakietka_l', kol)
        for ch in str(poz):
            kafel(f'pc{ch}', kol)
        kafel('plakietka_p', kol)
        x += 2; y += 1
        k = max(1, proc * BAR_W // 100); j = max(k, duch * BAR_W // 100)
        kafel('ramka_l'); kafel(f'hp{i}_{k}')
        if j > k: kafel(f'duch{j - k}')
        if j < BAR_W: kafel(f'tlo{BAR_W - j}')
        kafel('ramka_p'); x += 2
        for ch in f'{proc * 14 // 10}/140':
            im = Image.open(os.path.join(TEX, 'hc_ukosnik.png' if ch == '/' else f'hc{ch}.png')); wklej(im, x, y); x += 4
        y += 11
    tlo.resize((tlo.width * S, tlo.height * S), Image.NEAREST).save(os.path.join(H, 'wynik', 'podglad.png'))


if __name__ == '__main__':
    main()
