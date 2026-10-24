"""Model 3D korzenia kozlka (wyrwanie i lot do reki w trybie feeling): trzy RÓŻNE pomysłem warianty.
  a = gruby kłącz z korzonkami (ciemny, sękaty walec + 7 grubych korzeni)
  b = wiązka jasnych korzeni z resztką ziemi (mały kłącz + broda 13 kremowych korzeni, grudka ziemi w środku)
  c = korzeń z rozetą liści u góry (kłącz + krótkie korzenie + ogonki i trzy ucięte liście)
Wspólna tekstura 32x32 (atlas): kora kłącza, korzenie, ziemia, łodyga, liść (kopia karty kozlek_lisc).
Siatka 1:1 (wymiar >= 1 albo płaszczyzna 0), 1 teksel na jednostkę modelu jak karty zbliżenia (16 px na 0,62 bloku).
Obroty tylko po jednej osi, co 22,5 st. (model przedmiotu Javy).

Użycie: korzen3d.py <tex> <nexo_out> [bbmodel_out]
  -> <tex>/kozlek_korzen3d.png, <nexo_out>/pack/assets/nexo/models/zielarz/korzen3d_{a,b,c}.json
  -> [bbmodel_out]/korzen3d_{a,b,c}.bbmodel (podgląd tools/bbrender/render.sh)
"""
import base64, colorsys, io, json, os, sys, uuid
from PIL import Image

W = 32   # atlas 32x32 px; uv modelu Javy = px / 2


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def miesz(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def grad(stopnie, t):
    """stopnie = [(t, kolor), ...] rosnąco; liniowo między nimi."""
    for (t0, c0), (t1, c1) in zip(stopnie, stopnie[1:]):
        if t <= t1:
            return miesz(c0, c1, (t - t0) / (t1 - t0) if t1 > t0 else 0)
    return stopnie[-1][1]


def odcien(c, dh=0.0, dv=1.0):
    h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in c))
    return tuple(round(x * 255) for x in colorsys.hsv_to_rgb((h + dh) % 1, s, min(1, v * dv)))


# ---------------------------------------------------------------- regiony atlasu (px): x0, y0, x1, y1
KORA = (0, 0, 12, 8)       # kłącz: gora jasna (swiatlo), dol AO; dwa miękkie pierścienie węzłów
KORZ = (12, 0, 32, 13)     # korzenie: kolumna = jeden korzeń (lekko inny odcień), v0 = nasada (AO), potem jasno, końcówka ciemniej
ZIEM = (0, 8, 12, 16)      # ziemia: miękkie grudy, AO u dołu
LOD = (0, 16, 6, 24)       # ogonek/łodyga: jasna góra, ciemna nasada
CIETY = (6, 16, 8, 18)     # ucięty koniec łodygi (jasny środek)
LISC = (16, 16, 32, 32)    # liść (karta kozlek_lisc)


