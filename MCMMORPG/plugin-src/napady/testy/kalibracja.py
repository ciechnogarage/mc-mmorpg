#!/usr/bin/env python3
"""Kalibracja symulacji napadu z zapisow napadow z cialami MM.

Czyta plugins/Napady/zapisy/*.jsonl.gz, bierze tylko odcinki z cialami (klatka "m":"c"; zdarzenia
liczone wg trybu ostatniej klatki) i liczy to, co Symulacja bierze z configu sym.*:
predkosc (bl./tik, marsz), szarza (mnoznik predkosci w walce), wrecz-zasieg, wrecz-co (tiki miedzy
ciosami), luk-zasieg, luk-co, celnosc (trafienia strzala / strzaly), strzala (obrazenia strzaly), wrecz (obrazenia
ciosu po pancerzu), odstep (srednia odleglosc od najblizszego swojego
przy drzwiach magazynu w GRABIEZY - tlok; symulacja ustawia promien rozpychania, porownaj tryb c z s).
Wypisuje fragment YAML do wklejenia w config.yml i liczbe probek kazdej wartosci.

Uzycie: python3 testy/kalibracja.py <zapis.jsonl.gz>... [--tryb c|s]
  --tryb s liczy to samo z odcinkow symulacji (sprawdzenie, czy symulacja oddaje config).
"""
import gzip
import json
import statistics as st
import sys
from collections import defaultdict


def kwantyl(xs, q):
    xs = sorted(xs)
    if not xs:
        return None
    i = min(len(xs) - 1, max(0, int(round(q * (len(xs) - 1)))))
    return xs[i]


def czytaj(pliki, tryb):
    marsz, walka = [], []                 # predkosc na tik z par klatek
    wrecz_odl, luk_odl, strz_dmg, wrecz_dmg = [], [], [], []
    wrecz_t, strzal_t = defaultdict(list), defaultdict(list)
    strzaly, traf_strzala = defaultdict(int), defaultdict(int)
    tiki = 0
    odstepy_tlok = []                     # najblizszy swoj napastnik przy drzwiach (grabi/szturmuje)
    for f in pliki:
        poprz = {}                       # id -> (t, x, z, akcja)
        tryb_teraz = None
        t_ost = None
        with gzip.open(f, 'rt', encoding='utf-8') as fh:
            naglowek = json.loads(fh.readline())
            luk = {j['id'] for j in naglowek['jednostki'] if 'LUCZ' in j['r']}
            for linia in fh:
                r = json.loads(linia)
                if 'u' in r:
                    tryb_teraz = r.get('m', 'c')
                    if tryb_teraz != tryb:
                        poprz = {}
                        continue
                    if t_ost is not None:
                        tiki += r['t'] - t_ost
                    t_ost = r['t']
                    tl = [u for u in r['u'] if isinstance(u[0], int) and u[6] in ('grabi', 'szturmuje') and u[5] > 0]
                    if len(tl) > 1:
                        odstepy_tlok.append(st.mean(min(((a[1] - b[1]) ** 2 + (a[3] - b[3]) ** 2) ** 0.5 for b in tl if b is not a) for a in tl))
                    for u in r['u']:
                        if not isinstance(u[0], int):
                            continue             # gracz
                        uid, x, z, akcja = u[0], u[1], u[3], u[6]
                        p = poprz.get(uid)
                        if p and 0 < r['t'] - p[0] <= 10:
                            v = ((x - p[1]) ** 2 + (z - p[2]) ** 2) ** 0.5 / (r['t'] - p[0])
                            if v > 0.03:         # stoi = brak probki (bije, czeka, w tlumie)
                                (marsz if akcja == 'maszeruje' and p[3] == 'maszeruje' else walka).append(v)
                        poprz[uid] = (r['t'], x, z, akcja)
                    continue
                if tryb_teraz != tryb:
                    continue
                e, d, t = r.get('e'), r.get('d', ''), r['t']
                if e == 'traf':
                    # a>v:dmg:odl:strzala|wrecz
                    a, reszta = d.split('>', 1)
                    _, dmg, odl, rodzaj = reszta.split(':')
                    a = (f, int(a))      # id jednostek powtarzaja sie miedzy zapisami
                    if rodzaj == 'strzala':
                        luk_odl.append(float(odl)); strz_dmg.append(float(dmg)); traf_strzala[a] += 1
                    else:
                        wrecz_odl.append(float(odl)); wrecz_dmg.append(float(dmg)); wrecz_t[a].append(t)
                elif e == 'strzal':
                    a = (f, int(d.split('>', 1)[0]))
                    strzaly[a] += 1
                    strzal_t[a].append(t)
    return dict(marsz=marsz, walka=walka, wrecz_odl=wrecz_odl, luk_odl=luk_odl, strz_dmg=strz_dmg,
                wrecz_dmg=wrecz_dmg, wrecz_t=wrecz_t, strzal_t=strzal_t, strzaly=strzaly,
                traf_strzala=traf_strzala, tiki=tiki, odstepy_tlok=odstepy_tlok)


