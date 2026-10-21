#!/usr/bin/env python3
"""Uzupelnia dzialajacy KsiegaPostaci/config.yml o reguly rak, nie ruszajac reszty pliku.
Listy typow sa SUMOWANE (typy dopisane recznie na serwerze zostaja), brakujace komunikaty dopisywane."""
import re, sys
DWU = ['BOW', 'CROSSBOW', 'MUSKET', 'GREATBOW', 'GREATSWORD', 'GREATAXE', 'GREATHAMMER', 'HALBERD', 'LANCE', 'KOSA',
       'GREATSTAFF', 'STAFF', 'SPEAR']
LEWA = ['OFF_CATALYST']
JEDNO = ['SHIELD']
p = sys.argv[1]
s = open(p, encoding='utf-8').read()

def lista(s, klucz, wymagane):
    m = re.search(rf'^{klucz}: \[([^\]]*)\]$', s, re.M)
    if not m:
        return s.replace('typy-mikstur-i-uzytkowych:', f'{klucz}: [{", ".join(wymagane)}]\ntypy-mikstur-i-uzytkowych:', 1)
    jest = [t.strip() for t in m.group(1).split(',') if t.strip()]
    nowe = jest + [t for t in wymagane if t not in jest]
    return s[:m.start()] + f'{klucz}: [{", ".join(nowe)}]' + s[m.end():]

s = lista(s, 'typy-dwureczne', DWU)
s = lista(s, 'typy-tylko-lewa', LEWA)
# 12.10 13:39 wdrozono SHIELD w typy-tylko-lewa - blad (Nosiciel Relikwii ma tarcze jako bron): usun
m = re.search(r'^typy-tylko-lewa: \[([^\]]*)\]$', s, re.M)
bez = [t.strip() for t in m.group(1).split(',') if t.strip() and t.strip() != 'SHIELD']
s = s[:m.start()] + f'typy-tylko-lewa: [{", ".join(bez)}]' + s[m.end():]
s = lista(s, 'typy-zawsze-jednoreczne', JEDNO)
if 'dwureczna-do-prawej:' not in s:
    s = s.replace("komunikaty:\n", "komunikaty:\n  dwureczna-do-prawej: '&cBroń dwuręczną zakładasz do prawej ręki.'\n  tylko-lewa: '&cTo trzyma się w lewej ręce.'\n", 1)
open(p, 'w', encoding='utf-8').write(s)
assert 'SHIELD' not in re.search(r'^typy-tylko-lewa: .*$', s, re.M).group(0)
for k in ('typy-dwureczne:', 'typy-tylko-lewa:', 'typy-zawsze-jednoreczne:', 'dwureczna-do-prawej:', 'tylko-lewa:', 'STAFF', 'SPEAR'):
    assert k in s, 'brak ' + k
print('config scalony:', p)
