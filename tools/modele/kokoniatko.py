"""Kokoniątko K1 (CLAUDE, 03.10): kulisty odwłok dominuje, na szczycie pęknięty kokon (białe płaty) odsłania
turkusowy worek jadu; mała brązowa głowa z czterema czarnymi oczami; 8 krótkich nóg rozstawionych na boki,
końce zabłocone; nić przędzy ciągnie się z tyłu po ziemi. Każda noga = kość od biodra (animacja drobienia)."""
from pathlib import Path
import json
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'skora':   {'ramp': [(52, 66, 32), (76, 94, 46), (102, 122, 62), (132, 152, 84)], 'plamy': .1},
 'noga':    {'ramp': [(60, 72, 40), (86, 100, 58), (114, 130, 80), (146, 162, 108)], 'plamy': .05},
 'mul':     {'ramp': [(46, 32, 22), (68, 48, 32), (92, 66, 44), (118, 88, 60)]},
 'glowa':   {'ramp': [(58, 40, 26), (86, 60, 38), (112, 82, 54), (140, 106, 72)]},
 'kokon':   {'ramp': [(150, 144, 120), (186, 180, 156), (214, 208, 186), (236, 232, 214)]},
 'jad':     {'ramp': [(20, 80, 70), (36, 130, 114), (63, 209, 180), (150, 240, 220)]},
 'oko':     {'ramp': [(12, 12, 14), (18, 18, 20), (26, 26, 30), (40, 40, 46)]},
 'blik':    {'ramp': [(200, 210, 210)] * 4},
 'rzesa':   {'ramp': [(52, 70, 22), (74, 96, 30), (100, 124, 42), (128, 150, 60)]},
}

NOGI = [  # (z biodra, kierunek z stopy, długość) — przednie wysunięte do przodu, tylne do tyłu
    (-2.5, -6.0, 1.0), (-0.5, -2.0, 1.0), (1.5, 2.5, 1.0), (3.5, 6.5, .95)]


def buduj():
    m = Model(MAT, budzet=220, atlas=128)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('tulow', (0, 5, 1), 'korzen')
    m.elipsoida('tulow', (0, 7, 1.5), (4.8, 5.0, 5.4), 'skora')
    # pęknięty kokon: płaty na szczycie z otworem, worek jadu w środku
    m.czesc('kokon', (0, 9, 1.5), 'tulow')
    for x in range(-5, 5):
        for y in range(8, 13):
            for z in range(-4, 8):
                p = (x + .5, y + .5, z + .5)
                d = (p[0] / 5.1) ** 2 + ((p[1] - 7) / 5.4) ** 2 + ((p[2] - 1.5) / 5.7) ** 2
                if .82 <= d <= 1.0 and p[1] > 9.6:
                    otwor = (p[0] / 2.9) ** 2 + ((p[2] - 1) / 3.2) ** 2 < 1
                    if not otwor:
                        m.czesci['kokon'].vox[x, y, z] = 'kokon'
    for i, (x, z, h) in enumerate(((-3, -2.5, 2), (3, -2, 2), (3.5, 4, 1), (-3.5, 4.5, 2))):   # zadarte płaty
        m.detal('kokon', f'plat_{i}', (x - .5, 11.5, z - .5), (x + .5, 11.5 + h, z + .5), 'kokon')
    m.czesc('swiatlo_worek', (0, 9, 1), 'tulow')
    m.elipsoida('swiatlo_worek', (0, 10.6, 1), (2.6, 1.9, 2.9), 'jad')
    # głowa: mała, brązowa, z przodu nisko; 4 oczy (2 duże w środku, 2 małe po bokach) + kły
    m.czesc('glowa', (0, 4, -4), 'tulow')
    m.elipsoida('glowa', (0, 4.6, -5), (2.6, 2.2, 2.0), 'glowa')
    for x0, x1, y0, y1 in ((-1.5, -.5, 4, 6), (.5, 1.5, 4, 6), (-2.5, -1.5, 4.5, 5.5), (1.5, 2.5, 4.5, 5.5)):
        m.detal('glowa', f'oko_{x0}', (x0, y0, -7.2), (x1, y1, -6.2), 'oko')
    m.czesc('szczeki', (0, 3, -6.5), 'glowa')
    for s in (-1, 1):
        m.detal('szczeki', f'kiel_{s}', (s * 1.0 - .5, 1.5, -7.6), (s * 1.0 + .5, 3.5, -6.6), 'kokon')
    # nogi: biodro pod brzegiem odwłoka, kolano wyżej i na zewnątrz, stopa na ziemi; koniec zabłocony
    for s, strona in ((-1, 'l'), (1, 'r')):
        for i, (zb, zs, k) in enumerate(NOGI):
            n = f'noga_{strona}{i + 1}'
            biodro = (s * 3.6, 4.5, zb)
            m.czesc(n, biodro, 'tulow')
            kolano = (s * 6.8 * k, 6.2, zb + (zs - zb) * .45)
            kostka = (s * 8.2 * k, 2.2, zs * .92)
            stopa = (s * 8.6 * k, .5, zs)
            m.kapsula(n, biodro, kolano, 1.0, .85, 'noga')
            m.kapsula(n, kolano, kostka, .85, .75, 'noga')
            m.kapsula(n, kostka, stopa, .75, .7, 'mul')
    # rzęsa zwisająca z boków odwłoka
    m.czesc('rzesa', (0, 7, 1.5), 'tulow')
    for i, (x, z, y, l) in enumerate(((-4.8, -1, 9, 3), (-4.5, 3.5, 10, 4), (4.8, 0, 9, 3), (4.6, 4.5, 10, 3), (-2.5, 6.5, 10, 3), (2.5, 6.5, 8, 2))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    # nić przędzy z tyłu, ciągnie się po ziemi
    m.czesc('nic', (0, 3, 6.5), 'tulow')
    for i, (y, z) in enumerate(((4, 6.5), (3, 7.5), (2, 8.5), (1, 9.5), (0, 10.5))):   # schodkiem do ziemi
        m.detal('nic', f'nic_{i}', (-.5, y, z), (.5, y + 1, z + 1), 'kokon')
    m.detal('nic', 'nic_ziemia', (-.5, 0, 11.5), (.5, 1, 15), 'kokon')
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/kokoniatko'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_kokoniatko.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_kokoniatko', hitbox=(10, 8, 5), tag_y=14)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 1.2, {'tulow': ('position', (0, -.3, 0)), 'swiatlo_worek': ('scale', (.06, .06, .06))})
    from anim_kokoniatko import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
