#!/usr/bin/env python3
"""Księga postaci v7: pikselowa tekstura GUI 176×166 w atlasie 256×256.

Geometria 46 slotów pozostaje z v4/v5. Wzorce i ikony służą tylko do podglądu.
"""

from pathlib import Path
import argparse

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
WORLD = Path('/opt/docker/mc/data/world1')
ASSETS = WORLD / 'plugins/Nexo/pack/assets'
EQUIPMENT = ASSETS / 'nexo/textures/item/mmo_equipment'
PROFESSIONS = ASSETS / 'nexo/textures/item/profesje'
WYNN = WORLD / 'docs/research/wynncraft/klatki/17_ekwipunek_E_pergamin_A8m00.jpg'

# Trzynaście nieprzezroczystych barw; malinowa skóra służy tylko zakładce.
C = '#50352D'  # pojedynczy ciemny kontur
L0 = '#614335'  # cień skóry
J = '#6C533F'   # wspólne ciemne tło biżuterii i sakiewki
L1 = '#805D42'  # skóra
L2 = '#A1805D'  # oświetlony rant skóry
P0 = '#AF996D'  # cień pergaminu
P1 = '#CAB486'  # arkusz spod spodu
P2 = '#D7C191'  # płytka wnęka
P3 = '#E5D1A5'  # czysta strona
B0 = '#B79969'  # wnęka torby
B1 = '#C7AB7B'  # materiał torby
B2 = '#BFA776'  # bardzo płytkie pola torby
R = '#A46954'   # skórzana wstążka
PALETTE = (C, L0, J, L1, L2, P0, P1, P2, P3, B0, B1, B2, R)


def rect(draw, xy, color):
    draw.rectangle(xy, fill=color)


def polygon(draw, points, color):
    draw.polygon(points, fill=color)


def slot_positions():
    """Dokładne współrzędne 46 slotów z ksiega_v4.py."""
    armor = [(8, y) for y in (8, 26, 44, 62)]
    jewelry = [(98, 18), (116, 18), (98, 36), (116, 36)]
    special = [(154, 28), (77, 62)]
    inventory = [(x, y) for y in (84, 102, 120)
                 for x in (8, 26, 44, 62, 80, 98, 116, 134, 152)]
    hotbar = [(x, 142) for x in (8, 26, 44, 62, 80, 98, 116, 134, 152)]
    return armor, jewelry, special, inventory, hotbar


def cover(draw):
    # Stopniowane narożniki i pas skóry widoczny wokół obu kart oraz torby.
    polygon(draw, [(5, 0), (170, 0), (170, 1), (173, 1),
                   (173, 4), (175, 4), (175, 160), (173, 160),
                   (173, 163), (170, 163), (170, 165), (5, 165),
                   (5, 163), (2, 163), (2, 160), (0, 160),
                   (0, 5), (2, 5), (2, 2), (5, 2)], C)
    polygon(draw, [(5, 2), (170, 2), (172, 4), (173, 6),
                   (173, 158), (171, 161), (168, 163), (7, 163),
                   (4, 161), (2, 158), (2, 7)], L0)
    polygon(draw, [(6, 3), (168, 3), (171, 5), (172, 8),
                   (172, 157), (169, 161), (7, 161),
                   (4, 158), (4, 8)], L1)
    draw.line([(7, 4), (167, 4)], fill=L2)
    draw.line([(4, 9), (4, 156)], fill=L2)
    draw.line([(8, 162), (167, 162)], fill=L0)


def cover_rim(draw):
    """Gruba skórzana oprawa na wierzchu krawędzi arkuszy."""
    draw.rounded_rectangle((0, 0, 175, 165), radius=3, outline=C)
    draw.rounded_rectangle((1, 1, 174, 164), radius=3, outline=L0)
    draw.rounded_rectangle((2, 2, 173, 163), radius=2, outline=L1)
    draw.rounded_rectangle((3, 3, 172, 162), radius=2, outline=L1)
    draw.line([(7, 4), (168, 4)], fill=L2)
    draw.line([(4, 7), (4, 157)], fill=L2)
    draw.line([(171, 8), (171, 157)], fill=L0)
    draw.line([(8, 161), (167, 161)], fill=L0)
    # Jednopikselowy cień arkuszy na skórze.
    draw.line([(5, 11), (5, 95)], fill=L0)
    draw.line([(170, 12), (170, 73)], fill=L0)


