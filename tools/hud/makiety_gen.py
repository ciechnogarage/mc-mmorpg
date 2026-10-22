from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'praca/pack/assets/mythichud/textures/assets/karta'; OUT.mkdir(parents=True,exist_ok=True)
MC=ROOT/'praca/pack/assets/minecraft/textures/gui/sprites/hud'; MC.mkdir(parents=True,exist_ok=True)
MO=ROOT/'makiety'; MO.mkdir(exist_ok=True)
C={'dark':'#1b1513','wood':'#30231e','wood2':'#3b2a21','gold':'#a98b52','light':'#d7c191','slot':'#251b18','red':'#bf3732','redlight':'#e85c4b','blue':'#356a91','green':'#9cae56','exp':'#c8a75d','empty':'#49332a'}
def up(im):return im.resize((im.width*2,im.height*2),Image.Resampling.NEAREST)
def save2(im,name):up(im).save(OUT/name)
# 124 x 54 GUI: one outer line, simple leather surface and brass corner pins.
im=Image.new('RGBA',(124,54));d=ImageDraw.Draw(im)
d.rectangle((0,0,123,53),fill=C['dark']);d.rectangle((1,1,122,52),fill=C['gold']);d.rectangle((2,2,121,51),fill=C['wood']);d.rectangle((4,4,35,35),fill=C['dark']);d.rectangle((5,5,34,34),outline=C['gold']);d.rectangle((38,19,120,27),fill=C['dark']);d.rectangle((39,20,119,26),fill=C['empty']);
for y,h in [(31,5),(39,5),(47,3)]:d.rectangle((38,y,120,y+h-1),fill=C['dark']);d.rectangle((39,y+1,119,y+h-2),fill=C['empty'])
for x in (3,120):
 for y in (3,50):d.rectangle((x,y,x+1,y+1),fill=C['light'])
save2(im,'karta_tlo.png')
for name,col,w,h in [('hp',C['red'],80,5),('mana',C['blue'],80,3),('stamina',C['green'],80,3),('exp',C['exp'],80,1),('absorpcja',C['gold'],80,2)]:
 a=Image.new('RGBA',(w,h),col);ImageDraw.Draw(a).line((0,0,w-1,0),fill=C['light'] if name=='absorpcja' else col);save2(a,name+'.png')
a=Image.new('RGBA',(32,32));d=ImageDraw.Draw(a);d.rectangle((0,0,31,31),outline=C['redlight'],width=2);save2(a,'portret_alarm.png')
a=Image.new('RGBA',(20,12));d=ImageDraw.Draw(a);d.ellipse((0,0,19,11),fill=C['dark'],outline=C['gold']);save2(a,'medalion.png')
a=Image.new('RGBA',(96,15));d=ImageDraw.Draw(a);d.rectangle((0,0,95,14),fill=C['dark']);d.rectangle((1,1,94,13),fill=C['wood']);d.line((2,2,93,2),fill=C['gold']);save2(a,'koordynaty.png')
# Vanilla hotbar has to stay 182x22, because 2x would exceed the 256px Nexo limit.
a=Image.new('RGBA',(182,22));d=ImageDraw.Draw(a);d.rectangle((0,0,181,21),fill=C['dark']);d.rectangle((1,1,180,20),fill=C['gold']);d.rectangle((2,2,179,19),fill=C['wood']);
for i in range(9):
 x=3+i*20;d.rectangle((x,3,x+17,18),fill=C['slot']);d.line((x+1,4,x+16,4),fill=C['wood2'])
for x in (42,102,142):d.rectangle((x,1,x+1,20),fill=C['gold'])
for x in (1,180):d.rectangle((x,1,x+1,3),fill=C['light'])
a.save(MC/'hotbar.png')
# Mockups: world screenshot is real reference, scaled with nearest interpolation; old UI center is covered by a clean world crop.
ref=Image.open(ROOT/'ref/obecny_hud_minimapa_lewy_gorny.png').convert('RGB')
world=ref.crop((0,550,400,1080)).resize((1920,1080),Image.Resampling.NEAREST)
sky=Image.new('RGB',(1920,260),'#7b91a1');world.paste(sky,(0,0))
font=ImageFont.load_default(size=8)
def txt(d,xy,s,fill='#e5d1a5',scale=1):
 layer=Image.new('RGBA',(max(1,len(s)*9),16),(0,0,0,0));ImageDraw.Draw(layer).text((0,0),s,font=font,fill=fill,stroke_width=1,stroke_fill='#1b1513');return layer.resize((layer.width*scale,layer.height*scale),Image.Resampling.NEAREST),xy
def addtxt(im,xy,s,fill,scale):
 t,_=txt(None,xy,s,fill,scale);im.alpha_composite(t,xy)
