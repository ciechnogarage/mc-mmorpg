#!/usr/bin/env python3
"""Loch 001 Kwietna Polana: opis 9 sekcji -> obraz sterujacy + pack Irisa + spec (wspolrzedne sekcji).

Spec gry: /opt/docker/mc/data/world1/docs/lochy/loch-001-kwietna-polana.md (sekcja 2).
Zasady z pomiarow (make_poziom.py, pamiec iris-imagemap-progi / iris-wyspa-w-pustce):
  * region[0] = pustka, bez `rarity`, piksele w srodkach pasm 256/N,
  * rockPalette = powietrze -> wiszaca wyspa, czyste urwisko na brzegu,
  * WSZYSTKIE biomy na jednym generatorze (inaczej wysokosci sie sumuja), plaskie polany min=max,
  * polana walki >= r 12 (mieszanie wysokosci ~14 bl.), arena r 30.
Teren jest docelowo ZAMRAZANY (pieczony raz w kuzni, potem gotowe regiony w szablonie MD) -
Iris nie generuje nic na produkcji.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA, LEVELS  # noqa: E402
import json, math, os, shutil, sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_poziom as MP   # dekoratory/drzewa/snippety z dzialajacego packa (budowa przez mutacje)

OUT_ROOT = sys.argv[1] if len(sys.argv) > 1 else f"{DATA}/plugins/Iris/packs"
PREVIEW_DIR = LEVELS
PID = "loch_001"
SIZE = 352                      # px = bloki; srodek obrazu = (0,0) swiata
Y0 = 100                        # wysokosc polan

# --- wyspa: wydluzona pld-pln (gracz idzie z poludnia +z na polnoc -z)
RIM = 5

# --- sekcje (kolejnosc przebiegu). r = promien plaskiej polany walki / placu.
SEKCJE = [
    dict(id="oboz",      nazwa="Oboz Przybycia",      xz=(0, 118),   r=13),
    dict(id="sciezka",   nazwa="Lesna Sciezka",       xz=(38, 74),   r=12),
    dict(id="lucznicy",  nazwa="Zakret Lucznikow",    xz=(46, 22),   r=16),
    dict(id="rytual",    nazwa="Polana Rytualu",      xz=(6, -14),   r=13),
    dict(id="zasoby",    nazwa="Strefa Zasobow",      xz=(-62, 4),   r=12),   # boczna, opcjonalna
    dict(id="brama",     nazwa="Zlamana Brama",       xz=(0, -48),   r=3),    # waskie przejscie
    dict(id="elita",     nazwa="Legowisko Elity",     xz=(0, -72),   r=13),
    dict(id="arena",     nazwa="Serce Gaju",          xz=(0, -112),  r=30),
    dict(id="wyjscie",   nazwa="Wyjscie z Nagroda",   xz=(0, -150),  r=8),
]
S = {s["id"]: s for s in SEKCJE}
# glowna sciezka (lamana przez punkty) + odnoga do strefy zasobow
TRASA = [S["oboz"]["xz"], (14, 100), S["sciezka"]["xz"], (58, 48), S["lucznicy"]["xz"], (26, 2),
         S["rytual"]["xz"], (2, -32), S["brama"]["xz"], S["elita"]["xz"], S["arena"]["xz"], S["wyjscie"]["xz"]]
ODNOGA = [(-6, -8), (-34, -2), S["zasoby"]["xz"]]
SCIEZKA_POL = 1.5               # pol szerokosci ubitej sciezki
POBOCZE = 3                     # pas bez wysokich roslin przy sciezce
POBOCZE_POLANY = 3
# SKALY Z TERENU WYLACZONE. Zmierzone w 4 wypiekach: biom skal (generator 107-116) podnosi teren w promieniu
# ~14 bl. + r skaly, psujac plaskosc sasiednich polan (rozrzut 3-5), a waskie grzbiety rozmywa w pagorki
# (gardziel bramy 51 bl.). Oslony lucznikow i gardziel bramy = glazy/ruiny wklejane FAWE po upieczeniu.
SKALY = []
GAJ_Z = -84                     # na polnoc od tej linii (z < GAJ_Z) - wisniowy gaj wokol areny
LAS = [((40, 74), 40), ((20, 96), 24)]          # gesty las wokol Lesnej Sciezki (kolo)
ZASOBY_R = 24

ZONES = ["pustka", "brzeg", "laka", "las", "gaj", "polana", "sciezka", "pobocze", "skaly", "zasoby"]
N = len(ZONES)
BLUE = {z: round((i + 0.5) * 256 / N) for i, z in enumerate(ZONES)}


def coast(theta):
    k = 1 + sum(a * math.sin(n * theta + f) for n, a, f in LOBES)
    return k


def d_seg(p, a, b):
    (px, pz), (ax, az), (bx, bz) = p, a, b
    vx, vz = bx - ax, bz - az
    L2 = vx * vx + vz * vz or 1e-9
    t = max(0.0, min(1.0, ((px - ax) * vx + (pz - az) * vz) / L2))
    return math.hypot(px - ax - t * vx, pz - az - t * vz)


def d_line(p, pts):
    return min(d_seg(p, pts[i], pts[i + 1]) for i in range(len(pts) - 1))


SZLAK_W = 25                    # pol szerokosci ladu wzdluz trasy (bez sekcji)
SEKCJA_MARGINES = 20            # lad wokol polany sekcji
def inside(x, z):
    """Wyspa = kretny szlak nad pustka: lad tylko w pasie wokol trasy i wokol sekcji, wiec skrot
    na przelaj konczy sie urwiskiem. Zwraca (na wyspie?, odleglosc od brzegu)."""
    p = (x, z)
    fal = 3.5 * math.sin(x / 11.0 + 0.7) + 2.5 * math.sin(z / 7.0 + 2.1) + 1.5 * math.sin((x + z) / 5.0)
    d = min(d_line(p, TRASA) - SZLAK_W, d_line(p, ODNOGA) - SZLAK_W + 3)
    for s in SEKCJE:
        d = min(d, math.hypot(x - s["xz"][0], z - s["xz"][1]) - s["r"] - SEKCJA_MARGINES)
    d -= fal
    return d < 0, -d


def zone_at(x, z):
    on, dist = inside(x, z)
    if not on:
        return "pustka"
    if dist < RIM:
        return "brzeg"
    p = (x, z)
    dp = min(d_line(p, TRASA), d_line(p, ODNOGA))
    polana = any(math.hypot(x - s["xz"][0], z - s["xz"][1]) <= s["r"] for s in SEKCJE)
    if dp <= SCIEZKA_POL and not (math.hypot(x - S["arena"]["xz"][0], z - S["arena"]["xz"][1]) < S["arena"]["r"] - 2):
        return "sciezka"
    if polana:
        return "polana"
    if any(math.hypot(x - cx, z - cz) <= r for (cx, cz), r in SKALY):
        return "skaly"
    if dp <= SCIEZKA_POL + POBOCZE or any(math.hypot(x - s["xz"][0], z - s["xz"][1]) <= s["r"] + POBOCZE_POLANY for s in SEKCJE):
        return "pobocze"
    if math.hypot(x - S["zasoby"]["xz"][0], z - S["zasoby"]["xz"][1]) <= ZASOBY_R:
        return "zasoby"
    if z < GAJ_Z + 6 * math.sin(x / 13):
        return "gaj"
    if any(math.hypot(x - cx, z - cz) <= r for (cx, cz), r in LAS):
        return "las"
    return "laka"


COL = {"pustka": (18, 18, 28), "brzeg": (120, 110, 90), "laka": (140, 200, 90), "las": (60, 130, 60),
       "gaj": (240, 160, 200), "polana": (200, 230, 150), "sciezka": (170, 130, 80), "pobocze": (175, 215, 120),
       "skaly": (130, 130, 135), "zasoby": (90, 170, 150)}


def draw():
    h = SIZE // 2
    img = Image.new("RGB", (SIZE, SIZE)); px = img.load()
    prev = Image.new("RGB", (SIZE, SIZE)); pp = prev.load()
    count = {z: 0 for z in ZONES}
    for py in range(SIZE):
        for pxx in range(SIZE):
            zn = zone_at(pxx - h, py - h)
            count[zn] += 1
            px[pxx, py] = (0, 0, BLUE[zn])
            pp[pxx, py] = COL[zn]
    # podglad z numerami sekcji
    big = prev.resize((SIZE * 2, SIZE * 2), Image.NEAREST)
    d = ImageDraw.Draw(big)
    for i, s in enumerate(SEKCJE, 1):
        cx, cz = (s["xz"][0] + h) * 2, (s["xz"][1] + h) * 2
        d.ellipse((cx - 9, cz - 9, cx + 9, cz + 9), fill=(30, 30, 30))
        d.text((cx - 3, cz - 6), str(i), fill=(255, 255, 255))
        d.text((cx + 12, cz - 6), s["nazwa"], fill=(20, 20, 20))
    return img, big, count


def build():
    P = f"{OUT_ROOT}/{PID}"
    # budowa przez mutacje dzialajacego packa: generujemy poziom_01 obok i przejmujemy jego biomy/drzewa/snippety
    tmp = f"/tmp/loch001_baza"
    shutil.rmtree(tmp, ignore_errors=True); os.makedirs(tmp)
    MP.OUT_ROOT = tmp; MP.PREVIEW_DIR = tmp   # nie nadpisuj podgladu/specu poziom_01
    MP.build(dict(MP.LEVEL))
    base = f"{tmp}/poziom_01"
    shutil.rmtree(P, ignore_errors=True)
    shutil.copytree(base, P)
    for d in ("biomes", "regions", "dimensions", "images"):
        shutil.rmtree(f"{P}/{d}", ignore_errors=True); os.makedirs(f"{P}/{d}")

    img, prev, count = draw()
    img.save(f"{P}/images/{PID}_mapa.png")
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    prev.save(f"{PREVIEW_DIR}/{PID}_podglad.png")

    B = {z: json.load(open(f"{base}/biomes/{z}.json")) for z in MP.ZONES}
    def lay(pal, lo, hi): return {"palette": [{"block": f"minecraft:{b}"} for b in pal], "minHeight": lo, "maxHeight": hi}
    def gen(lo, hi): return [{"generator": "wyspa_fale", "min": lo, "max": hi}]
    ROT = {"enabled": True, "yAxis": {"enabled": True, "interval": 90, "min": 0, "max": 360}}
    OAK = ["antioch3", "antioch4", "antioch5"]
    def drzewa(sub, names, c, dn): return {"chance": c, "density": dn, "place": [f"trees/{sub}/{n}" for n in names], "rotation": ROT}

    biomy = {
        "pustka": B["pustka"], "brzeg": B["brzeg"], "laka": B["laka"], "gaj": B["gaj"],
        # polany walki: bez wysokich roslin - wysoka trawa zaslaniala pol ekranu na Zakrecie Lucznikow (zrzut l001_4)
        "polana": dict(B["polana"], decorators=[d for d in B["polana"].get("decorators", []) if isinstance(d, dict)
                       and not any(t in p.get("block", "") for p in d.get("palette", [])
                                   for t in ("tall_grass", "large_fern", "rose_bush", "lilac", "peony", "sunflower"))]),
        "pobocze": B["pobocze"],
        # sciezka plaska jak polany (w poziom_01 byla 100-102 i robila garb 2 bl. w poprzek polany - zmierzone)
        "sciezka": dict(B["sciezka"], generators=gen(100, 100)),
        # gesty, ciemniejszy las debowy wokol Lesnej Sciezki
        "las": dict(B["laka"], name="Las", derivative="DARK_FOREST", vanillaDerivative="DARK_FOREST", color="#3c823c",
                    layers=[lay(["grass_block"] * 5 + ["podzol"] * 2 + ["moss_block"], 1, 1), lay(["dirt"], 2, 3),
                            lay(["dirt", "coarse_dirt"], 1, 3), lay(["stone", "andesite", "tuff"], 12, 20)],
                    generators=gen(99, 105),
                    objects=[drzewa("oak", OAK, 0.9, 2), drzewa("birch", OAK, 0.3, 1)]),
        # skaly: oslony i gardziel. Ten sam generator, wyzej -> kopce kamienia (lagodne przez mieszanie ~14 bl.;
        # ostre krawedzie dokladamy FAWE po upieczeniu)
        "skaly": dict(name="Skaly", derivative="WINDSWEPT_HILLS", vanillaDerivative="WINDSWEPT_HILLS", color="#828287",
                      layers=[lay(["stone"] * 4 + ["mossy_cobblestone"] * 2 + ["andesite", "tuff", "moss_block"], 1, 2),
                              lay(["stone", "andesite", "tuff", "cobblestone"], 20, 30)],
                      decorators=[], generators=gen(107, 116)),
        # strefa zasobow: omszala, ziola, brzozy - wezly surowcow (MythicMobs) dokladamy osobno
        "zasoby": dict(B["laka"], name="Zasoby", derivative="OLD_GROWTH_BIRCH_FOREST",
                       vanillaDerivative="OLD_GROWTH_BIRCH_FOREST", color="#5aaa96",
                       layers=[lay(["moss_block"] * 3 + ["grass_block"] * 3 + ["rooted_dirt"], 1, 1), lay(["dirt"], 2, 3),
                               lay(["stone", "andesite", "tuff"], 10, 18)],
                       generators=gen(99, 103),
                       objects=[drzewa("birch", OAK, 0.12, 1)]),
    }
    # KWIATY: wlasciciel po przejsciu poziom_01 (2026-09-26): "najebane od chuja kwiatow". Pelne dekoratory flower-forest
    # z mnoznikiem 1.0 -> szanse dekoratorow z kwiatami x KWIATY, dywan 'wildflowers' usuniety.
    KW = float(os.environ.get("KWIATY", "0.2"))
    KWIAT = ("dandelion", "poppy", "orchid", "allium", "bluet", "tulip", "daisy", "cornflower", "lily_of_the_valley",
             "rose_bush", "lilac", "peony", "sunflower", "pink_petals", "wildflowers")
    def mniej_kwiatow(decs):
        out = []
        for d in decs:
            if isinstance(d, str):
                if "wildflowers" not in d: out.append(d)
                continue
            if any(k in p.get("block", "") for p in d.get("palette", []) for k in KWIAT) and isinstance(d.get("chance"), (int, float)):
                d = dict(d, chance=round(d["chance"] * KW, 4))
            out.append(d)
        return out
    for zn in ("laka", "pobocze", "polana", "las", "zasoby", "sciezka"):
        if isinstance(biomy[zn].get("decorators"), list):
            biomy[zn] = dict(biomy[zn], decorators=mniej_kwiatow(biomy[zn]["decorators"]))
    # polany plaskie na Y0; brzeg/pustka na wysokosci brzegu (urwisko)
    for zn, doc in biomy.items():
        doc = dict(doc); doc["name"] = zn.capitalize()
        json.dump(doc, open(f"{P}/biomes/{zn}.json", "w"), indent=1)
        json.dump({"name": zn.capitalize(), "landBiomes": [zn]}, open(f"{P}/regions/{zn}.json", "w"), indent=1)

    json.dump({"name": "Loch 001 - Kwietna Polana", "fluidHeight": 0, "bedrock": False, "landChance": 1,
               "regions": ZONES, "continentalStyle": {"style": "SIMPLEX"},
               "regionStyle": {"imageMap": {"image": f"{PID}_mapa", "coordinateScale": 1, "interpolationMethod": "NONE",
                                            "channel": "BLUE", "tiled": False, "centered": True}},
               "rockPalette": {"palette": [{"block": "minecraft:air"}]}},
              open(f"{P}/dimensions/{PID}.json", "w"), indent=1)

    DER = {z: json.load(open(f"{P}/biomes/{z}.json"))["derivative"] for z in ZONES}
    spec = {"id": PID, "size": SIZE, "zones": ZONES, "blue": BLUE, "derivative": DER, "y_polan": Y0,
            "sekcje": SEKCJE, "trasa": TRASA, "odnoga": ODNOGA, "skaly": SKALY, "pixels_per_zone": count}
    json.dump(spec, open(f"{PREVIEW_DIR}/{PID}_spec.json", "w"), indent=1)
    shutil.rmtree(tmp, ignore_errors=True)
    return P, spec


if __name__ == "__main__":
    P, spec = build()
    print("pack:", P)
    print("BLUE:", spec["blue"])
    print("piksele:", spec["pixels_per_zone"])
