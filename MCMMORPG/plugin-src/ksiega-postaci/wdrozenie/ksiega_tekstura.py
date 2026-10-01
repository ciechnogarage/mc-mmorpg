#!/usr/bin/env python3
"""Finalny render ekwipunku A. Uruchom: python3 ksiega_A_final.py.

Tekstura klienta ma 256x256 px; gotowy panel zajmuje 176x166 px od (0, 0).
Podgląd i arkusz porównawczy są generowane osobno, bez zmiany world1.
"""
import ast
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
ICONS = HERE.parent / 'ikony'
WORLD = Path('/opt/docker/mc/data/world1')
HUB = WORLD / 'plugins/Nexo/pack/assets/nexo/textures/gui/menu_hub_bg.png'
WYNN = WORLD / 'docs/research/wynncraft/klatki/17_ekwipunek_E_pergamin_A8m00.jpg'
PW, PH = 176, 166

# Kolory wybrane z istniejącego huba. 17 nieprzezroczystych barw w teksturze.
P = {
    'ink': '#0d0704',
    'wood0': '#32190b', 'wood1': '#4c2b16', 'wood2': '#664126',
    'wood3': '#7f5a3a', 'wood4': '#997552',
    'brass0': '#784f1e', 'brass1': '#b99953', 'brass2': '#d9c277',
    'brass3': '#f9eba2',
    'green0': '#0c2d19', 'green1': '#143d21', 'green2': '#1d4c29',
    'green3': '#285b31',
    'canvas0': '#78603e', 'canvas1': '#997552', 'canvas2': '#b2926e',
    'red': '#954321',
}

FONT = {
 'A':['010','101','111','101','101'], 'B':['110','101','110','101','110'],
 'C':['011','100','100','100','011'], 'D':['110','101','101','101','110'],
 'E':['111','100','110','100','111'], 'I':['111','010','010','010','111'],
 'J':['011','001','001','101','010'], 'K':['101','101','110','101','101'],
 'L':['100','100','100','100','111'], 'N':['101','111','111','111','101'],
 'O':['010','101','101','101','010'], 'P':['110','101','110','100','100'],
 'R':['110','101','110','101','101'], 'S':['011','100','010','001','110'],
 'T':['111','010','010','010','010'], 'U':['101','101','101','101','111'],
 'W':['101','101','111','111','101'], 'Y':['101','101','010','010','010'],
 'Z':['111','001','010','100','111'], 'Ż':['111','001','010','100','111'],
 'Ć':['011','100','100','100','011'], 'Ń':['101','111','111','111','101'],
 '1':['010','110','010','010','111'], '2':['111','001','111','100','111'],
 '3':['111','001','111','001','111'], '4':['101','101','111','001','001'],
 '5':['111','100','111','001','111'], '6':['111','100','111','101','111'],
 '7':['111','001','010','010','010'], '8':['111','101','111','101','111'],
 '9':['111','101','111','001','111'], ' ':['000']*5,
}
LABELS = ((38,9,'POSTAĆ'), (104,9,'BIŻUTERIA'), (146,19,'SAKWA'))

def rect(d, box, color): d.rectangle(box, fill=P.get(color, color))
def line(d, points, color): d.line(points, fill=P.get(color, color), width=1)
def poly(d, points, color): d.polygon(points, fill=P.get(color, color))
def dot(d, x, y, color): d.point((x, y), fill=P.get(color, color))

def small(d, x, y, s, color):
    for ch in s.upper():
        for yy, row in enumerate(FONT[ch]):
            for xx, on in enumerate(row):
                if on == '1': dot(d, x+xx, y+yy, color)
        if ch == 'Ż': dot(d, x+1, y-2, color)
        if ch == 'Ć':
            dot(d, x+2, y-2, color)
            dot(d, x+1, y-1, color)
        x += 4

def label(d, x, y, s):
    # Wersaliki 3x5, odstęp 1 px; akcenty zajmują dwa piksele nad nimi.
    # Cień o jeden piksel poniżej i na prawo od całego glifu.
    small(d, x+1, y+1, s, 'green0')
    small(d, x, y, s, 'brass2')

