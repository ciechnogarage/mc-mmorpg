#!/usr/bin/env python3
"""Grafika zaklec w dloni (Plomienie, Odmrozenie) — v2 „jak Skyrim”, 2026-09-30.

Generuje CALY katalog assetow Nexo `zaklecia:` (tekstury + modele + definicje przedmiotow):
    python3 grafika.py [katalog_wyjscia]      # domyslnie v2/src/Nexo/pack/assets/zaklecia
    python3 grafika.py --v1 [client.jar]      # stara wersja v1 (ogien = vanillowy fire_0) do src/

Tekstury: 16x16 na klatke (przedmioty = 16x16, bez mixeli), animacja przez .mcmeta, arkusze 16x512.
  plomien.png / plomien_b.png  32 kl. x1 tick — kula ognia z trzema jezorami, szum petlowy plynie w gore; _b = to samo
                               przesuniete o pol petli i odbite (warstwy nie migocza w takt)
  mroz.png / mroz_b.png        32 kl. x1 tick — lodowa kula + gwiazdka blysku + 3 ramiona wiru (ton zewnetrzny
                               polprzezroczysty) + migajace iskry; _b przesuniete o 1/3 i odbite (wir w druga strone)
  krysztal.png                 statyczna — dwa odlamki lodu (3x6, 3x4) rysowane siatka znakow
Modele (patrz scianka()): cialo = 4 warstwy composite z jedna pionowa sciana, obrocone o 45/135/225/315 st. (3. os.)
albo wachlarzem przodem do kamery (1. os.). Sciana to plaska sciana „up” obrocona 45 st. w elemencie + 45 st. w display
— dzieki temu zachowuje normalna prawie „do gory” i MC nie przyciemnia jej cieniowaniem kierunkowym (pionowa
plaszczyzna wychodzila brazowa: 0,45-0,74 jasnosci). Podstawa ognia w SRODKU modelu (y=8), wiec skala rosnie od dloni.
Mroz ma dodatkowo 4 karty odlamkow lodu, ktore przy rzucaniu okrazaja kule.
Definicja przedmiotu (items model definition 1.21.4+, pola wg minecraft.wiki „Items model definition”):
  gui/ground/fixed                 -> plaska animowana ikona (*_ikona)
  nie uzywany                      -> spoczynek „s” (reka opuszczona; wlasny gracz z wcisnietym PPM -> „k”, patrz nizej)
  using_item -> range_dispatch minecraft:use_duration (ticki od startu uzycia): WZROST w0..w7 (x1,00 -> x1,62, 0,35 s)
             -> od 8. ticku range_dispatch minecraft:use_cycle period 8 (pozostale ticki mod 8): PULS p0..p3 co 2 ticki
                (x1,52/1,57/1,61/1,57) + obrot odlamkow o0..o7 (11,25 st./tick)
  druga dlon przy rzucaniu oburacz: view_entity + keybind_down key.use -> „k” (tylko u wlasnego gracza)
Composite: kazda warstwa ma wlasny display, wiec odlamki obracaja sie niezaleznie od skali ciala.
"""
import json, math, pathlib, random, sys
from PIL import Image

N = 32  # klatki
TAU = 2 * math.pi

def kwant(h, progi, paleta):
    for p, c in zip(progi, paleta):
        if h >= p:
            return c
    return None

def wariant(fn, dt, lustro):
    return lambda x, y, t, f: fn(16 - x if lustro else x, y, (t + dt) % 1.0, f)

def arkusz(fn, n=N):
    im = Image.new('RGBA', (16, 16 * n), (0, 0, 0, 0)); px = im.load()
    for f in range(n):
        t = f / n
        for y in range(16):
            for x in range(16):
                c = fn(x + 0.5, y + 0.5, t, f)
                if c: px[x, 16 * f + y] = c if len(c) == 4 else (*c, 255)
    return im

