"""Author optional player-state pose studies through the active Blockbench API.

No runtime mapping, held-item alignment or feeling approval is implied. The
source file is immutable; current editor receives five new clips. No new tab.
"""
import argparse
import copy
import hashlib
import json
import math
from pathlib import Path
import sys

from project_identity import canonical
from review_session import ReviewSession, literal

CREATED = ('run', 'crouch_idle', 'bow_draw', 'bow_hold', 'block')
DEFERRED = {
    'crouch_walk': 'Needs moving crouch foot-contact solver and world-speed validation; no dummy clip authored.',
    'swim_idle': 'Needs verified editor-to-runtime axes, waterline/body origin and head look; no dummy clip authored.',
    'swim': 'Needs swim orientation and locomotion contract; no dummy clip authored.',
}


def retime_clip(source, name, length):
    original_length = float(source['length'])
    if not all(math.isfinite(v) and v > 0 for v in (original_length, length)):
        raise ValueError('Positive finite durations required')
    result = copy.deepcopy(source)
    for field in ('uuid', 'selected', 'playing', 'saved', 'saved_name', 'path'):
        result.pop(field, None)
    result.update(name=name, length=length)
    for track in result['animators'].values():
        for key in track.get('keyframes', []):
            key.pop('uuid', None)
            key['time'] = round(float(key['time']) * length / original_length, 9)
    for marker in result.get('markers', []):
        if 'time' in marker:
            marker['time'] = round(float(marker['time']) * length / original_length, 9)
    return result


def verify_only_new_clips(before, after):
    remaining = copy.deepcopy(after)
    added = [a for a in remaining['animations'] if a['name'] in CREATED]
    if sorted(a['name'] for a in added) != sorted(CREATED):
        raise ValueError('Expected exactly five new named clips')
    remaining['animations'] = [a for a in remaining['animations'] if a['name'] not in CREATED]
    encode = lambda value: json.dumps(canonical(value), sort_keys=True, allow_nan=False)
    if encode(before) != encode(remaining):
        raise ValueError('Unexpected change outside the five new clips')


