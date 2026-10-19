"""Podglad: kazda tekstura x8, plus zlozony ped (4 karty pionowo) i kepa w swiecie obok siebie — ciaglosc stylu."""
import sys, glob, os
from PIL import Image, ImageDraw
d, prefix, out = sys.argv[1], sys.argv[2], sys.argv[3]
pl = sorted(glob.glob(f'{d}/{prefix}_*.png'))
S = 8; M = 10; tlo = (122, 160, 88)
ims = [(os.path.basename(p)[len(prefix)+1:-4], Image.open(p).convert('RGBA')) for p in pl]
w = M + len(ims) * (16 * S + M)
H = 16 * S + 2 * M + 14
# zlozony ped: dol, srodek, gora, kwiat (od dolu) x8 — i ta sama kepa x8
sklad = [n for n in ('kwiat', 'gora', 'srodek', 'dol') if any(k == n for k, _ in ims)]
H2 = len(sklad) * 16 * S + 2 * M if sklad else 0
img = Image.new('RGB', (max(w, 2 * (16 * S) + 3 * M), H + H2), tlo)
dr = ImageDraw.Draw(img)
for i, (n, im) in enumerate(ims):
    x = M + i * (16 * S + M)
    img.paste(im.resize((16 * S, 16 * S), Image.NEAREST), (x, M), im.resize((16 * S, 16 * S), Image.NEAREST))
    dr.text((x, M + 16 * S + 2), n, fill=(20, 20, 20))
if sklad:
    m = dict(ims); y = H + M
    for n in sklad:
        t = m[n].resize((16 * S, 16 * S), Image.NEAREST); img.paste(t, (M, y), t); y += 16 * S
    if 'swiat' in m:
        t = m['swiat'].resize((16 * S * 2, 16 * S * 2), Image.NEAREST)
        img.paste(t, (2 * M + 16 * S, H + H2 - M - 16 * S * 2), t)
img.save(out); print(out)
