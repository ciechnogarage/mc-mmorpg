"""Purchawka (Loch 001 / wyspa, rola: maly kamikaze - podtacza sie skokami, nabrzmiewa i peka chmura zarodnikow).
Baza: Purchawiec W2 (wybor wlasciciela 2026-09-26: trzy purchawce jako trzy rozne moby).

Czym rozni sie od Purchawca (W3) i Muchomorca (W1): bez rak (sama kula na guzach), skacze zamiast chodzic,
jasna ochra zamiast brunatnej ziemi, swiecacy otwor na czubku (`swiatlo_otwor`) - zapala sie przy nabrzmiewaniu,
zmruzone oczka i szeroki usmiech-pekniecie (malowane). Animacje: idle, walk (podskoki ze squashem),
wybuch (0.8 s nabrzmiewania z drzeniem = zapowiedz, peka), hurt, spawn (wyskakuje z ziemi), death (pekniecie).
Uzycie: tools/modele/zbuduj.sh purchawka
"""
import sys

import z_pliku as Z
from bb import kosc as B, szescian as S

BAZA = '/opt/docker/mc/data/world1/output/lochy/modele-wlasne/_archiwum/purchawiec_wersje/purchawiec_w2.bbmodel'
POD_ZIEMIA = {'spawn:15'}


def przebuduj():
    Z.wczytaj(BAZA)
    Z.usun_kosc('reka_l', 'reka_p', 'purchawka_bark_p')      # bez rak i bez amunicji - to ona sama jest pociskiem
    Z.dodaj('bulwa', B('swiatlo_otwor', (0, 14, 0), [S('otwor', (-1, 14, -1), (1, 15, 1), 7)]))
    # grzybnia w osobnej kosci - znika razem z bulwa przy wybuchu/smierci
    root = Z.kosc('purchawiec')
    nitki = [c for c in root['children'] if isinstance(c, str)]
    root['children'] = [c for c in root['children'] if not isinstance(c, str)] + [B('fx_grzybnia', (0, 0, 0), nitki)]


Z0, P1, ZERO = (0, 0, 0), (1, 1, 1), (0, 0, 0)


def _drzenie(t0, t1, amp, krok=0.1):
    kl, t, zn = [], t0, 1
    while t < t1 - 1e-6:
        kl.append((round(t, 3), (0, 0, zn * amp))); t += krok; zn = -zn
    return kl


