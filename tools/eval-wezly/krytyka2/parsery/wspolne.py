# -*- coding: utf-8 -*-
"""Wspolne ladowanie danych dla krytyki profesji (krytyka2).
Uruchamiane w kontenerze: /d = /opt/docker/mc/data/world1, /o = katalog skryptow.
Tylko ODCZYT."""
import os, re, glob, yaml, json, collections

ROOT = '/d'
P = ROOT + '/plugins'

class Loader(yaml.SafeLoader):
    pass

def load(path):
    with open(path, encoding='utf-8', errors='replace') as f:
        return yaml.load(f, Loader=Loader) or {}

def lines(path):
    with open(path, encoding='utf-8', errors='replace') as f:
        return f.read().split('\n')

def rel(path):
    return path.replace(ROOT + '/', '')

def find_line(path, pattern, start=0):
    """Numer linii (1-based) pierwszego dopasowania regex od linii start."""
    rx = re.compile(pattern)
    for i, l in enumerate(lines(path)):
        if i >= start and rx.search(l):
            return i + 1
    return None

def key_line(path, key):
    return find_line(path, r"^\s*'?\"?" + re.escape(key) + r"'?\"?\s*:")

# ---------------------------------------------------------------- MMOItems
def parse_params(s):
    """'type=MATERIAL,id=X,amount=3' lub z ';' -> dict (klucze lower)."""
    out = {}
    for part in re.split(r'[;,]', s):
        if '=' in part:
            k, v = part.split('=', 1)
            out[k.strip().lower()] = v.strip().strip('"')
    return out

RX_MI = re.compile(r'(mmoitems?)\{([^}]*)\}', re.I)

def load_items():
    items = {}  # id -> dict
    for f in sorted(glob.glob(P + '/MMOItems/item/*.yml')):
        typ = os.path.basename(f)[:-4].upper()
        d = load(f)
        for iid, cfg in (d or {}).items():
            if not isinstance(cfg, dict):
                continue
            base = cfg.get('base', {}) if isinstance(cfg.get('base'), dict) else {}
            items[iid] = dict(id=iid, type=typ, file=rel(f), line=key_line(f, iid),
                              base=base, material=str(base.get('material', '?')).upper(), cfg=cfg)
    return items

def load_recipes():
    recs = []
    for f in sorted(glob.glob(P + '/MMOItems/crafting-stations/*.yml')):
        st = os.path.basename(f)[:-4]
        d = load(f)
        cmd = d.get('command') or {}
        for rid, r in (d.get('recipes') or {}).items():
            if not isinstance(r, dict):
                continue
            ln = key_line(f, rid)
            kind = 'upgrade' if 'item' in r and 'output' not in r else 'craft'
            out = None
            if 'output' in r:
                m = RX_MI.search(str(r['output']))
                if m:
                    p = parse_params(m.group(2))
                    out = dict(type=p.get('type', '').upper(), id=p.get('id'), amount=int(float(p.get('amount', 1))))
                else:
                    out = dict(raw=str(r['output']))
            ings = []
            for ing in r.get('ingredients') or []:
                m = RX_MI.search(str(ing))
                if m:
                    p = parse_params(m.group(2))
                    ings.append(dict(type=p.get('type', '').upper(), id=p.get('id'), amount=int(float(p.get('amount', 1)))))
                else:
                    ings.append(dict(raw=str(ing)))
            conds = []
            for c in r.get('conditions') or []:
                m = re.match(r'\s*(\w+)\{(.*)\}\s*$', str(c))
                if m:
                    conds.append(dict(kind=m.group(1).lower(), **parse_params(m.group(2))))
                else:
                    conds.append(dict(kind='?', raw=str(c)))
            trig = []
            for t in (r.get('triggers') or []) + (r.get('on-use') or []):
                m = re.match(r'\s*(\w+)\{(.*)\}\s*$', str(t))
                if m:
                    trig.append(dict(kind=m.group(1).lower(), **parse_params(m.group(2))))
            xp = {}
            for t in trig:
                if t['kind'] == 'exp':
                    xp[t.get('profession')] = xp.get(t.get('profession'), 0) + float(t.get('amount', 0))
            prof_lvl = None
            prof_name = None
            cls_lvl = None
            for c in conds:
                if c['kind'] == 'profession':
                    prof_name = c.get('profession'); prof_lvl = int(float(c.get('level', 0)))
                if c['kind'] == 'level':
                    cls_lvl = int(float(c.get('level', 0)))
            recs.append(dict(station=st, file=rel(f), line=ln, rid=rid, kind=kind, out=out, ings=ings,
                             conds=conds, trig=trig, xp=xp, prof=prof_name, prof_lvl=prof_lvl, cls_lvl=cls_lvl,
                             time=float(r.get('crafting-time', 0) or 0), options=r.get('options', {}),
                             cmd=cmd.get('name'), perm=cmd.get('permission'), item=r.get('item')))
    return recs

# ---------------------------------------------------------------- MMOCore
def load_curve(name):
    f = P + '/MMOCore/exp-curves/%s.txt' % name
    return [float(x) for x in open(f).read().split() if x.strip()]

