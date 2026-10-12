# -*- coding: utf-8 -*-
"""Ikony 16x16 profesji v2. Ksztalty = istniejace mapy pikseli z tools/ikony-profesji (ta sama rodzina:
kontur (24,22,30), 3 tony s/b/h ze swiatlem z gory-lewej, akcent a/A) + dwa nowe ksztalty (kilof, namiot).
Kolor: material/narzedzie/rzadki = ksztalt przebarwiony w strone akcentu krainy z zachowaniem jasnosci
(identycznosc ksztaltu zostaje, kraina czyta sie barwa); buff/klejnot = barwa typu. Plakietka tieru
(kolor krainy) w prawym gornym rogu — tam, gdzie plakietki v1 (pomiar zajetosci rogow w tier.py)."""
import colorsys
import importlib
import os
import sys

from PIL import Image

import tresc as T

OUTLINE = (24, 22, 30, 255)
PRZEZROCZ = (0, 0, 0, 0)

NOWE = {
    # kilof: glowica metalowa (s/b/h), trzonek drewniany (a/A)
    'kilof': ('iron', 'wood', [
        "................",
        "...ooooooooo....",
        "..obhhhhhhhbo...",
        ".obsoooooooosbo.",
        ".oso...oAo..oso.",
        "..o...oAao...o..",
        "......oao.......",
        ".....oao........",
        "....oAao........",
        "....oao.........",
        "...oAao.........",
        "...oao..........",
        "..oAao..........",
        "..oao...........",
        "..oo............",
        "................"]),
    # namiot obozowy: plotno (a/A), wejscie i palik (s/b)
    'namiot': ('wood', 'fleece', [
        "................",
        ".......oo.......",
        "......oAAo......",
        ".....oAAaao.....",
        ".....oAaaao.....",
        "....oAAaaaao....",
        "....oAaaoaao....",
        "...oAAaosoaao...",
        "...oAaaosoaao...",
        "..oAAaosssoaao..",
        "..oAaaosssoaao..",
        ".oAAaaosbsoaaao.",
        ".oooooooooooooo.",
        "................",
        "................",
        "................"]),
}

# serce bossa (rzadki skladnik aktu): czerwien z polyskiem w lewym gornym platku
NOWE['serce'] = ('ruby', 'ruby', [
    "................",
    "................",
    "...ooo....ooo...",
    "..oAAbo..obbbo..",
    ".oAhbbbooobbbso.",
    ".oAbbbbbbbbbbso.",
    ".obbbbbbbbbbbso.",
    ".obbbbbbbbbbsso.",
    "..obbbbbbbbsso..",
    "...obbbbbbsso...",
    "....obbbbsso....",
    ".....obbsso.....",
    "......osso......",
    ".......oo.......",
    "................",
    "................"])

KOLOR_TYPU = {  # barwa (h, s) dla buffow i klejnotow
    'MIKSTURA_LECZNICZA': (0.99, 0.75), 'MIKSTURA_MANY': (0.60, 0.70), 'ODTRUTKA': (0.33, 0.60),
    'ELIKSIR_SILY': (0.04, 0.80), 'ELIKSIR_UMYSLU': (0.78, 0.60), 'ELIKSIR_SKORY': (0.58, 0.20),
    'ELIKSIR_ZWINNOSCI': (0.45, 0.65), 'OLEJ': (0.11, 0.80),
    'KLEJNOT_ZDROWIA': (0.98, 0.75), 'KLEJNOT_OCHRONY': (0.58, 0.25), 'KLEJNOT_ODNOWY': (0.36, 0.70),
    'KLEJNOT_ZWINNOSCI': (0.47, 0.70), 'KLEJNOT_CELNOSCI': (0.13, 0.85),
    'RUNA_SILY': (0.03, 0.75), 'RUNA_MOCY': (0.70, 0.65), 'RUNA_ZARU': (0.08, 0.85), 'RUNA_GROZY': (0.88, 0.60),
    'RUNA_POSPIECHU': (0.52, 0.65),
    'PRZYPRAWA': (0.07, 0.70), 'OGIEN': (0.05, 0.90), 'OBUMARCIE': (0.80, 0.25),
}
BEZ_PRZEBARWIENIA = {'POSILEK_SYTY', 'POSILEK_ROSOL', 'POSILEK_PROWIANT', 'UCZTA', 'DANIE_MISTRZA', 'OPATRUNEK'}


