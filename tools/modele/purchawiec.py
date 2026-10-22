"""Purchawiec (Loch 001, rola: trujacy - chmura zarodnikow + rzut purchawka). Generator modelu ME, wlasny szkielet.

KONCEPT (po 2. recenzji 2026-09-26): ciezka, stara purchawka ~1.4 bloku. Organiczna bulwa przechylona na bok
(jedna strona zapadnieta), na szczycie komin wyrzutowy z postrzepionym brzegiem. Brudna ochra i szary braz, dol
ziemisty z grzybnia; toksyczna zielen (#9BB02A) TYLKO w otworach: pionowa szczelina-paszcza (kosc `swiatlo_zarodniki`
- MM rozjasnia ja brightness), komin, pory. Dwa metne, nierowne oczka. Grube kikuty zamiast rak, trzy guzy-nogi.
Animacje: idle (oddech), walk (ciezkie przelewanie + squash przy kazdym opadnieciu), puff (0.6 s nabrzmiewania z
drzeniem = zapowiedz, komin "kicha"), throw (pocisk 5x5 wyrasta z bulwy, zamach nad glowa, rzut w 1.0 s), hurt,
spawn (wyrasta z ziemi), death (nadyma sie, peka - szczyt wylatuje, skorka opada).
Kosc `zarodniki` (bez kostek) = punkt dla czasteczek MythicMobs na szczycie komina.
Uzycie: tools/modele/zbuduj.sh purchawiec
"""
import sys

import animacje as A
import tekstura
from bb import ELEMENTY, KOSCI, kosc as B, szescian as S, waliduj, zapisz

POD_ZIEMIA = {'spawn:8'}

# warstwy bulwy: (szerokosc, y0, y1, przesuniecie x, przesuniecie z) - przechylona ku -x, asymetryczna
WARSTWY = [(12, 2, 4, 0, 0), (16, 4, 7, 0.5, 0), (18, 7, 11, 0, 0), (18, 11, 14, -0.5, 0.5),
           (16, 14, 17, -1, 0.5), (12, 17, 19, -1.5, 1), (8, 19, 21, -2, 1)]


def zbuduj():
    bulwa = [S(f'bulwa_{i + 1}', (ox - w / 2, y0, oz - w / 2), (ox + w / 2, y1, oz + w / 2), 3)
             for i, (w, y0, y1, ox, oz) in enumerate(WARSTWY)]
    bulwa += [
        S('wybrzuszenie_p', (9, 6, -4), (10, 11, 3), 3),       # jedna strona pelna...
        S('wybrzuszenie_t', (-5, 6, 9), (4, 12, 10), 3),
        # ...druga zapadnieta: brak wybrzuszenia po -x, za to faldy
        S('falda_1', (-10, 8, -3), (-9, 11, 2), 2),
    ]
    komin = [S('komin', (-4, 21, -1), (0, 23, 3), 4),
             S('komin_brzeg_1', (-4, 23, -1), (-3, 24, 0), 4), S('komin_brzeg_2', (-1, 23, 2), (0, 25, 3), 4),
             S('komin_brzeg_3', (-4, 23, 2), (-3, 24, 3), 4)]
    szczyt = B('szczyt', (-2, 19, 1), [S('bulwa_7x', (-5, 19, -2), (1, 21, 4), 3)] + komin + [B('zarodniki', (-2, 24, 1), [])])
    # swiecace wnetrze szczeliny: x 0..1 = kolumna 8 sciany north warstwy bulwa_3 (kolumna = 9 - x - 0.5 + przesuniecie)
    szczelina = B('swiatlo_zarodniki', (0.5, 9, -9), [S('szczelina', (0, 6, -9.4), (1, 12, -8.4), 5)])
    amunicja = [B('purchawka_bark', (6, 14, 3), [S('purchawka_bark', (4, 13, 1), (8, 16, 5), 4)])]
    rece = []
    for st, sx in (('l', -1), ('p', 1)):
        x = 9.5 * sx
        # kikut konczy sie 3 px nad ziemia (przy przechyle walk +-12 i squashu nie szoruje po podlozu)
        dzieci = [S(f'reka_{st}', (x - 1, 5, -1), (x + 1, 9, 1), 3),
                  S(f'reka_grzybnia_{st}', (x - 1, 4, -1.1), (x + 1, 5, -1.1), 5)]
        if sx > 0:  # pocisk w prawej rece: wyrasta 0 -> 1, znika dokladnie w chwili rzutu
            # pocisk na zewnatrz dloni i z przodu (wewnatrz bulwy byl niewidoczny)
            dzieci.append(B('fx_pocisk', (x + 2, 5, -3), [S('pocisk', (x - 0.5, 1, -5.5), (x + 4.5, 6, -0.5), 6)]))
        rece.append(B(f'reka_{st}', (x, 9, 0), dzieci, rot=(0, 0, sx * 12)))
    nogi = [B(f'guz_{i}', (x, 2, z), [S(f'guz_{i}', (x - 2.5, 0.3, z - 2.5), (x + 2.5, 3.3, z + 2.5), 2)])
            for i, (x, z) in enumerate(((-4, -3), (4, -3), (0, 4)))]
    grzybnia = [S(f'grzybnia_{i}', a, b, 5) for i, (a, b) in enumerate((
        ((-6, 0, -6.1), (-3, 3, -6.1)), ((3, 0, 6.1), (6, 3, 6.1)), ((6.1, 0, -2), (6.1, 3, 1)), ((-6.1, 0, 1), (-6.1, 3, 4))))]
    korpus = B('bulwa', (0, 2, 0), bulwa + [szczyt, szczelina] + amunicja + rece)
    hitbox = B('hitbox', (0, 16, 0), [S('hitbox_box', (-9, 0, -9), (9, 24, 9), 1)])
    cien = B('shadow', (0, 0, 0), [S('shadow_box', (-9, 0, -9), (9, 1, 9), 1)])
    return [B('purchawiec', (0, 0, 0), [korpus] + nogi + grzybnia), hitbox, cien, B('tag_name', (0, 28, 0), [])]


