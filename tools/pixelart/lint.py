#!/usr/bin/env python3
"""Linter sprite'a: pomiary zamiast oka. Uzycie: lint.py plik.png [plik2.png ...]

Sprawdza:
  kolory      liczba kolorow nieprzezroczystych
  sieroty     piksele bez ani jednego sasiada (8-sasiedztwo) w tym samym kolorze
  duplikaty   pary SASIADUJACYCH kolorow z roznica jasnosci Y < 15 (niechciany AA)
  zmiany      najwiecej zmian koloru w jednym rzedzie (szum)
  profil      najdluzszy prosty odcinek lewej i prawej krawedzi sylwetki
  tlo         udzial pikseli brzegu z kontrastem |dY| < 25 wobec tla
              (slot MC Y139, ciemne Y28, tooltip Y6) — tyle sylwetki "znika"
"""
import sys
from PIL import Image

Y = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
TLA = {'slot': 139, 'ciemne': 28, 'tooltip': 6}


def lint(sciezka):
    im = Image.open(sciezka).convert('RGBA')
    w, h = im.size
    px = im.load()
    op = lambda x, y: 0 <= x < w and 0 <= y < h and px[x, y][3] > 0
    kol = {px[x, y][:3] for y in range(h) for x in range(w) if op(x, y)}

    sieroty = []
    dup = {}
    for y in range(h):
        for x in range(w):
            if not op(x, y):
                continue
            c = px[x, y][:3]
            sas = [(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1) if (dx or dy)]
            sas_op = [s for s in sas if op(*s)]
            if sas_op and not any(px[s][:3] == c for s in sas_op):
                sieroty.append((x, y))
            for s in ((x + 1, y), (x, y + 1)):
                if op(*s):
                    d = px[s][:3]
                    if d != c and abs(Y(c) - Y(d)) < 15:
                        k = tuple(sorted((c, d)))
                        dup[k] = dup.get(k, 0) + 1

    zmiany = 0
    for y in range(h):
        rz = [px[x, y][:3] for x in range(w) if op(x, y)]
        zmiany = max(zmiany, sum(1 for a, b in zip(rz, rz[1:]) if a != b))

    def najdluzszy(bok):
        xs = []
        for y in range(h):
            r = [x for x in range(w) if op(x, y)]
            if r:
                xs.append(r[0] if bok == 'L' else r[-1])
        best = run = 1
        for a, b in zip(xs, xs[1:]):
            run = run + 1 if a == b else 1
            best = max(best, run)
        return best

    brzeg = [(x, y) for y in range(h) for x in range(w) if op(x, y) and
             any(not op(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    tlo = {n: round(100 * sum(1 for p in brzeg if abs(Y(px[p]) - v) < 25) / max(1, len(brzeg)))
           for n, v in TLA.items()}

    print(f'== {sciezka.split("/")[-1]}  {w}x{h}')
    print(f'   kolory {len(kol)} | sieroty {len(sieroty)} {sieroty[:8]} | zmiany/rzad max {zmiany}')
    print(f'   duplikaty (dY<15, sasiadujace): {len(dup)} ' +
          ' '.join(f'{a}/{b}x{n}' for (a, b), n in list(dup.items())[:4]))
    print(f'   profil: najdluzsza prosta L={najdluzszy("L")} P={najdluzszy("P")} px')
    print(f'   brzeg zlewa sie z tlem (% brzegu): ' + ', '.join(f'{k} {v}%' for k, v in tlo.items()))


for p in sys.argv[1:]:
    lint(p)