# ---------- ogien ----------
OG = [(255, 250, 214), (255, 216, 72), (250, 142, 28), (208, 62, 18)]
OG_P = [0.80, 0.56, 0.34, 0.14]
rnd = random.Random(11)
# szum petlowy: sumy sinusow o calkowitych czestotliwosciach (okres 16 px w pionie)
FAL = [(rnd.choice([1, 2, 3]), rnd.uniform(0.5, 1.6), rnd.uniform(0, TAU), rnd.uniform(0.6, 1.0)) for _ in range(5)]
def szum(x, yy):
    return sum(w * math.sin(TAU * m * yy / 16 + kx * x + p) for m, kx, p, w in FAL) / sum(w for *_, w in FAL)

def lza(x, y, cx, cy, R, H, lean=0.0):
    """pole 'lzy': kula promienia R w (cx,cy), czubek H px nad srodkiem, pochylony o lean px na czubku."""
    dy = y - cy
    if dy >= 0:
        return 1 - math.hypot(x - cx, dy * 1.1) / R
    wz = -dy / H
    if wz >= 1:
        return -1
    sx = cx + lean * wz * wz
    pol = R * (1 - wz) ** 0.7
    return (1 - abs(x - sx) / max(pol, 0.05)) * (1 - 0.75 * wz) - 0.12 * wz

JEZ = [(-2.6, 0.00, 3.0, 7.0, -1.6), (2.7, 0.55, 2.9, 6.5, 1.7), (0.0, 0.2, 4.4, 10.5, 0.0)]  # dx, faza, R, H, pochyl
def plomien(x, y, t, f):
    cy = 11.8
    h = -1
    for dx, ph, R, H, ln in JEZ:
        a = TAU * (t * 2 + ph)
        hh = H * (1 + 0.22 * math.sin(a) + 0.08 * math.sin(3 * a))
        cyj = cy + (0 if dx == 0 else 1.3)
        h = max(h, lza(x, y, 8.0 + dx, cyj - (0 if dx == 0 else 1.2), R, hh, ln + 0.8 * math.sin(a + 1)))
    wyso = max(0.0, cy - y) / 10.5
    n = szum(x, y + 32 * t)
    h += 0.30 * n * (0.25 + wyso)
    return kwant(h, OG_P, OG)

# ---------- mroz ----------
MCY = 9.5  # srodek kuli mrozu (wiersz tekstury)
MR = [(248, 254, 255), (196, 240, 255), (128, 200, 248), (70, 132, 214, 170)]
MR_P = [0.80, 0.58, 0.36, 0.16]
FALM = [(2, 0.9, 0.3, 1.0), (3, 0.5, 1.7, 0.7), (5, 0.3, 4.1, 0.45)]
def mroz(x, y, t, f):
    cx, cy = 8.0, MCY
    dx, dy = x - cx, (y - cy) * 1.08
    r = math.hypot(dx, dy); th = math.atan2(dy, dx)
    h = (0.42 + 0.58 * (1 - r / 2.6)) if r < 2.6 else -1  # kula rdzenia
    p = 0.5 + 0.5 * math.sin(TAU * t * 2)  # blysk rdzenia: gwiazdka 4-ramienna oddycha
    gw = max(0, 1 - abs(dx) / 0.6) * (1 - abs(dy) / (3.4 + 1.4 * p)) + max(0, 1 - abs(dy) / 0.6) * (1 - abs(dx) / (3.4 + 1.4 * p))
    if gw > 0.25:
        h = max(h, 0.62 + 0.3 * gw)
    # 3 cienkie ramiona wiru: pasmo wokol spirali th = w*t - k*r
    for i in range(3):
        faza = th - TAU * t - TAU * i / 3 + 0.55 * r
        d = abs(math.atan2(math.sin(faza), math.cos(faza)))  # odl. katowa od osi ramienia
        grub = 0.62 * max(0.0, 1 - (r - 2.0) / 5.2)  # rad, cienieje ku koncowi
        if 1.6 < r < 7.2 and d * r < grub * r + 0.55:
            h = max(h, 0.78 - 0.52 * (r - 1.6) / 5.6)
    # iskry lodu migajace
    for sx, sy, ph in [(3, 4, 0.1), (12, 5, 0.45), (13, 13, 0.7), (2, 12, 0.85), (8, 2, 0.3)]:
        if abs(x - 0.5 - sx) < 0.1 and abs(y - 0.5 - sy) < 0.1 and ((t + ph) % 1.0) < 0.22:
            return (246, 253, 255)
    return kwant(h, MR_P, MR)

