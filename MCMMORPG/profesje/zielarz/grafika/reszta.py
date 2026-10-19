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
    'd': '5d3f2a', 'D': '45301f', 'e': '6e4c33', 'E': '79553a',   # ziemia (barwy dirt vanilli)
    'a': '7fc059', 'A': '55ab2d', 's': '2f5a3a', 'S': '3e6e4e', 'T': '23452c',   # czosnek jasny zoltozielony / konwalia sinozielona
    'x': 'f0d9a8', 'X': 'd4b47c', 'u': '3b2a1e',          # plotno rekawic (cieple), kontur
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
T['kozlek_swiat_wykopany'] = [  # po wykopaniu: ciemny dolek, zwiedly listek
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
    "........v.......",
    "......vgv.......",
    "...dEdDDDDdEd...",
    "..dDDDDDDDDDDd..",
    ".dEdDDDDDDDdEdd.",
]
# ---------------------------------------------------------------- SZEROKOLISTNE (sobowtory)
T['szerokolistna_swiat'] = [  # wspolny blok: same liscie, z daleka czosnek = konwalia
    "................",
    "................",
    "................",
    "....H.....H.....",
    "...Hh....Hh.....",
    "...hhH..Hhh..H..",
    "....hhH.hhh.Hh..",
    ".H..LhH.hhL.hh..",
    ".hH.LhhHhhL.hL..",
    "..hHLhhlhhLHhL..",
    "..LhhLhllhLhhL..",
    "...LhhLllLhhL...",
    "....LhhllhhL....",
    ".....LLllLL.....",
    "......LLhL......",
    ".......LL.......",
]
T['czosnek_lisc'] = [  # lancetowaty, jasny zoltozielony, na jasnym ogonku
    ".......j........",
    "......aj........",
    ".....aajA.......",
    "....aaajAA......",
    "....aaajAAA.....",
    "...aaaajAAA.....",
    "...aaaajAAA.....",
    "....aaajAA......",
    "....aaajAA......",
    ".....aajA.......",
    "......ajA.......",
    ".......j........",
    ".......j........",
    ".......j........",
    ".......h........",
    ".......h........",
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
T['czosnek_baldach'] = [  # baldach z gory: gwiazdki bialych kwiatow w kole (karta pozioma)
    "................",
    "......W..W......",
    "....WWcW.WcW....",
    "...WcW.WWW.W....",
    "....W.WcWcW.W...",
    "..W.WW.lWW.WcW..",
    ".WcW.WcWlWc.W...",
    "..W.W.llHllW.W..",
    "...WcWlHjHlWcW..",
    "..W.W.llHll.W...",
    ".WcW.WcWlWcW.W..",
    "..W.WW.lW.W.WcW.",
    "....W.WcWcW..W..",
    "...WcW.WWW.WcW..",
    "....W..WcW..W...",
    "........W.......",
]
T['rekawice'] = [  # rekawice zielarza: cieply bez, kciuk, mankiet z rzemykiem
    "................",
    "......u..u......",
    ".....uXuuXu.u...",
    ".....uXuuXuuXu..",
    "..u..uXuuXuuXu..",
    ".uXu.uXuuXuuXu..",
    ".uXxuuxXXxXXXu..",
    "..uXxxxxxxxxXu..",
    "...uXxxxxxxxXu..",
    "...uXxxxxxxxXu..",
    "....uXxxxxxXu...",
    "....uDDDDDDDu...",
    "....uDeDeDeDu...",
    "....uDDDDDDDu...",
    ".....uuuuuuu....",
    "................",
]
T['konwalia_lisc'] = [  # szeroki owalny, ciemny sinozielony, bez ogonka, obejmuje lodyge
    "................",
    ".......S........",
    "......SSs.......",
    ".....SSSHs......",
    "....SSSSHss.....",
    "...SSSSSHsss....",
    "...SSSSSHsss....",
    "...SSSShHsss....",
    "...SSShhHsss....",
    "....SShhHss.....",
    "....SShhHss.....",
    ".....ShhHs......",
    ".....ShhHs......",
    "......hhH.......",
    ".....ThhHT......",
    ".....TThTT......",
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
    """Starszy lisc: zolknie sam czubek i brzegi przy czubku (pelne pasy na szerokosc wygladaly z bliska jak deska)."""
    im = im.copy(); px = im.load()
    for y in range(0, 7):
        xs = [x for x in range(16) if px[x, y][3]]
        if not xs: continue
        for x in xs:
            brzeg = x in (xs[0], xs[-1])
            if y < 2 or (y < 4 and brzeg) or (y < 7 and brzeg and y % 2 == 0):
                px[x, y] = ((0xd0, 0xc0, 0x58) if y < 2 else (0xa8, 0xb0, 0x48)) + (255,)
    return im

def bez_bieli(im):
    """Lodyzka bez kwiatow (biale gwiazdki przechodza na pozioma karte baldachu)."""
    im = im.copy(); px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and r > 180 and g > 180 and b > 180: px[x, y] = (0, 0, 0, 0)
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    ims = {k: obraz(v) for k, v in T.items()}
    ims['czosnek_lisc_stary'] = stary(ims['czosnek_lisc']); ims['konwalia_lisc_stary'] = stary(ims['konwalia_lisc'])
    ims['czosnek_lodyzka'] = bez_bieli(ims['czosnek_kwiat'])
    for k, im in ims.items(): im.save(f'{out}/{k}.png')
    print(' '.join(sorted(ims)))
