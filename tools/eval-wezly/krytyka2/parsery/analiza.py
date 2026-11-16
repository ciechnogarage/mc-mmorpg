# -*- coding: utf-8 -*-
"""Krytyka2 - analiza profesji: A petle, B balans, C martwa tresc, D ekonomia, E stackowanie.
Uruchom: docker run --rm -v /opt/docker/mc/data/world1:/d -v /tmp/k2parse:/o -w /o python:3.12-alpine \
  sh -c "pip install -q pyyaml && python /o/analiza.py"
Wynik: /o/wynik.txt (czytelny) + /o/wynik.json. Tylko ODCZYT plikow serwera."""
import sys, math, json, re, collections, glob, os
sys.path.insert(0, '/o')
from wspolne import *

OUT = []
def say(*a):
    OUT.append(' '.join(str(x) for x in a))

items = load_items()
recs = load_recipes()
profs = load_profs()
mobs = mm_yaml('mobs'); skills = mm_yaml('skills'); dts = mm_yaml('droptables')
rsp = mm_yaml('randomspawns'); spawners = mm_yaml('spawners'); mmitems = mm_yaml('items')
corp = corpus()

# ================================================================ SWIATY / GENERATORY
mvw = load(P + '/Multiverse-Core/worlds.yml')
world_gen = {}
for w, c in (mvw or {}).items():
    if isinstance(c, dict):
        world_gen[w] = c.get('generator')
bukkit = load(ROOT + '/bukkit.yml').get('worlds', {})
level_name = [l.split('=', 1)[1] for l in lines(ROOT + '/server.properties') if l.startswith('level-name=')][0]

# ================================================================ SPAWN MOBOW
spawn_src = collections.defaultdict(list)
for sid, s in spawners.items():
    spawn_src[s['cfg'].get('MobName')].append('spawner %s (%s:%s, swiat %s, cooldown %ss)' % (sid, s['file'], s['line'], s['cfg'].get('World'), s['cfg'].get('Cooldown')))
for rid, r in rsp.items():
    c = r['cfg']
    spawn_src[c.get('Type')].append('randomspawn %s chance=%s swiat=%s%s (%s:%s)' % (rid, c.get('Chance'), c.get('Worlds'), ' [EXAMPLE]' if r['example'] else '', r['file'], r['line']))
RX_SUMMON = re.compile(r'summon\{[^}]*?\b(?:m|mob|type|t)=([\w-]+)', re.I)
summoned_by = collections.defaultdict(list)
for sk, s in skills.items():
    for l in s['cfg'].get('Skills', []) or []:
        for m in RX_SUMMON.finditer(str(l)):
            summoned_by[m.group(1)].append(sk)
# MythicDungeons
md_mobs = collections.defaultdict(list); md_skills = collections.defaultdict(list)
for f in glob.glob(P + '/MythicDungeons/maps/*/functions.yml') + glob.glob(P + '/MythicDungeons/maps/*/*.yml'):
    for i, l in enumerate(lines(f)):
        m = re.search(r'^\s*mob:\s*(\S+)', l)
        if m: md_mobs[m.group(1)].append('%s:%d' % (rel(f), i + 1))
        m = re.search(r'mythicSkill:\s*(\S+)', l)
        if m: md_skills[m.group(1)].append('%s:%d' % (rel(f), i + 1))
# skill -> mob callers
RX_ANYSKILL = re.compile(r'(?:skill\{s(?:kill)?=|skills=|\bs=)([\w-]+)', re.I)
def skill_names_in(line):
    out = set()
    for m in RX_ANYSKILL.finditer(line): out.add(m.group(1))
    m = RX_RANDOMSKILL.search(line)
    if m:
        for p in m.group(1).split(','):
            if p.strip(): out.add(p.strip().split()[0])
    return out
skill_called_by_mob = collections.defaultdict(set); skill_called_by_skill = collections.defaultdict(set)
for mid, mo in mobs.items():
    for l in (mo['cfg'] or {}).get('Skills', []) or []:
        for n in skill_names_in(str(l)): skill_called_by_mob[n].add(mid)
for sk, s in skills.items():
    for l in s['cfg'].get('Skills', []) or []:
        for n in skill_names_in(str(l)):
            if n != sk: skill_called_by_skill[n].add(sk)

# osiagalnosc mobow i skilli (punkt staly)
reach_mob = set(k for k in mobs if spawn_src.get(k) and any('EXAMPLE' not in x for x in spawn_src[k]))
reach_mob |= set(k for k in md_mobs if k in mobs)
reach_skill = set(md_skills)
changed = True
while changed:
    changed = False
    for sk in skills:
        if sk in reach_skill: continue
        if skill_called_by_mob[sk] & reach_mob or skill_called_by_skill[sk] & reach_skill:
            reach_skill.add(sk); changed = True
    for mob, sks in summoned_by.items():
        if mob in mobs and mob not in reach_mob and set(sks) & reach_skill:
            reach_mob.add(mob); changed = True

