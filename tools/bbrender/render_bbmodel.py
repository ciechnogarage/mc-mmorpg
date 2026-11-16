"""Offline podglad modeli .bbmodel (Blockbench) bez Blockbencha i bez klienta MC.

Rysuje poze spoczynkowa: szesciany i siatki z teksturami osadzonymi w pliku, rzut ortogonalny,
cieniowanie scian jak w MC. Sluzy do PRZEGLADU i wyboru modeli; ostateczna ocena i tak w kliencie QA.

Uzycie (w obrazie pixelart, bo potrzebny numpy + Pillow):
  tools/pixelart/uruchom.sh tools/bbrender/render_bbmodel.py OUT.png model1.bbmodel [model2.bbmodel ...]
    [--kolumny 4] [--rozmiar 320] [--podpis "nazwa1" ...]
Kazdy model = kafel z dwoma widokami (3/4 z przodu, 3/4 z tylu) i podpisem.
"""
import argparse
import base64
import io
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

POMIN_KOSCI = {'hitbox', 'shadow'}  # kosci techniczne ModelEngine
# fragmenty nazw kosci-efektow, ktore ModelEngine pokazuje tylko w trakcie animacji/skilla
POMIN_FRAGMENTY = ('shadow', 'slash', 'telegraph', 'splash', '_fx', 'fx_', 'hitbox')


def rot_matrix(deg):
    """Blockbench/three.js: Euler 'ZYX' -> M = Rz * Ry * Rx."""
    x, y, z = (math.radians(a) for a in deg)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def rot_of(obj):
    r = obj.get('rotation', [0, 0, 0])
    if isinstance(r, dict):  # stary format: {angle, axis, origin}
        v = [0, 0, 0]
        v['xyz'.index(r.get('axis', 'y'))] = r.get('angle', 0)
        return v
    return r or [0, 0, 0]


def wczytaj_tekstury(model):
    res = model.get('resolution', {'width': 16, 'height': 16})
    tex = []
    for t in model.get('textures', []):
        src = t.get('source', '')
        img = None
        if src.startswith('data:image'):
            img = np.asarray(Image.open(io.BytesIO(base64.b64decode(src.split(',', 1)[1]))).convert('RGBA'))
        uw = t.get('uv_width', res['width']) or res['width']
        uh = t.get('uv_height', res['height']) or res['height']
        tex.append((img, uw, uh))
    return tex


class Transform:
    """Lancuch obrotow od elementu do korzenia: lista (macierz, origin)."""

    def __init__(self, chain):
        self.chain = chain

    def __call__(self, p):
        for m, o in self.chain:
            p = (p - o) @ m.T + o
        return p


def ukryte_przez_animacje(model):
    """Kosci ze skala 0 w pierwszej klatce animacji spoczynkowej (idle) - ModelEngine chowa tak efekty."""
    anims = model.get('animations') or []
    idle = next((a for a in anims if a.get('name', '').lower() in ('idle', 'animation.idle')), None)
    idle = idle or next((a for a in anims if 'idle' in a.get('name', '').lower()), None)
    ukryte = set()
    for uuid, an in ((idle or {}).get('animators') or {}).items():
        kl = [k for k in an.get('keyframes', []) if k.get('channel') == 'scale']
        if not kl:
            continue
        k0 = min(kl, key=lambda k: float(k.get('time', 0)))
        dp = (k0.get('data_points') or [{}])[0]
        try:
            if all(abs(float(dp.get(o, 1))) < 1e-3 for o in 'xyz'):
                ukryte.add(uuid)
        except (TypeError, ValueError):  # wyrazenia molang - nie ruszamy
            pass
    return ukryte


def zbierz(model):
    """Flaga visibility z edytora jest pomijana - ModelEngine jej nie uzywa (autorzy ukrywaja w edytorze cale ciala).
    Zwraca liste (element, Transform) widocznych elementow wg drzewa outlinera."""
    ukryte = ukryte_przez_animacje(model)
    elems = {e['uuid']: e for e in model.get('elements', [])}
    groups = {g['uuid']: g for g in model.get('groups', [])} if isinstance(model.get('groups'), list) else {}
    wynik, uzyte = [], set()

    def grupa(g, chain):
        if g.get('name', '').lower() in POMIN_KOSCI or any(f in g.get('name', '').lower() for f in POMIN_FRAGMENTY) or g.get('uuid') in ukryte:
            return
        o = np.array(g.get('origin', [0, 0, 0]), float)
        chain = [(rot_matrix(rot_of(g)), o)] + chain
        for ch in g.get('children', []):
            wezel(ch, chain)

    def wezel(n, chain):
        if isinstance(n, dict):
            grp = dict(groups.get(n.get('uuid'), {}), **n)
            grupa(grp, chain)
        elif n in elems:
            e = elems[n]
            uzyte.add(n)
            o = np.array(e.get('origin', [0, 0, 0]), float)
            wynik.append((e, Transform([(rot_matrix(rot_of(e)), o)] + chain)))
        elif n in groups:
            grupa(groups[n], chain)

    outliner = model.get('outliner') or list(elems)
    for n in outliner:
        wezel(n, [])
    return wynik


