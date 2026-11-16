#!/usr/bin/env python3
"""Ocena ikon: arkusz w powiekszeniu, test sylwety przy realnych 16 px,
podglad w prawdziwym slocie panelu oraz POMIARY.

Po co pomiary: "wyglada slabo" nie mowi, co poprawic. Te liczby mowia.
  - kolorow          — komercyjna ikona 32x32 ma 10-20; wiecej = brud
  - srednia jasnosc  — musi odstawac od dna wneki (ok. 30 L), inaczej ikona tonie
  - rozpietosc       — ponizej ~120 L nie ma modelunku, bryla jest plaska
  - pikseli @16      — ile zostaje po zejsciu do realnej wielkosci; <60 = ikona chuda
  - kontur           — ile % obrysu ma najciemniejszy kolor; <90% = rozmyta krawedz

Uzycie: python3 kontrola.py <katalog-z-png-32x32> [<panel.png> <ox> <oyt>]
"""
import sys, glob, os
from PIL import Image

KAT = sys.argv[1]
PANEL = sys.argv[2] if len(sys.argv) > 2 else None
OX = int(sys.argv[3]) if len(sys.argv) > 3 else 0
OYT = int(sys.argv[4]) if len(sys.argv) > 4 else 0
TLO = (34, 31, 30, 255)          # kolor dna wneki menu — na tym ikona ma sie wybic


def lum(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


import re
# Pomijamy pliki pomocnicze: powiekszenia (*_8x.png) i wyniki kontroli (_arkusz...).
def _roboczy(p):
    b = os.path.basename(p)
    return not b.startswith('_') and not re.search(r'_\d+x\.png$', b)


pliki = sorted(p for p in glob.glob(os.path.join(KAT, '*.png'))
               if _roboczy(p) and Image.open(p).size == (32, 32))
if not pliki:
    print('brak ikon 32x32 w', KAT); sys.exit(1)

print(f'{"ikona":14} {"kolorow":>7} {"jasnosc":>8} {"rozpietosc":>11} {"px@16":>6} {"kontur":>7}')
obrazki = []
for p in pliki:
    im = Image.open(p).convert('RGBA')
    obrazki.append((os.path.basename(p)[:-4], im))
    widoczne = [c for c in im.getdata() if c[3] > 8]
    L = sorted(lum(c) for c in widoczne)
    mini = im.resize((16, 16), Image.NEAREST)
    n16 = sum(1 for c in mini.getdata() if c[3] > 8)
    # kontur: piksele bryly sasiadujace z przezroczystoscia
    px = im.load(); obrys = []
    for y in range(32):
        for x in range(32):
            if px[x, y][3] > 8 and any(
                    not (0 <= x + dx < 32 and 0 <= y + dy < 32) or px[x + dx, y + dy][3] <= 8
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                obrys.append(lum(px[x, y]))
    ciemny = sum(1 for v in obrys if v < 25) / max(1, len(obrys)) * 100
    print(f'{os.path.basename(p)[:-4]:14} {len(set(widoczne)):7} {sum(L)/len(L):8.1f} '
          f'{L[-1]-L[0]:11.1f} {n16:6} {ciemny:6.0f}%')

# --- arkusz 8x
sheet = Image.new('RGBA', (32 * len(obrazki), 32), TLO)
for i, (_, im) in enumerate(obrazki):
    sheet.alpha_composite(im, (i * 32, 0))
sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(
    os.path.join(KAT, '_arkusz_8x.png'))

# --- sylwety przy realnych 16 px
s16 = Image.new('RGBA', (16 * len(obrazki), 16), TLO)
for i, (_, im) in enumerate(obrazki):
    s16.alpha_composite(im.resize((16, 16), Image.NEAREST), (i * 16, 0))
s16.resize((s16.width * 8, s16.height * 8), Image.NEAREST).save(
    os.path.join(KAT, '_sylwety_16px.png'))

# --- podglad w prawdziwych slotach panelu
if PANEL and os.path.exists(PANEL):
    pan = Image.open(PANEL).convert('RGBA')
    for k, (_, im) in enumerate(obrazki[:9]):
        pan.alpha_composite(im.resize((16, 16), Image.NEAREST), (OX + 8 + 18 * k, OYT + 36))
    for k, (_, im) in enumerate(obrazki[:3]):
        pan.alpha_composite(im.resize((16, 16), Image.NEAREST), (OX + 8 + 18 * (k * 4), OYT + 54))
    pan.resize((pan.width * 3, pan.height * 3), Image.NEAREST).save(
        os.path.join(KAT, '_w_panelu_3x.png'))

print('\nzapisane:', os.path.join(KAT, '_arkusz_8x.png'), '/ _sylwety_16px.png',
      '/ _w_panelu_3x.png' if PANEL else '')
