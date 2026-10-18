#!/usr/bin/env python3
"""Sceny do nagran wycinka zielarza: steruje klientem QA (qa.sh) i serwerem (RCON) — test jak gracz, krok po kroku."""
import re, subprocess, sys, time, os
H = os.path.dirname(os.path.abspath(__file__))
def R(c): return subprocess.run(['docker','exec','klon-zielarz','rcon-cli',c],capture_output=True,text=True).stdout
def X(*a): subprocess.run([H+'/qa.sh','x',*a],capture_output=True)
def czekaj(s): time.sleep(s)
def okaz():
    t=R('rzemioslo okaz QaZielarz'); cz={}
    for m in re.finditer(r'(\w+?)(\d+)(x?)(\{[^}]*\})?',t.split(':',1)[1] if ':' in t else ''):
        typ,nr,wz,ce=m.groups(); d={}
        for kv in re.findall(r'(\w+)=([\w.]+)',ce or ''): d[kv[0]]=kv[1]
        cz[(typ,int(nr))]=dict(zabrana=bool(wz),**d)
    m=re.search(r'boczne=\[([^\]]*)\]',t); bocz=[v.strip()=='true' for v in m.group(1).split(',')] if m else []
    return cz,bocz
def celuj(typ,nr,y=0.5): R(f'rzemioslo celuj QaZielarz {typ} {nr} {y}'); czekaj(0.9)
def lpm(): X('click','1')
def ppm_klik(): X('click','3')
def trzymaj(s): X('mousedown','3'); czekaj(s); X('mouseup','3')
def wstan(): X('keydown','shift'); czekaj(0.3); X('keyup','shift')
def start(x,y,z,tp): R(f'minecraft:teleport QaZielarz {tp}'); czekaj(1.2); R(f'rzemioslo kepa-start QaZielarz {x} {y} {z}'); czekaj(1.8)

def dziurawiec():
    start(42,-59,42,'42.5 -59 38.6 0 18')
    cz,_=okaz(); pedy=[(nr,d) for (t,nr),d in cz.items() if t=='ped']
    dobre=[nr for nr,d in pedy if d.get('faza')=='kw' and d.get('chory')=='false']
    zle=[nr for nr,d in pedy if d.get('faza')!='kw' or d.get('chory')=='true']
    h={nr:float(d['h']) for nr,d in pedy}
    if dobre: celuj('ped',dobre[0],h[dobre[0]]-0.32); czekaj(0.4); lpm(); czekaj(2.0)
    if len(dobre)>1: celuj('ped',dobre[1],0.12); czekaj(0.4); lpm(); czekaj(2.0)        # za nisko: zdrewniala lodyga
    if zle: celuj('ped',zle[0],h[zle[0]]-0.32); czekaj(0.4); lpm(); czekaj(2.0)          # chory albo przekwitly
    reszta=[nr for nr,d in pedy if nr not in dobre[:2]+zle[:1]]
    if reszta: celuj('ped',reszta[0],0.08); czekaj(0.4); trzymaj(1.4); czekaj(1.6)      # wyrwanie z korzeniem
    wstan(); czekaj(2.5)

def kozlek(dobrze=True, kepa=(57,-59,41), tp='54.4 -59 41.5 -90 22'):
    start(*kepa,tp)
    _,bocz=okaz()
    if dobrze:
        for i,b in enumerate(bocz):
            if b: celuj('gleba',i); czekaj(0.3); lpm(); czekaj(1.4)
        celuj('rozeta',0,0.05); czekaj(0.3)
        for _ in range(4):
            trzymaj(1.5); czekaj(0.9)
            if not okaz()[0]: break
    else:
        celuj('rozeta',0,0.05); czekaj(0.3); trzymaj(3.2)
    czekaj(1.5)
    if okaz()[0]: wstan()
    czekaj(2.5)

def liscie(kepa=(42,-59,61), tp='42.5 -59 58.4 0 22'):
    start(*kepa,tp)
    celuj('lisc',-1); czekaj(0.3); ppm_klik(); czekaj(2.0)
    celuj('lisc',-1); czekaj(0.3); trzymaj(1.8); czekaj(2.2)
    celuj('lisc',-1); czekaj(0.3); ppm_klik(); czekaj(2.0)
    wstan(); czekaj(2.5)

if __name__=='__main__': globals()[sys.argv[1]](*sys.argv[2:])
