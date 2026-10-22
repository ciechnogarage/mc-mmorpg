"""Malarz pod ksztalt (bossowie i modele "od pomyslu", 2026-09-27) - zastepuje jeden wzor na wszystko z tekstura.maluj.

Zasady (gorna polka LittleRoom + metoda pixel-art z pamieci):
- material = 5 DYSKRETNYCH tonow (ciemny..jasny), bez szumu per piksel i bez tonow posrednich;
- ton bazowy z kierunku sciany: swiatlo z gory i z lewego przodu (up najjasniejsza, down najciemniejsza);
- na scianach bocznych jasna gorna krawedz (1 px) i cien kontaktowy u dolu (1-2 px);
- wzor materialu jako PLASKIE PLAMY i pociagniecia w osi czesci (slojy kory w pionie, przetchlinki brzozy w poziomie);
- mech rosnie tylko u gory: pokrywa sciane `up`, na bocznych schodzi z gornej krawedzi zaciekami;
- akcenty (maska, serce) maluje generator modelu naklejkami, nie ten modul.

Material: dict(ramp=[5 x (r,g,b)], wzor='kora'|'brzoza'|'mech'|'kwiat'|'kamien'|'swiatlo'|'gladki'|'zwis',
               mech=0..1 (ile mchu od gory), mech_ramp=[4 x (r,g,b)]).
"""
import numpy as np

BAZA = {'up': 3, 'north': 2, 'west': 2, 'east': 1, 'south': 1, 'down': 0}
BOK = ('north', 'south', 'east', 'west')


def _ton(ramp, i):
    return ramp[max(0, min(len(ramp) - 1, i))]


def _plamy(t, rng, ile, dl=(2, 5), sz=(1, 3), delta=(-1, 1), pion=True):
    """plaskie plamy: prostokaty tonu bazowego +-1, wydluzone w osi czesci"""
    h, w = t.shape
    for _ in range(ile):
        a, b = rng.integers(dl[0], dl[1] + 1), rng.integers(sz[0], sz[1] + 1)
        ph, pw = (a, b) if pion else (b, a)
        y, x = rng.integers(0, max(1, h - ph + 1)), rng.integers(0, max(1, w - pw + 1))
        t[y:y + ph, x:x + pw] += rng.choice(delta)


