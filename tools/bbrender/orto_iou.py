"""Orthographic Blockbench rasterizer and concept silhouette comparison.

Example: python3 orto_iou.py mob.bbmodel ortho.png --view front --panel 0 --sheet front.png
Panels in an ortho sheet are equal-width front/side/back columns. Camera axes:
front looks along +Z (from -Z), side from +X, back from +Z. Pixels per model texel
are controlled with --scale; comparison normalizes both masks by standing height
and their foot/bounding-box baseline. Technical/effect bones are skipped.
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

HIDDEN = ('hitbox', 'shadow', 'fx_', '_fx', 'slash', 'telegraph', 'splash')
VIEWS = {'front': lambda p: np.array([p[0], p[1], -p[2]]),
         'side': lambda p: np.array([p[2], p[1], p[0]]),
         'back': lambda p: np.array([-p[0], p[1], p[2]])}
FACES = {
    'north': ((1, 1, 0), (0, 1, 0), (1, 0, 0)),
    'south': ((0, 1, 1), (1, 1, 1), (0, 0, 1)),
    'east': ((1, 1, 1), (1, 1, 0), (1, 0, 1)),
    'west': ((0, 1, 0), (0, 1, 1), (0, 0, 0)),
    'up': ((0, 1, 0), (1, 1, 0), (0, 1, 1)),
    'down': ((0, 0, 1), (1, 0, 1), (0, 0, 0)),
}


def rotation(deg):
    x, y, z = np.radians(deg)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    return np.array([[cz*cy, cz*sy*sx-sz*cx, cz*sy*cx+sz*sx],
                     [sz*cy, sz*sy*sx+cz*cx, sz*sy*cx-cz*sx],
                     [-sy, cy*sx, cy*cx]])


def _rot(obj):
    r = obj.get('rotation') or [0, 0, 0]
    if isinstance(r, dict):
        v = [0, 0, 0]
        v['xyz'.index(r.get('axis', 'y'))] = r.get('angle', 0)
        return v
    return r


def _animation(model, name, time):
    if not name:
        return {}
    anim = next((a for a in model.get('animations', []) if a.get('name') == name), None)
    if anim is None:
        raise ValueError(f'Animation {name!r} not found')
    result = {}
    for uuid, data in anim.get('animators', {}).items():
        channels = {}
        for channel in ('rotation', 'position', 'scale'):
            frames = sorted((k for k in data.get('keyframes', []) if k.get('channel') == channel), key=lambda k: float(k['time']))
            if not frames:
                continue
            def vec(k):
                dp = (k.get('data_points') or [{}])[0]
                default = 1 if channel == 'scale' else 0
                return np.array([float(dp.get(c, default)) for c in 'xyz'])
            if time <= float(frames[0]['time']):
                value = vec(frames[0])
            elif time >= float(frames[-1]['time']):
                value = vec(frames[-1])
            else:
                a, b = next((a, b) for a, b in zip(frames, frames[1:]) if float(a['time']) <= time <= float(b['time']))
                t = (time-float(a['time']))/(float(b['time'])-float(a['time']))
                value = vec(a)*(1-t)+vec(b)*t
            channels[channel] = value
        result[uuid] = channels
    return result


def _texture(model):
    imgs = []
    resolution = model.get('resolution', {'width': 16, 'height': 16})
    for tex in model.get('textures', []):
        src = tex.get('source', '')
        img = np.asarray(Image.open(io.BytesIO(base64.b64decode(src.split(',', 1)[1]))).convert('RGBA')) if src.startswith('data:image') else None
        imgs.append((img, tex.get('uv_width') or resolution['width'], tex.get('uv_height') or resolution['height']))
    return imgs


def _objects(model, anim_name=None, time=0):
    elements = {e['uuid']: e for e in model.get('elements', [])}
    groups = {g['uuid']: g for g in model.get('groups', [])} if isinstance(model.get('groups'), list) else {}
    poses = _animation(model, anim_name, time)
    result = []
    def visit(item, chain):
        if isinstance(item, str) and item in elements:
            e = elements[item]
            pivot = np.array(e.get('origin', [0, 0, 0]), float)
            result.append((e, [(rotation(_rot(e)), pivot, np.zeros(3), np.ones(3))] + chain))
            return
        g = dict(groups.get(item, {})) if isinstance(item, str) else item
        if not g or any(x in g.get('name', '').lower() for x in HIDDEN):
            return
        pose = poses.get(g.get('uuid'), {})
        scale = pose.get('scale', np.ones(3))
        if np.max(np.abs(scale)) < .001:
            return
        pivot = np.array(g.get('origin', [0, 0, 0]), float)
        rot = np.array(_rot(g), float) + pose.get('rotation', np.zeros(3))
        offset = pose.get('position', np.zeros(3))
        for child in g.get('children', []):
            visit(child, [(rotation(rot), pivot, offset, scale)] + chain)
    for item in model.get('outliner', list(elements)):
        visit(item, [])
    return result


def _transform(p, chain):
    for matrix, pivot, offset, scale in chain:
        p = ((p-pivot)*scale) @ matrix.T + pivot + offset
    return p


def _triangles(model, view, animation=None, time=0):
    camera = VIEWS[view]
    result = []
    for e, chain in _objects(model, animation, time):
        if e.get('type', 'cube') != 'cube' or 'from' not in e:
            continue
        inflate = float(e.get('inflate', 0))
        lo, hi = np.array(e['from'], float)-inflate, np.array(e['to'], float)+inflate
        for face_name, face in (e.get('faces') or {}).items():
            if face_name not in FACES or face.get('texture') is None or 'uv' not in face:
                continue
            corners = []
            for flags in FACES[face_name]:
                p = np.where(flags, hi, lo)
                corners.append(camera(_transform(p, chain)))
            tl, tr, bl = corners
            br = tr+bl-tl
            u0, v0, u1, v1 = face['uv']
            uv = np.array([[u0,v0],[u1,v0],[u0,v1],[u1,v1]], float)
            turns = (int(face.get('rotation', 0))//90)%4
            if turns:
                uv = uv[[[1,3,0,2],[3,2,1,0],[2,0,3,1]][turns-1]]
            pts = np.array([tl,tr,bl,br])
            for inds in ((0,1,2),(2,1,3)):
                result.append((pts[list(inds)], uv[list(inds)], int(face['texture'])))
    return result


def rasterize(model, view='front', scale=4, animation=None, time=0, background=(139,139,139)):
    """Return RGBA raster, mask and model bounds at fixed pixels/model texel."""
    tris = _triangles(model, view, animation, time)
    if not tris:
        raise ValueError('No visible textured cubes')
    allpts = np.concatenate([t[0] for t in tris])
    xymin = np.floor(allpts[:,:2].min(0)*scale).astype(int)-8
    xymax = np.ceil(allpts[:,:2].max(0)*scale).astype(int)+8
    width, height = int(xymax[0]-xymin[0]+1), int(xymax[1]-xymin[1]+1)
    rgb = np.empty((height,width,4), np.uint8)
    rgb[:,:,:3] = background
    rgb[:,:,3] = 255
    mask = np.zeros((height,width), bool)
    depth = np.full((height,width), -np.inf)
    tex = _texture(model)
    for points, uv, tid in tris:
        if tid >= len(tex) or tex[tid][0] is None:
            continue
        img, uw, uh = tex[tid]
        x = points[:,0]*scale-xymin[0]
        y = xymax[1]-points[:,1]*scale
        x0,x1 = max(0,int(np.floor(x.min()))), min(width-1,int(np.ceil(x.max())))
        y0,y1 = max(0,int(np.floor(y.min()))), min(height-1,int(np.ceil(y.max())))
        if x1<x0 or y1<y0:
            continue
        X,Y = np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
        den = (y[1]-y[2])*(x[0]-x[2])+(x[2]-x[1])*(y[0]-y[2])
        if abs(den)<1e-8:
            continue
        a = ((y[1]-y[2])*(X-x[2])+(x[2]-x[1])*(Y-y[2]))/den
        b = ((y[2]-y[0])*(X-x[2])+(x[0]-x[2])*(Y-y[2]))/den
        c = 1-a-b
        z = a*points[0,2]+b*points[1,2]+c*points[2,2]
        U = a*uv[0,0]+b*uv[1,0]+c*uv[2,0]
        V = a*uv[0,1]+b*uv[1,1]+c*uv[2,1]
        tx = np.clip((U*img.shape[1]/uw).astype(int),0,img.shape[1]-1)
        ty = np.clip((V*img.shape[0]/uh).astype(int),0,img.shape[0]-1)
        color = img[ty,tx]
        subdepth = depth[y0:y1+1,x0:x1+1]
        take = (a>=-1e-5)&(b>=-1e-5)&(c>=-1e-5)&(z>subdepth)&(color[:,:,3]>127)
        subdepth[take] = z[take]
        rgb[y0:y1+1,x0:x1+1][take] = color[take]
        mask[y0:y1+1,x0:x1+1][take] = True
    return Image.fromarray(rgb), mask, (xymin,xymax)


def concept_mask(image, threshold=28, background=None):
    rgba = np.asarray(image.convert('RGBA'))
    rgb = rgba[:,:,:3].astype(float)
    if background is None:
        h,w = rgb.shape[:2]
        patch = np.concatenate([rgb[:max(2,h//20),:max(2,w//20)].reshape(-1,3),
                                rgb[:max(2,h//20),-max(2,w//20):].reshape(-1,3),
                                rgb[-max(2,h//20):,:max(2,w//20)].reshape(-1,3),
                                rgb[-max(2,h//20):,-max(2,w//20):].reshape(-1,3)])
        background = np.median(patch,axis=0)
    else:
        background = np.array(background,float)
    delta = np.linalg.norm(rgb-background,axis=2)
    mask = (delta>threshold)&(rgba[:,:,3]>127)
    return mask, tuple(int(x) for x in background)


def _bounds(mask):
    ys,xs = np.where(mask)
    if len(xs)==0:
        raise ValueError('Empty silhouette mask: check panel/background/threshold')
    return xs.min(),ys.min(),xs.max()+1,ys.max()+1


def compare(concept, model_image, model_mask, threshold=28, height=512, background=None):
    """Normalize standing heights and bottom baselines; return score and three-panel sheet."""
    cm, bg = concept_mask(concept,threshold,background)
    cx0,cy0,cx1,cy1 = _bounds(cm)
    mx0,my0,mx1,my1 = _bounds(model_mask)
    # Resample entire crops, preserve proportions, align horizontal bbox centers and feet.
    def crop(image, mask, bounds):
        x0,y0,x1,y1 = bounds
        factor = height/(y1-y0)
        nw = max(1,round((x1-x0)*factor))
        tile = image.crop(bounds).resize((nw,height),Image.Resampling.NEAREST)
        m = Image.fromarray((mask[y0:y1,x0:x1]*255).astype('uint8')).resize((nw,height),Image.Resampling.NEAREST)
        return tile,np.asarray(m)>127
    ci,cm = crop(concept,cm,(cx0,cy0,cx1,cy1))
    mi,mm = crop(model_image,model_mask,(mx0,my0,mx1,my1))
    def feet_center(mask):
        # Anchor at the feet under the torso, excluding a shovel/weapon or knuckle
        # at the outer edges of the bottom band. The bottom of the crop is ground.
        upper = mask[height//5:height*3//5]
        ux = np.where(upper)[1]
        torso = float(np.median(ux)) if len(ux) else mask.shape[1]/2
        lower = mask[height*4//5:height-2]
        lx = np.where(lower)[1]
        lx = lx[(lx>torso-height/4)&(lx<torso+height/4)]
        return float((lx.min()+lx.max())/2) if len(lx) else torso
    cf, mf = feet_center(cm), feet_center(mm)
    anchor = max(cf,mf)+16
    cx,mx = round(anchor-cf),round(anchor-mf)
    width = max(cx+ci.width,mx+mi.width)+16
    a,b = np.zeros((height,width),bool),np.zeros((height,width),bool)
    a[:,cx:cx+ci.width]=cm; b[:,mx:mx+mi.width]=mm
    union = (a|b).sum()
    iou = float((a&b).sum()/union) if union else 1.0
    bg = (139,139,139)
    sheet = Image.new('RGB',(width*3,height+28),bg)
    sheet.paste(ci.convert('RGB'),(cx,28)); sheet.paste(mi.convert('RGB'),(width+mx,28))
    diff = np.full((height,width,3),bg,np.uint8)
    diff[a&b]=(70,132,78); diff[a&~b]=(228,84,75); diff[b&~a]=(67,144,223)
    sheet.paste(Image.fromarray(diff),(width*2,28))
    draw = ImageDraw.Draw(sheet)
    for i,label in enumerate(('KONCEPT','MODEL',f'RÓŻNICA  IoU={iou:.3f}')):
        draw.text((i*width+8,8),label,fill='white')
    return iou,sheet


def panel(image, index, count=3, inset=0):
    w,h=image.size
    return image.crop((round(w*index/count)+inset,0,round(w*(index+1)/count)-inset,h))


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('model',type=Path); ap.add_argument('concept',type=Path,nargs='?')
    ap.add_argument('--view',choices=VIEWS,default='front'); ap.add_argument('--panel',type=int)
    ap.add_argument('--crop',nargs=4,type=int,metavar=('X0','Y0','X1','Y1'),help='Exact concept view rectangle; overrides --panel')
    ap.add_argument('--panels',type=int,default=3); ap.add_argument('--inset',type=int,default=0)
    ap.add_argument('--scale',type=float,default=4); ap.add_argument('--height',type=int,default=512)
    ap.add_argument('--threshold',type=float,default=28); ap.add_argument('--background',nargs=3,type=int)
    ap.add_argument('--animation'); ap.add_argument('--time',type=float,default=0)
    ap.add_argument('--sheet',type=Path); ap.add_argument('--render',type=Path)
    args=ap.parse_args()
    model=json.loads(args.model.read_text())
    render,mask,_=rasterize(model,args.view,args.scale,args.animation,args.time)
    if args.render:
        args.render.parent.mkdir(parents=True,exist_ok=True); render.save(args.render)
    if args.concept:
        concept=Image.open(args.concept)
        if args.crop is not None:
            concept=concept.crop(tuple(args.crop))
        elif args.panel is not None:
            concept=panel(concept,args.panel,args.panels,args.inset)
        score,sheet=compare(concept,render,mask,args.threshold,args.height,args.background)
        print(f'{args.view} IoU={score:.4f}')
        if args.sheet:
            args.sheet.parent.mkdir(parents=True,exist_ok=True);sheet.save(args.sheet)
    else:
        print(f'{args.view}: {render.width}x{render.height}, {mask.sum()} silhouette pixels')


if __name__=='__main__':
    main()
