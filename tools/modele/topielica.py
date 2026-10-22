"""Topielica, Matka Mułu — boss Lochu 002. Przód modelu: -Z."""
import sys

import animacje as A
import bb
import malarz
import tekstura

S = bb.szescian
B = bb.kosc
Z = (0, 0, 0)
P = (1, 1, 1)
POD_ZIEMIA = {'zanurzenie:33', 'wynurzenie:30', 'death:8'}


def bryla():
    bb.wyczysc()
    dol = B('dol', (0, 9, 0), [
        S('miednica', (-10, 8, -6), (10, 16, 8), 2),
        S('szata_przod', (-12, 3, -9), (12, 11, -5), 2),
        S('szata_tyl', (-11, 2, 6), (11, 12, 10), 2),
        S('szata_lewa', (-14, 3, -5), (-10, 12, 7), 2),
        S('szata_prawa', (10, 4, -5), (14, 12, 7), 2),
        S('mul_ogon', (-5, 2, 8), (5, 7, 20), 2),
        S('mul_ogon_koniec', (-4, 0, 17), (4, 5, 25), 2),
    ])
    nogi = []
    for side, x in (('lewa', -8), ('prawa', 8)):
        nogi.append(B('noga_' + side, (x, 13, 1), [
            S('udo_' + side, (x-4, 5, -3), (x+4, 14, 6), 2),
            S('stopa_' + side, (x-5, 0, -9), (x+5, 5, 7), 1),
            S('palec_a_' + side, (x-5, 0, -13), (x-2, 2, -8), 3),
            S('palec_b_' + side, (x+2, 0, -14), (x+5, 2, -8), 3),
        ]))
    rece = []
    for side, s in (('lewa', -1), ('prawa', 1)):
        x = s * 18
        rece.append(B('ramie_' + side, (x, 38, 1), [
            S('bark_' + side, (min(x, x+s*8), 31, -4), (max(x, x+s*8), 41, 7), 2),
            S('ramie_kosc_' + side, (min(x+s*4, x+s*10), 18, -2), (max(x+s*4, x+s*10), 33, 5), 3),
            B('dlon_' + side, (s*25, 19, -1), [
                S('dlon_mul_' + side, (min(s*23, s*30), 14, -5), (max(s*23, s*30), 21, 4), 2),
                S('pazur_a_' + side, (min(s*24, s*26), 6, -8), (max(s*24, s*26), 15, -4), 4),
                S('pazur_b_' + side, (min(s*28, s*30), 5, -5), (max(s*28, s*30), 15, -1), 4),
                S('pazur_c_' + side, (min(s*30, s*32), 7, 0), (max(s*30, s*32), 16, 4), 4),
            ]),
            S('wodorost_' + side, (min(x+s*8, x+s*10), 11, 5), (max(x+s*8, x+s*10), 31, 7), 1),
        ], rot=(0, 0, -s*8)))
    wlosy = []
    for side, s in (('lewy', -1), ('prawy', 1)):
        x = s * 9
        wlosy.append(B('wlos_' + side, (x, 50, -3), [
            S('wlos_' + side, (min(x, x+s*7), 27, -10), (max(x, x+s*7), 52, -4), 1),
            S('pasmo_a_' + side, (min(x+s*4, x+s*7), 11, -12), (max(x+s*4, x+s*7), 28, -8), 1),
            S('pasmo_b_' + side, (min(x+s*8, x+s*11), 18, -6), (max(x+s*8, x+s*11), 37, -2), 2),
        ], rot=(0, 0, s*8)))
    glowa = B('hi_glowa', (0, 49, -5), [
        S('szyja', (-5, 42, -8), (5, 50, 1), 2),
        S('czaszka', (-11, 47, -13), (11, 61, 5), 3),
        S('twarz_cien', (-9, 48, -14), (9, 58, -13), 1),
        S('maska_topielicy', (-8, 47, -16), (8, 60, -14), 4),
        S('oczodol_lewy', (-7, 52, -17), (-1, 58, -16), 1),
        S('oczodol_prawy', (1, 52, -17), (7, 58, -16), 1),
        B('swiatlo_oczy', (0, 54, -17), [
            S('oko_lewe', (-5, 54, -18), (-3, 56, -17), 5),
            S('oko_prawe', (3, 54, -18), (5, 56, -17), 5),
        ]),
        S('nos_kosc', (-2, 49, -18), (2, 54, -16), 4),
        S('usta_cien', (-6, 46, -17), (6, 49, -16), 1),
        S('szczeka', (-6, 43, -16), (6, 47, -10), 3),
        S('zab_lewy', (-5, 44, -18), (-3, 49, -16), 4),
        S('zab_prawy', (3, 44, -18), (5, 49, -16), 4),
        S('wlos_czolo', (-10, 58, -17), (-5, 65, -11), 1),
        S('korona_srodek', (-3, 60, -2), (3, 69, 4), 2),
        S('korona_lewa', (-11, 59, -1), (-8, 69, 3), 2),
        S('korona_prawa', (8, 59, -1), (11, 66, 3), 2),
        S('korona_rog', (4, 61, 2), (7, 72, 5), 3),
    ] + wlosy)
    jaja = B('jaja_kosc', (0, 22, -11), [
        S('jaja', (-6, 19, -12), (6, 27, -9), 5),
        S('jaja_bulka_lewa', (-7, 20, -14), (-3, 25, -11), 5),
        S('jaja_bulka_prawa', (3, 20, -14), (7, 25, -11), 5),
    ])
    tulow = B('tulow', (0, 15, 0), [
        S('brzuch', (-12, 14, -8), (12, 32, 8), 2),
        S('piers', (-17, 29, -9), (17, 43, 10), 2),
        S('garb', (-19, 39, -2), (19, 49, 14), 2),
        S('serce_cien', (-7, 25, -10), (7, 37, -9), 1),
        B('swiatlo_serce', (0, 31, -10), [S('serce_mulu', (-4, 27, -12), (4, 35, -9), 5)]),
        S('zebro_gora', (-10, 37, -12), (10, 39, -9), 4),
        S('zebro_lewe', (-12, 30, -12), (-7, 37, -9), 4),
        S('zebro_prawe', (7, 30, -12), (12, 37, -9), 4),
        S('zebro_dol', (-10, 25, -11), (10, 27, -9), 4),
        S('mech_bark_lewy', (-19, 40, -4), (-9, 45, 8), 1),
        S('mech_bark_prawy', (9, 39, -4), (19, 44, 8), 1),
        glowa, jaja, *rece,
    ])
    hitbox = B('hitbox', (0, 38, 0), [S('hitbox_box', (-14, 0, -14), (14, 64, 14), 1)])
    shadow = B('shadow', (0, 0, 0), [S('shadow_box', (-15, 0, -15), (15, 1, 15), 1)])
    return [B('topielica', (0, 0, 0), [dol, *nogi, tulow]), hitbox, shadow, B('tag_name', (0, 74, 0), [])]


