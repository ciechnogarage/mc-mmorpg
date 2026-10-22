"""Cierniowy Lucznik (Loch 001, rola: dystans - uczy zaslony/uniku, odskakuje przy podejsciu). Generator modelu ME.

KONCEPT (po recenzji 2026-09-26): chudy, przygarbiony stwor z pedow jezyny (~1.6 bloku). Tulow i konczyny oplecione
lukowatymi pedami z haczykowatymi kolcami wystajacymi poza obrys, "wlosy" z wygietych pedow, czarnofioletowe grona
jezyn. Plochliwa twarz: duze jasne oczy z czerwonawa zrenica (czerwien = rodzina Straznika Gaju), liściaste uszy,
ktore kladzie po sobie przy strachu. Luk z galezi z owijka na chwycie; cieciwa z dwoch odcinkow tworzacych "V"
przy naciagu; strzala-kolec lezy na luku w osi celu i cofa sie z cieciwa.
Animacje: idle (nerwowe spojrzenia), walk, shoot (bokiem do celu, naciag 0.7 s z drzeniem, odrzut), leap_back
(przysiad-wybicie-ladowanie, bez przesuwania korzenia - skok robi MythicMobs), hurt, death (pada na plecy, luk wypada).
Obroty = prawdziwy ruch (jak spoczynkowy): kosc zwisajaca obrocona o +x idzie do przodu (-z).
Uzycie: tools/modele/zbuduj.sh cierniowy_lucznik
"""
import sys

import animacje as A
import tekstura
from bb import ELEMENTY, KOSCI, kosc as B, szescian as S, waliduj, zapisz

POD_ZIEMIA = set()
DY = -0.8  # nogi opuszczone: stopy maja stac na ziemi, nie wisiec 0.84 px (recenzja)


def S2(nazwa, a, b, walor, **kw):
    """szescian z normalizacja naroznikow (from < to) - bez odwroconych elementow."""
    lo = tuple(min(a[i], b[i]) for i in range(3))
    hi = tuple(max(a[i], b[i]) for i in range(3))
    return S(nazwa, lo, hi, walor, **kw)


