import json,sys,urllib.request,urllib.parse,datetime
q=sys.argv[1]
rngs=[('2024-11-01','2025-01-01'),('2025-01-01','2025-07-01'),('2025-07-01','2026-01-01'),('2026-01-01','2026-09-30')]
for a,b in rngs:
  u=f"https://arctic-shift.photon-reddit.com/api/comments/search?subreddit=WynnCraft&body={urllib.parse.quote(q)}&limit=100&after={a}&before={b}"
  try: d=json.load(urllib.request.urlopen(urllib.request.Request(u,headers={'User-Agent':'r'}),timeout=120))['data']
  except Exception as e: print(a,'ERR',e); continue
  for c in d:
    print(datetime.datetime.utcfromtimestamp(c['created_utc']).date(),c['link_id'],c.get('score'),c['author'],':',c['body'].replace('\n',' ')[:450])
