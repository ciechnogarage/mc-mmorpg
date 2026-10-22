#!/usr/bin/env python3
"""Plotno tekstowe: JEDEN ZNAK = JEDEN PIKSEL. Do edycji pojedynczych pikseli.

Po co to istnieje. Dotychczas rysowalem generatorem: pisalem caly skrypt, renderowalem
finał, ogladalem i latalem skrypt. To jest petla wsadowa — nie da sie w niej ruszyc
jednego piksela, nie da sie sprawdzac w trakcie, a bledy sie kumuluja. Tu rysunek jest
PLIKIEM TEKSTOWYM, ktory edytuje sie linijka po linijce, i po kazdej zmianie widac wynik.

Format pliku .txt:
    # paleta: znak=RRGGBB, po jednym na linie, po '#paleta'
    # potem linie rysunku; kropka = przezroczyste

Uzycie:
    siatka.py podglad <plik.txt> [skala]     -> <plik>_podglad.png (1x,4x,8x + na tle)
    siatka.py png     <plik.txt> <out.png>   -> czysty PNG 1:1
"""
import sys, os
from PIL import Image


def wczytaj(sciezka):
    paleta, wiersze, tryb = {}, [], None
    for ln in open(sciezka, encoding='utf-8').read().splitlines():
        if ln.strip() == '#paleta':
            tryb = 'p'; continue
        if ln.strip() == '#rysunek':
            tryb = 'r'; continue
        if tryb == 'p':
            if '=' in ln:
                k, v = ln.split('=', 1)
                v = v.strip().lstrip('#')
                paleta[k.strip()] = tuple(int(v[i:i+2], 16) for i in (0, 2, 4)) + (255,)
        elif tryb == 'r':
            wiersze.append(ln.rstrip('\n'))
    while wiersze and not wiersze[-1].strip():
        wiersze.pop()
    return paleta, wiersze


def do_obrazu(paleta, wiersze):
    w = max((len(r) for r in wiersze), default=1)
    h = len(wiersze)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, r in enumerate(wiersze):
        for x, ch in enumerate(r):
            if ch in paleta:
                px[x, y] = paleta[ch]
            elif ch not in ('.', ' '):
                raise SystemExit(f'BLAD: znak {ch!r} w wierszu {y+1}, kolumna {x+1} nie ma koloru')
    return im


def podglad(im, out, tla=((0x2a, 0x14, 0x0c), (0xec, 0xdd, 0xbd))):
    """1x, 4x i 8x obok siebie, a pod spodem 1x na dwoch tlach, na ktorych obiekt
    faktycznie usiadzie. Sprawdzanie na wlasciwym tle jest polowa roboty —
    sylwetka czytelna na bialym potrafi zniknac na drewnie."""
    m = 8
    gora = [im, im.resize((im.width*4, im.height*4), Image.NEAREST),
            im.resize((im.width*8, im.height*8), Image.NEAREST)]
    szer = sum(g.width for g in gora) + m*(len(gora)+1)
    wys = max(g.height for g in gora) + m*2
    dol_h = im.height*4 + m*2
    out_im = Image.new('RGB', (max(szer, (im.width*4+m)*len(tla)+m), wys+dol_h), (20, 20, 22))
    x = m
    for g in gora:
        out_im.paste(g, (x, m), g); x += g.width + m
    x = m
    for t in tla:
        kafel = Image.new('RGB', (im.width*4, im.height*4), t)
        s = im.resize((im.width*4, im.height*4), Image.NEAREST)
        kafel.paste(s, (0, 0), s)
        out_im.paste(kafel, (x, wys + m)); x += im.width*4 + m
    out_im.save(out)


if __name__ == '__main__':
    cmd, plik = sys.argv[1], sys.argv[2]
    pal, wier = wczytaj(plik)
    im = do_obrazu(pal, wier)
    if cmd == 'png':
        im.save(sys.argv[3])
    else:
        o = os.path.splitext(plik)[0] + '_podglad.png'
        podglad(im, o)
        print(f'{im.width}x{im.height}, kolorow={len({p for p in im.getdata() if p[3]})} -> {o}')
