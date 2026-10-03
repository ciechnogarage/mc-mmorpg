#!/usr/bin/env python3
"""Karty Księgi dla 14 aktywnych stacji MMOItems 6.10.1.

Czyta serwerowe YAML tylko do odczytu. Zmienia wyłącznie gui-layout;
wyniki i tekstury zapisuje pod własnym out/.
"""
from collections import Counter
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import ksiega_wspolne as W

SOURCE = Path('/opt/docker/mc/data/world1/plugins/MMOItems/crafting-stations')
OUT = HERE / 'out'
STATIONS = (
    ('profession-alchemy', 'Alchemik'),
    ('profession-building', 'Budowniczy'),
    ('profession-cooking', 'Kucharz'),
    ('profession-enchanting', 'Zaklinacz'),
    ('profession-farming', 'Farmer'),
    ('profession-fishing', 'Rybak'),
    ('profession-herbalism', 'Zielarz'),
    ('profession-hunting', 'Myśliwy'),
    ('profession-jewelcrafting', 'Jubiler'),
    ('profession-mining', 'Górnik'),
    ('profession-smithing', 'Kowal'),
    ('profession-tailoring', 'Krawiec'),
    ('profession-woodworking', 'Drwal-stolarz'),
    ('chowance', 'Poskramiacz'),
)
QUEUE = [36, 37, 38, 45, 46]
QUEUE_PREV, QUEUE_NEXT = 27, 30
RECIPE_PREV, RECIPE_NEXT = 5, 8
BODY = re.compile(r'(?m)^    ([a-z_]+):\s*$')
SLOTS = re.compile(r'(?m)^(      slots: )\[[^\n]*\](?:\s*#.*)?$')
TITLE_TEMPLATE = re.search(r"^menu_title: '([^']+)'", (
    Path('/opt/docker/mc/data/world1/plugins/DeluxeMenus/gui_menus/hub_glowne.yml')
    .read_text(encoding='utf-8')), re.M).group(1)


def split_items(gui):
    start = gui.index('  items:\n') + len('  items:\n')
    part = gui[start:]
    marks = list(BODY.finditer(part))
    result = {}
    for i, mark in enumerate(marks):
        end = marks[i + 1].start() if i + 1 < len(marks) else len(part)
        result[mark.group(1)] = part[mark.start():end].strip('\n')
    return gui[:start], result


def slots_of(block):
    match = SLOTS.search(block)
    assert match, block[:100]
    return [int(x) for x in re.findall(r'\d+', match.group(0))]


def with_slots(block, slots):
    assert len(SLOTS.findall(block)) == 1
    return SLOTS.sub(lambda m: m.group(1) + '[' + ', '.join(map(str, slots)) + ']', block, count=1)


def recipe_slots(n):
    assert 1 <= n <= 29
    if n > 20:
        return [9 * row + col for row in range(1, 6) for col in range(5, 9)]
    widths = {
        5: [3, 2], 6: [3, 3], 8: [4, 4], 9: [3, 3, 3],
        10: [4, 3, 3], 14: [4, 4, 4, 2],
    }.get(n)
    assert widths is not None, n
    result = []
    for row, width in enumerate(widths, 1):
        columns = (5, 6, 7, 8) if width == 4 else (5, 6, 7) if width == 3 else (6, 7)
        result.extend(9 * row + col for col in columns)
    assert len(result) == n
    return result


def page_title(codes, page_placeholder):
    title = TITLE_TEMPLATE
    for old, code in zip(range(0x4168, 0x416C), codes):
        assert title.count(chr(old)) == 1
        title = title.replace(chr(old), chr(code))
    return title + ('&f ' + page_placeholder if page_placeholder else '')


def nav_item(key, slot, label):
    return (f'    {key}:\n'
            f'      function: {key}\n'
            '      item: ARROW\n'
            f"      name: '&6{label}'\n"
            f'      slots: [{slot}]\n'
            '      hide_if_no_page: true\n'
            '      no_page:\n'
            '        item: AIR')


def queue_nav(key, slot, label):
    return (f'    {key}:\n'
            f'      function: {key}\n'
            '      item: ARROW\n'
            f"      name: '&6{label}'\n"
            f'      slots: [{slot}]')


def recipe_count(raw):
    recipes = raw.split('\nrecipes:\n', 1)[1].split('\ngui-layout:\n', 1)[0]
    return len(re.findall(r"(?m)^  ['\w-]+:\s*$", recipes))


