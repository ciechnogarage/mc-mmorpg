# -*- coding: utf-8 -*-
"""Tekstury blokow wezlow v2 (16x16, deterministyczne): zloze = kamien krainy z wtraceniami akcentu,
pien = kora krainy (bok) + slojki (gora), rosliny = sylwetka ikony surowca (cross), lowisko = piasek z muszlami."""
import hashlib
import random

from PIL import Image

import tresc as T

OUT = (24, 22, 30, 255)


def _rng(nid):
    return random.Random(int(hashlib.md5(nid.encode()).hexdigest()[:8], 16))


def _mix(a, b, k):
    return tuple(int(a[i] * (1 - k) + b[i] * k) for i in range(3))


def zloze(nid, t, glina=False):
    r = _rng(nid)
    s, b, h = T.KRAINY[t][1]
    if glina:
        s, b, h = (96, 78, 64), (132, 108, 88), (168, 140, 116)
    akc = T.KRAINY[t][2]
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            v = r.random()
            c = h if v > 0.82 else b if v > 0.35 else s
            px[x, y] = c + (255,)
    # wtracenia rudy: 5 grudek 2x2 z cieniem i polyskiem
    for _ in range(5 if not glina else 3):
        x, y = r.randint(1, 13), r.randint(1, 13)
        for dx, dy, kol in ((0, 0, akc[2]), (1, 0, akc[1]), (0, 1, akc[1]), (1, 1, akc[0])):
            px[x + dx, y + dy] = (kol if not glina else _mix(kol, (150, 150, 150), 0.6)) + (255,)
    return im


def pien(nid, t):
    r = _rng(nid)
    drewno = ((66, 42, 26), (99, 64, 38), (132, 90, 56))
    akc = T.KRAINY[t][2][1]
    tony = [_mix(c, akc, 0.25 if t > 1 else 0) for c in drewno]
    bok = Image.new('RGBA', (16, 16))
    px = bok.load()
    for x in range(16):
        baza = tony[1] if x % 4 else tony[0]
        for y in range(16):
            v = r.random()
            c = tony[2] if v > 0.9 else tony[0] if v < 0.12 else baza
            px[x, y] = c + (255,)
    gora = Image.new('RGBA', (16, 16))
    pg = gora.load()
    jasne = _mix(tony[2], (230, 210, 170), 0.35)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 7:
                c = tony[0]
            else:
                c = jasne if int(d) % 3 else tony[1]
            pg[x, y] = c + (255,)
    return bok, gora


def roslina(ik, iid, t):
    ksz = iid.lower().replace('prof_', '', 1)
    if ksz not in ik.ksz:
        ksz = {'mining': 'ore_chunk'}.get(ksz, None)
    return ksz


def lowisko(nid, t):
    r = _rng(nid)
    piasek = ((168, 148, 104), (196, 178, 132), (222, 206, 166))
    akc = T.KRAINY[t][2]
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            v = r.random()
            px[x, y] = (piasek[2] if v > 0.8 else piasek[1] if v > 0.3 else piasek[0]) + (255,)
    for _ in range(4):
        x, y = r.randint(1, 13), r.randint(1, 13)
        px[x, y] = akc[2] + (255,)
        px[x + 1, y] = akc[1] + (255,)
        px[x, y + 1] = akc[0] + (255,)
    return im


def tekstura_bloku(nid, w, ik, model, katalog):
    t = w['tier']
    prof = w['profesja']
    if prof == 'fishing':
        return {f'{katalog}/{nid}.png': lowisko(nid, t)}
    if prof == 'mining':
        return {f'{katalog}/{nid}.png': zloze(nid, t, glina=('glina' in nid))}
    if prof == 'woodworking':
        bok, gora = pien(nid, t)
        return {f'{katalog}/{nid}.png': bok, f'{katalog}/{nid}_top.png': gora}
    # rosliny: sylwetka glownego surowca (bez plakietki); T2+ przebarwiona w strone krainy
    it = model.items[w['glowny']]
    spec = it['ikona'] or {}
    ksz = spec.get('ksztalt') or it['id'].lower().replace('prof_', '', 1)
    if ksz in ik.ksz:
        im = ik.obraz(ksz).copy()
    else:  # ikona v1 spoza generatora ksztaltow — gotowa tekstura z packa
        import glob as _g
        pliki = _g.glob(katalog.split('/block/')[0] + f'/item/**/{ksz}.png', recursive=True)
        if not pliki:
            raise SystemExit(f'brak ksztaltu i tekstury dla rosliny {ksz}')
        im = Image.open(pliki[0]).convert('RGBA')
    if t > 1:
        ik.przebarw_kraina(im, t, 0.45)
    # roslina w swiecie bez konturu ikony (vanilla rosliny go nie maja; z nim wygladala jak powiekszony przedmiot)
    px = im.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and px[x, y][:3] == OUT[:3]:
                px[x, y] = (0, 0, 0, 0)
    return {f'{katalog}/{nid}.png': im}