# ---------------------------------------------------------------- tekstura odlamkow
KRYSZTALY = {  # siatki odlamkow lodu (W biel, L jasny, M sredni, D ciemny), rysowane recznie
    "duzy": [".W.", "LWM", "LWM", "MLD", "MLD", ".D."],
    "maly": [".W.", "LWM", "MLD", ".D."],
}
KOL_K = {"W": (250, 254, 255), "L": (196, 240, 255), "M": (128, 200, 248), "D": (70, 130, 215)}
KRYSZTAL_UV = {"duzy": (0, 0), "maly": (4, 0)}  # lewy gorny rog w krysztal.png
def krysztal():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for nazwa, siatka in KRYSZTALY.items():
        ox, oy = KRYSZTAL_UV[nazwa]
        assert len({len(w) for w in siatka}) == 1, nazwa
        for y, w in enumerate(siatka):
            for x, ch in enumerate(w):
                if ch != ".":
                    px[ox + x, oy + y] = (*KOL_K[ch], 255)
    return im

# ---------------------------------------------------------------- modele
def odlamek(rodzaj, wys, z):
    """odlamek-karta: plaska sciana „up” jak scianka ciala (ten sam obrot 45+45 st. -> pion w plaszczyznie x=8).
    wys = dolna krawedz w pionie (px modelu), z = srodek w poziomie."""
    siatka = KRYSZTALY[rodzaj]; w, h = len(siatka[0]), len(siatka); u, v = KRYSZTAL_UV[rodzaj]
    return {"from": [wys, 8, z - w / 2], "to": [wys + h, 8, z + w / 2], "shade": False, "light_emission": 15,
            "rotation": {"angle": 45, "axis": "z", "origin": [8, 8, 8]},
            "faces": {"up": {"texture": "#k", "uv": [u, v, u + w, v + h], "rotation": 90}}}

# odlamki wokol kuli mrozu (srodek kuli w modelu: y = 24 - MCY = 14.5); dwie karty (druga obrocona o 90 st.)
ODL = [[("duzy", 11, 14.5), ("maly", 15, 1.5)], [("duzy", 13, 1.5), ("maly", 10, 14.0)]]

