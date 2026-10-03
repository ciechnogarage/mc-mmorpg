#!/usr/bin/env python3
"""Odtwarza sześć stron Chowańców z serwerowych YAML (tylko odczyt).

Serwerowa kopia tools/chowance/generuj_menu.py nie jest dostępna w tym
środowisku. Ten lokalny generator zachowuje bloki źródłowe poza układem,
niewidocznymi polami na grafice, lore emblematu i usunięciem powrotów.
"""
from collections import Counter
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.path.insert(0, str(HERE.parent / 'siatka'))
import ksiega_wspolne as W
from siatka import split_menu, SLOT, SLOTS, MATERIAL

SOURCE = Path('/opt/docker/mc/data/world1/plugins/DeluxeMenus/gui_menus')
OUT = HERE / 'out'
IDS = ('menazeria', 'hodowla', 'lochy', 'ranking', 'magazyn', 'bestiariusz')
LABELS = ('Chowańce', 'Hodowla', 'Lochy', 'Ranking', 'Magazyn', 'Bestiariusz')
EMBLEM = [r * 9 + c for r in range(3) for c in range(4)]
RIGHT = [r * 9 + c for r in range(6) for c in (5, 6, 7, 8)]
LEFT_LOWER = [r * 9 + c for r in range(3, 6) for c in range(4)]
def tile_slots(col, row):
    return [9*row+col, 9*row+col+1, 9*(row+1)+col, 9*(row+1)+col+1]

TILES = {
    'hodowla': {'jajo_1': None},
    'lochy': {'gniazdo': tile_slots(5,1), 'krypta': tile_slots(7,1),
              'mistrzowie': tile_slots(5,3), 'wieza': tile_slots(7,3)},
    'ranking': {'turniej': tile_slots(5,1), 'pojedynek': tile_slots(7,1)},
}
INFO = {'menazeria': 'info', 'hodowla': 'postep', 'ranking': 'top',
        'magazyn': 'informacja', 'bestiariusz': 'postep'}
COMMAND_GROUP = re.compile(r'^    [a-z_]+_click_commands:\s*\n((?:      - .*\n)+)', re.M)
PLACEHOLDER = re.compile(r'%[a-zA-Z0-9_]+%')


def positions(ident, blocks):
    slots = {}
    if ident == 'menazeria':
        for i, slot in enumerate((36, 37, 38, 39, 46, 47), 1):
            slots[f'druzyna_{i}'] = slots[f'druzyna_{i}_pusty'] = slot
        # Dziewięć akcji dzieli cztery tematyczne kafle; każdy obszar
        # kafla ma osobne niewidoczne pola kliknięcia dla swoich opcji.
        slots.update(walka_dziki=[14,23], gracze=15, starter=24,
                     magazyn=[16,25], bestiariusz=[17,26],
                     hodowla=[32,41], warsztat=[33,42],
                     lochy=[34,43], ranking=[35,44])
    elif ident == 'hodowla':
        slots.update(rodzic_1=37, rodzic_1_pusty=37, rodzic_2=38,
                     rodzic_2_pusty=38, jajo_1=14, jajo_2=15, jajo_3=16)
    elif ident == 'lochy':
        slots.update(TILES['lochy'])
    elif ident == 'ranking':
        slots.update(TILES['ranking'])
    elif ident == 'magazyn':
        # 28 wpisów: pełne 24 miejsca po prawej, pełny rząd po lewej.
        entries = RIGHT + [27, 28, 29, 30]
        slots.update({f'm_{i}': entries[i-1] for i in range(1, 29)})
        slots.update({f'd_{i}': s for i, s in enumerate((36, 37, 38, 39, 45, 46), 1)})
        slots.update(poprzednia=47, nastepna=48)
    else:
        # Dwadzieścia pól po prawej, osiem po lewej: po jednym nagłówku
        # nad każdą grupą bez zasłaniania przedmiotów.
        entries = [r*9+c for r in range(1,6) for c in (5,6,7,8)] + \
                  [r*9+c for r in (4,5) for c in (0,1,2,3)]
        for i, slot in enumerate(entries, 1):
            for state in ('z', 'w', 'n'):
                slots[f'b_{i}_{state}'] = slot
        slots.update(poprzednia=5, nastepna=8)
    if ident in INFO:
        slots[INFO[ident]] = EMBLEM
    expected = set(blocks) - {'wstecz', 'zamknij'}
    if ident == 'magazyn':
        expected.add('informacja')
    assert set(slots) == expected, (ident, expected - set(slots), set(slots) - expected)
    return slots


