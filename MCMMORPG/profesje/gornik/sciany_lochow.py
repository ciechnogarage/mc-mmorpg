#!/usr/bin/env python3
"""Miejsca na przodki gornika w mapach MythicDungeons (tylko odczyt plikow regionow) — NIE wdraza.
Przodek potrzebuje (Gornik.java: twarz, wytycz, zywaSciana):
 - punkt P = blok skaly na wysokosci piersi: przed nim (w poziomie) wolne pole, pod tym polem wolne (stopy)
   i podloga (staly blok), czyli P = podloga + 2;
 - zyla idzie wzdluz sciany do 6 blokow od P, kazdy blok musi byc skala z wolnym polem przed soba -> ciag >= MIN;
 - strop: staly blok skaly 1..4 nad polem przed sciana (bez stropu nie ma zawalu ani obrywki — wtedy oznaczone).
Wynik: najlepsze ciagi scian (srodek ciagu = P), co najmniej 8 blokow od siebie (przodekPrzy lapie +-3).
Uzycie: sciany_lochow.py [katalog_map=/opt/docker/mc/data/world1/plugins/MythicDungeons/maps] [min_ciag=5] [ile_na_mape=6]
"""
import os, sys, glob, re
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../kuznia/tools'))
import mca

KAT = sys.argv[1] if len(sys.argv) > 1 else '/opt/docker/mc/data/world1/plugins/MythicDungeons/maps'
MIN = int(sys.argv[2]) if len(sys.argv) > 2 else 5
ILE = int(sys.argv[3]) if len(sys.argv) > 3 else 6
# Gornik.skala/twarda (Tag.BASE_STONE_OVERWORLD + bruk, piaskowiec, kalcyt, naciek + deepslate/bazalt/czernit)
SKALA = re.compile(r'^(stone|granite|diorite|andesite|tuff|deepslate|cobbled_deepslate|polished_deepslate|deepslate_\w+|cobblestone|'
                   r'mossy_cobblestone|sandstone|calcite|dripstone_block|basalt|smooth_basalt|polished_basalt|blackstone|\w*blackstone\w*)$')
TWARDA = re.compile(r'deepslate|basalt|blackstone|^granite$')
PRZEP = re.compile(r'(air|grass$|short_grass|tall_grass|fern|flower|dandelion|poppy|tulip|orchid|allium|bluet|daisy|'
                   r'cornflower|lily_of|carpet|vine|sapling|mushroom$|torch|button|rail|sign|leaf_litter|bush$|dead_bush|snow$|'
                   r'cobweb|lantern|chain|candle|pressure_plate|lever|ladder|glow_lichen|moss_carpet|hanging_roots|pointed_dripstone)')
CIECZ = {'minecraft:water', 'minecraft:lava'}
KIER = [(0, -1, 'N'), (0, 1, 'S'), (1, 0, 'E'), (-1, 0, 'W')]

def przep(n): return n is None or (n not in CIECZ and bool(PRZEP.search(n.split(':')[-1])))
def skala(n): return n is not None and bool(SKALA.match(n.split(':')[-1]))

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

def sciana(b, x, y, z, dx, dz):
    """Blok (x,y,z) to zywa sciana zwrocona w (dx,dz): skala, wolne pole przed nia."""
    return skala(b.get((x, y, z))) and przep(b.get((x + dx, y, z + dz)))

def stanowisko(b, x, y, z, dx, dz):
    """P na wysokosci piersi: przed nim wolne pole i wolne stopy, pod stopami podloga."""
    fx, fz = x + dx, z + dz
    return sciana(b, x, y, z, dx, dz) and przep(b.get((fx, y - 1, fz))) and not przep(b.get((fx, y - 2, fz)))

def strop(b, x, y, z, dx, dz):
    fx, fz = x + dx, z + dz
    for h in range(1, 5):
        n = b.get((fx, y + h, fz))
        if not przep(n): return h if skala(n) else -h
    return 0

for d in sorted(glob.glob(os.path.join(KAT, '*/'))):
    nazwa = os.path.basename(d.rstrip('/'))
    if not glob.glob(os.path.join(d, 'region', 'r.*.mca')): print(f'{nazwa}: brak regionow'); continue
    b = bloki(d)
    if not b: print(f'{nazwa}: regiony bez blokow (teren z generatora, nie do policzenia z pliku)'); continue
    kand = []
    for (x, y, z), n in b.items():
        if not skala(n): continue
        for dx, dz, k in KIER:
            if not stanowisko(b, x, y, z, dx, dz): continue
            wx, wz = -dz, dx   # wzdluz sciany (jak Gornik.wzdluz)
            ciag = 1
            for s in (1, -1):
                for i in range(1, 6):
                    if not sciana(b, x + wx * s * i, y, z + wz * s * i, dx, dz): break
                    ciag += 1
            if ciag < MIN: continue
            h = strop(b, x, y, z, dx, dz)
            kand.append((ciag, h > 0, x, y, z, k, h, bool(TWARDA.search(n)), n.split(':')[-1]))
    kand.sort(key=lambda c: (-c[0], not c[1]))
    wybrane = []
    for c in kand:
        if all(max(abs(c[2] - o[2]), abs(c[4] - o[4])) >= 8 or abs(c[3] - o[3]) > 3 for o in wybrane): wybrane.append(c)
        if len(wybrane) >= ILE: break
    print(f'{nazwa}: blokow {len(b)}, scian-kandydatow {len(kand)} (ciag >= {MIN}), wybrane {len(wybrane)}')
    for ciag, maStrop, x, y, z, k, h, tw, mat in wybrane:
        st = f'strop +{h}' if h > 0 else ('strop nie-skala' if h < 0 else 'BEZ STROPU (bez zawalu)')
        print(f'  {x},{y},{z} twarz {k} ciag {ciag} {mat}{" (twarda: klin)" if tw else ""} {st}  -> rzemioslo przodek {nazwa}* {x} {y} {z} trwale')
