"""Transactional multi-joint death prototype on the verified working project."""
import argparse
import json
from pathlib import Path
from review_session import ReviewSession, literal

SOURCE=Path('/opt/docker/mc/data/world1/output/undead-player-v5/motion-repair-01/race_nieumarly.bbmodel')
OUT=Path('/opt/docker/mc/data/world1/output/undead-player-v5/motion-repair-02')

def main():
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=SOURCE);p.add_argument('--out',type=Path,default=OUT);args=p.parse_args();source=args.source;out=args.out
 s=ReviewSession(); manifest=s.bind(source);before=s.export();out.mkdir(exist_ok=False)
 (out/'before.bbmodel').write_text(json.dumps(before,indent=2))
 state=s.snapshot()
 code='''const a=Animation.all.find(a=>a.name==="death");
const edits=Animation.all.filter(a=>["death","jump_start","jump_end"].includes(a.name));
Undo.initEdit({animations:edits});
try {
Timeline.pause(); AnimationItem.all.forEach(item=>{item.playing=false;});
Modes.options.animate.select();a.select();for(const x of AnimationItem.all)x.playing=false;a.playing=true;
AnimationItem.all.forEach(item=>{item.playing=false;});a.playing=true;
function track(name,frames){const tr=a.getBoneAnimator(Group.all.find(g=>g.name===name));for(const k of tr.rotation.slice())k.remove();for(const [t,x,y,z] of frames)tr.addKeyframe({channel:"rotation",time:t,interpolation:"linear",data_points:[{x,y,z}]});}
track("root",[[0,0,0,0],[.2,-3,0,0],[.4,-12,0,0],[.6,-40,0,0],[.85,-82,0,-6],[1,-90,0,-8],[1.2,-90,0,-8]]);
track("right_leg",[[0,0,0,0],[.2,14,0,0],[.4,32,0,-3],[.6,25,0,-4],[.85,28,0,-8],[1.2,25,0,-8]]);
track("left_leg",[[0,0,0,0],[.2,8,0,0],[.4,24,0,4],[.6,18,0,6],[.85,10,0,8],[1.2,8,0,8]]);
track("right_shin",[[0,0,0,0],[.2,-28,0,0],[.4,-64,0,0],[.6,-50,0,0],[.85,-62,0,0],[1.2,-55,0,0]]);
track("left_shin",[[0,0,0,0],[.2,-16,0,0],[.4,-48,0,0],[.6,-36,0,0],[.85,-22,0,0],[1.2,-18,0,0]]);
track("right_foot",[[0,0,0,0],[.2,14,0,0],[.4,25,0,0],[.6,15,0,0],[1.2,15,0,0]]);
track("left_foot",[[0,0,0,0],[.2,8,0,0],[.4,20,0,0],[.6,10,0,0],[1.2,6,0,0]]);
track("torso",[[0,2,0,0],[.2,7,-2,1],[.4,12,-4,2],[.6,2,-3,1],[.85,10,3,0],[1.05,7,3,0],[1.2,8,3,0]]);
track("hi_head",[[0,0,0,0],[.2,4,0,-2],[.4,8,0,-3],[.6,-3,0,-4],[.85,-7,0,-5],[1.05,-3,8,-8],[1.2,-4,8,-8]]);
track("right_arm",[[0,0,0,-3],[.2,8,0,-8],[.4,22,0,-16],[.6,35,0,-23],[.85,12,0,-30],[1.05,9,0,-30],[1.2,8,0,-30]]);
track("left_arm",[[0,0,0,3],[.2,4,0,6],[.4,12,0,10],[.6,22,0,16],[.85,5,0,23],[1.05,-3,0,12],[1.2,-4,0,12]]);
track("right_forearm",[[0,0,0,0],[.4,16,0,0],[.6,25,0,0],[.85,10,0,0],[1.2,22,0,0]]);
track("left_forearm",[[0,0,0,0],[.4,9,0,0],[.6,18,0,0],[.85,6,0,0],[1.2,8,0,0]]);
for(const clip of edits){
 Timeline.pause(); AnimationItem.all.forEach(item=>{item.playing=false;});
 clip.select();AnimationItem.all.forEach(item=>{item.playing=false;});clip.playing=true;const root=clip.getBoneAnimator(Group.all.find(g=>g.name==="root"));
 for(const k of root.position.slice())k.remove();
 for(let i=0;i<=Math.round(clip.length*60);i++)root.addKeyframe({channel:"position",time:i/60,interpolation:"linear",data_points:[{x:0,y:0,z:0}]});
 for(const k of root.position){Timeline.setTime(k.time);Animator.preview();let low=Infinity;
 for(const c of Cube.all){if(c.name==="player_hitbox"||c.visibility===false)continue;c.mesh.updateWorldMatrix(true,false);low=Math.min(low,new THREE.Box3().setFromObject(c.mesh).min.y);}
 if(!Number.isFinite(low))throw new Error("Missing geometry");k.set("y",-low);
 }
}
Undo.finishEdit("Multi-joint death prototype and dense ground support");Canvas.updateAll();
}catch(e){Undo.cancelEdit();throw e;}
JSON.stringify({authored:true})'''
 try:
  print(s.js(code));after=s.export()
  for key in ('elements','groups','outliner','textures'):
   if before[key]!=after[key]:raise RuntimeError('Unexpected non-animation change: '+key)
  (out/'race_nieumarly.bbmodel').write_text(json.dumps(after,indent=2))
  (out/'manifest.json').write_text(json.dumps(dict(manifest,status='AUTHORED_NOT_ACCEPTED',changes=['death','jump_start','jump_end']),indent=2))
 finally:s.restore(state)
 print(out/'race_nieumarly.bbmodel')
if __name__=='__main__':main()
