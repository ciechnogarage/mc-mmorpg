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
    if dobre: celuj('ped',dobre[0],h[dobre[0]]*0.42); czekaj(0.4); lpm(); czekaj(2.0)
    if len(dobre)>1: celuj('ped',dobre[1],h[dobre[1]]*0.1); czekaj(0.4); lpm(); czekaj(2.0)        # za nisko: zdrewniala lodyga
    if zle: celuj('ped',zle[0],h[zle[0]]*0.42); czekaj(0.4); lpm(); czekaj(2.0)          # chory albo przekwitly
    reszta=[nr for nr,d in pedy if nr not in dobre[:2]+zle[:1]]
    if reszta: celuj('ped',reszta[0],0.08); czekaj(0.4); trzymaj(1.2); czekaj(1.6)      # nagina sie, puszczony wraca
    if reszta: celuj('ped',reszta[0],0.08); czekaj(0.4); trzymaj(2.4); czekaj(1.6)      # trzymany dlugo: wyrwany
    wstan(); czekaj(2.5)

def kozlek(dobrze=True, kepa=(57,-59,41), tp='54.4 -59 41.5 -90 22'):
    start(*kepa,tp)
    _,bocz=okaz()
    if dobrze:
        for i,b in enumerate(bocz):
            if b: celuj('gleba',i); czekaj(0.3); lpm(); czekaj(1.4)
        celuj('rozeta',0,0.05); czekaj(0.3)
        # gracz patrzy na roslinie: drzy mocno (napiecie > 0,8) = puszcza; uspokaja sie (< 0,5) = chwyta znowu
        X('mousedown','3'); trzyma=True; t0=time.time()
        while time.time()-t0 < 14:
            m=re.search(r'napiecie=([0-9.]+)', R('rzemioslo okaz QaZielarz'))
            if not m: break
            n=float(m.group(1))
            if trzyma and n>0.8: X('mouseup','3'); trzyma=False
            elif not trzyma and n<0.5: X('mousedown','3'); trzyma=True
            czekaj(0.08)
        X('mouseup','3')
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

def kozlek_zle(): kozlek(False,(57,44),'54.4 -59 44.5 -90 22')

def eq():
    """Sloty z przedmiotami MMOItems gracza: {slot: (id, ilosc, jakosc)}."""
    t=R('data get entity QaZielarz Inventory'); w={}
    for m in re.finditer(r'\{count: (\d+), Slot: (\d+)b(.*?)id: "minecraft:[a-z_]+"\}', t):
        tid=re.search(r'MMOITEMS_ITEM_ID: "(\w+)"', m.group(3)); jq=re.search(r'"rzemioslo:jakosc": (\d)', m.group(3))
        if tid: w[int(m.group(2))]=(tid.group(1), int(m.group(1)), int(jq.group(1)) if jq else None)
    return w

def suszarnia():
    """Jak gracz: ziele z plecaka na pasek, PPM na belke suszarni, czekanie, zdjecie."""
    przed=eq(); print('przed', przed)
    ziele=[(s,v) for s,v in przed.items() if v[0]=='PROF_MEDICINAL_HERB']
    if not ziele: print('BRAK ZIELA'); return
    s,v=max(ziele,key=lambda x:x[1][1])
    R(f'item replace entity QaZielarz hotbar.4 from entity QaZielarz {"hotbar."+str(s) if s<9 else "inventory."+str(s-9)}')
    if s!=4: R(f'item replace entity QaZielarz {"hotbar."+str(s) if s<9 else "inventory."+str(s-9)} with air')
    R('minecraft:teleport QaZielarz 62.5 -59 56.2 facing 62.5 -55.6 58.5'); czekaj(1.5)
    X('key','5'); czekaj(0.5); ppm_klik(); czekaj(1.5)
    print('powieszone', v); ppm_klik(); czekaj(1.5)          # za wczesnie: jeszcze wilgotne
    czekaj(122); ppm_klik(); czekaj(1.5)
    po=eq(); print('po', po)

if __name__=='__main__': globals()[sys.argv[1]](*sys.argv[2:])
