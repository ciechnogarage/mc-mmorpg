"""Animacje Szeptuchy: mieszanie kotła, rzut, szept i przerwanie."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


DANE = {
    'idle': {'dl': 2.4, 'petla': 'loop', 'kosci': {
        'tulow': {'position': K((0, (0, 0, 0)), (1.2, (0, .35, 0)),
                                  (2.4, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.2, (0, -3, 0)),
                                   (2.4, (0, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (1, 1, 1)), (1.2, (1.05, 1.05, 1.05)),
                                      (2.4, (1, 1, 1)))},
    }},
    'walk': {'dl': 1.4, 'petla': 'loop', 'kosci': {
        'tulow': {'position': K((0, (0, 0, 0)), (.35, (0, .7, 0)),
                                  (.7, (0, 0, 0)), (1.05, (0, .7, 0)),
                                  (1.4, (0, 0, 0))),
                  'rotation': K((0, (0, -3, 0)), (.7, (0, 3, 0)),
                                 (1.4, (0, -3, 0)))},
        'reka_l': {'rotation': K((0, (0, 0, 0)), (.7, (8, 0, 0)),
                                    (1.4, (0, 0, 0)))},
        'reka_r': {'rotation': K((0, (0, 0, 0)), (.7, (-7, 0, 0)),
                                    (1.4, (0, 0, 0)))},
        'kociol': {'rotation': K((0, (0, 0, -2)), (.7, (0, 0, 2)),
                                    (1.4, (0, 0, -2)))},
    }},
    'attack': {'dl': .6, 'petla': 'once', 'hit': .2, 'recover': .4,
               'skrypt': 'mm:BAGNA_SZEPTUCHA_ATTACK_KLATKA', 'kosci': {
        'reka_l': {'rotation': K((0, (0, 0, 0)), (.13, (20, 0, -12)),
                                    (.2, (-28, 0, 16)), (.6, (0, 0, 0)))},
        'tulow': {'rotation': K((0, (0, 0, 0)), (.13, (7, 0, 0)),
                                   (.2, (-10, 0, 0)), (.6, (0, 0, 0))),
                  'position': K((0, (0, 0, 0)), (.13, (0, .5, 0)),
                                    (.2, (0, .6, 0)), (.6, (0, 0, 0)))},
    }},
    'channel': {'dl': 1.0, 'petla': 'loop', 'windup': 3.0, 'hit': 3.0,
                'kosci': {
        'reka_r': {'rotation': K((0, (0, 0, 0)), (.25, (5, -8, 0)),
                                    (.5, (0, -16, 0)), (.75, (-5, -8, 0)),
                                    (1, (0, 0, 0)))},
        'chochla': {'position': K((0, (3, 0, 0)), (.25, (0, 0, 3)),
                                   (.5, (-3, 0, 0)), (.75, (0, 0, -3)),
                                   (1, (3, 0, 0)))},
        'glowka_chochli': {'position': K((0, (3, 0, 0)), (.25, (0, 0, 3)),
                                          (.5, (-3, 0, 0)), (.75, (0, 0, -3)),
                                          (1, (3, 0, 0)))},
        'glowa': {'rotation': K((0, (13, 0, 0)), (.5, (17, 0, 0)),
                                   (1, (13, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (1, 1, 1)), (.5, (1.22, 1.22, 1.22)),
                                      (1, (1, 1, 1)))},
    }},
    'throw': {'dl': 1.2, 'petla': 'once', 'windup': .8, 'hit': .85,
              'recover': .35, 'skrypt': 'mm:BAGNA_SZEPTUCHA_THROW_KLATKA',
              'kosci': {
        'reka_l': {'rotation': K((0, (0, 0, 0)), (.35, (30, 0, -17)),
                                    (.8, (52, 0, -24)), (.85, (-37, 0, 20)),
                                    (1.2, (0, 0, 0)))},
        'tulow': {'rotation': K((0, (0, 0, 0)), (.8, (12, 0, 0)),
                                   (.85, (-12, 0, 0)), (1.2, (0, 0, 0))),
                  'position': K((0, (0, 0, 0)), (.8, (0, .75, 0)),
                                    (.85, (0, .75, 0)), (1.2, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.8, (-13, 0, 0)),
                                   (1.2, (0, 0, 0)))},
        'fx_flaszka': {'scale': K((0, (1, 1, 1)), (.8, (1, 1, 1)),
                                    (.84, (1, 1, 1)), (.85, (0, 0, 0)),
                                    (1.2, (0, 0, 0)))},
    }},
    'push': {'dl': .9, 'petla': 'once', 'windup': .6, 'hit': .65,
             'recover': .25, 'skrypt': 'mm:BAGNA_SZEPTUCHA_PUSH_KLATKA',
             'kosci': {
        'tulow': {'rotation': K((0, (0, 0, 0)), (.3, (-10, 0, 0)),
                                   (.6, (-24, 0, 0)), (.65, (-30, 0, 0)),
                                   (.9, (0, 0, 0))),
                  'position': K((0, (0, 0, 0)), (.15, (0, .4, 0)),
                                    (.3, (0, .7, 0)),
                                    (.6, (0, 1.4, 0)), (.65, (0, 1.4, 0)),
                                    (.85, (0, .4, 0)), (.9, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.6, (-8, 0, 0)),
                                   (.65, (-16, 0, 0)), (.9, (0, 0, 0)))},
        'reka_l': {'rotation': K((0, (0, 0, 0)), (.65, (-22, 0, -14)),
                                    (.9, (0, 0, 0)))},
        'reka_r': {'rotation': K((0, (0, 0, 0)), (.65, (-22, 0, 14)),
                                    (.9, (0, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (1, 1, 1)), (.65, (1.3, 1.3, 1.3)),
                                      (.9, (1, 1, 1)))},
    }},
    'interrupted': {'dl': 1.0, 'petla': 'once', 'hit': .3,
                    'recover': .7, 'skrypt': 'mm:BAGNA_SZEPTUCHA_INTERRUPTED_KLATKA',
                    'kosci': {
        'kociol': {'rotation': K((0, (0, 0, 0)), (.3, (90, 0, 0)),
                                    (1, (90, 0, 0))),
                   'position': K((0, (0, 0, 0)), (.3, (0, 2, 0)),
                                     (1, (0, -1, -3)))},
        'reka_r': {'rotation': K((0, (0, 0, 0)), (.3, (32, 0, 22)),
                                    (1, (6, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.3, (20, 0, 0)),
                                   (1, (14, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (1, 1, 1)), (.3, (.5, .5, .5)),
                                      (1, (.35, .35, .35)))},
    }},
    'hurt': {'dl': .3, 'petla': 'once', 'kosci': {
        'tulow': {'rotation': K((0, (0, 0, 0)), (.1, (10, 0, 7)),
                                   (.3, (0, 0, 0))),
                  'position': K((0, (0, 0, 0)), (.1, (0, .8, 0)),
                                    (.3, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.1, (14, 0, 0)),
                                   (.3, (0, 0, 0)))},
    }},
    'death': {'dl': 1.2, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, (0, 0, 0)), (1.2, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.2, (30, 0, 15)))},
        'reka_l': {'rotation': K((0, (0, 0, 0)), (1.2, (30, 0, -18)))},
        'reka_r': {'rotation': K((0, (0, 0, 0)), (1.2, (30, 0, 18)))},
        'kociol': {'rotation': K((0, (0, 0, 0)), (1.2, (65, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (1, 1, 1)), (1.2, (.3, .3, .3)))},
    }},
    'spawn': {'dl': 1.0, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, (0, -4, 0)), (.75, (0, 0, 0)),
                                   (1, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (18, 0, 0)), (1, (0, 0, 0)))},
        'swiatlo_kociol': {'scale': K((0, (.45, .45, .45)), (1, (1, 1, 1)))},
    }},
}

# Bez kanału skali ModelEngine pokazuje flaszkę również w idle.
for nazwa, animacja in DANE.items():
    if nazwa != 'throw':
        animacja['kosci']['fx_flaszka'] = {
            'scale': K((0, (0, 0, 0)), (animacja['dl'], (0, 0, 0))),
        }

# Szeroki obieg chochli wokół krawędzi kotła i widoczne przechylenie przed czarem.
DANE['channel']['kosci']['reka_r']['rotation'] = K(
    (0, (8, -22, -12)), (.25, (30, -8, 18)),
    (.5, (-12, 20, 22)), (.75, (-28, 12, -18)),
    (1, (8, -22, -12)))
for n in ('chochla', 'glowka_chochli'):
    DANE['channel']['kosci'][n]['position'] = K(
        (0, (5, 1, 0)), (.25, (0, 3, -5)),
        (.5, (-5, 1, 0)), (.75, (0, -2, 5)),
        (1, (5, 1, 0)))
DANE['channel']['kosci']['kociol'] = {'rotation': K(
    (0, (0, 0, -5)), (.25, (7, 0, 8)),
    (.5, (12, 0, 0)), (.75, (5, 0, -8)),
    (1, (0, 0, -5)))}
DANE['channel']['kosci']['tulow'] = {'rotation': K(
    (0, (0, 0, 0)), (.25, (8, 0, -5)),
    (.5, (12, 0, 0)), (.75, (8, 0, 5)),
    (1, (0, 0, 0)))}
DANE['channel']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.25, (0, .4, 0)),
    (.5, (0, .65, 0)), (.75, (0, .4, 0)),
    (1, (0, 0, 0)))}
DANE['attack']['kosci']['kociol'] = {'rotation': K(
    (0, (0, 0, 0)), (.13, (18, 0, 0)),
    (.2, (-22, 0, 0)), (.35, (8, 0, 0)), (.6, (0, 0, 0)))}
DANE['attack']['kosci']['reka_r'] = {'rotation': K(
    (0, (0, 0, 0)), (.13, (35, 0, 20)),
    (.2, (-38, 0, -15)), (.6, (0, 0, 0)))}
DANE['throw']['kosci']['kociol'] = {'rotation': K(
    (0, (0, 0, 0)), (.55, (8, 0, -5)),
    (.8, (20, 0, -12)), (.85, (-16, 0, 8)),
    (1.2, (0, 0, 0)))}



# MM-5 (03.10, MM): smierc - kluczowa poza (lezenie) najpozniej w 0,8 s, dlugosc 1,0 s: ME zdejmuje model z encja
# ~1 s po smierci, a "trup" z ARMOR_STAND nie dziala. Czasy przeskalowane, ostatnia poza trzymana do konca.
def _skroc_smierc(d, poza=0.8, dl=1.0):
    koniec = max(t for k in d['kosci'].values() for kl in k.values() for t, _, _ in kl)
    s = min(1.0, poza / koniec) if koniec > 0 else 1.0
    for k in d['kosci'].values():
        for kanal, kl in list(k.items()):
            nowe = [(round(t * s, 3), v, i) for t, v, i in kl]
            if nowe[-1][0] < dl:
                nowe.append((dl, nowe[-1][1], nowe[-1][2]))
            k[kanal] = nowe
    d['dl'] = dl


_skroc_smierc(DANE['death'])

def main(model=None):
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/szeptucha/bagna_szeptucha.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
