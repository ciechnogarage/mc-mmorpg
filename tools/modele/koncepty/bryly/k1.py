"""K1 Straznik - bryla wg szkicu (koncepty_v1): 7.5 bl z korona, garb, maska brzozowa nisko przed barkami, serce w klatce
zeber, lewa reka-maczuga do ziemi, prawa chuda z palcami-korzeniami, szeroka korona zwiedlej sakury. Przod = -Z."""
import sys
from wspolne import S, B, zapisz, konar, pasmo, kepa

nogi = []
for s in (-1, 1):
    x0 = 9 * s
    nogi.append(B(f'noga{s}', (x0 * 1.3, 30, 2), [
        S(f'udo{s}', (min(x0, x0 + 13 * s), 16, -5), (max(x0, x0 + 13 * s), 32, 9), 2),
        S(f'golen{s}', (min(x0 + s, x0 + 14 * s), 5, -6), (max(x0 + s, x0 + 14 * s), 18, 8), 2),
        S(f'stopa{s}', (min(x0 - s, x0 + 17 * s), 0, -13), (max(x0 - s, x0 + 17 * s), 6, 9), 1),
        S(f'palec{s}a', (min(x0 + 2 * s, x0 + 6 * s), 0, -19), (max(x0 + 2 * s, x0 + 6 * s), 3, -13), 1),
        S(f'palec{s}b', (min(x0 + 10 * s, x0 + 14 * s), 0, -20), (max(x0 + 10 * s, x0 + 14 * s), 3, -13), 1),
    ], rot=(0, 0, 6 * s)))

reka_l = B('bark_l', (-24, 88, 4), [
    S('ramie_l', (-36, 60, -4), (-24, 90, 10), 2),
    S('lokiec_l', (-38, 54, -6), (-24, 64, 10), 2),
    B('przedr_l', (-31, 58, 2), [
        S('przedr_l1', (-38, 34, -6), (-24, 58, 10), 2),
        S('przedr_l2', (-41, 20, -9), (-21, 36, 13), 2),        # maczuga: grubieje ku piesci
        S('piesc_l', (-44, 6, -12), (-18, 21, 14), 1),
        S('klykiec_l1', (-44, 6, -18), (-38, 12, -12), 1), S('klykiec_l2', (-35, 6, -19), (-29, 12, -12), 1),
        S('klykiec_l3', (-26, 6, -18), (-20, 12, -12), 1),
        pasmo(-40, 50, -6.2, 18, 3), pasmo(-34, 44, -6.2, 12, 2),
    ], rot=(4, 0, -4)),
], rot=(27, 0, -10))
reka_p = B('bark_p', (24, 86, 4), [
    S('ramie_p', (22, 60, -2), (32, 88, 8), 2),
    B('przedr_p', (27, 60, 3), [
        S('przedr_p1', (22, 26, -2), (32, 60, 8), 2),
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
    S('miednica', (-16, 26, -8), (16, 38, 11), 2),
    S('talia', (-12, 36, -7), (12, 48, 9), 2),
    S('piers', (-21, 46, -12), (21, 72, 15), 2),
    S('brzuch_przod', (-15, 38, -11), (15, 48, -6), 2), S('piers_luk_l', (-22, 60, -14), (-12, 72, -8), 2), S('piers_luk_p', (12, 60, -14), (22, 72, -8), 2),
    S('garb', (-25, 68, -6), (25, 90, 20), 2),
    S('kark', (-19, 84, 6), (19, 98, 24), 2), S('kark_grzbiet', (-12, 94, 10), (12, 100, 22), 3),
    S('bark_masa_p', (20, 72, -6), (31, 90, 14), 2),
    S('garb_szczyt', (-18, 88, -2), (18, 96, 16), 3),        # swiatlo na garbie
    S('bark_l_masa', (-36, 74, -8), (-20, 96, 16), 2), S('kamien', (-34, 90, -2), (-24, 98, 8), 3),
    # klatka zeber z sercem
    S('wneka', (-10, 48, -13), (10, 66, -11), 1),
    S('serce', (-5, 52, -14), (5, 62, -10), 5),
    S('zebro1', (-12, 63, -15), (12, 65, -12), 3), S('zebro2', (-12, 58, -15), (-6, 60, -12), 3),
    S('zebro3', (6, 58, -15), (12, 60, -12), 3), S('zebro4', (-12, 52, -15), (12, 54, -12), 3),
    S('zebro5', (-11, 47, -15), (11, 49, -12), 3),
    B('leb', (0, 80, -8), [
        S('szyja', (-6, 72, -14), (6, 84, -6), 1),
        S('maska', (-8, 66, -24), (8, 86, -14), 4),
        S('broda', (-5, 62, -23), (5, 66, -15), 4),
        S('szpara', (-5, 77, -24.5), (5, 79, -23.5), 5),
        S('lenticela1', (-6, 83, -24.2), (-2, 84, -23.9), 1), S('lenticela2', (2, 70, -24.2), (6, 71, -23.9), 1),
    ], rot=(22, 0, 0)),
    reka_l, reka_p,
    pasmo(-24, 78, -8.2, 20, 3), pasmo(-18, 74, -12.2, 14, 2), pasmo(18, 72, -12.2, 16, 3), pasmo(24, 80, -8.2, 22, 2),
    # przygasle lampiony zaplatane w korone (pomysl z K2)
    S('lampion_sznur', (-15, 104, 12), (-14, 112, 13), 1), S('lampion', (-17, 98, 10), (-12, 104, 15), 5),
] + korona, rot=(-28, 0, 0))

zapisz(sys.argv[1], 'k1_straznik', nogi + [tulow])
