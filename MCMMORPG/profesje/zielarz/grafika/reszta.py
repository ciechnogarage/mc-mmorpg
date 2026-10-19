"""Kozlek, czosnek niedzwiedzi, konwalia: bloki w swiecie + karty do zblizenia. Jeden znak = jeden piksel (16x16)."""
import os, sys
from PIL import Image

PAL = {
    'G': '177c04', 'g': '4a8f28', 'l': '55ab2d', 'j': '7fc059',
    'L': '377f13', 'h': '50922f', 'H': '64b132',          # liscie konwalii/czosnku (vanilla lily)
    'v': '8a9a3a', 'M': 'c9c860', 'y': 'e6d65a',          # starzejace sie / zolta koncowka
    'W': 'ffffff', 'w': 'e7e7e7', 'c': 'd6e8e8', 'C': 'b2d0d0',   # biel kwiatow
    'R': 'fccbe7', 'r': 'f7b9dc', 'q': 'f396cd', 'Q': 'e563a3',   # roz kozlka
    'K': 'eadbb0', 'k': 'c9b07a', 'z': '9a7f50',          # korzen
    'd': '6b4a2c', 'D': '4e3420', 'e': '8a6440', 'E': 'a07a50',   # ziemia
}
T = {}
# ---------------------------------------------------------------- KOZLEK
T['kozlek_swiat'] = [
    "....rRQRr.......",
    "...RqRrRqR......",
    "....QrqRQ.......",
    ".....llll.......",
    ".......l........",
    ".....j.l........",
    "....jgjl........",
    ".......l.j......",
    ".......ljgj.....",
    ".......l........",
    "..j.j..l..j.j...",
    "..gjgj.l.jgjg...",
    "...gGgjlggGg....",
    ".jgjgGglgGgjgj..",
    "..GgG.GgG..GgG..",
    "......GgG.......",
]
T['kozlek_lodyga'] = [  # odcinek lodygi z para pierzastych lisci
    ".......l........",
    ".......l........",
    "..j.j..l..j.j...",
    "..gjgj.l.jgjg...",
    "...Gg.glg.gG....",
    "......Ggg.......",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
]
T['kozlek_baldach'] = [  # baldach: kopula drobnych rozowo-bialych kwiatkow
    "................",
    "................",
    ".....rRWRr......",
    "...rRqRrRqRr....",
    "..RqrWRqRWrqR...",
    "..qRrqrRrqRrq...",
    "...QrQq.qQrQ....",
    "....l.l.l.l.....",
    ".....l.lll......",
    "......lll.......",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
]
T['kozlek_lisc'] = [  # lisc rozety: pierzasty, lezy (os glowna poziomo, nasada z lewej)
    "................",
    "................",
    "................",
    "....j...j...j...",
    "...jg..jg..jg.j.",
    "..ggg.ggg.ggg.gj",
    "hhhhhhhhhhhhhhhh",
    "..GgG.GgG.GgG.G.",
    "...Gg..Gg..Gg...",
    "....G...G...G...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
T['kozlek_korzen'] = [  # korzen: palowy z korzonkami (pionowo, gora = szyjka)
    ".......Kk.......",
    ".......Kk.......",
    "......KKkz......",
    "......Kkkz......",
    "....k.Kkz.......",
    ".....kKkz..k....",
    "......Kkzkk.....",
    "......Kkz.......",
    "...kk.Kkz.......",
    ".....kKkz.......",
    "......Kz...k....",
    "......Kkzkk.....",
    ".......kz.......",
    ".......kz.......",
    "........z.......",
    "........z.......",
]
T['kozlek_gleba'] = [  # placek luznej ziemi (lezy plasko), poszarpany brzeg
    "................",
    "....dd.de.......",
    "..ddeEdddEd.d...",
    ".dEdDdedEddEdd..",
    "..dEddDdEdeDdEd.",
    ".deDdEddDdedEd..",
    "dEddedEddEdDdEd.",
    ".dDdEdDdedEddedd",
    "ddEdedEdDdEdDed.",
    ".dedDdEddEdedEd.",
    "..dEddedDdEddd..",
    ".ddEdEdDdedEdd..",
    "...dDdedEddEd...",
    "....ddEddedd....",
    "......dd.dd.....",
    "................",
]
T['kozlek_gleba_rozkopana'] = [  # rozluzniona ziemia: ciemniejsza, grudy, szczelina
    "................",
    "....DD.De.......",
    "..DDeEDDDED.D...",
    ".DEDdDeDEDDEDD..",
    "..DEDDdDEDeDDED.",
    ".DeDDEdDDDeDED..",
    "DEDDeDEdd.DDdED.",
    ".DdDED...DeDDeDD",
    "DDEDeDd..dEDdeD.",
    ".DeDdDEd.DeDeDE.",
    "..DEDDeDDdEDDD..",
    ".DDEDEDdDeDEDD..",
    "...DdDeDEDDED...",
    "....DDEDDeDD....",
    "......DD.DD.....",
    "................",
]
T['kozlek_swiat_wykopany'] = [  # po wykopaniu: dolek, rozrzucona ziemia, zwiedly lisc
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".........v......",
    "...d....vg......",
    "..dEd.dd.v..d...",
    ".dDDDdEdDdd.dEd.",
    "dEdDDDDdEdDdedEd",
]
# ---------------------------------------------------------------- SZEROKOLISTNE (sobowtory)
T['szerokolistna_swiat'] = [  # wspolny blok: z daleka czosnek = konwalia
    "................",
    "................",
    "......W..W......",
    ".....WcW.cW.....",
    "......W.l.W.....",
    "...H...l.l...H..",
    "..Hh...l.l..hH..",
    "..hhH..l.l.Hhh..",
    "..LhhH.lll.hhL..",
    "...LhH.ll.Hhh...",
    "...LhhHllHhhL...",
    "....LhhllhhL....",
    "....LLhllhLL....",
    ".....LLhlLL.....",
    "......LLhL......",
    ".......LL.......",
]
T['czosnek_lisc'] = [  # lancetowaty lisc na ogonku, jasny nerw
    ".......j........",
    "......Hj........",
    "......Hjh.......",
    ".....HHjhh......",
    ".....HHjhh......",
    ".....HHjhL......",
    ".....HhjhL......",
    ".....hhjhL......",
    "......hjL.......",
    "......hjL.......",
    ".......jL.......",
    ".......h........",
    ".......h........",
    ".......h........",
    ".......L........",
    ".......L........",
]
T['czosnek_kwiat'] = [  # baldach bialych gwiazdek na prostej lodyzce
    "....W...W.......",
    "...WcW.WcW.W....",
    "....W.W.W.WcW...",
    ".W..l.WcW..W....",
    "WcW.l..l..l.....",
    ".W.l..l.l.l.....",
    "...l..l.l.l.....",
    "....l.l.ll......",
    ".....lll........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......l........",
    ".......h........",
    ".......h........",
]
T['konwalia_lisc'] = [  # szeroki eliptyczny lisc wprost z pochwy, bez ogonka
    ".......H........",
    "......HHj.......",
    ".....HHHjh......",
    "....HHHHjhh.....",
    "....HHHHjhhL....",
    "....HHHhjhhL....",
    "....HHHhjhhL....",
    "....HHhhjhhL....",
    ".....HhhjhL.....",
    ".....HhhjhL.....",
    "......hhjL......",
    "......hhjL......",
    ".......hjL......",
    ".......hL.......",
    "......LhhL......",
    "......LLhL......",
]
T['konwalia_kwiat'] = [  # wygieta lodyzka, dzwonki zwisaja na jedna strone
    "................",
    "........lll.....",
    ".......l...l....",
    "......l....cl...",
    ".....l.....WW...",
    "....l.....cWWc..",
    "...l.c.....W.c..",
    "...l.WW.........",
    "...lcWWc........",
    "...l.WWc........",
    "...l..w.........",
    "...l............",
    "...l............",
    "...h............",
    "...h............",
    "...h............",
]

T['szerokolistna_swiat_zerwana'] = [  # oskubana kepa: krotkie ogonki i pochwy
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......h...h.....",
    "....h.h..hL.....",
    ".....hL.hL..h...",
    "...L.LhlhL.hL...",
    "....LLhlhLLL....",
    ".....LLLLL......",
]

def obraz(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.': px[x, y] = tuple(int(PAL[ch][i:i+2], 16) for i in (0, 2, 4)) + (255,)
    return im

def stary(im):
    """Starszy lisc: koncowka zolknie pasami (bez szachownicy — szum wyglada tanio)."""
    im = im.copy(); px = im.load()
    for y in range(0, 5):
        kol = (0xd8, 0xc8, 0x5a) if y < 2 else (0xb0, 0xb4, 0x48) if y < 4 else (0x8a, 0x9a, 0x3a)
        for x in range(16):
            if px[x, y][3]: px[x, y] = kol + (255,)
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    ims = {k: obraz(v) for k, v in T.items()}
    ims['czosnek_lisc_stary'] = stary(ims['czosnek_lisc']); ims['konwalia_lisc_stary'] = stary(ims['konwalia_lisc'])
    for k, im in ims.items(): im.save(f'{out}/{k}.png')
    print(' '.join(sorted(ims)))