def bookmarks(draw):
    # Trzy miękkie języki nad krawędzią kart, bez metalowych elementów.
    polygon(draw, [(34, 1), (39, 1), (39, 7), (37, 9),
                   (35, 7), (35, 3), (34, 3)], L0)
    rect(draw, (35, 1, 37, 6), L2)
    polygon(draw, [(67, 1), (71, 1), (71, 7), (69, 9),
                   (67, 7)], L1)
    rect(draw, (68, 2, 69, 6), L2)
    polygon(draw, [(161, 0), (167, 0), (167, 8), (164, 10),
                   (161, 8)], L0)
    polygon(draw, [(162, 1), (166, 1), (166, 7), (164, 9),
                   (162, 7)], R)


def pages(draw):
    # Dolne i prawe krawędzie dwóch luźnych arkuszy, przesuniętych o 1 px.
    polygon(draw, [(6, 8), (18, 8), (18, 6), (46, 6), (46, 7),
                   (73, 7), (73, 9), (96, 9), (96, 99),
                   (99, 99), (99, 102), (82, 102), (82, 101),
                   (59, 101), (59, 102), (27, 102), (27, 101),
                   (7, 101), (7, 98), (5, 98), (5, 49),
                   (6, 49)], P0)
    polygon(draw, [(6, 7), (18, 7), (18, 5), (44, 5), (44, 6),
                   (74, 6), (74, 8), (94, 8), (94, 81),
                   (97, 81), (97, 99), (79, 99), (79, 100),
                   (57, 100), (57, 101), (29, 101), (29, 100),
                   (8, 100), (8, 98), (6, 98), (6, 47),
                   (7, 47)], P1)
    polygon(draw, [(8, 8), (20, 8), (20, 6), (42, 6), (42, 7),
                   (73, 7), (73, 9), (93, 9), (93, 80),
                   (96, 80), (96, 98), (78, 98), (78, 99),
                   (56, 99), (56, 100), (30, 100), (30, 99),
                   (9, 99), (9, 97), (7, 97), (7, 48),
                   (8, 48)], P3)
    # Prawa strona ma osobną, lekko nierówną górną krawędź.
    polygon(draw, [(98, 8), (114, 8), (114, 6), (138, 6),
                   (138, 7), (157, 7), (157, 6), (167, 6),
                   (167, 8), (170, 8), (170, 11), (171, 11),
                   (171, 76), (170, 76), (170, 81),
                   (168, 81), (168, 83), (98, 83)], P0)
    polygon(draw, [(97, 7), (115, 7), (115, 5), (137, 5),
                   (137, 6), (156, 6), (156, 5), (166, 5),
                   (166, 7), (169, 7), (169, 10), (170, 10),
                   (170, 75), (169, 75), (169, 80),
                   (167, 80), (167, 81), (97, 81)], P1)
    polygon(draw, [(99, 8), (117, 8), (117, 6), (135, 6),
                   (135, 7), (155, 7), (155, 6), (165, 6),
                   (165, 8), (168, 8), (168, 11), (169, 11),
                   (169, 74), (168, 74), (168, 79),
                   (166, 79), (166, 80), (98, 80)], P3)
    # Dwa pojedyncze podwinięcia na zewnętrznych rogach kart.
    polygon(draw, [(7, 94), (9, 94), (9, 98), (12, 98),
                   (12, 99), (8, 99), (8, 97), (7, 97)], P2)
    polygon(draw, [(165, 75), (169, 74), (169, 78),
                   (166, 78), (166, 80), (163, 80)], P2)
    # Strony łagodnie schodzą w zagłębienie grzbietu; żadnej skórzanej kolumny.
    polygon(draw, [(79, 8), (84, 8), (83, 39), (84, 57),
                   (82, 79), (78, 79)], P2)
    polygon(draw, [(85, 8), (89, 8), (88, 44), (89, 79),
                   (83, 79), (84, 56), (83, 39)], P1)
    polygon(draw, [(90, 8), (92, 8), (92, 79), (89, 79),
                   (88, 44), (89, 25)], P0)
    draw.line([(91, 9), (90, 46), (91, 78)], fill=L2)
    polygon(draw, [(93, 8), (96, 8), (95, 38), (96, 79),
                   (92, 79), (92, 8)], P1)
    rect(draw, (97, 8, 98, 79), P2)
    # Odpryski krawędzi mają długość 1–2 px i nie dotykają ikon.
    for y in (14, 40, 76):
        rect(draw, (7, y, 7, y + 1), P0)
    for y in (19, 53, 68):
        rect(draw, (168, y, 169, y), L1)
    # Okno modelu: jasna cisza za sylwetką, tylko lekka krawędź.
    rect(draw, (25, 8, 75, 78), P0)
    rect(draw, (26, 9, 74, 77), P3)
    rect(draw, (26, 9, 73, 9), P2)
    rect(draw, (26, 10, 26, 76), P2)


