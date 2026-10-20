"""Trzy warianty wygladu dziurawca do wyboru (20.10). Jedna roslina = ten sam model w swiecie i z bliska, tylko w innej skali.
A "Kwiat z gry": gesty krzew 16x16 jak krzewy vanilli (piwonia, rozany krzew), karty krzyzowe.
B "Ilustracja": 4 warstwy 32x32 (tyl, srodek, przod, kwiaty), cieniowanie glebia, kontur tylko po stronie cienia.
Wspolna ziemia pod rosliną: plat gleby z cieniem, kamykami i zdzblami. Wariant C (bryly) nie ma tekstur.
Paleta: zielenie krzewow vanilli (rose_bush/peony), zolcie mniszka i dzikich kwiatow. Swiatlo z gory-lewej.
Uzycie: wzory.py <katalog tex>"""
import os, sys, random
from PIL import Image

PAL = {
    '0': '1a381f', '1': '204626', '2': '265a25', '3': '2b702a', '4': '4a8f28', '5': '55ab2d', '6': '7fc059',
    'Y': 'ffec4f', 'y': 'fed639', 'o': 'f5ba27', 'O': 'f19d25', 'r': 'bd6a22', 'k': '5a3a12',
    'd': '5d3f2a', 'D': '45301f', 'e': '6e4c33', 'E': '79553a', 'n': '2e1e12', 's': '8a8a84', 'S': '6b6b66',
}

