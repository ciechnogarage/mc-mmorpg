import json, re, csv, collections, statistics, os, math
A='/w/api'; O='/w/dane'
def L(f): return json.load(open(os.path.join(A,f)))
def W(name, header, rows):
    with open(os.path.join(O,name),'w',newline='') as fh:
        w=csv.writer(fh); w.writerow(header); w.writerows(rows)
strip=lambda s: re.sub('<[^>]+>','',s) if isinstance(s,str) else s
out={}
# ---------- ITEMS
items=L('v3_item_database_fullResult.json')
out['items_total']=len(items)
typ=collections.Counter(i['type'] for i in items)
out['items_by_type']=dict(typ)
sub=collections.Counter((i['type'],i.get('subType') or '-') for i in items)
W('items_typ_podtyp.csv',['typ','podtyp','liczba'],[(a,b,c) for (a,b),c in sorted(sub.items())])
gear=[i for i in items if i['type'] in('weapon','armour','accessory')]
tier=collections.Counter(i.get('tier') for i in gear)
out['gear_total']=len(gear); out['gear_by_tier']=dict(tier)
# tier x level bucket
def lvl(i): return (i.get('requirements') or {}).get('level',0) or 0
buckets=list(range(0,130,10))
rows=[]
tiers=['common','unique','rare','legendary','set','fabled','mythic']
for b in buckets:
    r=[f'{b}-{b+9}']
    for t in tiers: r.append(sum(1 for i in gear if i.get('tier')==t and b<=lvl(i)<b+10))
    r.append(sum(1 for i in gear if b<=lvl(i)<b+10)); rows.append(r)
W('gear_poziom_x_rzadkosc.csv',['poziom']+tiers+['razem'],rows)
out['gear_max_level']=max(lvl(i) for i in gear)
out['gear_level_counter_top']=collections.Counter(lvl(i) for i in gear).most_common(12)
# ids
idc=collections.Counter(); nid=collections.defaultdict(list)
for i in gear:
    ids=i.get('identifications') or {}
    idc.update(ids.keys()); nid[i.get('tier')].append(len(ids))
W('identyfikacje_czestosc.csv',['identyfikacja','liczba_przedmiotow','procent_gear'],[(k,v,round(100*v/len(gear),1)) for k,v in idc.most_common()])
out['distinct_identifications_on_gear']=len(idc)
out['ids_per_item_by_tier']={t:(round(statistics.mean(v),2),max(v)) for t,v in nid.items() if v}
# rolled vs fixed ids
rolled=fixed=0
for i in gear:
    for k,v in (i.get('identifications') or {}).items():
        if isinstance(v,dict): rolled+=1
        else: fixed+=1
out['id_values_rolled_vs_fixed']=(rolled,fixed)
# roll ranges: ratio min/raw and max/raw for positive
rat_min=[];rat_max=[]
for i in gear:
    for k,v in (i.get('identifications') or {}).items():
        if isinstance(v,dict) and v.get('raw',0)>0:
            rat_min.append(v['min']/v['raw']); rat_max.append(v['max']/v['raw'])
out['roll_range_positive_min_over_raw']=(round(min(rat_min),2),round(statistics.median(rat_min),2),round(max(rat_min),2))
out['roll_range_positive_max_over_raw']=(round(min(rat_max),2),round(statistics.median(rat_max),2),round(max(rat_max),2))
neg_min=[];neg_max=[]
for i in gear:
    for k,v in (i.get('identifications') or {}).items():
        if isinstance(v,dict) and v.get('raw',0)<0:
            neg_min.append(v['min']/v['raw']); neg_max.append(v['max']/v['raw'])
if neg_min: out['roll_range_negative_min_over_raw']=(round(min(neg_min),2),round(statistics.median(neg_min),2),round(max(neg_min),2)); out['roll_range_negative_max_over_raw']=(round(min(neg_max),2),round(statistics.median(neg_max),2),round(max(neg_max),2))
# major ids
maj=collections.Counter()
for i in items:
    m=i.get('majorIds')
    if m: maj.update(m.keys() if isinstance(m,dict) else m)
