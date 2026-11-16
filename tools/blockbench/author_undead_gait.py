"""Author a new short-stride, low-lift undead gait; separate candidate only."""
import argparse
import json
import math
from pathlib import Path
from review_session import ReviewSession,literal


def tracks_for(model):
    groups={g['name']:g for g in model['groups']}
    tracks={}
    def add(bone,channel,t,v):tracks.setdefault(bone,{}).setdefault(channel,[]).append([t,v])
    length=.75
    for frame in range(31):
        t=frame/40;phase=t/length
        bob=-.85-.10*math.cos(4*math.pi*phase)
        add('root','position',t,[0,bob,0])
        pitch=-8.5-1.5*math.cos(4*math.pi*phase)
        add('torso','rotation',t,[pitch,2.5*math.sin(2*math.pi*(phase-.13)),2.0+1.8*math.sin(2*math.pi*phase)])
        add('hi_head','rotation',t,[-pitch*.72,-1.2*math.sin(2*math.pi*(phase-.2)),-1.5])
        for side,offset in [('right',0),('left',.5)]:
            hip=groups[side+'_leg'];shin=groups[side+'_shin'];foot=groups[side+'_foot']
            if any(any(g.get('rotation',[0,0,0])) for g in (hip,shin,foot)):
                raise ValueError('Gait solve requires unrotated leg rest transforms')
            upper=hip['origin'][1]-shin['origin'][1];lower=shin['origin'][1]-foot['origin'][1]
            if upper<=0 or lower<=0:raise ValueError('Unexpected leg rig')
            p=(phase+offset)%1
            if p<.5:z=-2.25+9*p;lift=0
            else:
                u=2*(p-.5);z=2.25-4.5*(u*u*(3-2*u));lift=(.85 if side=='right' else .35)*math.sin(math.pi*u)**2
            down=upper+lower+bob-lift;r=math.hypot(down,z)
            clamp=lambda x:max(-1,min(1,x))
            a=math.degrees(math.atan2(-z,down)+math.acos(clamp((upper*upper+r*r-lower*lower)/(2*upper*r))))
            b=-math.degrees(math.acos(clamp((r*r-upper*upper-lower*lower)/(2*upper*lower))))
            for part,angle in [('leg',a),('shin',b),('foot',-a-b)]:add(side+'_'+part,'rotation',t,[angle,0,0])
        lag=math.sin(2*math.pi*(phase-.17))
        add('right_arm','rotation',t,[-5+4*lag,2,-7])
        add('right_forearm','rotation',t,[5+2*math.sin(2*math.pi*(phase-.24)),0,0])
        add('left_arm','rotation',t,[8-7*math.sin(2*math.pi*(phase-.08)),-3,11])
        add('left_forearm','rotation',t,[22+3*math.sin(2*math.pi*(phase-.16)),0,0])
    return length,tracks


def main():
    p=argparse.ArgumentParser();p.add_argument('--source',required=True,type=Path);p.add_argument('--out',required=True,type=Path);args=p.parse_args()
    s=ReviewSession();manifest=s.bind(args.source);before=s.export();length,tracks=tracks_for(before)
    args.out.mkdir(exist_ok=False);(args.out/'before.bbmodel').write_text(json.dumps(before,indent=2));state=s.snapshot()
    code='const data='+literal(tracks)+';const a=Animation.all.find(a=>a.name==="walk"); if(!a)throw new Error("Missing walk");Undo.initEdit({animations:[a]});try{Timeline.pause();for(const item of AnimationItem.all)item.playing=false;for(const id of Object.keys(a.animators))a.removeAnimator(id);a.setLength('+str(length)+');for(const [name,channels] of Object.entries(data)){const g=Group.all.find(g=>g.name===name);if(!g)throw new Error("Missing bone "+name);const tr=a.getBoneAnimator(g);for(const [channel,frames] of Object.entries(channels))for(const [time,values] of frames)tr.addKeyframe({channel,time,interpolation:"linear",data_points:[{x:values[0],y:values[1],z:values[2]}]});}Undo.finishEdit("Author distinct undead short-stride gait");Canvas.updateAll();Project.name="ROBOCZY - NOWY CHOD nieumarlego 01, twarz B";}catch(e){Undo.cancelEdit();throw e;}JSON.stringify({changed:"walk"})'
    try:
        print(s.js(code));after=s.export()
        for key in ('elements','groups','outliner','textures'):
            if before[key]!=after[key]:raise RuntimeError('Unexpected appearance change')
        (args.out/'race_nieumarly.bbmodel').write_text(json.dumps(after,indent=2))
        (args.out/'manifest.json').write_text(json.dumps(dict(manifest,status='GAIT_PROTOTYPE_NOT_ACCEPTED',changes=['walk'],length=length,notes=['New leg trajectories, short steps, asymmetric lift, forward trunk and independent head counterpose','Runtime speed matching and equipment not verified']),indent=2))
    finally:s.restore(state)
    print(args.out/'race_nieumarly.bbmodel')
if __name__=='__main__':main()
