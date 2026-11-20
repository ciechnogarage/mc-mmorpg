#!/usr/bin/env python3
"""Woda w mapach MythicDungeons (tylko odczyt plikow regionow) -> kandydaci na lowiska w lochach.
Lowisko = spojna tafla wody (woda, nad nia powietrze) o polu >= MIN blokow — tam splawik moze siasc.
Wynik: tafle z srodkiem, polem, glebokoscia i komenda znacznika (rzemioslo postaw lowisko_tN ...) — NIE wdraza.
Uzycie: woda_lochow.py [katalog_map=/opt/docker/mc/data/world1/plugins/MythicDungeons/maps] [min_pole=6]
"""
import os, sys, glob, collections
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../kuznia/tools'))
import mca  # czytnik Anvil bez zaleznosci (kuznia/tools/mca.py)

KAT = sys.argv[1] if len(sys.argv) > 1 else '/opt/docker/mc/data/world1/plugins/MythicDungeons/maps'
MIN = int(sys.argv[2]) if len(sys.argv) > 2 else 6
WODA = {'minecraft:water'}
POW = mca.AIR

def bloki_mapy(d):
    """{(x,y,z): nazwa} tylko dla wody i blokow tuz nad woda (oszczednie: sekcje z woda w palecie)."""
    woda, nad = set(), {}
    for p in glob.glob(os.path.join(d, 'region', 'r.*.mca')):
        r = mca.Region(p)
        for cx, cz in r.chunks():
            ch = r.chunk(cx & 31, cz & 31)
            if not ch: continue
            secs = {s.get('Y', 0): s for s in ch.get('sections', [])}
            lok = set()
            for sy, sec in secs.items():
                pal = (sec.get('block_states') or {}).get('palette') or []
                if not any(b.get('Name') in WODA for b in pal): continue
                for ly in range(16):
                    for lz in range(16):
                        for lx in range(16):
                            if mca._section_block(sec, lx, ly, lz) in WODA:
                                lok.add((cx * 16 + lx, sy * 16 + ly, cz * 16 + lz))
            for (x, y, z) in lok:   # blok nad woda (moze lezec w nastepnej sekcji)
                ny = y + 1; s = secs.get(ny >> 4)
                nad[(x, y, z)] = mca._section_block(s, x & 15, ny & 15, z & 15) if s else 'minecraft:air'
            woda |= lok
    return woda, nad

def tafle(woda, nad):
    pow_ = {w for w in woda if (nad.get(w) or 'minecraft:air') in POW}
    zost, wynik = set(pow_), []
    while zost:
        st = zost.pop(); q = collections.deque([st]); t = [st]
        while q:
            x, y, z = q.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y, z + dz)
                if n in zost: zost.remove(n); q.append(n); t.append(n)
        if len(t) >= MIN:
            gl = []
            for (x, y, z) in t:
                g = 0
                while (x, y - g, z) in woda: g += 1
                gl.append(g)
            sx = sum(p[0] for p in t) / len(t); sz = sum(p[2] for p in t) / len(t)
            c = min(t, key=lambda p: (p[0] - sx) ** 2 + (p[2] - sz) ** 2)    # blok tafli najblizej srodka
            wynik.append({'pole': len(t), 'y': t[0][1], 'srodek': c, 'gleb_max': max(gl),
                          'x': (min(p[0] for p in t), max(p[0] for p in t)), 'z': (min(p[2] for p in t), max(p[2] for p in t))})
    return sorted(wynik, key=lambda w: -w['pole'])

def main():
    for d in sorted(glob.glob(os.path.join(KAT, '*/'))):
        n = os.path.basename(d.rstrip('/'))
        if not os.path.isdir(os.path.join(d, 'region')): continue
        pliki = glob.glob(os.path.join(d, 'region', 'r.*.mca'))
        nch = sum(len(mca.Region(p).chunks()) for p in pliki)
        if not nch: print(f'== {n}: brak zapisanych chunkow (region pusty — teren z generatora, np. Iris; wody nie da sie policzyc z pliku)'); continue
        woda, nad = bloki_mapy(d)
        tt = tafle(woda, nad)
        print(f'== {n}: {nch} chunkow, wody {len(woda)} blokow, tafli >= {MIN}: {len(tt)}')
        for t in tt[:12]:
            x, y, z = t['srodek']
            print(f"  pole {t['pole']:4d}  y={t['y']}  x {t['x'][0]}..{t['x'][1]}  z {t['z'][0]}..{t['z'][1]}  glebokosc<= {t['gleb_max']}"
                  f"  srodek {x} {y} {z}  znacznik (dno pod srodkiem): rzemioslo postaw lowisko_tN <swiat-instancji> {x} {y - t['gleb_max']} {z}")

if __name__ == '__main__':
    main()
