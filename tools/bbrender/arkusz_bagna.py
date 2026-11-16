"""Wspólny arkusz: koncept | przód 3/4 | bok 90° | tył 3/4 | oczy gracza.

Użycie: python3 arkusz_bagna.py [--ids pijawka slimak] [--size 300]
Render wywołuje bezpośrednio python3 render_bbmodel.py z WIDOKI w env.
"""
import argparse
import os
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path('/opt/docker/mc/data/world1/output/lochy/bagna')
RENDER = Path(__file__).with_name('render_bbmodel.py')
IDS = ('pijawka', 'mulowiec', 'kosciej', 'szeptucha', 'kokoniara',
       'kokoniatko', 'rechotnik', 'slimak', 'ognik', 'krol_ropuch')
WIDOKI = '145:20,90:10,-35:20,145:5'
PODPISY = ('KONCEPT', 'PRZOD 3/4', 'BOK 90°', 'TYL 3/4', 'OCZY GRACZA · 4 BL.')


def build(mob, root=ROOT, dest=None, size=300):
    root = Path(root)
    model = root / 'modele' / mob / f'bagna_{mob}.bbmodel'
    concept = root / 'szkice' / f'{mob}_{"k2" if mob == "rechotnik" else "k1"}.png'
    dest = Path(dest) if dest else root / 'modele' / mob / 'odbior.png'
    with tempfile.TemporaryDirectory() as temp:
        rendered = Path(temp) / 'render.png'
        env = os.environ.copy()
        env['WIDOKI'] = WIDOKI
        subprocess.run(['python3', str(RENDER), str(rendered), str(model),
                        '--kolumny', '1', '--rozmiar', str(size)],
                       env=env, check=True, capture_output=True, text=True)
        with Image.open(rendered) as im:
            frames = [im.crop((i * size, 0, (i + 1) * size, size)).convert('RGB')
                      for i in range(4)]
    with Image.open(concept) as im:
        concept_im = im.convert('RGB')
    height = size + 54
    sheet = Image.new('RGB', (size * 5, height), '#3a3e46')
    draw = ImageDraw.Draw(sheet)
    for i, (label, source) in enumerate(zip(PODPISY, [concept_im] + frames)):
        source.thumbnail((size - 16, size - 16), Image.Resampling.LANCZOS)
        if i == 4:
            source = source.resize((round(source.width * .65), round(source.height * .65)),
                                   Image.Resampling.LANCZOS)
        x = i * size + (size - source.width) // 2
        y = 32 + (size - source.height) // 2
        sheet.paste(source, (x, y))
        draw.text((i * size + 8, 9), label, fill='#f5f1e8')
        if i:
            draw.line((i * size, 0, i * size, height), fill='#767b80')
    dest.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(dest)
    return dest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ids', nargs='*', default=IDS)
    parser.add_argument('--size', type=int, default=300)
    args = parser.parse_args(argv)
    for mob in args.ids:
        if mob not in IDS:
            parser.error(f'nieznany id: {mob}')
        print(build(mob, size=args.size), flush=True)


if __name__ == '__main__':
    main()
