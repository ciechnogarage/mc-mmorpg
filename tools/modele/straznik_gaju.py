"""Straznik Gaju - boss Lochu 001 (koncept K1, zaakceptowany 2026-09-27; szkic: output/lochy/modele-wlasne/straznik_gaju/).
Zgarbiony drzewiec: maska z kory brzozy z jedna swiecaca szpara, w klatce zeber bursztynowe serce (skradzione swiatlo
gaju - latarnie je przygaszaja), reka-maczuga i chuda reka z palcami-korzeniami oparte o ziemie, korona zwiedlej sakury
z zaplatanym lampionem. Geometria = bryla etapu 2 (tools/modele/koncepty/bryly/k1.py); tu: materialy i malowanie
pod ksztalt (malarz.py), atlas 512 (1 teksel = 1 px), kosci swiatlo_* do mechaniki ME brightness. Przod = -Z.
Uzycie: tools/modele/zbuduj.sh straznik_gaju
"""
import sys

import bb
import malarz
import tekstura
from bryly_wspolne import S, B, konar, pasmo, kepa

POD_ZIEMIA = {'spawn'}

GLOWA = True                      # glowa nad barkami (wlasciciel 2026-09-27: 'brak glowy')
PALETA = bb.WARIANT in (2, 3)   # wariant: spokojniejsza faktura, swiatlo na calej bryle, mniej barw


