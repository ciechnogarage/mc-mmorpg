#!/usr/bin/env python3
"""Skórzana księga postaci: tekstura 176×166 w atlasie 256×256.

Plan światła (jawny, od góry i lewej):
* okładka: górna/lewa krawędź L3, płaszczyzna L2, prawa/dół L0;
* grzbiet: lewa połowa L3/L2, prawa L1; dwa wypukłe paski mają
  jasny górny rant L3 i dolny cień L0;
* kartki: płaszczyzna P2, jasny model P3, zgięcie przy grzbiecie
  w trzech płaskich kolumnach P1/P0/P1 (bez gradientu);
* wnęki: P0 na górze i po lewej, P3 na dole i po prawej;
  wnętrze P1. Skórzane wnęki: I/L0/L1/L3 analogicznie.

Paleta tekstury (14 barw, przezroczystość poza okładką):
I #1D1412, L0 #2F1E1A, L1 #4D3126, L2 #774C31, L3 #A56E44,
L1G #56392B, L2D #6B432D, L2G #825639,
P0 #8E704D, P1 #BA9A6C, P2 #DEC497, P3 #F5E3BE.
R #A65C50, B #63839A: punktowe znaczniki pasa na mikstury.

Małe bryły są rysowane ręcznie siatką znaków (jeden znak = piksel).
Ziarno skóry powtarza ręcznie wskazane, rzadkie punkty bez losowania.
"""

from pathlib import Path
import sys

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
PACK = Path('/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/mmo_equipment')
sys.path.insert(0, '/opt/docker/mc/data/world1/tools/pixelart')
from siatka import do_obrazu  # noqa: E402
from warsztat import wymus_palete  # noqa: E402

I = (0x1D, 0x14, 0x12, 255)
L0 = (0x2F, 0x1E, 0x1A, 255)
L1 = (0x4D, 0x31, 0x26, 255)
L2 = (0x77, 0x4C, 0x31, 255)
L3 = (0xA5, 0x6E, 0x44, 255)
L1G = (0x56, 0x39, 0x2B, 255)
L2D = (0x6B, 0x43, 0x2D, 255)
L2G = (0x82, 0x56, 0x39, 255)
P0 = (0x8E, 0x70, 0x4D, 255)
P1 = (0xBA, 0x9A, 0x6C, 255)
P2 = (0xDE, 0xC4, 0x97, 255)
P3 = (0xF5, 0xE3, 0xBE, 255)
R = (0xA6, 0x5C, 0x50, 255)
B = (0x63, 0x83, 0x9A, 255)
PALETTE = (I, L0, L1, L2, L3, L1G, L2D, L2G, P0, P1, P2, P3, R, B)
T = (0, 0, 0, 0)

# Ręcznie rozplanowane krawędzie. Każdy wiersz wzorca ma 18 znaków.
PAPER_SLOT = (
    'aaaaaaaaaaaaaaaaaa',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'accccccccccccccccc',
)

LEATHER_SLOT = (
    'aaaaaaaaaaaaaaaaaa',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'abbbbbbbbbbbbbbbbc',
    'accccccccccccccccc',
)

# Narożnik wszytej kieszeni nad sakwą, 22×6. Dalsze boki są rysowane
# tym samym motywem, więc wnętrze slotu pozostaje pełne i czytelne.
POUCH_TOP = (
    '....iiiiiiiiiiiiii....',
    '..ii33333333333333ii..',
    '.i322222222222222223i.',
    'i32222222222222222223i',
    'i32222112112112222223i',
    'i22222222222222222222i',
)

# 5×5 szew rogu, miejscowo wyjaśnia promień zamiast miękkiego AA.
CORNER_TL = (
    '.....',
    '..i33',
    '.i333',
    '.3332',
    '.3322',
)

def box(d, xy, color):
    d.rectangle(xy, fill=color)

def grid(im, rows, x, y, legend):
    assert rows and len({len(r) for r in rows}) == 1, 'Nierówne wiersze siatki'
    assert all(set(r) <= set(legend) | {'.'} for r in rows)
    tile = do_obrazu(legend, rows)
    im.alpha_composite(tile, (x, y))

