#!/usr/bin/env python3
"""Generuje tlo panelu ekwipunku dla MMOInventory (glif Nexo mmo_equipment_bg).

Rozmiar 176x222 = dokladny rozmiar GUI skrzyni 6-rzedowej w pikselach GUI (1x).
MUSI byc <= 256 px w kazdym wymiarze — Nexo odrzuca wieksze tekstury przy walidacji
packu ("above allowed 256x256 resolution") i podmienia je na placeholder, czyli
panel w ogole nie dociera do gracza. Wczesniejsza wersja miala 528x666 (3x) i
wlasnie tak byla odrzucana.

Uzycie: python3 generate_bg.py [sciezka-wyjsciowa]
"""
import sys
from PIL import Image, ImageDraw

W, H = 176, 222
OUT = sys.argv[1] if len(sys.argv) > 1 else "mmo_equipment_bg.png"

TOP = (26, 36, 46)       # gora - rozjasniona
BOTTOM = (6, 8, 10)      # dol - prawie czarny
BORDER = (86, 126, 150)  # stalowy blekit ramki
BORDER_DIM = (44, 66, 80)

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

# pionowy gradient + delikatne rozjasnienie ku gorze-srodkowi
for y in range(H):
    t = y / (H - 1)
    base = tuple(int(TOP[i] + (BOTTOM[i] - TOP[i]) * (t ** 0.85)) for i in range(3))
    for x in range(W):
        dx = (x - W / 2) / (W / 2)
        glow = max(0.0, 1.0 - (dx * dx + (t * 1.6) ** 2)) * 14
        px[x, y] = (
            min(255, int(base[0] + glow)),
            min(255, int(base[1] + glow)),
            min(255, int(base[2] + glow)),
            255,
        )

d = ImageDraw.Draw(img)
# zewnetrzna ramka (przygaszona) i wewnetrzna (jasna) - obie 1 px, ostre przy 1x
d.rectangle([0, 0, W - 1, H - 1], outline=BORDER_DIM + (255,), width=1)
d.rectangle([3, 3, W - 4, H - 4], outline=BORDER + (255,), width=1)
# zaokraglenie naroznikow: wygas skrajne piksele obu ramek
for cx, cy in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
    px[cx, cy] = (0, 0, 0, 0)

img.save(OUT)
print(f"{OUT}: {img.size[0]}x{img.size[1]}")