def maluj(tex_dir):
    im = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    px = im.load()
    # kora: hue-shift (światło cieplejsze/żółtsze, cień chłodniejszy/czerwieńszy), bez szumu per piksel
    kora = [(0, hx('c29a62')), (0.18, hx('a07a48')), (0.55, hx('7a5a36')), (0.85, hx('5e4228')), (1, hx('3f2b1b'))]
    x0, y0, x1, y1 = KORA
    for y in range(y0, y1):
        t = (y - y0) / (y1 - y0 - 1)
        for x in range(x0, x1):
            c = grad(kora, t)
            # węzły kłącza: dwa miękkie pierścienie (kolumny 3-4 i 8), niski kontrast
            if x in (3, 8): c = odcien(c, 0.005, 0.86)
            if x in (4, 9): c = odcien(c, 0, 0.94)
            px[x, y] = c + (255,)
    # korzenie: każdy korzeń (kolumna) lekko inny, długość do 13
    korz = [(0, hx('9c8256')), (0.12, hx('e9d9ab')), (0.55, hx('dcc690')), (0.85, hx('c4a874')), (1, hx('a88a5a'))]
    x0, y0, x1, y1 = KORZ
    for x in range(x0, x1):
        k = x - x0
        dv = (1.0, 0.94, 1.03, 0.9, 0.97, 1.05, 0.92, 1.0, 0.96, 1.02)[k % 10]
        dh = (0, 0.006, -0.004, 0.01, 0, -0.006, 0.004, 0, 0.008, -0.002)[k % 10]
        for y in range(y0, y1):
            t = (y - y0) / (y1 - y0 - 1)
            px[x, y] = odcien(grad(korz, t), dh, dv) + (255,)
    # ziemia: gruda (jaśniejsza góra), AO u dołu, dwie miękkie jaśniejsze bryłki
    ziem = [(0, hx('7e5a3c')), (0.35, hx('6a4a31')), (0.75, hx('523826')), (1, hx('38261a'))]
    x0, y0, x1, y1 = ZIEM
    for y in range(y0, y1):
        t = (y - y0) / (y1 - y0 - 1)
        for x in range(x0, x1):
            c = grad(ziem, t)
            for (bx, by, r) in ((2.5, 10.5, 1.6), (8.5, 12.5, 1.8)):
                d = ((x - bx) ** 2 + (y - by) ** 2) ** 0.5
                if d < r: c = odcien(c, 0.004, 1.0 + 0.12 * (1 - d / r))
            px[x, y] = c + (255,)
    lod = [(0, hx('8cc860')), (0.3, hx('5aa236')), (0.75, hx('3c7a2a')), (1, hx('2b4f22'))]
    x0, y0, x1, y1 = LOD
    for y in range(y0, y1):
        t = (y - y0) / (y1 - y0 - 1)
        for x in range(x0, x1):
            px[x, y] = odcien(grad(lod, t), 0, (1.04, 1.0, 0.95, 1.02, 0.97, 1.0)[x - x0]) + (255,)
    for x in range(CIETY[0], CIETY[2]):
        for y in range(CIETY[1], CIETY[3]):
            px[x, y] = hx('b9df86') + (255,)
    # liść: karta zbliżenia (ten sam liść co rozeta)
    lisc = os.path.join(tex_dir, 'kozlek_lisc.png')
    if os.path.exists(lisc):
        im.alpha_composite(Image.open(lisc).convert('RGBA'), (LISC[0], LISC[1]))
    return im


# ---------------------------------------------------------------- geometria
def region_uv(reg, du, dv, w, h):
    """Prostokąt w regionie (px), przesunięty o (du, dv), zawinięty do wymiaru regionu."""
    x0, y0, x1, y1 = reg
    rw, rh = x1 - x0, y1 - y0
    w, h = min(w, rw), min(h, rh)
    u = x0 + (du % max(1, rw - w + 1))
    v = y0 + (dv % max(1, rh - h + 1))
    return [u, v, u + w, v + h]


def kostka(a, b, reg, rot=None, du=0, dv=0, gora=None, dol=None, dwustronna=False):
    """Prostopadłościan a..b; boki z regionu reg (pion = oś Y modelu), góra/dół z gora/dol (domyślnie reg)."""
    sx, sy, sz = (b[i] - a[i] for i in range(3))
    f = {}
    f['north'] = region_uv(reg, du, dv, sx, sy)
    f['south'] = region_uv(reg, du + 1, dv, sx, sy)
    f['east'] = region_uv(reg, du + 2, dv, sz, sy)
    f['west'] = region_uv(reg, du + 3, dv, sz, sy)
    f['up'] = region_uv(gora or reg, du, 0, sx, sz)
    f['down'] = region_uv(dol or reg, du, 99, sx, sz)
    if dwustronna:   # płaszczyzna (liść): tylko przód i tył
        f = {'north': region_uv(reg, 0, 0, sx or sz, sy), 'south': region_uv(reg, 0, 0, sx or sz, sy)}
    return {'from': list(a), 'to': list(b), 'rot': rot, 'faces': f}


def korzen(x, z, y_top, dl, os_, kat, kol):
    """Belka 1x1 zwisająca z (x, y_top, z), dł. dl, odchylona o kat wokół osi os_ w punkcie zaczepienia."""
    return kostka((x, y_top - dl, z), (x + 1, y_top, z + 1), KORZ, rot=(os_, kat, (x + 0.5, y_top, z + 0.5)), du=kol, dv=0,
                  gora=KORA, dol=KORZ)


