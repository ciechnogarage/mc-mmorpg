import sys
def rv(b,i):
    r=0;s=0
    while True:
        x=b[i];i+=1;r|=(x&0x7f)<<s;s+=7
        if not x&0x80: return r,i
def fields(b):
    i=0;out=[]
    while i<len(b):
        k,i=rv(b,i);f=k>>3;w=k&7
        if w==0: v,i=rv(b,i)
        elif w==1: v=b[i:i+8];i+=8
        elif w==2:
            l,i=rv(b,i);v=b[i:i+l];i+=l
        elif w==5: v=b[i:i+4];i+=4
        else: raise ValueError('wire %d'%w)
        out.append((f,w,v))
    return out
def top(path):
    b=open(path,'rb').read()
    return b,fields(b)
if __name__=='__main__':
    b,fs=top(sys.argv[1])
    from collections import Counter
    c=Counter((f,w) for f,w,_ in fs)
    print('top-level:',c)
    for f,w,v in fs[:3]:
        if w==2:
            print(f,'len',len(v))
            try:
                for sf,sw,sv in fields(v)[:12]:
                    print('  ',sf,sw,(sv[:60] if isinstance(sv,bytes) else sv))
            except Exception as e: print('  err',e)