def title_template():
    hub = (SOURCE / 'hub_glowne.yml').read_text(encoding='utf-8')
    template = re.search(r"^menu_title: '([^']+)'", hub, re.M).group(1)
    old = [chr(c) for c in range(0x4168, 0x416c)]
    assert all(template.count(c) == 1 for c in old)
    return template, old


def invisible_item(block):
    assert '    item_flags:' not in block
    return MATERIAL.sub('    material: nexo-mmo_icon_blank\n'
                        '    item_flags: [HIDE_ADDITIONAL_TOOLTIP]', block, count=1)


def on_artwork(ident, name):
    return (name == INFO.get(ident) or
            ident == 'menazeria' and not name.startswith('druzyna_') or
            ident in ('lochy', 'ranking'))


def warehouse_info():
    return ("  'informacja':\n"
            "    material: nexo-mmo_icon_blank\n"
            "    item_flags: [HIDE_ADDITIONAL_TOOLTIP]\n"
            "    slots: [" + ', '.join(map(str, EMBLEM)) + "]\n"
            "    update: true\n"
            "    display_name: '&6&lMagazyn'\n"
            "    lore:\n"
            "      - '&7Strona: &f%chowance_magazyn_strona%&7/&f%chowance_magazyn_stron%'\n")


def render(ident, raw, slots, codes):
    header, blocks = split_menu(raw)
    template, old = title_template()
    title = template
    for before, after in zip(old, map(chr, codes)):
        title = title.replace(before, after)
    header = re.sub(r'^menu_title:.*$', "menu_title: '" + title + "'", header,
                    count=1, flags=re.M)
    header = re.sub(r'^size: *\d+$', 'size: 54', header, count=1, flags=re.M)
    header = re.sub(r'^#.*\n', '', header)
    header = '# Wygenerowane przez strony/chowance/generuj_menu.py.\n' + header
    rewritten = []
    for name, block in blocks.items():
        if name in ('wstecz', 'zamknij'):
            continue
        place = slots[name]
        if on_artwork(ident, name):
            block = invisible_item(block)
        if isinstance(place, list):
            block = SLOT.sub('    slots: [' + ', '.join(map(str, place)) + ']', block, count=1)
        else:
            block = SLOT.sub(lambda m: m.group(1) + str(place), block, count=1)
        rewritten.append(block.rstrip())
    if ident == 'magazyn':
        rewritten.append(warehouse_info().rstrip())
    return header.rstrip() + '\n' + '\n'.join(rewritten) + '\n', title


