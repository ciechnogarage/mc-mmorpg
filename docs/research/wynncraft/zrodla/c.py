import json,sys,urllib.request,datetime
def get(u):
    return json.load(urllib.request.urlopen(urllib.request.Request(u,headers={'User-Agent':'research'}),timeout=60))
for pid in sys.argv[1:]:
    p=get(f"https://arctic-shift.photon-reddit.com/api/posts/ids?ids={pid}")['data'][0]
    print("=====",pid,datetime.datetime.utcfromtimestamp(p['created_utc']).date(),p['score'],p['title'],'|',p.get('url'))
    print((p.get('selftext') or '')[:2500])
    try:
        t=get(f"https://arctic-shift.photon-reddit.com/api/comments/tree?link_id=t3_{pid}&limit=200")['data']
    except Exception as e: print('ERR',e); continue
    def walk(n,d=0):
        c=n.get('data',n)
        if 'body' in c: print('  '*d+f"- [{c.get('score')}] {c.get('author')}: "+c['body'].replace('\n',' ')[:500])
        for ch in (c.get('children') or []):
            if isinstance(ch,dict): walk(ch,d+1)
    for n in t: walk(n)
