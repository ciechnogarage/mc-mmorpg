#!/usr/bin/env python3
"""Portal 54-slotowy: prawdziwe kafle Mapa, 9 punktów, skala tekstur 2 px/GUI."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import runpy

out = Path(__file__).resolve().parent
font = runpy.run_path(str(out/'rysuj_ksiege.py'))['NARROW']
font['·']=('00','00','00','00','00','11','11','00','00')
for digit, rows in {
 '0':('01110','10001','10011','10101','11001','10001','01110'),
 '1':('00100','01100','00100','00100','00100','00100','01110'),
 '2':('01110','10001','00001','00010','00100','01000','11111'),
 '3':('11110','00001','00001','01110','00001','00001','11110'),
 '4':('00010','00110','01010','10010','11111','00010','00010'),
 '5':('11111','10000','10000','11110','00001','00001','11110'),
 '6':('01110','10000','10000','11110','10001','10001','01110'),
 '7':('11111','00001','00010','00100','01000','01000','01000'),
 '8':('01110','10001','10001','01110','10001','10001','01110'),
 '9':('01110','10001','10001','01111','00001','00001','01110'),
 '/':('00001','00001','00010','00100','01000','10000','10000'),
}.items():font[digit]=('00000',)+rows+('00000',)
P = {
 'ink':'#251c2b', 'leather':'#39232e', 'rim':'#61323a',
 'blue':'#183650', 'blue2':'#286287', 'sea':'#496e78',
 'paper':'#b7a17b', 'cream':'#d5c39c', 'sand':'#bca57c',
 'gold':'#ce9135', 'light':'#f4c657', 'forest':'#536b54',
 'stone':'#899499',
}
C={k:tuple(bytes.fromhex(v[1:])) for k,v in P.items()}
im=Image.new('RGB',(352,444),C['leather']);d=ImageDraw.Draw(im)
def box(b,c,edge=None,w=1):d.rectangle(b,fill=C[c],outline=C[edge] if edge else None,width=w)
def ln(p,c,w=1):d.line(p,fill=C[c],width=w)
def ell(b,c,edge=None,w=1):d.ellipse(b,fill=C[c],outline=C[edge] if edge else None,width=w)
def text_width(s):return sum(len(font[c][0])+1 for c in s)-1
def txt(s,x,y,color='blue',align='left'):
 assert s==s.upper() and all(c in font for c in s),s
 w=text_width(s)*2
 if align=='right': x-=w
 elif align=='center':x-=w//2
 x-=x%2;y-=y%2
 for ch in s:
  rows=font[ch]
  for j,row in enumerate(rows):
   for i,bit in enumerate(row):
    if bit=='1':box((x+2*i,y+2*j,x+2*i+1,y+2*j+1),color)
  x+=(len(rows[0])+1)*2
 return w

def terrain(rgb):
 r,g,b=rgb
 # Beige outside the captured Minecraft tiles is transparent map padding.
 if rgb==(196,178,136):return C['sea']
 if b>r*1.35 and b>g*1.15:return C['sea']
 if g>r*1.17 and g>b*1.08:return C['forest']
 if r>g*1.28 and r>b*1.3:return C['gold'] if r>105 else C['forest']
 if min(rgb)>100:return C['sand']
 if max(rgb)<45:return C['forest']
 return C['sand'] if r+g>b*1.9 else C['stone']

# Source is a copy of real world/b6 map tiles. Crop covers all seven zone markers.
source=Image.open(out/'world-map-source.png').convert('RGB').crop((10,40,148,224))
source.putdata([terrain(p) for p in source.get_flattened_data()])
# Smooth on the source grid so scattered map-tile pixels cannot become patches.
source=source.quantize(colors=5,dither=Image.Dither.NONE)
source=source.filter(ImageFilter.ModeFilter(9)).filter(ImageFilter.ModeFilter(9)).convert('RGB')
assert len(source.getcolors(138*184) or [])<=5
source=source.resize((308,220),Image.Resampling.NEAREST)
box((0,0,351,443),'leather')
box((4,8,347,438),'rim','ink',2)
box((9,14,342,433),'leather','gold',2)
box((14,31,337,261),'paper','ink',2)
im.paste(source,(22,35))
d.rectangle((20,33,330,257),outline=C['gold'],width=2)
d.rectangle((23,36,327,254),outline=C['paper'],width=1)
box((70,4,281,34),'ink','gold',2)
box((74,8,277,30),'blue','light')
txt('PORTAL · MAPA KRAINY',176,8,'light','center')
for x in (85,267):ell((x-4,14,x+4,22),'gold','ink')

# Item stacks occupy the circles; fixed labels remain legible when locked.
nodes=[
 ('SZEPCZĄCY','GAJ',1),
 ('WIETRZNE','TURNIE',5),
 ('WILCZE','OSTĘPY',19),
 ('START',None,22),
 ('RDZAWE','WĄWOZY',25),
 ('ŚWIETLISTY','GĄSZCZ',37),
 ('PERŁOWA','ZATOKA',41),
 ('WYGASŁE','PALENISKO',43),
]
labels=[]
circles=[]
for line1,line2,slot in nodes:
 col,row=slot%9,slot//9
 x,y=32+36*col,52+36*row
 circles.append((x-16,y-16,x+16,y+16))
 ell((x-16,y-16,x+16,y+16),'ink','gold',2)
 ell((x-14,y-14,x+14,y+14),'blue2','light')
 ell((x-10,y-10,x+10,y+10),'blue','gold')
 lines=[line1]+([line2] if line2 else [])
 width=max(text_width(s)*2 for s in lines)+8
 px=x-width//2+(2 if slot==43 else 0)
 py=y+17
 right=px+width-1; bottom=py+(38 if line2 else 22)
 assert 24<=px and right<=327 and bottom<=255, (line1,px,py,right,bottom)
 for old in labels:
  assert right+4<old[0] or px>old[2]+4 or bottom+4<old[1] or py>old[3]+4, (line1,old)
 labels.append((px,py,right,bottom))
 box((px,py,right,bottom),'paper','gold')
 box((px+2,py+2,right-2,bottom-2),'cream')
 for j,s in enumerate(lines):
  text_y=(py+4+j*14)//2*2
  ink_rows=[k for ch in s for k,row_bits in enumerate(font[ch]) if '1' in row_bits]
  assert text_y+2*min(ink_rows)-py>=3
  assert bottom-(text_y+2*max(ink_rows)+1)>=3
  txt(s,x+(2 if slot==43 else 0),py+4+j*14,align='center')
for i,(left,top,right,bottom) in enumerate(labels):
 for j,(cl,ct,cr,cb) in enumerate(circles):
  if i!=j:assert right<cl or left>cr or bottom<ct or top>cb, (nodes[i],nodes[j])

# Lower inventory: three aligned actions on the actual clickable slots.
box((5,264,346,437),'rim','gold')
box((9,268,342,433),'leather')
ln([(17,358),(335,358)],'gold')
# Back arrow is inventory A, column 1.
box((45,277,105,310),'ink','gold',2)
box((49,281,101,306),'blue2','light')
d.polygon([(58,294),(73,283),(73,289),(91,289),(91,299),(73,299),(73,305)],fill=C['light'])
txt('MENU',75,329,'light','center')
# Nexus now occupies column 4.
ell((146,270,206,330),'ink','gold',2)
ell((150,274,202,326),'blue','light',2)
ell((156,280,196,320),'blue2','gold',2)
box((139,334,213,354),'paper','gold')
txt('STOLICA',176,336,'blue','center')
# The numeric variant is wholly inside the fourth texture tile.
box((244,276,330,348),'ink','gold',2)
box((248,280,326,344),'blue','light')
txt('ODKRYTE',287,284,'light','center')
box((254,305,320,337),'paper','gold')
txt('ODKRYWAJ MIEJSCA',176,375,'light','center')
txt('CHODZĄC PO ŚWIECIE',176,398,'cream','center')
ln([(19,426),(333,426)],'gold')
assert im.size==(352,444)
assert len(im.getcolors(352*444) or [])<=14
im.save(out/'portal_2x.png')
for i,(x,y) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
 im.crop((x,y,x+176,y+222)).save(out/f'portal_{i}.png')
base=im.copy()
for count in range(10):
 im=base.copy();d=ImageDraw.Draw(im)
 txt(f'{count}/9',287,309,'blue','center')
 im.crop((176,222,352,444)).save(out/f'portal_4_count_{count}.png')
print('Portal 352x444; paleta',len(im.getcolors(352*444)),'kolorów')
