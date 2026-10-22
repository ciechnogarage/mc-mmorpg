"""Author the face-only candidate transactionally in Blockbench; capture equal views."""
from mcp_client import call
from pathlib import Path
import json,base64,copy
ROOT=Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)
out=ROOT/'output/undead-player-v5/face-review'
call('initialize',{'protocolVersion':'2025-03-26','capabilities':{},'clientInfo':{'name':'face-review','version':'1'}});call('notifications/initialized',notification=True)
def tool(name,args):
 r=call('tools/call',{'name':name,'arguments':args})
 if 'error' in r or r.get('result',{}).get('isError'):raise RuntimeError(r)
 for c in r['result'].get('content',[]):
  if c['type']=='text' and c['text'].startswith('Error'):raise RuntimeError(c['text'])
 return r['result']
def js(code):return tool('risky_eval',{'code':code})
def literal(data):return json.dumps(data).replace('/','\\u002f')
def load_model(version):
 m=json.loads((ROOT/f'output/undead-player-v{version}/race_nieumarly.bbmodel').read_text())
 js('newProject(Formats.free); Codecs.project.parse('+literal(m)+'); Modes.options.edit.select(); Project.name='+literal('REFERENCJA - V'+str(version))+'; "loaded"')
 return m
views=[('front',[0,28,-24],[0,28,-2]),('three-quarter',[16,31,-24],[0,28,-2]),('side',[24,28,-2],[0,28,-2]),('whole',[36,25,-55],[0,16,0])]
def capture(label):
 for name,pos,target in views:
  tool('set_camera_angle',{'position':pos,'target':target,'projection':'perspective'})
  result=tool('capture_screenshot',{})
  c=next(c for c in result['content'] if c['type']=='image')
  (out/f'{label}-{name}.png').write_bytes(base64.b64decode(c['data']))
 print(label,'captured',flush=True)
v2=load_model(2);capture('reference-v2')
v4=load_model(4);js('Project.name="A - OBECNA TWARZ V4"; "labelled"');capture('A-current-v4')
head_id=next(g['uuid'] for g in v2['groups'] if g['name']=='hi_head')
def find_node(nodes,ident):
 for n in nodes:
  if isinstance(n,dict):
   if n['uuid']==ident:return n
   result=find_node(n['children'],ident)
   if result:return result
ids=set(find_node(v2['outliner'],head_id)['children'])
head_cubes=[copy.deepcopy(c) for c in v2['elements'] if c['uuid'] in ids]
for c in head_cubes:
 for key in ['from','to','origin']:c[key][2]-=.9
tex=copy.deepcopy(v2['textures'][0]);tex.update({'name':'race_nieumarly_face_v2.png','id':'1','namespace':'','folder':'','uuid':'a42da18a-1332-4efb-8e46-47f7480c7aaf'})
# Body, head group rest pose and animation tracks remain untouched.
code='const head=Group.all.find(g=>g.name==="hi_head"); const old=head.children.slice(); Undo.initEdit({elements:old,textures:[],outliner:true}); try { const tx=new Texture('+literal({k:v for k,v in tex.items() if k!='source'})+').fromDataURL('+literal(tex['source'])+').add(false); for(const c of old)c.remove(); for(const data of '+literal(head_cubes)+') { for(const face of Object.values(data.faces))face.texture=tx.uuid; new Cube(data).addTo(head).init(); } Canvas.updateAll(); Undo.finishEdit("Restore V2 face only",{elements:head.children.slice(),textures:[tx],outliner:true}); "Face candidate authored"; } catch(e) { Undo.cancelEdit(); throw e; }'
print(js(code),flush=True)
js('Project.name="B - TWARZ V2 NA STROJU V4"; "labelled"')
r=tool('export_model',{'codec_id':'project','max_content_length':2000000})
e=json.loads(next(c['text'] for c in r['content'] if c['type']=='text'));assert not e['truncated'];(out/'candidate-B-face-v2.bbmodel').write_text(e['content'])
capture('B-face-v2')
