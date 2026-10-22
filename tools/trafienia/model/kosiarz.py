"""Kosiarz v3 (Loch 004 Łany Strachów, Akt 1 v2) — CLAUDE 12.10. Szkic K1 „Żniwiarz w pochyle” + haczyk K3 (snop w tułowiu).
Ogrodnik Bractwa, który przysiągł polom; Szczep „dotrzymał za niego”: płótno wypchane słomą, worek zamiast twarzy,
oczy-świetliki jak u Strachów, słomkowy kapelusz z pędami, łapcie z łyka, kosa oburącz w postawie koszenia.
v3: czyste prostopadłościany jak LR (v2 z wokseli SDF = schodkowe grudy przy górnej półce), detal malowany (malarz_kosiarz).
Pod Trafienia: kości-znaczniki traf_*, kikuty (wiechcie słomy) ukryte do czasu odcięcia.
Uruchom: python3 kosiarz.py  ->  output/trafienia/kosiarz/l004_kosiarz.bbmodel"""
import sys
from pathlib import Path
sys.path.insert(0, '/home/dev/Desktop/Przemek/MC/tools/modele')
sys.path.insert(0, str(Path(__file__).parent))
import json
import numpy as np
import bryly_sdf
from bryly_sdf import Model
import malarz_kosiarz

OUT = Path('/opt/docker/mc/data/world1/output/trafienia/kosiarz/l004_kosiarz.bbmodel')

MAT = {
 'plotno':   {'ramp': [(78, 72, 58), (106, 98, 80), (134, 124, 100), (158, 148, 120)], 'wzor': 'splot'},
 'worek':    {'ramp': [(96, 84, 62), (126, 112, 84), (152, 136, 104), (174, 158, 122)], 'wzor': 'twarz'},
 'plotno_c': {'ramp': [(46, 42, 36), (62, 57, 48), (80, 73, 60), (96, 88, 72)], 'wzor': 'splot'},
 'sloma':    {'ramp': [(146, 112, 48), (182, 146, 66), (210, 178, 92), (232, 206, 128)], 'wzor': 'zdzbla'},
 'powroslo': {'ramp': [(86, 66, 34), (112, 86, 44), (136, 106, 56), (154, 122, 68)], 'wzor': 'skret'},
 'lyko':     {'ramp': [(64, 50, 30), (88, 68, 40), (110, 88, 52), (126, 102, 62)], 'wzor': 'plecionka'},
 'drewno':   {'ramp': [(42, 30, 18), (60, 43, 27), (78, 57, 36), (94, 70, 44)], 'wzor': 'sloje'},
 'stal':     {'ramp': [(78, 80, 82), (112, 114, 114), (150, 150, 146), (200, 200, 192)], 'wzor': 'stal'},
 'pedy':     {'ramp': [(48, 76, 30), (68, 100, 40), (92, 128, 52), (116, 152, 68)], 'wzor': 'pedy'},
 'drzazgi':  {'ramp': [(120, 92, 58), (150, 118, 76), (176, 142, 96), (196, 162, 112)], 'wzor': 'sloje'},
 'oko':      {'ramp': [(255, 186, 70)] * 4},
}

RH = np.array([5.5, 18.0, -9.5])          # prawa dłoń (górny uchwyt)
KB = np.array([-16.0, 10.0, -12.0])   # ostrze 9 px nad ziemią (goleń): pochylenie nie wbija go w podłogę        # koniec kosiska przy ostrzu
KIER = (KB - RH) / np.linalg.norm(KB - RH)
T = RH - KIER * 5                          # koniec kosiska za prawą dłonią
t_l = (-5.5 - RH[0]) / (KB[0] - RH[0])
LH = RH + (KB - RH) * t_l                  # lewa dłoń na kosisku (x = -5.5)
LOK = {'l': np.array([-5.5, 19.5, -0.5]), 'r': np.array([5.5, 19.5, -0.5])}
DLON = {'l': LH, 'r': RH}


def box(m, cz, nazwa, lo, hi, mat):
    m.detal(cz, nazwa, tuple(lo), tuple(hi), mat)


def kepka(m, cz, nazwa, x, y, z, kier='y-', n=4, dl=2):
    """pęk źdźbeł 1×1×dl rozstawiony wokół (x,y,z)"""
    off = [(-1, 0), (1, 0), (0, -1), (0, 1), (0, 0), (-1, 1), (1, -1)][:n]
    for i, (a, b) in enumerate(off):
        if kier[0] == 'y':
            y0, y1 = (y - dl, y) if kier[1] == '-' else (y, y + dl)
            box(m, cz, f'{nazwa}_{i}', (x + a - .5, y0, z + b - .5), (x + a + .5, y1, z + b + .5), 'sloma')
        else:
            z0, z1 = (z - dl, z) if kier[1] == '-' else (z, z + dl)
            box(m, cz, f'{nazwa}_{i}', (x + a - .5, y + b - .5, z0), (x + a + .5, y + b + .5, z1), 'sloma')