def convert(ident, raw, codes):
    before, gui = raw.split('\ngui-layout:\n', 1)
    gui_head, old = split_items(gui)
    assert {'queued_item', 'recipe'} <= old.keys()
    n = recipe_count(raw)
    layout = recipe_slots(n)
    placeholder = re.search(r'^  name:.*?((?:\{page\}/\{max_page\}))', gui_head, re.M)
    title = page_title(codes, placeholder.group(1) if placeholder else '')
    gui_head = re.sub(r"(?m)^  name:.*$", "  name: '" + title + "'", gui_head, count=1)
    gui_head = re.sub(r'(?m)^  slots: \d+$', '  slots: 54', gui_head, count=1)
    parts = []
    for key in ('previous_page', 'next_page'):
        slot = RECIPE_PREV if key == 'previous_page' else RECIPE_NEXT
        if key in old:
            parts.append(with_slots(old[key], [slot]))
        else:
            parts.append(nav_item(key, slot, 'Poprzednia strona' if key == 'previous_page' else 'Następna strona'))
    for key, slot, label in (('previous_queue_item', QUEUE_PREV, 'Poprzednie wyroby'),
                             ('next_queue_item', QUEUE_NEXT, 'Następne wyroby')):
        parts.append(with_slots(old[key], [slot]) if key in old else queue_nav(key, slot, label))
    parts += [with_slots(old['queued_item'], QUEUE), with_slots(old['recipe'], layout)]
    extra = set(old) - {'previous_page', 'next_page', 'previous_queue_item',
                        'next_queue_item', 'queued_item', 'recipe'}
    assert not extra, (ident, extra)
    result = before + '\ngui-layout:\n' + gui_head.rstrip() + '\n' + '\n\n'.join(parts) + '\n'
    validate(ident, raw, result, n, layout, codes)
    return result, layout, title, n


def validate(ident, old_raw, new_raw, n, layout, codes):
    old_before, old_gui = old_raw.split('\ngui-layout:\n', 1)
    new_before, new_gui = new_raw.split('\ngui-layout:\n', 1)
    assert old_before == new_before, ident
    _, old = split_items(old_gui)
    _, new = split_items(new_gui)
    assert set(old) <= set(new)
    for key in old:
        assert SLOTS.sub('      slots: []', old[key]) == SLOTS.sub('      slots: []', new[key]), (ident, key)
    placements = {key: slots_of(block) for key, block in new.items()}
    all_slots = [slot for arr in placements.values() for slot in arr]
    assert all(0 <= slot < 54 and slot % 9 != 4 for slot in all_slots)
    assert len(all_slots) == len(set(all_slots)), (ident, Counter(all_slots))
    assert placements['recipe'] == layout and placements['queued_item'] == QUEUE
    assert len(layout) >= n or {'next_page', 'previous_page'} <= new.keys()
    assert '  slots: 54\n' in new_gui and len(re.findall(r'(?m)^  name: ', new_gui)) == 1
    assert new_gui.split("  name: '", 1)[1].startswith('&f')
    assert chr(codes[0]) in new_gui
    assert all(re.search(r'crafting-time: (?:[1-9]\d*|0\.\d*[1-9])', old_raw) for _ in [0])


def icon_base():
    W.R((0, 0, 51, 38), 'blue0')
    W.L([(2, 34), (49, 34)], 'gold0', 2)
    W.L([(3, 36), (48, 36)], 'wood2')


