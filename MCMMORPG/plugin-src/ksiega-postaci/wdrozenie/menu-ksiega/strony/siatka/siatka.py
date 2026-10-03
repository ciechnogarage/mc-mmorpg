#!/usr/bin/env python3
"""Karty Księgi z gniazdami 1×1 i miniaturami 2×2; eksport Nexo/DeluxeMenus.

Uruchom: python3 strony/siatka/siatka.py
Źródłowe YAML są czytane wyłącznie z serwerowej kopii DeluxeMenus.
"""
from collections import Counter
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE.parent))
import ksiega_wspolne as W  # noqa: E402

SOURCE = Path('/opt/docker/mc/data/world1/plugins/DeluxeMenus/gui_menus')
OUT = HERE / 'out'
IDS = ('wiara', 'ksiega_zaklec', 'profession_main')
TITLES = ('Wiara', 'Księga zaklęć', 'Profesje')
SCENES = (W.scene_skills, W.scene_journal, W.scene_professions)
CAPTIONS = ('Patron', 'Zaklęcia', 'Warsztaty')
ITEM_START = re.compile(r"^  '([^']+)':\s*$", re.M)
SLOT = re.compile(r'^(    slot: )\d+\s*$', re.M)
COMMAND_GROUP = re.compile(r'^    [a-z_]+_click_commands:\s*\n((?:      - .*\n)+)', re.M)
PLACEHOLDER = re.compile(r'%[a-zA-Z0-9_]+%')


def split_menu(raw):
    match = re.search(r'^items:\s*$', raw, re.M)
    assert match, 'brak items:'
    header = raw[:match.end()]
    body = raw[match.end():]
    markers = list(ITEM_START.finditer(body))
    assert markers, 'brak pozycji'
    items = {}
    for idx, marker in enumerate(markers):
        end = markers[idx + 1].start() if idx + 1 < len(markers) else len(body)
        name = marker.group(1)
        assert name not in items, name
        items[name] = body[marker.start():end]
        assert len(SLOT.findall(items[name])) == 1, name
    assert not body[:markers[0].start()].strip()
    return header, items


def original_slot(block):
    return int(SLOT.search(block).group(0).split(':', 1)[1])


def place_items(ident, blocks):
    """Pozycje współdzielą slot, jeśli DeluxeMenus rozstrzyga wariant priorytetem."""
    names = [n for n in blocks if n not in ('wstecz', 'zamknij')]
    result = {}
    if ident == 'wiara':
        result['stan'] = 10
        for n in ('modlitwa', 'modlitwa_trwa', 'modlitwa_brak_patrona'):
            result[n] = 37
        for n in names:
            if n.startswith('b_'):
                old = original_slot(blocks[n])
                result[n] = {10: 15, 11: 16, 12: 17,
                             14: 33, 15: 34, 16: 35}[old]
    elif ident == 'ksiega_zaklec':
        result['puste'] = 10
        spell_slots = [9 * row + col for row in range(1, 5) for col in range(5, 9)]
        for n, slot in zip((n for n in names if n != 'puste'), spell_slots):
            result[n] = slot
    else:
        result['zasady'] = 10
        work_slots = [9 * row + col for row in range(1, 5) for col in range(5, 9)]
        for n, slot in zip((n for n in names if n != 'zasady'), work_slots):
            result[n] = slot
    assert set(result) == set(names), (ident, set(names) - set(result))
    assert all(0 <= s < 54 and s % 9 != 4 for s in result.values())
    return result