def check(ident, raw, menu, slots):
    old_header, old = split_menu(raw)
    new_header, new = split_menu(menu)
    keep = set(old) - {'wstecz', 'zamknij'}
    if ident == 'magazyn':
        keep.add('informacja')
    assert set(new) == keep
    def stable_header(header):
        return [line for line in header.splitlines()
                if line and not line.startswith(('#', 'menu_title:', 'size:'))]
    assert stable_header(old_header) == stable_header(new_header)
    for name in keep:
        if name == 'informacja':
            assert new[name].strip() == warehouse_info().strip()
            continue
        before, after = old[name], new[name]
        if on_artwork(ident, name):
            before = invisible_item(before)
        if isinstance(slots[name], list):
            before = SLOT.sub('    slot: X', before, count=1)
            after = SLOTS.sub('    slot: X', after, count=1)
        else:
            before = SLOT.sub('    slot: X', before, count=1)
            after = SLOT.sub('    slot: X', after, count=1)
        assert before.strip() == after.strip(), (ident, name)
    def commands(items):
        return Counter(line for block in items for group in COMMAND_GROUP.findall(block)
                       for line in group.splitlines())
    old_commands = commands(old[n] for n in keep if n in old)
    new_commands = commands(new[n] for n in keep)
    assert old_commands == new_commands
    old_placeholders = Counter(PLACEHOLDER.findall(''.join(old[n] for n in old if n in keep)))
    new_placeholders = Counter(PLACEHOLDER.findall(''.join(new[n] for n in keep)))
    if ident == 'magazyn':
        new_placeholders.subtract(PLACEHOLDER.findall(warehouse_info()))
    assert old_placeholders == new_placeholders
    old_title = re.search(r"^menu_title: '([^']+)'", old_header, re.M).group(1)
    new_title = re.search(r"^menu_title: '([^']+)'", new_header, re.M).group(1)
    bare_title = re.sub(r'[\uE000-\uF8FF\u4AC0-\u4AD7]', '',
                        re.sub(r'&[0-9a-fk-or]', '', new_title, flags=re.I))
    assert not bare_title, (ident, bare_title)
    title_placeholders = set(PLACEHOLDER.findall(old_title))
    if title_placeholders:
        assert title_placeholders <= set(PLACEHOLDER.findall(new[INFO[ident]]))
    for name, block in new.items():
        if on_artwork(ident, name):
            assert '    material: nexo-mmo_icon_blank\n' in block, (ident, name)
            assert '    item_flags: [HIDE_ADDITIONAL_TOOLTIP]\n' in block, (ident, name)
    occupied = {}
    for name, place in slots.items():
        for slot in (place if isinstance(place, list) else [place]):
            assert 0 <= slot <= 53 and slot % 9 != 4, (ident, name, slot)
            occupied.setdefault(slot, []).append(name)
    for slot, names in occupied.items():
        if len(names) > 1:
            bases = {re.sub(r'(_pusty|_[zwn])$', '', n) for n in names}
            assert len(bases) == 1 and (set(names) == {next(iter(bases)), next(iter(bases))+'_pusty'}
                    or set(names) == {next(iter(bases))+'_z', next(iter(bases))+'_w',
                                      next(iter(bases))+'_n'}), (ident, slot, names)
    return len(old), len(new), sum(old_commands.values()), sum(new_commands.values()), len(occupied)


