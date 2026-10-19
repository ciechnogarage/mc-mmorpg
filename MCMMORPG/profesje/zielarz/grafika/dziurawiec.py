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
T['srodek'] = [
    ".......g........",
    "....j..g..j.....",
    "...jgl.g.lgj....",
    "....GgggggG.....",
    "......Ggg.......",
    "..j....g....j...",
    ".jgl.j.g.j.lgj..",
    "..Gglglgg.lgGg..",
    "....GglgggG.....",
    ".......g........",
    "....l..g..l.....",
    "...jgl.g.lgj....",
    "...lGgggggGl....",
    "..jgG.Ggg.Ggj...",
    ".......g........",
    ".......g........",
]
T['gora'] = [
    "......jlj.......",
    ".....jjljj......",
    "...j..glg..j....",
    "..jlj..l..jlj...",
    "...glglllglg....",
    "....GgglggG.....",
    ".j.....l.....j..",
    "jlj..j.l.j..jlj.",
    ".glg.jjljj.glg..",
    "..Gglgglgglgg...",
    "....GGglgGG.....",
    "...j...l...j....",
    "..jlj..l..jlj...",
    "...glglglglg....",
    ".....GgggG......",
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

T['swiat_scieta'] = [  # po scieciu: kikuty z dolnymi liscmi
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......j..j......",
    "...j..g..g..j...",
    "..jgj.g..g.jgj..",
    "...G.jgv.gj.G...",
    "....v.Gg.gG.v...",
    "...vgv.g.g.vgv..",
    "....G..g.g..G...",
    "......g.g.g.....",
    "......b.b.b.....",
    ".....b..b..b....",
]

def obraz(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.': px[x, y] = tuple(int(PAL[ch][i:i+2], 16) for i in (0, 2, 4)) + (255,)
    return im

def chory(im):
    """Plamy choroby 2x2: rdzawy srodek, jasniejsza obwodka (czarne pojedyncze piksele wygladaly jak dziury)."""
    im = im.copy(); px = im.load()
    for (x, y) in [(4, 3), (9, 10), (3, 9), (10, 3)]:
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
