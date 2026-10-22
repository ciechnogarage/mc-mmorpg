"""Sklada podglad jednego modelu z zrzutow bb_mcp_arkusz.py + bb_oczy_gracza.py: gora = przod|tyl|atak, dol = oczy gracza.
Uzycie: python3 arkusz_podgladu.py DIR_ZRZUTOW OUT.png"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw
d = Path(sys.argv[1]); C = 360
def kafel(f, podpis):
    t = Image.new('RGB', (C, C), (139, 139, 139))
    if f:
        im = Image.open(f).convert('RGBA'); bb = im.split()[3].point(lambda v: 255 if v > 20 else 0).getbbox()
        im = im.crop(bb); im.thumbnail((C - 12, C - 24))
        t.paste(im, ((C - im.width) // 2, 18 + (C - 18 - im.height) // 2), im.split()[3])
    ImageDraw.Draw(t).text((5, 4), podpis, fill=(15, 15, 15)); return t
g = lambda suf: next(iter(sorted(d.glob('*__' + suf + '.png'))), None)
rzedy = [[(g('przod'), 'przod 3/4 z gory'), (g('tyl'), 'tyl 3/4 z gory'), (g('atak'), 'klatka ataku')],
         [(g('oczy_2bl'), 'oczy gracza, 2 bloki'), (g('oczy_4bl'), 'oczy gracza, 4 bloki'), (None, '')]]
S = Image.new('RGB', (C * 3, C * 2), (139, 139, 139))
for r, rz in enumerate(rzedy):
    for c, (f, p) in enumerate(rz):
        S.paste(kafel(f, p), (c * C, r * C))
S.save(sys.argv[2])
