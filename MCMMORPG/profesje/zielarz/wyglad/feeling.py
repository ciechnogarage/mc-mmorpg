#!/usr/bin/env python3
"""Ustawia zielarz.feeling (brak|subtelny|mmo|rytual) w config.yml Rzemiosla, nie ruszajac reszty pliku.
Uzycie: feeling.py <plugins/Rzemioslo/config.yml> <brak|subtelny|mmo|rytual>   potem: rzemioslo reload (albo restart).
brak = stary wyglad chwili zbioru (bez warstwy ZFeeling)."""
import re, sys
p, w = sys.argv[1], sys.argv[2].lower()
assert w in ("brak", "subtelny", "mmo", "rytual"), "feeling: brak|subtelny|mmo|rytual"
s = open(p, encoding="utf-8").read()
if re.search(r"(?m)^  feeling:.*$", s) and re.search(r"(?m)^zielarz:\s*$", s):
    s = re.sub(r"(?m)^  feeling:.*$", f"  feeling: {w}", s, count=1)
elif re.search(r"(?m)^zielarz:\s*$", s):
    s = re.sub(r"(?m)^zielarz:\s*$", f"zielarz:\n  feeling: {w}", s, count=1)
else:
    s = s.rstrip("\n") + f"\nzielarz:\n  feeling: {w}\n"
open(p, "w", encoding="utf-8").write(s)
print("zielarz.feeling:", w)
