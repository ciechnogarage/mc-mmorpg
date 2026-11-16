#!/usr/bin/env python3
"""Wylacza glod: kafel HUD, spadek jedzenia (Multiverse), zapis jedzenia per postac (MMOProfiles), najedzenie przy wejsciu (CMI).
Uzycie: zmien.py <katalog plugins>"""
import sys, re
K = sys.argv[1]
def zmien(p, f):
    s = open(p).read(); s2 = f(s)
    if s2 == s: print('BEZ ZMIAN', p); return
    open(p, 'w').write(s2); print('zmieniony', p)
def hud(s):
    return re.sub(r"    glod:\n(      .*\n)+", "", s)
zmien(K + '/MythicHUD/layouts/hotbar.yml', hud)
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
