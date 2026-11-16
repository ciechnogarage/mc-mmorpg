#!/usr/bin/env python3
"""Wylacza glod: kafel HUD (pancerz do okna postaci), spadek jedzenia (Multiverse), zapis jedzenia per postac (MMOProfiles), najedzenie przy wejsciu (CMI).
Uzycie: zmien.py <katalog plugins>"""
import sys, re, os, shutil
K = sys.argv[1]
def zmien(p, f):
    s = open(p).read(); s2 = f(s)
    if s2 == s: print('BEZ ZMIAN', p); return
    open(p, 'w').write(s2); print('zmieniony', p)
# HUD: pas przy hotbarze (kafle glod + zbroja) znika; pancerz przechodzi do okna postaci (decyzja 16.11).
H = K + '/MythicHUD'
zmien(H + '/layouts/hotbar.yml', lambda s: s if s.startswith('# Pusty') else
      '# Pusty od 16.11: kafel glodu usuniety (glod wylaczony), pancerz w oknie postaci (l1-okno.yml).\n')
zmien(H + '/config.yml', lambda s: s.replace('  - hotbar-layout\n', ''))
ZX, ZY = int(os.environ.get('ZBROJA_X', 144)), int(os.environ.get('ZBROJA_Y', 18))
def warstwy(przyg):
    # Zakladka przy prawej krawedzi okna, na wysokosci klasy: nazwy klas maja do 18 znakow i nachodza na wszystko w wierszu.
    s_, kol = ('_p', '<#A89A8C>') if przyg else ('', '<#FFF1CC>')
    return (f"    zbroja-liczba:\n      text: '{kol}%zbroja-value%'\n      font: default\n      outlined: true\n"
            f"      align: center\n      offset: {{x: {ZX + 26}, y: -{ZY + 4}}}\n"
            f"    zbroja:\n      texture: {{path: assets/l1/dane.png}}\n      scale: 0.5\n      offset: {{x: 0, y: 0}}\n"
            f"      function: {{listener: armor, increment: 1, orientation: horizontal}}\n"
            f"    zbroja-tlo:\n      texture: {{path: assets/l1/zbroja_skrzydlo{s_}.png}}\n      scale: 0.5\n"
            f"      offset: {{x: {ZX}, y: -{ZY}}}\n")
def okno(s):
    if 'zbroja-liczba:' in s: return s
    cz = re.split(r'(?m)^(?=l1-okno)', s)
    for i, c in enumerate(cz):
        m = re.match(r'l1-okno(-przygaszone)?:', c)
        if m: cz[i] = re.sub(r"(    exp-proc:\n(?:      .*\n)+)", lambda x: x.group(1) + warstwy(bool(m.group(1))), c, count=1)
    return ''.join(cz)
zmien(H + '/hud_assets/hud/l1-okno.yml', okno)
T = H + '/source-pack/assets/mythichud/textures/assets/l1/'
for n in ('zbroja_skrzydlo.png', 'zbroja_skrzydlo_p.png'):
    shutil.copyfile(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'tex', n), T + n); print('tekstura', T + n)
# Multiverse: przez 'mv modify <swiat> set hunger false' (wdroz.sh), bo plugin sam zapisuje worlds.yml
def prof(s):
    for k in ('food-level', 'saturation'):
        s = s.replace(f'  {k}: true\n', f'  {k}: false\n', 1)
    return s
zmien(K + '/MMOProfiles/config.yml', prof)
def cmi(s):
    old = ("joinServer:\n  Enabled: false\n  Commands:\n  - cmi msg [playerName] !This is event command example!\n"
           "  - cmi msg [playerName] !Originates from sneakingSwapHandItems event!\n")
    return s.replace(old, "joinServer:\n  Enabled: true\n  Commands:\n  - asConsole! cmi feed [playerName]\n")
zmien(K + '/CMI/Settings/EventCommands.yml', cmi)