# display: (rotacja, translacja, skala) — osobno SPOCZYNEK (reka opuszczona, poza ITEM) i RZUT (reka przed siebie).
# W 3. osobie os y modelu wskazuje „przod” przy opuszczonej rece i „gore” przy wyciagnietej, dlatego spoczynek ma obrot x.
BAZA = {
    "s": {"thirdperson_righthand": ([90, 0, 0], [0, 1, -2], 0.44), "firstperson_righthand": ([0, 0, 0], [1.5, 3.6, -1], 0.22)},
    "r": {"thirdperson_righthand": ([0, 0, 0], [0, -3, -4], 0.40), "firstperson_righthand": ([28, 0, 0], [1.5, 2.6, -1], 0.20)},
}
# "k": druga dlon przy rzucaniu oburacz (tylko wlasny gracz): w 3. os. reka uniesiona jak przy rzucie (poza BOW
# dwureczna), w 1. os. druga reka zostaje nisko — jak w spoczynku.
BAZA["k"] = {"thirdperson_righthand": BAZA["r"]["thirdperson_righthand"], "firstperson_righthand": BAZA["s"]["firstperson_righthand"]}
M_1OS = 0.35  # w 1. osobie rosnie slabiej (nie zaslaniac ekranu)
def display(tryb, m, obrot=0.0, obrot_1os=None, dz=0):
    """obrot = dodatkowy obrot wokol pionu modelu (warstwa/odlamki); obrot_1os — osobny dla 1. osoby (domyslnie = obrot)."""
    d = {}
    for ctx, (rot, tr, s) in BAZA[tryb].items():
        pierwsza = ctx.startswith("first")
        mm = 1 + (m - 1) * M_1OS if pierwsza else m
        ob = obrot_1os if (pierwsza and obrot_1os is not None) else obrot
        r = [rot[0], rot[1] + ob, rot[2] + dz]
        for strona in ("right", "left"):
            # Lewa reka: MC odbija lustrzanie translacje x oraz rotacje y i z (ItemTransform.apply). Odbicie obrotu bez
            # odbicia geometrii polozyloby sciane na plasko (45-45) i odwrocilo ja tylem, wiec podajemy -y i -z:
            # po odbiciu obrot jest taki sam jak w prawej, a lustrzana zostaje tylko pozycja (translacja x).
            rr = r if strona == "right" else [r[0], -r[1], -r[2]]
            d[ctx.replace("right", strona)] = {"rotation": [round(v, 4) + 0 for v in rr], "translation": tr,
                                               "scale": [round(s * mm, 4)] * 3}
    d["head"] = {"translation": [0, 8, 0], "scale": [0.6, 0.6, 0.6]}
    return d

WZROST = [1.00, 1.10, 1.22, 1.34, 1.46, 1.56, 1.62, 1.58]  # ticki 0..7 uzycia
PULS = [1.52, 1.57, 1.61, 1.57]                           # use_cycle period 8, co 2 ticki
OBROT = [i * 11.25 for i in range(8)]                     # odlamki: 11,25 st./tick = 90 st. na petle 8 tickow

# Cialo = 4 warstwy composite, kazda z jedna pionowa sciana, obrocone w display wokol pionu.
# Dlaczego tak: MC cieniuje przedmioty kierunkowo (shader: 0,4 + 0,6*swiatlo) po normalnej = kierunku sciany z JSON-a
# obroconym tylko przez display — obrot elementu normalnej nie zmienia, a "shade": false nic nie daje (zmierzone kostka
# w grze: gora 1,0, boki 0,45-0,87). Pionowa sciana "north/south" wychodzila wiec brazowa. Sciana "up" obrocona 45 st.
# w elemencie + 45 st. w display (z) jest pionowa, a jej normalna odchyla sie od pionu tylko o 45 st. -> jasnosc ~1,0
# z kazdej strony. Kat elementu > 45 st. (np. 90) odrzuca parser Nexo („Failed to deserialize”) — stad podzial 45+45.
# Kierunki (znak katow, obrot uv) sprawdzone w grze tekstura-strzalka.
def scianka(tex, rot_uv=90):
    """plaska sciana „up” (y=8, x 8..24) obrocona w elemencie o 45 st. wokol z; kolejne 45 st. dodaje display (z),
    razem pion: plaszczyzna x=8, y 8..24. Normalna zostaje „up” obrocona tylko przez display (45 st. od pionu)."""
    return {"from": [8, 8, 0], "to": [24, 8, 16], "shade": False, "light_emission": 15,
            "rotation": {"angle": 45, "axis": "z", "origin": [8, 8, 8]},
            "faces": {"up": {"texture": tex, "uv": [0, 0, 16, 16], "rotation": rot_uv}}}
DOPELNIENIE_Z = 45  # display: dopelnienie obrotu sciany do pionu

