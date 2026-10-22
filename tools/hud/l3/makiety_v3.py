# Makiety HUD v3 (19.10): lewa kolumna = okno postaci + towarzysze (Chowaniec, slugi) + cel + druzyna.
# Jednostki GUI, render przy GUI 3 i GUI 4 na 1920x1080. Obraz pixelart.
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance
W = Path('/w'); HUD = Path('/hud'); N = Image.Resampling.NEAREST; T = (0,0,0,0)
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C','gold0':'#8A6A30','gold1':'#D5A96B',
     'gold2':'#FFE7AE','cream':'#FFF1CC','navy0':'#183650','navy1':'#286287','blue':'#3F98CB','red':'#CE4042','redhi':'#F36D62',
     'green':'#61964A','green0':'#3E6A33','soul':'#B9E6E0','soul0':'#4E8C88','grey':'#7A6F75','grey0':'#4A4048','orange':'#E08A3C'}
def c(k): return P.get(k,k)
def rect(d,b,col): d.rectangle(b,fill=c(col))
def line(d,pts,col): d.line(pts,fill=c(col))
prov = json.loads((HUD/'mcfont/assets/minecraft/font/include/default.json').read_text())['providers']
FONT = {}
for p in prov:
    if p.get('file') not in ('minecraft:font/ascii.png','minecraft:font/accented.png'): continue
    sh = Image.open(HUD/'mcfont/assets/minecraft/textures/font'/p['file'].split('/')[-1]).convert('RGBA')
    cw = sh.width//16; ch = sh.height//len(p['chars'])
    for r, chars in enumerate(p['chars']):
        for col, char in enumerate(chars):
            if char == '\0' or char in FONT: continue
            g = sh.crop((col*cw,r*ch,(col+1)*cw,(r+1)*ch)); bb = g.getchannel('A').getbbox()
            if bb: g = g.crop((0,0,bb[2],g.height)); adv = bb[2]+1
            else: g = Image.new('RGBA',(0,g.height)); adv = 4
            FONT[char] = (g, adv, 7-p['ascent'])
def tw(s): return sum(FONT.get(ch,FONT['?'])[1] for ch in s)
def text(L,x,y,s,col='cream',outline=True,align='left'):
    if align=='right': x -= tw(s)
    cx=x
    for ch in s:
        g,adv,dy = FONT.get(ch,FONT['?'])
        if g.width:
            a=g.getchannel('A')
            if outline:
                o=Image.new('RGBA',g.size,c('ink')); o.putalpha(a); L.alpha_composite(o,(cx+1,y+dy+1))
            t=Image.new('RGBA',g.size,c(col)); t.putalpha(a); L.alpha_composite(t,(cx,y+dy))
        cx+=adv
    return cx-x
def corner(d,x,y,sx,sy):
    rect(d,(min(x,x+sx*4),y,max(x,x+sx*4),y),'gold0'); rect(d,(x,min(y,y+sy*4),x,max(y,y+sy*4)),'gold0')
def panel(d,b,rogi=True):
    x0,y0,x1,y1=b
    rect(d,(x0+2,y0,x1-2,y1),'ink'); rect(d,(x0,y0+2,x1,y1-2),'ink')
    rect(d,(x0+2,y0+1,x1-2,y1-1),'leather0'); rect(d,(x0+1,y0+2,x1-1,y1-2),'leather0'); rect(d,(x0+3,y0+3,x1-3,y1-3),'leather1')
    line(d,[(x0+5,y0+2),(x1-5,y0+2)],'leather2')
    if rogi:
        for xx,sx in ((x0+2,1),(x1-2,-1)):
            for yy,sy in ((y0+2,1),(y1-2,-1)): corner(d,xx,yy,sx,sy)
def recess(d,b):
    x0,y0,x1,y1=b; rect(d,(x0+1,y0,x1-1,y1),'ink'); rect(d,(x0,y0+1,x1,y1-1),'ink'); rect(d,(x0+1,y0+1,x1-1,y1-1),'leather0')
def bar(d,b,frac,tones):
    recess(d,b); x0,y0,x1,y1=b[0]+1,b[1]+1,b[2]-1,b[3]-1; n=round((x1-x0+1)*max(0,min(1,frac)))
    if n>0:
        hi,mid,lo=tones; rect(d,(x0,y0,x0+n-1,y1),mid); line(d,[(x0,y0),(x0+n-1,y0)],hi)
        if y1-y0>=2: line(d,[(x0,y1),(x0+n-1,y1)],lo)