# ================================================================ ZRODLA PRZEDMIOTOW
src = collections.defaultdict(list)   # id -> [dict(kind, what, ev, rate_note, live)]
# wezly
node_ev = {}
for mid, mo in mobs.items():
    if not mid.startswith('NODE_'): continue
    it = collections.Counter(); xp = collections.Counter()
    for l in (mo['cfg'] or {}).get('Skills', []) or []:
        if '~onInteract' in str(l):
            for m in RX_SKILLREF.finditer(str(l)):
                a, b = skill_ev(skills, m.group(1)); it.update(a); xp.update(b)
    live = mid in reach_mob
    node_ev[mid] = dict(items=dict(it), xp=dict(xp), live=live, spawners=spawn_src.get(mid, []), line='%s:%s' % (mo['file'], mo['line']))
    for iid, v in it.items():
        src[iid].append(dict(kind='wezel', what=mid, ev=v, live=live, rate='harvest'))
# moby
mob_drop_ev = {}
for mid, mo in mobs.items():
    drops = (mo['cfg'] or {}).get('Drops', []) or []
    ev = collections.Counter()
    for d in drops:
        name = str(d).split()[0]
        if name in dts: ev.update(droptable_ev(dts, name))
    xp = collections.Counter()
    for l in (mo['cfg'] or {}).get('Skills', []) or []:
        if '~onDeath' in str(l):
            for m in RX_EXP.finditer(str(l)): xp[m.group(1)] += float(m.group(2))
    # Template dziedziczy dropy i skille
    tpl = (mo['cfg'] or {}).get('Template')
    if tpl and tpl in mobs:
        for d in (mobs[tpl]['cfg'] or {}).get('Drops', []) or []:
            name = str(d).split()[0]
            if name in dts: ev.update(droptable_ev(dts, name))
        for l in (mobs[tpl]['cfg'] or {}).get('Skills', []) or []:
            if '~onDeath' in str(l):
                for m in RX_EXP.finditer(str(l)): xp[m.group(1)] += float(m.group(2))
    rs_ch = [float(rsp[r]['cfg'].get('Chance', 0)) for r in rsp if rsp[r]['cfg'].get('Type') == mid and not rsp[r]['example']]
    mob_drop_ev[mid] = dict(items=dict(ev), xp=dict(xp), live=mid in reach_mob, rs_chance=max(rs_ch) if rs_ch else 0)
    for iid, v in ev.items():
        src[iid].append(dict(kind='mob', what=mid, ev=v, live=mid in reach_mob, rs=max(rs_ch) if rs_ch else 0))
# Nexo bloki
nexo_blocks = {}
for f in glob.glob(P + '/Nexo/items/*.yml'):
    d = load(f)
    for bid, c in (d or {}).items():
        if not isinstance(c, dict): continue
        cb = ((c.get('Mechanics') or {}).get('custom_block') or {})
        loots = ((cb.get('drop') or {}).get('loots') or [])
        xp = 0
        for ev in ((c.get('Mechanics') or {}).get('custom') or {}).values():
            for a in ev.get('actions', []) if isinstance(ev, dict) else []:
                m = RX_EXP.search(str(a))
                if m: xp += float(m.group(2))
        for lo in loots:
            if lo.get('mmoitems_id'):
                v = float(lo.get('probability', 1)) * (float(lo.get('min_amount', 1)) + float(lo.get('max_amount', lo.get('min_amount', 1)))) / 2
                nexo_blocks.setdefault(bid, dict(items={}, xp=xp, line='%s:%s' % (rel(f), key_line(f, bid))))['items'][lo['mmoitems_id']] = v
iris_nexo = collections.defaultdict(list)
for f in glob.glob(P + '/Iris/packs/*/dimensions/*.json') + glob.glob(P + '/Iris/packs/*/*/*.json'):
    for i, l in enumerate(lines(f)):
        for m in re.finditer(r'nexo:([a-z0-9_]+)', l):
            iris_nexo[m.group(1)].append('%s:%d' % (rel(f), i + 1))
worlds_iris = [w for w, g in world_gen.items() if g and 'iris' in str(g).lower()]
# gestosc zloz w paczce Iris (bloki/chunk ~ srednia perChunk * srednia size) -> czas szukania+kopania
ore_bpc = {}
def _walk(o):
    if isinstance(o, dict):
        if 'palette' in o:
            for pb in o['palette']:
                bb = str(pb.get('block', '')) if isinstance(pb, dict) else ''
                if bb.startswith('nexo:'):
                    ore_bpc[bb[5:]] = (o.get('minPerChunk', 1) + o.get('maxPerChunk', 1)) / 2 * (o.get('minSize', 1) + o.get('maxSize', 1)) / 2
        for v in o.values(): _walk(v)
    elif isinstance(o, list):
        for v in o: _walk(v)
for f in glob.glob(P + '/Iris/packs/overworld/dimensions/*.json'):
    _walk(json.load(open(f)))
def ore_time(bid):
    b = ore_bpc.get(bid)
    return max(10.0, 10.0 * 48 / b) if b else 60.0
for bid, b in nexo_blocks.items():
    live = bool(iris_nexo.get(bid)) and bool(worlds_iris)
    for iid, v in b['items'].items():
        src[iid].append(dict(kind='nexo-blok', what=bid, ev=v, live=live, iris=iris_nexo.get(bid, [])))
