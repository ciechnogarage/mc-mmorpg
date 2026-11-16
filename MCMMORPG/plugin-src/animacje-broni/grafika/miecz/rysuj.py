#!/usr/bin/env python3
"""Trzy natywnie rysowane warianty miecza 16x16 i arkusz porownawczy."""

from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

from paleta import OBRYS, Y
from kontrast import skoki


KATALOG = Path(__file__).resolve().parent
JAR = Path('/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar')

# Stal klingi, trzy tony mosieznych okuc i skora rekojesci.
PALETA = {
    'O': OBRYS,
    'd': (68, 76, 90),
    'm': (110, 130, 148),
    'l': (147, 167, 181),
    'h': (198, 213, 216),
    'q': (171, 188, 195),
    'k': (83, 65, 49),
    'n': (149, 117, 76),
    'u': (194, 158, 100),
    's': (94, 58, 48),
    'b': (142, 88, 57),
    't': (201, 143, 83),
}


def puste():
    return [['.' for _ in range(16)] for _ in range(16)]


def punkty(g, symbol, wspolrzedne):
    for x, y in wspolrzedne:
        assert 0 <= x < 16 and 0 <= y < 16
        g[y][x] = symbol


def klinga(g, wiersze, blik):
    """Rysuj poziome przekroje po siatce 16x16, bez skalowania."""
    for y, lewy, prawy in wiersze:
        for x in range(lewy, prawy + 1):
            offset = x - lewy
            szerokosc = prawy - lewy + 1
            if offset == 1 and (x, y) in blik:
                g[y][x] = 'q'
            elif offset == 0 or offset == 1:
                g[y][x] = 'l'
            elif offset == szerokosc - 2:
                g[y][x] = 'd' if szerokosc >= 5 else 'm'
            elif offset == szerokosc - 3 and szerokosc >= 5:
                g[y][x] = 'm'
            else:
                g[y][x] = 'l'


def poszerz_okucia(g, jelec, glowica):
    """Rdzen + jeden piksel obrzeza: metal pozostaje wewnatrz konturu."""
    komorki = set(glowica)
    for x, y in jelec:
        for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            xx, yy = x + dx, y + dy
            if 0 <= xx < 16 and 0 <= yy < 16:
                komorki.add((xx, yy))
    for x, y in komorki:
        g[y][x] = 'k'
    return komorki


def wypelnij_okucia(g, komorki):
    for x, y in komorki:
        if g[y][x] == 'O':
            continue
        # Zgodnie z kierunkiem swiatla: lewa/gorna strona jasniejsza.
        przy_skorze = any(0 <= x + dx < 16 and 0 <= y + dy < 16 and
                          g[y + dy][x + dx] in ('s', 'b', 't')
                          for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)))
        jasne = not przy_skorze and (x - 1, y) in komorki and (x, y - 1) in komorki
        g[y][x] = 'u' if jasne else 'n'
    # Jednopikselowe tony wsrod okuc wygladaja jak szum, nie jak faseta.
    poprawki = []
    for x, y in komorki:
        ton = g[y][x]
        if ton not in ('n', 'u'):
            continue
        sasiedzi = [g[yy][xx] for yy in range(max(0, y - 1), min(16, y + 2))
                    for xx in range(max(0, x - 1), min(16, x + 2))
                    if (xx, yy) != (x, y)]
        if ton not in sasiedzi:
            drugi = 'u' if ton == 'n' else 'n'
            if drugi in sasiedzi:
                poprawki.append((x, y, drugi))
    for x, y, ton in poprawki:
        g[y][x] = ton


def kontur(g):
    stara = [r[:] for r in g]
    for y in range(16):
        for x in range(16):
            if stara[y][x] == '.':
                continue
            if any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or
                   stara[y + dy][x + dx] == '.'
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                g[y][x] = 'O'
    return g


