"""Fragment 1 Lochu 001, runda 'zaleznosci 2': latarnie przy sciezce, swiecace akcenty przy sekretach, Blade Drzewo
(punkt orientacyjny), 3 slupki zagadki wokol Kwiatu Gaju, okruszki poprzednikow (item_display). Uzycie: loch001_okruszki.py <swiat>
Wypisuje JSON z pozycjami dla build_loch001_md.py (-> kuznia/levels/loch001_okruszki.json)."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS, LEVELS, KONTENER  # noqa: E402
import json, math, subprocess, sys
sys.path.insert(0, TOOLS)
import make_loch001 as M
W = sys.argv[1]
def rc(c): return subprocess.run(["docker", "exec", KONTENER, "rcon-cli", f"execute in minecraft:{W} {c}"], capture_output=True, text=True).stdout.replace("\x00", "")
def sb(x, y, z, b): return rc(f"run setblock {x} {y} {z} minecraft:{b}")
def grunt(x, z):
    rc(f"run forceload add {x} {z}")
    for y in range(115, 94, -1):
        if not any("passed" in rc(f"if block {x} {y} {z} {t}") for t in ("air", "#minecraft:leaves", "#minecraft:replaceable", "#minecraft:flowers", "#minecraft:logs")):
            return y
def wolne(x, z, g, h=4):     # czysci kolumne nad gruntem z lisci/pni (w lesie)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            rc(f"run fill {x+dx} {g+1} {z+dz} {x+dx} {g+h} {z+dz} air replace #minecraft:leaves")
    rc(f"run fill {x} {g+1} {z} {x} {g+h} {z} air replace #minecraft:logs")
OUT = {}
# 1. latarnie na slupkach przy sciezce z obozu (odsuniete prostopadle od trasy)
lat = []
for t in (0.25, 0.55, 0.85):
    (ax, az), (bx, bz) = (0, 118), (14, 100)
    px, pz = ax + (bx - ax) * t, az + (bz - az) * t
    L = math.hypot(bx - ax, bz - az); nx, nz = (bz - az) / L, -(bx - ax) / L   # prostopadla
    for k in (3, -3, 4, -4):
        x, z = round(px + nx * k), round(pz + nz * k)
        if M.zone_at(x, z) not in ("sciezka", "pustka", "brzeg") and 2 <= M.d_line((x, z), M.TRASA) <= 5: break
    g = grunt(x, z)
    sb(x, g + 1, z, "spruce_fence"); sb(x, g + 2, z, "spruce_fence"); sb(x, g + 3, z, "lantern")
    lat.append((x, g + 3, z))
OUT["latarnie"] = lat
# 2. swiecace akcenty przy sekretach (podpowiedz wzrokowa): shroomlight w ziemi przy skrytce, swiece przy szczatkach
g = grunt(12, 93); sb(12, g, 93, "shroomlight"); g = grunt(8, 91); sb(8, g, 91, "shroomlight")
for x, z in ((56, 57), (53, 58)):
    g = grunt(x, z); r = rc(f"if block {x} {g+1} {z} air")
    if "passed" in r: sb(x, g + 1, z, "candle[candles=2,lit=true]")
# 3. Blade Drzewo - punkt orientacyjny srodka lasu (widac z obozu i z polany)
blade = None
for x, z in ((40, 90), (36, 96), (44, 94)):
    g = grunt(x, z); wolne(x, z, g, 14)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            rc(f"run fill {x+dx} {g+1} {z+dz} {x+dx} {g+16} {z+dz} air replace #minecraft:leaves")
    if "Placed" in rc(f"run place feature minecraft:pale_oak {x} {g+1} {z}"):
        blade = (x, g + 1, z); break
OUT["blade_drzewo"] = blade
# 4. zagadka: 3 slupki roznej wysokosci wokol Kwiatu Gaju (61,69); na szczycie krysztal (mob MM) - dotknac od najnizszego
fil = []
for (x, z), h in (((58, 71), 1), ((64, 71), 3), ((61, 74), 2)):
    g = grunt(x, z); wolne(x, z, g, 6)
    for i in range(1, h + 1): sb(x, g + i, z, "mossy_stone_bricks" if i < h else "chiseled_stone_bricks")
    fil.append({"x": x, "z": z, "wys": h, "szczyt": g + h})
OUT["slupki"] = fil
# 5. okruszki poprzednikow (item_display, trwale, bez hitboxa): strzaly w ziemi -> porzucony plecak -> list przy skrytce
def disp(x, z, item, rot, skala, tag, dy=0.0):
    g = grunt(x, z)
    rc(f'run summon item_display {x+0.5} {g+1+dy} {z+0.5} {{item:{{id:"minecraft:{item}",count:1}},Tags:["l001_okruszek","{tag}"],'
       f'transformation:{{left_rotation:{rot},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[{skala}f,{skala}f,{skala}f]}}}}')
    return (x, g + 1, z)
PLASKO = "[0.7071f,0f,0f,0.7071f]"; SKOS = "[0f,0f,0.3827f,0.9239f]"
rc('run kill @e[tag=l001_okruszek]')
OUT["okruszki"] = {
    "strzaly": [disp(16, 98, "arrow", SKOS, 1.0, "strzala", 0.3), disp(17, 99, "arrow", "[0.2706f,0.2706f,0.6533f,0.6533f]", 1.0, "strzala", 0.3),
                disp(15, 96, "arrow", SKOS, 1.0, "strzala", 0.3)],
    "plecak": disp(13, 95, "bundle", PLASKO, 0.9, "plecak", 0.05),
    "list": disp(11, 94, "paper", PLASKO, 0.7, "list", 0.02),
    "tarcza": disp(57, 66, "shield", "[0.5f,0.5f,0.5f,0.5f]", 1.0, "tarcza", 0.1),
}
subprocess.run(["docker", "exec", KONTENER, "rcon-cli", "save-all flush"], capture_output=True)
json.dump(OUT, open(f"{LEVELS}/loch001_okruszki.json", "w"), indent=1)
print(json.dumps(OUT))