def cover(im):
    d = ImageDraw.Draw(im)
    # Obrys z narożnikami o promieniu 3 px, bez prostokątnego podkładu.
    box(d, (4, 0, 171, 165), I)
    box(d, (2, 2, 173, 163), I)
    box(d, (1, 4, 174, 161), I)
    box(d, (4, 2, 171, 161), L0)
    box(d, (2, 4, 173, 159), L0)
    box(d, (5, 4, 170, 158), L2)
    box(d, (4, 6, 171, 156), L2)
    box(d, (5, 5, 169, 5), L3)
    box(d, (5, 6, 5, 155), L3)
    box(d, (6, 158, 169, 159), L1)
    box(d, (170, 6, 171, 157), L1)
    grid(im, CORNER_TL, 1, 1, {'i': I, '3': L3, '2': L2})
    # Rząd pojedynczych ściegów co 3 px: jasna nić na ciemnej skórze.
    for sx in range(9, 168, 3):
        d.point((sx, 3), fill=P1)
        d.point((sx, 161), fill=P1)
    for sy in range(10, 155, 3):
        d.point((3, sy), fill=P1)
        d.point((172, sy), fill=P1)

def paper_and_spine(im):
    d = ImageDraw.Draw(im)
    # Lewa kartka schodzi pod sloty 9-13; krawędź na dole ma 2 px.
    box(d, (4, 7, 79, 101), P2)
    box(d, (80, 82, 97, 101), P2)
    box(d, (7, 7, 74, 7), P3)
    box(d, (4, 8, 4, 98), P1)
    box(d, (5, 8, 5, 98), P3)
    box(d, (5, 99, 97, 99), P1)
    box(d, (5, 100, 97, 100), P0)
    box(d, (4, 101, 97, 101), P1)
    box(d, (76, 8, 76, 80), P1)
    box(d, (77, 8, 77, 80), P0)
    box(d, (78, 8, 78, 80), P1)
    # Okno postaci ma wyraźny ciemny kontur i jasne, gładkie wnętrze.
    box(d, (25, 8, 75, 78), I)
    box(d, (26, 9, 74, 77), L2)
    box(d, (27, 10, 73, 76), P3)
    box(d, (27, 10, 72, 10), P2)
    box(d, (27, 11, 27, 75), P2)
    # Skórzany, lekko wypukły grzbiet. Zagięcia kartki są płaskimi polami.
    box(d, (79, 4, 95, 83), I)
    box(d, (80, 5, 94, 81), L1)
    box(d, (82, 6, 91, 80), L2)
    box(d, (82, 7, 83, 80), L3)
    box(d, (84, 6, 84, 80), L3)
    box(d, (92, 7, 94, 80), L0)
    box(d, (86, 6, 89, 80), L2)
    # Dwa poprzeczne wypukłe paski, światło tylko na górze i po lewej.
    for by in (25, 52):
        box(d, (80, by, 94, by), L0)
        box(d, (81, by+1, 93, by+2), L3)
        box(d, (82, by+3, 93, by+4), L2)
        box(d, (82, by+5, 94, by+5), L0)
        d.point((82, by+1), fill=P1)
        d.point((86, by+1), fill=P1)
        d.point((90, by+1), fill=P1)
    # Kartka prawa ma 1-2 px przyciemniony brzeg na zewnętrznej stronie.
    box(d, (96, 6, 169, 81), P2)
    box(d, (97, 6, 169, 8), P3)
    box(d, (97, 9, 97, 80), P1)
    box(d, (98, 9, 98, 80), P0)
    box(d, (99, 9, 99, 80), P1)
    box(d, (100, 9, 169, 80), P2)
    box(d, (168, 9, 168, 79), P1)
    box(d, (169, 9, 169, 80), P0)
    # Odtwórz czysty pas tytułu: czytelny dla tekstu klienta #404040.
    box(d, (97, 6, 169, 16), P2)
    box(d, (97, 6, 169, 6), P3)
    box(d, (104, 61, 123, 78), P2)
    box(d, (96, 81, 169, 81), P1)

