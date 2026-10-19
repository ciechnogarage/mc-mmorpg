"""Dziurawiec: blok w swiecie + karty pedu do zblizenia. Jeden znak = jeden piksel (16x16).
Swiatlo z gory-lewej. Paleta z roslin vanilli (mniszek, konwalia)."""
import os, sys
from PIL import Image

PAL = {
    'G': '177c04',  # ciemna zielen (cien liscia, lodyga)
    'g': '4a8f28',  # srednia
    'l': '55ab2d',  # jasna
    'j': '7fc059',  # mlody lisc / swiatlo
    'v': '8a9a3a',  # starszy, zolknacy lisc
    'Y': 'ffec4f', 'y': 'fed639', 'o': 'f5ba27', 'O': 'f19d25', 'r': 'bd6a22',
    'k': '2a2a12',  # czarne kropki dziurawca / plamy choroby
    'n': '5c3a20', 'b': '7e5330', 'B': 'a0703e',  # drewno
    'p': '9a5a2a', 'P': 'c08040', 'q': '6e4020',  # przekwitle (rdza)
    'm': 'a8b84a', 'M': 'c9c860',  # paki
}

T = {}
# para lisci w ksztalcie V przyczepiona do lodygi w kolumnie 7 (3 wiersze): klucz motywu
T['swiat'] = [  # kepa w swiecie: 3 pedy, pary lisci, plaskie baldachy (bez ziemi, jak kwiaty vanilli)
    "..Y...Y.........",
    ".YoY.YoY..Y.....",
    "..Y.l.Y..YoY....",
    "...l.l.l..Y.Y...",
    "....l.l.l.l.YoY.",
    ".....lll.l...Y..",
    ".jl...l.l...l...",
    "..jgl.l.ll.l....",
    "...Ggl.l.lgj....",
    ".....l..lgGj....",
    "..jl.l.jl.l.....",
    "...jgl.lgl.l....",
    "....Gglg.lgGj...",
    "......g..g.G....",
    "......b..b......",
    "......b.b.......",
]
T['dol'] = [  # zdrewnialy dol pedu: brazowa lodyga, stara para lisci, slad po opadlej
    ".......b........",
    ".....v.b.v......",
    "....vgbbbgv.....",
    ".....Gnbng......",
    ".......b........",
    ".......b........",
    ".......b........",
    ".......B........",
    "......nb........",
    ".....n.b........",
    ".......b........",
    ".......B........",
    ".......b........",
    ".......b........",
    "......nbB.......",
    ".....n.b.n......",
]
T['srodek'] = [  # dojrzala lodyga, dwie pary lisci (V), ciemniejsze
    ".......g........",
    ".......g........",
    ".....j.g.j......",
    "....jglglgj.....",
    ".....GgggG......",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
    "....l..g..l.....",
    "...jgl.g.lgj....",
    "....GgggggG.....",
    "......Ggg.......",
    ".......g........",
    ".......g........",
    ".......g........",
]
T['gora'] = [  # gorny ped: mlode jasne liscie, gesto, dwie pary + boczne pedy
    ".......l........",
    "......jlj.......",
    ".....jjljj......",
    "......glg.......",
    ".......l........",
    "....j..l..j.....",
    "...jlj.l.jlj....",
    "....glglglg.....",
    ".....GglgG......",
    ".......l........",
    ".......l........",
    ".....j.l.j......",
    "....jglglgj.....",
    ".....GgggG......",
    ".......g........",
    ".......g........",
]
T['kwiat'] = [  # baldach: 6 gwiazdek na szypulkach wyrastajacych z lodygi
    "...Y......Y.....",
    "..YoY.Y..YoY....",
    "...Yl.YoYkYl.Y..",
    ".Y..l..Yl..l.YoY",
    "YoY.l...l..l.lY.",
    ".Yl..l..l.l..l..",
    "...l.l..l.l.l...",
    "....ll..l.ll....",
    ".....l..l.l.....",
    "......l.l.l.....",
    ".......lll......",
    ".......l........",
    ".....j.l.j......",
    "....jglglgj.....",
    ".....GglgG......",
    ".......l........",
]
T['pak'] = [  # paki: zielonozolte kulki na szypulkach
    "................",
    "...M......M.....",
    "..mMm.M..mMm....",
    "...ml.mMm.lm.M..",
    "....l..ml..lmMm.",
    ".m..l...l..l.ml.",
    "mMm.l...l..l.l..",
    ".ml..l..l.l..l..",
    "...l.l..l.l.l...",
    "....ll..l.ll....",
    ".....l..l.l.....",
    "......l.l.l.....",
    ".......lll......",
    ".....j.l.j......",
    "....jglglgj.....",
    ".......l........",
]
T['przekwit'] = [  # przekwitle: rdzawe, oklapniete
    "................",
    "................",
    "...p......p.....",
    "..pqP.p..pqP....",
    "...qv.pqPkqv.p..",
    ".p..v..qv..v.pqP",
    "pqP.v...v..v.vq.",
    ".qv..v..v.v..v..",
    "...v.v..v.v.v...",
    "....vv..v.vv....",
    ".....v..v.v.....",
    "......v.v.v.....",
    ".......vvv......",
    ".....v.g.v......",
    "....vgvgvgv.....",
    ".......g........",
]

T['swiat_scieta'] = [  # po scieciu: krotkie kikuty z dolnymi liscmi
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......B..B......",
    "......g..g..B...",
    "...v.vgv.g..g...",
    "....vGg.vgv.g...",
    "......g..gGvgv..",
    "......g..g..g...",
    "......b..b..b...",
    "......b.b.......",
]

def obraz(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.': px[x, y] = tuple(int(PAL[ch][i:i+2], 16) for i in (0, 2, 4)) + (255,)
    return im

def chory(im):
    """Plamy choroby: kropki na pikselach lisci (nie na lodydze), staly wzor."""
    im = im.copy(); px = im.load()
    for (x, y) in [(5, 3), (9, 3), (4, 10), (10, 10), (6, 11), (9, 11), (5, 6), (9, 7)]:
        if px[x, y][3] and x != 7: px[x, y] = (0x2a, 0x2a, 0x12, 255)
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    ims = {k: obraz(v) for k, v in T.items()}
    ims['srodek_chory'] = chory(ims['srodek']); ims['gora_chory'] = chory(ims['gora'])
    for k, im in ims.items(): im.save(f'{out}/dziurawiec_{k}.png')
    print(' '.join(sorted(ims)))
