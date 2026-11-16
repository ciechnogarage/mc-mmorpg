"""Pijawka K1 (CLAUDE, 03.10): gruby łuk 6 segmentów (łańcuch kości do falowania), przewężenia między nimi,
okrągła przyssawka z 8 zębami i turkusowym gardłem (wnęka), ogon zanurzony w mule, rzęsa na grzbiecie."""
from math import sin, cos, pi
from pathlib import Path
import json
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
 'skora':  {'ramp': [(52, 46, 28), (76, 68, 40), (100, 92, 56), (128, 118, 74)], 'plamy': .08},
 'pas':    {'ramp': [(34, 30, 20), (48, 42, 28), (64, 58, 38), (82, 74, 50)]},
 'spod':   {'ramp': [(84, 82, 52), (110, 108, 70), (136, 134, 92), (160, 158, 114)]},
 'warga':  {'ramp': [(40, 26, 22), (60, 38, 32), (82, 54, 44), (104, 70, 58)]},
 'gardlo': {'ramp': [(14, 52, 46), (26, 96, 84), (63, 209, 180), (150, 240, 220)]},
 'zab':    {'ramp': [(150, 140, 110), (184, 174, 140), (210, 200, 170), (232, 224, 198)]},
 'mul':    {'ramp': [(40, 28, 20), (60, 42, 28), (82, 60, 40), (106, 80, 54)]},
 'rzesa':  {'ramp': [(48, 66, 22), (70, 92, 30), (96, 120, 42), (124, 146, 60)]},
}

# segmenty od przyssawki (−Z) do ogona: (z, y środka, promień poprzeczny) — łuk w górę pośrodku
SEG = [(-9.0, 5.8, 4.0), (-5.5, 6.4, 3.6), (-2.0, 6.1, 3.2), (1.5, 5.6, 2.7), (5.0, 4.2, 2.2), (8.2, 2.4, 1.7)]


def buduj():
    m = Model(MAT, budzet=200, atlas=128)
    m.czesc('korzen', (0, 0, 0))
    rodzic = 'korzen'
    for i, (z, y, r) in enumerate(SEG):
        n = f'seg{i + 1}'
        pz = z + 1.75 if i else z            # pivot na złączu z poprzednim (od strony głowy)
        m.czesc(n, (0, y, z - 1.75) if i else (0, y, z), rodzic)
        m.elipsoida(n, (0, y, z), (r, r * (.95 if i < 4 else .7), 2.15), 'skora')   # ogon spłaszczony
        # pas na złączu: ciemniejszy pierścień nieco węższy (przewężenie)
        if i < len(SEG) - 1:
            m.elipsoida(n, (0, y - .1, z + 1.9), (r * .86, r * .8, .7), 'pas')
        # jaśniejszy spód
        m.pudlo(n, (-r * .6, max(0, y - r), z - 1.4), (r * .6, max(1, y - r + 1), z + 1.4), 'spod')
        rodzic = n
    # przyssawka: dysk z przodu seg1, wnęka gardła, 8 zębów na obwodzie
    z0, y0, r0 = SEG[0]
    m.czesc('paszcza', (0, y0, z0 - 2), 'seg1')
    m.elipsoida('paszcza', (0, y0, z0 - 2.2), (r0 + .4, r0 + .3, 1.3), 'warga')
    m.wneka('paszcza', (0, y0, z0 - 3.6), (r0 - 1.0, r0 - 1.1, 1), 3, ('warga', 'warga'))
    for k in range(9):                                   # 9 grubych, nieregularnych zębów
        a = k / 9 * 2 * pi + (.15 if k % 2 else -.1)
        rr = r0 - (.4 if k % 3 else .9)
        x, y = rr * cos(a), y0 + rr * sin(a)
        h = 1.5 if k % 2 else 1.0
        m.detal('paszcza', f'zab_{k}', (x - .75, y - .75, z0 - 3.9 - (h - 1)), (x + .75, y + .75, z0 - 2.9), 'zab')
    m.czesc('swiatlo_paszcza', (0, y0, z0 - 3), 'paszcza')
    m.odejmij('seg1', 'elipsoida', (0, y0, z0 - 2.2), (r0 - 1.4, r0 - 1.5, 1.6))     # lej przechodzi też przez pierwszy segment
    m.detal('swiatlo_paszcza', 'gardlo_dno', (-1, y0 - 1, z0 - 1.2), (1, y0 + 1, z0 - .2), 'gardlo')
    m.czesc('p_paszcza_pkt', (0, y0, z0 - 3.5), 'paszcza')
    # ogon w mule: kałuża mułu pod ostatnim segmentem
    m.czesc('mul', (0, 0, 9), 'korzen')
    m.elipsoida('mul', (0, .4, 9.5), (3.4, 1.2, 3.0), 'mul')
    # rzęsa: kępy na grzbiecie i zwisy po bokach
    m.czesc('rzesa', (0, 9, 0), 'seg3')
    for i, (x, z, y, l) in enumerate(((-2.6, -6, 7.5, 3), (2.7, -5, 7.6, 2), (-2.6, -1, 8.3, 3), (2.6, 2, 7.8, 3),
                                      (-2.2, 5, 6, 2), (2.0, 5.5, 6, 2))):
        m.detal('rzesa', f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    for i, (si, x, w) in enumerate(((1, -1, 2), (2, 0, 3), (3, -1, 2))):    # kępy leżą NA grzbiecie segmentu
        z, y, r = SEG[si]
        top = int(y + r * .95)
        m.detal('rzesa', f'kepa_{i}', (x - .5, top - .5, z - 1), (x - .5 + w, top + .5, z + 1), 'rzesa')
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/pijawka'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_pijawka.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_pijawka', hitbox=(10, 7, 4), tag_y=12)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 2.0, {'seg2': ('rotation', (2, 0, 0)), 'seg4': ('rotation', (-2, 2, 0)), 'seg6': ('rotation', (0, 4, 0))})
    from anim_pijawka import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