def zbuduj():
    nogi = []
    for strona, sx in (('l', -1), ('p', 1)):
        x = 2 * sx
        stopa = B(f'stopa_{strona}', (x, 1 + DY, -1), [  # pivot w kostce (nie w piecie) - stopa nie odrywa sie od goleni
            # +0.1: w spoczynku stopa 0.14 px nad ziemia (niewidoczne), przejscia animacji nie przecinaja ziemi
            S(f'stopa_{strona}', (x - 1, DY + 0.1, -1.5), (x + 1, DY + 1.1, 2), 2),
            S(f'palec_{strona}', (x - 0.5, DY + 0.1, -3), (x + 0.5, DY + 1.1, -1.5), 1)])
        golen = B(f'golen_{strona}', (x, 7 + DY, -1), [S(f'golen_{strona}', (x - 0.75, 1 + DY, -1.75), (x + 0.75, 7 + DY, -0.25), 2),
                                                       stopa], rot=(-22.5, 0, 0))
        ped = S2(f'ped_udo_{strona}', (x + sx * 1, 8 + DY, -0.5), (x + sx * 2, 12 + DY, 0.5), 5)
        hak = S2(f'kolec_udo_{strona}', (x + sx * 2, 10 + DY, -0.5), (x + sx * 3, 11 + DY, 0.5), 4)
        nogi.append(B(f'udo_{strona}', (x, 13 + DY, 0), [S(f'udo_{strona}', (x - 1, 7 + DY, -1), (x + 1, 13 + DY, 1), 3), ped, hak,
                                                          golen], rot=(22.5, 0, 0)))
    tulow = [
        S('miednica', (-3, 12, -2), (3, 15, 2), 3),
        S('klatka', (-3, 15, -2), (3, 19, 2), 3),
        S('barki', (-3.5, 19, -2.5), (3.5, 22.5, 2.5), 3),
        # pedy oplatajace tulow: segmenty 1 px wychodzace poza obrys, pod katem 22.5
        S('ped_1', (-4, 13, -1), (-3, 20, 0), 5, origin=(-3.5, 13, -0.5), rot=(0, 0, -22.5)),
        S('ped_2', (3, 14, 0), (4, 21, 1), 5, origin=(3.5, 14, 0.5), rot=(0, 0, 22.5)),
        S('ped_3', (-2, 15, 2), (-1, 22, 3), 5, origin=(-1.5, 15, 2.5), rot=(22.5, 0, 0)),
        S('ped_4', (1, 12, -3), (2, 18, -2), 5, origin=(1.5, 12, -2.5), rot=(-22.5, 0, 0)),
        # haczykowate kolce na krawedziach sylwetki (skierowane w dol)
        S('kolec_1', (-5, 19, -0.5), (-4, 21, 0.5), 4, origin=(-4.5, 21, 0), rot=(0, 0, -22.5)),
        S('kolec_2', (4, 17, -0.5), (5, 19, 0.5), 4, origin=(4.5, 19, 0), rot=(0, 0, 22.5)),
        S('kolec_3', (-0.5, 20, 2.5), (0.5, 22, 4.5), 4, origin=(0, 22, 2.5), rot=(-45, 0, 0)),
        S('kolec_4', (-2.5, 16, 2), (-1.5, 17, 4), 4, origin=(-2, 17, 2), rot=(-45, 0, 0)),
        S('kolec_5', (1.5, 18, 2), (2.5, 19, 4), 4, origin=(2, 19, 2), rot=(-45, 0, 0)),
        # grona jezyn (ciemny fiolet) - na boku i biodrze, NIE na twarzy
        # grona jezyn 2x2 (pojedyncze piksele byly niewidoczne z 4 blokow)
        S('jagoda_1', (2, 14, -3.5), (4, 16, -2.5), 6), S('jagoda_2', (3, 16, -3.5), (4, 17, -2.5), 6),
        S('jagoda_3', (-5, 13, -1), (-3, 15, 1), 6),
        S('lisc_bok', (-6, 17, -1), (-3, 21, -1), 5),
    ]
    glowa = B('hi_glowa', (0, 22, -0.5), rot=(8, 0, 0), dzieci=[
        S('glowa', (-3, 22, -3.5), (3, 27, 2.5), 3),
        S('pysk', (-1.5, 22.5, -4.5), (1.5, 24, -3.5), 2),
        S('wlos_1', (-2, 27, -1), (-1, 30, 0), 5, origin=(-1.5, 27, -0.5), rot=(0, 0, 22.5)),
        S('wlos_2', (0, 27, 0), (1, 30, 1), 5, origin=(0.5, 27, 0.5), rot=(22.5, 0, 0)),
        S('wlos_3', (1, 27, -1), (2, 30, 0), 5, origin=(1.5, 27, -0.5), rot=(0, 0, -45)),
        S('wlos_4', (-1, 27, 1), (0, 30, 2), 5, origin=(-0.5, 27, 1.5), rot=(45, 0, 0)),
        B('ucho_l', (-3, 25, 0), [S('ucho_l', (-6, 24, 0), (-3, 27, 0), 5)], rot=(-25, 22.5, 22.5)),  # uszy po sobie
        B('ucho_p', (3, 25, 0), [S('ucho_p', (3, 24, 0), (6, 27, 0), 5)], rot=(-25, -22.5, -22.5)),
    ])
    # luk w lewej dloni: w spoczynku wzdluz osi z. Ramiona luku to osobne kosci (uginaja sie przy naciagu),
    # odcinki cieciwy maja pivot na koncach luku i przy naciagu obracaja sie ku sobie ("V").
    # cieciwa jako belka 1x1 (plaszczyzna bylaby widziana krawedzia z perspektywy gracza - recenzja)
    cieciwa_g = B('cieciwa_g', (-4.5, 9, -8), [S('cieciwa_g', (-5, 8.5, -8), (-4, 9.5, 0), 8)])
    cieciwa_d = B('cieciwa_d', (-4.5, 9, 8), [S('cieciwa_d', (-5, 8.5, 0), (-4, 9.5, 8), 8)])
    luk_g = B('luk_g', (-4.5, 6.5, -1), [S('luk_ramie_g', (-5, 6, -8), (-4, 7, -1), 7, origin=(-4.5, 6.5, -1), rot=(-22.5, 0, 0)),
                                         S('luk_koniec_g', (-5, 8, -9), (-4, 9, -8), 4), cieciwa_g])
    luk_d = B('luk_d', (-4.5, 6.5, 1), [S('luk_ramie_d', (-5, 6, 1), (-4, 7, 8), 7, origin=(-4.5, 6.5, 1), rot=(22.5, 0, 0)),
                                        S('luk_koniec_d', (-5, 8, 8), (-4, 9, 9), 4), cieciwa_d])
    # strzala lezy na luku prostopadle do niego (lokalnie wzdluz -y = do przodu po uniesieniu reki), nock przy cieciwie
    strzala = B('fx_strzala', (-4.5, 9, 0), [S('strzala', (-5, -1, -0.5), (-4, 9, 0.5), 4),
                                          S('strzala_grot', (-5, -3, -0.5), (-4, -1, 0.5), 6)])
    luk = B('luk', (-4.5, 7, 0), [S('luk_chwyt', (-5.5, 5, -1), (-3.5, 8, 1), 9), luk_g, luk_d, strzala])
    rece = []
    for strona, sx in (('l', -1), ('p', 1)):
        x = 4.5 * sx
        dlon = [S(f'dlon_{strona}', (x - 1, 6, -1), (x + 1, 8, 1), 2),
                S(f'szpon_{strona}', (x - 0.5, 5, -1.5), (x + 0.5, 6, -0.5), 1)]
        if sx < 0:
            dlon.append(luk)
        przedr = B(f'przedramie_{strona}', (x, 14, 0), [S(f'przedramie_{strona}', (x - 1, 8, -1), (x + 1, 14, 1), 2),
                                                        S2(f'ped_przedr_{strona}', (x + sx * 1, 9, -0.5), (x + sx * 2, 13, 0.5), 5)] + dlon)
        rece.append(B(f'ramie_{strona}', (x, 21, 0), [
            S(f'ramie_{strona}', (x - 1, 14, -1), (x + 1, 21.5, 1), 3),
            S2(f'kolec_ramie_{strona}', (x + sx * 1, 18, -0.5), (x + sx * 2.5, 19, 0.5), 4, origin=(x + sx * 1, 18.5, 0),
               rot=(0, 0, sx * 22.5)),
            przedr]))
    korpus = B('tulow', (0, 13, 0), tulow + [glowa] + rece, rot=(-12, 0, 0))  # czujny, lekko pochylony
    # luk wypuszczony przy smierci: lezy plasko na ziemi obok ciala (w innych animacjach skala 0)
    luk_ziemia = B('fx_luk_ziemia', (-8, 0, 2), [
        S('luk_z_chwyt', (-9, 0.1, 1), (-7, 1.1, 3), 9),
        S('luk_z_ramie_1', (-8.5, 0.1, -5), (-7.5, 1.1, 1), 7, origin=(-8, 0.6, 1), rot=(0, 22.5, 0)),
        S('luk_z_ramie_2', (-8.5, 0.1, 3), (-7.5, 1.1, 9), 7, origin=(-8, 0.6, 3), rot=(0, -22.5, 0)),
    ])
    hitbox = B('hitbox', (0, 24, 0), [S('hitbox_box', (-5, 0, -5), (5, 31, 5), 1)])  # obejmuje glowe z pedami
    cien = B('shadow', (0, 0, 0), [S('shadow_box', (-5, 0, -5), (5, 1, 5), 1)])
    return [B('cierniowy_lucznik', (0, 0, 0), [korpus] + nogi), luk_ziemia, hitbox, cien, B('tag_name', (0, 32, 0), [])]


