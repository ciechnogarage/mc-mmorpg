"""Reproducible command-only arena build; run locally, then RCON each budowa.txt line."""
from pathlib import Path

OUT = Path(__file__).with_name('budowa.txt')
commands = []
def emit(command):
    commands.append('minecraft:execute in minecraft:arena run minecraft:' + command)
def block(x, y, z, material):
    emit(f'setblock {x} {y} {z} minecraft:{material} replace')
def fill(x1, y1, z1, x2, y2, z2, material):
    emit(f'fill {x1} {y1} {z1} {x2} {y2} {z2} minecraft:{material} replace')

# Flat combat ring (walkable Y=64); edge guard is a circular wall with north/east gates.
for x in range(-22, 23):
    for z in range(-22, 23):
        d = x*x + z*z
        if d <= 20*20:
            block(x, 63, z, 'polished_deepslate' if d < 18*18 else 'chiseled_deepslate')
        if 20*20 <= d <= 22*22:
            gate_north = abs(x) <= 2 and z < -18
            gate_east = abs(z) <= 2 and x > 18
            if not (gate_north or gate_east):
                block(x, 63, z, 'deepslate_bricks')
                fill(x, 64, z, x, 67, z, 'deepslate_brick_wall')
# Raised spectator terraces and perimeter lamps.
for x in range(-31, 32):
    for z in range(-31, 32):
        d = x*x + z*z
        if 23*23 <= d <= 29*29 and not (-5 <= x <= -3 and z in (-29, -28)):
            block(x, 67, z, 'stone_bricks' if d < 27*27 else 'polished_andesite')
            if (x + z) % 13 == 0 and not (x > 18 and abs(z) <= 2) and not (z < -18 and abs(x) <= 2):
                fill(x, 63, z, x, 66, z, 'deepslate_bricks')
        if 29*29 <= d <= 30*30 and (x + z) % 7 == 0:
            block(x, 68, z, 'sea_lantern')
# Arrival platform, stairs and a safe path to the north gate.
fill(-6, 64, -35, 6, 64, -25, 'polished_andesite')
fill(-2, 63, -25, 2, 63, -19, 'smooth_stone')
for z, y in [(-29, 65), (-28, 66), (-27, 67)]:
    fill(-5, y, z, -3, y, z, 'stone_bricks')
fill(-6, 65, -35, -6, 65, -25, 'sea_lantern')
fill(6, 65, -35, 6, 65, -25, 'sea_lantern')
# Safe east path and training court.
fill(19, 63, -2, 32, 63, 2, 'smooth_stone')
fill(32, 64, -11, 42, 64, 11, 'polished_andesite')
for z in (-6, 0, 6):
    fill(34, 64, z-1, 36, 64, z+1, 'chiseled_stone_bricks')
    block(35, 64, z, 'sea_lantern')
for z in range(-11, 12):
    block(42, 65, z, 'deepslate_brick_wall')
OUT.write_text('\n'.join(commands) + '\n', encoding='utf-8')
print(f'{OUT}: {len(commands)} komend')
