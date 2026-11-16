"""Król Ropuch K1 (CLAUDE, 03.10): piramidalna, przysadzista ropucha ~3 bl. Głowa = górna czaszka + ruchoma żuchwa
(pivot z tyłu, wnętrze paszczy różowe widoczne dopiero po otwarciu), worek gardłowy turkus pod żuchwą, oczy w guzach,
korona z rzęsy ze złotymi pierścieniami, brodawki z turkusowym jadem, łapy na ziemi, prawa trzyma berło z rybią czaszką."""
from math import sin, cos, pi, sqrt
from pathlib import Path
import json
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'skora':   {'ramp': [(50, 62, 30), (74, 90, 44), (96, 114, 56), (108, 126, 64)], 'plamy': .1},
 'grzbiet': {'ramp': [(38, 48, 24), (56, 70, 34), (74, 90, 44), (84, 100, 50)], 'plamy': .12},
 'brzuch':  {'ramp': [(110, 104, 66), (142, 134, 88), (170, 162, 112), (198, 190, 140)]},
 'warga':   {'ramp': [(120, 114, 74), (150, 142, 96), (176, 168, 120), (204, 196, 150)]},
 'usta':    {'ramp': [(22, 18, 14), (30, 24, 18), (40, 32, 24), (52, 42, 30)]},
 'wnetrze': {'ramp': [(96, 46, 50), (130, 66, 70), (164, 92, 94), (192, 120, 120)]},
 'turkus':  {'ramp': [(20, 80, 70), (36, 130, 114), (63, 209, 180), (150, 240, 220)]},
 'zrenica': {'ramp': [(14, 14, 14)] * 4},
 'zloto':   {'ramp': [(110, 84, 30), (150, 116, 42), (186, 150, 60), (214, 184, 92)]},
 'rzesa':   {'ramp': [(40, 58, 20), (58, 82, 28), (80, 106, 40), (106, 132, 56)]},
 'mul':     {'ramp': [(46, 32, 22), (68, 48, 32), (92, 66, 44), (118, 88, 60)]},
 'kosc':    {'ramp': [(150, 142, 116), (184, 176, 148), (212, 204, 178), (234, 228, 206)]},
 'drewno':  {'ramp': [(56, 40, 26), (78, 56, 36), (100, 74, 48), (124, 94, 62)]},
}
HY = 25      # wysokość linii ust
HC, HR = (0, 30, -4), (17, 10.5, 13)   # elipsoida głowy


