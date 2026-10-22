"""Arkusz odbiorczy: koncept, dwie kamery 3/4, oczy gracza z 4 bloków."""
import base64
import sys
import time
from pathlib import Path
from PIL import Image, ImageDraw

import bb_sesja as B

src=Path(sys.argv[1]); koncept=Path(sys.argv[2]); dst=Path(sys.argv[3])
dst.parent.mkdir(parents=True,exist_ok=True)
u=B.otworz(src)
try:
    time.sleep(2)  # tekstura atlasu po przełączeniu projektu Blockbench
    def wybierz():
        current=B.js('ModelProject.all.find(p=>p.uuid===' + B.literal(u) + ').select(); JSON.stringify(Project.uuid)')
        if current!=u:raise RuntimeError(f'karta przejęta: {current} != {u}')
    wybierz()
    B.ukryj_techniczne()
    info=B.js('Modes.options.edit.select(); const b=new THREE.Box3(); Outliner.elements.forEach(e=>{if(e.mesh&&e.visibility!==false)b.expandByObject(e.mesh)});'
              'const c=b.getCenter(new THREE.Vector3()),z=b.getSize(new THREE.Vector3()); JSON.stringify({c:[c.x,c.y,c.z],z:[z.x,z.y,z.z]})')
    c,z=info['c'],info['z']; d=max(z)*1.35+8
    cameras=[
        ([c[0]-d*.55,c[1]+d*.35,c[2]-d*.85],c),
        ([c[0]+d*.55,c[1]+d*.35,c[2]+d*.85],c),
        ([-20,26,-58],[0,c[1],0]),
    ]
    imgs=[]
    for pos,target in cameras:
        for attempt in range(8):
            wybierz()
            B.sesja().tool('set_camera_angle',{'position':pos,'target':target,'projection':'perspective'})
            if B.js('JSON.stringify(Project.uuid)') != u:
                continue
            res=B.sesja().tool('capture_screenshot',{})
            if B.js('JSON.stringify(Project.uuid)') == u:
                break
        else:
            raise RuntimeError('nie udało się uchwycić stabilnej karty Blockbench')
        import io
        img=Image.open(io.BytesIO(base64.b64decode(next(x['data'] for x in res['content'] if x['type']=='image')))).convert('RGBA')
        imgs.append(img)
finally:
    B.zamknij(u)

W,H=420,480
sheet=Image.new('RGB',(4*W,H),(112,112,112)); draw=ImageDraw.Draw(sheet)
sources=[Image.open(koncept).convert('RGBA'),*imgs]
labels=['KONCEPT','PRZÓD 3/4','TYŁ 3/4','OCZY GRACZA · 4 BL.']
for i,img in enumerate(sources):
    if i:
        alpha=img.getchannel('A'); box=alpha.point(lambda a:255 if a>20 else 0).getbbox()
        if box:img=img.crop(box)
    img.thumbnail((W-20,H-58))
    x=i*W+(W-img.width)//2; y=38+(H-40-img.height)//2
    sheet.paste(img,(x,y),img if i else None)
    draw.text((i*W+12,10),labels[i],fill=(245,244,231))
    if i:draw.line((i*W,0,i*W,H),fill=(53,57,54),width=2)
sheet.save(dst)
print(dst)
