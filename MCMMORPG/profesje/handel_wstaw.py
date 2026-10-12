# -*- coding: utf-8 -*-
"""Wstawia listy przedmiotow profesji v2 do PRODUKCYJNEGO rynku i skupu bez ruszania wygladu.
tools/handel/generuj.py generuje stary wyglad (przed stylem Ksiegi 03.10, strony/handel) — uruchamiamy go na KOPII
CoreTools, a stad bierzemy tylko zawartosc kategorii:
  skup.yml  — w kazdej kategorii zostaje 'name:' z produkcji, pozycje z wygenerowanego,
  rynek.yml — w sekcji categories: podmieniamy linie 'id: [...]' (listy dozwolonych przedmiotow).
Uzycie: python3 handel_wstaw.py <CoreTools produkcji> <CoreTools wygenerowany>
"""
import re
import sys


def kategorie(tekst, wciecie='    '):
    i = tekst.index('\n  categories:\n') + len('\n  categories:\n')
    glowa, cialo = tekst[:i], tekst[i:]
    bloki, nazwa = {}, None
    kolejnosc = []
    for linia in cialo.split('\n'):
        m = re.match(r'^' + wciecie + r'([a-z_0-9]+):\s*$', linia)
        if m:
            nazwa = m.group(1)
            kolejnosc.append(nazwa)
            bloki[nazwa] = [linia]
        elif nazwa:
            bloki[nazwa].append(linia)
    return glowa, kolejnosc, bloki


def skup(prod, gen):
    gp, kol, bp = kategorie(prod)
    _, _, bg = kategorie(gen)
    out = [gp.rstrip('\n')]
    for k in kol:
        b = bp[k]
        nazwa = next((l for l in b if l.startswith('      name:')), None)
        nowe = [l for l in bg.get(k, b)[1:] if not l.startswith('      name:')]
        out += [b[0]] + ([nazwa] if nazwa else []) + nowe
    return '\n'.join(out).rstrip('\n') + '\n'


def rynek(prod, gen):
    """Kategorie przedmiotow AH zaczynaja sie od 'minimum_bid_increment' (przyciski o tej samej nazwie — nie)."""
    for kat in ('surowce', 'polprodukty', 'narzedzia', 'potrawy', 'sprzet'):
        wz = r'(\n    ' + kat + r':\n      minimum_bid_increment:.*\n(?:      .*\n|        .*\n|          .*\n)*?          id: )(\[.*?\])'
        mg = re.search(wz, gen)
        if not mg:
            raise SystemExit(f'rynek: brak kategorii {kat} w wygenerowanym')
        prod, n = re.subn(wz, lambda m: m.group(1) + mg.group(2), prod, count=1)
        if n != 1:
            raise SystemExit(f'rynek: brak kategorii {kat} w produkcji')
    return prod


if __name__ == '__main__':
    P, G = sys.argv[1], sys.argv[2]
    for f, fn in (('Shops/skup.yml', skup), ('AuctionHouse/rynek.yml', rynek)):
        prod, gen = open(f'{P}/{f}', encoding='utf-8').read(), open(f'{G}/{f}', encoding='utf-8').read()
        nowy = fn(prod, gen)
        open(f'{P}/{f}', 'w', encoding='utf-8').write(nowy)
        print(f, 'zapisano')