# rybactwo
fish = []
for p, pr in profs.items():
    of = pr.get('on-fish') or {}
    for tname, t in of.items():
        entries = []
        for e in t.get('items', []) or []:
            m = re.match(r"\s*mmoitem\{([^}]*)\}\s+(\S+)\s+(\S+)\s+(\S+)", str(e))
            if m:
                pp = parse_params(m.group(1)); entries.append((pp, rng_avg(m.group(3)), float(m.group(4))))
        tot = sum(e[2] for e in entries)
        for pp, amt, w in entries:
            xp = rng_avg(pp.get('experience', '0'))
            fish.append(dict(id=pp['id'], p=w / tot, amt=amt, xp=xp, tugs=pp.get('tugs')))
            src[pp['id']].append(dict(kind='rybactwo', what='%s/%s' % (p, tname), ev=w / tot * amt, live=True))
fish_xp_per_catch = sum(f['p'] * f['xp'] for f in fish)
# receptury
for r in recs:
    if r['out'] and r['out'].get('id'):
        src[r['out']['id']].append(dict(kind='receptura', what='%s/%s' % (r['station'], r['rid']), ev=r['out']['amount'], live=True))
# komendy give poza MythicMobs (menu, skrypty), sklepy CoreTools, drop-tables MMOCore, loot-chests, MD loot
RX_MIREF = re.compile(r'mmoitems?\{[^}]*\bid=(\w+)', re.I)
for fp, txt in corp.items():
    if fp.startswith('plugins/MythicMobs/') or fp.startswith('plugins/MMOItems/crafting-stations') or fp.startswith('plugins/MMOCore/professions'):
        continue
    for i, l in enumerate(txt.split('\n')):
        if l.lstrip().startswith('#'): continue
        for m in RX_GIVE.finditer(l):
            src[m.group(2)].append(dict(kind='komenda-give', what='%s:%d' % (fp, i + 1), ev=0, live=True))
        if 'drop-tables' in fp or 'loot-chests' in fp or 'loottables' in fp:
            for m in RX_MIREF.finditer(l):
                src[m.group(1)].append(dict(kind='mmocore-drop/loot', what='%s:%d' % (fp, i + 1), ev=0, live=True))
for f in glob.glob(P + '/CoreTools/Shops/*.yml') + glob.glob(P + '/CoreTools/AuctionHouse/*.yml'):
    L = lines(f)
    for i, l in enumerate(L):
        m = re.match(r'\s*id:\s*(\w+)', l)
        if m and i > 0 and any('source: mmoitems' in L[j] for j in range(max(0, i - 3), i)):
            src[m.group(1)].append(dict(kind='sklep-coretools', what='%s:%d' % (rel(f), i + 1), ev=0, live=True, example='example' in f))

# ================================================================ UZYCIA
uses = collections.defaultdict(list)
for r in recs:
    for ing in r['ings']:
        if ing.get('id'):
            uses[ing['id']].append('%s/%s (x%d) %s:%s' % (r['station'], r['rid'], ing['amount'], r['file'], r['line']))

# ================================================================ A. PETLE
say('=' * 100); say('A. PETLE RECEPTUR'); say('=' * 100)
edges = collections.defaultdict(list)
for r in recs:
    if not (r['out'] and r['out'].get('id')): continue
    for ing in r['ings']:
        if ing.get('id'):
            edges[ing['id']].append((r['out']['id'], r))
say('Receptur: %d (stacji %d); krawedzi skladnik->wyjscie: %d' % (len(recs), len(set(r['station'] for r in recs)), sum(len(v) for v in edges.values())))
cycles = []
def dfs(start, node, path, rpath, seen):
    for nxt, r in edges.get(node, []):
        if nxt == start:
            cycles.append((path + [nxt], rpath + [r]))
        elif nxt not in seen and nxt > start:
            dfs(start, nxt, path + [nxt], rpath + [r], seen | {nxt})
for n in sorted(edges):
    dfs(n, n, [n], [], {n})
say('Cykli (proste, A->...->A): %d' % len(cycles))
for path, rp in cycles:
    ratio = 1.0; xp = 0
    for a, r in zip(path, rp):
        need = [i['amount'] for i in r['ings'] if i.get('id') == a][0]
        ratio *= r['out']['amount'] / need
        xp += sum(r['xp'].values())
    say('  CYKL', ' -> '.join(path), 'mnoznik ilosci=%.3f xp/obieg=%s' % (ratio, xp), ' | '.join('%s:%s' % (r['file'], r['line']) for r in rp))
self_loop = [r for r in recs if r['out'] and any(i.get('id') == r['out'].get('id') for i in r['ings'])]
no_ing = [r for r in recs if not r['ings']]
say('Receptury z wyjsciem == skladnikiem tego samego id: %d' % len(self_loop))
for r in self_loop: say('   ', r['station'], r['rid'], r['file'], r['line'])
say('Receptury bez skladnikow: %d' % len(no_ing))
for r in no_ing: say('   ', r['station'], r['rid'], r['file'], r['line'])
say('Receptury z wyjsciem ilosciowo > wejsciem (out.amount > suma skladnikow):')
for r in recs:
    if r['out'] and r['out'].get('amount', 1) > 1:
        say('   %s/%s: out %sx%d  <-  %s  %s:%s' % (r['station'], r['rid'], r['out']['id'], r['out']['amount'],
            ', '.join('%sx%d' % (i.get('id'), i.get('amount', 0)) for i in r['ings']), r['file'], r['line']))
say('Ten sam przedmiot wytwarzany przez >1 recepture (alternatywne sciezki):')
byout = collections.defaultdict(list)
for r in recs:
    if r['out'] and r['out'].get('id'): byout[r['out']['id']].append(r)
