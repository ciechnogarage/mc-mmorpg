#!/usr/bin/env python3
"""Karty Księgi z emblematem i gniazdami 1×1; eksport Nexo/DeluxeMenus.

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
ITEM_START = re.compile(r"^  '([^']+)':\s*$", re.M)
SLOT = re.compile(r'^(    slot: )\d+\s*$', re.M)
SLOTS = re.compile(r'^    slots: \[[\d, ]+\]\s*$', re.M)
MATERIAL = re.compile(r'^(    material: ).+$', re.M)
COMMAND_GROUP = re.compile(r'^    [a-z_]+_click_commands:\s*\n((?:      - .*\n)+)', re.M)
PLACEHOLDER = re.compile(r'%[a-zA-Z0-9_]+%')
EMBLEM_SLOTS = [9 * row + col for row in range(3) for col in range(4)]
INFO = {'wiara': 'stan', 'profession_main': 'zasady'}
LEFT_PROFESSIONS = ('smithing', 'tailoring', 'alchemy', 'enchanting')


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
        assert len(SLOT.findall(items[name])) + len(SLOTS.findall(items[name])) == 1, name
    assert not body[:markers[0].start()].strip()
    return header, items


def original_slot(block):
    return int(SLOT.search(block).group(0).split(':', 1)[1])


def place_items(ident, blocks):
    """Pozycje współdzielą slot, jeśli DeluxeMenus rozstrzyga wariant priorytetem."""
    names = [n for n in blocks if n not in ('wstecz', 'zamknij')]
    result = {}
    if ident == 'wiara':
        result['stan'] = EMBLEM_SLOTS
        for n in ('modlitwa', 'modlitwa_trwa', 'modlitwa_brak_patrona'):
            result[n] = 38
        for n in names:
            if n.startswith('b_'):
                old = original_slot(blocks[n])
                result[n] = {10: 23, 11: 24, 12: 25,
                             14: 32, 15: 33, 16: 34}[old]
    elif ident == 'ksiega_zaklec':
        result['puste'] = 38
        spell_slots = [9 * row + col for row in range(1, 6) for col in (5, 6, 7)]
        for n, slot in zip((n for n in names if n != 'puste'), spell_slots):
            result[n] = slot
    else:
        result['zasady'] = EMBLEM_SLOTS
        for n, slot in zip(LEFT_PROFESSIONS, (37, 38, 46, 47)):
            result[n] = slot
        work_slots = [9 * row + col for row in range(2, 5) for col in (5, 6, 7)]
        for n, slot in zip((n for n in names if n not in ('zasady', *LEFT_PROFESSIONS)), work_slots):
            result[n] = slot
    assert set(result) == set(names), (ident, set(names) - set(result))
    assert all(0 <= s < 54 and s % 9 != 4
               for placement in result.values() for s in (placement if isinstance(placement, list) else [placement]))
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
        if name == INFO.get(ident):
            block = MATERIAL.sub(r'\1nexo-mmo_icon_blank', block, count=1)
            block = SLOT.sub('    slots: [' + ', '.join(map(str, EMBLEM_SLOTS)) + ']', block, count=1)
        else:
            block = SLOT.sub(lambda m: m.group(1) + str(slots[name]), block, count=1)
        rewritten.append(block.rstrip())
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
    """Tabliczka grupy, odsunięta od gniazd i ramy emblematu."""
    W.R((left, y, right, y + 22), 'gold0', 'leather0')
    W.R((left + 2, y + 2, right - 2, y + 20), 'blue0', 'gold1')
    W.bitmap_text(label, (left + right) // 2, y + 15, 'gold2', max_width=right-left-10)


def emblem_faith(x, y):
    """Ołtarz z kolumnami, świątynnym tympanonem i znakiem patrona."""
    W.R((x, y, x+51, y+38), 'blue0')
    W.G([(x+2,y+14),(x+26,y+2),(x+49,y+14)], 'stone2', 'ink')
    W.L([(x+7,y+14),(x+45,y+14)], 'paper4', 2)
    for cx in (11, 39):
        W.R((x+cx-3,y+15,x+cx+3,y+31),'stone1','ink')
        W.R((x+cx-5,y+13,x+cx+5,y+16),'stone2','ink')
    W.R((x+4,y+32,x+48,y+38),'stone0','ink')
    W.R((x+16,y+27,x+36,y+33),'gold1','ink')
    W.G([(x+26,y+9),(x+29,y+17),(x+36,y+19),(x+29,y+22),
         (x+26,y+29),(x+23,y+22),(x+16,y+19),(x+23,y+17)],'gold2','red0')
    W.R((x+24,y+18,x+28,y+21),'paper4')


def emblem_spells(x, y):
    """Dwie różne dłonie z czerwonym płomieniem i błękitną runą."""
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+0,y+38),(x+7,y+28),(x+8,y+18),(x+11,y+17),
         (x+12,y+27),(x+15,y+18),(x+18,y+19),(x+17,y+29),
         (x+22,y+25),(x+24,y+28),(x+18,y+38)],'paper2','ink')
    W.G([(x+51,y+38),(x+44,y+28),(x+43,y+18),(x+40,y+17),
         (x+39,y+27),(x+36,y+18),(x+33,y+19),(x+34,y+29),
         (x+29,y+25),(x+27,y+28),(x+33,y+38)],'paper1','ink')
    W.G([(x+9,y+3),(x+14,y+12),(x+18,y+8),(x+18,y+19),
         (x+12,y+23),(x+7,y+17)],'fire','red0')
    W.G([(x+12,y+9),(x+15,y+17),(x+12,y+21),(x+10,y+16)],'gold2')
    W.G([(x+38,y+3),(x+45,y+11),(x+39,y+21),(x+32,y+13)],'blue2','paper4')
    W.G([(x+38,y+7),(x+42,y+12),(x+38,y+17),(x+35,y+12)],'blue0','gold2')
    W.star(x+26,y+12,'paper4')


def emblem_professions(x, y):
    """Duże kowadło, młot i szczypce; osobna kompozycja od kafla."""
    W.R((x,y,x+51,y+38),'stone0')
    W.R((x+3,y+32,x+48,y+38),'wood1','ink')
    W.G([(x+5,y+17),(x+34,y+17),(x+39,y+13),(x+43,y+13),
         (x+39,y+22),(x+31,y+24),(x+29,y+30),(x+35,y+30),
         (x+37,y+34),(x+13,y+34),(x+16,y+30),(x+22,y+30),
         (x+20,y+24),(x+9,y+22)],'stone2','ink')
    W.L([(x+9,y+18),(x+34,y+18)],'paper4',2)
    W.L([(x+4,y+4),(x+18,y+16)],'wood2',3)
    W.R((x+1,y+2,x+10,y+7),'stone1','ink')
    W.L([(x+48,y+5),(x+36,y+16)],'wood2',3)
    W.G([(x+43,y+1),(x+49,y+5),(x+45,y+12),(x+40,y+8)],'stone2','ink')
    W.star(x+34,y+10,'gold2')


EMBLEMS = (emblem_faith, emblem_spells, emblem_professions)


def group_frame(left, right, top, bottom, slots):
    """Wspólna podstawa wiąże jedno lub kilka gniazd w grupę."""
    W.R((left,top,right,bottom),'paper1','gold0')
    W.R((left+3,top+3,right-3,bottom-3),'paper3','paper0')
    if len(slots) == 1:
        W.G([(left+20,top+11),(left+28,top+20),(left+20,top+29),
             (left+12,top+20)],'gold1','gold0')
        W.R((left+19,top+19,left+21,top+21),'paper4')
    for slot in sorted(set(slots)):
        socket(slot)


def foot_ornament(cx, y):
    W.L([(cx-44,y),(cx-9,y)],'gold0')
    W.L([(cx+9,y),(cx+44,y)],'gold0')
    W.G([(cx,y-5),(cx+5,y),(cx,y+5),(cx-5,y)],'gold1','gold0')
    W.R((cx,y,cx,y),'paper4')


def draw_page(title, ident, slots, index):
    image = W.new_canvas()
    W.draw_book()
    W.draw_title(title)
    W.hero_frame(14,34,EMBLEMS[index])
    left = sorted({slot for name, placement in slots.items() if name != INFO.get(ident)
                   for slot in (placement if isinstance(placement,list) else [placement]) if slot % 9 < 4})
    right = sorted({slot for name, placement in slots.items() if name != INFO.get(ident)
                    for slot in (placement if isinstance(placement,list) else [placement]) if slot % 9 > 4})
    assert all(slot // 9 >= 3 for slot in left)
    assert all(slot // 9 >= 1 for slot in right)
    heading(('Modlitwa', 'Twoje dłonie', 'Rzemiosła')[index],22,157,146)
    if len(left) == 1:
        group_frame(45,127,176,218,left)
        foot_ornament(86,235)
    else:
        group_frame(28,151,180,249,left)
    heading(('Wybór patrona', 'Wybór zaklęć', 'Profesje')[index],194,330,
            77 if ident != 'ksiega_zaklec' else 43)
    group_frame(198,326,103 if ident != 'ksiega_zaklec' else 67,
                177 if ident == 'wiara' else 249 if ident == 'ksiega_zaklec' else 213,right)
    W.draw_footer()
    foot_ornament(262, 226 if ident == 'wiara' else 253 if ident == 'ksiega_zaklec' else 235)
    W.draw_lower('Wróć do Księgi')
    W.clean_orphans()
    assert not W.find_orphans(image)
    assert {color for _, color in image.getcolors(352 * 444)} <= set(W.C.values())
    return image


def slot_file(dest, ident, slots):
    occupants = {}
    for name, placement in slots.items():
        for slot in (placement if isinstance(placement, list) else [placement]):
            occupants.setdefault(slot, []).append(name)
    lines = [f'{ident} — 54 sloty; kolumna 4 jest nieklikalnym grzbietem.',
             'Lewa strona: emblemat 4×3; rzędy 3–5: stan i akcje. Prawa: gniazda.', '']
    for slot in range(54):
        role = 'grzbiet' if slot % 9 == 4 else ', '.join(occupants.get(slot, ['pusty']))
        lines.append(f'{slot:02d} | rząd {slot // 9}, kol {slot % 9} | {role}')
    lines.append('\nPowrót wyłącznie przez plakietkę dolnego panelu pluginu w ukrytym EQ.')
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
    assert set(new) == kept
    for name in kept:
        before = SLOT.sub('    slot: X', old[name], count=1)
        after = new[name]
        if name == INFO.get(ident):
            before = MATERIAL.sub('    material: nexo-mmo_icon_blank', before, count=1)
            after = re.sub(r'^    slots: \[[^\n]+\]$', '    slot: X', after, count=1, flags=re.M)
            assert '    slots: [' + ', '.join(map(str, EMBLEM_SLOTS)) + ']' in new[name]
        else:
            after = SLOT.sub('    slot: X', after, count=1)
        before = before.strip()
        after = after.strip()
        assert before == after, (ident, name)
        if name != INFO.get(ident):
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
    new_slots = [slot for placement in slots.values()
                 for slot in (placement if isinstance(placement,list) else [placement])]
    assert all(0 <= slot < 54 and slot % 9 != 4 and slot != 45 for slot in new_slots)
    assert 'powrot_ksiega' not in generated and 'slot: 45' not in generated
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
        image = draw_page(TITLES[index], ident, slots, index)
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
        results.append(f'{ident}: pozycje {result[0]}→{result[2]} (bez Wstecz/Zamknij); '
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
