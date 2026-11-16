#!/usr/bin/env python3
"""IKONY HANDLU — ta sama technika i palety co tools/menu-hub-gui/generate_icons2.py
(naglowek skopiowany 1:1, zeby nie odpalac generatora huba jako efektu ubocznego importu).

ORYGINALNY OPIS PALET:

DLACZEGO OD NOWA: dwa razy probowalem generowac glify proceduralnie (cieniowanie
po sasiadach, elipsa liczona wzorem) i dwa razy wyszlo zle — ukosne ramie dostalo
paski wzdluz przekatnej, a moneta bawole oko bez kierunku swiatla. Procedura
odbiera decyzje o pojedynczym pikselu, a to wlasnie ona robi roznice. Generowana
zostaje TYLKO plakietka (ramka = geometria). Glify sa rysowane recznie.

SWIATLO: z lewej-gory, jednakowo we wszystkich ikonach. W kazdym ksztalcie
krawedz lewa/gorna dostaje jasny walor, prawa/dolna ciemny, srodek posredni.

GESTOSC: siatka sztuki 16x16, zapis 32x32 (1 px sztuki = 2x2 tekseli), zeby
ikona miala dokladnie te sama ziarnistosc co panel (1 teksel = 1 px GUI).

PALETY (indeks 0 = najjasniejszy):
  ABCDEFG mosiadz | hijklm skora zielona | 12345 zelazo | 67890 zloto
  nopqr drewno    | stuv cialo           | wyz! czerwien | +-*/ zielen magii
"""
import sys, os
sys.path.insert(0, '/pxlib')
from PIL import Image
from warsztat import rampa

A = 16
MOS   = rampa(50, 0.35, 0.98, 24, 0.95, 0.22, 7)
SKORA = rampa(126, 0.50, 0.48, 148, 0.78, 0.14, 6)
FE    = rampa(214, 0.08, 0.88, 224, 0.28, 0.22, 5)
ZLO   = rampa(52, 0.42, 1.00, 32, 0.95, 0.28, 5)
DRE   = rampa(30, 0.45, 0.72, 20, 0.82, 0.22, 5)
CIAL  = rampa(28, 0.35, 0.92, 16, 0.62, 0.40, 4)
CZER  = rampa(14, 0.60, 0.88, 2, 0.92, 0.32, 4)   # NIE 8->356: to zawija przez cyjan
MAG   = rampa(160, 0.45, 1.00, 190, 0.85, 0.40, 4)
PER   = rampa(44, 0.16, 0.98, 32, 0.48, 0.46, 4)   # pergamin
CIEN  = (0x0b, 0x0f, 0x0a, 255)

PAL = {'.': None, 'x': CIEN}
for i, c in enumerate(MOS):   PAL['ABCDEFG'[i]] = c
for i, c in enumerate(SKORA): PAL['hijklm'[i]] = c
for i, c in enumerate(FE):    PAL['12345'[i]] = c
for i, c in enumerate(ZLO):   PAL['67890'[i]] = c
for i, c in enumerate(DRE):   PAL['nopqr'[i]] = c
for i, c in enumerate(CIAL):  PAL['stuv'[i]] = c
for i, c in enumerate(CZER):  PAL['wyz!'[i]] = c
for i, c in enumerate(MAG):   PAL['+-*/'[i]] = c
for i, c in enumerate(PER):   PAL['<>[]'[i]] = c


def kafelek():
    """Plakietka: mosiezna rama, zielona skora. Generowana — to ramka."""
    im = Image.new('RGBA', (A, A), (0, 0, 0, 0))
    p = im.load()
    for y in range(A):
        for x in range(A):
            if (x, y) in ((0, 0), (A - 1, 0), (0, A - 1), (A - 1, A - 1)):
                continue
            if x in (0, A - 1) or y in (0, A - 1):
                p[x, y] = MOS[1] if (y == 0 or x == 0) else MOS[5]
            elif x in (1, A - 2) or y in (1, A - 2):
                p[x, y] = MOS[3] if (y == 1 or x == 1) else MOS[6]
            elif x == 2 or y == 2:
                p[x, y] = CIEN
            else:
                p[x, y] = SKORA[3] if y < 6 else (SKORA[2] if y < 12 else SKORA[1])
    return im


