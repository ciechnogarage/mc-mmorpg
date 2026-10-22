"""Arena Bagien (wariant forge/tools/przeciwnicy/arena.py): plac 29x29, mur 3 bl., 4 filary-oslony 2x2x4 (Koscieja),
podwyzszenie z rampa, + BASEN 5x7 gl. 2 przy KLADCE (Pijawki wyskakuja z wody) i bloto (mud) przy murze.
Zapis: /opt/docker/mc/bagna-test/data/world/generated/bagna/structures/arena_bagna.nbt; wklejenie:
  execute in minecraft:overworld run place template bagna:arena_bagna2 -14 -64 -14   (srodek areny = 0.5,-60,0.5)
Punkty (wzgledem srodka): gracz start (0,-7), mob (0,+6); basen x -12..-8, z -3..3; kladka x -7, z -3..3."""
import os, random, sys
sys.path.insert(0, '/opt/docker/mc/forge/tools/budowle')
import budowla
budowla.GENERATED = '/opt/docker/mc/bagna-test/data/world/generated'
from budowla import Budowla
R = 14
b = Budowla(); random.seed(11)
pod = ['grass_block'] * 5 + ['moss_block'] * 2 + ['coarse_dirt', 'rooted_dirt', 'podzol']   # bez mud: pathfinder mineflayera staje przed blotem (03.10)
for x in range(-R, R + 1):
    for z in range(-R, R + 1):
        b.bloki[(x, 0, z)] = 'minecraft:' + random.choice(pod)
        for y in (-1, -2, -3): b.bloki[(x, y, z)] = 'minecraft:dirt'
        if max(abs(x), abs(z)) == R:
            for y in (1, 2, 3):
                b.bloki[(x, y, z)] = 'minecraft:' + random.choice(['mossy_stone_bricks', 'mossy_cobblestone', 'stone_bricks'])
for cx, cz in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
    for dx in (0, 1):
        for dz in (0, 1):
            for y in (1, 2, 3, 4):
                b.bloki[(cx + dx, y, cz + dz)] = 'minecraft:' + ('mossy_stone_bricks' if y < 4 else 'chiseled_stone_bricks')
for x in range(8, 13):
    for z in range(-12, -7):
        for y in (1, 2): b.bloki[(x, y, z)] = 'minecraft:' + ('stone_bricks' if y == 1 else 'mossy_stone_bricks')
b.bloki[(7, 1, -10)] = 'minecraft:stone_brick_stairs[facing=east]'
# basen gl. 2 (woda y=-1,0), dno z blota, kladka (deski) wzdluz brzegu
for x in range(-12, -7):
    for z in range(-3, 4):
        b.bloki[(x, -2, z)] = 'minecraft:mud'
        b.bloki[(x, -1, z)] = 'minecraft:water'
        b.bloki[(x, 0, z)] = 'minecraft:water'
for z in range(-3, 4):
    b.bloki[(-7, 0, z)] = 'minecraft:spruce_planks'
    # polka gl. 1 przy kladce: moby z dna moga wyjsc (gl. 2 bez stopnia = Pijawki uwiezione, pomiar 03.10)
    b.bloki[(-8, -1, z)] = 'minecraft:mud'
for x, z in ((0, -R), (0, R), (-R, 0), (R, 0)): b.bloki[(x, 4, z)] = 'minecraft:lantern'
i = b.zapisz('bagna', 'arena_bagna2')   # nowa nazwa: place template trzyma stary szablon w pamieci
print('zapisano', i.get('plik'), len(b.bloki), 'blokow')
