"""Ciężki chód i ataki Mułowca na geometrii CLAUDE."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


DANE = {
    'idle': {'dl': 2.4, 'petla': 'loop', 'kosci': {
        'barki': {'position': K((0, (0, 0, 0)), (1.2, (0, .5, 0)),
                                   (2.4, (0, 0, 0)))},
        'swiatlo_rana': {'scale': K((0, (1, 1, 1)), (1.2, (1.08, 1.08, 1.08)),
                                    (2.4, (1, 1, 1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.2, (-3, 0, 0)),
                                   (2.4, (0, 0, 0)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (2.4, (0, 0, 0)))},
    }},
    'walk': {'dl': 1.4, 'petla': 'loop', 'kosci': {
        'korzen': {'position': K((0, (0, 0, 0)), (.35, (0, .8, 0)),
                                   (.7, (0, 0, 0)), (1.05, (0, .8, 0)),
                                   (1.4, (0, 0, 0)))},
        'barki': {'rotation': K((0, (0, 0, 0)), (.7, (0, 0, 0)),
                                   (1.4, (0, 0, 0)))},
        'noga_l': {'rotation': K((0, (0, 0, 0)), (.35, (0, -9, 0)),
                                    (.7, (0, 0, 0)), (1.05, (0, 7, 0)),
                                    (1.4, (0, 0, 0)))},
        'noga_r': {'rotation': K((0, (0, 0, 0)), (.35, (0, 7, 0)),
                                    (.7, (0, 0, 0)), (1.05, (0, -9, 0)),
                                    (1.4, (0, 0, 0)))},
        'ramie_l': {'rotation': K((0, (0, 0, 0)), (1.4, (0, 0, 0)))},
        'ramie_r': {'rotation': K((0, (0, 0, 0)), (1.4, (0, 0, 0)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (1.4, (0, 0, 0)))},
    }},
    'attack': {'dl': 1.4, 'petla': 'once', 'windup': .8, 'hit': .9,
               'recover': .5, 'skrypt': 'mm:BAGNA_MULOWIEC_ATTACK_KLATKA',
               'kosci': {
        'rzesa': {'position': K((0, (0, 0, 0)), (.4, (0, 0, 0)),
                                   (.8, (-.35, 0, 0)),
                                   (1.4, (0, 0, 0)))},
        'tulow': {'rotation': K((0, (0, 0, 0)), (.4, (0, 8, 0)),
                                   (.8, (0, 12, 0)), (.9, (0, -7, 0)),
                                   (1.4, (0, 0, 0)))},
        'ramie_r': {'rotation': K((0, (0, 0, 0)), (.4, (0, 8, 0)),
                                     (.8, (0, 16, 0)), (.9, (0, -16, 0)),
                                     (1.4, (0, 0, 0)))},
        'przedramie_r': {'rotation': K((0, (0, 0, 0)), (.8, (0, 10, 0)),
                                          (.9, (0, -12, 0)), (1.4, (0, 0, 0)))},
        'dlon_r': {'rotation': K((0, (0, 0, 0)), (.8, (0, 4, 0)),
                                    (.9, (0, -8, 0)), (1.4, (0, 0, 0))),
                   'position': K((0, (0, 0, 0)), (.8, (0, 1, 0)),
                                     (.9, (0, 3.5, 0)), (1.15, (0, 2, 0)),
                                     (1.4, (0, 0, 0)))},
        'lopata': {'rotation': K((0, (0, 0, 0)), (.8, (0, 5, 0)),
                                    (.9, (0, -10, 0)), (1.4, (0, 0, 0)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (1.4, (0, 0, 0)))},
    }},
    'throw': {'dl': 1.4, 'petla': 'once', 'windup': 1, 'hit': 1.05,
              'recover': .35, 'skrypt': 'mm:BAGNA_MULOWIEC_THROW_KLATKA',
              'kosci': {
        'ramie_l': {'rotation': K((0, (0, 0, 0)), (.45, (12, 0, -10)),
                                     (1, (35, 0, -18)), (1.05, (-10, 0, 8)),
                                     (1.4, (0, 0, 0))),
                    'position': K((0, (0, 0, 0)), (.15, (0, 1.4, 0)),
                                      (.3, (0, 2, 0)),
                                      (1, (0, 2, 0)),
                                      (1.05, (0, 3.4, 0)),
                                      (1.25, (0, 2.5, 0)),
                                      (1.4, (0, 0, 0)))},
        'przedramie_l': {'rotation': K((0, (0, 0, 0)), (.45, (18, 0, 0)),
                                          (1, (28, 0, 0)), (1.05, (-10, 0, 0)),
                                          (1.4, (0, 0, 0)))},
        'dlon_l': {'rotation': K((0, (0, 0, 0)), (1, (20, 0, 0)),
                                    (1.05, (-8, 0, 0)), (1.4, (0, 0, 0)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (.4, (0, 0, 0)),
                              (.45, (1, 1, 1)), (1.05, (1, 1, 1)),
                              (1.08, (0, 0, 0)), (1.4, (0, 0, 0))),
                   'position': K((0, (0, 0, 0)), (1.05, (0, 1, -1)),
                                     (1.4, (0, 0, 0)))},
        'tulow': {'rotation': K((0, (0, 0, 0)), (1, (0, -12, 0)),
                                   (1.05, (0, 8, 0)), (1.4, (0, 0, 0)))},
    }},
    'hurt': {'dl': .35, 'petla': 'once', 'kosci': {
        'barki': {'rotation': K((0, (0, 0, 0)), (.12, (0, 8, 0)),
                                   (.35, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.12, (14, 0, 0)),
                                   (.35, (0, 0, 0)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (.35, (0, 0, 0)))},
    }},
    'death': {'dl': 1.5, 'petla': 'once', 'kosci': {
        'tulow': {'scale': K((0, (1, 1, 1)), (1.5, (1.1, 1, 1.1)))},
        'barki': {'rotation': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.5, (28, 0, 0)))},
        'ramie_l': {'rotation': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))},
        'ramie_r': {'rotation': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))},
        'swiatlo_rana': {'scale': K((0, (1, 1, 1)), (1.5, (.5, .5, .5)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))},
    }},
    'spawn': {'dl': 1.5, 'petla': 'once', 'kosci': {
        'tulow': {'scale': K((0, (1, .18, 1)), (1, (1, 1, 1)),
                              (1.5, (1, 1, 1)))},
        'barki': {'rotation': K((0, (22, 0, 0)), (1.5, (0, 0, 0)))},
        'swiatlo_rana': {'scale': K((0, (.4, .4, .4)), (1.5, (1, 1, 1)))},
        'fx_mul': {'scale': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))},
    }},
}

# Zamach angażuje masę tułowia; łopata idzie nad głowę i uderza w 0,9 s.
cios = DANE['attack']['kosci']
cios['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.4, (0, 2.5, 1)),
    (.8, (0, 5, 2)), (.9, (0, 3, -2.5)),
    (1.12, (0, 1.5, -1)), (1.4, (0, 0, 0))),
    'rotation': K((0, (0, 0, 0)), (.4, (-7, 10, 0)),
                  (.8, (-15, 35, 0)), (.9, (18, -10, 0)),
                  (1.12, (8, -5, 0)), (1.4, (0, 0, 0)))}
cios['tulow']['rotation'] = K(
    (0, (0, 0, 0)), (.4, (0, 8, 0)),
    (.8, (0, 16, 0)), (.9, (0, -12, 0)),
    (1.1, (0, -5, 0)), (1.4, (0, 0, 0)))
cios['ramie_r']['rotation'] = K(
    (0, (0, 0, 0)), (.4, (0, 0, 0)), (.45, (105, 12, -25)),
    (.8, (105, 12, -25)), (.9, (-28, -12, 14)),
    (1.12, (-12, 0, 8)), (1.4, (0, 0, 0)))
cios['przedramie_r']['rotation'] = K(
    (0, (0, 0, 0)), (.8, (30, 0, -12)),
    (.9, (-24, 0, 12)), (1.4, (0, 0, 0)))
cios['dlon_r']['rotation'] = K(
    (0, (0, 0, 0)), (.45, (0, 0, 0)),
    (.8, (55, 0, 0)), (.9, (-45, 0, 0)),
    (1.15, (-12, 0, 0)), (1.4, (0, 0, 0)))
cios['lopata']['rotation'] = K(
    (0, (0, 0, 0)), (1.4, (0, 0, 0)))
cios['fx_mul'] = {'scale': K(
    (0, (0, 0, 0)), (.88, (0, 0, 0)),
    (.9, (2.3, 1.6, 2.3)), (1.03, (1.5, 1.1, 1.5)),
    (1.08, (0, 0, 0)), (1.4, (0, 0, 0))),
    'position': K((0, (0, 0, 0)), (.88, (0, 0, 0)),
                  (.9, (0, 2, -18)), (1.08, (0, 2, -18)),
                  (1.18, (0, 0, 0)), (1.4, (0, 0, 0)))}
cios['rzesa']['position'] = K(
    (0, (0, 0, 0)), (.4, (0, 0, 0)),
    (.8, (-.35, 0, 0)), (1.4, (0, 0, 0)))



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
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/mulowiec/bagna_mulowiec.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
