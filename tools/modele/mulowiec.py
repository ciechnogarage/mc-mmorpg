"""Mułowiec K2 (CLAUDE, 03.10): utopiony robotnik grobli, którego muł obrósł — postawa goryla: garbate barki z mchem
i rzęsą, mała głowa wysunięta POD linią barków, okrągły brzuch z mułu, resztki sinej koszuli na piersi, turkusowa
rana z zaciekami (słaby punkt), długie ręce na knykciach, łopata w prawej dłoni (trzonek = segment w obróconej kości)."""
from pathlib import Path
import json
import numpy as np
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
from kokoniara import segment
import tekstura
import atlas_wspolny

MAT = {
 'mul':     {'ramp': [(56, 40, 26), (82, 60, 38), (106, 80, 52), (122, 94, 62)], 'plamy': .1},
 'mul_c':   {'ramp': [(40, 28, 18), (58, 42, 28), (76, 56, 36), (90, 68, 44)]},
 'mech':    {'ramp': [(52, 66, 30), (74, 92, 42), (94, 114, 54), (108, 128, 62)], 'plamy': .12},
 'tkanina': {'ramp': [(58, 72, 78), (80, 98, 104), (104, 122, 126), (124, 140, 142)]},
 'turkus':  {'ramp': [(20, 80, 70), (36, 130, 114), (63, 209, 180), (150, 240, 220)]},
 'oko':     {'ramp': [(14, 12, 10)] * 4},
 'drewno':  {'ramp': [(60, 42, 26), (84, 60, 38), (108, 80, 52), (126, 96, 64)]},
 'zelazo':  {'ramp': [(70, 72, 74), (98, 100, 102), (128, 128, 128), (150, 150, 148)]},
 'rzesa':   {'ramp': [(52, 70, 22), (74, 96, 30), (100, 124, 42), (126, 150, 60)]},
}


