#!/usr/bin/env python3
"""Dorysuj kafel Otwartej Areny do czterech pasów menu Drużyna."""

from pathlib import Path

from PIL import Image, ImageDraw


HERE = Path(__file__).resolve().parent
SOURCES = HERE / "zrodla"
ECONOMY = Path(
    "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/gui"
)
SIZE = (352, 444)
STRIP_SIZE = (176, 222)
TILE_SOURCE = (230, 105, 302, 178)  # GILDIA: ramka i tabliczka 72 x 73
TILE_TARGET = (193, 177)  # sloty 41, 42, 50, 51

NAVY = (24, 54, 80)
OUTLINE = (37, 28, 43)
CREAM = (255, 241, 204)
PALE = (255, 231, 174)
GOLD = (206, 145, 53)
GOLD_LIGHT = (244, 198, 87)
GOLD_DARK = (140, 83, 44)
TAN = (169, 116, 75)
MAROON = (114, 38, 61)
RED_LIGHT = (184, 61, 76)


def assemble(images):
    canvas = Image.new("RGBA", SIZE)
    for image, position in zip(images, ((0, 0), (176, 0), (0, 222), (176, 222))):
        canvas.alpha_composite(image.convert("RGBA"), position)
    return canvas


def letter(image, x, y):
    """Pobierz dokładnie piksele litery 6 x 10 z podpisu kafla."""
    pixels = image.convert("RGB")
    result = Image.new("RGBA", (6, 10), (0, 0, 0, 0))
    for yy in range(10):
        for xx in range(6):
            if pixels.getpixel((x + xx, y + yy)) == NAVY:
                result.putpixel((xx, yy), (*NAVY, 255))
    if not result.getbbox():
        raise ValueError(f"Nie znaleziono litery: {x}, {y}")
    return result


def arena_symbol():
    """Dwa jasne miecze w X i mała tarcza, wypełniające 80% pola."""
    icon = Image.new("RGB", (40, 28), NAVY)
    draw = ImageDraw.Draw(icon)

    # Kontur mieczy jest ciepłym cieniem ostrza z palety istniejących kafli.
    for start, end, tip in (
        ((4, 3), (28, 19), [(1, 0), (9, 3), (4, 8)]),
        ((35, 3), (11, 19), [(38, 0), (30, 3), (35, 8)]),
    ):
        draw.line((start, end), fill=TAN, width=7)
        draw.line((start, end), fill=PALE, width=5)
        draw.line((start, end), fill=CREAM, width=2)
        draw.polygon(tip, fill=PALE)
        draw.line((tip[0], tip[2]), fill=CREAM, width=2)

    # Złote jelce, rękojeści i głowice pozostają czytelne pod małą tarczą.
    for grip, guard, pommel in (
        (((27, 18), (35, 25)), ((23, 21), (29, 14)), (35, 25)),
        (((12, 18), (4, 25)), ((16, 21), (10, 14)), (4, 25)),
    ):
        draw.line(grip, fill=GOLD_DARK, width=6)
        draw.line(grip, fill=GOLD_LIGHT, width=3)
        draw.line(guard, fill=GOLD_DARK, width=5)
        draw.line(guard, fill=GOLD_LIGHT, width=2)
        draw.ellipse((pommel[0] - 2, pommel[1] - 2,
                      pommel[0] + 2, pommel[1] + 2), fill=GOLD_LIGHT)

    draw.polygon([(17, 9), (22, 9), (26, 12), (25, 19), (20, 24),
                  (14, 19), (13, 12)], fill=MAROON)
    draw.polygon([(17, 10), (22, 10), (25, 12), (24, 18), (20, 22),
                  (15, 18), (14, 12)], fill=GOLD_LIGHT)
    draw.polygon([(17, 12), (22, 12), (23, 13), (22, 18), (20, 20),
                  (17, 18), (16, 13)], fill=RED_LIGHT)
    draw.line([(17, 13), (17, 17), (19, 19)], fill=CREAM, width=1)
    return icon


def main():
    paths = [SOURCES / f"hub_spolecznosc_{i}.png" for i in range(1, 5)]
    originals = [Image.open(path) for path in paths]
    if any(image.size != STRIP_SIZE for image in originals):
        raise ValueError("Każdy pas źródłowy musi mieć 176 x 222 px")
    canvas = assemble(originals)

    # E: podpis SKLEP w hub_ekonomia; pozostałe litery: podpisy GILDIA i DRUŻYNA.
    economy = assemble([Image.open(ECONOMY / f"hub_ekonomia_{i}.png") for i in range(1, 5)])
    letters = {
        "A": letter(canvas, 280, 160),  # GILDIA
        "R": letter(canvas, 212, 88),   # DRUŻYNA
        "E": letter(economy, 308, 88),  # SKLEP
        "N": letter(canvas, 244, 88),   # DRUŻYNA
    }

    tile = canvas.crop(TILE_SOURCE).copy().convert("RGB")
    draw = ImageDraw.Draw(tile)
    draw.rectangle((11, 10, 60, 44), fill=NAVY)  # wnętrze obrazka, bez ramki
    tile.paste(arena_symbol(), (16, 13))
    draw.rectangle((18, 55, 57, 64), fill=CREAM)  # dawny napis GILDIA
    for index, character in enumerate("ARENA"):
        tile.paste(letters[character], (18 + index * 8, 55), letters[character])

    output = canvas.copy()
    output.paste(tile, TILE_TARGET)
    changed = (193, 177, 265, 250)
    for y in range(SIZE[1]):
        for x in range(SIZE[0]):
            if not (changed[0] <= x < changed[2] and changed[1] <= y < changed[3]):
                if output.getpixel((x, y)) != canvas.getpixel((x, y)):
                    raise AssertionError(f"Zmiana poza kaflem: {x}, {y}")

    for index, (original, position) in enumerate(
        zip(originals, ((0, 0), (176, 0), (0, 222), (176, 222))), 1
    ):
        x, y = position
        output.crop((x, y, x + 176, y + 222)).convert(original.mode).save(
            HERE / f"hub_spolecznosc_{index}.png"
        )

    preview = Image.new("RGB", (2656, 1776), OUTLINE)
    preview.paste(output.convert("RGB").resize((1408, 1776), Image.Resampling.NEAREST), (0, 0))
    preview.paste(tile.resize((576, 584), Image.Resampling.NEAREST), (1440, 0))
    preview.paste(canvas.crop(TILE_SOURCE).convert("RGB").resize(
        (576, 584), Image.Resampling.NEAREST), (2048, 0))
    preview.save(HERE / "podglad.png")


if __name__ == "__main__":
    main()
