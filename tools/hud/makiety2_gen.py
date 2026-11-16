from pathlib import Path
from collections import Counter
import json
from PIL import Image, ImageDraw, ImageEnhance

ROOT = Path(__file__).resolve().parent
OUT = ROOT/'praca/pack/assets/mythichud/textures/assets/karta'
HOT = ROOT/'praca/pack/assets/minecraft/textures/gui/sprites/hud'
MO = ROOT/'makiety2'
for path in (OUT, HOT, MO): path.mkdir(parents=True, exist_ok=True)
# Fourteen shared opaque colours, sampled/adapted from Księga Wędrowca (kb_1..4).
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C',
     'gold0':'#8A6A30','gold1':'#D5A96B','gold2':'#FFE7AE','cream':'#FFF1CC',
     'navy0':'#183650','navy1':'#286287','blue':'#3F98CB','red':'#CE4042',
     'redhi':'#F36D62','green':'#61964A'}
T=(0,0,0,0)
N=Image.Resampling.NEAREST

def rect(d, box, c):
    x0,y0,x1,y1=box
    d.rectangle((min(x0,x1),min(y0,y1),max(x0,x1),max(y0,y1)),fill=P.get(c,c))
def line(d, pts, c, w=1): d.line(pts, fill=P.get(c,c), width=w)
def save2(im, name): im.resize((im.width*2,im.height*2),N).save(OUT/name)
def corner(d,x,y,sx,sy):
    # Two-pixel L fitting an irregular corner, with a one-pixel rivet.
    rect(d,(x,y,x+sx*5,y+sy),'gold0');rect(d,(x,y,x+sx,y+sy*5),'gold0')
    rect(d,(x+sx,y+sy,x+sx*4,y+sy),'gold2');rect(d,(x+sx,y+sy,x+sx,y+sy*4),'gold2')
    rect(d,(x+sx*2,y+sy*2,x+sx*2,y+sy*2),'cream')

def panel(d,box):
    x0,y0,x1,y1=box
    rect(d,(x0+2,y0,x1-2,y1),'ink');rect(d,(x0,y0+2,x1,y1-2),'ink')
    rect(d,(x0+2,y0+1,x1-2,y1-1),'leather0');rect(d,(x0+1,y0+2,x1-1,y1-2),'leather0')
    rect(d,(x0+3,y0+3,x1-3,y1-3),'leather1')
    line(d,[(x0+6,y0+2),(x1-6,y0+2)],'leather2')
    line(d,[(x0+4,y1-2),(x1-4,y1-2)],'leather0')
    for xx,sx in ((x0+3,1),(x1-3,-1)):
        for yy,sy in ((y0+3,1),(y1-3,-1)): corner(d,xx,yy,sx,sy)

# 142x62 GUI split so every 2x source dimension stays <=256.
card=Image.new('RGBA',(142,66),T);d=ImageDraw.Draw(card)
panel(d,(0,0,141,65))
# One navy nameplate spans the width. A 16-character wide test name fits here.
rect(d,(4,3,137,17),'gold0');rect(d,(5,4,136,16),'navy0')
line(d,[(6,4),(135,4)],'gold1');line(d,[(6,16),(135,16)],'ink')
rect(d,(8,6,9,8),'gold2');rect(d,(132,6,133,8),'gold2')
# Portrait: dark contour at 3..40, gold 2px frame at 4..39, inner shadow, 32px head at 6..37.
rect(d,(3,21,40,58),'ink');rect(d,(4,22,39,57),'gold0')
line(d,[(5,23),(38,23)],'gold2');line(d,[(5,23),(5,56)],'gold2')
line(d,[(38,23),(38,56)],'leather0');line(d,[(6,56),(38,56)],'leather0')
rect(d,(6,24,37,55),'ink')
# Class recess; same edge vocabulary, no separate noisy outline.
rect(d,(42,19,139,30),'leather0');line(d,[(43,19),(138,19)],'leather2')
# Bar recesses. HP 8px, mana/stamina 4px, EXP 2px seam.
for box in ((46,33,138,42),(53,45,138,50),(53,53,138,58),(46,62,138,64)):
    x0,y0,x1,y1=box
    rect(d,(x0+1,y0,x1-1,y1),'ink');rect(d,(x0,y0+1,x1,y1-1),'ink')
    if y1-y0>2:rect(d,(x0+1,y0+1,x1-1,y1-1),'leather0')