# twarz malowana (north glowy 6x5): duze jasne oczy z czerwonawa zrenica, uniesione brwi, maly pysk
T = {'w': (228, 222, 196), 'W': (190, 184, 156), 'r': (150, 34, 30), 'k': (22, 12, 10), 'b': (60, 70, 26),
     'a': (236, 172, 40), 'A': (255, 214, 110)}
NAKLEJKI = {
    ('glowa', 'north'): (['kk..kk', 'aA..Aa', 'kk..kk', '......', '......'], T),  # bursztynowe szparki pod brwia (W1)
    ('pysk', 'north'): (['...', '.k.'], T),
    ('luk_chwyt', 'west'): (['..', 'kk', '..'], T), ('luk_chwyt', 'east'): (['..', 'kk', '..'], T),
    ('jagoda_1', 'north'): (['w.', '..'], {'w': (150, 120, 170)}), ('jagoda_3', 'west'): (['w.', '..'], {'w': (150, 120, 170)}),
}

Z0 = (0, 0, 0)
UKRYTA, PELNA = (0, 0, 0), (1, 1, 1)
LUK_SPOCZ = (30, 0, -40)    # luk z przodu i przechylony na zewnatrz (zmierzone bb_obrys: x -10..-3, poza nogami)
REKA_L_SPOCZ = (65, 0, -15)  # reka z lukiem odsunieta w bok - luk poza sylwetka tulowia i nog
BIODRA, NOGI = 7, 80      # smierc: opadniecie bioder i uniesienie nog do poziomu (na ziemi)
PRZYSIAD, LADOWANIE = 3.9, 2.8  # zmierzone bb_obrys.py: stopy 2.3/1.4 px nad ziemia przy poprzednim -2/-1.4  # lewa reka lekko wysunieta: poza bazowa we wszystkich animacjach
ROLKA = (0, 0, -18)  # obrot luku wokol wlasnej osi przy naciagu: plaszczyzna cieciwy przez dlon
# cieciwa przy pelnym naciagu: kat i skala odcinka gornego/dolnego; zmierzone bb_cieciwa.py (2026-09-26, tulow -12):
# szczyty 0.9-1.7 px od dloni w pelnym naciagu, 2-4.4 px w polowie
CG, SG, CD, SD = 62, 2.05, 33, 2.8


