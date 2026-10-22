"""Korowiec (Loch 001, rola: ciezki obronca z garda - "Korowy Strazny": gracz ma go obejsc albo przelamac garde).
Wyrosl z Korzeniaka W3 (wlasciciel 2026-09-26: trzy korzeniaki jako trzy rozne moby).

Czym sie rozni od Pniaczka (roj) i Korzeniaka (strefa): najwiekszy i najciezszy - szeroki, niski pien obrosniety mchem
dookola, bez lisci, grube rece z korowymi tarczami na przedramionach, oko jako waska czerwona szczelina (swieci:
kosc `swiatlo_oko`). Animacje: idle, walk (ciezki), garda (petla: skrzyzowane tarcze przed soba), attack (ciezkie
uderzenie obiema rekami z gory), hurt, spawn (wynurza sie), ukorzenienie, death (przewraca sie, zapada plytko).
Obroty = prawdziwy ruch (jak spoczynkowy): +x obraca czubek kosci ku +z (do tylu); reka zwisajaca na +x idzie do przodu.
Uzycie: tools/modele/zbuduj.sh korowiec
"""
import sys

import animacje as A
import tekstura
from bb import KOSCI, ELEMENTY, kosc as B, szescian as S, waliduj, zapisz

# celowo pod ziemia (animacja:max glebokosc px): wynurza sie / zapada plytko / wbija dlonie w ziemie
POD_ZIEMIA = {'spawn:8', 'death:7', 'ukorzenienie:1.5', 'attack:1'}  # attack: szpony w ziemie; death: zapada sie plytko


PIEN = [(10, 3, 9, 0), (9, 9, 13, 22.5), (7, 13, 16, 0)]   # (szer., y0, y1, obrot Y) - szeroki, niski, ciezki
OKO_Y = 11
REKA = (13, 7, 2.5)   # (bark y, lokiec y, dlon y)
POCHYL = -8
SZCZYT = PIEN[-1][2]


