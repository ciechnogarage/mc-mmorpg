#!/usr/bin/env python3
"""Audyt profesji: graf zrodel/ujsc, slepe zaulki, bilans XP, puste poziomy."""
import glob, os, re, json, collections
import yaml

P = "."
def w(p):
    try:
        with open(p, encoding="utf-8") as f: return yaml.safe_load(f) or {}
    except Exception as e:
        print("YAML FAIL", p, e); return {}

# --- katalog przedmiotow ---
items = {}           # "TYPE:ID" -> dane
for p in glob.glob("MMOItems/item/*.yml"):
    typ = os.path.basename(p)[:-4].upper()
    for iid, d in (w(p) or {}).items():
        items[f"{typ}:{iid}"] = d or {}
prof_items = {k for k in items if k.split(":")[1].startswith("PROF_")}

# --- receptury ---
RE_ITEM = re.compile(r"mmoitems?\{([^}]*)\}")
def parse_ref(s):
    m = RE_ITEM.search(s)
    if not m: return None, 1
    kv = dict(re.findall(r"(\w+)=([^,;]+)", m.group(1)))
    t, i = kv.get("type"), kv.get("id")
    amt = kv.get("amount", "1")
    try: amt = int(amt)
    except: amt = 1
    return (f"{t}:{i}" if t and i else None), amt

recipes = []   # dict(station, name, out, outamt, ings{}, prof, lvl, xp)
for p in glob.glob("MMOItems/crafting-stations/*.yml"):
    st = os.path.basename(p)[:-4]
    d = w(p)
    for rname, r in (d.get("recipes") or {}).items():
        if not isinstance(r, dict): continue
        out, oamt = parse_ref(str(r.get("output","")))
        ings = {}
        for ing in (r.get("ingredients") or []):
            k, a = parse_ref(str(ing))
            if k: ings[k] = ings.get(k,0)+a
        prof=lvl=None; xp=0; plvl=None
        for c in (r.get("conditions") or []):
            m = re.search(r"profession\{profession=(\w+),level=(\d+)\}", str(c))
            if m: prof, lvl = m.group(1), int(m.group(2))
            m2 = re.search(r"^level\{level=(\d+)\}", str(c).strip())
            if m2: plvl = int(m2.group(1))
        for t in (r.get("triggers") or []):
            m = re.search(r"exp\{profession=(\w+),amount=(\d+)\}", str(t))
            if m:
                xp = int(m.group(2))
                if not prof: prof = m.group(1)
        recipes.append(dict(station=st,name=rname,out=out,oamt=oamt,ings=ings,
                            prof=prof,lvl=lvl,xp=xp,plvl=plvl,time=r.get("crafting-time",0)))

# --- zrodla ze swiata ---
sources = collections.defaultdict(list)   # "TYPE:ID" -> [(zrodlo, xp, prof, amt)]
RE_GIVE = re.compile(r'mi give (\w+) (\S+) <[^>]*> (\d+)')
RE_EXP  = re.compile(r'mmocore admin exp give <[^>]*> (\w+) ([\d.]+)')
for p in glob.glob("MythicMobs/**/*.yml", recursive=True):
    d = w(p); txt = open(p,encoding="utf-8").read()
    for name, node in (d or {}).items():
        if not isinstance(node, dict): continue
        blob = yaml.safe_dump(node, allow_unicode=True)
        gives = RE_GIVE.findall(blob); exps = RE_EXP.findall(blob)
        # drops MythicMobs
        for dr in (node.get("Drops") or []):
            m = re.match(r'mmoitems\s+(\w+)\s+(\S+)\s+([\d-]+)', str(dr).strip())
            if m: sources[f"{m.group(1)}:{m.group(2)}"].append((f"{os.path.basename(p)}:{name}",0,None,m.group(3)))
        for t,i,a in gives:
            xp = int(float(exps[0][1])) if exps else 0
            pr = exps[0][0] if exps else None
            sources[f"{t}:{i}"].append((f"{os.path.basename(p)}:{name}", xp, pr, a))
# droptables
for p in glob.glob("MythicMobs/droptables/*.yml"):
    d = w(p)
    for name, node in (d or {}).items():
        blob = yaml.safe_dump(node, allow_unicode=True) if isinstance(node,(dict,list)) else str(node)
        for m in re.finditer(r'mmoitems\s+(\w+)\s+(\S+)', blob):
            sources[f"{m.group(1)}:{m.group(2)}"].append((f"droptable:{name}",0,None,"?"))
# rybactwo
fi = w("MMOCore/professions/fishing.yml")
for entry in ((fi.get("on-fish") or {}).get("default") or {}).get("items", []):
    m = re.search(r"type=(\w+);id=(\w+)", str(entry))
    mx = re.search(r"experience=(\d+)-(\d+)", str(entry))
    if m: sources[f"{m.group(1)}:{m.group(2)}"].append(("on-fish", int(mx.group(2)) if mx else 0,"fishing","?"))

# --- ujscia ---
consumed = collections.Counter()
for r in recipes:
    for k,a in r["ings"].items(): consumed[k]+=1
produced = collections.Counter()
for r in recipes:
    if r["out"]: produced[r["out"]]+=1

