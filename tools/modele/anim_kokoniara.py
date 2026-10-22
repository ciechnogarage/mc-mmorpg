"""Animacje Kokoniary: kroki ośmiu nóg, przędzenie i rozwarcie kokonu."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


UDA = [f'udo_{bok}{i}' for bok in ('l', 'r') for i in range(1, 5)]
GOLENIE = [f'golen_{bok}{i}' for bok in ('l', 'r') for i in range(1, 5)]
PARY_A = {'udo_l1', 'udo_r2', 'udo_l3', 'udo_r4'}


def stan(nazwa, dl, kosci, *, petla='once', hit=None, windup=None,
         recover=None, skrypt=False):
    d = {'dl': dl, 'petla': petla, 'kosci': kosci}
    if hit is not None:
        d['hit'] = hit
    if windup is not None:
        d['windup'] = windup
    if recover is not None:
        d['recover'] = recover
    if skrypt:
        d['skrypt'] = f'mm:BAGNA_KOKONIARA_{nazwa.upper()}_KLATKA'
    return d


DANE = {
    'idle': stan('idle', 2, {
        'tulow': {'position': K((0, (0, 0, 0)), (1, (0, .5, 0)),
                                  (2, (0, 0, 0)))},
        'odwlok': {'rotation': K((0, (0, 0, 0)), (1, (-3, 0, 0)),
                                    (2, (0, 0, 0)))},
        'swiatlo_kokon': {'scale': K((0, (1, 1, 1)), (1, (1.05, 1.05, 1.05)),
                                     (2, (1, 1, 1)))},
    }, petla='loop'),
    'walk': stan('walk', 1, {
        'korzen': {'position': K((0, (0, 0, 0)), (.125, (0, .9, 0)),
                                   (.25, (0, 1.5, 0)), (.375, (0, .9, 0)),
                                   (.5, (0, 0, 0)), (.6, (0, .9, 0)),
                                   (.75, (0, 1.5, 0)), (.9, (0, .9, 0)),
                                   (1, (0, 0, 0)))},
        'tulow': {'position': K((0, (0, 0, 0)), (.25, (0, 1.5, 0)),
                                  (.5, (0, 0, 0)), (.75, (0, 1.5, 0)),
                                  (1, (0, 0, 0)))},
        'odwlok': {'rotation': K((0, (0, 0, -2)), (.5, (0, 0, 2)),
                                    (1, (0, 0, -2)))},
        **{n: {'rotation': K((0, (0, 0, 0)),
                              (.25, (0, 15 if n in PARY_A else -15, 0)),
                              (.5, (0, 0, 0)),
                              (.75, (0, -15 if n in PARY_A else 15, 0)),
                              (1, (0, 0, 0)))}
           for n in UDA},
        **{n: {'position': K((0, (0, 0, 0)),
                              (.25, (0, 1.2 if n.replace('golen_', 'udo_') in PARY_A else 0, 0)),
                              (.5, (0, 0, 0)),
                              (.75, (0, 0 if n.replace('golen_', 'udo_') in PARY_A else 1.2, 0)),
                              (1, (0, 0, 0)))} for n in GOLENIE},
    }, petla='loop'),
    'attack': stan('attack', .6, {
        'tulow': {'position': K((0, (0, 0, 0)), (.12, (0, .4, -1)),
                                   (.2, (0, .7, -3.5)), (.6, (0, 0, 0)))},
        'glowa': {'position': K((0, (0, 0, 0)), (.12, (0, 0, 1)),
                                   (.2, (0, 0, -1)), (.6, (0, 0, 0)))},
        'kly': {'rotation': K((0, (0, 0, 0)), (.12, (28, 0, 0)),
                                 (.2, (-18, 0, 0)), (.6, (0, 0, 0)))},
    }, hit=.2, recover=.4, skrypt=True),
    'spin': stan('spin', 1.4, {
        'odwlok': {'position': K((0, (0, 0, 0)), (.5, (0, -1, -1)),
                                    (1, (0, -3.5, -3)), (1.05, (0, -3.5, -3)),
                                    (1.4, (0, 0, 0))),
                   'rotation': K((0, (0, 0, 0)), (1, (60, 0, 0)),
                                  (1.4, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1, (-9, 0, 0)),
                                   (1.4, (0, 0, 0)))},
        'fx_kokon': {'scale': K((0, (0, 0, 0)), (.9, (0, 0, 0)),
                                (1, (.5, .5, .5)), (1.05, (1, 1, 1)),
                                (1.12, (1, 1, 1)),
                                (1.14, (0, 0, 0)), (1.4, (0, 0, 0))),
                     'position': K((0, (0, 0, 0)), (1, (0, 0, -4)),
                                   (1.05, (0, 0, -5)),
                                   (1.12, (0, 0, -8)), (1.4, (0, 0, 0)))},
    }, windup=1, hit=1.05, recover=.35, skrypt=True),
    'leap_prep': stan('leap_prep', .8, {
        'tulow': {'scale': K((0, (1, 1, 1)), (.8, (1.08, .55, 1.08))),
                  'position': K((0, (0, 0, 0)), (.8, (0, -3.3, 0)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (.8, (0, 8 if n in PARY_A else -8, 0)))}
           for n in UDA},
        **{n: {'rotation': K((0, (0, 0, 0)), (.8, (12, 0, 0)))}
           for n in GOLENIE},
    }, petla='hold', windup=.8, hit=.8),
    'leap': stan('leap', .7, {
        'korzen': {'position': K((0, (0, 0, 0)), (.2, (0, 2.5, 0)),
                                   (.4, (0, 5, -2)), (.7, (0, 1, -3)))},
        'tulow': {'scale': K((0, (1.08, .55, 1.08)), (.2, (1, 1, 1)),
                              (.7, (1, 1, 1))),
                  'position': K((0, (0, -3.3, 0)), (.2, (0, 0, 0)),
                                    (.7, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.4, (-15, 0, 0)),
                                   (.7, (10, 0, 0)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (.4, (-10, 0, 0)),
                              (.7, (12, 0, 0)))} for n in UDA},
    }, hit=.4, recover=.3, skrypt=True),
    'hatch': stan('hatch', 1.5, {
        'swiatlo_kokon': {'scale': K((0, (1, 1, 1)), (.6, (1.2, 1.2, 1.2)),
                                     (.9, (1.45, 1.45, 1.45)),
                                     (1.5, (1, 1, 1)))},
        'odwlok': {'scale': K((0, (1, 1, 1)), (.9, (1.15, 1.08, 1.15)),
                              (1.5, (1, 1, 1))),
                   'rotation': K((0, (0, 0, 0)), (.9, (-12, 0, 0)),
                                  (1.5, (0, 0, 0)))},
        'tulow': {'position': K((0, (0, 0, 0)), (.9, (0, .5, 0)),
                                   (1.5, (0, 0, 0)))},
    }, hit=.9, recover=.6, skrypt=True),
    'hurt': stan('hurt', .3, {
        'tulow': {'position': K((0, (0, 0, 0)), (.1, (0, .6, 0)),
                                   (.3, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.1, (14, 0, 0)),
                                   (.3, (0, 0, 0)))},
    }),
    'death': stan('death', 1.5, {
        'korzen': {'position': K((0, (0, 0, 0)), (.5, (0, .25, 0)),
                                   (1.5, (0, .25, 0)))},
        'tulow': {'scale': K((0, (1, 1, 1)), (1.5, (1.1, .67, 1.1)))},
        'odwlok': {'rotation': K((0, (0, 0, 0)), (1.5, (18, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.5, (26, 0, 0)))},
        'swiatlo_kokon': {'scale': K((0, (1, 1, 1)), (1.5, (.55, .55, .55)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (1.5, (0, 20, 0)))} for n in UDA},
    }),
    'spawn': stan('spawn', 1.5, {
        'tulow': {'scale': K((0, (1, .2, 1)), (.9, (1, 1, 1)),
                              (1.5, (1, 1, 1)))},
        'swiatlo_kokon': {'scale': K((0, (.5, .5, .5)), (1.5, (1, 1, 1)))},
        **{n: {'rotation': K((0, (0, 0, 0)), (1.5, (0, 0, 0)))} for n in UDA},
    }),
}

# Geometria pocisku ma być niewidoczna poza krótką fazą spin.
for nazwa, animacja in DANE.items():
    if nazwa != 'spin':
        animacja['kosci']['fx_kokon'] = {'scale': K((0, (0, 0, 0)),
                                                   (animacja['dl'], (0, 0, 0)))}

# Głęboki przysiad, rozpostarcie ośmiu nóg i mocny impuls ciężkiego odwłoka.
DANE['leap_prep']['kosci']['tulow']['scale'] = K(
    (0, (1, 1, 1)), (.8, (1.16, .45, 1.16)))
DANE['leap_prep']['kosci']['tulow']['position'] = K(
    (0, (0, 0, 0)), (.8, (0, -3.8, 0)))
DANE['leap_prep']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.2, (0, 1.05, 0)),
    (.4, (0, 1.6, 0)),
    (.8, (0, 1.6, 0)))}
for n in UDA:
    znak = 1 if n.startswith('udo_l') else -1
    DANE['leap_prep']['kosci'][n]['rotation'] = K(
        (0, (0, 0, 0)), (.8, (0, znak * 22, 0)))
    DANE['leap']['kosci'][n]['rotation'] = K(
        (0, (0, znak * 22, 0)), (.4, (-14, znak * 30, 0)),
        (.7, (14, znak * 26, 0)))
for n in GOLENIE:
    DANE['leap_prep']['kosci'][n]['rotation'] = K(
        (0, (0, 0, 0)), (.8, (22, 0, 0)))
    DANE['leap']['kosci'][n] = {'rotation': K(
        (0, (22, 0, 0)), (.4, (-12, 0, 0)),
        (.7, (18, 0, 0)))}
DANE['leap']['kosci']['tulow']['scale'] = K(
    (0, (1.16, .45, 1.16)), (.2, (1, 1.08, 1)),
    (.4, (1, 1.12, 1)), (.7, (1.08, .85, 1.08)))
DANE['leap']['kosci']['tulow']['position'] = K(
    (0, (0, -3.8, 0)), (.2, (0, 0, 0)),
    (.7, (0, 0, 0)))
DANE['leap']['kosci']['korzen']['position'] = K(
    (0, (0, 1.6, 0)), (.2, (0, 7.3, 0)),
    (.4, (0, 10.6, -2)), (.7, (0, 3, -3)))
DANE['hatch']['kosci']['odwlok']['scale'] = K(
    (0, (1, 1, 1)), (.65, (1.18, 1.08, 1.18)),
    (.9, (1.32, 1.15, 1.32)),
    (1.02, (.9, .88, .9)), (1.5, (1, 1, 1)))
DANE['hatch']['kosci']['odwlok']['rotation'] = K(
    (0, (0, 0, 0)), (.65, (-8, 0, -10)),
    (.9, (-18, 0, 15)), (1.02, (16, 0, -8)),
    (1.5, (0, 0, 0)))



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
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/kokoniara/bagna_kokoniara.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
