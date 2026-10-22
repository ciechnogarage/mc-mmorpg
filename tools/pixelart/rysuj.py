#!/usr/bin/env python3
"""Minimalna biblioteka do pixel-artu z PELNA kontrola nad kazdym pikselem.

Zalozenie: znak w siatce ASCII = konkretny kolor z palety. Zadnego solvera,
zadnych prymitywow rysujacych za ciebie. Cieniowanie jest decyzja rysownika,
nie funkcji — bo to wlasnie te decyzje odrozniaja grafike komercyjna od
generowanej. Solver daje poprawna bryle; przesade, ktora sprawia, ze ikona
32 px czyta sie z daleka, trzeba narysowac.

Uzycie:
    from rysuj import Paleta, z_siatki, zapisz
    P = Paleta({'.': None, '0': '#0a0d16', '1': '#191f2e', ...})
    img = z_siatki(WIERSZE, P)
    zapisz(img, '/w/out/helm')          # zapisuje helm.png i helm_8x.png
"""
from PIL import Image


class Paleta(dict):
    """Znak -> kolor. '.' i ' ' zawsze znacza 'przezroczyste'."""

    def __init__(self, mapping):
        super().__init__()
        for k, v in mapping.items():
            self[k] = None if v is None else self._rgb(v)
        self.setdefault('.', None)
        self.setdefault(' ', None)

    @staticmethod
    def _rgb(v):
        if isinstance(v, (tuple, list)):
            return tuple(v) if len(v) == 4 else tuple(v) + (255,)
        s = v.lstrip('#')
        return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

    def kontroluj(self, limit=24):
        """Ile realnych kolorow ma paleta. Komercyjny asset 32x32 trzyma sie 12-20."""
        n = len({c for c in self.values() if c})
        if n > limit:
            print(f"UWAGA: paleta ma {n} kolorow, limit {limit}")
        return n


def z_siatki(wiersze, paleta, rozmiar=32):
    """Siatka ASCII -> obrazek. Wiersze krotsze niz 'rozmiar' sa dopelniane."""
    img = Image.new('RGBA', (rozmiar, rozmiar), (0, 0, 0, 0))
    px = img.load()
    for y, w in enumerate(wiersze[:rozmiar]):
        for x, ch in enumerate(w[:rozmiar]):
            c = paleta.get(ch)
            if c:
                px[x, y] = c
    return img


def zapisz(img, sciezka_bez_rozszerzenia, skale=(8,)):
    img.save(f'{sciezka_bez_rozszerzenia}.png')
    for s in skale:
        img.resize((img.width * s, img.height * s), Image.NEAREST).save(
            f'{sciezka_bez_rozszerzenia}_{s}x.png')


def luminancja(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