def rgb(ch): return tuple(int(PAL[ch][i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def z_siatki(rows):
    w = len(rows[0]); assert all(len(r) == w for r in rows), [len(r) for r in rows]
    im = Image.new('RGBA', (w, len(rows)), (0, 0, 0, 0)); px = im.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.': px[x, y] = rgb(ch)
    return im

# ---------------------------------------------------------------- A: krzew jak w vanilli (masa lisci + korona kwiatow)
A = [
    "...Yy....yY.....",
    "..YorY..YroY.Yy.",
    "Yy.yO.4..yO.YorY",
    "orY.4.4...4..yO.",
    "yO.454.Yy.4.4.4.",
    ".4.4.4YorY.454..",
    ".454.4.yO.44.45.",
    "..4544.4.4434.4.",
    ".5543344343.454.",
    "..4433343433344.",
    ".4433232332332..",
    "..3322232223232.",
    "...32212.2121...",
    "....21.1.12.1...",
    ".....1.1.1..1...",
    ".....0..1..0....",
]

# ---------------------------------------------------------------- B: warstwy 32x32 malowane z kodu (decyzje jawne)
KWIAT_D = ["..Y..", ".yoY.", "YorOy", ".Ok..", "..O.."]   # duzy kwiat: 5 platkow, srodek, gruczol (k) na brzegu
KWIAT_M = [".Y.", "yoO", ".O."]                                 # maly / dalszy
PAK = [".y.", "yO."]                                              # paczek
LISCIE = [["..aa", "abbb", "bb.."], [".aa", "abb"], ["aab"], ["..a", "abb", "b.."]]   # podluzne, siedzace, rozne rozmiary

def krzywa(xb, xt, yb, yt, t): return round(xb + (xt - xb) * t * t), round(yb + (yt - yb) * t)

def warstwa(rnd, pedy, jasny, ciemny, lodyga, kwiaty=False, masa=None):
    """pedy: (x podstawy, x czubka, y czubka). Lodyga lukiem, liscie naprzemianlegle w roznym rozstawie,
    u gory 2-3 szypulki z kwiatami (baldachogrono). masa: tonacje ciemnej masy krzewu pod spodem (tylko tylna warstwa)."""
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); px = im.load()
    def put(x, y, c):
        if 0 <= x < 32 and 0 <= y < 32: px[x, y] = rgb(c)
    if masa:   # masa lisci: suma elips wokol srodkowej czesci pedow, jasniejsza od lewej-gory
        srodki = [(krzywa(xb, xt, 31, yt, 0.55), 6 + rnd.random() * 3) for xb, xt, yt in pedy]
        for y in range(32):
            for x in range(32):
                for (cx, cy), r in srodki:
                    d = ((x - cx) / r) ** 2 + ((y - cy) / (r * 1.3)) ** 2
                    if d < 1:
                        put(x, y, masa[0] if (x - cx) + (y - cy) > r * 0.4 else masa[1] if (x + y) % 2 or d > 0.5 else masa[2]); break
    for (xb, xt, yt) in pedy:
        steps = 31 - yt
        for i in range(steps + 1):
            x, y = krzywa(xb, xt, 31, yt, i / steps); put(x, y, lodyga)
        y = 29 - rnd.randint(0, 2); strona = rnd.choice((-1, 1))
        while y > yt + 3:
            t = (31 - y) / steps; x, _ = krzywa(xb, xt, 31, yt, t)
            sp = LISCIE[min(3, int(t * 3.2) + rnd.randint(0, 1))]
            for dy, r in enumerate(sp):
                for dx, ch in enumerate(r):
                    if ch == '.': continue
                    xx = x - len(r) + dx if strona < 0 else x + 1 + (len(r) - 1 - dx)
                    put(xx, y + dy - 1, jasny if ch == 'a' else ciemny)
            strona = -strona; y -= rnd.randint(2, 4)
        if kwiaty:
            for i, (ox, oy) in enumerate([(-3, 1), (2, -1), (4, 2)][:2 + rnd.randint(0, 1)]):
                for k in range(1, 3): put(xt + round(ox * k / 3), yt + 2 - k, lodyga)   # szypulka
                motyw = KWIAT_D if i == 1 else KWIAT_M if rnd.random() < 0.75 else PAK
                for dy, r in enumerate(motyw):
                    for dx, ch in enumerate(r):
                        if ch != '.': put(xt + ox - len(r) // 2 + dx, yt + oy - len(motyw) // 2 + dy - 1, ch)
    return im

def selout(im, kolor, strona=(1, 1)):
    """Kontur tylko po stronie cienia (prawo-dol): czytelna sylwetka bez ciezkiej czarnej obwodki."""
    px = im.load(); w, h = im.size; dodaj = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3]: continue
            sx, sy = x - strona[0], y - strona[1]
            if 0 <= sx < w and 0 <= sy < h and px[sx, sy][3]: dodaj.append((x, y))
    for x, y in dodaj: px[x, y] = rgb(kolor)
    return im

# ---------------------------------------------------------------- ziemia pod roslina (lezy plasko)
def ziemia(rnd):
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); px = im.load()
    for y in range(32):
        for x in range(32):
            d = ((x - 15.5) / 15) ** 2 + ((y - 15.5) / 15) ** 2
            if d > 1: continue
            if d > 0.72 and (x + y + rnd.randint(0, 2)) % 3 == 0: continue   # postrzepiony brzeg wtapia sie w trawe
            c = 'D' if d < 0.18 else 'd' if d < 0.35 else ('d' if rnd.random() < 0.7 else 'e') if d < 0.75 else ('e' if rnd.random() < 0.5 else 'E')
            px[x, y] = rgb(c)
    for x, y in [(8, 20), (22, 9), (24, 22)]:   # kamyki
        for dx, dy, c in [(0, 0, 's'), (1, 0, 's'), (0, 1, 'S'), (1, 1, 'S')]: px[x + dx, y + dy] = rgb(c)
    for x, y in [(4, 14), (27, 15), (13, 4), (17, 28), (6, 24), (25, 6)]:   # zdzbla trawy przy brzegu
        px[x, y] = rgb('4'); px[x, y - 1] = rgb('5')
    return im

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    rnd = random.Random(7)
    z_siatki(A).save(f'{out}/wzora_dziurawiec.png')
    tyl = warstwa(rnd, [(9, 6, 13), (15, 14, 10), (21, 24, 12), (26, 28, 16)], '2', '1', '1', masa=('0', '1', '2'))
    srodek = warstwa(rnd, [(12, 10, 9), (18, 19, 7), (23, 22, 11)], '4', '3', '3', kwiaty=True)
    przod = warstwa(rnd, [(8, 7, 15), (16, 15, 12), (22, 23, 14)], '6', '5', '4', kwiaty=True)
    kwiaty = Image.new('RGBA', (32, 32), (0, 0, 0, 0))   # kwiaty siedza w warstwach srodek/przod (na swoich pedach)
    selout(srodek, '1'); selout(przod, '2')
    for n, im in [('tyl', tyl), ('srodek', srodek), ('przod', przod)]: im.save(f'{out}/wzorb_{n}.png')
    ziemia(rnd).save(f'{out}/wzor_ziemia.png')
    print('wzory: A, B x4, ziemia')
