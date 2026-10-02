#!/usr/bin/env python3
"""Rysuje teksture InventoryScreen 176x166 w skali 2 i dwa podglady."""

from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw


HERE = Path(__file__).resolve().parent
ICONS = HERE.parent / "ikony"
ITEMS = Path("/opt/docker/mc/data/world1/plugins/Nexo/pack/assets")
S = 2

# Szesnascie plaskich barw, bez szumu i gradientow.
C = {
    "cover": "#3C2A1E",
    "cover_light": "#6A4B32",
    "cover_dark": "#271A13",
    "left": "#D6C4A0",
    "right": "#DECDAA",
    "paper_edge": "#BCA77F",
    "paper_light": "#E9DBB8",
    "spine": "#785537",
    "spine_dark": "#4D3525",
    "bag": "#5A402C",
    "bag_light": "#866448",
    "bag_dark": "#362619",
    "paper_slot": "#C4AF89",
    "paper_shadow": "#A58D68",
    "bag_slot": "#473220",
    "bag_slot_shadow": "#2E1F16",
}


def box(draw, x0, y0, x1, y1, color):
    draw.rectangle((x0, y0, x1, y1), fill=C.get(color, color))


def line(draw, points, color, width=1):
    draw.line(points, fill=C.get(color, color), width=width)


def frame(draw, mask_draw, bounds, width=2):
    """Jednolity obrys bez osobnych linii swiatla na stykach."""
    draw.rectangle(bounds, outline=C["spine_dark"], width=width)
    mask_draw.rectangle(bounds, outline=1, width=width)


def active_hands_frame(draw, mask_draw):
    """Jeden trzytekselowy obrys schodka obejmujacy obie aktywne rece."""
    segments = (
        (150, 118, 189, 120), (150, 118, 152, 163),
        (187, 118, 189, 163), (187, 161, 197, 163),
        (195, 161, 197, 205), (159, 203, 197, 205),
        (159, 161, 161, 205), (150, 161, 161, 163),
    )
    for bounds in segments:
        box(draw, *bounds, "spine_dark")
        mask_draw.rectangle(bounds, fill=1)


def frame_spurs(im, frame_texels):
    """Wykrywa konce linii; naroza grubszych ramek maja blok 2x2."""
    pixels = im.load()
    result = []
    for x, y in sorted(frame_texels):
        color = pixels[x, y]
        same = lambda px, py: ((px, py) in frame_texels
                               and pixels[px, py] == color)
        linear = ((same(x-1, y) and same(x+1, y))
                  or (same(x, y-1) and same(x, y+1)))
        corner = any(same(x+dx, y) and same(x, y+dy)
                     and same(x+dx, y+dy)
                     for dx in (-1, 1) for dy in (-1, 1))
        if not (linear or corner):
            result.append((x, y))
    return result


def paper_slot(draw, gx, gy):
    """16x16 GUI wewnatrz ramki x-1..x+16, y-1..y+16."""
    x, y = gx * S, gy * S
    x0, y0, x1, y1 = x - 2, y - 2, x + 33, y + 33
    box(draw, x0, y0, x1, y1, "paper_shadow")
    box(draw, x - 1, y - 1, x + 32, y + 32, "paper_light")
    box(draw, x, y, x + 31, y + 31, "paper_slot")
    line(draw, [(x, y + 31), (x, y), (x + 31, y)], "paper_shadow")
    line(draw, [(x + 1, y + 31), (x + 31, y + 31),
                (x + 31, y + 1)], "paper_light")


def leather_slot(draw, gx, gy):
    """Wglebienie bez osobnej ramy wokol kazdego slotu."""
    x, y = gx * S, gy * S
    box(draw, x, y, x + 31, y + 31, "bag_slot")
    line(draw, [(x, y + 30), (x, y), (x + 30, y)], "bag_slot_shadow")
    line(draw, [(x + 1, y + 31), (x + 31, y + 31),
                (x + 31, y + 1)], "bag_light")


def stitched_frame(draw, gx0, gy0, gx1, gy1):
    """Cienki rowek i rzadkie, rowne szwy grupy paska."""
    x0, y0, x1, y1 = gx0 * S, gy0 * S, gx1 * S + 1, gy1 * S + 1
    draw.rounded_rectangle((x0, y0, x1, y1), radius=4,
                           outline=C["bag_dark"], width=2)
    for x in range(x0 + 8, x1 - 6, 16):
        box(draw, x, y0 + 1, x + 3, y0 + 1, "bag_light")
        box(draw, x, y1 - 1, x + 3, y1 - 1, "bag_light")
    for y in range(y0 + 8, y1 - 6, 16):
        box(draw, x0 + 1, y, x0 + 1, y + 3, "bag_light")
        box(draw, x1 - 1, y, x1 - 1, y + 3, "bag_light")


