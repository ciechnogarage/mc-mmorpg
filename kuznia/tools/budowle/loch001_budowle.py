"""Budowle Lochu 001 (spec: docs/lochy/loch-001-kwietna-polana.md, sekcja 2). Uruchamiac w kuzni na upieczonym swiecie.
Uzycie: python3 loch001_budowle.py <swiat> [wersja]
Kazda budowla dostaje nazwe z sufiksem wersji (serwer pamieta brak szablonu do restartu -> nowe id przy kazdym przebiegu).
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS, LEVELS  # noqa: E402
import json, math, sys
sys.path.insert(0, f'{TOOLS}/budowle')
from budowla import Budowla
import wklej

SWIAT = sys.argv[1]
V = sys.argv[2] if len(sys.argv) > 2 else 'v1'
T = wklej.Teren(SWIAT)
MECH = {'cobblestone': 'mossy_cobblestone', 'stone_bricks': 'mossy_stone_bricks'}
wyniki = []


def dodaj(nazwa, b, x, z, **kw):
    info = T.osadz(b, 'loch001', f'{nazwa}_{V}', x, z, **kw)
    wyniki.append((nazwa, info['cel'], info['blokow'], info['odpowiedz']))


# 1. OBOZ PRZYBYCIA (0,118) - nasz, bezpieczny. Rysowany recznie (gotowe 'camp*' z biblioteki kopaly doly w terenie).
#    Srodek: krag ogniska (punkt kontrolny MD; model ME bonfire_lit stanie w miejscu 'F'), lawy z pni, namiot zwiadowczyni
#    na zachodzie, zaopatrzenie, latarnie na slupkach. Plac na wschod od ogniska wolny: tam manekin (model ME).
#    legenda: F ognisko, c bruk, L/l pnie-lawy (os x / os z), T/t namiot, C skrzynia, B beczka, P slupek, * latarnia
OBOZ = [
 # y=0 (grunt)
 ['...........',
  '..TTTTT....',
  '..T...T....',
  '..T...T..P.',
  '..T...T....',
  '..TTTTT....',
  '....LLL....',
  '..l.ccc.l..',
  '..l.cFc.l..',
  '..l.ccc.l..',
  '....LLL..CB',
 ],
 # y=1
 ['...........',
  '..wwwww....',
  '..w...w....',
  '..w...w..P.',
  '..w...w....',
  '..ww.ww....',
  '...........',
  '...........',
  '...........',
  '...........',
  '.........BB',
 ],
 # y=2
 ['...........',
  '...www.....',
  '...w.w.....',
  '...w.w...*.',
  '...w.w.....',
  '...w.w.....',
  '...........',
  '...........',
  '...........',
  '...........',
  '..........*',
 ],
 # y=3 kalenica
 ['...........',
  '....w......',
  '....w......',
  '....w......',
  '....w......',
  '....w......',
  '...........', '...........', '...........', '...........', '...........',
 ],
]
ob = Budowla().warstwy([OBOZ[0][6:], ], {
    'F': 'campfire[lit=true]', 'c': 'cobblestone', 'L': 'stripped_spruce_log[axis=x]', 'l': 'stripped_spruce_log[axis=z]',
    'C': 'chest[facing=west]', 'B': 'barrel[facing=up]', 'P': 'spruce_fence', '*': 'lantern', 'T': 'air'}, poczatek=(0, 0, 6))
ob.warstwy([w[6:] for w in (OBOZ[1], OBOZ[2])], {'B': 'barrel[facing=up]', '*': 'lantern'}, poczatek=(0, 1, 6))
# namiot 'A' 7 szer. x 9 dl. x 4 wys., plotno bialo-brazowe w pasy, rama ze swierka, wejscie od pld (strona ogniska)
for z in range(9):
    for h in range(4):
        for sx in (-1, 1):
            x = 3 + sx * (3 - h)
            if z == 8 and h < 3 and sx == 1 and False:
                continue
            blok = 'stripped_spruce_log[axis=z]' if h == 3 else ('brown_wool' if z % 3 == 1 else 'white_wool')
            if z in (0, 8):
                blok = 'spruce_log' if h == 3 or (x in (0, 6)) else blok
            ob.postaw((x, h, z - 4), blok)
    if z == 0:     # tyl zamkniety
        for h in range(3):
            for x in range(3 - (2 - h), 4 + (2 - h)):
                ob.postaw((x, h, -4), 'white_wool', nadpisz=False)
ob.postaw((3, 0, 3), 'red_bed[facing=north,part=foot]'); ob.postaw((3, 0, 2), 'red_bed[facing=north,part=head]')
dodaj('oboz_przybycia', ob, 0, 114, zaglebienie=0, fundament='minecraft:dirt')

# 3. OBOZ PLEMIENIA CIERNIA = Zakret Lucznikow (46,22) r16. Palisada lukiem po stronie polnocnej (skad strzelaja),
#    brama palisady tam, gdzie trasa wychodzi na pln-zach (kat ~225). Wieza straznicza z lucznikiem. Gracz wchodzi od pld;
#    wybor: frontem pod ostrzalem albo flanka przez las (wsch) za glazami.
pal = Budowla(); cx, cz, R = 46, 22, 18
for st in range(185, 356, 3):
    if 212 <= st <= 238:
        continue                      # wyrwa na trase
    a = math.radians(st); x = round(cx + R * math.cos(a)); z = round(cz + R * math.sin(a))
    h = 4 + (st // 3) % 2
    for y in range(h):
        pal.postaw((x, y, z), 'spruce_log')
    pal.postaw((x, h, z), 'spruce_fence')
T.odswiez()
info = T.osadz(pal, 'loch001', f'palisada_{V}', min(p[0] for p in pal.bloki) + cx - cx, min(p[2] for p in pal.bloki), srodek=False, przytnij=True, zaglebienie=0)
wyniki.append(('palisada', info['cel'], info['blokow'], info['odpowiedz']))
dodaj('wieza', Budowla().obiekt('structures/tower.iob'), 56, 8, zaglebienie=0, fundament='minecraft:spruce_planks')
dodaj('ognisko_wroga', Budowla().obiekt('clutter/camp1.iob'), 46, 14, zaglebienie=0)
for i, (x, z, ob) in enumerate([(40, 9, 'boulder13'), (64, 24, 'boulder4'), (66, 36, 'boulder6')]):
    dodaj(f'glaz_{i}', Budowla().obiekt(f'clutter/{ob}.iob', obrot=90 * i, podmiany=MECH), x, z)
for i, (x, z, ob) in enumerate([(50, 30, 'oaklog2'), (39, 26, 'oaklog3')]):
    dodaj(f'pien_{i}', Budowla().obiekt(f'clutter/{ob}.iob', obrot=90 * i), x, z, zaglebienie=0)

# 4. Polana Rytualu (6,-14) r13: krag 6 obeliskow r=10, przejscie sciezki wolne (obeliski na katach poza osia trasy)
for i in range(6):
    a = math.radians(30 + 60 * i)
    dodaj(f'obelisk_{i}', Budowla().obiekt(f'clutter/obelisk{1 + i % 7}.iob', obrot=90 * (i % 4), podmiany=MECH),
          round(6 + 10 * math.cos(a)), round(-14 + 10 * math.sin(a)), zaglebienie=1, fundament='minecraft:stone')

# 6. Zlamana Brama (0,-48): kolumnada ruin3 na osi + mur z ubytkami do urwisk po obu stronach, wyrwa ~6 bl. na sciezke
dodaj('brama_ruina', Budowla().obiekt('structures/ruin3.iob', podmiany=MECH), 0, -48, zaglebienie=2, fundament='minecraft:stone_bricks')
MUR = {'stone_bricks': 5, 'mossy_stone_bricks': 4, 'cracked_stone_bricks': 2, 'mossy_cobblestone': 1}
# mur przez cala szyje wyspy (x -60..60 przyciete do ladu) z wyrwa x -3..3 na sciezce; przechodzi przez ruine
dodaj('brama_mur_w', Budowla().mur((0, 0, 0), (56, 0, 0), wys=6, paleta=MUR, grub=2, ubytek=0.35, ziarno=11), -60, -48,
      fundament='minecraft:stone_bricks', srodek=False, przytnij=True)
dodaj('brama_mur_e', Budowla().mur((0, 0, 0), (56, 0, 0), wys=6, paleta=MUR, grub=2, ubytek=0.35, ziarno=12), 4, -48,
      fundament='minecraft:stone_bricks', srodek=False, przytnij=True)

# 7. OGNISKO ODPOCZYNKU przed arena - cichy punkt kontrolny
dodaj('ognisko_odpoczynku', Budowla().obiekt('clutter/camp5.iob'), -12, -86, zaglebienie=0)

# 8. SERCE GAJU - wielka sakura nad Wyjsciem z Nagroda (skrzynia stoi pod korona). genericsak1 46x51x50 z packa ma
#    zielone liscie debu i brzozowy pien -> przemalowane na wisnie (skrecony oakspindle wygladal jak ciemny znak zapytania).
WISNIA = {'oak_leaves': 'cherry_leaves[persistent=true]', 'stripped_birch_wood': 'cherry_wood', 'birch_wood': 'cherry_wood',
          'birch_log': 'cherry_wood', 'oak_log': 'cherry_wood', 'oak_wood': 'cherry_wood'}
dodaj('serce_gaju', Budowla().obiekt('trees/sakura/genericsak1.iob', podmiany=WISNIA), 0, -163, zaglebienie=1,
      fundament='minecraft:cherry_wood')

# KWIATY GAJU x3 - ukryte kapliczki (opcjonalne wyzwanie; interakcja = MM/MD pozniej). Omszaly cokol + kwitnaca azalia.
KAPL = [['mmm', 'mmm', 'mmm'], ['.c.', 'cMc', '.c.'], ['...', '.a.', '...']]
for n, (x, z) in {'kwiat_A': (62, 70), 'kwiat_B': (-80, 14), 'kwiat_C': (-36, -100)}.items():
    dodaj(n, Budowla().warstwy(KAPL, {'m': 'mossy_stone_bricks', 'c': 'moss_carpet', 'M': 'moss_block',
                                     'a': 'flowering_azalea'}), x, z, zaglebienie=1)

for n, cel, bl, odp in wyniki:
    print(f'{n:14s} {str(cel):18s} {bl:6d} blokow | {odp}')
json.dump([dict(nazwa=n, cel=c, blokow=b) for n, c, b, _ in wyniki], open(f'{LEVELS}/loch001_budowle_{V}.json', 'w'), indent=1)