def miecz_a():
    """Prosty zolnierski: klinga srednia, poprzeczny jelec, okragla glowica."""
    g = puste()
    klinga(g, [(0, 15, 15), (1, 13, 15), (2, 12, 15), (3, 11, 15),
               (4, 10, 14), (5, 9, 13), (6, 8, 12), (7, 7, 11),
               (8, 6, 10), (9, 5, 9)], {(13, 2), (12, 3), (11, 4)})
    for y, a, b in [(10, 5, 7), (11, 4, 6), (12, 3, 5),
                    (13, 2, 4), (14, 1, 3)]:
        for x in range(a, b + 1):
            g[y][x] = 's' if x == b else 'b' if x == a else 't'
    punkty(g, 'd', [(0, 14), (1, 14), (2, 14), (0, 15), (1, 15), (2, 15)])
    punkty(g, 'm', [(1, 14), (1, 15)])
    # Dwa krotkie ramiona jelca i stalowy wezel u nasady.
    punkty(g, 'd', [(2, 7), (3, 7), (2, 8), (3, 8), (4, 8), (3, 9),
                    (4, 9), (5, 9), (6, 9), (7, 9), (6, 10), (7, 10),
                    (8, 10), (7, 11), (8, 11), (9, 11), (8, 12), (9, 12)])
    punkty(g, 'm', [(3, 8), (4, 9), (5, 9), (6, 10), (7, 10), (8, 11)])
    punkty(g, 'l', [(3, 7), (4, 8), (5, 9), (7, 10)])
    okucia = poszerz_okucia(g,
        [(2, 7), (3, 8), (4, 8), (4, 9), (5, 9), (6, 9), (7, 10), (8, 11), (9, 12)],
        [(x, y) for y, a, b in [(13, 0, 2), (14, 0, 3), (15, 0, 3)]
         for x in range(a, b + 1)])
    kontur(g)
    punkty(g, 'm', [(6, 9), (7, 9), (6, 10), (7, 10), (7, 11), (8, 11)])
    punkty(g, 'l', [(3, 7), (3, 8)])
    punkty(g, 's', [(6, 11), (5, 12), (4, 13)])
    punkty(g, 'b', [(5, 11), (4, 12), (3, 13)])
    punkty(g, 't', [(2, 13), (2, 14)])
    wypelnij_okucia(g, okucia)
    return g


def miecz_b():
    """Ciezszy: szersza klinga, krotki hakowaty jelec, kwadratowa glowica."""
    g = puste()
    klinga(g, [(0, 15, 15), (1, 13, 15), (2, 11, 15), (3, 10, 15),
               (4, 9, 14), (5, 8, 13), (6, 7, 12), (7, 6, 11),
               (8, 5, 10), (9, 5, 9)], {(12, 2), (11, 3), (10, 4)})
    for y, a, b in [(10, 5, 7), (11, 4, 6), (12, 3, 5),
                    (13, 2, 4), (14, 1, 3)]:
        for x in range(a, b + 1):
            g[y][x] = 's' if x == b else 'b' if x == a else 't'
    punkty(g, 'd', [(0, 13), (1, 13), (0, 14), (1, 14), (2, 14),
                    (0, 15), (1, 15), (2, 15), (3, 15)])
    punkty(g, 'm', [(1, 14), (2, 14), (1, 15), (2, 15)])
    punkty(g, 'd', [(2, 8), (2, 9), (3, 9), (4, 9), (4, 10), (5, 10),
                    (6, 10), (7, 10), (8, 10), (8, 11), (9, 11),
                    (9, 12), (10, 12), (10, 13)])
    punkty(g, 'm', [(3, 9), (4, 10), (5, 10), (6, 10), (8, 11), (9, 12)])
    punkty(g, 'l', [(2, 8), (3, 9), (5, 10), (8, 10)])
    okucia = poszerz_okucia(g,
        [(2, 8), (3, 9), (4, 9), (4, 10), (5, 10), (6, 10),
         (7, 10), (8, 11), (9, 12), (10, 13)],
        [(x, y) for y in (13, 14, 15) for x in range(4)])
    kontur(g)
    punkty(g, 'm', [(5, 11), (5, 10), (6, 10), (1, 14), (2, 14)])
    punkty(g, 's', [(6, 11), (5, 12), (4, 13)])
    punkty(g, 'b', [(4, 12), (3, 13)])
    punkty(g, 'b', [(3, 12)])
    punkty(g, 't', [(2, 13)])
    wypelnij_okucia(g, okucia)
    punkty(g, 'n', [(5, 10), (5, 11)])
    return g


