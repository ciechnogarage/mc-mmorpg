#!/usr/bin/env python3
"""Rysuje komplet 15 ikon zaklęć 16×16 i arkusz do ich oceny."""

from pathlib import Path
from shutil import copyfile

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "nowe"
BG = "#2e1c12"

# Paleta lecznicza jest oparta na stare/ikona_leczenie.png.
DEEP = (40, 92, 70, 255)
SHADOW = (52, 127, 88, 255)
MID = (104, 199, 129, 255)
LIGHT = (160, 229, 161, 255)
PALE = (213, 245, 183, 255)
WHITE = (247, 255, 225, 255)

FIRE_TO_HEAL = {
    (125, 59, 36, 255): DEEP,
    (141, 73, 51, 255): SHADOW,
    (184, 61, 55, 255): SHADOW,
    (255, 161, 90, 255): MID,
    (255, 192, 107, 255): LIGHT,
    (255, 224, 135, 255): PALE,
    (255, 245, 212, 255): WHITE,
    (213, 155, 105, 255): MID,
    (243, 202, 138, 255): LIGHT,
    (200, 151, 109, 255): SHADOW,
}

COPIES = {
    "plomienie": "stare",
    "odmrozenie": "stare",
    "ognisty_pocisk": "stare",
    "kula_ognia": "stare",
    "oslona": "stare",
    "przywolany_miecz": "stare",
    "atronach_ognia": "stare",
    "duch_wilka": "wzory",
}


def source(folder, name):
    return Image.open(ROOT / folder / f"ikona_{name}.png").convert("RGBA")


def recolor(original):
    result = original.copy()
    result.putdata([FIRE_TO_HEAL.get(original.getpixel((x, y)), original.getpixel((x, y)))
                    for y in range(16) for x in range(16)])
    return result


def healing_hand():
    original = source("wzory", "plomienie")
    image = recolor(original)
    # Wierzch i kontur dłoni pozostają piksel w piksel jak we wzorze.
    hand_end = {9: 7, 10: 8, 11: 7, 12: 8, 13: 9, 14: 6}
    for y, end in hand_end.items():
        for x in range(1, end + 1):
            pixel = original.getpixel((x, y))
            if pixel in {(255, 245, 212, 255), (255, 224, 135, 255)}:
                pixel = (243, 202, 138, 255)
            image.putpixel((x, y), pixel)
    # Jasny znak na końcu strumienia; wtopiony w jego istniejącą sylwetkę.
    for x, y in [(12, 4), (11, 5), (12, 5), (13, 5), (12, 6)]:
        image.putpixel((x, y), WHITE)
    return image


def healing_comet():
    image = recolor(source("wzory", "ognisty_pocisk"))
    # Głowa komety otrzymuje dwa płaty i pojedynczy dolny czubek.
    image.putpixel((11, 1), (0, 0, 0, 0))
    image.putpixel((13, 1), (0, 0, 0, 0))
    image.putpixel((11, 2), DEEP)
    for x, y in [(10, 2), (12, 2), (9, 3), (10, 3), (11, 3),
                 (12, 3), (13, 3), (10, 4), (11, 4), (12, 4), (11, 5)]:
        image.putpixel((x, y), PALE)
    for x, y in [(10, 2), (12, 2), (10, 3), (11, 3), (12, 3), (11, 4)]:
        image.putpixel((x, y), WHITE)
    return image


def healing_burst():
    image = recolor(source("wzory", "kula_ognia"))
    # Plus jest czytelny także przy rzeczywistych 16 pikselach.
    draw = ImageDraw.Draw(image)
    draw.rectangle((5, 5, 10, 10), fill=PALE)
    draw.rectangle((7, 5, 8, 10), fill=WHITE)
    draw.rectangle((5, 7, 10, 8), fill=WHITE)
    return image


def heart():
    image = source("stare", "leczenie")
    draw = ImageDraw.Draw(image)
    # Światło w rdzeniu i trzy małe iskry przy bokach oraz pod sercem.
    draw.point((7, 7), fill=WHITE)
    draw.point((8, 7), fill=WHITE)
    draw.point((7, 8), fill=PALE)
    for x, y in [(0, 5), (0, 6), (15, 9), (15, 10), (4, 15), (5, 15)]:
        draw.point((x, y), fill=PALE if x != 15 else LIGHT)
    return image


def bandaged_heart(base):
    image = base.copy()
    draw = ImageDraw.Draw(image)
    border = (115, 82, 60, 255)
    beige = (244, 211, 158, 255)
    cream = (255, 237, 190, 255)
    dot = (170, 133, 94, 255)
    # Jeden ukośny plaster, szeroki na 2 px, z dwoma otworami.
    draw.line((3, 11, 11, 3), fill=border, width=4)
    draw.line((3, 11, 11, 3), fill=beige, width=2)
    draw.point((6, 8), fill=cream)
    draw.point((8, 6), fill=cream)
    draw.point((5, 9), fill=dot)
    draw.point((9, 5), fill=dot)
    return image


