# Makiety nowego HUD-a world1 (17.10). Uruchamiane w obrazie pixelart: python3 -I /w/gen.py [--tylko WARIANT]
# Wszystko liczone w jednostkach GUI i skalowane NEAREST przez s (skala GUI). Ekran 1920x1080.
# Grafika w makiecie ma gestosc 1x (1 teksel = 1 jednostka GUI); w paczce bedzie rysowana 2x.
from pathlib import Path
import json, sys
from PIL import Image, ImageDraw, ImageEnhance

HUD = Path('/hud'); W = Path('/w'); OUT = W/'out'; OUT.mkdir(exist_ok=True)
SW, SH = 1920, 1080
N = Image.Resampling.NEAREST
T = (0, 0, 0, 0)
# Paleta Ksiegi z makiety2_gen.py + dwa tony stanow (trucizna, dusze) z tych samych rodzin.
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C',
     'gold0':'#8A6A30','gold1':'#D5A96B','gold2':'#FFE7AE','cream':'#FFF1CC',
     'navy0':'#183650','navy1':'#286287','blue':'#3F98CB','red':'#CE4042',
     'redhi':'#F36D62','green':'#61964A','green0':'#3E6A33','soul':'#B9E6E0','soul0':'#4E8C88'}

def c(k): return P.get(k, k)
def rect(d, box, col):
    x0, y0, x1, y1 = box
    d.rectangle((min(x0,x1), min(y0,y1), max(x0,x1), max(y0,y1)), fill=c(col))
def line(d, pts, col, w=1): d.line(pts, fill=c(col), width=w)

def corner(d, x, y, sx, sy):
    rect(d,(x,y,x+sx*4,y),'gold0'); rect(d,(x,y,x,y+sy*4),'gold0')
    rect(d,(x+sx,y+sy,x+sx*3,y+sy),'gold2'); rect(d,(x+sx,y+sy,x+sx,y+sy*3),'gold2')

def panel(d, box, rogi=True):
    x0, y0, x1, y1 = box
    rect(d,(x0+2,y0,x1-2,y1),'ink'); rect(d,(x0,y0+2,x1,y1-2),'ink')
    rect(d,(x0+2,y0+1,x1-2,y1-1),'leather0'); rect(d,(x0+1,y0+2,x1-1,y1-2),'leather0')
    rect(d,(x0+3,y0+3,x1-3,y1-3),'leather1')
    line(d,[(x0+5,y0+2),(x1-5,y0+2)],'leather2')
    if rogi:
        for xx, sx in ((x0+2,1),(x1-2,-1)):
            for yy, sy in ((y0+2,1),(y1-2,-1)): corner(d, xx, yy, sx, sy)

def recess(d, box):
    """Wneka paska: ciemny kontur, ciemne dno."""
    x0, y0, x1, y1 = box
    rect(d,(x0+1,y0,x1-1,y1),'ink'); rect(d,(x0,y0+1,x1,y1-1),'ink')
    rect(d,(x0+1,y0+1,x1-1,y1-1),'leather0')

def fill(d, box, frac, tones):
    """Trojtonowe wypelnienie wewnatrz wneki box (wsp. wneki)."""
    x0, y0, x1, y1 = box[0]+1, box[1]+1, box[2]-1, box[3]-1
    n = round((x1-x0+1)*max(0, min(1, frac)))
    if n <= 0: return
    hi, mid, lo = tones
    rect(d,(x0,y0,x0+n-1,y1),mid)
    line(d,[(x0,y0),(x0+n-1,y0)],hi)
    if y1-y0 >= 3: line(d,[(x0,y1),(x0+n-1,y1)],lo)

