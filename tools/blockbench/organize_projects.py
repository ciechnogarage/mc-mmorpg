"""Snapshot and label existing projects without closing or overwriting their work."""
from pathlib import Path
from mcp_client import call
import json,hashlib
out=Path('/opt/docker/mc/data/world1/output/blockbench-tab-backups/2026-09-18');out.mkdir(parents=True,exist_ok=True)
call('initialize',{'protocolVersion':'2025-03-26','capabilities':{},'clientInfo':{'name':'project-organizer','version':'1'}});call('notifications/initialized',notification=True)
def tool(name,args):
 r=call('tools/call',{'name':name,'arguments':args})
 if 'error' in r or r.get('result',{}).get('isError'):raise RuntimeError(r)
 result=r['result'];text=next((c['text'] for c in result.get('content',[]) if c['type']=='text'),'')
 if text.startswith('Error'):raise RuntimeError(text)
 return text
def js(code):return tool('risky_eval',{'code':code})
def parsed(s):
 v=json.loads(s)
 return json.loads(v) if isinstance(v,str) else v
projects=parsed(js('JSON.stringify(ModelProject.all.map(p=>({name:p.name,uuid:p.uuid,saved:p.saved,selected:p===Project})))'))
labels={
'6b05580f-ad90-897f-de91-d953273fcee4':'ARCHIWUM - V1 wczesny podglad',
'7f757ddf-e641-d08d-5f24-99cc32933227':'ARCHIWUM - V1',
'd2dc6be6-3c56-69fa-2e06-43dc00190e3f':'ARCHIWUM - V2',
'2ba344e7-7060-0d51-406a-fa635103d99c':'ARCHIWUM - V3 przed korekta ataku',
'83e7b99f-5cdf-94ab-f7ba-6d1b2143fd9e':'ARCHIWUM - V3',
'ed2ecaf4-c54a-e93a-0049-39ca0758e0fa':'ARCHIWUM - V4 przed korekta upadku',
'33b15100-8faf-4695-92dd-0db22a646b1d':'A - OBECNA TWARZ V4',
'49ce556e-1bf4-ce8d-7ae5-c477f121f5af':'REFERENCJA - caly model V2',
'a7d08919-5165-b0c5-b18e-3863bbaac0ff':'B - TWARZ V2 NA STROJU V4'}
manifest=[]
for p in projects:
 if p['uuid'] not in labels:continue
 js('ModelProject.all.find(p=>p.uuid==='+json.dumps(p['uuid'])+').select(); "selected"')
 export=parsed(tool('export_model',{'codec_id':'project','max_content_length':2000000}))
 assert not export['truncated']
 path=out/(p['uuid']+'.bbmodel');payload=export['content']
 if path.exists() and path.read_text()!=payload:raise RuntimeError('Existing snapshot differs; refusing overwrite '+str(path))
 path.write_text(payload)
 label=labels[p['uuid']]
 js('Project.name='+json.dumps(label)+'; "labelled"')
 manifest.append(dict(p,label=label,path=str(path.resolve()),sha256=hashlib.sha256(payload.encode()).hexdigest()))
(out/'index.json').write_text(json.dumps(manifest,indent=2))
js('ModelProject.all.find(p=>p.uuid==="a7d08919-5165-b0c5-b18e-3863bbaac0ff").select(); "B selected"')
print(js('JSON.stringify(ModelProject.all.map(p=>({name:p.name,selected:p===Project})))'))
print('Saved',len(manifest),'snapshots; no project closed.')
