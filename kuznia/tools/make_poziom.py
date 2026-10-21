#!/usr/bin/env python3
"""Generator poziomu wiezy: opis -> obraz sterujacy + pack Irisa.

Zasady wynikajace z pomiarow w kuzni (patrz pamiec iris-imagemap-progi):
  * region[0] = pustka -> wszystko poza obrazem jest pustka za darmo,
  * BRAK `rarity` -> pasma rowne 256/N, piksele w SRODKACH pasm,
  * `rockPalette` = powietrze -> pod warstwami biomu nic nie ma: wyspa wisi w powietrzu,
    a grubosc warstw (rosnaca od brzegu do srodka) daje zwezajacy sie spod,
  * pustka stoi na TEJ SAMEJ wysokosci co brzeg, tylko z warstw powietrza -> brak
    interpolacyjnego zjazdu terenu na krawedzi, zamiast tego czyste urwisko.
Uklad (ksztalt, strefy, sciezka, polany) jest staly = da sie go przetestowac.
Ziarno instancji zmienia tylko rzezbe i to, co Iris stawia losowo (drzewa, kwiaty).
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA, LEVELS  # noqa: E402
import json, math, os, shutil, sys
from PIL import Image

OVER = "/opt/docker/mc/data/world1/plugins/Iris/packs/overworld"
OUT_ROOT = sys.argv[1] if len(sys.argv) > 1 else f"{DATA}/plugins/Iris/packs"
PREVIEW_DIR = LEVELS

# ---------------------------------------------------------------- poziom 1
LEVEL = {
    "id": "poziom_01",
    "name": "Poziom 1 - Kwietna Polana",
    "size": 256,                 # px obrazu = bloki (coordinateScale 1)
    "R": 96,                     # promien wyspy
    "rim": 9,                    # szerokosc strefy brzegu
    "lobes": [(3, 0.12, 0.7), (5, 0.07, 2.1), (9, 0.035, 4.0)],   # (k, amplituda, faza) linii brzegowej
    "arena": (0, -40, 32),       # polana-serce (boss): x, z, r. r=24 -> korony z krawedzi gaju nie sieg. pola walki
    "spawn_r": 12,               # polana startowa; r>=12, bo mieszanie wysokosci siega ~14 blokow
    "path_amp": 9, "path_wave": 16, "path_half": 1.5,
    "pobocze": 4, "pobocze_arena": 6,   # pas bez drzew/wysokich roslin przy sciezce i wokol polan
}
ZONES = ["pustka", "brzeg", "laka", "gaj", "polana", "sciezka", "pobocze"]   # KOLEJNOSC = indeks regionu
N = len(ZONES)
BLUE = {z: round((i + 0.5) * 256 / N) for i, z in enumerate(ZONES)}   # srodki pasm

def coast_r(theta, L):
    return L["R"] * (1 + sum(a * math.sin(k * theta + f) for k, a, f in L["lobes"]))

def path_x(z, L):
    """Os sciezki. Zaczyna sie DOKLADNIE w srodku polany startowej i konczy w osi areny
    (wczesniej przy spawnie byla 8 blokow z boku - z miejsca startu widac bylo 1-4 bloki sciezki)."""
    sx, sz = L["spawn"]; ax, az, ar = L["arena"]
    z0, z1 = sz, az + ar                      # od srodka spawnu do krawedzi areny
    t = min(1.0, max(0.0, (z0 - z) / max(1e-6, z0 - z1)))
    return sx + L["path_amp"] * math.sin(math.pi * t) * math.sin((z + 10) / L["path_wave"] * 1.0 + 0.6)

def zone_at(x, z, L):
    th = math.atan2(z, x)
    rc = coast_r(th, L)
    d = math.hypot(x, z)
    if d >= rc: return "pustka"
    if d >= rc - L["rim"]: return "brzeg"
    ax, az, ar = L["arena"]; sx, sz = L["spawn"]
    da, ds = math.hypot(x - ax, z - az), math.hypot(x - sx, z - sz)
    on_path_z = az + ar - 2 <= z <= sz
    dxp = abs(x - path_x(z, L)) if on_path_z else 1e9
    if dxp <= L["path_half"] and da > ar - 2: return "sciezka"     # sciezka przecina tez polane startowa
    if da <= ar or ds <= L["spawn_r"]: return "polana"
    if dxp <= L["path_half"] + L["pobocze"]: return "pobocze"
    if da <= ar + L["pobocze_arena"] or ds <= L["spawn_r"] + L["pobocze"]: return "pobocze"
    # gaj sakury: polnocna czesc wyspy z falistym brzegiem
    if z < -8 + 10 * math.sin(x / 17): return "gaj"
    return "laka"

def draw(L):
    S = L["size"]; h = S // 2
    # polana startowa: na poludniu, 22 bloki od linii brzegowej
    rs = coast_r(math.pi / 2, L)
    L["spawn"] = (0, int(rs - 22))
    img = Image.new("RGB", (S, S)); px = img.load()
    prev = Image.new("RGB", (S, S)); pp = prev.load()
    COL = {"pustka": (18, 18, 28), "brzeg": (120, 110, 90), "laka": (140, 200, 90),
           "gaj": (240, 160, 200), "polana": (200, 230, 150), "sciezka": (170, 130, 80), "pobocze": (175, 215, 120)}
    count = {z: 0 for z in ZONES}
    for py in range(S):
        for pxx in range(S):
            zn = zone_at(pxx - h, py - h, L)
            count[zn] += 1
            px[pxx, py] = (0, 0, BLUE[zn])
            pp[pxx, py] = COL[zn]
    return img, prev, count

# ---------------------------------------------------------------- pack
def load(p): return json.load(open(p))

def build(L):
    pid = L["id"]; P = f"{OUT_ROOT}/{pid}"
    shutil.rmtree(P, ignore_errors=True)
    for d in ("dimensions", "regions", "biomes", "generators", "images", "objects/trees"):
        os.makedirs(f"{P}/{d}", exist_ok=True)

    img, prev, count = draw(L)
    img.save(f"{P}/images/{pid}_mapa.png")
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    prev.resize((512, 512), Image.NEAREST).save(f"{PREVIEW_DIR}/{pid}_podglad.png")

    # obiekty drzew z overworld (sprawdzone, profesjonalne)
    for sub in ("oak", "birch", "sakura"):
        shutil.copytree(f"{OVER}/objects/trees/{sub}", f"{P}/objects/trees/{sub}") if sub != "sakura" else None
    shutil.copytree(f"{OVER}/snippet", f"{P}/snippet")          # snippety dekoratorow

    # generatory
    # horizontalScale 14 dawal dlugie, rownolegle tarasy jak poziomice (zrzut v1). Mniejsze wygladzanie
    # + drugi, drobny szum o malej sile rozbija linie stopni na nieregularne.
    json.dump({"interpolator": {"function": "BILINEAR_STARCAST_9", "horizontalScale": 7},
               "seed": 7246661,
               "composite": [{"style": {"style": "IRIS_DOUBLE", "zoom": 0.9,
                                        "fracture": {"style": "NOWHERE", "zoom": 0.2, "multiplier": 10}},
                              "seed": 10056, "bezier": True},
                             {"style": {"style": "SIMPLEX", "zoom": 0.09}, "seed": 20411, "opacity": 0.35}]},
              open(f"{P}/generators/wyspa_fale.json", "w"), indent=1)
    json.dump({"interpolator": {"function": "NONE", "horizontalScale": 1}, "seed": 310,
               "composite": [{"seed": 310, "style": {"style": "FLAT"}}]},
              open(f"{P}/generators/plaski.json", "w"), indent=1)

    # dekoratory z overworld
    ff = load(f"{OVER}/biomes/temperate/flower-forest.json")
    cb = load(f"{OVER}/biomes/temperate/cherry-blossom-forest.json")
    # Dekoratory bywaja odwolaniami tekstowymi do snippetow ("snippet/decorator/leaf") -
    # zostawiamy je bez zmian (katalog snippet kopiujemy nizej). Wyrzucamy plyty bruku:
    # na lace wygladaja jak smieci.
    JUNK = ("cobblestone_slab", "_leaves")   # kostki lisci na trawie wygladaja jak ciemne szesciany (zrzut v1)
    def is_junk(d):
        return not isinstance(d, str) and any(j in p.get("block", "") for p in d.get("palette", []) for j in JUNK)
    def scaled(decs, k, keep_snippets=True):
        out = []
        for d in decs:
            if isinstance(d, str):
                if keep_snippets and not any(b in d for b in ("leaf", "bush")): out.append(d)
                continue
            if is_junk(d): continue
            d = json.loads(json.dumps(d))
            if isinstance(d.get("chance"), (int, float)): d["chance"] = round(d["chance"] * k, 4)
            out.append(d)
        return out
    def grass_only(decs):
        return [d for d in decs if not isinstance(d, str) and d.get("palette")
                and all("grass" in p.get("block", "") or "fern" in p.get("block", "") for p in d["palette"])]
    ROT = {"enabled": True, "yAxis": {"enabled": True, "interval": 90, "min": 0, "max": 360}}
    def trees(decl, scale=None, edit=None):
        out = []
        for sub, names, c, dn in decl:
            o = {"chance": c, "density": dn, "place": [f"trees/{sub}/{n}" for n in names], "rotation": ROT}
            if scale: o["scale"] = scale
            if edit: o["edit"] = edit
            out.append(o)
        return out
    # Sakury z overworld maja ~40-53 bloki szerokosci, pnie z bialej brzozy i ZIELONE liscie debu.
    # Zmniejszamy (NONE = najblizszy sasiad, zachowuje typy blokow) i przemalowujemy na wisnie.
    # persistent=true: inaczej liscie opadalyby w instancji (daleko od pnia po skalowaniu).
    # Skalowanie gigantow (40-53 bl.) metoda NONE zjadalo cienkie pnie - zostawaly rozowe kupki lisci
    # na trawie (zrzut v1). Zamiast tego male deby/brzozy z laki (7x13x7) przebarwione na wisnie.
    SAK_EDIT = [
        {"find": [{"block": "minecraft:oak_leaves"}], "chance": 1,
         "replace": {"palette": [{"block": "minecraft:cherry_leaves[persistent=true]"}]}},
        {"find": [{"block": "minecraft:birch_leaves"}], "chance": 1,
         "replace": {"palette": [{"block": "minecraft:cherry_leaves[persistent=true]"}]}},
        # cherry_wood (kora ze wszystkich stron) zamiast *_log: podmiana gubi os pnia, a kora wyglada tak samo z kazdej strony
        {"find": [{"block": "minecraft:oak_log"}, {"block": "minecraft:oak_wood"}, {"block": "minecraft:birch_log"},
                  {"block": "minecraft:birch_wood"}, {"block": "minecraft:stripped_birch_wood"}], "chance": 1,
         "replace": {"palette": [{"block": "minecraft:cherry_wood"}]}},
    ]

    OAK = ["antioch3", "antioch4", "antioch5"]
    BIR = ["antioch3", "antioch4", "antioch5"]
    SAK = ["genericsak1", "genericsak2", "genericsak3", "genericsak4", "genericsak5"]
    def lay(pal, lo, hi): return {"palette": [{"block": f"minecraft:{b}"} for b in pal], "minHeight": lo, "maxHeight": hi}

    BIOMES = {
      "pustka": dict(derivative="THE_VOID", color="#101015",
                     layers=[lay(["air"], 1, 1)],
                     generators=[{"generator": "wyspa_fale", "min": 98, "max": 102}]),
      "brzeg": dict(derivative="FOREST", color="#7a6e5a",
                    layers=[lay(["grass_block"]*7 + ["moss_block"]*2 + ["coarse_dirt"], 1, 1),
                            lay(["dirt"], 1, 2),
                            lay(["stone", "andesite", "tuff", "mossy_cobblestone"], 3, 7)],
                    generators=[{"generator": "wyspa_fale", "min": 98, "max": 102}],
                    decorators=grass_only(ff.get("decorators", [])),
                    objects=trees([("birch", BIR, 0.03, 1)])),
      "laka": dict(derivative="FLOWER_FOREST", color="#8cc85a",
                   layers=[lay(["grass_block"], 1, 1), lay(["dirt"], 2, 3),
                           lay(["dirt", "coarse_dirt"], 1, 3),
                           lay(["stone", "andesite", "tuff"], 10, 18)],
                   generators=[{"generator": "wyspa_fale", "min": 99, "max": 104}],
                   decorators=scaled(ff.get("decorators", []), 1.0),
                   objects=trees([("oak", OAK, 0.14, 1), ("birch", BIR, 0.08, 1)])),
      "gaj": dict(derivative="CHERRY_GROVE", color="#f0a0c8",
                  layers=[lay(["grass_block"], 1, 1), lay(["dirt"], 2, 3),
                          lay(["dirt", "rooted_dirt"], 2, 4),
                          lay(["stone", "andesite", "tuff"], 14, 24)],
                  generators=[{"generator": "wyspa_fale", "min": 100, "max": 106}],
                  decorators=scaled(cb.get("decorators", []), 1.0),
                  objects=trees([("oak", OAK, 0.8, 2), ("birch", BIR, 0.5, 1)], edit=SAK_EDIT)),   # gaj: gesto, wisnie z malych drzew
      "polana": dict(derivative="MEADOW", color="#c8e696",
                     layers=[lay(["grass_block"], 1, 1), lay(["dirt"], 2, 3),
                             lay(["stone", "tuff", "andesite"], 20, 30)],
                     # TEN SAM generator co reszta wyspy, zerowy zakres (min=max) -> szum nic nie wnosi, wysokosc
                     # stala niezaleznie od ziarna. NIE mieszac nazw generatorow miedzy sasiadami: zmierzone,
                     # ze wysokosci sie SUMUJA (plaski 101 + wyspa_fale ~100 = y 202; plaski 50 -> y 150).
                     generators=[{"generator": "wyspa_fale", "min": 100, "max": 100}],
                     decorators=scaled(ff.get("decorators", []), 0.35, keep_snippets=False)),
      "pobocze": dict(derivative="BIRCH_FOREST", color="#afd778",
                      layers=[lay(["grass_block"], 1, 1), lay(["dirt"], 2, 3), lay(["dirt", "coarse_dirt"], 1, 3),
                              lay(["stone", "andesite", "tuff"], 10, 18)],
                      generators=[{"generator": "wyspa_fale", "min": 99, "max": 103}],
                      # tylko niskie: wysoka trawa zaslaniala graczowi pol ekranu (zrzut z kamery gracza)
                      decorators=[d for d in scaled(ff.get("decorators", []), 0.4, keep_snippets=False)
                                  if not any(t in p.get("block", "") for p in d.get("palette", [])
                                             for t in ("tall_grass", "large_fern", "rose_bush", "lilac", "peony", "sunflower"))]),
      "sciezka": dict(derivative="PLAINS", color="#aa8250",
                      layers=[lay(["dirt_path"]*6 + ["coarse_dirt"]*3 + ["gravel"]*2, 1, 1),
                              lay(["dirt"], 2, 3), lay(["stone", "andesite"], 8, 16)],
                      generators=[{"generator": "wyspa_fale", "min": 100, "max": 102}]),
    }
    for zn, b in BIOMES.items():
        doc = {"name": zn.capitalize(), "derivative": b["derivative"], "vanillaDerivative": b["derivative"],
               "color": b["color"], "layers": b["layers"], "decorators": b.get("decorators", []),
               "generators": b["generators"]}
        if b.get("objects"): doc["objects"] = b["objects"]
        json.dump(doc, open(f"{P}/biomes/{zn}.json", "w"), indent=1)
        json.dump({"name": zn.capitalize(), "landBiomes": [zn]}, open(f"{P}/regions/{zn}.json", "w"), indent=1)

    json.dump({"name": L["name"], "fluidHeight": 0, "bedrock": False, "landChance": 1,
               "regions": ZONES,
               "continentalStyle": {"style": "SIMPLEX"},
               "regionStyle": {"imageMap": {"image": f"{pid}_mapa", "coordinateScale": 1,
                                            "interpolationMethod": "NONE", "channel": "BLUE",
                                            "tiled": False, "centered": True}},
               "rockPalette": {"palette": [{"block": "minecraft:air"}]}},
              open(f"{P}/dimensions/{pid}.json", "w"), indent=1)

    spec = {"id": pid, "zones": ZONES, "blue": BLUE, "spawn_xz": L["spawn"],
            "arena_xz": L["arena"][:2], "pixels_per_zone": count}
    json.dump(spec, open(f"{PREVIEW_DIR}/{pid}_spec.json", "w"), indent=1)
    return P, spec

if __name__ == "__main__":
    P, spec = build(LEVEL)
    print("pack:", P)
    print("BLUE (srodki pasm):", spec["blue"])
    print("spawn:", spec["spawn_xz"], " arena:", spec["arena_xz"])
    print("piksele na strefe:", spec["pixels_per_zone"])
