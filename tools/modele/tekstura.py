"""Malowanie tekstury modelu z generatora (wzorzec: tekstury LittleRoom, np. lr_owlbear_cub, dreadroot).

Jak maluje LR i co tu odtwarzamy:
- kazda sciana ma miekki gradient jasnosci: jasniejsza gora (swiatlo z gory), ciemniejszy dol (AO), jasna gorna krawedz,
- kolor jest ciagly (interpolacja palety + drobne odchylenie odcienia), stad setki kolorow bez "moro",
- pociagniecia zalezne od materialu (bruzdy kory, luki pedow, brodawki, wlokna), a NIE losowe plamy,
- cechy (oczy, znaczenia, paszcze) sa MALOWANE na scianie ("naklejki"), nie doklejane jako kostki <1 px.
1 teksel = 1 px modelu. MATERIALY: nazwa -> dict(ramp=[ciemny..jasny] = DYSKRETNE tony (plaskie plamy), wzor=None|'kora'|'pedy'|'wlokna'|'brodawki'
|'mech'|'gladki', alfa=None|'draperia'|'lisc', szum=0.12, gradient=1.0).
Naklejki: {(element, sciana): (['wiersze', ...], {znak: (r,g,b) | (r,g,b,a)})} - wiersze wysrodkowane na scianie,
'.' = bez zmian. Sciana 'north' = przod modelu (-z), obraz ogladany od przodu (lewa kolumna = prawa strona modelu).
"""
import base64
import io
import math

import numpy as np
from PIL import Image

# male liscie (X = blaszka, n = nerw, j = swiatlo) - lisc skierowany w gore/na zewnatrz
MALE_LISCIE = {
    (3, 3): ['.jX', 'XnX', 'Xn.'],
    (3, 4): ['..j', '.jX', 'XnX', 'Xn.'],
    (4, 3): ['..jX', 'XnnX', 'XX..'],
    (4, 4): ['..jX', '.jXX', 'XnX.', 'Xn..'],
}
SCIANY_WYM = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}
# swiatlo z gory i lekko z lewego przodu (jak LR): gora najjasniejsza, spod najciemniejszy
TON_SCIANY = {'up': 0.16, 'north': 0.04, 'west': 0.02, 'east': -0.05, 'south': -0.04, 'down': -0.28}


