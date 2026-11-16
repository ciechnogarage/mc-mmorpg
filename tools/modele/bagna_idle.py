"""Dodaje wyłącznie spokojny oddech do modelu etapu bryły."""
import json
from pathlib import Path

import animacje


def dodaj(sciezka, dlugosc, kosci):
    path = Path(sciezka)
    data = json.loads(path.read_text())
    def walk(nodes):
        for node in nodes:
            if isinstance(node, dict):
                yield node
                yield from walk(node.get('children', []))
    ids = {bone['name']: bone['uuid'] for bone in walk(data['outliner'])}
    ruch = {}
    for name, (kanal, amp) in kosci.items():
        if name not in ids:
            raise KeyError(name)
        zero = (0, 0, 0) if kanal != 'scale' else (1, 1, 1)
        middle = tuple(zero[i] + amp[i] for i in range(3))
        ruch[name] = {kanal: [(0, zero, 'linear'), (dlugosc / 2, middle, 'linear'), (dlugosc, zero, 'linear')]}
    spec = {'idle': {'petla': 'loop', 'dlugosc': dlugosc, 'kosci': ruch}}
    data['animations'] = animacje.zbuduj(spec, ids, do_me=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1))