# 3. osoba: (obrot, tekstura) — dwie skrzyzowane plaszczyzny, kazda z obu stron (sciana jednostronna).
WARSTWY = [(45, "a"), (135, "b"), (225, "a"), (315, "b")]
# 1. osoba: 90 st. = sciana przodem do kamery, 270 = tylem (jednostronna -> niewidoczna). Mroz: tylko 2 warstwy,
# bo 4 nalozone spirale wiru zlewaja sie w plame; ogien zyskuje na 4 (gestsze jezory).
WARSTWY_1OS = {"plomien": [90 - 22, 90 - 7, 90 + 7, 90 + 22], "mroz": [90 - 9, 90 + 9, 270, 270]}
def model_cialo(zew, m, tryb="r", obrot=0, obrot_1os=None, tex="a"):
    t = f"zaklecia:item/{zew}" + ("_b" if tex == "b" else "")
    return {"textures": {"p": t, "particle": t}, "elements": [scianka("#p")], "display": display(tryb, m, obrot, obrot_1os, DOPELNIENIE_Z)}

KARTY = [(0, 90 - 15, 0), (90, 90 + 15, 1), (180, 270 - 15, 0), (270, 270 + 15, 1)]  # obrot 3os, obrot 1os, zestaw
def model_odlamki(m, obrot, tryb="r", karta=0):
    o3, o1, zest = KARTY[karta]
    return {"textures": {"k": "zaklecia:item/krysztal", "particle": "zaklecia:item/krysztal"},
            "elements": [odlamek(*o) for o in ODL[zest]], "display": display(tryb, m, o3 + obrot, o1, DOPELNIENIE_Z)}

ref = lambda n: {"type": "minecraft:model", "model": f"zaklecia:item/{n}"}
def warstwy(*n):
    return ref(n[0]) if len(n) == 1 else {"type": "minecraft:composite", "models": [ref(x) for x in n]}

def definicja(nazwa, odlamki):
    def stan(k_cialo, k_odl=None):
        return warstwy(*[f"{nazwa}_{k_cialo}_{i}" for i in range(len(WARSTWY))],
                       *([f"mroz_odlamki_{k_odl}_{i}" for i in range(len(KARTY))] if odlamki else []))
    wzrost = [{"threshold": i, "model": stan(f"w{i}", f"w{i}")} for i in range(1, len(WZROST))]
    puls = [{"threshold": i, "model": stan(f"p{i // 2}", f"o{i}")} for i in range(1, 8)]
    wzrost.append({"threshold": len(WZROST), "model": {"type": "minecraft:range_dispatch", "property": "minecraft:use_cycle",
                   "period": 8, "entries": puls, "fallback": stan("p0", "o0")}})
    return {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                      "cases": [{"when": ["gui", "ground", "fixed"], "model": ref(f"{nazwa}_ikona")}],
                      "fallback": {"type": "minecraft:condition", "property": "minecraft:using_item",
                                   # druga dlon przy rzucaniu oburacz: vanilla „uzywa” tylko jednej reki, wiec zaklecie w
                                   # drugiej nie dostaje using_item. U WLASNEGO gracza (view_entity) wciśniety PPM
                                   # (keybind_down key.use) pokazuje je jako rzucane (pelna wielkosc, bez wzrostu).
                                   # Inni gracze widza te dlon w spoczynku — ograniczenie klienta.
                                   "on_false": {"type": "minecraft:condition", "property": "minecraft:view_entity",
                                                "on_true": {"type": "minecraft:condition", "property": "minecraft:keybind_down",
                                                            "keybind": "key.use", "on_true": stan("k", "k"),
                                                            "on_false": stan("s", "s")},
                                                "on_false": stan("s", "s")},
                                   "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration",
                                               "entries": wzrost, "fallback": stan("w0", "w0")}}},
            "hand_animation_on_swap": False}


