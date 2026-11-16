"""Wspólne, spokojne materiały i eksport modeli SDF Bagien."""
from __future__ import annotations

import json
from pathlib import Path
import uuid

from bryly_sdf import Model

ROOT = Path('/opt/docker/mc/data/world1/output/lochy/bagna')

MAT = {
    'mul': [(41,34,25),(70,53,37),(95,72,49),(121,96,66)],
    'mech': [(34,39,22),(55,65,31),(78,90,43),(105,117,59)],
    'skora': [(36,41,31),(57,65,43),(79,87,56),(104,113,74)],
    'tkanina': [(28,39,40),(43,60,62),(63,80,82),(87,105,105)],
    'kosc': [(65,63,49),(113,109,83),(158,151,111),(203,191,146)],
    'korzen': [(30,28,22),(51,45,32),(77,66,43),(108,89,57)],
    'metal': [(44,42,37),(71,70,62),(101,101,89),(142,141,122)],
    'przedza': [(60,57,45),(105,102,79),(153,148,113),(193,184,143)],
    'sluz': [(40,55,44),(64,80,54),(90,110,73),(133,152,104)],
    'cien': [(13,20,19),(21,30,27),(31,42,36),(47,59,47)],
    'turkus': [(7,42,43),(10,71,70),(20,101,96),(47,135,122)],
    'turkus_jasny': [(12,65,64),(25,124,117),(63,199,178),(159,240,220)],
}

def materialy(*names):
    return {n:{'ramp':MAT[n], 'plamy': .08 if n in ('mul','mech','skora','tkanina','korzen') else 0,
               'jasna_krawedz': n in ('sluz','metal')} for n in names}

def nowy(*names, atlas=128, budzet=250):
    m=Model(materialy(*names),budzet=budzet,atlas=atlas)
    m.czesc('korzen',(0,0,0))
    return m

def pasma(m, bone, prefix, positions, mat='mech'):
    for i,(x,y,z,length) in enumerate(positions):
        m.detal(bone,f'{prefix}_{i}',(x,y-length,z),(x+1,y,z+1),mat)

def oko(m,bone,name,x,y,z,scale=1):
    m.detal(bone,f'{name}_cien',(x,y,z),(x+2*scale,y+2*scale,z+1),'cien')
    m.detal(bone,f'{name}_turkus',(x,y+scale,z-1),(x+2*scale,y+2*scale,z),'turkus')
    m.detal(bone,f'{name}_blik',(x+scale,y+scale,z-2),(x+scale+1,y+scale+1,z-1),'turkus_jasny')

def puste(m, names):
    for name,pivot,parent in names:
        m.czesc(name,pivot,parent)

def _idle(path, bone, length=2.0, movement=(0,.35,0)):
    data=json.loads(Path(path).read_text())
    def find(nodes):
        for n in nodes:
            if isinstance(n,dict):
                if n.get('name')==bone:return n['uuid']
                got=find(n.get('children',[]))
                if got:return got
    uid=find(data['outliner'])
    if not uid:raise ValueError(f'brak kości oddechu {bone}')
    def key(t,v):
        return {'channel':'position','data_points':[{'x':str(v[0]),'y':str(v[1]),'z':str(v[2])}],
                'uuid':str(uuid.uuid4()),'time':t,'color':-1,'interpolation':'catmullrom'}
    data['animations']=[{'uuid':str(uuid.uuid4()),'name':'idle','loop':'loop','override':False,
                         'length':length,'snapping':20,'selected':False,'anim_time_update':'',
                         'blend_weight':'','start_delay':'','loop_delay':'',
                         'animators':{uid:{'name':bone,'type':'bone','keyframes':[
                             key(0,(0,0,0)),key(length/2,movement),key(length,(0,0,0))]}}}]
    Path(path).write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')))

def zapisz(m, id, hitbox, tag_y, idle_bone, idle_length=2.0):
    dest=ROOT/'modele'/id
    dest.mkdir(parents=True,exist_ok=True)
    path=dest/f'bagna_{id}.bbmodel'
    report=m.zapisz(path,f'bagna_{id}',hitbox=hitbox,tag_y=tag_y)
    _idle(path,idle_bone,idle_length)
    (dest/'raport_sdf.json').write_text(json.dumps(report,indent=2,ensure_ascii=False))
    return report