def maluj(f, w, h, mat, rng):
    ramp = mat['ramp']
    wz = mat.get('wzor', 'gladki')
    t = np.full((h, w), BAZA[f], int)   # indeksy tonow
    bok = f in BOK
    n = max(1, (w * h) // 18)

    if wz == 'kora':
        # sloje: pionowe bruzdy (ton -1/-2) co 3-5 px, przerywane; plamy wydluzone w pionie
        # spokoj=True: rzadsze bruzdy i wieksze plaskie plamy (duze swiatlo czyta sie zamiast drobnego wzoru)
        sp = mat.get('spokoj')
        _plamy(t, rng, n // 2 if sp else n, dl=(4, 9) if sp else (3, 7), sz=(2, 3) if sp else (1, 2), pion=bok)
        if bok:
            x = int(rng.integers(0, 3))
            while x < w:
                y = int(rng.integers(0, 3))
                while y < h:
                    d = int(rng.integers(2, 7))
                    t[y:y + d, x] -= 1 if (sp or rng.random() < 0.7) else 2
                    y += d + int(rng.integers(2, 6) if sp else rng.integers(1, 4))
                x += int(rng.integers(5, 9) if sp else rng.integers(3, 6))
            if h >= 6 and w >= 5 and rng.random() < 0.5:        # suk: ciemne oczko z jasnym obrzezem
                y, x = int(rng.integers(1, h - 3)), int(rng.integers(1, w - 3))
                t[y:y + 3, x:x + 2] += 1
                t[y + 1, x] -= 3
    elif wz == 'brzoza':
        _plamy(t, rng, n // 2, dl=(2, 4), sz=(1, 2), delta=(-1,), pion=False)
        for _ in range(max(1, (w * h) // 30)):                  # przetchlinki: poziome ciemne kreski
            y, x, d = int(rng.integers(0, h)), int(rng.integers(0, max(1, w - 2))), int(rng.integers(2, 5))
            t[y, x:x + d] = 0
    elif wz == 'kamien':
        _plamy(t, rng, n, dl=(2, 4), sz=(2, 3), pion=False)
        for _ in range(max(1, (w * h) // 60)):                  # rysy
            y, x = int(rng.integers(0, h)), int(rng.integers(0, w))
            for _k in range(int(rng.integers(3, 7))):
                if 0 <= y < h and 0 <= x < w:
                    t[y, x] = 0
                y += 1; x += int(rng.integers(-1, 2))
    elif wz == 'kwiat':
        t[:] = BAZA[f] // 2 + 1
        _plamy(t, rng, n * 2, dl=(1, 2), sz=(1, 2), delta=(1, 2), pion=False)
    elif wz == 'mech':
        _plamy(t, rng, n * 2, dl=(1, 3), sz=(1, 2), pion=False)
    elif wz == 'swiatlo':
        # rdzen jasny, brzegi ton nizej (w grze i tak swieci koscia swiatlo_*)
        t[:] = 3
        if h > 2 and w > 2:
            t[1:-1, 1:-1] = 4
        t[0, :] = 2; t[-1, :] = 2; t[:, 0] = 2; t[:, -1] = 2
    elif wz == 'zwis':
        t[:] = 2

    if bok and wz not in ('swiatlo', 'zwis'):
        t[0, :] += 1                                             # jasna gorna krawedz
        if h >= 4:
            t[-1, :] -= 1                                        # cien kontaktowy
        if h >= 10:
            t[-2, :] -= 1 if rng.random() < 0.5 else 0

    img = np.zeros((h, w, 4), np.uint8)
    for i in range(len(ramp)):
        img[np.clip(t, 0, len(ramp) - 1) == i] = list(ramp[i]) + [255]

    if wz == 'zwis':                                             # pasma mchu: pionowe nitki z przerwami, alfa
        img[:] = 0
        mr = mat.get('mech_ramp', ramp)
        for x in range(w):
            if rng.random() < 0.25:
                continue
            d = int(h * rng.uniform(0.55, 1.0))
            for y in range(d):
                img[y, x] = list(_ton(mr, 1 if y > d - 3 else (2 if (x + y) % 5 else 3))) + [255]

    p = mat.get('mech', 0)
    if p and wz != 'zwis':                                      # mech od gory
        mr = mat['mech_ramp']
        if f == 'up':
            m = rng.random((h, w)) < p
            if m.any():
                img[m] = [list(_ton(mr, 2 + int(v))) + [255] for v in rng.random(int(m.sum())) < 0.3]
        elif bok:
            for x in range(w):
                d = int(rng.integers(0, max(1, int(h * p * 0.6)) + 1))
                if rng.random() < p + 0.2:
                    for y in range(min(d + 1, h)):
                        img[y, x] = list(_ton(mr, 2 if y == 0 else (1 if y < d else 0))) + [255]
    return img


# ---------------------------------------------------------------------------------------------------------------
# kora2 (2026-09-27, po "tekstura odstaje jakosciowo"): malowanie jak gorna polka LR (zahar_abomination, hollow_keeper)
# - kazda plaszczyzna ma BRYLE: jasniejszy srodek i gora, fazowane krawedzie (jasna gora/lewa, ciemny dol/prawa),
# - sloje o NISKIM kontrascie, plynnie falujace (nie losowe czarne kreski), kilka rzezbionych szczelin z jasnym brzegiem,
# - kolor ciagly z przesunieciem odcienia (cien chlodniejszy/czerwienszy, swiatlo cieplejsze), kwantyzacja do ~14 tonow,
# - sciany `up` pionowych czesci = sloje przyrostow (sciety koniec), chyba ze mech.
BAZA2 = {'up': 0.54, 'north': 0.42, 'west': 0.40, 'east': 0.30, 'south': 0.28, 'down': 0.14}   # ciemne cialo (plan jasnosci)


def _rampa(ramp, v):
    v = np.clip(v, 0, 1) * (len(ramp) - 1)
    i = np.clip(np.floor(v).astype(int), 0, len(ramp) - 2)
    f = (v - i)[..., None]
    r = np.array(ramp, float)
    return r[i] * (1 - f) + r[i + 1] * f


def maluj2(f, w, h, mat, rng):
    if mat.get('wzor') != 'kora2':
        return maluj(f, w, h, mat, rng)
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    v = np.full((h, w), BAZA2[f])
    bok = f in BOK
    if bok:
        v += 0.14 * (1 - yy / max(h - 1, 1)) - 0.06                       # swiatlo z gory
    cy, cx = (h - 1) / 2, (w - 1) / 2
    d = np.sqrt(((yy - cy) / max(h / 2, 1)) ** 2 + ((xx - cx) / max(w / 2, 1)) ** 2)
    v += 0.07 * np.clip(1 - d, 0, 1)                                         # bryla plaszczyzny
    # fazowane krawedzie
    v[0, :] += 0.12; v[:, 0] += 0.05
    v[-1, :] -= 0.12; v[:, -1] -= 0.07
    if h > 4:
        v[1, :] += 0.05; v[-2, :] -= 0.05
    ph = rng.uniform(0, 6.3, 3)
    if bok or f == 'down':
        # sloje: plynne falujace pasma wzdluz czesci (pion), niski kontrast
        v += 0.045 * np.sin(xx * 1.9 + 1.4 * np.sin(yy * 0.23 + ph[0]) + ph[1])
        v += 0.03 * np.sin(xx * 0.7 + 0.8 * np.sin(yy * 0.11 + ph[2]))
        # kilka rzezbionych szczelin: ciemna linia + jasny brzeg z lewej
        for _ in range(max(1, w // 6)):
            x0 = rng.uniform(1, max(1.5, w - 2)); a = rng.uniform(0.8, 1.6); fz = rng.uniform(0, 6.3)
            y0 = int(rng.integers(0, max(1, h // 3))); y1 = h - int(rng.integers(0, max(1, h // 3)))
            for y in range(y0, y1):
                x = int(round(x0 + a * np.sin(y * 0.3 + fz)))
                if 0 <= x < w:
                    v[y, x] -= 0.26
                if 0 <= x - 1 < w:
                    v[y, x - 1] += 0.07
    else:
        # sloje przyrostow na scietym koncu
        r = np.sqrt((yy - cy) ** 2 + (xx - cx) ** 2)
        v += 0.05 * np.sin(r * 1.7 + ph[0])
    v += rng.normal(0, 0.012, (h, w))                                        # minimalne ziarno
    v = np.round(v * 14) / 14
    img = np.zeros((h, w, 4), np.uint8)
    img[..., :3] = _rampa(mat['ramp'], v).astype(np.uint8)
    img[..., 3] = 255
    p = mat.get('mech', 0)
    if p:                                                                     # mech: organiczne plamy od gory
        mr = np.array(mat['mech_ramp'], float)
        if f == 'up':
            m = (np.sin(xx * 0.9 + ph[0]) + np.sin(yy * 0.8 + ph[1]) + rng.normal(0, 0.5, (h, w))) > (1.2 - 2.4 * p)
        elif bok:
            gl = (p * h * 0.5) * (0.6 + 0.4 * np.sin(xx[0] * 0.8 + ph[2])) + rng.normal(0, 0.6, w)
            m = yy < gl[None, :]
        else:
            m = np.zeros((h, w), bool)
        if m.any():
            mv = np.clip(v[m] + 0.1, 0, 1) * (len(mr) - 1)
            i = np.clip(mv.astype(int), 0, len(mr) - 2); fr = (mv - i)[:, None]
            img[m, :3] = (mr[i] * (1 - fr) + mr[i + 1] * fr).astype(np.uint8)
    return img


# ---------------------------------------------------------------------------------------------------------------
# Bagna (2026-10-03): wspólna, dyskretna metoda dla całej rodziny.
# Każdy materiał ma dokładnie cztery barwy. Kierunek ściany wybiera bazę:
# góra +1, bok 0, spód -1. Reszta to pasma lub plamy w osi części.
BAGNA_MATERIALY = {
    'mul': ((37, 43, 33), (57, 64, 44), (78, 84, 57), (99, 103, 73)),
    'kosc': ((65, 65, 55), (106, 105, 83), (145, 144, 112), (179, 177, 144)),
    'tkanina_siec': ((34, 46, 41), (53, 68, 58), (73, 87, 74), (97, 109, 92)),
    'kokon': ((55, 61, 55), (94, 101, 88), (130, 137, 119), (164, 170, 148)),
    'skora_ropuchy': ((35, 48, 34), (59, 75, 47), (83, 96, 58), (109, 117, 75)),
    'sluz': ((35, 55, 52), (55, 83, 75), (79, 109, 96), (107, 135, 117)),
    'skorupa': ((47, 46, 38), (75, 75, 57), (105, 103, 76), (133, 129, 96)),
    'plomien': ((18, 68, 65), (28, 116, 103), (63, 209, 180), (159, 240, 220)),
    'rzesa': ((29, 43, 35), (47, 64, 47), (69, 86, 59), (91, 107, 71)),
}


def maluj_bagna(f, w, h, mat, rng):
    """Spokojna ściana 3–4 tonów, bez szumu i cieniowania od środka.

    `mat` to nazwa lub dict z `material`, `os`, `mul_od_dolu` i
    `rzesa_od_gory`. Rzęsa jako osobny materiał zachowuje kanał alfa.
    """
    if f not in BAZA:
        raise ValueError(f'Nieznana ściana: {f}')
    if w <= 0 or h <= 0:
        raise ValueError('Wymiary muszą być dodatnie')
    cfg = {'material': mat} if isinstance(mat, str) else dict(mat)
    nazwa = cfg['material']
    ramp = BAGNA_MATERIALY[nazwa]
    base = 2 if f == 'up' else (0 if f == 'down' else 1)
    t = np.full((h, w), base, dtype=np.int8)
    alpha = np.full((h, w), 255, dtype=np.uint8)
    pion = cfg.get('os', 'pion' if f in BOK else 'poziom') == 'pion'
    if nazwa == 'rzesa':
        alpha[:] = 0
        if f == 'up':
            alpha[:] = 255
        elif f in BOK:
            # Dwa lub trzy szerokie, schodkowo zwężane zwisy od górnej krawędzi.
            for x0 in range(0, w, max(4, w // 3)):
                length = min(h, max(2, int(h * (0.25 + 0.12 * rng.random()))))
                for y in range(length):
                    width = 3 if y < length // 2 else (2 if y < length - 1 else 1)
                    alpha[y, x0:min(w, x0 + width)] = 255
        t[alpha > 0] = base
    else:
        # 2–4 duże, wydłużone plamy jednego sąsiedniego tonu. Ich łączna
        # powierzchnia zostawia zwykle 60–75% ściany bazowej, razem z rantem.
        second = base + (1 if base < 2 else -1)
        count = 3 if min(w, h) >= 9 else 2
        for i in range(count):
            if pion:
                pw = max(2, w // (count + 2))
                ph = max(3, int(h * (0.46 if i == 0 else 0.34)))
                x0 = min(max(0, w - pw), int((i + 0.5) * w / count - pw / 2))
                y0 = min(max(1, h - ph - 1), max(1, int((i % 2 + 0.2) * h / 3)))
            else:
                pw = max(3, int(w * (0.62 if i == 0 else 0.50)))
                ph = max(2, h // (count + 1))
                x0 = min(max(0, w - pw), max(0, int((i % 2 + 0.2) * w / 3)))
                y0 = min(max(0, h - ph), int((i + 0.5) * h / count - ph / 2))
            # Plama ma bark, szerszy korpus i krótszy koniec; bez izolowanych pikseli.
            for yy in range(y0, min(h, y0 + ph)):
                inset = 1 if yy in (y0, y0 + ph - 1) and pw >= 5 else 0
                t[yy, x0 + inset:min(w, x0 + pw - inset)] = second

        # Jeden duży detal materiału, bez kratek i drobnych kropek na całej ścianie.
        if w >= 8 and h >= 8:
            if nazwa == 'skorupa':
                t[h // 2, w // 3:min(w, w // 3 + max(3, w // 3))] = max(0, base - 1)
            elif nazwa == 'tkanina_siec':
                t[h // 3:min(h, h // 3 + 2), w // 2:min(w, w // 2 + 3)] = second
            elif nazwa == 'skora_ropuchy':
                t[h // 3:min(h, h // 3 + 3), w // 3:min(w, w // 3 + 4)] = second
            elif nazwa == 'kokon':
                t[h // 2:min(h, h // 2 + 2), w // 4:min(w, w // 4 + w // 2)] = second
            elif nazwa == 'sluz':
                t[h // 3, w // 4:min(w, 3 * w // 4)] = min(3, base + 1)

        if f in BOK:
            t[0, :] = min(3, base + 1)  # 1 px jasnej górnej krawędzi
            if w >= 7 and h >= 7:
                # Szczelina/cień kontaktowy tylko w dwóch krótkich miejscach.
                t[h - 3:h - 1, w // 4:w // 4 + 2] = max(0, base - 1)
                t[h // 2:h // 2 + 2, 3 * w // 4:3 * w // 4 + 1] = max(0, base - 1)
            if nazwa == 'mul' or cfg.get('mul_od_dolu'):
                for x0, length in ((w // 5, max(2, h // 6)), (3 * w // 5, max(3, h // 4))):
                    t[h - length:h, x0:min(w, x0 + 2)] = 0
                    if x0 + 2 < w and length > 3:
                        t[h - length // 2:h, x0 + 2] = 0
            if cfg.get('rzesa_od_gory'):
                for x0, length in ((w // 3, max(2, h // 5)), (3 * w // 4, max(3, h // 4))):
                    t[0:length, x0:min(w, x0 + 2)] = 0

    arr = np.empty((h,w,4),dtype=np.uint8)
    arr[:,:,:3] = np.asarray(ramp,dtype=np.uint8)[np.clip(t,0,3)]
    arr[:,:,3] = alpha
    return arr
