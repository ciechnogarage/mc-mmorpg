from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).parent / 'staging/plugins/Nexo/pack/assets/nexo/textures/item/ui_settings'
ROOT.mkdir(parents=True, exist_ok=True)

INK = '#251c2b'
BROWN = '#8c532c'
GOLD = '#f4c657'
CREAM = '#fff1cc'
BLUE = '#183650'
CYAN = '#63bad0'
RED = '#b83d4c'


def base():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 15, 15), fill=INK)
    d.rectangle((1, 1, 14, 14), fill=BROWN)
    d.rectangle((2, 2, 13, 13), fill=GOLD)
    d.rectangle((3, 3, 12, 12), fill=BLUE)
    return im, d


im, d = base()
d.rectangle((5, 4, 10, 5), fill=CREAM)
d.rectangle((6, 6, 9, 8), fill=CREAM)
d.rectangle((4, 9, 11, 10), fill=RED)
d.rectangle((4, 11, 11, 11), fill=CYAN)
im.save(ROOT / 'card.png')

im, d = base()
d.ellipse((4, 4, 11, 11), outline=CREAM)
d.ellipse((6, 6, 9, 9), outline=GOLD)
d.point((7, 7), fill=RED)
im.save(ROOT / 'goal.png')

im, d = base()
d.polygon([(4, 4), (7, 5), (10, 4), (12, 5), (12, 11), (9, 10), (6, 11), (4, 10)], fill=CREAM)
d.line([(7, 5), (7, 10)], fill=BROWN)
d.line([(10, 4), (10, 10)], fill=BROWN)
d.line([(5, 8), (7, 9), (9, 7), (11, 7)], fill=CYAN)
im.save(ROOT / 'map.png')

im, d = base()
d.ellipse((4, 4, 11, 11), outline=CREAM)
d.arc((6, 6, 9, 9), 170, 340, fill=CYAN)
d.line([(7, 8), (10, 5)], fill=GOLD)
d.point((10, 5), fill=RED)
im.save(ROOT / 'radar.png')
