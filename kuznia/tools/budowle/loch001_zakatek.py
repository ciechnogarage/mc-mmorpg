"""Ukryty zakatek za latarnia (Loch 001, fragment 1): polanka wycieta w lesie, otoczona zywoplotem, jedyne wejscie
zatkane sciana korzeni - wypala ja swiatlo Latarni Gaju (skill loch_001_latarnia_1). W srodku Skarb Latarni.
Uzycie: loch001_zakatek.py <swiat>. Wypisuje wspolrzedne sciany korzeni i skarbu (do build_loch001_md.py)."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import KONTENER  # noqa: E402
import json, math, random, subprocess, sys
W = sys.argv[1]; random.seed(7)
def rc(c): return subprocess.run(["docker", "exec", KONTENER, "rcon-cli", f"execute in minecraft:{W} {c}"], capture_output=True, text=True).stdout
def sb(x, y, z, b): rc(f"run setblock {x} {y} {z} minecraft:{b}")
CX, CZ, G = 64, 82, 101           # srodek polanki, grunt (zmierzony)
R = 5                              # promien zywoplotu
WEJ = (60.5, 85.5)                 # strona latarni (57,88)
# 1. wyciecie lasu w kregu R+1 i w przesmyku do latarni
rc(f"run fill {CX-R-1} {G+1} {CZ-R-1} {CX+R+1} {G+16} {CZ+R+1} air replace #minecraft:leaves")
rc(f"run fill {CX-R-1} {G+1} {CZ-R-1} {CX+R+1} {G+16} {CZ+R+1} air replace #minecraft:logs")
rc(f"run fill 56 {G+1} 84 61 {G+16} 89 air replace #minecraft:leaves")
rc(f"run fill 56 {G+1} 84 61 {G+16} 89 air replace #minecraft:logs")
# 2. zywoplot + sciana korzeni w luce od strony latarni
sciana = []
for x in range(CX - R - 1, CX + R + 2):
    for z in range(CZ - R - 1, CZ + R + 2):
        d = math.hypot(x - CX, z - CZ)
        if not (R - 0.5 <= d < R + 0.7): continue
        wej = math.hypot(x - WEJ[0], z - WEJ[1]) < 1.8
        for y in range(G + 1, G + 4):
            if wej:
                sb(x, y, z, random.choice(["mangrove_roots", "mangrove_roots", "muddy_mangrove_roots", "rooted_dirt"])); sciana.append((x, y, z))
            else:
                sb(x, y, z, random.choice(["oak_leaves[persistent=true]"] * 5 + ["flowering_azalea_leaves[persistent=true]", "mangrove_roots"]))
        if not wej: sb(x, G + 4, z, "oak_leaves[persistent=true]")
# 3. wnetrze: mech, swiecace grzyby w ziemi, kapliczka ze swiecami, skarb
for x in range(CX - R + 1, CX + R):
    for z in range(CZ - R + 1, CZ + R):
        if math.hypot(x - CX, z - CZ) < R - 1 and random.random() < 0.45: sb(x, G, z, "moss_block")
for x, z in ((CX - 2, CZ + 2), (CX + 2, CZ - 1), (CX - 1, CZ - 3)): sb(x, G, z, "shroomlight")
for x, z in ((CX + 1, CZ + 2), (CX - 3, CZ), (CX + 3, CZ + 1)): sb(x, G + 1, z, "flowering_azalea")
sb(CX, G + 1, CZ - 2, "mossy_stone_bricks"); sb(CX - 1, G + 1, CZ - 2, "mossy_stone_brick_stairs[facing=east]")
sb(CX + 1, G + 1, CZ - 2, "mossy_stone_brick_stairs[facing=west]"); sb(CX, G + 2, CZ - 2, "mossy_stone_brick_slab")
sb(CX, G + 3, CZ - 2, "candle[candles=3,lit=true]"); sb(CX - 1, G + 2, CZ - 2, "candle[candles=1,lit=true]"); sb(CX + 1, G + 2, CZ - 2, "candle[candles=2,lit=true]")
SKARB = (CX, G + 1, CZ)
sb(*SKARB, "barrel[facing=up,open=false]")
subprocess.run(["docker", "exec", KONTENER, "rcon-cli", "save-all flush"], capture_output=True)
print(json.dumps({"sciana": sciana, "skarb": SKARB}))