out['items_with_major_ids']=sum(1 for i in items if i.get('majorIds')); out['distinct_major_ids']=len(maj)
# powder slots
out['powder_slots']=dict(collections.Counter(i.get('powderSlots',0) for i in gear if i['type'] in ('weapon','armour')))
out['restriction']=dict(collections.Counter(i.get('restriction') for i in items))
out['dropRestriction']=dict(collections.Counter(i.get('dropRestriction') for i in items))
out['attackSpeed']=dict(collections.Counter(i.get('attackSpeed') for i in items if i['type']=='weapon'))
out['weapon_class']=dict(collections.Counter((i.get('requirements') or {}).get('classRequirement') for i in items if i['type']=='weapon'))
out['elements_per_gear']=dict(collections.Counter(len(i.get('elements') or []) for i in gear))
out['icon_base_items']=dict(collections.Counter(((i.get('icon') or {}).get('value') or {}).get('id') if isinstance((i.get('icon') or {}).get('value'),dict) else 'skin/'+str((i.get('icon') or {}).get('format')) for i in items).most_common(15))
out['icon_formats']=dict(collections.Counter((i.get('icon') or {}).get('format') for i in items))
# stat curves: armour baseHealth by level, weapon averageDps by level
def curve(sel, key, fname, getter):
    rows=[]
    for b in range(0,125,5):
        vals=[getter(i) for i in items if sel(i) and b<=lvl(i)<b+5 and getter(i) is not None]
        if vals: rows.append((f'{b}-{b+4}',len(vals),round(statistics.median(vals)),round(max(vals))))
    W(fname,['poziom','n','mediana','max'],rows)
def hp(i):
    v=(i.get('base') or {}).get('baseHealth'); 
    if isinstance(v,dict): v=v.get('raw')
    return v
curve(lambda i:i['type']=='armour' and i.get('subType')=='chestplate','hp','krzywa_hp_napiersnika.csv',hp)
curve(lambda i:i['type']=='weapon','dps','krzywa_dps_broni.csv',lambda i:i.get('averageDps'))
# skill point req sum vs level
def spsum(i):
    r=i.get('requirements') or {}
    return sum(r.get(k,0) or 0 for k in ('strength','dexterity','intelligence','defence','agility'))
curve(lambda i:i['type'] in('weapon','armour','accessory'),'sp','krzywa_wymagania_sp.csv',spsum)
# lore & drop
out['gear_with_lore_pct']=round(100*sum(1 for i in gear if i.get('lore'))/len(gear),1)
# ingredients
ing=[i for i in items if i['type']=='ingredient']
out['ingredients_total']=len(ing)
out['ingredient_tiers']=dict(collections.Counter(i.get('tier') for i in ing))
sk=collections.Counter()
for i in ing: sk.update((i.get('requirements') or {}).get('skills',[]))
out['ingredient_professions']=dict(sk)
out['ingredient_levels']={f'{b}-{b+9}':sum(1 for i in ing if b<=lvl(i)<b+10) for b in range(0,130,10)}
out['ingredients_with_droppedBy']=sum(1 for i in ing if i.get('droppedBy'))
out['ingredients_with_position_mods']=sum(1 for i in ing if any((i.get('ingredientPositionModifiers') or {}).values()))
mobs=set()
for i in ing:
    for d in i.get('droppedBy') or []:
        mobs.add(d.get('name') if isinstance(d,dict) else d)
out['distinct_mobs_dropping_ingredients']=len(mobs)
# materials, tools, tomes, charms
out['materials']=dict(collections.Counter(i.get('subType') for i in items if i['type']=='material'))
out['tools']=dict(collections.Counter(i.get('subType') for i in items if i['type']=='tool'))
out['tomes']=dict(collections.Counter(i.get('subType') for i in items if i['type']=='tome'))
out['charms']=[i['displayName'] for i in items if i['type']=='charm']
# ---------- RECIPES
rec=L('recipes.json'); out['recipes_total']=len(rec)
out['recipes_by_skill']=dict(collections.Counter(r['skill'] for r in rec))
out['recipes_by_type']=dict(collections.Counter(r['type'] for r in rec))
out['recipe_level_bands']=sorted(set((r['level']['minimum'],r['level']['maximum']) for r in rec))[:60]
mats=collections.Counter()
for r in rec:
    for m in r['materials']: mats[m['item']]+=1
