# Makiety celu zadania (19.10): trzy warianty bez ciezkiego panelu, na jasnym i ciemnym tle. Obraz pixelart.
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance
W = Path('/w'); HUD = Path('/hud'); N = Image.Resampling.NEAREST; T = (0,0,0,0)
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C','gold0':'#8A6A30','gold1':'#D5A96B',
     'gold2':'#FFE7AE','cream':'#FFF1CC','navy0':'#183650','navy1':'#286287','green':'#61964A'}
def c(k): return P.get(k,k)
def rect(d,b,col): d.rectangle(b,fill=c(col))
prov = json.loads((HUD/'mcfont/assets/minecraft/font/include/default.json').read_text())['providers']
FONT = {}
for p in prov:
    if p.get('file') not in ('minecraft:font/ascii.png','minecraft:font/accented.png'): continue
    sh = Image.open(HUD/'mcfont/assets/minecraft/textures/font'/p['file'].split('/')[-1]).convert('RGBA')
    cw = sh.width//16; ch = sh.height//len(p['chars'])
    for r, chars in enumerate(p['chars']):
        for col, char in enumerate(chars):
            if char == '\0' or char in FONT: continue
            g = sh.crop((col*cw, r*ch, (col+1)*cw, (r+1)*ch)); bb = g.getchannel('A').getbbox()
            if bb: g = g.crop((0,0,bb[2],g.height)); adv = bb[2]+1
            else: g = Image.new('RGBA',(0,g.height)); adv = 4
            FONT[char] = (g, adv, 7-p['ascent'])
def tw(s): return sum(FONT.get(ch, FONT['?'])[1] for ch in s)
def text(L, x, y, s, col='cream', outline=True):
    cx = x
    for ch in s:
        g, adv, dy = FONT.get(ch, FONT['?'])
        if g.width:
            a = g.getchannel('A')
            if outline:
                for ox, oy in ((-1,0),(1,0),(0,-1),(0,1),(1,1)):
                    o = Image.new('RGBA', g.size, c('ink')); o.putalpha(a); L.alpha_composite(o,(cx+ox, y+dy+oy))
            t = Image.new('RGBA', g.size, c(col)); t.putalpha(a); L.alpha_composite(t,(cx, y+dy))
        cx += adv
    return cx - x

def ksiazka():
    im = Image.new('RGBA',(9,9),T); d = ImageDraw.Draw(im)
    rect(d,(1,1,7,7),'ink'); rect(d,(2,1,7,6),'gold1'); rect(d,(2,1,2,6),'gold0'); rect(d,(3,7,7,7),'cream'); rect(d,(4,3,6,3),'leather2')
    return im
def pipsy(L, x, y, n, done):
    d = ImageDraw.Draw(L)
    for i in range(n):
        bx = x + i*7
        rect(d,(bx,y,bx+5,y+2),'ink'); rect(d,(bx+1,y,bx+4,y+1), 'gold2' if i < done else 'leather1')

TYT, CEL, N_, D_ = 'Pierwsze kroki', 'Odbierz młode od Strażniczki Ogrodu', 4, 1

def wariant_a(L, x, y):  # tracker bez tla
    L.alpha_composite(ksiazka(), (x, y-1))
    w = text(L, x+12, y, TYT, 'gold2'); text(L, x+12+w+5, y, f'{D_}/{N_}', 'gold1')
    pipsy(L, x+12, y+10, N_, D_)
    text(L, x+12, y+16, '› ' + CEL, 'cream')

def wariant_b(L, x, y):  # przezroczysta wstega z zlotym akcentem
    band = Image.new('RGBA', (230, 30), T)
    for i in range(230):
        a = int(150 * max(0, 1 - i/230)**1.2)
        for j in range(30): band.putpixel((i, j), (37, 28, 43, a))
    L.alpha_composite(band, (x, y-3))
    d = ImageDraw.Draw(L); rect(d,(x, y-3, x+1, y+26),'gold1')
    L.alpha_composite(ksiazka(), (x+5, y-1))
    w = text(L, x+17, y, TYT, 'gold2'); text(L, x+17+w+5, y, f'{D_}/{N_}', 'gold1')
    pipsy(L, x+17, y+10, N_, D_)
    text(L, x+17, y+15, CEL, 'cream')

def wariant_c(L, x, y):  # zakladka tylko pod tytulem
    w = tw(TYT) + tw(f'{D_}/{N_}') + 30
    d = ImageDraw.Draw(L)
    rect(d,(x, y-2, x+w, y+10),'ink'); rect(d,(x+1, y-1, x+w-1, y+9),'leather1'); rect(d,(x+1,y-1,x+w-1,y-1),'leather2')
    rect(d,(x+w-3, y+10, x+w, y+12),'ink')
    L.alpha_composite(ksiazka(), (x+3, y))
    tw1 = text(L, x+14, y+1, TYT, 'gold2', outline=False); text(L, x+14+tw1+5, y+1, f'{D_}/{N_}', 'gold1', outline=False)
    pipsy(L, x+3, y+14, N_, D_)
    text(L, x+3, y+19, CEL, 'cream')

def okno(L):
    d = ImageDraw.Draw(L); x, y, w = 4, 4, 154
    rect(d,(x+2,y,x+w-2,y+43),'ink'); rect(d,(x,y+2,x+w,y+41),'ink'); rect(d,(x+2,y+1,x+w-2,y+42),'leather0'); rect(d,(x+3,y+3,x+w-3,y+40),'leather1')
    rect(d,(x+5,y+5,x+38,y+38),'ink'); rect(d,(x+6,y+6,x+37,y+37),'gold1')
    rect(d,(x+42,y+5,x+w-5,y+16),'gold0'); rect(d,(x+43,y+6,x+w-6,y+15),'navy0')
    text(L, x+46, y+8, 'UiQA', 'cream', outline=False); text(L, x+44, y+20, 'Miecz Korony', 'gold1', outline=False)
    rect(d,(x+42,y+32,x+w-5,y+36),'ink'); rect(d,(x+43,y+33,x+80,y+35),'gold1')

tlo = Image.open(W/'dzien.png').convert('RGBA')
wynik = []
for nazwa, fn in (('A_tracker', wariant_a), ('B_wstega', wariant_b), ('C_zakladka', wariant_c)):
    for pora in ('dzien', 'noc'):
        bg = tlo.crop((0, 0, 720, 300))
        if pora == 'noc': bg = ImageEnhance.Brightness(bg).enhance(.25)
        L = Image.new('RGBA', (240, 100), T); okno(L); fn(L, 6, 56)
        bg.alpha_composite(L.resize((720, 300), N))
        wynik.append(bg)
arkusz = Image.new('RGB', (720*2+10, 300*3+20), (20, 16, 22))
for i, im in enumerate(wynik): arkusz.paste(im.convert('RGB'), ((i % 2)*730, (i//2)*310))
arkusz.save(W/'out/cel_warianty.png'); print('ok')