def render_menu(source, ident, codes, slots):
    header, blocks = split_menu(source)
    header = header[re.search(r'^menu_title:', header, re.M).start():]
    header = '# Wygenerowane przez strony/siatka/siatka.py; źródłowe pozycje zachowane.\n' + header
    template = (SOURCE / 'hub_glowne.yml').read_text(encoding='utf-8')
    template = re.search(r"^menu_title: '([^']+)'", template, re.M).group(1)
    old_chars = [chr(code) for code in range(0x4168, 0x416c)]
    assert all(template.count(char) == 1 for char in old_chars)
    title = template
    for old, new in zip(old_chars, map(chr, codes)):
        title = title.replace(old, new)
    header = re.sub(r"^menu_title:.*$", "menu_title: '" + title + "'", header, count=1, flags=re.M)
    header = re.sub(r'^size:\s*\d+\s*$', 'size: 54', header, count=1, flags=re.M)
    rewritten = []
    for name, block in blocks.items():
        if name in ('wstecz', 'zamknij'):
            continue
        rewritten.append(SLOT.sub(lambda m: m.group(1) + str(slots[name]), block, count=1).rstrip())
    # Ostatni rząd skrzyni jest klikalną częścią dolnego panelu. Kolumna 4 pozostaje grzbietem.
    rewritten.append("""  'powrot_ksiega':
    material: nexo-hub_wstecz
    slot: 45
    display_name: '&e&lWróć do Księgi'
    left_click_commands:
      - '[openguimenu] hub_glowne'""")
    return header.rstrip() + '\n\n' + '\n\n'.join(rewritten) + '\n', title


def socket(slot):
    """K12/B: skóra, złoto i granat; lżejszy obrys na kremowym papierze."""
    col, row = slot % 9, slot // 9
    x, y = 14 + 36 * col, 34 + 36 * row
    W.R((x + 3, y + 3, x + 32, y + 32), 'leather0', 'gold0')
    W.R((x + 5, y + 5, x + 30, y + 30), 'blue0', 'gold0')
    W.L([(x + 7, y + 7), (x + 28, y + 7)], 'blue1')
    W.L([(x + 7, y + 28), (x + 28, y + 28)], 'gold0')