for k, v in sorted(byout.items()):
    if len(v) > 1:
        say('   %s: %s' % (k, ' | '.join('%s/%s [%s] xp=%s lvl=%s cls=%s %s:%s' % (r['station'], r['rid'], ', '.join('%sx%d' % (i.get('id'), i.get('amount', 0)) for i in r['ings']), r['xp'], r['prof_lvl'], r['cls_lvl'], r['file'], r['line']) for r in v)))
say('Receptury typu upgrade (klucz item:) : %d' % sum(1 for r in recs if r['kind'] == 'upgrade'))
say('Receptury, ktorych nazwa sugeruje upgrade, a maja output: (MMOItems traktuje je jako ZWYKLY craft):')
for r in recs:
    if 'upgrade' in r['rid'] and r['kind'] == 'craft':
        say('   %s/%s out=%s ingr=%s cond=%s %s:%s' % (r['station'], r['rid'], r['out'], [(i.get('id'), i.get('amount')) for i in r['ings']], r['conds'], r['file'], r['line']))

# ================================================================ C. warunki (potrzebne tez w B)
say(''); say('=' * 100); say('C. TRESC NIEOSIAGALNA / MARTWA'); say('=' * 100)
classes = {}
for f in glob.glob(P + '/MMOCore/classes/*.yml') + glob.glob(P + '/MMOCore/classes/*/*.yml'):
    d = load(f)
    if isinstance(d, dict) and ('display' in d or 'max-level' in d or 'options' in d):
        classes[os.path.basename(f)[:-4]] = d.get('max-level')
say('Klasy MMOCore i max-level:', classes)
say('Profesje MMOCore (max-level, exp-curve):', {k: (v['max-level'], v['exp-curve']) for k, v in profs.items()})
bad = []
for r in recs:
    why = []
    for c in r['conds']:
        if c['kind'] == 'profession':
            pn = c.get('profession')
            if pn not in profs: why.append('profesja %s NIE ISTNIEJE' % pn)
            elif int(float(c.get('level', 0))) > int(profs[pn]['max-level'] or 0): why.append('poziom %s > max %s' % (c.get('level'), profs[pn]['max-level']))
        if c['kind'] == 'level':
            mx = max([v for v in classes.values() if v] or [0])
            if int(float(c.get('level', 0))) > mx: why.append('level klasy %s > max %s' % (c.get('level'), mx))
        if c['kind'] == 'class':
            for cl in c.get('list', '').split(','):
                if cl.strip().lower() not in classes: why.append('klasa %s nie istnieje' % cl)
        if c['kind'] == 'permission':
            why.append('wymaga permisji %s (sprawdz LuckPerms)' % c.get('list'))
    for t in r['trig']:
        if t['kind'] == 'exp' and t.get('profession') not in profs:
            why.append('trigger exp do nieistniejacej profesji %s' % t.get('profession'))
    for i in r['ings'] + ([r['out']] if r['out'] else []):
        if i.get('id') and i['id'] not in items:
            why.append('id %s NIE ISTNIEJE w item/*.yml' % i['id'])
        elif i.get('id') and i.get('type') and items[i['id']]['type'] != i['type']:
            why.append('typ %s != faktyczny %s dla %s' % (i['type'], items[i['id']]['type'], i['id']))
        if i.get('raw'): why.append('niesparsowany: %s' % i['raw'])
    if r['out'] is None and r['kind'] == 'craft': why.append('brak output')
    if why: bad.append((r, why))
say('Receptury z problematycznymi warunkami/skladnikami: %d' % len(bad))
for r, why in bad:
    say('   %s/%s %s:%s -> %s' % (r['station'], r['rid'], r['file'], r['line'], '; '.join(why)))

# ================================================================ KOSZT SUROWCOW (sekundy zbierania na sztuke)
T_HARVEST10 = 3600 / 240.0   # 10 wezlow: 1 zbior / 15 s
T_KILL = 20.0
T_FISH = 25.0                # zalozenie: 1 polow / 25 s
T_ORE = 10.0                 # zalozenie: 1 blok rudy / 10 s (jesli istnieje w swiecie)
def gather_options(iid, mode):
    """mode='prod' - tylko to, co realnie istnieje; 'projekt' - kazdy wezel/ruda ma spawner/istnieje; moby tylko T1/T2 z randomspawnu."""
    opts = []
    for s in src.get(iid, []):
        live = s['live'] if mode == 'prod' else True
        if not live or s['ev'] <= 0: continue
        if s['kind'] == 'wezel': opts.append((T_HARVEST10 / s['ev'], s))
        elif s['kind'] == 'mob':
            if s.get('rs', 0) >= 0.01: opts.append((T_KILL / s['ev'], s))
        elif s['kind'] == 'rybactwo': opts.append((T_FISH / s['ev'], s))
        elif s['kind'] == 'nexo-blok': opts.append((ore_time(s['what']) / s['ev'], s))
    return sorted(opts, key=lambda x: x[0])