# --- Czcionka bitmapowa MC (ascii + accented) z poprawnym ascent ---
prov = json.loads((HUD/'mcfont/assets/minecraft/font/include/default.json').read_text())['providers']
FONT = {}
for p in prov:
    if p.get('file') not in ('minecraft:font/ascii.png', 'minecraft:font/accented.png'): continue
    sheet = Image.open(HUD/'mcfont/assets/minecraft/textures/font'/p['file'].split('/')[-1]).convert('RGBA')
    cw = sheet.width//16; ch = sheet.height//len(p['chars'])
    hgt = p.get('height', 8); asc = p['ascent']
    sc = hgt/ch
    for row, chars in enumerate(p['chars']):
        for col, char in enumerate(chars):
            if char == '\0' or char in FONT: continue
            g = sheet.crop((col*cw,row*ch,(col+1)*cw,(row+1)*ch))
            bb = g.getchannel('A').getbbox()
            if sc != 1: g = g.resize((max(1,round(cw*sc)), hgt), N); bb = g.getchannel('A').getbbox()
            if bb: g = g.crop((0,0,bb[2],g.height)); adv = bb[2]+1
            else: g = Image.new('RGBA',(0,g.height)); adv = 4
            FONT[char] = (g, adv, 7-asc)
def tw(s): return sum(FONT.get(ch, FONT['?'])[1] for ch in s)

def text(layer, x, y, s, col='cream', align='left', shadow=True):
    """Tekst w jednostkach GUI na warstwie GUI (1x). y = linia gorna kapitalikow (ascii 7 px)."""
    w = tw(s)
    if align == 'center': x -= w//2
    if align == 'right': x -= w
    cx = x
    for ch in s:
        g, adv, dy = FONT.get(ch, FONT['?'])
        if g.width:
            a = g.getchannel('A')
            if shadow:
                sh = Image.new('RGBA', g.size, c('ink')); sh.putalpha(a); layer.alpha_composite(sh,(cx+1,y+dy+1))
            t = Image.new('RGBA', g.size, c(col)); t.putalpha(a); layer.alpha_composite(t,(cx,y+dy))
        cx += adv
    return w

# --- Ikony 7x7 / 9x9 ---
def ikona(kind):
    im = Image.new('RGBA',(9,9),T); d = ImageDraw.Draw(im)
    if kind == 'serce':
        for y,x0,x1 in ((1,1,3),(1,5,7),(2,0,8),(3,0,8),(4,1,7),(5,2,6),(6,3,5),(7,4,4)): rect(d,(x0,y,x1,y),'red')
        rect(d,(1,2,2,2),'redhi'); rect(d,(2,1,2,1),'cream')
    elif kind == 'kropla':
        for y,x0,x1 in ((0,4,4),(1,3,5),(2,3,5),(3,2,6),(4,1,7),(5,1,7),(6,1,7),(7,2,6),(8,3,5)): rect(d,(x0,y,x1,y),'blue' if y<5 else 'navy1')
        rect(d,(3,4,3,5),'gold2')
    elif kind == 'dusza':
        for y,x0,x1 in ((0,3,5),(1,2,6),(2,1,7),(3,1,7),(4,1,7),(5,2,6),(6,2,7),(7,3,8),(8,5,8)): rect(d,(x0,y,x1,y),'soul')
        rect(d,(3,3,3,4),'ink'); rect(d,(5,3,5,4),'ink'); rect(d,(6,7,7,8),'soul0')
    elif kind == 'blysk':
        for b in ((4,0,6,0),(3,1,5,1),(3,2,4,2),(2,3,6,3),(4,4,5,4),(3,5,4,5),(2,6,3,6),(2,7,2,7)): rect(d,b,'gold2')
    elif kind == 'trucizna':
        rect(d,(3,0,5,1),'gold1'); rect(d,(2,2,6,8),'green'); rect(d,(3,3,4,4),'gold2'); line(d,[(2,8),(6,8)],'green0')
    elif kind == 'rany':
        for y,x0,x1 in ((0,2,6),(1,1,7),(2,1,7),(3,1,7),(4,2,6),(5,2,6),(6,3,5)): rect(d,(x0,y,x1,y),'cream')
        rect(d,(2,2,3,3),'ink'); rect(d,(5,2,6,3),'ink'); rect(d,(3,7,5,8),'cream'); rect(d,(4,5,4,5),'ink')
    elif kind == 'babel':
        d.ellipse((1,1,7,7),fill=c('blue')); d.ellipse((2,2,6,6),fill=c('navy1')); rect(d,(3,2,4,3),'cream')
    elif kind == 'tarcza':
        for y,x0,x1 in ((1,1,7),(2,1,7),(3,1,7),(4,1,7),(5,2,6),(6,3,5),(7,4,4)): rect(d,(x0,y,x1,y),'gold1')
        rect(d,(3,2,5,4),'navy0')
    elif kind == 'udziec':
        for y,x0,x1 in ((1,2,5),(2,1,6),(3,1,6),(4,2,6),(5,3,5)): rect(d,(x0,y,x1,y),'leather2')
        rect(d,(5,5,7,7),'cream')
    return im

