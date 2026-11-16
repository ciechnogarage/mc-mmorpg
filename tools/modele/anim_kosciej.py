"""Animacje kosciej; uruchom po eksporcie geometrii. Nie zmienia kostek ani atlasu."""
from pathlib import Path
from anim_bagna import zastosuj

DANE = {'idle': {'dl': 2,
          'petla': 'loop',
          'kosci': {'kregoslup': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (1, (0.0, 0.35, 0.0), 'linear'),
                                               (2, (0.0, 0.0, 0.0), 'linear')]},
                    'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (1, (0.0, -5.0, 0.0), 'linear'),
                                           (2, (0.0, 0.0, 0.0), 'linear')]},
                    'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (2, (0.0, 0.0, 0.0), 'linear')]}}},
 'walk': {'dl': 1.2,
          'petla': 'loop',
          'kosci': {'miednica': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (0.3, (0.0, 0.6, 0.0), 'linear'),
                                              (0.6, (0.0, 0.0, 0.0), 'linear'),
                                              (0.9, (0.0, 0.6, 0.0), 'linear'),
                                              (1.2, (0.0, 0.0, 0.0), 'linear')]},
                    'kregoslup': {'rotation': [(0, (0.0, 0.0, -3.0), 'linear'),
                                               (0.6, (0.0, 0.0, 3.0), 'linear'),
                                               (1.2, (0.0, 0.0, -3.0), 'linear')]},
                    'ramie_l': {'rotation': [(0, (8.0, 0.0, 0.0), 'linear'),
                                             (0.6, (-8.0, 0.0, 0.0), 'linear'),
                                             (1.2, (8.0, 0.0, 0.0), 'linear')]},
                    'ramie_p': {'rotation': [(0, (-8.0, 0.0, 0.0), 'linear'),
                                             (0.6, (8.0, 0.0, 0.0), 'linear'),
                                             (1.2, (-8.0, 0.0, 0.0), 'linear')]},
                    'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (1.2, (0.0, 0.0, 0.0), 'linear')]}}},
 'attack': {'dl': 0.6,
            'petla': 'once',
            'kosci': {'klatka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (0.13, (12.0, 0.0, 10.0), 'linear'),
                                              (0.2, (-16.0, 0.0, -10.0), 'linear'),
                                              (0.37, (-8.0, 0.0, -4.0), 'linear'),
                                              (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'ramie_p': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (0.13, (22.0, 0.0, -12.0), 'linear'),
                                               (0.2, (-32.0, 0.0, 12.0), 'linear'),
                                               (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'przedramie_p': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                                    (0.13, (18.0, 0.0, 0.0), 'linear'),
                                                    (0.2, (-35.0, 0.0, 0.0), 'linear'),
                                                    (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (0.6, (0.0, 0.0, 0.0), 'linear')]}},
            'hit': 0.2,
            'recover': 0.4,
            'skrypt': 'mm:BAGNA_KOSCIEJ_ATTACK_KLATKA'},
 'shoot': {'dl': 1.6,
           'petla': 'once',
           'kosci': {'klatka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.3, (5.0, 7.0, -8.0), 'linear'),
                                             (1.18, (10.0, 12.0, -13.0), 'linear'),
                                             (1.25, (-3.0, -2.0, 3.0), 'linear'),
                                             (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (1.18, (0.0, 7.0, 0.0), 'linear'),
                                            (1.25, (0.0, -3.0, 0.0), 'linear'),
                                            (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'ramie_l': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (0.3, (6.0, 0.0, -6.0), 'linear'),
                                              (1.18, (14.0, 0.0, -16.0), 'linear'),
                                              (1.25, (-1.0, 0.0, 0.0), 'linear'),
                                              (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'ramie_p': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (0.3, (5.0, 0.0, 8.0), 'linear'),
                                              (1.18, (23.0, 0.0, 23.0), 'linear'),
                                              (1.25, (-12.0, 0.0, -8.0), 'linear'),
                                              (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'dlon_p': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.3, (-2.0, 0.0, 0.0), 'linear'),
                                             (1.18, (-5.0, 0.0, 0.0), 'linear'),
                                             (1.25, (0.0, 0.0, 0.0), 'linear'),
                                             (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'strzala': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.18, (-3.0, 0.0, 0.0), 'linear'),
                                              (1.25, (1.0, 0.0, 0.0), 'linear'),
                                              (1.3, (1.0, 0.0, 0.0), 'linear'),
                                              (1.6, (1.0, 0.0, 0.0), 'linear')],
                                 'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                           (1.24, (1.0, 1.0, 1.0), 'linear'),
                                           (1.25, (0.0, 0.0, 0.0), 'linear'),
                                           (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'p_cieciwa': {'position': [(0, (0.0, 0.0, 0.0), 'linear'),
                                                (1.18, (-4.0, 0.0, 0.0), 'linear'),
                                                (1.25, (0.0, 0.0, 0.0), 'linear'),
                                                (1.6, (0.0, 0.0, 0.0), 'linear')]},
                     'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.23, (0.0, 0.0, 0.0), 'linear'),
                                              (1.25, (1.0, 1.0, 1.0), 'linear'),
                                              (1.32, (1.0, 1.0, 1.0), 'linear'),
                                              (1.34, (0.0, 0.0, 0.0), 'linear'),
                                              (1.6, (0.0, 0.0, 0.0), 'linear')]}},
           'windup': 1.2,
           'hit': 1.25,
           'recover': 0.35,
           'skrypt': 'mm:BAGNA_KOSCIEJ_SHOOT_KLATKA'},
 'sink': {'dl': 0.5,
          'petla': 'once',
          'kosci': {'korzen': {'scale': [(0, (1.0, 1.0, 1.0), 'linear'),
                                         (0.2, (1.0, 0.72, 1.0), 'linear'),
                                         (0.5, (1.0, 0.08, 1.0), 'linear')]},
                    'klatka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.5, (-10.0, 0.0, 0.0), 'linear')]},
                    'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.5, (0.0, 0.0, 0.0), 'linear')]}},
          'windup': 0.5,
          'hit': 0.5},
 'emerge': {'dl': 0.6,
            'petla': 'once',
            'kosci': {'korzen': {'scale': [(0, (1.0, 0.08, 1.0), 'linear'),
                                           (0.35, (1.0, 1.1, 1.0), 'linear'),
                                           (0.6, (1.0, 1.0, 1.0), 'linear')]},
                      'klatka': {'rotation': [(0, (-10.0, 0.0, 0.0), 'linear'),
                                              (0.42, (8.0, 0.0, 0.0), 'linear'),
                                              (0.6, (0.0, 0.0, 0.0), 'linear')]},
                      'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                               (0.6, (0.0, 0.0, 0.0), 'linear')]}}},
 'hurt': {'dl': 0.3,
          'petla': 'once',
          'kosci': {'klatka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.1, (15.0, 0.0, 8.0), 'linear'),
                                            (0.3, (0.0, 0.0, 0.0), 'linear')]},
                    'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                           (0.1, (-14.0, 0.0, 0.0), 'linear'),
                                           (0.3, (0.0, 0.0, 0.0), 'linear')]},
                    'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.3, (0.0, 0.0, 0.0), 'linear')]}}},
 'death': {'dl': 1.2,
           'petla': 'once',
           'kosci': {'klatka': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                             (0.7, (-27.0, 0.0, 19.0), 'linear'),
                                             (1.2, (-34.0, 0.0, 24.0), 'linear')]},
                     'glowa': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                            (0.8, (-23.0, 0.0, 13.0), 'linear'),
                                            (1.2, (-35.0, 0.0, 18.0), 'linear')]},
                     'ramie_l': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (-12.0, 0.0, -30.0), 'linear')]},
                     'ramie_p': {'rotation': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (-14.0, 0.0, 32.0), 'linear')]},
                     'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (0.0, 0.0, 0.0), 'linear')]}}},
 'spawn': {'dl': 1.2,
           'petla': 'once',
           'kosci': {'korzen': {'scale': [(0, (1.0, 0.08, 1.0), 'linear'),
                                          (0.65, (1.0, 1.06, 1.0), 'linear'),
                                          (1.2, (1.0, 1.0, 1.0), 'linear')]},
                     'klatka': {'rotation': [(0, (-15.0, 0.0, 0.0), 'linear'),
                                             (0.8, (5.0, 0.0, 0.0), 'linear'),
                                             (1.2, (0.0, 0.0, 0.0), 'linear')]},
                     'fx_strzala': {'scale': [(0, (0.0, 0.0, 0.0), 'linear'),
                                              (1.2, (0.0, 0.0, 0.0), 'linear')]}}}}

