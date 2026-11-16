"""Ślimak Bagienny K1 (CLAUDE, 03.10): skorupa = rura zwinięta w spiralę logarytmiczną (zwoje stykają się,
rowek między nimi ciemny), stopa ze śluzem, czułki z turkusowymi oczami, pęknięcie z tyłu (słaby punkt),
na szczycie mini-mokradło: trzcina, grzybek, rzęsa zwisająca z krawędzi."""
from math import pi, sin, cos, exp, log, atan2, sqrt
from pathlib import Path
import json
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'sluz':    {'ramp': [(58, 66, 40), (84, 96, 58), (110, 124, 76), (140, 154, 98)], 'plamy': .05, 'jasna_krawedz': True},
 'sluz_j':  {'ramp': [(96, 108, 70), (128, 140, 92), (156, 168, 116), (186, 196, 146)]},
 'skorupa': {'ramp': [(62, 58, 34), (88, 82, 48), (116, 108, 64), (146, 136, 84)], 'plamy': .08},
 'mech':    {'ramp': [(46, 62, 28), (66, 86, 38), (88, 110, 50), (114, 136, 66)], 'plamy': .06},
 'rowek':   {'ramp': [(24, 24, 16), (34, 34, 22), (46, 44, 28), (60, 56, 36)]},
 'mul':     {'ramp': [(48, 35, 24), (70, 52, 34), (93, 70, 47), (120, 92, 63)], 'plamy': .07},
 'rzesa':   {'ramp': [(30, 46, 22), (46, 66, 30), (66, 90, 42), (92, 116, 60)]},
 'turkus':  {'ramp': [(18, 70, 62), (34, 112, 98), (63, 209, 180), (159, 240, 220)]},
 'oko_c':   {'ramp': [(10, 12, 10)] * 4},
 'grzyb':   {'ramp': [(79, 54, 37), (110, 76, 49), (143, 102, 66), (174, 131, 88)]},
}

C = (0.0, 13.0, 6.0)        # oś spirali (Y,Z), skorupa nad tylną połową stopy
R0, ZWOJE = 10.5, 2.3
B = log(R0 / 2.2) / (2 * pi * ZWOJE)
K = 0.33                    # promień rury / promień spirali -> zwoje stykają się


def sciezka(t):
    """t w [0, 2pi*ZWOJE]; start na dole skorupy, zwój idzie do tyłu i w górę."""
    R = R0 * exp(-B * t)
    phi = -pi / 2 - t
    return (C[1] + R * sin(phi), C[2] - R * cos(phi)), R


def skorupa(m):
    c = m.czesci['skorupa']
    probki = [sciezka(i / 240 * 2 * pi * ZWOJE) + (i / 240 * 2 * pi * ZWOJE,) for i in range(241)]
    d2d = {}
    for x in range(-12, 12):
        for y in range(1, 28):
            for z in range(-7, 19):
                p = (x + .5, y + .5, z + .5)
                best = None
                for (py, pz), R, t in probki:
                    r = K * R
                    rx = r * (2.8 if t < 2 * pi else 2.0)        # skorupa szeroka: z przodu kopuła, nie wieża (RECENZJA_ART)
                    d = (p[0] / rx) ** 2 + ((p[1] - py) / r) ** 2 + ((p[2] - pz) / r) ** 2
                    if d <= 1 and (best is None or d < best[0]):
                        best = (d, py, pz, r, t)
                if best:
                    d, py, pz, r, t = best
                    c.vox[x, y, z] = 'mech' if p[1] > C[1] + 5 and t < 1.2 * pi else 'skorupa'
                    d2d[x, y, z] = sqrt((p[1] - py) ** 2 + (p[2] - pz) ** 2) / r
    # pępek spirali: wypukły środek zamiast dziury
    for x in range(-4, 4):
        for y in range(int(C[1]) - 3, int(C[1]) + 4):
            for z in range(int(C[2]) - 3, int(C[2]) + 4):
                if ((x + .5) / 3.6) ** 2 + ((y + .5 - C[1]) / 2.8) ** 2 + ((z + .5 - C[2]) / 2.8) ** 2 <= 1:
                    c.vox.setdefault((x, y, z), 'skorupa'); d2d.setdefault((x, y, z), 0)
    # rowek = ciągła linia na granicy zwojów (R(t)*(1-K)), na skrajnej ścianie boku
    kolumny = {}
    for (x, y, z) in c.vox:
        lo, hi = kolumny.get((y, z), (x, x)); kolumny[y, z] = (min(lo, x), max(hi, x))
    import math
    for i in range(2000):
        t = i / 2000 * 2 * pi * ZWOJE
        R = R0 * math.exp(-B * t) * (1 - K)
        phi = -pi / 2 - t
        y = int(math.floor(C[1] + R * sin(phi))); z = int(math.floor(C[2] - R * cos(phi)))
        if (y, z) in kolumny and R > 1.2:
            lo, hi = kolumny[y, z]
            for x, w in ((lo, 1), (hi, -1)):
                for dy in (0, 1):                       # żłobek 2 px szerokości, 1 px głębokości
                    q = (x, y + dy, z)
                    if q in c.vox:
                        del c.vox[q]
                        if (x + w, y + dy, z) in c.vox:
                            del c.vox[x + w, y + dy, z]; m.czesci['skret'].vox[x + w, y + dy, z] = 'rowek'