def buduj():
    m = Model(MAT, budzet=200, atlas=128)
    m.czesc('korzen', (0, 0, 0))
    for s, st in ((-1, 'l'), (1, 'r')):
        x = s * 2
        m.czesc('noga_' + st, (x, 17, 0), 'korzen')
        box(m, 'noga_' + st, f'udo_{st}', (x - 1.5, 9, -1.5), (x + 1.5, 17, 1.5), 'plotno_c')
        kepka(m, 'noga_' + st, f'kolano_{st}', x, 9.5, -2, 'z-', n=3, dl=1)
        m.czesc('golen_' + st, (x, 9, 0), 'noga_' + st)
        box(m, 'golen_' + st, f'gol_{st}', (x - 1.5, 2, -1.5), (x + 1.5, 9, 1.5), 'plotno_c')
        for y0 in (3, 6):
            box(m, 'golen_' + st, f'onuca_{st}_{y0}', (x - 2, y0, -2), (x + 2, y0 + 1, 2), 'powroslo')
        box(m, 'golen_' + st, f'lapec_{st}', (x - 1.5, 0, -3.5), (x + 1.5, 2, 1.5), 'lyko')
    m.czesc('tulow', (0, 17, 0), 'korzen')
    box(m, 'tulow', 'biodra', (-3.5, 17, -2), (3.5, 20, 2), 'plotno_c')
    box(m, 'tulow', 'pas', (-4, 19, -2.5), (4, 20, 2.5), 'powroslo')
    box(m, 'tulow', 'wezel', (1.5, 17, -3.5), (3.5, 19, -2.5), 'powroslo')
    m.czesc('barki', (0, 20, 0), 'tulow')
    box(m, 'barki', 'piers', (-4, 20, -2), (4, 28, 2), 'plotno')
    box(m, 'barki', 'garb', (-3, 23, 2), (3, 27, 4), 'plotno')
    box(m, 'barki', 'rozdarcie', (0.5, 22, -3), (2.5, 25, -2), 'sloma')          # tu ostrze grzęźnie w snopie
    kepka(m, 'barki', 'rozd', 1.5, 22.5, -3, 'z-', n=3, dl=1)
    kepka(m, 'barki', 'kolnierz', 0, 28, -1.5, 'y+', n=5, dl=1)
    for s, st in ((-1, 'l'), (1, 'r')):
        bark = np.array([s * 5.5, 27, 0])
        m.czesc('ramie_' + st, tuple(bark), 'barki')
        box(m, 'ramie_' + st, f'ramie_{st}_box', (bark[0] - 1.5, 19, -1.5), (bark[0] + 1.5, 27.5, 1.5), 'plotno')
        # obrócona belka jako LIŚĆ: obrót rodzica w BB obraca dzieci, więc dłoń wisi na nieobróconej kości przedramienia
        m.czesc('przedramie_' + st, tuple(LOK[st]), 'ramie_' + st)
        m.segment('przedramie_seg_' + st, 'przedramie_' + st, np.eye(3), tuple(LOK[st]), tuple(DLON[st]), 3, 'plotno')
        m.czesc('dlon_' + st, tuple(DLON[st]), 'przedramie_' + st)
        x, y, z = DLON[st]
        box(m, 'dlon_' + st, f'piesc_{st}', (x - 1.5, y - 1.5, z - 1.5), (x + 1.5, y + 1.5, z + 1.5), 'plotno_c')
        kepka(m, 'dlon_' + st, f'mankiet_{st}', x, y + 1.5, z + 1.5, 'y+', n=3, dl=1)
    _, dl_kos = m.segment('kosisko', 'dlon_r', np.eye(3), tuple(T), tuple(KB), 2, 'drewno')
    # pod-hitboxy ModelEngine (ob_ = obracany prostopadłościan): kosa wystaje 1,4 bl. poza hitbox bazy, bez nich klik w nią to pudło
    m.czesc('ob_kosisko', tuple(T), 'kosisko')
    m.detal('ob_kosisko', 'ob_kosisko_box', (T[0] - 1.5, T[1], T[2] - 1.5), (T[0] + 1.5, T[1] + dl_kos, T[2] + 1.5), 'stal')
    # złamane kosisko (buława, topór): w dłoni zostaje krótki kikut z drzazgami; schowany do czasu złamania
    m.segment('kikut_kosiska', 'dlon_r', np.eye(3), tuple(T), tuple(RH + KIER * 4), 2, 'drewno', koniec='drzazgi')
    m.czesc('ostrze', tuple(KB), 'dlon_r')
    box(m, 'ostrze', 'osadka', (-17, 9, -13.5), (-15, 13, -10.5), 'drewno')
    m.czesc('ob_ostrze', tuple(KB), 'ostrze')
    m.detal('ob_ostrze', 'ob_ostrze_box', (-17, 8, -25), (8, 11, -9.5), 'stal')
    for i, x in enumerate(np.arange(-16, 8, 2.0)):
        f = (x + 16) / 24
        zc = -12 - 10 * f ** 1.7; w = round(5.0 - 3.0 * f)
        box(m, 'ostrze', f'blacha_{i}', (x, 9, zc - w / 2), (x + 2, 10, zc + w / 2), 'stal')
    m.czesc('glowa', (0, 28, -0.5), 'barki')
    box(m, 'glowa', 'worek', (-3, 28.5, -3.5), (3, 34.5, 2.5), 'worek')
    box(m, 'glowa', 'wiazanie', (-3.5, 28, -4), (3.5, 29, 3), 'powroslo')
    kepka(m, 'glowa', 'szyja', 0, 28, -4.5, 'y-', n=4, dl=1)
    m.czesc('swiatlo_oczy', (0, 31.5, -3.5), 'glowa')
    for s in (-1, 1):
        box(m, 'swiatlo_oczy', f'oko_{s}', (s * 1.5 - .5, 31, -4.5), (s * 1.5 + .5, 32, -3.5), 'oko')
    m.czesc('kapelusz', (0, 34.5, -0.5), 'glowa')
    box(m, 'kapelusz', 'rondo', (-7, 34.5, -7.5), (7, 35.5, 6.5), 'sloma')
    box(m, 'kapelusz', 'glowka', (-3, 35.5, -3.5), (3, 38.5, 2.5), 'sloma')
    box(m, 'kapelusz', 'tasma', (-3.5, 35.5, -4), (3.5, 36.5, 3), 'powroslo')
    # pędy Szczepu rosną z GŁOWY przez główkę kapelusza: strącony kapelusz odsłania je, nie zabiera
    m.czesc('pedy', (0, 38.5, -0.5), 'glowa')
    for i, (x, z, h) in enumerate(((-1.5, -1, 4), (0.5, -2, 5), (1.5, 0.5, 3))):
        box(m, 'pedy', f'ped_{i}', (x - .5, 38.5, z - .5), (x + .5, 38.5 + h, z + .5), 'pedy')
        box(m, 'pedy', f'lisc_{i}', (x + .5, 37.5 + h, z - .5), (x + 2.5, 38.5 + h, z + .5), 'pedy')
    return m


