# -*- coding: utf-8 -*-
"""Audyt PROJEKTOWY profesji — twarde progi z PROJEKT.md. Czyta tylko pliki pluginow (nie generator),
wiec sprawdza to, co faktycznie trafi na serwer.

Uzycie (host bez pyyaml):
  docker run --rm -v <plugins>:/p:ro -v <ten katalog>:/s:ro python:3.12-slim \
    sh -c 'pip -q install pyyaml >/dev/null 2>&1; python -I /s/audyt.py /p'
Kod wyjscia 0 = wszystkie bramki PASS. Kazdy FAIL wypisuje konkretne id i liczby.
"""
import collections
import glob
import os
import re
import sys

import yaml

P = sys.argv[1] if len(sys.argv) > 1 else '/p'
SZCZEGOLY = '-v' in sys.argv

ZBIERACKIE = ['mining', 'woodworking', 'herbalism', 'farming', 'fishing', 'hunting']
RZEMIOSLO = ['smithing', 'tailoring', 'jewelcrafting', 'alchemy', 'enchanting', 'woodworking']
OGOLNE = ['cooking', 'building']
WSZYSTKIE = sorted(set(ZBIERACKIE + RZEMIOSLO + OGOLNE))
DEMO = ['mythical-forge', 'arcane-forge', 'steel-crafting-station']
VANILLA_ZRODLA = ('mineblock', 'placeblock', 'craftitem', 'fishitem', 'smeltitem', 'enchantitem', 'repairitem',
                  'brewpotion', 'killmob', 'eat')
VANILLA_SEKCJE = ('alchemy-experience', 'base-enchant-exp', 'repair-exp', 'on-fish', 'on-mine')
MAX_LUKA = 3          # najwiecej poziomow bez nowej odblokowanej rzeczy
TOLERANCJA_AKCJI = 0.15


def ld(f):
    try:
        with open(f, encoding='utf-8', errors='ignore') as h:
            return yaml.safe_load(h) or {}
    except FileNotFoundError:
        return {}
    except Exception as e:  # zly YAML to tez FAIL
        WYNIKI['yaml'].append(f'{os.path.relpath(f, P)}: {e}')
        return {}


WYNIKI = collections.OrderedDict((k, []) for k in (
    'yaml', 'istnienie', 'ujscie', 'przemiany', 'narzedzia', 'mielenie', 'luki', 'sila', 'buffy', 'vanilla', 'demo',
    'perki', 'mistrzostwa', 'zlecenia', 'wezly', 'zrodla'))


def g(t):
    return 10 * 1.25 ** (t - 1)