def K(*pozy):
    return [(t, v, 'linear') for t, v in pozy]


# Założenie strzały (0–0,3), pełny naciąg (0,3–1,2), puste łęczysko po 1,25.
strzal = DANE['shoot']['kosci']
strzal['ramie_p']['rotation'] = K(
    (0, (0, 0, 0)), (.3, (8, 0, 10)),
    (.7, (20, 0, 27)), (1.18, (32, 0, 35)),
    (1.25, (-14, 0, -10)), (1.6, (0, 0, 0)))
strzal['przedramie_p'] = {'rotation': K(
    (0, (0, 0, 0)), (.3, (8, 0, 0)),
    (.7, (25, 0, 0)), (1.18, (42, 0, 0)),
    (1.25, (-16, 0, 0)), (1.6, (0, 0, 0)))}
strzal['dlon_p']['position'] = K(
    (0, (0, 0, 0)), (.3, (-2, 0, -1)),
    (.7, (-4, 0, 1)), (1.18, (-6.5, 0, 2)),
    (1.25, (0, 0, 0)), (1.6, (0, 0, 0)))
strzal['p_cieciwa']['position'] = K(
    (0, (0, 0, 0)), (.3, (-1, 0, 0)),
    (.7, (-3, 0, 0)), (1.18, (-6, 0, 0)),
    (1.25, (0, 0, 0)), (1.6, (0, 0, 0)))
strzal['strzala']['position'] = K(
    (0, (0, -2, 0)), (.3, (-1, 0, 0)),
    (.7, (-2, 0, 0)), (1.18, (-3, 0, 0)),
    (1.25, (1, 0, 0)), (1.6, (1, 0, 0)))
strzal['luk'] = {'rotation': K(
    (0, (0, 0, 0)), (.3, (0, -5, 0)),
    (.7, (0, -10, -4)), (1.18, (0, -14, -8)),
    (1.25, (0, 6, 5)), (1.6, (0, 0, 0)))}



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
    model = Path(model or '/opt/docker/mc/data/world1/output/lochy/bagna/modele/kosciej/bagna_kosciej.bbmodel')
    zastosuj(model, DANE, model.with_name('czasy.json'))


if __name__ == '__main__':
    main()