def bag(draw):
    # Torba z matowego płótna; pojedyncze wyraźne załamanie klapy.
    polygon(draw, [(98, 81), (170, 81), (170, 139),
                   (168, 141), (7, 141), (5, 139),
                   (5, 101), (98, 101)], P0)
    polygon(draw, [(99, 82), (169, 82), (169, 138),
                   (167, 139), (8, 139), (6, 138),
                   (6, 102), (99, 102)], B1)
    rect(draw, (6, 102, 97, 102), P0)
    rect(draw, (99, 82, 168, 82), P0)
    rect(draw, (7, 139, 168, 140), P0)
    polygon(draw, [(5, 141), (170, 141), (170, 161),
                   (168, 163), (7, 163), (5, 161)], P0)
    rect(draw, (6, 142, 169, 160), B1)
    rect(draw, (7, 142, 168, 142), P0)
    rect(draw, (7, 160, 168, 161), B0)


def groups(draw):
    # Jeden ciemny panel obejmuje biżuterię i pole do sakiewki.
    polygon(draw, [(97, 14), (168, 14), (171, 17), (171, 54),
                   (168, 57), (97, 57), (94, 54), (94, 17)], C)
    polygon(draw, [(97, 15), (168, 15), (170, 17), (170, 54),
                   (168, 56), (97, 56), (95, 54), (95, 17)], L0)
    rect(draw, (97, 16, 168, 55), J)
    draw.line([(97, 16), (168, 16)], fill=L1)
    draw.line([(96, 17), (96, 54)], fill=L1)
    # Dwie czynne ręce tworzą pionową naszywkę mimo uskoku slotu 13.
    polygon(draw, [(77, 57), (94, 57), (97, 60), (97, 80),
                   (100, 83), (100, 100), (97, 103), (78, 103),
                   (75, 100), (75, 81), (73, 79), (73, 61)], P0)
    polygon(draw, [(77, 59), (93, 59), (95, 61), (95, 81),
                   (98, 84), (98, 99), (96, 101), (79, 101),
                   (77, 99), (77, 80), (75, 78), (75, 62)], P1)
    # Zapas leży na jaśniejszym, oddzielnym płacie.
    polygon(draw, [(43, 81), (75, 81), (78, 83), (78, 100),
                   (75, 103), (44, 103), (41, 100), (41, 84)], P1)
    rect(draw, (43, 83, 76, 100), P2)
    # Ta sama forma naszywki pod parą 0+1.
    polygon(draw, [(8, 138), (42, 138), (45, 141), (45, 160),
                   (42, 163), (8, 163), (5, 160), (5, 141)], L2)
    polygon(draw, [(9, 140), (41, 140), (43, 142), (43, 159),
                   (41, 161), (9, 161), (7, 159), (7, 142)], P0)
    # Kieszeń 2–4 ma klapkę i jedno ciemne załamanie u góry.
    polygon(draw, [(46, 137), (94, 137), (98, 141), (98, 161),
                   (95, 164), (45, 164), (42, 161), (42, 141)], L2)
    polygon(draw, [(46, 139), (94, 139), (96, 142), (96, 160),
                   (94, 162), (46, 162), (44, 160), (44, 142)], P1)
    polygon(draw, [(46, 139), (94, 139), (96, 142), (94, 144),
                   (46, 144), (44, 142)], P0)
    rect(draw, (46, 144, 94, 144), L2)
    # Sakiewka pozostawia dwa piksele skóry pomiędzy slotem a prawym rantem.
    polygon(draw, [(157, 23), (165, 23), (167, 26), (170, 28),
                   (172, 32), (172, 41), (170, 45), (166, 49),
                   (156, 49), (152, 46), (150, 42), (150, 32),
                   (152, 28), (155, 26)], L2)
    polygon(draw, [(158, 25), (164, 25), (165, 28), (168, 29),
                   (170, 33), (170, 41), (168, 44), (164, 47),
                   (157, 47), (153, 43), (152, 40), (152, 33),
                   (155, 29), (157, 28)], P1)
    rect(draw, (157, 26, 165, 27), L1)
    draw.line([(153, 31), (151, 35), (151, 40)], fill=P0)
    draw.line([(170, 32), (171, 35), (171, 40)], fill=P0)
    # Blady znak herbowy w jedynej wolnej partii prawej karty.
    draw.line([(134, 61), (145, 61), (145, 70), (139, 76),
               (133, 70), (133, 62)], fill=P2, width=1)
    draw.line([(136, 64), (142, 64), (142, 69), (139, 72),
               (136, 69), (136, 64)], fill=P2, width=1)


