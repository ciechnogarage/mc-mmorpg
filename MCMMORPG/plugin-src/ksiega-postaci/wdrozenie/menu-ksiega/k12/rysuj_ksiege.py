#!/usr/bin/env python3
"""Finalne menu Q: dwanaście kafli 2×2 na jednej stronie księgi."""
from pathlib import Path
import json
import sys
from collections import Counter
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent
VARIANT = sys.argv[1] if len(sys.argv) > 1 else 'a'
assert VARIANT in ('old', 'a', 'b', 'c')
S = (352, 444)
P = {
    'ink': '#251c2b', 'leather0': '#39232e', 'leather1': '#61323a',
    'wood0': '#3b2930', 'wood1': '#684338',
    'wood2': '#946044', 'wood3': '#bb8050', 'paper0': '#a9744b',
    'paper1': '#d5a96b', 'paper2': '#f0cf8c', 'paper3': '#ffe7ae',
    'paper4': '#fff1cc', 'gold0': '#8c532c', 'gold1': '#ce9135',
    'gold2': '#f4c657', 'red0': '#72263d', 'red1': '#b83d4c',
    'blue0': '#183650', 'blue1': '#286287',
    'blue2': '#4897a9', 'sky': '#96bec2',
    'green0': '#315343', 'green1': '#608057',
    'stone0': '#555f68', 'stone1': '#899499', 'stone2': '#c6c1a8',
    'fire': '#f2954a', 'table0': '#3a2620', 'table1': '#4a3028',
}
assert len(P) <= 32
C = {k: tuple(bytes.fromhex(v[1:])) for k, v in P.items()}
im = Image.new('RGB', S, C['wood0'])
d = ImageDraw.Draw(im)

def R(box, c, outline=None, width=1):
    d.rectangle(box, fill=C[c], outline=C[outline] if outline else None, width=width)

def L(points, c, width=1):
    d.line(points, fill=C[c], width=width, joint='curve')

def G(points, c, outline=None):
    d.polygon(points, fill=C[c])
    if outline:
        d.line(points + [points[0]], fill=C[outline], width=1)

def E(box, c, outline=None, width=1):
    d.ellipse(box, fill=C[c], outline=C[outline] if outline else None, width=width)

def load_minecraft_glyphs():
    """Read the actual Minecraft bitmap providers, including Polish letters."""
    root = Path('/home/dev/.claude/jobs/7bc77651/tmp/mcfont/assets/minecraft')
    providers = json.loads((root / 'font' / 'include' / 'default.json').read_text())['providers']
    glyphs = {}
    for provider in providers:
        filename = provider['file'].split('/')[-1]
        if filename not in ('ascii.png', 'accented.png'):
            continue
        atlas = Image.open(root / 'textures' / 'font' / filename).convert('RGBA')
        rows = provider['chars']
        cell_w, cell_h = atlas.width // len(rows[0]), atlas.height // len(rows)
        assert cell_h == provider.get('height', cell_h)
        for row_index, row in enumerate(rows):
            for col_index, char in enumerate(row):
                if char == '\0':
                    continue
                alpha = atlas.crop((col_index*cell_w, row_index*cell_h,
                                    (col_index+1)*cell_w, (row_index+1)*cell_h)).getchannel('A')
                bounds = alpha.getbbox()
                width = bounds[2] if bounds else 4
                glyphs[char] = (alpha.crop((0,0,width,cell_h)), width, provider['ascent'])
    for char in 'ąćęłńóśźżĘ':
        assert char in glyphs and glyphs[char][0].getbbox(), char
    return glyphs

GLYPHS = load_minecraft_glyphs()

def text_width(s, sx=1, bold=False, spacing=1):
    missing = [char for char in s if char not in GLYPHS]
    if missing:
        raise ValueError(f'Brak glifów Minecrafta: {missing}')
    return (sum(GLYPHS[c][1]+spacing for c in s)-spacing)*sx + int(bold)