# Icon beds sit left of mana/stamina bars.
rect(d,(45,44,52,51),'navy0');rect(d,(45,52,52,59),'leather0')
save2(card.crop((0,0,124,66)),'karta_tlo.png')
save2(card.crop((124,0,142,66)),'karta_koniec.png')

def bar(w,h,tones,name):
    im=Image.new('RGBA',(w,h),T);d=ImageDraw.Draw(im)
    rect(d,(1,0,w-2,h-1),tones[1])
    line(d,[(2,0),(w-3,0)],tones[0])
    if h>=3:line(d,[(1,h-1),(w-2,h-1)],tones[2])
    im.putpixel((0,0),T);im.putpixel((w-1,0),T)
    im.putpixel((0,h-1),T);im.putpixel((w-1,h-1),T)
    save2(im,name+'.png')
bar(90,8,('redhi','red','leather2'),'hp')
bar(90,8,('gold1','green','gold0'),'hp_trucizna')
bar(90,8,('leather2','leather0','ink'),'hp_uschniecie')
bar(84,4,('blue','navy1','navy0'),'mana')
bar(84,4,('gold2','green','gold0'),'stamina')
bar(90,2,('gold2','gold0','gold0'),'exp')
bar(90,2,('gold2','gold1','gold0'),'absorpcja')
# 1px GUI red edge, small corner sparks, transparency over the portrait.
im=Image.new('RGBA',(38,38),T);d=ImageDraw.Draw(im)
line(d,[(1,6),(1,31),(6,36),(31,36),(36,31),(36,6),(31,1),(6,1),(1,6)],'red')
for x,y in ((1,4),(4,1),(33,1),(36,4),(1,33),(4,36),(33,36),(36,33)):rect(d,(x,y,x,y),'redhi')
save2(im,'portret_alarm.png')
# Round 20 GUI medal with 2-tone rim, dark face, one small glint.
im=Image.new('RGBA',(20,20),T);d=ImageDraw.Draw(im)
d.ellipse((0,0,19,19),fill=P['ink']);d.ellipse((1,1,18,18),fill=P['gold0'])
d.ellipse((2,2,17,17),fill=P['gold1']);d.ellipse((3,3,16,16),fill=P['navy0'])
line(d,[(5,4),(9,3),(14,5)],'gold2');line(d,[(5,15),(10,16),(14,14)],'gold0')
save2(im,'medalion.png')
# Distinct 7x7 GUI resource glyphs.
for name in ('kropla','blysk'):
    im=Image.new('RGBA',(7,7),T);d=ImageDraw.Draw(im)
    if name=='kropla':
        for y,x0,x1 in ((0,3,3),(1,2,4),(2,2,4),(3,1,5),(4,1,5),(5,2,4),(6,3,3)):rect(d,(x0,y,x1,y),'blue' if y<4 else 'navy1')
        rect(d,(2,2,2,3),'gold2')
    else:
        for box in ((3,0,4,0),(2,1,4,1),(2,2,3,2),(1,3,3,3),(3,4,4,4),(3,5,3,5),(2,6,2,6)):rect(d,box,'gold2')
        line(d,[(4,1),(4,3),(3,4)],'gold0')
    save2(im,name+'.png')
# Coordinates and minimap frame reuse leather, same L-corners.
im=Image.new('RGBA',(64,14),T);d=ImageDraw.Draw(im);panel(d,(0,0,63,13));save2(im,'koordynaty.png')
im=Image.new('RGBA',(131,131),T);d=ImageDraw.Draw(im)
for box,col in (((0,0,130,1),'ink'),((0,129,130,130),'ink'),((0,0,1,130),'ink'),((129,0,130,130),'ink'),
                ((2,2,128,2),'leather2'),((2,128,128,128),'leather0'),((2,2,2,128),'leather2'),((128,2,128,128),'leather0')):rect(d,box,col)
for xx,sx in ((3,1),(127,-1)):
    for yy,sy in ((3,1),(127,-1)):corner(d,xx,yy,sx,sy)
im.save(ROOT/'praca/NMinimap/frames/karta_square.png')
# Vanilla hotbar source remains 1x due 256px import limit. Nine readable 20px slots.
im=Image.new('RGBA',(182,22),T);d=ImageDraw.Draw(im)
rect(d,(2,0,179,21),'ink');rect(d,(0,2,181,19),'ink')
rect(d,(2,1,179,20),'leather0');rect(d,(1,2,180,19),'leather0')
line(d,[(4,1),(177,1)],'leather2');line(d,[(4,20),(177,20)],'gold0')
for i in range(9):
    x=3+i*20
    rect(d,(x,3,x+17,18),'ink');rect(d,(x+1,4,x+16,17),'leather0' if i<2 or i>=7 else 'leather1')
    line(d,[(x+2,4),(x+15,4)],'leather2')