out['recipe_distinct_materials']=len(mats)
# ---------- SETS
sets=L('item_sets.json'); out['sets_total']=len(sets)
out['set_sizes']=dict(collections.Counter(len(v['parts']) for v in sets.values()))
# ---------- ABILITY TREES
rows=[]; spells=[]
combo_re=re.compile(r'Click Combo:\s*([A-Z\-]+)')
mana_re=re.compile(r'Mana Cost:\s*(\d+)')
for c in ['archer','warrior','assassin','mage','shaman']:
    t=L(f'tree_{c}.json'); cls=L(f'class_{c}.json')
    nodes=[(p,k,v) for p,pg in t['pages'].items() for k,v in pg.items()]
    arch=collections.Counter(); ap=collections.Counter(); req_arch=0; locks=0; nodeq=0
    for p,k,v in nodes:
        r=v.get('requirements') or {}
        ap[r.get('ABILITY_POINTS')]+=1
        if 'ARCHETYPE' in r: req_arch+=1
        if v.get('locks'): locks+=1
        if 'NODE' in r: nodeq+=1
        desc=' '.join(strip(x) for x in v['description'])
        m=re.search(r'(\w[\w ]*?) Archetype',desc)
        a=None
        for an in t['archetypes'].values():
            if strip(an['name']) + ' Archetype' in desc: a=strip(an['name'])
        arch[a or '(brak/wspolny)']+=1
        cm=combo_re.search(desc)
        if cm:
            mm=mana_re.search(desc)
            spells.append((c,strip(v['name']),cm.group(1),mm.group(1) if mm else '',p))
    amap=L(f'map_{c}.json')
    conn=sum(1 for pg in amap.values() for e in pg if e['type']=='connector')
    rows.append((c,strip(cls['name']),len(t['pages']),len(nodes),conn,sum(ap[x]*x for x in ap if x),dict(sorted(((k or 0),v) for k,v in ap.items())),req_arch,nodeq,locks,json.dumps(dict(arch),ensure_ascii=False)))
W('drzewka_umiejetnosci.csv',['klasa','nazwa','stron','wezlow','lacznikow','suma_AP_calego_drzewa','rozklad_kosztu_AP','wezly_z_wymog_archetypu','wezly_z_wymog_wezla','wezly_blokujace','wezly_na_archetyp'],rows)
W('spelle_kombinacje.csv',['klasa','umiejetnosc','kombinacja','mana','strona'],spells)
out['spells_with_combo']=len(spells)
# class detail
W('klasy_archetypy.csv',['klasa','archetyp','trudnosc','obrazenia','obrona','zasieg','szybkosc'],
  [(c,strip(re.sub('&.','',a['name'])),a['difficulty'],a['damage'],a['defence'],a['range'],a['speed']) for c in ['archer','warrior','assassin','mage','shaman'] for a in L(f'class_{c}.json')['archetypes'].values()])
# aspects
rows=[]
for c in ['archer','warrior','assassin','mage','shaman']:
    a=L(f'aspects_{c}.json')
    for x in a:
        th=[x['tiers'][k]['threshold'] for k in sorted(x['tiers'],key=int)]
        rows.append((c,x['name'],x['rarity'],len(th),'/'.join(map(str,th))))
W('aspekty.csv',['klasa','aspekt','rzadkosc','tierow','progi_tierow'],rows)
out['aspects_total']=len(rows); out['aspects_by_rarity']=dict(collections.Counter(r[2] for r in rows))
out['aspect_threshold_patterns']={f'{a}|{b}':c for (a,b),c in collections.Counter((r[2],r[4]) for r in rows).items()}
# ---------- MAP
we=L('world_events.json'); out['world_events_total']=len(we)
out['world_events_difficulty']=dict(collections.Counter(e['difficulty'] for e in we))
out['world_events_length']=dict(collections.Counter(e['length'] for e in we))
out['world_events_scheduled']=sum(1 for e in we if e.get('schedule'))
W('wydarzenia_swiata.csv',['nazwa','poziom','trudnosc','dlugosc','harmonogram','lokacje'],[(e['name'],e['level'],e['difficulty'],e['length'],json.dumps(e.get('schedule')),len(e['location'])) for e in sorted(we,key=lambda e:e['level'])])
for f,key in [('camps.json','camps'),('raids.json','raids')]:
    d=L(f)
    W(f'{key}.csv',['nazwa','typ','poziom','trudnosc','dlugosc','liczba_nagrod','tiers'],[(x['name'],x['type'],x['level'],x['difficulty'],x['length'],len(x['rewards']),json.dumps(dict(collections.Counter(r.get('tier') or r['type'] for r in x['rewards'])))) for x in d])