def _szum(h, w, rng, skala):
    gh, gw = int(h // skala) + 2, int(w // skala) + 2
    g = rng.random((gh, gw))
    ys, xs = np.arange(h) / skala, np.arange(w) / skala
    y0, x0 = ys.astype(int), xs.astype(int)
    fy, fx = (ys - y0)[:, None], (xs - x0)[None, :]
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


ODCIEN_PLAM = 6      # max odchylenie kanalu RGB miedzy plamami (stale w obrebie plamy)
AKCENTY = 0.07       # udzial pojedynczych pikseli o ton jasniejszych/ciemniejszych


def _kolor(ramp, v, rng):
    """v 0..1 -> dyskretny ton palety + odcien zmieniany PLAMAMI (jak reczne malowanie LR).
    Zmierzone na LR: 24-46% identycznych sasiadow i setki kolorow; szum per piksel dawal 2-6%,
    sama dyskretna paleta 62-74% i 30-40 kolorow (recenzje 2026-09-26)."""
    n = len(ramp)
    h, w = v.shape
    i = np.clip(np.floor(np.clip(v, 0, 0.999) * n).astype(int), 0, n - 1)
    akc = rng.random((h, w))
    i = np.clip(i + (akc < AKCENTY / 2) - ((akc > 1 - AKCENTY / 2) & (i > 0)), 0, n - 1)
    rgb = ramp[i].astype(float)
    for c in range(3):  # odcien plamami: pole niskiej czestotliwosci, kwantowane co 3
        pole = (_szum(h, w, rng, 7) - 0.5) * 2 * ODCIEN_PLAM
        rgb[..., c] += np.round(pole / 4) * 4
    return rgb


def maluj(sciana, w, h, mat, rng):
    ramp = np.array(mat['ramp'], float)
    wzor = mat.get('wzor')
    boczna = sciana not in ('up', 'down')
    v = np.full((h, w), 0.5 + TON_SCIANY[sciana] + mat.get('jasnosc', 0.0))  # jasnosc: przesuniecie tonu materialu
    amp = mat.get('szum', 0.12)
    v += amp * 1.6 * (_szum(h, w, rng, 4) - 0.5)  # plamy 3-5 px, bez ziarna per piksel
    if boczna and h > 1:
        g = mat.get('gradient', 1.0)
        yy = np.linspace(0, 1, h)[:, None]
        v += g * (0.12 * (1 - yy) - 0.16 * yy ** 1.6)   # gora jasniej, dol AO
        v[0, :] += 0.07 * g                            # jasna gorna krawedz
        if h > 2:
            v[-1, :] -= 0.06 * g
    if wzor == 'kora' and boczna:
        # pionowe bruzdy co 2-3 px z falowaniem; obok bruzdy jasny grzbiet (wypuklosc kory)
        x = int(rng.integers(0, 2))
        while x < w:
            fal = 0
            for y in range(h):
                if rng.random() < 0.18:
                    fal = int(np.clip(fal + rng.choice([-1, 1]), -1, 1))
                xx = min(w - 1, max(0, x + fal))
                v[y, xx] -= 0.2
                if xx + 1 < w:
                    v[y, xx + 1] += 0.08
            x += int(rng.integers(2, 4))
    elif wzor == 'kora_skos' and boczna:  # wlokna ukosne jak u dreadroot: bruzdy biegna po skosie, przerywane
        kier = 1 if rng.random() < 0.5 else -1
        for k in range(-h, w + h, 3):
            for y in range(h):
                x = k + kier * (y // 2)
                if 0 <= x < w and rng.random() > 0.15:
                    v[y, x] -= 0.18
                    if 0 <= x + 1 < w:
                        v[y, x + 1] += 0.07
    elif wzor in ('kora', 'kora_skos'):  # przekroj pnia na gorze/dole: sloje koncentryczne
        yy, xx = np.mgrid[0:h, 0:w]
        r = np.hypot(xx + 0.5 - w / 2, yy + 0.5 - h / 2)
        v += 0.08 * np.cos(r * 2.2)
    elif wzor == 'pedy':  # luki pedow jezyny: ukosne pasma z ciemna krawedzia
        for k in range(-h, w, 3):
            for y in range(h):
                x = k + y // 2
                if 0 <= x < w:
                    v[y, x] -= 0.14
                    if x + 1 < w:
                        v[y, x + 1] += 0.06
    elif wzor == 'wlokna' and boczna:  # trzon/miazsz: drobne podluzne wlokna, niski kontrast
        for x in range(w):
            if rng.random() < 0.5:
                y0 = int(rng.integers(0, max(1, h - 2)))
                v[y0:y0 + int(rng.integers(2, max(3, h // 2 + 1))), x] -= 0.08
    elif wzor == 'brodawki':  # purchawka: grona brodawek (ciemna podstawa + 1 px swiatla nad nia)
        for _ in range(max(1, w * h // 7)):
            x, y = int(rng.integers(0, w)), int(rng.integers(1, max(2, h)))
            if y < h:
                v[y, x] -= 0.22
                v[y - 1, x] += 0.12
    elif wzor == 'mech' and boczna:  # kepki: jasniejsze konce u gory, ciemne u nasady
        for x in range(w):
            v[:, x] += 0.05 * np.sin(x * 1.7 + rng.random() * 6)
    rgb = _kolor(ramp, v, rng)
    a = np.full((h, w), 255.0)
    kind = mat.get('alfa')
    if kind == 'draperia':
        for x in range(w):
            cut = int(rng.integers(0, max(1, (h * 2) // 3) + 1))
            if cut:
                a[h - cut:, x] = 0
        a[rng.random((h, w)) < 0.1] = 0
    elif kind == 'korzenie':  # strefa korzeni: rozgalezione korzenie od srodka, reszta przezroczysta
        a[:] = 0
        cy, cx = h / 2, w / 2
        for k in range(7):
            ang = k * 2 * math.pi / 7 + rng.random() * 0.5
            x, y, grub = cx, cy, 2
            for step in range(int(max(w, h) * 0.55)):
                ang += (rng.random() - 0.5) * 0.5
                x += math.cos(ang); y += math.sin(ang)
                for dx in range(grub):
                    xi, yi = int(x) + dx, int(y)
                    if 0 <= xi < w and 0 <= yi < h:
                        a[yi, xi] = 255
                        rgb[yi, xi] = ramp[min(len(ramp) - 1, 1 + dx)]
                if step == int(max(w, h) * 0.2):
                    grub = 1
                if rng.random() < 0.08:  # boczna odnoga
                    bx, by, ba = x, y, ang + rng.choice([-1, 1]) * 0.9
                    for _ in range(int(rng.integers(2, 5))):
                        bx += math.cos(ba); by += math.sin(ba)
                        if 0 <= int(bx) < w and 0 <= int(by) < h:
                            a[int(by), int(bx)] = 255; rgb[int(by), int(bx)] = ramp[0]
        a[int(cy) - 1:int(cy) + 2, int(cx) - 1:int(cx) + 2] = 255
    elif kind == 'lisc' and (w, h) in MALE_LISCIE:  # male liscie rysowane recznie (formula daje krzyzyk "+")
        for yy, row in enumerate(MALE_LISCIE[(w, h)]):
            for xx, ch in enumerate(row):
                if ch == '.':
                    a[yy, xx] = 0
                elif ch == 'n':
                    rgb[yy, xx] = ramp[0]
                elif ch == 'j':
                    rgb[yy, xx] = ramp[-1]
    elif kind == 'lisc':
        yy, xx = np.mgrid[0:h, 0:w]
        dx = np.abs(xx + 0.5 - w / 2) / (w / 2)
        dy = np.abs(yy + 0.5 - h / 2) / (h / 2)
        a[dx ** 1.2 + dy * 0.9 > 1.05] = 0
        if h >= 3:
            rgb[h // 2, 1:w - 1] = ramp[1]
            rgb[: h // 2] = rgb[: h // 2] * 0.8 + ramp[-1] * 0.2
    return np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8)


def _naklej(kafel, wiersze, klucz):
    h, w = kafel.shape[:2]
    nh, nw = len(wiersze), max(len(r) for r in wiersze)
    oy, ox = max(0, (h - nh) // 2), max(0, (w - nw) // 2)
    for y, row in enumerate(wiersze):
        for x, ch in enumerate(row):
            if ch == '.' or not (0 <= oy + y < h and 0 <= ox + x < w):
                continue
            c = klucz[ch]
            kafel[oy + y, ox + x] = list(c) + ([255] if len(c) == 3 else [])


def atlas(elementy, materialy, wybierz, rozmiar=128, ziarno=7, naklejki=None, malarz=None,
          wspoldzielone=False):
    """Upakowuje sciany (polkowo), maluje, naklada naklejki, ustawia uv. Zwraca (data_uri, zajetosc).
    malarz(sciana, w, h, mat, rng) -> kafel RGBA; domyslnie maluj() z tego modulu (np. malarz.maluj dla bossow)."""
    rng = np.random.default_rng(ziarno)
    naklejki = naklejki or {}
    sciany = []
    for e in elementy:
        mat = materialy[wybierz(e['name'])]
        d = [e['to'][i] - e['from'][i] for i in range(3)]
        for f in e['faces']:
            ax, ay = SCIANY_WYM[f]
            w, h = max(1, math.ceil(d[ax] - 1e-6)), max(1, math.ceil(d[ay] - 1e-6))
            sciany.append((h, w, e, f, mat))
    sciany.sort(key=lambda s: -s[0])
    img = np.zeros((rozmiar, rozmiar, 4), np.uint8)
    x = y = wys_polki = 0
    uzyte={}
    for h, w, e, f, mat in sciany:
        klucz=(wybierz(e['name']),f,w,h)
        if wspoldzielone and (e['name'],f) not in naklejki and klucz in uzyte:
            e['faces'][f]['uv']=uzyte[klucz]
            e['faces'][f]['texture']=0
            continue
        if x + w > rozmiar:
            x, y, wys_polki = 0, y + wys_polki, 0
        if y + h > rozmiar:
            raise ValueError('atlas przepelniony')
        kafel = (malarz or maluj)(f, w, h, mat, rng)
        if (e['name'], f) in naklejki:
            _naklej(kafel, *naklejki[(e['name'], f)])
        img[y:y + h, x:x + w] = kafel
        e['faces'][f]['uv'] = [x, y, x + w, y + h]
        e['faces'][f]['texture'] = 0
        if wspoldzielone and (e['name'],f) not in naklejki:
            uzyte[klucz]=e['faces'][f]['uv']
        x += w
        wys_polki = max(wys_polki, h)
    b = io.BytesIO()
    Image.fromarray(img).save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode(), (y + wys_polki) / rozmiar
