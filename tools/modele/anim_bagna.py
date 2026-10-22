"""Nakładanie animacji na gotowy .bbmodel, z ponownym powiązaniem UUID kości.

Wartości klatek są już w konwencji Blockbench 4 / ModelEngine.
"""
from __future__ import annotations

import json
import uuid
from pathlib import Path

from animacje import zbuduj


def _kosci(outliner):
    wynik = {}

    def odwiedz(x):
        if not isinstance(x, dict):
            return
        nazwa = x['name']
        if nazwa in wynik:
            raise ValueError(f'powtórzona kość: {nazwa}')
        wynik[nazwa] = x['uuid']
        for dziecko in x.get('children', []):
            odwiedz(dziecko)

    for korzen in outliner:
        odwiedz(korzen)
    return wynik


def zastosuj(sciezka, definicje, czasy=None):
    """Zastępuje animacje o podanych nazwach; geometrii i atlasu nie zmienia."""
    sciezka = Path(sciezka)
    model = json.loads(sciezka.read_text(encoding='utf-8'))
    uuid_kosci = _kosci(model['outliner'])
    spec = {
        nazwa: {
            'dlugosc': dane['dl'],
            'petla': dane['petla'],
            'kosci': dane['kosci'],
        } for nazwa, dane in definicje.items()
    }
    nowe = zbuduj(spec, uuid_kosci, do_me=False)
    for animacja in nowe:
        dane = definicje[animacja['name']]
        if skrypt := dane.get('skrypt'):
            t = dane['hit']
            animacja['animators']['effects'] = {
                'name': 'Effects', 'type': 'effect', 'keyframes': [{
                    'channel': 'timeline', 'time': t, 'interpolation': 'linear',
                    'uuid': str(uuid.uuid4()), 'data_points': [{'script': skrypt}],
                }],
            }
    nazwy = set(definicje)
    model['animations'] = [a for a in model.get('animations', []) if a['name'] not in nazwy] + nowe
    bufor = sciezka.with_suffix('.bbmodel.tmp')
    bufor.write_text(json.dumps(model, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
    bufor.replace(sciezka)
    if czasy is not None:
        Path(czasy).write_text(json.dumps({n: {k: v for k, v in d.items()
                                               if k in ('dl', 'windup', 'hit', 'recover')}
                                          for n, d in definicje.items()}, ensure_ascii=False, indent=2) + '\n')
