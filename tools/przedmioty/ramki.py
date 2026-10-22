#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Ramki tooltipu per rzadkosc (tooltip_style 1.21.2+) do packa Nexo: assets/przedmioty/textures/gui/sprites/tooltip/.
Wzorzec: vanilla 1.21.11 (client.jar) — frame/background 100x100, nine_slice border 10/9; linia ramki na wcieciu 9..90.
Uzycie: ramki.py <katalog assets/przedmioty> [--podglad plik.png]"""
import json, os, sys
from PIL import Image, ImageDraw

STYLE = {  # id: (kolor gora, kolor dol, podwojna+narozniki)
 'magiczny':   ((110, 110, 255), (45, 45, 150), False),
 'rzadki':     ((255, 235, 90), (150, 120, 20), False),
 'epicki':     ((240, 110, 255), (120, 35, 150), False),
 'legendarny': ((255, 175, 40), (160, 85, 0), True),
 'unikatowy':  ((255, 95, 80), (140, 25, 20), True),
 'zestaw':     ((90, 255, 110), (20, 125, 45), True),
}
TLO = (16, 0, 16, 240)

def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))

def ramka(gora, dol, ozdobna):
    im = Image.new('RGBA', (100, 100), (0, 0, 0, 0)); px = im.load()
    for y in range(9, 91):
        c = mix(gora, dol, (y - 9) / 81) + (235,)
        for x in (9, 90): px[x, y] = c
        if ozdobna:
            ci = mix(gora, dol, (y - 9) / 81) + (90,)
            for x in (11, 88):
                if 11 <= y <= 88: px[x, y] = ci
    for x in range(9, 91):
        px[x, 9] = gora + (235,); px[x, 90] = dol + (235,)
        if ozdobna and 11 <= x <= 88:
            px[x, 11] = gora + (90,); px[x, 88] = dol + (90,)
    if ozdobna:   # narozniki: romb 3x3 na rogach ramki
        for cx, cy, k in ((9, 9, gora), (90, 9, gora), (9, 90, dol), (90, 90, dol)):
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1), (0, 0)):
                x, y = cx + dx, cy + dy
                if 0 <= x < 100 and 0 <= y < 100: px[x, y] = (255, 255, 255, 255) if (dx, dy) == (0, 0) else k + (255,)
    return im

def tlo(gora):
    im = Image.new('RGBA', (100, 100), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = mix(TLO[:3], gora, 0.07) + (TLO[3],)
    d.rectangle([8, 8, 91, 91], fill=c)            # maska jak vanilla 1.21.11: 8..91, sciete narozniki
    for x, y in ((8, 8), (91, 8), (8, 91), (91, 91)):
        im.putpixel((x, y), (0, 0, 0, 0))
    return im

META_R = {'gui': {'scaling': {'type': 'nine_slice', 'width': 100, 'height': 100, 'border': 10, 'stretch_inner': True}}}
META_T = {'gui': {'scaling': {'type': 'nine_slice', 'width': 100, 'height': 100, 'border': 9}}}

def main():
    cel = os.path.join(sys.argv[1], 'textures', 'gui', 'sprites', 'tooltip'); os.makedirs(cel, exist_ok=True)
    podglad = []
    for sid, (g, d, oz) in STYLE.items():
        r, t = ramka(g, d, oz), tlo(g)
        r.save(os.path.join(cel, f'{sid}_frame.png')); t.save(os.path.join(cel, f'{sid}_background.png'))
        json.dump(META_R, open(os.path.join(cel, f'{sid}_frame.png.mcmeta'), 'w'), indent=1)
        json.dump(META_T, open(os.path.join(cel, f'{sid}_background.png.mcmeta'), 'w'), indent=1)
        podglad.append(Image.alpha_composite(t, r))
    if '--podglad' in sys.argv:
        out = Image.new('RGBA', (110 * len(podglad), 110), (40, 40, 40, 255))
        for i, im in enumerate(podglad): out.paste(im, (i * 110 + 5, 5), im)
        out.resize((out.width * 3, out.height * 3), Image.NEAREST).save(sys.argv[sys.argv.index('--podglad') + 1])
    print('ramki:', ', '.join(STYLE))

if __name__ == '__main__':
    main()
