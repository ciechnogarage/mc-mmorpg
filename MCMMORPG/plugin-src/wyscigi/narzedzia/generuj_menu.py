#!/usr/bin/env python3
"""Menu DeluxeMenus wyścigów z config.yml pluginu (statystyki gatunków jako paski). Bez pyyaml: prosty parser linii gatunków."""
import re, sys, pathlib
tu = pathlib.Path(__file__).resolve().parent.parent
cfg = (tu / 'src/main/resources/config.yml').read_text()
gat = []
for m in re.finditer(r"^  (\w+):\s*\{(.*)\}\s*$", cfg, re.M):
    pola = {k: (a if a else b).strip() for k, a, b in re.findall(r"(\w[\w-]*): (?:'((?:[^']|'')*)'|([^,]*))", m.group(2))}
    if 'model' not in pola: continue
    pola['id'] = m.group(1)
    gat.append(pola)
def pasek(v, lo, hi):
    n = 2 + round((float(v) - lo) / (hi - lo) * 6) if hi > lo else 5
    return '&a' + '▮' * n + '&8' + '▮' * (8 - n)
zakres = {k: (min(float(g[k]) for g in gat), max(float(g[k]) for g in gat)) for k in ('predkosc', 'przyspieszenie', 'skret')}
out = tu / 'wdrozenie/pliki/DeluxeMenus/gui_menus'
out.mkdir(parents=True, exist_ok=True)

def esc(s): return s.replace("'", "''")

glowne = """menu_title: '&8Wyścigi Chowańców'
open_command: []
size: 27
update_interval: 20
items:
  tlo:
    material: GRAY_STAINED_GLASS_PANE
    slots: [0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,22,23,24,25,26]
    display_name: ' '
  czasowka:
    material: CLOCK
    slot: 10
    display_name: '&eCzasówka'
    lore:
      - '&7Sam na torze, walczysz z zegarem.'
      - '&7Najlepszy czas trafia do rekordów.'
      - ''
      - '&fKliknij, by wybrać wierzchowca.'
    left_click_commands:
      - '[openguimenu] wyscigi_czasowka'
  wyscig:
    material: FIREWORK_ROCKET
    slot: 12
    display_name: '&6Wyścig z innymi'
    lore:
      - '&7Do 8 jeźdźców naraz. Poczekalnia 20 s,'
      - '&7każdy, kto dołączy, wydłuża ją o chwilę.'
      - '&7Jeśli nikt nie przyjdzie, jedziesz sam.'
      - ''
      - '&fKliknij, by wybrać wierzchowca.'
    left_click_commands:
      - '[openguimenu] wyscigi_wyscig'
  rekordy:
    material: WRITABLE_BOOK
    slot: 14
    display_name: '&bRekordy toru'
    lore:
      - '&7Najlepsze przejazdy i najszybsze okrążenie.'
    left_click_commands:
      - '[console] wyscigi rekordy %player_name%'
      - '[close]'
  sterowanie:
    material: OAK_SIGN
    slot: 16
    display_name: '&fJak jeździć'
    lore:
      - '&fW &7gaz, &fS &7hamulec, &fmysz &7skręt'
      - '&fShift w zakręcie &7= poślizg; im dłużej,'
      - '&7tym mocniejsze turbo po puszczeniu'
      - '&7(iskry: &fbiałe &7→ &9niebieskie &7→ &6pomarańczowe&7)'
      - '&fSpacja &7podskok, &fA/D &7unik w bok'
      - '&dPola ▶▶ &7na torze przyspieszają'
      - '&fW &7tuż przed &aSTART! &7= turbo startowe'
      - '&7Za wcześnie = wierzchowiec się spłoszy'
      - '&7Wyjście: przytrzymaj &fShift &7na postoju'
      - '&7Widok z boku: &fF5'
"""
(out / 'wyscigi.yml').write_text(glowne)

for tryb, tytul in (('czasowka', 'Czasówka'), ('wyscig', 'Wyścig')):
    s = [f"menu_title: '&8{tytul}: wybierz wierzchowca'", 'open_command: []', 'size: 36', 'update_interval: 20', 'items:',
         '  tlo:', '    material: GRAY_STAINED_GLASS_PANE', '    slots: [0,1,2,3,4,5,6,7,8,9,17,18,26,27,28,29,30,32,33,34,35]', "    display_name: ' '"]
    sloty = [10, 11, 12, 14, 15, 16]
    for g, slot in zip(gat, sloty):
        s += [f"  {g['id']}:", f"    material: nexo-{g['ikona']}", f"    slot: {slot}", f"    display_name: '&e{esc(g['nazwa'])}'", '    lore:',
              f"      - '&7{esc(g['opis'])}'", "      - ''",
              f"      - '&7Prędkość      {pasek(g['predkosc'], *zakres['predkosc'])}'",
              f"      - '&7Przyspieszenie {pasek(g['przyspieszenie'], *zakres['przyspieszenie'])}'",
              f"      - '&7Skręt          {pasek(g['skret'], *zakres['skret'])}'",
              "      - ''", "      - '&fKliknij, by ruszyć.'",
              '    left_click_commands:', f"      - '[console] wyscigi {tryb} %player_name% {g['id']}'", "      - '[close]'"]
    s += ['  wroc:', '    material: ARROW', '    slot: 31', "    display_name: '&7Wróć'", '    left_click_commands:', "      - '[openguimenu] wyscigi'"]
    (out / f'wyscigi_{tryb}.yml').write_text('\n'.join(s) + '\n')
print('gatunki:', [g['id'] for g in gat])