for x in (42,102,142):
    line(d,[(x,2),(x,19)],'gold0');rect(d,(x,1,x+1,3),'gold1');rect(d,(x,18,x+1,20),'gold1')
for x in (2,178):rect(d,(x,2,x+1,4),'gold1')
im.save(HOT/'hotbar.png')
# Compact shield/armor and drumstick/hunger wings, 35x18 GUI.
def wing(name):
    im=Image.new('RGBA',(35,18),T);d=ImageDraw.Draw(im);panel(d,(0,0,34,17))
    if name=='zbroja':
        for y,x0,x1 in ((3,5,10),(4,4,11),(5,4,11),(6,4,11),(7,4,11),(8,5,10),(9,5,10),(10,6,9),(11,7,8)):rect(d,(x0,y,x1,y),'gold1')
        rect(d,(6,5,9,8),'navy0');rect(d,(7,5,7,10),'gold2')
    else:
        for y,x0,x1 in ((4,4,8),(5,3,9),(6,3,10),(7,3,10),(8,4,9),(9,5,8)):rect(d,(x0,y,x1,y),'gold1')
        rect(d,(9,9,11,10),'cream');rect(d,(11,10,12,11),'cream')
    save2(im,name+'_skrzydlo.png')
wing('zbroja');wing('glod')
# Hunger fill in a 16px recess. Three tones; armor now number, not a bar.
im=Image.new('RGBA',(16,3),T);d=ImageDraw.Draw(im)
line(d,[(0,0),(15,0)],'gold2');line(d,[(0,1),(15,1)],'gold1');line(d,[(0,2),(15,2)],'gold0');save2(im,'glod_fill.png')
# No longer used by YAML, generated only for compatibility with a previous staged pack.
im=Image.new('RGBA',(2,2),T);save2(im,'dane.png')
(OUT/'zbroja_fill.png').unlink(missing_ok=True)

# Exact bitmap atlas layout from default.json, including Polish accented glyphs.
providers=json.loads((ROOT/'mcfont/assets/minecraft/font/include/default.json').read_text())['providers']
FONT={}
for p in providers:
    if p['file'] not in ('minecraft:font/ascii.png','minecraft:font/accented.png'):continue
    sheet=Image.open(ROOT/'mcfont/assets/minecraft/textures/font'/p['file'].split('/')[-1]).convert('RGBA')
    cw=sheet.width//16;ch=sheet.height//len(p['chars'])
    for row,chars in enumerate(p['chars']):
        for col,char in enumerate(chars):
            if char=='\0' or char in FONT:continue
            glyph=sheet.crop((col*cw,row*ch,(col+1)*cw,(row+1)*ch))
            bbox=glyph.getchannel('A').getbbox()
            if bbox:
                glyph=glyph.crop((0,0,bbox[2],ch));advance=bbox[2]+1
            else: glyph=Image.new('RGBA',(0,ch));advance=4
            FONT[char]=(glyph,advance)
def text_width(s):return sum(FONT.get(c,FONT['?'])[1] for c in s)
def mc_text(im,xy,s,color='cream',scale=1,align='left',outline=True):
    w=text_width(s);h=14
    tmp=Image.new('RGBA',(w+4,h+4),T);x=1
    for c in s:
        g,advance=FONT.get(c,FONT['?'])
        if g.width:
            alpha=g.getchannel('A');tile=Image.new('RGBA',g.size,P.get(color,color));tile.putalpha(alpha)
            if outline:
                shade=Image.new('RGBA',g.size,P['ink']);shade.putalpha(alpha)
                tmp.alpha_composite(shade,(x+1,2))
            tmp.alpha_composite(tile,(x,1))
        x+=advance
    x,y=xy
    if align=='center':x-=w*scale//2
    if align=='right':x-=w*scale
    im.alpha_composite(tmp.resize((tmp.width*scale,tmp.height*scale),N),(int(x),int(y)))

