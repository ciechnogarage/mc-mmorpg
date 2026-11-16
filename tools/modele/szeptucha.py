"""Szeptucha K1 (CLAUDE, 03.10): zgarbiona zielarka — stożek płaszcza z sieci rybackiej przechodzi w spiczasty kaptur
odchylony do tyłu; twarz w cieniu kaptura (długi nos, turkusowe oczy); kościste ręce na kotle trzymanym przed sobą,
w kotle świecący wywar (kanał Wywaru = punkt skupienia); chochla na długiej lasce; korale z muszli; rzęsa."""
from pathlib import Path
import json
import numpy as np
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
from kokoniara import segment
import tekstura
import atlas_wspolny

MAT = {
 'plaszcz': {'ramp': [(38, 50, 40), (54, 70, 56), (72, 90, 72), (86, 104, 84)], 'plamy': .1},
 'siec':    {'ramp': [(74, 80, 62), (98, 104, 82), (122, 126, 100), (140, 144, 116)]},
 'cien':    {'ramp': [(12, 14, 12)] * 4},
 'skora':   {'ramp': [(92, 100, 78), (116, 124, 98), (140, 146, 118), (160, 164, 136)]},
 'turkus':  {'ramp': [(20, 80, 70), (36, 130, 114), (63, 209, 180), (150, 240, 220)]},
 'kociol':  {'ramp': [(26, 28, 28), (38, 40, 40), (52, 54, 54), (66, 68, 68)]},
 'drewno':  {'ramp': [(60, 42, 26), (84, 60, 38), (108, 80, 52), (126, 96, 64)]},
 'kosc':    {'ramp': [(150, 142, 116), (184, 176, 148), (212, 204, 178), (234, 228, 206)]},
 'rzesa':   {'ramp': [(52, 70, 22), (74, 96, 30), (100, 124, 42), (126, 150, 60)]},
 'mul':     {'ramp': [(46, 32, 22), (68, 48, 32), (92, 66, 44), (118, 88, 60)]},
}


