#!/usr/bin/env python3
"""Karta MMOCore 176x222; sloty: x=8+18*kolumna, y=18+18*rzad."""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)
OUT = ROOT / 'plugins/Nexo/pack/assets/nexo/textures/gui/karta_postaci_bg.png'
TMP = Path('/home/dev/.claude/jobs/931ad3b9/tmp')
INVENTORY = ROOT / 'plugins/Nexo/pack/assets/minecraft/textures/gui/container/inventory.png'
W, H = 176, 222

# Dokladne kolory wystepujace w inventory.png.
INK = (7, 9, 13, 255)
DARK = (19, 23, 30, 255)
SLATE = (28, 35, 43, 255)
PLATE = (37, 45, 55, 255)
EDGE = (71, 82, 96, 255)
LIGHT = (88, 101, 116, 255)
BRASS_DARK = (74, 55, 24, 255)
BRASS = (138, 106, 48, 255)
GOLD = (216, 185, 120, 255)

HEADERS = (1, 4, 7)
BODY = (10, 19, 28, 37, 12, 13, 14, 21, 22, 23,
        30, 31, 32, 39, 40, 41, 16, 25, 34, 43)
BUTTONS = (45, 49, 53)
SLOTS = HEADERS + BODY + BUTTONS


def slot_xy(slot):
    return 8 + 18 * (slot % 9), 18 + 18 * (slot // 9)


im = Image.new('RGBA', (W, H), SLATE)
d = ImageDraw.Draw(im)

# Rama i ciemne, wolne pole pod tekst klienta zaczynajacy sie w x=8,y=6.
d.rectangle((0, 0, 175, 221), outline=INK, width=2)
d.line((2, 2, 173, 2), fill=LIGHT)
d.line((2, 2, 2, 219), fill=LIGHT)
d.line((3, 219, 173, 219), fill=INK)
d.line((173, 3, 173, 219), fill=INK)
d.rectangle((4, 4, 171, 217), outline=EDGE)
d.rectangle((6, 6, 169, 215), fill=SLATE)
for x in (4, 169):
    for y in (4, 215):
        d.rectangle((x, y, x + 2, y + 2), fill=BRASS)
        d.point((x + 1, y + 1), fill=GOLD)
d.rectangle((7, 6, 168, 16), fill=DARK)
d.line((7, 17, 168, 17), fill=BRASS_DARK)


def panel(x0, x1):
    """Fazowana oprawa kolumny ikon lub zwartej siatki 3 kolumn."""
    y0, y1 = 17, 109
    d.rectangle((x0, y0, x1, y1), fill=INK)
    d.rectangle((x0 + 1, y0 + 1, x1 - 1, y1 - 1), fill=PLATE)
    d.rectangle((x0 + 2, y0 + 2, x1 - 2, y1 - 2), fill=DARK)
    d.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=LIGHT)
    d.line((x0 + 1, y0 + 1, x0 + 1, y1 - 1), fill=LIGHT)
    d.line((x0 + 2, y1 - 1, x1 - 1, y1 - 1), fill=INK)
    d.line((x1 - 1, y0 + 2, x1 - 1, y1 - 1), fill=INK)
    for x in (x0 + 2, x1 - 3):
        for y in (y0 + 3, y1 - 4):
            d.rectangle((x, y, x + 1, y + 1), fill=BRASS_DARK)
            d.point((x, y), fill=GOLD)


for bounds in ((22, 47), (58, 119), (130, 155)):
    panel(*bounds)

# Ramiona WALKA spinaja slot 4 z trzema kolumnami ponizej.
d.line((61, 27, 79, 27), fill=BRASS)
d.line((98, 27, 116, 27), fill=BRASS)
d.line((62, 28, 79, 28), fill=BRASS_DARK)
d.line((98, 28, 115, 28), fill=BRASS_DARK)
for x in (61, 115):
    d.rectangle((x, 26, x + 1, 29), fill=BRASS)
    d.point((x, 26), fill=GOLD)
d.rectangle((87, 34, 90, 37), fill=BRASS_DARK)
d.line((87, 34, 90, 34), fill=GOLD)


def well(slot):
    """Studzienka 18x18: ciemna gora/lewo, jasny dol/prawo."""
    x, y = slot_xy(slot)
    d.rectangle((x, y, x + 17, y + 17), fill=INK)
    d.rectangle((x + 2, y + 2, x + 15, y + 15), fill=DARK)
    d.line((x, y, x + 16, y), fill=INK)
    d.line((x, y, x, y + 16), fill=INK)
    d.line((x + 1, y + 1, x + 16, y + 1), fill=SLATE)
    d.line((x + 1, y + 1, x + 1, y + 16), fill=SLATE)
    d.line((x + 1, y + 17, x + 17, y + 17), fill=LIGHT)
    d.line((x + 17, y + 1, x + 17, y + 17), fill=LIGHT)
    d.line((x + 2, y + 16, x + 16, y + 16), fill=EDGE)
    d.line((x + 16, y + 2, x + 16, y + 16), fill=EDGE)


