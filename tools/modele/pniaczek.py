"""Pniaczek (Loch 001 / wyspa, rola: roj - maly, szybki, atakuje stadem). Baza: Korzeniak W1 (wlasciciel 2026-09-26:
"fajne sa, podobaja mi sie" - trzy korzeniaki jako trzy rozne moby).

Czym sie rozni od Korzeniaka (W2, strefa korzeni) i Korowca (W3, ciezki): dwoje zielonych, swiecacych oczu
(`swiatlo_oczy`) zamiast czerwonego oka, cztery pajecze korzenie, szybkie dreptanie, zlosliwy "gremlin".
Poprawki techniczne: siatka 1:1 (bylo kilkanascie kostek 0.3-0.8 px), twarz przypieta do korpusu (na czubku slizgala
sie po korze przy kiwaniu), palce i stopy nad ziemia, pusty srodek pnia przykryty (widac go bylo przy death).
Nowe animacje: walk (0.45 s, pajecze dreptanie), death (przewraca sie na plecy i przebiera nogami).
Uzycie: tools/modele/zbuduj.sh pniaczek
"""
import sys

import z_pliku as Z
from bb import kosc as B, szescian as S

BAZA = '/opt/docker/mc/data/world1/output/lochy/modele-wlasne/_archiwum/korzeniak_wersje/korzeniak_w1.bbmodel'
POD_ZIEMIA = {'spawn:20'}
TWARZ = ('oczodol_l', 'oczodol_p', 'oko_l', 'oko_p', 'rysa_1', 'paszcza', 'zab_1', 'zab_2')


def przebuduj():
    Z.wczytaj(BAZA)
    Z.przenies(TWARZ, 'korpus')                                   # twarz nie slizga sie z kiwajacym czubkiem
    Z.usun('sek_oko', 'mech_krawedz', 'rysa_1')
    Z.do_siatki(pomin=('hitbox_box', 'shadow_box'))
    # oczy duze i swiecace, w osobnej kosci (MM: brightness); oczodoly glebsze
    Z.zmien('oko_l', od=(-3, 8.5, -5), do=(-1, 10.5, -4))
    Z.zmien('oko_p', od=(1, 9.5, -5), do=(3, 11.5, -4))
    Z.zmien('oczodol_l', od=(-3.5, 8, -4.5), do=(-0.5, 11, -3.5))
    Z.zmien('oczodol_p', od=(0.5, 9, -4.5), do=(3.5, 12, -3.5))
    oczy = [Z.element('oko_l')['uuid'], Z.element('oko_p')['uuid']]
    korpus = Z.kosc('korpus')
    korpus['children'] = [c for c in korpus['children'] if c not in oczy]
    korpus['children'].append(B('swiatlo_oczy', (0, 10, -4), oczy))
    Z.zmien('paszcza', od=(-2, 6, -4.5), do=(2, 7.5, -3.5))
    Z.zmien('zab_1', od=(-1.5, 6.5, -5.5), do=(-0.5, 7.5, -4.5))
    Z.zmien('zab_2', od=(0.5, 6, -5.5), do=(1.5, 7, -4.5))
    # rdzen (pusty srodek) -> pelna czapa slojow
    Z.zmien('rdzen', od=(-4, 12, -3), do=(3, 13, 3.5))
    # palce i stopy nad ziemia
    for st in ('l', 'p'):
        for j in range(3):
            e = Z.element(f'palec_{st}{j}')
            e['from'][1], e['to'][1] = 1.5, 3.5
    for i in range(4):
        Z.zmien(f'stopa_{i}', przesun=(0, 0.5, 0))
        Z.zmien(f'koniec_{i}', przesun=(0, 0.5, 0))


Z0, P1, ZERO = (0, 0, 0), (1, 1, 1), (0, 0, 0)


