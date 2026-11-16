"""K3 Matka Korzeni - bryla wg szkicu: pien-tron ~2.6 bl z korzeniami rozchodzacymi sie po ziemi, z niego tulow
pol-olbrzymki, glowa z welonem z mchu do pasa (widac dwa swiatelka), galezie-rogi z glowy, dlugie rece z lokciami na
zewnatrz, dlonie z palcami-korzeniami trzymaja swiecacy kwiat Gaju przed soba. Przod = -Z."""
import sys
from wspolne import S, B, zapisz, konar, pasmo, kepa

pien = [S('pien_st', (-30, 0, -26), (30, 10, 28), 1), S('pien_sr', (-26, 10, -22), (26, 24, 24), 1),
        S('pien_gr', (-22, 24, -18), (22, 36, 20), 1), S('pien_zab_l', (-22, 36, -10), (-12, 42, 14), 1),
        S('pien_zab_p', (12, 36, -16), (20, 40, 8), 1), S('pien_kora', (-26.2, 12, -22.2), (-8, 22, -21.9), 2),
        S('pien_kora2', (6, 2, -26.2), (22, 8, -25.9), 2)]
# korzenie: grube belki od podstawy pnia promieniscie po ziemi, konce zakopane (lekki spadek)
korzenie = []
for i, (ry, dl, g) in enumerate(((20, 40, 5), (-25, 44, 5), (70, 36, 4), (-75, 38, 4), (160, 34, 4), (-150, 30, 4), (115, 28, 3), (-110, 30, 3))):
    korzenie.append(B(f'korzen{i}', (0, 0, 0), [S(f'korzen{i}k', (-g / 2, 0, -24 - dl), (g / 2, g * 0.8, -20), 1),
                                                 S(f'korzen{i}w', (-g / 2 - 1, 0, -24), (g / 2 + 1, g + 2, -18), 1)], rot=(0, ry, 0)))
kw_x, kw_y, kw_z = 0, 54, -34

cialo = B('cialo', (0, 40, 0), [
    S('biodra', (-18, 36, -14), (18, 50, 14), 2),
    S('tulow', (-16, 48, -12), (16, 76, 12), 2), S('piers', (-14, 60, -14), (14, 74, -10), 2),
    S('barki', (-24, 70, -11), (24, 82, 11), 2), S('barki_swiatlo', (-22, 80, -10), (22, 84, 10), 3),
    S('bark_mech_l', (-26, 66, -12), (-18, 74, 12), 1), S('bark_mech_p', (18, 66, -12), (26, 74, 12), 1),
    S('szyja', (-5, 82, -5), (5, 88, 5), 2),
    B('glowa', (0, 86, 0), [
        S('czaszka', (-8, 84, -9), (8, 102, 8), 2),
        S('twarz_cien', (-7, 86, -9.5), (7, 98, -9), 1),
        S('oko_l', (-5, 92, -10), (-2, 94, -9.4), 5), S('oko_p', (2, 92, -10), (5, 94, -9.4), 5),
        # welon: pasma mchu od czola do pasa, przed twarza (odstep, zeby oczy przeswitywaly)
    ] + [pasmo(x, 100, -10.5 - (abs(x) % 3) * 0.5, 30 + (x * 7) % 14, 2) for x in (-8, -6, -3, 0, 3, 6)]
      + [pasmo(x, 98, -8 + (x % 4), 22 + (x * 5) % 10, 2) for x in (-9, 8)]
      + [konar((-5, 100, 2), 24, 4, (-10, 0, 28), 2, 3, widly=((0, 0, 34), (12, 0, -26))),
         konar((5, 100, 2), 26, 4, (-8, 0, -26), 2, 3, widly=((10, 0, 30), (0, 0, -32)))], rot=(14, 0, 0)),
    # rece: ramie w bok i w dol, przedramie do przodu i do srodka, dlonie po bokach kwiatu
    B('ramie_l', (-22, 78, 0), [S('ramie_lk', (-26, 46, -4), (-18, 80, 4), 2),
                                B('przedr_l', (-22, 48, 0), [S('przedr_lk', (-26, 45, -30), (-18, 53, 0), 2),
                                                              S('dlon_l', (-16, 44, -38), (-6, 56, -28), 1)]
                                  + [S(f'palec_l{i}', (-6, 46 + i * 3, -38 + i), (0, 47 + i * 3, -37 + i), 1) for i in range(3)], rot=(0, -10, 0))],
      rot=(0, 0, -22)),
    B('ramie_p', (22, 78, 0), [S('ramie_pk', (18, 46, -4), (26, 80, 4), 2),
                               B('przedr_p', (22, 48, 0), [S('przedr_pk', (18, 45, -30), (26, 53, 0), 2),
                                                            S('dlon_p', (6, 44, -38), (16, 56, -28), 1)]
                                 + [S(f'palec_p{i}', (0, 46 + i * 3, -38 + i), (6, 47 + i * 3, -37 + i), 1) for i in range(3)], rot=(0, 10, 0))],
      rot=(0, 0, 22)),
    # swiecacy kwiat Gaju w dloniach
    S('kwiat', (kw_x - 5, kw_y - 4, kw_z - 5), (kw_x + 5, kw_y + 6, kw_z + 5), 5),
    S('kwiat_platki', (kw_x - 8, kw_y - 1, kw_z - 3), (kw_x + 8, kw_y + 3, kw_z + 3), 4), S('kwiat_platki2', (kw_x - 3, kw_y - 1, kw_z - 8), (kw_x + 3, kw_y + 3, kw_z + 8), 4),
], rot=(-10, 0, 0))

zapisz(sys.argv[1], 'k3_matka', pien + korzenie + [cialo])
