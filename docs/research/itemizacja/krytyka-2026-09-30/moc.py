import sys,random
sys.path.insert(0,'.')
from tresc import *

TIERY_AFF=[('T3',2,0.08),('T2',1.5,0.30),('T1',1,1.0)]
def losuj(poj,pref,suf,rnd):
    wynik=[];grupy=[(pref,2),(suf,2)];rnd.shuffle(grupy)
    for rod,mx in grupy:
        rod=rod[:];rnd.shuffle(rod);z=0
        for f in rod:
            if z>=mx:break
            if rnd.random()>0.7:continue
            z+=1
            for tn,w,ch in TIERY_AFF:
                if rnd.random()<=ch and w<=poj+1e-9:
                    poj-=w;wynik.append((f,tn));break
    return wynik
rnd=random.Random(1)
HP={'woj':(24,0.6),'nin':(20,0.404),'mag':(18,0.354),'ako':(22,0.505)}
def baza(L): return max(i for i,p in enumerate(POZIOMY) if p<=L)
def affstats(arch,kat,L,rar):
    cb,cs={'ZWYKLY':(0,0),'MAGICZNY':(2.0,0.02),'RZADKI':(4.0,0.1)}[rar]
    if rar=='ZWYKLY': return {}
    rod=PULE[arch][kat]
    pref=[f for f in rod if AFFIKSY[f][0]=='P'];suf=[f for f in rod if AFFIKSY[f][0]=='S']
    out={}
    for f,tn in losuj(cb+cs*L,pref,suf,rnd):
        typ,formy,stat,wart,sc=AFFIKSY[f];t=int(tn[1])
        v=wart[t-1]+sc*(1+0.5*(t-1))*(L-1)
        out[stat]=out.get(stat,0)+v
    return out
def kit(arch,L,rar,N=400):
    tot={}
    i=baza(L);mn=MNOZNIK_BAZY[i]
    for _ in range(N):
        s={}
        def add(d,m=1):
            for k,v in d.items(): s[k]=s.get(k,0)+v*m
        waga,dye,trim,impl,nazwy=ZBROJE[arch][i]
        for sid,mat,udz in SLOTY_ZB:
            v1,sc=WAGA_PANCERZ[waga]; add({'armor':(v1+sc*(L-1))*udz*mn})
            add({k:(a+b*(L-1))*mn for k,(a,b) in impl.items()})
            add(affstats(arch,'zbroja',L,rar))
        rod=BRON_ARCH[arch][0][-1]
        typ,d1,ds,spd,dwu,extra,mats,opis=RODZAJE[rod]
        if d1: add({'weapon':(d1+ds*(L-1))*mn})
        add({k:(a+b*(L-1))*mn for k,(a,b) in extra.items() if k!='arrow-velocity'})
        add(affstats(arch,'bron',L,rar))
        for t in AKC_TYPY:
            add({k:(a+b*(L-1))*mn for k,(a,b) in AKC_IMPL[t][arch].items()})
            add(affstats(arch,'ozdoba',L,rar))
        for k,v in s.items(): tot[k]=tot.get(k,0)+v/N
    return tot
keys=['max-health','defense','armor','weapon','physical-damage','magic-damage','skill-damage','critical-strike-chance','lifesteal','dodge-rating','max-mana']
for arch in ARCH:
  for L in (1,5,10):
    hb=HP[arch][0]+HP[arch][1]*(L-1)
    for rar in ('ZWYKLY','MAGICZNY','RZADKI'):
      k=kit(arch,L,rar)
      print(f"{arch} L{L:2} {rar:8} HPbaza {hb:5.1f} HPgear {k.get('max-health',0):5.1f} ({100*k.get('max-health',0)/hb:4.0f}%) def {k.get('defense',0):5.1f} arm {k.get('armor',0):5.1f} "+' '.join(f"{x[:6]}={k.get(x,0):.1f}" for x in keys[3:] if k.get(x)))