HP=('redhi','red','leather2'); MANA=('blue','navy1','navy0'); EXP=('gold2','gold1','gold0'); GREY=('grey','grey0','grey0')
def twarz(kolor='gold1', wlosy='leather2', s=8):
    f=Image.new('RGBA',(8,8),c(wlosy)); d=ImageDraw.Draw(f)
    rect(d,(1,2,6,7),kolor); rect(d,(1,4,2,4),'cream'); rect(d,(5,4,6,4),'cream'); rect(d,(2,4,2,4),'navy0'); rect(d,(5,4,5,4),'navy0'); rect(d,(3,6,4,6),'leather2')
    return f.resize((s,s),N)
def ikona(kind):
    im=Image.new('RGBA',(9,9),T); d=ImageDraw.Draw(im)
    if kind=='ogien':
        for y,a,b in ((0,4,4),(1,3,5),(2,3,6),(3,2,6),(4,1,7),(5,1,7),(6,1,7),(7,2,6),(8,3,5)): rect(d,(a,y,b,y),'redhi' if y<4 else 'orange')
        rect(d,(4,5,4,7),'gold2')
    elif kind=='dusza':
        for y,a,b in ((0,3,5),(1,2,6),(2,1,7),(3,1,7),(4,1,7),(5,2,6),(6,2,7),(7,3,8),(8,5,8)): rect(d,(a,y,b,y),'soul')
        rect(d,(3,3,3,4),'ink'); rect(d,(5,3,5,4),'ink')
    elif kind=='tarcza':
        for y,a,b in ((1,1,7),(2,1,7),(3,1,7),(4,1,7),(5,2,6),(6,3,5),(7,4,4)): rect(d,(a,y,b,y),'gold1')
        rect(d,(3,2,5,4),'navy0')
    elif kind=='korona':
        rect(d,(1,3,7,6),'gold2'); rect(d,(1,1,1,3),'gold2'); rect(d,(4,0,4,3),'gold2'); rect(d,(7,1,7,3),'gold2'); rect(d,(2,5,6,5),'gold0')
    elif kind=='serce':
        for y,a,b in ((1,1,3),(1,5,7),(2,0,8),(3,0,8),(4,1,7),(5,2,6),(6,3,5),(7,4,4)): rect(d,(a,y,b,y),'red')
    elif kind=='ksiazka':
        rect(d,(1,1,7,7),'ink'); rect(d,(2,1,7,6),'gold1'); rect(d,(3,7,7,7),'cream'); rect(d,(4,3,6,3),'leather2')
    elif kind=='strzalka':
        d.polygon([(2,1),(6,4),(2,7)],fill=c('gold1'))
    elif kind=='rasa':
        rect(d,(1,3,7,7),'gold1'); rect(d,(1,1,1,3),'gold1'); rect(d,(4,0,4,3),'gold1'); rect(d,(7,1,7,3),'gold1')
    elif kind=='zasieg':
        d.ellipse((1,1,7,7),outline=c('grey')); rect(d,(4,2,4,4),'grey'); rect(d,(4,4,6,4),'grey')
    elif kind=='gwiazda':
        d.polygon([(4,0),(5,3),(8,3),(6,5),(7,8),(4,6),(1,8),(2,5),(0,3),(3,3)],fill=c('gold2'))
    return im
def kieszen(L,d,x,y,kind):
    recess(d,(x,y,x+10,y+10)); rect(d,(x+1,y+1,x+9,y+9),'leather1'); L.alpha_composite(ikona(kind),(x+1,y+1))

# ---------- elementy ----------
OX=4
def okno(L,st,zwin=False):
    d=ImageDraw.Draw(L); x,y=OX,4
    if zwin:
        panel(d,(x,y,x+43,y+43)); rect(d,(x+5,y+5,x+38,y+38),'ink'); L.alpha_composite(twarz(s=32),(x+6,y+6))
        if st['hp']/st['hpmax']<.3:
            for b in ((x+5,y+5,x+38,y+5),(x+5,y+38,x+38,y+38),(x+5,y+5,x+5,y+38),(x+38,y+5,x+38,y+38)): rect(d,b,'redhi')
        return y+44
    w=145; panel(d,(x,y,x+w,y+55))
    rect(d,(x+5,y+5,x+38,y+38),'ink'); L.alpha_composite(twarz(s=32),(x+6,y+6))
    recess(d,(x+30,y+30,x+40,y+40)); rect(d,(x+31,y+31,x+39,y+39),'leather1'); L.alpha_composite(ikona('rasa'),(x+31,y+31))
    rect(d,(x+42,y+5,x+w-5,y+16),'gold0'); rect(d,(x+43,y+6,x+w-6,y+15),'navy0')
    text(L,x+46,y+8,st['nick'],'cream',False); text(L,x+43,y+20,st['klasa'],'gold1',False)
    bar(d,(x+42,y+31,x+112,y+35),st['exp'],EXP); text(L,x+116,y+30,f"{round(st['exp']*100)}%",'gold1',False)
    line(d,[(x+6,y+41),(x+w-6,y+41)],'leather0'); line(d,[(x+6,y+42),(x+w-6,y+42)],'leather2')
    L.alpha_composite(ikona('ogien'),(x+7,y+44)); text(L,x+18,y+45,st['wiara'],'gold1',False); L.alpha_composite(ikona('gwiazda'),(x+w-27,y+44)); text(L,x+w-16,y+45,'+3','gold2',False)
    return y+56

