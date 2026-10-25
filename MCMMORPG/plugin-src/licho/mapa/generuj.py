#!/usr/bin/env python3
"""Mapa „Zagroda Zielarki” dla minigry Licho. Zapisuje src/main/resources/mapa.txt: `x y z blockdata` (bez powietrza).

Układ (Y=63 ziemia, gracz stoi na 64, sufit z barier na 69 — dachy poza zasięgiem):
  chata zielarki  x -20..-5,  z -18..-6   (izba z piecem, półki z ziołami, kąt z beczkami)
  stodoła         x   4..20,  z -18..-4   (siano, wóz, żłób, beczki)
  wiata-suszarnia x -20..-8,  z   5..15   (pęki ziół, kotły, kosze)
  ogród i ule     x   4..21,  z   5..19   (grządki, dynie, ule, kompostowniki)
  studnia + lipa  środek podwórza
  komora          y 40..44 pod mapą: tu Zamawiacze czekają oślepieni na koniec rozproszenia
Uruchom: python3 mapa/generuj.py  (z katalogu pluginu)
"""
import os
import random

R = random.Random(7)
B = {}
G = 63  # poziom ziemi
SUF = 69  # bariera sufitu
X0, X1, Z0, Z1 = -24, 24, -22, 22  # płot


def put(x, y, z, b):
    B[(x, y, z)] = b


def fill(x1, y1, z1, x2, y2, z2, b):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                put(x, y, z, b)


def clear(x1, y1, z1, x2, y2, z2):
    fill(x1, y1, z1, x2, y2, z2, None)