def leather_bag_and_bar(im):
    d = ImageDraw.Draw(im)
    # Torba L: w górnym rzędzie od slotu 14, pełna szerokość od y=102.
    box(d, (98, 82, 170, 139), L0)
    box(d, (5, 102, 170, 139), L0)
    box(d, (98, 83, 168, 100), L2)
    box(d, (6, 103, 168, 136), L2)
    box(d, (6, 103, 168, 103), L3)
    box(d, (6, 104, 6, 135), L3)
    box(d, (7, 136, 169, 138), L1)
    box(d, (169, 84, 170, 138), L1)
    # Płaskie szwy po zewnętrznej krawędzi torby, co 3 piksele.
    for sx in range(9, 168, 3):
        d.point((sx, 139), fill=P1)
    # Hotbar jako osobny, grubszy pas skóry.
    box(d, (4, 140, 171, 162), I)
    box(d, (5, 141, 170, 160), L1)
    box(d, (6, 142, 168, 158), L2)
    box(d, (5, 141, 169, 141), L3)
    box(d, (6, 142, 6, 158), L3)
    box(d, (6, 159, 169, 160), L0)
    for sx in range(8, 168, 3):
        d.point((sx, 140), fill=P1)
        d.point((sx, 161), fill=P1)

def sewn_frame(im, x, y, count, tight=False):
    d = ImageDraw.Draw(im)
    left = x - 4
    if tight:
        # Zapas 11+12: cienka, papierowa oprawa. Jej prawy brzeg x=78
        # sąsiaduje z ciemnym x=79 aktywnej pary, tworząc podwójną linię.
        right, top, bottom = x + count*18 - 2, y - 3, y + 17
        box(d, (left, top, right, top), P0)
        box(d, (left, bottom, right, bottom), P0)
        box(d, (left, top, left, bottom), P0)
        box(d, (right, top, right, bottom-2), P0)
        d.point((right, bottom-1), fill=P3)
        box(d, (left+1, top+1, right-1, top+1), P3)
        box(d, (left+1, top+1, left+1, bottom-1), P3)
        for sx in range(left+4, right-2, 3):
            d.point((sx, top), fill=P3)
            d.point((sx, bottom), fill=P3)
        for sy in range(top+3, bottom-2, 3):
            d.point((left, sy), fill=P3)
        return

    # Hotbar 0+1: te same dwie ciemne warstwy i jasne ściegi co aktywna L.
    right, top, bottom = x + count*18 - 1, y - 4, y + 20
    box(d, (left, top, right, top+1), I)
    box(d, (left, bottom-1, right, bottom), I)
    box(d, (left, top, left+1, bottom), I)
    box(d, (right-1, top, right, bottom), I)
    box(d, (left+1, top+2, right-1, top+2), L0)
    box(d, (left+1, top+2, left+1, bottom-2), L0)
    box(d, (right-1, top+2, right-1, bottom-2), L0)
    box(d, (left+1, bottom-2, right-1, bottom-2), L0)
    for sx in range(left+4, right-2, 3):
        d.point((sx, top+1), fill=P1)
        d.point((sx, bottom-1), fill=P1)
    for sy in range(top+4, bottom-2, 3):
        d.point((left, sy), fill=P1)
        d.point((right, sy), fill=P1)
    d.point((right-1, y+16), fill=L2)

def potion_belt_frame(im):
    """Niższy, wspólny przeszyty pas wokół hotbarowych slotów 2–4."""
    d = ImageDraw.Draw(im)
    box(d, (44, 140, 96, 140), L0)
    box(d, (46, 141, 94, 141), L3)
    d.point((44, 141), fill=L3)
    box(d, (44, 143, 44, 157), L3)
    box(d, (96, 141, 96, 157), L0)
    box(d, (46, 159, 94, 159), L2)
    box(d, (44, 160, 96, 160), L0)
    for sx in range(48, 94, 4):
        d.point((sx, 140), fill=P1)
        d.point((sx, 160), fill=P1)
    for sy in range(145, 158, 4):
        d.point((44, sy), fill=P1)
        d.point((96, sy), fill=P1)
    # Po jednym barwnym ściegu na 2 i 3; slot 4 ma nić w kolorze skóry.
    d.point((53, 141), fill=R)
    d.point((71, 141), fill=B)
    d.point((89, 141), fill=P1)