def leb():
    if not GLOWA:
            return B('leb', (0, 80, -8), [
                S('szyja', (-6, 72, -14), (6, 84, -6), 1),
                S('leb_kora', (-7, 67, -20), (7, 85, -12), 2),
                S('maska', (-8, 66, -24), (8, 86, -20), 4),
                S('broda', (-5, 62, -23), (5, 66, -20), 4),
                B('swiatlo_maska', (0, 78, -24), [S('szpara', (-4, 77, -24.5), (4, 79, -23.5), 5)]),
            ], rot=(22, 0, 0))
    # twarz: drewniana czaszka (wybor wlasciciela 2026-09-29); warianty do porownan: 4 = stara maska, 5 = kora, 7 = dziupla
    twarz = {4: 'maska', 5: 'kora', 7: 'dziupla'}.get(bb.WARIANT, 'czaszka')
    glowa = [S('szyja', (-6, 80, -14), (6, 92, -4), 1),
             S('leb_kora', (-10, 80, -24), (10, 102, -6), 2),
             S('czolo', (-11, 98, -28), (11, 103, -20), 2),                 # nawis: cien nad twarza
             S('czolo_rog_l', (-14, 99, -24), (-10, 102, -14), 2, origin=(-12, 100, -19), rot=(0, 0, 22.5)),
             S('czolo_rog_p', (10, 99, -24), (14, 102, -14), 2, origin=(12, 100, -19), rot=(0, 0, -22.5))]
    broda = [pasmo(x, 81, -24.1, 8 + (x * 7) % 7, 1) for x in (-9, -7, 6, 8)]
    if twarz == 'maska':
        glowa += [S('maska', (-8, 81, -28), (8, 101, -24), 4),
                  B('swiatlo_maska', (0, 93, -28), [S('szpara', (-4, 92, -28.5), (4, 94, -27.5), 5)]),
                  S('broda', (-5, 77, -27), (5, 81, -24), 4)]
        broda += [pasmo(x, 77.5, -27.1, 6 + (x * 5) % 5, 1) for x in (-4, -1, 2)]
    elif twarz == 'kora':
        # twarz wyrzezbiona w korze: grzbiet nosa, wystajace policzki, gleboki cien oczodolow pod nawisem, 2 oczy jak serce
        glowa += [S('nos', (-2, 84, -28), (2, 98, -24), 3),
                  S('policzek_l', (-10, 82, -27), (-3, 91, -24), 3), S('policzek_p', (3, 82, -27), (10, 91, -24), 3),
                  S('oczodol_l', (-9, 91, -24.2), (-2, 98, -24.1), 1), S('oczodol_p', (2, 91, -24.2), (9, 98, -24.1), 1),
                  B('swiatlo_oczy', (0, 94, -25), [S('oko_l', (-8, 93, -25), (-5, 95, -24), 5), S('oko_p', (5, 93, -25), (8, 95, -24), 5)]),
                  S('usta', (-7, 80, -24.2), (7, 82, -24.1), 1)]
    elif twarz == 'czaszka':
        # wychudzona drewniana czaszka (wybor wlasciciela 2026-09-29: B, "straszniejszy"): gleboko zapadniete oczodoly pod
        # wysunietym korowym nawisem, w nich male swiecace punkty; dlugi waski pysk, rozwarta szczeka z ciemna paszcza,
        # dlugie cienkie kly jak ciernie, korzen przerastajacy przez oczodol, pekniecie przez czaszke
        glowa += [S('czolo_nawis', (-11, 97, -34), (11, 101, -28), 2),
                  S('czaszka', (-9, 86, -30), (9, 99, -22), 3),

                  S('oczodol_l', (-8, 88, -30), (-2, 96, -30), 1), S('oczodol_p', (2, 88, -30), (8, 96, -30), 1),
                  B('swiatlo_oczy', (0, 91, -31), [S('oko_l', (-6, 90, -31), (-4, 92, -30), 5), S('oko_p', (4, 90, -31), (6, 92, -30), 5)])]
        # pysk opuszczony w dol (patrzy spode lba) - jedna kosc, obrot wokol nasady przy czaszce
        glowa += [B('pysk_kosc', (0, 84, -30), [
            S('pysk', (-4, 82, -43), (4, 90, -30), 3),
            S('pysk_grzbiet', (-2, 89, -41), (2, 91, -30), 3),
            S('nozdrza', (-2, 86, -43), (2, 88, -43), 1),
            S('paszcza', (-3, 77, -40), (3, 82, -30), 1),
            S('zuchwa', (-4, 73, -39), (4, 77, -28), 3)]
            + [S(f'kiel_g{i}', (x, 77, z), (x + 1, 82, z + 1), 4) for i, (x, z) in enumerate(((-4, -42), (3, -42), (-4, -37), (3, -37), (-1, -43)))]
            + [S(f'kiel_d{i}', (x, 77, z), (x + 1, 80, z + 1), 4) for i, (x, z) in enumerate(((-3, -38), (2, -38), (-3, -33), (2, -33)))],
            rot=(-14, 0, 0))]
        glowa += [B('korzen_oczodol', (-5, 94, -30), [S('korzen_oko', (-6, 93, -38), (-4, 95, -30), 2)], rot=(-22.5, 0, 15))]
    else:
        # dziupla: wielka ciemna jama z grubym korowym brzegiem, w srodku jedno swiecace oko
        glowa += [S('dziupla', (-6, 83, -24.2), (6, 97, -24.1), 1),
                  S('warga_g', (-8, 97, -27), (8, 100, -24), 3), S('warga_d', (-8, 80, -27), (8, 83, -24), 3),
                  S('warga_l', (-9, 82, -26), (-6, 98, -24), 3), S('warga_p', (6, 82, -26), (9, 98, -24), 3),
                  B('swiatlo_oko', (0, 90, -25), [S('oko', (-2, 88, -25.5), (2, 92, -24.5), 5)])]
    return B('leb', (0, 90, -10), glowa + broda, rot=(22, 0, 0))


