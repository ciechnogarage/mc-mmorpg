"""Animacje Pijawki: fala po sześciu segmentach i zwinięty wyskok."""
from math import pi, sin
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


def fala(n):
    return K(*[(round(i * .04, 3), (0, round(4 * sin(2 * pi * (i * .04 - (n - 1) * .12) / .8), 2), 0))
               for i in range(21)])


DANE = {
    'idle': {'dl': 2.0, 'petla': 'loop', 'kosci': {
        'seg2': {'rotation': K((0, (0, 0, 0)), (1, (2, 3, 0)), (2, (0, 0, 0)))},
        'seg4': {'rotation': K((0, (0, 0, 0)), (1, (-2, -2, 0)), (2, (0, 0, 0)))},
        'seg6': {'rotation': K((0, (0, 0, 0)), (1, (0, 4, 0)), (2, (0, 0, 0)))},
        'mul': {'scale': K((0, (1, 1, 1)), (1, (1.04, 1, 1.04)), (2, (1, 1, 1)))},
    }},
    'walk': {'dl': .8, 'petla': 'loop', 'kosci': {
        **{f'seg{i}': {'rotation': fala(i)} for i in range(1, 7)},
        'seg1': {'rotation': fala(1),
                 'position': K((0, (0, 0, 0)), (.2, (0, .5, -.5)),
                                   (.4, (0, 0, 0)), (.6, (0, .5, -.5)),
                                   (.8, (0, 0, 0)))},
        'mul': {'scale': K((0, (1, 1, 1)), (.4, (1.12, 1, 1.06)),
                             (.8, (1, 1, 1)))},
    }},
    'attack': {'dl': .5, 'petla': 'once', 'hit': .15, 'recover': .3,
               'skrypt': 'mm:BAGNA_PIJAWKA_ATTACK_KLATKA', 'kosci': {
        'seg1': {'position': K((0, (0, 0, 0)), (.1, (0, .3, 1)),
                                  (.15, (0, .5, -1.25)), (.5, (0, 0, 0))),
                 'rotation': K((0, (0, 0, 0)), (.1, (0, 4, 0)),
                                (.15, (0, -6, 0)), (.5, (0, 0, 0)))},
        'paszcza': {'scale': K((0, (1, 1, 1)), (.15, (1.35, 1.35, 1.2)),
                               (.5, (1, 1, 1)))},
        'swiatlo_paszcza': {'scale': K((0, (1, 1, 1)), (.15, (1.25, 1.25, 1.25)),
                                       (.5, (1, 1, 1)))},
    }},
    'wyskok': {'dl': 1.1, 'petla': 'once', 'windup': .6, 'hit': .9,
               'recover': .2, 'skrypt': 'mm:BAGNA_PIJAWKA_WYSKOK_KLATKA',
               'kosci': {
        'seg1': {'position': K((0, (0, 0, 0)), (.6, (0, 0, 0)),
                                  (.72, (0, 4, -1)), (.84, (0, 8, -3)),
                                  (.9, (0, 5, -5)), (1.1, (0, 0, 0)))},
        **{f'seg{i}': {'rotation': K((0, (0, 0, 0)),
                                      (.3, (.3 * (10 + (i - 2) * 5), 0, 0)),
                                      (.6, (.3 * (22 + (i - 2) * 5), 0, 0)),
                                      (.78, (0, 0, 0)), (.9, (-2.1, 0, 0)),
                                      (1.1, (0, 0, 0)))}
           for i in range(2, 6)},
        'paszcza': {'scale': K((0, (1, 1, 1)), (.6, (1, 1, 1)),
                               (.9, (1.45, 1.45, 1.2)), (1.1, (1, 1, 1)))},
        'mul': {'scale': K((0, (1, 1, 1)), (.6, (1.08, 1, 1.08)),
                             (.9, (1.2, 1, 1.2)), (1.1, (1, 1, 1))),
                'position': K((0, (0, 0, 0)), (.1, (0, 0, 0)),
                              (.2, (0, 0, -2)), (.3, (0, 0, -3)),
                              (.4, (0, 0, -4)), (.5, (0, 0, -5)),
                              (.6, (0, 0, -6)), (.7, (0, 0, -3)),
                              (.8, (0, 7, 0)), (.9, (0, 9, -2)),
                              (1, (0, 4, 0)), (1.1, (0, 0, 0)))},
    }},
    'hurt': {'dl': .3, 'petla': 'once', 'kosci': {
        'seg1': {'rotation': K((0, (0, 0, 0)), (.12, (4.8, 0, 0)),
                                  (.3, (0, 0, 0)))},
        'seg2': {'rotation': K((0, (0, 0, 0)), (.12, (4.2, 0, 0)),
                                  (.3, (0, 0, 0)))},
        'seg3': {'rotation': K((0, (0, 0, 0)), (.12, (3, 0, 0)),
                                  (.3, (0, 0, 0)))},
    }},
    'death': {'dl': 1.0, 'petla': 'once', 'kosci': {
        **{f'seg{i}': {'scale': K((0, (1, 1, 1)), (1, (1.06, .75, 1.05)))}
           for i in range(1, 7)},
        'seg1': {'scale': K((0, (1, 1, 1)), (1, (1.06, .75, 1.05))),
                 'rotation': K((0, (0, 0, 0)), (1, (25, 0, 0)))},
        'mul': {'scale': K((0, (1, 1, 1)), (1, (1.3, 1, 1.25)))},
    }},
    'spawn': {'dl': .8, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, (0, -4, 0)), (.8, (0, 0, 0)))},
        'mul': {'scale': K((0, (1, 1, 1)), (.8, (1, 1, 1)))},
    }},
}

