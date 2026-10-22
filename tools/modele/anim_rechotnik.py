"""Animacje rechotnik; uruchom po eksporcie geometrii. Nie zmienia kostek ani atlasu."""
from pathlib import Path
from anim_bagna import zastosuj

DANE = {'idle': {'dl': 2,
          'petla': 'loop',
          'kosci': {'worek': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                        (1, (1.045, 1.08, 1.08), 'linear'),
                                        (2, (1.0, 1.0, 1.0), 'linear')]},
                    'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (1, (-2.0, 0.0, 0.0), 'linear'),
                                           (2, (0.0, 0.0, 0.0), 'linear')]},
                    'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'), (2, (0.0, 0.0, 0.0), 'linear')]}}},
 'walk': {'dl': 0.9,
          'petla': 'loop',
          'kosci': {'korzen': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.225, (0.0, 0.75, 0.0), 'linear'),
                                            (0.45, (0.0, 0.0, 0.0), 'linear'),
                                            (0.675, (0.0, 0.75, 0.0), 'linear'),
                                            (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'tulow': {'rotation': [(0, (0.0, 0.0, -3.0), 'linear'),
                                           (0.45, (0.0, 0.0, 3.0), 'linear'),
                                           (0.9, (0.0, 0.0, -3.0), 'linear')]},
                    'ramie_l': {'rotation': [(0, (8.0, 0.0, 0.0), 'linear'),
                                             (0.45, (-8.0, 0.0, 0.0), 'linear'),
                                             (0.9, (8.0, 0.0, 0.0), 'linear')]},
                    'ramie_r': {'rotation': [(0, (-8.0, 0.0, 0.0), 'linear'),
                                             (0.45, (8.0, 0.0, 0.0), 'linear'),
                                             (0.9, (-8.0, 0.0, 0.0), 'linear')]},
                    'wlocznia': {'position': [(0, (0.0, 0.25, 0.0), 'linear'),
                                              (0.9, (0.0, 0.25, 0.0), 'linear')]},
                    'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.9, (0.0, 0.0, 0.0), 'linear')]}}},
 'attack': {'dl': 0.7,
            'petla': 'once',
            'kosci': {'tulow': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.16, (10.0, 0.0, -9.0), 'linear'),
                                             (0.25, (-12.0, 0.0, 11.0), 'linear'),
                                             (0.4, (-7.0, 0.0, 5.0), 'linear'),
                                             (0.7, (0.0, 0.0, 0.0), 'linear')]},
                      'ramie_r': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (0.16, (32.0, -8.0, 18.0), 'linear'),
                                               (0.25, (-32.0, 12.0, -22.0), 'linear'),
                                               (0.4, (-19.0, 7.0, -12.0), 'linear'),
                                               (0.7, (0.0, 0.0, 0.0), 'linear')]},
                      'przedramie_r': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                                    (0.16, (20.0, 0.0, 0.0), 'linear'),
                                                    (0.25, (-25.0, 0.0, 0.0), 'linear'),
                                                    (0.7, (0.0, 0.0, 0.0), 'linear')]},
                      'wlocznia': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                                (0.16, (0.0, 0.0, 13.0), 'linear'),
                                                (0.25, (0.0, 0.0, -10.0), 'linear'),
                                                (0.7, (0.0, 0.0, 0.0), 'linear')],
                                   'position': [(0, (0.0, 1.25, 0.0), 'linear'),
                                                (0.16, (0.0, 1.7, 0.0), 'linear'),
                                                (0.25, (0.0, 1.7, 0.0), 'linear'),
                                                (0.4, (0.0, 1.0, 0.0), 'linear'),
                                                (0.7, (0.0, 0.0, 0.0), 'linear')]},
                      'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.7, (0.0, 0.0, 0.0), 'linear')]}},
            'hit': 0.25,
            'recover': 0.45,
            'skrypt': 'mm:BAGNA_RECHOTNIK_ATTACK_KLATKA'},
 'croak': {'dl': 1,
           'petla': 'once',
           'kosci': {'worek': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                         (0.3, (1.12, 1.17, 1.18), 'linear'),
                                         (0.72, (1.38, 1.42, 1.42), 'linear'),
                                         (1, (1.48, 1.47, 1.5), 'linear')]},
                     'tulow': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.6, (12.0, 0.0, 0.0), 'linear'),
                                            (1, (15.0, 0.0, 0.0), 'linear')]},
                     'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.7, (-9.0, 0.0, 0.0), 'linear'),
                                            (1, (-12.0, 0.0, 0.0), 'linear')]},
                     'wlocznia': {'position': [(0, (0.0, 0.1, 0.0), 'linear'),
                                               (0.4, (0.0, 0.8, 0.0), 'linear'),
                                               (1, (0.0, 0.8, 0.0), 'linear')]},
                     'ramie_l': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1, (12.0, 0.0, -19.0), 'linear')]},
                     'ramie_r': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1, (12.0, 0.0, 19.0), 'linear')]},
                     'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'), (1, (0.0, 0.0, 0.0), 'linear')]}},
           'windup': 1,
           'hit': 1},
 'tongue': {'dl': 0.6,
            'petla': 'once',
            'kosci': {'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.08, (10.0, 0.0, 0.0), 'linear'),
                                             (0.15, (-16.0, 0.0, 0.0), 'linear'),
                                             (0.37, (-8.0, 0.0, 0.0), 'linear'),
                                             (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'szczeka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (0.1, (15.0, 0.0, 0.0), 'linear'),
                                               (0.15, (-24.0, 0.0, 0.0), 'linear'),
                                               (0.38, (-18.0, 0.0, 0.0), 'linear'),
                                               (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'worek': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                          (0.15, (1.2, 1.19, 1.2), 'linear'),
                                          (0.6, (1.0, 1.0, 1.0), 'linear')]},
                      'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.12, (0.0, 0.0, 0.0), 'linear'),
                                             (0.14, (1.0, 1.0, 1.0), 'linear'),
                                             (0.36, (1.0, 1.0, 1.0), 'linear'),
                                             (0.38, (0.0, 0.0, 0.0), 'linear'),
                                             (0.6, (0.0, 0.0, 0.0), 'linear')],
                                   'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                                (0.15, (24.0, 0.0, 0.0), 'linear'),
                                                (0.36, (18.0, 0.0, 0.0), 'linear'),
                                                (0.6, (0.0, 0.0, 0.0), 'linear')]}},
            'hit': 0.15,
            'recover': 0.45,
            'skrypt': 'mm:BAGNA_RECHOTNIK_TONGUE_KLATKA'},
 'slam': {'dl': 0.9,
          'petla': 'once',
          'kosci': {'korzen': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.35, (0.0, 1.4, 0.0), 'linear'),
                                            (0.55, (0.0, 0.0, 0.0), 'linear'),
                                            (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'tulow': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.36, (16.0, 0.0, 0.0), 'linear'),
                                           (0.55, (-24.0, 0.0, 0.0), 'linear'),
                                           (0.71, (-16.0, 0.0, 0.0), 'linear'),
                                           (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'ramie_l': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.38, (22.0, 0.0, -12.0), 'linear'),
                                             (0.55, (-32.0, 0.0, 12.0), 'linear'),
                                             (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'ramie_r': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.38, (25.0, 0.0, 15.0), 'linear'),
                                             (0.55, (-39.0, 0.0, -15.0), 'linear'),
                                             (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'worek': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                        (0.39, (1.2, 1.26, 1.28), 'linear'),
                                        (0.55, (1.02, 0.94, 1.02), 'linear'),
                                        (0.9, (1.0, 1.0, 1.0), 'linear')]},
                    'wlocznia': {'position': [(0, (0.0, 0.2, 0.0), 'linear'),
                                              (0.2, (0.0, 0.5, 0.0), 'linear'),
                                              (0.6, (0.0, 0.5, 0.0), 'linear'),
                                              (0.9, (0.0, 0.0, 0.0), 'linear')]},
                    'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.9, (0.0, 0.0, 0.0), 'linear')]}},
          'windup': 0.4,
          'hit': 0.55,
          'recover': 0.35,
          'skrypt': 'mm:BAGNA_RECHOTNIK_SLAM_KLATKA'},
 'hurt': {'dl': 0.3,
          'petla': 'once',
          'kosci': {'tulow': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.1, (14.0, 0.0, 7.0), 'linear'),
                                           (0.3, (0.0, 0.0, 0.0), 'linear')]},
                    'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.1, (10.0, 0.0, 0.0), 'linear'),
                                           (0.3, (0.0, 0.0, 0.0), 'linear')]},
                    'wlocznia': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (0.1, (0.0, 0.4, 0.0), 'linear'),
                                              (0.3, (0.0, 0.0, 0.0), 'linear')]},
                    'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.3, (0.0, 0.0, 0.0), 'linear')]}}},
 'death': {'dl': 1.2,
           'petla': 'once',
           'kosci': {'tulow': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.65, (-25.0, 0.0, 18.0), 'linear'),
                                            (1.2, (-28.0, 0.0, 21.0), 'linear')]},
                     'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.75, (-17.0, 0.0, 10.0), 'linear'),
                                            (1.2, (-24.0, 0.0, 12.0), 'linear')]},
                     'ramie_l': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (-12.0, 0.0, -31.0), 'linear')]},
                     'ramie_r': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (-18.0, 0.0, 29.0), 'linear')]},
                     'worek': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'), (1.2, (0.7, 0.74, 0.74), 'linear')]},
                     'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (1.2, (0.0, 0.0, 0.0), 'linear')]}}},
 'spawn': {'dl': 1,
           'petla': 'once',
           'kosci': {'korzen': {'position': [(0, (0.0, 2.0, 0.0), 'linear'),
                                             (0.55, (0.0, 0.5, 0.0), 'linear'),
                                             (1, (0.0, 0.0, 0.0), 'linear')]},
                     'tulow': {'rotation': [(0, (-22.0, 0.0, 0.0), 'linear'),
                                            (0.7, (-7.0, 0.0, 0.0), 'linear'),
                                            (1, (0.0, 0.0, 0.0), 'linear')]},
                     'worek': {'scale': [(0, (0.7, 0.75, 0.78), 'linear'),
                                         (0.7, (1.08, 1.1, 1.1), 'linear'),
                                         (1, (1.0, 1.0, 1.0), 'linear')]},
                     'fx_jezyk': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (1, (0.0, 0.0, 0.0), 'linear')]}}}}

