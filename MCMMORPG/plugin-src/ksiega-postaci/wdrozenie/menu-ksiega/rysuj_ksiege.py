#!/usr/bin/env python3
"""Księga Wędrowca — deterministyczna tekstura GUI i plansze kontrolne."""
from pathlib import Path
import json
from collections import Counter
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent
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
    root = OUT.parent.parent / 'mcfont' / 'assets' / 'minecraft'
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
    # The original Latin motto uses a middle dot absent from these two atlases.
    dot = Image.new('L',(3,8))
    ImageDraw.Draw(dot).rectangle((1,3,2,4),fill=255)
    glyphs['·'] = (dot,3,7)
    for char in 'ąćęłńóśźżĘ':
        assert char in glyphs and glyphs[char][0].getbbox(), char
    return glyphs

GLYPHS = load_minecraft_glyphs()

# Tom Thumb uppercase bitmaps, packed row-major, from the published BDF
# conversion: https://github.com/adafruit/Adafruit-GFX-Library/blob/master/Fonts/TomThumb.h
# M, N, I and W are tailored for legibility at GUI pixel scale.
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
for char, base, mark in (
    ('Ą','A','ogonek'), ('Ć','C','acute'), ('Ę','E','ogonek'),
    ('Ń','N','acute'), ('Ó','O','acute'), ('Ś','S','acute'),
    ('Ź','Z','acute'), ('Ż','Z','dot'),
):
    NARROW[char] = marked_capital(base, mark)

def narrow_width(s):
    return sum(len(NARROW[ch][0]) for ch in s) + len(s)-1

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
    for ch in s:
        rows = NARROW[ch]
        assert all(len(row)==len(rows[0]) for row in rows), ch
        for yy,row in enumerate(rows):
            for xx,bit in enumerate(row):
                if bit == '1':
                    R((x+xx*2,top+yy*2,
                       x+xx*2+1,top+yy*2+1),color)
        x += (len(rows[0])+1)*2
    return width

def text_width(s, sx=1, bold=False):
    missing = [char for char in s if char not in GLYPHS]
    if missing:
        raise ValueError(f'Brak glifów Minecrafta: {missing}')
    return (sum(GLYPHS[c][1]+1 for c in s)-1)*sx + int(bold)

