#!/usr/bin/env bash
# Usuwa swiaty testowe z dysku i z bukkit.yml (inaczej Iris wczytuje je przy KAZDYM starcie).
# Serwer musi byc zatrzymany. Zostawia: world*, pr2, pr3, ctl.
set -e
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
D=$DATA
python3 - "$DATA/bukkit.yml" <<'EOF'
import re, sys
p=sys.argv[1]; s=open(p).read()
keep={'pr2','pr3','ctl'}
names=re.findall(r'^  ([A-Za-z0-9_]+):\n    generator: \S+\n', s, re.M)
for n in names:
    if n not in keep:
        s=re.sub(r'^  %s:\n    generator: \S+\n' % re.escape(n), '', s, flags=re.M)
open(p,'w').write(s)
print("usuniete z bukkit.yml:", [n for n in names if n not in keep])
EOF
for w in "$D"/s[0-9]*_* "$D"/soak_* "$D"/dbg* "$D"/w01*; do [ -d "$w" ] && rm -rf "$w"; done
true
