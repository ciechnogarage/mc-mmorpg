#!/usr/bin/env python3
"""Arkusze kontrolne stron w rozdzielczości GUI (NEAREST)."""
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
GROUPS = (
    ('out', 'hub_postac'),
    ('siatka/out', 'wiara'),
    ('poczta-dziennik/out', 'dziennik_przygod'),
    ('mmocore/out', 'player-stats'),
    ('chowance/out', 'chowance_menazeria'),
    ('handel/out/gen', 'sklep'),
    ('warsztaty/out', 'profession-smithing'),
)


def pages(folder):
    return sorted((HERE / folder).glob('*/*_2x.png'))


def half(path):
    image = Image.open(path).convert('RGB')
    assert image.size == (352, 444), path
    return image.resize((176, 222), Image.Resampling.NEAREST)


def sheet(paths, columns):
    rows = (len(paths) + columns - 1) // columns
    result = Image.new('RGB', (176 * columns, 222 * rows), '#3a2620')
    for index, path in enumerate(paths):
        result.paste(half(path), ((index % columns) * 176,
                                  (index // columns) * 222))
    return result


def main():
    k12 = ROOT / 'k12/ksiega_2x.png'
    samples = [next(path for path in pages(folder)
                    if path.stem == f'{ident}_2x')
               for folder, ident in GROUPS]
    sheet([k12, *samples], 4).save(HERE / 'arkusz_menu.png')
    all_pages = [path for folder, _ in GROUPS for path in pages(folder)]
    assert len(all_pages) == 50, len(all_pages)
    sheet(all_pages, 8).save(HERE / 'arkusz_wszystkie.png')
    print(f'Arkusz menu: 8 obrazów; wszystkie: {len(all_pages)} stron')


if __name__ == '__main__':
    main()
