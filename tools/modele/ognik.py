"""Błędny Ognik K1 (CLAUDE, runda 2 po RECENZJA_ART): łza-płomień ze stosu przekrojów zwężających się i wygiętych
do tyłu; trzy warstwy jasności (ciemny obrys z tyłu/boków, turkus, biało-turkusowy rdzeń z przodu z czaszką);
trzy nierówne, rozdzielone języki płomienia (bez symetrycznych rogów); kościane iskry na orbicie.
Cały płomień świeci (kości swiatlo_*), bo to ognik nad bagnem nocą."""
from math import sin, cos, pi
from pathlib import Path
import json
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'obrys':  {'ramp': [(10, 46, 44), (16, 64, 60), (24, 86, 80), (34, 104, 96)]},
 'plomien': {'ramp': [(30, 120, 106), (44, 156, 138), (62, 190, 168), (84, 214, 190)], 'plamy': .05},
 'rdzen':  {'ramp': [(150, 236, 214), (184, 246, 230), (214, 252, 242), (240, 255, 250)]},
 'czaszka': {'ramp': [(14, 52, 48), (20, 64, 58), (26, 76, 70), (34, 90, 82)]},
 'kosc':   {'ramp': [(150, 142, 116), (184, 176, 148), (212, 204, 178), (234, 228, 206)]},
 'rzesa':  {'ramp': [(36, 56, 22), (54, 78, 30), (76, 102, 42), (102, 128, 58)]},
}
Y0 = 5   # dół płomienia nad ziemią
# przekroje łzy: (dy, dz wychylenia do tyłu, promień)
import math as _m
def _r(t):   # profil łzy: pękata podstawa, długi zwężający się czubek (t = 0..1)
    return 5.4 * (_m.sin(_m.pi * min(1, t * 1.35)) ** .6 if t < .37 else (1 - (t - .37) / .63) ** 1.15) + .45
PRZEKROJE = [(dy, 4.6 * (dy / 15) ** 2, _r(dy / 15)) for dy in range(0, 16)]


def buduj():
    m = Model(MAT, budzet=240, atlas=128)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('plomien', (0, Y0 + 5, 0), 'korzen')
    m.czesc('swiatlo_plomien', (0, Y0 + 5, 0), 'plomien')
    for dy, dz, r in PRZEKROJE:                                  # zewnętrzna warstwa: turkus płomienia
        m.elipsoida('swiatlo_plomien', (0, Y0 + dy + .5, dz), (r, .9, r * .9), 'plomien')
    for dy, dz, r in PRZEKROJE[2:12]:                             # ciemny obrys tylko z tyłu (głębia), nie z przodu
        m.elipsoida('swiatlo_plomien', (0, Y0 + dy + .5, dz + r * .5), (r * .9, .9, r * .5), 'obrys')
    # rdzeń: duży, jasny, wypchnięty przez front płomienia
    m.odejmij('swiatlo_plomien', 'elipsoida', (0, Y0 + 5.5, -2.6), (3.6, 4.0, 2.6))
    m.czesc('swiatlo_rdzen', (0, Y0 + 5.5, -1.5), 'plomien')
    m.elipsoida('swiatlo_rdzen', (0, Y0 + 5.5, -1.6), (3.4, 3.9, 3.0), 'rdzen')
    # czaszka na rdzeniu
    m.czesc('czaszka', (0, Y0 + 5.5, -4.6), 'swiatlo_rdzen')
    for x0 in (-2, 1):
        m.detal('czaszka', f'oczodol_{x0}', (x0, Y0 + 6, -5.2), (x0 + 1, Y0 + 7.5, -4.2), 'czaszka')
    m.detal('czaszka', 'nos', (-.5, Y0 + 4.6, -5.2), (.5, Y0 + 5.6, -4.2), 'czaszka')
    m.detal('czaszka', 'usta', (-1.5, Y0 + 3.0, -5.2), (1.5, Y0 + 4.0, -4.2), 'czaszka')
    # trzy nierówne języki: lewy krótki, prawy długi, tylny najdłuższy (wszystkie w górę i do tyłu)
    for n, a, b, c, r0 in (('jezyk_l', (-3.2, Y0 + 8, .5), (-5.0, Y0 + 11.5, 2.0), (-4.2, Y0 + 14, 3.6), 1.3),
                           ('jezyk_r', (3.0, Y0 + 9, 1.0), (5.2, Y0 + 13, 3.0), (4.4, Y0 + 17, 5.0), 1.2)):
        m.czesc(n, a, 'plomien')
        m.kapsula(n, a, b, r0, .9, 'plomien')
        m.kapsula(n, b, c, .9, .55, 'plomien')
    m.czesc('swiatlo_jezyk', (0, Y0 + 14, 4), 'swiatlo_plomien')
    m.kapsula('swiatlo_jezyk', (0, Y0 + 14, 4.2), (-1, Y0 + 19, 7), 1.2, .55, 'plomien')
    # kilka pasm rzęsy przewieszonych przez płomień (rzadziej niż w r1)
    m.czesc('rzesa', (0, Y0 + 10, 1), 'plomien')
    for i, (x, z, y, l) in enumerate(((-3.8, 1.5, 9, 4), (3.6, 2.5, 10, 5), (1.5, 4.5, 12, 4))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, Y0 + y - l, z - .5), (x + .5, Y0 + y, z + .5), 'rzesa')
    # kościane iskry na orbicie
    m.czesc('iskry', (0, Y0 + 1, 0), 'korzen')
    for k in range(5):
        a = k / 5 * 2 * pi + .3
        x, z = 6.2 * cos(a), 6.2 * sin(a)
        dx, dz = (1, 0) if abs(cos(a)) < .7 else (0, 1)
        m.detal('iskry', f'kosc_{k}', (x - .5, Y0 + .5 + (k % 2) * 2, z - .5), (x + .5 + dx, Y0 + 1.5 + (k % 2) * 2, z + .5 + dz), 'kosc')
    m.czesc('p_rdzen', (0, Y0 + 5.5, -3), 'swiatlo_rdzen')
    print('dosunięte:', m.dosun_detale(pomin=('iskry',)))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/ognik'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_ognik.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_ognik', hitbox=(11, 21, 12), tag_y=27)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 2.0, {'plomien': ('position', (0, .8, 0))})
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
