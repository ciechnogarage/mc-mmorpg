"""Animacje Ognika: latający rdzeń, krążące iskry i rozbłysk."""
from pathlib import Path
from anim_bagna import zastosuj


def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


DANE = {
    'idle': {'dl': 2.0, 'petla': 'loop', 'kosci': {
        'plomien': {'position': K((0, (0, 0, 0)), (1, (0, .8, 0)),
                                    (2, (0, 0, 0)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (1, (0, 180, 0)),
                                  (2, (0, 360, 0)))},
        'jezyk_l': {'rotation': K((0, (0, 0, 0)), (1, (0, 0, 6)),
                                     (2, (0, 0, 0)))},
        'jezyk_r': {'rotation': K((0, (0, 0, 0)), (1, (0, 0, -6)),
                                     (2, (0, 0, 0)))},
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)), (1, (1.07, 1.07, 1.07)),
                                     (2, (1, 1, 1)))},
    }},
    'walk': {'dl': 1.0, 'petla': 'loop', 'kosci': {
        'plomien': {'position': K((0, (0, 0, 0)), (.25, (0, 1.2, 0)),
                                    (.5, (0, 0, 0)), (.75, (0, .8, 0)),
                                    (1, (0, 0, 0))),
                    'rotation': K((0, (0, 0, -5)), (.5, (0, 0, 5)),
                                   (1, (0, 0, -5)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (1, (0, 360, 0)))},
        'jezyk_l': {'rotation': K((0, (0, 0, 0)), (.5, (-8, 0, 9)),
                                     (1, (0, 0, 0)))},
        'jezyk_r': {'rotation': K((0, (0, 0, 0)), (.5, (8, 0, -9)),
                                     (1, (0, 0, 0)))},
    }},
    'attack': {'dl': .5, 'petla': 'once', 'hit': .2, 'recover': .3,
               'skrypt': 'mm:BAGNA_OGNIK_ATTACK_KLATKA', 'kosci': {
        'plomien': {'position': K((0, (0, 0, 0)), (.14, (0, .4, 1.5)),
                                    (.2, (0, .8, -2.5)), (.5, (0, 0, 0))),
                    'rotation': K((0, (0, 0, 0)), (.14, (12, 0, 0)),
                                   (.2, (-16, 0, 0)), (.5, (0, 0, 0)))},
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)), (.2, (1.2, 1.2, 1.2)),
                                     (.5, (1, 1, 1)))},
    }},
    'flee': {'dl': .6, 'petla': 'once', 'kosci': {
        'plomien': {'scale': K((0, (1, 1, 1)), (.15, (.8, .85, 1.45)),
                                 (.45, (.8, .85, 1.45)), (.6, (1, 1, 1))),
                    'rotation': K((0, (0, 0, 0)), (.15, (-16, 0, 0)),
                                   (.6, (0, 0, 0))),
                    'position': K((0, (0, 0, 0)), (.2, (0, 1.5, -1)),
                                    (.6, (0, 0, 0)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (.6, (0, 300, 0)))},
    }},
    'charge': {'dl': 1.0, 'petla': 'once', 'windup': 1.0, 'hit': 1.0,
               'kosci': {
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)), (.4, (1.15, 1.15, 1.15)),
                                     (.7, (1.35, 1.35, 1.35)),
                                     (1, (1.5, 1.5, 1.5)))},
        'plomien': {'rotation': K((0, (0, 0, 0)), (.35, (0, 0, 5)),
                                   (.5, (0, 0, -7)), (.65, (0, 0, 8)),
                                   (.8, (0, 0, -9)), (1, (0, 0, 0)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (1, (0, 540, 0)))},
    }},
    'explode': {'dl': .6, 'petla': 'once', 'hit': .1, 'recover': .5,
                'skrypt': 'mm:BAGNA_OGNIK_EXPLODE_KLATKA', 'kosci': {
        'plomien': {'scale': K((0, (1, 1, 1)), (.1, (1.3, 1.3, 1.3)),
                                 (.25, (.8, .8, .8)), (.6, (0, 0, 0)))},
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)),
                                     (.6, (1, 1, 1)))},
        'iskry': {'scale': K((0, (1, 1, 1)), (.1, (1.8, 1.8, 1.8)),
                               (.6, (0, 0, 0))),
                  'rotation': K((0, (0, 0, 0)), (.6, (0, 450, 0)))},
    }},
    'blink': {'dl': .4, 'petla': 'once', 'hit': .2, 'recover': .2,
              'skrypt': 'mm:BAGNA_OGNIK_BLINK_KLATKA', 'kosci': {
        'korzen': {'scale': K((0, (1, 1, 1)), (.18, (0, 0, 0)),
                              (.22, (0, 0, 0)), (.4, (1, 1, 1)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (.4, (0, 180, 0)))},
    }},
    'hurt': {'dl': .3, 'petla': 'once', 'kosci': {
        'plomien': {'rotation': K((0, (0, 0, 0)), (.1, (0, 0, 16)),
                                   (.3, (0, 0, 0)))},
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)), (.1, (.72, .72, .72)),
                                     (.3, (1, 1, 1)))},
    }},
    'death': {'dl': 1.0, 'petla': 'once', 'kosci': {
        'plomien': {'scale': K((0, (1, 1, 1)), (.55, (.65, .65, .65)),
                                 (1, (0, 0, 0))),
                    'position': K((0, (0, 0, 0)), (1, (0, -3, 0)))},
        'swiatlo_rdzen': {'scale': K((0, (1, 1, 1)), (1, (1, 1, 1)))},
        'iskry': {'scale': K((0, (1, 1, 1)), (1, (0, 0, 0)))},
    }},
    'spawn': {'dl': .8, 'petla': 'once', 'kosci': {
        'korzen': {'scale': K((0, (0, 0, 0)), (.55, (1.12, 1.12, 1.12)),
                              (.8, (1, 1, 1)))},
        'iskry': {'rotation': K((0, (0, 0, 0)), (.8, (0, 270, 0)))},
    }},
}