def buduj():
    m = Model(MAT, budzet=540, atlas=128)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('stopa', (0, 2, 0), 'korzen')
    m.elipsoida('stopa', (0, 2.5, 0), (7.0, 2.6, 15.5), 'sluz')            # długa, płaska stopa
    m.pudlo('stopa', (-5, 0, -13), (5, 1, 14), 'sluz_j')                   # mokry spód
    m.czesc('tulow', (0, 5, 2), 'stopa')
    m.elipsoida('tulow', (0, 5.5, 2), (6.0, 3.6, 10), 'sluz')
    m.czesc('skorupa', (0, 6, 5), 'tulow', obrot=(0, 25, 0))   # spirala obrócona ku kamerze 3/4
    m.czesc('skret', (0, 14, 6), 'skorupa')
    skorupa(m)
    m.czesc('szyja', (0, 6, -9), 'tulow')
    m.kapsula('szyja', (0, 5, -8), (0, 8, -15), 3.6, 3.0, 'sluz')
    m.czesc('glowa', (0, 8, -15), 'szyja')
    m.elipsoida('glowa', (0, 8.5, -16), (3.6, 3.2, 3.4), 'sluz')
    m.detal('glowa', 'paszcza', (-2, 6, -19.5), (2, 7, -18.5), 'rowek')
    for s, side in ((-1, 'l'), (1, 'r')):
        m.czesc('czulek_' + side, (s * 1.8, 10.5, -16.5), 'glowa')
        m.kapsula('czulek_' + side, (s * 1.8, 10.5, -16.5), (s * 3.6, 18.5, -19.5), 1.0, .7, 'sluz')
        m.czesc('swiatlo_oko_' + side, (s * 3.4, 18, -19.2), 'czulek_' + side)
        m.elipsoida('swiatlo_oko_' + side, (s * 3.4, 18, -19.2), (1.4, 1.4, 1.4), 'turkus')
        m.detal('swiatlo_oko_' + side, 'zrenica_' + side, (s * 3.4 - .5, 17.5, -20.8), (s * 3.4 + .5, 18.5, -19.8), 'oko_c')
        m.czesc('macka_' + side, (s * 1.6, 6.5, -18.5), 'glowa')
        m.kapsula('macka_' + side, (s * 1.6, 6.5, -18.5), (s * 3.2, 4.5, -21), .8, .6, 'sluz')
    m.czesc('p_glowa', (0, 7, -20), 'glowa')
    # pęknięcie z tyłu zewnętrznego zwoju: turkusowa szczelina z ciemnym obrzeżem
    m.czesc('pekniecie', (0, 14, 16), 'skorupa')
    m.czesc('swiatlo_pekniecie', (0, 14, 16.5), 'pekniecie')
    m.czesc('p_pekniecie', (0, 14, 16.5), 'pekniecie')
    for i, (y0, y1, x) in enumerate(((9, 13, 0), (12, 16, 1), (15, 19, 0))):
        m.detal('swiatlo_pekniecie', f'szczelina_{i}', (x - .5, y0, 15.2), (x + .5, y1, 16.2), 'turkus')
    m.detal('pekniecie', 'obrzeze_l', (-1.5, 9, 15), (-.5, 19, 16), 'rowek')
    m.detal('pekniecie', 'obrzeze_r', (1.5, 12, 15), (2.5, 19, 16), 'rowek')
    # mini-mokradło na szczycie zewnętrznego zwoju
    m.czesc('trzcina', (0, 23, 6), 'skorupa')
    for i, (x, z, h) in enumerate(((-2, 4, 3), (0, 6, 4), (2, 5, 3), (-1, 8, 2))):
        m.detal('trzcina', f'trzcina_{i}', (x - .5, 22, z - .5), (x + .5, 22 + h, z + .5), 'mech')
    m.detal('trzcina', 'kolba', (-.5, 26, 5.5), (.5, 28, 6.5), 'grzyb')
    m.czesc('grzyb', (3, 23, 9), 'skorupa')
    m.detal('grzyb', 'trzon', (2.5, 22, 8.5), (3.5, 24, 9.5), 'sluz_j')
    m.detal('grzyb', 'kapelusz', (1.5, 24, 7.5), (4.5, 25, 10.5), 'grzyb')
    m.czesc('rzesa', (0, 20, 6), 'skorupa')
    for i, (x, z, y, l) in enumerate(((-4.5, -2, 15, 4), (-4.8, 2, 19, 5), (-4.2, 10, 20, 4), (-4.5, 14, 14, 3),
                                      (4.2, -1, 16, 4), (4.6, 4, 20, 6), (4.0, 11, 19, 4), (4.4, 15, 13, 3))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z), (x + .5, y, z + 1), 'rzesa')
    m.czesc('sluz_slad', (0, 0, 14), 'stopa')                                 # smuga śluzu za ślimakiem
    m.pudlo('sluz_slad', (-4, 0, 13), (4, 1, 22), 'sluz_j')
    m.pudlo('sluz_slad', (-3, 0, 22), (3, 1, 27), 'sluz_j')
    m.czesc('mul_dol', (0, 1, 0), 'stopa')
    for i, (x, z, l) in enumerate(((-5.5, -6, 3), (5.5, -2, 2), (-5.5, 6, 2), (5.5, 9, 3))):
        m.detal('mul_dol', f'zaciek_{i}', (x - .5, 0, z), (x + .5, l, z + 2), 'mul')
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/slimak'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_slimak.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_slimak', hitbox=(22, 26, 12), tag_y=32)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 3.0, {'czulek_l': ('rotation', (4, 0, 2)), 'czulek_r': ('rotation', (-3, 0, -2)),
                           'glowa': ('rotation', (2, 0, 0))})
    from anim_slimak import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