def _drg_x(rot, dx):
    return (rot[0] + dx, rot[1], rot[2])


def _drg(rot, dz):
    return (rot[0], rot[1], rot[2] + dz)


def _cieciwa(znak, kat, skala, t0=0.3, t1=0.95, n=5):
    """Klatki odcinka cieciwy: wierzcholek 'V' jedzie PROSTO od srodka luku (spoczynek) do punktu pelnego naciagu,
    a kat i dlugosc odcinka liczone sa z tej pozycji w kazdej klatce (niezalezna interpolacja kata i skali dawala
    'X' w polowie naciagu). Po strzale drganie i powrot."""
    import math
    L = 8.0
    wzd, pop = L * skala * math.cos(math.radians(kat)), L * skala * math.sin(math.radians(kat))
    rot, sc = [(0, Z0), (t0, Z0)], [(0, PELNA), (t0, PELNA)]
    for k in range(1, n + 1):
        f = k / n
        a, p = L + f * (wzd - L), f * pop
        t = t0 + f * (t1 - t0)
        rot.append((t, (znak * math.degrees(math.atan2(p, a)), 0, 0)))
        sc.append((t, (1, 1, round(math.hypot(a, p) / L, 3))))
    rot += [(0.98, (-znak * 8, 0, 0)), (1.03, (znak * 4, 0, 0)), (1.08, Z0)]
    sc += [(0.98, PELNA)]
    return {'rotation': rot, 'scale': sc}


def _strzala_ukryta(anim):
    for n, a in anim.items():
        if n != 'shoot':
            a['kosci'].setdefault('fx_strzala', {})['scale'] = [(0, UKRYTA), (a['dlugosc'], UKRYTA)]
        if n != 'death':
            a['kosci'].setdefault('fx_luk_ziemia', {})['scale'] = [(0, UKRYTA), (a['dlugosc'], UKRYTA)]
    return anim