def _klatki(pozy):
    return [(czas, wektor, 'linear') for czas, wektor in pozy]


# Dwa wyraźne odbicia stóp. Korzeń unosi całą sylwetkę o 2,6 px.
DANE['walk']['kosci'].update({
    'korzen': {'position': _klatki([(0, (0, 0, 0)), (.1, (0, 1.3, 0)),
                                     (.12, (0, 1.5, 0)),
                                     (.225, (0, 2.6, 0)), (.35, (0, 1.5, 0)),
                                     (.45, (0, 0, 0)), (.55, (0, 1.3, 0)),
                                     (.575, (0, 1.35, 0)),
                                     (.675, (0, 2.6, 0)), (.8, (0, 1.5, 0)),
                                     (.9, (0, 0, 0))])},
    'udo_l': {'rotation': _klatki([(0, (0, 0, 0)), (.12, (0, 0, 0)),
                                    (.225, (-16, 0, 7)), (.45, (0, 0, 0)),
                                    (.575, (0, 0, 0)), (.675, (9, 0, -5)),
                                    (.9, (0, 0, 0))])},
    'udo_r': {'rotation': _klatki([(0, (0, 0, 0)), (.225, (9, 0, 5)),
                                    (.45, (0, 0, 0)), (.575, (0, 0, 0)),
                                    (.675, (-16, 0, -7)), (.9, (0, 0, 0))])},
    'golen_l': {'rotation': _klatki([(0, (0, 0, 0)), (.225, (-20, 0, 0)),
                                      (.45, (0, 0, 0)), (.675, (9, 0, 0)),
                                      (.9, (0, 0, 0))])},
    'golen_r': {'rotation': _klatki([(0, (0, 0, 0)), (.225, (9, 0, 0)),
                                      (.45, (0, 0, 0)), (.675, (-20, 0, 0)),
                                      (.9, (0, 0, 0))])},
})