def animacje():
    a = {
        'idle': {'petla': 'loop', 'dlugosc': 2.4, 'kosci': {
            'tulow': {'rotation': [(0, Z), (1.2, (3, 0, 2)), (2.4, Z)]},
            'hi_glowa': {'rotation': [(0, Z), (1.2, (-5, 6, 0)), (2.4, Z)]},
            'wlos_lewy': {'rotation': [(0, Z), (1.2, (5, 0, -10)), (2.4, Z)]},
            'wlos_prawy': {'rotation': [(0, Z), (1.2, (-5, 0, 10)), (2.4, Z)]},
            'swiatlo_serce': {'scale': [(0, P), (1.2, (1.2, 1.2, 1.2)), (2.4, P)]}}},
        'walk': {'petla': 'loop', 'dlugosc': 1.0, 'kosci': {
            'tulow': {'rotation': [(0, (0, 0, -5)), (0.5, (0, 0, 5)), (1.0, (0, 0, -5))]},
            'noga_lewa': {'rotation': [(0, (8, 0, 0)), (0.5, (-8, 0, 0)), (1.0, (8, 0, 0))],
                          'position': [(0, (0, 3, 0)), (0.25, (0, 1, 0)), (0.5, (0, 3, 0)), (0.75, (0, 1, 0)), (1.0, (0, 3, 0))]},
            'noga_prawa': {'rotation': [(0, (-8, 0, 0)), (0.5, (8, 0, 0)), (1.0, (-8, 0, 0))],
                           'position': [(0, (0, 3, 0)), (0.25, (0, 1, 0)), (0.5, (0, 3, 0)), (0.75, (0, 1, 0)), (1.0, (0, 3, 0))]},
            'ramie_lewa': {'rotation': [(0, (-8, 0, -4)), (0.5, (8, 0, -4)), (1.0, (-8, 0, -4))]},
            'ramie_prawa': {'rotation': [(0, (8, 0, 4)), (0.5, (-8, 0, 4)), (1.0, (8, 0, 4))]}}},
        'attack': {'petla': 'once', 'dlugosc': 0.7, 'kosci': {
            'ramie_prawa': {'rotation': [(0, Z), (0.25, (-55, -25, -20)), (0.42, (70, 10, 15)), (0.7, Z)]},
            'tulow': {'rotation': [(0, Z), (0.25, (0, -15, 0)), (0.42, (12, 10, 0)), (0.7, Z)]}}},
        'wlosy': {'petla': 'once', 'dlugosc': 1.3, 'kosci': {
            'wlos_lewy': {'rotation': [(0, Z), (0.25, (-40, -20, -20)), (0.95, (-40, -20, -20)), (1.03, (75, 0, 10)), (1.3, Z)]},
            'wlos_prawy': {'rotation': [(0, Z), (0.25, (-40, 20, 20)), (0.95, (-40, 20, 20)), (1.03, (75, 0, -10)), (1.3, Z)]},
            'hi_glowa': {'rotation': [(0, Z), (0.95, (-20, 0, 0)), (1.1, (20, 0, 0)), (1.3, Z)]}}},
        'kaluza': {'petla': 'once', 'dlugosc': 1.4, 'kosci': {
            'tulow': {'rotation': [(0, Z), (0.4, (18, 0, 0)), (1.2, (18, 0, 0)), (1.4, Z)]},
            'ramie_lewa': {'rotation': [(0, Z), (0.3, (60, 0, -30)), (1.2, (60, 0, -30)), (1.4, Z)]},
            'ramie_prawa': {'rotation': [(0, Z), (0.3, (60, 0, 30)), (1.2, (60, 0, 30)), (1.4, Z)]}}},
        'zanurzenie': {'petla': 'once', 'dlugosc': 1.7, 'kosci': {
            'topielica': {'position': [(0, Z), (0.15, (0, 3, 0)), (0.5, (0, -30, 0)), (1.7, (0, -30, 0))]},
            'tulow': {'rotation': [(0, Z), (0.35, (22, 0, 0)), (1.7, (22, 0, 0))]}}},
        'wynurzenie': {'petla': 'once', 'dlugosc': 0.9, 'kosci': {
            'topielica': {'position': [(0, (0, -30, 0)), (0.45, (0, 3, 0)), (0.65, Z), (0.9, Z)]},
            'ramie_lewa': {'rotation': [(0, (35, 0, -30)), (0.5, (-20, 0, -45)), (0.9, Z)]},
            'ramie_prawa': {'rotation': [(0, (35, 0, 30)), (0.5, (-20, 0, 45)), (0.9, Z)]}}},
        'skrzek': {'petla': 'once', 'dlugosc': 1.3, 'kosci': {
            'jaja_kosc': {'scale': [(0, P), (0.6, (1.25, 1.3, 1.25)), (0.8, (0.85, 0.9, 0.85)), (1.3, P)]},
            'tulow': {'rotation': [(0, Z), (0.6, (20, 0, 0)), (1.3, Z)]},
            'ramie_lewa': {'rotation': [(0, Z), (0.6, (40, 0, -25)), (1.3, Z)]},
            'ramie_prawa': {'rotation': [(0, Z), (0.6, (40, 0, 25)), (1.3, Z)]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.35, 'kosci': {
            'topielica': {'position': [(0, Z), (0.12, (0, 1, 0)), (0.35, Z)]},
            'tulow': {'rotation': [(0, Z), (0.12, (-15, 0, 8)), (0.35, Z)]},
            'hi_glowa': {'rotation': [(0, Z), (0.12, (16, 0, 0)), (0.35, Z)]}}},
        'death': {'petla': 'hold', 'dlugosc': 1.5, 'kosci': {
            'topielica': {'position': [(0, Z), (0.6, (0, -3, 0)), (1.5, (0, -8, 0))]},
            'tulow': {'rotation': [(0, Z), (0.8, (35, 0, 0)), (1.5, (45, 0, 0))]},
            'hi_glowa': {'rotation': [(0, Z), (1.5, (30, 0, 0))]},
            'ramie_lewa': {'rotation': [(0, Z), (1.5, (0, 0, -65))]},
            'ramie_prawa': {'rotation': [(0, Z), (1.5, (0, 0, 65))]},
            'swiatlo_serce': {'scale': [(0, P), (1.2, (0.4, 0.4, 0.4)), (1.5, (0, 0, 0))]}}},
    }
    return a


MATERIALY = {
    'mul': {'ramp': [(24, 29, 24), (41, 51, 40), (61, 73, 51), (82, 96, 62), (110, 120, 76)], 'wzor': 'kamien'},
    'wlos': {'ramp': [(14, 28, 25), (24, 46, 39), (35, 69, 53), (49, 93, 64), (68, 118, 76)], 'wzor': 'zwis'},
    'kosc': {'ramp': [(58, 64, 53), (91, 102, 77), (129, 140, 101), (164, 173, 131), (194, 201, 157)], 'wzor': 'gladki'},
    'swiatlo': {'ramp': [(18, 81, 77), (30, 127, 117), (49, 175, 156), (91, 218, 192), (171, 245, 216)], 'wzor': 'swiatlo'},
    'ciemne': {'ramp': [(8, 17, 16), (14, 25, 23), (22, 35, 30), (30, 46, 36), (43, 59, 43)], 'wzor': 'gladki'},
    'techniczne': {'ramp': [(30, 30, 30)] * 5, 'wzor': 'gladki'},
}


def material(nazwa):
    if nazwa.startswith(('hitbox', 'shadow')):
        return 'techniczne'
    if nazwa.startswith(('wlos', 'pasmo', 'wodorost', 'mech')):
        return 'wlos'
    if nazwa.startswith(('oko', 'serce_mulu', 'jaja')):
        return 'swiatlo'
    if nazwa.startswith(('maska', 'zebro', 'zab', 'pazur', 'korona', 'czaszka')):
        return 'kosc'
    if nazwa.endswith('cien') or nazwa.startswith('oczodol'):
        return 'ciemne'
    return 'mul'


def main():
    model = bryla()
    ruch = animacje()
    bb.waliduj({k for a in ruch.values() for k in a['kosci']})
    tex, zaj = tekstura.atlas(bb.ELEMENTY, MATERIALY, material, rozmiar=256, ziarno=29, malarz=malarz.maluj2)
    print(f'atlas 256: {zaj:.0%}')
    bb.zapisz(sys.argv[1], 'topielica', model, tex, 256, A.zbuduj(ruch, bb.KOSCI, do_me=True))


if __name__ == '__main__':
    main()