# naroza scian szescianu: (TL, TR, BL) jako wybor from(0)/to(1) na osiach x,y,z
SCIANY = {
    'north': ((1, 1, 0), (0, 1, 0), (1, 0, 0)),
    'south': ((0, 1, 1), (1, 1, 1), (0, 0, 1)),
    'east': ((1, 1, 1), (1, 1, 0), (1, 0, 1)),
    'west': ((0, 1, 0), (0, 1, 1), (0, 0, 0)),
    'up': ((0, 1, 0), (1, 1, 0), (0, 1, 1)),
    'down': ((0, 0, 1), (1, 0, 1), (0, 0, 0)),
}


def czworokaty(e, tf):
    """Szescian -> lista (TL, TR, BL, uv4, rotacja_uv, tekstura)."""
    inf = e.get('inflate', 0)
    a = np.array(e['from'], float) - inf
    b = np.array(e['to'], float) + inf
    ab = (a, b)
    out = []
    for nazwa, face in (e.get('faces') or {}).items():
        if nazwa not in SCIANY or face.get('texture') is None or 'uv' not in face:
            continue
        pts = [np.array([ab[s[0]][0], ab[s[1]][1], ab[s[2]][2]]) for s in SCIANY[nazwa]]
        tl, tr, bl = (tf(p) for p in pts)
        out.append((tl, tr, bl, face['uv'], face.get('rotation', 0) or 0, face['texture']))
    return out


def trojkaty_siatki(e, tf):
    """Siatka (mesh) -> lista trojkatow (p0,p1,p2, uv0,uv1,uv2, tekstura)."""
    o = np.array(e.get('origin', [0, 0, 0]), float)
    verts = {k: tf(np.array(v, float) + o) for k, v in e.get('vertices', {}).items()}  # wierzcholki wzgledem origin
    out = []
    for face in (e.get('faces') or {}).values():
        vs = face.get('vertices', [])
        uv = face.get('uv', {})
        if face.get('texture') is None or len(vs) < 3 or any(v not in uv or v not in verts for v in vs):
            continue
        for i in range(1, len(vs) - 1):
            k = (vs[0], vs[i], vs[i + 1])
            out.append(tuple(verts[x] for x in k) + tuple(np.array(uv[x], float) for x in k) + (face['texture'],))
    return out


def widok(yaw, pitch):
    """Macierz kamery: obrot wokol Y o yaw, potem pochylenie o pitch. Kamera patrzy w -Z."""
    y, p = math.radians(yaw), math.radians(pitch)
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    return rx @ ry


SWIATLO = np.array([0.35, 0.9, 0.5])
SWIATLO = SWIATLO / np.linalg.norm(SWIATLO)


