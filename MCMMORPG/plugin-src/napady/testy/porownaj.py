#!/usr/bin/env python3
"""Porownanie wynikow napadow: symulacja (bez cial) vs ciala MM, te same ziarna.

Czyta plugins/Napady/zapisy/*.jsonl.gz, dzieli wg naglowka "lod" (ciala | symulacja; auto pomijane, bo
miesza tryby) i dla kazdego napadu liczy: t brama zburzona, t grabiez, t koniec (s), polegli atak/obrona,
wynik. Wypisuje mediany obu trybow i roznice. Exit 1, gdy ktoras mediana czasu lub poleglych rozni sie
o wiecej niz --prog (domyslnie 20%) albo wyniki (kto wygral) sa rozne w wiecej niz 1/4 par ziaren.

Uzycie: python3 testy/porownaj.py <zapis.jsonl.gz>... [--prog 0.2]
Zapisy: `napad lod ciala` + `napad start N Z`, potem `napad lod symulacja` + `napad start N Z` (to samo Z).
"""
import gzip
import json
import statistics as st
import sys


def napad(f):
    with gzip.open(f, 'rt', encoding='utf-8') as fh:
        h = json.loads(fh.readline())
        strona = {j['id']: j['s'] for j in h['jednostki']}
        r = dict(ziarno=h['ziarno'], lod=h.get('lod'), brama=None, grabiez=None, koniec=None,
                 padl_atak=0, padl_obrona=0, wynik=None)
        for linia in fh:
            e = json.loads(linia)
            if 'e' not in e:
                continue
            t, typ, d = e['t'] / 20, e['e'], e.get('d', '')
            if typ == 'brama' and d == 'zburzona':
                r['brama'] = t
            elif typ == 'faza' and d == 'GRABIEZ':
                r['grabiez'] = t
            elif typ == 'faza' and d.startswith('ODWROT:'):
                r['wynik'] = d.split(':', 1)[1]
            elif typ == 'padl' and d.isdigit():
                r['padl_atak' if strona.get(int(d)) == 'ATAK' else 'padl_obrona'] += 1
            elif typ == 'koniec':
                r['koniec'] = t
                r['wynik'] = r['wynik'] or d
        return r


def main():
    a = sys.argv[1:]
    prog = 0.2
    if '--prog' in a:
        i = a.index('--prog'); prog = float(a[i + 1]); del a[i:i + 2]
    if not a:
        print(__doc__); sys.exit(2)
    tryby = {'ciala': {}, 'symulacja': {}}
    for f in a:
        r = napad(f)
        if r['lod'] in tryby and r['koniec'] is not None:
            tryby[r['lod']].setdefault(r['ziarno'], []).append(r)
    wspolne = sorted(set(tryby['ciala']) & set(tryby['symulacja']))
    print(f"napadow: ciala {sum(map(len, tryby['ciala'].values()))}, symulacja {sum(map(len, tryby['symulacja'].values()))}, "
          f"wspolnych ziaren {len(wspolne)}")
    if not wspolne:
        sys.exit(2)
    zle = []
    print(f"{'miara':14s} {'ciala':>8s} {'symulacja':>10s} {'roznica':>8s}")
    for k in ('brama', 'grabiez', 'koniec', 'padl_atak', 'padl_obrona'):
        wart = {}
        for m in tryby:
            xs = [r[k] for z in wspolne for r in tryby[m][z] if r[k] is not None]
            wart[m] = st.median(xs) if xs else None
        c, s = wart['ciala'], wart['symulacja']
        if c is None or s is None:
            print(f"{k:14s} {str(c):>8s} {str(s):>10s}      -"); continue
        rel = (s - c) / c if c else (0.0 if s == 0 else 1.0)
        # polegli: male liczby, roznica 1 jednostki nie jest bledem
        ok = abs(rel) <= prog or (k.startswith('padl') and abs(s - c) <= 1)
        if not ok:
            zle.append(k)
        print(f"{k:14s} {c:8.1f} {s:10.1f} {rel:+8.0%}{'' if ok else '  <-- poza progiem'}")
    rozne = sum(1 for z in wspolne if tryby['ciala'][z][0]['wynik'] != tryby['symulacja'][z][0]['wynik'])
    print(f"wynik rozny w {rozne}/{len(wspolne)} parach ziaren")
    for z in wspolne:
        c, s = tryby['ciala'][z][0], tryby['symulacja'][z][0]
        print(f"  ziarno {z}: ciala brama {c['brama']} koniec {c['koniec']} polegli {c['padl_atak']}/{c['padl_obrona']} | "
              f"sym brama {s['brama']} koniec {s['koniec']} polegli {s['padl_atak']}/{s['padl_obrona']}")
    if rozne * 4 > len(wspolne):
        zle.append('wynik')
    print('ZGODNE' if not zle else 'ROZNE: ' + ', '.join(zle))
    sys.exit(1 if zle else 0)


if __name__ == '__main__':
    main()
