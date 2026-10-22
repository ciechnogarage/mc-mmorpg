"""Próbnik materiałów Bagien: ściana 16×24, kostka i Hollow 16×24."""
from pathlib import Path
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from malarz import BAGNA_MATERIALY, maluj_bagna


OUT = Path(sys.argv[1]) if len(sys.argv)>1 else Path('/opt/docker/mc/data/world1/output/lochy/bagna/szkice/materialy_B.png')
FONT_PATH = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font = ImageFont.truetype(FONT_PATH, 15)
small = ImageFont.truetype(FONT_PATH, 12)
faces = ('up','north','down')
W,H = 850, 9*132+54
im = Image.new('RGB',(W,H),(145,145,145))
d = ImageDraw.Draw(im)
d.text((20,14),'BAGNA / PRÓBNIK MATERIAŁÓW   •   1 teksel = 1 px',font=font,fill=(24,24,24))
d.text((225,39),'ŚCIANA 16×24',font=small,fill=(25,25,25))
d.text((430,39),'KOSTKA',font=small,fill=(25,25,25))
d.text((650,39),'HOLLOW 16×24',font=small,fill=(25,25,25))
hollow=Image.open('/home/dev/.claude/jobs/b09594e6/tmp/hollow_atlas.png').convert('RGBA').crop((80,100,96,124))


def map_face(arr, points):
    """Rzutuj próbkę na płaszczyznę kostki, pasmami bez interpolacji."""
    p0,p1,p2,p3=points
    def pt(u,v):
        return ((1-u)*(1-v)*p0[0]+u*(1-v)*p1[0]+u*v*p2[0]+(1-u)*v*p3[0],
                (1-u)*(1-v)*p0[1]+u*(1-v)*p1[1]+u*v*p2[1]+(1-u)*v*p3[1])
    d.polygon(points,fill=(100,100,100))
    for yy in range(8):
        for xx in range(8):
            pix=arr[int((yy+.5)*arr.shape[0]/8),int((xx+.5)*arr.shape[1]/8)]
            if pix[3]==0:
                continue
            d.polygon([pt(xx/8,yy/8),pt((xx+1)/8,yy/8),
                       pt((xx+1)/8,(yy+1)/8),pt(xx/8,(yy+1)/8)],fill=tuple(pix[:3]))
    d.line(points+[points[0]],fill=(40,40,40),width=1)


def cube(arrays, cx, y, direction):
    # Dwie perspektywy: zwykła kostka pokazuje górę i bok, odwrócona spód.
    if direction=='down':
        a=(cx,y+7); b=(cx+32,y+23); c=(cx,y+40); e=(cx-32,y+23)
        g=(cx+32,y+55); f=(cx,y+72); h=(cx-32,y+55)
        map_face(arrays[1],[a,b,c,e])
        map_face(arrays[1],[e,c,f,h])
        map_face(arrays[2],[c,b,g,f])
    else:
        a=(cx,y+7); b=(cx+32,y+23); c=(cx,y+40); e=(cx-32,y+23)
        g=(cx+32,y+75); f=(cx,y+92); h=(cx-32,y+75)
        map_face(arrays[0],[a,b,c,e])
        map_face(arrays[1],[e,c,f,h])
        map_face(arrays[1],[c,b,g,f])

for i,nazwa in enumerate(BAGNA_MATERIALY):
    y=58+i*132
    rng=np.random.default_rng(1029+i)
    arrays=[]
    for j,face in enumerate(faces):
        cfg=nazwa if nazwa=='rzesa' else {'material':nazwa,'mul_od_dolu':True,'rzesa_od_gory':True}
        arr=maluj_bagna(face,16,24,cfg,rng)
        arrays.append(arr)
    tile=Image.fromarray(arrays[1],'RGBA').resize((64,96),Image.Resampling.NEAREST)
    base=Image.new('RGB',tile.size,(115,115,115))
    base.paste(tile,mask=tile.getchannel('A'))
    im.paste(base,(225,y))
    d.rectangle((224,y-1,289,y+96),outline=(50,50,50),width=1)
    cube(arrays,460,y,'north')
    reference=hollow.resize((64,96),Image.Resampling.NEAREST)
    im.paste(reference,(650,y),reference)
    d.rectangle((649,y-1,714,y+96),outline=(50,50,50),width=1)
    d.text((20,y+12),nazwa,font=font,fill=(20,20,20))
    rgb=np.concatenate([a[:,:,:3].reshape(-1,3) for a in arrays])
    colors=len(np.unique(rgb,axis=0))
    pairs=[]
    for a in arrays:
        visible=a[:,:,3]>0
        eq_h=np.all(a[:,:-1,:3]==a[:,1:,:3],axis=2)
        eq_v=np.all(a[:-1,:,:3]==a[1:,:,:3],axis=2)
        pairs.extend(eq_h[visible[:,:-1]&visible[:,1:]].tolist())
        pairs.extend(eq_v[visible[:-1,:]&visible[1:,:]].tolist())
    pct=100*sum(pairs)/len(pairs) if pairs else 0
    _,counts=np.unique(arrays[1][:,:,:3].reshape(-1,3),axis=0,return_counts=True)
    dominant=100*counts.max()/counts.sum()
    d.text((20,y+47),f'{colors} tony  /  baza {dominant:.1f}%',font=small,fill=(25,25,25))
    print(f'{nazwa:16s} {colors} tony   baza {dominant:.1f}%   sąsiedzi {pct:.1f}%')
    d.line((16,y+113,W-16,y+113),fill=(105,105,105),width=1)

OUT.parent.mkdir(parents=True,exist_ok=True)
im.save(OUT)
print(OUT)