def lodyga(x, z, y0, dl, os_, kat):
    return kostka((x, y0, z), (x + 1, y0 + dl, z + 1), LOD, rot=(os_, kat, (x + 0.5, y0, z + 0.5)), gora=CIETY, dol=LOD)


def wariant_a():
    """Gruby kłącz z korzonkami: przysadzista, ciemna bulwa (sylwetka „ziemniaka z pazurami”), kilka grubych korzeni
    w korze i krótkie jasne korzonki - ciężki, ciemny, zupełnie inny obrys niż broda B."""
    e = [kostka((4, 8, 5), (12, 13, 11), KORA),                                       # bulwa 8x5x6
         kostka((10, 9, 6), (14, 12, 10), KORA, rot=('z', -22.5, (11, 10, 8)), du=3),   # sęk w prawo, opada
         kostka((2, 9, 6), (6, 12, 9), KORA, rot=('z', 22.5, (5, 10, 7.5)), du=6),      # sęk w lewo
         kostka((6, 13, 6), (10, 14, 10), KORA, du=1, dv=0),                            # garb na górze
         lodyga(7, 7, 14, 2, 'x', 0), lodyga(9, 8, 14, 1, 'z', -22.5)]                  # ucięte nasady łodyg
    for (x, z, dl, os_, kat, du) in ((5, 6, 6, 'z', -22.5, 0), (9, 8, 7, 'x', -22.5, 4), (7, 5, 5, 'x', 22.5, 8)):   # grube korzenie 2x2 w korze
        e.append(kostka((x, 9 - dl, z), (x + 2, 9, z + 2), KORA, rot=(os_, kat, (x + 1, 9, z + 1)), du=du, gora=KORA, dol=ZIEM))
    for (x, z, dl, os_, kat, kol) in ((4, 9, 5, 'z', -45, 1), (11, 6, 5, 'z', 45, 7), (11, 10, 4, 'x', -45, 13), (6, 10, 5, 'x', -45, 16),
                                      (8, 5, 4, 'x', 45, 3)):
        e.append(korzen(x, z, 9, dl, os_, kat, kol))
    return e


def wariant_b():
    """Wiązka jasnych korzeni z resztką ziemi: mały kłącz, gęsta kremowa broda, grudka ziemi uwięziona w środku."""
    e = [kostka((6, 12, 6), (10, 15, 10), KORA, du=2),
         lodyga(7, 7, 15, 1, 'x', 0), lodyga(8, 8, 15, 1, 'z', 22.5)]
    broda = ((6, 6, 11, 'z', -22.5), (6, 7, 12, 'x', 0), (6, 9, 11, 'x', -22.5), (7, 6, 12, 'x', 22.5), (7, 9, 12, 'x', -22.5),
             (8, 6, 11, 'x', 22.5), (8, 9, 12, 'z', 22.5), (9, 6, 11, 'z', 22.5), (9, 7, 12, 'z', 22.5), (9, 9, 11, 'x', -22.5),
             (7, 8, 12, 'z', -22.5), (8, 7, 12, 'z', 0), (6, 8, 10, 'z', -45), (9, 8, 10, 'z', 45))
    for i, (x, z, dl, os_, kat) in enumerate(broda):
        e.append(korzen(x, z, 12, dl, os_, kat, i))
    e += [kostka((5, 6, 5), (9, 9, 9), ZIEM),                         # gruda ziemi oblepia brodę z przodu-lewej
          kostka((9, 8, 9), (11, 10, 11), ZIEM, du=5, dv=2),          # druga, mniejsza, wystaje z boku
          kostka((5, 3, 8), (6, 4, 9), ZIEM, du=8, dv=4)]             # okruch nisko na korzeniu
    return e


