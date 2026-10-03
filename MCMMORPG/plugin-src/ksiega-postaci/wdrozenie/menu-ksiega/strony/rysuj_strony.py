#!/usr/bin/env python3
"""Siedem kolejnych kart Księgi, eksportowanych jako cztery glify Nexo."""
from pathlib import Path
from PIL import Image
import ksiega_wspolne as W

OUT = Path(__file__).resolve().parent / 'out'
ROOT = Path(__file__).resolve().parent.parent
R, L, G, E = W.R, W.L, W.G, W.E


def field(x,y,bg='blue0'):
    R((x,y,x+51,y+38),bg)

def stats(x,y):
    field(x,y,'green0')
    G([(x+8,y+4),(x+44,y+4),(x+42,y+35),(x+10,y+35)],'paper2','ink')
    G([(x+11,y+6),(x+41,y+6),(x+39,y+32),(x+13,y+32)],'paper4')
    for yy in (11,17,23,29): L([(x+16,y+yy),(x+23,y+yy)],'gold0')
    L([(x+25,y+27),(x+30,y+21),(x+34,y+23),(x+39,y+12)],'red1',2)
    for xx,yy in ((25,27),(30,21),(34,23),(39,12)): R((x+xx,y+yy,x+xx+1,y+yy+1),'gold2')

def attributes(x,y):
    field(x,y,'blue0')
    for xx,yy,col in ((12,16,'red1'),(26,10,'blue2'),(40,16,'green1')):
        G([(x+xx,y+yy-9),(x+xx+7,y+yy),(x+xx,y+yy+12),(x+xx-7,y+yy)],'gold1','ink')
        G([(x+xx,y+yy-6),(x+xx+4,y+yy),(x+xx,y+yy+8),(x+xx-4,y+yy)],col,'paper4')
    L([(x+4,y+35),(x+48,y+35)],'gold1',2)

def faith(x,y):
    field(x,y,'blue0')
    R((x+5,y+29,x+47,y+37),'stone0','ink')
    R((x+12,y+24,x+40,y+30),'stone2','ink')
    R((x+23,y+13,x+29,y+25),'paper4','gold0')
    G([(x+26,y+3),(x+30,y+11),(x+26,y+16),(x+22,y+11)],'fire','red0')
    G([(x+26,y+7),(x+28,y+11),(x+26,y+13),(x+24,y+11)],'gold2')
    W.star(x+10,y+10,'gold2'); W.star(x+42,y+9,'gold2')

def change_character(x,y):
    W.scene_character(x,y)
    G([(x+3,y+18),(x+9,y+14),(x+9,y+22)],'gold2','ink')
    G([(x+49,y+18),(x+43,y+14),(x+43,y+22)],'gold2','ink')

def skill(x,y):
    field(x,y,'wood1')
    G([(x+4,y+28),(x+17,y+24),(x+26,y+29),(x+36,y+24),(x+49,y+28),(x+46,y+36),(x+27,y+33),(x+6,y+36)],'paper2','ink')
    G([(x+6,y+28),(x+17,y+26),(x+25,y+29),(x+25,y+34),(x+7,y+34)],'paper4')
    G([(x+27,y+29),(x+35,y+26),(x+47,y+28),(x+45,y+34),(x+27,y+34)],'paper3')
    G([(x+26,y+3),(x+29,y+12),(x+37,y+13),(x+30,y+18),(x+26,y+24),(x+22,y+18),(x+15,y+13),(x+23,y+12)],'gold2','red0')
    W.star(x+26,y+12,'paper4')

def tree(x,y):
    field(x,y,'blue0')
    L([(x+26,y+33),(x+26,y+15),(x+14,y+9)],'gold1',3)
    L([(x+26,y+15),(x+39,y+8)],'gold1',3)
    L([(x+26,y+26),(x+10,y+23)],'gold1',3)
    L([(x+26,y+26),(x+42,y+25)],'gold1',3)
    for xx,yy,col in ((26,32,'paper4'),(14,9,'red1'),(39,8,'blue2'),(10,23,'green1'),(42,25,'gold2')):
        E((x+xx-5,y+yy-5,x+xx+5,y+yy+5),col,'ink')
        R((x+xx-1,y+yy-1,x+xx+1,y+yy+1),'paper4')

