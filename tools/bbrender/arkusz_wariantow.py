"""Porownanie wariantow: wiersz = wariant (przod 3/4, tyl 3/4, oczy gracza 4 bloki). Uzycie: arkusz_wariantow.py DIR OUT n..."""
import sys
from pathlib import Path
from PIL import Image, ImageDraw
d, out, nr = Path(sys.argv[1]), sys.argv[2], sys.argv[3:]
C = 300
S = Image.new('RGB', (70 + C * 3, C * len(nr)), (139, 139, 139)); dr = ImageDraw.Draw(S)
for r, n in enumerate(nr):
    dr.text((10, r * C + C // 2 - 6), f'W{n}', fill=(10, 10, 10))
    for c, suf in enumerate(('przod', 'tyl', 'oczy_4bl')):
        f = next(iter(sorted((d / f'w{n}').glob(f'*__{suf}.png'))), None)
        if not f:
            continue
        im = Image.open(f).convert('RGBA'); bb = im.split()[3].point(lambda v: 255 if v > 20 else 0).getbbox()
        im = im.crop(bb); im.thumbnail((C - 10, C - 10))
        S.paste(im, (70 + c * C + (C - im.width) // 2, r * C + (C - im.height) // 2), im.split()[3])
    dr.line([(0, r * C), (S.width, r * C)], fill=(90, 90, 90))
S.save(out)