def nowe_animacje():
    T = [0, 0.1125, 0.225, 0.3375, 0.45]
    walk = {'korpus': {'rotation': [(0, (0, 0, -4)), (0.1125, (3, 0, 0)), (0.225, (0, 0, 4)), (0.3375, (3, 0, 0)), (0.45, (0, 0, -4))],
                       'position': [(0, Z0), (0.1125, (0, 0.7, 0)), (0.225, Z0), (0.3375, (0, 0.7, 0)), (0.45, Z0)]},
            'hi_czubek': {'rotation': [(0, (0, 5, 0)), (0.225, (0, -5, 0)), (0.45, (0, 5, 0))]},
            'ramie_l': {'rotation': [(0, (20, 0, 0)), (0.225, (-10, 0, 0)), (0.45, (20, 0, 0))]},
            'ramie_p': {'rotation': [(0, (-10, 0, 0)), (0.225, (20, 0, 0)), (0.45, (-10, 0, 0))]}}
    for i, faza in ((0, 0), (3, 0), (1, 0.5), (2, 0.5)):  # pary po przekatnej, jak pajak
        # pol cyklu noga w gorze (unoszenie do 22 st. i opuszczenie), pol cyklu na ziemi
        kl = []
        for t in T:
            f = ((t / 0.45) + faza) % 1.0
            kl.append((t, (22 * (1 - abs(4 * f - 1)) if f < 0.5 else 0, 0, 0)))
        walk[f'noga_{i}'] = {'rotation': kl}
    return {
        'walk': {'petla': 'loop', 'dlugosc': 0.45, 'kosci': walk},
        'death': {'petla': 'hold', 'dlugosc': 1.4, 'kosci': {
            # przewraca sie na plecy i przebiera nogami w powietrzu, oczy gasna
            'korzen': {'rotation': [(0, Z0), (0.25, (0, 0, 15)), (0.55, (0, 0, 175)), (0.65, (0, 0, 165)), (0.75, (0, 0, 172)),
                                    (1.4, (0, 0, 172))],
                       'position': [(0, Z0), (0.25, (0, 4, 0)), (0.45, (0, PLECY + 2, 0)), (0.55, (0, PLECY, 0)), (1.4, (0, PLECY, 0))]},
            'noga_0': {'rotation': [(0, Z0), (0.6, (30, 0, 0)), (0.75, (-10, 0, 0)), (0.9, (25, 0, 0)), (1.05, (-5, 0, 0)), (1.4, (15, 0, 0))]},
            'noga_1': {'rotation': [(0, Z0), (0.6, (-10, 0, 0)), (0.75, (30, 0, 0)), (0.9, (-5, 0, 0)), (1.05, (20, 0, 0)), (1.4, (10, 0, 0))]},
            'noga_2': {'rotation': [(0, Z0), (0.6, (25, 0, 0)), (0.75, (-5, 0, 0)), (0.9, (20, 0, 0)), (1.4, (5, 0, 0))]},
            'noga_3': {'rotation': [(0, Z0), (0.6, (-5, 0, 0)), (0.75, (25, 0, 0)), (0.9, (0, 0, 0)), (1.4, (20, 0, 0))]},
            'swiatlo_oczy': {'scale': [(0, P1), (0.9, P1), (1.2, ZERO), (1.4, ZERO)]}}},
    }


PLECY = 17.5  # uniesienie korzenia po przewrocie (model do gory nogami lezy na czapie) - dobrane bramka ziemi
KORA = [(52, 48, 18), (70, 64, 24), (88, 78, 32), (106, 92, 40), (124, 108, 50), (142, 124, 62)]
MATERIALY = {
    'kora': {'ramp': KORA, 'wzor': 'kora', 'szum': 0.1, 'jasnosc': 0.05},
    'kora_jasna': {'ramp': KORA[1:] + [(156, 138, 72)], 'wzor': 'kora', 'szum': 0.1, 'jasnosc': 0.05},
    'korzen': {'ramp': KORA[:-1], 'wzor': 'kora', 'szum': 0.1},
    'czapa': {'ramp': [(90, 78, 34), (112, 98, 44), (132, 116, 56)], 'wzor': 'kora', 'szum': 0.06},
    'szczelina': {'ramp': [(22, 18, 8), (32, 28, 12)], 'szum': 0.03, 'gradient': 0},
    'oko': {'ramp': [(170, 230, 90), (214, 250, 150)], 'szum': 0.02, 'gradient': 0},
    'zab': {'ramp': [(170, 150, 96), (196, 178, 122)], 'szum': 0.03},
    'mech': {'ramp': [(56, 66, 14), (74, 86, 18), (94, 108, 26), (116, 128, 38)], 'wzor': 'mech', 'alfa': 'draperia'},
    'lisc': {'ramp': [(58, 96, 24), (86, 130, 34), (118, 162, 50), (150, 190, 70)], 'alfa': 'lisc', 'szum': 0.08},
    'palec': {'ramp': [(30, 26, 10), (48, 42, 16), (64, 56, 22)], 'szum': 0.05},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n.startswith(('oczodol', 'paszcza')):
        return 'szczelina'
    if n.startswith('oko'):
        return 'oko'
    if n.startswith('zab'):
        return 'zab'
    if n == 'rdzen':
        return 'czapa'
    if n.startswith('mech'):
        return 'mech'
    if n.startswith('lisc'):
        return 'lisc'
    if n.startswith('palec'):
        return 'palec'
    if n.startswith(('zebro', 'sek', 'drzazga', 'lodyga')):
        return 'kora_jasna'
    if n.startswith(('udo', 'przypora', 'stopa', 'koniec', 'przedramie', 'ramie')):
        return 'korzen'
    return 'kora'


def main():
    przebuduj()
    Z.zapisz_model(sys.argv[1], 'pniaczek', MATERIALY, material, animacje_nowe=nowe_animacje(), ziarno=41)


if __name__ == '__main__':
    main()
