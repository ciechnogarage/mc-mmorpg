"""Dziurawiec v3: blok w swiecie + karty pedu do zblizenia. 16x16, swiatlo z gory-lewej.
Zielen 5 tonow jak w roslinach vanilli (1 cien gleboki .. 5 swiatlo), lewa strona lisci jasna, prawa w cieniu.
Kwiat: 5 platkow (3x3), pomaranczowy srodek, czarne kropki gruczolow na brzegu platka (cecha dziurawca).
Rysunki = siatki znakow; kwiaty stemplowane recznie narysowanym motywem w recznie wybranych miejscach."""
import os, sys
from PIL import Image

PAL = {
    '1': '204626', '2': '2b702a', 'h': '357a24', '3': '3f7a2c', '4': '55ab2d', '5': '7fc059', '6': 'a6d86e',
    'Y': 'fff27a', 'y': 'ffe04a', 'o': 'f5ba27', 'O': 'd98a1a', 'r': 'a8641c', 'k': '2a2a12',
    'n': '4a2e18', 'b': '6e4a2a', 'B': '8e6236',
    'v': '8a9a3a', 'V': '6e7a2a', 'w': 'b0b450',
    'p': '9a5a2a', 'P': 'c08040', 'q': '6e4020',
    'm': 'c9c860', 'M': 'e0dc78', 'u': '9aa040',
}

# motywy kwiatostanu 3x3 (kwitnacy z kropka / bez, pak, przekwit); '.' = przezroczyste
KWIAT = [".Y.", "YoY", ".y."]      # krzyzyk platkow (3 px nie pomiesci 5 platkow czytelnie), pomarancz tylko w srodku
KWIAT_K = KWIAT
PAK = [".M.", "Mmu", ".u."]
PRZEK = [".P.", "pqp", ".q."]

T = {}
T['gora'] = [  # gorny ped: mlode, jasne, siedzace liscie w parach
    "......545.......",
    ".....55453......",
    "......343.......",
    "..55...4.....32.",
    ".554433433432...",
    "..4433.4.3322...",
    "....33.4.32.....",
    ".......4........",
    "...55..4..33....",
    "..55433433322...",
    "...4433.3h32....",
    ".....33.32......",
    ".......3........",
    "...5...3....32..",
    "..5433333333h2..",
    "...433.3.3322...",
]
T['srodek'] = [  # dojrzaly srodek pedu: ciemniejsze, wieksze liscie
    ".......3........",
    "...54..3...2....",
    "..544..3..221...",
    "...33.33.2211...",
    "....2.3h.21.....",
    ".......3........",
    ".......3........",
    "..54...3....21..",
    ".5443.33..2211..",
    "..332.3h.2211...",
    "...2..3.21......",
    ".......3........",
    ".......h........",
    ".......3........",
    ".......3........",
    ".......h........",
]
T['dol'] = [  # zdrewnialy dol: brazowa lodyga, stare zolknace liscie
    ".......V........",
    "..ww...V...VV...",
    ".wv33..V..2vVV..",
    "..vv22.Vh.21V...",
    "....22.Vh.1.....",
    ".......Vh.......",
    "......Vh........",
    "......Vh........",
    "......vV........",
    "......VV........",
    ".......VV.......",
    ".......VV.......",
    ".......Vb.......",
    ".......bB.......",
    ".......bB.......",
    ".......b........",
]
SZYPULKI = [  # wspolny szkielet szczytu: szypulki zbiegajace sie do lodygi (x=7)
    "................",
    "................",
    "................",
    "...4.......3....",
    "....4..4..3.....",
    ".4...4.4.3...3..",
    "..4..4.4.3..3...",
    "...4..44.3.3....",
    "....4.444.3.....",
    ".....4443.......",
    "......443.......",
    ".......4........",
    ".....5.4.3......",
    "....5543332.....",
    ".....4433.......",
    ".......3........",
]
MIEJSCA = [(1, 1), (6, 0), (11, 1), (0, 4), (12, 4), (5, 3), (9, 3)]   # lewy-gorny rog motywu 3x3

T['swiat'] = [  # yepa w swiecie: 4 pedy, ywiatostany na gorze, ten sam rysuney lisci w miniaturze
    "..Yy.....Yy.....",
    "..yOy.Yy.yOy.Yy.",
    "...yo.yOy.yo.yOy",
    ".Yy.4..yo.3...yo",
    ".yOy.4..4.3..3..",
    "..yo..4.4.3.3...",
    "...54..44.33.32.",
    "..5543.4.432322.",
    "...4433..4.3221.",
    ".55..3.4.43.....",
    "55433..4..3.32..",
    ".4433.343.33221.",
    "....3..4.3..2...",
    ".....3.4.3......",
    "......2.3.......",
    ".....2..3.2.....",
]
T['swiat_scieta'] = [  # po scieciu: kikuty z dolnymi liscmi
    "................", "................", "................", "................", "................",
    "................",
    "......5..4......",
    "...5..3..3..3...",
    "..553.3..3.332..",
    "...43.53.3.32...",
    "....v.33.32.V...",
    "...wvv.3.3.VVv..",
    "....v..3.3..V...",
    "......b..b.b....",
    "......b.b..b....",
    ".....n..n.n.....",
]

def siatka(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    return [list(r) for r in rows]

def stempel(g, motyw, x, y):
    for dy, r in enumerate(motyw):
        for dx, ch in enumerate(r):
            if ch != '.' and 0 <= x + dx < 16 and 0 <= y + dy < 16: g[y + dy][x + dx] = ch

def szczyt(motywy):
    g = siatka(SZYPULKI)
    for (x, y), m in zip(MIEJSCA, motywy): stempel(g, m, x, y)
    return [''.join(r) for r in g]

T['kwiat'] = szczyt([KWIAT, KWIAT_K, KWIAT, KWIAT, KWIAT, KWIAT_K, KWIAT])
T['pak'] = szczyt([PAK] * 7)
T['przekwit'] = szczyt([PRZEK, PRZEK, PAK, PRZEK, PRZEK, PRZEK, PRZEK])

def obraz(rows):
    g = siatka(rows)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y, r in enumerate(g):
        for x, ch in enumerate(r):
            if ch != '.': px[x, y] = tuple(int(PAL[ch][i:i+2], 16) for i in (0, 2, 4)) + (255,)
    return im

def chory(im):
    """Plamy choroby 2x2: rdzawy srodek, jasniejsza obwodka — tylko na lisciach (nie lodydze x=7)."""
    im = im.copy(); px = im.load()
    for (x, y) in [(2, 4), (10, 9), (2, 14), (11, 2)]:
        for dx in (0, 1):
            for dy in (0, 1):
                xx, yy = x + dx, y + dy
                if px[xx, yy][3] and xx != 7: px[xx, yy] = (0x8a, 0x4a, 0x22, 255) if (dx + dy) % 2 == 0 else (0xa8, 0x70, 0x30, 255)
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    ims = {k: obraz(v) for k, v in T.items()}
    ims['srodek_chory'] = chory(ims['srodek']); ims['gora_chory'] = chory(ims['gora'])
    for k, im in ims.items(): im.save(f'{out}/dziurawiec_{k}.png')
    print(' '.join(sorted(ims)))