# kolumna 0 sciany north = strona +x modelu. Szczelina (kostka swiatlo_zarodniki) jest na x 0.5-1.5.
N = {'k': (24, 18, 12), 'K': (44, 34, 22), 'y': (155, 176, 42), 'Y': (110, 128, 24), 'e': (176, 170, 120),
     'E': (60, 52, 30), 'c': (70, 54, 34), 'p': (110, 128, 24)}
# kolumna c sciany north warstwy o szer. w i przesunieciu ox odpowiada x = w/2 + ox - c - 0.5 (kolumna 0 = strona +x).
# Szczelina (swiecaca kostka) jest w x 0..1 -> ciemna obwodka z pylem po obu stronach, oczka nad nia.
N = {'k': (24, 18, 12), 'K': (44, 34, 22), 'y': (155, 176, 42), 'Y': (110, 128, 24), 'e': (176, 170, 120),
     'E': (60, 52, 30), 'c': (70, 54, 34), 'p': (110, 128, 24)}
NAKLEJKI = {
    ('bulwa_4', 'north'): (['..................', '.....eE....E......', '......k.k.........'], N),     # szer 18, ox -0.5
    ('bulwa_3', 'north'): (['.......k.k........', '.......k.kY.......', '......yk.k........', '....p..k.k.......p'], N),  # 18, 0
    ('bulwa_2', 'north'): (['......k.k.......', '......kyk.......', '..p.............'], N),                  # 16, ox 0.5
    ('bulwa_2', 'east'): (['................', '...p........p...', '................'], N),
    ('bulwa_3', 'west'): (['..................', '....p.......p.....', '..................', '.......p..........'], N),
    ('komin', 'up'): (['.cc.', 'ckKc', 'cKyc', '.cc.'], N),
}

Z0, P1, UKRYTY = (0, 0, 0), (1, 1, 1), (0, 0, 0)


def _pocisk_ukryty(anim):
    for n, a in anim.items():
        if n != 'throw':
            a['kosci']['fx_pocisk'] = {'scale': [(0, UKRYTY), (a['dlugosc'], UKRYTY)]}
    return anim


def _drzenie(t0, t1, amp, os=2, krok=0.05):
    kl, t, zn = [], t0, 1
    while t < t1 - 1e-6:
        v = [0, 0, 0]; v[os] = zn * amp
        kl.append((round(t, 3), tuple(v))); t += krok; zn = -zn
    return kl