def towarzysze(L,y,st):
    """Panel towarzyszy: Chowaniec (gorny wiersz) + slugi (dolny). Szerokosc jak okno."""
    d=ImageDraw.Draw(L); x=OX; w=145
    rows=(1 if st.get('pet') else 0)+(1 if st.get('slugi') else 0)
    if not rows: return y
    h=6+rows*15; panel(d,(x,y,x+w,y+h),rogi=False)
    yy=y+4
    if st.get('pet'):
        p=st['pet']; zem=p.get('zemdlony')
        kieszen(L,d,x+4,yy,'ogien')
        text(L,x+17,yy+1,p['nazwa'],'grey' if zem else 'gold2',False); text(L,x+17+tw(p['nazwa'])+4,yy+1,f"{p['poziom']}",'gold1',False)
        if zem: text(L,x+w-5,yy+1,'zemdlony','redhi',False,'right')
        else: text(L,x+w-5,yy+1,f"{p['hp']}/{p['hpmax']}",'cream',False,'right')
        bar(d,(x+17,yy+9,x+w-5,yy+12),0 if zem else p['hp']/p['hpmax'],GREY if zem else ('gold2','green','green0'))
        line(d,[(x+18,yy+11),(x+18+round((w-24)*p['exp']),yy+11)],'gold1')
        yy+=15
    if st.get('slugi'):
        for i,s in enumerate(st['slugi']):
            sx=x+4+i*40
            kieszen(L,d,sx,yy,'dusza'); bar(d,(sx+12,yy+1,sx+36,yy+5),s['hp'],HP)
            text(L,sx+12,yy+6,f"{s['s']}s",'soul',False) if True else None
        text(L,x+w-5,yy+2,f"{len(st['slugi'])}/{st['limit']}",'soul',False,'right')
        for i in range(len(st['slugi']),st['limit']):
            sx=x+4+i*40; recess(d,(sx,yy,sx+10,yy+10))
    return y+h+2

def cel(L,y,st):
    if not st.get('cel'): return y
    band=Image.new('RGBA',(230,30),T)
    for i in range(230):
        a=int(150*max(0,1-i/230)**1.2)
        for j in range(30): band.putpixel((i,j),(37,28,43,a))
    band=band.transpose(Image.FLIP_LEFT_RIGHT); X1=L.width-4; X0=X1-230
    L.alpha_composite(band,(X0,y)); d=ImageDraw.Draw(L); rect(d,(X1-1,y,X1,y+29),'gold1')
    L.alpha_composite(ikona('ksiazka'),(X1-14,y+4)); L.alpha_composite(ikona('strzalka').transpose(Image.FLIP_LEFT_RIGHT),(X1-15,y+16))
    w=text(L,X1-18,y+5,st['cel'][1],'gold1',True,'right'); text(L,X1-18-w-4,y+5,st['cel'][0],'gold2',True,'right'); text(L,X1-18,y+17,st['cel'][2],'cream',True,'right')
    return y+32

def druzyna(L,y,st):
    """Ramki druzyny: wiersz 13 GUI. Glowa 8x8 (head size 1), lider korona, nick, klasa skrot+poziom, HP 3 px + zasob 1 px."""
    if not st.get('druzyna'): return y
    d=ImageDraw.Draw(L); x=OX
    text(L,x+1,y,f"Drużyna {len(st['druzyna'])+1}/8",'gold2')
    y+=10
    for m in st['druzyna']:
        daleko=m.get('daleko'); nis=m['hp']<.3
        band=Image.new('RGBA',(150,13),T)
        for i in range(150):
            a=int(140*max(0,1-i/150)**1.1)
            for j in range(13): band.putpixel((i,j),(37,28,43,a))
        L.alpha_composite(band,(x,y-1))
        recess(d,(x,y,x+9,y+9)); L.alpha_composite(twarz(kolor='gold1' if not daleko else 'grey', wlosy=m.get('wl','leather2')),(x+1,y+1))
        if m.get('lider'): L.alpha_composite(ikona('korona'),(x+4,y-5))
        kol='grey' if daleko else ('redhi' if nis else 'cream')
        text(L,x+13,y,m['nick'],kol); text(L,x+120,y,f"{m['lv']}",'grey' if daleko else 'gold1',True,'right')
        bar(d,(x+13,y+8,x+120,y+11),m['hp'],GREY if daleko else HP)
        line(d,[(x+14,y+11),(x+14+round(105*m['mana']),y+11)],'grey0' if daleko else 'blue')
        if daleko: L.alpha_composite(ikona('zasieg'),(x+122,y+2))
        if m.get('martwy'): L.alpha_composite(ikona('dusza'),(x+122,y+1))
        y+=14
    return y

