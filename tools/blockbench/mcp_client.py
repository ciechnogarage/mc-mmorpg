import json,urllib.request,pathlib
URL='http://127.0.0.1:3000/bb-mcp'
session=None
seq=0
def call(method,params=None,notification=False):
 global session,seq
 seq+=1
 data={'jsonrpc':'2.0','method':method}
 if not notification:data['id']=seq
 if params is not None:data['params']=params
 headers={'Content-Type':'application/json','Accept':'application/json, text/event-stream'}
 if session:headers['Mcp-Session-Id']=session
 with urllib.request.urlopen(urllib.request.Request(URL,json.dumps(data).encode(),headers),timeout=30) as r:
  session=r.headers.get('Mcp-Session-Id',session)
  if r.status==202:return None
  if 'text/event-stream' in r.headers.get('Content-Type',''):
   for raw in r:
    line=raw.decode().strip()
    if line.startswith('data:'):
     obj=json.loads(line[5:])
     if obj.get('id')==seq:return obj
  else:return json.load(r)
