"""Plan Lochu 001 z sekcjami i budowlami (do przegladu). Uzycie: mapa_opisana.py OUT.png"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS  # noqa: E402
import sys, json
sys.path.insert(0, TOOLS)
import make_loch001 as M
from PIL import Image, ImageDraw
S = M.SIZE; h = S // 2; K = 3
img = Image.new('RGB', (S, S)); px = img.load()
for py in range(S):
    for pxx in range(S):
        px[pxx, py] = M.COL[M.zone_at(pxx - h, py - h)]
img = img.resize((S * K, S * K), Image.NEAREST)
# przyciecie do wyspy
d = ImageDraw.Draw(img)
def P(x, z): return ((x + h) * K, (z + h) * K)
def znak(x, z, kolor, r=7):
    cx, cy = P(x, z); d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=kolor, outline=(20, 20, 20), width=2)
EL = [((-2, 118), (230, 120, 40), 'Oboz Przybycia'), ((46, 22), (120, 60, 30), 'Oboz Ciernia + wieza'),
      ((-14, -86), (230, 120, 40), 'Ognisko Odpoczynku'), ((0, -163), (240, 150, 200), 'Serce Gaju (sakura)'),
      ((62, 70), (250, 220, 60), 'Kwiat Gaju A'), ((-80, 14), (250, 220, 60), 'Kwiat Gaju B'), ((-36, -100), (250, 220, 60), 'Kwiat Gaju C')]
for s in M.SEKCJE:
    cx, cy = P(*s['xz']); d.ellipse((cx - 11, cy - 11, cx + 11, cy + 11), fill=(30, 30, 35))
    d.text((cx - 3, cy - 6), str(M.SEKCJE.index(s) + 1), fill=(255, 255, 255))
for (x, z), k, n in EL:
    znak(x, z, k)
# mur bramy
d.line([P(-22, -48), P(-3, -48)], fill=(90, 90, 95), width=6); d.line([P(4, -48), P(40, -48)], fill=(90, 90, 95), width=6)
lo = [P(x, z) for x in range(-110, 110) for z in range(-176, 150) if M.zone_at(x, z) != 'pustka']
xs = [p[0] for p in lo]; ys = [p[1] for p in lo]
img = img.crop((max(0, min(xs) - 30), max(0, min(ys) - 30), min(S * K, max(xs) + 30), min(S * K, max(ys) + 30)))
img.save(sys.argv[1]); print(img.size)