def renderuj(model, yaw, pitch, rozmiar):
    tex = wczytaj_tekstury(model)
    prymitywy = []  # (punkty 3D [n,3], uv [n,2], tekstura) - probki gotowe do rzutu
    for e, tf in zbierz(model):
        if e.get('type', 'cube') == 'cube' and 'from' in e:
            for tl, tr, bl, uv, rot, ti in czworokaty(e, tf):
                prymitywy.append(('q', tl, tr, bl, uv, rot, ti))
        elif e.get('type') == 'mesh':
            for t in trojkaty_siatki(e, tf):
                prymitywy.append(('t',) + t)
    if not prymitywy:
        return None, 0
    V = widok(yaw, pitch)
    wszystkie = np.array([p for pr in prymitywy for p in pr[1:4]])
    cam = wszystkie @ V.T
    lo, hi = cam[:, :2].min(0), cam[:, :2].max(0)
    skala = (rozmiar * 0.88) / max(hi[0] - lo[0], hi[1] - lo[1], 1e-6)
    srodek = (lo + hi) / 2
    obraz = np.zeros((rozmiar, rozmiar, 4), np.uint8)
    zbuf = np.full((rozmiar, rozmiar), -np.inf)

    def rysuj(P, UV, ti, normal):
        img, uw, uh = tex[ti] if 0 <= ti < len(tex) else (None, 16, 16)
        if img is None:
            return
        h, w = img.shape[:2]
        tu = np.clip((UV[:, 0] * w / uw).astype(int), 0, w - 1)
        tv = np.clip((UV[:, 1] * h / uh).astype(int), 0, h - 1)
        kol = img[tv, tu].astype(float)
        m = kol[:, 3] > 127
        if not m.any():
            return
        n = normal @ V.T
        if n[2] < 0:  # sciana tylem do kamery - i tak narysuj (cienkie plaszczyzny dwustronne), ale ciemniej
            n = -n
        jas = 0.55 + 0.45 * max(0.0, float(normal @ SWIATLO)) if np.linalg.norm(normal) else 1.0
        C = P[m] @ V.T
        x = ((C[:, 0] - srodek[0]) * skala + rozmiar / 2).astype(int)
        y = (rozmiar / 2 - (C[:, 1] - srodek[1]) * skala).astype(int)
        z = C[:, 2]
        ok = (x >= 0) & (x < rozmiar) & (y >= 0) & (y < rozmiar)
        x, y, z, k = x[ok], y[ok], z[ok], kol[m][ok]
        order = np.argsort(z)  # od najdalszych: blizsze nadpisza
        x, y, z, k = x[order], y[order], z[order], k[order]
        lepsze = z > zbuf[y, x]
        x, y, z, k = x[lepsze], y[lepsze], z[lepsze], k[lepsze]
        zbuf[y, x] = z
        k[:, :3] *= jas
        obraz[y, x] = np.clip(k, 0, 255).astype(np.uint8)

    for pr in prymitywy:
        if pr[0] == 'q':
            _, tl, tr, bl, uv, rot, ti = pr
            du, dv = tr - tl, bl - tl
            n_s = max(2, int(np.linalg.norm(du) * skala * 2.2) + 1)
            n_t = max(2, int(np.linalg.norm(dv) * skala * 2.2) + 1)
            s, t = np.meshgrid(np.linspace(0.001, 0.999, n_s), np.linspace(0.001, 0.999, n_t))
            s, t = s.ravel(), t.ravel()
            P = tl + np.outer(s, du) + np.outer(t, dv)
            # obrot UV sciany (co 90 st.) = obrot wspolrzednych probkowania
            su, tu_ = s, t
            for _ in range(int(rot) // 90 % 4):
                su, tu_ = tu_, 1 - su
            u1, v1, u2, v2 = uv
            UV = np.stack([u1 + su * (u2 - u1), v1 + tu_ * (v2 - v1)], 1)
            normal = np.cross(dv, du)
            nn = np.linalg.norm(normal)
            rysuj(P, UV, ti, normal / nn if nn else normal)
        else:
            _, p0, p1, p2, q0, q1, q2, ti = pr
            e1, e2 = p1 - p0, p2 - p0
            n = max(3, int(max(np.linalg.norm(e1), np.linalg.norm(e2)) * skala * 2.2) + 1)
            a, b = np.meshgrid(np.linspace(0, 1, n), np.linspace(0, 1, n))
            a, b = a.ravel(), b.ravel()
            m = a + b <= 1
            a, b = a[m], b[m]
            P = p0 + np.outer(a, e1) + np.outer(b, e2)
            UV = q0 + np.outer(a, q1 - q0) + np.outer(b, q2 - q0)
            normal = np.cross(e1, e2)
            nn = np.linalg.norm(normal)
            rysuj(P, UV, ti, normal / nn if nn else normal)
    return Image.fromarray(obraz), len(prymitywy)


def kafel(sciezka, podpis, rozmiar):
    model = json.load(open(sciezka))
    tlo = (58, 62, 70, 255)
    widoki = [tuple(map(float, w.split(':'))) for w in os.environ['WIDOKI'].split(',')] if os.environ.get('WIDOKI') else [(145, 20), (-35, 20)]
    k = Image.new('RGBA', (rozmiar * len(widoki), rozmiar + 22), tlo)
    for i, (yaw, pitch) in enumerate(widoki):  # przod 3/4 (model patrzy w -Z), tyl 3/4
        img, n = renderuj(model, yaw, pitch, rozmiar)
        if img is not None:
            k.alpha_composite(img, (i * rozmiar, 0))
    ImageDraw.Draw(k).text((6, rozmiar + 5), podpis, fill=(235, 235, 235, 255))
    return k


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('out')
    ap.add_argument('modele', nargs='+')
    ap.add_argument('--kolumny', type=int, default=3)
    ap.add_argument('--rozmiar', type=int, default=300)
    ap.add_argument('--podpis', nargs='*')
    a = ap.parse_args(argv)
    podpisy = a.podpis or [m.rsplit('/', 1)[-1].replace('.bbmodel', '') for m in a.modele]
    kafle = []
    for m, p in zip(a.modele, podpisy):
        try:
            kafle.append(kafel(m, p, a.rozmiar))
        except Exception as exc:  # jeden zly model nie blokuje arkusza
            print(f'BLAD {m}: {exc}', file=sys.stderr)
    if not kafle:
        return 1
    w, h = kafle[0].size
    kol = min(a.kolumny, len(kafle))
    rzedy = math.ceil(len(kafle) / kol)
    arkusz = Image.new('RGBA', (w * kol, h * rzedy), (40, 42, 48, 255))
    for i, k in enumerate(kafle):
        arkusz.alpha_composite(k, ((i % kol) * w, (i // kol) * h))
    arkusz.convert('RGB').save(a.out)
    print(f'{a.out}: {len(kafle)} modeli')
    return 0


if __name__ == '__main__':
    sys.exit(main())