# Mockups are compositions at actual GUI 3/4 pixel steps. HUD art is never blurred.
ref=Image.open(ROOT/'ref/obecny_hud_minimapa_lewy_gorny.png').convert('RGB')
world=ref.crop((0,550,400,1080)).resize((1920,1080),N)
world.paste(Image.new('RGB',(1920,260),'#7B91A1'),(0,0))
def texture(name):return Image.open(OUT/(name+'.png')).convert('RGBA')
def paste_gui(im,asset,x,y,s):
    if isinstance(asset,str):asset=texture(asset)
    im.alpha_composite(asset.resize((asset.width*s//2,asset.height*s//2),N),(int(x),int(y)))
def item_icon(im,cx,cy,s,kind):
    icon=Image.new('RGBA',(14,14),T);d=ImageDraw.Draw(icon)
    if kind=='sword':
        line(d,[(3,11),(10,4)],'cream',2);rect(d,(9,2,11,4),'gold2');line(d,[(2,9),(6,13)],'gold1',2);rect(d,(1,11,3,13),'leather0')
    elif kind=='potion':
        rect(d,(5,1,8,3),'gold1');rect(d,(4,4,9,10),'red');line(d,[(4,4),(9,4)],'cream');rect(d,(5,8,6,9),'redhi')
    elif kind=='map':
        rect(d,(2,2,11,11),'cream');line(d,[(4,3),(4,10),(7,8),(9,10)],'navy1');rect(d,(7,4,8,5),'red')
    elif kind=='book':
        rect(d,(2,3,11,11),'gold0');rect(d,(3,4,10,10),'navy0');line(d,[(6,4),(6,10)],'cream');rect(d,(7,5,8,6),'gold2')
    elif kind=='bag':
        rect(d,(3,5,10,11),'gold0');rect(d,(4,6,9,10),'leather2');line(d,[(5,3),(8,3)],'cream')
    im.alpha_composite(icon.resize((14*s,14*s),N),(int(cx-7*s),int(cy-7*s)))

def render_card(im,s,state,name='Wędrowiec1234567',klass='Strażnik Żywiołów',level='24'):
    x,y=6*s,8*s;low=state=='niskie_hp_trucizna';empty=state=='pusta_stamina'
    paste_gui(im,'karta_tlo',x,y,s);paste_gui(im,'karta_koniec',x+124*s,y,s)
    # Simulated 32x32 GUI head. Live texture comes from the MythicHUD head layer.
    face=Image.new('RGBA',(32,32),P['leather0']);fd=ImageDraw.Draw(face)
    rect(fd,(0,0,31,8),'ink');rect(fd,(3,6,28,12),'leather2');rect(fd,(4,13,27,27),'gold1')
    rect(fd,(7,17,11,19),'ink');rect(fd,(20,17,24,19),'ink');rect(fd,(13,24,18,25),'leather2')
    im.alpha_composite(face.resize((32*s,32*s),N),(x+6*s,y+24*s))
    if low:paste_gui(im,'portret_alarm',x+3*s,y+21*s,s)
    # Fill is 3-tone texture, clipped horizontally to listener fraction.
    def fill(asset,gx,gy,width,frac):
        source=texture(asset);gui=source.resize((source.width//2,source.height//2),N)
        n=max(0,min(width,round(width*frac)))
        if n:im.alpha_composite(gui.crop((0,0,n,gui.height)).resize((n*s,gui.height*s),N),(x+gx*s,y+gy*s))
    fill('hp_trucizna' if low else 'hp',47,34,90,.18 if low else 1)
    fill('mana',54,46,84,.7);fill('stamina',54,54,84,0 if empty else .76)
    fill('exp',47,62,90,.42)
    if not low:fill('absorpcja',47,40,90,.21)
    paste_gui(im,'kropla',x+45*s,y+44*s,s);paste_gui(im,'blysk',x+45*s,y+52*s,s)
    paste_gui(im,'medalion',x+24*s,y+45*s,s)
    mc_text(im,(x+8*s,y+4*s),name,'cream',s)
    mc_text(im,(x+43*s,y+19*s),klass,'gold1',s)
    mc_text(im,(x+92*s,y+33*s),'4 / 20' if low else '20 / 20','cream',s,align='center')
    mc_text(im,(x+34*s,y+49*s),level,'cream',s,align='center')
    if empty:mc_text(im,(x+134*s,y+50*s),'!','redhi',s,align='right')
    return x,y

def render(gui,variant,state):
    s=gui;im=world.convert('RGBA')
    if state=='noc':im=ImageEnhance.Brightness(im).enhance(.28)
    x,y=render_card(im,s,state)
    # NMinimap uses physical screen pixels and its own frame.
    mapx=1920-60-256;mapy=60
    patch=ref.crop((64,64,312,312)).resize((254,254),N).convert('RGBA')
    im.alpha_composite(patch,(mapx+1,mapy+1))
    frame=Image.open(ROOT/'praca/NMinimap/frames/karta_square.png').convert('RGBA').resize((262,262),N)
    im.alpha_composite(frame,(mapx-3,mapy-3))
    # Fixed GUI placement chosen for GUI 3; GUI 4 alignment must be checked in client.
    plate_y=108*s;plate_x=1920-(6+64)*s
    paste_gui(im,'koordynaty',plate_x,plate_y,s)
    mc_text(im,(plate_x+5*s,plate_y+2*s),'X 154 Z 357','cream',s)
    if state=='walka_chowancow':
        d=ImageDraw.Draw(im)
        for j,label in enumerate(('Przeciwnik: Wilk','Twój stwór: Kruk')):
            yy=y+(74+j*50)*s
            rect(d,(x,yy,x+141*s,yy+40*s),'ink');rect(d,(x+s,yy+s,x+140*s,yy+39*s),'leather0')
            mc_text(im,(x+5*s,yy+3*s),label,'cream',s)
            rect(d,(x+5*s,yy+27*s,x+100*s,yy+31*s),'red')
    hx=960-91*s;hy=1080-24*s
    hot=Image.open(HOT/'hotbar.png').convert('RGBA').resize((182*s,22*s),N)
    im.alpha_composite(hot,(hx,hy))
    # Occupied slots: items only, no slot letters. Live game supplies the actual items.
    for i,kind in {0:'sword',1:'sword',2:'potion',3:'potion',4:'potion',5:'bag',6:'book',7:'map',8:'book'}.items():
        item_icon(im,hx+(12+i*20)*s,hy+11*s,s,kind)
    # Variant A has small pictogram tabs. B is the clean recommended configuration.
    if variant=='A':
        for ix,kind in ((22,'sword'),(72,'potion'),(122,'bag'),(162,'map')):
            xx=hx+ix*s;yy=hy-8*s;d=ImageDraw.Draw(im)
            rect(d,(xx-6*s,yy-2*s,xx+6*s,yy+8*s),'ink')
            rect(d,(xx-5*s,yy-s,xx+5*s,yy+7*s),'gold0')
            tiny=Image.new('RGBA',(14,14),T)
            item_icon(tiny,7,7,1,kind)
            im.alpha_composite(tiny.resize((9*s,9*s),N),(xx-4*s,yy-s))
    # Compact wing assets and text number / 3-tone hunger bar.
    wx=hx-42*s;wy=hy+2*s
    paste_gui(im,'zbroja_skrzydlo',wx,wy,s)
    mc_text(im,(wx+26*s,wy+4*s),'12','cream',s,align='center')
    wx=hx+189*s
    paste_gui(im,'glod_skrzydlo',wx,wy,s)
    paste_gui(im,'glod_fill',wx+15*s,wy+10*s,s)
    mc_text(im,(hx-5*s,hy-29*s),'Lewa: Promienie','cream',s)
    mc_text(im,(hx+187*s,hy-29*s),'Prawa: Promienie','cream',s,align='right')
    target=MO/f'{variant}_gui{gui}_{state}.png'
    im.convert('RGB').save(target)
    return im

for gui in (3,4):
    for variant in ('A','B'):
        for state in ('pelne_hp','niskie_hp_trucizna','pusta_stamina','walka_chowancow','noc'):
            render(gui,variant,state)
# Focused review artifacts: texture at exact x4 and HUD bottom crop at GUI 3 x4.
review=Image.new('RGBA',(154,74),T)
render_card(review,1,'pelne_hp',name='Wędrowiec1234567',klass='Wołający Przodków')
review.crop((6,8,148,74)).resize((568,264),N).save(MO/'karta_x4.png')
levels=Image.new('RGBA',(568*3,264),T)
for j,n in enumerate(('1','10','100')):
    sample=Image.new('RGBA',(154,74),T)
    render_card(sample,1,'pelne_hp',name='Wędrowiec1234567',klass='Wołający Przodków',level=n)
    levels.alpha_composite(sample.crop((6,8,148,74)).resize((568,264),N),(j*568,0))
levels.save(MO/'karta_poziomy_x4.png')
full=Image.open(MO/'B_gui3_pelne_hp.png')
full.crop((590,950,1330,1080)).resize((2960,520),N).save(MO/'dol_x4.png')
print('generated',len(list(MO.glob('*.png'))),'mockups; long name',text_width('Wędrowiec1234567'),'class',text_width('Strażnik Żywiołów'),'worst 16W',text_width('W'*16))
