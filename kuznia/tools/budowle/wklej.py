"""Osadzanie budowli na upieczonym terenie i wklejanie z konsoli (`place template`) w kuzni.

    import wklej
    w = wklej.Teren('loch_001_015948_1001')
    w.osadz(budowla, 'loch001', 'glaz_a', x=60, z=26, zaglebienie=1, fundament='minecraft:stone')

osadz(): grunt pod obrysem z plikow regionow (mca) -> podstawa = mediana gruntu - zaglebienie; kolumny, pod ktorymi
grunt jest nizej, dostaja fundament (nic nie wisi); zapis .nbt pod NOWA nazwa (serwer pamieta brak szablonu do restartu)
-> `execute in <swiat> run place template` -> kontrola: wypisuje odpowiedz serwera."""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import TOOLS, DATA, KONTENER  # noqa: E402
import os, statistics, subprocess, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, TOOLS)
import mca

MIEKKIE = ('leaves', 'grass', 'petals', 'flower', 'bush', 'log', 'wood', 'fern', 'poppy', 'dandelion', 'tulip', 'daisy',
           'lilac', 'peony', 'rose', 'allium', 'orchid', 'bluet', 'cornflower', 'lily', 'moss_carpet', 'vine', 'sapling',
           'mushroom', 'snow')


def rcon(cmd):
    r = subprocess.run(['docker', 'exec', KONTENER, 'rcon-cli', cmd], capture_output=True, text=True)
    return r.stdout.replace('\x00', '').strip()


class Teren:
    def __init__(self, swiat):
        self.swiat = swiat
        self.odswiez()

    def odswiez(self):
        rcon('save-all flush'); time.sleep(1)
        self.W = mca.World(f'{DATA}/{self.swiat}/region')

    def grunt(self, x, z):
        c = mca.column(self.W, x, z)
        for y, b in c or []:
            if not any(s in b for s in MIEKKIE):
                return y
        return None

    def osadz(self, budowla, ns, nazwa, x, z, zaglebienie=1, fundament='minecraft:dirt', y=None, srodek=True, przytnij=False, fundament_do=2):
        """x,z = SRODEK obrysu (srodek=True) albo rog min (srodek=False). place template stawia ROG w podanym punkcie."""
        bl = budowla.bloki
        x0 = min(p[0] for p in bl); y0 = min(p[1] for p in bl); z0 = min(p[2] for p in bl)
        if srodek:
            x -= (max(p[0] for p in bl) - x0) // 2
            z -= (max(p[2] for p in bl) - z0) // 2
        spod = {}
        for (bx, by, bz) in bl:            # najnizszy blok kazdej kolumny budowli
            k = (bx, bz)
            spod[k] = min(spod.get(k, by), by)
        grunty = {k: self.grunt(x + k[0] - x0, z + k[1] - z0) for k in spod}
        if przytnij:                       # bez ladu pod kolumna -> kolumna wypada (mur nie wisi nad pustka)
            for k in [k for k, g in grunty.items() if g is None]:
                for p in [p for p in bl if (p[0], p[2]) == k]:
                    del bl[p]
                del spod[k]; del grunty[k]
            # zapis przesuwa minimum do (0,0,0): po obcieciu rog budowli sie przesunal - przesun tez punkt wklejenia
            nx0 = min(p[0] for p in bl); nz0 = min(p[2] for p in bl)
            x += nx0 - x0; z += nz0 - z0
            grunty = {(k[0], k[1]): g for k, g in grunty.items()}
            x0, z0 = nx0, nz0
        znane = [g for g in grunty.values() if g is not None]
        if not znane:
            raise ValueError(f'{nazwa}: brak gruntu pod obrysem ({x},{z})')
        baza = y if y is not None else int(statistics.median(znane)) + 1 - zaglebienie
        for k, g in grunty.items():        # fundament: od gruntu do spodu kolumny - TYLKO pod podstawa budowli
            if g is None or spod[k] - y0 > fundament_do:
                continue                   # kolumna zaczyna sie wysoko (galaz, nawis) - nie podpieramy jej sciana
            spod_swiat = baza + spod[k] - y0
            for yy in range(g + 1, spod_swiat):
                budowla.postaw((k[0], y0 + (yy - baza), k[1]), fundament, nadpisz=False)
        nazwa = nazwa.lower()              # id szablonu: male litery (plik i komenda musza sie zgadzac)
        info = budowla.zapisz(ns, nazwa)
        # zapis przesuwa minimum do (0,0,0); fundament mogl obnizyc minimum y
        y_min = min(p[1] for p in budowla.bloki)
        cel = (x, baza - (y0 - y_min), z)
        odp = rcon(f'execute in minecraft:{self.swiat} run place template {ns}:{nazwa} {cel[0]} {cel[1]} {cel[2]}')
        info.update(cel=cel, odpowiedz=odp, grunt_min=min(znane), grunt_max=max(znane))
        return info
