"""Strefa korzeni Korzeniaka (Loch 001) - OSOBNY, stojacy model efektu (recenzja 2026-09-26: strefa w modelu moba
jechala razem z nim i zyla ~0.4 s). MythicMobs przywoluje go jako nieruchomy dummy na czas dzialania spowolnienia.

Plat rozgalezionych korzeni na ziemi (srednica = SREDNICA px; dopasowac do promienia slow w skillu MM: 16 px = 1 blok)
+ 5 kolcow korzeni wystajacych z ziemi. Animacje: spawn (wyrasta 0.4 s), idle (kolce drgaja), death (wciaga sie 0.5 s).
Uzycie: tools/modele/zbuduj.sh korzeniak_strefa
"""
import sys

import animacje as A
import tekstura
from bb import ELEMENTY, KOSCI, kosc as B, szescian as S, waliduj, zapisz

POD_ZIEMIA = {'spoczynek:0.5', 'idle:0.5', 'spawn:7', 'death:7'}  # kolce korzeni wychodza Z ziemi: podstawy pod nia
SREDNICA = 24  # px = 1.5 bloku (promien 0.75 bloku)

KOLCE = [(-7, -4, 22.5, 0), (6, -6, 0, -22.5), (-3, 7, -22.5, 0), (8, 5, 0, 22.5), (0, -9, 22.5, 0)]


def zbuduj():
    r = SREDNICA / 2
    plat = B('plat', (0, 0, 0), [S('plat', (-r, 0.1, -r), (r, 0.1, r), 5)])
    kolce = [B(f'kolec_{i}', (x, 0, z), [S(f'kolec_{i}', (x - 0.5, 0, z - 0.5), (x + 0.5, 4 + i % 2, z + 0.5), 2,
                                          origin=(x, 0, z), rot=(rx, 0, rz))])
             for i, (x, z, rx, rz) in enumerate(KOLCE)]
    hitbox = B('hitbox', (0, 0.5, 0), [S('hitbox_box', (-0.5, 0, -0.5), (0.5, 1, 0.5), 1)])
    return [B('strefa', (0, 0, 0), [plat] + kolce), hitbox]


Z0, UKRYTA, PELNA = (0, 0, 0), (0, 0, 0), (1, 1, 1)


def animacje():
    wyrost = {f'kolec_{i}': {'position': [(0, (0, -6, 0)), (0.1 + 0.05 * i, (0, -6, 0)), (0.3 + 0.05 * i, (0, 0.5, 0)), (0.4 + 0.05 * i, Z0), (0.7, Z0)]}
              for i in range(len(KOLCE))}
    drgania = {f'kolec_{i}': {'rotation': [(0, Z0), (0.3 + 0.2 * i, (0, 0, 4 if i % 2 else -4)), (0.5 + 0.2 * i, Z0), (2.0, Z0)]}
               for i in range(len(KOLCE))}
    chowanie = {f'kolec_{i}': {'position': [(0, Z0), (0.1 + 0.05 * i, (0, 0.5, 0)), (0.3 + 0.05 * i, (0, -6, 0)), (0.6, (0, -6, 0))]}
                for i in range(len(KOLCE))}
    return {
        'spawn': {'petla': 'once', 'dlugosc': 0.7, 'kosci': dict(wyrost, plat={'scale': [(0, (0.2, 1, 0.2)), (0.35, (1.05, 1, 1.05)), (0.45, PELNA)]})},
        'idle': {'petla': 'loop', 'dlugosc': 2.0, 'kosci': drgania},
        'death': {'petla': 'hold', 'dlugosc': 0.6, 'kosci': dict(chowanie, plat={'scale': [(0, PELNA), (0.5, (0.1, 1, 0.1)), (0.6, UKRYTA)]})},
    }


MATERIALY = {
    'plat': {'ramp': [(48, 40, 16), (72, 62, 26), (96, 82, 38), (118, 102, 50)], 'alfa': 'korzenie', 'szum': 0.08},
    'kolec': {'ramp': [(52, 48, 18), (76, 68, 26), (100, 86, 36), (122, 104, 46)], 'wzor': 'kora', 'szum': 0.1},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith('hitbox'):
        return 'techniczny'
    return 'plat' if n == 'plat' else 'kolec'


def main():
    outliner = zbuduj()
    anim = animacje()
    waliduj({k for a in anim.values() for k in a['kosci']})
    tex, _ = tekstura.atlas(ELEMENTY, MATERIALY, material, ziarno=5)
    zapisz(sys.argv[1], 'korzeniak_strefa', outliner, tex, 128, A.zbuduj(anim, KOSCI, do_me=True))


if __name__ == '__main__':
    main()
