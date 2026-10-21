#!/usr/bin/env python3
"""Metryki upieczonego lochu z plikow regionow (bez zrzutow). Czyta spec z make_<loch>.py.
Uzycie: loch_metrics.py <spec.json> <katalog region/>
Sprawdza (spec gry sekcja 2): zgodnosc stref, lity blok w pustce, plaskosc polan sekcji,
ciaglosc trasy (skok gruntu <= 1 blok miedzy kolejnymi probkami), zasloniecie areny, szerokosc gardzieli bramy.
Wynik: linie PASS/FAIL + podsumowanie; kod wyjscia 1 przy jakimkolwiek FAIL."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import TOOLS  # noqa: E402
import json, math, sys
sys.path.insert(0, TOOLS)
import mca

spec = json.load(open(sys.argv[1]))
W = mca.World(sys.argv[2])
sys.path.insert(0, TOOLS)
gen = __import__('make_' + spec['id'].replace('_', ''))       # make_loch001
SOFT = ("leaves", "grass", "petals", "flower", "bush", "allium", "poppy", "dandelion", "orchid", "tulip", "daisy",
        "bluet", "cornflower", "lily", "rose", "lilac", "peony", "fern", "wildflowers", "log", "wood", "moss_carpet",
        "vine", "sapling", "mushroom")
DER2Z = {("minecraft:" + v.lower()): k for k, v in spec['derivative'].items()}
wyniki = []


def ok(nazwa, warunek, opis):
    wyniki.append((warunek, nazwa, opis))


def ground(x, z):
    c = mca.column(W, x, z)
    if not c:
        return None
    for y, b in c:
        if not any(s in b for s in SOFT):
            return y, b
    return None


# 1. strefy + pustka
dobre = zle = pustka_lita = 0
for x in range(-170, 171, 6):
    for z in range(-170, 171, 6):
        exp = gen.zone_at(x, z)
        c = mca.column(W, x, z)
        if c is None:
            continue
        if exp == 'pustka':
            pustka_lita += any(not any(s in b for s in ('leaves', 'log', 'wood')) for _, b in c)
            continue
        # MC trzyma biomy w komorkach 4x4x4 - przy granicy stref odczyt z definicji sie rozjezdza; pomijamy pas 4 bl.
        if any(gen.zone_at(x + dx, z + dz) != exp for dx in (-4, 0, 4) for dz in (-4, 0, 4)):
            continue
        g = ground(x, z)
        got = DER2Z.get(mca.biome(W, x, g[0], z)) if g else None
        dobre += exp == got; zle += exp != got
ok('strefy', dobre / max(dobre + zle, 1) >= 0.97, f'{100 * dobre / max(dobre + zle, 1):.1f}% zgodnych ({dobre}/{dobre + zle})')
ok('pustka', pustka_lita == 0, f'lity blok w pustce: {pustka_lita}')

# 2. plaskosc polan sekcji (walka wymaga miejsca na uniki)
for s in spec['sekcje']:
    (sx, sz), r = s['xz'], max(3, s['r'] - 2)
    ys = [g[0] for dx in range(-r, r + 1) for dz in range(-r, r + 1) if dx * dx + dz * dz <= r * r
          for g in [ground(sx + dx, sz + dz)] if g]
    if not ys:
        ok(f"polana {s['id']}", False, 'brak terenu'); continue
    rozrzut = max(ys) - min(ys)
    ok(f"polana {s['id']}", rozrzut <= 2 and len(ys) > 0.9 * math.pi * r * r * 0.9,
       f"y={min(ys)}-{max(ys)} (rozrzut {rozrzut}), grunt w {len(ys)} pkt")

# 3. ciaglosc trasy: probka co 1 blok wzdluz lamanej; skok > 1 = gracz musi skakac
def probki(pts):
    for i in range(len(pts) - 1):
        (ax, az), (bx, bz) = pts[i], pts[i + 1]
        n = max(1, int(math.hypot(bx - ax, bz - az)))
        for k in range(n):
            yield round(ax + (bx - ax) * k / n), round(az + (bz - az) * k / n)
for nazwa, pts in (('trasa', spec['trasa']), ('odnoga', spec['odnoga'])):
    prev = None; skoki = []; brak = 0
    for x, z in probki(pts):
        g = ground(x, z)
        if not g:
            brak += 1; continue
        if prev is not None and abs(g[0] - prev) > 1:
            skoki.append((x, z, prev, g[0]))
        prev = g[0]
    ok(f'ciaglosc {nazwa}', not skoki and not brak, f'skoki>1: {len(skoki)} {skoki[:3]}, dziury: {brak}')

# 4. arena: nic wysokiego nad polem walki
ax, az = next(s['xz'] for s in spec['sekcje'] if s['id'] == 'arena')
cov = tot = 0
for dx in range(-24, 25, 2):
    for dz in range(-24, 25, 2):
        if dx * dx + dz * dz > 24 * 24:
            continue
        c = mca.column(W, ax + dx, az + dz); g = ground(ax + dx, az + dz); tot += 1
        cov += bool(c and g and c[0][0] > g[0] + 3)
ok('arena odslonieta', cov / max(tot, 1) <= 0.05, f'zakryte {100 * cov / max(tot, 1):.0f}%')

# 5. gardziel bramy - mierzona dopiero po wklejeniu budowli (teren jej nie robi). Tu tylko: jest grunt na osi.
bx, bz = next(s['xz'] for s in spec['sekcje'] if s['id'] == 'brama')
gb = ground(bx, bz)
ok('grunt na osi bramy', bool(gb), f'y={gb[0] if gb else None} (gardziel = budowla, mierzona po wklejeniu)')

zle_n = sum(1 for w, _, _ in wyniki if not w)
for w, n, o in wyniki:
    print(('PASS ' if w else 'FAIL ') + f'{n:22s} {o}')
print(f'WYNIK: {len(wyniki) - zle_n}/{len(wyniki)} PASS')
sys.exit(1 if zle_n else 0)