# Dokładnie te same współrzędne co w rysuj_koncepty.py.
ARMOR = [(8,y) for y in (8,26,44,62)]
JEWELRY = [(98,18),(116,18),(98,36),(116,36)]
EQUIP = [(8,84),(26,84),(44,84),(62,84),(80,84),(77,62),(154,28)]
BAG = [(x,y) for y in (84,102,120) for x in (8,26,44,62,80,98,116,134,152)
       if not (y == 84 and x in (8,26,44,62,80))]
BAR = [(x,142) for x in (8,26,44,62,80,98,116,134,152)]
SLOTS = ARMOR + JEWELRY + EQUIP + BAG + BAR
assert len(SLOTS) == len(set(SLOTS)) == 46

def verify_original_geometry():
    """Odczytaj listy pozycji z funkcji pixel_slots w źródle konceptów."""
    tree=ast.parse((HERE/'rysuj_koncepty.py').read_text())
    fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='pixel_slots')
    picks=[]
    for node in fn.body:
        if isinstance(node,ast.Assign) and isinstance(node.targets[0],ast.Name):
            if node.targets[0].id in ('armor','jewelry','equip','bag','bar'):
                picks.append(node)
    source=ast.Module(body=picks,type_ignores=[])
    scope={}
    exec(compile(source,str(HERE/'rysuj_koncepty.py'),'exec'),scope)
    old=scope['armor']+scope['jewelry']+scope['equip']+scope['bag']+scope['bar']
    assert len(old)==46 and set(old)==set(SLOTS), 'Geometria slotów różni się od konceptu A'

def slot(d, x, y, group):
    # Jedna barwa obrysu. Wnętrze o ton ciemniejsze od otaczającego materiału.
    if group in ('armor', 'jewel', 'equip'):
        base, bottom = 'green0', 'green2'
    elif group == 'bag':
        base, bottom = 'canvas0', 'canvas2'
    else:
        base, bottom = 'wood0', 'wood3'
    rect(d,(x,y,x+15,y+15),'ink')
    rect(d,(x+1,y+1,x+14,y+14),base)
    # Wklęsłość: cień na górnej i lewej ścianie, odbite światło na dolnej.
    line(d,[(x+2,y+14),(x+13,y+14)],bottom)
    dot(d,x+14,y+13,bottom)

def corner(d,x,y,sx,sy):
    rect(d,(x,y,x+5,y+5),'brass0')
    rect(d,(x+1,y+1,x+4,y+4),'brass1')
    line(d,[(x+1,y+1),(x+4,y+1)],'brass2')
    dot(d,x+2,y+2,'ink')
    dot(d,x+1 if sx>0 else x+4,y+1 if sy>0 else y+4,'brass3')

def frame(d):
    rect(d,(0,0,175,165),'ink')
    rect(d,(1,1,174,164),'wood0')
    rect(d,(2,2,173,163),'wood2')
    line(d,[(2,2),(173,2)],'wood4')
    line(d,[(2,2),(2,163)],'wood4')
    line(d,[(173,3),(173,163)],'wood0')
    line(d,[(3,163),(173,163)],'wood0')
    # Deski: tylko dwie świadome kreski na długi odcinek.
    line(d,[(11,3),(76,3)],'wood3')
    line(d,[(92,3),(165,3)],'wood3')
    line(d,[(3,41),(3,82)],'wood3')
    line(d,[(172,61),(172,95)],'wood3')
    line(d,[(16,162),(65,162)],'wood3')
    line(d,[(110,162),(153,162)],'wood3')
    for x,y,sx,sy in ((1,1,1,1),(169,1,-1,1),(1,159,1,-1),(169,159,-1,-1)):
        corner(d,x,y,sx,sy)

