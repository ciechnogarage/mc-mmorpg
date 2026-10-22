"""Author one reviewable feeling variant, reusing the current workshop tab."""
import argparse
import json
from pathlib import Path
from review_session import ReviewSession, literal


def main():
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--baseline',required=True);p.add_argument('--out',required=True);p.add_argument('--intensity',required=True,type=float);args=p.parse_args()
 if not 0 <= args.intensity <= 1.5:raise ValueError('intensity outside prototype range')
 s=ReviewSession();manifest=s.bind(args.source);before=s.export();base=json.loads(Path(args.baseline).read_text())
 for key in ('elements','groups','outliner','textures'):
  if before[key]!=base[key]:raise ValueError('Variants must have identical appearance')
 out=Path(args.out);out.mkdir(parents=True,exist_ok=False);(out/'before.bbmodel').write_text(json.dumps(before,indent=2))
 names=['idle','walk','hurt']; originals=[a for a in base['animations'] if a['name'] in names]
 if len(originals)!=3:raise ValueError('Missing baseline clips')
 state=s.snapshot()
 code='const originals='+literal(originals)+';const strength='+str(args.intensity)+''';
const targets=Animation.all.filter(a=>["idle","walk","hurt"].includes(a.name));
Undo.initEdit({animations:targets});
try{
 Timeline.pause();for(const item of AnimationItem.all)item.playing=false;
 for(const original of originals){const a=Animation.all.find(a=>a.name===original.name);for(const id of Object.keys(a.animators))a.removeAnimator(id);a.markers.splice(0);a.extend(original);a.setLength(original.length);}
 function setTrack(a,name,frames){const tr=a.getBoneAnimator(Group.all.find(g=>g.name===name));for(const k of tr.rotation.slice())k.remove();for(const [t,x,y,z] of frames)tr.addKeyframe({channel:"rotation",time:t,interpolation:"linear",data_points:[{x,y,z}]});}
 if(strength>0){
 const idle=Animation.all.find(a=>a.name==="idle");
 const times=[0,.9,1.1,1.4,3];
 setTrack(idle,"torso",times.map((t,i)=>[t,2+[0,.05,-.2,.25,0][i]*strength,0,-.8*strength]));
 setTrack(idle,"right_arm",times.map((t,i)=>[t,1.5*strength,0,-3-[0,.1,1.2,.2,0][i]*strength]));
 setTrack(idle,"left_arm",times.map((t,i)=>[t,-.5*strength,0,3+.3*strength]));
 setTrack(idle,"right_forearm",[[0,9*strength,0,0],[3,9*strength,0,0]]);
 setTrack(idle,"left_forearm",[[0,2*strength,0,0],[3,2*strength,0,0]]);
 const walk=Animation.all.find(a=>a.name==="walk");
 for(const tr of Object.values(walk.animators)){
 const originalKeys=(tr.rotation||[]).map(k=>({time:k.time,x:Number(k.get("x")),y:Number(k.get("y")),z:Number(k.get("z"))}));
 function delayed(axis,time){const t=((time-strength/12)%1+1)%1;const lo=originalKeys.filter(k=>k.time<=t+1e-8).sort((a,b)=>b.time-a.time)[0];const hi=originalKeys.filter(k=>k.time>=t-1e-8).sort((a,b)=>a.time-b.time)[0];if(!lo||!hi)throw new Error("Missing loop samples");const u=hi.time>lo.time?(t-lo.time)/(hi.time-lo.time):0;return lo[axis]*(1-u)+hi[axis]*u;}
 for(const k of tr.rotation||[]){
  if(tr.name==="right_arm")k.set("x",delayed("x",k.time)*(1-.35*strength));
  if(tr.name==="left_arm")k.set("x",Number(k.get("x"))*(1-.12*strength));
  if(tr.name==="right_forearm")k.set("x",Number(k.get("x"))+9*strength);
  if(tr.name==="left_forearm")k.set("x",Number(k.get("x"))+2*strength);
  if(tr.name==="torso"){k.set("y",delayed("y",k.time)*(1-.25*strength));k.set("z",delayed("z",k.time)-.8*strength);}
 }
 }
 const hurt=Animation.all.find(a=>a.name==="hurt");
 setTrack(hurt,"right_forearm",[[0,9*strength,0,0],[.05,15*strength,0,0],[.15,11*strength,0,0],[.2,7*strength,0,0],[.25,10*strength,0,0],[.35,9*strength,0,0]]);
 setTrack(hurt,"left_forearm",[[0,2*strength,0,0],[.05,5*strength,0,0],[.15,3*strength,0,0],[.35,2*strength,0,0]]);
 }
 Undo.finishEdit("Undead feeling prototype");Canvas.updateAll();
}catch(e){Undo.cancelEdit();throw e;}
JSON.stringify({changed:targets.map(a=>a.name),strength})'''
 try:
  print(s.js(code))
  s.js('Project.name='+literal('ROBOCZY - '+out.name+' (twarz B kandydat)')+'; JSON.stringify({labelled:true})')
  after=s.export()
  for key in ('elements','groups','outliner','textures'):
   if before[key]!=after[key]:raise RuntimeError('Unexpected appearance change')
  (out/'race_nieumarly.bbmodel').write_text(json.dumps(after,indent=2))
  (out/'manifest.json').write_text(json.dumps(dict(manifest,status='PROTOTYPE_NOT_ACCEPTED',intensity=args.intensity,changed_clips=names,baseline=str(Path(args.baseline).resolve())),indent=2))
 finally:s.restore(state)
 print(out/'race_nieumarly.bbmodel')
if __name__=='__main__':main()