def kikuty(m):
    for s, st in ((-1, 'l'), (1, 'r')):
        k = 'kikut_reka_' + st
        x, y, z = LOK[st]
        m.czesc(k, tuple(LOK[st]), 'ramie_' + st)
        box(m, k, f'rekaw_{st}', (x - 2, y - 1, z - 2), (x + 2, y, z + 2), 'plotno')
        kepka(m, k, f'kr_{st}', x, y - 1, z, 'y-', n=7, dl=2)
        k = 'kikut_noga_' + st
        m.czesc(k, (s * 2, 17, 0), 'korzen')
        box(m, k, f'nogawka_{st}', (s * 2 - 2, 16, -2), (s * 2 + 2, 17, 2), 'plotno_c')
        kepka(m, k, f'kn_{st}', s * 2, 16, 0, 'y-', n=7, dl=2)


def znaczniki(m):
    Z = [('traf_glowa_a', 'glowa', (0, 31.5, 1.5)), ('traf_glowa_b', 'glowa', (0, 31.5, -2.5)),
         ('traf_kapelusz_a', 'kapelusz', (-5, 35, -0.5)), ('traf_kapelusz_b', 'kapelusz', (5, 35, -0.5)),
         ('traf_kapelusz_c', 'kapelusz', (0, 35, -7.0)), ('traf_kapelusz_d', 'kapelusz', (0, 35, 6.0)),
         ('traf_pedy_a', 'pedy', (0, 38.5, -0.8)), ('traf_pedy_b', 'pedy', (0.3, 43.0, -1.2)),
         ('traf_brzuch_a', 'tulow', (0, 17.5, 0)), ('traf_brzuch_b', 'tulow', (0, 19.5, 0)),
         ('traf_piers_a', 'barki', (0, 21.5, 0.3)), ('traf_piers_b', 'barki', (0, 26.5, 0.3)),
         ('traf_stopa_l', 'golen_l', (-2, 1.5, -0.5)), ('traf_stopa_r', 'golen_r', (2, 1.5, -0.5)),
         ('traf_kosisko_a', 'dlon_r', tuple(T)), ('traf_kosisko_b', 'dlon_r', tuple(KB)),
         ('traf_ostrze_b', 'ostrze', (7.0, 9.5, -22.0))]
    for n, rodzic, pkt in Z:
        m.czesc(n, pkt, rodzic)


def main():
    bryly_sdf.malarz = malarz_kosiarz.maluj
    m = buduj(); kikuty(m); znaczniki(m)
    r = m.zapisz(OUT, 'l004_kosiarz', hitbox=(12, 40, 20), tag_y=45)
    from anim_kosiarz import dodaj
    dodaj(OUT)
    d = json.loads(OUT.read_text())
    print('elementy', len(d['elements']), 'kosci', sum(1 for _ in __import__('re').finditer('"name"', json.dumps(d['outliner']))), '->', OUT)


if __name__ == '__main__':
    main()
