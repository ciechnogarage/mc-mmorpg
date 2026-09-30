"""Naprawa kreatora 2026-09-30 (agent KREATOR): config.yml CharacterCreatora.
Uzycie: python3 apply.py <config.yml>   (kopia <config.yml>.bak-naprawa obok)
- names: wieksza pula sylab + names.blocked (filtr imion),
- scene.flyover: przywrocony przelot po stworzeniu postaci (zniknal przy cofnieciu 'proste' 30.09 rano),
- scene.fade: krotsze sciemnienie (zmierzone na klonie, patrz README).
Idempotentne: drugi przebieg nic nie zmienia."""
import sys, shutil, os, re
p = sys.argv[1]; here = os.path.dirname(os.path.abspath(__file__))
s = open(p, encoding='utf-8').read()
if not os.path.exists(p + '.bak-naprawa'):
    shutil.copy(p, p + '.bak-naprawa')
# names: cala sekcja najwyzszego poziomu
names = open(os.path.join(here, 'names.yml'), encoding='utf-8').read().rstrip('\n') + '\n'
s = re.sub(r'^names:\n(?:[ #].*\n|\n)*', '', s, flags=re.M)
i = s.index('\nscene:\n')
s = s[:i + 1] + names + '\n' + s[i + 1:]
# flyover: na koniec sekcji scene (scene jest ostatnia w pliku), bez duplikatu
if not re.search(r'^  flyover:\n', s, re.M):
    fly = open(os.path.join(here, 'flyover.yml'), encoding='utf-8').read().rstrip('\n') + '\n'
    s = s.rstrip('\n') + '\n' + fly
# fade
FADE = {'in-ticks': 3, 'stay-ticks': 4, 'out-ticks': 6}
for k, v in FADE.items():
    s = re.sub(rf'^(    {k}: )\d+', rf'\g<1>{v}', s, count=1, flags=re.M)
open(p, 'w', encoding='utf-8').write(s)
print('ok', p)
