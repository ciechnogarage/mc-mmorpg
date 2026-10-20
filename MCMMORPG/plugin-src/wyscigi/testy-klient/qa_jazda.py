#!/usr/bin/env python3
"""Klient QA jedzie jak czlowiek: W trzymane przez xdotool, skret ruchem myszy (wzgledny), poslizg Shiftem w zakretach.
Kat liczony z pozycji wierzchowca (`wyscigi poz`) i obrotu gracza (`data get entity Rotation`) przez RCON klona.

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
try:
    while time.time() - t0 < limit:
        r = rcon(f'wyscigi poz {nick}')
        m = re.search(r'POZ ([-\d.]+) [-\d.]+ ([-\d.]+) .*faza=(\w+) meta=(-?\d+)', r)
        if not m:
            print('koniec: brak kierowcy'); break
        px, pz, faza, meta = float(m.group(1)), float(m.group(2)), m.group(3), int(m.group(4))
        if meta >= 0:
            print('meta'); break
        rot = re.search(r'\[([-\d.]+)f, ([-\d.]+)f\]', rcon(f'data get entity {nick} Rotation'))
        if not rot:
            continue
        yaw = float(rot.group(1))
        zakres = range(n) if idx < 0 else [(idx + k - 10) % n for k in range(21)]
        idx = min(zakres, key=lambda i: (pkt[i][0] - px) ** 2 + (pkt[i][1] - pz) ** 2)
        cel = pkt[(idx + 4) % n]
        cel_yaw = math.degrees(math.atan2(-(cel[0] - px), cel[1] - pz))
        roznica = zawin(cel_yaw - yaw)
        if poslizg:
            a, b, c = pkt[(idx + 2) % n], pkt[(idx + 4) % n], pkt[(idx + 9) % n]
            skret = abs(zawin(math.degrees(math.atan2(-(c[0] - b[0]), c[1] - b[1])) - math.degrees(math.atan2(-(b[0] - a[0]), b[1] - a[1]))))
            chce = faza == 'JAZDA' and (skret > 20 or (shift and skret > 10))
            if chce != shift:
                x('keydown' if chce else 'keyup', 'shift'); shift = chce
        dx = int(max(-400, min(400, roznica * 0.6 / STOPNI_NA_PIKSEL)))   # ~60% bledu na krok: plynnie jak reka
        if abs(dx) > 1:
            x('mousemove_relative', '--', str(dx), '0')
        print(f'{time.time() - t0:6.1f}s {faza:10} yaw {yaw:7.1f} cel {cel_yaw:7.1f} dx {dx:5d} shift {shift}', flush=True)
finally:
    x('keyup', 'w'); x('keyup', 'shift')