def author_code(run_data):
    return 'const runData=' + literal(run_data) + ';const wanted=' + literal(CREATED) + ''';
if(wanted.some(name=>Animation.all.some(a=>a.name===name)))throw new Error("State clip already exists");
const needed=["root","torso","right_leg","right_shin","right_foot","left_leg","left_shin","left_foot","right_arm","left_arm","right_forearm","left_forearm"];
for(const name of needed)if(Group.all.filter(g=>g.name===name).length!==1)throw new Error("Missing or ambiguous bone: "+name);
const made=[];let crouchProbe=null;
Undo.initEdit({animations:made});
try{
 function isolated(a){Timeline.pause();for(const item of AnimationItem.all)item.playing=false;Modes.options.animate.select();a.select();for(const item of AnimationItem.all)item.playing=false;a.playing=true;}
 function make(name,length,loop){const a=new Animation({name,length,loop,override:false}).add();made.push(a);return a;}
 function rotation(a,name,frames){const tr=a.getBoneAnimator(Group.all.find(g=>g.name===name));for(const [time,x,y,z] of frames)tr.addKeyframe({channel:"rotation",time,interpolation:"linear",data_points:[{x,y,z}]});}
 function constant(a,name,x,y=0,z=0){rotation(a,name,[[0,x,y,z],[a.length,x,y,z]]);}
 const run=new Animation(runData).add();made.push(run);
 const crouch=make("crouch_idle",1,"loop");
 for(const side of ["right","left"]){constant(crouch,side+"_leg",22);constant(crouch,side+"_shin",-44);constant(crouch,side+"_foot",22);}
 constant(crouch,"torso",12);
 constant(crouch,"right_arm",1.5,0,-3);constant(crouch,"left_arm",-.5,0,3);
 constant(crouch,"right_forearm",9);constant(crouch,"left_forearm",2);
 const root=crouch.getBoneAnimator(Group.all.find(g=>g.name==="root"));
 for(const time of [0,1])root.addKeyframe({channel:"position",time,interpolation:"linear",data_points:[{x:0,y:0,z:0}]});
 isolated(crouch);Timeline.setTime(0);Animator.preview();
 const soles={right:Infinity,left:Infinity};
 for(const cube of Cube.all){let p=cube.parent;let side=null;let visible=cube.visibility!==false;
  while(p&&p!=="root"){if(p.visibility===false)visible=false;if(p.name==="right_foot")side="right";if(p.name==="left_foot")side="left";p=p.parent;}
  if(!visible||!side)continue;if(!cube.mesh)throw new Error("Missing foot mesh");cube.mesh.updateWorldMatrix(true,false);
  const positions=cube.mesh.geometry.getAttribute("position");if(!positions)throw new Error("Missing foot vertices");
  for(let i=0;i<positions.count;i++){const v=new THREE.Vector3().fromBufferAttribute(positions,i).applyMatrix4(cube.mesh.matrixWorld);soles[side]=Math.min(soles[side],v.y);}
 }
 if(!Number.isFinite(soles.right)||!Number.isFinite(soles.left))throw new Error("Both visible feet required");
 const shift=-Math.min(soles.right,soles.left);for(const k of root.position)k.set("y",shift);
 crouchProbe={before:soles,root_y:shift,after:{right:soles.right+shift,left:soles.left+shift},scope:"static preview only; no moving IK or clipping approval"};
 const drawn={left_arm:[85,0,8],left_forearm:[5,0,0],right_arm:[72,-28,-12],right_forearm:[72,0,0]};
 const neutral={left_arm:[-.5,0,3],left_forearm:[2,0,0],right_arm:[1.5,0,-3],right_forearm:[9,0,0]};
 const draw=make("bow_draw",.5,"hold");const hold=make("bow_hold",1,"loop");
 for(const name of Object.keys(drawn)){
  rotation(draw,name,[[0,...neutral[name]],[.35,...drawn[name]],[.5,...drawn[name]]]);
  constant(hold,name,...drawn[name]);
 }
 const block=make("block",1,"loop");constant(block,"left_arm",65,0,22);constant(block,"left_forearm",55);
 Undo.finishEdit("Prototype player state clips (not runtime accepted)");Canvas.updateAll();
}catch(error){Undo.cancelEdit();throw error;}
JSON.stringify({created:made.map(a=>({name:a.name,uuid:a.uuid,length:a.length,loop:a.loop})),crouch_probe:crouchProbe})'''


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args(argv)
    state = None
    report = None
    session = None
    failure = None
    try:
        source_bytes = args.source.read_bytes()
        source_hash = hashlib.sha256(source_bytes).hexdigest()
        source = json.loads(source_bytes)
        if any(a['name'] in CREATED for a in source['animations']):
            raise ValueError('Source already contains proposed clip names')
        walks = [a for a in source['animations'] if a['name'] == 'walk']
        if len(walks) != 1:
            raise ValueError('Exactly one source walk required')
        run = retime_clip(walks[0], 'run', .7)
        session = ReviewSession()
        identity = session.bind(args.source)
        if identity['source_sha256'] != source_hash:
            raise ValueError('Source changed during preflight')
        before = session.export()
        args.out.mkdir(parents=True, exist_ok=False)
        (args.out / 'before.bbmodel').write_text(json.dumps(before, indent=2))
        report = dict(identity, status='INCOMPLETE', planned_clips=list(CREATED), deferred=DEFERRED,
                      limitations=['Run retimes walk; sprint cadence, speed and held items unverified.',
                                   'Crouch is static support prototype; transitions, collisions and camera unverified.',
                                   'Bow/block are right-handed pose studies; no held-item alignment, triggers or layer arbitration validated.',
                                   'This is not a feeling repair or visual acceptance.',
                                   'Camera and keyframe selections are not restored. MCP risky_eval also touches Undo history.'])
        manifest_path = args.out / 'manifest.json'
        manifest_path.write_text(json.dumps(report, indent=2))
        state = session.snapshot()
        report['authoring'] = session.js(author_code(run))
        after = session.export()
        verify_only_new_clips(before, after)
        if hashlib.sha256(args.source.read_bytes()).hexdigest() != source_hash:
            raise ValueError('Source file changed during authoring')
        target = args.out / 'race_nieumarly.bbmodel'
        target.write_text(json.dumps(after, indent=2))
        report['output_sha256'] = hashlib.sha256(target.read_bytes()).hexdigest()
        report['output'] = str(target.resolve())
    except BaseException as exc:
        failure = exc
        if report is not None:
            report['error'] = f'{type(exc).__name__}: {exc}'
    finally:
        if state is not None:
            try:
                session.restore(state)
                report['editor_state_restored'] = True
            except BaseException as exc:
                if report is not None:
                    report['restore_error'] = f'{type(exc).__name__}: {exc}'
                failure = failure or exc
        if report is not None:
            if failure is None:
                report['status'] = 'PROTOTYPE_NOT_ACCEPTED'
            (args.out / 'manifest.json').write_text(json.dumps(report, indent=2))
    if failure is not None:
        print(f'Authoring failed: {failure}', file=sys.stderr)
        return 1
    print(args.out / 'race_nieumarly.bbmodel')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