def load_profs():
    profs = {}
    for f in sorted(glob.glob(P + '/MMOCore/professions/*.yml')):
        d = load(f)
        profs[os.path.basename(f)[:-4]] = dict(file=rel(f), **{k: d.get(k) for k in ('name', 'max-level', 'exp-curve', 'exp-sources', 'on-fish')},
                                               raw=d)
    return profs

# ---------------------------------------------------------------- MythicMobs
def mm_yaml(sub):
    out = {}
    for f in sorted(glob.glob(P + '/MythicMobs/%s/*.yml' % sub)):
        d = load(f)
        for k, v in (d or {}).items():
            out[k] = dict(file=rel(f), line=key_line(f, k), cfg=v, example=os.path.basename(f).startswith('Example'))
    return out

RX_GIVE = re.compile(r'(?:mi|mmoitems)\s+give\s+(\w+)\s+(\w+)\s+\S+\s+(\d+)', re.I)
RX_EXP = re.compile(r'mmocore\s+admin\s+exp\s+give\s+\S+\s+(\w+)\s+(\d+(?:\.\d+)?)', re.I)
RX_SKILLREF = re.compile(r'\bskill\{s(?:kill)?=([\w-]+)', re.I)
RX_RANDOMSKILL = re.compile(r'randomskill\{skills=([^}]*)\}', re.I)

def skill_ev(skills, name, seen=None):
    """Wartosc oczekiwana (itemy, xp) jednego wywolania skilla."""
    seen = seen or set()
    items = collections.Counter(); xp = collections.Counter()
    if name not in skills or name in seen:
        return items, xp
    seen = seen | {name}
    for line in skills[name]['cfg'].get('Skills', []) or []:
        s = str(line)
        for m in RX_GIVE.finditer(s):
            items[m.group(2)] += float(m.group(3))
        for m in RX_EXP.finditer(s):
            xp[m.group(1)] += float(m.group(2))
        m = RX_RANDOMSKILL.search(s)
        if m:
            parts = [p.strip().split() for p in m.group(1).split(',') if p.strip()]
            tot = sum(float(p[1]) if len(p) > 1 else 1 for p in parts)
            for p in parts:
                w = (float(p[1]) if len(p) > 1 else 1) / tot
                i2, x2 = skill_ev(skills, p[0], seen)
                for k, v in i2.items(): items[k] += v * w
                for k, v in x2.items(): xp[k] += v * w
        for m in RX_SKILLREF.finditer(s):
            i2, x2 = skill_ev(skills, m.group(1), seen)
            items.update(i2); xp.update(x2)
    return items, xp

def rng_avg(s):
    s = str(s)
    if '-' in s:
        a, b = s.split('-', 1)
        return (float(a) + float(b)) / 2
    return float(s)

RX_DROPLINE = re.compile(r'mmoitem\{([^}]*)\}\s+(\S+)\s*(\S+)?', re.I)

def droptable_ev(dts, name):
    ev = collections.Counter()
    for line in dts.get(name, {}).get('cfg', {}).get('Drops', []) or []:
        m = RX_DROPLINE.search(str(line))
        if m:
            p = parse_params(m.group(1))
            amt = rng_avg(m.group(2)); ch = float(m.group(3)) if m.group(3) else 1.0
            ev[p.get('id')] += amt * ch
    return ev

# ---------------------------------------------------------------- korpus tekstowy
SKIP_DIRS = ('userdata', 'playerdata', 'globalplayerdata', '/data/players', '/data/worlds', 'Iris/cache', 'LuckPerms/libs',
             'CMI/Translations', 'CMI/Saves', 'CMI/Logs', 'CMI/DatabaseBackups', 'spark', 'bStats', 'Nexo/pack', 'backups',
             'Iris/prefetch', 'Iris/convert', 'FastAsyncWorldEdit', 'MythicMobs/generation', 'plugindata', 'Chunky')
TEXT_EXT = ('.yml', '.yaml', '.json', '.txt', '.properties', '.conf', '.toml', '.sk', '.js', '.mcfunction')

def corpus():
    out = {}
    for dp, dn, fn in os.walk(P):
        if any(s in dp for s in SKIP_DIRS):
            continue
        for f in fn:
            if not f.endswith(TEXT_EXT) or '.bak' in f:
                continue
            fp = os.path.join(dp, f)
            try:
                if os.path.getsize(fp) > 4_000_000:
                    continue
                out[rel(fp)] = open(fp, encoding='utf-8', errors='replace').read()
            except Exception:
                pass
    return out

def refs(corp, token, exclude=()):
    """Pliki:linie z tokenem (granica slowa), bez plikow z exclude."""
    rx = re.compile(r'(?<![A-Za-z0-9_])' + re.escape(token) + r'(?![A-Za-z0-9_])')
    hits = []
    for fp, txt in corp.items():
        if fp in exclude:
            continue
        if token not in txt:
            continue
        for i, l in enumerate(txt.split('\n')):
            if rx.search(l) and not l.lstrip().startswith('#'):
                hits.append('%s:%d' % (fp, i + 1))
    return hits
