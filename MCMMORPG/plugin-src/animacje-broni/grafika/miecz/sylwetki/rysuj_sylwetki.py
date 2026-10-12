#!/usr/bin/env python3
"""Rysuje sześć natywnych sylwetek miecza 16x16 i arkusz porównawczy."""

from collections import deque
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont


HERE = Path(__file__).resolve().parent
CLIENT = Path("/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar")
POTION = Path("/opt/docker/mc/data/world1/output/warsztat/czesci/mikstura_v9_styl_v6_b.png")
INK = (28, 12, 24, 255)
BACKGROUNDS = (
    ("JASNE", (220, 220, 220, 255)),
    ("SLOT VANILLA", (139, 139, 139, 255)),
    ("CIEMNE", (30, 30, 36, 255)),
)


def pixels(rows, details=()):
    """rows: y -> (pierwszy x, ostatni x), plus ręcznie dobrane piksele."""
    result = set(details)
    for y, (left, right) in rows.items():
        result.update((x, y) for x in range(left, right + 1))
    return result


# Każdy wzór jest rysowany piksel po pikselu w siatce docelowej.
# Zbiory nie są skalowanymi lub przesuniętymi kopiami tekstury vanilli.
DESIGNS = (
    # 1: prosty szeroki płat i długi krzyż z wyraźnymi końcówkami.
    pixels(
        {0: (9, 10), 1: (8, 11), 2: (7, 11), 3: (6, 10),
         4: (5, 9), 5: (4, 8), 6: (3, 7), 7: (2, 6),
         8: (1, 5), 9: (0, 4), 10: (3, 5), 11: (2, 4),
         12: (1, 3), 13: (0, 2), 14: (0, 2), 15: (0, 2)},
        {(0, 6), (0, 7), (1, 7), (1, 8), (2, 8),
         (5, 10), (6, 10), (6, 11), (7, 11), (7, 12), (8, 12), (8, 13)},
    ),
    # 2: długa wąska klinga, zawinięte ramię jelca i pierścieniowa głowica.
    pixels(
        {0: (10, 11), 1: (9, 12), 2: (8, 11), 3: (7, 10),
         4: (6, 9), 5: (5, 8), 6: (4, 7), 7: (3, 6),
         8: (2, 5), 9: (1, 4), 10: (2, 4), 11: (2, 4),
         12: (1, 3), 13: (0, 2), 14: (0, 2), 15: (0, 2)},
        {(0, 7), (0, 8), (0, 9), (1, 10),
         (5, 9), (5, 10), (6, 11), (7, 11), (7, 12),
         (0, 12), (2, 15)},
    ),
    # 3: szeroki sztych i zwężająca się do rękojeści głownia.
    pixels(
        {0: (9, 15), 1: (7, 15), 2: (6, 14), 3: (5, 12),
         4: (5, 10), 5: (4, 8), 6: (3, 7), 7: (2, 6),
         8: (1, 5), 9: (0, 4), 10: (3, 5), 11: (2, 4),
         12: (1, 3), 13: (0, 2), 14: (0, 2), 15: (0, 1)},
        {(0, 8), (0, 9), (1, 9), (2, 9),
         (6, 10), (7, 10), (7, 11), (8, 11), (8, 12)},
    ),
    # 4: liściasta głownia z maksimum szerokości w środku, okrągły jelec.
    pixels(
        {0: (9, 9), 1: (8, 10), 2: (6, 11), 3: (5, 11),
         4: (4, 10), 5: (3, 9), 6: (2, 8), 7: (2, 7),
         8: (1, 5), 9: (0, 4), 10: (2, 6), 11: (2, 5),
         12: (1, 4), 13: (0, 2), 14: (0, 2), 15: (0, 2)},
        {(0, 8), (0, 9), (1, 9), (1, 10),
         (5, 11), (6, 11), (6, 12), (7, 12)},
    ),
    # 5: krótka, równa klinga, szeroki talerzowy jelec i krótki chwyt.
    pixels(
        {2: (7, 11), 3: (6, 11), 4: (5, 10), 5: (4, 9),
         6: (3, 8), 7: (2, 7), 8: (1, 6), 9: (0, 6),
         10: (0, 8), 11: (1, 8), 12: (2, 6),
         13: (1, 3), 14: (0, 2), 15: (0, 2)},
        {(0, 8), (1, 8), (0, 11), (1, 12)},
    ),
    # 6: skrócona gruba klinga z barkiem, szczątkowy jelec i duża głowica.
    pixels(
        {2: (8, 10), 3: (6, 10), 4: (4, 9), 5: (3, 8),
         6: (2, 7), 7: (1, 6), 8: (0, 5), 9: (0, 4),
         10: (2, 4), 11: (2, 3), 12: (1, 3), 13: (0, 3),
         14: (0, 4), 15: (0, 4)},
        {(1, 9), (1, 10), (5, 10)},
    ),
)


