# Wyciaga graf receptur ze stacji MMOItems + nazwy przedmiotow PROF_* -> JSON.
import yaml, glob, json, re, os
P='/w/plugins/MMOItems'
items={}
for f in glob.glob(P+'/item/*.yml'):
    typ=os.path.basename(f)[:-4].upper()
    d=yaml.safe_load(open(f)) or {}
    for k,v in d.items():
        if not isinstance(v,dict): continue
        b=v.get('base',{})
        items[k]={'type':typ,'name':b.get('name',''),'material':b.get('material',''),
                  'model':b.get('item-model') or b.get('model') or '', 'tier':b.get('tier',''),
                  'lore':b.get('lore',[]), 'keys':list(b.keys())}
rec=[]
def parse_ing(s):
    m=re.match(r"(\w+)\{(.*)\}",s)
    if not m: return {'raw':s}
    kv=dict(x.split('=',1) for x in m.group(2).split(',') if '=' in x)
    return {'kind':m.group(1),'id':kv.get('id'),'type':kv.get('type'),'amount':int(kv.get('amount',1)),'vanilla':kv.get('item') or kv.get('type')}
for f in sorted(glob.glob(P+'/crafting-stations/*.yml')):
    st=os.path.basename(f)[:-4]
    d=yaml.safe_load(open(f)) or {}
    for rid,r in (d.get('recipes') or {}).items():
        out=r.get('output','')
        if isinstance(out,dict): o={'id':out.get('id'),'amount':out.get('amount',1),'type':out.get('type')}
        else:
            m=re.search(r"id=(\w+)",str(out)); a=re.search(r"amount=(\d+)",str(out))
            o={'id':m.group(1) if m else str(out),'amount':int(a.group(1)) if a else 1}
        lvl=None;prof=None
        for c in r.get('conditions',[]) or []:
            m=re.search(r"profession=(\w+),level=(\d+)",str(c))
            if m: prof,lvl=m.group(1),int(m.group(2))
        exp=0
        for t in r.get('triggers',[]) or []:
            m=re.search(r"amount=(\d+)",str(t)); exp+=int(m.group(1)) if m else 0
        rec.append({'station':st,'rid':rid,'out':o,'prof':prof,'lvl':lvl,'time':r.get('crafting-time',0),
                    'ing':[parse_ing(str(i)) for i in r.get('ingredients',[]) or []],'exp':exp,'rawout':str(out)[:120]})
json.dump({'items':items,'recipes':rec},open('/o/receptury.json','w'),ensure_ascii=False,indent=1)
print(len(items),'items',len(rec),'recipes')
for r in rec:
    ing=' + '.join(f"{i.get('amount')}x{i.get('id') or i.get('raw')}" for i in r['ing'])
    print(f"{r['station'][:22]:22} L{r['lvl']!s:>3} t{r['time']!s:>3} xp{r['exp']:>4}  {r['out']['amount']}x{r['out']['id']} <= {ing}")