def cost_table(mode):
    INF = float('inf')
    cost = {i: INF for i in items}; how = {}
    for i in items:
        o = gather_options(i, mode)
        if o: cost[i] = o[0][0]; how[i] = ('zbior', o[0][1])
    for _ in range(50):
        ch = False
        for r in recs:
            if not (r['out'] and r['out'].get('id') in cost): continue
            if r['cls_lvl'] and r['station'] in ('steel-crafting-station',): pass
            c = 0
            for ing in r['ings']:
                c += ing['amount'] * cost.get(ing.get('id'), INF)
            c /= r['out']['amount']
            if c < cost[r['out']['id']] - 1e-9:
                cost[r['out']['id']] = c; how[r['out']['id']] = ('receptura', r); ch = True
        if not ch: break
    return cost, how

def expand(iid, qty, how, acc):
    h = how.get(iid)
    if h is None: acc['BRAK:' + iid] += qty; return
    if h[0] == 'zbior': acc[iid] += qty; return
    r = h[1]
    for ing in r['ings']:
        expand(ing['id'], qty * ing['amount'] / r['out']['amount'], how, acc)

def hours_for(iid, qty, mode):
    o = gather_options(iid, mode)
    if not o: return None, None, 'brak zrodla'
    s = o[0][1]
    if s['kind'] == 'wezel':
        n = qty / s['ev']; return n / 20.0, n / 240.0, '%s: %.2f szt./zbior -> %d zbiorow' % (s['what'], s['ev'], math.ceil(n))
    if s['kind'] == 'mob':
        n = qty / s['ev']; h = n * T_KILL / 3600; return h, h, '%s: %.3f szt./zabicie -> %d zabic' % (s['what'], s['ev'], math.ceil(n))
    if s['kind'] == 'rybactwo':
        n = qty / s['ev']; h = n * T_FISH / 3600; return h, h, 'polow: %.3f szt./polow -> %d polowow' % (s['ev'], math.ceil(n))
    if s['kind'] == 'nexo-blok':
        n = qty / s['ev']; h = n * ore_time(s['what']) / 3600; return h, h, 'blok %s: %.2f szt./blok, %.0f s/blok -> %d blokow' % (s['what'], s['ev'], ore_time(s['what']), math.ceil(n))

# ================================================================ B. BALANS
say(''); say('=' * 100); say('B. BALANS RZEMIOSLO vs ZBIERACTWO'); say('=' * 100)
curves = {n: load_curve(n) for n in ('rzemioslo', 'zbieractwo', 'levels')}
for n, c in curves.items():
    say('Krzywa %s: %d linii, suma 1->50 (linie 1..49) = %s' % (n, len(c), int(sum(c[:49]))))
B = {}
for mode in ('projekt', 'prod'):
    cost, how = cost_table(mode)
    say(''); say('--- Tryb: %s ---' % ('PROJEKT (kazdy typ wezla ma spawner, rudy Nexo sa w swiecie; moby T1/T2)' if mode == 'projekt' else 'PRODUKCJA (tylko realne zrodla: istniejace spawnery, randomspawny, rybactwo)'))
    say('Koszt surowca = sekundy zbierania na 1 szt. (10 wezlow: 15 s/zbior; mob 20 s/zabicie; polow 25 s; ruda max(10, 10*48/gestosc_na_chunk) s/blok)'); say('Gestosc zloz Iris (bloki/chunk):', ore_bpc)
    for p in sorted(profs):
        curve = curves[profs[p]['exp-curve']]
        R = [r for r in recs if r['xp'].get(p, 0) > 0]
        acc = collections.Counter(); path = []; stuck = None; crafts_total = 0; craft_time = 0
        for L in range(1, 50):
            need = curve[L - 1]
            best = None
            for r in R:
                if (r['prof_lvl'] or 0) > L: continue
                c = sum(i['amount'] * cost.get(i.get('id'), float('inf')) for i in r['ings'])
                if c == float('inf') or c <= 0: continue
                xp = r['xp'][p]
                for i in r['ings']:
                    h = how.get(i['id'])
                    if h and h[0] == 'receptura' and h[1]['xp'].get(p, 0) > 0 and (h[1]['prof_lvl'] or 0) <= L:
                        xp += i['amount'] / h[1]['out']['amount'] * h[1]['xp'][p]
                ratio = xp / c
                if best is None or ratio > best[0]: best = (ratio, r, xp, c)
            if best is None:
                stuck = L; break
            n = need / best[2]; crafts_total += n; craft_time += n * best[1]['time']
            for i in best[1]['ings']:
                expand(i['id'], i['amount'] * n, how, acc)
            path.append((L, best[1]['rid'], best[2], best[3]))
        # skompresuj sciezke
        segs = []
        for L, rid, xp, c in path:
            if segs and segs[-1][2] == rid: segs[-1][1] = L
            else: segs.append([L, L, rid, xp, c])
        h1 = h10 = 0; det = []
        for iid, q in sorted(acc.items(), key=lambda x: -x[1]):
            if iid.startswith('BRAK:'):
                det.append('%s: %.0f szt. (BRAK ZRODLA)' % (iid[5:], q)); continue
            a, b, note = hours_for(iid, q, mode)
            if a is None: det.append('%s: %.0f szt. (brak zrodla)' % (iid, q)); continue
            h1 += a; h10 += b; det.append('%s: %.0f szt. [%s] = %.1f h (1 wezel) / %.1f h (10 wezlow)' % (iid, q, note, a, b))
        B.setdefault(mode, {})[p] = dict(xp50=sum(curve[:49]), stuck=stuck, segs=segs, raw=dict(acc), h1=h1, h10=h10, crafts=crafts_total, craft_time_h=craft_time / 3600)
        say('')
        say('[%s] %s  krzywa=%s  XP 1->50=%d  receptur z XP=%d' % (p, profs[p]['name'], profs[p]['exp-curve'], sum(curve[:49]), len(R)))
        if stuck: say('   ZABLOKOWANE od poziomu %d: brak wykonalnej receptury (skladniki bez zrodla w tym trybie)' % stuck)
        for s in segs:
            say('   poziomy %2d-%2d: %-26s xp/craft (z lancuchem)=%.0f  koszt=%.0f s  -> %.2f XP/s zbierania' % (s[0], s[1], s[2], s[3], s[4], s[3] / s[4]))
        say('   craftow ~%.0f, czas kolejki craftu %.1f h' % (crafts_total, craft_time / 3600))
        for d in det: say('     -', d)
        if not stuck: say('   SUMA zbieractwa: %.1f h (1 wezel) / %.1f h (10 wezlow)' % (h1, h10))

