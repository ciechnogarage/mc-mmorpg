"""Kokoniara K1 (CLAUDE, 03.10): "pająk grozy" — niski głowotułów, rząd 6 turkusowych oczu, kły-sierpy;
8 długich nóg z kolanem wysoko nad grzbietem (udo + goleń = dwie kości); odwłok = kokon (jajo z oplotem przędzy)
uniesiony z tyłu, przez szczeliny prześwituje turkusowy rdzeń (swiatlo_kokon, słaby punkt przy 50%)."""
import numpy as np
from pathlib import Path
import json
from bryly_sdf import Model, segment  # zgodność dla mulowiec.py i szeptucha.py
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'chityna': {'ramp': [(40, 34, 22), (58, 50, 32), (78, 68, 44), (96, 84, 56)], 'plamy': .08},
 'noga':    {'ramp': [(46, 40, 26), (66, 58, 38), (86, 76, 50), (104, 92, 62)], 'plamy': .05},
 'mul':     {'ramp': [(40, 28, 20), (58, 40, 28), (78, 56, 38), (98, 72, 50)]},
 'przedza': {'ramp': [(98, 94, 72), (124, 118, 92), (148, 142, 112), (168, 160, 128)], 'plamy': .1},
 'nici':    {'ramp': [(54, 52, 40), (70, 68, 52), (86, 84, 64), (100, 98, 76)]},
 'turkus':  {'ramp': [(20, 80, 70), (36, 130, 114), (63, 209, 180), (150, 240, 220)]},
 'kiel':    {'ramp': [(150, 142, 116), (184, 176, 148), (212, 204, 178), (234, 228, 206)]},
 'rzesa':   {'ramp': [(40, 58, 20), (58, 82, 28), (80, 106, 40), (106, 132, 56)]},
}
# nogi: (z biodra, z kolana, z stopy) — przednie wysunięte, tylne cofnięte
NOGI = [(-8, -17, -27), (-5.5, -8, -11), (-3, 3, 7), (-.5, 11, 21)]
KOLANA = [23, 20.5, 19, 22]      # różne wysokości kolan par (bez efektu identycznych szczudeł)
ZASIEG = [21, 23.5, 22.5, 19.5]


def buduj():
    m = Model(MAT, budzet=420, atlas=256)
    m.czesc('korzen', (0, 0, 0))
    m.czesc('tulow', (0, 9, -4), 'korzen')
    m.elipsoida('tulow', (0, 9, -4), (5.2, 3.4, 6.2), 'chityna')
    m.czesc('glowa', (0, 9, -10), 'tulow')
    m.elipsoida('glowa', (0, 9.2, -11), (3.8, 3.0, 2.8), 'chityna')
    m.czesc('swiatlo_oczy', (0, 10.5, -13.5), 'glowa')
    for i, (x, y, w) in enumerate(((-3.2, 10, 1), (-2, 10.2, 2), (.0, 10.2, 2), (2.2, 10, 1), (-2.5, 11.4, 1), (1.5, 11.4, 1))):
        m.detal('swiatlo_oczy', f'oko_{i}', (x, y, -14.2), (x + w, y + 1, -11.5), 'turkus')
    m.czesc('kly', (0, 7, -13), 'glowa')
    for s in (-1, 1):
        m.kapsula('kly', (s * 1.6, 7.5, -13), (s * 2.4, 4.0, -15.5), 1.3, 1.0, 'kiel')
        m.kapsula('kly', (s * 2.4, 4.0, -15.5), (s * 1.0, 2.2, -16), 1.0, .7, 'kiel')
    # odwłok-kokon: jajo z oplotem, pochylone do tyłu i w górę
    m.czesc('odwlok', (0, 10, 1), 'tulow', obrot=(-28, 0, 0))
    m.jajo_oplot('odwlok', (0, 17, 6), 6.5, 8.5, 7.0, 6, 'przedza', 'nici')
    for (x, y, z) in ((-5.5, 16, 6), (5.5, 18, 5), (0, 20, 12.5), (0, 12, 0.5)):      # szczeliny na rdzeń (ostatnia: z przodu, widoczna od gracza)
        m.odejmij('odwlok', 'elipsoida', (x, y, z), (1.6, 2.2, 1.6))
    m.czesc('swiatlo_kokon', (0, 17, 6), 'odwlok', obrot=(-28, 0, 0))
    m.elipsoida('swiatlo_kokon', (0, 17, 6), (4.6, 6.2, 5.0), 'turkus')
    m.czesc('p_odwlok_pkt', (0, 17, 6), 'odwlok')
    m.czesc('p_kadziel', (0, 12, 13), 'odwlok')
    # nogi: udo w górę do kolana (wysoko nad grzbietem), goleń w dół do ziemi; 1 kostka na segment w obróconej kości
    I = np.eye(3)
    for s_, st in ((-1, 'l'), (1, 'r')):
        for i, (zb, zk, zs) in enumerate(NOGI):
            biodro = (s_ * 4.2, 9.5, zb)
            kolano = (s_ * (12 + (i % 2)), KOLANA[i] + (.8 if s_ > 0 and i == 1 else 0), zk)
            stopa = (s_ * ZASIEG[i], 1.0, zs)
            Ru, Lu = segment(m, f'udo_{st}{i + 1}', 'tulow', I, biodro, kolano, 2, 'noga')
            segment(m, f'golen_{st}{i + 1}', f'udo_{st}{i + 1}', Ru, kolano, stopa, 2, 'noga', koniec='mul',
                    P=(biodro[0], biodro[1] + Lu, biodro[2]))
    m.czesc('rzesa', (0, 12, -4), 'tulow')
    for i, (x, z, y, l) in enumerate(((-4.5, -6, 10, 3), (4.5, -3, 10, 3), (-3, 1, 11, 2), (3.5, -8, 10, 2))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    # pocisk kokonu (ukrywany skalą 0 poza 'spin')
    m.czesc('fx_kokon', (0, 9, -18), 'glowa')
    m.elipsoida('fx_kokon', (0, 9, -18), (2.0, 2.4, 2.0), 'przedza')
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/kokoniara'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_kokoniara.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_kokoniara', hitbox=(32, 26, 10), tag_y=34)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 2.0, {'tulow': ('position', (0, -.5, 0)), 'odwlok': ('rotation', (3, 0, 0))})
    from anim_kokoniara import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