def upper(d):
    rect(d,(5,3,170,101),'ink')
    rect(d,(6,4,169,100),'brass0')
    line(d,[(6,4),(169,4)],'brass2')
    line(d,[(6,6),(6,100)],'brass1')
    rect(d,(8,5,167,98),'green1')
    line(d,[(8,5),(167,5)],'green3')
    line(d,[(8,5),(8,98)],'green3')
    # Portret: bez nadrukowanej postaci; klient maluje model w tym polu.
    rect(d,(25,17,74,78),'green2')
    line(d,[(25,17),(74,17)],'green3')
    line(d,[(25,17),(25,78)],'green3')
    line(d,[(74,18),(74,78)],'green0')
    line(d,[(26,78),(74,78)],'green0')
    # Cokół pod modelem, czytelny także przy jego renderze z klienta.
    rect(d,(32,75,68,78),'green0')
    line(d,[(33,75),(67,75)],'brass0')
    # Tylko jedna podziałka między postacią i biżuterią.
    line(d,[(95,13),(95,57)],'green3')
    dot(d,95,12,'brass1')
    dot(d,95,58,'brass0')
    for x,y,s in LABELS:
        label(d,x,y,s)

def hand_patch(d):
    # Jedna skórzana naszywka obejmuje drugą rękę i slot 13 pod nią.
    rect(d,(75,59,96,101),'ink')
    rect(d,(76,60,95,100),'wood2')
    line(d,[(76,60),(95,60)],'wood4')
    line(d,[(76,60),(76,99)],'wood4')
    line(d,[(95,61),(95,100)],'wood0')
    for y in (65,72,80,88,95):
        dot(d,94,y,'brass1')
    # Pionowy szew między dwoma aktywnymi polami.
    line(d,[(84,78),(84,83)],'brass0')
    line(d,[(87,78),(87,83)],'brass2')
    dot(d,85,81,'brass2')
    dot(d,86,81,'brass2')

def equipment_strip(d):
    rect(d,(6,80,96,101),'ink')
    rect(d,(7,81,95,100),'wood2')
    rect(d,(8,82,94,99),'green2')
    line(d,[(7,81),(94,81)],'brass1')
    line(d,[(7,82),(7,99)],'wood4')
    for x in (42,76):
        line(d,[(x,82),(x,99)],'brass0')
        line(d,[(x+1,83),(x+1,98)],'green3')
    hand_patch(d)

def satchel(d):
    # Cztery górne pola tworzą klapę; 18 dolnych wnętrze płóciennej torby.
    rect(d,(96,78,170,102),'ink')
    rect(d,(98,80,168,101),'wood3')
    rect(d,(99,82,167,100),'canvas1')
    line(d,[(98,80),(168,80)],'brass1')
    for x in (103,161):
        rect(d,(x,77,x+2,84),'wood0')
        line(d,[(x,78),(x,81)],'brass2')
        dot(d,x+1,83,'brass0')
    rect(d,(4,101,171,140),'wood0')
    rect(d,(5,102,170,139),'wood2')
    rect(d,(7,104,168,138),'canvas1')
    line(d,[(7,104),(168,104)],'canvas2')
    line(d,[(7,120),(168,120)],'canvas0')
    line(d,[(7,121),(168,121)],'canvas2')
    # Oszczędny splot w wolnych 2-pikselowych przerwach między rzędami.
    for y in (118,136):
        for x in range(16,163,18):
            dot(d,x,y,'canvas2')
    for y in (108,115,124,131):
        dot(d,5,y,'brass1')
        dot(d,170,y,'brass1')

def crossed_weapons(d):
    # Miniaturowy znak dłoni/broni, bez osobnej barwy konturu.
    line(d,[(8,137),(12,141)],'brass2')
    line(d,[(12,137),(8,141)],'brass2')
    dot(d,8,137,'brass3')
    dot(d,12,137,'brass3')