def zbuduj():
    nogi = []
    for s in (-1, 1):
        x0 = 9 * s
        nogi.append(B(f'noga{s}', (x0 * 1.3, 30, 2), [
            S(f'udo{s}', (min(x0, x0 + 13 * s), 16, -5), (max(x0, x0 + 13 * s), 32, 9), 2),
            S(f'golen{s}', (min(x0 + s, x0 + 14 * s), 5, -6), (max(x0 + s, x0 + 14 * s), 18, 8), 2),
            S(f'stopa{s}', (min(x0 - s, x0 + 17 * s), 0, -13), (max(x0 - s, x0 + 17 * s), 6, 9), 1),
            S(f'palec{s}a', (min(x0 + 2 * s, x0 + 6 * s), 0, -19), (max(x0 + 2 * s, x0 + 6 * s), 3, -13), 1),
            S(f'palec{s}b', (min(x0 + 10 * s, x0 + 14 * s), 0, -20), (max(x0 + 10 * s, x0 + 14 * s), 3, -13), 1),
            S(f'plyta_uda{s}', (min(x0 + 13 * s, x0 + 15 * s), 18, -3), (max(x0 + 13 * s, x0 + 15 * s), 30, 8), 3),
            S(f'korzonek{s}a', (min(x0 + 17 * s, x0 + 24 * s), 0, 0), (max(x0 + 17 * s, x0 + 24 * s), 2, 2), 1),
            S(f'korzonek{s}b', (min(x0 + 4 * s, x0 + 6 * s), 0, 9), (max(x0 + 4 * s, x0 + 6 * s), 2, 15), 1),
        ], rot=(0, 0, 0)))

    reka_l = B('bark_l', (-24, 88, 4), [
        S('ramie_l', (-35, 64, -3), (-25, 90, 9), 2),
        S('plyta_ramie_l', (-38, 66, -2), (-36, 84, 8), 3),
        S('suk_l', (-42, 74, 1), (-36, 76, 3), 2, origin=(-36, 75, 2), rot=(0, 0, -22.5)),
        pasmo(-36.2, 88, -4, 22, 3), 
        S('lokiec_l', (-36, 56, -4), (-24, 66, 10), 2),
        B('przedr_l', (-31, 58, 2), [
            S('przedr_l1', (-37, 40, -5), (-24, 58, 9), 2),
            S('plyta_przedr_l', (-40, 36, -4), (-38, 52, 8), 3),
            S('galazka_l', (-46, 44, 0), (-38, 46, 2), 2, origin=(-38, 45, 1), rot=(0, 0, 22.5)),
            S('przedr_l2', (-41, 22, -9), (-20, 42, 13), 2),        # maczuga: grubieje ku piesci
            S('piesc_l', (-44, 6.1, -12), (-18, 21, 14), 1),
            S('klykiec_l1', (-44, 6, -18), (-38, 12, -12), 1), S('klykiec_l2', (-35, 6, -19), (-29, 12, -12), 1),
            S('klykiec_l3', (-26, 6, -18), (-20, 12, -12), 1),
            pasmo(-36, 52, -5.1, 16, 3), pasmo(-30, 46, -5.1, 12, 2),
        ], rot=(4, 0, -4)),
    ], rot=(27, 0, -10))
    reka_p = B('bark_p', (24, 86, 4), [
        S('ramie_p', (23, 62, -1), (31, 88, 7), 2),
        S('suk_p', (32, 70, 2), (37, 72, 4), 2, origin=(32, 71, 3), rot=(0, 0, 22.5)),
        pasmo(31.1, 86, -1, 18, 2), 
        B('przedr_p', (27, 60, 3), [
            S('przedr_p1', (22, 26, -2), (32, 50, 8), 2), S('przedr_p0', (23, 48, -1), (31, 62, 7), 2),
            S('plyta_przedr_p', (32, 32, 0), (34, 50, 7), 3),
            S('dlon_p', (20, 18, -4), (34, 28, 10), 1),
            B('palce_p', (27, 18, 3), [S('pl1', (20, 6, -4), (22, 18, -2), 1), S('pl2', (24, 5, -6), (26, 18, -4), 1),
                                       S('pl3', (28, 5, -5), (30, 18, -3), 1), S('pl4', (32, 2, -2), (34, 18, 0), 1)], rot=(-10, 0, 0)),
        ], rot=(2, 0, 6)),
    ], rot=(27, 0, 10))

    korona = [konar((-10, 94, 12), 24, 5, (-12, 0, 30), 2, 4, widly=((10, 0, 30), (-15, 0, -25))),
              konar((2, 96, 14), 28, 5, (18, 0, 4), 2, 5, widly=((-12, 0, 28), (8, 0, -34))),
              konar((12, 94, 10), 22, 4, (-20, 0, -36), 2, 4, widly=((0, 0, 40), (20, 0, -20))),
              konar((-20, 90, 6), 16, 4, (-30, 0, 60), 1, 4),
              konar((18, 90, 14), 18, 3, (25, 0, -55), 1, 3),
              # zlamany konar zwisa na bok (zwiedla sakura)
              konar((22, 92, 4), 16, 3, (0, 0, -115), 1, 3, kwiat_w=3)]

    tulow = B('tulow', (0, 34, 2), [
        # odwrocony trapez: waska talia -> szeroka klatka -> masywne barki (szkic K1), boki sciete 22.5 st.
        S('miednica', (-13, 26, -7), (13, 36, 10), 2),
        S('talia', (-10, 34, -6), (10, 46, 9), 2),
        S('brzuch', (-13, 44, -9), (13, 54, 12), 2),
        S('piers_dol', (-17, 52, -11), (17, 62, 14), 2),
        S('piers', (-21, 60, -13), (21, 74, 16), 2),
        S('bok_skos_l', (-18, 42, -7), (-12, 60, 11), 2, origin=(-15, 51, 2), rot=(0, 0, -22.5)),
        S('bok_skos_p', (12, 42, -7), (18, 60, 11), 2, origin=(15, 51, 2), rot=(0, 0, 22.5)),
        S('piers_luk_l', (-22, 62, -15), (-12, 74, -9), 2), S('piers_luk_p', (12, 62, -15), (22, 74, -9), 2),
        S('garb', (-27, 72, -8), (27, 90, 20), 2),
        S('kark', (-19, 84, 6), (19, 98, 24), 2), S('kark_grzbiet', (-12, 94, 10), (12, 100, 22), 3),
        # sciecia krawedzi garbu (22.5 st. - dozwolone w ME): bez tarasow ze schodkowych plyt
        S('garb_skos_l', (-27, 82, -4), (-17, 92, 20), 2, origin=(-22, 87, 8), rot=(0, 0, 22.5)),
        S('garb_skos_p', (17, 82, -4), (27, 92, 20), 2, origin=(22, 87, 8), rot=(0, 0, -22.5)),
        S('kark_skos_l', (-20, 92, 6), (-10, 100, 22), 2, origin=(-15, 96, 14), rot=(0, 0, 22.5)),
        S('kark_skos_p', (10, 92, 6), (20, 100, 22), 2, origin=(15, 96, 14), rot=(0, 0, -22.5)),
        S('garb_skos_t', (-18, 84, 16), (18, 96, 26), 2, origin=(0, 90, 21), rot=(22.5, 0, 0)),
        S('kikut_1', (-12, 80, 20), (-8, 84, 30), 2, origin=(-10, 82, 20), rot=(22.5, 0, 0)),
        S('kikut_2', (6, 74, 20), (9, 77, 27), 2, origin=(7.5, 75.5, 20), rot=(-22.5, 0, 0)),
        pasmo(-22, 80, 20.2, 26, 3), pasmo(-12, 76, 20.2, 18, 2), pasmo(4, 84, 26.2, 30, 3), pasmo(14, 78, 20.2, 22, 2),
        pasmo(-25.2, 90, 2, 28, 3), pasmo(24.2, 88, 4, 24, 3),
        S('bark_masa_p', (20, 72, -6), (31, 90, 14), 2),
        S('garb_szczyt', (-18, 88, -2), (18, 96, 16), 3),        # swiatlo na garbie
        S('bark_l_masa', (-36, 74, -8), (-20, 96, 16), 2), S('kamien', (-34, 90, -2), (-24, 98, 8), 3),
        # klatka zeber z sercem
        S('wneka', (-10, 48, -13), (10, 66, -11), 1),
        B('swiatlo_serce', (0, 57, -12), [S('serce', (-5, 52, -14), (5, 62, -10), 5)]),
        S('zebro1', (-12, 63, -15), (12, 65, -12), 3), S('zebro2', (-12, 58, -15), (-6, 60, -12), 3),
        S('zebro3', (6, 58, -15), (12, 60, -12), 3), S('zebro4', (-12, 52, -15), (12, 54, -12), 3),
        S('zebro5', (-11, 47, -15), (11, 49, -12), 3),
        leb(),
        reka_l, reka_p,
        pasmo(-24, 78, -8.2, 20, 3), pasmo(-18, 74, -12.2, 14, 2), pasmo(18, 72, -12.2, 16, 3), pasmo(24, 80, -8.2, 22, 2),
        # przygasle lampiony zaplatane w korone (pomysl z K2)
        S('lampion_sznur', (-15, 104, 12), (-14, 112, 13), 1), B('swiatlo_lampion', (-14.5, 101, 12.5), [S('lampion', (-17, 98, 10), (-12, 104, 15), 5)]),
    ] + korona, rot=(-28, 0, 0))
    return nogi + [tulow]