# ---------------------------------------------------------------- STRUMIENIE (item_display z pluginu, 2026-09-30)
# Kontrakt pluginu (Strumien.java): model 1 blok w osi z (z=0 dlon -> z=16 koniec), plugin skaluje TYLKO z do dlugosci
# strumienia (0,4-7 bl.), kontekst NONE, jasnosc 15/15. Tekstura: klatka 16 (w poprzek) x 32 (wzdluz, v=0 przy dloni),
# 16 klatek x 1 tick, wzor przewija sie 2 px/klatke = przeplyw od dloni. Stozek waski przy dloni, szeroki na koncu;
# koniec rwie sie (ogien: dziury z szumu) albo rzednie alfa (mroz: mgla), zeby w 1. os. bylo widac cel.
# Geometria: dwie plaszczyzny „up” obrocone +-45 st. wokol z (X w przekroju) — obie maja normalna w gore,
# wiec kierunkowe cieniowanie MC ich nie przyciemnia (patrz scianka()).
W, L, NK = 16, 32, 16
def fal(seed, n, kmax=3):
    r = random.Random(seed)
    return [(r.randint(1, kmax), r.uniform(0.4, 1.4), r.uniform(0, TAU), r.uniform(0.5, 1.0)) for _ in range(n)]
F1 = fal(3, 5); F2 = fal(9, 4, 4); F3 = fal(21, 5, 5)
def s_szum(F, x, v):  # okresowy w v (okres L)
    return sum(w * math.sin(TAU * m * v / L + kx * x + p) for m, kx, p, w in F) / sum(f[3] for f in F)

S_OG = [(255, 252, 226, 255), (255, 222, 90, 245), (252, 150, 34, 215), (222, 70, 20, 160)]
S_OG_P = [0.86, 0.60, 0.34, 0.12]
F4 = [(random.Random(40 + i).randint(4, 7), random.Random(60 + i).uniform(0.6, 1.8), random.Random(80 + i).uniform(0, TAU),
       random.Random(90 + i).uniform(0.5, 1.0)) for i in range(5)]
def s_ogien(x, v, f):
    s = v - 2 * f  # przewijanie: wzor plynie od dloni (v rosnie)
    t = v / L
    kl = math.sin(TAU * 4 * s / L)                         # „kleby”: 4 na dlugosc, brzeg na przemian puchnie
    pol = (1.0 + 6.8 * t ** 0.7) * (1 + 0.22 * kl * min(1, t * 3))
    c = 7.5 + 0.9 * s_szum(F2, 0.3, s) * t                 # lekkie wicie
    h = 1 - abs(x - c) / pol
    h += 0.40 * s_szum(F4, x, s) * (0.35 + t)              # jezory i dziury w skali ~5 tekseli
    h += 0.12 * kl                                         # rdzen przerywany w rytm klebow
    h -= max(0.0, t - 0.62) * 1.0 * (0.6 + 0.4 * s_szum(F2, x * 2, s))  # koniec rwie sie na jezory, nie zweza
    h += 0.12 * (1 - t) ** 3                               # troche jasniej przy dloni
    for p, c_ in zip(S_OG_P, S_OG):
        if h >= p: return c_
    return None

S_MR = [(250, 254, 255, 225), (196, 236, 255, 180), (132, 194, 245, 140), (80, 140, 225, 110)]
S_MR_P = [0.80, 0.52, 0.30, 0.12]
S_ISK = [(random.Random(i).uniform(2, 13), random.Random(i + 50).uniform(0, L), random.Random(i + 99).choice([2, 4])) for i in range(14)]
def s_mroz(x, v, f):
    s = v - 2 * f; t = v / L
    kl = math.sin(TAU * 3 * s / L + 1.0)                   # lagodniejsze kleby mgly (3 na dlugosc)
    pol = (1.2 + 7.0 * t ** 0.7) * (1 + 0.15 * kl * min(1, t * 3))
    h = 1 - abs(x - 7.5) / pol
    h += 0.30 * s_szum(F2, x * 1.3, s * 1.0) * (0.35 + t) + 0.25 * s_szum(F4, x * 0.8, s + 5) * (0.3 + t)
    h -= max(0.0, t - 0.6) * 0.8 * (0.6 + 0.4 * s_szum(F1, x * 2, s))
    h += 0.2 * (1 - t) ** 3
    # drobiny lodu: pojedyncze jasne piksele plynace szybciej (4 px/klatke = 2 okresy na petle)
    for ix, iv, sp in S_ISK:
        vv = (iv + sp * f) % L
        if int(x) == int(ix) and int(v) == int(vv) and t < 0.85 and abs(x - 7.5) < pol + 1:
            return (255, 255, 255, 255)
    for p, c_ in zip(S_MR_P, S_MR):
        if h >= p: return c_
    return None

