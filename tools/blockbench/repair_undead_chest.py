"""Author a ribcage candidate in the existing workshop; keep chosen face B."""
import json
from pathlib import Path
from review_session import ReviewSession, literal
SOURCE=Path('/opt/docker/mc/data/world1/output/undead-player-v5/feeling-strong-02/race_nieumarly.bbmodel')
OUT=Path('/opt/docker/mc/data/world1/output/undead-player-v5/chest-repair-01')

def main():
 s=ReviewSession();manifest=s.bind(SOURCE);before=s.export();OUT.mkdir(exist_ok=False)
 (OUT/'before.bbmodel').write_text(json.dumps(before,indent=2))
 template=next(c for c in before['elements'] if c['name']=='rib_r_1')
 code='const template='+literal(template)+''';
const ribs=Cube.all.filter(c=>c.name.startsWith("rib_"));const shadow=Cube.all.find(c=>c.name==="chest_shadow");const sternum=Cube.all.find(c=>c.name==="sternum");const torso=Group.all.find(g=>g.name==="torso");
if(!shadow||!sternum||!torso)throw new Error("Missing chest parts");
const old=ribs.concat([shadow,sternum]);const created=[];
Undo.initEdit({elements:old,outliner:true});
try{
 for(const c of ribs)c.remove();
 shadow.from=[-2.55,16.4,-.7];shadow.to=[2.55,23.1,1.4];
 sternum.from=[-.33,18.15,-2.3];sternum.to=[.33,22.25,-1.6];
 function add(name,from,to,origin,rotation){const data=JSON.parse(JSON.stringify(template));delete data.uuid;data.name=name;data.from=from;data.to=to;data.origin=origin;data.rotation=rotation;
 for(const face of Object.values(data.faces)){if(typeof face.texture==="number")face.texture=Texture.all[face.texture].uuid;}
 const c=new Cube(data).addTo(torso).init();created.push(c);}
 const rows=[{y:21.15,w:2.15},{y:19.85,w:2.35},{y:18.55,w:1.95}];
 for(let i=0;i<rows.length;i++){const r=rows[i];for(const side of [-1,1]){
  const label=side===1?"r":"l";
  const from=side===1?[.2,r.y,-2.35]:[-r.w,r.y,-2.35];
  const to=side===1?[r.w,r.y+.5,-1.65]:[-.2,r.y+.5,-1.65];
  add("rib_front_"+label+"_"+i,from,to,[side*.2,r.y+.25,-2],[0,0,-side*22.5]);
  const y=r.y-(r.w-.2)*.3827;
  add("rib_wrap_"+label+"_"+i,[side===1?r.w-.4:-r.w-.15,y,-1.8],[side===1?r.w+.15:-r.w+.4,y+.5,.5],[side*(r.w-.1),y+.25,-1],[0,side*22.5,0]);
 }}
 Canvas.updateAll();Undo.finishEdit("Recess chest and wrap three ribs around torso",{elements:created.concat([shadow,sternum]),outliner:true});
 Modes.options.edit.select();Project.name="ROBOCZY - V5 klatka 01, twarz B";
}catch(e){Undo.cancelEdit();throw e;}
JSON.stringify({newRibs:created.length})'''
 print(s.js(code));after=s.export()
 if before['textures']!=after['textures'] or before['groups']!=after['groups']:raise RuntimeError('Unexpected texture/rig change')
 old={c['uuid']:c for c in before['elements'] if not c['name'].startswith('rib_') and c['name'] not in ('chest_shadow','sternum')}
 new={c['uuid']:c for c in after['elements']}
 if any(c!=new.get(uid) for uid,c in old.items()):raise RuntimeError('Unexpected change outside chest')
 (OUT/'race_nieumarly.bbmodel').write_text(json.dumps(after,indent=2))
 (OUT/'manifest.json').write_text(json.dumps(dict(manifest,status='ART_CANDIDATE_NOT_ACCEPTED',face='B accepted',changes=['rib geometry','chest recess','sternum']),indent=2))
 print(OUT/'race_nieumarly.bbmodel')
if __name__=='__main__':main()