def zbuduj():
    pien = []
    nazwy = ['pien_dol', 'pien_srodek', 'pien_gora', 'pien_szczyt']
    for (w, y0, y1, ry), n in zip(PIEN, nazwy):
        pien.append(S(n, (-w / 2, y0, -w / 2), (w / 2, y1, w / 2), 3, rot=(0, ry, 0) if ry else (0, 0, 0)))
    pien += [
        S('zebro_1', (-PIEN[0][0] / 2 - 1, 3, -2), (-PIEN[0][0] / 2, PIEN[1][2] - 2, -1), 4),
        S('zebro_2', (PIEN[0][0] / 2, 3, 1), (PIEN[0][0] / 2 + 1, PIEN[0][2], 2), 4),
        S('sek', (PIEN[1][0] / 2, PIEN[1][1], -1), (PIEN[1][0] / 2 + 1, PIEN[1][1] + 2, 1), 4),
    ]
    # oko: 3x3 w kosci swiatlo_oko, osadzone w szczelinie kory (bez czarnej ramki)
    front = next(w for (w, y0, y1, ry) in PIEN if y0 <= OKO_Y < y1) / 2
    # oko: waska pozioma szczelina 3x1 (zmruzone, groznie) - swieci
    oko = B('swiatlo_oko', (0, OKO_Y, -front), [S('oko', (-1.5, OKO_Y - 0.5, -front - 0.5), (1.5, OKO_Y + 0.5, -front + 0.5), 5)])
    korona = [
        S('drzazga_1', (-2, SZCZYT, -1), (-1, SZCZYT + 3, 0), 4, origin=(-1.5, SZCZYT, -0.5), rot=(0, 0, 22.5)),
        S('drzazga_2', (1, SZCZYT, 0), (2, SZCZYT + 4, 1), 4, origin=(1.5, SZCZYT, 0.5), rot=(22.5, 0, 0)),
    ]
    lisc_sz = 0   # bez lisci - ciezki, stary pien
    liscie = []
    for j, (px, ry, rz) in enumerate(((0, -45, 25), (1, 45, -25))):
        dzieci = []
        if lisc_sz:
            x0 = -lisc_sz if j == 0 else 1
            dzieci = [S(f'lisc_{j + 1}', (x0, SZCZYT + 1, 0.5), (x0 + lisc_sz, SZCZYT + 4, 0.5), 5),
                      S(f'lisc_{j + 1}x', (x0 + lisc_sz / 2, SZCZYT + 1, -1.5), (x0 + lisc_sz / 2, SZCZYT + 4, 2.5), 5)]
        liscie.append(B(f'o_lisc_{j + 1}', (px, SZCZYT + 2, 0), dzieci, rot=(0, ry, rz)))
    # mech: zwisajace plaszczyzny (znak rozpoznawczy bossa); W3 - caly obwod
    mech = []
    obwod = 8     # obrosniety mchem dookola
    for i in range(obwod):
        strona = i % 4
        w0 = PIEN[1][0] / 2 + 0.1
        yg = PIEN[1][2]
        dl = 6
        off = (i // 4) * 2.5 - 1.5
        if strona == 0:
            mech.append(S(f'mech_{i}', (-w0, yg - dl, off - 1), (-w0, yg, off + 1.5), 5))
        elif strona == 1:
            mech.append(S(f'mech_{i}', (w0, yg - dl, off - 1), (w0, yg, off + 1.5), 5))
        elif strona == 2:
            mech.append(S(f'mech_{i}', (off - 1, yg - dl, w0), (off + 1.5, yg, w0), 5))
        else:
            mech.append(S(f'mech_{i}', (off - 1, yg - dl + 2, -w0), (off + 1.5, yg, -w0), 5))
    nogi = []
    for i, ry in enumerate((45, -45, 180)):
        przypora = S(f'przypora_{i}', (-1.5, 1, -2), (1.5, 4, 1), 2)
        udo = S(f'udo_{i}', (-1, 0.2, -5), (1, 3.2, -2), 2)
        koniec = B(f'stopa_{i}', (0, 2, -5), [S(f'koniec_{i}', (-0.5, 1.5, -8), (0.5, 2.5, -5), 1)], rot=(-22.5, 0, 0))
        nogi.append(B(f'noga_{i}', (0, 3, 0), [przypora, udo, koniec], rot=(0, ry, 0)))
    bark, lokiec, dlon = REKA
    rece = []
    for st, sx in (('l', -1), ('p', 1)):
        x = (PIEN[1][0] / 2 + 0.5) * sx
        xa, xb = (x - 2, x) if sx < 0 else (x, x + 2)
        ramie = S(f'ramie_{st}', (xa, lokiec, -1.5), (xb, bark, 1.5), 3)   # grube ramie
        # reka-korzen 2 px z rozwidleniem (zamiast 3 kostek 1 px - recenzja 3)
        przedr = S(f'przedramie_{st}', (xa, dlon + 1, -1), (xb, lokiec, 1), 2)
        szpony = [S(f'szpon_{st}{j}', (xa + j, dlon, -1), (xa + j + 1, dlon + 1, 0), 1) for j in (0, 1)]
        dodatki = [S(f'pnacze_reka_{st}', (x + sx * 2.1, dlon, -1), (x + sx * 2.1, bark - 2, 1), 5)]
        # korowa tarcza na zewnetrznej stronie przedramienia (garda)
        tarcza = S(f'tarcza_{st}', (xa - 1 if sx < 0 else xb, dlon + 1, -3), (xa if sx < 0 else xb + 1, lokiec + 1, 3), 4)
        k_przedr = B(f'przedramie_{st}', (x, lokiec, 0), [przedr, tarcza] + szpony, rot=(-5, 0, 0))
        rece.append(B(f'ramie_{st}', (x, bark, 0), [ramie, k_przedr] + dodatki, rot=(8, 0, sx * 6)))
    korzonki = B('fx_korzonki', (0, 0, 0), [
        S('korzonek_1', (-6, 0, -2), (-5, 4, -1), 2, origin=(-5.5, 0, -1.5), rot=(0, 0, 22.5)),
        S('korzonek_2', (5, 0, 1), (6, 5, 2), 2, origin=(5.5, 0, 1.5), rot=(0, 0, -22.5)),
        S('korzonek_3', (-1, 0, 5), (0, 4, 6), 2, origin=(-0.5, 0, 5.5), rot=(-22.5, 0, 0)),
    ])
    korpus = B('korpus', (0, 3, 0), pien + [oko] + korona + liscie + mech + rece, rot=(POCHYL, 0, 4))
    wys = SZCZYT + 3
    hitbox = B('hitbox', (0, OKO_Y, 0), [S('hitbox_box', (-5, 0, -5), (5, wys, 5), 1)])
    cien = B('shadow', (0, 0, 0), [S('shadow_box', (-6, 0, -6), (6, 1, 6), 1)])
    return [B('korzen', (0, 0, 0), [korpus] + nogi), korzonki, hitbox, cien, B('tag_name', (0, wys + 4, 0), [])]


# --- naklejki: oko jak u bossa (biala obwodka, czerwien, ciemna zrenica), szczelina kory ciemnym brazem wokol
O = {'r': (214, 30, 24), 'R': (150, 16, 12), 'w': (236, 228, 204), 'W': (196, 186, 160), 'k': (30, 16, 10),
     'd': (44, 38, 16), 'D': (58, 50, 20), 'z': (170, 150, 96), 'm': (96, 112, 26), 'M': (66, 80, 16)}
PNACZA = (['.m....', '.M..m.', '.m..M.', '.M..m.', '....M.'], O)
_seg_oka = next(n for (w, y0, y1, ry), n in zip(PIEN, ['pien_dol', 'pien_srodek', 'pien_gora', 'pien_szczyt']) if y0 <= OKO_Y < y1)
NAKLEJKI = {
    ('oko', 'north'): (['rRr'], O),
    (_seg_oka, 'north'): (['.........', '..dDDDd..', '.d.....d.', '..dDDDd..'], O),  # brew i powieka wokol szczeliny
    ('pien_dol', 'north'): (['........', '........', '..dzdz..', '...ddd..'], O),   # paszcza z drzazgami (pod okiem)
    ('pien_dol', 'west'): PNACZA, ('pien_dol', 'east'): PNACZA, ('pien_dol', 'south'): PNACZA,
    ('pien_srodek', 'west'): PNACZA, ('pien_srodek', 'south'): PNACZA,
}

Z0 = (0, 0, 0)
UKRYTA, PELNA = (0, 0, 0), (1, 1, 1)


def _fx_ukryte(anim):
    for nazwa, a in anim.items():
        if nazwa != 'ukorzenienie':
            a['kosci']['fx_korzonki'] = {'scale': [(0, UKRYTA), (a['dlugosc'], UKRYTA)]}
    return anim


def animacje():
    T = [0, 0.125, 0.25, 0.375, 0.5]
    walk = {'korpus': {'rotation': [(0, (0, 0, -6)), (0.125, Z0), (0.25, (0, 0, 6)), (0.375, Z0), (0.5, (0, 0, -6))],
                       'position': [(0, (0, 0, 0.5)), (0.125, (0, 0.6, 0)), (0.25, (0, 0, -0.5)), (0.375, (0, 0.6, 0)), (0.5, (0, 0, 0.5))]},
            'ramie_l': {'rotation': [(0, (15, 0, 0)), (0.25, (-15, 0, 0)), (0.5, (15, 0, 0))]},
            'ramie_p': {'rotation': [(0, (-15, 0, 0)), (0.25, (15, 0, 0)), (0.5, (-15, 0, 0))]}}
    # noga: przenoszenie (uniesiona +x, zamiatanie do przodu) -> podparcie (na ziemi, odpycha do tylu), fazy co 1/3
    for i, faza in ((0, 0), (1, 1 / 3), (2, 2 / 3)):
        klatki = []
        for k in range(5):
            t = T[k]
            f = ((t / 0.5) + faza) % 1.0
            if f < 0.35:   # przenoszenie
                u = f / 0.35
                klatki.append((t, (25 * (1 - abs(2 * u - 1)), -12 + 24 * u, 0)))
            else:          # podparcie: stopa na ziemi, zamiata do tylu
                u = (f - 0.35) / 0.65
                klatki.append((t, (0, 12 - 24 * u, 0)))
        walk[f'noga_{i}'] = {'rotation': klatki}
    anim = {
        'idle': {'petla': 'loop', 'dlugosc': 2.4, 'kosci': {
            'korpus': {'rotation': [(0, (0, 0, -2)), (1.14, (0, 0, 2)), (1.2, (0, 8, 2)), (1.3, (0, 0, 2)), (2.4, (0, 0, -2))],
                       'position': [(0, Z0), (1.2, (0, 0.3, 0)), (2.4, Z0)]},
            'o_lisc_1': {'rotation': [(0, Z0), (0.4, (0, 0, 10)), (0.8, (0, 0, -5)), (1.2, (0, 0, 10)), (1.6, (0, 0, -5)), (2.4, Z0)]},
            'o_lisc_2': {'rotation': [(0, Z0), (0.5, (0, 0, -10)), (1.0, (0, 0, 5)), (1.5, (0, 0, -10)), (2.4, Z0)]},
            'ramie_l': {'rotation': [(0, Z0), (1.2, (5, 0, -3)), (2.4, Z0)]},
            'ramie_p': {'rotation': [(0, (5, 0, 3)), (1.2, Z0), (2.4, (5, 0, 3))]}}},
        'walk': {'petla': 'loop', 'dlugosc': 0.5, 'kosci': walk},
        # garda (zmierzone bb_obrys: tarcze x -6..4 przed okiem, yaw -+40): tarcze przed okiem, korpus pochylony do przodu, lekkie drzenie pod ciezarem
        'garda': {'petla': 'loop', 'dlugosc': 1.6, 'kosci': {
            'korpus': {'rotation': [(0, (-6, 0, 0)), (0.8, (-6, 0, 1.5)), (1.6, (-6, 0, 0))],
                       'position': [(0, (0, -0.5, 0)), (0.8, (0, -0.7, 0)), (1.6, (0, -0.5, 0))]},
            'ramie_l': {'rotation': [(0, (85, -40, 0)), (0.8, (88, -42, 0)), (1.6, (85, -40, 0))]},
            'ramie_p': {'rotation': [(0, (85, 40, 0)), (0.8, (88, 42, 0)), (1.6, (85, 40, 0))]},
            'przedramie_l': {'rotation': [(0, (45, 0, 0)), (1.6, (45, 0, 0))]},
            'przedramie_p': {'rotation': [(0, (45, 0, 0)), (1.6, (45, 0, 0))]}}},
        'attack': {'petla': 'once', 'dlugosc': 1.1, 'kosci': {
            # 0-0.45 zamach: rece nad glowa i na boki (poza sylwetka), trzymanie do 0.6; 0.68 uderzenie w ziemie, squash
            'korpus': {'rotation': [(0, Z0), (0.45, (15, 0, 0)), (0.6, (17, 0, 0)), (0.68, (-16, 0, 0)), (0.85, (-13, 0, 0)), (1.1, Z0)],
                       'position': [(0, Z0), (0.45, (0, -0.5, 0.5)), (0.68, (0, 0.3, -1.5)), (1.1, Z0)],
                       'scale': [(0, PELNA), (0.67, PELNA), (0.7, (1.12, 0.88, 1.12)), (0.85, PELNA)]},
            'ramie_l': {'rotation': [(0, Z0), (0.45, (160, 0, -40)), (0.6, (165, 0, -42)), (0.68, (75, 0, -5)), (0.85, (75, 0, -5)), (1.1, Z0)]},
            'ramie_p': {'rotation': [(0, Z0), (0.45, (160, 0, 40)), (0.6, (165, 0, 42)), (0.68, (75, 0, 5)), (0.85, (75, 0, 5)), (1.1, Z0)]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.35, 'kosci': {
            'korpus': {'rotation': [(0, Z0), (0.08, (14, 0, 4)), (0.35, Z0)],
                       'scale': [(0, PELNA), (0.05, (1.08, 0.92, 1.08)), (0.15, PELNA)]},
            'ramie_l': {'rotation': [(0, Z0), (0.08, (-25, 0, -20)), (0.35, Z0)]},
            'ramie_p': {'rotation': [(0, Z0), (0.08, (-25, 0, 20)), (0.35, Z0)]}}},
        'spawn': {'petla': 'once', 'dlugosc': 1.0, 'kosci': {
            # wynurza sie: plytko spod ziemi + skala 0.3 -> 1, otrzasa sie
            'korzen': {'position': [(0, (0, -6, 0)), (0.6, (0, -1, 0)), (0.8, (0, 0.4, 0)), (1.0, Z0)],
                       'scale': [(0, (0.3, 0.3, 0.3)), (0.6, (0.9, 0.9, 0.9)), (0.8, (1.05, 1.08, 1.05)), (1.0, PELNA)],
                       'rotation': [(0, (0, 0, 8)), (0.3, (0, 0, -8)), (0.6, (0, 0, 6)), (1.0, Z0)]},
            'o_lisc_1': {'scale': [(0, UKRYTA), (0.7, UKRYTA), (0.9, (1.2, 1.2, 1.2)), (1.0, PELNA)]},
            'o_lisc_2': {'scale': [(0, UKRYTA), (0.75, UKRYTA), (0.95, (1.2, 1.2, 1.2)), (1.0, PELNA)]}}},
        'ukorzenienie': {'petla': 'once', 'dlugosc': 1.0, 'kosci': {
            # przysiada, wbija rece w ziemie, liscie drza; spod stop wyskakuja korzonki (MM stawia wtedy model strefy)
            'korpus': {'position': [(0, Z0), (0.3, (0, -1, 0)), (0.8, (0, -1, 0)), (1.0, Z0)],
                       'rotation': [(0, Z0), (0.3, (-8, 0, 0)), (0.8, (-8, 0, 0)), (1.0, Z0)]},
            'ramie_l': {'rotation': [(0, Z0), (0.3, (20, 0, -25)), (0.8, (20, 0, -25)), (1.0, Z0)]},
            'ramie_p': {'rotation': [(0, Z0), (0.3, (20, 0, 25)), (0.8, (20, 0, 25)), (1.0, Z0)]},
            'o_lisc_1': {'rotation': [(0, Z0), (0.35, (0, 0, 12)), (0.45, (0, 0, -12)), (0.55, (0, 0, 12)), (0.65, (0, 0, -12)), (1.0, Z0)]},
            'o_lisc_2': {'rotation': [(0, Z0), (0.4, (0, 0, -12)), (0.5, (0, 0, 12)), (0.6, (0, 0, -12)), (1.0, Z0)]},
            'fx_korzonki': {'scale': [(0, UKRYTA), (0.3, UKRYTA), (0.4, (1, 1.2, 1)), (0.5, PELNA), (0.8, PELNA), (1.0, UKRYTA)]}}},
        'death': {'petla': 'hold', 'dlugosc': 1.4, 'kosci': {
            # przewraca sie na bok (0.6 s), potem zapada plytko i kurczy (liscie wiedna)
            'korpus': {'rotation': [(0, Z0), (0.2, (0, 0, -8)), (0.6, (0, 0, 70)), (0.7, (0, 0, 64)), (0.8, (0, 0, 70)), (1.4, (0, 0, 70))],
                       'position': [(0, Z0), (0.6, (1, -1, 0)), (1.4, (1, -1, 0))]},
            'korzen': {'position': [(0, Z0), (0.8, Z0), (1.4, (0, -4, 0))],
                       'scale': [(0, PELNA), (0.8, PELNA), (1.4, (0.8, 0.6, 0.8))]},
            # rece przy tulowiu (nie rozrzucone w ziemie po stronie upadku)
            'ramie_l': {'rotation': [(0, Z0), (0.6, (20, 0, 25)), (1.4, (20, 0, 25))]},
            'ramie_p': {'rotation': [(0, Z0), (0.6, (20, 0, -25)), (1.4, (20, 0, -25))]},
            'o_lisc_1': {'scale': [(0, PELNA), (0.6, PELNA), (1.0, UKRYTA), (1.4, UKRYTA)]},
            'o_lisc_2': {'scale': [(0, PELNA), (0.6, PELNA), (1.0, UKRYTA), (1.4, UKRYTA)]}}},
    }
    return _fx_ukryte(anim)


# paleta: ciemna oliwka dreadroota (recenzja 3: gorne 10% jasnosci ~0.42, nie 0.63), wlokna po skosie
KORA = [(40, 38, 14), (56, 52, 20), (74, 66, 26), (92, 80, 32), (108, 94, 40), (122, 106, 48)]
MECH = [(56, 66, 14), (74, 86, 18), (94, 108, 26), (116, 128, 38)]
MATERIALY = {
    'kora': {'ramp': KORA, 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.12},
    'kora_jasna': {'ramp': KORA[1:] + [(134, 118, 56)], 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.12},
    'korzen': {'ramp': KORA[:-1], 'wzor': 'kora_skos', 'szum': 0.1, 'jasnosc': 0.12},
    'szpon': {'ramp': [(30, 26, 10), (46, 40, 16), (62, 54, 22)], 'szum': 0.08},
    'oko': {'ramp': [(180, 24, 20), (214, 30, 24)], 'szum': 0.02, 'gradient': 0},
    'pnacze': {'ramp': MECH, 'wzor': 'mech', 'alfa': 'draperia'},
    'mech': {'ramp': MECH, 'wzor': 'mech', 'alfa': 'draperia', 'szum': 0.08},
    'lisc': {'ramp': [(58, 80, 24), (76, 100, 30), (96, 120, 40), (114, 138, 52)], 'alfa': 'lisc', 'szum': 0.08},
    'lodyzka': {'ramp': [(60, 84, 26), (78, 104, 34)], 'szum': 0.05},
    'kwiat_r': {'ramp': [(170, 80, 110), (206, 118, 146), (232, 160, 184)], 'szum': 0.06},
    'kwiat_b': {'ramp': [(200, 196, 170), (226, 222, 198), (244, 240, 222)], 'szum': 0.05},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n == 'oko':
        return 'oko'
    if n.startswith(('zebro', 'sek', 'drzazga')):
        return 'kora_jasna'
    if n.startswith(('udo', 'przypora', 'koniec', 'przedramie', 'korzonek')):
        return 'korzen'
    if n.startswith('szpon'):
        return 'szpon'
    if n.startswith('pnacze'):
        return 'pnacze'
    if n.startswith('mech_'):
        return 'mech'
    if n.startswith('lodyzka'):
        return 'lodyzka'
    if n.startswith(('kwiat_r', 'kwiat_b')):
        return n.rsplit('_', 1)[0]
    if n.startswith('lisc'):
        return 'lisc'
    return 'kora'


def main():
    outliner = zbuduj()
    anim = animacje()
    waliduj({k for a in anim.values() for k in a['kosci']})
    tex_src, _ = tekstura.atlas(ELEMENTY, MATERIALY, material, naklejki=NAKLEJKI)
    zapisz(sys.argv[1], 'korowiec', outliner, tex_src, 128, A.zbuduj(anim, KOSCI, do_me=True))


if __name__ == '__main__':
    main()
