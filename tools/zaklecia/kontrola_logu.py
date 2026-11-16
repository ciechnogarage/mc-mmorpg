#!/usr/bin/env python3
"""Czyta fragment logu po wdrozeniu (stdin): bledy dotyczace zaklec + stan packa. Kod wyjscia 1 = blad."""
import re, sys
linie = [re.sub(r'\x1b\[[0-9;]*m|§.', '', l) for l in sys.stdin]
bloki, zle = [], 0
for i, l in enumerate(linie):
    if 'Configuration Error' in l or 'internal error' in l or 'Exception' in l:
        okno = ''.join(linie[i:i + 6])
        if re.search(r'Zaklecia\.yml|Zak_|ZAKLECIE_|zakZasob|zakMaZasob|zaklecia', okno):
            zle += 1; bloki.append(okno)
pack = [l.strip() for l in linie if 'ResourcePack has been uploaded' in l or 'Finished generating resourcepack' in l]
reload = any('finished reloading' in l.lower() for l in linie)
print(f'MythicMobs przeladowany: {"tak" if reload else "NIE"}')
print(f'bledy zaklec: {zle}'); [print('  ' + b.replace('\n', '\n  ')) for b in bloki[:5]]
print('pack:', pack[-1] if pack else '(bez przeladowania packa)')
sys.exit(1 if zle or not reload else 0)
