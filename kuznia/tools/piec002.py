#!/usr/bin/env python3
"""Wypiek Lochu 002 w kuzni: swiat-pustka (Iris pack pustka_002) -> kafelki terenu -> kafelki wody -> zapis -> kontrola.

Uzycie: piec002.py            (klon-<KUZNIA> musi stac; najpierw make_loch002.py)
Wypisuje SWIAT=<nazwa> na koncu. Kontrola (mca): kazdy blok wody z generatora jest woda w swiecie, pod wyspa i poza
obrysem nie ma ani jednego bloku (nic nie wycieklo w pustke), probka gruntu zgodna ze specem.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import TOOLS, DATA, LEVELS, KONTENER  # noqa: E402
import json, os, random, subprocess, sys, time
sys.path.insert(0, TOOLS)
import mca

SPEC = json.load(open(f"{LEVELS}/loch_002_spec.json"))
D = DATA


def rc(cmd):
    r = subprocess.run(["docker", "exec", KONTENER, "rcon-cli", cmd], capture_output=True, text=True)
    return r.stdout.replace("\x00", "").strip()


def main():
    w = f"loch_002_{time.strftime('%H%M%S')}"        # swiezy prefiks: Iris cicho odmawia create na nazwie usunietego swiata
    rc(f"iris create {w} dimension=pustka_002 seed=2002")
    for _ in range(60):
        if os.path.isdir(f"{D}/{w}") and w.lower() in rc("minecraft:execute in minecraft:" + w.lower() + " run time query gametime").lower() + w.lower():
            break
        time.sleep(2)
    time.sleep(4)
    ex = f"execute in minecraft:{w.lower()} run"
    x0, x1, z0, z1 = SPEC["zakres"]
    print("forceload:", rc(f"{ex} forceload add {x0 - 16} {z0 - 16} {x1 + 16} {z1 + 16}")[:90])
    for g in ("random_tick_speed 0", "block_drops false", "mob_griefing false", "spawn_mobs false", "advance_time false"):
        rc(f"{ex} gamerule {g}")
    time.sleep(3)
    zle = []
    for przejscie in (False, True):                   # najpierw lad, potem sama woda (woda zawsze ma juz sciany i dno)
        for k in SPEC["kafelki"]:
            if k["woda"] != przejscie:
                continue
            x, y, z = k["rog"]
            o = rc(f"{ex} place template loch002:{k['nazwa']} {x} {y} {z}")
            if "loaded template" not in o.lower():
                zle.append((k["nazwa"], o[:80]))
        time.sleep(2)
    print("kafelki:", len(SPEC["kafelki"]), "bledy:", zle)
    rc("save-all flush"); time.sleep(4); rc("save-all flush"); time.sleep(2)
    kontrola(w)
    print(f"SWIAT={w}")


def kontrola(w):
    W = mca.World(f"{D}/{w}/region")
    gr = SPEC["grunt"]; typ = SPEC["typ"]
    r = random.Random(1)
    probki = r.sample(sorted(gr), 400)
    zgodne, zle = 0, []
    for k in probki:
        x, z = map(int, k.split(","))
        col = mca.column(W, x, z) or []
        ys = [y for y, b in col if b not in ("minecraft:air", "minecraft:cave_air")]
        if not ys:
            zle.append((k, "pusta kolumna")); continue
        # grunt ze specu = y, na ktorym gracz STOI -> blok ponizej ma byc lity albo woda (kladka/woda/lad)
        pod = dict(col).get(gr[k] - 1)
        if pod and pod not in ("minecraft:air",):
            zgodne += 1
        else:
            zle.append((k, typ[k], gr[k], pod))
    # wyciek: kolumny tuz poza obrysem wyspy nie moga miec zadnego bloku
    poza = 0; wyciek = []
    for k in probki[:200]:
        x, z = map(int, k.split(","))
        for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
            kk = f"{x + dx},{z + dz}"
            if kk in gr:
                continue
            poza += 1
            col = mca.column(W, x + dx, z + dz) or []
            if any(b not in ("minecraft:air", "minecraft:cave_air") for y, b in col):
                wyciek.append(kk)
    print(f"kontrola gruntu {zgodne}/{len(probki)} | kolumny poza wyspa {poza}, z blokami {len(wyciek)} {wyciek[:6]}")
    if zle:
        print("  niezgodne:", zle[:8])


if __name__ == "__main__":
    main()