# Przysiad, wybicie, krótki lot i lądowanie dokładnie w klatce skryptu MM.
DANE['slam']['kosci']['korzen'] = {'position': _klatki([
    (0, (0, 0, 0)), (.2, (0, 0, 0)), (.4, (0, 2.5, -1)),
    (.48, (0, 6.5, -3)), (.55, (0, 1, -3)), (.67, (0, .2, -1)),
    (.9, (0, 0, 0))])}
DANE['slam']['kosci']['wlocznia']['position'] = _klatki([
    (0, (0, .2, 0)), (.2, (0, 1.8, 0)), (.4, (0, 1.8, 0)),
    (.6, (0, .5, 0)), (.9, (0, 0, 0))])
DANE['slam']['kosci']['tulow'] = {'rotation': _klatki([
    (0, (0, 0, 0)), (.2, (16, 0, 0)), (.4, (-8, 0, 0)),
    (.48, (-19, 0, 0)), (.55, (24, 0, 0)), (.7, (10, 0, 0)),
    (.9, (0, 0, 0))])}
for bok, znak in [('l', 1), ('r', -1)]:
    DANE['slam']['kosci']['udo_' + bok] = {'rotation': _klatki([
        (0, (0, 0, 0)), (.2, (25, 0, 8 * znak)),
        (.4, (-12, 0, 5 * znak)), (.48, (-18, 0, 6 * znak)),
        (.55, (23, 0, 8 * znak)), (.7, (12, 0, 0)), (.9, (0, 0, 0))])}
    DANE['slam']['kosci']['golen_' + bok] = {'rotation': _klatki([
        (0, (0, 0, 0)), (.2, (-20, 0, 0)), (.4, (9, 0, 0)),
        (.48, (12, 0, 0)), (.55, (-22, 0, 0)), (.9, (0, 0, 0))])}

