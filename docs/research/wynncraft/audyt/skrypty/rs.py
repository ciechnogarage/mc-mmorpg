import json,sys,urllib.request,urllib.parse,time
UA={'User-Agent':'WynnAuditResearch/1.0'}
def s(q, after='2025-01-01', limit=100):
    u='https://arctic-shift.photon-reddit.com/api/posts/search?'+urllib.parse.urlencode({'subreddit':'WynnCraft','title':q,'after':after,'limit':limit,'sort':'desc'})
    for i in range(3):
        try: return json.load(urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=60))['data']
        except Exception as e: time.sleep(3)
    return []
out={}
for q in sys.argv[1:]:
    d=s(q); out[q]=[{k:p.get(k) for k in ('id','title','score','num_comments','created_utc','selftext','permalink')} for p in d]
    print(q, len(d)); time.sleep(1.5)
json.dump(out,open('posts_'+str(int(time.time()))+'.json','w'),ensure_ascii=False)
