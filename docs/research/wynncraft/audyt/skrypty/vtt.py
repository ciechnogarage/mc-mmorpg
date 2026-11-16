import re,sys
t=open(sys.argv[1]).read()
lines=[];prev=''
for l in t.split('\n'):
    if '-->' in l or l.startswith(('WEBVTT','Kind:','Language:')) or not l.strip(): continue
    l=re.sub(r'<[^>]+>','',l).strip()
    if l and l!=prev: lines.append(l); prev=l
# dedupe rolling captions
out=[]
for l in lines:
    if out and l.startswith(out[-1]): out[-1]=l
    elif not out or l not in out[-1]: out.append(l)
print(' '.join(out))