def wariant_c():
    """Korzeń z rozetą liści u góry: cała roślina wyrwana - kłącz, krótkie korzenie, ogonki i trzy ucięte liście."""
    e = [kostka((5, 6, 5), (11, 9, 11), KORA, du=1)]
    for i, (x, z, dl, os_, kat) in enumerate(((5, 5, 6, 'z', -45), (5, 9, 6, 'x', -45), (7, 5, 6, 'x', 22.5), (8, 8, 6, 'z', 0),
                                              (10, 5, 6, 'x', 45), (10, 10, 6, 'z', 45), (6, 7, 5, 'z', -22.5), (9, 7, 6, 'z', 22.5))):
        e.append(korzen(x, z, 7, dl, os_, kat, i * 2))
    e += [lodyga(7, 7, 9, 4, 'z', -22.5), lodyga(8, 8, 9, 4, 'x', 22.5), lodyga(8, 7, 9, 3, 'z', 22.5)]
    # liście: płaszczyzny 6x6 na końcach ogonków, rozchylone (dwustronne, tekstura z alfą)
    e += [kostka((2, 11, 8), (8, 17, 8), LISC, rot=('z', 22.5, (7, 11, 8)), dwustronna=True),
          kostka((9, 11, 8), (15, 17, 8), LISC, rot=('z', -22.5, (9, 11, 8)), dwustronna=True),
          kostka((8, 11, 3), (8, 17, 9), LISC, rot=('x', -22.5, (8, 11, 9)), dwustronna=True)]
    return e


WARIANTY = {'a': wariant_a, 'b': wariant_b, 'c': wariant_c}


def model_java(elem):
    out = []
    for e in elem:
        j = {'from': e['from'], 'to': e['to'], 'faces': {}}
        for k, uv in e['faces'].items():
            j['faces'][k] = {'uv': [c * 16 / W for c in uv], 'texture': '#t'}
        if e['rot']:
            os_, kat, o = e['rot']
            j['rotation'] = {'angle': kat, 'axis': os_, 'origin': list(o)}
        out.append(j)
    # display: w rece/GUI na wypadek wydania jako przedmiot (encja uzywa NONE)
    return {'textures': {'t': 'nexo:block/zielarz/kozlek_korzen3d', 'particle': 'nexo:block/zielarz/kozlek_korzen3d'},
            'elements': out,
            'display': {'gui': {'rotation': [20, 145, 0], 'scale': [0.8, 0.8, 0.8]},
                        'ground': {'scale': [0.5, 0.5, 0.5]}, 'fixed': {'scale': [0.9, 0.9, 0.9]}}}


def model_bb(elem, im, nazwa):
    b = io.BytesIO(); im.save(b, 'PNG')
    els = []
    for e in elem:
        r = [0, 0, 0]; o = [8, 8, 8]
        if e['rot']:
            os_, kat, o = e['rot']; r['xyz'.index(os_)] = kat
        els.append({'name': 'k', 'uuid': str(uuid.uuid4()), 'from': e['from'], 'to': e['to'], 'origin': list(o), 'rotation': r,
                    'faces': {k: {'uv': uv, 'texture': 0} for k, uv in e['faces'].items()}})
    return {'meta': {'format_version': '4.10', 'model_format': 'java_block'}, 'name': nazwa, 'resolution': {'width': W, 'height': W},
            'elements': els, 'outliner': [e['uuid'] for e in els],
            'textures': [{'name': 'kozlek_korzen3d', 'source': 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()}]}


def main(tex, nexo_out, bb_out=None):
    im = maluj(tex)
    im.save(os.path.join(tex, 'kozlek_korzen3d.png'))
    m = os.path.join(nexo_out, 'pack/assets/nexo/models/zielarz'); os.makedirs(m, exist_ok=True)
    for k, f in WARIANTY.items():
        el = f()
        json.dump(model_java(el), open(os.path.join(m, f'korzen3d_{k}.json'), 'w'), indent=1)
        if bb_out:
            os.makedirs(bb_out, exist_ok=True)
            json.dump(model_bb(el, im, f'korzen3d_{k}'), open(os.path.join(bb_out, f'korzen3d_{k}.bbmodel'), 'w'))
        print(k, len(el), 'elementow')


if __name__ == '__main__':
    main(*sys.argv[1:])