def item(kind):
    im = Image.new('RGBA',(16,16),T); d = ImageDraw.Draw(im)
    if kind == 'miecz':
        line(d,[(3,12),(11,4)],'cream',2); rect(d,(10,2,13,5),'gold2'); line(d,[(2,9),(6,13)],'gold1',2); rect(d,(1,12,3,14),'leather0')
    elif kind == 'mikstura':
        rect(d,(6,2,9,4),'gold1'); rect(d,(4,5,11,13),'red'); line(d,[(4,5),(11,5)],'cream'); rect(d,(5,10,6,11),'redhi')
    elif kind == 'mapa':
        rect(d,(2,2,13,13),'cream'); line(d,[(4,3),(4,12),(8,9),(11,12)],'navy1'); rect(d,(9,4,10,5),'red')
    elif kind == 'ksiega':
        rect(d,(2,3,13,13),'gold0'); rect(d,(3,4,12,12),'navy0'); line(d,[(7,4),(7,12)],'cream')
    elif kind == 'zwoj':
        rect(d,(3,3,12,12),'gold1'); rect(d,(4,4,11,11),'cream'); line(d,[(5,6),(10,6)],'leather2'); line(d,[(5,8),(9,8)],'leather2')
    return im

def twarz():
    f = Image.new('RGBA',(8,8),c('leather2')); d = ImageDraw.Draw(f)
    rect(d,(0,0,7,1),'ink'); rect(d,(0,2,0,3),'ink'); rect(d,(7,2,7,3),'ink')
    rect(d,(1,2,6,7),'gold1'); rect(d,(1,4,2,4),'cream'); rect(d,(5,4,6,4),'cream')
    rect(d,(2,4,2,4),'navy0'); rect(d,(5,4,5,4),'navy0'); rect(d,(3,6,4,6),'leather2'); rect(d,(1,7,6,7),'leather1')
    return f.resize((32,32), N)

# --- Tlo ---
def tlo(stan):
    nazwa = {'walka':'walka','woda':'woda','noc':'noc'}.get(stan, 'dzien')
    p = W/'tla'/f'{nazwa}.png'
    if p.exists(): return Image.open(p).convert('RGBA').resize((SW,SH))
    if nazwa == 'noc' and (W/'tla'/'dzien.png').exists():
        return ImageEnhance.Brightness(Image.open(W/'tla'/'dzien.png').convert('RGBA')).enhance(.3)
    im = Image.new('RGBA',(SW,SH)); d = ImageDraw.Draw(im)
    for y in range(SH):
        if y < 560: t = y/560; col = (int(120+t*60), int(160+t*50), int(220+t*25))
        else: t = (y-560)/520; col = (int(98-t*40), int(140-t*50), int(60-t*25))
        d.line([(0,y),(SW,y)], fill=col)
    if stan == 'woda': im = Image.blend(im, Image.new('RGBA',(SW,SH),(30,60,170,255)), .55)
    return im