def emblem(ident, x, y):
    """14 czytelnych sylwetek 52×39, rysowanych osobno od miniatur profesji."""
    icon_base()
    if ident == 'profession-alchemy':
        W.G([(20, 4), (32, 4), (31, 8), (29, 8), (29, 16), (40, 29),
             (37, 33), (15, 33), (12, 29), (23, 16), (23, 8), (21, 8)], 'stone2', 'ink')
        W.G([(18, 26), (34, 26), (38, 30), (36, 31), (16, 31), (14, 30)], 'red1')
        W.R((23, 3, 29, 6), 'gold2', 'ink')
        W.star(40, 10)
    elif ident == 'profession-building':
        for bx, by in ((7, 15), (22, 15), (37, 15), (14, 25), (29, 25)):
            W.R((bx, by, bx + 13, by + 8), 'red1', 'ink')
            W.L([(bx + 2, by + 2), (bx + 10, by + 2)], 'gold1')
        W.G([(7, 7), (19, 4), (27, 8), (17, 12)], 'stone2', 'ink')
        W.L([(19, 11), (34, 29)], 'wood2', 3)
    elif ident == 'profession-cooking':
        W.E((8, 12, 43, 32), 'stone1', 'ink', 2)
        W.E((12, 15, 39, 27), 'red0', 'gold1')
        W.R((19, 10, 32, 13), 'stone2', 'ink')
        W.L([(15, 7), (15, 3)], 'paper4', 2)
        W.L([(25, 7), (25, 2)], 'paper4', 2)
        W.L([(35, 7), (35, 3)], 'paper4', 2)
    elif ident == 'profession-enchanting':
        W.G([(6, 13), (24, 15), (26, 31), (7, 28)], 'paper3', 'gold0')
        W.G([(26, 15), (45, 13), (44, 28), (26, 31)], 'paper2', 'gold0')
        W.L([(26, 15), (26, 31)], 'gold1', 2)
        W.G([(26, 1), (30, 8), (37, 10), (30, 12), (26, 19), (22, 12), (15, 10), (22, 8)], 'blue2', 'gold2')
        W.star(39, 5)
    elif ident == 'profession-farming':
        for dx in (-9, -3, 3, 9):
            W.L([(26 + dx, 31), (25 + dx, 12)], 'gold1', 2)
            W.G([(24 + dx, 14), (20 + dx, 8), (24 + dx, 3), (28 + dx, 9)], 'gold2', 'gold0')
        W.R((14, 25, 38, 29), 'red1', 'ink')
        W.L([(7, 32), (43, 32)], 'green1', 3)
    elif ident == 'profession-fishing':
        W.G([(6, 20), (17, 12), (35, 12), (43, 19), (35, 27), (17, 27)], 'blue2', 'ink')
        W.G([(6, 20), (1, 13), (1, 27)], 'gold1', 'ink')
        W.E((30, 17, 33, 20), 'paper4')
        W.L([(38, 5), (46, 6), (45, 18), (40, 22)], 'gold2', 2)
        W.L([(40, 22), (36, 19)], 'stone2')
    elif ident == 'profession-herbalism':
        W.L([(11, 31), (39, 7)], 'gold1', 2)
        for cx, cy, direct in ((18, 23, -1), (27, 17, 1), (34, 10, -1)):
            W.G([(cx, cy), (cx + 10*direct, cy - 10), (cx + 12*direct, cy - 3)], 'green1', 'gold0')
        W.R((11, 28, 42, 32), 'stone2', 'ink')
        W.E((18, 24, 35, 29), 'green0', 'gold1')
    elif ident == 'profession-hunting':
        W.L([(13, 5), (8, 18), (13, 31)], 'wood2', 3)
        W.L([(13, 5), (24, 18), (13, 31)], 'paper4')
        W.L([(8, 18), (43, 18)], 'gold2', 2)
        W.G([(44, 18), (35, 13), (35, 23)], 'stone2', 'ink')
        W.L([(24, 12), (28, 18), (24, 24)], 'red1', 2)
    elif ident == 'profession-jewelcrafting':
        W.E((10, 11, 42, 33), 'gold1', 'ink', 3)
        W.E((16, 17, 36, 30), 'blue0', 'gold0', 2)
        W.G([(26, 2), (38, 11), (26, 23), (14, 11)], 'blue2', 'gold2')
        W.G([(26, 4), (32, 11), (26, 18), (20, 11)], 'paper4', 'blue1')
    elif ident == 'profession-mining':
        W.L([(15, 31), (34, 7)], 'wood2', 4)
        W.G([(12, 12), (28, 3), (43, 7), (39, 12), (28, 9), (16, 16)], 'stone2', 'ink')
        W.G([(5, 30), (10, 23), (17, 27), (15, 33)], 'stone1', 'ink')
        W.G([(39, 28), (44, 23), (48, 29), (44, 33)], 'blue2', 'gold2')
    elif ident == 'profession-smithing':
        W.G([(6, 18), (35, 18), (42, 13), (47, 13), (42, 24),
             (33, 26), (30, 32), (18, 32), (16, 26), (9, 23)], 'stone2', 'ink')
        W.L([(12, 4), (28, 17)], 'wood2', 3)
        W.R((7, 2, 16, 8), 'stone1', 'ink')
        W.star(34, 10)
    elif ident == 'profession-tailoring':
        W.R((9, 13, 31, 30), 'red1', 'ink')
        W.E((9, 10, 31, 16), 'gold1', 'ink')
        W.E((9, 27, 31, 33), 'gold1', 'ink')
        for xx in (13, 18, 23, 28):
            W.L([(xx, 17), (xx, 27)], 'paper3')
        W.L([(42, 3), (34, 31)], 'stone2', 2)
        W.E((39, 3, 43, 7), 'blue0', 'stone2')
    elif ident == 'profession-woodworking':
        W.E((7, 17, 32, 33), 'wood2', 'ink', 2)
        W.E((12, 20, 27, 30), 'wood3', 'gold0')
        W.E((17, 22, 22, 27), 'gold1', 'gold0')
        W.L([(14, 10), (39, 20)], 'stone2', 3)
        for xx in (21, 27, 33):
            W.L([(xx, 13 + (xx-21)//3), (xx-2, 20 + (xx-21)//3)], 'stone1')
        W.R((10, 8, 15, 12), 'wood2', 'ink')
    else:  # chowance — łapa na pieczęci
        W.G([(26, 4), (44, 12), (42, 27), (26, 35), (10, 27), (8, 12)], 'red0', 'gold1')
        W.E((20, 19, 32, 29), 'gold2', 'ink')
        for px, py in ((15, 16), (22, 12), (30, 12), (37, 16)):
            W.E((px-3, py-4, px+3, py+3), 'gold2', 'ink')


def socket(slot):
    col, row = slot % 9, slot // 9
    x, y = 14 + 36*col, 34 + 36*row
    W.R((x+3, y+3, x+32, y+32), 'leather0', 'gold0')
    W.R((x+5, y+5, x+30, y+30), 'blue0', 'gold0')
    W.L([(x+7, y+7), (x+28, y+7)], 'blue1')
    W.L([(x+7, y+28), (x+28, y+28)], 'gold0')


def arrow_frame(slot):
    socket(slot)


def heading(label, a, b, y):
    W.R((a, y, b, y+22), 'gold0', 'leather0')
    W.R((a+2, y+2, b-2, y+20), 'blue0', 'gold1')
    W.bitmap_text(label, (a+b)//2, y+15, 'gold2', max_width=b-a-8)


def draw_page(ident, title, layout):
    pic = W.new_canvas()
    W.draw_book()
    W.draw_title(title)
    W.hero_frame(14, 34, lambda x, y: emblem(ident, x, y))
    heading('Kolejka', 53, 119, 146)
    heading('Receptury', 230, 299, 39)
    W.R((18, 177, 157, 250), 'paper1', 'gold0')
    W.R((21, 180, 154, 248), 'paper3', 'paper0')
    if layout:
        last_row = max(slot//9 for slot in layout)
        W.R((194, 66, 330, min(250, 34+36*last_row+35)), 'paper1', 'gold0')
        W.R((197, 69, 327, min(248, 34+36*last_row+32)), 'paper3', 'paper0')
    for slot in QUEUE + layout:
        socket(slot)
    for slot in (QUEUE_PREV, QUEUE_NEXT, RECIPE_PREV, RECIPE_NEXT):
        arrow_frame(slot)
    W.draw_footer()
    W.draw_lower('Wróć do Księgi')
    W.clean_orphans()
    assert not W.find_orphans(pic)
    assert {color for _, color in pic.getcolors(352*444)} <= set(W.C.values())
    return pic


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'gen').mkdir(exist_ok=True)
    glyph_lines = ['# Warsztaty: 14 kart po cztery ćwiartki, U+4B10–U+4B47.']
    mapping = []
    checks = []
    pages = []
    for index, (ident, title) in enumerate(STATIONS):
        raw = (SOURCE / (ident + '.yml')).read_text(encoding='utf-8')
        codes = [0x4B10 + 4*index + i for i in range(4)]
        output, layout, generated_title, n = convert(ident, raw, codes)
        (OUT / 'gen' / (ident + '.yml')).write_text(output, encoding='utf-8')
        pic = draw_page(ident, title, layout)
        folder = OUT / ident
        folder.mkdir(exist_ok=True)
        pic.save(folder / (ident + '_2x.png'))
        for quarter, (x, y) in enumerate(((0, 0), (176, 0), (0, 222), (176, 222)), 1):
            pic.crop((x, y, x+176, y+222)).save(folder / f'{ident}_{quarter}.png')
            glyph_lines.extend((f'{ident}_{quarter}:', '  font: minecraft:default',
                                f'  texture: nexo:gui/{ident}_{quarter}',
                                f'  ascent: {13 if quarter <= 2 else -98}',
                                '  height: 111', f'  char: "{chr(codes[quarter-1])}"'))
        mapping.append(f'U+{codes[0]:04X} {"chowance_menazeria" if ident == "chowance" else "profession_main"}  # {ident}')
        checks.append(f'{ident}: receptury {n}, gniazda {len(layout)}, strony {(n+len(layout)-1)//len(layout)}, '
                      f'kolejka {len(QUEUE)}/10 z nawigacją, 54 sloty, kol. 4 pusta, glify U+{codes[0]:04X}–U+{codes[-1]:04X}, '
                      'stare elementy zachowane, kolizje 0, piksele osierocone 0')
        pages.append(pic)
    (OUT / 'glify_warsztaty.yml').write_text('\n'.join(glyph_lines)+'\n', encoding='utf-8')
    (OUT / 'podstrony.txt').write_text('\n'.join(mapping)+'\n', encoding='utf-8')
    (OUT / 'kontrola.txt').write_text('\n'.join(checks)+'\n', encoding='utf-8')
    sheet = Image.new('RGB', (352*7, 444*2), W.C['table0'])
    for i, page in enumerate(pages):
        sheet.paste(page, ((i % 7)*352, (i // 7)*444))
    sheet.save(OUT / 'arkusz_x2.png')
    print('\n'.join(checks))


if __name__ == '__main__':
    main()