def draw_background(with_frame_texels=False):
    im = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    frame_mask = Image.new("1", im.size)
    mask_draw = ImageDraw.Draw(frame_mask)

    # Ksiega: lekko sciete rogi, szeroki ciemnobrazowy rant.
    d.polygon([(8, 0), (343, 0), (348, 3), (351, 8),
               (351, 323), (348, 328), (343, 331), (8, 331),
               (3, 328), (0, 323), (0, 8), (3, 3)], fill=C["cover_dark"])
    d.polygon([(9, 3), (342, 3), (347, 7), (348, 9),
               (348, 322), (344, 327), (9, 328), (4, 323),
               (4, 9)], fill=C["cover"])
    line(d, [(9, 4), (342, 4)], "cover_light", 2)
    line(d, [(5, 10), (5, 321)], "cover_light", 2)
    line(d, [(346, 10), (346, 322)], "cover_dark", 2)
    line(d, [(10, 326), (343, 326)], "cover_dark", 2)

    # Dwie osobne kartki, widoczne warstwy tylko przy dolnych krawedziach.
    d.rounded_rectangle((8, 8, 193, 202), radius=4, fill=C["paper_edge"])
    d.rounded_rectangle((10, 10, 191, 198), radius=3, fill=C["left"])
    d.rounded_rectangle((195, 8, 344, 202), radius=4, fill=C["paper_edge"])
    d.rounded_rectangle((198, 10, 342, 198), radius=3, fill=C["right"])
    line(d, [(15, 198), (186, 198)], "paper_light")
    line(d, [(15, 200), (186, 200)], "paper_edge")
    line(d, [(202, 198), (337, 198)], "paper_light")
    line(d, [(202, 200), (337, 200)], "paper_edge")

    # Plaski pionowy grzbiet, konczy sie przed dolna kieszenia.
    box(d, 187, 8, 200, 161, "spine")
    box(d, 187, 8, 190, 161, "spine_dark")
    box(d, 196, 8, 198, 161, "cover_light")
    line(d, [(191, 10), (191, 160)], "bag_light")

    # Torba zajmuje dol. Jej gorna krawedz przykrywa koniec kartek.
    d.rounded_rectangle((8, 198, 344, 325), radius=5, fill=C["bag_dark"])
    d.rounded_rectangle((10, 201, 342, 323), radius=4, fill=C["bag"])
    line(d, [(13, 202), (338, 202)], "bag_light", 2)
    line(d, [(12, 204), (12, 318)], "bag_light")
    line(d, [(17, 323), (337, 323)], "bag_dark", 2)

    # Cztery sloty y=84 sa kieszenia torby wsunieta w prawa strone.
    d.rounded_rectangle((192, 162, 341, 203), radius=4,
                        fill=C["bag_dark"])
    d.rounded_rectangle((194, 165, 339, 201), radius=3,
                        fill=C["bag"])
    line(d, [(198, 165), (336, 165)], "bag_light")

    # Okno modelu gracza: jasne, plaskie, z delikatna krawedzia.
    box(d, 51, 16, 151, 157, "spine_dark")
    box(d, 53, 18, 149, 155, "right")
    line(d, [(53, 154), (53, 18), (148, 18)], "paper_light")
    line(d, [(54, 155), (149, 155), (149, 19)], "paper_shadow")

    for y in (8, 26, 44, 62):
        paper_slot(d, 8, y)
    paper_slot(d, 77, 62)
    for x in (8, 26, 44, 62, 80):
        paper_slot(d, x, 84)
    for x in (98, 116):
        for y in (18, 36):
            paper_slot(d, x, y)
    paper_slot(d, 154, 28)

    for x in (98, 116, 134, 152):
        leather_slot(d, x, 84)
    for y in (102, 120):
        for i in range(9):
            leather_slot(d, 8 + 18 * i, y)

    # Pasek jest czescia tej samej torby; grupy zaznaczaja tylko szwy.
    line(d, [(17, 278), (335, 278)], "bag_dark", 2)
    line(d, [(17, 280), (335, 280)], "bag_light")
    for bounds in ((6, 140, 43, 159), (42, 140, 97, 159),
                   (150, 140, 169, 159)):
        stitched_frame(d, *bounds)
    for i in range(9):
        leather_slot(d, 8 + 18 * i, 142)

    # Dwa jasne teksele oddzielaja zapasowa lewa reke od aktywnej prawej.
    box(d, 157, 164, 158, 201, "left")
    box(d, 157, 202, 158, 202, "bag_dark")

    # Obrysy na koncu: sloty nie wycinaja z nich pojedynczych tekseli.
    for bounds in (
        (12, 12, 52, 159),   # zbroja, wspolna lewa linia z oknem postaci
        (51, 16, 151, 157), # okno postaci
        (12, 164, 86, 203), # rekawice i pas
        (85, 164, 156, 203),# zapas, przerwa dwutekselowa przed aktywna para
        (192, 32, 269, 109),# bizuteria
        (304, 52, 343, 95),# sakwa
    ):
        frame(d, mask_draw, bounds)
    active_hands_frame(d, mask_draw)

    if with_frame_texels:
        frame_texels = {(x, y) for y in range(206) for x in range(344)
                        if frame_mask.getpixel((x, y))}
        return im, frame_texels
    return im