def miecz_c():
    """Szybszy: wezszą klinga, smukly zamiatany jelec, dyskowa glowica."""
    g = puste()
    klinga(g, [(0, 15, 15), (1, 13, 15), (2, 12, 15), (3, 11, 14),
               (4, 10, 13), (5, 9, 12), (6, 8, 11), (7, 7, 10),
               (8, 6, 9), (9, 5, 8)], {(13, 2), (12, 3), (11, 4)})
    for y, a, b in [(10, 5, 7), (11, 4, 6), (12, 3, 5),
                    (13, 2, 4), (14, 1, 3)]:
        for x in range(a, b + 1):
            g[y][x] = 's' if x == b else 'b' if x == a else 't'
    punkty(g, 'd', [(0, 14), (1, 14), (2, 14), (3, 14),
                    (0, 15), (1, 15), (2, 15)])
    punkty(g, 'm', [(1, 14), (2, 14), (1, 15)])
    punkty(g, 'd', [(3, 7), (4, 7), (3, 8), (4, 8), (5, 8), (5, 9),
                    (6, 9), (7, 9), (7, 10), (8, 10), (8, 11),
                    (9, 11), (9, 12)])
    punkty(g, 'm', [(4, 7), (4, 8), (5, 9), (6, 9), (7, 10), (8, 11)])
    punkty(g, 'l', [(4, 7), (5, 8), (6, 9), (8, 10)])
    okucia = poszerz_okucia(g,
        [(3, 7), (4, 7), (4, 8), (5, 8), (5, 9),
         (6, 9), (7, 10), (8, 11), (9, 12)],
        [(x, y) for y, a, b in [(13, 1, 2), (14, 0, 3), (15, 0, 2)]
         for x in range(a, b + 1)])
    kontur(g)
    punkty(g, 'm', [(7, 9), (6, 10)])
    punkty(g, 's', [(6, 11), (5, 12), (4, 13)])
    punkty(g, 'b', [(5, 11), (4, 12), (3, 13)])
    punkty(g, 't', [(2, 13), (2, 14)])
    wypelnij_okucia(g, okucia)
    return g


def obraz(g):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(g):
        for x, znak in enumerate(r):
            if znak != '.':
                im.putpixel((x, y), (*PALETA[znak], 255))
    return im


def arkusz(warianty):
    with ZipFile(JAR) as z:
        vanilla = Image.open(BytesIO(z.read('assets/minecraft/textures/item/iron_sword.png'))).convert('RGBA')
    szer, wys = 1320, 594
    out = Image.new('RGB', (szer, wys), (205, 208, 212))
    draw = ImageDraw.Draw(out)
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 14)
    font_big = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 17)
    tla = [('TLO NEUTRALNE', (205, 208, 212), (25, 24, 30)),
           ('TLO CIEMNE', (31, 33, 38), (226, 225, 222)),
           ('SLOT VANILLA (139,139,139)', (139, 139, 139), (25, 24, 30))]
    for pas, (etykieta, bg, fg) in enumerate(tla):
        y0 = 12 + pas * 198
        draw.rectangle((0, pas * 198, szer, (pas + 1) * 198 - 1), fill=bg)
        draw.text((14, y0), etykieta, font=font_big, fill=fg)
        for i, (nazwa, im) in enumerate(warianty.items()):
            x0 = 14 + i * 440
            stary = Image.open(KATALOG / f'miecz_{nazwa}_v1.png').convert('RGBA')
            draw.text((x0, y0 + 24), f'MIECZ {nazwa.upper()}', font=font_big, fill=fg)
            for j, (podpis, ikona) in enumerate((('V1', stary), ('NOWY', im), ('VANILLA', vanilla))):
                xx = x0 + j * 142
                draw.text((xx, y0 + 49), podpis, font=font, fill=fg)
                draw.rectangle((xx + 101, y0 + 50, xx + 119, y0 + 68), outline=fg)
                out.paste(ikona, (xx + 103, y0 + 52), ikona)
                zoom = ikona.resize((112, 112), Image.Resampling.NEAREST)
                out.paste(zoom, (xx, y0 + 70), zoom)
    out.save(KATALOG / 'arkusz.png')


def main():
    warianty = {}
    for nazwa, funkcja in [('miecz_a', miecz_a), ('miecz_b', miecz_b), ('miecz_c', miecz_c)]:
        g = funkcja()
        im = obraz(g)
        im.save(KATALOG / f'{nazwa}.png')
        warianty[nazwa[-1]] = im
        print(f'== {nazwa}: kontrast (>60 Y; bez obrysu)')
        skoki(g, PALETA, znaki='dmlhqknusbt', prog=60)
    arkusz(warianty)
    print('Odstepy Y zelaza:', [round(abs(Y(PALETA[a]) - Y(PALETA[b]))) for a, b in
                               [('d', 'm'), ('m', 'l'), ('l', 'h')]])
    print('Odstepy Y skory:', [round(abs(Y(PALETA[a]) - Y(PALETA[b]))) for a, b in
                              [('s', 'b'), ('b', 't')]])


if __name__ == '__main__':
    main()
