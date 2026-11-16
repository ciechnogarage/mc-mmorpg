#!/usr/bin/env python3
"""Top-down build preview, based on the same deterministic block plan."""

from pathlib import Path
from PIL import Image, ImageDraw
import build

build.main()
surface = {}
for (x, y, z), block in build.BLOCKS.items():
    if (x, z) not in surface or y > surface[(x, z)][0]:
        surface[(x, z)] = (y, block)


def color(block):
    if 'deepslate' in block or 'blackstone' in block:
        return (48, 50, 58)
    if 'amethyst' in block:
        return (145, 103, 183)
    if 'sea_lantern' in block:
        return (176, 213, 216)
    if 'spruce' in block or 'oak' in block:
        return (88, 59, 43)
    if 'red_wool' in block:
        return (151, 46, 48)
    if 'calcite' in block or 'quartz' in block:
        return (220, 218, 198)
    if 'stone_brick' in block or 'andesite' in block or 'diorite' in block:
        return (128, 129, 128)
    if 'leaves' in block or 'azalea' in block or 'moss' in block:
        return (66, 111, 67)
    if 'grass' in block:
        return (91, 124, 68)
    if 'dirt' in block:
        return (112, 83, 55)
    if 'lantern' in block:
        return (222, 174, 83)
    return (159, 145, 118)


scale = 8
img = Image.new('RGB', (112 * scale, 112 * scale), (34, 42, 61))
draw = ImageDraw.Draw(img)
for (x, z), (_, block) in surface.items():
    px, pz = (x + 56) * scale, (z + 56) * scale
    draw.rectangle((px, pz, px + scale - 1, pz + scale - 1), fill=color(block))
for x, z, label in [(0, 41, 'START'), (0, 0, 'NEXUS'), (-30, 19, 'RYNEK'),
                    (30, 19, 'PROFESJE'), (-30, -20, 'POSTAC'),
                    (30, -20, 'TABLICA'), (0, -34, 'SWIATYNIA')]:
    px, pz = (x + 56) * scale, (z + 56) * scale
    draw.ellipse((px - 3, pz - 3, px + 3, pz + 3), fill=(235, 90, 70))
    draw.text((px + 6, pz - 6), label, fill=(255, 255, 255), stroke_width=1, stroke_fill=(0, 0, 0))
Path('/opt/docker/mc/data/world1/output/stolica').mkdir(parents=True, exist_ok=True)
img.save('/opt/docker/mc/data/world1/output/stolica/mapa.png')