def bitmap_text(s, cx, baseline, color='blue0', sx=1, sy=1, bold=False,
                max_width=None):
    width = text_width(s,sx,bold)
    if max_width is not None and width > max_width:
        raise ValueError(f'Tekst za szeroki: {s}: {width}>{max_width}')
    x = int(cx-width//2)
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
        x += (advance+1)*sx
    return width

def star(x,y,c='gold2'):
    L([(x-3,y),(x+3,y)],c)
    L([(x,y-3),(x,y+3)],c)
    R((x,y,x,y),'paper4')

def leaf(x,y,direction=1):
    G([(x,y),(x+3*direction,y-3),(x+6*direction,y-2),
       (x+5*direction,y+1),(x+2*direction,y+2)],'green1','green0')
    L([(x,y),(x+4*direction,y)],'gold0')

# Unmarked leather tabletop: item sprites own the entire inventory field.
R((0,0,351,443),'table0')
R((0,282,351,302),'table1')
R((0,430,351,443),'table1')
L([(0,303),(351,303)],'wood1')
L([(0,429),(351,429)],'wood1')

# Leather cover and page block; the desk appears around the book.
R((4,13,347,252),'ink')
R((5,10,346,250),'leather0','gold0')
R((9,15,342,248),'leather1')
for o in range(4):
    R((14+o,26+o,172-o,250-o),'paper0')
    R((179+o,26+o,337-o,250-o),'paper0')
L([(19,249),(169,249)],'paper2')
L([(183,249),(333,249)],'paper2')
# Leaf edges are staggered around the page block and at the fold.
for y in (34,74,114,154,194,234,246):
    L([(11,y),(16,y+1)],'paper2')
    L([(336,y+1),(341,y)],'paper2')
for x in (166,169,172,179,182,185):
    L([(x,39),(x,248)],'paper0' if x in (166,185) else 'paper1')

# Open folios with hard-edged shading; light falls from upper left.
G([(13,27),(78,23),(154,26),(171,34),(171,241),(165,250),
   (84,249),(13,247)],'paper2','paper0')
G([(182,34),(198,26),(273,23),(339,27),(339,247),(268,249),
   (187,250),(181,241)],'paper1','paper0')
G([(17,31),(90,28),(156,30),(168,38),(168,242),(162,247),
   (88,246),(17,244)],'paper3')
G([(184,38),(198,30),(264,28),(335,31),(335,244),(264,246),
   (190,247),(184,242)],'paper2')
G([(18,34),(154,32),(166,39),(166,241),(160,245),(18,241)],'paper4')
G([(186,39),(198,32),(334,34),(334,241),(192,245),(186,241)],'paper3')
# The folios curve into a deep, stepped gutter.
G([(157,33),(166,35),(173,41),(173,239),(168,249),(160,245)],'paper2')
G([(165,35),(173,38),(176,45),(176,243),(170,250),(166,241)],'paper1')
G([(172,38),(177,42),(179,244),(176,251),(172,244)],'paper0')
G([(180,42),(186,36),(193,33),(188,244),(182,251),(178,244)],'paper0')
G([(184,42),(190,36),(194,34),(190,241),(185,249),(181,243)],'paper1')
L([(177,43),(177,243)],'leather0',2)
L([(171,45),(171,239)],'gold0')
L([(184,45),(184,239)],'gold0')
# Turned upper and lower corners.
G([(314,31),(334,32),(334,52)],'paper1','paper0')
G([(334,52),(323,41),(314,31)],'paper4','paper0')
G([(18,232),(18,245),(37,248)],'paper1','paper0')
G([(18,232),(27,241),(37,248)],'paper4','paper0')
# Silk bookmarks jut beyond the boards.
G([(338,63),(351,63),(351,111),(345,105),(339,112)],'blue0','gold0')
R((342,70,347,95),'blue1')
G([(1,191),(14,191),(14,235),(8,229),(2,236)],'red0','gold0')
R((4,198,9,219),'red1')
G([(322,238),(335,238),(335,252),(329,248),(323,252)],'gold1','gold0')

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
bitmap_text('Księga',176,22,'gold2',sx=2,sy=2,max_width=235)

# Restrained folio marginalia; no text is hidden under a clickable frame.
for x,sgn in ((20,1),(332,-1)):
    L([(x,56),(x+3*sgn,68),(x,80),(x+3*sgn,93)],'green0')
    for y in (59,71,83): leaf(x,y,sgn)
for x,sgn in ((20,1),(332,-1)):
    L([(x,194),(x+3*sgn,205),(x,217),(x+3*sgn,230)],'green0')
    for y in (197,210,223): leaf(x,y,sgn)
# Three tiny margin doodles, below the chapter tiles and outside the controls.
# Carmine flower and curling stem on the left outer edge.
L([(19,224),(18,236),(24,247)],'green0')
leaf(19,233,1)
E((17,220,21,224),'red1','red0')
R((19,221,19,221),'gold2')
# Snail, with an open shell spiral.
G([(28,244),(36,244),(39,247),(33,249),(27,248)],'green1','green0')
E((28,236,36,244),'gold1','gold0')
E((31,238,34,241),'paper2','gold0')
L([(37,245),(39,241),(41,241)],'green0')
R((40,240,40,240),'ink')
# Little bird perched by the right leaf edge.
G([(320,242),(326,239),(332,241),(330,245),(324,246)],'blue1','blue0')
G([(323,243),(327,241),(329,245)],'paper2')
G([(331,242),(335,243),(331,244)],'gold1')
R((329,241,329,241),'ink')
L([(326,245),(325,249)],'gold0')

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

tiles = [
    (14,34,'PRZYGODA',scene_adventure),
    (86,34,'POSTAĆ',scene_character),
    (14,106,'EKWIPUNEK',scene_equipment),
    (86,106,'ZDOLNOŚCI',scene_skills),
    (194,34,'PROFESJE',scene_professions),
    (266,34,'HANDEL',scene_trade),
    (194,106,'CHOWAŃCE',scene_pets),
    (266,106,'KOMPANIA',scene_community),
]
for tx,ty,label,painter in tiles:
    mx,my = miniature_frame(tx,ty)
    painter(mx,my)
    # Two adjacent one-texel ink keylines keep the scenes distinct at GUI scale 3.
    d.rectangle((mx-1,my-1,mx+52,my+39),outline=C['ink'],width=1)
    d.rectangle((mx,my,mx+51,my+38),outline=C['ink'],width=1)

# Standard plaques leave one GUI pixel on each side of a tile.
# EKWIPUNEK alone needs the full 36 GUI pixels and may cover the tile's gold rim.
tile_caption_boxes = []
for tx,ty,label,_ in tiles:
    left,right,cx = (tx,tx+71,tx+36) if label == 'EKWIPUNEK' else (tx+2,tx+69,tx+36)
    R((left,ty+50,right,ty+71),'paper2','gold0')
    R((left+1,ty+51,right-1,ty+70),'paper4')
    narrow_text(label,cx,ty+52,'blue0',max_gui=36 if label == 'EKWIPUNEK' else 34)
    tile_caption_boxes.append((left,ty+50,right+1,ty+72))

# Ornamental rulings tie each pair of slots to its folio.
for xa,xb in ((25,157),(195,327)):
    L([(xa,181),(xb,181)],'gold0')
    L([(xa+9,184),(xb-9,184)],'gold1')

def seal(cx,cy,color,icon,label):
    # One medallion in each complete 36-pixel-wide GUI slot.
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
    half_width = 40
    R((cx-half_width,214,cx+half_width-1,235),'paper2','gold0')
    R((cx-half_width+1,215,cx+half_width-2,234),'paper4')
    narrow_text(label,cx,216,'blue0',max_gui=38)

seal(68,196,'red1','?','POMOC')
seal(176,196,'blue1','gear','USTAWIENIA')
seal(284,196,'red0','x','ZAMKNIJ')
medallion_caption_boxes = [(cx-40,214,cx+40,236) for cx in (68,176,284)]
for x in (31,141,211,321):
    G([(x,247),(x+3,244),(x+6,247),(x+3,250)],'gold1','gold0')

# Bookplate centered in the free GUI band y=127..137.
R((103,254,249,274),'leather0','gold0')
R((111,255,241,273),'blue0','gold1')
bitmap_text('Wędrowca',176,270,'gold2',sx=2,sy=2,max_width=126)
# Decorative brass clasps end with the cover at y=126 GUI.
for x in (18,333):
    R((x-5,240,x+5,252),'gold0','ink')
    R((x-2,243,x+2,249),'gold2')

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
    in_caption = (5 <= y <= 26 or 88 <= y <= 104 or
                  160 <= y <= 176 or 216 <= y <= 231 or 254 <= y <= 274)
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
(OUT/'sieroty.txt').write_text(
    'Test: piksel bez sąsiada tego samego koloru w otoczeniu 8 pól.\n'
    f'Przed retuszem: {len(pre_orphans)}\nPo retuszu: {len(remaining)}\n',
    encoding='utf-8')
assert not remaining, remaining[:8]

im.save(OUT/'ksiega.png')
im.save(OUT/'podglad_x1.png')
im.resize((704,888),Image.Resampling.NEAREST).save(OUT/'podglad_x2.png')
im.resize((528,666),Image.Resampling.NEAREST).save(OUT/'podglad_x3_symulacja.png')

slot_lines = ['Sloty skrzyni 0–53; kolumny 0–8, wiersze 0–5.']
for tx,ty,label,_ in tiles:
    col,row = (tx-14)//36,(ty-34)//36
    slots = [9*(row+dr)+col+dc for dr in (0,1) for dc in (0,1)]
    slot_lines.append(f'{label}: {", ".join(map(str,slots))} (kolumny {col}–{col+1}, wiersze {row}–{row+1})')
for label,col in (('POMOC',1),('USTAWIENIA',4),('ZAMKNIJ',7)):
    slot_lines.append(f'{label}: {36+col} (kolumna {col}, wiersz 4; x={7+18*col}..{25+18*col}, y=89..107 GUI; górne granice wyłączne)')
(OUT/'sloty_przyciskow.txt').write_text('\n'.join(slot_lines)+'\n',encoding='utf-8')

# Coverage proof. Green blocks mark the eight exact 2×2 slot targets;
# cyan marks the three seal click slots. All tile outlines land on slot edges.
grid = im.copy()
gd = ImageDraw.Draw(grid)
digits = {
    '0': ('111','101','101','101','111'),
    '1': ('010','110','010','010','111'),
    '2': ('111','001','111','100','111'),
    '3': ('111','001','111','001','111'),
    '4': ('101','101','111','001','001'),
    '5': ('111','100','111','001','111'),
    '6': ('111','100','111','101','111'),
    '7': ('111','001','001','001','001'),
    '8': ('111','101','111','101','111'),
    ',': ('000','000','000','010','100'),
}
def grid_label(x,y,s):
    for n,ch in enumerate(s):
        for yy,line in enumerate(digits[ch]):
            for xx,v in enumerate(line):
                if v=='1': grid.putpixel((x+n*4+xx,y+yy),C['ink'])
for row in range(6):
    for col in range(9):
        x,y=14+36*col,34+36*row
        gd.rectangle((x,y,x+35,y+35),outline=C['blue2'],width=1)
        grid_label(x+2,y+2,f'{col},{row}')
for tx,ty,_,_ in tiles:
    gd.rectangle((tx,ty,tx+71,ty+71),outline=C['green0'],width=2)
for col in (1,4,7):
    x,y=14+36*col,34+36*4
    gd.rectangle((x,y,x+35,y+35),outline=C['red1'],width=2)
grid.resize((704,888),Image.Resampling.NEAREST).save(OUT/'siatka.png')

# Inspection sheets, generated from the actual finished texture.
mini = Image.new('RGB',(4*52*4+3*18,4*39*2+3*32),C['wood0'])
for n,(tx,ty,label,_) in enumerate(tiles):
    piece=im.crop((tx+10,ty+8,tx+62,ty+47)).resize((208,156),Image.Resampling.NEAREST)
    mini.paste(piece,((n%4)*226,(n//4)*188))
mini.save(OUT/'zoom_miniatury.png')

word_boxes = tile_caption_boxes + [
    *medallion_caption_boxes,
]
zoom_cell_w, zoom_cell_h = 736, 200
labels=Image.new('RGB',(3*zoom_cell_w,4*zoom_cell_h),C['wood0'])
for n,box in enumerate(word_boxes):
    crop=im.crop(box)
    enlarged=crop.resize((crop.width*8,crop.height*8),Image.Resampling.NEAREST)
    labels.paste(enlarged,((n%3)*zoom_cell_w+8,(n//3)*zoom_cell_h+8))
labels.save(OUT/'zoom_podpisy.png')

# Each GUI pixel is enlarged 10 times. The identifying letter is printed
# below its bitmap so the specimens can be checked against the source font.
alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZĄĆĘŁŃÓŚŹŻ'
assert len(set(NARROW[ch] for ch in alphabet)) == len(alphabet)
cell_w, cell_h, columns = 92, 126, 9
alphabet_sheet = Image.new('RGB',(columns*cell_w,4*cell_h),C['paper4'])
ad = ImageDraw.Draw(alphabet_sheet)
try:
    caption_font = ImageFont.truetype('DejaVuSans.ttf',20)
except OSError:
    caption_font = ImageFont.load_default()
for index,char in enumerate(alphabet):
    col,row = index%columns,index//columns
    ox,oy = col*cell_w,row*cell_h
    ad.rectangle((ox,oy,ox+cell_w-1,oy+cell_h-1),outline=C['gold0'])
    rows = NARROW[char]
    gx = ox+(cell_w-len(rows[0])*10)//2
    gy = oy+4
    for yy,line in enumerate(rows):
        for xx,bit in enumerate(line):
            if bit == '1':
                ad.rectangle((gx+xx*10,gy+yy*10,gx+xx*10+9,gy+yy*10+9),
                             fill=C['blue0'])
    ad.text((ox+cell_w//2,oy+108),char,fill=C['ink'],
            font=caption_font,anchor='mm')
alphabet_sheet.save(OUT/'alfabet.png')

width_lines = ['Szerokości napisów (px GUI):']
width_lines += [f'{label}: {narrow_width(label)} px GUI' for _,_,label,_ in tiles]
width_lines += [f'{label}: {narrow_width(label)} px GUI'
                for label in ('POMOC','USTAWIENIA','ZAMKNIJ')]
(OUT/'szerokosci.txt').write_text('\n'.join(width_lines)+'\n',encoding='utf-8')

assert im.size == (352,444)
assert len(im.getcolors(352*444) or []) <= 32
print('Zapisano A3; rozmiar 352×444, RGB, paleta',len(im.getcolors(352*444)),'sieroty',len(remaining))
print('Szerokości podpisów w px GUI:', ', '.join(f'{label}={narrow_width(label)}' for _,_,label,_ in tiles))
print('Medaliony:', ', '.join(f'{label}={narrow_width(label)}' for label in ('POMOC','USTAWIENIA','ZAMKNIJ')))
