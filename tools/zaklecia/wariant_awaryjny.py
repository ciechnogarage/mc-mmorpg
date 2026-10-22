#!/usr/bin/env python3
"""Wariant awaryjny skilli zaklec (bez pluginu Zaklecia): zakZasob/zakMaZasob -> komenda MMOCore + <mana>.
Uzycie: wariant_awaryjny.py <wejscie.yml> <wyjscie.yml>. Koszt: linia w logu serwera co tik rzucania."""
import re, sys
s = open(sys.argv[1], encoding='utf-8').read()
def warunek(m):
    wciecie, skill, neg, a = m.group(1), m.group(2), m.group(3), float(m.group(4))
    prog = f'>{a - 0.01:.2f}'
    linia = f'{wciecie}- skill{{s={skill};sync=true}} ?{"!" if neg else ""}variableInRange{{var=caster.zak_mana;value={prog}}}'
    return (f'{wciecie}- setvariable{{var=caster.zak_mana;type=FLOAT;value="<mana>"}} @self\n' if not neg else '') + linia
s = re.sub(r'^( +)- skill\{s=(\w+);sync=true\} \?(!?)zakMaZasob\{zasob=mana;a=([\d.]+)\}$', warunek, s, flags=re.M)
s = re.sub(r'zakZasob\{zasob=mana;a=([\d.]+)\} @self', r'command{c="mmocore admin resource-mana take <caster.name> \1"} @self', s)
s = re.sub(r'zakZasob\{zasob=stamina;a=([\d.]+)\} @target', r'command{c="mmocore admin resource-stamina take <target.name> \1"} @target', s)
kod = '\n'.join(l for l in s.splitlines() if not l.lstrip().startswith('#'))
assert 'zakZasob' not in kod and 'zakMaZasob' not in kod, 'zostala mechanika pluginu'
open(sys.argv[2], 'w', encoding='utf-8').write(s)
