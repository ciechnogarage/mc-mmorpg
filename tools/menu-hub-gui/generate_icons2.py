#!/usr/bin/env python3
"""IKONY huba — 21 sztuk, cieplna paleta panelu.

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

G['postac'] = [                      # popiersie w kapturze — twarz w cieniu
    '............', '....nnnn....', '...noooon...', '..nostttpn..',
    '..nostuupn..', '..nostuupn..', '...ottuupo..', '....tuup....',
    '...1222221..', '..122333221.', '.12233333221', '.12222222221',
]
G['ekwipunek'] = [                   # napiersnik
    '............', '..11....11..', '.1221..1221.', '.12233333221',
    '.12222222221', '.12233333221', '.12222222221', '.1223333221.',
    '..122222221.', '..12233221..', '...1222 1...'.replace(' ', '2'), '....1221....',
]
G['umiejetnosci'] = [
    '............',
    '...nn..nn...',
    '..n<<nn<<n..',
    '.n<<<<n<<<<n',
    '.n<<<<n<<<<n',
    '.n<>>>n<>>>n',
    '.n<>>>n<>>>n',
    '.n>>>>n>>>>n',
    '..nnnnnnnnn.',
    '...ooooooo..',
    '............',
    '............',
]
G['przygoda'] = [                    # kompas
    '............', '....CCCC....', '..CCDDDDCC..', '.CDDxxxxDDC.',
    '.CDxx wwxxDC'.replace(' ', 'x'), 'CDxxxwwxxxDC', 'CDxxx11xxxDC', '.CDxx11xxxDC',
    '.CDDxxxxDDC.', '..CCDDDDCC..', '....CCCC....', '............',
]
G['ekonomia'] = [
    '............',
    '....6666....',
    '..66677789..',
    '.6667777899.',
    '.6677887799.',
    '.6777887899.',
    '.6777788899.',
    '..77788899..',
    '....8899....',
    '............',
    '..66777889..',
    '..88999999..',
]
G['spolecznosc'] = [                 # dwie postacie, jedna z przodu
    '............', '..ss....ss..', '.stts..stts.', '.stts..stts.',
    '..ss....ss..', '.1221..1221.', '122332122332', '122332122332',
    '.12233122332', '.12222122222', '.1222.12222.', '............',
]
G['ustawienia'] = [                  # zebatka
    '............', '...1..11..1.', '..112112211.', '.11222222211',
    '..1222xx2221', '..122xxxx221', '..122xxxx221', '..1222xx2221',
    '.11222222211', '..112112211.', '...1..11..1.', '............',
]
G['pomoc'] = [
    '............',
    '...66666....',
    '..6777778...',
    '..67...889..',
    '......7889..',
    '.....77889..',
    '....7788....',
    '....7889....',
    '....789.....',
    '............',
    '....778.....',
    '....889.....',
]
G['wstecz'] = [                      # strzalka w lewo, mosiadz
    '............', '.....A......', '....AB......', '...ABB......',
    '..ABBBBBBBB.', '.ABBBBBBBBBC', '.ACDDDDDDDDD', '..ADDDDDDDD.',
    '...ADD......', '....AD......', '.....D......', '............',
]
G['zamknij'] = [                     # X, ramiona grubosci 3, swiatlo z lewej
    '............', '............', '.wyz....wyz.', '..wyz..wyz..',
    '...wyzwyz...', '....wyyz....', '....wyyz....', '...wyzwyz...',
    '..wyz..wyz..', '.wyz....wyz.', '............', '............',
]
G['statystyki'] = [                  # slupki
    '............', '.........66.', '.........77.', '......66.78.',
    '......77.78.', '...66.78.78.', '...77.78.78.', '.66.78.78.78',
    '.77.78.78.78', '.78.78.78.78', '.88.88.88.88', '............',
]
G['atrybuty'] = [
    '............',
    '...666666...',
    '..67777776..',
    '.6778888776.',
    '..77888877..',
    '...788887...',
    '....7887....',
    '.....78.....',
    '............',
    '............',
    '............',
    '............',
]
G['profesje'] = [                    # mlotek: glowica zelazna, trzonek drewniany
    '............', '..1111111...', '.12222221...', '.12233221...',
    '.13333331...', '..1113111...', '....npo.....', '....npo.....',
    '....npo.....', '....npo.....', '....nqo.....', '....ooo.....',
]
G['zadania'] = [
    '............',
    '..<<<<<<<<..',
    '.<>>>>>>>><.',
    '.<>[[[[[[><.',
    '.<>[[[[[[><.',
    '.<>[[[[[[><.',
    '.<>[[[[[[><.',
    '.<>>>>>>>><.',
    '..<<<<<<<<..',
    '.....ww.....',
    '....wy!y....',
    '.....!!.....',
]
G['pvp'] = [                         # dwa skrzyzowane miecze
    '............', '.1.......1..', '.21.....12..', '..21...12...',
    '...21.12....', '....2112....', '....2112....', '...C21.12C..',
    '..CDD...DDC.', '..CD.....DC.', '.CD.......DC', '............',
]
G['rzucanie'] = [                    # rozdzka z iskra
    '.........+..', '....+...+/..', '.....+.+*...', '......++....',
    '....++++++..', '.....+**....', '....C-*.....', '...Cn.......',
    '..Cn........', '.Cn.........', 'Co..........', '............',
]
G['gildia'] = [
    '..n.........',
    '..n66666666.',
    '..n6wwwwwwy.',
    '..n6wy!!yyy.',
    '..n6wy!!yyy.',
    '..n6wwyyyyy.',
    '..n6wwwyyy..',
    '..n6wwyy....',
    '..n6wy......',
    '..n.........',
    '..nn........',
    '.nnpp.......',
]
G['znajomi'] = [                     # dwie dlonie w uscisku
    '............', '............', '..sss..sss..', '.stttsstttt.',
    '.sttuuuuttt.', '..tuuuuuutt.', '..tuuuuuut..', '...tuuuut...',
    '....tuut....', '.....tt.....', '............', '............',
]
G['zmien_postac'] = [
    '............',
    '..A.........',
    '.AAA...D....',
    'AAAAA..D....',
    '..A....D....',
    '..A....D....',
    '..A....D....',
    '..A....D....',
    '..A....D....',
    '..A..DDDDD..',
    '..A...DDD...',
    '.......D....',
]
G['drzewka'] = [
    '............',
    '.....66.....',
    '....6776....',
    '.....77.....',
    '.....CC.....',
    '..CCCCCCCC..',
    '..C......C..',
    '..C......C..',
    '.666....666.',
    '.677....776.',
    '............',
    '............',
]
G['wbudowie'] = [                    # tabliczka "w budowie" — deska z gwozdziami
    '............', '..nnnnnnnn..', '.nCoooooooCn', '.noooooooon.',
    '.nooppppoon.', '.noppppppon.', '.nooppppoon.', '.noooooooon.',
    '.nCoooooooCn', '..nnnnnnnn..', '.....pp.....', '.....pp.....',
]
G['wiara'] = [                       # znicz ofiarny: mosiezna czara z plomieniem (neutralny dla wszystkich wiar)
    '.....w......',
    '.....wy.....',
    '....wyy.....',
    '....y66y....',
    '...wy677y...',
    '...y66777z..',
    '....y777z...',
    '.AABBBBBCDD.',
    '..BCCCCCDE..',
    '...CDDDDE...',
    '.....DE.....',
    '...BCDDEF...',
]
G['zmien_postac_alias'] = G['zmien_postac']
del G['zmien_postac_alias']

for _n, _g in G.items():
    assert len(_g) == 12, f'{_n}: {len(_g)} wierszy'
    for _i, _r in enumerate(_g):
        assert len(_r) == 12, f'{_n} wiersz {_i}: {len(_r)} znakow -> {_r!r}'
        for _c in _r:
            assert _c in PAL, f'{_n} wiersz {_i}: nieznany znak {_c!r}'


def ikona(nazwa):
    im = kafelek()
    p = im.load()
    for y, row in enumerate(G[nazwa]):
        for x, ch in enumerate(row):
            c = PAL.get(ch)
            if c:
                p[2 + x, 2 + y] = c
    return im


OUT = '/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/hub'
os.makedirs(OUT, exist_ok=True)
for n in G:
    ikona(n).resize((32, 32), Image.NEAREST).save(f'{OUT}/{n}.png')

S, gap, kol = 6, 8, 7
ns = sorted(G)
rz = (len(ns) + kol - 1) // kol
sheet = Image.new('RGBA', (kol * A * S + (kol + 1) * gap, rz * A * S + (rz + 1) * gap),
                  (0x18, 0x33, 0x24, 255))
for i, n in enumerate(ns):
    r, c = divmod(i, kol)
    sheet.alpha_composite(ikona(n).resize((A * S, A * S), Image.NEAREST),
                          (gap + c * (A * S + gap), gap + r * (A * S + gap)))
sheet.save('/opt/docker/mc/data/world1/output/warsztat/ikony/_wszystkie.png')
print(f'ikon: {len(G)} -> {OUT}')
