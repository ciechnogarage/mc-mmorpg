"""Animacje Króla Ropuch na szkielecie CLAUDE; wszystkie czasy z kontraktu."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


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
        d['skrypt'] = f'mm:BAGNA_KROL_ROPUCH_{nazwa.upper()}_KLATKA'
    return d


DANE = {
    'idle': stan('idle', 3, {
        'tulow': {'scale': K((0, (1, 1, 1)), (1.5, (1.025, 1, 1.025)),
                              (3, (1, 1, 1)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (1.5, (1.07, 1.07, 1.07)),
                                    (3, (1, 1, 1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.5, (-2, 0, 0)),
                                   (3, (0, 0, 0)))},
    }, petla='loop'),
    'walk': stan('walk', 1.6, {
        'korzen': {'position': K((0, (0, 0, 0)), (.4, (0, 3, 0)),
                                   (.8, (0, 0, 0)), (1.2, (0, 3, 0)),
                                   (1.6, (0, 0, 0)))},
        'udo_l': {'rotation': K((0, (0, 0, 0)), (.4, (-12, 0, 0)),
                                   (.8, (0, 0, 0)), (1.2, (8, 0, 0)),
                                   (1.6, (0, 0, 0)))},
        'udo_r': {'rotation': K((0, (0, 0, 0)), (.4, (8, 0, 0)),
                                   (.8, (0, 0, 0)), (1.2, (-12, 0, 0)),
                                   (1.6, (0, 0, 0)))},
        'lapa_l': {'rotation': K((0, (0, 0, 0)), (.8, (0, 7, 0)),
                                    (1.6, (0, 0, 0)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (.8, (0, -7, 0)),
                                    (1.6, (0, 0, 0)))},
    }, petla='loop'),
    'attack': stan('attack', 1, {
        'tulow': {'rotation': K((0, (0, 0, 0)), (.25, (0, 15, 0)),
                                   (.35, (0, -18, 0)), (1, (0, 0, 0)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (.25, (0, 20, 0)),
                                    (.35, (0, -28, 0)), (1, (0, 0, 0)))},
        'berlo': {'rotation': K((0, (0, 0, 0)), (.25, (0, 20, 0)),
                                   (.35, (0, -20, 0)), (1, (0, 0, 0)))},
    }, hit=.35, recover=.65, skrypt=True),
    'swallow_prep': stan('swallow_prep', 1.2, {
        'szczeka': {'rotation': K((0, (0, 0, 0)), (.3, (12, 0, 0)),
                                     (.8, (30, 0, 0)), (1.2, (42, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1.2, (-9, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (1.2, (1.24, 1.24, 1.24)))},
        'lapa_l': {'rotation': K((0, (0, 0, 0)), (1.2, (0, -16, 0)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (1.2, (0, 16, 0)))},
        'fx_jezyk': {'scale': K((0, (1, 1, 0)), (.8, (1, 1, 0)),
                                  (1.2, (1, 1, .4)))},
    }, petla='hold', windup=1.2, hit=1.2),
    'swallow': stan('swallow', .6, {
        'szczeka': {'rotation': K((0, (42, 0, 0)), (.14, (38, 0, 0)),
                                     (.2, (-4, 0, 0)), (.6, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (-9, 0, 0)), (.2, (10, 0, 0)),
                                   (.6, (0, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1.24, 1.24, 1.24)),
                                    (.2, (.85, .85, .85)), (.6, (1, 1, 1)))},
        'fx_jezyk': {'scale': K((0, (1, 1, .4)), (.14, (1, 1, .25)),
                                  (.2, (1, 1, 0)), (.6, (0, 0, 0)))},
    }, hit=.2, recover=.4, skrypt=True),
    'spit': stan('spit', .8, {
        'szczeka': {'rotation': K((0, (0, 0, 0)), (.25, (22, 0, 0)),
                                     (.35, (34, 0, 0)), (.8, (0, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (.25, (1.3, 1.25, 1.35)),
                                    (.35, (.75, .75, .75)), (.8, (1, 1, 1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.35, (-12, 0, 0)),
                                   (.8, (0, 0, 0)))},
    }, hit=.35, recover=.45, skrypt=True),
    'tongue': stan('tongue', 1.6, {
        'szczeka': {'rotation': K((0, (0, 0, 0)), (.6, (12, 0, 0)),
                                     (1, (22, 0, 0)), (1.15, (33, 0, 0)),
                                     (1.6, (0, 0, 0))),
                    'position': K((0, (0, 0, 0)), (1, (0, 0, 0)),
                                    (1.15, (0, 0, -4)), (1.6, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1, (-10, 0, 0)),
                                   (1.15, (-18, 0, 0)), (1.6, (0, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (1, (1.25, 1.25, 1.25)),
                                    (1.6, (1, 1, 1)))},
        'fx_jezyk': {'scale': K((0, (1, 1, 0)), (1, (1, 1, 0)),
                                  (1.15, (1, 1, 1)), (1.3, (1, 1, 1)),
                                  (1.6, (0, 0, 0)))},
    }, windup=1, hit=1.15, recover=.45, skrypt=True),
    'leap_prep': stan('leap_prep', 1, {
        'tulow': {'scale': K((0, (1, 1, 1)), (1, (1.06, .82, 1.06))),
                  'rotation': K((0, (0, 0, 0)), (1, (9, 0, 0)))},
        'udo_l': {'rotation': K((0, (0, 0, 0)), (1, (0, -8, 0)))},
        'udo_r': {'rotation': K((0, (0, 0, 0)), (1, (0, 8, 0)))},
        'lapa_l': {'rotation': K((0, (0, 0, 0)), (1, (0, -12, 0))),
                   'position': K((0, (0, 0, 0)), (1, (0, 1.65, 0)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (1, (0, 12, 0))),
                   'position': K((0, (0, 0, 0)), (1, (0, 1.65, 0)))},
    }, petla='hold', windup=1, hit=1),
    'leap': stan('leap', .8, {
        'korzen': {'position': K((0, (0, 0, 0)), (.2, (0, 3, 0)),
                                   (.5, (0, 7, -3)), (.6, (0, 6, -4)),
                                   (.8, (0, 4, -4)))},
        'tulow': {'scale': K((0, (1.06, .82, 1.06)), (.2, (1, 1, 1)),
                              (.8, (1, 1, 1))),
                  'rotation': K((0, (9, 0, 0)), (.5, (-12, 0, 0)),
                                 (.8, (8, 0, 0)))},
        'udo_l': {'rotation': K((0, (0, -8, 0)), (.5, (-14, 0, 0)),
                                   (.8, (14, 0, 0)))},
        'udo_r': {'rotation': K((0, (0, 8, 0)), (.5, (-14, 0, 0)),
                                   (.8, (14, 0, 0)))},
        'lapa_l': {'position': K((0, (0, 1.65, 0)), (.2, (0, 0, 0)),
                                  (.8, (0, 0, 0)))},
        'lapa_r': {'position': K((0, (0, 1.65, 0)), (.2, (0, 0, 0)),
                                  (.8, (0, 0, 0)))},
    }, hit=.6),
    'land': stan('land', .8, {
        'korzen': {'position': K((0, (0, 4, -4)), (.05, (0, .2, -4)),
                                   (.35, (0, 0, -2)), (.8, (0, 0, 0)))},
        'tulow': {'scale': K((0, (1, 1, 1)), (.05, (1.12, .78, 1.1)),
                              (.45, (1, 1, 1)), (.8, (1, 1, 1))),
                  'rotation': K((0, (8, 0, 0)), (.05, (0, 0, 0)),
                                 (.8, (0, 0, 0)))},
        'udo_l': {'rotation': K((0, (14, 0, 0)), (.05, (0, 0, 0)),
                                   (.8, (0, 0, 0)))},
        'udo_r': {'rotation': K((0, (14, 0, 0)), (.05, (0, 0, 0)),
                                   (.8, (0, 0, 0)))},
        'mul': {'scale': K((0, (1, 1.5, 1)), (.2, (1, 1.5, 1)),
                            (.8, (1, 1, 1)))},
    }, hit=.05, recover=.75, skrypt=True),
    'summon': stan('summon', 1.6, {
        'korzen': {'position': K((0, (0, 0, 0)), (.2, (0, 1.2, 0)),
                                   (.4, (0, 1.5, 0)),
                                   (1, (0, 1.5, 0)), (1.4, (0, 1.3, 0)),
                                   (1.5, (0, .8, 0)), (1.6, (0, 0, 0)))},
        'lapa_l': {'rotation': K((0, (0, 0, 0)), (.55, (-18, 0, -25)),
                                    (1, (-25, 0, -36)), (1.6, (0, 0, 0)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (.55, (-18, 0, 25)),
                                    (1, (-25, 0, 36)), (1.6, (0, 0, 0)))},
        'swiatlo_brodawki': {'scale': K((0, (1, 1, 1)), (1, (1.02, 1.02, 1.02)),
                                        (1.6, (1, 1, 1)))},
        'swiatlo_korona': {'scale': K((0, (1, 1, 1)), (1, (1.03, 1.03, 1.03)),
                                      (1.6, (1, 1, 1)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (1, (-12, 0, 0)),
                                   (1.6, (0, 0, 0)))},
        'fx_korona': {'scale': K((0, (0, 0, 0)), (.4, (0, 0, 0)),
                                   (1, (1.3, 1.3, 1.3)),
                                   (1.25, (1.3, 1.3, 1.3)),
                                   (1.6, (0, 0, 0)))},
    }, hit=1, recover=.6, skrypt=True),
    'hurt': stan('hurt', .35, {
        'tulow': {'rotation': K((0, (0, 0, 0)), (.12, (0, 8, 0)),
                                   (.35, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (.12, (10, 0, 0)),
                                   (.35, (0, 0, 0)))},
    }),
    'death': stan('death', 2, {
        'korzen': {'position': K((0, (0, 0, 0)), (.4, (0, .35, 0)),
                                   (2, (0, .35, 0)))},
        'tulow': {'scale': K((0, (1, 1, 1)), (1.2, (1.08, .82, 1.08)),
                              (2, (1.16, .8, 1.15)))},
        'glowa': {'rotation': K((0, (0, 0, 0)), (2, (25, 0, 12)))},
        'szczeka': {'rotation': K((0, (0, 0, 0)), (2, (15, 0, 0)))},
        'lapa_l': {'rotation': K((0, (0, 0, 0)), (2, (28, 0, -18)))},
        'lapa_r': {'rotation': K((0, (0, 0, 0)), (2, (28, 0, 18)))},
        'swiatlo_worek': {'scale': K((0, (1, 1, 1)), (2, (.5, .5, .5)))},
        'mul': {'scale': K((0, (1, 1, 1)), (.7, (1.08, 1.5, 1.08)),
                            (2, (1.22, 1.8, 1.22)))},
    }),
    'spawn': stan('spawn', 2, {
        'korzen': {'position': K((0, (0, -10, 0)), (1.25, (0, 0, 0)),
                                   (2, (0, 0, 0)))},
        'glowa': {'rotation': K((0, (18, 0, 0)), (2, (0, 0, 0)))},
        'swiatlo_worek': {'scale': K((0, (.55, .55, .55)), (2, (1, 1, 1)))},
        'mul': {'scale': K((0, (.8, 1, .8)), (1, (1.2, 1, 1.2)),
                             (2, (1, 1, 1)))},
    }),
}


# Rzęsa jest osobną kością bryły: podąża za głową, aby nie odrywać się
# podczas pierwszych klatek szerokiego otwarcia pyska i przywołania.
for nazwa in ('summon', 'swallow_prep', 'tongue'):
    DANE[nazwa]['kosci']['rzesa'] = {
        'rotation': DANE[nazwa]['kosci']['glowa']['rotation'],
    }
for nazwa in ('summon', 'tongue'):
    DANE[nazwa]['kosci']['rzesa']['position'] = K(
        (0, (0, 0, -.35)), (DANE[nazwa]['dl'], (0, 0, -.35)))
DANE['swallow_prep']['kosci']['rzesa']['position'] = K(
    (0, (0, 0, 0)), (.1, (0, 0, -.25)), (.3, (0, 0, -.25)),
    (1.2, (0, 0, 0)))

# ME pokazuje kość bez kanału skali w pełnym rozmiarze. Każdy stan musi
# zatem jawnie ukryć oba efekty poza ich oknem emisji.
for nazwa, animacja in DANE.items():
    kosci = animacja['kosci']
    for efekt in ('fx_jezyk', 'fx_korona'):
        kosci.setdefault(efekt, {'scale': K((0, (0, 0, 0)),
                                             (animacja['dl'], (0, 0, 0)))})
    rzesa = kosci.setdefault('rzesa', {})
    pozycje = rzesa.get('position', K((0, (0, 0, 0)),
                                      (animacja['dl'], (0, 0, 0))))
    rzesa['position'] = [(t, (xyz[0], xyz[1] - .3, xyz[2]), mode)
                         for t, xyz, mode in pozycje]

# Ciężar zbiera się w szerokim przysiadzie, a gardziel i berło reagują po lądowaniu.
DANE['leap_prep']['kosci']['tulow']['scale'] = K(
    (0, (1, 1, 1)), (.5, (1.1, .8, 1.1)),
    (1, (1.16, .7, 1.16)))
DANE['leap_prep']['kosci']['tulow']['rotation'] = K(
    (0, (0, 0, 0)), (.5, (6, 0, 0)),
    (1, (13, 0, 0)))
DANE['leap_prep']['kosci']['mul'] = {'position': K(
    (0, (0, 0, 0)), (.6, (0, 0, 0)),
    (.7, (0, .5, 0)), (1, (0, .5, 0)))}
DANE['leap']['kosci']['mul'] = {'position': K(
    (0, (0, .5, 0)), (.2, (0, 0, 0)),
    (.8, (0, 0, 0)))}
for n, znak in (('udo_l', -1), ('udo_r', 1)):
    DANE['leap_prep']['kosci'][n]['rotation'] = K(
        (0, (0, 0, 0)), (.5, (0, znak * 12, 0)),
        (1, (0, znak * 24, 0)))
    DANE['leap']['kosci'][n]['rotation'] = K(
        (0, (0, znak * 24, 0)), (.5, (-18, znak * 18, 0)),
        (.8, (18, znak * 25, 0)))
    DANE['land']['kosci'][n]['rotation'] = K(
        (0, (18, znak * 25, 0)), (.05, (8, znak * 32, 0)),
        (.35, (0, znak * 16, 0)), (.8, (0, 0, 0)))
for n in ('lapa_l', 'lapa_r'):
    DANE['leap_prep']['kosci'][n]['position'] = K(
        (0, (0, 0, 0)), (.65, (0, 1.65, 0)),
        (1, (0, 2.25, 0)))
    DANE['leap']['kosci'][n]['position'] = K(
        (0, (0, 2.25, 0)), (.2, (0, 0, 0)),
        (.8, (0, 0, 0)))
DANE['leap']['kosci']['korzen']['position'] = K(
    (0, (0, 0, 0)), (.2, (0, 4, 0)),
    (.5, (0, 10, -4)), (.6, (0, 9, -5)),
    (.8, (0, 4, -5)))
DANE['land']['kosci']['swiatlo_worek'] = {'scale': K(
    (0, (1, 1, 1)), (.05, (.82, .82, .82)),
    (.22, (1.18, 1.18, 1.18)),
    (.48, (.95, .95, .95)), (.8, (1, 1, 1)))}
DANE['land']['kosci']['berlo'] = {'rotation': K(
    (0, (0, 0, 0)), (.05, (0, 0, -9)),
    (.24, (0, 0, 8)), (.48, (0, 0, -3.5)),
    (.8, (0, 0, 0)))}


def main(model=None):
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/krol_ropuch/bagna_krol_ropuch.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
