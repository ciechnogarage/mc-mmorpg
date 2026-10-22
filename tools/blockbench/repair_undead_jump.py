"""Author a separate jump repair candidate via a transactional Blockbench edit."""
import json
from pathlib import Path
from review_session import ReviewSession, literal

SOURCE=Path('/opt/docker/mc/data/world1/output/undead-player-v5/face-review/candidate-B-face-v2.bbmodel')
OUT=Path('/opt/docker/mc/data/world1/output/undead-player-v5/motion-repair-01')
LABEL='ROBOCZY - V5 naprawa ruchu 01 (twarz B kandydat)'

def main():
    s=ReviewSession(); manifest=s.bind(SOURCE)
    projects=s.js('JSON.stringify(ModelProject.all.map(p=>p.name))')
    if LABEL in projects:raise RuntimeError('Workshop already exists; refusing duplicate or overwrite')
    OUT.mkdir(exist_ok=False)
    before=s.export();(OUT/'before.bbmodel').write_text(json.dumps(before,indent=2))
    s.js('newProject(Formats.free); Codecs.project.parse('+literal(before)+'); Project.name='+literal(LABEL)+'; JSON.stringify({created:Project.uuid})')
    code='''const targets=Animation.all.filter(a=>["jump_start","jump_end"].includes(a.name));
if(targets.length!==2)throw new Error("Missing jump clips");
Undo.initEdit({animations:targets});
try {
for(const a of targets){
 Timeline.pause(); AnimationItem.all.forEach(item=>{item.playing=false;});
 Modes.options.animate.select(); a.select();
 AnimationItem.all.forEach(item=>{item.playing=false;}); a.playing=true;
 const length=a.length; const times=Array.from({length:Math.round(length*20)+1},(_,i)=>i/20);
 const root=a.getBoneAnimator(Group.all.find(g=>g.name==="root"));
 for(const k of root.position.slice())k.remove();
 for(const side of ["right","left"]){
  for(const part of ["leg","shin","foot"]){
   const tr=a.getBoneAnimator(Group.all.find(g=>g.name===side+"_"+part));
   for(const k of tr.rotation.slice())k.remove();
   for(const t of times){const peak=a.name==="jump_start"?20:18;
    const angle=peak*(t<=0.1?t/0.1:(length-t)/(length-0.1));
    tr.addKeyframe({channel:"rotation",time:t,interpolation:"linear",data_points:[{x:part==="shin"?-2*angle:angle,y:0,z:0}]});
   }
  }
 }
 for(const t of times)root.addKeyframe({channel:"position",time:t,interpolation:"linear",data_points:[{x:0,y:0,z:0}]});
 for(const k of root.position){
  Timeline.setTime(k.time);Animator.preview();
  let low=Infinity;
  for(const c of Cube.all){let p=c.parent;let foot=false;while(p&&p!=="root"){if(p.name==="right_foot"||p.name==="left_foot")foot=true;p=p.parent;}if(!foot)continue;
   c.mesh.updateWorldMatrix(true,false);low=Math.min(low,new THREE.Box3().setFromObject(c.mesh).min.y);
  }
  if(!Number.isFinite(low))throw new Error("No feet"); k.set("y",-low);
 }
}
Undo.finishEdit("Repair jump support using actual foot geometry");
Modes.options.edit.select();Canvas.updateAll();
} catch(e){Undo.cancelEdit();throw e;}
JSON.stringify({edited:targets.map(a=>a.name)})'''
    print(s.js(code))
    after=s.export()
    for key in ('elements','groups','outliner','textures'):
        if before[key]!=after[key]:raise RuntimeError('Unexpected non-animation change: '+key)
    (OUT/'race_nieumarly.bbmodel').write_text(json.dumps(after,indent=2))
    (OUT/'manifest.json').write_text(json.dumps(dict(manifest,status='AUTHORED_NOT_ACCEPTED',changes=['jump_start','jump_end'],face='B candidate, choice pending'),indent=2))
    print(OUT/'race_nieumarly.bbmodel')
if __name__=='__main__':main()