# ------------------------------------------------------------------ teren
for x in range(X0 - 6, X1 + 7):
    for z in range(Z0 - 6, Z1 + 7):
        fill(x, G - 3, z, x, G - 1, z, "minecraft:dirt")
        r = R.random()
        top = "minecraft:grass_block"
        if r < 0.06:
            top = "minecraft:coarse_dirt"
        elif r < 0.09:
            top = "minecraft:moss_block"
        put(x, G, z, top)
        # trawa i kwiaty poza budynkami (nadpisywane niżej)
        r2 = R.random()
        if r2 < 0.22:
            put(x, G + 1, z, "minecraft:short_grass")
        elif r2 < 0.25:
            put(x, G + 1, z, R.choice(["minecraft:poppy", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:dandelion"]))

# ścieżki z ubitej ziemi: od bramy (południe, z=22) do studni i do każdego budynku
def path(x1, z1, x2, z2, w=1):
    for x in range(min(x1, x2) - w, max(x1, x2) + w + 1):
        for z in range(min(z1, z2) - w, max(z1, z2) + w + 1):
            if R.random() < 0.9:
                put(x, G, z, "minecraft:dirt_path")
            else:
                put(x, G, z, "minecraft:coarse_dirt")
            put(x, G + 1, z, None)


path(0, 22, 0, -2)           # brama -> studnia
path(-12, -1, 12, -1)        # podwórze wschód-zachód
path(-12, -5, -12, -1, 0)    # do chaty
path(12, -3, 12, -1, 1)      # do stodoły
path(-14, 2, -14, 5, 0)      # do wiaty
path(12, 2, 12, 5, 0)        # do ogrodu

# ------------------------------------------------------------------ płot wiklinowy + bariery obwodu
for x in range(X0, X1 + 1):
    for z in (Z0, Z1):
        put(x, G + 1, z, "minecraft:spruce_fence")
for z in range(Z0, Z1 + 1):
    for x in (X0, X1):
        put(x, G + 1, z, "minecraft:spruce_fence")
for x in (X0, X1):
    for z in (Z0, Z1):
        put(x, G + 1, z, "minecraft:stripped_spruce_log")
        put(x, G + 2, z, "minecraft:lantern")
# brama (południe) — wrota z płotkami, zamknięte
put(-1, G + 1, Z1, "minecraft:spruce_fence_gate[facing=south]")
put(0, G + 1, Z1, "minecraft:spruce_fence_gate[facing=south]")
put(1, G + 1, Z1, "minecraft:spruce_fence_gate[facing=south]")
for x in (-2, 2):
    put(x, G + 1, Z1, "minecraft:stripped_spruce_log")
    put(x, G + 2, Z1, "minecraft:stripped_spruce_log")
    put(x, G + 3, Z1, "minecraft:lantern")
# bariera wzdłuż płotu (nie przeskoczysz) i sufit
for x in range(X0, X1 + 1):
    for z in (Z0, Z1):
        fill(x, G + 2, z, x, SUF, z, "minecraft:barrier") if B.get((x, G + 2, z)) is None else None
for z in range(Z0, Z1 + 1):
    for x in (X0, X1):
        fill(x, G + 2, z, x, SUF, z, "minecraft:barrier") if B.get((x, G + 2, z)) is None else None
for x in (-2, 2):
    put(x, G + 3, Z1, "minecraft:lantern")
    fill(x, G + 4, Z1, x, SUF, Z1, "minecraft:barrier")
fill(X0, SUF, Z0, X1, SUF, Z1, "minecraft:barrier")


# ------------------------------------------------------------------ budynki: pomocnicze
def zrebowa_sciana_x(x1, x2, z, y1, y2):
    """Ściana z bali (spruce_log leżący wzdłuż X), narożniki to słupy."""
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            put(x, y, z, "minecraft:spruce_log[axis=x]")


def zrebowa_sciana_z(z1, z2, x, y1, y2):
    for y in range(y1, y2 + 1):
        for z in range(z1, z2 + 1):
            put(x, y, z, "minecraft:spruce_log[axis=z]")


def slup(x, z, y1, y2, b="minecraft:stripped_dark_oak_log"):
    fill(x, y1, z, x, y2, z, b)


def dach_dwuspadowy_x(x1, x2, z1, z2, y0, mat="dark_oak"):
    """Dach o kalenicy wzdłuż X; spadki na północ/południe, okap 1 blok."""
    zs, zn = z2 + 1, z1 - 1
    y = y0
    while zn <= zs:
        for x in range(x1 - 1, x2 + 2):
            if zn == zs:
                put(x, y, zn, f"minecraft:{mat}_slab[type=bottom]")
            else:
                put(x, y, zn, f"minecraft:{mat}_stairs[facing=south,half=bottom]")
                put(x, y, zs, f"minecraft:{mat}_stairs[facing=north,half=bottom]")
        # szczyty (trójkąt ściany) z desek
        for z in range(zn + 1, zs):
            for x in (x1, x2):
                put(x, y, z, "minecraft:spruce_planks")
        zn += 1
        zs -= 1
        y += 1


def okno(x, y, z):
    put(x, y, z, "minecraft:spruce_trapdoor[half=top,open=false]") if False else put(x, y, z, "minecraft:glass_pane")


def drzwi(x, z, facing):
    put(x, G + 1, z, f"minecraft:spruce_door[facing={facing},half=lower,hinge=left,open=true]")
    put(x, G + 2, z, f"minecraft:spruce_door[facing={facing},half=upper,hinge=left,open=true]")


# ------------------------------------------------------------------ CHATA ZIELARKI
cx1, cx2, cz1, cz2 = -20, -5, -18, -6
clear(cx1, G + 1, cz1, cx2, SUF - 1, cz2)
fill(cx1, G - 1, cz1, cx2, G - 1, cz2, "minecraft:cobblestone")
fill(cx1, G, cz1, cx2, G, cz2, "minecraft:cobblestone")  # podmurówka
fill(cx1 + 1, G, cz1 + 1, cx2 - 1, G, cz2 - 1, "minecraft:spruce_planks")  # podłoga
zrebowa_sciana_x(cx1, cx2, cz1, G + 1, G + 4)
zrebowa_sciana_x(cx1, cx2, cz2, G + 1, G + 4)
zrebowa_sciana_z(cz1, cz2, cx1, G + 1, G + 4)
zrebowa_sciana_z(cz1, cz2, cx2, G + 1, G + 4)
for x, z in ((cx1, cz1), (cx2, cz1), (cx1, cz2), (cx2, cz2)):
    slup(x, z, G + 1, G + 4)
# ścianka działowa: izba (zachód) | komora z zapasami (wschód), przejście
for z in range(cz1 + 1, cz2):
    fill(-11, G + 1, z, -11, G + 4, z, "minecraft:spruce_log[axis=z]")
fill(-11, G + 1, -12, -11, G + 2, -11, None)
# strop izby: belki
for x in range(cx1 + 1, cx2):
    if x % 3 == 0:
        fill(x, G + 4, cz1 + 1, x, G + 4, cz2 - 1, "minecraft:stripped_spruce_log[axis=z]")
dach_dwuspadowy_x(cx1, cx2, cz1, cz2, G + 5)
# okna z okiennicami
for x in (-17, -14, -8):
    put(x, G + 2, cz2, "minecraft:glass_pane")
    put(x, G + 2, cz2 + 1, "minecraft:spruce_trapdoor[facing=south,open=true,half=bottom]") if False else None
for x in (-17, -8):
    put(x, G + 2, cz1, "minecraft:glass_pane")
put(cx1, G + 2, -12, "minecraft:glass_pane")
drzwi(-12, cz2, "south")   # wejście od podwórza
put(-12, G, cz2 + 1, "minecraft:spruce_stairs[facing=north]") if False else None
# przed chatą: ławka i donice
put(-15, G + 1, cz2 + 1, "minecraft:spruce_stairs[facing=north]")
put(-16, G + 1, cz2 + 1, "minecraft:spruce_stairs[facing=north]")
put(-9, G + 1, cz2 + 1, "minecraft:potted_azalea_bush")
put(-7, G + 1, cz2 + 1, "minecraft:barrel[facing=up]")

# izba (x -19..-12): piec z kominem, stół, ława, półki
fill(-19, G + 1, -17, -18, G + 3, -16, "minecraft:bricks")       # piec w rogu
put(-18, G + 1, -15, "minecraft:smoker[facing=south]")
put(-19, G + 1, -15, "minecraft:bricks")
put(-18, G + 2, -15, "minecraft:brick_stairs[facing=south,half=top]")
fill(-19, G + 4, -17, -18, G + 9, -17, "minecraft:bricks")       # komin przez dach
put(-19, G + 10, -17, "minecraft:campfire[lit=true,signal_fire=false]")
put(-18, G + 10, -17, "minecraft:brick_slab")
put(-17, G + 1, -17, "minecraft:cauldron")                        # kocioł przy piecu
put(-16, G + 1, -17, "minecraft:barrel[facing=up]")
# stół na środku izby
for x in (-16, -15):
    put(x, G + 1, -12, "minecraft:spruce_fence")
    put(x, G + 2, -12, "minecraft:spruce_pressure_plate")
put(-16, G + 1, -12, "minecraft:spruce_fence")
fill(-16, G + 1, -12, -15, G + 1, -12, "minecraft:spruce_slab[type=top]")
put(-16, G + 2, -12, "minecraft:brewing_stand")
put(-15, G + 2, -12, "minecraft:potted_fern")
put(-17, G + 1, -12, "minecraft:spruce_stairs[facing=east]")
put(-14, G + 1, -12, "minecraft:spruce_stairs[facing=west]")
# ława pod oknem i skrzynia-beczka
for x in (-18, -17, -16):
    put(x, G + 1, -7, "minecraft:spruce_stairs[facing=north]")
put(-19, G + 1, -7, "minecraft:barrel[facing=north]")
put(-19, G + 1, -8, "minecraft:composter[level=3]")
# półki z ziołami na zachodniej ścianie (pułapki na trapdoorach) + doniczki
for z in (-14, -13, -11, -10):
    put(-19, G + 3, z, "minecraft:spruce_trapdoor[facing=east,half=top,open=false]")
    put(-19, G + 3 + 1, z, R.choice(["minecraft:potted_fern", "minecraft:potted_dead_bush", "minecraft:potted_brown_mushroom", "minecraft:potted_red_mushroom"]))
put(-19, G + 1, -10, "minecraft:loom[facing=east]")
put(-19, G + 1, -11, "minecraft:chiseled_bookshelf[facing=east]")
# pęki ziół pod stropem izby
for x, z in ((-17, -10), (-14, -15), (-13, -9), (-16, -14)):
    put(x, G + 3, z, "minecraft:hanging_roots")
# lampy na łańcuchu
put(-15, G + 3, -10, "minecraft:lantern[hanging=true]")
put(-15, G + 3, -15, "minecraft:lantern[hanging=true]")

# komora z zapasami (x -10..-6): beczki, worki (siano), kosze
for x, z, b in ((-10, -17, "minecraft:barrel[facing=up]"), (-9, -17, "minecraft:barrel[facing=up]"), (-10, -16, "minecraft:barrel[facing=east]"),
                (-6, -17, "minecraft:hay_block[axis=y]"), (-7, -17, "minecraft:hay_block[axis=x]"), (-6, -16, "minecraft:hay_block[axis=y]"),
                (-6, -10, "minecraft:composter[level=5]"), (-6, -9, "minecraft:composter[level=2]"), (-10, -8, "minecraft:cauldron"),
                (-8, -17, "minecraft:carved_pumpkin[facing=south]"), (-6, -13, "minecraft:barrel[facing=west]"), (-6, -12, "minecraft:barrel[facing=up]"),
                (-10, -10, "minecraft:pumpkin")):
    put(x, G + 1, z, b)
put(-6, G + 2, -17, "minecraft:hay_block[axis=y]")
put(-10, G + 2, -17, "minecraft:barrel[facing=up]")
put(-8, G + 3, -12, "minecraft:lantern[hanging=true]")

# ------------------------------------------------------------------ STODOŁA
sx1, sx2, sz1, sz2 = 4, 20, -18, -4
clear(sx1, G + 1, sz1, sx2, SUF - 1, sz2)
fill(sx1, G, sz1, sx2, G, sz2, "minecraft:coarse_dirt")
for x in range(sx1 + 1, sx2):
    for z in range(sz1 + 1, sz2):
        if R.random() < 0.35:
            put(x, G, z, "minecraft:packed_mud")
# słupy i ściany z pionowych desek
for y in range(G + 1, G + 6):
    for x in range(sx1, sx2 + 1):
        put(x, y, sz1, "minecraft:stripped_spruce_log[axis=y]" if x % 4 == 0 else "minecraft:spruce_planks")
    for z in range(sz1, sz2 + 1):
        put(sx1, y, z, "minecraft:stripped_spruce_log[axis=y]" if z % 4 == 2 else "minecraft:spruce_planks")
        put(sx2, y, z, "minecraft:stripped_spruce_log[axis=y]" if z % 4 == 2 else "minecraft:spruce_planks")
    for x in range(sx1, sx2 + 1):
        put(x, y, sz2, "minecraft:stripped_spruce_log[axis=y]" if x % 4 == 0 else "minecraft:spruce_planks")
# szerokie wrota od podwórza (otwarte), małe drzwi od tyłu
fill(10, G + 1, sz2, 14, G + 4, sz2, None)
fill(10, G + 5, sz2, 14, G + 5, sz2, "minecraft:stripped_spruce_log[axis=x]")
fill(18, G + 1, sz1, 18, G + 2, sz1, None)
# szczeliny między deskami (światło)
for x in (6, 9, 15, 18):
    put(x, G + 4, sz2, "minecraft:spruce_fence")
dach_dwuspadowy_x(sx1, sx2, sz1, sz2, G + 6, "spruce")
# wnętrze: stogi siana, wóz, żłób, beczki
for x, z, h in ((5, -17, 3), (6, -17, 3), (7, -17, 2), (5, -16, 2), (6, -16, 2), (5, -15, 1),
                (19, -17, 2), (19, -16, 2), (18, -16, 1), (19, -15, 1)):
    for y in range(h):
        put(x, G + 1 + y, z, "minecraft:hay_block[axis=" + R.choice("xyz") + "]")
put(8, G + 1, -17, "minecraft:hay_block[axis=x]")
# wóz drabiniasty (koła = grindstone, skrzynia z desek)
fill(11, G + 2, -14, 15, G + 2, -12, "minecraft:spruce_slab[type=bottom]")
for x in (11, 15):
    for z in (-14, -12):
        put(x, G + 3, z, "minecraft:spruce_fence")
put(12, G + 1, -15, "minecraft:grindstone[face=floor,facing=east]")
put(14, G + 1, -15, "minecraft:grindstone[face=floor,facing=east]")
put(12, G + 1, -11, "minecraft:grindstone[face=floor,facing=east]")
put(14, G + 1, -11, "minecraft:grindstone[face=floor,facing=east]")
put(13, G + 3, -13, "minecraft:hay_block[axis=x]")
put(10, G + 1, -13, "minecraft:spruce_fence")  # dyszel
# żłób z kotłów i beczki pod ścianą wschodnią
for z in (-10, -9, -8):
    put(19, G + 1, z, "minecraft:cauldron")
for z, b in ((-7, "minecraft:barrel[facing=up]"), (-6, "minecraft:barrel[facing=west]"), (-5, "minecraft:barrel[facing=up]")):
    put(19, G + 1, z, b)
put(19, G + 2, -5, "minecraft:barrel[facing=up]")
for z in (-10, -6):
    put(5, G + 1, z, "minecraft:composter[level=7]")
put(5, G + 1, -8, "minecraft:pumpkin")
put(5, G + 1, -7, "minecraft:carved_pumpkin[facing=east]")
put(6, G + 1, -5, "minecraft:stripped_oak_log[axis=y]")  # pieniek do rąbania
put(8, G + 1, -5, "minecraft:anvil[facing=north]")
put(12, G + 4, -9, "minecraft:lantern[hanging=true]")
put(7, G + 4, -12, "minecraft:lantern[hanging=true]")
put(17, G + 4, -12, "minecraft:lantern[hanging=true]")

# ------------------------------------------------------------------ WIATA-SUSZARNIA
wx1, wx2, wz1, wz2 = -20, -8, 5, 15
clear(wx1, G + 1, wz1, wx2, SUF - 1, wz2)
fill(wx1, G, wz1, wx2, G, wz2, "minecraft:packed_mud")
for x in (wx1, -14, wx2):
    for z in (wz1, 10, wz2):
        slup(x, z, G + 1, G + 4)
dach_dwuspadowy_x(wx1, wx2, wz1, wz2, G + 5, "spruce")
# drążki z pękami ziół
for z in (7, 10, 13):
    fill(wx1 + 1, G + 4, z, wx2 - 1, G + 4, z, "minecraft:stripped_spruce_log[axis=x]")
    for x in range(wx1 + 1, wx2):
        if R.random() < 0.55:
            put(x, G + 3, z, R.choice(["minecraft:hanging_roots", "minecraft:hanging_roots", "minecraft:cave_vines[berries=false]"]))
# stanowiska: kotły do warzenia, kosze, beczki, stół
for x, z, b in ((-19, 6, "minecraft:cauldron"), (-18, 6, "minecraft:campfire[lit=false]"), (-19, 8, "minecraft:barrel[facing=up]"),
                (-19, 9, "minecraft:composter[level=4]"), (-19, 12, "minecraft:barrel[facing=east]"), (-19, 14, "minecraft:hay_block[axis=y]"),
                (-16, 14, "minecraft:composter[level=6]"), (-12, 14, "minecraft:barrel[facing=up]"), (-11, 14, "minecraft:barrel[facing=up]"),
                (-9, 14, "minecraft:cauldron"), (-9, 12, "minecraft:pumpkin"), (-9, 6, "minecraft:stripped_oak_log[axis=y]"),
                (-12, 6, "minecraft:composter[level=1]"), (-16, 9, "minecraft:smoker[facing=east]")):
    put(x, G + 1, z, b)
for x in (-16, -15, -14):
    put(x, G + 1, 11, "minecraft:spruce_slab[type=top]")
put(-15, G + 2, 11, "minecraft:brewing_stand")
put(-14, G + 2, 11, "minecraft:potted_dead_bush")
put(-14, G + 3, 8, "minecraft:lantern[hanging=true]")

# ------------------------------------------------------------------ OGRÓD + ULE
gx1, gx2, gz1, gz2 = 4, 21, 5, 19
# płotek ogrodu z furtką
for x in range(gx1, gx2 + 1):
    for z in (gz1, gz2):
        put(x, G + 1, z, "minecraft:oak_fence")
for z in range(gz1, gz2 + 1):
    for x in (gx1, gx2):
        put(x, G + 1, z, "minecraft:oak_fence")
put(12, G + 1, gz1, "minecraft:oak_fence_gate[facing=north,open=true]")
put(11, G + 1, gz1, None)
put(13, G + 1, gz1, None)
# grządki (ziemia uprawna + rośliny) z miedzami
uprawy = ["minecraft:wheat[age=7]", "minecraft:carrots[age=7]", "minecraft:beetroots[age=3]", "minecraft:potatoes[age=7]"]
for i, z in enumerate(range(7, 18, 3)):
    for x in range(6, 12):
        put(x, G, z, "minecraft:farmland[moisture=7]")
        put(x, G, z + 1, "minecraft:farmland[moisture=7]")
        put(x, G + 1, z, uprawy[i % 4])
        put(x, G + 1, z + 1, uprawy[i % 4])
    put(5, G, z, "minecraft:water") if i == 1 else None
# dynie na grządce przy płocie
for x, z in ((14, 7), (16, 8), (15, 10), (18, 7)):
    put(x, G + 1, z, "minecraft:pumpkin")
for x, z in ((14, 8), (16, 7), (17, 10)):
    put(x, G + 1, z, "minecraft:pumpkin_stem[age=7]") if False else put(x, G, z, "minecraft:farmland[moisture=5]")
# pasieka: ule na pieńkach, rząd przy płocie
for x in (14, 16, 18, 20):
    put(x, G + 1, 17, "minecraft:stripped_oak_log[axis=y]")
    put(x, G + 2, 17, "minecraft:beehive[facing=south]")
put(20, G + 1, 14, "minecraft:composter[level=8]")
put(20, G + 1, 13, "minecraft:composter[level=4]")
put(14, G + 1, 13, "minecraft:barrel[facing=up]")
put(17, G + 1, 13, "minecraft:cauldron")
# strach na wróble
put(9, G + 1, 12, "minecraft:spruce_fence")
put(9, G + 2, 12, "minecraft:hay_block[axis=y]")
put(9, G + 3, 12, "minecraft:carved_pumpkin[facing=south]")

# ------------------------------------------------------------------ STUDNIA, LIPA, DREWUTNIA, WOZY NA PODWÓRZU
# studnia (kamień + daszek)
for x in range(-2, 3):
    for z in range(-1, 4):
        if x in (-2, 2) or z in (-1, 3):
            put(x, G + 1, z, "minecraft:cobblestone_wall" if (x, z) not in ((-2, -1), (2, -1), (-2, 3), (2, 3)) else "minecraft:mossy_cobblestone")
        else:
            put(x, G, z, "minecraft:water")
            put(x, G + 1, z, None)
for x, z in ((-2, -1), (2, -1), (-2, 3), (2, 3)):
    put(x, G + 2, z, "minecraft:spruce_fence")
    put(x, G + 3, z, "minecraft:spruce_fence")
fill(-2, G + 4, -1, 2, G + 4, 3, "minecraft:spruce_slab[type=bottom]")
fill(-1, G + 4, 0, 1, G + 4, 2, "minecraft:spruce_planks")
put(0, G + 3, 1, "minecraft:iron_chain[axis=y]")
put(0, G + 2, 1, "minecraft:lantern[hanging=true]")
put(3, G + 1, 1, "minecraft:cauldron")  # wiadro-koryto obok studni

# lipa (drzewo) w rogu podwórza
def drzewo(x, z, h=6):
    fill(x, G + 1, z, x, G + h, z, "minecraft:oak_log")
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(h - 2, h + 2):
                if abs(dx) + abs(dz) + max(0, dy - h) * 2 <= 4 and (dx, dz) != (0, 0) and R.random() < 0.88:
                    if G + 1 + dy < SUF:
                        put(x + dx, G + 1 + dy, z + dz, "minecraft:oak_leaves[persistent=true]")
    put(x, G + h + 1, z, "minecraft:oak_leaves[persistent=true]")


drzewo(-19, 19)
drzewo(21, -21 + 3) if False else None

# drewutnia: sągi drewna pod daszkiem przy chacie
for x in range(-4, -1):
    for y in range(G + 1, G + 3):
        put(x, y, -17, "minecraft:oak_log[axis=z]")
        put(x, y, -16, "minecraft:oak_log[axis=z]")
fill(-4, G + 3, -17, -2, G + 3, -16, "minecraft:spruce_slab[type=bottom]")
put(-3, G + 1, -14, "minecraft:stripped_oak_log[axis=y]")  # pniak
put(-1, G + 1, -14, "minecraft:oak_log[axis=x]")
# podwórze: rozrzucone sprzęty (tu Licho ma gdzie się wtopić)
for x, z, b in ((-6, 1, "minecraft:barrel[facing=up]"), (-5, 1, "minecraft:barrel[facing=up]"), (-6, 2, "minecraft:hay_block[axis=y]"),
                (5, -1, "minecraft:composter[level=3]"), (7, 2, "minecraft:hay_block[axis=x]"), (-9, 3, "minecraft:stripped_oak_log[axis=y]"),
                (-20, 1, "minecraft:barrel[facing=north]"), (-21, -2, "minecraft:hay_block[axis=y]"), (-21, -1, "minecraft:hay_block[axis=y]"),
                (22, 1, "minecraft:pumpkin"), (22, -1, "minecraft:barrel[facing=up]"), (-6, 19, "minecraft:hay_block[axis=z]"),
                (-7, 19, "minecraft:composter[level=2]"), (6, 20, "minecraft:barrel[facing=up]"), (-12, 18, "minecraft:cauldron"),
                (-14, 20, "minecraft:stripped_oak_log[axis=y]"), (23, 15, "minecraft:hay_block[axis=y]"), (-22, 10, "minecraft:barrel[facing=up]")):
    put(x, G + 1, z, b)
# latarnie na słupkach przy ścieżce
for x, z in ((-3, 12), (3, 12), (-8, -2), (8, -2)):
    put(x, G + 1, z, "minecraft:spruce_fence")
    put(x, G + 2, z, "minecraft:spruce_fence")
    put(x, G + 3, z, "minecraft:lantern")

# ------------------------------------------------------------------ KOMORA (pod mapą, zamknięta)
KY = 40
fill(-3, KY, -3, 3, KY + 5, 3, "minecraft:spruce_planks")
fill(-2, KY + 1, -2, 2, KY + 4, 2, None)
fill(-2, KY, -2, 2, KY, 2, "minecraft:spruce_planks")
for x, z in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
    slup(x, z, KY + 1, KY + 4, "minecraft:stripped_dark_oak_log")
put(0, KY + 4, 0, "minecraft:lantern[hanging=true]")
for z in (-1, 0, 1):
    put(-2, KY + 1, z, "minecraft:spruce_stairs[facing=east]")
    put(2, KY + 1, z, "minecraft:spruce_stairs[facing=west]")
put(0, KY + 1, -2, "minecraft:brewing_stand")
put(1, KY + 1, -2, "minecraft:hanging_roots") if False else None

# ------------------------------------------------------------------ zapis
here = os.path.dirname(os.path.abspath(__file__))
out = os.path.join(here, "..", "src", "main", "resources", "mapa.txt")
n = 0
with open(out, "w") as f:
    f.write("# Zagroda Zielarki — wygenerowane przez mapa/generuj.py, nie edytuj ręcznie\n")
    for (x, y, z), b in sorted(B.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        if b is None:
            continue
        f.write(f"{x} {y} {z} {b}\n")
        n += 1
print(f"zapisano {n} bloków -> {os.path.normpath(out)}")
