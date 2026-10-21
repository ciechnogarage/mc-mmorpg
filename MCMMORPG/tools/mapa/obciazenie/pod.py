# pod.py <plik> <Klasa.metoda czesc> [glebokosc]: co jest POD dana metoda (suma czasu dzieci, rekurencyjnie do glebokosci)
import struct,sys
from collections import defaultdict
from pb import fields, top, rv
b,fs=top(sys.argv[1]); cel=sys.argv[2]; gl=int(sys.argv[3]) if len(sys.argv)>3 else 2
thread=[v for f,w,v in fs if f==2][0]
nodes=[]
for n in [v for f,w,v in fields(thread) if f==3]:
    cls=meth=''; t=0.0; kids=[]
    for f,w,v in fields(n):
        if f==3: cls=v.decode('utf8','replace')
        elif f==4: meth=v.decode('utf8','replace')
        elif f==8: t=sum(struct.unpack('<%dd'%(len(v)//8),v))
        elif f==9:
            i=0
            while i<len(v):
                x,i=rv(v,i); kids.append(x)
    nodes.append((cls+'.'+meth,t,kids))
agg=defaultdict(float); suma=0
def zejdz(i,d,pref):
    for k in nodes[i][2]:
        agg[pref+nodes[k][0]]+=nodes[k][1]
        if d>1: zejdz(k,d-1,pref+'  ')
for i,(nm,t,k) in enumerate(nodes):
    if cel in nm: suma+=t; zejdz(i,gl,'')
print('%s: %.0f ms'%(cel,suma))
for k,v in sorted(agg.items(),key=lambda x:-x[1])[:25]: print('  %7.0f ms  %s'%(v,k))