def slot(draw, x, y, paper):
    # Widoczne, lecz płytkie zagłębienie bez obrysu dookoła komórki.
    if (x, y) in ((98, 18), (116, 18), (98, 36), (116, 36)):
        rect(draw, (x, y, x + 15, y + 15), L1)
        rect(draw, (x + 1, y + 1, x + 14, y + 14), L2)
        return
    if (x, y) in ((77, 62), (80, 84)):
        recess = P0
    elif y == 142 and x in (8, 26):
        recess = B0
    elif y == 142 and x in (44, 62, 80):
        recess = B0
    elif y == 84 and x in (44, 62):
        recess = P1
    else:
        recess = P2 if paper else B2
    rect(draw, (x, y, x + 15, y + 15), recess)


def make_texture():
    image = Image.new('RGBA', (256, 256))
    d = ImageDraw.Draw(image)
    cover(d)
    pages(d)
    bag(d)
    groups(d)
    armor, jewelry, special, inventory, hotbar = slot_positions()
    positions = armor + jewelry + special + inventory + hotbar
    assert len(positions) == 46 and len(set(positions)) == 46
    paper_slots = set(armor + jewelry + special +
                      [(x, 84) for x in (8, 26, 44, 62, 80)])
    for x, y in positions:
        slot(d, x, y, (x, y) in paper_slots)
    # Te obszary nadpisują ewentualny brzeg podkładki.
    rect(d, (104, 61, 123, 78), P3)  # klient rysuje przycisk książki
    rect(d, (97, 6, 169, 13), P3)    # jasny pasek tytułu (97,6)
    cover_rim(d)
    bookmarks(d)
    assert all(image.getpixel((x, y))[3] == 0
               for y in range(166, 256) for x in range(256))
    assert all(image.getpixel((x, y))[3] == 0
               for y in range(166) for x in range(176, 256))
    colors = {pixel[:3] for pixel in image.get_flattened_data() if pixel[3]}
    assert len(colors) <= 14, colors
    assert colors <= {Image.new('RGB', (1, 1), c).getpixel((0, 0)) for c in PALETTE}
    return image