def belt(d):
    rect(d,(2,139,173,164),'ink')
    rect(d,(3,140,172,162),'wood1')
    line(d,[(4,140),(171,140)],'brass0')
    line(d,[(4,141),(171,141)],'wood4')
    line(d,[(4,162),(171,162)],'wood0')
    # Wspólna oprawa dłoni 0–1; mosiężny rygiel wspólnego schowka.
    rect(d,(6,138,42,160),'brass0')
    rect(d,(7,139,41,159),'wood2')
    line(d,[(7,139),(41,139)],'brass2')
    line(d,[(7,159),(41,159)],'wood0')
    crossed_weapons(d)
    # Ładownica mikstur 2–4 z jedną profilowaną klapką i trzema szwami.
    rect(d,(43,137,96,161),'brass0')
    rect(d,(44,138,95,160),'wood2')
    poly(d,[(44,137),(95,137),(95,140),(89,141),(50,141),(44,140)],'wood4')
    line(d,[(45,137),(94,137)],'brass2')
    line(d,[(51,141),(88,141)],'wood0')
    for x in (60,78):
        line(d,[(x,159),(x,161)],'brass1')
    for x in (52,87):
        dot(d,x,139,'brass2')
    # Trzy zwykłe komory, ostatnia osobno jako futerał dziennika.
    for x in (98,116,134):
        line(d,[(x-2,140),(x-2,160)],'brass0')
        dot(d,x-2,159,'brass1')
    rect(d,(150,138,169,160),'brass0')
    rect(d,(151,139,168,159),'wood2')
    line(d,[(151,139),(168,139)],'brass2')
    line(d,[(152,159),(167,159)],'wood0')
    # W pasie nad slotami nie ma miejsca na pełne cyfry 3x5 z odstępem.

def ornament(d):
    # Zakładka; trzy tony, bez losowej faktury.
    poly(d,[(80,3),(88,3),(88,15),(84,12),(80,15)],'red')
    line(d,[(81,3),(87,3)],'brass2')
    line(d,[(82,5),(82,10)],'wood4')
    dot(d,84,8,'brass2')
    # Mała latarnia w prawym górnym polu, nad sakwą.
    line(d,[(153,5),(160,5)],'brass0')
    line(d,[(156,6),(156,9)],'brass2')
    rect(d,(153,10,159,16),'ink')
    rect(d,(154,11,158,15),'brass1')
    rect(d,(155,12,157,14),'brass3')
    line(d,[(153,16),(159,16)],'brass0')
    dot(d,156,17,'brass2')
    # Herb na wolnym suknie, pióro i trzy monety. Omija sprite książki
    # w x=104..123, y=61..78 z dwupikselowym marginesem.
    poly(d,[(138,63),(146,63),(146,69),(142,73),(138,69)],'brass0')
    poly(d,[(140,64),(144,64),(144,68),(142,70),(140,68)],'green3')
    dot(d,142,66,'brass2')
    line(d,[(158,57),(155,68)],'brass0')
    line(d,[(158,57),(162,60),(155,65)],'brass2')
    dot(d,155,68,'brass3')
    rect(d,(152,70,159,76),'ink')
    rect(d,(153,71,158,75),'brass0')
    line(d,[(153,71),(157,71)],'brass1')
    line(d,[(154,74),(157,74)],'wood0')
    for x,y in ((162,72),(164,75)):
        rect(d,(x,y,x+2,y+1),'brass0')
        line(d,[(x,y),(x+1,y)],'brass2')

def texture():
    im = Image.new('RGBA',(256,256),(0,0,0,0))
    d = ImageDraw.Draw(im)
    frame(d)
    upper(d)
    equipment_strip(d)
    satchel(d)
    belt(d)
    ornament(d)
    for group, positions in (('armor',ARMOR),('jewel',JEWELRY),('equip',EQUIP),
                             ('bag',BAG),('bar',BAR)):
        for x,y in positions: slot(d,x,y,group)
    return im

def avatar(im):
    d=ImageDraw.Draw(im)
    o='#24171a'; hair='#3c241b'; hair_hi='#744326'; skin='#a16a4b'
    red='#992f22'; red_hi='#ce6040'; teal='#075e50'; teal_hi='#269279'
    shoe='#4a2c1f'; metal='#88999c'
    d.rectangle((41,18,58,30),fill=o);d.rectangle((43,19,57,22),fill=hair)
    d.rectangle((43,20,50,20),fill=hair_hi);d.rectangle((44,23,55,29),fill=skin)
    d.rectangle((44,22,47,25),fill=hair);d.point((47,26),fill=o);d.point((53,26),fill=o)
    d.rectangle((46,29,53,30),fill=o);d.rectangle((38,29,61,53),fill=o)
    d.rectangle((39,30,60,49),fill=red);d.rectangle((39,31,42,46),fill=red_hi)
    d.rectangle((55,30,58,47),fill=red_hi);d.rectangle((43,31,56,48),fill=teal)
    d.rectangle((45,31,47,46),fill=teal_hi)
    for x in (34,61):
        d.rectangle((x,30,x+5,55),fill=o)
        d.rectangle((x+1,31,x+4,49),fill=red)
        d.rectangle((x+1,50,x+4,54),fill=skin)
    d.rectangle((43,49,57,55),fill=o);d.rectangle((44,50,56,54),fill=teal_hi)
    for x in (42,51):
        d.rectangle((x,55,x+7,74),fill=o);d.rectangle((x+1,55,x+6,69),fill=teal)
        d.rectangle((x,70,x+7,74),fill=shoe);d.rectangle((x,74,x+7,75),fill=o)
    d.rectangle((47,38,53,43),fill=o);d.line(((48,39),(51,42)),fill=metal)