def nowe_animacje():
    return {
        'idle': {'petla': 'loop', 'dlugosc': 2.4, 'kosci': {
            'bulwa': {'scale': [(0, P1), (1.2, (1.04, 1.06, 1.04)), (2.4, P1)],
                      'rotation': [(0, (0, 0, -3)), (1.2, (0, 0, 3)), (2.4, (0, 0, -3))]},
            'purchawka_bark_l': {'scale': [(0, P1), (0.8, (1.1, 1.1, 1.1)), (1.6, P1), (2.4, P1)]}}},
        'walk': {'petla': 'loop', 'dlugosc': 0.6, 'kosci': {
            # podskok: przysiad (squash) -> lot (stretch) -> ladowanie (squash)
            'bulwa': {'position': [(0, Z0), (0.1, (0, -0.5, 0)), (0.3, (0, 3, 0)), (0.5, (0, 0.3, 0)), (0.6, Z0)],
                      'scale': [(0, (1.12, 0.88, 1.12)), (0.12, (1.1, 0.9, 1.1)), (0.3, (0.92, 1.1, 0.92)), (0.52, (1.14, 0.86, 1.14)),
                                (0.6, (1.12, 0.88, 1.12))],
                      'rotation': [(0, Z0), (0.3, (-8, 0, 0)), (0.6, Z0)]},
            'guz_0': {'position': [(0, Z0), (0.3, (0, 1.5, 0)), (0.6, Z0)]},
            'guz_1': {'position': [(0, Z0), (0.3, (0, 1.5, 0)), (0.6, Z0)]},
            'guz_2': {'position': [(0, Z0), (0.3, (0, 1.5, 0)), (0.6, Z0)]}}},
        'wybuch': {'petla': 'once', 'dlugosc': 1.0, 'kosci': {
            # 0-0.8 nabrzmiewa z drzeniem (ucieczka), otwor swieci coraz mocniej; 0.8-0.9 peka; 0.9 znika (MM: chmura)
            'bulwa': {'scale': [(0, P1), (0.8, (1.35, 1.3, 1.35)), (0.88, (1.6, 1.5, 1.6)), (0.9, ZERO), (1.0, ZERO)],
                      'rotation': [(0, Z0)] + _drzenie(0.1, 0.8, 5) + [(0.8, Z0), (1.0, Z0)]},
            'swiatlo_otwor': {'scale': [(0, P1), (0.8, (2, 1.5, 2)), (0.88, (2.5, 2, 2.5)), (0.9, ZERO), (1.0, ZERO)]},
            'guz_0': {'scale': [(0, P1), (0.89, P1), (0.9, ZERO), (1.0, ZERO)]},
            'guz_1': {'scale': [(0, P1), (0.89, P1), (0.9, ZERO), (1.0, ZERO)]},
            'guz_2': {'scale': [(0, P1), (0.89, P1), (0.9, ZERO), (1.0, ZERO)]},
            'fx_grzybnia': {'scale': [(0, P1), (0.89, P1), (0.9, ZERO), (1.0, ZERO)]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.3, 'kosci': {
            'bulwa': {'scale': [(0, P1), (0.06, (1.15, 0.85, 1.15)), (0.15, (0.92, 1.08, 0.92)), (0.3, P1)],
                      'rotation': [(0, Z0), (0.06, (12, 0, 6)), (0.3, Z0)]}}},
        'spawn': {'petla': 'once', 'dlugosc': 0.8, 'kosci': {
            'purchawiec': {'position': [(0, (0, -15, 0)), (0.35, (0, 2, 0)), (0.5, (0, 3, 0)), (0.7, Z0), (0.8, Z0)]},
            'bulwa': {'scale': [(0, (0.8, 1.2, 0.8)), (0.35, (0.9, 1.15, 0.9)), (0.7, (1.15, 0.85, 1.15)), (0.8, P1)]}}},
        'death': {'petla': 'hold', 'dlugosc': 0.5, 'kosci': {
            'bulwa': {'scale': [(0, P1), (0.2, (1.3, 1.25, 1.3)), (0.28, (1.5, 1.4, 1.5)), (0.3, ZERO), (0.5, ZERO)]},
            'swiatlo_otwor': {'scale': [(0, P1), (0.28, (2.5, 2, 2.5)), (0.3, ZERO), (0.5, ZERO)]},
            'guz_0': {'scale': [(0, P1), (0.29, P1), (0.3, ZERO), (0.5, ZERO)]},
            'guz_1': {'scale': [(0, P1), (0.29, P1), (0.3, ZERO), (0.5, ZERO)]},
            'guz_2': {'scale': [(0, P1), (0.29, P1), (0.3, ZERO), (0.5, ZERO)]},
            'fx_grzybnia': {'scale': [(0, P1), (0.29, P1), (0.3, ZERO), (0.5, ZERO)]}}},
    }


# oczka zmruzone (ciemne kreski z iskra) i szeroki, poszarpany usmiech-pekniecie
N = {'k': (40, 30, 16), 'K': (70, 54, 30), 'y': (176, 200, 60), 'c': (90, 70, 40)}
NAKLEJKI = {
    # bulwa_4 (14x2): zmruzone oczka ponad usmiechem; bulwa_3 (14x2): usmiech lukiem od kacika do kacika
    ('bulwa_4', 'north'): (['..KkK..KkK....', '...y.....y....'], N),
    ('bulwa_3', 'north'): (['.k..........k.', '..kKkKkKkKkk..'], N),
    ('bulwa_7', 'up'): (['cccc', 'c..c', 'c..c', 'cccc'], N),
}
_B = [(116, 92, 60), (140, 114, 76), (162, 136, 94), (182, 158, 114), (198, 176, 132), (210, 190, 148), (220, 202, 162)]
MATERIALY = {
    **{f'bulwa_{i}': {'ramp': _B[max(0, i - 2):i + 1], 'wzor': 'brodawki', 'szum': 0.08, 'gradient': 0.15} for i in range(1, 8)},
    'purchawka': {'ramp': [(120, 96, 60), (146, 120, 80), (170, 144, 100)], 'wzor': 'brodawki', 'szum': 0.1},
    'otwor': {'ramp': [(160, 210, 50), (196, 240, 90)], 'szum': 0.03, 'gradient': 0},
    'grzybnia': {'ramp': [(150, 144, 124), (176, 170, 150), (200, 196, 178)], 'alfa': 'draperia', 'szum': 0.05, 'gradient': 0.3},
    'guz': {'ramp': [(88, 70, 44), (108, 88, 58), (128, 106, 72)], 'wzor': 'brodawki', 'szum': 0.08},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n.startswith('bulwa_'):
        return n
    if n.startswith('purchawka'):
        return 'purchawka'
    if n == 'otwor':
        return 'otwor'
    if n.startswith('grzybnia'):
        return 'grzybnia'
    return 'guz'


def main():
    przebuduj()
    Z.zapisz_model(sys.argv[1], 'purchawka', MATERIALY, material, naklejki=NAKLEJKI, animacje_nowe=nowe_animacje(),
                   usun_animacje=('puff', 'throw'), ziarno=17)


if __name__ == '__main__':
    main()
