#!/usr/bin/env python3
"""Polskie komunikaty rzucania umiejętności MMOCore (messages.yml, sekcja casting:). Etap 3, 01.10.
Uruchomienie z katalogu world1: python3 tools/klasy-rasowe/komunikaty/tlumacz_rzucanie.py, potem `mmocore reload`."""
import os, re
p = os.path.join('/opt/docker/mc/data/world1', 'plugins', 'MMOCore', 'messages.yml')
t = open(p, encoding='utf-8').read()
NOWE = {
    'no-mana': "'&cBrakuje Ci {mana}: potrzeba jeszcze {mana-required}.' # {skill}, {mana-cost}",
    'no-stamina': "'&cBrakuje Ci staminy: potrzeba jeszcze {stamina-required}.' # {skill}, {stamina-cost}",
    'on-cooldown': "'&cTa umiejętność odnawia się jeszcze {cooldown} s.' # {skill}",
}
# tylko w sekcji casting: — 'on-cooldown' występuje też wcześniej w innych sekcjach (sucha próba 01.10)
a = t.index('\ncasting:\n') + 1
b = re.search(r'^\S', t[a + len('casting:\n'):], re.M)
b = a + len('casting:\n') + (b.start() if b else len(t))
sekcja = t[a:b]
for k, v in NOWE.items():
    sekcja, n = re.subn(r'^(  ' + re.escape(k) + r': ).*$', lambda m: m.group(1) + v, sekcja, count=1, flags=re.M)
    assert n == 1, k
t = t[:a] + sekcja + t[b:]
open(p, 'w', encoding='utf-8').write(t)
print('przetłumaczono:', ', '.join(NOWE))
