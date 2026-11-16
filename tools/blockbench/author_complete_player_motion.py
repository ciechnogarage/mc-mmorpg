"""Apply player motion programs to a verified editor project, export a candidate."""
import argparse
import hashlib
import json
from pathlib import Path

from player_motion_program import build_states
from project_identity import canonical
from review_session import ReviewSession, literal


def publish_candidate(out, source, manifest, model):
    """Publish a native codec export; failures leave an explicit incomplete receipt.

    Editor authoring and filesystem publication are separate transactions.
    A failed publication intentionally leaves the editor candidate available.
    """
    receipt = out / 'manifest.json'
    path = out / 'race_nieumarly.bbmodel'
    temporary = out / 'race_nieumarly.bbmodel.tmp'
    manifest['status'] = 'INCOMPLETE'
    receipt.write_text(json.dumps(manifest, indent=2))
    try:
        if hashlib.sha256(source.read_bytes()).hexdigest() != manifest['source_sha256']:
            raise ValueError('Source changed during authoring')
        temporary.write_text(json.dumps(model, indent=2, allow_nan=False))
        if hashlib.sha256(source.read_bytes()).hexdigest() != manifest['source_sha256']:
            raise ValueError('Source changed during publication')
        temporary.replace(path)
        manifest.update(status='READY_FOR_VISUAL_REVIEW', output_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
        receipt_tmp = out / 'manifest.json.tmp'
        receipt_tmp.write_text(json.dumps(manifest, indent=2))
        receipt_tmp.replace(receipt)
    except Exception as exc:
        manifest.update(status='INCOMPLETE', error=str(exc), editor_candidate_retained=True)
        receipt.write_text(json.dumps(manifest, indent=2))
        raise
    return path


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source', required=True, type=Path)
    p.add_argument('--out', required=True, type=Path)
    args = p.parse_args()
    s = ReviewSession()
    manifest = s.bind(args.source)
    before = s.export()
    names = [g['name'] for g in before['groups']]
    if len(names) != len(set(names)):
        raise ValueError('Ambiguous bone names; refusing authoring')
    data = build_states({g['name']: g for g in before['groups']})
    if any(a['name'] in {*data, 'attack_left'} for a in before['animations']):
        raise ValueError('New clip names already present; refusing duplicate authoring')
    args.out.mkdir(parents=True, exist_ok=False)
    manifest['status'] = 'INCOMPLETE'
    (args.out / 'manifest.json').write_text(json.dumps(manifest, indent=2))
    (args.out / 'before.bbmodel').write_text(json.dumps(before, indent=2))
    code = 'const programs=' + literal(data) + ''';
const changedNames=['attack','hurt','spawn','jump_end'];
const changed=changedNames.map(n=>Animation.all.find(a=>a.name===n));
const idle=Animation.all.find(a=>a.name==='idle');
if(!idle||changed.some(a=>!a))throw new Error('Missing base clips');
const neutral={};
for(const tr of Object.values(idle.animators)){
 const k=tr.keyframes.find(k=>k.channel==='rotation'&&k.time===0);
 if(k)neutral[tr.name]=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));
}
const created=[];
const originalModel=JSON.parse(Codecs.project.compile());
Undo.initEdit({animations:changed});
try{
 Timeline.pause();for(const a of AnimationItem.all)a.playing=false;
 function key(a,bone,t,xyz,channel='rotation'){
  const matches=Group.all.filter(g=>g.name===bone);if(matches.length!==1)throw new Error('Missing or ambiguous bone '+bone);const g=matches[0];
  const tr=a.getBoneAnimator(g);
  const old=tr.keyframes.find(k=>k.channel===channel&&Math.abs(k.time-t)<1e-8);
  if(old)old.remove();
  tr.addKeyframe({channel,time:t,interpolation:'linear',data_points:[{x:xyz[0],y:xyz[1],z:xyz[2]}]});
 }
 for(const name of changedNames){
  const a=Animation.all.find(a=>a.name===name);
  if(name==='attack'||name==='hurt'){
   for(const [id,tr] of Object.entries(a.animators))if(tr.name==='root'||/_(leg|shin|foot)$/.test(tr.name))a.removeAnimator(id);
   a.override=true;
  }
  for(const [bone,xyz]of Object.entries(neutral)){
   if(name==='attack'||name==='hurt')key(a,bone,0,xyz);
   key(a,bone,a.length,xyz);
  }
 }
 for(const[name,program]of Object.entries(programs)){
  const a=new Animation({name,length:program.length,loop:program.loop,override:program.override}).add();created.push(a);
  for(const[bone,channels]of Object.entries(program.tracks))for(const[channel,frames]of Object.entries(channels))for(const[t,xyz]of frames)key(a,bone,t,xyz,channel);
  if(name==='bow_draw'||name==='bow_draw_left')for(const[bone,xyz]of Object.entries(neutral))if(program.tracks[bone])key(a,bone,0,xyz);
 }
 const attack=Animation.all.find(a=>a.name==='attack');
 const left=new Animation({name:'attack_left',length:attack.length,loop:attack.loop,override:true}).add();created.push(left);
 for(const tr of Object.values(attack.animators)){
  const bone=tr.name.startsWith('right_')?'left_'+tr.name.slice(6):tr.name.startsWith('left_')?'right_'+tr.name.slice(5):tr.name;
  for(const k of tr.keyframes){const d=k.data_points[0];let xyz=[Number(d.x),Number(d.y),Number(d.z)];if(k.channel==='rotation')xyz=[xyz[0],-xyz[1],-xyz[2]];else if(k.channel==='position')xyz[0]*=-1;key(left,bone,k.time,xyz,k.channel);}
 }
 for(const[bone,xyz]of Object.entries(neutral)){key(left,bone,0,xyz);key(left,bone,left.length,xyz);}
 Canvas.updateAll();
 const finalModel=JSON.parse(Codecs.project.compile());
 for(const field of ['elements','groups','outliner','textures'])if(JSON.stringify(originalModel[field])!==JSON.stringify(finalModel[field]))throw new Error('Appearance postcondition failed '+field);
 Undo.finishEdit('Player states and layered combat',{animations:changed.concat(created)});
 Modes.options.edit.select();Project.name='Nieumarly - player motion candidate';
}catch(e){for(const a of created)if(Animation.all.includes(a))a.remove();Undo.cancelEdit();throw e;}
JSON.stringify({changed:changedNames,created:created.map(a=>a.name)})'''
    result = s.js(code)
    after = s.export()
    for field in ('elements', 'groups', 'outliner', 'textures'):
        if canonical({field: before[field]}) != canonical({field: after[field]}):
            raise RuntimeError('Unexpected appearance change: ' + field)
    manifest.update(source_changes=result,
                    limitations=['No runtime calibration or equipment alignment acceptance',
                                 'Swim origin and head tracking require client verification',
                                 'Sparse combat tracks preserve locomotion; blend behavior requires runtime'])
    path = publish_candidate(args.out, args.source, manifest, after)
    print(json.dumps(result))
    print(path)


if __name__ == '__main__':
    main()
