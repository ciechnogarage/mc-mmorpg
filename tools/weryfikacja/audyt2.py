#!/usr/bin/env python3
"""Audyt profesji, czesc 2: zaleznosci miedzy profesjami, dostepnosc zrodel."""
import glob, os, re, collections, yaml
def w(p):
    try: return yaml.safe_load(open(p,encoding="utf-8")) or {}
    except Exception as e: print("FAIL",p,e); return {}

RE_REF=re.compile(r"mmoitems?\{([^}]*)\}")
def ref(s):
    m=RE_REF.search(s)
    if not m: return None,1
    kv=dict(re.findall(r"(\w+)\s*=\s*([^,;]+)",m.group(1)))
    a=kv.get("amount","1")
    try:a=int(a)
    except:a=1
    return (f"{kv.get('type')}:{kv.get('id')}" if kv.get("type") and kv.get("id") else None),a

# receptury
rec=[]
for p in glob.glob("MMOItems/crafting-stations/*.yml"):
    st=os.path.basename(p)[:-4]; d=w(p)
    for n,r in (d.get("recipes") or {}).items():
        if not isinstance(r,dict): continue
        o,oa=ref(str(r.get("output","")))
        ing={}
        for i in (r.get("ingredients") or []):
            k,a=ref(str(i))
            if k: ing[k]=ing.get(k,0)+a
        pr=lv=None; xp=0
        for c in (r.get("conditions") or []):
            m=re.search(r"profession\{profession=(\w+),level=(\d+)\}",str(c))
            if m: pr,lv=m.group(1),int(m.group(2))
        for t in (r.get("triggers") or []):
            m=re.search(r"exp\{profession=(\w+),amount=(\d+)\}",str(t))
            if m: xp=int(m.group(2)); pr=pr or m.group(1)
        rec.append(dict(st=st,n=n,out=o,ing=ing,pr=pr,lv=lv,xp=xp))

# zrodla swiata
src=collections.defaultdict(set)   # item -> {zrodlo}
mob_of_dt=collections.defaultdict(set)
for p in glob.glob("MythicMobs/droptables/*.yml"):
    for dt,node in (w(p) or {}).items():
        if not isinstance(node,dict): continue
        for dr in (node.get("Drops") or []):
            m=re.search(r"mmoitems?\{type=(\w+);id=([A-Za-z0-9_]+)\}",str(dr))
            if m: src[f"{m.group(1)}:{m.group(2)}"].add(f"droptable:{dt}")
mobs={}
for p in glob.glob("MythicMobs/mobs/*.yml"):
    for n,node in (w(p) or {}).items():
        if isinstance(node,dict): mobs[n]=node
# wezly: mi give w skillach
node_prof={}
for p in glob.glob("MythicMobs/skills/*.yml"):
    for n,node in (w(p) or {}).items():
        blob=yaml.safe_dump(node,allow_unicode=True) if isinstance(node,(dict,list)) else str(node)
        for t,i,a in re.findall(r'mi give (\w+) ([A-Za-z0-9_]+) <[^>]*> (\d+)',blob):
            src[f"{t}:{i}"].add(f"wezel:{n}")
        for pr,x in re.findall(r'mmocore admin exp give <[^>]*> (\w+) ([\d.]+)',blob):
            node_prof[n]=(pr,float(x))
# nexo bloki
for p in glob.glob("Nexo/items/*.yml"):
    txt=open(p,encoding="utf-8").read()
    for i in re.findall(r"mmoitems_id:\s*([A-Za-z0-9_]+)",txt):
        src[f"MATERIAL:{i}"].add("nexo:blok")
# rybactwo
for e in (((w("MMOCore/professions/fishing.yml").get("on-fish") or {}).get("default") or {}).get("items") or []):
    m=re.search(r"type=(\w+);id=([A-Za-z0-9_]+)",str(e))
    if m: src[f"{m.group(1)}:{m.group(2)}"].add("on-fish")

produced={r["out"] for r in rec if r["out"]}
consumed=collections.Counter()
for r in rec:
    for k in r["ing"]: consumed[k]+=1

# --- randomspawns: ktore moby faktycznie spawnuja ---
spawnowane=set()
for p in glob.glob("MythicMobs/randomspawns/*.yml"):
    for n,node in (w(p) or {}).items():
        if isinstance(node,dict) and node.get("Type"): spawnowane.add(node["Type"])
