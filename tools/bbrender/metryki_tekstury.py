"""Metryki tekstury .bbmodel porownywalne z LR (recenzja 2026-09-26): liczba kolorow, % identycznych sasiadow
(plaskosc plam; LR 29-48%), mediana jasnosci V (dreadroot 0.33). Uzycie: python3 metryki_tekstury.py model.bbmodel..."""
import base64, io, json, sys
import numpy as np
from PIL import Image
for p in sys.argv[1:]:
    d = json.load(open(p))
    im = np.asarray(Image.open(io.BytesIO(base64.b64decode(d['textures'][0]['source'].split(',', 1)[1]))).convert('RGBA'))
    a = im[..., 3] > 127
    rgb = im[..., :3].astype(int)
    kol = len({tuple(c) for c in rgb[a]})
    same = tot = 0
    for dy, dx in ((0, 1), (1, 0)):
        A, Bm = rgb[:im.shape[0] - dy, :im.shape[1] - dx], rgb[dy:, dx:]
        m = a[:im.shape[0] - dy, :im.shape[1] - dx] & a[dy:, dx:]
        same += (np.all(A == Bm, axis=-1) & m).sum(); tot += m.sum()
    v = rgb[a].max(axis=1) / 255
    print(f'{p.rsplit("/", 1)[-1]:28} kolory={kol:5d}  identyczni sasiedzi={100 * same / max(tot, 1):5.1f}%  mediana V={np.median(v):.2f}')
