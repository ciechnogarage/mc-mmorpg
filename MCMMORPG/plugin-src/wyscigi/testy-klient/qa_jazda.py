#!/usr/bin/env python3
"""Klient QA jedzie jak czlowiek z klawiatura: W + A/D przez xdotool, poslizg Shift + strona w zakretach.
Kat liczony z pozycji i kierunku wierzchowca (`wyscigi poz`) przez RCON klona.

  qa_jazda.py <klon> <kontener-klienta> <nick> [sekundy] [--poslizg]
Czulosc myszy MC 0.5 (domyslna): 0.15 stopnia na piksel.
"""
import math, re, subprocess, sys, time

klon, klient, nick = sys.argv[1], sys.argv[2], sys.argv[3]
limit = float(sys.argv[4]) if len(sys.argv) > 4 and not sys.argv[4].startswith('--') else 200
poslizg = '--poslizg' in sys.argv
STOPNI_NA_PIKSEL = 0.15


def rcon(cmd):
    out = subprocess.run(['docker', 'exec', 'klon-' + klon, 'rcon-cli', cmd], capture_output=True, text=True).stdout
    return re.sub(r'\x1b\[[0-9;]*m', '', out)


def x(*a):
    subprocess.run(['docker', 'exec', '-e', 'DISPLAY=:99', klient, 'xdotool', *a], capture_output=True)


def zawin(k):
    k %= 360
    return k - 360 if k >= 180 else k


tor = re.search(r'TOR ([\d.]+) (.*)', rcon('wyscigi tor'))
pkt = [tuple(map(float, s.split(','))) for s in tor.group(2).split(';') if s]
n = len(pkt)
idx, shift = -1, False
x('keydown', 'w')
t0 = time.time()
trzyma = set()
def klawisze(chce):
    for k in ('a', 'd', 'shift'):
        if k in chce and k not in trzyma: x('keydown', k); trzyma.add(k)
        if k not in chce and k in trzyma: x('keyup', k); trzyma.discard(k)
strona = 0
try:
    while time.time() - t0 < limit:
        r = rcon(f'wyscigi poz {nick}')
        m = re.search(r'POZ ([-\d.]+) [-\d.]+ ([-\d.]+) kier=([-\d.]+) .*faza=(\w+) meta=(-?\d+)', r)
        if not m:
            print('koniec: brak kierowcy'); break
        px, pz, kier, faza, meta = float(m.group(1)), float(m.group(2)), float(m.group(3)), m.group(4), int(m.group(5))
        if meta >= 0:
            print('meta'); break
        zakres = range(n) if idx < 0 else [(idx + k - 10) % n for k in range(21)]
        idx = min(zakres, key=lambda i: (pkt[i][0] - px) ** 2 + (pkt[i][1] - pz) ** 2)
        cel = pkt[(idx + 4) % n]
        roznica = zawin(math.degrees(math.atan2(-(cel[0] - px), cel[1] - pz)) - kier)
        chce = set()
        if poslizg and faza == 'JAZDA':
            a, b, c = pkt[(idx + 2) % n], pkt[(idx + 4) % n], pkt[(idx + 9) % n]
            skret = zawin(math.degrees(math.atan2(-(c[0] - b[0]), c[1] - b[1])) - math.degrees(math.atan2(-(b[0] - a[0]), b[1] - a[1])))
            if not shift and abs(skret) > 20: shift, strona = True, (1 if skret > 0 else -1)
            elif shift and (abs(skret) < 7 or (1 if skret > 0 else -1) != strona or roznica * strona < -25): shift = False
        if shift:
            chce.add('shift'); do_srodka = roznica * strona > -4
            chce.add(('d' if strona > 0 else 'a') if do_srodka else ('a' if strona > 0 else 'd'))
        elif roznica > 3: chce.add('d')
        elif roznica < -3: chce.add('a')
        klawisze(chce)
        print(f'{time.time() - t0:6.1f}s {faza:10} kier {kier:7.1f} roz {roznica:6.1f} {sorted(chce)}', flush=True)
        time.sleep(0.05)
finally:
    x('keyup', 'w'); x('keyup', 'shift'); x('keyup', 'a'); x('keyup', 'd')
