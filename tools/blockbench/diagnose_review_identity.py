import json
from pathlib import Path
from review_session import ReviewSession
from project_identity import canonical
s=ReviewSession()
a=canonical(json.loads(Path('/opt/docker/mc/data/world1/output/undead-player-v5/face-review/candidate-B-face-v2.bbmodel').read_text()))
b=canonical(s.export())
def diff(a,b,p=''):
 if type(a)!=type(b): print(p,repr(a)[:100],repr(b)[:100]);return
 if isinstance(a,dict):
  for k in a.keys()|b.keys():
   if k not in a or k not in b:print(p+'/'+k,'added/removed',repr(a.get(k))[:100],repr(b.get(k))[:100])
   else:diff(a[k],b[k],p+'/'+k)
 elif isinstance(a,list):
  if len(a)!=len(b):print(p,'length',len(a),len(b))
  else:
   for i,(x,y) in enumerate(zip(a,b)):diff(x,y,p+'/'+str(i))
 elif a!=b: print(p,repr(a)[:100],repr(b)[:100])
diff(a,b)