def heading(label, left, right, y=43):
    """Mała tabliczka w pustym rzędzie, z bitmapowym fontem Minecrafta."""
    W.R((left, y, right, y + 22), 'gold0', 'leather0')
    W.R((left + 2, y + 2, right - 2, y + 20), 'blue0', 'gold1')
    W.bitmap_text(label, (left + right) // 2, y + 15, 'gold2', max_width=right-left-10)


def bottom_back():
    """Klikalny powrót w dolnym rzędzie GUI, nad dekoracją ekwipunku."""
    W.R((18, 216, 157, 248), 'leather0', 'gold0')
    W.R((21, 219, 154, 245), 'blue0', 'gold1')
    W.bitmap_text('Wróć do Księgi', 105, 237, 'gold2', max_width=94)
    socket(45)


def draw_page(title, elements):
    """Rysuj dowolną listę: {'type':'socket','slot':N}, tile, heading.

    Kafel tile przyjmuje x/y, label i painter z ksiega_wspolne.
    """
    image = W.new_canvas()
    W.draw_book()
    W.draw_title(title)
    occupied = set()
    for element in elements:
        kind = element['type']
        if kind == 'socket':
            slot = element['slot']
            assert 0 <= slot < 54 and slot % 9 != 4
            assert slot not in occupied, slot
            occupied.add(slot)
            socket(slot)
        elif kind == 'tile':
            W.tile(element['x'], element['y'], element['label'], element['painter'], element.get('caption'))
        elif kind == 'heading':
            heading(element['label'], element['left'], element['right'], element.get('y', 43))
        else:
            raise ValueError(kind)
    W.draw_footer()
    bottom_back()
    W.draw_lower('Wróć do Księgi')
    W.clean_orphans()
    assert not W.find_orphans(image)
    assert {color for _, color in image.getcolors(352 * 444)} <= set(W.C.values())
    return image


def elements_for(ident, slots, index):
    elements = [
        dict(type='heading', label=('Stan i łaska', 'Twoje dłonie', 'Zasady')[index], left=22, right=157),
        dict(type='heading', label=('Wybór patrona', 'Wybór zaklęć', 'Warsztaty')[index], left=194, right=330),
        dict(type='tile', x=50, y=106, label=CAPTIONS[index], painter=SCENES[index]),
    ]
    for slot in sorted(set(slots.values())):
        elements.append(dict(type='socket', slot=slot))
    return elements


def slot_file(dest, ident, slots):
    occupants = {}
    for name, slot in slots.items():
        occupants.setdefault(slot, []).append(name)
    occupants[45] = ['powrot_ksiega']
    lines = [f'{ident} — 54 sloty; kolumna 4 jest nieklikalnym grzbietem.',
             'Rząd 0: tabliczki; rzędy 1–4: treść; rząd 5: powrót w dolnym panelu.', '']
    for slot in range(54):
        role = 'grzbiet' if slot % 9 == 4 else ', '.join(occupants.get(slot, ['pusty']))
        lines.append(f'{slot:02d} | rząd {slot // 9}, kol {slot % 9} | {role}')
    lines.append('\nDolny panel ekwipunku: dekoracja wspólna z podstronami; klik DeluxeMenus: slot 45.')
    (dest / 'sloty.txt').write_text('\n'.join(lines) + '\n', encoding='utf-8')


def validate(ident, original, generated, slots):
    old_header, old = split_menu(original)
    new_header, new = split_menu(generated)
    def functional_header(header):
        return [line for line in header.splitlines()
                if line and not line.startswith('#')
                and not line.startswith(('menu_title:', 'size:'))]
    assert functional_header(old_header) == functional_header(new_header)
    kept = set(old) - {'wstecz', 'zamknij'}
    assert set(new) == kept | {'powrot_ksiega'}
    for name in kept:
        before = SLOT.sub('    slot: X', old[name], count=1).strip()
        after = SLOT.sub('    slot: X', new[name], count=1).strip()
        assert before == after, (ident, name)
        assert original_slot(new[name]) == slots[name]
    assert 'size: 54' in new_header
    def commands(blocks):
        return Counter(line.strip() for block in blocks for group in COMMAND_GROUP.findall(block)
                       for line in group.splitlines())
    old_commands = commands(old[n] for n in kept)
    new_commands = commands(new[n] for n in kept)
    assert old_commands == new_commands
    old_placeholders = Counter(PLACEHOLDER.findall('\n'.join(old[n] for n in kept)))
    new_placeholders = Counter(PLACEHOLDER.findall('\n'.join(new[n] for n in kept)))
    assert old_placeholders == new_placeholders
    new_slots = [original_slot(block) for block in new.values()]
    assert all(0 <= slot < 54 and slot % 9 != 4 for slot in new_slots)
    assert len(new_slots) == len(new)
    return len(old), len(kept), len(new), sum(old_commands.values()), sum(new_commands.values())


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    glyphs = ['# Cztery ćwiartki na stronę; char U+4A40 + 4*i + N-1.']
    previews = []
    results = []
    for index, ident in enumerate(IDS):
        original = (SOURCE / f'{ident}.yml').read_text(encoding='utf-8')
        _, blocks = split_menu(original)
        slots = place_items(ident, blocks)
        codes = [0x4A40 + 4 * index + n for n in range(4)]
        menu, title = render_menu(original, ident, codes, slots)
        result = validate(ident, original, menu, slots)
        (OUT / 'dm' / f'{ident}.yml').write_text(menu, encoding='utf-8')
        image = draw_page(TITLES[index], elements_for(ident, slots, index))
        dest = OUT / ident
        dest.mkdir(parents=True, exist_ok=True)
        image.save(dest / f'{ident}_2x.png')
        for n, (x, y) in enumerate(((0, 0), (176, 0), (0, 222), (176, 222)), 1):
            image.crop((x, y, x + 176, y + 222)).save(dest / f'{ident}_{n}.png')
        slot_file(dest, ident, slots)
        for n, code in enumerate(codes, 1):
            glyphs += [f'{ident}_{n}:', '  font: minecraft:default',
                       f'  texture: nexo:gui/{ident}_{n}',
                       f'  ascent: {13 if n <= 2 else -98}', '  height: 111',
                       f'  char: "{chr(code)}"']
        previews.append(image)
        results.append(f'{ident}: pozycje {result[0]}→{result[1]}+powrót={result[2]}; '
                       f'komendy {result[3]}→{result[4]}; sloty OK; piksele osierocone 0; '
                       f'glify U+{codes[0]:04X}–U+{codes[-1]:04X}; title {title}')
    (OUT / 'glify_siatka.yml').write_text('\n'.join(glyphs) + '\n', encoding='utf-8')
    sheet = Image.new('RGB', (352 * 3, 444), W.C['table0'])
    for index, image in enumerate(previews):
        sheet.paste(image, (index * 352, 0))
    sheet.save(OUT / 'arkusz_x2.png')
    (OUT / 'kontrola.txt').write_text('\n'.join(results) + '\n', encoding='utf-8')
    print('\n'.join(results))


if __name__ == '__main__':
    main()