for slot in BODY:
    well(slot)


def header(slot):
    x, y = slot_xy(slot)
    d.rectangle((x, y, x + 17, y + 17), fill=BRASS_DARK)
    d.line((x, y, x + 16, y), fill=GOLD)
    d.line((x, y, x, y + 16), fill=GOLD)
    d.line((x + 1, y + 1, x + 16, y + 1), fill=BRASS)
    d.line((x + 1, y + 1, x + 1, y + 16), fill=BRASS)
    d.rectangle((x + 2, y + 2, x + 15, y + 15), fill=DARK)
    d.line((x + 2, y + 2, x + 15, y + 2), fill=INK)
    d.line((x + 2, y + 2, x + 2, y + 15), fill=INK)
    d.line((x + 2, y + 15, x + 15, y + 15), fill=BRASS)
    d.line((x + 15, y + 2, x + 15, y + 15), fill=BRASS)
    d.line((x + 1, y + 17, x + 17, y + 17), fill=BRASS_DARK)
    d.line((x + 17, y + 1, x + 17, y + 17), fill=BRASS_DARK)
    d.point((x + 1, y + 1), fill=GOLD)
    d.point((x + 16, y + 16), fill=GOLD)


for slot in HEADERS:
    header(slot)

# Dolny rzad: jedna listwa i trzy wieksze mosiężne przyciski.
d.rectangle((7, 110, 168, 128), fill=DARK)
d.line((7, 110, 168, 110), fill=LIGHT)
d.line((7, 111, 168, 111), fill=BRASS_DARK)
d.line((7, 127, 168, 127), fill=BRASS)
d.line((7, 128, 168, 128), fill=INK)
for slot in BUTTONS:
    x, y = slot_xy(slot)
    d.rectangle((x - 1, y, x + 18, y + 19), fill=BRASS_DARK)
    d.line((x - 1, y, x + 17, y), fill=GOLD)
    d.line((x - 1, y, x - 1, y + 18), fill=GOLD)
    d.line((x, y + 19, x + 18, y + 19), fill=INK)
    d.line((x + 18, y + 1, x + 18, y + 19), fill=INK)
    d.rectangle((x + 1, y + 2, x + 16, y + 15), fill=DARK)
    d.line((x, y + 1, x + 17, y + 1), fill=BRASS)
    d.line((x, y + 1, x, y + 16), fill=BRASS)
    d.line((x + 1, y + 16, x + 17, y + 16), fill=BRASS)
    d.line((x + 17, y + 2, x + 17, y + 16), fill=BRASS)
    d.point((x, y + 1), fill=GOLD)
    d.point((x + 17, y + 16), fill=GOLD)

# Zachowany uklad dolnych 3 rzedow ekwipunku i hotbara.
d.rectangle((7, 135, 168, 216), fill=DARK)
d.line((7, 135, 168, 135), fill=BRASS)
for row_y in (140, 158, 176, 198):
    for col in range(9):
        x = 8 + 18 * col
        d.rectangle((x, row_y, x + 17, row_y + 17), fill=INK)
        d.rectangle((x + 1, row_y + 1, x + 16, row_y + 16), fill=SLATE)
        d.line((x + 1, row_y + 1, x + 16, row_y + 1), fill=EDGE)
        d.line((x + 1, row_y + 1, x + 1, row_y + 16), fill=EDGE)

for x, y in ((0, 0), (175, 0), (0, 221), (175, 221)):
    im.putpixel((x, y), (0, 0, 0, 0))

with Image.open(INVENTORY) as source:
    palette = set(source.convert('RGBA').get_flattened_data())
assert set(im.get_flattened_data()) <= palette, 'Barwa spoza inventory.png'

OUT.parent.mkdir(parents=True, exist_ok=True)
TMP.mkdir(parents=True, exist_ok=True)
im.save(OUT)
im.resize((W * 3, H * 3), Image.Resampling.NEAREST).save(TMP / 'karta_po_czysty.png')

# Cienki obrys zajetych slotow wewnatrz studzienki; faza pozostaje widoczna.
preview = im.copy()
pd = ImageDraw.Draw(preview)
for slot in SLOTS:
    x, y = slot_xy(slot)
    pd.rectangle((x + 3, y + 3, x + 14, y + 14), outline=GOLD)
preview.resize((W * 3, H * 3), Image.Resampling.NEAREST).save(TMP / 'karta_po.png')
print(f'{OUT} ({W}x{H}); podglady x3: karta_po.png, karta_po_czysty.png')
