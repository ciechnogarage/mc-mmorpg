import json,sys,collections
klasy=[];walki=[]
for l in open(sys.argv[1],errors='replace'):
    if l.startswith('KLASA '): klasy.append(json.loads(l[6:]))
    elif l.startswith('WALKA '): walki.append(json.loads(l[6:]))
info={k['klasa']:k for k in klasy}
print(f"{'klasa':18}{'arch':5}{'HP':>5}{'panc':>5}{'broń':>14} | " + ' | '.join(f'{m[4:]:>8} n/u czas,%HP' for m in ['GAJ_WILK','GAJ_KORZENIAK','GAJ_LUCZNIK']))
for k in klasy:
    w={(x['mob'],x['tryb']):x for x in walki if x['klasa']==k['klasa']}
    def c(m,t):
        x=w.get((m,t)); 
        if not x: return '   -   '
        return f"{x['czas_s']:>4.0f}s{x['proc_hp']:>3}%" + ('☠' if x['zgon'] else ('' if x['zabity'] else '⌛'))
    print(f"{k['klasa'][:17]:18}{k['archetyp']:5}{k['hp_max']:>5.0f}{k['pancerz']:>5.0f}{str(k['bron'])[:13]:>14} | " + ' | '.join(c(m,'nieuwazny')+' '+c(m,'uwazny') for m in ['GAJ_WILK','GAJ_KORZENIAK','GAJ_LUCZNIK']))
# srednie per archetyp
arch=collections.defaultdict(list)
for x in walki: arch[info[x['klasa']]['archetyp']].append(x)
print()
for a,xs in arch.items():
    zab=[x for x in xs if x['zabity']]
    print(a, 'walk',len(xs),'wygranych',len(zab),'zgonow',sum(x['zgon'] for x in xs),'sredni czas wygranej',round(sum(x['czas_s'] for x in zab)/max(1,len(zab)),1),'s, srednia strata',round(sum(x['proc_hp'] for x in xs)/len(xs)),'% HP')
