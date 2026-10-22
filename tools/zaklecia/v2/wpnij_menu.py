#!/usr/bin/env python3
"""Wpina Ksiege zaklec w DeluxeMenus (idempotentnie): rejestracja w config.yml + kafelek w hub_umiejetnosci
(slot 22). Tytul ksiegi bierze glif panelu RAZEM ze znakami przesuniecia z tytulu huba (bez nich tlo sie przesuwa)."""
import re, sys, pathlib
dm = pathlib.Path(sys.argv[1]); cfg = dm / 'config.yml'; hub = dm / 'gui_menus/hub_umiejetnosci.yml'; ks = dm / 'gui_menus/ksiega_zaklec.yml'
s = cfg.read_text(encoding='utf-8')
if 'ksiega_zaklec' not in s:
    s = s.replace("  hub_umiejetnosci:\n    file: hub_umiejetnosci.yml\n", "  hub_umiejetnosci:\n    file: hub_umiejetnosci.yml\n  ksiega_zaklec:\n    file: ksiega_zaklec.yml\n", 1)
    assert 'ksiega_zaklec' in s, 'nie znalazlem wpisu hub_umiejetnosci w config.yml'; cfg.write_text(s, encoding='utf-8')
h = hub.read_text(encoding='utf-8')
tytul = next(l for l in h.splitlines(True) if l.startswith('menu_title'))
for plik, napis in ((ks, 'Księga zaklęć'), (dm / 'gui_menus/ulubione_zaklec.yml', 'Ulubione zaklęcia')):
    if plik.exists():
        k = plik.read_text(encoding='utf-8'); plik.write_text(re.sub(r"^menu_title:.*\n", tytul.replace('Umiejętności', napis), k, count=1, flags=re.M), encoding='utf-8')
if 'ulubione_zaklec' not in (s2 := cfg.read_text(encoding='utf-8')):
    s2 = s2.replace("  ksiega_zaklec:\n    file: ksiega_zaklec.yml\n", "  ksiega_zaklec:\n    file: ksiega_zaklec.yml\n  ulubione_zaklec:\n    file: ulubione_zaklec.yml\n", 1)
    assert 'ulubione_zaklec' in s2; cfg.write_text(s2, encoding='utf-8')
if "'ksiega':" not in h:
    assert "\n  'wstecz':" in h and 'slot: 22' not in h, 'slot 22 w hub_umiejetnosci zajety albo brak wstecz'
    h = h.replace("\n  'wstecz':", """
  'ksiega':
    material: TRIAL_KEY
    item_model: 'zaklecia:plomienie'
    slot: 22
    display_name: '&6&lKsięga zaklęć'
    lore:
      - ''
      - '&7Weź zaklęcia do prawej i lewej dłoni.'
    left_click_commands:
      - '[openguimenu] ksiega_zaklec'

  'wstecz':""", 1); hub.write_text(h, encoding='utf-8')
print('menu wpiete')
