#!/usr/bin/env python3
"""Ustawia komunikaty.zielarz-wariant (v7|a|b|c) w config.yml Rzemiosla, nie ruszajac reszty pliku.
Uzycie: wariant.py <plugins/Rzemioslo/config.yml> <v7|a|b|c>   potem: rzemioslo reload (albo restart)."""
import re, sys
p, w = sys.argv[1], sys.argv[2].lower()
assert w in ("v7", "a", "b", "c"), "wariant: v7|a|b|c"
s = open(p, encoding="utf-8").read()
if re.search(r"(?m)^  zielarz-wariant:.*$", s):
    s = re.sub(r"(?m)^  zielarz-wariant:.*$", f"  zielarz-wariant: {w}", s)
elif re.search(r"(?m)^komunikaty:\s*$", s):
    s = re.sub(r"(?m)^komunikaty:\s*$", f"komunikaty:\n  zielarz-wariant: {w}", s, count=1)
else:
    s = s.rstrip("\n") + f"\nkomunikaty:\n  zielarz-wariant: {w}\n"
open(p, "w", encoding="utf-8").write(s)
print("zielarz-wariant:", w)