class Ikony:
    def __init__(self, katalog_ikon):
        sys.path.insert(0, katalog_ikon)
        self.ksz = {}
        for m in ('generate_icons', 'generate_icons2', 'generate_icons3', 'generate_icons4', 'generate_icons5'):
            mod = importlib.import_module(m)
            self.ksz.update(mod.ICONS)
        gi = importlib.import_module('generate_icons')
        self.render_v1 = gi.render
        for n, (ton, akc, rows) in NOWE.items():
            assert len(rows) == 16 and all(len(r) == 16 for r in rows), n
            self.ksz[n] = (gi.mk(ton, akc), rows)

    def obraz(self, ksztalt):
        if ksztalt not in self.ksz:
            raise SystemExit(f'brak ksztaltu ikony {ksztalt}')
        cmap, rows = self.ksz[ksztalt]
        return self.render_v1(rows, cmap)

    @staticmethod
    def _lum(c):
        return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]

    def przebarw_kraina(self, im, t, sila=0.5):
        akc = T.KRAINY[t][2][1]
        px = im.load()
        for y in range(16):
            for x in range(16):
                c = px[x, y]
                if c[3] == 0 or c[:3] == OUTLINE[:3]:
                    continue
                mix = tuple(c[i] * (1 - sila) + akc[i] * sila for i in range(3))
                l0, l1 = self._lum(c), self._lum(mix) or 1
                k = l0 / l1
                px[x, y] = tuple(max(0, min(255, int(v * k))) for v in mix) + (c[3],)
        return im

    def przebarw_typ(self, im, klucz):
        h, s = KOLOR_TYPU[klucz]
        px = im.load()
        for y in range(16):
            for x in range(16):
                c = px[x, y]
                if c[3] == 0 or c[:3] == OUTLINE[:3]:
                    continue
                _, _, v = colorsys.rgb_to_hsv(*(ch / 255 for ch in c[:3]))
                r, g, b = colorsys.hsv_to_rgb(h, s, v)
                px[x, y] = (int(r * 255), int(g * 255), int(b * 255), c[3])
        return im

    @staticmethod
    def plakietka(im, t):
        base, hi = T.KRAINY[t][2][1], T.KRAINY[t][2][2]
        px = im.load()
        x0, y0 = 11, 1
        for dy in range(4):
            for dx in range(4):
                px[x0 + dx, y0 + dy] = OUTLINE if (dx in (0, 3) or dy in (0, 3)) else base + (255,)
        px[x0 + 1, y0 + 1] = hi + (255,)
        return im

    def ikona(self, spec):
        """spec z model.Model.items[...]['ikona']; zwraca obraz albo None (stara ikona v1 zostaje)."""
        if not spec or spec.get('stara'):
            return None
        im = self.obraz(spec['ksztalt']).copy()
        styl = spec['styl']
        t = spec.get('kraina') or spec.get('tier') or 1
        if styl in ('material', 'narzedzie', 'rzadki', 'inne'):
            if not (styl == 'material' and t == 1):
                sila = 0.2 if spec['ksztalt'] == 'serce' else 0.65 if styl == 'rzadki' else 0.45
                self.przebarw_kraina(im, t, sila)
        elif styl in ('buff', 'klejnot'):
            if spec['kolor'] not in BEZ_PRZEBARWIENIA:
                self.przebarw_typ(im, spec['kolor'])
        return self.plakietka(im, t)
