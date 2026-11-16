"""Korzeniak (Loch 001, rola: strefa korzeni - przysiada, wbija korzenie, pod nim rozlewa sie spowolnienie).
Baza: Korzeniak W2 (wlasciciel 2026-09-26: trzy korzeniaki jako trzy rozne moby). Stoi w katalogu modelu `korzeniak`.

Czym sie rozni od Pniaczka (W1, roj) i Korowca (W3, ciezki): JEDNO czerwone oko w bialej obwodce jak u bossa
(swieci - kosc `swiatlo_oko`), poszarpana paszcza, trzy korzenie, pnacza; animacja `ukorzenienie` z platem korzeni
(fx) i death przez zapadniecie sie w ziemie (recenzja porownawcza: najbardziej tematyczne).
Zmiany wzgledem W2: tekstura przemalowana plaskimi plamami (W2 mial szum ~3500 kolorow), usuniety pas mchu przez twarz
i zebro przecinajace oko, plat strefy przy ziemi (byl 1 px nad nia), korzenie nie wchodza w ziemie w chodzie.
Uzycie: tools/modele/zbuduj.sh korzeniak
"""
import sys

import z_pliku as Z
from bb import kosc as B, szescian as S

BAZA = '/opt/docker/mc/data/world1/output/lochy/modele-wlasne/_archiwum/korzeniak_wersje/korzeniak_w2.bbmodel'
POD_ZIEMIA = {'spawn:18', 'death:19', 'ukorzenienie:1'}  # celowo: wychodzi z ziemi / zapada sie w nia


def przebuduj():
    Z.wczytaj(BAZA)
    Z.usun('pnacze_1', 'zebro_4')
    Z.zmien('strefa', od=(-12, 0.1, -12), do=(12, 0.1, 12))
    for i in range(3):
        Z.zmien(f'koniec_{i}', przesun=(0, 0.8, 0))
    # swiecace oko nad malowanym (naklejka pien_srodek: kolumny 2-3, wiersz 2)
    Z.dodaj('korpus', B('swiatlo_oko', (0.5, 9.6, -4), [S('oko', (-0.5, 9.1, -4.1), (1.5, 10.1, -3.1), 5)]))


O = {'r': (214, 30, 24), 'R': (140, 12, 10), 'W': (220, 214, 196), 'w': (236, 232, 214), 'k': (26, 20, 8),
     'z': (182, 164, 110), 'Z': (146, 128, 80), 'm': (98, 112, 26), 'M': (70, 82, 16)}
NAKLEJKI = {
    ('pien_srodek', 'north'): (['.......', '.kWwk.m', '.krRwkM', '.kkkkMm'], O),
    ('pien_dol', 'north'): (['.........', '.kkk..kk.', 'kzkkkkZkk', '.k.kzk.k.'], O),
    ('oko', 'north'): (['rR'], O),
}
# oliwka bossa z nasyceniem ~0.4 (recenzja porownawcza), bez szumu per piksel
KORA = [(46, 42, 14), (64, 58, 20), (84, 74, 28), (104, 90, 36), (124, 106, 46), (142, 122, 58)]
MECH = [(60, 72, 14), (80, 94, 20), (102, 118, 28), (124, 138, 40)]
MATERIALY = {
    'kora': {'ramp': KORA, 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.1},
    'kora_jasna': {'ramp': KORA[1:] + [(158, 138, 70)], 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.1},
    'korzen': {'ramp': KORA[:-1], 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.1},
    'szpon': {'ramp': [(30, 26, 10), (46, 40, 16), (62, 54, 22)], 'szum': 0.08},
    'oko': {'ramp': [(200, 28, 22), (214, 30, 24)], 'szum': 0.02, 'gradient': 0},
    'pnacze': {'ramp': MECH, 'wzor': 'mech', 'alfa': 'draperia'},
    'mech': {'ramp': MECH, 'wzor': 'mech'},
    'lisc': {'ramp': [(58, 92, 24), (80, 118, 32), (104, 144, 44), (130, 168, 60)], 'alfa': 'lisc', 'szum': 0.08},
    'strefa': {'ramp': [(48, 40, 16), (72, 62, 26), (96, 82, 38), (118, 102, 50)], 'alfa': 'korzenie', 'szum': 0.08},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n == 'oko':
        return 'oko'
    if n == 'strefa':
        return 'strefa'
    if n.startswith(('zebro', 'sek', 'drzazga', 'lodyga')):
        return 'kora_jasna'
    if n.startswith(('udo', 'przypora', 'koniec', 'przedramie')):
        return 'korzen'
    if n.startswith('szpon'):
        return 'szpon'
    if n.startswith('pnacze'):
        return 'pnacze'
    if n.startswith('mech'):
        return 'mech'
    if n.startswith('lisc'):
        return 'lisc'
    return 'kora'


def main():
    przebuduj()
    Z.zapisz_model(sys.argv[1], 'korzeniak', MATERIALY, material, naklejki=NAKLEJKI, ziarno=7)


if __name__ == '__main__':
    main()
