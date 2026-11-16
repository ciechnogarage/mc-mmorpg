#!/usr/bin/env python3
"""Zlicza z logs/latest.log nagrody miedzy znacznikami ZNACZNIK-START/KONIEC (prawda z serwera, nie z bota)."""
import re, sys
log = open('/opt/docker/mc/data/world1/logs/latest.log', encoding='utf-8', errors='replace').read().splitlines()
nicks = sys.argv[1:]
cur = None; rows = []
for l in log:
    m = re.search(r'ZNACZNIK-START (\S+)', l)
    if m and any(n in l for n in nicks):
        cur = {'proba': m.group(1), 'give': {n: [] for n in nicks}, 'exp': {n: [] for n in nicks}}; continue
    if cur is None: continue
    m = re.search(r'ZNACZNIK-KONIEC \S+ zyje=(\w+)', l)
    if m:
        cur['zyje'] = m.group(1); rows.append(cur); cur = None; continue
    m = re.search(r'Successfully gave (.+?) to (\S+?)\.$', l)
    if m and m.group(2) in nicks: cur['give'][m.group(2)].append(m.group(1))
    m = re.search(r'(\S+) now has ([0-9.,]+) EXP in (\S+)', l)
    if m and m.group(1) in nicks: cur['exp'][m.group(1)].append(m.group(2))
for r in rows:
    print(r['proba'].ljust(28), ' | '.join(f"{n}: {len(r['give'][n])} wydan {r['give'][n]} expgive={len(r['exp'][n])}" for n in nicks), '| wezel po:', r.get('zyje'))
