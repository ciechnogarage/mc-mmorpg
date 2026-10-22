#!/usr/bin/env bash
# Podglad Kosiarza bez kikutow (w grze schowane do odciecia): podglad.sh OUT.png
set -euo pipefail
cd /opt/docker/mc/data/world1/output/trafienia/kosiarz
python3 - <<'PY'
import json
d=json.load(open('l004_kosiarz.bbmodel')); usun=set()
def w(n):
    out=[]
    for c in n:
        if isinstance(c,dict):
            if c['name'].startswith('kikut_'):
                def zb(x):
                    for y in x['children']:
                        (usun.add(y) if isinstance(y,str) else zb(y))
                zb(c); continue
            c['children']=w(c['children'])
        out.append(c)
    return out
d['outliner']=w(d['outliner']); d['elements']=[e for e in d['elements'] if e['uuid'] not in usun]
json.dump(d,open('_podglad.bbmodel','w'))
PY
/home/dev/Desktop/Przemek/MC/tools/bbrender/render.sh "$1" _podglad.bbmodel --kolumny 1 --rozmiar 520 >/dev/null 2>&1; rm -f _podglad.bbmodel