def active_hands_frame(im):
    """Jedna schodkowa oprawa dla przesuniętych o 3 px aktywnych rąk."""
    d = ImageDraw.Draw(im)
    # Ciemna, dwuwarstwowa L łączy drugą rękę (77,62) ze slotem 13.
    # Dolny brzeg górnego ramienia jest w y=79; w y=80 zostaje papier.
    outline = [(74, 57), (93, 57), (95, 59), (95, 78),
               (97, 78), (97, 80), (97, 100), (96, 101),
               (79, 101), (79, 80), (78, 79), (74, 79),
               (72, 77), (72, 59)]
    d.line(outline + [outline[0]], fill=I)
    box(d, (74, 58, 93, 58), I)
    box(d, (73, 59, 73, 77), I)
    box(d, (94, 59, 94, 77), I)
    box(d, (97, 80, 97, 100), I)
    box(d, (80, 101, 96, 101), I)
    box(d, (75, 59, 93, 59), L0)
    box(d, (74, 60, 74, 77), L0)
    box(d, (93, 60, 93, 77), L0)
    box(d, (96, 81, 96, 82), L0)
    box(d, (80, 81, 80, 82), L0)
    box(d, (80, 101, 96, 101), L0)
    box(d, (74, 79, 78, 79), I)
    box(d, (74, 80, 78, 80), P2)
    # Ciemny lewy brzeg x=79 i jasny zapas x=78 rozcinają obie oprawy.
    box(d, (79, 80, 79, 101), I)
    # Szwy na całym obwodzie, w tej samej barwie co hotbar.
    for sx in range(77, 93, 3):
        d.point((sx, 58), fill=P1)
    for sy in range(62, 78, 3):
        d.point((73, sy), fill=P1)
        d.point((95, sy), fill=P1)
    for sx in range(82, 96, 3):
        d.point((sx, 101), fill=P1)
    for sy in (84, 88, 92, 96, 100):
        d.point((79, sy), fill=P1)
        d.point((97, sy), fill=P1)
    d.point((78, 79), fill=P1)
    d.point((97, 79), fill=P1)

def pouch(im):
    d = ImageDraw.Draw(im)
    # Kieszonka jest wszyta wokół obszaru (154,28), a nie położona na nim.
    # Sama wnęka 18×18 przyjdzie później, dlatego pozostaje czytelna.
    box(d, (151, 25, 171, 43), I)
    box(d, (152, 44, 170, 45), I)
    box(d, (154, 46, 168, 47), I)
    box(d, (157, 48, 165, 48), I)
    box(d, (152, 26, 170, 43), L1)
    box(d, (153, 44, 169, 45), L1)
    box(d, (155, 46, 167, 46), L2)
    box(d, (158, 47, 164, 47), L0)
    box(d, (152, 26, 152, 42), L3)
    grid(im, POUCH_TOP, 150, 23,
         {'i': I, '3': L3, '2': L2, '1': P1})
    for sy in (31, 34, 37, 40):
        d.point((151, sy), fill=P1)
        d.point((171, sy), fill=P1)
    for sx in (156, 159, 162, 165):
        d.point((sx, 46), fill=P1)

def slot(im, x, y, material):
    assert material in ('paper', 'leather')
    rows = PAPER_SLOT if material == 'paper' else LEATHER_SLOT
    legend = ({'a': P0, 'b': P1, 'c': P3} if material == 'paper'
              else {'a': L0, 'b': L1, 'c': L2})
    grid(im, rows, x-1, y-1, legend)

def leather_grain(im):
    """Rzadkie, ręcznie rozłożone punkty tylko we wnętrzu pól skóry."""
    grain = (
        (1, 1), (7, 2), (14, 1), (18, 4), (4, 5), (11, 5),
        (16, 7), (2, 9), (8, 8), (13, 10), (0, 13), (5, 12),
        (10, 14), (17, 13), (3, 16), (8, 17), (15, 17),
        (20, 2), (22, 9), (21, 15), (12, 19), (6, 20),
        (19, 20), (0, 21), (14, 22), (23, 22),
        (3, 3), (9, 3), (12, 7), (21, 6), (3, 19), (18, 11),
    )
    # Dwa przesunięte kafle 24×23 dają ok. 7% na gotowych płaszczyznach,
    # bez losowego szumu i bez dotykania krawędzi.
    seed = set(grain)
    src = im.copy()
    for y in range(2, 164):
        for x in range(2, 174):
            base = src.getpixel((x, y))
            if base not in (L1, L2):
                continue
            if any(src.getpixel(p) != base for p in
                   ((x-1, y), (x+1, y), (x, y-1), (x, y+1))):
                continue
            a = (x % 24, y % 23) in seed
            b = ((x+9) % 24, (y+7) % 23) in seed
            if a or b:
                im.putpixel((x, y),
                            L1G if base == L1 else (L2G if a else L2D))

