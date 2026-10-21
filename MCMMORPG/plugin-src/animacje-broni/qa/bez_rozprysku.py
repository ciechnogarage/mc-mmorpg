# Zdejmuje 'on-attack' (rozprysk MMOItems: stozek 6,5 bl. z ukrytym cooldownem 3 s) z typow broni bialej w item-types.yml.
import re, sys
TYPY = {'SWORD', 'DAGGER', 'SPEAR', 'HAMMER', 'GAUNTLET', 'WHIP', 'GREATSWORD', 'LONG_SWORD', 'KATANA', 'THRUSTING_SWORD',
        'AXE', 'GREATAXE', 'HALBERD', 'LANCE', 'GREATHAMMER', 'GREATSTAFF', 'STAVE', 'SIERP', 'KOSA'}
p = sys.argv[1]; s = open(p, encoding='utf-8').read(); n = 0
def zdejmij(m):
    global n
    if m.group(1) not in TYPY: return m.group(0)
    b, k = re.subn(r'(?m)^    on-attack: \S+\n', '', m.group(0)); n += k; return b
s = re.sub(r'(?ms)^([A-Z_]+):\n.*?(?=^\S|\Z)', zdejmij, s)
open(p, 'w', encoding='utf-8').write(s); print('zdjete on-attack:', n)