def casting(x,y):
    field(x,y,'blue0')
    G([(x+6,y+37),(x+12,y+27),(x+18,y+25),(x+22,y+19),(x+25,y+21),(x+27,y+15),(x+31,y+17),(x+33,y+23),(x+42,y+25),(x+47,y+32),(x+42,y+38)],'paper1','ink')
    L([(x+22,y+20),(x+20,y+29)],'gold0',2)
    G([(x+28,y+2),(x+31,y+8),(x+39,y+10),(x+32,y+14),(x+29,y+21),(x+25,y+14),(x+18,y+11),(x+25,y+8)],'gold2','red0')
    W.star(x+29,y+10,'paper4')

def spellbook(x,y):
    field(x,y,'wood1')
    G([(x+4,y+11),(x+18,y+7),(x+26,y+11),(x+35,y+7),(x+48,y+11),(x+45,y+35),(x+28,y+32),(x+26,y+36),(x+24,y+32),(x+7,y+35)],'gold1','ink')
    G([(x+7,y+13),(x+18,y+10),(x+24,y+13),(x+24,y+31),(x+9,y+32)],'paper4')
    G([(x+28,y+13),(x+35,y+10),(x+45,y+13),(x+43,y+32),(x+28,y+31)],'paper3')
    L([(x+26,y+11),(x+26,y+35)],'ink',2)
    E((x+33,y+17,x+40,y+24),'blue1','gold0')
    W.star(x+36,y+20,'gold2')

def auction(x,y):
    field(x,y,'blue0')
    R((x+5,y+31,x+47,y+38),'wood1','ink')
    R((x+28,y+28,x+43,y+32),'gold1','ink')
    L([(x+16,y+28),(x+32,y+7)],'wood2',4)
    G([(x+21,y+3),(x+40,y+16),(x+36,y+22),(x+17,y+9)],'stone2','ink')
    L([(x+23,y+5),(x+37,y+15)],'paper4',2)
    W.star(x+35,y+27,'gold2')

def shop(x,y):
    field(x,y,'sky')
    R((x+6,y+14,x+46,y+36),'wood2','ink')
    G([(x+4,y+13),(x+10,y+4),(x+42,y+4),(x+48,y+13)],'red1','ink')
    for xx in (13,24,35): R((x+xx,y+5,x+xx+5,y+13),'paper4')
    R((x+11,y+20,x+41,y+27),'paper2','ink')
    for xx in (14,23,32): E((x+xx,y+20,x+xx+6,y+26),'gold2','gold0')
    R((x+7,y+29,x+45,y+33),'wood0','ink')

def buyback(x,y):
    W.scene_trade(x,y)
    E((x+34,y+17,x+45,y+28),'gold2','ink')
    E((x+37,y+20,x+42,y+25),'gold1','gold0')
    R((x+39,y+21,x+40,y+24),'paper4')

def party(x,y):
    field(x,y,'blue0')
    for xx,yy,col in ((7,14,'stone1'),(22,7,'gold1'),(37,14,'red1')):
        G([(x+xx,y+yy+14),(x+xx+1,y+yy+4),(x+xx+7,y+yy),(x+xx+13,y+yy+4),(x+xx+14,y+yy+14)],col,'ink')
        R((x+xx+3,y+yy+9,x+xx+11,y+yy+11),'paper4','ink')
        R((x+xx+6,y+yy+2,x+xx+8,y+yy+5),'gold2')
    L([(x+4,y+34),(x+48,y+34)],'gold1',2)

def friends(x,y):
    field(x,y,'blue0')
    G([(x+1,y+28),(x+14,y+17),(x+26,y+24),(x+32,y+18),(x+49,y+28),(x+38,y+38),(x+28,y+32),(x+22,y+37),(x+12,y+33)],'paper1','ink')
    G([(x+4,y+29),(x+15,y+22),(x+23,y+26),(x+16,y+32)],'red1','ink')
    G([(x+47,y+29),(x+37,y+22),(x+30,y+26),(x+37,y+32)],'blue2','ink')
    L([(x+17,y+27),(x+28,y+30)],'paper4',3)
    W.star(x+26,y+10,'gold2')

