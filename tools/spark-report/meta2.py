import struct,sys
from pb import fields, top
def g(msg,path):
    cur=[msg]
    for p in path:
        nxt=[]
        for m in cur:
            for f,w,v in fields(m):
                if f==p: nxt.append(v)
        cur=nxt
    return cur
for path in sys.argv[1:]:
    b,fs=top(path)
    md=[v for f,w,v in fs if f==1][0]
    ps=g(md,[8])[0]
    mem=g(ps,[1])[0]
    heap=fields(g(mem,[1])[0]); nonheap=fields(g(mem,[2])[0])
    hv={f:v for f,w,v in heap}; nv={f:v for f,w,v in nonheap}
    print('### %s'%path.split('/')[5])
    print('  heap used %.2f GB / max %.2f GB   non-heap used %.0f MB (committed %.0f MB)'%(
        hv[1]/2**30, hv[4]/2**30, nv[1]/2**20, nv[2]/2**20))
    for pool in g(mem,[3]):
        pf={f:v for f,w,v in fields(pool)}
        u=fields(pf[2]); uu={f:v for f,w,v in u}
        print('    pool %-20s used %6.0f MB committed %6.0f MB'%(pf[1].decode(), uu.get(1,0)/2**20, uu.get(2,0)/2**20))
    for gc in g(ps,[2]):
        gf={f:v for f,w,v in fields(gc)}
        st={f:v for f,w,v in fields(gf[2])} if 2 in gf else {}
        def d(x): return struct.unpack('<d',x)[0] if isinstance(x,bytes) else x
        if st: print('    GC %-22s count=%-5d avg pause=%6.2f ms  co ~%.1f s'%(gf[1].decode(), st.get(1,0), d(st.get(2,0)), d(st.get(3,0))/1000))
    up=g(ps,[3])
    if up: print('    uptime %.1f h'%(up[0]/3600000))
    for i,lbl in ((5,'mspt'),(4,'tps')):
        for sub in g(ps,[i]):
            vals=[struct.unpack('<d',v)[0] for f,w,v in fields(sub) if w==1]
            print('    %s: %s'%(lbl, ' '.join('%.2f'%x for x in vals)))