def pas(L,H,Wg,st):
    d=ImageDraw.Draw(L); hx=Wg//2-91
    hb=Image.open(HUD/'praca/pack/assets/minecraft/textures/gui/sprites/hud/hotbar.png').convert('RGBA'); L.alpha_composite(hb,(hx,H-22))
    y0=H-39; kieszen(L,d,hx,y0,'serce'); bar(d,(hx+12,y0,hx+80,y0+10),st['hp']/st['hpmax'],HP)
    text(L,hx+16,y0+2,f"{st['hp']}/{st['hpmax']}",'cream',False)
    bar(d,(hx+102,y0,hx+170,y0+10),.3,('soul','soul0','soul0')); kieszen(L,d,hx+172,y0,'dusza')
    for i in range(10):
        bx=hx+i*18; recess(d,(bx,y0+12,bx+16,y0+15)); rect(d,(bx+1,y0+13,bx+15,y0+14),'gold1') if i<8 else None
    d.ellipse((Wg//2-12,y0-11,Wg//2+11,y0+12),fill=c('ink')); d.ellipse((Wg//2-11,y0-10,Wg//2+10,y0+11),fill=c('gold1')); d.ellipse((Wg//2-9,y0-8,Wg//2+8,y0+9),fill=c('navy0'))
    text(L,Wg//2,y0-3,str(st['poziom']),'cream',False,'right') if False else text(L,Wg//2-6,y0-3,str(st['poziom']),'cream',False)

def mapa(im):
    m=Image.open(W/'dzien.png').convert('RGBA').crop((1606,60,1860,314)); im.alpha_composite(m,(1606,60))
    d=ImageDraw.Draw(im); d.rectangle((1602,56,1863,317),outline=(37,28,43,255),width=4)

def scena(nazwa,tlo,st,s=3):
    im=Image.open(W/tlo).convert('RGBA'); Wg,H=1920//s,1080//s
    if st.get('noc'): im=ImageEnhance.Brightness(im).enhance(.35)
    L=Image.new('RGBA',(Wg,H),T)
    y=okno(L,st,zwin=st.get('walka',False))
    y=towarzysze(L,y+2,st)
    if not st.get('walka'): cel(L, (319+3*s)//s, st)
    druzyna(L,y+3,st)
    pas(L,H,Wg,st)
    mapa(im); im.alpha_composite(L.resize((Wg*s,H*s),N))
    im.convert('RGB').save(W/'out'/f'{nazwa}_gui{s}.png')

BAZA=dict(nick='Przemidar',klasa='Nosiciel Relikwii',poziom=37,hp=52,hpmax=64,exp=.42,wiara='Solenna  140',limit=3,
          cel=('Kwietna Polana','2/5','Zbierz 3 Dusze przy kapliczce'))
PET=dict(nazwa='Iskrzyk',poziom=12,hp=31,hpmax=40,exp=.6)
SLUGI=[dict(hp=.9,s=11),dict(hp=.45,s=6)]
PARTY=[dict(nick='Mirela',lv=38,hp=.92,mana=.7,lider=True,wl='gold0'),dict(nick='Krzysztof',lv=36,hp=.22,mana=.4),
       dict(nick='Ostrowidz',lv=37,hp=.65,mana=.9,wl='ink'),dict(nick='Zgrzyt',lv=35,hp=.8,mana=.2,daleko=True)]
scena('A_solo_akolita','dzien.png',dict(BAZA,pet=PET,slugi=SLUGI))
scena('A_solo_akolita','dzien.png',dict(BAZA,pet=PET,slugi=SLUGI),s=2)
scena('B_druzyna_walka','walka.png',dict(BAZA,pet=PET,slugi=SLUGI,druzyna=PARTY,walka=True,hp=14))
scena('C_druzyna_pelna','dzien.png',dict(BAZA,pet=dict(PET,zemdlony=True),druzyna=PARTY+[dict(nick='Wędrowiec1234567',lv=34,hp=.5,mana=.5),dict(nick='Aelyra',lv=39,hp=1,mana=1),dict(nick='Gorrath',lv=36,hp=0,mana=0,martwy=True)]),s=3)
scena('C_druzyna_pelna','dzien.png',dict(BAZA,pet=dict(PET,zemdlony=True),druzyna=PARTY+[dict(nick='Wędrowiec1234567',lv=34,hp=.5,mana=.5),dict(nick='Aelyra',lv=39,hp=1,mana=1),dict(nick='Gorrath',lv=36,hp=0,mana=0,martwy=True)]),s=4)
print('ok')