# Przewrót całego ciała na bok; ostatnia poza pozostaje płasko przy ziemi.
DANE['death']['kosci']['korzen'] = {
    'rotation': _klatki([(0, (0, 0, 0)), (.25, (0, 0, 12)),
                          (.6, (0, 0, 90)), (.75, (0, 0, 90)),
                          (1.2, (0, 0, 90))]),
    'position': _klatki([(0, (0, 0, 0)), (.25, (0, 2.5, 0)),
                          (.5, (0, 12, 0)), (.6, (0, 14.2, 0)),
                          (.75, (0, 14.2, 0)),
                          (1.2, (0, 14.2, 0))]),
}
DANE['death']['kosci']['tulow'] = {'rotation': _klatki([
    (0, (0, 0, 0)), (.35, (-8, 0, 0)), (.7, (0, 0, 0)), (1.2, (0, 0, 0))])}
DANE['death']['kosci']['glowa'] = {'rotation': _klatki([
    (0, (0, 0, 0)), (.55, (0, 0, -10)), (1.2, (0, 0, -12))])}
DANE['death']['kosci']['wlocznia'] = {'rotation': _klatki([
    (0, (0, 0, 0)), (.7, (0, 0, -26)), (1.2, (0, 0, -26))])}
DANE['death']['kosci']['siec'] = {'position': _klatki([
    (0, (0, 0, 0)), (.5, (0, 0, 0)), (.65, (0, 1, 0)),
    (1.2, (0, 1, 0))])}

for animacja in DANE.values():
    if 'worek' in animacja['kosci']:
        animacja['kosci']['swiatlo_worek'] = animacja['kosci'].pop('worek')

for nazwa, kat in [('dlon_l', 2), ('dlon_r', -2),
                   ('przedramie_l', 2), ('stopa_l', 2), ('stopa_r', -2)]:
    DANE['idle']['kosci'][nazwa] = {'rotation': _klatki([
        (0, (0, 0, 0)), (1, (0, kat, 0)), (2, (0, 0, 0))])}
for nazwa in ('grzbiet', 'swiatlo_oczy'):
    DANE['idle']['kosci'][nazwa] = {'scale': _klatki([
        (0, (1, 1, 1)), (1, (1.02, 1.02, 1.02)),
        (2, (1, 1, 1))])}

# Pęcznieje fizyczny worek, a nie wyłącznie jego turkusowe światło.
DANE['croak']['kosci']['worek'] = {'scale': _klatki([
    (0, (1, 1, 1)), (.3, (1.1, 1.1, 1.1)),
    (.72, (1.23, 1.26, 1.24)), (1, (1.28, 1.3, 1.28))])}
DANE['croak']['kosci']['swiatlo_worek'] = {'scale': _klatki([
    (0, (1, 1, 1)), (.3, (1.04, 1.04, 1.04)),
    (.72, (1.06, 1.06, 1.06)), (1, (1.06, 1.06, 1.06))])}
DANE['tongue']['kosci']['worek'] = {'scale': _klatki([
    (0, (1.28, 1.3, 1.28)), (.15, (1.1, 1.1, 1.1)),
    (.23, (.82, .83, .82)), (.6, (1, 1, 1))])}
DANE['tongue']['kosci']['fx_jezyk']['scale'] = _klatki([
    (0, (0, 0, 0)), (.12, (0, 0, 0)),
    (.15, (1, 1, 1.9)), (.3, (1, 1, 1.9)),
    (.38, (0, 0, 0)), (.6, (0, 0, 0))])
DANE['attack']['kosci']['ramie_r']['rotation'] = _klatki([
    (0, (0, 0, 0)), (.16, (55, -12, 26)),
    (.25, (-42, 14, -30)), (.4, (-20, 7, -12)),
    (.7, (0, 0, 0))])
DANE['attack']['kosci']['wlocznia']['rotation'] = _klatki([
    (0, (0, 0, 0)), (.16, (0, 0, 35)),
    (.25, (0, 0, -22)), (.7, (0, 0, 0))])


def main(model=None):
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/rechotnik/bagna_rechotnik.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
