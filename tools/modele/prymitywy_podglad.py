"""Powtarzalny próbnik sześciu brył, renderowany bez serwera."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from bryly_sdf import Model

OUT=Path('/opt/docker/mc/data/world1/output/lochy/bagna/_zespol/prymitywy.png')
MAT={k:{'ramp':[tuple(max(0,int(v*f)) for v in rgb) for f in (.55,.75,.9,1)]}
     for k,rgb in {'a':(101,130,79),'b':(204,182,119),'c':(25,159,154),'d':(76,44,43)}.items()}

def sample(name):
    m=Model(MAT,budzet=3000)
    m.czesc('forma',(0,0,0))
    if name=='spirala_skorupy':m.spirala_skorupy('forma',(0,10,0),7,2.1,5,'a','b')
    elif name=='jajo_oplot':m.jajo_oplot('forma',(0,10,0),6,9,5,6,'a','b')
    elif name=='noga_stawowa':m.noga_stawowa('forma',(-5,8,0),(0,20,0),(8,0,0),(2,1.5,1),'a')
    elif name=='plomien':m.plomien('forma',(0,2,0),4,[(-35,9),(0,13),(42,8)],'c','b')
    elif name=='wneka':
        m.elipsoida('forma',(0,10,0),(7,8,6),'a')
        m.wneka('forma',(0,10,-6),(4,4,5),5,('d','b'))
    elif name=='pierscienie':
        m.elipsoida('forma',(0,7,0),(5,5,13),'a')
        m.pierscienie('forma','z',3,.27,'b')
    return m.czesci['forma'].vox

def render(vox,name):
    im=Image.new('RGB',(360,310),(48,50,47)); d=ImageDraw.Draw(im)
    if not vox:return im
    # Dwa rzuty z buforem głębokości: front (-Z) i bok (+X).
    for panel,(horiz,depth,front),title in ((0,(0,2,-1),'przód'),(1,(2,0,1),'bok')):
        vis={}
        for p,mat in vox.items():
            key=(p[horiz],p[1]); dep=p[depth]*front
            if key not in vis or dep>vis[key][0]:vis[key]=(dep,mat)
        xx=[q[0] for q in vis]; yy=[q[1] for q in vis]
        scale=min(10,145/max(1,max(xx)-min(xx)+1),235/max(1,max(yy)-min(yy)+1))
        left=panel*180+90-(max(xx)-min(xx)+1)*scale/2
        bottom=280-(235-(max(yy)-min(yy)+1)*scale)/2
        for (x,y),(dep,mat) in vis.items():
            color=MAT[mat]['ramp'][2]
            x0=left+(x-min(xx))*scale;y0=bottom-(y-min(yy)+1)*scale
            d.rectangle((x0,y0,x0+scale-.3,y0+scale-.3),fill=color)
        d.text((panel*180+70,284),title,fill=(170,177,163))
    d.text((12,10),name,fill=(230,231,222))
    return im

def main():
    names=['spirala_skorupy','jajo_oplot','noga_stawowa','plomien','wneka','pierscienie']
    sheet=Image.new('RGB',(1080,620),(32,34,32))
    for i,name in enumerate(names):sheet.paste(render(sample(name),name),((i%3)*360,(i//3)*310))
    OUT.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(OUT)
    print(OUT)

if __name__=='__main__':main()
