import json,sys
d=json.load(open('sonda.json'))
def unnbt(x):
    if isinstance(x,dict) and 'type' in x and 'value' in x:
        t=x['type'];v=x['value']
        if t=='compound': return {k:unnbt(vv) for k,vv in v.items()}
        if t=='list':
            inner=v.get('value',[]) if isinstance(v,dict) else v
            return [unnbt({'type':v['type'],'value':i}) if isinstance(v,dict) and 'type' in v else unnbt(i) for i in inner]
        return v
    if isinstance(x,dict): return {k:unnbt(vv) for k,vv in x.items()}
    if isinstance(x,list): return [unnbt(i) for i in x]
    return x
def txt(c,inh=None):
    out=[]
    col=c.get('color',inh) if isinstance(c,dict) else inh
    if isinstance(c,str): return [(inh,c)]
    if c.get('text'): 
        f=[k for k in ('bold','italic','underlined','strikethrough','obfuscated') if c.get(k)==1]
        out.append((col+('+'+'+'.join(f) if f else ''),c['text']))
    for e in c.get('extra',[]) or []: out+=txt(e,col)
    return out
for it in d:
    print('=====',it['id'],it.get('poz'),it.get('tier'),it.get('odp'))
    for s in it['surowe']:
        if s['type']=='custom_name':
            print('NAME:', ' | '.join(f'[{c}]{t}' for c,t in txt(unnbt(s['data']))))
        elif s['type']=='lore':
            for l in s['data']:
                print('  ', ' '.join(f'[{c}]{t!r}' for c,t in txt(unnbt(l),'dark_purple')))
        else:
            print('  COMP',s['type'], json.dumps(unnbt(s['data']),ensure_ascii=False)[:200])