def add_icon(preview, name, gx, gy):
    source = ICONS / name
    icon = Image.open(source).convert("RGBA")
    if icon.size != (16, 16):
        raise ValueError(f"Niepoprawny rozmiar ikony: {source}")
    icon = icon.resize((32, 32), Image.Resampling.NEAREST)
    preview.alpha_composite(icon, (gx * S, gy * S))


def add_item(preview, name, gx, gy):
    """Przykladowy przedmiot, tylko w podgladzie; atlas pozostaje pusty."""
    sources = {
        "sword": ITEMS / "mapa/textures/item/ikona/iron_sword.png",
        "ingot": ITEMS / "mapa/textures/item/ikona/iron_ingot.png",
        "stone": ITEMS / "nexo/textures/item/profesje/rough_stone.png",
        "cut_stone": ITEMS / "nexo/textures/item/profesje/cut_stone.png",
    }
    if name in sources:
        icon = Image.open(sources[name]).convert("RGBA")
        if icon.size != (16, 16):
            raise ValueError(f"Niepoprawny rozmiar przedmiotu: {sources[name]}")
    else:
        icon = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(icon)
        if name == "pouch":
            d.polygon([(5, 2), (10, 2), (11, 4), (10, 6), (13, 9),
                       (12, 13), (9, 15), (5, 15), (2, 12), (2, 9),
                       (5, 6), (4, 4)], fill="#674126", outline="#2E1F16")
            d.rectangle((5, 4, 10, 5), fill="#B88A4C")
            d.line([(4, 9), (4, 12), (6, 13)], fill="#A9773E")
            d.point((9, 9), fill="#D7B76C")
        elif name == "shield":
            d.polygon([(4, 1), (11, 1), (13, 3), (12, 10), (8, 15),
                       (3, 11), (2, 3)], fill="#AC8454", outline="#302A25")
            d.polygon([(5, 3), (10, 3), (11, 9), (8, 12), (5, 9)],
                      fill="#CDB38A")
            d.line([(7, 3), (7, 11)], fill="#77746A", width=2)
            d.point((8, 7), fill="#E4D2AF")
        elif name == "bread":
            d.ellipse((2, 5, 13, 13), fill="#9A5C2E", outline="#4A321F")
            d.ellipse((3, 4, 12, 11), fill="#D6A45B")
            d.line([(5, 5), (4, 8)], fill="#EEC985")
            d.line([(8, 4), (7, 8)], fill="#EEC985")
            d.line([(11, 5), (10, 8)], fill="#EEC985")
        elif name == "planks":
            d.rectangle((2, 3, 13, 12), fill="#9A683B", outline="#392719")
            d.line([(3, 6), (12, 6)], fill="#C18B50")
            d.line([(3, 9), (12, 9)], fill="#C18B50")
            d.line([(7, 4), (7, 5)], fill="#52361F")
            d.line([(10, 7), (10, 8)], fill="#52361F")
        elif name == "gem":
            d.polygon([(5, 2), (10, 2), (14, 6), (8, 14), (2, 6)],
                      fill="#4A9E9B", outline="#234B53")
            d.polygon([(5, 3), (10, 3), (8, 11)], fill="#9AD8CB")
            d.line([(3, 6), (13, 6)], fill="#D5E9D6")
        else:
            raise ValueError(f"Nieznany przedmiot: {name}")
    preview.alpha_composite(icon.resize((32, 32), Image.Resampling.NEAREST),
                            (gx * S, gy * S))


def make_items_preview(background):
    preview = background.crop((0, 0, 352, 332)).copy()
    icons = [
        ("vanilla_slot/helmet.png", 8, 8),
        ("vanilla_slot/chestplate.png", 8, 26),
        ("vanilla_slot/leggings.png", 8, 44),
        ("vanilla_slot/boots.png", 8, 62),
        ("left_hand.png", 77, 62),
        ("gloves.png", 8, 84), ("belt.png", 26, 84),
        ("spare_right.png", 44, 84), ("spare_left.png", 62, 84),
        ("right_hand.png", 80, 84),
        ("necklace.png", 98, 18), ("ring.png", 116, 18),
        ("ring.png", 98, 36), ("charm.png", 116, 36),
    ]
    for name, x, y in icons:
        add_icon(preview, name, x, y)
    for name, x, y in (
        ("pouch", 154, 28),
        ("ingot", 98, 84), ("stone", 116, 84),
        ("planks", 134, 84), ("gem", 152, 84),
        ("cut_stone", 26, 102), ("ingot", 62, 102),
        ("planks", 98, 102), ("stone", 134, 102),
        ("sword", 8, 142), ("shield", 26, 142),
        ("bread", 80, 142),
    ):
        add_item(preview, name, x, y)
    add_icon(preview, "health_potion.png", 44, 142)
    add_icon(preview, "mana_potion.png", 62, 142)
    return preview.resize((704, 664), Image.Resampling.NEAREST)