def animacje():
    # chod: uda i golenie w tych samych klatkach; stopa = -(udo + golen), zeby lezala plasko (palce nie wbijaja sie)
    T = [0, 0.175, 0.35, 0.525, 0.7]
    UDO = {'l': [-25, 0, 25, 0, -25], 'p': [25, 0, -25, 0, 25]}
    GOLEN = {'l': [-30, -45, 0, -10, -30], 'p': [0, -10, -30, -45, 0]}
    walk = {
        'tulow': {'position': [(0, Z0), (0.175, (0, 0.8, 0)), (0.35, Z0), (0.525, (0, 0.8, 0)), (0.7, Z0)],
                  'rotation': [(0, (0, 5, 0)), (0.35, (0, -5, 0)), (0.7, (0, 5, 0))]},
        'ramie_l': {'rotation': [(0, _drg_x(REKA_L_SPOCZ, 7)), (0.35, _drg_x(REKA_L_SPOCZ, -7)), (0.7, _drg_x(REKA_L_SPOCZ, 7))]},
        'ramie_p': {'rotation': [(0, (-10, 0, 5)), (0.35, (15, 0, 5)), (0.7, (-10, 0, 5))]},
        'luk': {'rotation': [(0, LUK_SPOCZ), (0.7, LUK_SPOCZ)]},
    }
    for st in ('l', 'p'):
        walk[f'udo_{st}'] = {'rotation': [(t, (u, 0, 0)) for t, u in zip(T, UDO[st])]}
        walk[f'golen_{st}'] = {'rotation': [(t, (g, 0, 0)) for t, g in zip(T, GOLEN[st])]}
        walk[f'stopa_{st}'] = {'rotation': [(t, (-(u + g), 0, 0)) for t, u, g in zip(T, UDO[st], GOLEN[st])]}
    anim = {
        'idle': {'petla': 'loop', 'dlugosc': 2.0, 'kosci': {
            'tulow': {'position': [(0, Z0), (1.0, (0, -0.4, 0)), (2.0, Z0)]},
            'hi_glowa': {'rotation': [(0, Z0), (0.3, Z0), (0.38, (0, 25, 5)), (0.9, (0, 25, 5)), (0.98, (-8, -15, 0)),
                                      (1.5, (-8, -15, 0)), (1.58, Z0), (2.0, Z0)]},
            'ucho_l': {'rotation': [(0, Z0), (0.36, Z0), (0.4, (0, 0, 15)), (0.5, Z0), (2.0, Z0)]},
            'ucho_p': {'rotation': [(0, Z0), (0.96, Z0), (1.0, (0, 0, -15)), (1.1, Z0), (2.0, Z0)]},
            'ramie_l': {'rotation': [(0, REKA_L_SPOCZ), (1.0, _drg_x(REKA_L_SPOCZ, 4)), (2.0, REKA_L_SPOCZ)]},
            'ramie_p': {'rotation': [(0, (5, 0, 5)), (1.0, (9, 0, 5)), (2.0, (5, 0, 5))]},
            'luk': {'rotation': [(0, LUK_SPOCZ), (2.0, LUK_SPOCZ)]}}},
        'walk': {'petla': 'loop', 'dlugosc': 0.7, 'kosci': walk},
        'shoot': {'petla': 'once', 'dlugosc': 1.3, 'kosci': {
            # 0-0.25 staje bokiem (lewy bark do celu) i unosi luk; 0.25-0.95 naciag z drzeniem; 0.95 strzal + odrzut
            'tulow': {'rotation': [(0, Z0), (0.25, (0, -35, 0)), (0.95, (0, -38, 0)), (1.0, (-4, -32, 0)), (1.3, Z0)]},
            'hi_glowa': {'rotation': [(0, Z0), (0.25, (-5, 30, 0)), (0.62, (-5, 30, 0)), (0.7, (-5, 32, 3)), (0.78, (-5, 28, -3)),
                                      (0.86, (-5, 32, 3)), (0.95, (-5, 30, 0)), (1.3, Z0)]},
            # wykrok +-12: przy sztywnym biodrze wiekszy rozkrok obniza kostki pod ziemie (bramka ziemi)
            'udo_l': {'rotation': [(0, Z0), (0.12, (-6, 0, 0)), (0.25, (-12, 0, 0)), (1.05, (-12, 0, 0)), (1.3, Z0)]},
            'udo_p': {'rotation': [(0, Z0), (0.25, (12, 0, 0)), (1.05, (12, 0, 0)), (1.3, Z0)]},
            # wykrok: stopy plasko (stopa = -udo)
            'golen_l': {'rotation': [(0, Z0), (0.12, (-8, 0, 0)), (0.25, (-16, 0, 0)), (1.05, (-16, 0, 0)), (1.3, Z0)]},  # ugiete kolano tylnej nogi
            'stopa_l': {'rotation': [(0, Z0), (0.12, (14, 0, 0)), (0.25, (28, 0, 0)), (1.05, (28, 0, 0)), (1.3, Z0)]},
            'stopa_p': {'rotation': [(0, Z0), (0.25, (-12, 0, 0)), (1.05, (-12, 0, 0)), (1.3, Z0)]},
            'ramie_l': {'rotation': [(0, REKA_L_SPOCZ), (0.12, (70, 20, 0)), (0.25, (104, 35, 0)), (0.6, (104, 35, 0)), (0.7, (104, 35, 1)), (0.8, (104, 35, -1)),
                                     (0.9, (104, 35, 1)), (0.95, (104, 35, 0)), (1.0, (94, 35, 0)), (1.1, (104, 35, 0)), (1.3, REKA_L_SPOCZ)]},
            # luk obraca sie do pionu dopiero, gdy reka jest juz uniesiona (inaczej koniec luku zahacza o ziemie)
            # luk wyprowadzony przed cialo (55 -> 0 przy unoszeniu reki: suma katow stala ~pion, bez wirowania)
            'luk': {'rotation': [(0, LUK_SPOCZ), (0.25, ROLKA), (0.62, ROLKA), (0.69, _drg(ROLKA, 4)), (0.76, _drg(ROLKA, -4)),
                                 (0.83, _drg(ROLKA, 4)), (0.9, _drg(ROLKA, -4)), (0.95, ROLKA), (1.05, ROLKA), (1.3, LUK_SPOCZ)]},
            # ramiona luku uginaja sie do srodka przy pelnym naciagu
            'luk_g': {'rotation': [(0, Z0), (0.3, Z0), (0.95, (-14, 0, 0)), (0.99, Z0), (1.3, Z0)]},
            'luk_d': {'rotation': [(0, Z0), (0.3, Z0), (0.95, (14, 0, 0)), (0.99, Z0), (1.3, Z0)]},
            # cieciwa: odcinki od koncow luku schodza sie w punkt zaczepu (V), po strzale drga
            # katy/skale policzone z geometrii pozy (dlon przy policzku blizej gornego konca luku) i sprawdzone pomiarem
            'cieciwa_g': _cieciwa(-1, CG, SG),
            'cieciwa_d': _cieciwa(1, CD, SD),
            # strzala na luku: cofa sie razem z cieciwa (lokalne +y), znika w chwili strzalu
            'fx_strzala': {'scale': [(0, UKRYTA), (0.2, UKRYTA), (0.3, PELNA), (0.95, PELNA), (0.97, UKRYTA), (1.3, UKRYTA)],
                        'position': [(0, Z0), (0.3, Z0), (0.95, (0, 6, 0)), (0.97, Z0)]},
            # prawa reka: 0.3 chwyta cieciwe przy luku (wyprostowana), do 0.95 ciagnie ja do policzka razem z wierzcholkiem 'V'
            'ramie_p': {'rotation': [(0, Z0), (0.3, (95, 50, 0)), (0.8, (95, 65, 0)), (0.95, (95, 88, 0)), (1.0, (95, 70, 0)), (1.3, Z0)]},
            'przedramie_p': {'rotation': [(0, Z0), (0.3, (5, 0, 0)), (0.625, (40, 0, 0)), (0.8, (95, 0, 0)), (0.95, (125, 0, 0)), (1.0, (40, 0, 0)), (1.3, Z0)]}}},
        'leap_back': {'petla': 'once', 'dlugosc': 0.6, 'kosci': {
            # przysiad (kolana do przodu, goleni w tyl) -> wybicie z podkulonymi nogami -> amortyzacja; korzen stoi (skok robi MM)
            # opuszczenie korzenia = zmierzone uniesienie stop przy zgietych nogach (stopy dotykaja ziemi); w locie 0
            'cierniowy_lucznik': {'position': [(0, Z0), (0.06, (0, -PRZYSIAD * 0.3, 0)), (0.12, (0, -PRZYSIAD, 0)), (0.2, (0, -PRZYSIAD, 0)), (0.3, Z0),
                                               (0.45, (0, -LADOWANIE, 0)), (0.52, (0, -LADOWANIE * 0.1, 0)), (0.6, Z0)]},
            'tulow': {'rotation': [(0, Z0), (0.12, (-20, 0, 0)), (0.3, (15, 0, 0)), (0.45, (-10, 0, 0)), (0.6, Z0)],
                      'scale': [(0, PELNA), (0.45, (1.05, 0.95, 1.05)), (0.55, PELNA)]},
            'stopa_l': {'rotation': [(0, Z0), (0.12, (30, 0, 0)), (0.2, (30, 0, 0)), (0.3, Z0), (0.45, (20, 0, 0)), (0.6, Z0)]},
            'stopa_p': {'rotation': [(0, Z0), (0.12, (30, 0, 0)), (0.2, (30, 0, 0)), (0.3, Z0), (0.45, (20, 0, 0)), (0.6, Z0)]},
            'udo_l': {'rotation': [(0, Z0), (0.12, (40, 0, 0)), (0.2, (40, 0, 0)), (0.3, (50, 0, 0)), (0.45, (30, 0, 0)), (0.6, Z0)]},
            'udo_p': {'rotation': [(0, Z0), (0.12, (40, 0, 0)), (0.2, (40, 0, 0)), (0.3, (50, 0, 0)), (0.45, (30, 0, 0)), (0.6, Z0)]},
            'golen_l': {'rotation': [(0, Z0), (0.12, (-70, 0, 0)), (0.2, (-70, 0, 0)), (0.3, (-80, 0, 0)), (0.45, (-50, 0, 0)), (0.6, Z0)]},
            'golen_p': {'rotation': [(0, Z0), (0.12, (-70, 0, 0)), (0.2, (-70, 0, 0)), (0.3, (-80, 0, 0)), (0.45, (-50, 0, 0)), (0.6, Z0)]},
            'ucho_l': {'rotation': [(0, Z0), (0.1, (-40, 0, 0)), (0.5, (-40, 0, 0)), (0.6, Z0)]},
            'ucho_p': {'rotation': [(0, Z0), (0.1, (-40, 0, 0)), (0.5, (-40, 0, 0)), (0.6, Z0)]},
            # w przysiadzie reka z lukiem unosi sie do przodu (luk nie dotyka ziemi)
            'ramie_l': {'rotation': [(0, REKA_L_SPOCZ), (0.12, _drg_x(REKA_L_SPOCZ, 35)), (0.45, _drg_x(REKA_L_SPOCZ, 35)), (0.6, REKA_L_SPOCZ)]},
            'ramie_p': {'rotation': [(0, Z0), (0.2, (30, 0, 40)), (0.6, Z0)]},  # rownowaga przy odskoku (w bok i w tyl)
            # w przysiadzie luk poziomieje (dolny koniec w gore) - inaczej wbija sie w ziemie
            'luk': {'rotation': [(0, LUK_SPOCZ), (0.12, (0, 0, 0)), (0.45, (0, 0, 0)), (0.6, LUK_SPOCZ)]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.3, 'kosci': {
            'tulow': {'rotation': [(0, Z0), (0.08, (18, 0, -6)), (0.3, Z0)]},
            'hi_glowa': {'rotation': [(0, Z0), (0.08, (15, 10, 0)), (0.3, Z0)]},
            'ramie_l': {'rotation': [(0, REKA_L_SPOCZ), (0.08, _drg_x(REKA_L_SPOCZ, 15)), (0.3, REKA_L_SPOCZ)]},
            'ucho_l': {'rotation': [(0, Z0), (0.06, (-40, 0, 0)), (0.3, Z0)]},
            'ucho_p': {'rotation': [(0, Z0), (0.06, (-40, 0, 0)), (0.3, Z0)]},
            'luk': {'rotation': [(0, LUK_SPOCZ), (0.3, LUK_SPOCZ)]}}},
        'death': {'petla': 'hold', 'dlugosc': 1.2, 'kosci': {
            # pada na plecy: biodra opadaja do ziemi, tulow kladzie sie do tylu, nogi wyprostowane przed soba na ziemi,
            # luk wypada z dloni i kladzie sie plasko obok (bez sterczenia i bez wchodzenia w ziemie)
            'cierniowy_lucznik': {'position': [(0, Z0), (0.3, (0, -1, 0)), (0.8, (0, -BIODRA, 0)), (1.2, (0, -BIODRA, 0))]},
            'tulow': {'rotation': [(0, Z0), (0.3, (-12, 0, 0)), (0.8, (80, 0, 8)), (0.9, (74, 0, 8)), (1.0, (80, 0, 8)), (1.2, (80, 0, 8))]},
            'udo_l': {'rotation': [(0, Z0), (0.8, (NOGI, 0, -8)), (1.2, (NOGI, 0, -8))]},
            'udo_p': {'rotation': [(0, Z0), (0.8, (NOGI, 0, 8)), (1.2, (NOGI, 0, 8))]},
            'golen_l': {'rotation': [(0, Z0), (0.8, (22.5, 0, 0)), (1.2, (22.5, 0, 0))]},
            'golen_p': {'rotation': [(0, Z0), (0.8, (22.5, 0, 0)), (1.2, (22.5, 0, 0))]},
            'ramie_l': {'rotation': [(0, REKA_L_SPOCZ), (0.8, (0, 0, -80)), (1.2, (0, 0, -80))]},
            'ramie_p': {'rotation': [(0, Z0), (0.8, (0, 0, 80)), (1.2, (0, 0, 80))]},
            # luk wypada z dloni na poczatku upadku i od razu lezy plasko obok
            'luk': {'rotation': [(0, LUK_SPOCZ), (1.2, LUK_SPOCZ)],
                    'scale': [(0, PELNA), (0.12, PELNA), (0.14, UKRYTA), (1.2, UKRYTA)]},
            'fx_luk_ziemia': {'scale': [(0, UKRYTA), (0.12, UKRYTA), (0.14, PELNA), (1.2, PELNA)]},
            'ucho_l': {'rotation': [(0, Z0), (0.8, (-40, 0, 0)), (1.2, (-40, 0, 0))]},
            'ucho_p': {'rotation': [(0, Z0), (0.8, (-40, 0, 0)), (1.2, (-40, 0, 0))]}}},
    }
    return _strzala_ukryta(anim)


# wyglad W1 (wybor wlasciciela 2026-09-26): ciemna, czerwonobrazowa kora pedow jezyny, jasne kolce, bursztynowe szparki
PED = [(44, 26, 22), (62, 38, 30), (82, 52, 40), (102, 68, 50), (120, 82, 60)]
MATERIALY = {
    'ped': {'ramp': PED, 'wzor': 'pedy', 'szum': 0.1, 'jasnosc': 0.18},
    'ped_ciemny': {'ramp': [(32, 18, 16)] + PED[:-1], 'wzor': 'pedy', 'szum': 0.1, 'jasnosc': 0.18},
    'ped_czerw': {'ramp': [(74, 30, 28), (98, 42, 36), (122, 56, 44), (142, 72, 54)], 'wzor': 'pedy', 'szum': 0.1, 'jasnosc': 0.18},
    'kolec': {'ramp': [(196, 180, 136), (216, 202, 160), (234, 222, 184)], 'szum': 0.06},
    'jagoda': {'ramp': [(120, 20, 24), (170, 36, 34), (210, 70, 60)], 'szum': 0.15},   # niedojrzale, czerwone (W1)
    'lisc': {'ramp': [(52, 88, 22), (76, 118, 30), (104, 148, 44), (136, 176, 62)], 'alfa': 'lisc', 'szum': 0.08},
    'luk': {'ramp': [(96, 70, 40), (124, 94, 56), (150, 118, 72), (172, 140, 90)], 'wzor': 'kora', 'szum': 0.08},
    'owijka': {'ramp': [(120, 104, 70), (150, 134, 96), (176, 160, 120)], 'wzor': 'pedy', 'szum': 0.06},
    'cieciwa': {'ramp': [(74, 62, 48), (92, 80, 62)], 'szum': 0.03, 'gradient': 0},  # ciemniejsza od luku
    'grot': {'ramp': [(170, 150, 110), (200, 182, 142), (226, 210, 172)], 'szum': 0.05},
    'szpon': {'ramp': [(30, 24, 14), (48, 40, 22)], 'szum': 0.05},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n.startswith('cieciwa'):
        return 'cieciwa'
    if n.startswith('kolec') or n == 'strzala':
        return 'kolec'
    if n == 'strzala_grot':
        return 'grot'
    if n.startswith('jagoda'):
        return 'jagoda'
    if n.startswith(('lisc', 'ucho')):
        return 'lisc'
    if n == 'luk_chwyt':
        return 'owijka'
    if n.startswith('luk'):
        return 'luk'
    if n.startswith(('szpon', 'palec')):
        return 'szpon'
    if n.startswith(('ped_', 'wlos')):
        return 'ped_czerw'
    if n.startswith(('golen', 'stopa', 'przedramie', 'miednica')):
        return 'ped_ciemny'
    return 'ped'


def main():
    outliner = zbuduj()
    anim = animacje()
    waliduj({k for a in anim.values() for k in a['kosci']})
    tex, _ = tekstura.atlas(ELEMENTY, MATERIALY, material, ziarno=23, naklejki=NAKLEJKI)
    zapisz(sys.argv[1], 'cierniowy_lucznik', outliner, tex, 128, A.zbuduj(anim, KOSCI, do_me=True))


if __name__ == '__main__':
    main()