say(''); say('--- Zbieractwo: czas do 50 samym zbieraniem ---')
zb = curves['zbieractwo']; tot = sum(zb[:49])
gxp = collections.defaultdict(list)
for mid, n in node_ev.items():
    for p, x in n['xp'].items(): gxp[p].append(('wezel ' + mid, x, n['live']))
for bid, b in nexo_blocks.items():
    if b['xp']: gxp['mining'].append(('blok ' + bid, b['xp'], bool(iris_nexo.get(bid)) and bool(worlds_iris)))
for mid, m in mob_drop_ev.items():
    for p, x in m['xp'].items(): gxp[p].append(('mob %s (rs=%s)' % (mid, m['rs_chance']), x, m['live']))
gxp['fishing'].append(('polow (srednia wazona)', fish_xp_per_catch, True))
for p in sorted(gxp):
    say('[%s] XP 1->50 = %d (krzywa %s)' % (p, sum(curves[profs[p]['exp-curve']][:49]), profs[p]['exp-curve']))
    for what, x, live in sorted(gxp[p], key=lambda z: -z[1]):
        n = sum(curves[profs[p]['exp-curve']][:49]) / x
        if what.startswith('wezel'):
            say('   %-45s %.2f XP/zbior -> %d zbiorow = %.1f h (1 wezel, 20/h) / %.1f h (10 wezlow, 240/h)  %s' % (what, x, n, n / 20, n / 240, '' if live else '[BRAK SPAWNERA]'))
        elif what.startswith('mob'):
            say('   %-45s %.0f XP/zabicie -> %d zabic = %.1f h (1/20 s)  %s' % (what, x, n, n * 20 / 3600, '' if live else '[NIE SPAWNUJE SIE]'))
        elif what.startswith('polow'):
            say('   %-45s %.2f XP/polow -> %d polowow = %.1f h (1/25 s)' % (what, x, n, n * 25 / 3600))
        else:
            say('   %-45s %.0f XP/blok -> %d blokow = %.1f h (%.0f s/blok, gestosc %.1f/chunk)  %s' % (what, x, n, n * ore_time(what[5:]) / 3600, ore_time(what[5:]), ore_bpc.get(what[5:], 0), '' if live else '[BLOK NIE GENERUJE SIE W SWIECIE PRODUKCYJNYM]'))

# ================================================================ C. moby / skille / droptable / itemy
say(''); say('--- C1. Moby MythicMobs ---')
ex_counts = collections.Counter()
for k, v in list(mobs.items()) + list(skills.items()) + list(dts.items()) + list(rsp.items()) + list(mmitems.items()):
    if v['example']: ex_counts[v['file']] += 1
say('Pliki Example* (ladowane przez MythicMobs - folder nie jest wylaczony):', dict(ex_counts))
mob_files = set(m['file'] for m in mobs.values())
for mid, mo in sorted(mobs.items()):
    if mo['example']: continue
    other = refs(corp, mid, exclude=())
    other = [h for h in other if not h.startswith(mo['file'] + ':%s' % mo['line'])]
    status = 'OSIAGALNY' if mid in reach_mob else 'BRAK SPAWNU'
    say('  %-24s %-12s %s:%s | spawn=%s | summon-przez=%s | MD=%s | inne-odwolania=%s' % (mid, status, mo['file'], mo['line'], spawn_src.get(mid, []), summoned_by.get(mid, []), md_mobs.get(mid, []), other[:6]))
say(''); say('--- C2. Skille MythicMobs (niewywolywane / nieosiagalne) ---')
for sk, s in sorted(skills.items()):
    if s['example']: continue
    callers = skill_called_by_mob[sk] | skill_called_by_skill[sk]
    other = [h for h in refs(corp, sk) if not h.startswith('%s:%s' % (s['file'], s['line']))]
    if not callers and not md_skills.get(sk):
        say('  NIEWYWOLANY %-28s %s:%s  inne-odwolania=%s' % (sk, s['file'], s['line'], other[:5]))
    elif sk not in reach_skill:
        say('  NIEOSIAGALNY %-27s %s:%s  wolany przez=%s (zaden nie spawnuje sie)' % (sk, s['file'], s['line'], sorted(callers)))
say(''); say('--- C3. Droptables ---')
for dt, d in sorted(dts.items()):
    users = [m for m, mo in mobs.items() if any(str(x).split()[0] == dt for x in (mo['cfg'] or {}).get('Drops', []) or [])]
    live = [m for m in users if m in reach_mob]
    say('  %-24s %s:%s uzywany przez=%s osiagalni=%s%s' % (dt, d['file'], d['line'], users, live, ' [EXAMPLE]' if d['example'] else ''))
