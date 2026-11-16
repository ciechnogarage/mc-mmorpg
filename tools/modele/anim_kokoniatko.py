"""Ruch Kokoniątka; nakładka na gotowy model CLAUDE."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


NOGI = [f'noga_{bok}{i}' for bok in ('l', 'r') for i in range(1, 5)]
PARY_A = {'noga_l1', 'noga_r2', 'noga_l3', 'noga_r4'}

DANE = {
    'idle': {'dl': 1.2, 'petla': 'loop', 'kosci': {
        'tulow': {'position': K((0, (0, 0, 0)), (.6, (0, .3, 0)), (1.2, (0, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (.6, (1.06, 1.06, 1.06)),
                                    (1.2, (1, 1, 1)))},
    }},
    'walk': {'dl': .5, 'petla': 'loop', 'kosci': {
        'tulow': {'position': K((0, (0, 0, 0)), (.125, (0, .65, 0)),
                                  (.25, (0, 0, 0)), (.375, (0, .65, 0)),
                                  (.5, (0, 0, 0)))},
        **{n: {'rotation': K((0, (0, 16 if n in PARY_A else -16, 0)),
                              (.25, (0, -16 if n in PARY_A else 16, 0)),
                              (.5, (0, 16 if n in PARY_A else -16, 0)))}
           for n in NOGI},
    }},
    'attack': {'dl': .4, 'petla': 'once', 'hit': .12, 'recover': .28,
               'skrypt': 'mm:BAGNA_KOKONIATKO_ATTACK_KLATKA', 'kosci': {
        'tulow': {'position': K((0, (0, 0, 0)), (.08, (0, 0, 1)),
                                  (.12, (0, .7, -2.5)), (.4, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.08, (12, 0, 0)),
                                   (.12, (-22, 0, 0)), (.4, (0, 0, 0)))},
        'szczeki': {'rotation': K((0, (0, 0, 0)), (.1, (25, 0, 0)),
                                     (.12, (-20, 0, 0)), (.4, (0, 0, 0)))},
    }},
    'leap': {'dl': .9, 'petla': 'once', 'windup': .5, 'hit': .7, 'recover': .2,
             'skrypt': 'mm:BAGNA_KOKONIATKO_LEAP_KLATKA', 'kosci': {
        'rzesa': {'position': K((0, (0, -.25, 0)), (.9, (0, -.25, 0)))},
        'tulow': {'scale': K((0, (1, 1, 1)), (.5, (1.1, .72, 1.12)),
                              (.58, (1, 1, 1)), (.9, (1, 1, 1))),
                  'position': K((0, (0, 0, 0)), (.5, (0, 0, 1)),
                                    (.65, (0, 3.5, -2)), (.7, (0, 2, -4)),
                                    (.78, (0, .5, -4)), (.9, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.5, (18, 0, 0)),
                                   (.7, (-18, 0, 0)), (.9, (0, 0, 0)))},
        'szczeki': {'rotation': K((0, (0, 0, 0)), (.5, (0, 0, 0)),
                                     (.7, (32, 0, 0)), (.9, (0, 0, 0)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (.5, (20, 0, 0)),
                              (.65, (-12, 0, 0)), (.9, (0, 0, 0)))} for n in NOGI},
    }},
    'hurt': {'dl': .25, 'petla': 'once', 'kosci': {
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (.1, (.8, .8, .8)),
                                    (.25, (1, 1, 1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.1, (12, 0, 0)),
                                   (.25, (0, 0, 0)))},
    }},
    'death': {'dl': .8, 'petla': 'once', 'kosci': {
        'tulow': {'scale': K((0, (1, 1, 1)), (.8, (1.15, .55, 1.1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.8, (30, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (.8, (.45, .45, .45)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (.8, (0, 35, 0)))} for n in NOGI},
    }},
    'spawn': {'dl': .8, 'petla': 'once', 'kosci': {
        'tulow': {'scale': K((0, (1, .15, 1)), (.55, (1, 1, 1)),
                              (.8, (1, 1, 1)))},
        'swiatlo_worek': {'scale': K((0, (.45, .45, .45)), (.8, (1, 1, 1)))},
        **{n: {'rotation': K((0, (0, 35, 0)), (.8, (0, 0, 0)))} for n in NOGI},
    }},
}

# Nogi składają się pod pancerzem, po wybiciu rozpościerają się wokół kłów.
DANE['leap']['kosci']['tulow']['scale'] = K(
    (0, (1, 1, 1)), (.5, (1.18, .58, 1.18)),
    (.61, (1, 1.12, 1)), (.7, (1, 1.08, 1)),
    (.9, (1, 1, 1)))
DANE['leap']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.1, (0, .22, 0)),
    (.5, (0, .22, 0)),
    (.65, (0, 3.2, -1)), (.7, (0, 3.5, -2)),
    (.8, (0, 1.5, -2)), (.9, (0, 0, 0)))}
for n in NOGI:
    znak = 1 if n.startswith('noga_l') else -1
    DANE['leap']['kosci'][n]['rotation'] = K(
        (0, (0, 0, 0)), (.5, (-32, znak * 38, 0)),
        (.65, (32, znak * 28, 0)),
        (.7, (38, znak * 25, 0)),
        (.8, (12, znak * 18, 0)), (.9, (0, 0, 0)))
DANE['leap']['kosci']['szczeki']['rotation'] = K(
    (0, (0, 0, 0)), (.5, (-12, 0, 0)),
    (.65, (42, 0, 0)), (.7, (45, 0, 0)),
    (.9, (0, 0, 0)))
DANE['leap']['kosci']['szczeki']['scale'] = K(
    (0, (1, 1, 1)), (.5, (.85, .85, .85)),
    (.7, (1.3, 1.3, 1.3)), (.9, (1, 1, 1)))
DANE['leap']['kosci']['rzesa']['position'] = K(
    (0, (0, -.25, 0)), (.3, (0, -.5, 0)),
    (.5, (0, -.5, 0)), (.9, (0, -.25, 0)))



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
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/kokoniatko/bagna_kokoniatko.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
