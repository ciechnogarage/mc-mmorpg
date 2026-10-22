"""Reproducible K2/model 3/4 and orthographic comparison sheet."""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

HERE = Path(__file__).resolve().parent
ROOT = Path('/opt/docker/mc/data/world1/output/lochy/bagna')
sys.path.insert(0, str(HERE.parent / 'bbrender'))
import orto_iou
import render_bbmodel

GRAY = (139, 139, 139)
VIEWS = (
    ('front', (0, 0, 790, 724), 'PRZOD'),
    ('side', (810, 0, 1300, 724), 'BOK'),
    ('back', (1390, 0, 2172, 724), 'TYL'),
)


def build_preview(model_path=None):
    model_path = Path(model_path or ROOT / 'modele/mulowiec/mulowiec.bbmodel')
    model = json.loads(model_path.read_text())
    clean = Image.open(ROOT / 'szkice/mulowiec_k2_czysty.png').convert('RGB').resize((768, 512), Image.Resampling.LANCZOS)
    # Replace the connected studio backdrop with the exact comparison gray.
    for seed in ((0, 0), (767, 0), (0, 511), (767, 511)):
        ImageDraw.floodfill(clean, seed, GRAY, thresh=15)
    ortho = Image.open(ROOT / 'szkice/mulowiec_orto.png').convert('RGB')
    sheet = Image.new('RGB', (1536, 1532), GRAY)
    dr = ImageDraw.Draw(sheet)
    dr.rectangle((0, 0, 1536, 28), fill=(45, 45, 45))
    dr.text((12, 8), 'K2 3/4  |  MODEL 3/4  |  TEN SAM WZROST, TLO #8b8b8b', fill='white')
    sheet.paste(clean, (0, 30))
    rendered, _ = render_bbmodel.renderuj(model, 160, 10, 700)
    rendered = ImageOps.mirror(rendered)
    box = rendered.getbbox()
    if box is None:
        raise ValueError('empty model render')
    rendered = rendered.crop(box)
    # The cleaned K2 character runs from about y=55 to y=920 in 1024px source.
    target_h = 433
    target_w = round(rendered.width * target_h / rendered.height)
    rendered = rendered.resize((target_w, target_h), Image.Resampling.NEAREST)
    model_panel = Image.new('RGBA', (768, 512), GRAY + (255,))
    model_panel.alpha_composite(rendered, ((768-target_w)//2, 460-target_h))
    sheet.paste(model_panel.convert('RGB'), (768, 30))
    scores = {}
    for index, (view, crop, label) in enumerate(VIEWS):
        y = 554 + index * 326
        dr.rectangle((0, y, 1536, y+26), fill=(45, 45, 45))
        dr.text((12, y+7), label, fill='white')
        concept = ortho.crop(crop)
        rendered, mask, _ = orto_iou.rasterize(model, view, scale=5)
        score, comparison = orto_iou.compare(concept, rendered, mask, height=290)
        scores[view] = score
        if comparison.width > 1536:
            comparison = comparison.resize((1536, round(comparison.height * 1536 / comparison.width)), Image.Resampling.NEAREST)
        sheet.paste(comparison.convert('RGB'), ((1536-comparison.width)//2, y+28))
    dest = model_path.parent / 'podglad.png'
    sheet.save(dest)
    print(dest, ' '.join(f'{view}={score:.4f}' for view, score in scores.items()))
    return scores


if __name__ == '__main__':
    build_preview(sys.argv[1] if len(sys.argv)>1 else None)