# --- paleta (karta stylu Lochu 001: ciemne cialo, jasna maska, jedno cieple swiatlo, przygaszony roz zwiedlej sakury)
KORA = [(34, 28, 22), (52, 41, 30), (74, 58, 40), (100, 79, 54), (128, 104, 72)]
KORA_C = [(24, 20, 16), (38, 30, 23), (54, 43, 31), (74, 59, 42), (96, 78, 56)]
MECH = [(40, 54, 24), (58, 78, 30), (82, 104, 40), (112, 134, 56)]
BRZOZA = [(58, 54, 48), (150, 146, 132), (190, 186, 170), (222, 218, 204), (242, 240, 230)]
KWIAT = [(70, 48, 54), (98, 68, 78), (128, 94, 104), (158, 122, 130), (186, 154, 158)]   # zwiedla, przygaszona sakura
SUCHE = [(52, 38, 24), (74, 54, 32), (98, 72, 42), (124, 94, 56), (148, 116, 72)]
SWIATLO = [(150, 80, 16), (206, 124, 28), (244, 170, 48), (255, 212, 100), (255, 240, 176)]
KOSC = [(46, 40, 32), (82, 72, 56), (118, 106, 84), (150, 138, 110), (176, 164, 134), (196, 186, 156)]
ZEBRO = [(92, 84, 70), (122, 112, 94), (152, 140, 118), (182, 170, 146), (206, 196, 172)]
KAMIEN = [(52, 54, 56), (72, 74, 76), (96, 98, 98), (122, 124, 122), (146, 148, 144)]
WNEKA = [(14, 11, 9), (20, 16, 12), (26, 21, 16), (32, 26, 20), (40, 32, 24)]