def tier_poziomu(l):
    return min(10, max(1, (l - 1) // 10 + 1))


# ------------------------------------------------------------------ dane
items = {}
for f in glob.glob(P + '/MMOItems/item/*.yml'):
    t = os.path.basename(f)[:-4].upper()
    for k, v in ld(f).items():
        if isinstance(v, dict):
            items[(t, str(k))] = v.get('base', {}) or {}

stacje = {}
for f in glob.glob(P + '/MMOItems/crafting-stations/*.yml'):
    stacje[os.path.basename(f)[:-4]] = ld(f)

rz = ld(P + '/Rzemioslo/config.yml')          # plugin Rzemioslo (v2)
narz = rz.get('narzedzia', {}) or {}          # id -> {rodzaj, tier}
wezly = rz.get('wezly', {}) or {}             # nexo id -> {profesja, tier, glowny, poboczny}
lowiska = rz.get('lowiska', {}) or {}         # tier -> {glowny, poboczny}
zwierzyna = rz.get('zwierzyna', {}) or {}     # mm id -> tier
buffy = rz.get('buffy', {}) or {}             # id przedmiotu -> {grupa, czas, ...}
zlecenia = rz.get('zlecenia', {}) or {}       # profesja -> tier -> [ {id, ilosc} ]
haki = rz.get('haki-swiata', []) or []        # id przedmiotow zuzywanych przez lokacje (przeprawy)
surowce = set(rz.get('surowce', []) or [])    # id surowcow (zbieractwo) — nie wolno ich wytwarzac

profs = {os.path.basename(f)[:-4]: ld(f) for f in glob.glob(P + '/MMOCore/professions/*.yml')}
mst = ld(P + '/Mistrzostwa/config.yml')

# receptury
rec = []
for st, d in stacje.items():
    for rid, r in ((d or {}).get('recipes') or {}).items():
        r = r or {}
        tekst = lambda k: ' '.join(map(str, r.get(k) or []))
        o = re.search(r'type=(\w+)[,;]id=(\w+)', str(r.get('output', '')))
        pm = re.search(r'profession\{profession=(\w+),level=(\d+)', tekst('conditions'))
        ing = [(a.upper(), b, int(c)) for a, b, c in re.findall(r'type=(\w+)[,;]id=(\w+)[,;]amount=(\d+)', tekst('ingredients'))]
        rec.append(dict(st=st, id=rid, out=(o.group(1).upper(), o.group(2)) if o else None, ing=ing,
                        prof=pm.group(1) if pm else None, lv=int(pm.group(2)) if pm else None,
                        xp=re.findall(r'amount=(\d+)', tekst('triggers'))))

aktywne_stacje = {s for s in stacje if s not in DEMO}
chowance_txt = ''
for f in glob.glob(P + '/Chowance/**/*.yml', recursive=True):
    chowance_txt += open(f, encoding='utf-8', errors='ignore').read()

# ------------------------------------------------------------------ istnienie odwolan
for r in rec:
    if r['st'] in DEMO:
        continue
    for a, b, c in r['ing']:
        if (a, b) not in items:
            WYNIKI['istnienie'].append(f"{r['st']}/{r['id']}: skladnik {a}.{b} nie istnieje")
    if r['out'] and r['out'] not in items:
        WYNIKI['istnienie'].append(f"{r['st']}/{r['id']}: wynik {r['out']} nie istnieje")
for nid, n in narz.items():
    if not any(k[1] == nid for k in items):
        WYNIKI['istnienie'].append(f'narzedzie {nid} nie istnieje w MMOItems')
for w, d in list(wezly.items()) + [(f'lowisko T{t}', d) for t, d in lowiska.items()]:
    for k in ('glowny', 'poboczny'):
        if d.get(k) and not any(x[1] == d[k] for x in items):
            WYNIKI['istnienie'].append(f'{w}: {k} {d.get(k)} nie istnieje')

# ------------------------------------------------------------------ ujscie: kazdy aktywny PROF_ ma realne uzycie
uzyte = collections.defaultdict(set)
for r in rec:
    if r['st'] in aktywne_stacje:
        for a, b, c in r['ing']:
            uzyte[b].add('receptura:' + r['st'])
for b in buffy:
    uzyte[b].add('buff')
for b in narz:
    uzyte[b].add('narzedzie')
for b in haki:
    uzyte[b].add('swiat')
for b, k in (rz.get('uzycia') or {}).items():
    uzyte[b].add('uzycie:' + str(k))
for prof, tiers in zlecenia.items():
    for t, lst in (tiers or {}).items():
        for z in lst or []:
            uzyte[z.get('id')].add('zlecenie')
for i in set(re.findall(r'(PROF_[A-Z0-9_]+)', chowance_txt)):
    uzyte[i].add('chowance')
TYPY_SPRZETU = {'SWORD', 'AXE', 'DAGGER', 'HAMMER', 'HALBERD', 'GREATSWORD', 'GREATHAMMER', 'KATANA', 'SPEAR', 'BOW',
                'CROSSBOW', 'GREATBOW', 'STAFF', 'WAND', 'GREATSTAFF', 'TOME', 'SHIELD', 'ARMOR', 'NECKLACE', 'RING',
                'GLOVES', 'BELT', 'CHARM', 'TALISMAN', 'SIERP', 'KOSA', 'WHIP', 'FILAKTERIUM', 'GEM_STONE'}
for (t, i), b in items.items():
    if t in TYPY_SPRZETU and (i.startswith('PROF_') or i.startswith('RZM_')):
        uzyte[i].add('sprzet')
relikty = set(rz.get('relikty', []) or [])
for (t, i), b in items.items():
    if not (i.startswith('PROF_') or i.startswith('RZM_')) or i in relikty:
        continue
    if not uzyte.get(i):
        WYNIKI['ujscie'].append(f'{t}.{i} ({b.get("name", "?")}) — brak ujscia poza skupem')

# ------------------------------------------------------------------ przemiany: zadna receptura nie produkuje surowca
for r in rec:
    if r['st'] in aktywne_stacje and r['out'] and r['out'][1] in surowce:
        WYNIKI['przemiany'].append(f"{r['st']}/{r['id']} wytwarza surowiec {r['out'][1]} z {[x[1] for x in r['ing']]}")
if not surowce:
    WYNIKI['przemiany'].append('brak listy surowce: w Rzemioslo/config.yml — nie da sie sprawdzic przemian')

# ------------------------------------------------------------------ narzedzia: kazdy rodzaj x tier 1..10, uzywany przez wezly
RODZAJ_PROF = {'kilof': 'mining', 'siekiera': 'woodworking', 'sierp': 'herbalism', 'motyka': 'farming',
               'wedka': 'fishing', 'noz': 'hunting'}
jest = collections.defaultdict(set)
for nid, n in narz.items():
    jest[n.get('rodzaj')].add(int(n.get('tier', 0)))
for rodzaj in RODZAJ_PROF:
    brak = sorted(set(range(1, 11)) - jest.get(rodzaj, set()))
    if brak:
        WYNIKI['narzedzia'].append(f'{rodzaj}: brak tierow {brak}')
narz_v1 = [i for (t, i) in items if t == 'TOOL' and i.startswith('PROF_') and i not in narz and i not in relikty]
if narz_v1:
    WYNIKI['narzedzia'].append(f'narzedzia bez funkcji (nie w pluginie): {narz_v1}')
if not rz:
    WYNIKI['narzedzia'].append('brak pluginu Rzemioslo/config.yml — narzedzia nic nie wymagaja')

# ------------------------------------------------------------------ wezly: kazda profesja zbieracka ma zrodlo w kazdym tierze
for prof in ['mining', 'woodworking', 'herbalism', 'farming']:
    t_jest = {int(d.get('tier')) for d in wezly.values() if d.get('profesja') == prof}
    brak = sorted(set(range(1, 11)) - t_jest)
    if brak:
        WYNIKI['wezly'].append(f'{prof}: brak wezlow tierow {brak}')
brak_l = sorted(set(range(1, 11)) - {int(t) for t in lowiska})
if brak_l:
    WYNIKI['wezly'].append(f'fishing: brak lowisk tierow {brak_l}')
t_zw = {int(t['tier'] if isinstance(t, dict) else t) for t in zwierzyna.values()}
if 1 not in t_zw:
    WYNIKI['wezly'].append('hunting: zadna bestia T1 nie jest na liscie zwierzyna (Bestiariusz/Loch 001)')

# ------------------------------------------------------------------ mielenie: krzywa w akcjach
for pid, d in profs.items():
    if pid not in WSZYSTKIE:
        continue
    cv = d.get('exp-curve')
    plik = P + f'/MMOCore/exp-curves/{cv}.txt'
    if not os.path.exists(plik):
        WYNIKI['mielenie'].append(f'{pid}: krzywa {cv} nie jest plikiem (formula = {{level}} klasy!)')
        continue
    linie = [float(x) for x in open(plik).read().split()]
    if len(linie) < 99:
        WYNIKI['mielenie'].append(f'{pid}: krzywa {cv} ma {len(linie)} linii (<99)')
        continue
    zle = []
    for l in range(1, 100):
        akcje = linie[l - 1] / g(tier_poziomu(l))
        cel = 20 + 2 * l
        if abs(akcje - cel) / cel > TOLERANCJA_AKCJI:
            zle.append(f'L{l}:{akcje:.0f}≠{cel}')
    if zle:
        WYNIKI['mielenie'].append(f'{pid}: {len(zle)} poziomow poza celem, np. {zle[:4]}')
# XP receptur: ile wytworzen najlepszej receptury biezacego tieru na poziom (v1: 1254 olejow na 15 poziomow)
xp_rec = rz.get('xp-receptur', {}) or {}
for r in rec:
    if r['st'] in aktywne_stacje and r['prof'] in WSZYSTKIE and r['xp'] and r['st'] != 'chowance':
        WYNIKI['mielenie'].append(f"{r['st']}/{r['id']}: XP w triggerze receptury (liczy plugin — podwojne XP)")
krzywa_v2 = P + '/MMOCore/exp-curves/profesje_v2.txt'
need = [float(x) for x in open(krzywa_v2).read().split()] if os.path.exists(krzywa_v2) else []
for pid in RZEMIOSLO + OGOLNE:
    rs = [r for r in rec if r['prof'] == pid and r['st'] in aktywne_stacje and r['lv']]
    brak_xp = [r['id'] for r in rs if r['id'] not in xp_rec]
    if brak_xp:
        WYNIKI['mielenie'].append(f'{pid}: {len(brak_xp)} receptur bez XP w Rzemioslo/config.yml, np. {brak_xp[:3]}')
        continue
    najgorzej = (0, 0)
    for l in range(1, min(100, len(need) + 1)):
        t = tier_poziomu(l)
        dost = [xp_rec[r['id']] for r in rs if r['lv'] <= l and tier_poziomu(r['lv']) == t]
        if not dost:
            WYNIKI['mielenie'].append(f'{pid}: poziom {l} bez receptury biezacego tieru')
            continue
        ile = need[l - 1] / max(dost)
        najgorzej = max(najgorzej, (ile, l))
        if ile > 20 + 2 * l:
            WYNIKI['mielenie'].append(f'{pid}: poziom {l} wymaga {ile:.0f} wytworzen najlepszej receptury (> {20 + 2 * l} akcji)')
    if SZCZEGOLY:
        print(f'  [mielenie] {pid}: najwiecej {najgorzej[0]:.0f} wytworzen na poziom {najgorzej[1]}')
    za_duzo = [r['id'] for r in rs if need and xp_rec[r['id']] > 3 * need[min(98, r['lv'] - 1)]]
    if za_duzo:
        WYNIKI['mielenie'].append(f'{pid}: receptury dajace >3 poziomy naraz: {za_duzo[:4]}')

# ------------------------------------------------------------------ luki: nowa rzecz co <= MAX_LUKA poziomow do max
for pid in WSZYSTKIE:
    d = profs.get(pid, {})
    mx = int(d.get('max-level', 0) or 0)
    if mx < 100:
        WYNIKI['luki'].append(f'{pid}: max-level {mx} < 100')
    odbl = set()
    for r in rec:
        if r['prof'] == pid and r['st'] in aktywne_stacje and r['lv']:
            odbl.add(r['lv'])
    if pid in ZBIERACKIE:
        for t in range(1, 11):
            odbl.add(10 * t - 9)  # nowy wezel/lowisko/zwierzyna tieru
    for l, t in ((int(k), v) for k, v in (rz.get('perki-progi') or {}).items()):
        odbl.add(l)
    pozn = sorted(x for x in odbl if 1 <= x <= 100)
    luki = []
    prev = 1
    for x in pozn + [101]:
        if x - prev > MAX_LUKA and not (pid in ZBIERACKIE and pid != 'woodworking'):
            luki.append(f'{prev}->{x}')
        prev = x
    if pid in ZBIERACKIE and pid != 'woodworking':
        braki = [t for t in range(1, 11) if (10 * t - 9) not in pozn]
        if braki:
            luki.append(f'tiery bez zrodla {braki}')
    if luki:
        WYNIKI['luki'].append(f'{pid}: luki {luki[:6]}{"…" if len(luki) > 6 else ""} ({len(luki)})')

# ------------------------------------------------------------------ sila: sprzet z rzemiosla nie mocniejszy niz lup
v2_tiery = {'ZWYKLY', 'MAGICZNY', 'RZADKI', 'EPICKI', 'LEGENDARNY', 'UNIKATOWY', 'ZESTAW'}
for r in rec:
    if r['st'] not in aktywne_stacje or not r['out']:
        continue
    t, i = r['out']
    if t in TYPY_SPRZETU - {'GEM_STONE'}:
        b = items.get(r['out'], {})
        if not i.startswith('RZM_'):
            WYNIKI['sila'].append(f"{r['st']}/{r['id']}: sprzet {i} spoza linii RZM_ (bazy v2)")
        elif b.get('tier') not in (None, 'MAGICZNY'):
            WYNIKI['sila'].append(f'{i}: tier {b.get("tier")} (dozwolony tylko MAGICZNY)')
for (t, i), b in items.items():
    if i.startswith('PROF_') and t in TYPY_SPRZETU - {'GEM_STONE'} and b.get('tier') not in v2_tiery | {None}:
        if i not in relikty and any(r['out'] == (t, i) for r in rec if r['st'] in aktywne_stacje):
            WYNIKI['sila'].append(f'{i}: stary tier {b.get("tier")} i plaskie staty poza v2')

# ------------------------------------------------------------------ buffy
for (t, i), b in items.items():
    if t != 'CONSUMABLE' or not (i.startswith('PROF_') or i.startswith('RZM_')):
        continue
    ef = b.get('effects') or {}
    cd = float(b.get('item-cooldown', 0) or 0)
    for e, d in ef.items():
        dur = float((d or {}).get('duration', 0))
        if dur > cd and i not in relikty:
            WYNIKI['buffy'].append(f'{i}: efekt vanilli {e} {dur:.0f}s > cooldown {cd:.0f}s (staly buff)')
for bid, d in buffy.items():
    if d.get('grupa') not in ('mikstura', 'eliksir', 'olej', 'posilek', 'opatrunek'):
        WYNIKI['buffy'].append(f'{bid}: nieznana grupa {d.get("grupa")}')
    if d.get('grupa') in ('eliksir', 'olej', 'posilek') and not d.get('czas'):
        WYNIKI['buffy'].append(f'{bid}: buff bez czasu')
if not buffy:
    WYNIKI['buffy'].append('brak buffy: w pluginie — alchemia/gotowanie nie maja dzialajacych produktow')

# ------------------------------------------------------------------ vanilla XP
for pid, d in profs.items():
    for s in VANILLA_SEKCJE:
        if s in d:
            WYNIKI['vanilla'].append(f'{pid}: sekcja {s}')
    for e in (d.get('exp-sources') or []) if isinstance(d.get('exp-sources'), list) else []:
        if str(e).split('{')[0] in VANILLA_ZRODLA:
            WYNIKI['vanilla'].append(f'{pid}: zrodlo {e}')

# ------------------------------------------------------------------ demo stacje
for s in DEMO:
    if s in stacje:
        n = sum(1 for r in rec if r['st'] == s and any(b.startswith('PROF_') for a, b, c in r['ing']))
        WYNIKI['demo'].append(f'{s}: stacja demo aktywna ({n} receptur zjada PROF_)')

# ------------------------------------------------------------------ perki, mistrzostwa, zlecenia
if not rz.get('perki'):
    WYNIKI['perki'].append('brak perki: w pluginie — poziom profesji nic nie daje poza recepturami')
if not mst:
    WYNIKI['mistrzostwa'].append('plugin Mistrzostwa nie jest zainstalowany (brak Mistrzostwa/config.yml)')
else:
    if int(mst.get('prog', 0)) != 50 or int(mst.get('limit', 0)) != 2:
        WYNIKI['mistrzostwa'].append(f"prog/limit {mst.get('prog')}/{mst.get('limit')} != 50/2")
    if mst.get('sabotaz-testowy'):
        WYNIKI['mistrzostwa'].append('sabotaz-testowy niepusty!')
    lista = set(mst.get('mistrzowskie', []) or []) | set(mst.get('ogolne', []) or [])
    if lista != set(WSZYSTKIE):
        WYNIKI['mistrzostwa'].append(f'lista profesji {sorted(lista ^ set(WSZYSTKIE))} niezgodna')
for pid in RZEMIOSLO + OGOLNE:
    t_z = {int(t) for t in (zlecenia.get(pid) or {})}
    brak = sorted(set(range(1, 11)) - t_z)
    if brak:
        WYNIKI['zlecenia'].append(f'{pid}: brak zlecen tierow {brak}')
for pid in ZBIERACKIE:
    if not (rz.get('zlecenia-startowe') or {}).get(pid):
        WYNIKI['zlecenia'].append(f'{pid}: brak zlecenia startowego (narzedzie T1)')

# ------------------------------------------------------------------ zrodla: kazdy skladnik da sie zdobyc
zrodla = set(surowce)
for w in list(wezly.values()) + list(zwierzyna.values()):
    for k in ('glowny', 'poboczny', 'rzadki'):
        if w.get(k):
            zrodla.add(w[k])
for l in lowiska.values():
    zrodla.update(x for x in (l.get('glowny'), l.get('poboczny')) if x)
    zrodla.update(x[0] for x in l.get('rzadkie') or [])
for f in glob.glob(P + '/MythicMobs/**/*.yml', recursive=True):
    zrodla.update(re.findall(r'id=([A-Z0-9_]+)', open(f, encoding='utf-8', errors='ignore').read()))
zrodla.update(r['out'][1] for r in rec if r['out'] and r['st'] in aktywne_stacje)
for r in rec:
    if r['st'] not in aktywne_stacje:
        continue
    for a, b, c in r['ing']:
        if b not in zrodla:
            WYNIKI['zrodla'].append(f"{r['st']}/{r['id']}: skladnika {b} nie da sie zdobyc")

# ------------------------------------------------------------------ raport
fail = 0
for k, v in WYNIKI.items():
    stan = 'PASS' if not v else 'FAIL'
    fail += bool(v)
    print(f'{stan:4} {k:12} {len(v):4}')
    for x in (v if SZCZEGOLY else v[:5]):
        print(f'       - {x}')
    if v and not SZCZEGOLY and len(v) > 5:
        print(f'       … +{len(v) - 5} (pelna lista: -v)')
print(f'\nBRAMKI: {len(WYNIKI) - fail}/{len(WYNIKI)} PASS')
sys.exit(1 if fail else 0)
