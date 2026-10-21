import sys, math, collections; sys.path.insert(0,'/opt/docker/mc/forge/tools'); import mca, make_poziom as M
W=mca.World(sys.argv[1]); L=dict(M.LEVEL); L["spawn"]=(0,int(M.coast_r(math.pi/2,L)-22))
exec(open('/opt/docker/mc/forge/levels/review/walk2.py').read().split('sx, sz = L["spawn"]')[0].split('W = mca.World')[0])
PASS=("short_grass","tall_grass","fern","petals","wildflowers","leaf_litter","bush","dandelion","poppy","orchid","allium","bluet","tulip","daisy","cornflower","lily","sunflower","lilac","rose","peony","torchflower","eyeblossom")
TREE=("leaves","_log","_wood","cobblestone")
Z=collections.defaultdict(list); trees=collections.Counter(); topb=collections.Counter(); under=[]
for x in range(-120,121,2):
  for z in range(-120,121,2):
    zn=M.zone_at(x,z,L)
    if zn=="pustka": continue
    c=mca.column(W,x,z)
    if not c: continue
    g=[y for y,b in c if not any(p in b for p in PASS+TREE)]
    if g: Z[zn].append(g[0])
    if any(t in b for y,b in c for t in ("leaves",)): trees[zn]+=1
    topb[c[0][1].replace('minecraft:','')]+=1
    # grubosc wyspy: od gory do najnizszego bloku
    under.append((zn, g[0]-c[-1][0] if g else 0))
for zn,v in Z.items(): 
  v.sort(); print(f"{zn:8s} n={len(v):5d} y min/med/max={v[0]}/{v[len(v)//2]}/{v[-1]}  kolumn pod koroną: {100*trees[zn]/len(v):.0f}%")
th=collections.defaultdict(list)
for zn,t in under: th[zn].append(t)
print("grubosc (gora->najnizszy blok) mediana:", {k:sorted(v)[len(v)//2] for k,v in th.items()}, " max:",{k:max(v) for k,v in th.items()})
print("top bloki:", topb.most_common(18))
