import re,sys
def clean(t):
    t=re.sub(r'<ref[^>]*>.*?</ref>','',t,flags=re.S); t=re.sub(r'<[^>]+>','',t)
    t=re.sub(r'\[\[(?:File|Image):[^\]]*\]\]','',t)
    t=re.sub(r'\[\[([^|\]]*\|)?([^\]]*)\]\]',r'\2',t); t=re.sub(r"'''?",'',t)
    t=re.sub(r'\{\{[^{}]*\}\}','',t); t=re.sub(r'\{\{[^{}]*\}\}','',t)
    return t
f=sys.argv[1]; n=int(sys.argv[2]) if len(sys.argv)>2 else 3000
t=clean(open('api/wiki/'+f).read())
t=re.sub(r'\n{3,}','\n\n',t)
print(t[:n])
