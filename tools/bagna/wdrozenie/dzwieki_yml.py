#!/usr/bin/env python3
"""Wpisy bagna:* w plugins/Nexo/sounds.yml. Uzycie: dzwieki_yml.py dodaj <sounds.yml> <sounds.yml.bagna> | usun <sounds.yml>"""
import re, sys
p = sys.argv[2]; s = open(p).read()
Z0, Z1 = "# BAGNA (tools/bagna/wdrozenie)\n", "# BAGNA koniec\n"
if sys.argv[1] == "dodaj" and Z0 not in s:
    wpisy = open(sys.argv[3]).read().split("sounds:\n", 1)[1]
    blok = Z0 + wpisy + Z1
    if re.search(r"^sounds: \[\]\s*$", s, re.M):
        s = re.sub(r"^sounds: \[\]\s*$", "sounds:\n" + blok.rstrip("\n"), s, count=1, flags=re.M)
    else:
        s = s.replace("\nsounds:\n", "\nsounds:\n" + blok, 1)
elif sys.argv[1] == "usun":
    s = re.sub(re.escape(Z0) + r".*?" + re.escape(Z1), "", s, flags=re.S)
open(p, "w").write(s); print(sys.argv[1], "OK:", p)
