import json,sys,urllib.request,urllib.parse,time,re
UA={'User-Agent':'WynnAuditResearch/1.0'}
def get(u):
    for i in range(3):
        try: return json.load(urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=60))
        except Exception as e: time.sleep(4)
    return {}
def flat(nodes,out,depth=0):
    for n in nodes:
        d=n.get('data',n)
        if d.get('body'): out.append((d.get('score',0),depth,d['body']))
        rep=d.get('replies')
        if isinstance(rep,dict): rep=rep.get('data',{}).get('children',[])
        if isinstance(rep,list): flat(rep,out,depth+1)
for pid in sys.argv[1:]:
    p=get('https://arctic-shift.photon-reddit.com/api/posts/ids?ids='+pid).get('data',[{}])
    p=p[0] if p else {}
    t=get('https://arctic-shift.photon-reddit.com/api/comments/tree?link_id=t3_'+pid+'&limit=300')
    out=[]; flat(t.get('data',[]),out)
    out.sort(key=lambda x:-(x[0] or 0))
    with open(f'post_{pid}.txt','w') as f:
        f.write(f"# {p.get('title')} (score {p.get('score')}, {p.get('num_comments')} kom.) https://www.reddit.com/r/WynnCraft/comments/{pid}/\n{(p.get('selftext') or '')[:3000]}\n\n")
        for s,dp,b in out[:25]: f.write(f"[{s}] {'  '*dp}{b[:600]}\n")
    print(pid,len(out)); time.sleep(1.5)
