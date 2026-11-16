"""Dopracowanie techniczne odzyskanej wersji modelu BEZ zmiany wygladu (tekstura, bryla, animacje zostaja):
- kopia z Blockbencha 5 (format 5.0: kosci w 'groups', klatki w konwencji BB5) -> nasz format 4.10 (kosci w drzewie,
  klatki w konwencji BB4/ModelEngine: rotacja X,Y i pozycja X odwrocone) - ta sama postac co pliki z generatorow,
- nazwa modelu <baza>_w<N> (osobne karty w Blockbenchu),
- kosc h_* z koscmi-dziecmi -> hi_* (wiki ME Bone-Behaviors: dzieci h_ bez tagu nie obracaja sie z glowa),
- pivot hitboxu = wysokosc oczu (wiki ME Creating-a-Model), gdy byl na 0,
- podniesienie calego modelu o DY px (elementy, originy kosci, hitbox bez zmian wymiaru), gdy w pozie spoczynkowej
  cos siedzi pod ziemia (DY mierzone bramka bb_ziemia: min y spoczynku).
Uzycie: python3 dobij_wersje.py plik.bbmodel nazwa DY"""
import json
import sys

p, nazwa, dy = sys.argv[1], sys.argv[2], float(sys.argv[3])
m = json.load(open(p))
m['name'] = nazwa
zmiany = []
if str(m['meta'].get('format_version', '')).startswith('5'):
    grupy = {g['uuid']: g for g in m.pop('groups', [])}

    def scal(o):
        return [dict(grupy.get(x['uuid'], {}), **{k: v for k, v in x.items() if k != 'children'}, children=scal(x['children']))
                if isinstance(x, dict) else x for x in o]
    m['outliner'] = scal(m['outliner'])
    for a in m.get('animations', []):
        for an in a.get('animators', {}).values():
            for k in an.get('keyframes', []):
                for dp in k['data_points']:
                    if k['channel'] == 'rotation':
                        dp['x'] = str(-float(dp['x'])); dp['y'] = str(-float(dp['y']))
                    elif k['channel'] == 'position':
                        dp['x'] = str(-float(dp['x']))
    m['meta']['format_version'] = '4.10'
    zmiany.append('BB5 -> 4.10 (klatki w konwencji ME)')


def kosci(o):
    for x in o:
        if isinstance(x, dict):
            yield x
            yield from kosci(x['children'])


for k in kosci(m['outliner']):
    if k['name'].startswith('h_') and any(isinstance(c, dict) for c in k['children']):
        stara = k['name']; k['name'] = 'hi_' + k['name'][2:]; zmiany.append(f'{stara} -> {k["name"]}')
        for a in m.get('animations', []):
            for an in a.get('animators', {}).values():
                if an.get('name') == stara:
                    an['name'] = k['name']
if dy:
    for e in m['elements']:
        if 'from' in e:
            e['from'][1] += dy; e['to'][1] += dy
        if 'origin' in e:
            e['origin'][1] += dy
    for k in kosci(m['outliner']):
        k['origin'][1] += dy
    zmiany.append(f'podniesiony o {dy} px')
hb = next((k for k in kosci(m['outliner']) if k['name'] == 'hitbox'), None)
if hb and hb['origin'][1] <= dy + 0.01:
    wys = max(e['to'][1] for e in m['elements'] if 'from' in e and e['name'] != 'hitbox_box')
    hb['origin'][1] = round(wys * 0.6, 1); zmiany.append(f'pivot hitboxu (oczy) = {hb["origin"][1]}')
json.dump(m, open(p, 'w'), indent=1)
print(nazwa, '|', '; '.join(zmiany) or 'bez zmian')