if PALETA:
    # jedno swiatlo na calej bryle: gora (garb, barki, korona) ton jasniej, dol (nogi, dlonie) ciemniej; maska cieplo-szara
    # (nie najjasniejsza plama - fokus na sercu); korona glownie sucha, roz tylko akcentem
    BRZOZA = [(56, 52, 46), (126, 120, 106), (158, 150, 132), (184, 176, 156), (204, 196, 176)]
    KORA_G = [KORA[1], KORA[2], KORA[3], KORA[4], (152, 126, 90)]
else:
    KORA_G = KORA

KORA2 = [(30, 20, 18), (52, 34, 26), (80, 54, 36), (110, 78, 50), (142, 106, 68), (172, 138, 92)]
KORA2_C = [(22, 15, 14), (38, 25, 20), (58, 40, 29), (82, 58, 40), (108, 80, 54), (132, 104, 72)]

MATERIALY = {
    'kora': {'ramp': KORA2, 'wzor': 'kora2'},
    'kora_mech': {'ramp': KORA2, 'wzor': 'kora2', 'mech': 0.55, 'mech_ramp': MECH},
    'kora_c': {'ramp': KORA2_C, 'wzor': 'kora2'},
    'kora_plyta': {'ramp': KORA2[1:] + [(196, 162, 112)], 'wzor': 'kora2', 'mech': 0.3, 'mech_ramp': MECH},
    'konar': {'ramp': KORA2, 'wzor': 'kora2', 'mech': 0.25, 'mech_ramp': MECH},
    'korzen': {'ramp': KORA2_C, 'wzor': 'kora2'},
    'brzoza': {'ramp': BRZOZA, 'wzor': 'brzoza'},
    'kwiat': {'ramp': KWIAT, 'wzor': 'kwiat'},
    'suche': {'ramp': SUCHE, 'wzor': 'kwiat'},
    'swiatlo': {'ramp': SWIATLO, 'wzor': 'swiatlo'},
    'zebro': {'ramp': ZEBRO, 'wzor': 'kora2'},
    'kosc': {'ramp': KOSC, 'wzor': 'kora2'},
    'kiel': {'ramp': KOSC[2:] + [(214, 206, 180), (230, 224, 202)], 'wzor': 'gladki'},
    'kamien': {'ramp': KAMIEN, 'wzor': 'kamien', 'mech': 0.4, 'mech_ramp': MECH},
    'wneka': {'ramp': WNEKA, 'wzor': 'gladki'},
    'zwis': {'ramp': MECH, 'wzor': 'zwis', 'mech_ramp': MECH},
}