def render(gui,variant,state):
 im=world.convert('RGBA');s=gui;low=state=='niskie_hp_trucizna';empty=state=='pusta_stamina';fight=state=='walka_chowancow'
 x,y=6*s,7*s
 base=Image.open(OUT/'karta_tlo.png');base=base.resize((124*s,54*s),Image.Resampling.NEAREST);im.alpha_composite(base,(x,y))
 face=Image.new('RGBA',(32*s,32*s),'#342e30');dd=ImageDraw.Draw(face);dd.rectangle((7*s,6*s,24*s,12*s),fill='#4e3428');dd.rectangle((9*s,14*s,22*s,26*s),fill='#c09368');dd.rectangle((10*s,16*s,12*s,18*s),fill='#201b1a');dd.rectangle((19*s,16*s,21*s,18*s),fill='#201b1a');im.alpha_composite(face,(x+4*s,y+4*s))
 if low: dd=ImageDraw.Draw(im);dd.rectangle((x+4*s,y+4*s,x+36*s,y+36*s),outline='#e85c4b',width=s)
 addtxt(im,(x+39*s,y+2*s),'WEDROWIEC','#e5d1a5',s)
 addtxt(im,(x+39*s,y+11*s),'Wojownik','#af996d',s)
 def bar(row,frac,color,h):
  yy=y+row*s;ImageDraw.Draw(im).rectangle((x+39*s,yy,x+(39+int(80*frac))*s,yy+h*s),fill=color)
 bar(20,.18 if low else 1,'#6c9b4c' if low else '#bf3732',6)
 bar(32,.7,'#356a91',3);bar(40,0 if empty else .76,'#9cae56',3);bar(48,.42,'#c8a75d',1)
 addtxt(im,(x+66*s,y+19*s),'4 / 20' if low else '20 / 20','#ff625b' if low else '#ffffff',s)
 addtxt(im,(x+3*s,y+37*s),'24','#f3d67b',s)
 if empty:addtxt(im,(x+39*s,y+37*s),'BRAK STAMINY','#ffbc69',s)
 # Physical pixel NMinimap, anchored top right by shader side metadata.
 map_x=1920-60-256;map_y=60;d=ImageDraw.Draw(im);d.rectangle((map_x-5,map_y-5,map_x+260,map_y+260),fill='#1b1513',outline='#a98b52',width=3);patch=ref.crop((64,64,312,312)).resize((256,256),Image.Resampling.NEAREST);im.paste(patch,(map_x,map_y))
 plate=Image.open(OUT/'koordynaty.png').resize((96*s,15*s),Image.Resampling.NEAREST);im.alpha_composite(plate,(1920-(102*s),map_y+260+10));addtxt(im,(1920-98*s,map_y+260+13),'X 154  Z 357','#e5d1a5',s)
 if fight:
  for j,label in enumerate(('Przeciwnik: Wilk','Twoj stwor: Kruk')):
   yy=y+62*s+j*48*s;d.rectangle((x,yy,x+124*s,yy+40*s),fill='#251b18',outline='#a98b52',width=s);addtxt(im,(x+5*s,yy+3*s),label,'#e5d1a5',s);d.rectangle((x+5*s,yy+26*s,x+100*s,yy+31*s),fill='#a33b35')
 # hotbar: same center and 182 GUI pixel width as vanilla. Scale 4 exceeds width on 1920? fits.
 hx=960-91*s;hy=1080-24*s
 if variant=='A':
  hb=Image.open(MC/'hotbar.png').resize((182*s,22*s),Image.Resampling.NEAREST);im.alpha_composite(hb,(hx,hy))
 else:
  d=ImageDraw.Draw(im)
  for first,last in ((0,1),(2,4),(5,6),(7,8)):
   xx=hx+(first*20)*s;ww=(last-first+1)*20*s;d.rectangle((xx,hy,xx+ww-2*s,hy+22*s),fill='#30231e',outline='#a98b52',width=s)
   for i in range(first,last+1):d.rectangle((hx+i*20*s+3*s,hy+3*s,hx+i*20*s+17*s,hy+18*s),fill='#251b18')
 for i,label in enumerate(('I','II','HP','MP','U','V','VI','M','D')):addtxt(im,(hx+(i*20+7)*s,hy+4*s),label,'#cab486',s)
 for xoff,label in ((8,'BRON'),(47,'MIKSTURY'),(113,'UZYTEK'),(155,'MAPA')):addtxt(im,(hx+xoff*s,hy-19*s),label,'#d7c191',s)
 addtxt(im,(hx+6*s,hy-43*s),'Lewa: Promienie','#d7c191',s);addtxt(im,(hx+111*s,hy-43*s),'Prawa: Promienie','#d7c191',s)
 for wx,label,col in ((960-132*s,'ZBROJA','#a1acbf'),(960+98*s,'GLOD','#bfae70')):
  d.rectangle((wx,hy+6*s,wx+34*s,hy+21*s),fill='#30231e',outline='#a98b52',width=s)
  addtxt(im,(wx+3*s,hy+7*s),label,'#cab486',s)
  d.rectangle((wx+3*s,hy+17*s,wx+29*s,hy+19*s),fill=col)
 out=MO/f'{variant}_gui{gui}_{state}.png';im.convert('RGB').save(out)
 return im
for g in (3,4):
 for v in ('A','B'):
  for st in ('pelne_hp','niskie_hp_trucizna','pusta_stamina','walka_chowancow'):
   im=render(g,v,st)
   if g==3 and v=='A' and st=='pelne_hp':im.crop((0,0,430,210)).resize((860,420),Image.Resampling.NEAREST).convert('RGB').save(MO/'karta_crop_x2.png')
print('generated',len(list(MO.glob('*.png'))),'mockups')
