#!/usr/bin/env python3
"""Przerysowuje 15 ikon zaklęć 16×16 i tworzy arkusz porównawczy."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter

import rysuj_F as source

HERE = Path(__file__).resolve().parent
PALETTES = {
    'ogien':       dict(B='#7D3B24', L='#AF572B', main='#FFA15A', mid='#FFC06B', light='#FFE087', white='#FFF5D4'),
    'mroz':        dict(B='#35617A', L='#4885A0', main='#69B9D3', mid='#A4E3EE', light='#D2F7F5', white='#FFFFFF'),
    'leczenie':    dict(B='#285C46', L='#347F58', main='#68C781', mid='#A0E5A1', light='#D5F5B7', white='#F7FFE1'),
    'ochrona':     dict(B='#345A79', L='#477E9B', main='#5FB1CE', mid='#9ADBE7', light='#CBF4F1', white='#FFFFFF'),
    'przywolanie': dict(B='#624477', L='#8057A0', main='#BC8DDD', mid='#D9B0F1', light='#F0D9F8', white='#FFFFFF'),
    'iluzja':      dict(B='#456577', L='#64889A', main='#87B6C8', mid='#BDA9E5', light='#D5F2E9', white='#FFFFFF'),
}
source.SCHOOLS = PALETTES
source.SKIN_DARK = '#C8976D'
GROUPS = source.GROUPS


def custom_symbol(name, p):
    s = source.Symbol()
    a, b, c, w = (p[k] for k in ('main', 'mid', 'light', 'white'))
    if name == 'runa_ognia':
        # Dwupikselowy pierścień i ciemne pole oddzielają znak od obręczy.
        s.ell((2,2,13,13), a)
        s.ell((3,3,12,12), b)
        s.ell((4,4,11,11), p['B'])
        s.detail('line', [(4,5),(5,3),(8,3)], c)
        s.detail('line', [(11,7),(11,10),(9,12)], a)
        s.detail('poly', [(7,5),(10,9),(8,8),(8,11),(6,11),(6,8),(5,9)], c)
        s.detail('line', [(7,6),(8,8)], w)
        for box in ((7,1,8,2),(13,7,14,8),(7,13,8,14),(1,7,2,8)):
            s.rect(box, b)
        for x,y in ((7,1),(13,7),(8,14),(1,8)):
            s.detail('rect', (x,y,x,y), c)
    elif name == 'wielkie_uzdrowienie':
        # Zielone promienie pozostają za regularnym krzyżem z ciemną ramką.
        s.line([(3,3),(5,5)], a, 2)
        s.line([(11,5),(13,3)], a, 2)
        s.line([(3,12),(5,10)], a, 2)
        s.line([(11,10),(13,12)], a, 2)
        s.rect((6,1,8,14), b)
        s.rect((1,6,13,8), b)
        s.rect((5,2,9,13), p['B'])
        s.rect((2,5,12,9), p['B'])
        s.rect((6,2,8,12), c)
        s.rect((3,6,11,8), c)
        s.detail('line', [(6,3),(6,7),(4,7)], w)
        s.detail('line', [(8,9),(8,11)], b)
    elif name == 'oslona':
        s.poly([(2,2),(7,2),(8,1),(9,2),(13,2),(14,4),(14,9),(11,13),(8,14),
                (5,13),(2,9),(1,4)], p['L'])
        s.poly([(3,3),(7,3),(8,2),(9,3),(12,3),(13,5),(13,9),(10,12),
                (8,13),(6,12),(3,9),(2,5)], a)
        s.poly([(3,4),(8,4),(8,12),(6,11),(3,8)], b)
        s.poly([(8,4),(12,4),(12,8),(9,12),(8,12)], a)
        s.detail('line', [(8,4),(8,12)], p['B'])
        s.detail('poly', [(8,5),(11,8),(8,11),(5,8)], p['B'])
        s.detail('poly', [(8,6),(10,8),(8,10),(6,8)], c)
        s.detail('line', [(7,7),(8,6),(9,7)], w)
    elif name == 'uspokojenie':
        # Jasny sierp i dwie gwiazdki nie tworzą ciemnej masy.
        s.poly([(7,1),(5,2),(3,4),(2,7),(3,10),(5,12),(8,13),(11,12),
                (13,10),(10,11),(7,10),(5,8),(5,5)], a)
        s.poly([(7,2),(5,3),(4,5),(4,8),(6,11),(9,12),(11,11),(8,11),
                (6,9),(5,6)], b)
        s.poly([(6,3),(5,5),(5,7),(6,9),(8,10),(7,8),(6,6)], c)
        s.detail('line', [(5,4),(5,7)], w)
        s.line([(11,2),(11,5)], c)
        s.line([(9,3),(13,3)], c)
        s.rect((11,3,11,3), w)
        s.line([(12,7),(12,9)], b)
        s.line([(11,8),(13,8)], b)
        s.rect((12,8,12,8), w)
    elif name == 'leczenie':
        # Jednolite serce: blik u góry z lewej, środek i płaszczyzna cienia.
        s.poly([(2,5),(3,3),(5,3),(8,5),(10,3),(12,3),(13,5),
                (13,8),(11,10),(8,13),(5,11),(2,8)], a)
        s.poly([(3,5),(4,4),(5,4),(8,6),(9,5),(11,4),(12,5),
                (12,8),(8,12),(4,8)], b)
        s.poly([(4,5),(5,4),(7,6),(7,8),(5,7),(3,7)], c)
        s.detail('line', [(4,5),(5,5),(6,6)], w)
        s.detail('poly', [(11,7),(12,7),(12,8),(8,12),(8,11),(10,9)], a)
        s.detail('poly', [(12,8),(11,10),(8,13),(8,12),(10,10)], p['L'])
    elif name == 'uzdrowienie_innego':
        # Zdrowie przechodzi z dużego zielonego serca do małego złotego.
        s.poly([(1,9),(2,7),(4,7),(5,8),(6,7),(8,7),(9,9),
                (9,11),(5,14),(1,11)], a)
        s.poly([(2,9),(3,8),(4,8),(5,9),(6,8),(8,8),(8,11),
                (5,13),(2,11)], b)
        s.poly([(3,8),(4,8),(5,9),(4,10),(2,9)], c)
        s.detail('rect', (3,8,3,8), w)
        s.detail('poly', [(8,10),(8,11),(5,14),(5,13),(7,11)], p['L'])
    else:
        return source.draw_symbol(name, p)
    return s


def icon(name, school):
    p = PALETTES[school]
    s = custom_symbol(name, p)
    image = Image.new('RGBA', (16,16))
    # Kontur jest odcieniem szkoły i zajmuje tylko krawędź sylwetki.
    mask = s.mask.filter(ImageFilter.MaxFilter(3))
    image.paste(p['B'], (0,0,16,16), mask)
    image.alpha_composite(s.fill)
    d = ImageDraw.Draw(image)
    for kind, coords, color, width in s.details:
        if kind == 'line': d.line(coords, fill=color, width=width)
        elif kind == 'poly': d.polygon(coords, fill=color)
        elif kind == 'rect': d.rectangle(coords, fill=color)
        elif kind == 'ell': d.ellipse(coords, fill=color)
    # Wąskie szczeliny w materiale nadają formę bez przyciemniania całej ikony.
    inner_dark = p['B'] if school != 'iluzja' else '#3D5869'
    dark_details = {
        'plomienie': (((8,5),(8,6)), ((11,5),(11,7)), ((12,9),(12,9))),
        'ognisty_pocisk': (((7,8),(7,8)),),
        'kula_ognia': (((5,5),(6,4)), ((10,10),(11,9))),
        'odmrozenie': (((7,5),(7,6)), ((9,9),(10,10))),
        'leczenie_dloni': (((7,9),(7,10)), ((9,9),(9,10))),
        'zasklepienie_ran': (((7,7),(7,8)), ((9,9),(9,9))),
        'przywolany_miecz': (((10,5),(10,5)),),
        'uspokojenie': (((6,8),(7,9)), ((8,10),(9,11))),
    }
    for stroke in dark_details.get(name, ()):
        color = '#80553F' if name in ('leczenie_dloni','zasklepienie_ran') else (
            '#8D4933' if name == 'plomienie' else inner_dark)
        d.line(stroke, fill=color)
    if name == 'uzdrowienie_innego':
        # Pięciopikselowe serce ma własny złoty kontur, odrębny od zielonego.
        gold = {'.': None, 'D': '#A6773D', 'g': '#F2CD79',
                'l': '#FFF3C4'}
        for dy, row in enumerate(('.lDl.', 'DlggD',
                                  'DgggD', '.DgD.', '..D..')):
            for dx, shade in enumerate(row):
                if gold[shade]:
                    d.point((10+dx, 2+dy), fill=gold[shade])
        for x, y in ((8,7),(9,6),(10,5)):
            d.point((x,y), fill=p['white'])
    # Przy samym brzegu slotu zachowujemy jeden piksel przezroczystego marginesu.
    for i in range(16):
        image.putpixel((i,0), (0,0,0,0))
        image.putpixel((i,15), (0,0,0,0))
        image.putpixel((0,i), (0,0,0,0))
        image.putpixel((15,i), (0,0,0,0))
    return image


def font(size):
    path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    return ImageFont.truetype(path, size)


def preview(icons):
    bgs = ('#2e1c12', '#3b3a2a', '#8b8b8b')
    emerald = Image.open(HERE/'ref/wanilia16/emerald.png').convert('RGBA')
    names = [name for _, _, group in GROUPS for name in group]
    label_w, panel_w, row_h, top = 188, 328, 88, 49
    sheet = Image.new('RGB', (label_w+3*panel_w, top+row_h*15+10), '#D4CEC0')
    d = ImageDraw.Draw(sheet)
    d.text((12,10), 'IKONY ZAKLĘĆ 16×16   ·   nowa / obecna / szmaragd', fill='#302720', font=font(17))
    for row, name in enumerate(names):
        y = top+row*row_h
        school = next(s for _,s,group in GROUPS if name in group)
        d.rectangle((0,y,label_w-1,y+row_h-2), fill='#E5DECC' if row%2==0 else '#DAD2BE')
        d.text((10,y+16),name,fill='#302720',font=font(12))
        d.text((10,y+41),school,fill='#5C4B40',font=font(11))
        old = Image.open(HERE/'obecne'/f'ikona_{name}.png').convert('RGBA')
        for k, bg in enumerate(bgs):
            x = label_w+k*panel_w
            d.rectangle((x,y,x+panel_w-2,y+row_h-2),fill=bg)
            ink = '#F9EAD5' if k<2 else '#252525'
            d.text((x+9,y+3),bg,fill=ink,font=font(11))
            for i, (title, im) in enumerate((('NOWA', icons[name]),('OBECNA',old),('VANILLA',emerald))):
                xx = x+73+i*84
                d.text((xx,y+3),title,fill=ink,font=font(9))
                sheet.paste(im,(xx,y+22),im)
                big = im.resize((48,48),Image.Resampling.NEAREST)
                sheet.paste(big,(xx+20,y+22),big)
    sheet.save(HERE/'podglad.png')


def main():
    icons = {}
    for _, school, names in GROUPS:
        for name in names:
            im = icon(name, school)
            assert im.size == (16,16)
            pixels = list(im.get_flattened_data())
            opaque = [rgba for rgba in pixels if rgba[3]]
            bright = sum(0.2126*r + 0.7152*g + 0.0722*b > 150 for r,g,b,_ in opaque)
            assert {rgba[3] for rgba in pixels} <= {0,255}
            assert len(set(pixels)) <= 11, (name, len(set(pixels)))
            assert bright / len(opaque) >= .40, (name, bright / len(opaque))
            im.save(HERE/f'ikona_{name}.png')
            icons[name] = im
    assert len(icons) == 15
    preview(icons)


if __name__ == '__main__':
    main()
