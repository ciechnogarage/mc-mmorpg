#!/usr/bin/env python3
"""Dopisuje do dzialajacego KsiegaPostaci/config.yml klucze regul rak (12.10), nie ruszajac reszty pliku."""
import sys
p = sys.argv[1]
s = open(p, encoding='utf-8').read()
if 'typy-dwureczne:' not in s:
    s = s.replace('typy-mikstur-i-uzytkowych:', "typy-dwureczne: [BOW, CROSSBOW, MUSKET, GREATBOW, GREATSWORD, GREATAXE, GREATHAMMER, HALBERD, LANCE, KOSA, GREATSTAFF]\ntypy-tylko-lewa: [SHIELD, OFF_CATALYST]\ntypy-mikstur-i-uzytkowych:", 1)
if 'dwureczna-do-prawej:' not in s:
    s = s.replace("komunikaty:\n", "komunikaty:\n  dwureczna-do-prawej: '&cBroń dwuręczną zakładasz do prawej ręki.'\n  tylko-lewa: '&cTo trzyma się w lewej ręce.'\n", 1)
open(p, 'w', encoding='utf-8').write(s)
for k in ('typy-dwureczne:', 'typy-tylko-lewa:', 'dwureczna-do-prawej:', 'tylko-lewa:'):
    assert k in s, 'brak ' + k
print('config scalony:', p)