lp=L('loot_pools.json'); out['loot_pools']=[(x['name'],x['type'],len(x['rewards'])) for x in lp]
gn=L('gathering_nodes.json'); out['gathering_nodes_total']=len(gn)
res=collections.defaultdict(list)
for n in gn: res[n['resource']].append(n['level'])
W('wezly_zbierackie.csv',['surowiec','wezlow','poziom'],sorted([(k,len(v),v[0]) for k,v in res.items()],key=lambda r:r[2]))
out['gathering_node_types']=dict(collections.Counter(n['type'] for n in gn))
mk=L('v3_map_locations_markers.json'); out['markers_total']=len(mk)
W('znaczniki_mapy.csv',['ikona','liczba'],collections.Counter(m['icon'] for m in mk).most_common())
xs=[int(m['x']) for m in mk]; zs=[int(m['z']) for m in mk]
out['map_extent_markers']={'x':(min(xs),max(xs)),'z':(min(zs),max(zs))}
# ---------- TERRITORIES / GUILDS
ter=L('v3_guild_list_territory.json'); out['territories_total']=len(ter)
g=collections.Counter(v['guild']['name'] for v in ter.values() if v.get('guild') and v['guild'].get('name'))
out['territory_holders_top10']=g.most_common(10); out['guilds_holding_territory']=len(g)
out['territory_treasury']=dict(collections.Counter(v.get('treasury') for v in ter.values()))
out['territory_defences']=dict(collections.Counter(v.get('defences') for v in ter.values()))
rt=collections.Counter()
for v in ter.values():
    for r in v.get('resources',[]):
        if r['baseGeneration']>0: rt[(r['type'],r['baseGeneration'])]+=1
out['territory_resource_basegen']={f'{a}:{b}':c for (a,b),c in sorted(rt.items())}
lk=[len(v.get('links',[])) for v in ter.values()]; out['territory_links_mean_max']=(round(statistics.mean(lk),2),max(lk))
gl=L('guild_list.json'); out['guilds_total']=len(gl)
ss=L('guild_seasons.json'); out['guild_seasons']=len(ss)
s32=ss[max(ss,key=int)]
out['season_latest']={k:v for k,v in s32.items() if k!='ratingRewards'}
W('sezon_gildii_nagrody.csv',['typ','wartosc','prog_SR'],[(r['type'],r['value'],r['condition']['value']) for r in s32['ratingRewards']])
# leaderboards
for lb in ['combatSoloLevel','totalGlobalLevel','guildLevel','playerContent','professionsGlobalLevel','frumaCompletion','guildTerritories']:
    d=L(f'lb_{lb}.json'); vals=[v for v in d.values()]
    out[f'lb_{lb}_top_bottom']=(vals[0].get('score'),vals[-1].get('score'), len(vals))
    if lb=='combatSoloLevel':
        out['lb_combat_classes']=dict(collections.Counter(v.get('characterType') for v in vals))
        out['lb_combat_support_ranks']=dict(collections.Counter(v.get('supportRank') for v in vals))
        out['lb_combat_playtime_h']=(round(min(v['metadata']['playtime'] for v in vals)),round(statistics.median(v['metadata']['playtime'] for v in vals)),round(max(v['metadata']['playtime'] for v in vals)))
# online players by server
op=L('v3_player.json'); srv=collections.Counter(v for v in op['players'].values())
reg=collections.Counter(re.sub(r'\d+','',s) for s in srv.elements())
out['online_total']=op['total']; out['online_worlds']=len(srv); out['online_by_region']=dict(reg)
out['online_per_world_min_med_max']=(min(srv.values()),statistics.median(srv.values()),max(srv.values()))
W('gracze_na_swiatach.csv',['swiat','graczy'],sorted(srv.items(),key=lambda x:(re.sub(r'\d','',x[0]),int(re.sub(r'\D','',x[0]) or 0))))
json.dump(out,open(os.path.join(O,'statystyki_api.json'),'w'),ensure_ascii=False,indent=1,default=str)
print(json.dumps(out,ensure_ascii=False,indent=0,default=str)[:20000])