# --- Elementy HUD (rysowane na warstwie GUI W x H) ---
def hotbar(g, H, Wg, stan):
    hb = Image.open(HUD/'praca/pack/assets/minecraft/textures/gui/sprites/hud/hotbar.png').convert('RGBA')
    hx = Wg//2-91; hy = H-22
    g.alpha_composite(hb,(hx,hy))
    for i, k in {0:'miecz',1:'zwoj',2:'mikstura',3:'mikstura',7:'mapa',8:'ksiega'}.items():
        g.alpha_composite(item(k),(hx+3+i*20,hy+3))
    d = ImageDraw.Draw(g)
    rect(d,(hx-1,hy-1,hx+22,hy),'cream'); rect(d,(hx-1,hy+21,hx+22,hy+22),'cream')
    rect(d,(hx-1,hy,hx,hy+21),'cream'); rect(d,(hx+21,hy,hx+22,hy+21),'cream')
    return hx, hy

def kieszen(g, d, x, y, kind):
    recess(d,(x,y,x+10,y+10)); rect(d,(x+1,y+1,x+9,y+9),'leather1'); g.alpha_composite(ikona(kind),(x+1,y+1))

def pas_nad(g, H, Wg, st):
    """Wariant 1: pas nad hotbarem. Kieszen z ikona + pasek HP po lewej, zasob po prawej, medalion w srodku,
    stamina jako 10 jednostek tuz nad slotami."""
    hx = Wg//2-91; d = ImageDraw.Draw(g)
    y0 = H-22-17
    nis = st['hp']/st['hpmax'] < .3
    kieszen(g, d, hx, y0, 'serce')
    hp_box = (hx+12, y0, hx+80, y0+10)
    recess(d, hp_box)
    tony = ('gold2','green','green0') if st.get('trucizna') else ('redhi','red','leather2')
    fill(d, hp_box, st['hp']/st['hpmax'], tony)
    if nis:
        x0,y_,x1,y1 = hx-1, y0-1, hp_box[2]+1, y0+11
        for b in ((x0,y_,x1,y_),(x0,y1,x1,y1),(x0,y_,x0,y1),(x1,y_,x1,y1)): rect(d,b,'redhi')
    text(g,(hp_box[0]+hp_box[2])//2,y0+2,f"{st['hp']}/{st['hpmax']}",'redhi' if nis else 'cream','center')
    zs_box = (hx+102, y0, hx+170, y0+10)
    recess(d, zs_box)
    if st.get('dusze') is not None:
        for i in range(st['dusze_max']):
            bx = zs_box[0]+3+i*6
            rect(d,(bx,y0+3,bx+3,y0+7),'soul' if i < st['dusze'] else 'leather1')
            if i < st['dusze']: rect(d,(bx,y0+3,bx,y0+4),'cream')
        kieszen(g, d, hx+172, y0, 'dusza')
    else:
        fill(d, zs_box, st['mana']/st['manamax'], ('blue','navy1','navy0'))
        kieszen(g, d, hx+172, y0, 'kropla')
        text(g,(zs_box[0]+zs_box[2])//2,y0+2,f"{st['mana']}/{st['manamax']}",'cream','center')
    sy = y0+12
    for i in range(10):
        bx = hx+i*18+(1 if i else 0); bw = 17 if i else 16
        recess(d,(bx,sy,bx+bw-1,sy+3))
        if i < st['stam']:
            col, hi = ('gold1','gold2') if st['stam'] > 3 else ('red','redhi')
            rect(d,(bx+1,sy+1,bx+bw-2,sy+2),col); line(d,[(bx+1,sy+1),(bx+bw-2,sy+1)],hi)
    medal(g, Wg//2-12, y0-11, st['poziom'])
    ex = hx; ey = y0-12
    for k, t in st.get('efekty', []):
        g.alpha_composite(ikona(k),(ex,ey)); ex += 10 + text(g, ex+10, ey+1, t, 'cream') + 5
    if st.get('tlen') is not None:
        for i in range(st['tlen']): g.alpha_composite(ikona('babel'),(hx+182-9-i*8,ey))
    return ey

def pas_skrzydla(g, H, Wg, st):
    """Wariant 2: skrzydla po bokach hotbara. Lewe: HP/zasob/stamina. Prawe: efekty, tlen, zbroja, glod."""
    hx = Wg//2-91; d = ImageDraw.Draw(g)
    lx0, lx1 = hx-96, hx-4; top = H-36
    panel(d,(lx0,top,lx1,H-1))
    nis = st['hp']/st['hpmax'] < .3
    hp_box = (lx0+5, top+5, lx1-5, top+15)
    recess(d, hp_box)
    fill(d, hp_box, st['hp']/st['hpmax'], ('gold2','green','green0') if st.get('trucizna') else ('redhi','red','leather2'))
    if nis:
        rect(d,(hp_box[0]-1,hp_box[1]-1,hp_box[2]+1,hp_box[1]-1),'redhi'); rect(d,(hp_box[0]-1,hp_box[3]+1,hp_box[2]+1,hp_box[3]+1),'redhi')
        rect(d,(hp_box[0]-1,hp_box[1],hp_box[0]-1,hp_box[3]),'redhi'); rect(d,(hp_box[2]+1,hp_box[1],hp_box[2]+1,hp_box[3]),'redhi')
    g.alpha_composite(ikona('serce'),(hp_box[0]+2,hp_box[1]+1))
    text(g,(hp_box[0]+hp_box[2])//2+5,hp_box[1]+2,f"{st['hp']}/{st['hpmax']}",'redhi' if nis else 'cream','center')
    zs = (lx0+5, top+18, lx1-5, top+26)
    recess(d, zs)
    if st.get('dusze') is not None:
        for i in range(st['dusze_max']):
            bx = zs[0]+11+i*7
            rect(d,(bx,zs[1]+2,bx+4,zs[1]+6),'soul' if i < st['dusze'] else 'leather1')
        g.alpha_composite(ikona('dusza'),(zs[0]+1,zs[1]))
    else:
        fill(d, zs, st['mana']/st['manamax'], ('blue','navy1','navy0'))
        g.alpha_composite(ikona('kropla'),(zs[0]+1,zs[1]))
        text(g,(zs[0]+zs[2])//2+5,zs[1]+1,f"{st['mana']}/{st['manamax']}",'cream','center')
    sy = top+29
    for i in range(10):
        bx = lx0+5+i*8
        rect(d,(bx,sy,bx+6,sy+2),'ink')
        if i < st['stam']: rect(d,(bx+1,sy,bx+5,sy+1),'gold1' if st['stam']>3 else 'redhi')
    # prawe skrzydlo
    rx0, rx1 = hx+186, hx+278
    panel(d,(rx0,top,rx1,H-1))
    ex, ey = rx0+5, top+5
    for k, t in st.get('efekty', [])[:2]:
        g.alpha_composite(ikona(k),(ex,ey)); text(g, ex+11, ey+1, t, 'cream'); ey += 11
    if st.get('tlen') is not None:
        for i in range(st['tlen']): g.alpha_composite(ikona('babel'),(rx0+5+i*8,top+26))
    else:
        g.alpha_composite(ikona('tarcza'),(rx0+5,top+25)); text(g,rx0+16,top+26,'12','cream')
        g.alpha_composite(ikona('udziec'),(rx0+36,top+25)); text(g,rx0+47,top+26,'9','cream')
    return top

def karta_lewa(g, st, s_zwin=False, z_medalionem=False):
    """Okno postaci, lewy gorny rog. W walce zwija sie do portretu."""
    d = ImageDraw.Draw(g); x, y = 4, 4
    if s_zwin:
        panel(d,(x,y,x+43,y+43))
        rect(d,(x+5,y+5,x+38,y+38),'ink'); g.alpha_composite(twarz(),(x+6,y+6))
        if st['hp']/st['hpmax'] < .3:
            for b in ((x+5,y+5,x+38,y+5),(x+5,y+38,x+38,y+38),(x+5,y+5,x+5,y+38),(x+38,y+5,x+38,y+38)): rect(d,b,'redhi')
        if z_medalionem: medal(g, x+30, y+30, st['poziom'])
        return (x, y, x+43, y+43)
    w = 154
    panel(d,(x,y,x+w,y+43))
    rect(d,(x+5,y+5,x+38,y+38),'ink'); g.alpha_composite(twarz(),(x+6,y+6))
    rect(d,(x+42,y+5,x+w-5,y+16),'gold0'); rect(d,(x+43,y+6,x+w-6,y+15),'navy0'); line(d,[(x+44,y+6),(x+w-7,y+6)],'gold1')
    text(g,x+46,y+7,st['nick'],'cream')
    text(g,x+44,y+20,st['klasa'],'gold1')
    recess(d,(x+42,y+32,x+w-5,y+36)); fill(d,(x+42,y+32,x+w-5,y+36),st['exp'],('gold2','gold1','gold0'))
    if z_medalionem: medal(g, x+30, y+30, st['poziom'])
    return (x, y, x+w, y+43)

def medal(g, mx, my, lvl):
    d = ImageDraw.Draw(g)
    d.ellipse((mx,my,mx+23,my+23),fill=c('ink')); d.ellipse((mx+1,my+1,mx+22,my+22),fill=c('gold0'))
    d.ellipse((mx+2,my+2,mx+21,my+21),fill=c('gold1')); d.ellipse((mx+3,my+3,mx+20,my+20),fill=c('navy0'))
    text(g,mx+12,my+8,str(lvl),'cream','center')

def cel_zadania(g, x1, y, st, w=None):
    d = ImageDraw.Draw(g)
    w = max(tw(st['cel'][0]), tw(st['cel'][1])) + 13 if w is None else w
    x0 = x1-w
    panel(d,(x0,y,x1,y+27))
    text(g,x0+6,y+5,st['cel'][0],'gold2')
    text(g,x0+6,y+16,st['cel'][1],'cream')

def mapa_obraz():
    m = Image.new('RGBA',(127,127),c('#6E9A3E')); d = ImageDraw.Draw(m)
    for i in range(0,127,9): rect(d,(i,(i*7)%127,i+5,(i*7)%127+4),'#5F8A35')
    line(d,[(0,90),(40,70),(70,72),(127,40)],'#C8B07A',4); rect(d,(80,10,110,30),'#3A6FB0')
    rect(d,(30,30,42,40),'#8C532C'); rect(d,(50,95,58,103),'#8C532C')
    d.polygon([(63,58),(68,70),(63,66),(58,70)],fill=c('cream'),outline=c('ink'))
    return m

def mapa(im, s, st, ramka=True):
    """NMinimap: stale piksele ekranu, 254x254 z odstepem 60/60 od prawej/gory."""
    mx, my = SW-60-254, 60
    pm = W/'tla'/'dzien.png'
    m = Image.open(pm).convert('RGBA').crop((mx,my,mx+254,my+254)) if pm.exists() else mapa_obraz().resize((254,254),N)
    im.alpha_composite(m,(mx,my))
    if ramka:
        fr = Image.new('RGBA',(131,131),T); d = ImageDraw.Draw(fr); panel_ramka(d,131)
        im.alpha_composite(fr.resize((262,262),N),(mx-4,my-4))
    return mx, my

def panel_ramka(d, n):
    for b, col in (((0,0,n-1,1),'ink'),((0,n-2,n-1,n-1),'ink'),((0,0,1,n-1),'ink'),((n-2,0,n-1,n-1),'ink'),
                   ((2,2,n-3,2),'gold0'),((2,n-3,n-3,n-3),'gold0'),((2,2,2,n-3),'gold0'),((n-3,2,n-3,n-3),'gold0')):
        rect(d,b,col)
    for xx, sx in ((3,1),(n-4,-1)):
        for yy, sy in ((3,1),(n-4,-1)): corner(d,xx,yy,sx,sy)

def okno_prawe(im, g, s, st, Wg, zwin=False, z_medalionem=True):
    """Wariant P: okno postaci przyklejone do lewej krawedzi mapy. Mapa w px ekranu, okno w GUI."""
    mx, my = mapa(im, s, st, ramka=True)
    d = ImageDraw.Draw(g)
    # okno: prawa krawedz w miejscu lewej krawedzi ramki mapy (liczone dla tej skali w GUI, w grze stale!)
    x1 = (mx-4)//s + 1 if s == 4 else Wg - 80  # GUI 4 dopasowane; inne skale: ta sama stala z GUI 4 (80 jedn.)
    y0 = (my-4)//s if s == 4 else 14
    hmap = 262//4  # wysokosc ramki mapy w GUI 4
    if zwin:
        x0 = x1-44
        panel(d,(x0,y0,x1,y0+43))
        rect(d,(x0+5,y0+5,x0+38,y0+38),'ink'); g.alpha_composite(twarz(),(x0+6,y0+6))
        if st['hp']/st['hpmax'] < .3:
            for b in ((x0+5,y0+5,x0+38,y0+5),(x0+5,y0+38,x0+38,y0+38),(x0+5,y0+5,x0+5,y0+38),(x0+38,y0+5,x0+38,y0+38)): rect(d,b,'redhi')
        if z_medalionem: medal(g, x0+24, y0+24, st['poziom'])
        return x0, y0, hmap
    w = 112; x0 = x1-w
    panel(d,(x0,y0,x1,y0+hmap-1))
    rect(d,(x0+5,y0+5,x0+38,y0+38),'ink'); g.alpha_composite(twarz(),(x0+6,y0+6))
    ex0 = x0+66 if z_medalionem else x0+43
    if z_medalionem: medal(g, x0+42, y0+5, st['poziom'])
    text(g,x0+6,y0+41,st['nick'],'cream')
    text(g,x0+6,y0+51,st['klasa'],'gold1')
    recess(d,(ex0,y0+8,x1-6,y0+12)); fill(d,(ex0,y0+8,x1-6,y0+12),st['exp'],('gold2','gold1','gold0'))
    text(g,ex0,y0+16,'EXP','gold0'); text(g,x1-6,y0+16,f"{round(st['exp']*100)}%",'gold1','right')
    return x0, y0, hmap

def vanilla_teksty(g, H, Wg, st):
    if st.get('walka'):
        text(g, Wg//2, H-68, 'Za mało staminy!', 'redhi', 'center')
    text(g, Wg//2, H-59, 'Miecz Korony', 'cream', 'center')
    text(g, Wg//2-91, H-104, 'Lewa: Promień', 'gold1')
    text(g, Wg//2+91, H-104, 'Prawa: Tarcza', 'gold1', 'right')

def winieta(im, sila):
    v = Image.new('L',(SW//8,SH//8),0); d = ImageDraw.Draw(v)
    w, h = v.size
    for i in range(14):
        a = int(sila*(14-i)/14*255)
        d.rectangle((i,i,w-1-i,h-1-i),outline=a)
    v = v.resize((SW,SH), Image.Resampling.BILINEAR)
    red = Image.new('RGBA',(SW,SH),(170,30,30,255)); red.putalpha(v)
    im.alpha_composite(red)

STANY = {
 'spokoj': dict(nick='Przemidar', klasa='Miecz Korony', poziom=37, hp=64, hpmax=64, mana=80, manamax=120, stam=10, exp=.42,
                cel=('Pierwsze kroki 1/4','Pogłaszcz chowańca')),
 'walka': dict(nick='Przemidar', klasa='Miecz Korony', poziom=37, hp=14, hpmax=64, mana=35, manamax=120, stam=3, exp=.42, walka=True,
               efekty=[('rany','2:41')], cel=('Pierwsze kroki 1/4','Pogłaszcz chowańca')),
 'akolita': dict(nick='Przemidar', klasa='Nosiciel Relikwii', poziom=37, hp=41, hpmax=64, dusze=3, dusze_max=10, stam=7, exp=.7,
                 trucizna=True, efekty=[('trucizna','0:12')], cel=('Kwietna Polana','Zbierz 3 Dusze')),
 'woda': dict(nick='Przemidar', klasa='Miecz Korony', poziom=37, hp=60, hpmax=64, mana=110, manamax=120, stam=8, exp=.42, tlen=6,
              cel=('Topielica','Znajdź wejście do śluzy')),
 'skrajne': dict(nick='Wędrowiec1234567', klasa='Wołający Przodków', poziom=100, hp=1240, hpmax=1240, mana=480, manamax=520, stam=10, exp=.99,
                 efekty=[('rany','2:41'),('trucizna','0:12')], cel=('Rok Bez Pogrzebu 10/12','Zanieś relikwię do kaplicy')),
}

WARIANTY = {'L1':('L','nad'), 'L2':('L','skrzydla'), 'P1':('P','nad'), 'P2':('P','skrzydla')}

def render(war, stan, s):
    rodz, pas = WARIANTY[war]; st = STANY[stan]
    im = tlo(stan); Wg, H = SW//s, SH//s
    g = Image.new('RGBA',(Wg,H),T)
    zwin = bool(st.get('walka'))
    if rodz == 'L':
        karta_lewa(g, st, s_zwin=zwin, z_medalionem=(pas=='skrzydla'))
        mx, my = mapa(im, s, st)
        cel_zadania(g, (mx+258)//s, (my+258)//s + 3, st)
    else:
        x0, y0, hm = okno_prawe(im, g, s, st, Wg, zwin=zwin, z_medalionem=(pas=='skrzydla'))
        cel_zadania(g, x0 + (112 if not zwin else 44) + 66, y0+hm+3, st)
    vanilla_teksty(g, H, Wg, st)
    hotbar(g, H, Wg, st)
    if pas == 'nad': pas_nad(g, H, Wg, st)
    else: pas_skrzydla(g, H, Wg, st)
    if st['hp']/st['hpmax'] < .3: winieta(im, .45)
    im.alpha_composite(g.resize((Wg*s,H*s),N))
    p = OUT/f'{war}_{stan}_gui{s}.png'; im.convert('RGB').save(p)
    return im

def main():
    tylko = sys.argv[sys.argv.index('--tylko')+1] if '--tylko' in sys.argv else None
    gotowe = []
    for war in WARIANTY:
        if tylko and war != tylko: continue
        for stan in STANY: gotowe.append(render(war, stan, 3))
        for s in (2, 4): render(war, 'spokoj', s)
        render(war, 'walka', 4)
    # powiekszenia: lewa gora i dol, GUI 3, x2
    for war in WARIANTY:
        if tylko and war != tylko: continue
        for stan in ('spokoj','walka','akolita'):
            im = Image.open(OUT/f'{war}_{stan}_gui3.png')
            if WARIANTY[war][0] == 'L': gora = im.crop((0,0,560,160))
            else: gora = im.crop((1920-760,0,1920,500))
            dol = im.crop((540,860,1380,1080))
            z = Image.new('RGB',(max(gora.width,dol.width)*2, (gora.height+dol.height)*2+10),(20,16,22))
            z.paste(gora.resize((gora.width*2,gora.height*2),N),(0,0)); z.paste(dol.resize((dol.width*2,dol.height*2),N),(0,gora.height*2+10))
            z.save(OUT/f'{war}_{stan}_zoom.png')
    print('ok', len(list(OUT.glob('*.png'))), 'plikow;', 'nick16', tw('Wędrowiec1234567'), 'klasa', tw('Wołający Przodków'))

main()
