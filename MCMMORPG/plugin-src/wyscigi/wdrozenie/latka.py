#!/usr/bin/env python3
"""Zmiany W MIEJSCU w plikach innych pluginow (nie nadpisujemy calych plikow - inne galezie moga je zmieniac):
DeluxeMenus/config.yml (+3 menu), gui_menus/hub_spolecznosc.yml (+kafel), Chowance/config.yml (swiaty_zabronione += wyscigi).
  latka.py <katalog plugins> <hub-kafel.yml>      idempotentne; wypisuje, co zmienilo
"""
import re, sys, pathlib
P = pathlib.Path(sys.argv[1]); kafel = pathlib.Path(sys.argv[2]).read_text()
cfg = P / 'DeluxeMenus/config.yml'; t = cfg.read_text()
if 'wyscigi.yml' not in t:
    assert t.count('gui_menus:\n') == 1, 'DeluxeMenus/config.yml: nie ma jednego gui_menus:'
    t = t.replace('gui_menus:\n', 'gui_menus:\n' + ''.join(f"  {n}:\n    file: {n}.yml\n" for n in ('wyscigi', 'wyscigi_czasowka', 'wyscigi_wyscig')), 1)
    cfg.write_text(t); print('DeluxeMenus/config.yml: +3 menu')
hub = P / 'DeluxeMenus/gui_menus/hub_spolecznosc.yml'; h = hub.read_text()
if 'wyscigi_kafel' not in h:
    zajete = {int(s) for blok in re.findall(r'slots?: \[?([\d, ]+)\]?', h) for s in re.findall(r'\d+', blok)}
    nowe = {int(s) for s in re.findall(r'\d+', re.search(r'slots: \[([\d, ]+)\]', kafel).group(1))}
    assert not (zajete & nowe), f'hub_spolecznosc: sloty kafla zajete {sorted(zajete & nowe)}'
    hub.write_text(h.rstrip('\n') + '\n' + kafel); print('hub_spolecznosc.yml: +kafel', sorted(nowe))
ch = P / 'Chowance/config.yml'; c = ch.read_text()
m = re.search(r'^(\s*swiaty_zabronione: \[)([^\]]*)\]', c, re.M)
assert m, 'Chowance/config.yml: brak swiaty_zabronione'
if 'wyscigi' not in m.group(2):
    c = c[:m.start()] + m.group(1) + (m.group(2) + ', ' if m.group(2).strip() else '') + 'wyscigi]' + c[m.end():]
    ch.write_text(c); print('Chowance/config.yml: swiaty_zabronione += wyscigi')
print('latka: ok')