def rune():
    image = source("stare", "runa_ognia")
    draw = ImageDraw.Draw(image)
    dark = (67, 42, 38, 255)
    edge = (125, 59, 36, 255)
    gold = (255, 224, 135, 255)
    shine = (255, 245, 212, 255)
    # Okrąg ze starej ikony zostaje; wnętrze zmienia się w ciemny kamień.
    draw.ellipse((4, 4, 11, 11), fill=dark, outline=edge)
    # Trójkąt i krótki trzon tworzą znak strzałki do góry.
    draw.polygon([(7, 4), (8, 4), (11, 8), (9, 8), (9, 11),
                  (6, 11), (6, 8), (4, 8)], fill=gold)
    draw.point((7, 5), fill=shine)
    draw.point((8, 5), fill=shine)
    draw.line((7, 8, 7, 10), fill=shine)
    return image


def calm():
    image = Image.new("RGBA", (16, 16))
    mask = Image.new("1", (16, 16))
    m = ImageDraw.Draw(mask)
    # Zajmujące większość pola Z, z kreską co najmniej 2 px.
    m.rectangle((2, 2, 8, 3), fill=1)
    m.line((7, 3, 3, 9), fill=1, width=2)
    m.rectangle((2, 9, 8, 10), fill=1)
    # Mniejsze z w prawym dolnym rogu.
    m.rectangle((11, 9, 14, 10), fill=1)
    m.line((13, 10, 11, 12), fill=1, width=2)
    m.rectangle((11, 12, 14, 13), fill=1)
    # Jeden piksel ciemnego obrysu wokół obu liter.
    outline = (49, 75, 94, 255)
    lavender = (195, 174, 239, 255)
    turquoise = (172, 236, 229, 255)
    pixels = mask.load()
    for y in range(16):
        for x in range(16):
            if pixels[x, y]:
                for oy in range(max(0, y - 1), min(16, y + 2)):
                    for ox in range(max(0, x - 1), min(16, x + 2)):
                        image.putpixel((ox, oy), outline)
    for y in range(16):
        for x in range(16):
            if pixels[x, y]:
                image.putpixel((x, y), turquoise if y < 5 or x > 10 else lavender)
    return image


def preview(names):
    # Ten sam arkusz pokazuje faktyczne 16×16 i powiększenie bez interpolacji.
    font = ImageFont.load_default()
    canvas = Image.new("RGB", (1030, 670), BG)
    draw = ImageDraw.Draw(canvas)

    def cell(name, x, y, scale):
        icon = Image.open(OUT / f"ikona_{name}.png").convert("RGBA")
        icon = icon.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST)
        canvas.paste(icon, (x, y + 17), icon)
        draw.text((x, y), name.replace("_", " "), fill="#f1ddc7", font=font)

    pairs = [("plomienie", "leczenie_dloni"),
             ("ognisty_pocisk", "uzdrowienie_innego"),
             ("kula_ognia", "wielkie_uzdrowienie")]
    draw.text((12, 7), "OGIEN  |  LECZENIE", fill="#f1ddc7", font=font)
    for row, pair in enumerate(pairs):
        y = 30 + row * 104
        for col, name in enumerate(pair):
            x = 12 + col * 215
            cell(name, x, y, 1)
            cell(name, x + 132, y, 4)

    other = [name for name in names if name not in {item for pair in pairs for item in pair}]
    draw.text((12, 352), "POZOSTALE IKONY", fill="#f1ddc7", font=font)
    for i, name in enumerate(other):
        row, col = divmod(i, 3)
        x, y = 12 + col * 335, 378 + row * 94
        cell(name, x, y, 1)
        cell(name, x + 180, y, 4)
    canvas.save(ROOT / "podglad.png")


def main():
    OUT.mkdir(exist_ok=True)
    for name, folder in COPIES.items():
        copyfile(ROOT / folder / f"ikona_{name}.png", OUT / f"ikona_{name}.png")
    base_heart = heart()
    made = {
        "leczenie_dloni": healing_hand(),
        "uzdrowienie_innego": healing_comet(),
        "wielkie_uzdrowienie": healing_burst(),
        "leczenie": base_heart,
        "zasklepienie_ran": bandaged_heart(base_heart),
        "runa_ognia": rune(),
        "uspokojenie": calm(),
    }
    for name, image in made.items():
        image.save(OUT / f"ikona_{name}.png")
    preview(list(COPIES) + list(made))


if __name__ == "__main__":
    main()