def zanik(c, x, v, f, tryb):
    """koniec strumienia: ogien rwie sie dziurami z szumu (kolor zostaje zywy), mroz traci alfe schodkami (mgla)."""
    t = v / L
    if tryb == "dziury":
        prog = max(0.0, (t - 0.55) / 0.45) * 1.05 - 0.2  # prog dziur rosnie ku koncowi
        n = 0.5 + 0.5 * s_szum(F3, x * 1.7, v - 2 * f)    # organiczne dziury plynace ze strumieniem
        return None if n < prog else c
    # mroz: rwana mgla — dziury na calej dlugosci (gesciej ku koncowi), zeby nie wygladal jak tafla szkla
    if c[:3] != (255, 255, 255):  # drobiny lodu zostaja zawsze
        n = 0.5 + 0.5 * s_szum(F3, x * 1.4, v - 2 * f)
        if n < 0.22 + max(0.0, t - 0.5) * 0.9:
            return None
    k = 1.0 if t < 0.55 else (0.8 if t < 0.7 else (0.6 if t < 0.85 else 0.4))
    if t < 0.16 and c[:3] != (255, 255, 255):
        k = 0.55  # poczatek przy dloni: w 1. os. najblizej kamery, wiec rzadszy (nie zaslania rogow ekranu)
    return (*c[:3], int(c[3] * k))

def arkusz_strumienia(fn, tryb):
    im = Image.new('RGBA', (W, L * NK), (0, 0, 0, 0)); px = im.load()
    for f in range(NK):
        for v in range(L):
            for x in range(W):
                c = fn(x + 0.5, v + 0.5, f)
                c = c and zanik(c, x, v + 0.5, f, tryb)
                if c: px[x, f * L + v] = c
    return im

STRUMIENIE = {"plomienie": ("strumien_ogien", s_ogien, "dziury", 14.0), "odmrozenie": ("strumien_mroz", s_mroz, "alfa", 15.0)}
def model_strumienia(tex, pol):
    el = lambda kat: {"from": [8 - pol, 8, 0], "to": [8 + pol, 8, 16], "shade": False, "light_emission": 15,
                      "rotation": {"angle": kat, "axis": "z", "origin": [8, 8, 8]},
                      "faces": {"up": {"texture": "#s", "uv": [0, 16, 16, 0]}}}  # v=0 (dlon) przy z=0 — sprawdzone w grze
    return {"textures": {"s": f"zaklecia:item/{tex}", "particle": f"zaklecia:item/{tex}"}, "elements": [el(45), el(-45)]}

