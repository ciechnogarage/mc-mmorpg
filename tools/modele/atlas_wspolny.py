"""Wspólny UV dla równych ścian tego samego materiału; oszczędza atlas dużych mobów."""
import base64
import io
import math
import numpy as np
from PIL import Image
import tekstura


def atlas(elementy, materialy, wybierz, rozmiar=256, ziarno=7, naklejki=None, malarz=None, wspoldzielone=False):
    rng=np.random.default_rng(ziarno)
    tiles={}; refs={}
    for e in elementy:
        matid=wybierz(e['name']);mat=materialy[matid]
        d=[e['to'][i]-e['from'][i] for i in range(3)]
        for face in e['faces']:
            ax,ay=tekstura.SCIANY_WYM[face]
            w,h=max(1,math.ceil(d[ax]-1e-6)),max(1,math.ceil(d[ay]-1e-6))
            key=(matid,face,w,h)
            refs.setdefault(key,[]).append((e,face))
            if key not in tiles:
                tiles[key]=(malarz or tekstura.maluj)(face,w,h,mat,rng)
    order=sorted(tiles,key=lambda k:(-k[3],-k[2],k[0],k[1]))
    img=np.zeros((rozmiar,rozmiar,4),np.uint8)
    x=y=row=0
    for key in order:
        tile=tiles[key];h,w=tile.shape[:2]
        if x+w>rozmiar:x,y,row=0,y+row,0
        if y+h>rozmiar:raise ValueError(f'atlas współdzielony przepełniony: {len(order)} kafli, {key}')
        img[y:y+h,x:x+w]=tile
        for e,face in refs[key]:
            e['faces'][face]['uv']=[x,y,x+w,y+h]
            e['faces'][face]['texture']=0
        x+=w;row=max(row,h)
    data=io.BytesIO();Image.fromarray(img).save(data,'PNG')
    return 'data:image/png;base64,'+base64.b64encode(data.getvalue()).decode(),(y+row)/rozmiar
