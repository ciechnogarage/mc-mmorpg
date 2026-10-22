#!/usr/bin/env python3
"""Odtworz szesciopolowa szyna z oryginalnego inventory.png.

Uzycie: python3 inventory_akcesoria6.py BAZOWY_PNG WYNIK_PNG
Bazowy PNG to wersja z dziewiecioma zlotymi slotami (bez pliku .bak obok).
"""
import hashlib
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image


BASE_SHA256 = "f780e9f2b4d0d76c0486ea5d567c553fcfdfb94b4dbcbaf8a23f0896d86dcf7e"
ROW_BOX = (111, 78, 173, 101)  # tylko zmieniany fragment gornego rzedu
COLS = (8, 26, 44, 62, 80, 98, 116, 134, 152)
RIGHT_RIVET = (111, 90)


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    base_path, out_path = map(Path, sys.argv[1:])
    if hashlib.sha256(base_path.read_bytes()).hexdigest() != BASE_SHA256:
        raise SystemExit("Bazowy PNG nie jest oryginalna wersja z 9 slotami")

    with tempfile.TemporaryDirectory() as temp_dir:
        generated_path = Path(temp_dir) / "inventory.png"
        subprocess.run(
            [sys.executable, str(Path(__file__).with_name("generate_inventory_bg.py")),
             str(generated_path)],
            check=True,
            capture_output=True,
        )
        with Image.open(base_path) as source, Image.open(generated_path) as generated:
            base = source.convert("RGBA")
            result = base.copy()
            result.paste(generated.crop(ROW_BOX), ROW_BOX[:2])

    assert result.size == (256, 256)
    assert set(result.get_flattened_data()) <= set(base.get_flattened_data()), \
        "Nowy kolor poza paleta bazowa"
    for x in COLS:
        for y in range(84, 100):
            for xx in range(x, x + 16):
                if (RIGHT_RIVET[0] <= xx < RIGHT_RIVET[0] + 2 and
                        RIGHT_RIVET[1] <= y < RIGHT_RIVET[1] + 2):
                    continue  # nit prawego konca naklada sie na szosta wneke
                assert result.getpixel((xx, y)) == base.getpixel((xx, y)), \
                    "Przesuniete lub zmienione wnetrze slotu"

    for dy in range(2):
        for dx in range(2):
            assert result.getpixel((RIGHT_RIVET[0] + dx, RIGHT_RIVET[1] + dy)) == \
                base.getpixel((169 + dx, 90 + dy)), "Nit rozny od oryginalu"
            assert result.getpixel((112 + dx, 81 + dy)) == \
                result.getpixel((5 + dx, 81 + dy)), "Pozostal nit w rogu"
    assert result.getpixel((114, 78)) == result.getpixel((3, 78)), \
        "Gorny prawy naroznik nie jest jasny"

    changed = [(x, y) for y in range(256) for x in range(256)
               if result.getpixel((x, y)) != base.getpixel((x, y))]
    assert changed and all(ROW_BOX[0] <= x < ROW_BOX[2] and
                           ROW_BOX[1] <= y < ROW_BOX[3] for x, y in changed)
    result.save(out_path)
    print(f"{len(changed)} zmienionych pikseli; bbox: "
          f"{(min(x for x, y in changed), min(y for x, y in changed),
              max(x for x, y in changed) + 1, max(y for x, y in changed) + 1)}")


if __name__ == "__main__":
    main()