print("="*70); print("1. SLEPE ZAULKI")
EQUIP = {"ARMOR","SWORD","AXE","BOW","CROSSBOW","DAGGER","GREATSWORD","GREATAXE","GREATHAMMER",
 "GREATSTAFF","HALBERD","HAMMER","KATANA","LANCE","LONG_SWORD","MUSKET","SPEAR","STAFF","SHIELD",
 "THRUSTING_SWORD","WAND","WHIP","TOOL","RING","NECKLACE","BELT","ORNAMENT","TALISMAN",
 "CHARM","GLOVES","GAUNTLET","OFF_CATALYST","CATALYST","GEM_STONE","CONSUMABLE","TOME"}
dead = []
for k in sorted(set(list(produced)+list(sources))):
    if not k or k.split(":")[1].startswith("PROF_")==False: continue
    t = k.split(":")[0]
    if consumed[k]==0 and t not in EQUIP:
        srcs = [s[0] for s in sources.get(k,[])]
        dead.append((k, produced[k], len(srcs), srcs[:3]))
for k,pc,sc,ss in dead:
    print(f"  MARTWY: {k:38s} wytwarzany_w_recepturach={pc} zrodel_swiata={sc} {ss}")
print(f"  razem martwych PROF_*: {len(dead)}")

print(); print("="*70); print("2. SUROWCE Z JEDNEGO ZRODLA / BEZ ZRODLA")
for k in sorted(prof_items):
    ns = len(sources.get(k,[])); nr = produced[k]
    if ns+nr==0 and consumed[k]>0:
        print(f"  NIEZDOBYWALNY (a uzywany w {consumed[k]} recepturach): {k}")
print()
for k in sorted(prof_items):
    ns=len(sources.get(k,[]))
    if ns==1 and produced[k]==0 and consumed[k]>0:
        print(f"  JEDNO ZRODLO: {k:38s} <- {sources[k][0][0]}  (uzyc: {consumed[k]})")

print(); print("="*70); print("3. BILANS XP DO 50")
def total_xp(curve_a,curve_b,curve_c,maxl=50):
    # exp-curve dla poziomu L = koszt przejscia z L na L+1
    return sum(curve_a + L*curve_b + L*L*curve_c for L in range(1,maxl))
profs = {}
for p in sorted(glob.glob("MMOCore/professions/*.yml")):
    n = os.path.basename(p)[:-4]; d = w(p)
    profs[n]=d
    c = d.get("exp-curve","")
    m = re.match(r"([\d.]+) \+ \{level\} \* ([\d.]+) \+ \{level\} \* \{level\} \* ([\d.]+)", c)
    a,b,cc = (float(m.group(1)),float(m.group(2)),float(m.group(3))) if m else (0,0,0)
    tot = total_xp(a,b,cc)
    rs = [r for r in recipes if r["prof"]==n]
    xps = [r["xp"] for r in rs if r["xp"]]
    wsrc = [(s,xp) for k,v in sources.items() for (s,xp,pr,a2) in v if pr==n]
    best_node = max([xp for _,xp in wsrc], default=0)
    best_rec = max(xps, default=0)
    d["_"]=dict(tot=tot,rs=len(rs),best_rec=best_rec,best_node=best_node,curve=c)
    lv = sorted({r["lvl"] for r in rs if r["lvl"] is not None})
    print(f"  {n:14s} krzywa={c[:38]:40s} XP_do_50={tot:>9.0f}  receptur={len(rs):2d} maxXP/craft={best_rec:3d} maxXP/zbior={best_node:3d} bramki={lv}")
    if best_rec: print(f"{'':16s}-> craftow po najlepszej recepturze: {tot/best_rec:,.0f}")
    if best_node: print(f"{'':16s}-> zbiorow po najlepszym wezle:     {tot/best_node:,.0f}")

print(); print("="*70); print("4. ZRODLA XP POZA WARSZTATEM (per profesja)")
for n in sorted(profs):
    d=profs[n]
    kinds=[]
    if d.get("on-fish"): kinds.append("on-fish")
    if d.get("exp-sources"): kinds.append(f"exp-sources={d['exp-sources']}")
    if d.get("alchemy-experience"): kinds.append("alchemy-experience(vanilla brewing)")
    if d.get("repair-exp"): kinds.append("repair-exp(vanilla repair)")
    if d.get("base-enchant-exp"): kinds.append("base-enchant-exp(vanilla enchant)")
    nodes = sorted({s for k,v in sources.items() for (s,xp,pr,a2) in v if pr==n})
    print(f"  {n:14s} {kinds if kinds else 'BRAK'}  wezly/moby={len(nodes)} {nodes[:6]}")

print(); print("="*70); print("5. RECEPTURY: rozklad bramek poziomow")
bylvl=collections.Counter()
for r in recipes:
    if r["lvl"] is not None: bylvl[r["lvl"]]+=1
print("  ", dict(sorted(bylvl.items())))

print(); print("="*70); print("6. KUZNIE (arcane/mythical/steel) - warunki")
for st in ["arcane-forge","mythical-forge","steel-crafting-station"]:
    rs=[r for r in recipes if r["station"]==st]
    print(f"  {st}: receptur={len(rs)} warunki_profesji={sorted({(r['prof'],r['lvl']) for r in rs if r['prof']})} warunki_poziomu_gracza={sorted({r['plvl'] for r in rs if r['plvl']})}")
    for r in rs:
        if not r["prof"]: print(f"      bez warunku profesji: {r['name']} -> {r['out']}")

json.dump({"recipes":recipes,"sources":{k:v for k,v in sources.items()}}, open("/tmp/out/graf.json","w"), default=str)