# Cała sylwetka wznosi się podczas ładowania, języki podążają z opóźnieniem.
DANE['walk']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.25, (0, 2.2, 0)),
    (.5, (0, 0, 0)), (.75, (0, 1.8, 0)),
    (1, (0, 0, 0)))}
DANE['charge']['kosci']['korzen'] = {'position': K(
    (0, (0, 0, 0)), (.25, (0, -1, 0)),
    (.55, (0, 1, 0)), (.8, (0, 2.8, 0)),
    (1, (0, 3.5, 0)))}
DANE['charge']['kosci']['plomien']['scale'] = K(
    (0, (1, 1, 1)), (.3, (1.25, .8, 1.2)),
    (.7, (.92, 1.25, .92)), (1, (1.3, 1.48, 1.3)))
DANE['charge']['kosci']['swiatlo_rdzen']['scale'] = K(
    (0, (1, 1, 1)), (.3, (.8, .8, .8)),
    (.7, (1.3, 1.3, 1.3)), (1, (1.7, 1.7, 1.7)))
for n, znak in (('jezyk_l', 1), ('jezyk_r', -1)):
    DANE['walk']['kosci'][n]['rotation'] = K(
        (0, (0, 0, 0)), (.32, (12, 0, 18 * znak)),
        (.58, (-8, 0, -8 * znak)),
        (.82, (14, 0, 20 * znak)), (1, (0, 0, 0)))
    DANE['charge']['kosci'][n] = {'rotation': K(
        (0, (0, 0, 0)), (.35, (8, 0, 12 * znak)),
        (.7, (-12, 0, -18 * znak)),
        (1, (-20, 0, -28 * znak)))}
DANE['explode']['kosci']['swiatlo_rdzen']['scale'] = K(
    (0, (1.7, 1.7, 1.7)), (.1, (2.4, 2.4, 2.4)),
    (.2, (1.4, 1.4, 1.4)), (.6, (0, 0, 0)))
DANE['explode']['kosci']['plomien']['scale'] = K(
    (0, (1.3, 1.48, 1.3)), (.1, (1.9, 1.9, 1.9)),
    (.2, (.9, .9, .9)), (.6, (0, 0, 0)))
DANE['explode']['kosci']['korzen'] = {'position': K(
    (0, (0, 3.5, 0)), (.1, (0, 6, 0)),
    (.2, (0, 3, 0)), (.6, (0, 0, 0)))}


def main(model=None):
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/ognik/bagna_ognik.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