def guild(x,y):
    field(x,y,'blue0')
    R((x+9,y+3,x+12,y+38),'wood2','ink')
    G([(x+12,y+5),(x+44,y+5),(x+43,y+29),(x+29,y+37),(x+13,y+29)],'gold1','ink')
    G([(x+16,y+9),(x+40,y+9),(x+39,y+27),(x+28,y+32),(x+17,y+27)],'red0','gold2')
    G([(x+27,y+12),(x+30,y+21),(x+37,y+21),(x+32,y+25),(x+28,y+30),(x+24,y+25),(x+19,y+21),(x+26,y+21)],'paper4')

def pvp(x,y):
    field(x,y,'blue0')
    for points in ([(x+8,y+5),(x+37,y+34)],[(x+44,y+5),(x+15,y+34)]): L(points,'ink',7)
    L([(x+8,y+5),(x+37,y+34)],'stone2',4)
    L([(x+44,y+5),(x+15,y+34)],'paper4',4)
    L([(x+29,y+27),(x+42,y+21)],'gold2',4)
    L([(x+10,y+21),(x+23,y+27)],'gold2',4)
    W.star(x+26,y+18,'gold2')

def key_q(x,y):
    field(x,y,'wood1')
    R((x+7,y+4,x+44,y+35),'ink','gold0')
    R((x+10,y+7,x+41,y+32),'stone1','paper2')
    R((x+13,y+10,x+38,y+29),'paper3','gold0')
    W.bitmap_text('Q',x+26,y+26,'blue0',sx=2,sy=2,bold=True,max_width=24)

def about(x,y):
    field(x,y,'wood1')
    G([(x+4,y+10),(x+17,y+6),(x+26,y+10),(x+35,y+6),(x+48,y+10),(x+45,y+35),(x+28,y+32),(x+26,y+36),(x+24,y+32),(x+7,y+35)],'gold1','ink')
    G([(x+7,y+12),(x+17,y+9),(x+24,y+12),(x+24,y+31),(x+9,y+32)],'paper4')
    G([(x+28,y+12),(x+35,y+9),(x+45,y+12),(x+43,y+32),(x+28,y+31)],'paper3')
    L([(x+26,y+11),(x+26,y+35)],'ink',2)
    for yy in (16,21,26):
        L([(x+12,y+yy),(x+21,y+yy)],'gold0')
        L([(x+31,y+yy),(x+40,y+yy)],'blue1')
    W.star(x+26,y+4,'gold2')

def soon(x,y):
    W.scene_soon(x,y)
    R((x+24,y+17,x+28,y+23),'gold2','red0')
    R((x+25,y+18,x+27,y+19),'paper4')

def dungeon(x,y):
    field(x,y,'sky')
    G([(x,y+31),(x+12,y+8),(x+24,y+22),(x+35,y+6),(x+51,y+29),(x+51,y+38),(x,y+38)],'green1','ink')
    G([(x+10,y+36),(x+15,y+13),(x+25,y+8),(x+37,y+15),(x+43,y+36)],'stone1','ink')
    G([(x+16,y+37),(x+19,y+21),(x+26,y+17),(x+33,y+22),(x+36,y+37)],'blue0','ink')
    G([(x+20,y+38),(x+25,y+29),(x+30,y+38)],'paper2')
    for xx,yy in ((6,30),(11,34),(42,31),(47,35)):
        R((x+xx,y+yy,x+xx+2,y+yy+4),'green0')
        E((x+xx-1,y+yy-2,x+xx+3,y+yy+2),'paper4','gold1')

