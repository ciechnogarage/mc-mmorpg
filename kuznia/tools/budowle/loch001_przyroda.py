"""Przyroda fragmentu 1 Lochu 001: vanillowe place feature (powalone drzewa, olbrzymie grzyby, omszale glazy, mech,
swietliki, sciolka, krzewy) - gestosc i klimat bez modeli. Uzycie: loch001_przyroda.py <swiat>
Kazdy punkt sprawdzany: strefa (nie pustka/brzeg/sciezka), >=3 bl. od sciezki, >=4 bl. od starc; grunt szukany RCON-em."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS, KONTENER  # noqa: E402
import math, subprocess, sys
sys.path.insert(0, TOOLS)
import make_loch001 as M
W = sys.argv[1]
STARCIA = [(24, 98), (39, 74), (55, 57), (58, 42), (10, 92), (61, 69), (56, 90), (16, 112), (12, 110), (14, 100), (20, 94), (26, 88)]
def rc(cmd): return subprocess.run(["docker", "exec", KONTENER, "rcon-cli", f"execute in minecraft:{W} {cmd}"], capture_output=True, text=True).stdout.replace("\x00", "")
def grunt(x, z):
    rc(f"run forceload add {x} {z}")
    for y in range(112, 94, -1):
        if not any("passed" in rc(f"if block {x} {y} {z} {t}") for t in ("air", "#minecraft:leaves", "#minecraft:replaceable", "#minecraft:flowers")):
            return y
PLAN = {
    "fallen_oak_tree": [(26, 106), (44, 86), (30, 66)],
    "fallen_birch_tree": [(8, 100), (54, 72)],
    "azalea_tree": [(4, 104), (-8, 106)],
    "huge_red_mushroom": [(47, 88), (34, 80)],
    "huge_brown_mushroom": [(20, 80), (48, 58)],
    "forest_rock": [(52, 66), (62, 56), (-4, 110), (20, 112)],
    "moss_patch": [(36, 88), (24, 78)],
    "patch_firefly_bush": [(40, 92), (16, 88), (52, 80)],
    "patch_leaf_litter": [(32, 96), (42, 80), (28, 104)],
    "patch_bush": [(-6, 118), (50, 52), (-2, 112)],
}
ok = 0; zle = []
for f, pts in PLAN.items():
    for x, z in pts:
        zn = M.zone_at(x, z); dp = M.d_line((x, z), M.TRASA); de = min(math.dist((x, z), e) for e in STARCIA)
        if zn in ("pustka", "brzeg", "sciezka") or dp < 3 or de < 4:
            zle.append(f"{f}@{x},{z} ({zn}, sciezka {dp:.0f}, starcie {de:.0f})"); continue
        g = grunt(x, z)
        o = rc(f"run place feature minecraft:{f} {x} {g + 1} {z}")
        if "Placed" in o: ok += 1
        else: zle.append(f"{f}@{x},{z} y={g}: {o.strip()[:40]}")
subprocess.run(["docker", "exec", KONTENER, "rcon-cli", "save-all flush"], capture_output=True)
print(f"postawione: {ok}"); [print("  pominiete:", z) for z in zle]