def gray_figure(im):
    """Sylwetka wyłącznie w podglądzie; nie trafia do tekstury."""
    d = ImageDraw.Draw(im)
    shade = (110, 107, 101, 255)
    light = (145, 141, 133, 255)
    box(d, (46, 16, 56, 26), shade)
    box(d, (47, 16, 53, 17), light)
    box(d, (40, 28, 62, 49), shade)
    box(d, (41, 28, 56, 29), light)
    box(d, (35, 30, 39, 55), shade)
    box(d, (63, 30, 67, 55), shade)
    box(d, (42, 50, 48, 71), shade)
    box(d, (54, 50, 60, 71), shade)

def equipment_icon(kind):
    """Szary miecz lub tarcza, w palecie ikon wyposażenia Nexo."""
    icon = Image.new('RGBA', (16, 16), T)
    d = ImageDraw.Draw(icon)
    shadow = (8, 11, 16, 190)
    dark = (58, 68, 82, 175)
    mid = (126, 142, 162, 160)
    light = (190, 205, 224, 185)
    if kind == 'sword':
        d.polygon([(10, 3), (13, 2), (14, 3), (13, 6), (7, 12),
                   (5, 10)], fill=dark)
        d.line((11, 4, 6, 10), fill=light, width=2)
        d.point((13, 3), fill=light)
        d.line((3, 8, 8, 13), fill=shadow, width=2)
        d.line((4, 8, 8, 12), fill=mid, width=1)
        d.line((5, 11, 3, 13), fill=dark, width=2)
        d.point((2, 14), fill=light)
    else:
        d.polygon([(3, 3), (12, 3), (13, 5), (12, 11),
                   (8, 14), (4, 12), (2, 10), (2, 5)], fill=shadow)
        d.polygon([(4, 4), (11, 4), (12, 6), (11, 10),
                   (8, 13), (5, 11), (3, 9), (3, 6)], fill=mid)
        d.line((4, 5, 10, 5), fill=light, width=2)
        d.line((5, 7, 5, 10), fill=light)
        d.line((7, 11, 8, 12), fill=dark)
    return icon

def consumable_icon(kind):
    """Ikony 16×16 tylko do podglądu: dwa eliksiry i bochenek chleba."""
    icon = Image.new('RGBA', (16, 16), T)
    d = ImageDraw.Draw(icon)
    outline = (40, 30, 27, 255)
    glass_dark = (82, 88, 91, 255)
    glass_light = (201, 207, 197, 255)
    if kind in ('health', 'mana'):
        liquid = ((171, 49, 53, 255) if kind == 'health'
                  else (47, 99, 178, 255))
        liquid_light = ((230, 100, 86, 255) if kind == 'health'
                        else (100, 170, 218, 255))
        d.rectangle((6, 1, 9, 3), fill=outline)
        d.rectangle((6, 1, 9, 2), fill=(153, 113, 70, 255))
        d.polygon([(5, 3), (10, 3), (10, 5), (12, 7), (12, 12),
                   (10, 14), (5, 14), (3, 12), (3, 7), (5, 5)], fill=outline)
        d.polygon([(5, 6), (10, 6), (11, 8), (11, 12), (10, 13),
                   (5, 13), (4, 12), (4, 8)], fill=glass_dark)
        d.polygon([(5, 8), (10, 8), (11, 9), (11, 12), (10, 13),
                   (5, 13), (4, 12), (4, 9)], fill=liquid)
        d.line((5, 8, 9, 8), fill=liquid_light)
        d.point((5, 10), fill=liquid_light)
        d.line((5, 5, 6, 4), fill=glass_light)
        d.point((10, 6), fill=glass_light)
    else:
        crust = (142, 82, 39, 255)
        top = (207, 148, 71, 255)
        crumb = (237, 188, 105, 255)
        d.polygon([(2, 9), (4, 6), (7, 4), (11, 5), (13, 8),
                   (13, 12), (11, 14), (4, 14), (2, 12)], fill=outline)
        d.polygon([(3, 9), (5, 6), (8, 5), (11, 6), (12, 9),
                   (12, 12), (10, 13), (5, 13), (3, 12)], fill=crust)
        d.polygon([(4, 9), (6, 6), (9, 6), (11, 8), (11, 11),
                   (9, 12), (5, 12), (4, 11)], fill=top)
        d.line((6, 8, 8, 6), fill=crumb)
        d.line((9, 10, 10, 8), fill=crumb)
    return icon

