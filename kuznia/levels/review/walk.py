#!/usr/bin/env python3
"""Przechodniosc pieszo: mapa wysokosci stania (najwyzszy blok kolizyjny + 1),
BFS od spawnu z krokiem w gore <= 1 (skok w ADVENTURE), w dol dowolnie.
Uzycie: walk.py <region_dir>"""
import sys, math, collections
sys.path.insert(0, '/opt/docker/mc/forge/tools'); import mca, make_poziom as M
W = mca.World(sys.argv[1])
L = dict(M.LEVEL); L["spawn"] = (0, int(M.coast_r(math.pi / 2, L) - 22))
PASS = ("short_grass","tall_grass","fern","large_fern","petals","wildflowers","leaf_litter","bush","dandelion","poppy",
        "orchid","allium","bluet","tulip","daisy","cornflower","lily_of_the_valley","sunflower","lilac","rose_bush","peony",
        "torchflower","pitcher","dead_bush","sweet_berry","vine","carpet","sapling","mushroom","eyeblossom","cactus_flower","snow[")
H = {}; TOP = {}
def h(x, z):
    if (x, z) in H: return H[(x, z)]
    c = mca.column(W, x, z); v = None
    if c:
        for y, b in c:
            if not any(p in b for p in PASS): v = y + 1; TOP[(x,z)] = b; break
    H[(x, z)] = v; return v
sx, sz = L["spawn"]; ax, az, ar = L["arena"]
# 1) linia srodkowa sciezki: z od spawnu do areny
print("== sciezka (os x=9 sin((z+10)/16)), krok miedzy kolejnymi z, max z 3 kolumn szerokosci")
prev = None; steps = []
for z in range(sz, az + ar - 3, -1):
    xp = round(9 * math.sin((z + 10) / 16))
    hs = [h(xp + d, z) for d in (-1, 0, 1)]
    best = min([v for v in hs if v is not None], default=None)
    if prev is not None and best is not None: steps.append((z, best - prev, TOP.get((xp, z))))
    prev = best
up = [s for s in steps if s[1] > 1]; dn = [s for s in steps if s[1] < -1]
print(f"kroki w gore >1: {len(up)} {up[:12]}"); print(f"kroki w dol >1: {len(dn)} {dn[:8]}")
print("max w gore:", max(s[1] for s in steps), " zakres y na sciezce:", min(v for v in [H[k] for k in H] if v), )
# 2) BFS od spawnu
R = 118
seen = {(sx, sz)}; q = collections.deque([(sx, sz)])
while q:
    x, z = q.popleft(); y = h(x, z)
    for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
        n = (x+dx, z+dz)
        if n in seen or abs(n[0]) > R or abs(n[1]) > R: continue
        y2 = h(*n)
        if y2 is None or y is None: continue
        if y2 - y <= 1 and y2 > 90: seen.add(n); q.append(n)
land = [(x, z) for x in range(-R, R+1) for z in range(-R, R+1) if M.zone_at(x, z, L) != "pustka"]
arena_f = [(x, z) for (x, z) in land if (x-ax)**2 + (z-az)**2 <= 16*16]
print(f"== BFS od spawnu: osiagalne {len(seen)} kolumn; wyspa {len(land)}; osiagalne z wyspy {100*sum(1 for p in land if p in seen)/len(land):.1f}%")
print(f"pole walki areny r16 osiagalne: {100*sum(1 for p in arena_f if p in seen)/len(arena_f):.1f}%  srodek areny osiagalny: {(ax,az) in seen}")
for nm, p in {"laka_a": (3, 35), "laka_b": (8, 10), "brama": (3, -4)}.items():
    print(nm, "osiagalne:", p in seen, "h=", h(*p))
# najkrotsza droga (BFS dystans) spawn->arena
