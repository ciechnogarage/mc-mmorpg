import json,os,sys
D=sys.argv[1]
L=lambda f: json.load(open(os.path.join(D,f)))
types=L('types.json'); anim=L('anim.json')['typy']; traf=L('traf.json'); dwie=L('dwie.json'); ks=L('ksiega.json')
BRON={'miecz':['SWORD','LONG_SWORD','THRUSTING_SWORD'],'sztylet':['DAGGER','GAUNTLET'],'luk':['BOW'],'kostur':['WAND','STAFF','STAVE'],'bulawa':['HAMMER'],'tarcza':['SHIELD'],'drzewcowa':['HALBERD','LANCE'],'kusza':['CROSSBOW','MUSKET'],'grimuar':['TOME'],'klinga':['KATANA'],'luk_dlugi':['GREATBOW'],'kostur_dwureczny':['GREATSTAFF'],'sierp':['SIERP'],'topor':['AXE','GREATAXE'],'wlocznia':['SPEAR'],'totem':['TALISMAN'],'mlot_dwureczny':['GREATHAMMER'],'miecz_dwureczny':['GREATSWORD'],'lancuch':['WHIP'],'filakterium':['FILAKTERIUM'],'kosa':['KOSA']}
grupa={t:g for g,ts in BRON.items() for t in ts}
kostur=['STAFF','GREATSTAFF']
NIEBRON={'ORNAMENT','ACCESSORY','ARMOR','TOOL','CONSUMABLE','MISCELLANEOUS','SKIN','GEM_STONE','BLOCK','MATERIAL','RING','GLOVES','BELT','NECKLACE','CHARM'}
# itemy
cnt={};th={}
for f in os.listdir(os.path.join(D,'items')):
  t=f[:-5].upper(); d=json.load(open(os.path.join(D,'items',f))) or {}
  cnt[t]=len(d); th[t]=sum(1 for v in d.values() if isinstance(v,dict) and isinstance(v.get('base'),dict) and v['base'].get('two-handed') is True)
def traf_rodzaj(t):
  if t in traf['bronie-mmoitems']: return traf['bronie-mmoitems'][t]
  mat=str(types.get(t,{}).get('display','')).split(':')[0]
  for b in traf['bronie']:
    if any(z in mat for z in b['zawiera']): return b['rodzaj']+'(mat)'
  return traf['bron-domyslna']+'(dom)'
NORM={'ciete':{'ciecie','topor'},'klute':{'pchniecie'},'obuchowe':{'obuch','piesc'}}
wszystkie=[t for t in types if t not in NIEBRON]
for x in list(anim)+ks['typy-broni']+dwie['typy-broni']:
  if x not in wszystkie: wszystkie.append(x)
rows=[];rozjazdy={}
def R(t,k,opis): rozjazdy.setdefault(k,[]).append(t+': '+opis)
for t in wszystkie:
  a=anim.get(t); ar=a.get('rodzaj') if a else None; aw=a.get('waga') if a else None
  tr=traf_rodzaj(t); trb=tr.split('(')[0]
  kd=t in ks['typy-dwureczne']; kb=t in ks['typy-broni']; kl=t in ks['typy-tylko-lewa']
  db=t in dwie['typy-broni']; g=grupa.get(t)
  par=types.get(t,{}).get('parent'); mat=str(types.get(t,{}).get('display','-')).split(':')[0]
  dis='disable-melee-attacks' in types.get(t,{})
  ciez = aw is not None and aw>=1.4
  rows.append((t,par or '-',mat,'%d/%d'%(th.get(t,0),cnt.get(t,0)) if t in cnt else '-', 'brak' if a is None else (ar or 'bez rodzaju')+('/ciezka' if ciez else ''), tr, 'tak' if db else '-', ('B' if kb else '-')+('2H' if kd else '')+('L' if kl else ''), g or '-', 'K' if t in kostur else '-', 'tak' if dis else '-'))
  # rozjazdy
  if ar and trb not in NORM.get(ar,set()): R(t,'rodzaj Anim vs Trafienia',f'{ar} vs {tr}')
  if ar is None and a is not None and not tr.endswith('(dom)'): R(t,'rodzaj Anim vs Trafienia',f'Anim bez rodzaju vs {tr}')
  if a is None and t in types and not dis and t not in ('BOW','GREATBOW','CATALYST','OFF_CATALYST','MAIN_CATALYST','LUTE','MUSKET','CROSSBOW'): R(t,'typ broni wrecz bez AnimacjeBroni',f'{mat}')
  if (t not in traf['bronie-mmoitems']) and a is not None: R(t,'brak w Trafienia bronie-mmoitems (zgaduje z materialu)',tr)
  if ciez != kd and a is not None: R(t,'dwurecznosc: Anim waga>=1.4 vs Ksiega typy-dwureczne',f'Anim ciezka={ciez} Ksiega 2H={kd}')
  if t in th and cnt.get(t,0)>0 and (th[t]==cnt[t])!=kd and (th[t]>0 or kd): R(t,'dwurecznosc: flaga MMOItems vs Ksiega',f'{th[t]}/{cnt[t]} z two-handed, Ksiega 2H={kd}')
  if db and kd: R(t,'DwieBronie pozwala w lewej, Ksiega dwureczna','')
  if a is not None and ar in ('ciete','klute','obuchowe') and not kd and not db and t not in ('SHIELD',): R(t,'jednoreczna bron wrecz nie w DwieBronie typy-broni',f'Anim {ar}, Ksiega 1H')
  if db and not kb: R(t,'DwieBronie bron, Ksiega nie-bron','')
  if t in types and not kb and not dis and t not in NIEBRON: R(t,'typ MMOItems poza Ksiega typy-broni','')
  if (kb or a) and g is None and t not in('LUTE','CATALYST','MAIN_CATALYST','OFF_CATALYST'): R(t,'bez grupy bieglosci',f'Ksiega bron={kb} Anim={a is not None}')
  if t not in types: R(t,'typ w configu, nie ma w item-types.yml','')
  if dis and a is not None and ar: R(t,'MMOItems disable-melee-attacks, a Anim ma rodzaj ciosu wrecz',ar)
hdr=('typ','parent','material','2H flaga/itemy','AnimacjeBroni rodzaj','Trafienia rodzaj','DwieBronie lewa','Ksiega','grupa bieglosci','Kostur(Zaklecia)','MMOI disable-melee')
with open(os.path.join(D,'macierz.md'),'w') as f:
  f.write('| '+' | '.join(hdr)+' |\n|'+'---|'*len(hdr)+'\n')
  for r in rows: f.write('| '+' | '.join(r)+' |\n')
  f.write('\nKsiega: B=typy-broni, 2H=typy-dwureczne, L=tylko-lewa. Trafienia (mat)=zgadywane z materialu, (dom)=bron-domyslna.\n\n## Rozjazdy\n')
  for k,v in rozjazdy.items(): f.write(f'\n### {k} ({len(v)})\n'+'\n'.join('- '+x for x in v)+'\n')
print('typow',len(rows)); 
for k,v in rozjazdy.items(): print(len(v),k,'::',v)