def bitmap_text(s, cx, baseline, color='blue0', sx=1, sy=1, bold=False,
                max_width=None, spacing=1):
    width = text_width(s,sx,bold,spacing)
    if max_width is not None and width > max_width:
        raise ValueError(f'Tekst za szeroki: {s}: {width}>{max_width}')
    x = int(cx-width//2)
    if sx == sy == 2 and VARIANT != 'old':
        x -= x % 2  # piksel litery zaczyna się na pełnym pikselu GUI
    for char in s:
        mask, advance, ascent = GLYPHS[char]
        if sx != 1 or sy != 1:
            mask = mask.resize((mask.width*sx,mask.height*sy),Image.Resampling.NEAREST)
        if bold:
            thick = Image.new('L',(mask.width+1,mask.height))
            thick.paste(mask,(0,0))
            thick.paste(mask,(1,0),mask)
            mask = thick
        im.paste(C[color],(x,baseline-ascent*sy),mask)
        x += (advance+spacing)*sx
    return width

TOM_THUMB_HEX = dict(zip('ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    '57DA D75C 7246 D6DC F3CE F3C8 73D6 B7DA E92E 24D4 B75A 924E '
    'BFDA BFFA 56D4 D748 56F6 D7EA 711C E924 B6D6 B6A4 B7FA B55A '
    'B524 E54E'.split()))

def unpack_tom_thumb(value):
    bits = f'{int(value, 16):016b}'[:15]
    return tuple(bits[i:i+3] for i in range(0, 15, 3))

CAPITALS = {char: unpack_tom_thumb(value)
            for char, value in TOM_THUMB_HEX.items()}
CAPITALS.update({
    'I': ('1','1','1','1','1'),
    'M': ('10001','11011','10101','10001','10001'),
    'N': ('101','111','111','101','101'),
    'W': ('10001','10001','10101','10101','01010'),
    'Ł': ('100','101','110','100','111'),
})

def marked_capital(base, mark):
    body = CAPITALS[base]
    width = len(body[0])
    rows = [list('0'*width) for _ in range(9)]
    for y, line in enumerate(body, start=3):
        rows[y] = list(line)
    if mark == 'acute':
        rows[0][width-1] = '1'
        rows[1][width-2] = '1'
    elif mark == 'dot':
        rows[1][width//2] = '1'
    elif mark == 'ogonek':
        rows[8][width-1] = '1'
    else:
        raise ValueError(mark)
    return tuple(''.join(row) for row in rows)

NARROW = {char: tuple('0'*len(body[0]) for _ in range(3)) + body +
          ('0'*len(body[0]),) for char, body in CAPITALS.items()}
NARROW[' '] = ('00',)*9
for char, base, mark in (
    ('Ą','A','ogonek'), ('Ć','C','acute'), ('Ę','E','ogonek'),
    ('Ń','N','acute'), ('Ó','O','acute'), ('Ś','S','acute'),
    ('Ź','Z','acute'), ('Ż','Z','dot'),
):
    NARROW[char] = marked_capital(base, mark)

def narrow_width(s):
    return sum(len(NARROW[ch][0]) for ch in s) + len(s)-1 - int(s in ('ZDOLNOŚCI', 'CHOWAŃCE'))

def narrow_text(s,cx,top,color='blue0',max_gui=34):
    """Draw compact glyphs on whole GUI pixels (even texture coordinates)."""
    if s != s.upper() or any(ch not in NARROW for ch in s):
        raise ValueError(f'Podpis musi używać samych wersalików: {s}')
    width = narrow_width(s)
    if width > max_gui:
        raise ValueError(f'Podpis {s}: {width}>{max_gui} px GUI')
    assert cx % 2 == 0 and top % 2 == 0
    x = 2*((cx//2) - (width//2))
    assert x % 2 == 0
    for index,ch in enumerate(s):
        rows = NARROW[ch]
        assert all(len(row)==len(rows[0]) for row in rows), ch
        for yy,row in enumerate(rows):
            for xx,bit in enumerate(row):
                if bit == '1':
                    R((x+xx*2,top+yy*2,
                       x+xx*2+1,top+yy*2+1),color)
        x += (len(rows[0])+1)*2 - (2 if s in ('ZDOLNOŚCI', 'CHOWAŃCE') and index == 3 else 0)
    return width


def compact_text(s,cx,top,color="blue0",max_gui=34):
    width = narrow_width(s)
    if width > max_gui: raise ValueError(f"Tekst za szeroki: {s}: {width}>{max_gui}")
    x = 2*((cx//2)-(width//2))
    for ch in s:
        rows = NARROW[ch][:8]
        for yy,row in enumerate(rows):
            for xx,bit in enumerate(row):
                if bit == "1": R((x+2*xx,top+2*yy,x+2*xx+1,top+2*yy+1),color)
        x += 2*(len(rows[0])+1)
    return width

def star(x,y,c='gold2'):
    L([(x-3,y),(x+3,y)],c)
    L([(x,y-3),(x,y+3)],c)
    R((x,y,x,y),'paper4')

def leaf(x,y,direction=1):
    G([(x,y),(x+3*direction,y-3),(x+6*direction,y-2),
       (x+5*direction,y+1),(x+2*direction,y+2)],'green1','green0')
    L([(x,y),(x+4*direction,y)],'gold0')

# The book rests on a single leather desk that continues below the folios.
R((0,0,351,443),'table0')
R((0,263,351,443),'leather0')
R((5,266,346,438),'leather1','gold0')
R((9,270,342,434),'leather0')
L([(13,433),(339,433)],'wood2')
for cx in (18,334):
    for cy in (274,426):
        R((cx-2,cy-2,cx+2,cy+2),'gold0','ink')
        R((cx,cy,cx,cy),'gold2')

# Leather cover and page block; the desk appears around the book.
R((4,13,347,265),'ink')
R((5,10,346,263),'leather0','gold0')
R((9,15,342,260),'leather1')
for o in range(4):
    R((14+o,26+o,172-o,260-o),'paper0')
    R((179+o,26+o,337-o,260-o),'paper0')
L([(19,259),(169,259)],'paper2')
L([(183,259),(333,259)],'paper2')
# Leaf edges are staggered around the page block and at the fold.
for y in (34,74,114,154,194,234,258):
    L([(11,y),(16,y+1)],'paper2')
    L([(336,y+1),(341,y)],'paper2')
for x in (166,169,172,179,182,185):
    L([(x,39),(x,259)],'paper0' if x in (166,185) else 'paper1')

# Open folios with hard-edged shading; light falls from upper left.
G([(13,27),(78,23),(154,26),(171,34),(171,251),(165,260),
   (84,259),(13,257)],'paper2','paper0')
G([(182,34),(198,26),(273,23),(339,27),(339,257),(268,259),
   (187,260),(181,251)],'paper1','paper0')
G([(17,31),(90,28),(156,30),(168,38),(168,252),(162,257),
   (88,256),(17,254)],'paper3')
G([(184,38),(198,30),(264,28),(335,31),(335,254),(264,256),
   (190,257),(184,252)],'paper2')
G([(18,34),(154,32),(166,39),(166,251),(160,255),(18,251)],'paper4')
G([(186,39),(198,32),(334,34),(334,251),(192,255),(186,251)],'paper3')
# The folios curve into a deep, stepped gutter.
G([(157,33),(166,35),(173,41),(173,249),(168,259),(160,255)],'paper2')
G([(165,35),(173,38),(176,45),(176,253),(170,260),(166,251)],'paper1')
G([(172,38),(177,42),(179,254),(176,261),(172,254)],'paper0')
G([(180,42),(186,36),(193,33),(188,254),(182,261),(178,254)],'paper0')
G([(184,42),(190,36),(194,34),(190,251),(185,259),(181,253)],'paper1')
L([(177,43),(177,253)],'leather0',2)
L([(171,45),(171,249)],'gold0')
L([(184,45),(184,249)],'gold0')
# Turned upper and lower corners.
G([(314,31),(334,32),(334,52)],'paper1','paper0')
G([(334,52),(323,41),(314,31)],'paper4','paper0')
G([(18,242),(18,255),(37,258)],'paper1','paper0')
G([(18,242),(27,251),(37,258)],'paper4','paper0')
# Silk bookmarks jut beyond the boards.
G([(338,63),(351,63),(351,111),(345,105),(339,112)],'blue0','gold0')
R((342,70,347,95),'blue1')
G([(1,191),(14,191),(14,235),(8,229),(2,236)],'red0','gold0')
R((4,198,9,219),'red1')
G([(322,248),(335,248),(335,262),(329,258),(323,262)],'gold1','gold0')

# Gold and blue enamel plaque at the title band, engraved into cover.
R((45,3,307,30),'ink')
R((48,5,304,27),'gold0')
R((51,7,301,25),'blue0')
for xa,xb in ((55,87),(265,297)):
    L([(xa,9),(xb,9)],'blue2')
    L([(xa,23),(xb,23)],'gold1')
for x in (58,294):
    E((x-5,11,x+5,21),'gold1','gold0')
    R((x-1,14,x+1,18),'paper4')
bitmap_text('Księga' if VARIANT == 'old' else 'MENU',176,22,'gold2',sx=2,sy=2,max_width=235)

def miniature_frame(x,y):
    # Exactly one two-by-two-slot tile, with no click target slivers.
    R((x,y,x+71,y+71),'paper1','gold0')
    R((x+3,y+3,x+68,y+68),'paper3','paper0')
    R((x+6,y+4,x+65,y+50),'gold0','ink')
    R((x+8,y+6,x+63,y+48),'gold2')
    R((x+10,y+8,x+61,y+46),'blue0')
    for px,py in ((x+7,y+5),(x+64,y+5),(x+7,y+49),(x+64,y+49)):
        R((px-2,py-2,px+2,py+2),'gold2','gold0')
        R((px,py,px,py),'paper4')
    return x+10,y+8

def scene_adventure(x,y):
    R((x,y,x+51,y+38),'sky')
    E((x+38,y+3,x+46,y+11),'gold2','gold1')
    G([(x,y+26),(x+12,y+7),(x+23,y+25)],'stone1','blue0')
    G([(x+13,y+27),(x+30,y+4),(x+51,y+29)],'stone0','blue0')
    G([(x+27,y+10),(x+30,y+4),(x+34,y+10)],'paper4')
    G([(x+9,y+13),(x+12,y+7),(x+15,y+13)],'paper4')
    R((x,y+29,x+51,y+38),'green1')
    # The pale path remains visible through a separate stone trilith.
    G([(x+18,y+38),(x+24,y+27),(x+28,y+24),(x+34,y+38)],'gold1')
    G([(x+22,y+38),(x+26,y+28),(x+29,y+25),(x+30,y+38)],'paper2')
    R((x+5,y+15,x+12,y+37),'stone0','ink')
    R((x+7,y+16,x+10,y+35),'stone2')
    R((x+39,y+15,x+46,y+37),'stone0','ink')
    R((x+41,y+16,x+44,y+35),'stone2')
    R((x+5,y+12,x+46,y+18),'stone0','ink')
    R((x+8,y+13,x+43,y+15),'stone2')
    L([(x+17,y+19),(x+17,y+26)],'stone1')
    L([(x+34,y+19),(x+34,y+26)],'stone1')
    for q in (2,15,47): L([(x+q,y+35),(x+q+2,y+31)],'green0')

def scene_character(x,y):
    R((x,y,x+51,y+38),'blue0')
    # Shield-shaped heraldic field behind the hooded traveller.
    G([(x+7,y+3),(x+44,y+3),(x+43,y+27),(x+26,y+38),
       (x+8,y+27)],'gold1','ink')
    G([(x+10,y+5),(x+41,y+5),(x+40,y+26),(x+26,y+35),
       (x+11,y+26)],'red0')
    L([(x+26,y+5),(x+26,y+34)],'gold2')
    L([(x+11,y+19),(x+40,y+19)],'gold2')
    G([(x+5,y+38),(x+12,y+28),(x+21,y+25),(x+31,y+25),
       (x+40,y+28),(x+47,y+38)],'blue0','ink')
    G([(x+10,y+38),(x+17,y+29),(x+22,y+28),(x+26,y+35),
       (x+29,y+28),(x+35,y+29),(x+42,y+38)],'stone1','ink')
    G([(x+16,y+25),(x+17,y+12),(x+21,y+7),(x+30,y+7),
       (x+35,y+13),(x+36,y+25),(x+31,y+29),(x+20,y+29)],'blue0','ink')
    G([(x+19,y+15),(x+23,y+11),(x+29,y+11),(x+33,y+15),
       (x+31,y+23),(x+26,y+26),(x+21,y+23)],'paper1','ink')
    G([(x+17,y+14),(x+21,y+7),(x+26,y+4),(x+31,y+7),
       (x+35,y+14),(x+32,y+16),(x+29,y+12),(x+23,y+12),
       (x+20,y+17)],'blue1','ink')
    L([(x+24,y+7),(x+26,y+5),(x+28,y+7)],'gold2')
    R((x+22,y+17,x+23,y+18),'ink')
    R((x+29,y+17,x+30,y+18),'ink')
    L([(x+24,y+22),(x+28,y+22)],'red0')
    G([(x+23,y+30),(x+26,y+34),(x+29,y+30),(x+28,y+38),
       (x+24,y+38)],'gold1','gold0')

def scene_equipment(x,y):
    R((x,y,x+51,y+38),'blue0')
    for yy in (7,18,30): L([(x+2,y+yy),(x+49,y+yy)],'blue1')
    # Heavy heater shield, rim and boss.
    G([(x+8,y+8),(x+30,y+8),(x+31,y+26),(x+20,y+37),
       (x+7,y+26)],'gold1','ink')
    G([(x+11,y+11),(x+27,y+11),(x+28,y+25),(x+20,y+33),
       (x+10,y+25)],'red1','gold2')
    R((x+17,y+13,x+21,y+27),'gold2')
    R((x+13,y+18,x+25,y+22),'gold2')
    R((x+18,y+18,x+20,y+22),'paper4')
    # Blade to the right, separated from shield by a dark outline.
    G([(x+35,y+30),(x+43,y+4),(x+47,y+2),(x+46,y+8),
       (x+39,y+31)],'stone2','ink')
    L([(x+42,y+9),(x+37,y+28)],'paper4')
    L([(x+31,y+28),(x+43,y+31)],'gold2',3)
    L([(x+37,y+29),(x+34,y+37)],'wood0',3)
    E((x+32,y+36,x+35,y+38),'gold2','ink')
    star(x+47,y+31)

def scene_skills(x,y):
    R((x,y,x+51,y+38),'wood2')
    G([(x+0,y+27),(x+25,y+18),(x+51,y+27),(x+51,y+38),
       (x+0,y+38)],'wood1')
    for xx,yy in ((8,10),(42,8),(6,22),(46,22)):
        star(x+xx,y+yy,'gold1')
    # Seven-pointed magical flare over an open codex.
    G([(x+26,y+3),(x+29,y+11),(x+36,y+10),(x+31,y+16),
       (x+34,y+22),(x+26,y+18),(x+18,y+22),(x+21,y+16),
       (x+16,y+10),(x+23,y+11)],'gold1','red0')
    G([(x+26,y+7),(x+28,y+13),(x+32,y+13),(x+28,y+17),
       (x+26,y+20),(x+24,y+17),(x+20,y+13),(x+24,y+13)],'paper4')
    G([(x+3,y+24),(x+16,y+21),(x+26,y+24),(x+36,y+21),
       (x+49,y+24),(x+46,y+35),(x+29,y+32),(x+26,y+35),
       (x+23,y+32),(x+6,y+35)],'paper2','ink')
    G([(x+5,y+24),(x+17,y+23),(x+25,y+26),(x+25,y+32),
       (x+17,y+29),(x+6,y+32)],'paper4')
    G([(x+27,y+26),(x+35,y+23),(x+47,y+24),(x+46,y+32),
       (x+35,y+29),(x+27,y+32)],'paper3')
    L([(x+26,y+24),(x+26,y+35)],'gold0',2)
    for xx in (9,33): L([(x+xx,y+27),(x+xx+9,y+29)],'gold0')

def scene_professions(x,y):
    R((x,y,x+51,y+38),'stone0')
    for yy in (9,19,29): L([(x,y+yy),(x+51,y+yy)],'stone1')
    for xx in (12,31,43): L([(x+xx,y+2),(x+xx,y+8)],'blue0')
    G([(x+5,y+36),(x+10,y+26),(x+14,y+32),(x+20,y+23),
       (x+23,y+36)],'fire','red0')
    G([(x+9,y+36),(x+12,y+30),(x+16,y+35)],'gold2')
    # Anvil silhouette: horn, face, waist, foot.
    G([(x+12,y+19),(x+34,y+19),(x+40,y+16),(x+39,y+23),
       (x+29,y+26),(x+28,y+32),(x+34,y+32),(x+37,y+36),
       (x+16,y+36),(x+18,y+32),(x+23,y+32),(x+22,y+26),
       (x+9,y+23)],'stone2','ink')
    L([(x+14,y+20),(x+35,y+20)],'paper4',2)
    L([(x+20,y+28),(x+27,y+28)],'paper3')
    # Raised hammer; gold sparks at impact.
    L([(x+37,y+20),(x+47,y+7)],'wood2',3)
    G([(x+39,y+3),(x+50,y+8),(x+47,y+13),(x+36,y+8)],'stone2','ink')
    star(x+36,y+22,'gold2')

def scene_trade(x,y):
    R((x,y,x+51,y+38),'blue0')
    G([(x,y+24),(x+12,y+13),(x+21,y+24)],'paper2')
    G([(x+28,y+23),(x+42,y+12),(x+51,y+20),(x+51,y+38),
       (x+28,y+38)],'paper1')
    R((x,y+31,x+51,y+38),'wood1')
    # Upright balance with independent pans and dangling chains.
    R((x+24,y+7,x+27,y+31),'gold0','ink')
    R((x+16,y+32,x+35,y+35),'gold1','ink')
    L([(x+8,y+11),(x+44,y+11)],'gold2',3)
    E((x+24,y+7,x+28,y+12),'gold2','ink')
    for a,b in ((10,17),(41,34)):
        L([(x+a,y+12),(x+a-5,y+24)],'gold0')
        L([(x+a,y+12),(x+a+5,y+24)],'gold0')
        G([(x+a-7,y+24),(x+a+7,y+24),(x+a+4,y+28),
           (x+a-4,y+28)],'gold1','ink')
    for xx,yy in ((5,32),(10,29),(43,30),(47,33)):
        E((x+xx,y+yy,x+xx+4,y+yy+4),'gold2','gold0')

def scene_pets(x,y):
    R((x,y,x+51,y+38),'blue1')
    G([(x,y+25),(x+12,y+12),(x+23,y+28)],'green0')
    G([(x+29,y+29),(x+42,y+8),(x+51,y+25)],'green0')
    R((x,y+32,x+51,y+38),'green1')
    # Fox body, big curled tail and clear expression.
    G([(x+13,y+27),(x+5,y+15),(x+10,y+16),(x+21,y+24),
       (x+22,y+32),(x+17,y+35)],'red1','ink')
    G([(x+5,y+15),(x+11,y+17),(x+14,y+24)],'paper4')
    G([(x+20,y+22),(x+32,y+18),(x+41,y+22),(x+44,y+30),
       (x+36,y+35),(x+22,y+34)],'fire','ink')
    G([(x+28,y+21),(x+35,y+29),(x+42,y+25),(x+44,y+30),
       (x+36,y+35),(x+28,y+32)],'paper4')
    G([(x+28,y+18),(x+27,y+7),(x+33,y+13),(x+38,y+7),
       (x+42,y+18),(x+40,y+26),(x+29,y+26)],'fire','ink')
    G([(x+29,y+15),(x+29,y+10),(x+33,y+14)],'red0')
    G([(x+37,y+14),(x+40,y+10),(x+40,y+16)],'red0')
    R((x+31,y+19,x+32,y+20),'ink')
    R((x+38,y+19,x+39,y+20),'ink')
    G([(x+34,y+22),(x+37,y+22),(x+36,y+24)],'ink')
    R((x+25,y+33,x+27,y+38),'ink')
    R((x+39,y+32,x+41,y+38),'ink')
    leaf(x+7,y+31)

def scene_community(x,y):
    R((x,y,x+51,y+38),'blue0')
    G([(x,y+22),(x+13,y+9),(x+25,y+23)],'blue1')
    G([(x+18,y+23),(x+34,y+7),(x+51,y+24)],'blue1')
    R((x,y+30,x+51,y+38),'green0')
    for xx in (6,45):
        E((x+xx,y+15,x+xx+7,y+23),'paper1','ink')
        G([(x+xx-2,y+31),(x+xx-1,y+22),(x+xx+8,y+22),
           (x+xx+10,y+31)],'red0' if xx==6 else 'blue2','ink')
    E((x+22,y+17,x+28,y+23),'paper1','ink')
    G([(x+19,y+36),(x+20,y+23),(x+29,y+23),(x+32,y+36)],'stone0','ink')
    L([(x+13,y+36),(x+35,y+36)],'wood2',3)
    L([(x+21,y+35),(x+29,y+28)],'wood3',2)
    L([(x+29,y+35),(x+22,y+28)],'wood3',2)
    G([(x+23,y+32),(x+22,y+25),(x+26,y+20),(x+27,y+26),
       (x+31,y+22),(x+30,y+32)],'fire','red0')
    G([(x+25,y+31),(x+25,y+25),(x+28,y+28),(x+27,y+32)],'gold2')
    for xx,yy in ((18,13),(33,15),(28,8)): star(x+xx,y+yy,'gold1')

def scene_map(x,y):
    R((x,y,x+51,y+38),'green1')
    G([(x+2,y+2),(x+16,y+6),(x+16,y+35),(x+2,y+31)],'paper2','ink')
    G([(x+17,y+6),(x+34,y+2),(x+34,y+31),(x+17,y+35)],'paper4','ink')
    G([(x+35,y+2),(x+49,y+6),(x+49,y+35),(x+35,y+31)],'paper2','ink')
    L([(x+6,y+28),(x+18,y+23),(x+25,y+28),(x+38,y+19),(x+46,y+21)],'blue1',3)
    G([(x+31,y+6),(x+38,y+6),(x+35,y+15)],'red1','ink')
    E((x+33,y+5,x+37,y+9),'gold2','ink')
    R((x+10,y+11,x+12,y+13),'green0')
    R((x+42,y+26,x+44,y+28),'green0')

def scene_quests(x,y):
    R((x,y,x+51,y+38),'blue0')
    R((x+9,y+3,x+41,y+36),'paper2','ink')
    R((x+13,y+6,x+38,y+33),'paper4','gold0')
    for yy in (12,18,24):
        R((x+17,y+yy,x+20,y+yy+3),'gold2','gold0')
        L([(x+23,y+yy+2),(x+34,y+yy+2)],'blue1',2)
    R((x+18,y+28,x+34,y+30),'red1')
    star(x+40,y+5)

def scene_travel(x,y):
    R((x,y,x+51,y+38),'blue1')
    G([(x,y+29),(x+14,y+17),(x+29,y+30),(x+51,y+13),(x+51,y+38),(x,y+38)],'green0')
    G([(x+7,y+38),(x+24,y+22),(x+38,y+38)],'paper1','gold0')
    # Portal and a readable two-way signpost.
    R((x+4,y+7,x+18,y+29),'stone0','ink')
    R((x+7,y+10,x+15,y+27),'blue2','gold2')
    R((x+26,y+8,x+29,y+36),'wood2','ink')
    G([(x+17,y+11),(x+42,y+11),(x+47,y+16),(x+42,y+21),(x+17,y+21)],'gold1','ink')
    L([(x+21,y+15),(x+37,y+15)],'paper4',2)
    E((x+42,y+3,x+48,y+9),'gold2','gold0')
    star(x+11,y+5)

def scene_journal(x,y):
    R((x,y,x+51,y+38),'wood1')
    for yy in (6,19,32): L([(x,y+yy),(x+51,y+yy)],'wood2')
    G([(x+5,y+30),(x+10,y+9),(x+26,y+7),(x+42,y+10),(x+47,y+30),
       (x+27,y+35)],'gold1','ink')
    G([(x+9,y+28),(x+12,y+12),(x+25,y+10),(x+25,y+31)],'paper4','gold0')
    G([(x+27,y+10),(x+40,y+12),(x+43,y+28),(x+27,y+31)],'paper2','gold0')
    L([(x+26,y+10),(x+26,y+33)],'ink',2)
    # Three expedition badges, large enough to survive the pixel scale.
    E((x+14,y+15,x+21,y+22),'gold2','red0')
    star(x+17,y+18,'paper4')
    G([(x+32,y+15),(x+36,y+12),(x+40,y+16),(x+36,y+22)],'blue1','gold0')
    R((x+33,y+25,x+39,y+27),'red1','gold0')

def scene_mail(x,y):
    R((x,y,x+51,y+38),'blue1')
    for xx,yy in ((6,5),(45,7),(42,30)): star(x+xx,y+yy,'blue2')
    G([(x+6,y+26),(x+18,y+14),(x+41,y+14),(x+47,y+27),
       (x+41,y+34),(x+10,y+34)],'paper3','ink')
    G([(x+9,y+17),(x+26,y+27),(x+44,y+17)],'paper4','gold0')
    L([(x+10,y+32),(x+21,y+23)],'gold0')
    L([(x+42,y+32),(x+31,y+23)],'gold0')
    E((x+22,y+23,x+30,y+31),'red1','red0')
    R((x+25,y+26,x+27,y+28),'gold2')

def scene_soon(x,y):
    # A closed, wax-sealed folio: muted scene, still lit with gold trim.
    R((x,y,x+51,y+38),'leather0')
    R((x+8,y+3,x+43,y+36),'paper0','ink')
    R((x+12,y+6,x+39,y+33),'paper1','gold0')
    for yy in (12,17,22): L([(x+17,y+yy),(x+34,y+yy)],'gold0')
    L([(x+8,y+4),(x+43,y+35)],'red0',3)
    L([(x+43,y+4),(x+8,y+35)],'red0',3)
    E((x+20,y+14,x+32,y+26),'red1','ink')
    E((x+23,y+17,x+29,y+23),'gold1','red0')
    R((x+25,y+18,x+27,y+22),'gold2')

def scene_portal(x,y):
    R((x,y,x+51,y+38),'blue0')
    G([(x,y+31),(x+11,y+20),(x+22,y+31)],'green0')
    G([(x+25,y+31),(x+41,y+15),(x+51,y+28),(x+51,y+38),(x,y+38)],'green1')
    # Dwuczęściowy kamienny łuk, światło i ścieżka prowadząca w głąb.
    G([(x+9,y+38),(x+20,y+27),(x+26,y+24),(x+34,y+38)],'gold1')
    G([(x+17,y+38),(x+23,y+27),(x+27,y+26),(x+29,y+38)],'paper2')
    R((x+10,y+7,x+18,y+32),'stone0','ink')
    R((x+33,y+7,x+41,y+32),'stone0','ink')
    R((x+9,y+5,x+42,y+13),'stone0','ink')
    R((x+13,y+8,x+38,y+11),'stone2')
    R((x+18,y+14,x+33,y+28),'blue2','gold2')
    R((x+21,y+17,x+30,y+28),'sky')
    star(x+26,y+19,'paper4')
    for xx in (12,37):
        R((x+xx,y+16,x+xx+3,y+19),'gold2','gold0')

tiles = [
    (14,34,'Mapa świata',scene_map),
    (86,34,'Zadania',scene_quests),
    (194,34,'Podróże',scene_travel),
    (266,34,'Dziennik',scene_journal),
    (14,106,'Postać',scene_character),
    (86,106,'Umiejętności',scene_skills),
    (194,106,'Profesje',scene_professions),
    (266,106,'Chowańce',scene_pets),
    (14,178,'Handel',scene_trade),
    (86,178,'Społeczność',scene_community),
    (194,178,'Poczta',scene_mail),
    (266,178,'Wkrótce' if VARIANT == 'old' else 'Portal',scene_soon if VARIANT == 'old' else scene_portal),
]
assert len(tiles) == 12
for tx,ty,label,painter in tiles:
    assert tx in (14,86,194,266) and ty in (34,106,178)
    mx,my = miniature_frame(tx,ty)
    painter(mx,my)
    d.rectangle((mx-1,my-1,mx+52,my+39),outline=C['ink'],width=1)
    d.rectangle((mx,my,mx+51,my+38),outline=C['ink'],width=1)

# The caption plate spans almost the full tile; ink stays >=3 texels inboard.
tile_caption_boxes = []
short = {'Mapa świata':'MAPA', 'Zadania':'ZADANIA', 'Podróże':'PODRÓŻE',
         'Dziennik':'DZIENNIK', 'Postać':'POSTAĆ',
         'Umiejętności':'ZDOLNOŚCI', 'Profesje':'PROFESJE', 'Chowańce':'CHOWAŃCE',
         'Handel':'HANDEL', 'Społeczność':'DRUŻYNA', 'Poczta':'POCZTA', 'Portal':'PORTAL'}
two = {'Mapa świata':('MAPA','ŚWIATA'), 'Zadania':('ZADA','NIA'),
       'Podróże':('PODRÓ','ŻE'), 'Dziennik':('DZIENNIK',),
       'Postać':('POSTAĆ',), 'Umiejętności':('UMIEJĘT','NOŚCI'),
       'Profesje':('PROFE','SJE'), 'Chowańce':('CHOWAŃ','CE'),
       'Handel':('HANDEL',), 'Społeczność':('SPOŁECZ','NOŚĆ'),
       'Poczta':('POCZTA',), 'Portal':('PORTAL',)}
for tx,ty,label,_ in tiles:
    cap_top = ty+38 if VARIANT == 'c' else ty+44
    R((tx,cap_top,tx+71,ty+70),'paper2','gold0')
    R((tx+1,cap_top+1,tx+70,ty+69),'paper4')
    if VARIANT == 'old':
        lines = ('Dziennik',) if label == 'Dziennik przygód' else (label,)
    elif VARIANT == 'a':
        lines = (short[label],)
    elif VARIANT == 'b':
        lines = (short[label].upper(),)
    else:
        lines = two[label]
    for index,line in enumerate(lines):
        if VARIANT == 'old':
            width = bitmap_text(line,tx+36,ty+65,'blue0',bold=True,max_width=66)
        elif VARIANT == 'a':
            width = narrow_text(line,tx+36,ty+48,'blue0',max_gui=32)*2
        elif VARIANT == 'b':
            width = narrow_text(line,tx+36,ty+52,'blue0',max_gui=34)*2
        else:
            width = compact_text(line,tx+36,ty+38+16*index,'blue0',max_gui=34)*2
        left = tx+36-width//2
        assert left-tx >= 3 and tx+71-(left+width-1) >= 3, (label,width)
    tile_caption_boxes.append((tx,cap_top,tx+72,ty+71))

# A narrow footer makes the final row feel bound into the leather cover.
for xa,xb in ((23,158),(194,329)):
    L([(xa,254),(xb,254)],'gold0')
    L([(xa+9,257),(xb-9,257)],'paper1')

def seal(cx,cy,color,icon):
    E((cx-15,cy-15,cx+15,cy+15),'ink','gold0')
    E((cx-13,cy-13,cx+13,cy+13),color,'gold2')
    E((cx-10,cy-10,cx+10,cy+10),color,'gold0')
    for dx,dy in ((0,-14),(13,0),(0,13),(-13,0)):
        R((cx+dx-1,cy+dy-1,cx+dx+1,cy+dy+1),'gold2')
    if icon == '?':
        bitmap_text('?',cx,cy+10,'paper4',sx=3,sy=3,max_width=22)
    elif icon == 'gear':
        for dx,dy in ((-11,0),(11,0),(0,-11),(0,11),(-8,-8),(8,-8),(-8,8),(8,8)):
            R((cx+dx-1,cy+dy-1,cx+dx+1,cy+dy+1),'gold2','gold0')
        E((cx-9,cy-9,cx+9,cy+9),'gold2','gold0')
        E((cx-4,cy-4,cx+4,cy+4),'blue0','ink')
        R((cx-1,cy-1,cx+1,cy+1),'paper4')
    elif icon == 'x':
        L([(cx-7,cy-7),(cx+7,cy+7)],'gold0',6)
        L([(cx+7,cy-7),(cx-7,cy+7)],'gold0',6)
        L([(cx-7,cy-7),(cx+7,cy+7)],'paper4',3)
        L([(cx+7,cy-7),(cx-7,cy+7)],'paper4',3)

# Inventory row A. Each medallion sits in a whole 36x36 slot.
for args in ((32,296,'red1','?'),(68,296,'blue1','gear'),
             (320,296,'red0','x')):
    seal(*args)
R((116,278,235,313),'ink','gold0')
R((119,281,232,310),'paper1','gold2')
R((122,284,229,307),'paper3','gold0')
if VARIANT == 'old':
    bitmap_text('Zmień postać',176,299,'blue0',bold=True,max_width=90)
else:
    narrow_text('ZMIEŃ POSTAĆ',176,286,'blue0',max_gui=48)

# Inventory row B, columns 1..7: empty item sockets for plugin sprites.
R((50,314,301,349),'ink','gold0')
R((53,317,298,346),'wood2','gold1')
for col in range(1,8):
    x=14+36*col
    R((x+2,319,x+33,344),'leather0','gold0')
    R((x+4,321,x+31,342),'blue0','wood1')
    L([(x+6,322),(x+29,322)],'blue1')
    L([(x+6,341),(x+29,341)],'gold0')

# Row C: nine waypoint medallions, corresponding to nine hidden inventory slots.
for col in range(9):
    x=32+36*col
    E((x-17,351,x+17,385),'ink','gold0')
    E((x-14,354,x+14,382),'blue0','gold2')
    E((x-11,357,x+11,379),'blue1','gold0')
if VARIANT == 'old':
    R((106,358,246,388),'leather0','gold0')
    R((110,361,242,385),'blue0','gold1')
    bitmap_text('Wędrowca',176,379,'gold2',sx=2,sy=2,max_width=126)
else:
    R((85,395,267,423),'leather0','gold0')
    R((89,399,263,419),'blue0','gold1')
    narrow_text('SZYBKA PODRÓŻ',176,400,'gold2',max_gui=82)
# The hotbar region frames the quick-travel row.
L([(19,392),(333,392)],'wood2')
L([(19,430),(333,430)],'wood2')

def find_orphans(pic):
    """Find a texel with no matching colour among its eight neighbours."""
    pix = pic.load()
    return [(x,y,pix[x,y]) for y in range(1,pic.height-1)
            for x in range(1,pic.width-1)
            if all(pix[x+dx,y+dy] != pix[x,y]
                   for dy in (-1,0,1) for dx in (-1,0,1) if dx or dy)]

pre_orphans = find_orphans(im)
pix = im.load()
for x,y,color in pre_orphans:
    in_caption = (5 <= y <= 26 or any(t <= y <= t+71 for t in (34,106,178))
                  or 278 <= y <= 313 or 358 <= y <= 388)
    is_ink = color in (C['blue0'],C['gold2'],C['paper4'])
    if in_caption and is_ink:
        # Preserve a solitary accent/dot by connecting it to the letter.
        pix[x+1,y] = color
    else:
        neighbours = [pix[x+dx,y+dy] for dx,dy in
                      ((-1,0),(1,0),(0,-1),(0,1))]
        pix[x,y] = Counter(neighbours).most_common(1)[0][0]
for _ in range(8):
    remaining = find_orphans(im)
    if not remaining:
        break
    for x,y,color in remaining:
        pix[x+1,y] = color
remaining = find_orphans(im)
assert not remaining, remaining[:8]

if VARIANT != 'old':
    im.save(OUT/f'ksiega_{VARIANT}.png')
    if VARIANT == 'a':
        im.save(OUT/'ksiega_2x.png')
        for i,(x,y) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
            im.crop((x,y,x+176,y+222)).save(OUT/f'kb_{i}.png')
else:
    im.save(OUT/'ksiega_obecna.png')

# Every chest and hidden inventory slot is documented, including empty slots.
slot_map = {}
for tx,ty,label,_ in tiles:
    col,row=(tx-14)//36,(ty-34)//36
    for dr in (0,1):
        for dc in (0,1):
            slot=9*(row+dr)+col+dc
            assert slot not in slot_map
            slot_map[slot]=label
assert len(slot_map)==48
slot_lines=['MENU Q — siatka 9 × 6; jeden kafel = dokładnie 2 × 2 sloty.',
            'Kolumna 4 jest pustym grzbietem księgi.',
            'Skrzynia (sloty 0–53):']
for slot in range(54):
    slot_lines.append(f'{slot:02d} | rząd {slot//9}, kol {slot%9} | {slot_map.get(slot,"pusty grzbiet")}')
slot_lines.append('')
slot_lines.append('Ukryty ekwipunek gracza (kolumny 0–8):')
for name,y in (('A',278),('B',314),('C',350),('hotbar',394)):
    slot_lines.append(f'{name} | y={y}')
    for col in range(9):
        action = ('Pomoc' if col==0 else 'Ustawienia' if col==1 else
                  'Zmień postać' if col in (3,4,5) else
                  'Zamknij' if col==8 else 'puste') if name=='A' else (
                  'Stan i akcje — gniazdo przedmiotu' if 1 <= col <= 7 else 'puste') if name=='B' else (
                  'Szybka podróż — punkt ' + str(col+1)) if name=='C' else 'puste'
        slot_lines.append(f'  {name}{col} | x={14+36*col} | {action}')
(OUT/'sloty.txt').write_text('\n'.join(slot_lines)+'\n',encoding='utf-8')

assert im.size == (352,444)
assert len(im.getcolors(352*444) or []) <= 32
print('Zapisano menu K12; 352×444, RGB, paleta',len(im.getcolors(352*444)),'sieroty',len(remaining))
print('Szerokości podpisów:', ', '.join(f'{label}={text_width(label,bold=True)}' for _,_,label,_ in tiles))