def make_mask(points):
    image = Image.new("RGBA", (16, 16))
    for point in points:
        image.putpixel(point, INK)
    return image


def check_design(points):
    assert all(0 <= x < 16 and 0 <= y < 16 for x, y in points)
    queue = deque([next(iter(points))])
    seen = set(queue)
    while queue:
        x, y = queue.popleft()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p in points and p not in seen:
                    seen.add(p)
                    queue.append(p)
    assert seen == points, f"oderwane piksele: {points - seen}"


def font(size, bold=False):
    stem = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/{stem}", size)


def paste_on_bg(sheet, icon, xy, bg, scale):
    tile = Image.new("RGBA", icon.size, bg)
    tile.alpha_composite(icon)
    if scale != 1:
        tile = tile.resize((icon.width * scale, icon.height * scale), Image.Resampling.NEAREST)
    sheet.alpha_composite(tile, xy)


def create_sheet(designs, vanilla, potion):
    col_w, row_h = 178, 310
    sheet = Image.new("RGBA", (col_w * 8 + 24, row_h * 3 + 58), (245, 245, 245, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((16, 12), "MIECZ · ETAP 1 / SYLWETKI 16×16", font=font(25, True), fill=(22, 22, 26))
    draw.text((16, 39), "1× = natywne 16 px     |     8× = powiększenie bez interpolacji", font=font(13), fill=(50, 50, 54))
    icons = list(designs) + [vanilla, potion]
    for row, (name, bg) in enumerate(BACKGROUNDS):
        y = 58 + row * row_h
        draw.rectangle((0, y, sheet.width - 1, y + row_h - 1), fill=bg)
        ink = (21, 21, 24) if row < 2 else (242, 242, 244)
        draw.text((12, y + 5), name, font=font(16, True), fill=ink)
        for col, icon in enumerate(icons):
            x = 16 + col * col_w
            label = str(col + 1) if col < 6 else ("VANILLA" if col == 6 else "MIKSTURA")
            draw.text((x, y + 29), label, font=font(23 if col < 6 else 17, True), fill=ink)
            draw.text((x + 8, y + 62), "1×", font=font(12, True), fill=ink)
            paste_on_bg(sheet, icon, (x + 36, y + 62), bg, 1)
            paste_on_bg(sheet, icon, (x + 9, y + 85), bg, 8)
            draw.text((x + 80, y + 63), "8×", font=font(12, True), fill=ink)
            if col < 7:
                draw.line((x + col_w - 6, y + 30, x + col_w - 6, y + row_h - 8), fill=ink, width=1)
    sheet.save(HERE / "arkusz_sylwetek.png")


def main():
    with ZipFile(CLIENT) as archive:
        vanilla_color = Image.open(BytesIO(archive.read("assets/minecraft/textures/item/iron_sword.png"))).convert("RGBA")
    assert vanilla_color.size == (16, 16)
    vanilla_points = {(x, y) for y in range(16) for x in range(16) if vanilla_color.getpixel((x, y))[3]}
    vanilla = make_mask(vanilla_points)
    potion = Image.open(POTION).convert("RGBA")
    designs = []
    for index, points in enumerate(DESIGNS, 1):
        check_design(points)
        icon = make_mask(points)
        icon.save(HERE / f"sylwetka_{index}.png")
        designs.append(icon)
        shared = len(points & vanilla_points)
        pct = shared / len(points) * 100
        union = len(points | vanilla_points)
        print(f"{index}: {len(points)} px, wspólne z vanilla {shared}/{len(points)} = {pct:.1f}% kandydatki; IoU {shared / union * 100:.1f}%")
        assert pct < 60, f"sylwetka {index} jest zbyt podobna do vanilli"
    create_sheet(designs, vanilla, potion)


if __name__ == "__main__":
    main()
