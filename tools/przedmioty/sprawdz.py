#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Bramka statyczna po generuj.py: YAML bez duplikatow kluczy, spojne odwolania (modyfikatory, sety, tiery, ulepszenia).
Uruchamiac w dockerze (pyyaml):
  docker run --rm -v <plugins>:/p:ro -v /home/dev/Desktop/Przemek/MC/tools/przedmioty:/t:ro python:3.12-slim \
    sh -c 'pip -q install pyyaml >/dev/null 2>&1; python /t/sprawdz.py /p'
"""
import glob, os, sys, yaml

class Dup(yaml.SafeLoader):
    pass

def _map(loader, node, deep=False):
    seen = set()
    for k, _ in node.value:
        key = loader.construct_object(k, deep=deep)
        if key in seen:
            raise yaml.constructor.ConstructorError(None, None, f'duplikat klucza {key!r}', k.start_mark)
        seen.add(key)
    return loader.construct_mapping(node, deep)
Dup.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, _map)

def load(p):
    with open(p, encoding='utf-8') as f:
        return yaml.load(f, Loader=Dup) or {}

P = sys.argv[1]
MI = os.path.join(P, 'MMOItems')
bledy = []
pliki = glob.glob(f'{MI}/item/*.yml') + [f'{MI}/{x}' for x in ['item-tiers.yml', 'item-sets.yml', 'upgrade-templates.yml', 'custom-stats.yml', 'config.yml']] \
    + glob.glob(f'{MI}/modifiers/*.yml') + glob.glob(f'{MI}/crafting-stations/*.yml') + [f'{MI}/language/stats.yml', f'{MI}/language/lore-format.yml']
dane = {}
for p in pliki:
    try:
        dane[p] = load(p)
    except Exception as e:
        bledy.append(f'YAML {os.path.relpath(p, P)}: {e}')

mods = {}
for p in glob.glob(f'{MI}/modifiers/*.yml'):
    mods.update(dane.get(p, {}) or {})
tiery = set(dane.get(f'{MI}/item-tiers.yml', {}) or {})
sety = set(dane.get(f'{MI}/item-sets.yml', {}) or {})
upg = set(dane.get(f'{MI}/upgrade-templates.yml', {}) or {})

def refs(node, path):
    for k, v in (node or {}).items():
        if isinstance(v, dict) and not v:
            if k not in mods:
                bledy.append(f'{path}: brak publicznego modyfikatora {k}')
        elif isinstance(v, dict) and 'modifiers' in v:
            refs(v['modifiers'], path + '/' + k)

ids, n_prz = {}, 0
for p in glob.glob(f'{MI}/item/*.yml'):
    for pid, it in (dane.get(p) or {}).items():
        if pid in ids:
            bledy.append(f'id {pid} w {ids[pid]} i {os.path.basename(p)}')
        ids[pid] = os.path.basename(p)
        if not str(pid).startswith('PRZ_'):
            continue
        n_prz += 1
        b = (it or {}).get('base', {})
        if b.get('tier') and b['tier'] not in tiery:
            bledy.append(f'{pid}: nieznany tier {b["tier"]}')
        if b.get('set') and b['set'] not in sety:
            bledy.append(f'{pid}: nieznany set {b["set"]}')
        u = b.get('upgrade')
        if u and u.get('template') and u['template'] not in upg:
            bledy.append(f'{pid}: nieznany szablon ulepszen {u["template"]}')
        refs(it.get('modifiers'), pid)
for g, v in mods.items():
    if isinstance(v, dict) and isinstance(v.get('modifiers'), dict):
        refs(v['modifiers'], 'modifiers/' + g)


# MythicMobs: droptabele lupu + wpiecie w bestiariusz
import re as _re
mmd = os.path.join(P, 'MythicMobs', 'droptables')
tabele = {}
for p in glob.glob(f'{mmd}/*.yml'):
    try:
        tabele.update(load(p) or {})
    except Exception as e:
        bledy.append(f'YAML {os.path.relpath(p, P)}: {e}')
typ_id = {}
for p in glob.glob(f'{MI}/item/*.yml'):
    for pid in (dane.get(p) or {}):
        typ_id[str(pid)] = os.path.basename(p)[:-4].upper()
n_lup = 0
for nazwa, tab in tabele.items():
    for d in (tab or {}).get('Drops', []) or []:
        m = _re.match(r'mmoitem\{type=([A-Z_]+);id=([A-Z0-9_]+)\}', str(d))
        if m:
            if typ_id.get(m.group(2)) != m.group(1):
                bledy.append(f'droptabela {nazwa}: {m.group(1)}/{m.group(2)} nie istnieje')
            n_lup += 1
        elif str(d).startswith('PRZ_LUP_'):
            if str(d).split()[0] not in tabele:
                bledy.append(f'droptabela {nazwa}: brak zagniezdzonej {d}')
print(f'wpisow lupu mmoitem: {n_lup}')
# pule affiksow: warianty rzadkosci wymagaja do 3 prefiksow i 3 sufiksow (min=max) — mniejsza pula = przedmiot sie nie zbuduje
for g, v in mods.items():
    if isinstance(v, dict) and (g.startswith('pref_') or g.startswith('suf_')) and len((v.get('modifiers') or {})) < 3:
        bledy.append(f'pula {g} ma {len(v.get("modifiers") or {})} rodzin (< 3)')

print(f'plikow: {len(dane)}/{len(pliki)}, przedmiotow PRZ_: {n_prz}, modyfikatorow publicznych: {len(mods)}, tierow: {len(tiery)}, setow: {len(sety)}')
for b in bledy:
    print('BLAD', b)
sys.exit(1 if bledy else 0)