def material(n):
    if n in ('serce', 'szpara', 'lampion', 'oko', 'oko_l', 'oko_p'):
        return 'swiatlo'
    if n in ('nos', 'policzek_l', 'policzek_p') or n.startswith('warga'):
        return 'kora_plyta'
    if n in ('czaszka', 'pysk', 'pysk_grzbiet', 'zuchwa'):
        return 'kosc'
    if n.startswith('kiel'):
        return 'kiel'
    if n == 'korzen_oko':
        return 'korzen'
    if n == 'czolo_nawis':
        return 'kora_c'
    if n.startswith(('oczodol', 'usta', 'dziupla', 'nozdrza', 'paszcza')):
        return 'wneka'
    if n in ('maska', 'broda'):
        return 'brzoza'
    if n.startswith('zebro'):
        return 'zebro'
    if n == 'kamien':
        return 'kamien'
    if n == 'wneka':
        return 'wneka'
    if n.startswith('mech'):
        return 'zwis'
    if n.startswith('kw'):
        if PALETA:
            return 'kwiat' if int(n[2:]) % 4 == 0 else 'suche'
        return 'suche' if int(n[2:]) % 3 == 0 else 'kwiat'
    if n.startswith('konar'):
        return 'konar'
    if n.startswith(('garb', 'kark', 'bark', 'ramie')):
        return 'kora_mech'
    if n.startswith('plyta'):
        return 'kora_plyta'
    if n.startswith(('suk', 'galazka', 'kikut')):
        return 'konar'
    if n.startswith(('piesc', 'klykiec', 'dlon', 'pl', 'palec', 'stopa', 'lampion_sznur', 'korzonek')):
        return 'korzen'
    if n.startswith(('udo', 'golen', 'szyja', 'leb_kora', 'czolo')):
        return 'kora_c'
    return 'kora'


# maska: kora brzozy na korowym lbie (sciana north 16 x 20; wiersz 0 = gora). Bez ust: szpara (osobna swiecaca kostka,
# wiersze 7-8) w wyrzezbionej oprawie, zywica sciekajaca spod oka, pekniecie przez cala maske, obtluczone rogi.
M = {'k': BRZOZA[0], 's': (30, 25, 21), 'c': (110, 104, 92), 'j': BRZOZA[4]}
MASKA = ['cc.kk....kkk..cc',
         'c........c.....c',
         '...kkk...c.kk...',
         '.........c......',
         '.jjjj......jjjj.',
         '..cccc....cccc..',
         '....ccs..scc....',
         '...s........s...',
         '...s........s...',
         '....ssssssss....',
         '....s.......s...',
         '....s........s..',
         '.kk.s........s..',
         '.....s...c......',
         '.....s...c...kk.',
         '.........c......',
         '..kk.....c......',
         '........c.......',
         '.kkk....c...kk..',
         'cc.............c']
# serce: swiecace sloje drzewa (sciana north 10 x 10) - "serce drzewa", skradzione swiatlo gaju
SERCE = ['.dbbbbbbd.',
         'dbbaaaabbd',
         'bbaddddabb',
         'badbbbbdab',
         'badbccbdab',
         'badbccbdab',
         'badbbbbdab',
         'bbaddddabb',
         'dbbaaaabbd',
         '.dbbbbbbd.']
SK = {'d': SWIATLO[0], 'a': SWIATLO[1], 'b': SWIATLO[2], 'c': SWIATLO[4]}
CZASZKA = ['.cc...............',
           '..c.........s.....',
           '..cc.......ss.....',
           '...c..............',
           '..................',
           '..................',
           '..................',
           '........cc........',
           '.........c........',
           '........c.........',
           '........cc.......s',
           '.s.......c......ss',
           'ss.......c........']
CK = {'c': (30, 24, 18), 's': (70, 60, 44)}
NAKLEJKI = {('maska', 'north'): (MASKA, M), ('serce', 'north'): (SERCE, SK), ('czaszka', 'north'): (CZASZKA, CK)}


def main():
    bb.wyczysc()
    outliner = zbuduj()
    tex, zaj = tekstura.atlas(bb.ELEMENTY, MATERIALY, material, rozmiar=512, ziarno=11, naklejki=NAKLEJKI, malarz=malarz.maluj2)
    print(f'atlas 512: zajetosc {zaj:.0%}')
    bb.zapisz(sys.argv[1], 'straznik_gaju', outliner, tex, 512, [])


if __name__ == '__main__':
    main()
