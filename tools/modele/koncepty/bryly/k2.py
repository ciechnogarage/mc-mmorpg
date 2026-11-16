"""K2 Jelen Gaju - bryla wg szkicu: czworonog z korzeni ~7.5 bl z porozem, dlugie cienkie nogi (tylne ze stawem skokowym),
tulow-beczka wyzej z przodu, grzywa z mchu, pysk = wydrazona kloda z jednym swiatlem, poroze z galezi sakury z 3
latarniami lochu. Przod = -Z."""
import sys
from wspolne import S, B, zapisz, konar, pasmo, kepa


def noga_przod(n, x, z):
    return B(n, (x, 50, z), [S(n + 'u', (x - 3, 26, z - 3), (x + 3, 52, z + 3), 2),
                              S(n + 'd', (x - 2, 6, z - 2), (x + 2, 28, z + 2), 1),
                              S(n + 'k', (x - 3, 0, z - 4), (x + 3, 6, z + 3), 1)], rot=(-4, 0, 0))


def noga_tyl(n, x, z):
    # udo skosem do przodu, piszczel skosem do tylu (staw skokowy), cienka nadpeciowa do ziemi
    return B(n, (x, 52, z), [S(n + 'u', (x - 4, 36, z - 4), (x + 4, 56, z + 5), 2),
                              B(n + 'b', (x, 38, z), [S(n + 'g', (x - 3, 20, z - 3), (x + 3, 40, z + 3), 2),
                                                      B(n + 'c', (x, 20, z), [S(n + 's', (x - 2, 5, z - 2), (x + 2, 22, z + 2), 1),
                                                                              S(n + 'k', (x - 3, 0, z - 4), (x + 3, 6, z + 3), 1)], rot=(-28, 0, 0))],
                                rot=(40, 0, 0))], rot=(-14, 0, 0))


def latarnia(x, y, z):
    return [S(f'sznur{x}{y}', (x - 0.5, y - 5, z - 0.5), (x + 0.5, y, z + 0.5), 1),
            S(f'lat_daszek{x}{y}', (x - 4, y - 7, z - 4), (x + 4, y - 5, z + 4), 1),
            S(f'lat{x}{y}', (x - 3, y - 16, z - 3), (x + 3, y - 7, z + 3), 5),
            S(f'lat_dno{x}{y}', (x - 4, y - 18, z - 4), (x + 4, y - 16, z + 4), 1)]


tulow = B('tulow', (0, 60, 0), [
    S('beczka', (-13, 48, -24), (13, 74, 26), 2),
    S('piers', (-14, 44, -36), (14, 80, -18), 2),
    S('klab', (-12, 50, 22), (12, 70, 32), 2),
    S('grzbiet', (-9, 74, -30), (9, 80, 20), 3),
    S('ogon', (-3, 62, 32), (3, 70, 38), 2),
] + [pasmo(-13.2, 62, z, 12 + (z % 5), 2) for z in range(-34, -14, 4)]
  + [pasmo(11.2, 62, z, 10 + (z % 3), 2) for z in range(-32, -16, 5)]
  + [pasmo(-4 + x, 48, -36.2, 10 + x % 4, 2) for x in range(0, 8, 3)], rot=(8, 0, 0))

szyja = B('szyja', (0, 72, -30), [
    S('szyja_k', (-6, 70, -38), (6, 106, -26), 2),
    S('grzywa', (-7, 76, -28), (7, 106, -22), 1),
    B('leb', (0, 112, -34), [
        S('kloda', (-8, 96, -60), (8, 110, -34), 4),               # pysk-kloda, jasny
        S('kloda_pierscien', (-7, 97, -61), (7, 109, -60), 1),      # ciemne wnetrze od czola
        S('oko_swiatlo', (-3, 100, -61.5), (3, 106, -60.5), 5),
        S('kora_pas', (-8.2, 102, -52), (8.2, 104, -40), 2),
        S('ucho_l', (-12, 106, -38), (-8, 110, -34), 2), S('ucho_p', (8, 106, -38), (12, 110, -34), 2),
        konar((-6, 110, -38), 30, 4, (-8, 0, 30), 2, 4, widly=((0, 0, 36), (10, 0, -30))),
        konar((6, 110, -38), 32, 4, (-6, 0, -32), 2, 4, widly=((8, 0, 32), (0, 0, -36))),
    ] + latarnia(-26, 136, -40) + latarnia(22, 140, -44) + latarnia(-12, 120, -52), rot=(-10, 0, 0)),
], rot=(22, 0, 0))

nogi = [noga_przod('np_l', -8, -26), noga_przod('np_p', 8, -26), noga_tyl('nt_l', -8, 20), noga_tyl('nt_p', 8, 20)]
zapisz(sys.argv[1], 'k2_jelen', nogi + [tulow, szyja])