# Najpierw zwinięcie do tyłu, potem wyrzut całego ciała o ponad pół długości.
DANE['attack']['kosci']['paszcza']['scale'] = K(
    (0, (1, 1, 1)), (.10, (.7, .7, .8)),
    (.15, (1.55, 1.55, 1.55)), (.23, (1.3, 1.3, 1.2)),
    (.35, (.72, .72, .8)), (.5, (1, 1, 1)))
for i in range(2, 6):
    DANE['attack']['kosci'][f'seg{i}'] = {'rotation': K(
        (0, (0, 0, 0)), (.1, (3, 0, 0)),
        (.15, (-3, 0, 0)), (.3, (2, 0, 0)),
        (.5, (0, 0, 0)))}
DANE['wyskok']['kosci']['rzesa'] = {'scale': K(
    (0, (1, 1, 1)), (.6, (1.12, 1.12, 1.12)),
    (.9, (1.12, 1.12, 1.12)), (1.1, (1, 1, 1)))}
DANE['wyskok']['kosci']['paszcza']['scale'] = K(
    (0, (1, 1, 1)), (.6, (.8, .8, .85)),
    (.9, (1.4, 1.4, 1.3)), (1, (.8, .8, .85)),
    (1.1, (1, 1, 1)))
# Przesunięcie korzenia utrzymuje razem segmenty i pasma rzęsy.
DANE['attack']['kosci']['seg1']['position'] = K(
    (0, (0, 0, 0)), (.1, (0, 0, 1.5)), (.15, (0, 0, 0)),
    (.35, (0, 0, 0)), (.5, (0, 0, 0)))
DANE['attack']['kosci']['seg1']['rotation'] = K(
    (0, (0, 0, 0)), (.1, (0, 4, 0)), (.15, (0, -6, 0)),
    (.5, (0, 0, 0)))
DANE['attack']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.1, (0, 0, 2)), (.15, (0, 1.6, -9)),
    (.25, (0, 1.6, -7)), (.4, (0, .5, -2)), (.5, (0, 0, 0)))}
DANE['wyskok']['kosci']['seg1']['position'] = K(
    (0, (0, 0, 0)), (.6, (0, 0, 0)), (.72, (0, 4, -1)),
    (.84, (0, 8, -3)), (.9, (0, 5, -5)), (1.1, (0, 0, 0)))
DANE['wyskok']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.6, (0, 0, 0)), (.78, (0, 0, -2)),
    (.9, (0, 0, -7)), (1.1, (0, 0, 0)))}
DANE['death']['kosci']['seg1']['rotation'] = K(
    (0, (0, 0, 0)), (1, (10, 0, 0)))



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
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/pijawka/bagna_pijawka.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
