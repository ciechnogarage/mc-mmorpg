#!/usr/bin/env python3
"""Generate reproducible vanilla structures for the isolated Stolica world.

Run from the world1 server directory. This only writes the datapack; it does
not place blocks or change any live player destination.
"""

import math
import random
import sys
from pathlib import Path

sys.path.insert(0, '/opt/docker/mc/forge/tools/budowle')
from struktura import zapisz

ROOT = Path('/opt/docker/mc/data/world1/world/datapacks/stolica')
STRUCTURES = ROOT / 'data/stolica/structure'
FUNCTIONS = ROOT / 'data/stolica/function'
BLOCKS = {}
RNG = random.Random(20260930)


def put(x, y, z, block):
    BLOCKS[int(x), int(y), int(z)] = block


def box(x1, y1, z1, x2, y2, z2, block):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                put(x, y, z, block)


def disk(cx, y, cz, radius, block):
    for x in range(cx - radius, cx + radius + 1):
        for z in range(cz - radius, cz + radius + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 <= radius ** 2:
                put(x, y, z, block)


def ring(cx, y, cz, radius, width, block):
    for x in range(cx - radius, cx + radius + 1):
        for z in range(cz - radius, cz + radius + 1):
            d = math.hypot(x - cx, z - cz)
            if radius - width <= d <= radius:
                put(x, y, z, block)


def island():
    # Organic, tapered mass. All blocks live in the new void world.
    for x in range(-55, 56):
        for z in range(-55, 56):
            r = math.hypot(x, z)
            edge = 53 + 1.4 * math.sin(math.atan2(z, x) * 7) + 0.8 * math.sin(math.atan2(z, x) * 11)
            if r > edge:
                continue
            top = 100 if r < 45 else 100 - min(3, int((r - 45) / 3))
            bottom = max(71, int(71 + 24 * (r / edge) ** 1.8))
            for y in range(bottom, top + 1):
                if y == top:
                    material = 'minecraft:grass_block'
                elif y >= top - 3:
                    material = 'minecraft:dirt'
                elif y < 83:
                    material = 'minecraft:deepslate' if RNG.random() > 0.12 else 'minecraft:tuff'
                else:
                    material = 'minecraft:stone' if RNG.random() > 0.17 else 'minecraft:andesite'
                put(x, y, z, material)
    # Dripstone-like rock tips make the floating silhouette less like a disc.
    for cx, cz, depth in [(-23, -24, 17), (22, 15, 15), (7, -28, 12), (-32, 15, 10), (0, 6, 21)]:
        for y in range(71 - depth, 72):
            radius = max(0, int((y - (71 - depth)) / 5))
            disk(cx, y, cz, radius, 'minecraft:deepslate')


def streets():
    box(-4, 100, -43, 4, 100, 45, 'minecraft:stone_bricks')
    box(-43, 100, -4, 43, 100, 4, 'minecraft:stone_bricks')
    ring(0, 100, 0, 18, 7, 'minecraft:stone_bricks')
    for z in range(-40, 46, 5):
        for x in (-3, 3):
            put(x, 100, z, 'minecraft:polished_andesite')
    for x in range(-40, 41, 5):
        for z in (-3, 3):
            put(x, 100, z, 'minecraft:polished_andesite')
    # Low stone edge with regular lights, set back from the natural cliff.
    ring(0, 101, 0, 49, 1.2, 'minecraft:stone_brick_wall')
    for n in range(24):
        a = 2 * math.pi * n / 24
        x, z = round(48 * math.cos(a)), round(48 * math.sin(a))
        box(x, 101, z, x, 102, z, 'minecraft:polished_andesite')
        put(x, 103, z, 'minecraft:lantern')


def house(cx, cz, sx, sz, entry, roof='minecraft:deepslate_tiles'):
    x1, x2 = cx - sx // 2, cx + sx // 2
    z1, z2 = cz - sz // 2, cz + sz // 2
    box(x1, 100, z1, x2, 100, z2, 'minecraft:spruce_planks')
    for y in range(101, 107):
        for x in range(x1, x2 + 1):
            for z in (z1, z2):
                put(x, y, z, 'minecraft:stone_bricks' if y <= 102 else 'minecraft:stripped_spruce_wood')
        for z in range(z1, z2 + 1):
            for x in (x1, x2):
                put(x, y, z, 'minecraft:stone_bricks' if y <= 102 else 'minecraft:stripped_spruce_wood')
    for x in (x1, x2):
        for z in (z1, z2):
            box(x, 101, z, x, 108, z, 'minecraft:dark_oak_log[axis=y]')
    # Roof slopes toward both long sides; every layer steps in to a ridge.
    for h in range(4):
        for x in range(x1 - 1 + h, x2 + 2 - h):
            for z in (z1 - 1 + h, z2 + 1 - h):
                put(x, 107 + h, z, roof)
    box(x1 + 2, 110, cz - 1, x2 - 2, 110, cz + 1, roof)
    # Clear a two-block-high door and place warm windows.
    if entry == 'south':
        door = [(cx + dx, z2) for dx in (-1, 0, 1)]
    elif entry == 'north':
        door = [(cx + dx, z1) for dx in (-1, 0, 1)]
    elif entry == 'east':
        door = [(x2, cz + dz) for dz in (-1, 0, 1)]
    else:
        door = [(x1, cz + dz) for dz in (-1, 0, 1)]
    for x, z in door:
        for y in (101, 102, 103):
            BLOCKS.pop((x, y, z), None)
    for x in (x1 + 3, x2 - 3):
        for z in (z1, z2):
            if (x, z) not in door:
                box(x, 103, z, x, 104, z, 'minecraft:glass_pane')
    for z in (z1 + 3, z2 - 3):
        for x in (x1, x2):
            if (x, z) not in door:
                box(x, 103, z, x, 104, z, 'minecraft:glass_pane')
    for x in range(x1 + 3, x2 - 2, 5):
        put(x, 106, cz, 'minecraft:lantern')
    return (x1, x2, z1, z2)


def nexus():
    disk(0, 100, 0, 12, 'minecraft:polished_andesite')
    ring(0, 100, 0, 12, 2, 'minecraft:chiseled_stone_bricks')
    disk(0, 100, 0, 5, 'minecraft:polished_blackstone_bricks')
    ring(0, 100, 0, 6, 1, 'minecraft:amethyst_block')
    # Monumental open arch. No nether portal block: it cannot send players away by accident.
    for x in (-5, 5):
        box(x, 101, -2, x, 112, -2, 'minecraft:polished_blackstone_bricks')
        box(x, 101, 2, x, 112, 2, 'minecraft:polished_blackstone_bricks')
        for y in (103, 108):
            put(x, y, -2, 'minecraft:sea_lantern')
            put(x, y, 2, 'minecraft:sea_lantern')
    box(-5, 113, -2, 5, 114, 2, 'minecraft:polished_blackstone_bricks')
    box(-3, 113, -2, 3, 113, 2, 'minecraft:amethyst_block')
    for x, z in [(-8, -8), (8, -8), (-8, 8), (8, 8)]:
        box(x, 101, z, x, 104, z, 'minecraft:polished_andesite')
        put(x, 105, z, 'minecraft:lantern')


def details():
    # Arrival gate and view toward Nexus.
    box(-7, 101, 39, -7, 107, 39, 'minecraft:stone_bricks')
    box(7, 101, 39, 7, 107, 39, 'minecraft:stone_bricks')
    box(-7, 108, 39, 7, 109, 39, 'minecraft:stone_bricks')
    for x in (-7, 7):
        put(x, 105, 39, 'minecraft:sea_lantern')
    box(-5, 100, 37, 5, 100, 45, 'minecraft:polished_andesite')
    ring(0, 100, 41, 5, 1, 'minecraft:chiseled_stone_bricks')

    # Market: an open stall line beside a proper hall.
    house(-30, 19, 17, 15, 'east', 'minecraft:dark_oak_planks')
    for z in (12, 19, 26):
        box(-18, 101, z - 2, -18, 103, z + 2, 'minecraft:dark_oak_fence')
        box(-20, 104, z - 3, -15, 104, z + 3, 'minecraft:red_wool')
        box(-19, 101, z - 2, -16, 101, z - 2, 'minecraft:spruce_slab[type=top]')
        put(-17, 102, z, 'minecraft:lantern')
    # Profession quarter with real, recognizable stations.
    house(30, 19, 17, 15, 'west')
    box(17, 101, 13, 20, 101, 15, 'minecraft:stone_bricks')
    put(18, 102, 14, 'minecraft:anvil')
    put(19, 102, 14, 'minecraft:smithing_table')
    put(18, 101, 23, 'minecraft:brewing_stand')
    put(19, 101, 23, 'minecraft:crafting_table')
    put(30, 101, 19, 'minecraft:enchanting_table')
    # Hall for existing character, skills and equipment menus.
    house(-30, -20, 17, 15, 'east')
    for x in (-35, -30, -25):
        put(x, 101, -20, 'minecraft:bookshelf')
    # City board: destination signs and quests, without a direct dungeon start.
    house(30, -20, 17, 15, 'west', 'minecraft:dark_oak_planks')
    box(19, 101, -24, 19, 104, -16, 'minecraft:dark_oak_planks')
    for z in (-23, -20, -17):
        put(18, 102, z, 'minecraft:chiseled_bookshelf')
    # A party/guild hall and practical training yard near the arrival street.
    box(4, 100, 33, 17, 100, 36, 'minecraft:stone_bricks')
    box(-17, 100, 33, -4, 100, 36, 'minecraft:stone_bricks')
    house(23, 36, 13, 11, 'west', 'minecraft:spruce_planks')
    disk(-22, 100, 36, 7, 'minecraft:polished_andesite')
    ring(-22, 101, 36, 7, 0.8, 'minecraft:stone_brick_wall')
    for z in (35, 36, 37):
        BLOCKS.pop((-15, 101, z), None)
    box(-23, 101, 35, -23, 103, 35, 'minecraft:dark_oak_fence')
    put(-23, 104, 35, 'minecraft:carved_pumpkin[facing=south]')
    box(-21, 101, 38, -21, 103, 38, 'minecraft:dark_oak_fence')
    put(-21, 104, 38, 'minecraft:target')
    # Temple is an open civic landmark.
    house(0, -34, 23, 15, 'south', 'minecraft:calcite')
    disk(0, 100, -31, 6, 'minecraft:polished_diorite')
    for x in (-7, 7):
        for z in (-37, -31, -25):
            box(x, 101, z, x, 107, z, 'minecraft:quartz_pillar[axis=y]')
            put(x, 108, z, 'minecraft:sea_lantern')
    # Small gardens fill the margins and keep the city from reading as a flat test pad.
    for x, z in [(-37, 39), (37, 39), (-42, -36), (42, -36), (-11, 30), (12, 30)]:
        if x*x + z*z > 45*45:
            continue
        box(x, 101, z, x, 106, z, 'minecraft:oak_log[axis=y]')
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if dx*dx + dz*dz <= 10:
                    for y in (105, 106, 107):
                        put(x + dx, y, z + dz, 'minecraft:azalea_leaves[persistent=true]')
        put(x, 108, z, 'minecraft:flowering_azalea_leaves[persistent=true]')
    for x, z in [(-13, 33), (15, 34), (-38, -5), (39, 7), (-18, -39), (18, -39)]:
        disk(x, 100, z, 3, 'minecraft:moss_block')
        put(x, 101, z, 'minecraft:flowering_azalea')


def main():
    island()
    streets()
    nexus()
    details()
    STRUCTURES.mkdir(parents=True, exist_ok=True)
    FUNCTIONS.mkdir(parents=True, exist_ok=True)
    (ROOT / 'pack.mcmeta').write_text('{"pack":{"description":"Stolica Wyspy — odtwarzalny projekt","min_format":[94,1],"max_format":[94,1]}}\n')
    commands = []
    summaries = []
    for ty in (48, 88):
        for tx in range(-64, 64, 32):
            for tz in range(-64, 64, 32):
                tile = {(x,y,z): b for (x,y,z),b in BLOCKS.items()
                        if tx <= x < tx+32 and tz <= z < tz+32 and ty <= y < ty+40}
                if not tile:
                    continue
                name = f'tile_{tx+64}_{tz+64}_{ty}'
                info = zapisz(tile, STRUCTURES / f'{name}.nbt', 4671)
                x0, y0, z0 = info['offset']
                commands.append(f'execute in minecraft:stolica run place template stolica:{name} {x0} {y0} {z0}')
                summaries.append((name, info['blokow'], info['size']))
    (FUNCTIONS / 'build.mcfunction').write_text('\n'.join(commands) + '\n')
    (ROOT / 'manifest.txt').write_text('\n'.join(f'{name}: {count} blocks, {size}' for name,count,size in summaries) + '\n')
    print(f'{len(BLOCKS)} blocks across {len(commands)} structures')
    for item in summaries:
        print(*item)


if __name__ == '__main__':
    main()