print("="*78); print("A. MOBY BESTIARIUSZA BEZ LOSOWEGO SPAWNU")
best=[m for m in mobs if re.match(r"^(BEAST|UNDEAD|ELITE|BOSS|HUMANOID|MAGIC)_",m)]
brak=[m for m in sorted(best) if m not in spawnowane]
print(f"  mobow bestiariusza: {len(best)}, z losowym spawnem: {len(best)-len(brak)}, BEZ: {len(brak)}")
for m in brak: print(f"    {m}")

print(); print("="*78); print("B. PRZEDMIOTY DOSTEPNE TYLKO Z MOBOW BEZ SPAWNU")
dt_of_mob={m:set(n.get("Drops") or []) for m,n in mobs.items()}
for it,ss in sorted(src.items()):
    if not it.startswith(("MATERIAL","MISCELLANEOUS")): continue
    dts={s.split(":",1)[1] for s in ss if s.startswith("droptable:")}
    if not dts or len(ss)!=len(dts): continue
    mm={m for m,d in dt_of_mob.items() if d & dts}
    if mm and not (mm & spawnowane):
        print(f"  {it:34s} tylko z: {sorted(mm)} (brak losowego spawnu)")

print(); print("="*78); print("C. ZALEZNOSCI MIEDZY PROFESJAMI (skladniki z cudzej profesji)")
# kto produkuje co
owner={}
for r in rec:
    if r["out"] and r["pr"]: owner.setdefault(r["out"],set()).add(r["pr"])
for n,(pr,x) in node_prof.items():
    for it,ss in src.items():
        if f"wezel:{n}" in ss: owner.setdefault(it,set()).add(pr)
for it,ss in src.items():
    if "on-fish" in ss: owner.setdefault(it,set()).add("fishing")
    if "nexo:blok" in ss: owner.setdefault(it,set()).add("mining")
    if any(s.startswith("droptable:") for s in ss): owner.setdefault(it,set()).add("hunting")
profs=sorted({r["pr"] for r in rec if r["pr"]})
print(f"  {'profesja':15s} {'wlasne':>7s} {'obce':>5s}  obce zrodla skladnikow")
for pn in profs:
    wl=ob=0; skad=collections.Counter()
    for r in rec:
        if r["pr"]!=pn: continue
        for k in r["ing"]:
            o=owner.get(k,set())
            if pn in o: wl+=1
            else:
                ob+=1
                for q in (o or {"BRAK ZRODLA"}): skad[q]+=1
    print(f"  {pn:15s} {wl:7d} {ob:5d}  {dict(skad)}")

print(); print("="*78); print("D. WYROBY MARTWE PER PROFESJA")
EQ={"ARMOR","SWORD","AXE","BOW","CROSSBOW","DAGGER","GREATSWORD","GREATAXE","GREATHAMMER","GREATSTAFF",
"HALBERD","HAMMER","KATANA","LANCE","LONG_SWORD","MUSKET","SPEAR","STAFF","SHIELD","THRUSTING_SWORD",
"WAND","WHIP","TOOL","RING","NECKLACE","BELT","ORNAMENT","TALISMAN","CHARM","GLOVES",
"GAUNTLET","OFF_CATALYST","CATALYST","GEM_STONE","CONSUMABLE","TOME"}
print(f"  {'profesja':15s} {'receptur':>8s} {'wyrobow':>8s} {'MARTWYCH':>9s}  martwe")
for pn in profs:
    outs=[r["out"] for r in rec if r["pr"]==pn and r["out"]]
    dead=[o for o in set(outs) if consumed[o]==0 and o.split(":")[0] not in EQ]
    print(f"  {pn:15s} {len([r for r in rec if r['pr']==pn]):8d} {len(set(outs)):8d} {len(dead):9d}  {sorted(x.split(':')[1] for x in dead)}")

print(); print("="*78); print("E. BRAMKI POZIOMOW PER PROFESJA (co gracz odblokowuje i kiedy)")
for pn in profs:
    lv=sorted([r["lv"] for r in rec if r["pr"]==pn and r["lv"] is not None])
    puste=[L for L in range(1,51) if L not in lv]
    seg=[]
    a=None
    for L in range(1,52):
        if L in puste and a is None: a=L
        elif L not in puste and a is not None: seg.append(f"{a}-{L-1}" if L-1>a else f"{a}"); a=None
    print(f"  {pn:15s} odblokowania na poziomach {sorted(set(lv))}; BEZ NOWOSCI: {', '.join(seg)}")