def gray_figure(image):
    d = ImageDraw.Draw(image)
    shade = '#767672'
    light = '#A5A49D'
    rect(d, (45, 16, 55, 26), shade)
    rect(d, (46, 16, 52, 17), light)
    rect(d, (40, 28, 62, 49), shade)
    rect(d, (41, 28, 56, 29), light)
    rect(d, (35, 30, 39, 54), shade)
    rect(d, (63, 30, 67, 54), shade)
    rect(d, (42, 50, 48, 71), shade)
    rect(d, (54, 50, 60, 71), shade)


def sword_icon():
    # Vanilla-like miecz testowy: 16×16, jeden mocny obrys i lewe światło.
    image = Image.new('RGBA', (16, 16))
    d = ImageDraw.Draw(image)
    polygon(d, [(12, 1), (14, 1), (14, 4), (7, 11), (5, 9)], '#251B1E')
    polygon(d, [(12, 2), (13, 2), (13, 4), (7, 10), (6, 9)], '#849EAB')
    d.line((12, 2, 7, 8), fill='#D6DAD0')
    d.line((4, 8, 9, 13), fill='#251B1E', width=2)
    d.line((4, 8, 8, 12), fill='#8F693F')
    d.line((5, 10, 2, 13), fill='#251B1E', width=2)
    d.point((2, 13), fill='#BE9B5D')
    return image


def _paste_icon(panel, path, x, y):
    icon = Image.open(path).convert('RGBA')
    assert icon.size == (16, 16), path
    panel.alpha_composite(icon, (x, y))


def make_preview(texture):
    panel = texture.copy()
    gray_figure(panel)
    for name, x, y in (
        ('head', 8, 8), ('chest', 8, 26), ('legs', 8, 44),
        ('feet', 8, 62), ('necklace', 98, 18), ('ring', 116, 18),
        ('ring', 98, 36), ('charm', 116, 36),
        ('gloves', 8, 84), ('belt', 26, 84),
    ):
        _paste_icon(panel, EQUIPMENT / f'{name}.png', x, y)
    shield = ASSETS / 'minecraft/textures/gui/sprites/container/slot/shield.png'
    for x, y in ((62, 84), (77, 62), (26, 142)):
        _paste_icon(panel, shield, x, y)
    sword = sword_icon()
    for x, y in ((44, 84), (80, 84), (8, 142)):
        panel.alpha_composite(sword, (x, y))
    for name, x in (('combat_tonic', 44), ('antidote', 62), ('trail_ration', 80)):
        _paste_icon(panel, PROFESSIONS / f'{name}.png', x, 142)
    bg = Image.new('RGBA', (176, 166), '#292724')
    bg.alpha_composite(panel.crop((0, 0, 176, 166)))
    return bg.convert('RGB')


def make_comparison(texture):
    left = make_preview(texture).resize((528, 498), Image.Resampling.NEAREST)
    # Wynn w zrzucie zajmuje ok. 352 px, czyli 2× natywny panel.
    # Cofamy tę skalę i ponownie pokazujemy oba GUI w skali 3×.
    reference = Image.open(WYNN).convert('RGB')
    right = reference.crop((460, 176, 820, 524))
    right = right.resize((180, 174), Image.Resampling.NEAREST)
    right = right.resize((540, 522), Image.Resampling.NEAREST)
    canvas = Image.new('RGB', (1100, 554), '#242321')
    canvas.paste(left, (4, 28))
    canvas.paste(right, (548, 28))
    d = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    d.text((4, 7), 'NASZE E  |  v7  |  x3', fill='#E5D1A5', font=font)
    d.text((548, 7), 'WYNNCRAFT  |  klatka 17  |  skala podobna',
           fill='#E5D1A5', font=font)
    return canvas


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--suffix', default='', help='np. _iter1')
    args = parser.parse_args()
    texture = make_texture()
    texture.save(HERE / f'tekstura_v7{args.suffix}.png')
    make_comparison(texture).save(HERE / f'podglad_porownanie_v7{args.suffix}.png')
    print('rozmiar:', texture.size,
          'sloty:', sum(map(len, slot_positions())),
          'barwy:', len({p for p in texture.get_flattened_data() if p[3]}))


if __name__ == '__main__':
    main()