say(''); say('--- C4. Przedmioty MMOItems: zrodla i uzycia (MATERIAL, CONSUMABLE, PROF_*, + skladniki) ---')
interesting = sorted(set(i for i in items if i.startswith('PROF_') or items[i]['type'] in ('MATERIAL', 'CONSUMABLE')) | set(uses))
no_src = []; blind = []; prod_no_src = []
for iid in interesting:
    it = items.get(iid)
    ss = src.get(iid, [])
    real = [s for s in ss if s['kind'] != 'komenda-give' or 'example' not in s['what']]
    live = [s for s in ss if s['live'] and not s.get('example')]
    desc = '; '.join('%s:%s%s' % (s['kind'], s['what'], '' if s['live'] else '(MARTWE)') for s in ss) or '-'
    u = uses.get(iid, [])
    tag = []
    if not ss: tag.append('BRAK_ZRODLA'); no_src.append(iid)
    elif not live: tag.append('BRAK_ZRODLA_NA_PRODUKCJI'); prod_no_src.append(iid)
    if not u and it and it['type'] == 'MATERIAL': tag.append('SLEPY(brak uzycia)'); blind.append(iid)
    say('  %-28s %-12s %s %s:%s\n      zrodla: %s\n      uzycia: %s' % (iid, it['type'] if it else 'NIE_ISTNIEJE', ','.join(tag), it['file'] if it else '-', it['line'] if it else '-', desc, '; '.join(x.split(' ')[0] for x in u) or '-'))
say('PODSUMOWANIE C4: bez zadnego zrodla=%d %s' % (len(no_src), no_src))
say('                 zrodlo tylko martwe (produkcja)=%d %s' % (len(prod_no_src), prod_no_src))
say('                 MATERIAL bez uzycia=%d %s' % (len(blind), blind))
# receptury niewykonalne
for mode in ('projekt', 'prod'):
    cost, how = cost_table(mode)
    inf = [r for r in recs if any(cost.get(i.get('id'), float('inf')) == float('inf') for i in r['ings'])]
    say('Receptury NIEWYKONALNE (skladnik bez zrodla) w trybie %s: %d/%d' % (mode, len(inf), len(recs)))
    for r in inf:
        say('   %s/%s: %s  %s:%s' % (r['station'], r['rid'], [i.get('id') for i in r['ings'] if cost.get(i.get('id'), float('inf')) == float('inf')], r['file'], r['line']))
say(''); say('--- C5. Dostep do stacji ---')
for st in sorted(set(r['station'] for r in recs)):
    rr = [r for r in recs if r['station'] == st]
    cmd = rr[0]['cmd']; perm = rr[0]['perm']
    openers = []
    for fp, txt in corp.items():
        if 'crafting-stations' in fp: continue
        for i, l in enumerate(txt.split('\n')):
            if l.lstrip().startswith('#'): continue
            if (cmd and re.search(r'(?<![\w-])/?' + re.escape(cmd) + r'(?![\w-])', l)) or re.search(r'stations?\s+open\s+' + re.escape(st), l):
                openers.append('%s:%d' % (fp, i + 1))
    say('  %-28s komenda=/%s permisja=%r  otwierane-z=%s' % (st, cmd, perm, openers or 'NIGDZIE (tylko wpisanie komendy)'))

say(''); say('--- C6. Receptury vs sloty GUI stacji (receptury poza slotami wymagaja stron next/previous) ---')
for f in sorted(glob.glob(P + '/MMOItems/crafting-stations/*.yml')):
    d = load(f); gi = ((d.get('gui-layout') or {}).get('items') or {})
    rslots = []; nav = []
    for k, v in gi.items():
        if isinstance(v, dict) and (v.get('function') == 'recipe' or k == 'recipe'): rslots = v.get('slots', [])
        if isinstance(v, dict) and ('page' in k or str(v.get('function', '')) in ('next', 'previous')): nav.append(k)
    n = len(d.get('recipes') or {})
    say('  %-30s receptur=%2d slotow=%2d nawigacja=%s %s' % (os.path.basename(f), n, len(rslots), nav or '-', 'PRZEPELNIENIE' if n > len(rslots) and not nav else ''))

# ================================================================ D. EKONOMIA
say(''); say('=' * 100); say('D. EKONOMIA'); say('=' * 100)
RX_MONEY = re.compile(r'(\[takemoney\]|\[givemoney\]|\beco\s+(give|take|add|set|remove)\b|money\{|type:\s*has money|core-?eco\w*\s|coreeconomy|\bpay\b\s|money\s+(give|take|add)|cmi\s+money|sell_vault|buy_vault|price)', re.I)
for fp, txt in sorted(corp.items()):
    hits = []
    for i, l in enumerate(txt.split('\n')):
        if l.lstrip().startswith('#'): continue
        if RX_MONEY.search(l): hits.append(i + 1)
    if hits:
        say('  %-60s %d trafien, linie: %s' % (fp, len(hits), hits[:12]))
money_conds = [r for r in recs if any(c['kind'] == 'money' for c in r['conds'])]
say('Receptury MMOItems z warunkiem money: %d' % len(money_conds))
say('Trigger command/money w recepturach:', sum(1 for r in recs for t in r['trig'] if t['kind'] in ('command', 'money', 'vanilla', 'mmoitem')))

