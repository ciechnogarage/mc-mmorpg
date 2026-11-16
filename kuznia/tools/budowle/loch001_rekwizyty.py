"""Rekwizyty fragmentu 1 Lochu 001 (sekcje 1-2): woz, pnie, grzyby, glazy, skrytka. Uzycie: loch001_rekwizyty.py <swiat>
Stawiane setblockiem przez RCON kuzni na upieczonym swiecie, przed build_loch001_md.py (regiony ida do szablonu MD).
Kazdy rekwizyt szuka gruntu sam (pomija powietrze, liscie, trawe i kwiaty)."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import KONTENER  # noqa: E402
import subprocess, sys
W = sys.argv[1]
def r(cmd):
    return subprocess.run(["docker", "exec", KONTENER, "rcon-cli", f"execute in minecraft:{W} run {cmd}"],
                          capture_output=True, text=True).stdout.replace("\x00", "")
def test(cmd):
    return "passed" in subprocess.run(["docker", "exec", KONTENER, "rcon-cli", f"execute in minecraft:{W} {cmd}"],
                                      capture_output=True, text=True).stdout
def grunt(x, z):
    r(f"forceload add {x} {z}")
    for y in range(112, 94, -1):
        if not (test(f"if block {x} {y} {z} air") or test(f"if block {x} {y} {z} #minecraft:leaves")
                or test(f"if block {x} {y} {z} #minecraft:replaceable") or test(f"if block {x} {y} {z} #minecraft:flowers")):
            return y
    raise SystemExit(f"brak gruntu w {x},{z}")
def sb(x, y, z, b): r(f"setblock {x} {y} {z} {b}")

def woz(x, z):          # rozbity woz poszukiwaczy: skrzynia wozu, kolo odpadlo, dyszel zlamany, siano i beczka wysypane
    g = grunt(x, z) + 1
    for dx, dz, b in [(0, 0, "spruce_planks"), (1, 0, "spruce_planks"), (2, 0, "spruce_slab"), (0, 1, "spruce_planks"),
                      (1, 1, "spruce_slab"), (2, 1, "spruce_slab[type=top]")]:
        sb(x + dx, g, z + dz, f"minecraft:{b}")
    sb(x, g, z - 1, "minecraft:spruce_trapdoor[facing=north,open=true]")
    sb(x + 2, g, z - 1, "minecraft:spruce_trapdoor[facing=north,open=true]")
    sb(x + 1, g - 0, z + 2, "minecraft:spruce_trapdoor[facing=south,half=bottom]")      # kolo lezy obok
    sb(x + 3, g, z, "minecraft:spruce_fence"); sb(x + 4, g, z, "minecraft:spruce_slab")
    sb(x + 3, g, z + 1, "minecraft:hay_block[axis=x]"); sb(x - 1, g, z + 1, "minecraft:barrel[facing=east]")
    sb(x + 1, g + 1, z, "minecraft:white_carpet")
    return g
def pien(x, z, n=5, os_="x"):
    g = grunt(x, z) + 1
    for i in range(n):
        xx, zz = (x + i, z) if os_ == "x" else (x, z + i)
        sb(xx, g, zz, f"minecraft:oak_log[axis={os_}]")
        if i in (1, 2): sb(xx, g + 1, zz, "minecraft:moss_carpet")
    sb(x + (n if os_ == "x" else 0), g, z + (0 if os_ == "x" else n), "minecraft:brown_mushroom")
    return g
def grzyby(x, z):
    g = grunt(x, z) + 1
    for dx, dz, b in [(0, 0, "red_mushroom"), (1, 1, "brown_mushroom"), (-1, 1, "brown_mushroom"), (1, -1, "red_mushroom"), (0, 2, "brown_mushroom")]:
        if test(f"if block {x+dx} {g} {z+dz} #minecraft:replaceable") or test(f"if block {x+dx} {g} {z+dz} air"):
            sb(x + dx, g, z + dz, f"minecraft:{b}")
    return g
def glaz(x, z):
    g = grunt(x, z) + 1
    for dx in (0, 1):
        for dz in (0, 1):
            sb(x + dx, g, z + dz, "minecraft:mossy_cobblestone")
    sb(x, g + 1, z, "minecraft:cobblestone"); sb(x + 1, g + 1, z + 1, "minecraft:mossy_cobblestone")
    sb(x + 1, g + 1, z, "minecraft:moss_carpet"); sb(x - 1, g, z, "minecraft:cobblestone_slab")
    return g
def skrytka(x, z):      # beczka pod korzeniami, schowana w liściach - znajdzka (otwiera sie funkcja MD)
    g = grunt(x, z) + 1
    sb(x, g, z, "minecraft:barrel[facing=up,open=false]")
    for dx, dz in ((1, 0), (-1, 0), (0, -1), (1, -1), (-1, -1)):
        sb(x + dx, g, z + dz, "minecraft:oak_leaves[persistent=true]")
    sb(x, g + 1, z - 1, "minecraft:oak_leaves[persistent=true]"); sb(x, g + 1, z, "minecraft:hanging_roots") if False else None
    sb(x + 1, g + 1, z, "minecraft:oak_leaves[persistent=true]")
    return g

wyn = {"woz": woz(12, 110), "pien1": pien(30, 90), "pien2": pien(47, 80, 4, "z"), "grzyby": grzyby(50, 68),
       "glaz1": glaz(36, 104), "glaz2": glaz(28, 78), "skrytka": skrytka(10, 92)}
subprocess.run(["docker", "exec", KONTENER, "rcon-cli", "save-all flush"], capture_output=True)
for k, v in wyn.items(): print(f"{k:8} y={v}")