def icon(im,name,x,y):
    path=ICONS/(name+'.png')
    if not path.exists(): path=ICONS/'vanilla_slot'/(name+'.png')
    a=Image.open(path).convert('RGBA')
    assert a.size==(16,16),path
    im.alpha_composite(a,(x,y))

def sword(im,x,y):
    d=ImageDraw.Draw(im);o='#23171b';steel='#a8bdc2';shine='#e3e2cc'
    d.polygon([(x+12,y+1),(x+14,y+1),(x+14,y+4),(x+6,y+12),(x+3,y+9)],fill=o)
    d.polygon([(x+12,y+2),(x+13,y+2),(x+13,y+4),(x+6,y+10),(x+5,y+9)],fill=steel)
    d.line([(x+12,y+2),(x+6,y+8)],fill=shine)
    d.line([(x+4,y+8),(x+9,y+13)],fill=o)
    d.line([(x+4,y+9),(x+8,y+13)],fill='#c49a49')
    d.line([(x+5,y+11),(x+2,y+14)],fill=o)
    d.line([(x+5,y+11),(x+3,y+13)],fill='#633b24')

def shield(im,x,y):
    d=ImageDraw.Draw(im);o='#211b1c'
    d.polygon([(x+3,y+2),(x+12,y+2),(x+13,y+4),(x+12,y+10),
               (x+8,y+14),(x+3,y+10),(x+2,y+4)],fill=o)
    d.polygon([(x+4,y+3),(x+11,y+3),(x+11,y+10),(x+8,y+12),(x+4,y+9)],fill='#84969a')
    d.rectangle((x+5,y+4,x+7,y+9),fill='#1b5e50')
    d.line([(x+4,y+3),(x+10,y+3)],fill='#d2c9a6')

def pouch(im,x,y):
    d=ImageDraw.Draw(im);o='#24171a'
    d.rectangle((x+5,y+1,x+10,y+3),fill=o)
    d.rectangle((x+6,y+1,x+9,y+2),fill='#e2b458')
    d.polygon([(x+4,y+4),(x+11,y+4),(x+13,y+7),(x+12,y+13),
               (x+10,y+15),(x+5,y+15),(x+3,y+12),(x+3,y+7)],fill=o)
    d.polygon([(x+5,y+5),(x+10,y+5),(x+12,y+8),(x+11,y+12),
               (x+9,y+13),(x+6,y+13),(x+4,y+11),(x+4,y+8)],fill='#a35631')
    d.line([(x+5,y+6),(x+9,y+6)],fill='#d38045')
    d.rectangle((x+7,y+7,x+8,y+10),fill='#e2b458')

def journal(im,x,y):
    d=ImageDraw.Draw(im);o='#23191a'
    d.polygon([(x+3,y+2),(x+11,y+2),(x+13,y+4),(x+13,y+13),
               (x+4,y+14),(x+2,y+12),(x+2,y+4)],fill=o)
    d.polygon([(x+4,y+3),(x+10,y+3),(x+12,y+5),(x+12,y+12),
               (x+4,y+13),(x+3,y+11),(x+3,y+5)],fill='#644021')
    d.line([(x+4,y+4),(x+10,y+4)],fill='#b68b4a')
    d.line([(x+4,y+12),(x+10,y+12)],fill='#d7bc83')
    d.rectangle((x+6,y+6,x+9,y+7),fill='#b68b4a')

