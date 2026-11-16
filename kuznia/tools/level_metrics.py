#!/usr/bin/env python3
"""Metryki jakosci wyspy z plikow regionow (bez zrzutow): plaskosc spawnu, zasloniecie areny,
zgodnosc stref z rysunkiem, czystosc pustki. Jedna linia wyniku - do logu soak.sh."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import TOOLS  # noqa: E402
import sys, math; sys.path.insert(0, TOOLS)
import mca, make_poziom as M
W = mca.World(sys.argv[2])
L = dict(M.LEVEL); L["spawn"] = (0, int(M.coast_r(math.pi / 2, L) - 22))
DER = {"minecraft:the_void": "pustka", "minecraft:forest": "brzeg", "minecraft:flower_forest": "laka",
       "minecraft:cherry_grove": "gaj", "minecraft:meadow": "polana", "minecraft:plains": "sciezka", "minecraft:birch_forest": "pobocze"}
SOFT = ("leaves", "grass", "petals", "flower", "bush", "allium", "poppy", "dandelion", "orchid", "tulip",
        "daisy", "bluet", "cornflower", "lily", "rose", "lilac", "peony", "fern", "wildflowers", "log", "wood")
def ground(x, z):
    c = mca.column(W, x, z)
    if not c: return None
    for y, b in c:
        if not any(s in b for s in SOFT): return y, b
    return None
sx, sz = L["spawn"]; r = L["spawn_r"] - 1
ys = [g[0] for dx in range(-r, r + 1) for dz in range(-r, r + 1) if dx*dx + dz*dz <= r*r
      for g in [ground(sx + dx, sz + dz)] if g]
ax, az, ar = L["arena"]; fight = 16; cov = tot = 0
for dx in range(-fight, fight + 1, 2):
    for dz in range(-fight, fight + 1, 2):
        if dx*dx + dz*dz > fight*fight: continue
        c = mca.column(W, ax + dx, az + dz); tot += 1
        g = ground(ax + dx, az + dz)
        if c and g and c[0][0] > g[0] + 3: cov += 1
ok = bad = void_solid = 0
for x in range(-120, 121, 8):
    for z in range(-120, 121, 8):
        exp = M.zone_at(x, z, L); c = mca.column(W, x, z)
        if c is None: continue
        if exp == "pustka":
            if any(not any(s in b for s in ("leaves", "log", "wood")) for _, b in c): void_solid += 1
            continue
        g = ground(x, z)
        got = DER.get(mca.biome(W, x, g[0], z)) if g else None
        ok += exp == got; bad += exp != got
if not ys:
    print("BRAK TERENU NA SPAWNIE"); sys.exit(0)
print(f"spawn y={min(ys)}-{max(ys)} | arena zakryta {100*cov/max(tot,1):.0f}% | strefy {100*ok/max(ok+bad,1):.1f}% | lity blok w pustce: {void_solid}")
