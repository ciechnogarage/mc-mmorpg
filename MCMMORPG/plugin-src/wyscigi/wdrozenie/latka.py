#!/usr/bin/env python3
"""Zmiany W MIEJSCU w plikach innych pluginow (nie nadpisujemy calych plikow - inne galezie moga je zmieniac).
  latka.py <katalog plugins> <katalog wdrozenie>      idempotentne; wypisuje, co zmienilo

- DeluxeMenus/config.yml: menu wyscigow w gui_menus
- DeluxeMenus/gui_menus/hub_spolecznosc.yml: kafel WYSCIGI (niewidzialny przycisk na narysowanym tle),
  opis strony 'Spolecznosc' -> 'Druzyna' (kafel w MENU i tytul strony to DRUZYNA)
- KsiegaPostaci/config.yml: podstrony.identyfikatory i podstrony.strony (dolny panel Ksiegi, "wroc")
- MenuKey/config.yml: blocked-worlds += wyscigi (Q w wyscigu = pauza z pluginu Wyscigi)
- Chowance/config.yml: przywolanie.swiaty_zabronione += wyscigi
"""
import re, sys, pathlib

P = pathlib.Path(sys.argv[1]); WD = pathlib.Path(sys.argv[2])
MENU = ('wyscigi', 'wyscigi_czasowka', 'wyscigi_wyscig', 'wyscigi_pauza', 'wyscigi_zamknij')

# --- DeluxeMenus/config.yml
cfg = P / 'DeluxeMenus/config.yml'; t = cfg.read_text()
assert t.count('gui_menus:\n') == 1, 'DeluxeMenus/config.yml: nie ma jednego gui_menus:'
brak = [n for n in MENU if f'file: {n}.yml' not in t]
if brak:
    t = t.replace('gui_menus:\n', 'gui_menus:\n' + ''.join(f"  {n}:\n    file: {n}.yml\n" for n in brak), 1)
    cfg.write_text(t); print('DeluxeMenus/config.yml: +', ', '.join(brak))

# --- hub_spolecznosc.yml
hub = P / 'DeluxeMenus/gui_menus/hub_spolecznosc.yml'; h = hub.read_text()
kafel = (WD / 'hub-kafel.yml').read_text()
m = re.search(r'^  wyscigi_kafel:\n(?:    .*\n)*', h, re.M)
if m:
    if m.group(0).rstrip('\n') != kafel.rstrip('\n'):
        h = h[:m.start()] + kafel.rstrip('\n') + '\n' + h[m.end():]; print('hub_spolecznosc.yml: kafel podmieniony')
else:
    zajete = {int(s) for blok in re.findall(r'slots?: \[?([\d, ]+)\]?', h) for s in re.findall(r'\d+', blok)}
    nowe = {int(s) for s in re.findall(r'\d+', re.search(r'slots: \[([\d, ]+)\]', kafel).group(1))}
    assert not (zajete & nowe), f'hub_spolecznosc: sloty kafla zajete {sorted(zajete & nowe)}'
    h = h.rstrip('\n') + '\n' + kafel; print('hub_spolecznosc.yml: +kafel', sorted(nowe))
if "display_name: '&dSpołeczność'" in h:
    h = h.replace("display_name: '&dSpołeczność'", "display_name: '&dDrużyna'", 1); print('hub_spolecznosc.yml: opis Społeczność -> Drużyna')
hub.write_text(h)

# --- KsiegaPostaci/config.yml (wpisy tylko w obrebie wlasciwej sekcji)
kp = P / 'KsiegaPostaci/config.yml'; k = kp.read_text()
wpisy = [l.split('\t') for l in (WD / 'menu/ksiega_podstrony.txt').read_text(encoding='utf-8').splitlines() if l.strip()]
for sekcja, wartosc in (('identyfikatory', lambda z, i, m: i), ('strony', lambda z, i, m: f'{m} # {i}')):
    naglowek = f'\n  {sekcja}:\n'
    assert k.count(naglowek) == 1, f'KsiegaPostaci: brak/wiele {sekcja}'
    sek = k.split(naglowek, 1)[1]
    koniec = re.search(r'^  \S', sek, re.M)
    tresc = sek[:koniec.start()] if koniec else sek
    dodaj = ''.join(f'    "{z}": {wartosc(z, i, m)}\n' for z, i, m in wpisy if f'"{z}":' not in tresc)
    if dodaj:
        k = k.replace(naglowek, naglowek + dodaj, 1); print(f'KsiegaPostaci: podstrony.{sekcja} +{dodaj.count(chr(10))}')
kp.write_text(k)

# --- MenuKey/config.yml
mk = P / 'MenuKey/config.yml'; mtxt = mk.read_text()
m = re.search(r'^blocked-worlds:\n((?:  - .*\n)*)', mtxt, re.M)
assert m, 'MenuKey: brak blocked-worlds'
if '- wyscigi' not in m.group(1):
    mtxt = mtxt[:m.end()] + '  - wyscigi\n' + mtxt[m.end():]
    mk.write_text(mtxt); print('MenuKey/config.yml: blocked-worlds += wyscigi')

# --- Chowance/config.yml
ch = P / 'Chowance/config.yml'; c = ch.read_text()
m = re.search(r'^(\s*swiaty_zabronione: \[)([^\]]*)\]', c, re.M)
assert m, 'Chowance/config.yml: brak swiaty_zabronione'
if 'wyscigi' not in m.group(2):
    c = c[:m.start()] + m.group(1) + (m.group(2) + ', ' if m.group(2).strip() else '') + 'wyscigi]' + c[m.end():]
    ch.write_text(c); print('Chowance/config.yml: swiaty_zabronione += wyscigi')
print('latka: ok')
