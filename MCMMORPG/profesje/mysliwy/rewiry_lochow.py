#!/usr/bin/env python3
"""Miejsca na rewiry mysliwego w mapach MythicDungeons (tylko odczyt plikow regionow) — NIE wdraza.
Low potrzebuje ~40 blokow drogi: 4 tropy co 5,5 bloku (odchylenie do 28 st.) + zwierze 17 blokow za ostatnim
(Mysliwy.java: wytyczTrop, ODLEGLOSC_ZWIERZA). Tropy ida po prostej i szukaja gruntu +-4 bloki (ziemia()),
wiec w ciasnym lochu przeszlyby przez sciany. Skrypt liczy:
 - chodliwe pola (blok staly, nad nim 2 przepuszczalne), spojne obszary (sasiedzi 4, skok <= 1),
 - z kazdego punktu kandydata: najdluzsza PROSTA chodliwa linia w 8 kierunkach (bez przechodzenia przez sciane).
Kandydaci: punkty walk z wilkami z functions.yml mapy (mythicSkill zawierajace 'starcie'/'populacja'/'zasadzka')
oraz srodki najwiekszych obszarow.
Uzycie: rewiry_lochow.py [katalog_map=/opt/docker/mc/data/world1/plugins/MythicDungeons/maps] [min_linia=39]
"""
import os, sys, glob, re, collections
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../kuznia/tools'))
import mca

KAT = sys.argv[1] if len(sys.argv) > 1 else '/opt/docker/mc/data/world1/plugins/MythicDungeons/maps'
MIN = int(sys.argv[2]) if len(sys.argv) > 2 else 39
PRZEP = re.compile(r'(air|grass$|short_grass|tall_grass|fern|flower|dandelion|poppy|tulip|orchid|allium|bluet|daisy|'
                   r'cornflower|lily_of|carpet|vine|sapling|mushroom$|torch|button|rail|sign|leaf_litter|bush$|dead_bush|snow$)')
CIECZ = {'minecraft:water', 'minecraft:lava'}

def przep(n): return n is None or (n not in CIECZ and bool(PRZEP.search(n.split(':')[-1])))

def bloki(d):
    out = {}
    for p in glob.glob(os.path.join(d, 'region', 'r.*.mca')):
        r = mca.Region(p)
        for cx, cz in r.chunks():
            ch = r.chunk(cx & 31, cz & 31)
            if not ch: continue
            for sec in ch.get('sections', []):
                pal = (sec.get('block_states') or {}).get('palette') or []
                if not pal or all(b.get('Name') in mca.AIR for b in pal): continue
                sy = sec.get('Y', 0)
                for ly in range(16):
                    for lz in range(16):
                        for lx in range(16):
                            n = mca._section_block(sec, lx, ly, lz)
                            if n and n not in mca.AIR: out[(cx * 16 + lx, sy * 16 + ly, cz * 16 + lz)] = n
    return out

def chodliwe(b):
    ch = set()
    for (x, y, z), n in b.items():
        if przep(n) or n in CIECZ: continue
        if przep(b.get((x, y + 1, z))) and przep(b.get((x, y + 2, z))): ch.add((x, y + 1, z))
    return ch

def obszary(ch):
    po = collections.defaultdict(list)
    for (x, y, z) in ch: po[(x, z)].append(y)
    widz, wyn = set(), []
    for c in ch:
        if c in widz: continue
        q, ob = [c], []
        widz.add(c)
        while q:
            x, y, z = q.pop(); ob.append((x, y, z))
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for ny in po.get((x + dx, z + dz), []):
                    n = (x + dx, ny, z + dz)
                    if abs(ny - y) <= 1 and n not in widz: widz.add(n); q.append(n)
        wyn.append(ob)
    return sorted(wyn, key=len, reverse=True)

def linia(ch, po, x, y, z, dx, dz):
    n = 0
    while n < 80:
        nx, nz = x + dx, z + dz
        ys = [yy for yy in po.get((nx, nz), []) if abs(yy - y) <= 1]
        if not ys: break
        x, z, y = nx, nz, min(ys, key=lambda yy: abs(yy - y)); n += 1
    return n * (1.414 if dx and dz else 1)

def najblizsze(ch, x, y, z):
    return min(ch, key=lambda c: (c[0] - x) ** 2 + (c[2] - z) ** 2 + 4 * (c[1] - y) ** 2) if ch else None

KIER = [(1, 0, '+X'), (-1, 0, '-X'), (0, 1, '+Z'), (0, -1, '-Z'), (1, 1, '+X+Z'), (1, -1, '+X-Z'), (-1, 1, '-X+Z'), (-1, -1, '-X-Z')]

for d in sorted(glob.glob(os.path.join(KAT, '*/'))):
    nazwa = os.path.basename(d.rstrip('/'))
    if not glob.glob(os.path.join(d, 'region', 'r.*.mca')): print(f'{nazwa}: brak regionow'); continue
    b = bloki(d)
    if not b: print(f'{nazwa}: regiony bez blokow (teren z generatora, nie do policzenia z pliku)'); continue
    ch = chodliwe(b)
    po = collections.defaultdict(list)
    for (x, y, z) in ch: po[(x, z)].append(y)
    obs = obszary(ch)
    print(f'{nazwa}: blokow {len(b)}, chodliwych pol {len(ch)}, obszarow {len(obs)}; najwieksze: ' +
          ', '.join(f'{len(o)} pol x{min(c[0] for c in o)}..{max(c[0] for c in o)} z{min(c[2] for c in o)}..{max(c[2] for c in o)}' for o in obs[:3]))
    kand = []
    fy = os.path.join(d, 'functions.yml')
    if os.path.exists(fy):
        t = open(fy, encoding='utf-8').read()
        for m in re.finditer(r'mythicSkill: (\S+)[\s\S]*?x: (-?[\d.]+)\s+y: (-?[\d.]+)\s+z: (-?[\d.]+)', t):
            if re.search(r'starcie|populacja|zasadzka', m.group(1)): kand.append((m.group(1), *map(lambda v: int(float(v)), m.group(2, 3, 4))))
    for o in obs[:2]:
        sx = sum(c[0] for c in o) // len(o); sz = sum(c[2] for c in o) // len(o); sy = sum(c[1] for c in o) // len(o)
        kand.append((f'srodek obszaru {len(o)} pol', sx, sy, sz))
    for (opis, x, y, z) in kand:
        c = najblizsze(ch, x, y, z)
        if not c: continue
        dl = sorted(((linia(ch, po, *c, dx, dz), k) for dx, dz, k in KIER), reverse=True)
        ok = dl[0][0] >= MIN
        print(f'  {opis} @{x},{y},{z} -> grunt {c[0]},{c[1]},{c[2]}: najdluzsza prosta {dl[0][0]:.0f} bl. ({dl[0][1]}), druga {dl[1][0]:.0f} ({dl[1][1]})'
              + (f'  OK: rzemioslo rewir {nazwa}* {c[0]} {c[1]} {c[2]} BEAST_WILD_BOAR trwale' if ok else f'  ZA CIASNO (< {MIN})'))
