#!/usr/bin/env python3
"""Wpisy Bagien w plugins/PaskiMobow/config.yml (tekstowo, z zachowaniem komentarzy).
Uzycie: paski.py dodaj|usun <config.yml> [mm/paskimobow_bagna.yml]"""
import re, sys
tryb, p = sys.argv[1], sys.argv[2]
s = open(p).read()
ZNACZNIK = "# BAGNA"
def lista(s, klucz, elementy, dodaj):
    m = re.search(rf"^{klucz}: \[(.*?)\]", s, re.M)
    assert m, f"brak listy {klucz} w {p}"
    el = [x.strip() for x in m.group(1).split(",") if x.strip()]
    for e in elementy:
        if dodaj and e not in el: el.append(e)
        if not dodaj and e in el: el.remove(e)
    return s[:m.start()] + f"{klucz}: [{', '.join(el)}]" + s[m.end():]
W, P = ["'BAGNA_*'"], ["'BAGNA_P_*'", "'BAGNA_T_*'", "'BAGNA_KOKON'"]
if tryb == "dodaj":
    wpisy = [l for l in open(sys.argv[3]) if l.startswith("  BAGNA_")]
    s = lista(s, "wzorce", W, True); s = lista(s, "pomin", P, True)
    if ZNACZNIK not in s:
        s = s.replace("\nmoby:\n", "\nmoby:\n  " + ZNACZNIK + " (tools/bagna/wdrozenie)\n" + "".join(wpisy) + "  " + ZNACZNIK + " koniec\n", 1)
else:
    s = lista(s, "wzorce", W, False); s = lista(s, "pomin", P, False)
    s = re.sub(rf"  {ZNACZNIK} \(tools/bagna/wdrozenie\)\n.*?  {ZNACZNIK} koniec\n", "", s, flags=re.S)
open(p, "w").write(s)
print(tryb, "OK:", p)
