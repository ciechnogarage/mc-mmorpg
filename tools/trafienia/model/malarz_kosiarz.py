"""Malarz tekstur Kosiarza: płaskie tony jak LR (bez szumu per piksel) + wzór materiału ('wzor' w MAT).
Sygnatura jak bryly_sdf.malarz: (sciana, w, h, mat, rng) -> RGBA h×w. Ściana 'north' = przód (-Z)."""
import numpy as np

TON = {'up': 3, 'north': 2, 'west': 2, 'east': 1, 'south': 1, 'down': 0}


def maluj(sciana, w, h, mat, rng):
    R = np.asarray(mat['ramp'], np.uint8)
    t0 = TON[sciana]
    T = np.full((h, w), t0, int)
    wz = mat.get('wzor')
    pion = sciana not in ('up', 'down')
    if wz in ('splot', 'twarz'):
        # splot płótna: rzadkie ciemniejsze nitki w szachownicę, łata z szwem na większych ścianach
        for y in range(h):
            for x in range(w):
                if (x + y) % 2 == 0 and rng.random() < .08: T[y, x] -= 1
        if w >= 4 and h >= 4 and rng.random() < .4:
            pw, ph = min(w - 1, int(rng.integers(3, 5))), min(h - 1, int(rng.integers(3, 5)))
            x0, y0 = int(rng.integers(0, w - pw + 1)), int(rng.integers(0, h - ph + 1))
            T[y0:y0 + ph, x0:x0 + pw] = t0 + (1 if t0 < 3 else -1)
            for x in range(x0, x0 + pw, 2):
                T[y0, x] = 0; T[min(h - 1, y0 + ph - 1), x] = 0
    elif wz == 'zdzbla':
        if pion:
            for x in range(w):
                T[:, x] = int(rng.integers(1, 4))
                for y in range(h):
                    if rng.random() < .07: T[y, x] = 0
        else:
            for y in range(h):
                for x in range(w):
                    T[y, x] = 2 + ((x * 7 + y * 3) % 5 == 0) - ((x + y * 5) % 7 == 0)
    elif wz == 'skret':
        for y in range(h):
            for x in range(w):
                T[y, x] = 1 + (((x + y) % 3) == 0) + (t0 >= 2)
    elif wz == 'plecionka':
        for y in range(h):
            for x in range(w):
                T[y, x] = 2 if ((x // 2 + y // 2) % 2) else 1
                if x % 2 == 0 and y % 2 == 0 and rng.random() < .3: T[y, x] = 0
    elif wz == 'sloje':
        for x in range(w):
            T[:, x] = t0 - (1 if rng.random() < .35 else 0)
    elif wz == 'stal':
        for y in range(h):
            T[y, :] = 2 if y == 0 else 1 + (y < h / 2)
    elif wz == 'pedy':
        if pion:
            T[:, :] = 1; T[0, :] = 2
    if wz == 'twarz' and sciana == 'north':
        T[0:2, :] = 0                                   # cień ronda kapelusza
        yu = max(2, h - 2)                               # usta zszyte powrósłem
        for x in range(1, w - 1):
            T[yu, x] = 0 if x % 2 else 1
    if pion and h >= 4 and wz not in ('zdzbla', 'stal'):
        T[-1, :] -= 1                                    # AO u dołu
        if wz != 'twarz': T[0, :] = np.maximum(T[0, :], t0 + 1)
    T = np.clip(T, 0, 3)
    out = np.empty((h, w, 4), np.uint8)
    out[:, :, :3] = R[T]; out[:, :, 3] = 255
    if wz == 'stal':                                     # rdza: kilka plam
        for _ in range(w * h // 40):
            x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
            out[y, x, :3] = (112, 74, 44)
    return out
