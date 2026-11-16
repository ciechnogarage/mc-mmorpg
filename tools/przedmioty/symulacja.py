#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Rozklad z tabel (od 30.09 rzadkosc = modyfikator z dokladnym skladem, liczone analitycznie, bez losowania):
udzial rzadkosci, liczba affiksow (warianty), tiery affiksow (pasmo). Zgodnosc z gra mierzy evale/sonda-rzadkosc.js."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tresc import RARITY, WARIANTY, PASMA

def main():
    out = {}
    for tid, nazwa, kol, _, glow, udz in RARITY:
        if not udz:
            continue
        if tid not in WARIANTY:
            out[tid] = {'udzial': udz, 'liczba': {'0': 1.0}, 'srednio': 0, 'tiery': {}}
            continue
        war, pas = WARIANTY[tid]; sw = sum(w for *_, w in war)
        liczba = {}
        for p, s, w in war:
            liczba[str(p + s)] = liczba.get(str(p + s), 0) + w / sw
        c3, c2 = PASMA[pas]
        tiery = {'T3': c3, 'T2': (1 - c3) * c2, 'T1': (1 - c3) * (1 - c2)}
        out[tid] = {'udzial': udz, 'liczba': {k: round(v, 3) for k, v in sorted(liczba.items())},
                    'srednio': round(sum(int(k) * v for k, v in liczba.items()), 2), 'tiery': {k: round(v, 3) for k, v in tiery.items()},
                    'legendarny_efekt': tid == 'LEGENDARNY'}
    return out

if __name__ == '__main__':
    r = main()
    for k, v in r.items():
        print(f"{k:11s} udzial {v['udzial']:.3f}  sr {v['srednio']}  liczba {v['liczba']}  tiery {v['tiery']}")
    if len(sys.argv) > 1:
        json.dump(r, open(sys.argv[1], 'w'), indent=1)
