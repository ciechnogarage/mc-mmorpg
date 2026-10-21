"""Podglad budowli offline: izometria z kolorami srednimi tekstur z client.jar (bez serwera i klienta).
Uzycie: podglad.py OUT.png plik1.iob|plik1.nbt ... [--kolumny 6 --kafel 220]
Sluzy do PRZEGLADU biblioteki i wlasnych budowli; ocena koncowa - zrzut w kliencie QA."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS  # noqa: E402
import argparse, gzip, io, math, sys, zipfile, os
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import iob

JAR = '/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar'
_zip = zipfile.ZipFile(JAR)
_names = set(_zip.namelist())
_kolory = {}
DREWNA = ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped', 'pale_oak')


def _tex(nazwa):
    for kand in (nazwa, nazwa + '_top', nazwa + '_side', nazwa + '_front', nazwa + '_0', nazwa + '_stage0'):
        p = f'assets/minecraft/textures/block/{kand}.png'
        if p in _names:
            return p
    return None


STALE = {'water': (50, 90, 200, 190), 'lava': (230, 110, 20, 255), 'chest': (160, 110, 40, 255),
         'wall_torch': (250, 200, 80, 255), 'torch': (250, 200, 80, 255), 'redstone_wire': (160, 20, 20, 255),
         'redstone_wall_torch': (200, 40, 30, 255), 'campfire': (230, 130, 40, 255), 'decorated_pot': (150, 80, 50, 255),
         'skeleton_skull': (220, 220, 210, 255), 'chain': (60, 65, 80, 255), 'void_air': (0, 0, 0, 0)}
PRZEMIANY = [('waxed_', ''), ('smooth_sandstone', 'sandstone_top'), ('smooth_quartz', 'quartz_block_top'),
             ('quartz', 'quartz_block_side'), ('purpur', 'purpur_block'), ('snow_block', 'snow'),
             ('magma_block', 'magma'), ('petrified_oak', 'oak_planks'), ('cut_copper', 'cut_copper'),
             ('_bed', '_wool'), ('_wall_banner', '_wool'), ('_banner', '_wool'), ('_carpet', '_wool'),
             ('potted_', ''), ('copper_bulb', 'copper_bulb'), ('iron_door', 'iron_block')]


def kolor(blok):
    """(r,g,b,a) sredni kolor bloku (bez stanow). Schody/plyty/murki -> material bazowy."""
    n = blok.split('[')[0].split(':')[-1]
    if n in _kolory:
        return _kolory[n]
    if n in STALE:
        _kolory[n] = STALE[n]; return STALE[n]
    baza = n
    for a, b in PRZEMIANY:
        if a in baza:
            baza = baza.replace(a, b)
    for suf in ('_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_button', '_pressure_plate', '_trapdoor', '_door'):
        if baza.endswith(suf):
            baza = baza[: -len(suf)]
            break
    kand = [baza, baza + 's', baza.replace('_wood', '_log'), baza.replace('bricks', 'brick') + 's']
    if baza in DREWNA:
        kand.insert(0, baza + '_planks')
    if baza.endswith('brick'):
        kand.insert(0, baza + 's')
    p = next((t for t in (_tex(k) for k in kand) if t), None)
    if p is None:
        c = (255, 0, 255, 255)
    else:
        im = Image.open(io.BytesIO(_zip.read(p))).convert('RGBA')
        im = im.crop((0, 0, im.width, min(im.height, im.width)))   # animowane: pierwsza klatka
        px = [q for q in (im.get_flattened_data() if hasattr(im, 'get_flattened_data') else im.getdata()) if q[3] > 40]
        if not px:
            c = (0, 0, 0, 0)
        else:
            c = tuple(sum(q[i] for q in px) // len(px) for i in range(3)) + (255 if len(px) > 0.6 * im.width * im.width else 170,)
    if any(t in n for t in ('grass', 'fern', 'leaves', 'vine', 'lily_pad')) and c[3]:
        c = (int(c[0] * 0.55), int(c[1] * 0.85), int(c[2] * 0.45), c[3])   # tekstury szare - barwione biomem
    _kolory[n] = c
    return c


def _schem(p):
    """Sponge .schem v2/v3: Palette {stan: idx}, BlockData = varinty w kolejnosci (y, z, x)."""
    sys.path.insert(0, TOOLS); import mca
    d = mca.parse_nbt(gzip.open(p).read())
    while isinstance(d, dict) and len(d) == 1 and isinstance(next(iter(d.values())), dict):
        d = next(iter(d.values()))
    W, H, L = d['Width'], d['Height'], d['Length']
    bl = d.get('Blocks', d)
    pal = {v: k for k, v in bl['Palette'].items()}
    data = bl.get('Data', bl.get('BlockData'))
    data = bytes(x & 0xff for x in data)
    out, i, n = {}, 0, 0
    while i < len(data):
        v = s = 0
        while True:
            b = data[i]; i += 1; v |= (b & 0x7f) << s; s += 7
            if not b & 0x80: break
        x = n % W; z = (n // W) % L; y = n // (W * L); n += 1
        out[(x, y, z)] = pal[v]
    return out


def wczytaj(p):
    if p.endswith('.schem'):
        return _schem(p)
    if p.endswith('.iob'):
        return iob.czytaj(p)['bloki']
    sys.path.insert(0, TOOLS); import mca
    d = mca.parse_nbt(gzip.open(p).read())
    d = d.get('', d) if isinstance(d, dict) else d
    pal = [e['Name'] + ('[' + ','.join(f'{k}={v}' for k, v in e.get('Properties', {}).items()) + ']' if e.get('Properties') else '')
           for e in d['palette']]
    return {tuple(b['pos']): pal[b['state']] for b in d['blocks']}


POMIN = ('air', 'structure_void', 'cave_air', 'barrier', 'light')


def renderuj(bloki, kafel):
    bl = {k: v for k, v in bloki.items() if not any(v.split('[')[0].endswith(s) for s in POMIN)}
    if not bl:
        return Image.new('RGBA', (kafel, kafel))
    xs = [p[0] for p in bl]; ys = [p[1] for p in bl]; zs = [p[2] for p in bl]
    rx, ry, rz = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, max(zs) - min(zs) + 1
    s = (kafel * 0.9) / max((rx + rz) * math.cos(math.radians(30)), (rx + rz) * 0.5 + ry, 1)
    cx, cy = kafel / 2, kafel / 2

    def rzut(x, y, z):
        x -= (min(xs) + max(xs) + 1) / 2; z -= (min(zs) + max(zs) + 1) / 2; y -= (min(ys) + max(ys) + 1) / 2
        return cx + (x - z) * math.cos(math.radians(30)) * s, cy + (x + z) * 0.5 * s - y * s

    im = Image.new('RGBA', (kafel, kafel)); d = ImageDraw.Draw(im, 'RGBA')
    # malarz: od najdalszych (maly x+z, niski y) do najblizszych
    for (x, y, z) in sorted(bl, key=lambda p: (p[0] + p[2], p[1])):
        c = kolor(bl[(x, y, z)])
        if not c[3]:
            continue
        def sh(f): return tuple(int(v * f) for v in c[:3]) + (c[3],)
        if (x, y + 1, z) not in bl:
            d.polygon([rzut(x, y + 1, z), rzut(x + 1, y + 1, z), rzut(x + 1, y + 1, z + 1), rzut(x, y + 1, z + 1)], fill=sh(1.0))
        if (x + 1, y, z) not in bl:
            d.polygon([rzut(x + 1, y, z), rzut(x + 1, y + 1, z), rzut(x + 1, y + 1, z + 1), rzut(x + 1, y, z + 1)], fill=sh(0.72))
        if (x, y, z + 1) not in bl:
            d.polygon([rzut(x, y, z + 1), rzut(x + 1, y, z + 1), rzut(x + 1, y + 1, z + 1), rzut(x, y + 1, z + 1)], fill=sh(0.55))
    return im


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('out'); ap.add_argument('pliki', nargs='+')
    ap.add_argument('--kolumny', type=int, default=6); ap.add_argument('--kafel', type=int, default=220)
    a = ap.parse_args(argv)
    kaf = []
    for p in a.pliki:
        try:
            b = wczytaj(p)
        except Exception as e:
            print('BLAD', p, e, file=sys.stderr); continue
        xs = [q[0] for q in b]; ys = [q[1] for q in b]; zs = [q[2] for q in b]
        t = Image.new('RGBA', (a.kafel, a.kafel + 16), (58, 62, 70, 255))
        t.alpha_composite(renderuj(b, a.kafel), (0, 0))
        ImageDraw.Draw(t).text((4, a.kafel + 2), f"{os.path.basename(p).rsplit('.', 1)[0]} {max(xs)-min(xs)+1}x{max(ys)-min(ys)+1}x{max(zs)-min(zs)+1}",
                                fill=(235, 235, 235, 255))
        kaf.append(t)
    if not kaf:
        return 1
    k = min(a.kolumny, len(kaf)); rz = math.ceil(len(kaf) / k)
    ark = Image.new('RGB', (a.kafel * k, (a.kafel + 16) * rz), (40, 42, 48))
    for i, t in enumerate(kaf):
        ark.paste(t, ((i % k) * a.kafel, (i // k) * (a.kafel + 16)), t)
    ark.save(a.out); print(a.out, len(kaf))


if __name__ == '__main__':
    sys.exit(main())