# ================================================================ E. STACK
say(''); say('=' * 100); say('E. STACKOWANIE PROF_*'); say('=' * 100)
VAN16 = {'EGG', 'SNOWBALL', 'ENDER_PEARL', 'HONEY_BOTTLE', 'BUCKET', 'ARMOR_STAND', 'OAK_SIGN', 'WRITABLE_BOOK', 'WHITE_BANNER', 'BLUE_EGG', 'BROWN_EGG', 'WIND_CHARGE'}
VAN1_SUFFIX = ('_SWORD', '_PICKAXE', '_AXE', '_SHOVEL', '_HOE', '_HELMET', '_CHESTPLATE', '_LEGGINGS', '_BOOTS', '_BUCKET', '_STEW', '_SOUP', '_BOAT', '_RAFT', '_HORSE_ARMOR', '_MINECART', '_SHULKER_BOX', '_POTTERY_SHERD_X')
VAN1 = {'POTION', 'SPLASH_POTION', 'LINGERING_POTION', 'BOW', 'CROSSBOW', 'TRIDENT', 'SHIELD', 'FISHING_ROD', 'SHEARS', 'FLINT_AND_STEEL', 'ELYTRA', 'TOTEM_OF_UNDYING', 'MUSHROOM_STEW', 'RABBIT_STEW', 'BEETROOT_SOUP', 'SUSPICIOUS_STEW', 'SADDLE', 'CAKE', 'ENCHANTED_BOOK', 'WRITTEN_BOOK', 'MACE', 'BRUSH', 'SPYGLASS', 'GOAT_HORN', 'BUNDLE', 'MUSIC_DISC_CAT', 'CARROT_ON_A_STICK', 'WARPED_FUNGUS_ON_A_STICK', 'DEBUG_STICK', 'KNOWLEDGE_BOOK', 'TURTLE_HELMET', 'MILK_BUCKET', 'WATER_BUCKET', 'LAVA_BUCKET', 'HONEY_BOTTLE_X', 'STICK_X'}
def vstack(mat):
    if mat in VAN1 or mat.endswith(VAN1_SUFFIX) or mat.startswith('MUSIC_DISC'): return 1
    if mat in VAN16 or mat.endswith('_SIGN') or mat.endswith('_BANNER') or mat.endswith('_HANGING_SIGN'): return 16
    return 64
STACK_KEYS = ('max-stack', 'max-stack-size', 'unstackable', 'soulbound', 'gen-template', 'max-durability', 'durability', 'item-damage', 'unbreakable', 'tier', 'required-level', 'item-cooldown', 'max-consume', 'amount')
def random_like(v, path=''):
    out = []
    if isinstance(v, dict):
        if any(k in v for k in ('min', 'max', 'spread', 'max-spread', 'scale', 'base')) and path.split('.')[-1] not in ('base',):
            out.append(path)
        for k, x in v.items(): out += random_like(x, path + '.' + str(k))
    elif isinstance(v, str) and re.fullmatch(r'\s*-?\d+(\.\d+)?\s*[-~]\s*-?\d+(\.\d+)?\s*', v) and 'lore' not in path:
        out.append('%s=%s' % (path, v))
    elif isinstance(v, list):
        for i, x in enumerate(v): out += random_like(x, '%s[%d]' % (path, i))
    return out
nonstack = []
rows = []
for iid in sorted(i for i in items if i.startswith('PROF_')):
    it = items[iid]; b = it['base']
    mat = it['material']; vs = vstack(mat)
    keys = sorted(b.keys()); special = [k for k in keys if k in STACK_KEYS]
    rnd = random_like({k: v for k, v in b.items() if k not in ('lore', 'name')}, 'base')
    top = [k for k in it['cfg'] if k != 'base']
    rows.append((iid, it['type'], mat, vs, special, rnd, top))
    if vs < 64 or 'unstackable' in b or rnd or 'gen-template' in b: nonstack.append(iid)
say('PROF_* przedmiotow: %d' % len(rows))
say('%-28s %-11s %-18s %5s  %s' % ('id', 'typ', 'material', 'stack', 'klucze wplywajace/losowe'))
for r in rows:
    say('%-28s %-11s %-18s %5d  %s %s %s' % (r[0], r[1], r[2], r[3], r[4] or '', ('LOSOWE:' + str(r[5])) if r[5] else '', ('SEKCJE:' + str(r[6])) if r[6] else ''))
say('Nie stackuja sie do 64 lub maja losowe/unikalne dane: %d %s' % (len(nonstack), nonstack))
say('Wszystkie klucze base uzyte w PROF_*:', sorted(collections.Counter(k for i in items if i.startswith('PROF_') for k in items[i]['base']).items()))

open('/o/wynik.txt', 'w').write('\n'.join(OUT))
json.dump(dict(B=B, node_ev=node_ev, mob_drop_ev=mob_drop_ev, nexo_blocks=nexo_blocks, fish=fish, world_gen=world_gen,
               worlds_iris=worlds_iris, level_name=level_name, bukkit_worlds={k: (v or {}).get('generator') for k, v in bukkit.items()}),
          open('/o/wynik.json', 'w'), indent=1, default=str)
print('\n'.join(OUT))