def socket(slot):
    x, y = 14 + 36 * (slot % 9), 34 + 36 * (slot // 9)
    W.R((x+2, y+2, x+33, y+33), 'leather0', 'gold0')
    W.R((x+5, y+5, x+30, y+30), 'blue0', 'gold1')
    W.L([(x+7, y+7), (x+28, y+7)], 'blue1')
    W.L([(x+7, y+28), (x+28, y+28)], 'gold0')


def emblem(ident):
    def paint(x, y):
        W.R((x, y, x+51, y+38), 'blue0')
        if ident == 'menazeria':
            # Jeden duży lis: trójkątne uszy, jasny pysk i czytelne oczy.
            W.G([(x+7,y+4),(x+20,y+11),(x+32,y+11),(x+45,y+4),
                 (x+43,y+28),(x+32,y+37),(x+20,y+37),(x+9,y+28)],'red1','ink')
            W.G([(x+11,y+8),(x+20,y+15),(x+13,y+20)],'gold2','ink')
            W.G([(x+41,y+8),(x+32,y+15),(x+39,y+20)],'gold2','ink')
            W.G([(x+11,y+25),(x+25,y+30),(x+26,y+37),(x+19,y+36)],'paper4')
            W.G([(x+41,y+25),(x+27,y+30),(x+26,y+37),(x+33,y+36)],'paper4')
            for xx in (18,33):
                W.R((x+xx-2,y+22,x+xx+2,y+25),'ink')
                W.R((x+xx-1,y+22,x+xx-1,y+22),'paper4')
            W.G([(x+23,y+30),(x+29,y+30),(x+26,y+34)],'ink')
        elif ident == 'hodowla':
            W.E((x+11,y+3,x+40,y+37),'paper4','gold1',2)
            W.L([(x+26,y+5),(x+23,y+15),(x+29,y+21),(x+25,y+35)],'gold0',2)
            W.G([(x+3,y+34),(x+9,y+23),(x+16,y+34)],'green1','ink')
            W.G([(x+36,y+34),(x+43,y+23),(x+50,y+34)],'green1','ink')
        elif ident == 'lochy':
            W.G([(x+2,y+38),(x+7,y+10),(x+14,y+4),(x+38,y+4),
                 (x+45,y+10),(x+50,y+38)],'stone1','ink')
            W.G([(x+13,y+38),(x+17,y+18),(x+22,y+14),(x+30,y+14),
                 (x+35,y+18),(x+39,y+38)],'ink','gold0')
            W.G([(x+19,y+38),(x+21,y+24),(x+31,y+24),(x+33,y+38)],'red0','gold1')
            W.R((x+25,y+27,x+27,y+34),'gold2')
        elif ident == 'ranking':
            W.G([(x+9,y+5),(x+43,y+5),(x+40,y+23),(x+34,y+29),
                 (x+29,y+30),(x+29,y+34),(x+37,y+34),(x+37,y+38),
                 (x+15,y+38),(x+15,y+34),(x+23,y+34),(x+23,y+30),
                 (x+18,y+29),(x+12,y+23)],'gold1','ink')
            W.L([(x+9,y+9),(x+3,y+9),(x+4,y+20),(x+13,y+24)],'gold2',3)
            W.L([(x+43,y+9),(x+49,y+9),(x+48,y+20),(x+39,y+24)],'gold2',3)
            W.R((x+14,y+7,x+38,y+11),'gold2')
            W.star(x+26,y+19,'paper4')
        elif ident == 'magazyn':
            W.R((x+5,y+13,x+46,y+37),'wood2','ink')
            W.G([(x+3,y+13),(x+9,y+5),(x+42,y+5),(x+49,y+13)],'gold1','ink')
            W.R((x+8,y+15,x+43,y+35),'wood1','gold0')
            W.R((x+20,y+16,x+32,y+31),'gold2','ink')
            W.R((x+24,y+20,x+28,y+27),'ink')
        else:  # bestiariusz: otwarta księga z odciskiem łapy
            W.G([(x+3,y+9),(x+17,y+5),(x+26,y+10),(x+35,y+5),
                 (x+49,y+9),(x+47,y+34),(x+29,y+31),(x+26,y+37),
                 (x+23,y+31),(x+5,y+34)],'gold1','ink')
            W.G([(x+6,y+12),(x+17,y+8),(x+24,y+12),(x+24,y+30),(x+7,y+31)],'paper4')
            W.G([(x+28,y+12),(x+35,y+8),(x+46,y+12),(x+45,y+31),(x+28,y+30)],'paper3')
            W.L([(x+26,y+10),(x+26,y+36)],'ink',2)
            for xx,yy in ((33,17),(39,16),(43,19)):
                W.E((x+xx,y+yy,x+xx+3,y+yy+3),'red1','ink')
            W.E((x+34,y+23,x+42,y+29),'red1','ink')
    return paint


def miniature(kind):
    def paint(x, y):
        W.R((x,y,x+51,y+38),'blue0')
        if kind == 'pojedynek':
            for xx,coat in ((6,'red1'),(35,'blue1')):
                W.E((x+xx+3,y+5,x+xx+11,y+14),'paper2','ink')
                W.G([(x+xx,y+16),(x+xx+14,y+16),(x+xx+17,y+36),
                     (x+xx-3,y+36)],coat,'ink')
            W.L([(x+17,y+23),(x+35,y+17)],'stone2',3)
            W.L([(x+35,y+23),(x+17,y+17)],'gold2',3)
            W.star(x+26,y+12,'gold2')
        elif kind == 'starcia':
            W.L([(x+8,y+5),(x+42,y+35)],'ink',7)
            W.L([(x+44,y+5),(x+10,y+35)],'ink',7)
            W.L([(x+8,y+5),(x+42,y+35)],'stone2',4)
            W.L([(x+44,y+5),(x+10,y+35)],'paper4',4)
            W.L([(x+10,y+27),(x+22,y+18)],'gold2',4)
            W.L([(x+42,y+27),(x+30,y+18)],'gold2',4)
            W.star(x+26,y+18,'gold2')
        elif kind in ('kolekcja','magazyn'):
            W.R((x+5,y+15,x+47,y+36),'wood2','ink')
            W.G([(x+4,y+15),(x+10,y+5),(x+42,y+5),(x+48,y+15)],'gold1','ink')
            W.R((x+20,y+17,x+32,y+28),'gold2','ink')
            W.R((x+24,y+19,x+28,y+25),'ink')
        elif kind in ('rozwoj','hodowla'):
            W.E((x+8,y+7,x+31,y+36),'paper4','gold1',2)
            W.L([(x+20,y+9),(x+17,y+18),(x+22,y+25),(x+19,y+34)],'gold0',2)
            W.star(x+39,y+11,'gold2')
            W.star(x+43,y+28,'paper4')
        elif kind == 'wyprawy':
            W.G([(x+3,y+37),(x+8,y+8),(x+15,y+5),(x+37,y+5),
                 (x+44,y+8),(x+49,y+37)],'stone1','ink')
            W.G([(x+17,y+37),(x+19,y+18),(x+24,y+13),(x+29,y+13),
                 (x+34,y+18),(x+36,y+37)],'ink','gold0')
            W.R((x+24,y+23,x+28,y+33),'gold2')
        elif kind in ('gniazdo','krypta','mistrzowie','wieza'):
            colors = {'gniazdo':'green1','krypta':'stone2',
                      'mistrzowie':'gold1','wieza':'red1'}
            W.G([(x+6,y+38),(x+10,y+10),(x+18,y+4),(x+34,y+4),
                 (x+42,y+10),(x+46,y+38)],colors[kind],'ink')
            W.G([(x+15,y+38),(x+18,y+19),(x+24,y+15),(x+28,y+15),
                 (x+34,y+19),(x+37,y+38)],'blue0','gold0')
            if kind == 'mistrzowie': W.star(x+26,y+25,'gold2')
            elif kind == 'wieza':
                for yy in (13,20,27): W.L([(x+13,y+yy),(x+39,y+yy)],'gold2',2)
            elif kind == 'gniazdo':
                W.G([(x+19,y+33),(x+26,y+24),(x+33,y+33)],'green1','gold2')
            else: W.R((x+24,y+24,x+28,y+34),'red1','ink')
        elif kind == 'turniej':
            W.R((x+8,y+5,x+11,y+37),'wood2','ink')
            W.G([(x+11,y+5),(x+43,y+5),(x+39,y+25),(x+30,y+30),
                 (x+19,y+26)],'red1','gold1')
            W.star(x+26,y+17,'gold2')
        else:
            raise ValueError(kind)
    return paint


DESCRIPTIONS = {
    'hodowla': ('Dobierz parę.', 'Jaja dojrzewają', 'w czasie podróży.'),
    'lochy': ('Wybierz loch.', 'Każdy ma własne', 'stworzenia.'),
    'ranking': ('Zbieraj ELO.', 'Walcz z innymi.', 'Wygraj turniej.'),
}


def description_card(lines, x, y=142):
    W.R((x,y,x+143,y+107),'paper1','gold0')
    W.R((x+3,y+3,x+140,y+104),'paper3','gold0')
    W.R((x+6,y+6,x+137,y+101),'paper4')
    W.gui_body(lines,x+72,y+8,126,94,frame=(x,y,x+143,y+107))


def heading(label, x, y=43):
    W.R((x,y-4,x+135,y+26),'gold0','leather0')
    W.R((x+2,y-2,x+133,y+24),'blue0','gold1')
    W.gui_text(label,x+68,y+2,'gold2',max_gui=61,frame=(x,y-4,x+135,y+26))


def tile(x, y, caption, kind):
    W.tile(x,y,caption,miniature(kind),caption)


def draw_page(ident, slots):
    image = W.new_canvas()
    W.draw_book()
    W.draw_title(LABELS[IDS.index(ident)])
    W.hero_frame(14, 34, emblem(ident))
    if ident == 'menazeria':
        heading('Wybierz dział',194)
        for x,y,label,kind in ((194,70,'Starcia','starcia'),
                               (266,70,'Zbiory','kolekcja'),
                               (194,142,'Rozwój','rozwoj'),
                               (266,142,'Wyprawy','wyprawy')):
            tile(x,y,label,kind)
        heading('Twoja drużyna',22,146)
        W.R((22,176,157,249),'paper1','gold0')
        W.R((25,179,154,246),'paper3','paper0')
        for s in (36,37,38,39,46,47): socket(s)
    elif ident == 'hodowla':
        heading('Jaja',194)
        W.R((198,68,326,107),'paper1','gold0')
        W.R((201,71,323,104),'paper3','paper0')
        for s in (14,15,16): socket(s)
        heading('Rodzice',22,146)
        W.R((45,176,127,218),'paper1','gold0')
        W.R((48,179,124,215),'paper3','paper0')
        for s in (37,38): socket(s)
        description_card(DESCRIPTIONS[ident],194)
    elif ident == 'lochy':
        heading('Wybierz loch',194)
        for x,y,label,kind in ((194,70,'Gniazdo','gniazdo'),
                               (266,70,'Krypta','krypta'),
                               (194,142,'Mistrzowie','mistrzowie'),
                               (266,142,'Wieża','wieza')):
            tile(x,y,label,kind)
        description_card(DESCRIPTIONS[ident],14)
    elif ident == 'ranking':
        heading('Rywalizacja',194)
        tile(194,70,'Turniej','turniej')
        tile(266,70,'Pojedynek','pojedynek')
        description_card(DESCRIPTIONS[ident],14)
        description_card(('Sprawdź lokatę.',
                          'Wygrywaj walki.', 'Awansuj w rankingu.'),194)
    elif ident == 'bestiariusz':
        heading('Gatunki',194)
        heading('Wpisy',22,146)
        W.R((194,68,330,250),'paper1','gold0')
        W.R((197,71,327,247),'paper3','paper0')
        W.R((19,176,158,250),'paper1','gold0')
        W.R((22,179,155,247),'paper3','paper0')
        for s in [r*9+c for r in range(1,6) for c in (5,6,7,8)]: socket(s)
        for s in [r*9+c for r in (4,5) for c in (0,1,2,3)]: socket(s)
        for slot in (5, 8):
            socket(slot)
    else:
        # Magazyn: 28 wpisów + 6 członków drużyny + 2 przyciski stron
        # zajmuje wszystkie 36 pól poza emblematem.
        right = [s for s in range(54) if s%9 in (5,6,7,8)]
        left = sorted({s for name, place in slots.items() if name != INFO.get(ident)
                       for s in (place if isinstance(place,list) else [place]) if s%9<4})
        W.R((194,34,330,250),'paper1','gold0')
        W.R((197,37,327,247),'paper3','paper0')
        W.R((19,142,158,250),'paper1','gold0')
        W.R((22,145,155,247),'paper3','paper0')
        for s in right+left: socket(s)
    W.draw_footer()
    W.draw_lower('WRÓĆ DO MENU')
    W.clean_orphans()
    assert not W.find_orphans(image)
    assert {color for _,color in image.getcolors(352*444)} <= set(W.C.values())
    return image


def main():
    (OUT / 'gen').mkdir(parents=True, exist_ok=True)
    previews, glyphs, results, parents = [], ['# Chowańce: cztery ćwiartki Nexo na stronę.'], [], []
    for index, ident in enumerate(IDS):
        full = f'chowance_{ident}'
        raw = (SOURCE / f'{full}.yml').read_text(encoding='utf-8')
        _, blocks = split_menu(raw)
        slots = positions(ident, blocks)
        if ident == 'magazyn':
            assert sum(f'm_{i}' in slots for i in range(1,29)) == 28
            assert sum(f'd_{i}' in slots for i in range(1,7)) == 6
        if ident == 'bestiariusz':
            assert sum(all(f'b_{i}_{state}' in slots for state in 'zwn')
                       for i in range(1,29)) == 28
        codes = [0x4AC0 + 4*index + n for n in range(4)]
        menu, title = render(ident, raw, slots, codes)
        old_n, new_n, old_commands, new_commands, unique = check(ident, raw, menu, slots)
        (OUT / 'gen' / f'{full}.yml').write_text(menu, encoding='utf-8')
        img = draw_page(ident, slots)
        dest = OUT / full
        dest.mkdir(exist_ok=True)
        img.save(dest / f'{full}_2x.png')
        for n, (x, y) in enumerate(((0,0),(176,0),(0,222),(176,222)), 1):
            img.crop((x,y,x+176,y+222)).save(dest / f'{full}_{n}.png')
            glyphs += [f'{full}_{n}:', '  font: minecraft:default',
                       f'  texture: nexo:gui/{full}_{n}',
                       f'  ascent: {13 if n <= 2 else -98}', '  height: 111',
                       f'  char: "{chr(codes[n-1])}"']
        previews.append(img)
        parent = 'hub_glowne' if ident == 'menazeria' else 'chowance_menazeria'
        parents.append(f'U+{codes[0]:04X} {parent}  # {full}')
        results.append(f'{full}: {old_n}→{new_n} itemów; '
                       f'{old_commands}→{new_commands} komend; '
                       f'{unique} zajętych slotów; U+{codes[0]:04X}–U+{codes[-1]:04X}; '
                       f'tytuł {title}')
    (OUT / 'glify_chowance.yml').write_text('\n'.join(glyphs)+'\n', encoding='utf-8')
    (OUT / 'podstrony.txt').write_text('\n'.join(parents)+'\n', encoding='utf-8')
    sheet = Image.new('RGB', (352*3, 444*2), W.C['table0'])
    for i, img in enumerate(previews): sheet.paste(img, ((i%3)*352, (i//3)*444))
    sheet.save(OUT / 'arkusz_x2.png')
    # Osobny arkusz z każdą nową miniaturą i pełnymi emblematami.
    thumbs = [('Starcia','starcia'),('Zbiory','kolekcja'),
              ('Rozwój','rozwoj'),('Wyprawy','wyprawy'),
              ('Gniazdo','gniazdo'),('Krypta','krypta'),
              ('Mistrzowie','mistrzowie'),('Wieża','wieza'),
              ('Turniej','turniej'),('Pojedynek','pojedynek')]
    cell_w, cell_h = 576, 432
    contact = Image.new('RGB',(cell_w*4,cell_h*4),W.C['table0'])
    for index,ident in enumerate(IDS):
        frame=previews[index].crop((14,34,158,142))
        contact.paste(frame.resize((cell_w,cell_h),Image.Resampling.NEAREST),
                      ((index%4)*cell_w,(index//4)*cell_h))
    for offset,(label,kind) in enumerate(thumbs,6):
        canvas=W.new_canvas()
        tile(14,34,label,kind)
        crop=canvas.crop((14,34,86,106)).resize((288,288),Image.Resampling.NEAREST)
        contact.paste(crop,((offset%4)*cell_w+144,(offset//4)*cell_h+72))
    contact.save(OUT/'kafle_x4.png')
    results += ['Usunięte: wstecz ×6, zamknij ×1; dodany informacyjny emblemat '
                'magazynu; pozostałe nazwy itemów, komendy, warunki i '
                'placeholdery zachowane.',
                'Tytuły: bez tekstu po odjęciu kodów kolorów i glifów; '
                'zmienne tytułów w lore emblematów.',
                'Kafle i emblematy: nexo-mmo_icon_blank oraz '
                'HIDE_ADDITIONAL_TOOLTIP; gniazda 1×1 zachowały ikony.',
                'Listy: magazyn 28+6 miejsc, bestiariusz 28 miejsc; '
                'sloty 0–53, kolumna 4 pusta, tylko zamierzone warianty '
                'współdzielą slot.',
                'Obraz: sześć stron bez osieroconych pikseli, paleta K12; '
                'kafle_x4.png i arkusz_x2.png wygenerowane przez NEAREST.',
                'Ograniczenie: magazyn zajmuje 36/36 pól poza emblematem; '
                'nagłówek nad listą wymagałby zmniejszenia liczby pól.']
    (OUT / 'kontrola.txt').write_text('\n'.join(results)+'\n', encoding='utf-8')
    print('\n'.join(results))


if __name__ == '__main__':
    main()
