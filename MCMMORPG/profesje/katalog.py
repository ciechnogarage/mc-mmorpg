# -*- coding: utf-8 -*-
"""KATALOG.md — tabele do przegladu (generowane z modelu; nie edytowac recznie)."""
import os
from collections import defaultdict

import tresc as T
from model import PROF_PL


def pisz_katalog(g):
    m = g.m
    L = ['# Profesje v2 — katalog (GENEROWANY przez generuj.py)', '',
         f'Przedmioty: {len(m.items)} · receptury: {len(m.rec)} · stacje: {len({r["st"] for r in m.rec})} · '
         f'węzły: {len(m.wezly)} · łowiska: {len(m.lowiska)} · zwierzyna: {len(m.zwierzyna)}', '']
    L += ['## Krzywa', '', '| Poziom | XP do następnego | Akcji tieru |', '|---|---|---|']
    for l in (1, 10, 11, 25, 49, 50, 75, 99):
        L.append(f'| {l} | {int(round(T.akcje(l) * T.g(T.tier_poziomu(l))))} | {T.akcje(l)} |')
    L += ['', '## Surowce (główny / poboczny / rzadki)', '', '| Tier | Kraina | ' + ' | '.join(PROF_PL[p] for p in T.PROF_ZBIERACKIE) + ' |',
          '|---|---|' + '---|' * len(T.PROF_ZBIERACKIE)]
    for t in range(1, 11):
        kom = []
        for p in T.PROF_ZBIERACKIE:
            ids = [m.surowiec(p, t, s) for s in 'gpr']
            kom.append(' / '.join(m.items[i]['nazwa'] for i in ids if i))
        L.append(f'| {T.ROMAN[t]} | {T.KRAINY[t][0]} | ' + ' | '.join(kom) + ' |')
    L += ['', '## Rzadkie z lochów', '', '| Akt | Pył (wróg 3%) | Esencja (elita 30%) | Serce (boss) |', '|---|---|---|---|']
    for t, n in T.RZADKIE.items():
        L.append(f'| {t} | {n[0]} | {n[1]} | {n[2]} |')
    L += ['', '## Receptury per profesja i tier (poziom: wynik ← składniki · XP)', '']
    po = defaultdict(list)
    for r in m.rec:
        po[(r['prof'], r['tier'])].append(r)
    for p in ['smithing', 'woodworking', 'tailoring', 'jewelcrafting', 'alchemy', 'enchanting', 'cooking', 'building']:
        L += [f'### {PROF_PL[p]}', '']
        for t in range(1, 11):
            rs = sorted(po[(p, t)], key=lambda r: (r['lv'], r['id']))
            zw = [r for r in rs if not r['out'].startswith('RZM_')]
            sp = [r for r in rs if r['out'].startswith('RZM_')]
            L.append(f'- **{T.ROMAN[t]}** ' + '; '.join(
                f"{r['lv']}: {m.items[r['out']]['nazwa']}{' ×' + str(r['ile']) if r['ile'] > 1 else ''} ← "
                + ', '.join(f"{n}× {m.items[i]['nazwa'] if i in m.items else i}" for i, n in r['ing']) + f" · {r['xp']} XP"
                for r in zw) + (f'; sprzęt v2: {len(sp)} receptur' if sp else ''))
        L.append('')
    L += ['## Buffy (wartości T1 → T10)', '', '| Przedmiot | Grupa | Czas | Statystyki T1 | T10 |', '|---|---|---|---|---|']
    for k, (nazwa, grupa, czas, cd, staty, opis) in T.BUFFY.items():
        L.append(f'| {nazwa} | {grupa} | {czas} s | ' + ', '.join(f'{s} {a}' for s, (a, b) in staty.items())
                 + ' | ' + ', '.join(f'{s} {round(a + 9 * b, 3)}' for s, (a, b) in staty.items()) + ' |')
    L += ['', '## Klejnoty i runy (T1 → T10)', '', '| Nazwa | Profesja | Statystyka | T1 | T10 |', '|---|---|---|---|---|']
    for k, (nazwa, p, stat, (a, b)) in T.KLEJNOTY.items():
        L.append(f'| {nazwa} | {PROF_PL[p]} | {stat} | {a} | {round(a + 9 * b, 3)} |')
    f = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'KATALOG.md')
    try:
        open(f, 'w', encoding='utf-8').write('\n'.join(L) + '\n')
    except OSError as e:  # repo zamontowane tylko do odczytu (np. gen_klon.sh) — katalog nie jest potrzebny serwerowi
        print('KATALOG.md pominiety:', e)
