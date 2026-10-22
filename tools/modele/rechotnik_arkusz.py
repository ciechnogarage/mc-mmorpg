"""Arkusz wierności K2: koncept, model 3/4 i obie różnice ortograficzne.

python3 rechotnik_arkusz.py
"""
from pathlib import Path
import json
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path('/opt/docker/mc/data/world1')
OUT = ROOT / 'output/lochy/bagna'
sys.path.insert(0, str(ROOT / 'tools/bbrender'))
import orto_iou as O  # noqa: E402
import render_bbmodel as R  # noqa: E402


def label(draw, pos, text, size=24):
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', size)
    draw.text(pos, text, font=font, fill=(238, 238, 232))


def main():
    model = json.loads((OUT / 'modele/rechotnik/rechotnik.bbmodel').read_text())
    concept = Image.open(OUT / 'szkice/rechotnik_k2.png').convert('RGB')
    ortho = Image.open(OUT / 'szkice/rechotnik_orto.png').convert('RGB')
    bg = (139, 139, 139)
    sheet = Image.new('RGB', (1320, 1540), bg)
    draw = ImageDraw.Draw(sheet)
    label(draw, (15, 5), 'K2 / koncept', 22)
    label(draw, (675, 5), 'model / ten sam kat 3/4 i skala', 22)

    # K2: tylko postać po lewej, bez Steve'a. Neutralne tło zastępujemy #8b8b8b.
    ref = concept.crop((300, 210, 950, 945))
    pix = np.asarray(ref).copy()
    neutral = (pix.max(2).astype(int) - pix.min(2).astype(int) < 8)
    neutral &= (pix.mean(2) > 65) & (pix.mean(2) < 160)
    pix[neutral] = bg
    sheet.paste(Image.fromarray(pix), (5, 38))

    # Render jest rzutem ortograficznym; odbicie ustawia włócznię po tej samej
    # stronie obrazu co w K2 (układ osi kamery renderera jest odwrócony w X).
    rendered, _ = R.renderuj(model, 145, 15, 800)
    rendered = rendered.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    bounds = rendered.getbbox()
    figure = rendered.crop(bounds)
    h = 690  # wysokość od grotu do stóp na K2 w pikselach arkusza
    w = round(figure.width * h / figure.height)
    figure = figure.resize((w, h), Image.Resampling.NEAREST)
    tile = Image.new('RGBA', (650, 735), bg + (255,))
    tile.alpha_composite(figure, ((650 - w) // 2, 720 - h))
    sheet.paste(tile.convert('RGB'), (665, 38))

    specs = [('front', (0, 90, 650, 820), 790),
             ('side', (650, 90, 1100, 820), 1155)]
    for view, crop, y in specs:
        ref = ortho.crop(crop)
        render, mask, _ = O.rasterize(model, view, scale=4)
        score, compare = O.compare(ref, render, mask, height=512)
        label(draw, (15, y), f'{view.upper()} / IoU {score:.3f} / cel 0.850', 22)
        scaled = compare.resize((1290, 330), Image.Resampling.NEAREST)
        sheet.paste(scaled, (15, y + 35))
    dest = OUT / 'modele/rechotnik/podglad.png'
    sheet.save(dest)
    print(dest)


if __name__ == '__main__':
    main()