def preview(background):
    im=background.crop((0,0,PW,PH)).copy()
    for name,x,y in [
        ('helmet',8,8),('chestplate',8,26),('leggings',8,44),('boots',8,62),
        ('necklace',98,18),('ring',116,18),('ring',98,36),('charm',116,36),
        ('gloves',8,84),('belt',26,84),('spare_right',44,84),
        ('spare_left',62,84),('right_hand',80,84),('left_hand',77,62),
        ('health_potion',44,142),('mana_potion',62,142),('utility',80,142),
    ]: icon(im,name,x,y)
    avatar(im);sword(im,8,142);shield(im,26,142)
    pouch(im,154,28);journal(im,152,142)
    book=Image.open(ICONS/'recipe_book'/'button.png').convert('RGBA')
    assert book.size==(20,18)
    im.alpha_composite(book,(104,61))
    return im.resize((704,664),Image.Resampling.NEAREST)

def comparison(prev):
    # Równa wysokość ramek; Wynn to kadr z dostarczonej klatki, nie tekstura.
    hub=Image.open(HUB).convert('RGBA').resize((528,504),Image.Resampling.NEAREST)
    ours=prev.resize((528,498),Image.Resampling.NEAREST)
    wynn=Image.open(WYNN).convert('RGB').crop((451,171,823,528))
    wynn=wynn.resize((525,504),Image.Resampling.LANCZOS)
    sheet=Image.new('RGB',(1624,550),'#241914')
    draw=ImageDraw.Draw(sheet)
    labels=('NASZ EKWIPUNEK  x3','HUB  x3','WYNN  /  ZRZUT')
    for i,label in enumerate(labels):
        x=16+i*536
        draw.rectangle((x-2,28,x+530,536),fill='#0d0704')
        draw.text((x,8),label,font=ImageFont.load_default(),fill='#d9c277')
    sheet.paste(ours.convert('RGB'),(16,34))
    sheet.paste(hub.convert('RGB'),(552,34))
    sheet.paste(wynn,(1088+(528-wynn.width)//2,34+(504-wynn.height)//2))
    return sheet

def zoom_labels(bg):
    # Każdy wycinek jest skalowany dokładnie 8x metodą nearest neighbour.
    crops = [(33,3,65,17), (100,3,143,17), (142,13,169,27),
             (4,132,172,142)]
    zooms = [bg.crop(box).resize(((box[2]-box[0])*8,
                                  (box[3]-box[1])*8), Image.Resampling.NEAREST)
             for box in crops]
    sheet = Image.new('RGBA',(1360,260),P['ink'])
    x = 8
    for z in zooms[:3]:
        sheet.alpha_composite(z,(x,8))
        x += z.width+16
    sheet.alpha_composite(zooms[3],(8,144))
    return sheet

def verify_labels(bg):
    # Sprawdzenie każdego piksela każdej litery po złożeniu całej tekstury.
    for x,y,s in LABELS:
        for i,ch in enumerate(s):
            for yy,row in enumerate(FONT[ch]):
                for xx,on in enumerate(row):
                    if on == '1':
                        assert bg.getpixel((x+4*i+xx,y+yy))[:3] == tuple(
                            bytes.fromhex(P['brass2'][1:])), (s,i,ch,xx,yy)
            if ch == 'Ż': accents = ((1,-2),)
            elif ch == 'Ć': accents = ((2,-2),(1,-1))
            else: accents = ()
            for xx,yy in accents:
                assert bg.getpixel((x+4*i+xx,y+yy))[:3] == tuple(
                    bytes.fromhex(P['brass2'][1:])), (s,i,ch,xx,yy)

def main():
    verify_original_geometry()
    bg=texture()
    verify_labels(bg)
    bg.save(HERE/'tekstura_A.png')
    prev=preview(bg)
    prev.save(HERE/'podglad_A.png')
    zoom_labels(bg).save(HERE/'zoom_napisy.png')
    comparison(prev).save(HERE/'porownanie_A.png')
    colors={p for p in bg.crop((0,0,PW,PH)).get_flattened_data() if p[3]}
    assert len(colors)<=22,(len(colors),colors)
    assert bg.getpixel((200,200))[3]==0
    print(f'Gotowe: 46 slotów, {len(colors)} barw, tekstura 256x256.')

if __name__=='__main__':
    main()