def buduj():
    m = Model(MAT, budzet=680, atlas=256)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('tulow', (0, 14, 4), 'korzen')
    m.elipsoida('tulow', (0, 12, 5), (20, 12, 17), 'skora')               # szeroka podstawa (piramida)
    m.czesc('garb', (0, 20, 6), 'tulow')                                  # ciemniejszy grzbiet: osobna kość = czyste scalanie
    m.elipsoida('garb', (0, 20, 6), (17, 12, 15), 'grzbiet')
    m.czesc('brzuch', (0, 15, -6), 'tulow')
    m.elipsoida('brzuch', (0, 15, -6), (13, 9, 7), 'brzuch')
    # głowa: górna czaszka (y >= HY)
    m.czesc('glowa', (0, HY, 4), 'tulow')
    m.elipsoida('glowa', HC, HR, 'skora')
    m.odejmij('glowa', 'pudlo', (-30, 0, -30), (30, HY, 30))
    # Wcięta jama pozostawia w spoczynku ciemną, różową szczelinę pod górną wargą.
    m.odejmij('glowa', 'pudlo', (-9, HY, -21), (9, HY + 4, -12))
    m.pudlo('glowa', (-9, HY, -12.8), (9, HY + 3.5, -11.8), 'usta')
    m.pudlo('glowa', (-7.5, HY, -13.2), (7.5, HY + 1.2, -12.2), 'wnetrze')
    # żuchwa: dolna część elipsoidy głowy (y < HY), pivot z tyłu — obrót X otwiera paszczę
    m.czesc('szczeka', (0, HY, 6), 'glowa')
    m.elipsoida('szczeka', HC, HR, 'brzuch')
    m.odejmij('szczeka', 'pudlo', (-30, HY, -30), (30, 60, 30))
    c = m.czesci['szczeka']
    for (x, y, z) in list(c.vox):                                       # wierzch żuchwy = wnętrze paszczy
        if y == HY - 1:
            c.vox[x, y, z] = 'wnetrze'
    # linia ust na czole żuchwy: ciemna szczelina + jasna warga nad nią, kąciki opadają
    for x in range(-15, 15):
        u = (x + .5) / HR[0]
        for dy, mat in ((0, 'usta'), (1, 'warga')):
            y = HY - 1 + dy - (1 if abs(u) > .78 else 0)
            t = 1 - u * u - ((y + .5 - HC[1]) / HR[1]) ** 2
            if t <= 0:
                continue
            zf = HC[2] - HR[2] * sqrt(t)
            part = 'szczeka' if dy == 0 else 'glowa'
            m.detal(part, f'usta_{dy}_{x}', (x, y, zf - .6), (x + 1, y + 1, zf + .6), mat)
    # worek gardłowy pod żuchwą
    m.czesc('swiatlo_worek', (0, 19, -12), 'szczeka')
    m.elipsoida('swiatlo_worek', (0, 19, -12), (9, 6.5, 4.5), 'turkus')
    # oczy w guzach
    for s, st in ((-1, 'l'), (1, 'r')):
        m.czesc('guz_' + st, (s * 10, 36, -7), 'glowa')
        m.elipsoida('guz_' + st, (s * 10, 37, -7), (4.6, 4.2, 4.4), 'skora')
    m.czesc('swiatlo_oczy', (0, 37, -11), 'glowa')
    for s in (-1, 1):
        m.elipsoida('swiatlo_oczy', (s * 10, 37.5, -10.5), (2.8, 2.6, 1.6), 'turkus')
        m.detal('swiatlo_oczy', f'zrenica_{s}', (s * 10 - 1.5, 37, -12.4), (s * 10 + 1.5, 38, -11.4), 'zrenica')
    # korona: pierścień 7 pędów rzęsy, złote pierścienie, turkusowe krople
    m.czesc('korona', (0, 39, -3), 'glowa')
    m.elipsoida('korona', (0, 39, -3), (8, 1.4, 7), 'rzesa')
    for k in range(7):
        a = k / 7 * 2 * pi
        x, z = 7 * cos(a), -3 + 6 * sin(a)
        h = 6 + (k * 3 % 4)
        m.detal('korona', f'ped_{k}', (x - .5, 39, z - .5), (x + 1.5, 39 + h, z + 1.5), 'rzesa')
        m.detal('korona', f'pierscien_{k}', (x - .5, 40 + (k % 2), z - .5), (x + 1.5, 41 + (k % 2), z + 1.5), 'zloto')
        if k in (0, 2, 5):
            m.detal('korona', f'jad_korony_{k}', (x, 43 + h//2, z - 1),
                    (x + 1, 44 + h//2, z), 'turkus')
    m.czesc('swiatlo_korona', (0, 44, -3), 'korona')
    for k in range(0, 7, 2):
        a = k / 7 * 2 * pi
        x, z = 7 * cos(a), -3 + 6 * sin(a)
        m.detal('swiatlo_korona', f'kropla_{k}', (x, 43 + (k % 3), z), (x + 1, 44 + (k % 3), z + 1), 'turkus')
    # brodawki z turkusem na grzbiecie
    m.czesc('swiatlo_brodawki', (0, 30, 12), 'tulow')
    for i, (x, y, z) in enumerate(((-9, 29, 14), (8, 30, 15), (-14, 23, 15), (14, 24, 13), (0, 32, 13), (-5, 25, 19), (6, 26, 19), (-17, 17, 8), (17, 18, 9))):
        m.elipsoida('garb', (x, y, z), (1.8, 1.6, 1.8), 'grzbiet')
        m.detal('swiatlo_brodawki', f'jad_{i}', (x - .5, y + 1, z - .5), (x + .5, y + 2, z + .5), 'turkus')
    # przednie łapy na ziemi
    for s, st in ((-1, 'l'), (1, 'r')):
        n = 'lapa_' + st
        m.czesc(n, (s * 14, 20, -6), 'tulow')
        m.kapsula(n, (s * 14, 20, -6), (s * 17, 4, -13), 3.2, 2.4, 'skora')
        m.pudlo(n, (s * 17 - 3, 0, -18), (s * 17 + 3, 2, -11), 'skora')
        for f in (-2, 0, 2):
            m.detal(n, f'palec_{f}', (s * 17 + f - .5, 0, -20), (s * 17 + f + .5, 1, -18), 'skora')
        # tylne uda i stopy
        u = 'udo_' + st
        m.czesc(u, (s * 15, 10, 10), 'tulow')
        m.elipsoida(u, (s * 17, 9, 9), (6, 8, 9), 'skora')
        m.pudlo(u, (s * 15, 0, -2), (s * 15 + s * 10 if s > 0 else s * 15 + 1, 2, 7) if s > 0 else (s * 25, 0, -2), 'skora') if False else None
        lo, hi = ((15, 0, -3), (26, 2, 6)) if s > 0 else ((-26, 0, -3), (-15, 2, 6))
        m.pudlo(u, lo, hi, 'skora')
    # berło w prawej łapie
    m.czesc('berlo', (17, 4, -15), 'lapa_r')
    m.pudlo('berlo', (17, 0, -16), (19, 50, -14), 'drewno')
    for y in (12, 22, 32):
        m.detal('berlo', f'owijka_{y}', (16.5, y, -16.5), (19.5, y + 2, -13.5), 'zloto')
    m.pudlo('berlo', (16, 50, -17), (20, 57, -13), 'kosc')                  # rybia czaszka
    m.kapsula('berlo', (18, 54, -15), (25, 54, -15), 2.1, .65, 'kosc')
    m.detal('berlo', 'szczeka_ryby', (19, 51, -19), (26, 52, -14), 'kosc')
    for y in range(51, 57, 2):
        m.detal('berlo', f'osc_{y}', (14, y, -15.5), (22, y + 1, -14.5), 'kosc')
    m.detal('berlo', 'oko_ryby', (17, 54, -18.2), (19, 56, -17.2), 'usta')
    for i, y in enumerate((52, 54, 56)):
        m.detal('berlo', f'osc_ryby_{i}', (22, y, -15.5), (27, y + 1, -14.5), 'kosc')
    # rzęsa z krawędzi głowy i barków, muł przy stopach
    m.czesc('rzesa', (0, 30, 0), 'glowa')
    for i, (x, z, y, l) in enumerate(((-16, -6, 30, 6), (-13, -10, 29, 5), (16, -5, 31, 7), (12, -11, 30, 5), (-6, -14, 33, 4),
                                      (5, -14, 33, 3), (-17, 2, 31, 8), (17, 3, 30, 7), (-10, 8, 36, 6), (10, 9, 35, 5))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    m.czesc('mul', (0, 0, 4), 'korzen')
    m.elipsoida('mul', (0, .3, 4), (22, 1.2, 18), 'mul')
    m.czesc('p_paszcza', (0, HY, -16), 'szczeka')
    m.czesc('fx_jezyk', (0, HY - 1, -15), 'szczeka')                     # język (ukryty skalą 0 poza tongue/swallow)
    m.detal('fx_jezyk', 'jezyk', (-2, HY - 2, -40), (2, HY, -15), 'wnetrze')
    m.detal('fx_jezyk', 'koniec_jezyka', (-3, HY - 2.5, -43), (3, HY + .5, -40), 'wnetrze')
    m.czesc('fx_korona', (0, 46, -3), 'korona')                           # poświata przy przyzwaniu (summon)
    for k in range(8):
        import math
        a = k / 8 * 2 * math.pi
        m.detal('fx_korona', f'blask_{k}', (9 * math.cos(a) - .5, 45, -3 + 8 * math.sin(a) - .5), (9 * math.cos(a) + .5, 47, -3 + 8 * math.sin(a) + .5), 'turkus')
    m.uprosc(['tulow', 'garb', 'brzuch', 'glowa', 'udo_l', 'udo_r', 'lapa_l', 'lapa_r', 'mul', 'guz_l', 'guz_r'], krok=2, prog=.5)
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/krol_ropuch'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_krol_ropuch.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_krol_ropuch', hitbox=(40, 46, 34), tag_y=60)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 3.0, {'tulow': ('scale', (.02, .03, .02)), 'swiatlo_worek': ('scale', (.06, .06, .06))})
    from anim_krol_ropuch import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