def odstepy(per, maks):
    """Odstepy miedzy kolejnymi akcjami tej samej jednostki; dluzsze niz maks = przerwa (zmiana celu, marsz)."""
    out = []
    for ts in per.values():
        ts = sorted(ts)
        out += [b - a for a, b in zip(ts, ts[1:]) if 0 < b - a <= maks]
    return out


def main():
    a = sys.argv[1:]
    tryb = 'c'
    if '--tryb' in a:
        i = a.index('--tryb'); tryb = a[i + 1]; del a[i:i + 2]
    if not a:
        print(__doc__); sys.exit(2)
    d = czytaj(a, tryb)
    print(f"# {len(a)} zapisow, odcinki m={tryb}: {d['tiki']} tikow ({d['tiki'] / 20:.0f} s)")
    wyn, n = {}, {}

    def ustaw(k, v, ile, fmt):
        n[k] = ile
        if v is not None and ile >= 5:
            wyn[k] = fmt(v)

    vm = st.median(d['marsz']) if d['marsz'] else None
    vw = st.median(d['walka']) if d['walka'] else None
    ustaw('predkosc', vm, len(d['marsz']), lambda v: round(v, 3))
    ustaw('szarza', (vw / vm) if vm and vw else None, min(len(d['marsz']), len(d['walka'])), lambda v: round(v, 2))
    ustaw('wrecz-zasieg', kwantyl(d['wrecz_odl'], 0.9), len(d['wrecz_odl']), lambda v: round(v, 1))
    wo = odstepy(d['wrecz_t'], 60)
    ustaw('wrecz-co', st.median(wo) if wo else None, len(wo), lambda v: int(round(v)))
    ustaw('luk-zasieg', kwantyl(d['luk_odl'], 0.9), len(d['luk_odl']), lambda v: round(v, 1))
    lo = odstepy(d['strzal_t'], 120)
    # symulacja losuje odstep luk-co + U(0, luk-co/4): mediana ~ 1.125 x luk-co, wiec dzielimy (tryb s = config)
    ustaw('luk-co', st.median(lo) / 1.125 if lo else None, len(lo), lambda v: int(round(v)))
    s_all, t_all = sum(d['strzaly'].values()), sum(d['traf_strzala'].values())
    ustaw('celnosc', t_all / s_all if s_all else None, s_all, lambda v: round(min(v, 1.0), 2))
    ustaw('strzala', st.median(d['strz_dmg']) if d['strz_dmg'] else None, len(d['strz_dmg']), lambda v: round(v, 1))
    # odstep: miara tloku, nie promien wprost - porownaj --tryb c z --tryb s i stroj sym.odstep
    ustaw('odstep (pomiar)', st.mean(d['odstepy_tlok']) if d['odstepy_tlok'] else None, len(d['odstepy_tlok']), lambda v: round(v, 2))
    ustaw('wrecz', st.median(d['wrecz_dmg']) if d['wrecz_dmg'] else None, len(d['wrecz_dmg']), lambda v: round(v, 1))

    print('sym:')
    for k in ('predkosc', 'szarza', 'wrecz-zasieg', 'wrecz-co', 'luk-zasieg', 'luk-co', 'celnosc', 'strzala', 'wrecz', 'odstep (pomiar)'):
        if k in wyn:
            print(f'  {k}: {wyn[k]}    # probek {n[k]}')
        else:
            print(f'  # {k}: za malo probek ({n.get(k, 0)})')


if __name__ == '__main__':
    main()
