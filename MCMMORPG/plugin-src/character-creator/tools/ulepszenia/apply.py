"""Nakłada nową sekcję scene + names + szybki pierwszy check na config.yml CharacterCreatora.
Użycie: python3 apply.py <config.yml>  (kopia .bak-ulepszenia obok)"""
import sys, shutil, os, re
p = sys.argv[1]; here = os.path.dirname(os.path.abspath(__file__))
s = open(p, encoding='utf-8').read()
shutil.copy(p, p + '.bak-ulepszenia')
i = s.index('\nscene:\n')
head = s[:i + 1]
tail = s[i + 1:]
m = re.search(r'\n(?=[A-Za-z])', tail[1:])  # nastepna sekcja po scene (jesli jest)
rest = tail[1 + m.start() + 1:] if m else ''
head = re.sub(r'^names:\n(?:[ #].*\n|\n)*', '', head, flags=re.M)
head = head.replace('first-check-delay-ticks: 100', 'first-check-delay-ticks: 10')
names = open(os.path.join(here, 'names.yml'), encoding='utf-8').read()
scene = open(os.path.join(here, 'scene.yml'), encoding='utf-8').read()
out = head + names + '\n' + scene + ('\n' + rest if rest else '')
open(p, 'w', encoding='utf-8').write(out)
print('ok', p)