def locked(x,y):
    field(x,y,'stone0')
    G([(x+8,y+38),(x+8,y+9),(x+17,y+3),(x+35,y+3),(x+44,y+9),(x+44,y+38)],'blue0','ink')
    for xx in (15,24,33): R((x+xx,y+5,x+xx+3,y+36),'stone2','ink')
    R((x+10,y+17,x+42,y+20),'stone1','ink')
    R((x+10,y+33,x+42,y+36),'stone1','ink')
    E((x+23,y+20,x+29,y+27),'gold2','gold0')

def islands(x,y):
    field(x,y,'sky')
    for xx,yy in ((3,25),(31,19)):
        E((x+xx,y+yy,x+xx+18,y+yy+7),'paper4')
    G([(x+8,y+17),(x+22,y+11),(x+39,y+16),(x+35,y+24),(x+24,y+30),(x+14,y+24)],'green1','ink')
    G([(x+14,y+24),(x+24,y+30),(x+35,y+24),(x+26,y+37)],'stone1','ink')
    R((x+23,y+6,x+26,y+18),'wood1','ink')
    G([(x+17,y+12),(x+24,y+3),(x+31,y+12)],'green0','ink')

PAGES = [
    ('hub_postac','Postać',change_character,[('Statystyki','Statystyki',stats),('Atrybuty','Atrybuty',attributes),('Wiara','Wiara',faith),('Zmień postać','Zmień postać',change_character)]),
    ('hub_umiejetnosci','Umiejętności',spellbook,[('Umiejętności','Umiejętności',skill),('Drzewka rozwoju','Drzewka',tree),('Tryb rzucania','Rzucanie',casting),('Księga zaklęć','Zaklęcia',spellbook)]),
    ('hub_ekonomia','Handel',buyback,[('Dom aukcyjny','Aukcje',auction),('Sklep','Sklep',shop),('Skup','Skup',buyback)]),
    ('hub_spolecznosc','Społeczność',party,[('Drużyna','Drużyna',party),('Znajomi','Znajomi',friends),('Gildia','Gildia',guild)]),
    ('hub_ustawienia','Ustawienia',pvp,[('Tryb PvP','Tryb PvP',pvp)]),
    ('hub_pomoc','Pomoc',key_q,[('Sterowanie','Sterowanie',key_q),('O menu','O menu',about),('Wkrótce','Wkrótce',soon)]),
    ('stolica_podroze','Podróże',dungeon,[('Loch 001','Loch 001',dungeon),('Kolejne lochy','Dalsze lochy',locked),('Wyspy','Wyspy',islands)]),
]

def tile_positions(n):
    if n == 1: return [(230,106)]
    if n == 2: return [(194,106),(266,106)]
    if n == 3: return [(230,70),(194,142),(266,142)]
    if n == 4: return [(194,70),(266,70),(194,142),(266,142)]
    if n == 5: return [(194,34),(266,34),(230,106),(194,178),(266,178)]
    return [(194,34),(266,34),(194,106),(266,106),(194,178),(266,178)]

def slot_file(dest, ident, placements):
    slot_map={9*r+c:'info' for r in range(6) for c in range(4)}
    for x,y,label in placements:
        c=(x-14)//36; r=(y-34)//36
        for dr in (0,1):
            for dc in (0,1):
                key=9*(r+dr)+c+dc
                assert key not in slot_map
                slot_map[key]=label
    lines=[f'{ident} — siatka 9 × 6; kafle i ilustracja wyrównane do slotów.',
           'Kolumna 4 jest pustym grzbietem księgi.','Skrzynia (sloty 0–53):']
    for slot in range(54):
        role='grzbiet' if slot%9==4 else slot_map.get(slot,'pusty')
        lines.append(f'{slot:02d} | rząd {slot//9}, kol {slot%9} | {role}')
    lines += ['', 'Ukryty ekwipunek gracza (kolumny 0–8):']
    for name,y in (('A',278),('B',314),('C',350),('hotbar',394)):
        lines.append(f'{name} | y={y}')
        for c in range(9):
            role = ('Pomoc' if c==0 else 'Ustawienia' if c==1 else 'Wróć do Księgi' if c in (3,4,5) else 'Zamknij' if c==8 else 'puste') if name=='A' else ('Stan i akcje — gniazdo przedmiotu' if 1<=c<=7 else 'puste') if name=='B' else 'puste'
            lines.append(f'  {name}{c} | x={14+36*c} | {role}')
    (dest/'sloty.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')

