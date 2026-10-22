"""Szkice bossa: kostki w 5 walorach (1 najciemniejszy .. 5 swiatlo), kosci z dowolnym obrotem dla skosow."""
import sys
sys.path.insert(0, '/home/dev/Desktop/Przemek/MC/tools/modele')
import bb
from bb import kosc as B, szescian as _S


def S(n, od, do, w, **kw):
    return _S(n, od, do, w, **kw)


def lustro(n, od, do, w):
    """para kostek symetrycznych wzgledem X=0"""
    return [S(n + '_l', od, do, w), S(n + '_p', (-do[0], od[1], od[2]), (-od[0], do[1], do[2]), w)]


def zapisz(sciezka, nazwa, outliner):
    bb.zapisz(sciezka, nazwa, outliner, bb.tekstura_blockout(), 16, [])


_licz = [0]


def _n(p):
    _licz[0] += 1
    return f'{p}{_licz[0]}'


def kepa(x, y, z, r, w=4):
    """nieregularna kepa (kwiaty/liscie) wokol punktu: 3 kostki roznej wielkosci, walor w"""
    return [S(_n('kw'), (x - r, y - r * 0.5, z - r), (x + r, y + r * 0.5, z + r), w),
            S(_n('kw'), (x - r * 0.4 - 2, y + r * 0.3, z - r * 0.5), (x + r * 0.6, y + r * 0.9 + 1, z + r * 0.6), w),
            S(_n('kw'), (x + r * 0.3, y - r * 0.8 - 1, z - r * 0.2), (x + r * 1.2, y - r * 0.1, z + r * 0.9), w)]


def konar(o, dl, grub, rot, rozw=2, kw=4, wal=2, kwiat_w=4, widly=((0, 0, 32), (0, 0, -28))):
    """konar od punktu o w gore (przed obrotem), obrocony koscia; na koncu rozwidlenie albo kepa kwiatow"""
    x, y, z = o; g = max(grub / 2, 0.5)
    dz = [S(_n('konar'), (x - g, y, z - g), (x + g, y + dl, z + g), wal)]
    tip = (x, y + dl, z)
    if rozw:
        dz.append(konar(tip, dl * 0.62, max(grub * 0.7, 1), widly[0], rozw - 1, kw, wal, kwiat_w, widly))
        dz.append(konar(tip, dl * 0.55, max(grub * 0.65, 1), widly[1], rozw - 1, kw, wal, kwiat_w, widly))
    elif kw:
        dz += kepa(x, y + dl, z, kw, kwiat_w)
    return B(_n('b_konar'), o, dz, rot=rot)


def pasmo(x, y, z, dl, szer=2, w=1):
    """wiszace pasmo mchu (plaszczyzna w XY)"""
    return S(_n('mech'), (x, y - dl, z), (x + szer, y, z), w)