def buduj():
    m = Model(MAT, budzet=460, atlas=256)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('tulow', (0, 12, 0), 'korzen')
    m.elipsoida('tulow', (0, 14, 0), (10, 9, 8.5), 'mul')                       # brzuch
    m.czesc('barki', (0, 22, 1), 'tulow')
    m.elipsoida('barki', (0, 24, 2), (12.5, 6.5, 8), 'mech')
    # Lewy bark jest wyniesioną bryłą mchu, prawy osiadł pod ciężarem łopaty.
    m.elipsoida('barki', (-10, 29, 2), (7.5, 7, 7), 'mech')
    m.elipsoida('barki', (10, 24.5, 3), (6.5, 4.5, 7.5), 'mech')
    m.elipsoida('barki', (-6, 31, 5), (5, 4, 5), 'mul_c')
    m.elipsoida('barki', (9, 22, 7), (4.5, 3, 5), 'mul')
    # koszula: warstwa przed brzuchem/piersią, poszarpany dół
    m.czesc('koszula', (0, 19, -7), 'tulow')
    for x in range(-9, 9):
        for y in range(15, 24):
            for z in range(-11, -3):
                p = (x + .5, y + .5, z + .5)
                d = (p[0] / 10.6) ** 2 + ((p[1] - 15) / 10) ** 2 + (p[2] / 9.3) ** 2
                if .86 < d <= 1 and not (y < 17 and (x * 7 + y * 3) % 5 < 2):     # strzępy u dołu
                    m.czesci['koszula'].vox[x, y, z] = 'tkanina'
    # rana na piersi + zacieki
    m.czesc('swiatlo_rana', (-3, 19, -9), 'koszula')
    m.elipsoida('swiatlo_rana', (-3, 19.5, -9.2), (1.8, 2.2, 1.2), 'turkus')
    for i, (x, y0, l) in enumerate(((-3.5, 17, 2), (-2.5, 17, 3), (-4.5, 18, 2))):
        m.detal('swiatlo_rana', f'zaciek_{i}', (x, y0 - l, -10.2), (x + 1, y0, -9.2), 'turkus')
    # głowa: wysunięta pod linią barków, nawis czoła, oczy-szczeliny
    m.czesc('glowa', (0, 24, -6), 'barki')
    m.elipsoida('glowa', (0, 23.5, -10), (4.2, 4.4, 4.2), 'mul_c')
    m.pudlo('glowa', (-4, 25, -15), (4, 27, -12), 'mul_c')                      # nawis czoła
    for s in (-1, 1):
        m.detal('glowa', f'oko_{s}', (s * 2 - .5 if s > 0 else -2.5, 23.5, -14.5), (s * 2 + 1.5 if s > 0 else -.5, 24.5, -13.5), 'oko')
    m.detal('glowa', 'usta', (-1.5, 21, -14.3), (1.5, 22, -13.3), 'oko')
    # ręce: ramię, przedramię, pięść na knykciach
    for s, st in ((-1, 'l'), (1, 'r')):
        r, p, d = 'ramie_' + st, 'przedramie_' + st, 'dlon_' + st
        m.czesc(r, (s * 12, 26, 1), 'barki')
        m.kapsula(r, (s * 12, 26, 1), (s * 15, 15, -3), 4.0, 3.3, 'mech')
        m.czesc(p, (s * 15, 15, -3), r)
        m.kapsula(p, (s * 15, 15, -3), (s * 15.5, 4, -6), 3.3, 3.6, 'mul')
        m.czesc(d, (s * 15.5, 4, -6), p)
        m.elipsoida(d, (s * 15.5, 3, -6.5), (3.4, 3, 3.4), 'mul_c')
        for f in (-1.5, 0, 1.5):
            m.detal(d, f'knykiec_{f}', (s * 15.5 + f - .5, 0, -10), (s * 15.5 + f + .5, 2, -9), 'mul_c')
    # nogi krótkie, słoniowe
    for s, st in ((-1, 'l'), (1, 'r')):
        n = 'noga_' + st
        m.czesc(n, (s * 6, 8, 2), 'korzen')
        m.elipsoida(n, (s * 6, 6.5, 2), (4.2, 6.5, 4.6), 'mul')
        lo, hi = ((3, 0, -3), (10, 2, 6)) if s > 0 else ((-10, 0, -3), (-3, 2, 6))
        m.pudlo(n, lo, hi, 'mul_c')
    # Narzędzie uniesione: lemiesz ma pionową płaszczyznę czytelną od frontu.
    I = np.eye(3)
    R, L = segment(m, 'lopata', 'dlon_r', I, (15.5, 5, -7), (21, 15, -18), 2.4, 'drewno')
    m.czesc('ostrze', (21, 15, -18), 'dlon_r')  # nieobrócona kość zachowuje płaszczyznę lemiesza
    for y, half in ((14, 3), (15, 4), (16, 5), (17, 6), (18, 6),
                    (19, 6), (20, 6), (21, 6), (22, 5), (23, 4), (24, 3)):
        m.pudlo('ostrze', (21-half, y, -20), (21+half, y+1, -17), 'zelazo')
    m.detal('ostrze', 'grzbiet_ostrza', (16, 14, -21), (26, 16, -18), 'zelazo')
    m.detal('ostrze', 'krawedz_ostrza', (17, 24, -20.5), (25, 25, -17.5), 'zelazo')
    m.czesc('p_lopata_pkt', (21, 20, -19), 'ostrze')
    m.czesc('p_dlon', (-15.5, 2, -7), 'dlon_l')
    m.czesc('fx_mul', (-15.5, 6, -7), 'dlon_l')
    m.elipsoida('fx_mul', (-15.5, 7, -8), (2.5, 2.5, 2.5), 'mul_c')
    # rzęsa z barków i ramion, zacieki mułu na brzuchu
    m.czesc('rzesa', (0, 30, 2), 'barki')
    for i, (x, z, y, l) in enumerate(((-15, -2, 30, 6), (-11, -5, 30, 5), (-6, -6, 30, 4), (6, -6, 30, 5), (11, -5, 30, 6),
                                      (15, -1, 29, 7), (-16, 4, 29, 6), (16, 5, 28, 5), (-8, 8, 31, 5), (8, 8, 31, 6),
                                      (0, 9, 31, 4), (-18, 0, 24, 5), (18, 0, 24, 5))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    for i, (x, y, z, w, l) in enumerate(((-14, 29, 1, 2.5, 9), (-8, 33, 6, 1.5, 7),
                                         (7, 27, 8, 2, 7), (13, 25, 5, 3, 10))):
        m.detal('barki', f'pasmo_glonow_{i}', (x-w/2, y-l, z-1),
                (x+w/2, y, z+1), 'rzesa')
    m.czesc('zacieki', (0, 8, -6), 'tulow')
    for i, (x, l) in enumerate(((-6, 3), (-2, 4), (3, 2), (7, 3))):
        m.detal('zacieki', f'z_{i}', (x - .5, 7 - l, -7.5 + abs(x) * .25), (x + .5, 7, -4.0), 'mul_c')
    m.uprosc(['tulow', 'barki', 'noga_l', 'noga_r', 'ramie_l', 'ramie_r', 'przedramie_l', 'przedramie_r'], krok=2, prog=.5)
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/mulowiec'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_mulowiec.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_mulowiec', hitbox=(21, 35, 26), tag_y=40)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 2.4, {'barki': ('position', (0, -.6, 0)), 'glowa': ('rotation', (-3, 0, 0))})
    from anim_mulowiec import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
