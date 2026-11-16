"""Arena przeciwnikow (wspolna z poligonem klas): plac 29x29, niski mur, 4 filary-oslony, podwyzszenie z rampa.
Wynik: world/generated/przeciwnicy/structures/arena_v1.nbt (place template) + wklejenie w kuzni.
Uzycie: arena.py [wklej X Y Z]   (bez argumentow tylko zapis .nbt)"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS, KONTENER  # noqa: E402
import random, sys, os
sys.path.insert(0, f'{TOOLS}/budowle')
from budowla import Budowla
R = 14                                   # promien placu (29x29)
b = Budowla(); random.seed(11)
podloze = ['grass_block'] * 6 + ['moss_block'] * 2 + ['coarse_dirt', 'rooted_dirt']
for x in range(-R, R + 1):
    for z in range(-R, R + 1):
        b.bloki[(x, 0, z)] = 'minecraft:' + random.choice(podloze)
        b.bloki[(x, -1, z)] = 'minecraft:dirt'
        if max(abs(x), abs(z)) == R:                      # mur 3 bl. (moby nie uciekaja, gracz widzi ponad)
            for y in (1, 2, 3):
                b.bloki[(x, y, z)] = 'minecraft:' + random.choice(['mossy_stone_bricks', 'mossy_cobblestone', 'stone_bricks'])
# filary-oslony 2x2x4 w cwiartkach (linia strzalu lucznika, oslona przed pociskami)
for cx, cz in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
    for dx in (0, 1):
        for dz in (0, 1):
            for y in (1, 2, 3, 4):
                b.bloki[(cx + dx, y, cz + dz)] = 'minecraft:' + ('mossy_stone_bricks' if y < 4 else 'chiseled_stone_bricks')
# podwyzszenie 5x5 wys. 2 z rampa (pion: gracz nad mobami)
for x in range(8, 13):
    for z in range(-12, -7):
        for y in (1, 2): b.bloki[(x, y, z)] = 'minecraft:' + ('stone_bricks' if y == 1 else 'mossy_stone_bricks')
for i, (x, y) in enumerate(((7, 1), (6, 1), (5, 1))): pass
b.bloki[(7, 1, -10)] = 'minecraft:stone_brick_stairs[facing=east]'; b.bloki[(7, 2, -10)] = 'minecraft:air'
b.bloki[(7, 2, -9)] = 'minecraft:air'
b.bloki[(6, 1, -10)] = 'minecraft:stone_brick_slab'
# latarnie na murze (czytelnosc przy nocy)
for x, z in ((0, -R), (0, R), (-R, 0), (R, 0)): b.bloki[(x, 4, z)] = 'minecraft:lantern'
b.zapisz('przeciwnicy', 'arena_v1')
print('zapisano arena_v1.nbt', len(b.bloki), 'blokow')
if len(sys.argv) >= 5 and sys.argv[1] == 'wklej':
    import subprocess
    x, y, z = sys.argv[2:5]
    o = subprocess.run(['docker', 'exec', KONTENER, 'rcon-cli', f'execute in minecraft:overworld run place template przeciwnicy:arena_v1 {int(x)-R} {int(y)-1} {int(z)-R}'], capture_output=True, text=True).stdout
    print(o.replace('\x00', '').strip())