def orphan_count(im):
    """Zlicza jednopeskelowe wyspy barwy (sasiedztwo 8-kierunkowe)."""
    pixels = im.load()
    found = []
    for y in range(332):
        for x in range(352):
            color = pixels[x, y]
            if color[3] == 0:
                continue
            if all(not (0 <= x + dx < 352 and 0 <= y + dy < 332)
                   or pixels[x + dx, y + dy] != color
                   for dy in (-1, 0, 1) for dx in (-1, 0, 1)
                   if dx or dy):
                found.append((x, y))
    return found


def remove_orphans(im):
    """Wyrzuca pojedyncze artefakty z lukow przed zapisem atlasu."""
    for _ in range(3):
        singles = orphan_count(im)
        if not singles:
            break
        pixels = im.load()
        changes = []
        for x, y in singles:
            neighbors = [pixels[x + dx, y + dy]
                         for dy in (-1, 0, 1) for dx in (-1, 0, 1)
                         if (dx or dy) and 0 <= x + dx < 352
                         and 0 <= y + dy < 332 and pixels[x + dx, y + dy][3]]
            if neighbors:
                changes.append((x, y, Counter(neighbors).most_common(1)[0][0]))
        for x, y, color in changes:
            pixels[x, y] = color


def validate(im):
    assert im.size == (512, 512)
    assert im.crop((352, 0, 512, 512)).getbbox() is None
    assert im.crop((0, 332, 352, 512)).getbbox() is None
    colors = {im.getpixel((x, y)) for y in range(332) for x in range(352)
              if im.getpixel((x, y))[3]}
    assert len(colors) <= 16, f"Za duzo barw: {len(colors)}"
    p = im.load()
    paper_slots = ([(8, y) for y in (8, 26, 44, 62)]
                   + [(77, 62), (154, 28)]
                   + [(x, 84) for x in (8, 26, 44, 62, 80)]
                   + [(x, y) for x in (98, 116) for y in (18, 36)])
    bag_slots = ([(x, 84) for x in (98, 116, 134, 152)]
                 + [(8 + 18 * i, y) for y in (102, 120, 142)
                    for i in range(9)])
    for x, y in paper_slots:
        assert p[2*x + 16, 2*y + 16][:3] == tuple(
            bytes.fromhex(C["paper_slot"][1:]))
    for x, y in bag_slots:
        assert p[2*x + 16, 2*y + 16][:3] == tuple(
            bytes.fromhex(C["bag_slot"][1:]))
    for y in range(61*S, 79*S):
        for x in range(104*S, 124*S):
            assert p[x, y][:3] == tuple(bytes.fromhex(C["right"][1:])), \
                "Pole sprite'u ksiazki ma dekoracje"
    singles = orphan_count(im)
    assert not singles, f"Sieroty: {singles[:20]}"
    return len(colors), len(singles)


def main():
    background, frame_texels = draw_background(with_frame_texels=True)
    remove_orphans(background)
    colors, singles = validate(background)
    spurs = frame_spurs(background, frame_texels)
    assert not spurs, f"Zadziory ramek: {spurs[:20]}"
    background.save(HERE / "inventory.png")
    background.crop((0, 0, 352, 332)).resize(
        (704, 664), Image.Resampling.NEAREST).save(HERE / "podglad_x2.png")
    make_items_preview(background).save(HERE / "podglad_z_przedmiotami_x2.png")
    (HERE / "sieroty.txt").write_text(
        f"sieroty: {singles}\nbarwy: {colors}\n"
        "atlas: 512x512; grafika: 352x332; obszar poza grafika: przezroczysty\n",
        encoding="utf-8")
    (HERE / "styki.txt").write_text(
        f"zadziory ramek: {len(spurs)}\n"
        f"sprawdzone teksele ramek: {len(frame_texels)}\n"
        "test: dwa sasiednie teksele tej samej barwy w linii; "
        "naroza jako pelny blok 2x2\n",
        encoding="utf-8")
    print(f"Gotowe: {colors} barw, {singles} sierot, {len(spurs)} zadziorow")


if __name__ == "__main__":
    main()
