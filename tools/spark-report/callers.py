import struct,sys
from pb import fields, top, rv
b,fs=top(sys.argv[1])
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
    nodes.append((cls,meth,t,kids))
parent={}
for i,(c,m,t,k) in enumerate(nodes):
    for ch in k: parent[ch]=i
target=sys.argv[2]
hits=[i for i,(c,m,t,k) in enumerate(nodes) if target.lower() in (c+'.'+m).lower() and t>=100]
hits.sort(key=lambda i:-nodes[i][2])
for i in hits[:6]:
    chain=[]; j=i
    while j in parent and len(chain)<14:
        j=parent[j]; c,m,t,_=nodes[j]
        if c.startswith(('java.','jdk.','native','libc','libjvm')): continue
        chain.append('%s.%s'%(c.split('.')[-1],m))
    print('%6.0f ms  %s.%s'%(nodes[i][2],nodes[i][0],nodes[i][1]))
    print('        <- '+' <- '.join(chain[:10]))