def buduj():
    m = Model(MAT, budzet=480, atlas=256)
    m.czesc('korzen', (0, 0, 0))
    # płaszcz: szeroki dół, zgarbiona góra wysunięta do przodu
    m.czesc('tulow', (0, 10, 1), 'korzen')
    m.elipsoida('tulow', (0, 7, 1.5), (7.5, 7.5, 6.5), 'plaszcz')
    m.kapsula('tulow', (0, 9, 1.5), (0, 17, .5), 6.6, 4.4, 'plaszcz')
    m.elipsoida('tulow', (-3.5, 16, 2.5), (5.3, 4.5, 4.8), 'plaszcz')  # nierówny garb
    m.elipsoida('tulow', (4, 13, 3), (3.5, 3.2, 4.2), 'plaszcz')
    m.odejmij('tulow', 'pudlo', (-20, -5, -20), (20, 0, 20))
    # siatka sieci na powierzchni płaszcza
    c = m.czesci['tulow']; pelne = set(c.vox)
    for (x, y, z) in list(c.vox):
        pow_ = any((x + dx, y + dy, z + dz) not in pelne for dx, dy, dz in ((1,0,0),(-1,0,0),(0,0,1),(0,0,-1)))
        if pow_ and y > 2 and (y % 4 == 0 or (x + z) % 4 == 0):
            c.vox[x, y, z] = 'siec'
    # kaptur: spiczasty, odchylony do tyłu; otwór z cieniem z przodu
    m.czesc('glowa', (0, 18, -1), 'tulow')
    m.kapsula('glowa', (0, 20, -2), (0, 25.5, .5), 4.4, 3.0, 'plaszcz')
    m.kapsula('glowa', (0, 25.5, .5), (0, 31, 4.5), 3.0, .8, 'plaszcz')
    m.odejmij('glowa', 'elipsoida', (0, 20.5, -6.4), (3.0, 3.4, 2.8))
    m.pudlo('glowa', (-2, 18, -4), (2, 23, -3), 'cien')                       # cień we wnętrzu kaptura
    # twarz w cieniu: długi nos, oczy (świecące), broda
    m.detal('glowa', 'twarz', (-1.7, 17.5, -6.4), (1.7, 21, -4.2), 'skora')
    m.kapsula('glowa', (0, 20, -6), (0, 17.5, -11), 1.15, .75, 'skora')
    m.czesc('swiatlo_oczy', (0, 21, -5), 'glowa')
    for s in (-1, 1):
        m.detal('swiatlo_oczy', f'oko_{s}', (s * 1.4 - .5, 20.5, -7.2), (s * 1.4 + .5, 21.5, -6.2), 'turkus')
    # korale z muszli na szyi
    m.czesc('korale', (0, 16, -3), 'tulow')
    for i, x in enumerate((-4, -2.5, -1, .5, 2, 3.5)):
        y = 15.5 - (1.5 - abs(i - 2.5) * .5)
        m.detal('korale', f'muszla_{i}', (x - .5, y, -6.2 + abs(x) * .35), (x + .5, y + 1, -5.2 + abs(x) * .35), 'kosc')
    m.kapsula('korale', (-4.5, 15.2, -4.4), (4.5, 15.2, -4.4), 1.5, 1.5, 'siec')
    # ręce: kościste przedramiona do kotła
    for s, st in ((-1, 'l'), (1, 'r')):
        r = 'reka_' + st
        m.czesc(r, (s * 5.2, 16, -1.5), 'tulow')
        m.kapsula(r, (s * 5.2, 16, -1.5), (s * 7.5, 12.5, -5.5), 1.7, 1.25, 'plaszcz')
        m.elipsoida(r, (s * 7.5, 12.5, -5.5), (1.6, 1.5, 1.6), 'skora')
        m.kapsula(r, (s * 7.5, 12.5, -5.5), (s * 4.2, 12.5, -9.5), 1.15, .85, 'skora')
    # kocioł przed sobą na wysokości bioder + wywar
    m.czesc('kociol', (0, 10, -9), 'tulow')
    m.elipsoida('kociol', (0, 7.7, -10), (4.5, 4.2, 4.6), 'kociol')
    m.odejmij('kociol', 'pudlo', (-10, 10.5, -20), (10, 20, 0))
    m.odejmij('kociol', 'elipsoida', (0, 10.5, -10), (3.5, 1.4, 3.4))
    m.pudlo('kociol', (-4.5, 9.8, -15), (4.5, 11, -14), 'kociol')
    for x in (-3, 2):
        m.detal('kociol', f'kropla_wywaru_{x}', (x, 7.8, -15.2), (x+1, 10.5, -14.2), 'turkus')
    m.czesc('swiatlo_kociol', (0, 10.5, -10), 'kociol')
    m.elipsoida('swiatlo_kociol', (0, 10.2, -10), (3.8, .9, 3.2), 'turkus')
    for i, (x, z) in enumerate(((-2, -11), (1, -9), (2.5, -11.5))):         # bąble
        m.detal('swiatlo_kociol', f'babel_{i}', (x, 11, z), (x + 1, 12, z + 1), 'turkus')
    m.czesc('p_kociol_pkt', (0, 12, -10), 'kociol')
    m.czesc('p_dlon', (-4.2, 12.5, -9.5), 'reka_l')
    m.czesc('fx_flaszka', (-4.2, 12.5, -9.5), 'reka_l')                     # flaszka do rzutu (ukryta poza throw)
    m.detal('fx_flaszka', 'flaszka', (-5.2, 12.5, -10.5), (-3.2, 15.5, -8.5), 'kociol')
    m.detal('fx_flaszka', 'wywar_flaszki', (-5.2, 13, -10.7), (-3.2, 14.5, -10.5 + 1), 'turkus')
    # chochla na lasce: w kotle, laska wychodzi ukośnie nad ramię
    I = np.eye(3)
    segment(m, 'chochla', 'reka_r', I, (1.5, 10.5, -10), (13, 30, 1), 2, 'drewno')
    m.czesc('glowka_chochli', (1.5, 10, -10), 'reka_r')
    m.pudlo('glowka_chochli', (0, 9, -11.5), (3, 11, -8.5), 'drewno')
    # rzęsa i muł
    m.czesc('rzesa', (0, 24, 0), 'glowa')
    for i, (x, z, y, l) in enumerate(((-4, -3, 24, 5), (4, -3, 24, 6), (-4.5, 1, 23, 7), (4.5, 1.5, 22, 6), (0, 4, 28, 5),
                                      (-6, 0, 16, 4), (6, -1, 15, 5), (-7, 3, 11, 3))):
        part = 'rzesa' if y > 20 else 'tulow'
        m.detal(part, f'zwis_{i}', (x - .5, y - l, z - .5), (x + .5, y, z + .5), 'rzesa')
    m.czesc('mul', (0, 0, 1), 'korzen')
    m.elipsoida('mul', (0, .3, 1.5), (8.5, 1.0, 7.5), 'mul')
    m.uprosc(['tulow'], krok=2, prog=.5) if False else None
    print('dosunięte:', m.dosun_detale(pomin=()))
    return m


def main():
    out = Path('/opt/docker/mc/data/world1/output/lochy/bagna/modele/szeptucha'); out.mkdir(parents=True, exist_ok=True)
    path = out / 'bagna_szeptucha.bbmodel'
    old = tekstura.atlas
    try:
        tekstura.atlas = atlas_wspolny.atlas
        r = buduj().zapisz(path, 'bagna_szeptucha', hitbox=(12, 29, 22), tag_y=36)
    finally:
        tekstura.atlas = old
    dodaj_idle(path, 2.0, {'glowa': ('rotation', (3, 4, 0)), 'swiatlo_kociol': ('scale', (.05, .1, .05))})
    from anim_szeptucha import main as dodaj_animacje
    dodaj_animacje(path)
    (out / 'raport_sdf.json').write_text(json.dumps(r, indent=2)); print('kostki', r['razem'])


if __name__ == '__main__':
    main()
