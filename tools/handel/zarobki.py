#!/usr/bin/env python3
"""Zarobek na godzine gry wg profesji i poziomu (10/30/50) — z ceny.json i receptury.json.
Zbieracz spedza godzine na wezlach swojej profesji (cykl ze ZRODEL), opcjonalnie przetwarza
urobek w SWOIM warsztacie (receptury z bramka <= poziom, skladniki wylacznie z wlasnego zbioru).
Klikniecie receptury kosztuje 8 s (INTERAKCJA) zabrane z czasu zbierania.
Rynek = sprzedaz na aukcji po WARTOSCI minus oplaty (6% podatku + 1.5-2.5% wystawienia ~ 8.5%)."""
import json, importlib.util, os
H=os.path.dirname(os.path.abspath(__file__))
spec=importlib.util.spec_from_file_location('e',os.path.join(H,'ekonomia.py')); e=importlib.util.module_from_spec(spec)
import sys; sys.argv=['x','--cicho']; spec.loader.exec_module(e)
ceny=json.load(open(os.path.join(H,'ceny.json'))); rec=json.load(open(os.path.join(H,'receptury.json')))['recipes']
PROF={'woodworking':['wezel_dab','wezel_swierk','wezel_brzoza'],'herbalism':['wezel_ziola','wezel_ksiezyc','wezel_grzyby'],
 'farming':['wezel_poletko'],'jewelcrafting':['wezel_druza'],'building':['wezel_glina'],'cooking':['wezel_przyprawy'],
 'fishing':['polow'],'mining':['ruda_urobku','zyla_srebra','kamieniolom','zyla_klejnotow','druza_arkany'],'hunting':['bestie']}
FEE=0.915
def cena(i,rynek): return ceny[i]['wartosc']*FEE if rynek else ceny[i]['skup']
def godzina(src,prof,L,rynek):
    t,out=e.ZRODLA[src]; cykle=3600/t
    best=(sum(n*cena(i,rynek) for i,n in out.items())*cykle,'surowiec')
    for r in rec:
        if r['station']!='profession-'+prof or (r['lvl'] or 0)>L: continue
        o=r['out']['id']
        if o not in ceny or any(x['id'] not in out for x in r['ing']): continue
        # ile receptur na cykl: ogranicza najrzadszy skladnik
        k=min(out[x['id']]/x['amount'] for x in r['ing'])
        czas=t+k*e.INTERAKCJA            # cykl + klikanie receptur
        reszta=sum((out[i]-sum(x['amount']*k for x in r['ing'] if x['id']==i))*cena(i,rynek) for i in out)
        v=(k*r['out']['amount']*cena(o,rynek)+reszta)*3600/czas
        if v>best[0]: best=(v,f"{o[5:]} (L{r['lvl']})")
    return best
print(f"{'profesja':14} {'L':>3} | {'skup NPC $/h':>12} {'co':24} | {'aukcja $/h':>10} {'co':24}")
wyn={}
for p,srcs in PROF.items():
    for L in (10,30,50):
        n=max((godzina(s,p,L,False)+(s,)) for s in srcs); m=max((godzina(s,p,L,True)+(s,)) for s in srcs)
        wyn[(p,L)]=(n,m)
        print(f"{p:14} {L:3} | {n[0]:12.0f} {n[1][:24]:24} | {m[0]:10.0f} {m[1][:24]:24}")
# kontrola: receptury, w ktorych skup wyrobu < skup skladnikow (strata przy sprzedazy NPC)
print('\nReceptury, gdzie skup(wyrob) < skup(skladniki)  [tylko informacyjnie — to nie pętla]:')
for r in rec:
    o=r['out']['id']
    if o not in ceny or any(x['id'] not in ceny for x in r['ing']): continue
    wej=sum(ceny[x['id']]['skup']*x['amount'] for x in r['ing']); wyj=ceny[o]['skup']*r['out']['amount']
    if wyj<wej-1e-9: print(f"  {r['station'][11:]:14} L{r['lvl']:<3} {o:28} {wyj:7.2f} < {wej:7.2f}")
