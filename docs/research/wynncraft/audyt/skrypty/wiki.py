import json, sys, urllib.request, urllib.parse, time, os
UA={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) WynnAuditResearch/1.0'}
def get(page):
    q=urllib.parse.urlencode({'action':'parse','page':page,'prop':'wikitext','format':'json','redirects':1})
    d=json.load(urllib.request.urlopen(urllib.request.Request('https://wynncraft.wiki.gg/api.php?'+q,headers=UA),timeout=30))
    if 'error' in d: return None
    return d['parse']['title'], d['parse']['wikitext']['*']
for p in sys.argv[1:]:
    fn=p.replace('/','_').replace(' ','_')+'.txt'
    if os.path.exists(fn): continue
    try:
        r=get(p)
        if r: open(fn,'w').write('TITLE: '+r[0]+'\n'+r[1]); print(p,'->',r[0],len(r[1]))
        else: print(p,'MISSING')
    except Exception as e: print(p,'ERR',e)
    time.sleep(0.7)
