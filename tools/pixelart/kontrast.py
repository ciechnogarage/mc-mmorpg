#!/usr/bin/env python3
"""Pomiar na biezaco: pary SASIADUJACYCH pikseli wewnatrz wybranego obszaru (znaki siatki),
miedzy ktorymi jasnosc skacze bardziej niz prog. Uzycie w skrypcie:
    from kontrast import skoki; skoki(siatka, pal, znaki='LMS1234', prog=60, pomin='W1')"""


def skoki(siatka, pal, znaki, prog=60, pomin='W'):
    Y = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    wyn = {}
    for y, r in enumerate(siatka):
        for x, a in enumerate(r):
            if a not in znaki or a in pomin:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if y + dy < len(siatka) and x + dx < len(r):
                    b = siatka[y + dy][x + dx]
                    if b in znaki and b not in pomin and b != a:
                        d = abs(Y(pal[a]) - Y(pal[b]))
                        if d > prog:
                            wyn.setdefault(tuple(sorted((a, b))), []).append((x, y))
    for (a, b), lst in sorted(wyn.items(), key=lambda kv: -len(kv[1])):
        print(f'   skok {a}/{b} dY={abs(Y(pal[a]) - Y(pal[b])):.0f}: {len(lst)}x  np. {lst[:4]}')
    if not wyn:
        print('   brak skokow ponad prog')
    return wyn