G = {}
G['aukcje'] = [                      # mlotek licytatora: drewniana glowica ze zlotymi okuciami + podstawka
    '............', '.6nnnnnnnn8.', '.6oooooopp8.', '.7ppppppqq9.',
    '.6nnnnnnnn8.', '.....npo....', '.....npo....', '.....npo....',
    '.....nqo....', '..11111111..', '.1222222221.', '.1333333331.',
]
G['skup'] = [                        # waga szalkowa z mosiadzu, zlote szalki
    '.....AA.....', '.ABBBBBBBBC.', '.B...BC...D.', 'B.D..BC..B.D',
    'B..D.BC.B..D', '6778.BC.6778', '.889.BC.889.', '.....BC.....',
    '.....BC.....', '....ABCD....', '...ABBCDD...', '............',
]
G['sklep'] = [                       # sakwa kupca ze znakiem monety
    '............', '...n....q...', '....noqp....', '....6778....',
    '...nooopq...', '..noooooppq.', '.nooo677ooq.', '.nooo788opq.',
    '.noooo99ppq.', '..nooooppq..', '...qqqqqq...', '............',
]
G['wystaw'] = [                      # metka z cena na sznurku
    '..........n.', '.........n..', '....<<<<n...', '...<>>>>[...',
    '..<>>xx>>[..', '.<>>>xx>>>[.', '.<>>>>>>>>[.', '.<>>6778>>[.',
    '.<>>>889>>[.', '.<>>>>>>>>[.', '.<[[[[[[[[[.', '............',
]
G['strona_dalej'] = [                # strzalka strony w prawo (zelazo — nawigacja, nie wyjscie)
    '............', '....1.......', '....21......', '....221.....',
    '....2221....', '....22221...', '....33332...', '....3332....',
    '....332.....', '....32......', '....3.......', '............',
]
G['strona_wstecz'] = [r[::-1] for r in G['strona_dalej']]

for _n, _g in G.items():
    assert len(_g) == 12, f'{_n}: {len(_g)} wierszy'
    for _i, _r in enumerate(_g):
        assert len(_r) == 12, f'{_n} wiersz {_i}: {len(_r)} znakow -> {_r!r}'
        for _c in _r:
            assert _c in PAL, f'{_n} wiersz {_i}: nieznany znak {_c!r}'

# ---------------------------------------------------------------- KLAWIATURA CENY
# Cyfry 5x7 (klasyczny font bitmapowy), rysowane mosiadzem z tym samym swiatlem co reszta:
# piksel z pusta gora/lewa = jasny (A), z pusta dolem/prawa = ciemny (D), srodek = B.
FONT = {
 '0': ['.###.','#...#','#..##','#.#.#','##..#','#...#','.###.'],
 '1': ['..#..','.##..','..#..','..#..','..#..','..#..','.###.'],
 '2': ['.###.','#...#','....#','...#.','..#..','.#...','#####'],
 '3': ['####.','....#','....#','.###.','....#','....#','####.'],
 '4': ['...#.','..##.','.#.#.','#..#.','#####','...#.','...#.'],
 '5': ['#####','#....','####.','....#','....#','#...#','.###.'],
 '6': ['..##.','.#...','#....','####.','#...#','#...#','.###.'],
 '7': ['#####','....#','...#.','..#..','.#...','.#...','.#...'],
 '8': ['.###.','#...#','#...#','.###.','#...#','#...#','.###.'],
 '9': ['.###.','#...#','#...#','.####','....#','...#.','.##..'],
 'kropka': ['.....','.....','.....','.....','.....','.##..','.##..'],
}
def z_fontu(wzor, x0=3, y0=2):
    g = [['.'] * 12 for _ in range(12)]
    h, w = len(wzor), len(wzor[0])
    on = lambda x, y: 0 <= y < h and 0 <= x < w and wzor[y][x] == '#'
    for y in range(h):
        for x in range(w):
            if not on(x, y):
                continue
            c = 'B'
            if not on(x, y - 1) or not on(x - 1, y): c = 'A'
            if not on(x, y + 1) or not on(x + 1, y): c = 'D' if c == 'B' else 'B'
            g[y0 + y][x0 + x] = c
    return [''.join(r) for r in g]
