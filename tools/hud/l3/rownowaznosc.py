#!/usr/bin/env python3
"""Sprawdza warianty l3 (warianty.py) wobec zrodla: dla kazdego spojnego stanu gracza (efekty 0-4, zadanie, pet,
zemdlony, slugi 0-3) zbior widocznych elementow w biezacym wariancie == zbior widocznych w starym l3-kolumna-layout;
kolejnosc elementow w wariancie jak w zrodle; assety identyczne poza usunietymi warunkami polozenia.
Wymog: 'roznic 0', 'kolejnosc ok True', 'assety rozne ... 0'.

  python3 rownowaznosc.py zrodlo/l3.yml zrodlo/l3-kolumna.yml <katalog-wyjscia-warianty.py>
"""
import itertools
import re
import sys

def assets(path):
    out={}; order=[]; cur=None
    for ln in open(path, encoding='utf-8').read().split('\n'):
        m=re.match(r'^([A-Za-z0-9_.-]+):\s*$',ln)
        if m: cur=m.group(1); out[cur]=[ln]; order.append(cur); continue
        if cur is None: out.setdefault('__head__',[]).append(ln)
        else: out[cur].append(ln)
    return out,order
def conds(lines):
    return [l.strip()[2:] for l in lines if re.match(r'^    - ',l)]
def layout(path):
    txt=open(path,encoding='utf-8').read().split('\n')
    head=[];els=[];cur=None
    for ln in txt:
        m=re.match(r'^    ([A-Za-z0-9_.-]+):\s*$',ln)
        if m: cur={'name':m.group(1),'lines':[ln]}; els.append(cur); continue
        if cur is None: head.append(ln)
        else: cur['lines'].append(ln)
    for e in els:
        e['asset']=next(re.match(r'^\s+asset:\s*(\S+)',l).group(1) for l in e['lines'] if re.match(r'^\s+asset:',l))
    return head,els

SL, SA, OUT = sys.argv[1:4]
OA,_=assets(SA); _,OE=layout(SL)
NA,_=assets(OUT+'/hud_assets/hud/l3-kolumna.yml')
# nowy layout: wiele layoutow
cur=None; NL={}
for ln in open(OUT+'/layouts/l3.yml',encoding='utf-8').read().split('\n'):
    m=re.match(r'^([a-z0-9-]+):\s*$',ln)
    if m: cur=m.group(1); NL[cur]=[]; continue
    m=re.match(r'^    ([A-Za-z0-9_.-]+):\s*$',ln)
    if m and cur: NL[cur].append(m.group(1))
K=dict(l.split() for l in open(OUT+'/klucze.txt'))
STAN=('hud_uklad','hud_efekty_liczba','hud_zadanie_widac','chowance_aktywny_istnieje','chowance_aktywny_zemdlony','hud_slugi_liczba')
def ev(c,env):
    m=re.match(r'^placeholder\{ph=%([^%]+)%;(.*)\} (true|false) hide$',c)
    if not m: return None
    ph,args,out=m.groups()
    if ph not in STAN: return None
    a=dict(x.split('=',1) for x in args.split(';'))
    v=env[ph]
    if 'val' in a: r=str(v).lower()==a['val'].lower()
    else:
        o=a.get('o','=='); x=float(v); y=float(a['v'])
        r={'>=':x>=y,'>':x>y,'<':x<y,'<=':x<=y,'=':x==y,'==':x==y}[o]
    return r==(out=='true')
def vis(cs,env):
    return all(r is not False for r in (ev(c,env) for c in cs))
bad=0;n=0
for ef,zad,pet,zem,sl in itertools.product(range(5),('tak','nie'),('tak','nie'),('tak','nie'),range(4)):
    if pet=='nie' and zem=='tak': continue
    st=f"e{int(ef>0)}c{int(zad=='tak')}t{int(pet=='tak')+int(sl>0)}"
    env={'hud_uklad':st,'hud_efekty_liczba':ef,'hud_zadanie_widac':zad,'chowance_aktywny_istnieje':pet,'chowance_aktywny_zemdlony':zem if pet=='tak' else '','hud_slugi_liczba':sl}
    act=set(NL[K[st]])
    for e in OE:
        n+=1
        o=vis(conds(OA[e['asset']]),env)
        w=e['name'] in act and vis(conds(NA[e['asset']]),env)
        if o!=w: bad+=1; print('ROZNE',st,env,e['name'],o,w) if bad<10 else None
print('porownan',n,'roznic',bad)
# kolejnosc elementow w wariancie = kolejnosc w starym l3
order={e['name']:i for i,e in enumerate(OE)}
print('kolejnosc ok', all(NL[k]==sorted(NL[k],key=order.get) for k in NL))
# reszta assetu identyczna poza usunietymi warunkami
diffs=0
for k in OA:
    if k == '__head__': continue
    o=[l for l in OA[k] if not re.match(r'^    - placeholder\{ph=%hud_(uklad|zadanie_widac)%|^    - placeholder\{ph=%hud_efekty_liczba%;v=1;',l)]
    if o!=NA[k]: diffs+=1
print('assety rozne poza warunkami polozenia', diffs)