def main():
    OUT.mkdir(exist_ok=True)
    original=(ROOT/'hub_glowne.yml').read_text(encoding='utf-8')
    server=Path('/opt/docker/mc/data/world1/plugins/DeluxeMenus/gui_menus/hub_glowne.yml').read_text(encoding='utf-8')
    def title(source): return next(line.split("'",2)[1] for line in source.splitlines() if line.startswith('menu_title:'))
    template=title(original)
    assert template==title(server)
    source_chars=[chr(c) for c in range(0x4168,0x416C)]
    assert all(template.count(ch)==1 for ch in source_chars)
    glyph_lines=['# Cztery ćwiartki na stronę; char U+4A00 + 4*i + N-1.']
    title_lines=[]
    previews=[]
    miniatures=[]
    for i,(ident,heading,hero,tiles) in enumerate(PAGES):
        image=W.new_canvas()
        W.draw_book(); W.draw_title(heading)
        W.hero_frame(14,34,hero)
        W.left_ornament()
        placements=[]
        for (x,y),(label,caption,painter) in zip(tile_positions(len(tiles)),tiles):
            W.tile(x,y,label,painter,caption)
            placements.append((x,y,label))
            miniatures.append((ident,label,caption,painter))
        W.draw_footer(); W.draw_lower('Wróć do Księgi')
        W.clean_orphans()
        assert image.size==(352,444)
        assert len(image.getcolors(352*444) or [])<=32
        assert not W.find_orphans(image)
        assert {color for _, color in image.getcolors(352*444)}<=set(W.C.values())
        dest=OUT/ident; dest.mkdir(exist_ok=True)
        image.save(dest/f'{ident}_2x.png')
        for n,(left,top) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
            image.crop((left,top,left+176,top+222)).save(dest/f'{ident}_{n}.png')
        slot_file(dest,ident,placements)
        codes=[0x4A00+4*i+j for j in range(4)]
        for n,code in enumerate(codes,1):
            glyph_lines += [f'{ident}_{n}:', '  font: minecraft:default',f'  texture: nexo:gui/{ident}_{n}',f'  ascent: {13 if n<=2 else -98}', '  height: 111',f'  char: "{chr(code)}"']
        menu_title=template
        for old,new in zip(source_chars,map(chr,codes)):
            menu_title=menu_title.replace(old,new)
        title_lines += [f'{ident} | ' + "menu_title: '"+menu_title+"'" + ' | ' + ', '.join(f'U+{c:04X}' for c in codes)]
        previews.append(image.resize((176,222),Image.Resampling.NEAREST))
        print(ident,'colors',len(image.getcolors(352*444)),'orphans',len(W.find_orphans(image)))
    (OUT/'glify_strony.yml').write_text('\n'.join(glyph_lines)+'\n',encoding='utf-8')
    (OUT/'tytuly.txt').write_text('\n'.join(title_lines)+'\n',encoding='utf-8')
    k12=Image.open(ROOT/'k12/ksiega_2x.png').convert('RGB').resize((176,222),Image.Resampling.NEAREST)
    sheet=Image.new('RGB',(176*4,222*2),W.C['table0'])
    for idx,page in enumerate([k12]+previews): sheet.paste(page,((idx%4)*176,(idx//4)*222))
    sheet.save(OUT/'arkusz.png')
    cells=4; width=72*4; height=72*4
    contact=Image.new('RGB',(cells*width,((len(miniatures)+cells-1)//cells)*height),W.C['table0'])
    for idx,(_,label,caption,painter) in enumerate(miniatures):
        tile=W.new_canvas()
        W.tile(14,34,label,painter,caption)
        crop=tile.crop((14,34,86,106)).resize((width,width),Image.Resampling.NEAREST)
        contact.paste(crop,((idx%cells)*width,(idx//cells)*height))
    contact.save(OUT/'kafle_x4.png')

if __name__=='__main__': main()
