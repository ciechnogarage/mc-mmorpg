import struct,sys
from collections import defaultdict
from pb import fields, top, rv
b,fs=top(sys.argv[1])
src={}
for f,w,v in fs:
    if f==3:
        d=dict((ff,vv) for ff,ww,vv in fields(v))
        if 1 in d and 2 in d: src[d[1].decode()]=d[2].decode()
thread=[v for f,w,v in fs if f==2][0]
raw=[v for f,w,v in fields(thread) if f==3]
nodes=[]
for n in raw:
    cls=meth=''; t=0.0; kids=[]
    for f,w,v in fields(n):
        if f==3: cls=v.decode('utf8','replace')
        elif f==4: meth=v.decode('utf8','replace')
        elif f==8: t=sum(struct.unpack('<%dd'%(len(v)//8),v))
        elif f==9:
            i=0
            while i<len(v):
                x,i=rv(v,i); kids.append(x)
    nodes.append([cls,meth,t,kids])
ref=set()
for n in nodes: ref.update(n[3])
roots=[i for i in range(len(nodes)) if i not in ref]
total=sum(nodes[i][2] for i in roots)
# czas "roboczy" = bez parkowania w libc
work=total-sum(n[2] for n in nodes if n[0]=='native' and 'libc' in n[1])
print('total %.0f s, z tego praca %.1f s (%.1f%%)'%(total/1000, work/1000, 100*work/total))
print('\n-- korzenie --')
for i in roots: print('  %8.0f ms  %s.%s'%(nodes[i][2],nodes[i][0],nodes[i][1]))
agg=defaultdict(float)
for cls,meth,t,kids in nodes:
    agg[cls+'.'+meth]+=t
pat=sys.argv[2:] or ['Mojang','Skin','Character','Guilds','Yaml','save','Chunk','Spawn','Entity','Model','Mythic','Region','Autosave','World']
print('\n-- total-time wg wzorca --')
for p in pat:
    hits=sorted([(v,k) for k,v in agg.items() if p.lower() in k.lower()],reverse=True)[:5]
    for v,k in hits:
        if v>=200: print('  %8.0f ms  %s'%(v,k))