def animacje():
    anim = {
        'idle': {'petla': 'loop', 'dlugosc': 3.0, 'kosci': {
            'bulwa': {'scale': [(0, P1), (1.5, (1.03, 1.05, 1.03)), (3.0, P1)],
                      'rotation': [(0, (0, 0, -2)), (1.5, (0, 0, 2)), (3.0, (0, 0, -2))]},
            'reka_l': {'rotation': [(0, Z0), (1.5, (8, 0, -6)), (3.0, Z0)]},
            'reka_p': {'rotation': [(0, (6, 0, 4)), (1.5, Z0), (3.0, (6, 0, 4))]}}},
        'walk': {'petla': 'loop', 'dlugosc': 1.2, 'kosci': {
            # ciezkie przelewanie: przechyl +-12 na strone podparcia z opoznieniem, squash przy kazdym opadnieciu
            'bulwa': {'rotation': [(0, (0, 0, -12)), (0.4, (3, 0, -2)), (0.6, (0, 0, 12)), (1.0, (3, 0, 2)), (1.2, (0, 0, -12))],
                      'scale': [(0, (1.08, 0.9, 1.08)), (0.15, P1), (0.6, (1.08, 0.9, 1.08)), (0.75, P1), (1.2, (1.08, 0.9, 1.08))],
                      'position': [(0, (0, -0.5, 0)), (0.3, (0, 0.5, 0)), (0.6, (0, -0.5, 0)), (0.9, (0, 0.5, 0)), (1.2, (0, -0.5, 0))]},
            'guz_0': {'position': [(0, Z0), (0.3, (0, 2, -1)), (0.6, Z0), (1.2, Z0)]},
            'guz_1': {'position': [(0, Z0), (0.6, Z0), (0.9, (0, 2, -1)), (1.2, Z0)]},
            'guz_2': {'position': [(0, Z0), (0.45, (0, 1, 0.5)), (0.75, Z0), (1.2, Z0)]},
            'reka_l': {'rotation': [(0, (10, 0, -8)), (0.6, (-10, 0, -8)), (1.2, (10, 0, -8))]},
            'reka_p': {'rotation': [(0, (-10, 0, 8)), (0.6, (10, 0, 8)), (1.2, (-10, 0, 8))]}}},
        'puff': {'petla': 'once', 'dlugosc': 1.2, 'kosci': {
            # 0-0.6 nabrzmiewa z drzeniem (zapowiedz), 0.62 komin kicha (szczyt podskakuje), 0.7-1.2 powrot
            'bulwa': {'scale': [(0, P1), (0.6, (0.96, 1.14, 0.96)), (0.66, (1.1, 0.82, 1.1)), (0.75, (1.04, 0.92, 1.04)), (1.2, P1)],
                      'rotation': [(0, Z0)] + _drzenie(0.2, 0.6, 3) + [(0.6, Z0), (1.2, Z0)]},
            'szczyt': {'scale': [(0, P1), (0.6, (1.4, 1.2, 1.4)), (0.66, (0.9, 1.1, 0.9)), (0.8, P1), (1.2, P1)],
                       'position': [(0, Z0), (0.6, Z0), (0.64, (0, 2, 0)), (0.75, Z0), (1.2, Z0)]},
            'reka_l': {'rotation': [(0, Z0), (0.6, (0, 0, -35)), (0.66, (0, 0, -8)), (1.2, Z0)]},
            'reka_p': {'rotation': [(0, Z0), (0.6, (0, 0, 35)), (0.66, (0, 0, 8)), (1.2, Z0)]}}},
        'throw': {'petla': 'once', 'dlugosc': 1.4, 'kosci': {
            # 0-0.3 pocisk wyrasta w dloni, 0.3-0.85 zamach nad bulwe, 1.0 rzut (pocisk znika), powrot
            'fx_pocisk': {'scale': [(0, UKRYTY), (0.3, P1), (0.85, (1.15, 1.15, 1.15)), (0.99, (1.15, 1.15, 1.15)), (1.0, UKRYTY), (1.4, UKRYTY)]},
            # zamach bokiem i w gore (przez dol pocisk zahaczal o ziemie)
            'reka_p': {'rotation': [(0, Z0), (0.3, (20, 0, 30)), (0.55, (-40, 0, 70)), (0.85, (-150, 0, 40)), (1.0, (60, 0, 10)), (1.4, Z0)]},
            'bulwa': {'rotation': [(0, Z0), (0.85, (10, -20, 0)), (1.0, (-12, 15, 0)), (1.4, Z0)],
                      'scale': [(0, P1), (0.85, (1.02, 1.05, 1.02)), (1.0, (1.06, 0.92, 1.06)), (1.4, P1)]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.3, 'kosci': {
            'bulwa': {'scale': [(0, P1), (0.06, (1.1, 0.9, 1.1)), (0.15, (0.95, 1.06, 0.95)), (0.3, P1)],
                      'rotation': [(0, Z0), (0.06, (10, 0, 4)), (0.3, Z0)]},
            'szczyt': {'position': [(0, Z0), (0.06, (0, 1, 0)), (0.2, Z0)]}}},
        'spawn': {'petla': 'once', 'dlugosc': 1.2, 'kosci': {
            'purchawiec': {'position': [(0, (0, -8, 0)), (0.8, (0, -1, 0)), (1.0, (0, 0.3, 0)), (1.2, Z0)]},
            'bulwa': {'scale': [(0, (0.4, 0.4, 0.4)), (0.8, (0.9, 0.9, 0.9)), (1.0, (1.08, 1.1, 1.08)), (1.2, P1)]}}},
        'death': {'petla': 'hold', 'dlugosc': 1.3, 'kosci': {
            # nadyma sie (0.2), peka: szczyt z kominem wylatuje w gore i znika, pusta skorka opada i marszczy sie
            'bulwa': {'scale': [(0, P1), (0.2, (1.2, 1.2, 1.2)), (0.28, (1.15, 0.9, 1.15)), (0.9, (1.15, 0.55, 1.15)), (1.3, (1.15, 0.55, 1.15))],
                      'rotation': [(0, Z0), (0.3, (0, 0, 6)), (0.9, (0, 0, -3)), (1.3, (0, 0, -3))]},
            'szczyt': {'position': [(0, Z0), (0.22, Z0), (0.5, (0, 10, 0)), (1.3, (0, 10, 0))],
                       'scale': [(0, P1), (0.22, P1), (0.5, (0.6, 0.6, 0.6)), (0.55, UKRYTY), (1.3, UKRYTY)]},
            'reka_l': {'rotation': [(0, Z0), (0.9, (0, 0, -40)), (1.3, (0, 0, -40))]},
            'reka_p': {'rotation': [(0, Z0), (0.9, (0, 0, 40)), (1.3, (0, 0, 40))]}}},
    }
    return _pocisk_ukryty(anim)


# brudna ochra -> szary braz; kazda warstwa oswietlona jako czesc JEDNEJ bryly (ciemny dol, jasniejszy szczyt)
_B = [(62, 48, 32), (82, 64, 42), (104, 82, 54), (124, 100, 68), (142, 116, 80), (158, 132, 92), (170, 146, 104)]
MATERIALY = {
    **{f'bulwa_{i}': {'ramp': _B[max(0, i - 2):i + 1], 'wzor': 'brodawki', 'szum': 0.08, 'gradient': 0.15} for i in range(1, 8)},
    'bulwa_7x': {'ramp': _B[4:], 'wzor': 'brodawki', 'szum': 0.08, 'gradient': 0.15},
    'wybrzuszenie': {'ramp': _B[1:4], 'wzor': 'brodawki', 'szum': 0.08, 'gradient': 0.2},
    'komin': {'ramp': [(58, 44, 28), (80, 62, 40), (104, 82, 54)], 'szum': 0.08},
    'szczelina': {'ramp': [(140, 160, 36), (155, 176, 42)], 'szum': 0.03, 'gradient': 0},
    'purchawka': {'ramp': [(78, 60, 38), (100, 78, 50), (122, 98, 64)], 'wzor': 'brodawki', 'szum': 0.1},
    'pocisk': {'ramp': [(70, 56, 34), (96, 78, 44), (120, 100, 58)], 'wzor': 'brodawki', 'szum': 0.1},
    'reka': {'ramp': [(72, 56, 36), (96, 76, 50), (118, 96, 64)], 'szum': 0.08},
    'grzybnia': {'ramp': [(150, 144, 124), (176, 170, 150), (200, 196, 178)], 'alfa': 'draperia', 'szum': 0.05, 'gradient': 0.3},
    'guz': {'ramp': [(56, 44, 28), (74, 58, 38), (92, 74, 48)], 'wzor': 'brodawki', 'szum': 0.08},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n.startswith('bulwa_'):
        return n
    if n.startswith(('wybrzuszenie', 'falda')):
        return 'wybrzuszenie'
    if n.startswith('komin'):
        return 'komin'
    if n == 'szczelina':
        return 'szczelina'
    if n.startswith(('reka_grzybnia', 'grzybnia')):
        return 'grzybnia'
    if n.startswith('reka'):
        return 'reka'
    if n.startswith('purchawka'):
        return 'purchawka'
    if n == 'pocisk':
        return 'pocisk'
    return 'guz'


def main():
    outliner = zbuduj()
    anim = animacje()
    waliduj({k for a in anim.values() for k in a['kosci']})
    tex, _ = tekstura.atlas(ELEMENTY, MATERIALY, material, ziarno=11, naklejki=NAKLEJKI)
    zapisz(sys.argv[1], 'purchawiec', outliner, tex, 128, A.zbuduj(anim, KOSCI, do_me=True))


if __name__ == '__main__':
    main()
