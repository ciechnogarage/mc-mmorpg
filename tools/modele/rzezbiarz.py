"""Visual hull z trzech rzutów ortograficznych do modelu Blockbench/ModelEngine.

python3 rzezbiarz.py rechotnik --sheet szkice/rechotnik_orto.png
python3 rzezbiarz.py mulowiec
Współrzędne szkieletu są w tekselach modelu: X w prawo, Y od ziemi, Z do tyłu.
"""
from __future__ import annotations

import argparse
import base64
import colorsys
import io
import json
import math
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = Path(__file__).resolve().parent
ROOT = Path('/opt/docker/mc/data/world1/output/lochy/bagna')
sys.path.insert(0, str(HERE.parent / 'bbrender'))
import bb
import orto_iou


VIEWS = ('przod', 'bok', 'tyl')
FACES = {'north': (1, 0), 'south': (1, 0), 'east': (2, 0),
         'west': (2, 0), 'up': (1, 2), 'down': (1, 2)}


def maska(image: Image.Image, prog=25) -> np.ndarray:
    """Odetnij neutralne tło z gradientem, zamknij dziury 1 px."""
    rgb = np.asarray(image.convert('RGB'), np.int16)
    h, w = rgb.shape[:2]
    # Tło referencji ma pionowy gradient; brzegi widoku są wolne od postaci.
    edge = np.concatenate((rgb[:, :max(2, w//40)], rgb[:, -max(2, w//40):]), axis=1)
    bg = np.median(edge, axis=1)[:, None, :]
    dist = np.sqrt(np.sum((rgb - bg)**2, axis=2))
    saturation = rgb.max(2) - rgb.min(2)
    mask = ((dist > prog) | (saturation > 28)) & (np.asarray(image.convert('RGBA'))[:, :, 3] > 127)
    im = Image.fromarray((mask*255).astype('uint8'))
    return np.asarray(im.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))) > 127


def visual_hull(front: np.ndarray, side: np.ndarray, back: np.ndarray) -> np.ndarray:
    """Tablica (Y, X, Z); rzut tylny jest lustrem przodu."""
    if front.shape != back.shape or front.shape[0] != side.shape[0]:
        raise ValueError('Niezgodne wymiary znormalizowanych widoków')
    return front[:, :, None] & side[:, None, :] & back[:, ::-1, None]


def bez_wysp(vox: np.ndarray, minimum=4) -> np.ndarray:
    seen = np.zeros_like(vox, bool)
    out = vox.copy()
    Y, X, Z = vox.shape
    for y, x, z in np.argwhere(vox):
        y, x, z = int(y), int(x), int(z)
        if seen[y, x, z]:
            continue
        q = deque([(y, x, z)]); seen[y, x, z] = True; part = []
        while q:
            a, b, c = q.popleft(); part.append((a, b, c))
            for da, db, dc in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
                aa, bb, cc = a+da, b+db, c+dc
                if 0 <= aa < Y and 0 <= bb < X and 0 <= cc < Z and vox[aa, bb, cc] and not seen[aa, bb, cc]:
                    seen[aa, bb, cc] = True; q.append((aa, bb, cc))
        if len(part) < minimum:
            for p in part: out[p] = False
    return out


def greedy(vox: np.ndarray) -> list[tuple[tuple[int,int,int], tuple[int,int,int]]]:
    """Dokładny podział zajętości na prostopadłościany, bez nakładania."""
    rest = vox.copy(); boxes = []
    for y, x, z in np.argwhere(vox):
        y, x, z = int(y), int(x), int(z)
        if not rest[y, x, z]: continue
        x1 = x+1
        while x1 < rest.shape[1] and rest[y, x1, z]: x1 += 1
        z1 = z+1
        while z1 < rest.shape[2] and rest[y, x:x1, z1].all(): z1 += 1
        y1 = y+1
        while y1 < rest.shape[0] and rest[y1, x:x1, z:z1].all(): y1 += 1
        rest[y:y1, x:x1, z:z1] = False
        boxes.append(((y,x,z),(y1,x1,z1)))
    return boxes



def scal_z_tolerancja(boxes, tolerancja=1, cel=0):
    """Scal sąsiednie AABB, wypełniając najwyżej `tolerancja` wokseli."""
    boxes = list(boxes)
    def volume(lo, hi): return math.prod(max(0, hi[i]-lo[i]) for i in range(3))
    while len(boxes) > cel:
        best = None
        for i, (al, ah) in enumerate(boxes):
            for j in range(i+1, len(boxes)):
                bl, bh = boxes[j]
                lo = tuple(min(al[k],bl[k]) for k in range(3))
                hi = tuple(max(ah[k],bh[k]) for k in range(3))
                overlap = math.prod(max(0,min(ah[k],bh[k])-max(al[k],bl[k])) for k in range(3))
                extra = volume(lo,hi)-volume(al,ah)-volume(bl,bh)+overlap
                if extra <= tolerancja and (best is None or extra < best[0]):
                    best = (extra,i,j,lo,hi)
        if best is None: break
        _,i,j,lo,hi=best
        boxes.pop(j);boxes.pop(i);boxes.append((lo,hi))
    return boxes


def _segment_distance(p, a, b):
    a = np.asarray(a, float); b = np.asarray(b, float)
    v = b-a
    t = np.clip(np.dot(p-a, v)/max(1e-6, np.dot(v, v)), 0, 1)
    return np.sum((p-(a+t*v))**2)


def przypisz_kosci(vox: np.ndarray, bones: list[dict], center=None) -> np.ndarray:
    """Jawne obszary mają pierwszeństwo; reszta idzie do osi najbliższej kości."""
    Y, X, Z = vox.shape
    center = center or ((X/2), (Z/2))
    labels = np.empty(vox.shape, dtype=object); labels[:] = ''
    by_name = {b['nazwa']: b for b in bones}
    children = {b['nazwa']: [] for b in bones}
    for b in bones:
        if b.get('rodzic') in children: children[b['rodzic']].append(b)
    selectable = [b for b in bones if b['nazwa'] not in ('hitbox','tag_name') and not b['nazwa'].startswith(('p_','fx_','swiatlo_'))]
    segments = []
    for b in selectable:
        starts = [b['pivot']]
        ends = [c['pivot'] for c in children[b['nazwa']] if not c['nazwa'].startswith(('p_','fx_','swiatlo_'))]
        if not ends: ends = [b.get('koniec', b['pivot'])]
        segments.append((b['nazwa'], [(a, e) for a in starts for e in ends]))
    for y, x, z in np.argwhere(vox):
        p = np.array([x+0.5-center[0], y+0.5, z+0.5-center[1]])
        explicit = [b for b in selectable if 'obszar' in b and all(b['obszar'][0][i] <= p[i] < b['obszar'][1][i] for i in range(3))]
        if explicit:
            labels[y,x,z] = explicit[-1]['nazwa']
        else:
            labels[y,x,z] = min(segments, key=lambda s: min(_segment_distance(p,a,e) for a,e in s[1]))[0]
    return labels


def _normalizuj(image: Image.Image, height: int, anchor=None):
    m = maska(image)
    ys, xs = np.where(m)
    if len(xs) == 0: raise ValueError('Pusta sylwetka widoku')
    x0,x1,y0,y1 = xs.min(), xs.max()+1, ys.min(), ys.max()+1
    crop = image.crop((int(x0),int(y0),int(x1),int(y1))).convert('RGB')
    src = m[y0:y1,x0:x1]
    w = max(1, round(crop.width*height/crop.height))
    crop = crop.resize((w,height), Image.Resampling.NEAREST)
    src = np.asarray(Image.fromarray((src*255).astype('uint8')).resize((w,height),Image.Resampling.NEAREST))>127
    # y=0 to stopa, zgodnie z BB.
    return np.asarray(crop)[::-1].copy(), src[::-1].copy()


def _wyrownaj(a, b, mirror_b=True):
    ar, am = a; br, bm = b
    if mirror_b: br, bm = br[:, ::-1], bm[:, ::-1]
    w = max(am.shape[1], bm.shape[1])
    def pad(rgb, mask):
        left = (w-mask.shape[1])//2
        out = np.full((mask.shape[0], w, 3), 128, np.uint8)
        mm = np.zeros((mask.shape[0], w), bool)
        out[:,left:left+mask.shape[1]] = rgb
        mm[:,left:left+mask.shape[1]] = mask
        return out, mm
    return pad(ar,am), pad(br,bm)


def _paleta(images, masks, count=6):
    pixels = np.concatenate([im[m] for im,m in zip(images,masks)])
    if not len(pixels): raise ValueError('Brak pikseli palety')
    sample = pixels[::max(1,len(pixels)//10000)].astype(float)
    # Kolory w przestrzeni RGB po uprzednim wygładzeniu obrazów.
    # Farthest-point seeds preserve small saturated features (turquoise throat/eyes).
    centers = [sample[np.argmax(sample[:,1]+sample[:,2]-2*sample[:,0])]]
    for _ in range(count-1):
        dist=((sample[:,None,:]-np.asarray(centers)[None,:,:])**2).sum(2).min(1)
        centers.append(sample[np.argmax(dist)])
    centers = np.asarray(centers, float)
    for _ in range(20):
        ids = ((sample[:,None,:]-centers[None,:,:])**2).sum(2).argmin(1)
        new = np.array([sample[ids==k].mean(0) if np.any(ids==k) else centers[k] for k in range(count)])
        if np.allclose(new,centers,atol=0.1): break
        centers = new
    # Cztery tony na materiał; maksymalnie 24 barwy atlasu.
    ramps = np.clip(centers[:,None,:] * np.array([.72,.87,1.0,1.13])[None,:,None],0,255).astype('uint8')
    return centers, ramps


def _turkus(rgb):
    rr,gg,bb = [int(v)/255 for v in rgb]
    h,s,_ = colorsys.rgb_to_hsv(rr,gg,bb)
    return 160 <= h*360 <= 185 and s > .45


def _face_tile(face, lo, hi, sources, centers, ramps, offsets):
    """Próbkuj odpowiedni rzut w skali 1 teksel na 1 px ściany."""
    y0,x0,z0=lo; y1,x1,z1=hi
    if face in ('north','south'):
        w,h = x1-x0,y1-y0
        src = sources[0 if face=='north' else 2]
        ix = np.arange(x0,x1) if face=='north' else np.arange(x1-1,x0-1,-1)
        coords = (np.tile(ix,(h,1)),np.tile(np.arange(y1-1,y0-1,-1)[:,None],(1,w)))
    elif face in ('east','west'):
        w,h = z1-z0,y1-y0
        src = sources[1]
        iz = np.arange(z0,z1) if face=='east' else np.arange(z1-1,z0-1,-1)
        coords = (np.tile(iz,(h,1)),np.tile(np.arange(y1-1,y0-1,-1)[:,None],(1,w)))
    else:
        w,h=x1-x0,z1-z0
        src = sources[0]
        ix=np.arange(x0,x1)
        yy = np.full((h,w),min(src.shape[0]-1,max(0,y1-1 if face=='up' else y0)))
        coords=(np.tile(ix,(h,1)),yy)
    xx,yy=coords
    xx=np.clip(xx+offsets[face],0,src.shape[1]-1); yy=np.clip(yy,0,src.shape[0]-1)
    pix=src[yy,xx]
    ids=((pix[:,:,None,:].astype(float)-centers[None,None,:,:])**2).sum(3).argmin(2)
    brightness=pix.mean(2)-centers[ids].mean(2)
    tones=np.clip(2+np.rint(brightness/35).astype(int)+(1 if face=='up' else -1 if face=='down' else 0),0,3)
    encoded=(ids*4+tones).astype('uint8')
    if h>=3 and w>=3:
        encoded=np.asarray(Image.fromarray(encoded).filter(ImageFilter.ModeFilter(3)))
    tile=ramps.reshape(-1,3)[encoded]
    return np.dstack((tile,np.full((h,w),255,np.uint8)))



def widoczne_sciany(boxes):
    """Odrzuć tylko ściany całkowicie schowane za sąsiednimi wokselami."""
    dims=tuple(max(hi[i] for _,hi in boxes) for i in range(3))
    occupied=np.zeros(dims,bool)
    for lo,hi in boxes: occupied[lo[0]:hi[0],lo[1]:hi[1],lo[2]:hi[2]]=True
    result=[]
    for lo,hi in boxes:
        y0,x0,z0=lo;y1,x1,z1=hi
        faces=set(FACES)
        if z0>0 and occupied[y0:y1,x0:x1,z0-1].all(): faces.discard('north')
        if z1<dims[2] and occupied[y0:y1,x0:x1,z1].all(): faces.discard('south')
        if x0>0 and occupied[y0:y1,x0-1,z0:z1].all(): faces.discard('west')
        if x1<dims[1] and occupied[y0:y1,x1,z0:z1].all(): faces.discard('east')
        if y0>0 and occupied[y0-1,x0:x1,z0:z1].all(): faces.discard('down')
        if y1<dims[0] and occupied[y1,x0:x1,z0:z1].all(): faces.discard('up')
        result.append(faces)
    return result


def _atlas(cubes, sources, masks, size=128):
    centers,ramps=_paleta(sources,masks)
    regions=[]
    visible=widoczne_sciany([(lo,hi) for _,lo,hi in cubes])
    for (e,idxlo,idxhi),faces in zip(cubes,visible):
        for face in FACES:
            if face not in faces:
                e['faces'][face]['texture']=None
                continue
            d=np.subtract(idxhi,idxlo)
            axes=FACES[face]
            w,h=max(1,int(d[axes[0]])),max(1,int(d[axes[1]]))
            regions.append((h,w,e,face,idxlo,idxhi))
    regions.sort(key=lambda r:(-r[0],-r[1]))
    atlas=np.zeros((size,size,4),np.uint8)
    x=y=row=0
    identical=possible=0
    offsets={'north':0,'south':0,'east':0,'west':0,'up':0,'down':0}
    for h,w,e,face,lo,hi in regions:
        if x+w>size: x=0;y+=row;row=0
        if y+h>size: raise ValueError(f'Atlas {size}² przepełniony: {len(regions)} ścian')
        tile=_face_tile(face,lo,hi,sources,centers,ramps,offsets)
        col=tile[:,:,:3]
        identical += int(np.all(col[:,:-1]==col[:,1:],2).sum()+np.all(col[:-1]==col[1:],2).sum())
        possible += h*max(0,w-1)+w*max(0,h-1)
        atlas[y:y+h,x:x+w]=tile
        e['faces'][face]['uv']=[x,y,x+w,y+h]
        e['faces'][face]['texture']=0
        x+=w;row=max(row,h)
    used=atlas[:,:,3]>0
    rgb=atlas[:,:,:3]
    neighbors=identical/possible*100 if possible else 0.0
    colors=len(np.unique(rgb[used],axis=0)) if used.any() else 0
    buf=io.BytesIO();Image.fromarray(atlas).save(buf,'PNG')
    uri='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()
    return uri,colors,neighbors


def _sheet(model, concept, crops, dest):
    from PIL import ImageOps
    import render_bbmodel
    bg=(139,139,139)
    art=Image.open(ROOT/'szkice'/f"{model['name'].removeprefix('bagna_')}_k2.png").convert('RGB')
    if model['name']=='bagna_rechotnik': art=art.crop((300,210,950,945))
    elif model['name']=='bagna_mulowiec': art=art.crop((0,130,1090,920))
    art.thumbnail((600,700))
    shot,_=render_bbmodel.renderuj(model,145,15,750)
    shot=ImageOps.mirror(shot)
    bbx=shot.getbbox();shot=shot.crop(bbx) if bbx else shot
    shot.thumbnail((600,700),Image.Resampling.NEAREST)
    page=Image.new('RGB',(1800,1550),bg);draw=ImageDraw.Draw(page)
    page.paste(art,((600-art.width)//2,35))
    model_panel=Image.new('RGBA',(600,740),bg+(255,))
    model_panel.alpha_composite(shot,((600-shot.width)//2,735-shot.height))
    page.paste(model_panel.convert('RGB'),(600,0))
    draw.text((20,5),'KONCEPT K2',fill='white');draw.text((620,5),'MODEL 3/4',fill='white')
    score={}
    for i,(view,key) in enumerate(zip(('front','side','back'),VIEWS)):
        ref=concept[key] if isinstance(concept,dict) else concept.crop(tuple(crops[key]))
        render,mask,_=orto_iou.rasterize(model,view,scale=6)
        value,comparison=orto_iou.compare(ref,render,mask,height=250)
        score[view]=value
        comparison.thumbnail((1760,255),Image.Resampling.NEAREST)
        yy=745+i*265
        draw.text((20,yy),f'{key.upper()}  IoU {value:.4f}',fill='white')
        page.paste(comparison,((1800-comparison.width)//2,yy+20))
    page.save(dest)
    return score


def zbuduj(mob: str, images: dict[str,Image.Image], skeleton: dict, output: Path, concept=None, crops=None):
    h=round(float(skeleton['wys_bl'])*16)
    normalized={key:_normalizuj(images[key],h) for key in VIEWS}
    (front,fm),(back,bm)=_wyrownaj(normalized['przod'],normalized['tyl'],False)
    side,sm=normalized['bok']
    hull=bez_wysp(visual_hull(fm,sm,bm))
    if not hull.any(): raise ValueError('Visual hull pusty')
    bones=skeleton['kosci']
    xcenter=hull.shape[1]/2; zcenter=hull.shape[2]/2
    labels=przypisz_kosci(hull,bones,(xcenter,zcenter))
    # Światło jest oddzielną kością; określ je z rzutów, nie z koloru po kwantyzacji.
    for y,x,z in np.argwhere(hull):
        col=front[y,x] if fm[y,x] else side[y,z]
        if _turkus(col):
            light='swiatlo_oczy' if y>h*.70 and any(b['nazwa']=='swiatlo_oczy' for b in bones) else ('swiatlo_worek' if mob=='rechotnik' else 'swiatlo_rana')
            if any(b['nazwa']==light for b in bones): labels[y,x,z]=light
    bb.wyczysc()
    cubes=[]; links={b['nazwa']:[] for b in bones}
    for bone in bones:
        name=bone['nazwa']; portion=hull & (labels==name)
        for n,(ilo,ihi) in enumerate(scal_z_tolerancja(greedy(portion),int(skeleton.get('tolerancja_scalania',2)))):
            y0,x0,z0=ilo;y1,x1,z1=ihi
            lo=[x0-xcenter,y0,z0-zcenter];hi=[x1-xcenter,y1,z1-zcenter]
            uid=bb.szescian(f'{name}_{n:03}',lo,hi,3)
            e=bb.ELEMENTY[-1]
            links[name].append(uid)
            cubes.append((e,ilo,ihi))
    # Techniczny hitbox nie bierze udziału w sylwetce ani atlasie.
    hb=skeleton.get('hitbox',[16,h])
    d=float(hb[0])/2
    hu=bb.szescian('hitbox_box',[-d,0,-d],[d,float(hb[1]),d],1)
    for face in bb.ELEMENTY[-1]['faces'].values(): face['texture']=None
    links['hitbox'].append(hu)
    nodes={}
    for bone in bones:
        name=bone['nazwa'];nodes[name]=bb.kosc(name,bone['pivot'],links[name])
    for bone in bones:
        parent=bone.get('rodzic')
        if parent: nodes[parent]['children'].append(nodes[bone['nazwa']])
    roots=[nodes[b['nazwa']] for b in bones if not b.get('rodzic')]
    bb.waliduj(set())
    src,colors,neighbors=_atlas(cubes,[front,side,back],[fm,sm,bm],int(skeleton.get('atlas',128)))
    output.parent.mkdir(parents=True,exist_ok=True)
    bb.zapisz(output,f'bagna_{mob}',roots,src,int(skeleton.get('atlas',128)),[])
    scores={}
    if concept is not None:
        scores=_sheet(json.loads(output.read_text()),concept,crops,output.parent/'podglad.png')
    return {'elementy':len(bb.ELEMENTY),'kosci':len(bones),'barwy':colors,'sasiedzi':neighbors,'iou':scores}


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('id');ap.add_argument('--sheet',type=Path);ap.add_argument('--output',type=Path)
    args=ap.parse_args()
    sk=json.loads((ROOT/'orto'/f'{args.id}_szkielet.json').read_text())
    if args.sheet:
        src=Image.open(args.sheet)
        crops=sk['kadry']
        images={v:src.crop(tuple(crops[v])) for v in VIEWS}
    else:
        crops=None
        images={v:Image.open(ROOT/'orto'/f'{args.id}_{v}.png') for v in VIEWS}
        src=images
    out=args.output or ROOT/'modele'/args.id/f'bagna_{args.id}.bbmodel'
    stats=zbuduj(args.id,images,sk,out,src,crops)
    print(json.dumps(stats,ensure_ascii=False,indent=2))
    if stats['elementy']<80 or stats['elementy']>250 or stats['barwy']>=40 or any(stats['iou'].get(k,0)<.90 for k in ('front','side')):
        print('BRAMKI SPECYFIKACJI NIEOSIĄGNIĘTE',file=sys.stderr)
        return 1
    return 0


if __name__=='__main__': raise SystemExit(main())