def slot_positions():
    armor = [(8, y) for y in (8, 26, 44, 62)]
    jewelry = [(98, 18), (116, 18), (98, 36), (116, 36)]
    special = [(154, 28), (77, 62)]
    inventory = [(x, y) for y in (84, 102, 120)
                 for x in (8, 26, 44, 62, 80, 98, 116, 134, 152)]
    hotbar = [(x, 142) for x in (8, 26, 44, 62, 80, 98, 116, 134, 152)]
    return armor, jewelry, special, inventory, hotbar

def make_texture():
    im = Image.new('RGBA', (256, 256), T)
    cover(im)
    paper_and_spine(im)
    leather_bag_and_bar(im)
    pouch(im)
    armor, jewelry, special, inventory, hotbar = slot_positions()
    positions = armor + jewelry + special + inventory + hotbar
    assert len(positions) == 46 and len(set(positions)) == 46
    paper_slots = set(armor + jewelry + special + [(x, 84) for x in (8, 26, 44, 62, 80)])
    for x, y in positions:
        assert 0 <= x-1 and x+16 < 176 and 0 <= y-1 and y+16 < 166
        material = 'paper' if (x, y) in paper_slots else 'leather'
        slot(im, x, y, material)
    active_hands_frame(im)
    sewn_frame(im, 44, 84, 2, tight=True)
    sewn_frame(im, 8, 142, 2)
    leather_grain(im)
    potion_belt_frame(im)
    # Nie ma rysunku na strefie sprite książki ani na tytule.
    d = ImageDraw.Draw(im)
    box(d, (104, 61, 123, 78), P2)
    box(d, (97, 6, 169, 16), P2)
    wymus_palete(im, PALETTE)
    assert all(im.getpixel((x, y))[3] == 0 for y in range(166, 256) for x in range(256))
    assert all(im.getpixel((x, y))[3] == 0 for y in range(166) for x in range(176, 256))
    assert set(p for p in im.get_flattened_data() if p[3]) <= set(PALETTE)
    # Krawędzie 18×18 obejmują dokładnie wnętrza 16×16 z blockout_v3.
    for x, y in positions:
        paper = (x, y) in paper_slots
        assert im.getpixel((x, y)) in ((P1,) if paper else (L1, L1G))
        edge = im.getpixel((x+16, y+16))
        assert edge in ((P3, L2) if paper else (L2,)), ((x, y), edge)
    return im

def make_preview(texture, icons=False):
    panel = texture.copy()
    if icons:
        gray_figure(panel)
        overlays = [
            ('necklace.png', 98, 18), ('ring.png', 116, 18),
            ('ring.png', 98, 36), ('charm.png', 116, 36),
            ('gloves.png', 8, 84), ('belt.png', 26, 84),
        ]
        for filename, x, y in overlays:
            icon = Image.open(PACK / filename).convert('RGBA')
            assert icon.size == (16, 16)
            panel.alpha_composite(icon, (x, y))
        for kind, x, y in (
            ('sword', 44, 84), ('shield', 62, 84),
            ('sword', 8, 142), ('shield', 26, 142),
            ('shield', 77, 62), ('sword', 80, 84),
        ):
            panel.alpha_composite(equipment_icon(kind), (x, y))
        for kind, x in (('health', 44), ('mana', 62), ('bread', 80)):
            panel.alpha_composite(consumable_icon(kind), (x, 142))
    bg = Image.new('RGBA', (256, 256), (40, 40, 40, 255))
    bg.alpha_composite(panel)
    return bg.convert('RGB').resize((1024, 1024), Image.Resampling.NEAREST)

def main():
    tex = make_texture()
    tex.save(HERE / 'tekstura_v4.png')
    make_preview(tex).save(HERE / 'podglad_v4.png')
    make_preview(tex, True).save(HERE / 'podglad_v4_z_ikonami.png')
    print('Zapisano teksturę 256×256 i podglądy 1024×1024; slotów 18×18:',
          sum(map(len, slot_positions())), 'kolorów:', len(set(p for p in tex.get_flattened_data() if p[3])))

if __name__ == '__main__':
    main()