def zapisz(out):
    out = pathlib.Path(out)
    tx, md, it = out / "textures/item", out / "models/item", out / "items"
    for p in (tx, md, it):
        p.mkdir(parents=True, exist_ok=True)
    for nazwa, fn, n, ft in [("plomien", plomien, N, 1), ("plomien_b", wariant(plomien, 0.5, True), N, 1),
                             ("mroz", mroz, N, 1), ("mroz_b", wariant(mroz, 0.33, True), N, 1)]:
        arkusz(fn, n).save(tx / f"{nazwa}.png")
        (tx / f"{nazwa}.png.mcmeta").write_text(json.dumps({"animation": {"frametime": ft}}))
    krysztal().save(tx / "krysztal.png")
    js = lambda p, o: p.write_text(json.dumps(o, indent=1, ensure_ascii=False))
    for nazwa, zew, odl in [("plomienie", "plomien", False), ("odmrozenie", "mroz", True)]:
        js(md / f"{nazwa}_ikona.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"zaklecia:item/{zew}"}})
        kroki = [("s", 1.0, "s"), ("k", PULS[2], "k")] + [(f"w{i}", m, "r") for i, m in enumerate(WZROST)] + [(f"p{i}", m, "r") for i, m in enumerate(PULS)]
        for k, m, tryb in kroki:
            for j, (ob, tex) in enumerate(WARSTWY):
                js(md / f"{nazwa}_{k}_{j}.json", model_cialo(zew, m, tryb, ob, WARSTWY_1OS[zew][j], tex))
        js(it / f"{nazwa}.json", definicja(nazwa, odl))
    for j in range(len(KARTY)):
        js(md / f"mroz_odlamki_s_{j}.json", model_odlamki(1.0, 0, "s", j))
        js(md / f"mroz_odlamki_k_{j}.json", model_odlamki(PULS[2], 0, "k", j))
        for i, m in enumerate(WZROST):
            js(md / f"mroz_odlamki_w{i}_{j}.json", model_odlamki(m, 0, "r", j))
        for i, o in enumerate(OBROT):
            js(md / f"mroz_odlamki_o{i}_{j}.json", model_odlamki(PULS[i // 2], o, "r", j))
    for nazwa, (tex, fn, tryb, pol) in STRUMIENIE.items():
        arkusz_strumienia(fn, tryb).save(tx / f"{tex}.png")
        (tx / f"{tex}.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 1, "width": W, "height": L}}))
        js(md / f"strumien_{nazwa}.json", model_strumienia(tex, pol))
        js(it / f"strumien_{nazwa}.json", {"model": ref(f"strumien_{nazwa}")})
    print("zapisane:", out, sum(1 for _ in out.rglob("*.*")), "plikow")

# ---------------------------------------------------------------- v1 (archiwum)
def v1(jar):
    import io, zipfile
    OUT = pathlib.Path(__file__).parent / "src/Nexo/pack/assets/zaklecia/textures/item"
    RAMPA = [(0.00, (62, 118, 178)), (0.30, (104, 168, 222)), (0.55, (158, 212, 244)), (0.78, (214, 241, 255)), (1.00, (252, 255, 255))]
    z = zipfile.ZipFile(jar); tex = lambda n: z.read(f"assets/minecraft/textures/block/{n}")
    def kolor(t):
        for (t0, c0), (t1, c1) in zip(RAMPA, RAMPA[1:]):
            if t <= t1: return tuple(int(c0[i] + (c1[i] - c0[i]) * (t - t0) / (t1 - t0)) for i in range(3))
        return RAMPA[-1][1]
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ogien.png").write_bytes(tex("fire_0.png")); (OUT / "ogien.png.mcmeta").write_bytes(tex("fire_0.png.mcmeta"))
    im = Image.open(io.BytesIO(tex("soul_fire_0.png"))).convert("RGBA"); px = im.load()
    jas = lambda r, g, b: 0.3 * r + 0.59 * g + 0.11 * b
    wart = [jas(*px[x, y][:3]) for y in range(im.height) for x in range(im.width) if px[x, y][3]]
    lo, hi = min(wart), max(wart)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a: px[x, y] = (*kolor((jas(r, g, b) - lo) / (hi - lo)), 255)
    im.save(OUT / "szron.png"); (OUT / "szron.png.mcmeta").write_bytes(tex("soul_fire_0.png.mcmeta"))

if __name__ == "__main__":
    a = sys.argv[1:]
    if a[:1] == ["--v1"]:
        v1(a[1] if len(a) > 1 else "/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar")
    else:
        zapisz(a[0] if a else pathlib.Path(__file__).parent / "v2/src/Nexo/pack/assets/zaklecia")
