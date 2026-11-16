#!/usr/bin/env python3
"""Zestawienie pięciu ważnych prób każdej komórki macierzy rundy 1."""
import json
import math
import re
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PRIMARY = {
    'plomienie': 'MX1', 'odmrozenie': 'MX1',
    'ognisty_pocisk': 'MXK', 'kula_ognia': 'MXK', 'leczenie': 'MXL2',
    'runa_ognia': 'MX2A', 'oslona': 'MX2A', 'przywolany_miecz': 'MX2A',
    'duch_wilka': 'MX2B', 'atronach_ognia': 'MX2B',
    'leczenie_dloni': 'MXH', 'uspokojenie': 'MX3A',
    'uzdrowienie_innego': 'MXU', 'zasklepienie_ran': 'MX3B',
    'wielkie_uzdrowienie': 'MX3B',
}
QUEUE = {
    'MX1': 30, 'MXK': 34, 'MXL2': 41, 'MX2A': 38, 'MX2B': 39,
    'MXH': 42, 'MXU': 43, 'MX3A': 36, 'MX3B': 37,
    'MXALLY1': 45, 'MXALLY2': 45, 'MXALLY3': 45, 'MXF': 46,
}
METRICS = ('cost', 'hpDelta', 'damagePerSec', 'startMs', 'stopMs',
           'bossBars', 'sounds', 'messages', 'setSlotPerSec')
GESTURES = ('prawa', 'lewa', 'obie', 'podwojne')
TARGETS = ('husk', 'sojusznik', 'pvp', 'powietrze', 'sciana')
data = {group: json.loads((ROOT / 'bot/wyniki' / f'{group}.json').read_text())
        for group in QUEUE}
md5 = {group: re.search(r'md5=([0-9a-f]+)',
          (ROOT / 'kolejka' / f'b-1-{number}.out').open().readline()).group(1)
       for group, number in QUEUE.items()}

def valid(spell, target, row):
    if row['status'] != 'PASS' or 'cost' not in row['FEEL']:
        return False
    match = re.search(r'HP (-?\d+(?:\.\d+)?)→', row['detail'])
    health = float(match.group(1)) if match else None
    if target in ('sojusznik', 'pvp') and (health is None or health <= 0):
        return False
    if spell in ('leczenie_dloni', 'uzdrowienie_innego') and target in ('sojusznik', 'pvp') and health >= 18:
        return False
    if spell in ('leczenie', 'zasklepienie_ran', 'wielkie_uzdrowienie') and target in ('powietrze', 'sciana') and (health is None or health >= 18):
        return False
    return True

cells = []
for spell, group in PRIMARY.items():
    for gesture in GESTURES:
        for target in TARGETS:
            prefix = f'MX-{spell}-{gesture}-{target}-'
            candidates = [(group, row) for row in data[group]['rows']
                          if row['id'].startswith(prefix)]
            if target == 'sojusznik':
                candidates += [(extra, row)
                               for extra in ('MXALLY1', 'MXALLY2', 'MXALLY3', 'MXF')
                               for row in data[extra]['rows']
                               if row['id'].startswith(prefix)]
            chosen = [(source, row) for source, row in candidates
                      if valid(spell, target, row)][:5]
            if len(chosen) != 5:
                raise RuntimeError(f'{prefix}: {len(chosen)} ważnych prób')
            stats = {}
            for metric in METRICS:
                values = sorted(row['FEEL'][metric] for _, row in chosen
                                if isinstance(row['FEEL'].get(metric), (int, float)))
                stats[metric] = {
                    'n': len(values),
                    'median': statistics.median(values) if values else None,
                    'p95': values[math.ceil(.95 * len(values)) - 1] if values else None,
                }
            builds = sorted({md5[source] for source, _ in chosen})
            cells.append({'spell': spell, 'gesture': gesture, 'target': target,
                          'validRepetitions': 5,
                          'sourceGroups': sorted({source for source, _ in chosen}),
                          'jarMd5': builds, 'mixedBuilds': len(builds) > 1,
                          'metrics': stats})

result = {
    'method': ('Po pięć ważnych prób na komórkę; p95 = nearest rank. '
               'Odrzucono HP=0, niezraniony cel leczenia i BLAD. '
               'Start/stop to wskaźniki pakietowe, nie potwierdzone chwile efektu.'),
    'cells': len(cells),
    'mixedBuildCells': sum(cell['mixedBuilds'] for cell in cells),
    'data': cells,
}
destination = ROOT / 'krytyk/macierz-r1-statystyki.json'
destination.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print(f"{destination}: {result['cells']} komórek, {result['mixedBuildCells']} mieszanych md5")
