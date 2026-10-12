#!/usr/bin/env python3
"""Regenerate the M2 pages and verify text, sheets, and the K12 pixel gate."""
from io import BytesIO
from pathlib import Path
import subprocess
import sys

from PIL import Image, ImageChops

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
GENERATORS = (
    'rysuj_strony.py', 'siatka/siatka.py', 'poczta-dziennik/generuj.py',
    'mmocore/generuj.py', 'chowance/generuj_menu.py', 'handel/generuj.py',
    'warsztaty/warsztaty.py',
)


def run(*args):
    result = subprocess.run(args, cwd=ROOT, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f'{args}:\n{result.stdout[-2000:]}\n{result.stderr[-2000:]}')
    return result.stdout


def main():
    count = 0
    probe = (
        'import contextlib,io,runpy,sys; '
        'sys.path.insert(0,"strony"); import ksiega_wspolne as W; '
        'out=io.StringIO();\n'
        'with contextlib.redirect_stdout(out): '
        'runpy.run_path(sys.argv[1],run_name="__main__")\n'
        'print(W.TEXT_CHECKS)'
    )
    for relative in GENERATORS:
        count += int(run(sys.executable, '-c', probe, str(HERE / relative)).strip())
    run(sys.executable, str(HERE / 'arkusze_menu.py'))

    pages = (list(HERE.glob('out/*/*_2x.png')) +
             list(HERE.glob('*/out/*/*_2x.png')) +
             list(HERE.glob('handel/out/gen/*/*_2x.png')))
    assert len(pages) == 50, len(pages)
    k12_path = ROOT / 'k12/ksiega_2x.png'
    k12 = Image.open(k12_path).convert('RGB')
    prefix = run('git', 'rev-parse', '--show-prefix').strip()
    head = subprocess.check_output(
        ['git', 'show', f'HEAD:{prefix}k12/ksiega_2x.png'], cwd=ROOT)
    assert ImageChops.difference(k12, Image.open(BytesIO(head)).convert('RGB')).getbbox() is None
    for path in pages:
        image = Image.open(path).convert('RGB')
        assert image.size == k12.size, path
        if 'wystawienie' in path.parts:
            continue  # This page intentionally shows the backpack.
        for box in ((0,314,352,444), (0,263,116,314), (236,263,352,314)):
            assert ImageChops.difference(image.crop(box), k12.crop(box)).getbbox() is None, (path,box)

    changed = run('git', 'diff', '--name-only', 'HEAD', '--', 'strony')
    assert not [p for p in changed.splitlines()
                if p.endswith(('.yml', '/sloty.txt'))]
    print(f'0 naruszeń / {count} tekstów; 50 stron; 49 dołów i baner MENU piksel w piksel; YAML/sloty bez zmian')


if __name__ == '__main__':
    main()
