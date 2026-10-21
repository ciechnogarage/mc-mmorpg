#!/usr/bin/env python3
"""Loch 002 Bagna Zgnilych Korzeni: CALY teren proceduralnie (bez Irisa) -> kafelki .nbt + spec + podglad.

Spec gry: /opt/docker/mc/data/world1/docs/lochy/loch-002-bagna-zgnilych-korzeni.md
Dlaczego bez Irisa: bagno to baseny wody o dokladnych brzegach (sluzy osuszaja je `fill`), groble, kladki i przesmyk
z tama - Iris rozmywa waskie formy (pamiec loch001-teren-pieczenie), a woda z imagemap wylewa sie w pustke.
Tu znamy wysokosc kazdej kolumny, wiec spec od razu podaje grunt dla funkcji MD i mobow (bez mierzenia).

Zasady wody (sprawdzane asercja): kazdy blok wody ma pod soba i obok siebie tylko wode albo lity blok - nigdy
powietrze -> nic nie cieknie w pustke. Wklejanie w 2 przejsciach (bez wody, potem sama woda) - piec002.sh.

Uzycie: make_loch002.py  -> klony/<KUZNIA>/data/world/generated/loch002/structures/*.nbt + kuznia/levels/loch_002_spec.json
                             + kuznia/levels/loch_002_podglad.png
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA, LEVELS  # noqa: E402
import json, math, os, random, sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "budowle"))
import iob, struktura

OBJ = "/opt/docker/mc/data/world1/plugins/Iris/packs/overworld/objects"
GEN = f"{DATA}/world/generated/loch002/structures"
DATA_VERSION = 4671
H = 144                      # swiat x,z w [-H, H)
YW = 100                     # blok lustra wody (stoisz W wodzie na 99-100)
YL = 101                     # wierzch ladu -> gracz stoi na y=102
KAF = 64                     # kafelek szablonu (x,z)
rnd = random.Random(2002)
import time as _time
ZNACZNIK = _time.strftime("%H%M%S")

# ---------------- uklad (x, z); gracz idzie z poludniowego zachodu (+z) na polnoc (-z)
SEKCJE = [
    dict(id="przystan", nazwa="Przystan Rybakow", xz=(-56, 78), r=12),
    dict(id="kladki", nazwa="Kladki Pijawek", xz=(-28, 62), r=6),
    dict(id="wysepka", nazwa="Wysepka Martwych Drzew", xz=(0, 52), r=10),
    # r 7 -> 10: przed brama byl staw, kolowrot sluzy 1 stal w wodzie (gracz trul sie, krecac korba - sonda trasy 30.09)
    dict(id="przedtama", nazwa="Przed Tama", xz=(0, 38), r=10),
    dict(id="rozstaj", nazwa="Rozstaj", xz=(0, 0), r=8),
    dict(id="szeptucha", nazwa="Chata Szeptuchy", xz=(34, -8), r=11),
    dict(id="sluza2", nazwa="Sluza Zachodnia", xz=(-32, -6), r=6),
    dict(id="sluza3", nazwa="Sluza Wschodnia", xz=(66, -8), r=6),
    dict(id="kokoniarnia", nazwa="Kokoniarnia", xz=(54, -44), r=12),
    dict(id="grobla", nazwa="Grobla", xz=(22, -66), r=7),
    dict(id="topiel", nazwa="Topiel", xz=(0, -92), r=22),
]
S = {s["id"]: s for s in SEKCJE}
TRASA = [(-56, 78), (-42, 70), (-28, 62), (-14, 56), (0, 52), (0, 40), (0, 30), (0, 6), (0, 0), (18, -4), (34, -8),
         (46, -24), (54, -44), (40, -58), (22, -66), (10, -74), (0, -80)]
ODNOGI = [[(0, 0), (-20, -4), (-32, -6)], [(34, -8), (52, -8), (66, -8)]]
KOSCIEJ_WYSEPKA = ((14, 68), 4)          # osobna wysepka lucznika za woda (LoS)
PRZYSTAN_POMOST = [(-56, 90), (-56, 104)]  # pomost na poludnie w wode
TAMA_Z = (30, 31)                        # mur tamy przez przesmyk
PRZESMYK = (24, 37, 24)                  # z w [24,37] -> lad tylko dla |x| <= 24
# baseny sluz: (x0, x1, z0, z1) wnetrze wody; sciany = obrys 1 bl.; dno y=96
BASENY = {"b1": (-14, 14, 7, 28), "b2": (-60, -44, -18, 4), "b3": (76, 94, -18, 2)}
DNO_BASENU = 96
JEZIORO = (-18, 18, -135, -117)          # jezioro z wieza Archiwum, dno 88
DNO_JEZIORA = 88
WIEZA = (0, -126)
POOL_R = 6                               # plytka woda posrodku Topieli (trucizna)
STUDNIE = {"A": (-10, -82), "B": (-10, -102), "C": (10, -102), "D": (10, -82)}   # A osusza sluza 1, B-2, C-3, D nigdy
RAMIE = (-42, -100)                      # olbrzymie "ramie bagna" (obiekt Iris) - znak orientacyjny
OSLONY = [(-16, -92), (16, -92), (0, -76), (0, -109)]


# ---------------- szum wartosci (deterministyczny)
def _h(ix, iz, s):
    n = (ix * 374761393 + iz * 668265263 + s * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def szum(x, z, skala, s=0):
    fx, fz = x / skala, z / skala
    ix, iz = math.floor(fx), math.floor(fz)
    tx, tz = fx - ix, fz - iz
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b = _h(ix, iz, s), _h(ix + 1, iz, s)
    c, d = _h(ix, iz + 1, s), _h(ix + 1, iz + 1, s)
    return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * tz


def fbm(x, z, s=0):
    return 0.55 * szum(x, z, 23, s) + 0.3 * szum(x, z, 9, s + 1) + 0.15 * szum(x, z, 4, s + 2)


def d_seg(p, a, b):
    (px, pz), (ax, az), (bx, bz) = p, a, b
    vx, vz = bx - ax, bz - az
    L2 = vx * vx + vz * vz or 1e-9
    t = max(0.0, min(1.0, ((px - ax) * vx + (pz - az) * vz) / L2))
    return math.hypot(px - ax - t * vx, pz - az - t * vz)


def d_line(p, pts):
    return min(d_seg(p, pts[i], pts[i + 1]) for i in range(len(pts) - 1))


def w_rect(x, z, r, m=0):
    x0, x1, z0, z1 = r
    return x0 - m <= x <= x1 + m and z0 - m <= z <= z1 + m


def d_sek(x, z, sid):
    s = S[sid]; return math.hypot(x - s["xz"][0], z - s["xz"][1])


# ---------------- ksztalt wyspy
def na_wyspie(x, z):
    p = (x, z)
    d = d_line(p, TRASA) - 13
    for o in ODNOGI:
        d = min(d, d_line(p, o) - 10)
    for s in SEKCJE:
        d = min(d, math.hypot(x - s["xz"][0], z - s["xz"][1]) - s["r"] - (6 if s["id"] == "topiel" else 9))
    for r in list(BASENY.values()) + [JEZIORO]:
        x0, x1, z0, z1 = r
        dx = max(x0 - x, 0, x - x1); dz = max(z0 - z, 0, z - z1)
        d = min(d, math.hypot(dx, dz) - 6)
    (kx, kz), kr = KOSCIEJ_WYSEPKA
    d = min(d, math.hypot(x - kx, z - kz) - kr - 7)
    d = min(d, math.hypot(x - RAMIE[0], z - RAMIE[1]) - 16)
    d = min(d, d_line(p, PRZYSTAN_POMOST) - 7)
    d -= 4 * (fbm(x, z, 7) - 0.5)
    if PRZESMYK[0] <= z <= PRZESMYK[1] and abs(x) > PRZESMYK[2]:
        return False, 0
    if PRZESMYK[0] <= z <= PRZESMYK[1]:          # przesmyk: brzeg liczony od krawedzi |x|=24
        d = max(d, abs(x) - PRZESMYK[2] - 0.5)
    return d < 0, -d


# ---------------- klasyfikacja kolumny
def kolumna(x, z):
    """-> dict(typ, grunt=y wierzchu litego, woda=y lustra lub None, sciezka, ...)"""
    on, e = na_wyspie(x, z)
    if not on:
        return None
    p = (x, z)
    c = dict(typ="lad", e=e)
    dt = d_line(p, TRASA)
    do = min(d_line(p, o) for o in ODNOGI)
    c["dtr"] = min(dt, do)
    # baseny sluz
    for k, r in BASENY.items():
        if w_rect(x, z, r):
            return dict(c, typ="basen", basen=k, grunt=DNO_BASENU, woda=YW)
        if w_rect(x, z, r, 1):
            return dict(c, typ="sciana_basenu", basen=k, grunt=YL + 1)
    if w_rect(x, z, JEZIORO):
        return dict(c, typ="jezioro", grunt=DNO_JEZIORA, woda=YW)
    if w_rect(x, z, JEZIORO, 1):
        return dict(c, typ="sciana_basenu", grunt=YL + 1)
    # Topiel
    dtop = d_sek(x, z, "topiel")
    if dtop <= S["topiel"]["r"]:
        for k, (sx, sz) in STUDNIE.items():
            if abs(x - sx) <= 1 and abs(z - sz) <= 1:
                return dict(c, typ="studnia", studnia=k, grunt=DNO_BASENU - 1, woda=YW)
        if dtop <= POOL_R:
            return dict(c, typ="woda", grunt=YW - 2 + (1 if dtop > POOL_R - 2 else 0), woda=YW)
        return dict(c, typ="lad", grunt=YL, plasko=True)
    # brzeg: zawsze lad (woda nigdy nie dotyka pustki)
    if e < 3.5:
        return dict(c, typ="lad", grunt=YL + (1 if fbm(x, z, 3) > 0.62 else 0))
    # pomost przystani
    if d_line(p, PRZYSTAN_POMOST) <= 1.2 and z > 90:
        return dict(c, typ="kladka", grunt=YW - 2, woda=YW)
    sek = next((s for s in SEKCJE if math.hypot(x - s["xz"][0], z - s["xz"][1]) <= s["r"]), None)
    (kx, kz), kr = KOSCIEJ_WYSEPKA
    if sek or math.hypot(x - kx, z - kz) <= kr:
        return dict(c, typ="lad", grunt=YL, plasko=True, sekcja=sek["id"] if sek else "kosciej")
    # teren wolny: wyspy ladu z szumu, reszta zgnila woda
    n = fbm(x, z, 11)
    wiecej_ladu = 0.12 if d_sek(x, z, "kokoniarnia") < 24 or d_sek(x, z, "szeptucha") < 20 else 0
    # miedzy przystania a wysepka i wokol wysepki lucznika - celowo woda (kladki, LoS przez wode)
    wiecej_wody = 0.2 if (-50 <= x <= 20 and 50 <= z <= 76) else 0
    lad = n + wiecej_ladu - wiecej_wody > 0.53
    # kladka 5 bl. (skosna kladka 3 bl. miala "zabki" - na skosnym kroku gracz/bot zsuwal sie do wody, sonda trasy 30.09)
    if c["dtr"] <= (1.6 if lad else 2.5):
        return dict(c, typ="sciezka" if lad else "kladka", grunt=YL if lad else YW - 2, woda=None if lad else YW)
    if lad:
        return dict(c, typ="lad", grunt=YL + (1 if n > 0.66 else 0) + (1 if n > 0.76 and c["dtr"] > 6 else 0))
    return dict(c, typ="woda", grunt=YW - 1 - (1 if fbm(x, z, 5) > 0.5 else 0), woda=YW)


# ---------------- palety
def wybierz(pal, x, y, z, s=0, plamy=False):
    """plamy=True: material z szumu (placki 5-10 bl.), nie z losu na blok - losowanie co blok dawalo szachownice
    (krytyka Lochu 001: "szachownica mchu")."""
    tot = sum(pal.values())
    if plamy:
        v = min(0.999, max(0.0, (szum(x, z, 7, 60 + s) - 0.5) * 1.8 + 0.5 + (_h(x, z, 61 + s) - 0.5) * 0.12)) * tot
    else:
        v = _h(x * 31 + y, z * 17 - y, 77 + s) * tot
    for b, w in pal.items():
        v -= w
        if v <= 0:
            return b
    return b


P_LAD = {"mud": 26, "grass_block": 26, "moss_block": 16, "podzol": 10, "coarse_dirt": 8, "rooted_dirt": 8, "muddy_mangrove_roots": 6}
P_SCIEZKA = {"coarse_dirt": 5, "packed_mud": 4, "rooted_dirt": 2, "mud": 2}
P_DNO = {"mud": 50, "clay": 18, "dirt": 14, "gravel": 10, "muddy_mangrove_roots": 8}
P_POD = {"dirt": 4, "mud": 3, "packed_mud": 2, "clay": 1}
P_SKALA = {"stone": 5, "tuff": 3, "andesite": 2, "dirt": 3, "clay": 1}
P_MUR = {"mossy_stone_bricks": 5, "stone_bricks": 3, "cracked_stone_bricks": 2, "mud_bricks": 2}


class Swiat:
    def __init__(self):
        self.b = {}           # (x,y,z) -> blok
        self.kol = {}         # (x,z) -> kolumna
        self.grunt = {}       # (x,z) -> y na ktorym gracz STOI (wierzch litego +1)
        self.slad = None      # gdy zbior: zapisuje kazda pozycje postawiona przez budowle (celowe elementy na polanach)

    def set(self, x, y, z, blok, nadpisz=True):
        if blok in ("air", "minecraft:air"):          # powietrze = brak bloku (szablon nie wycina, a w pustce nic nie ma)
            self.b.pop((x, y, z), None); return
        if nadpisz or (x, y, z) not in self.b:
            self.b[(x, y, z)] = blok if ":" in blok else "minecraft:" + blok
            if self.slad is not None:
                self.slad.add((x, y, z))

    def get(self, x, y, z):
        return self.b.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, blok):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, blok)


W = Swiat()


def teren():
    for x in range(-H, H):
        for z in range(-H, H):
            c = kolumna(x, z)
            if not c:
                continue
            W.kol[(x, z)] = c
            g = c["grunt"]
            # spod wyspy: stozek w dol, grubszy w srodku
            glab = int(5 + min(c["e"], 30) * 0.55 + 8 * fbm(x, z, 21))
            dol = g - glab
            if c["typ"] == "sciana_basenu":                  # sciana musi siegac ponizej dna jeziora/basenu
                dol = min(dol, DNO_JEZIORA - 3)
            for y in range(dol, g + 1):
                if y == g:
                    if c["typ"] in ("woda", "basen", "jezioro", "studnia", "kladka"):
                        blok = wybierz(P_DNO, x, y, z, plamy=True)
                    elif c["typ"] == "sciezka":
                        blok = wybierz(P_SCIEZKA, x, y, z, plamy=True)
                    elif c["typ"] == "sciana_basenu":
                        blok = wybierz(P_MUR, x, y, z)
                    else:
                        blok = wybierz(P_LAD, x, y, z, plamy=True)
                elif c["typ"] == "sciana_basenu" and y >= DNO_BASENU - 1:
                    blok = wybierz(P_MUR, x, y, z)
                elif y >= g - 3:
                    blok = wybierz(P_POD, x, y, z)
                else:
                    blok = wybierz(P_SKALA, x, y, z)
                W.set(x, y, z, blok)
            if c.get("woda"):
                for y in range(g + 1, c["woda"] + 1):
                    W.set(x, y, z, "water")
            stoi = (c["woda"] + 1) if c.get("woda") else g + 1
            W.grunt[(x, z)] = stoi


def kladki():
    """Kladki: deski na y=YL nad woda (gracz stoi na 102 jak na ladzie), slupy z pni mangrowca co 4 bl."""
    for (x, z), c in W.kol.items():
        if c["typ"] == "kladka":
            W.set(x, YL, z, "spruce_planks" if _h(x, z, 5) > 0.15 else "mangrove_planks")
            W.grunt[(x, z)] = YL + 1
            if (x + z) % 4 == 0 and _h(x, z, 6) > 0.4:
                for y in range(c["grunt"] + 1, YW + 1):
                    W.set(x, y, z, "mangrove_log[axis=y]")
    # koniec pomostu: dwa pale z latarniami
    for x in (-57, -55):
        W.set(x, YL + 1, 104, "mangrove_fence"); W.set(x, YL + 2, 104, "lantern[hanging=false]")


def tama():
    """Mur tamy przez przesmyk (z=30..31, |x|<=24), brama 3 bl. na kladce, kolowrot sluzy 1 po poludniowej stronie."""
    for x in range(-PRZESMYK[2] - 1, PRZESMYK[2] + 2):
        for z in TAMA_Z:
            if (x, z) not in W.kol:
                continue
            g = W.kol[(x, z)]["grunt"]
            top = YL + 4 + (1 if x % 4 == 0 else 0)
            for y in range(min(g, DNO_BASENU), top + 1):
                W.set(x, y, z, wybierz(P_MUR, x, y, z, 3))
            W.grunt[(x, z)] = top + 1
    # przejscie bramy: kladka przez mur, krata z zelaza (usuwana przez sluze 1)
    for x in (-1, 0, 1):
        for z in TAMA_Z:
            W.set(x, YL, z, "spruce_planks")
            for y in range(YL + 1, YL + 4):
                W.set(x, y, z, "iron_bars")
            W.set(x, YL + 4, z, "mossy_stone_bricks")
            W.grunt[(x, z)] = YL + 1
    # kolowrot sluzy 1 (dekor) - przy nim staje korba (mob)
    W.set(3, YL + 1, 33, "spruce_log[axis=y]"); W.set(3, YL + 2, 33, "grindstone[face=floor,facing=north]")
    W.set(-3, YL + 1, 33, "chain[axis=y]"); W.set(-3, YL + 2, 33, "chain[axis=y]"); W.set(-3, YL + 3, 33, "chain[axis=y]")


def most_b1():
    """Most przez basen 1 (x=-1..1, z=6..29), porecze; zejscie po schodach na wschodniej scianie basenu."""
    x0, x1, z0, z1 = BASENY["b1"]
    for z in range(z0 - 1, z1 + 2):
        for x in (-1, 0, 1):
            W.set(x, YL, z, "spruce_planks"); W.grunt[(x, z)] = YL + 1
        for x in (-1, 0, 1):
            W.set(x, YL + 1, z, "air")                   # konce mostu przecinaja sciany basenu (wierzch 102)
        for x in (-2, 2):
            W.set(x, YL + 1, z, "spruce_fence")
        if z % 4 == 0:
            for x in (-2, 2):
                for y in range(DNO_BASENU + 1, YL + 1):
                    W.set(x, y, z, "spruce_log[axis=y]")


def schody_basenu(k, bok):
    """Schody z brzegu na dno basenu (widoczne dopiero po osuszeniu; pod woda nie przeszkadzaja)."""
    x0, x1, z0, z1 = BASENY[k]
    if bok == "wschod":
        for i, y in enumerate(range(YL, DNO_BASENU, -1)):
            z = z0 + 1 + i
            W.set(x1, y, z, "mud_brick_stairs[facing=north]")
            for yy in range(DNO_BASENU + 1, y):
                W.set(x1, yy, z, "mud_bricks")
        # przejscie w scianie na szczycie schodow
        W.set(x1 + 1, YL + 1, z0 + 1, "air"); W.grunt[(x1 + 1, z0 + 1)] = YL + 1
    else:  # polnoc
        for i, y in enumerate(range(YL, DNO_BASENU, -1)):
            x = x0 + 1 + i
            W.set(x, y, z0, "mud_brick_stairs[facing=west]")
            for yy in range(DNO_BASENU + 1, y):
                W.set(x, yy, z0, "mud_bricks")
        W.set(x0 + 1, YL + 1, z0 - 1, "air"); W.grunt[(x0 + 1, z0 - 1)] = YL + 1


def obiekt(sciezka, x, y_dol, z, obrot=0, podmiany=None, pomin=("minecraft:water",)):
    """Wklej obiekt Iris tak, by jego najnizszy blok stal na y_dol."""
    o = iob.czytaj(f"{OBJ}/{sciezka}")
    ymin = min(k[1] for k in o["bloki"])
    for (bx, by, bz), s in o["bloki"].items():
        nazwa = s.split("[")[0]
        if nazwa in pomin:
            continue
        for _ in range((obrot // 90) % 4):
            bx, bz = -bz, bx
        if podmiany:
            for a, b in podmiany.items():
                s = s.replace(a, b)
        if "leaves" in nazwa:                          # random_tick 0 w lochu, ale liscie i tak maja nie opadac
            if "persistent=" in s: s = s.replace("persistent=false", "persistent=true")
            elif "[" in s: s = s[:-1] + ",persistent=true]"
            else: s += "[persistent=true]"
        W.set(x + bx, y_dol + (by - ymin), z + bz, s)
    return o


def chata(x, z, ciemna=False):
    pod = {"spruce": "dark_oak"} if ciemna else None
    o = obiekt("jigsaw/swamp-hut/swamp-hut.iob", x, YW - 1, z, podmiany=pod)
    return o


def martwe_drzewo(x, z, wys, s):
    g = W.grunt.get((x, z), YL + 1)
    for y in range(g - 2, g + wys):
        W.set(x, y, z, "stripped_dark_oak_log[axis=y]" if _h(x, y, s) > 0.3 else "dark_oak_log[axis=y]")
    r = random.Random(s)
    for _ in range(r.randint(1, 3)):
        yy = g + r.randint(wys // 2, wys - 1); dx, dz = r.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
        for i in range(1, r.randint(2, 4)):
            W.set(x + dx * i, yy + (i // 2), z + dz * i, f"stripped_dark_oak_log[axis={'x' if dx else 'z'}]")
    for _ in range(r.randint(2, 5)):
        dx = r.choice([-1, 1])                        # pnacze przy pniu, przyczepione w strone pnia
        W.set(x + dx, g + r.randint(1, wys - 1), z, "vine[west=true]" if dx == 1 else "vine[east=true]", nadpisz=False)


def wieza_archiwum():
    """Szczyt zatopionej wiezy Archiwum w jeziorze na polnocy (Loch 003) - osuszenie po bossie odslania cala."""
    cx, cz = WIEZA
    for y in range(DNO_JEZIORA + 1, YL + 14):
        for x in range(cx - 5, cx + 6):
            for z in range(cz - 5, cz + 6):
                d = math.hypot(x - cx, z - cz)
                if 3.6 <= d <= 5.2:
                    okno = (y % 6 in (3, 4)) and (abs(x - cx) <= 1 or abs(z - cz) <= 1)
                    ulom = y > YL + 8 and szum(x * 3, z * 3 + y, 2, 9) > 0.55 - (y - YL - 8) * 0.06
                    if not okno and not ulom:
                        W.set(x, y, z, wybierz({"stone_bricks": 4, "mossy_stone_bricks": 4, "cracked_stone_bricks": 2, "chiseled_stone_bricks": 1}, x, y, z, 4))
                elif d < 3.6 and y == DNO_JEZIORA + 1:
                    W.set(x, y, z, "polished_andesite")
    # drzwi archiwum na dnie (tabliczka dla ciekawskich po osuszeniu)
    for y in (DNO_JEZIORA + 2, DNO_JEZIORA + 3, DNO_JEZIORA + 4):   # wejscie do wiezy (wybite w murze)
        for x in (cx - 1, cx, cx + 1):
            W.set(x, y, cz + 4, "air"); W.set(x, y, cz + 5, "air")
            W.set(x, y, cz + 4, "water"); W.set(x, y, cz + 5, "water")
    W.set(cx, DNO_JEZIORA + 5, cz + 5, "chiseled_stone_bricks")


def arena():
    cx, cz = S["topiel"]["xz"]
    for i, (ox, oz) in enumerate(OSLONY):
        for dx in (-1, 0, 1):
            for dz in (0, 1):
                h = 3 if (dx == 0) else 2 - (i + dx) % 2
                for y in range(YL + 1, YL + 1 + h):
                    W.set(ox + dx, y, oz + dz, wybierz({"mossy_cobblestone": 4, "cobblestone": 2, "mossy_stone_bricks": 2}, ox + dx, y, oz + dz, 8))
                W.grunt[(ox + dx, oz + dz)] = YL + 1 + h
    # obramowanie studni (niskie, czytelne z daleka)
    for k, (sx, sz) in STUDNIE.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if max(abs(dx), abs(dz)) == 2 and (dx + dz) % 2 == 0:
                    W.set(sx + dx, YL + 1, sz + dz, "mud_brick_wall")


def przyroda():
    """Mangrowce (obiekty Iris), martwe drzewa, trzcina, grzyby, lilie, swietliki - poza sciezka i polanami walki."""
    mangrowce = [f"trees/mangrove/mangrove{i}.iob" for i in (10, 11, 12, 13, 14, 15, 16, 17, 18)]
    zajete = []
    punkty = sorted(W.kol.keys())
    r = random.Random(7)
    r.shuffle(punkty)
    ile = 0
    # dojscia do schodow basenow wolne od mangrowcow: korzenie zarastaly przejscie b1 (15,8) -> trasa, bot utknal (trasa.js, 21.10)
    dojscia = []
    for k, bok in (("b1", "wschod"), ("b2", "polnoc"), ("b3", "polnoc")):
        x0, x1, z0, z1 = BASENY[k]
        p = (x1 + 1, z0 + 1) if bok == "wschod" else (x0 + 1, z0 - 1)
        dojscia.append([p, min(TRASA + [q for o in ODNOGI for q in o], key=lambda q: math.hypot(q[0] - p[0], q[1] - p[1]))])
    for (x, z) in punkty:
        c = W.kol[(x, z)]
        if c["typ"] not in ("lad", "woda") or c.get("plasko") or c["dtr"] < 5 or c["e"] < 6:
            continue
        if any(math.hypot(x - s["xz"][0], z - s["xz"][1]) < s["r"] + 4 for s in SEKCJE):
            continue
        if any(math.hypot(x - a, z - b) < 9 for a, b in zajete):
            continue
        gesto = d_sek(x, z, "kokoniarnia") < 26
        if not gesto and _h(x, z, 13) > (0.8 if c["typ"] == "lad" else 0.6):
            continue
        sc, ob = r.choice(mangrowce), r.choice([0, 90, 180, 270])
        zajete.append((x, z))
        # pomijamy PO losowaniu: reszta rozstawienia zostaje jak byla (wczesniej przesuniecie wsadzilo drzewo na kladki)
        if any(d_line((x, z), d) < 10 for d in dojscia):
            continue
        obiekt(sc, x, YW - 2, z, obrot=ob)
        ile += 1
    # olbrzymie "ramie bagna" za Topiela - znak orientacyjny widoczny z calej mapy
    obiekt("structures/swampforearm2.iob", RAMIE[0], YW - 6, RAMIE[1], obrot=90)
    # martwe drzewa: wysepka (oslony przed lucznikiem) + rozrzucone
    for (x, z) in [(-6, 48), (5, 55), (-3, 58), (7, 46), (14, 69), (-52, 72), (22, 8), (-24, -8), (44, -30),
                   (62, -52), (40, -46), (30, -70), (-18, -70), (20, -110)]:
        assert min(d_line((x, z), TRASA), min(d_line((x, z), o) for o in ODNOGI)) >= 2.5, f"martwe drzewo na trasie: {x},{z}"
    for i, (x, z) in enumerate([(-6, 48), (5, 55), (-3, 58), (7, 46), (14, 69), (-52, 72), (22, 8), (-24, -8), (44, -30),
                                (62, -52), (40, -46), (30, -70), (-18, -70), (20, -110)]):
        martwe_drzewo(x, z, 5 + i % 4, 100 + i)
    # drobnica
    for (x, z), c in W.kol.items():
        g = W.grunt[(x, z)]
        if W.get(x, g, z):         # cos juz stoi (budowla, drzewo)
            continue
        v = _h(x, z, 41)
        fight = c.get("plasko") or c["dtr"] < 2.2
        if c["typ"] == "lad":
            if fight:
                if v < 0.06: W.set(x, g, z, "short_grass")
                elif v < 0.10: W.set(x, g, z, "moss_carpet")
            else:
                if v < 0.16: W.set(x, g, z, "short_grass")
                elif v < 0.21: W.set(x, g, z, "fern")
                elif v < 0.25:
                    W.set(x, g, z, "tall_grass[half=lower]"); W.set(x, g + 1, z, "tall_grass[half=upper]")
                elif v < 0.28: W.set(x, g, z, "firefly_bush")
                elif v < 0.30: W.set(x, g, z, "brown_mushroom")
                elif v < 0.31: W.set(x, g, z, "red_mushroom")
                elif v < 0.37: W.set(x, g, z, "moss_carpet")
                elif v < 0.40: W.set(x, g, z, f"leaf_litter[facing=north,segment_amount={1 + int(_h(x, z, 43) * 4)}]")
        elif c["typ"] == "woda" and c["dtr"] > 2.5 and d_sek(x, z, "topiel") > POOL_R + 1:
            lad_obok = sum(W.kol.get((x + dx, z + dz), {}).get("typ") == "lad" for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if lad_obok and v < 0.35 and c["dtr"] > 3:
                # trzcina: blok pod nia (lustro wody) ma wode obok -> przezywa aktualizacje
                W.set(x, YW, z, "mud")
                for y in range(YW + 1, YW + 3 + int(_h(x, z, 42) * 2)):
                    W.set(x, y, z, "sugar_cane")
                W.grunt[(x, z)] = YW + 1
            elif v < 0.07:
                W.set(x, YW + 1, z, "lily_pad")
    return ile


def budowle():
    # przystan: chata rybaczki na palach + ognisko + pomost
    chata(-66, 74)
    W.set(-52, YL + 1, 74, "campfire[lit=true]")
    for x, z in ((-54, 72), (-50, 72), (-50, 76)):
        W.set(x, YL + 1, z, "spruce_log[axis=x]" if z == 72 else "spruce_log[axis=z]")
    W.set(-59, YL + 1, 70, "barrel[facing=up]"); W.set(-60, YL + 1, 70, "barrel[facing=up]")
    # chata szeptuchy (ciemna), kociol z wywarem przed wejsciem
    chata(45, 2, ciemna=True)            # z dala od trasy: (42,-12) okap nad odnoga, (44,-17) na sciezce do Kokoniarni
    W.set(36, YL + 1, -6, "water_cauldron[level=3]")
    W.set(35, YL + 1, -6, "campfire[lit=true,signal_fire=false]")
    # kolowroty sluz 2 i 3
    for (x, z) in ((-38, -6), (72, -8)):
        W.set(x, YL + 1, z, "spruce_log[axis=y]"); W.set(x, YL + 2, z, "grindstone[face=floor,facing=north]")
    # wrak na dnie basenu 1, skrzynia topielca w basenie 2, dzwon w basenie 3
    obiekt("structures/shipsidewaysbackhalfdegraded.iob", 8, DNO_BASENU + 1, 18, obrot=90)
    W.set(-8, DNO_BASENU + 1, 20, "barrel[facing=up]")          # skrzynia wraku (sekret po osuszeniu basenu 1)
    W.set(-9, DNO_BASENU + 1, 20, "spruce_slab"); W.set(-8, DNO_BASENU + 1, 21, "spruce_trapdoor[facing=north,half=bottom,open=false]")
    W.set(-51, DNO_BASENU + 1, -7, "barrel[facing=up]")
    for dx, dz in ((1, 0), (0, 1), (-1, 1)):
        W.set(-51 + dx, DNO_BASENU + 1, -7 + dz, "bone_block[axis=x]")
    W.set(83, DNO_BASENU + 1, -8, "mossy_stone_bricks"); W.set(83, DNO_BASENU + 2, -8, "mossy_stone_brick_wall")
    W.set(83, DNO_BASENU + 3, -8, "mossy_stone_brick_wall"); W.set(83, DNO_BASENU + 4, -8, "bell[attachment=floor,facing=north]")
    # kokoniarnia: pajeczyny i kokony (biala welna + pajeczyna) miedzy drzewami
    r = random.Random(55)
    kx, kz = S["kokoniarnia"]["xz"]
    for _ in range(26):
        x, z = kx + r.randint(-16, 16), kz + r.randint(-16, 16)
        if (x, z) in W.grunt and math.hypot(x - kx, z - kz) > 5 and W.kol[(x, z)]["typ"] not in ("kladka", "sciezka"):
            W.set(x, W.grunt[(x, z)] + r.randint(0, 3), z, "cobweb", nadpisz=False)
    for (x, z) in ((kx - 8, kz - 6), (kx + 7, kz - 8), (kx - 9, kz + 7)):
        g = W.grunt[(x, z)]
        W.set(x, g, z, "white_wool"); W.set(x, g + 1, z, "white_wool"); W.set(x, g + 2, z, "cobweb")
    # grobla: kamienne slupki przy ognisku przed Topiela
    gx, gz = S["grobla"]["xz"]
    W.set(gx, YL + 1, gz, "campfire[lit=true]")
    for x, z in ((gx - 3, gz - 3), (gx + 3, gz + 3)):
        W.set(x, YL + 1, z, "spruce_log[axis=x]")


WOLNE = ("minecraft:short_grass", "minecraft:moss_carpet", "minecraft:lily_pad")
def przejscie(kol, x=None, z=None):
    if z in TAMA_Z and x is not None and abs(x) > 1:      # mur tamy obok bramy - to sciana, nie przejscie
        return False
    return kol["typ"] in ("kladka", "sciezka") or kol.get("plasko")


def oczysc_przejscie():
    """Korzenie mangrowcow (obiekty Iris) siegaja ~5 bl. od pnia i wchodzily na kladki na wysokosc nog/glowy
    (68 blokow, takze na pomoscie startowym - sonda trasy 30.09). Czyscimy 2 bloki nad kazda kolumna trasy i polan."""
    n = 0
    for (x, z), c in W.kol.items():
        if not przejscie(c, x, z):
            continue
        g = W.grunt[(x, z)]
        for y in (g, g + 1):
            b = W.b.get((x, y, z))
            if b and b not in WOLNE:
                W.set(x, y, z, "air"); n += 1
    return n


NA_TRASIE_WOLNO = ("minecraft:iron_bars", "minecraft:mangrove_fence", "minecraft:lantern[hanging=false]")


def sprawdz_przejscie(dozwolone):
    """Budowle moga stac na polanach (ognisko, lawy, oslony), ale NIE na sciezce/kladce - wyjatek: krata bramy i pale
    na koncu pomostu (celowe). Chata postawiona na sciezce blokowala droge do Kokoniarni (sonda trasy 30.09)."""
    zle = []
    for (x, z), c in W.kol.items():
        if not przejscie(c, x, z):
            continue
        trasa = c["typ"] in ("kladka", "sciezka")
        g = W.grunt[(x, z)]
        for y in (g, g + 1):
            b = W.b.get((x, y, z))
            if not b or b in WOLNE:
                continue
            if (x, y, z) in dozwolone and (not trasa or b in NA_TRASIE_WOLNO):
                continue
            zle.append((x, y, z, b))
    return zle


PRZECHODNIE = ("air", "short_grass", "tall_grass", "fern", "large_fern", "moss_carpet", "lily_pad", "cobweb", "firefly_bush",
               "brown_mushroom", "red_mushroom", "leaf_litter", "vine", "sugar_cane", "lantern", "chain")


def przechodnia_trasa(cele, start=(-56, 100)):
    """Czy da sie DOJSC suchą nogą (bez brodzenia) od startu do kazdego celu. Krata otwarta, baseny osuszone (stan po
    sluzach). Krok w gore <= 1, w dol <= 4, 2 bloki wolne nad stopami. Pathfinder bota grzezl w gestym mangrowcu
    mimo drogi - ten test mowi o TERENIE, nie o bocie."""
    import collections
    def blok(x, y, z):
        b = W.b.get((x, y, z))
        if b is None:
            return "air"
        n = b.split(":")[1].split("[")[0]
        if n == "water":
            return "water"
        if n == "iron_bars":
            return "air"                                  # krata otwarta
        return n
    def stoi(x, z, y0):
        """wysokosc stop przy wejsciu z wysokosci y0 (szukamy gruntu w zakresie y0+1 .. y0-4)"""
        for y in range(y0 + 1, y0 - 5, -1):
            pod = blok(x, y - 1, z)
            if pod in PRZECHODNIE or pod == "water":
                continue
            if blok(x, y, z) in PRZECHODNIE and blok(x, y + 1, z) in PRZECHODNIE:
                if blok(x, y, z) == "water" or blok(x, y - 1, z) == "water":
                    return None
                return y
            return None
        return None
    sx, sz = start
    y0 = W.grunt[(sx, sz)]
    dro = {(sx, sz, y0)}; q = collections.deque([(sx, sz, y0)])
    osiagniete = {}
    while q:
        x, z, y = q.popleft()
        osiagniete.setdefault((x, z), y)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if (nx, nz) not in W.kol:
                continue
            ny = stoi(nx, nz, y)
            if ny is None or blok(x, y + 2, z) not in PRZECHODNIE and ny > y:
                continue
            k = (nx, nz, ny)
            if k not in dro:
                dro.add(k); q.append(k)
    brak = {n: xz for n, xz in cele.items() if not any((xz[0] + a, xz[1] + b) in osiagniete for a in (-1, 0, 1) for b in (-1, 0, 1))}
    return brak, len(osiagniete)


def sprawdz_wode():
    zle = []
    for (x, y, z), b in W.b.items():
        if b != "minecraft:water":
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
            if (x + dx, y + dy, z + dz) not in W.b:
                zle.append((x, y, z, dx, dy, dz)); break
    return zle


def zapisz():
    os.makedirs(GEN, exist_ok=True)
    for f in os.listdir(GEN):
        os.remove(f"{GEN}/{f}")
    kaf = {}
    for (x, y, z), b in W.b.items():
        if b == "minecraft:air":
            continue
        k = (x // KAF, z // KAF, b == "minecraft:water")
        kaf.setdefault(k, {})[(x, y, z)] = b
    lista = []
    for (i, j, woda), bl in sorted(kaf.items()):
        # znacznik czasu w nazwie: serwer BUFORUJE szablony po id do restartu - ta sama nazwa = stary teren (30.09)
        nazwa = f"{'w' if woda else 't'}{ZNACZNIK}_{i + 3}_{j + 3}"
        info = struktura.zapisz(bl, f"{GEN}/{nazwa}.nbt", DATA_VERSION)
        lista.append(dict(nazwa=nazwa, woda=woda, rog=info["offset"], blokow=info["blokow"]))
    # powietrze wycinajace (przejscie w scianie basenu) - osobno, po wszystkim
    return lista


def podglad(sciezka):
    s = 3
    img = Image.new("RGB", (2 * H * s, 2 * H * s), (16, 16, 26))
    px = img.load()
    top = {}
    for (x, y, z), b in W.b.items():
        if b == "minecraft:air":
            continue
        if y > top.get((x, z), (-999, ""))[0]:
            top[(x, z)] = (y, b)
    KOL = {"water": (52, 84, 70), "lily_pad": (40, 120, 40), "mud": (70, 58, 52), "grass_block": (86, 110, 52),
           "moss_block": (80, 120, 50), "planks": (130, 100, 60), "leaves": (50, 90, 40), "log": (80, 60, 40),
           "wood": (80, 60, 40), "roots": (80, 60, 45), "brick": (110, 110, 105), "cane": (120, 170, 80),
           "iron_bars": (200, 200, 200), "wool": (230, 230, 230), "cobweb": (220, 220, 220)}
    for (x, z), (y, b) in top.items():
        n = b.split(":")[1].split("[")[0]
        col = next((v for k, v in KOL.items() if k in n), (110, 105, 80))
        sh = max(0.6, min(1.3, 1 + (y - YL) * 0.05))
        col = tuple(min(255, int(c * sh)) for c in col)
        for a in range(s):
            for bb in range(s):
                px[(x + H) * s + a, (z + H) * s + bb] = col
    d = ImageDraw.Draw(img)
    for i in range(len(TRASA) - 1):
        (ax, az), (bx, bz) = TRASA[i], TRASA[i + 1]
        d.line([((ax + H) * s, (az + H) * s), ((bx + H) * s, (bz + H) * s)], fill=(250, 220, 90), width=1)
    for sk in SEKCJE:
        x, z = sk["xz"]
        d.ellipse(((x + H) * s - 5, (z + H) * s - 5, (x + H) * s + 5, (z + H) * s + 5), outline=(255, 255, 255))
        d.text(((x + H) * s + 8, (z + H) * s - 6), sk["nazwa"], fill=(255, 255, 255))
    for k, (x, z) in STUDNIE.items():
        d.text(((x + H) * s - 3, (z + H) * s - 5), k, fill=(255, 120, 120))
    img.save(sciezka)


if __name__ == "__main__":
    teren(); kladki()
    drzew = przyroda()
    oczyszczone = oczysc_przejscie()
    W.slad = set()
    tama(); most_b1()
    schody_basenu("b1", "wschod"); schody_basenu("b2", "polnoc"); schody_basenu("b3", "polnoc")
    budowle(); wieza_archiwum(); arena()
    # budowle celowo stoja na polanach (ognisko, lawy, beczki, oslony, obramowania studni, krata) - dozwolone
    zle_p = sprawdz_przejscie(W.slad); W.slad = None
    assert not zle_p, f"przejscie zablokowane w {len(zle_p)} miejscach, np. {zle_p[:6]}"
    print("oczyszczone bloki nad trasa:", oczyszczone)
    # stan "po sluzach": baseny bez wody (sekrety na dnie osiagalne schodami)
    zapas = {k: v for k, v in W.b.items() if v == "minecraft:water" and any(w_rect(k[0], k[2], r) for r in BASENY.values())}
    for k in zapas:
        del W.b[k]
    cele = {"kladki": S["kladki"]["xz"], "wysepka": S["wysepka"]["xz"], "kolowrot1": (4, 34), "most": (0, 18),
            "rozstaj": S["rozstaj"]["xz"], "wrak": (-8, 18), "szeptucha": (36, -6), "kolowrot2": (-37, -5),
            "skrzynia": (-51, -6), "kokoniarnia": S["kokoniarnia"]["xz"], "kokon_borka": (61, -51), "kolowrot3": (71, -7),
            "dzwon": (83, -6), "grobla": S["grobla"]["xz"], "arena": (8, -80), "srodek_areny": (0, -80)}
    brak, ile = przechodnia_trasa(cele)
    W.b.update(zapas)
    assert not brak, f"nieosiagalne sucha noga: {brak}"
    print(f"przechodniosc: {len(cele)}/{len(cele)} celow osiagalnych, {ile} kolumn dostepnych")
    zle = sprawdz_wode()
    assert not zle, f"woda styka sie z powietrzem w {len(zle)} miejscach, np. {zle[:5]}"
    lista = zapisz()
    podglad(f"{LEVELS}/loch_002_podglad.png")
    xs = [k[0] for k in W.kol]; zs = [k[1] for k in W.kol]
    spec = dict(id="loch_002", yw=YW, yl=YL, sekcje=SEKCJE, trasa=TRASA, odnogi=ODNOGI, baseny=BASENY, jezioro=JEZIORO,
                dno_basenu=DNO_BASENU, dno_jeziora=DNO_JEZIORA, studnie=STUDNIE, oslony=OSLONY, wieza=WIEZA,
                kosciej=KOSCIEJ_WYSEPKA, kafelki=lista, zakres=[min(xs), max(xs), min(zs), max(zs)],
                grunt={f"{x},{z}": g for (x, z), g in W.grunt.items()},
                typ={f"{x},{z}": c["typ"] for (x, z), c in W.kol.items()})
    json.dump(spec, open(f"{LEVELS}/loch_002_spec.json", "w"))
    print(f"kolumn {len(W.kol)} | blokow {len(W.b)} | kafelkow {len(lista)} | mangrowcow {drzew} | zakres x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)}")