for k in list(FONT):
    G['cyfra_' + k if k != 'kropka' else 'kropka'] = z_fontu(FONT[k], 4 if k == 'kropka' else 3, 2 if k != 'kropka' else 2)
G['cofnij'] = [                      # klawisz cofania: strzalka w lewo z kreska
    '............', '............', '...A........', '..AB........',
    '.ABBBBBBBBC.', 'ABBBBBBBBBC.', '.ACDDDDDDDD.', '..AD........',
    '...D........', '............', '............', '............',
]
G['dni'] = [                         # klepsydra: rama drewniana, piasek zloty
    '.nnnnnnnnnn.', '..p<>>>>[p..', '..p.6777.p..', '...p.78.p...',
    '....p88p....', '.....p9.....', '.....p......', '....p.6p....',
    '...p.677.p..', '..p.67788p..', '..p<>>>>[p..', '.nnnnnnnnnn.',
]
G['zatwierdz'] = [                   # ptaszek: zielen magii (neutralny, pierwsze klikniecie)
    '............', '..........+.', '.........+-.', '........+-*.',
    '.+.....+-*..', '.-+...+-*...', '.*-+.+-*....', '..*-+-*.....',
    '...*--*.....', '....**......', '............', '............',
]
G['zatwierdz_gotowe'] = [r.replace('+', '6').replace('-', '7').replace('*', '8') for r in G['zatwierdz']]

for _n, _g in G.items():
    assert len(_g) == 12 and all(len(r) == 12 for r in _g), _n
    for _r in _g:
        for _c in _r:
            assert _c in PAL, (_n, _c)

def kafelek_wybrany():
    """Wariant 'aktywny' (wybrany dzien, uzbrojone potwierdzenie): jasniejsza skora i zlota rama."""
    im = kafelek(); p = im.load()
    for y in range(A):
        for x in range(A):
            c = p[x, y]
            if c[3] == 0: continue
            if c in (SKORA[3], SKORA[2], SKORA[1]):
                p[x, y] = SKORA[max(0, SKORA.index(c) - 2)]
            elif c in MOS:
                p[x, y] = ZLO[min(len(ZLO) - 1, MOS.index(c) // 2)]
    return im
WYBRANE = {'dni_wybrane': 'dni', 'zatwierdz_gotowe': 'zatwierdz_gotowe'}


def ikona(nazwa, baza=None):
    im = (baza or kafelek)()
    p = im.load()
    for y, row in enumerate(G[nazwa]):
        for x, ch in enumerate(row):
            c = PAL.get(ch)
            if c:
                p[2 + x, 2 + y] = c
    return im


OUT = '/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/hub'
IKONY = {n: ikona(n) for n in G}
IKONY['dni_wybrane'] = ikona('dni', kafelek_wybrany)
IKONY['zatwierdz_gotowe'] = ikona('zatwierdz_gotowe', kafelek_wybrany)
for n, im in IKONY.items():
    im.resize((32, 32), Image.NEAREST).save(f'{OUT}/handel_{n}.png')
kol = 8; rz = (len(IKONY) + kol - 1) // kol
sheet = Image.new('RGBA', (kol * 104 + 8, rz * 104 + 8), (0x18, 0x33, 0x24, 255))
for i, (n, im) in enumerate(IKONY.items()):
    r, c = divmod(i, kol)
    sheet.alpha_composite(im.resize((96, 96), Image.NEAREST), (8 + c * 104, 8 + r * 104))
sheet.save('/home/dev/Desktop/Przemek/MC/tools/handel/gui/ikony-handel.png')
print('ikon:', len(IKONY), sorted(IKONY))
