"""Arkusz klatek z bb_klatki.py: wiersz = animacja, kolumny = klatki; WSPOLNY kadr (jeden bbox dla wszystkich)."""
import json, sys
from pathlib import Path
from PIL import Image, ImageDraw
d = Path(sys.argv[1]); m = json.load(open(d / 'klatki.json'))
ims = {x['plik']: Image.open(d / x['plik']).convert('RGBA') for x in m}
bb = None
for im in ims.values():
    b = im.split()[3].point(lambda v: 255 if v > 20 else 0).getbbox()
    if b: bb = b if bb is None else (min(bb[0], b[0]), min(bb[1], b[1]), max(bb[2], b[2]), max(bb[3], b[3]))
C = 230; anims = list(dict.fromkeys(x['anim'] for x in m))
S = Image.new('RGB', (90 + C * 5, C * len(anims)), (139, 139, 139)); dr = ImageDraw.Draw(S)
for r, a in enumerate(anims):
    dr.text((6, r * C + 8), a, fill=(10, 10, 10))
    for x in [x for x in m if x['anim'] == a]:
        im = ims[x['plik']].crop(bb); im.thumbnail((C - 6, C - 6))
        S.paste(im, (90 + x['i'] * C + (C - im.width) // 2, r * C + (C - im.height) // 2), im.split()[3])
        dr.text((90 + x['i'] * C + 4, r * C + 4), f"t={x['t']}", fill=(20, 20, 20))
S.save(sys.argv[2] if len(sys.argv) > 2 else d / 'arkusz_klatek.png')
