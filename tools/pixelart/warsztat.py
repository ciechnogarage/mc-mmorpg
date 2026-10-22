#!/usr/bin/env python3
"""Slownik operacji pixel-artowych — to, co Aseprite daje z pudelka.

Powstal jako TEST: czy braki jakosci brały sie z ubogiego slownika operacji
(wtedy Aseprite by je zalatal), czy z oceny wzrokowej (wtedy zakup nic nie da).
Zawiera dokladnie te rzeczy, ktorych brak wytknela analiza Forge of Kingdoms:
rampy z rotacja odcienia, dithering na WSKAZANYM obszarze, kontur, warstwy,
wymuszanie palety i podglad w docelowej skali na docelowym tle.
"""
import colorsys
from PIL import Image

BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def rampa(h0, s0, v0, h1, s1, v1, n):
    """Rampa z ROTACJA ODCIENIA. Nasze dotychczasowe rampy rotowaly odcien o 2.6
    stopnia, referencyjne o 20-27 — dlatego nasz cien byl tylko ciemniejszy,
    a ma byc cieplejszy albo chlodniejszy. Tu odcien, nasycenie i wartosc ida
    osobnymi torami, wiec da sie zrobic 'cienie w fiolet, swiatla w zloto'."""
    out = []
    for i in range(n):
        t = i / max(1, n - 1)
        h = (h0 + (h1 - h0) * t) % 360 / 360.0
        s = s0 + (s1 - s0) * t
        v = v0 + (v1 - v0) * t
        r, g, b = colorsys.hsv_to_rgb(h, s, v)
        out.append((int(r * 255), int(g * 255), int(b * 255), 255))
    return out


def kontur(img, kolor, tylko_zewnetrzny=True):
    """Kontur 1 px dookola nieprzezroczystych pikseli. Analiza wytknela brak
    konturu jako jedna z czterech glownych roznic — kontur trzyma sylwetke
    przy 16 px na ruchliwym tle."""
    w, h = img.size
    px = img.load()
    nowe = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 8:
                continue
            if any(0 <= x + dx < w and 0 <= y + dy < h and px[x + dx, y + dy][3] > 8
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                nowe.append((x, y))
    for (x, y) in nowe:
        px[x, y] = kolor
    return img


def dithering(img, maska, a, b, sila=0.5):
    """Dithering Bayera TYLKO w podanym obszarze. Moj wczesniejszy blad polegal
    na ditherowaniu calego plotna — wychodzil raster jak w druku gazetowym.
    Dithering ma siedziec na STYKU dwoch materialow, pasem kilku pikseli."""
    px = img.load()
    for (x, y) in maska:
        if 0 <= x < img.size[0] and 0 <= y < img.size[1]:
            prog = (BAYER4[y & 3][x & 3] + 0.5) / 16.0
            px[x, y] = b if prog < sila else a
    return img


def warstwy(rozmiar, elementy):
    """Skladanie warstw: [(obraz, (x,y)), ...]. Pozwala pracowac nad jednym
    elementem bez przerysowywania reszty — to jest ta czesc, ktora w Aseprite
    daja warstwy i pliki .ase."""
    out = Image.new('RGBA', rozmiar, (0, 0, 0, 0))
    for im, poz in elementy:
        out.alpha_composite(im, poz)
    return out


def wymus_palete(img, paleta):
    """Przyciaga kazdy piksel do najblizszego koloru z palety. Gwarantuje, ze
    zadna operacja nie przemyci koloru spoza zestawu."""
    px = img.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            if a <= 8:
                continue
            px[x, y] = min(paleta, key=lambda c: (c[0]-r)**2 + (c[1]-g)**2 + (c[2]-b)**2)
    return img


def podglad(img, sciezka, tla=((0x2a, 0x14, 0x0c), (0x52, 0x2c, 0x1b), (0xec, 0xdd, 0xbd))):
    """1x / 4x / 8x + wersja na tlach, na ktorych obiekt naprawde usiadzie.
    Sprawdzanie na wlasciwym tle to polowa roboty: sylwetka czytelna na bialym
    potrafi zniknac na drewnie."""
    m = 8
    gora = [img, img.resize((img.width*4, img.height*4), Image.NEAREST),
            img.resize((img.width*8, img.height*8), Image.NEAREST)]
    szer = max(sum(g.width for g in gora) + m*(len(gora)+1),
               (img.width*4+m)*len(tla)+m)
    wys_g = max(g.height for g in gora) + 2*m
    out = Image.new('RGB', (szer, wys_g + img.height*4 + 2*m), (22, 22, 24))
    x = m
    for g in gora:
        out.paste(g, (x, m), g); x += g.width + m
    x = m
    for t in tla:
        kafel = Image.new('RGB', (img.width*4, img.height*4), t)
        s = img.resize((img.width*4, img.height*4), Image.NEAREST)
        kafel.paste(s, (0, 0), s)
        out.paste(kafel, (x, wys_g + m)); x += img.width*4 + m
    out.save(sciezka)
    return out
