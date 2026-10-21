"""Zestaw do skladania budowli z klockow: obiekty Iris (.iob), bryly proceduralne, rysunki warstwami (ASCII).
Wynik: vanillowa struktura .nbt -> wklejana z konsoli `place template` (bez gracza, bez pluginu).

    b = Budowla()
    b.obiekt('clutter/boulder6.iob', (0, 0, 0), obrot=90, podmiany={'cobblestone': 'mossy_cobblestone'})
    b.mur((0,0,0), (10,0,0), wys=4, paleta={'stone_bricks': 5, 'mossy_stone_bricks': 3, 'cracked_stone_bricks': 2},
          ubytek=0.35, ziarno=7)
    b.warstwy(['..#..', '.###.'], legenda={'#': 'campfire[lit=true]'}, poczatek=(0,0,0))   # warstwy od dolu
    b.zapisz('loch001', 'brama')      # -> world/generated/loch001/structures/brama.nbt

Obrot 0/90/180/270 (zgodnie z ruchem wskazowek patrzac z gory) obraca tez stany: facing, axis, rotation, ksztalt schodow
zostaje (MC przelicza go sam przy aktualizacji sasiada - dla ruin to wystarcza).
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import DATA  # noqa: E402
import os, random, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import iob, struktura

OBIEKTY = '/opt/docker/mc/data/world1/plugins/Iris/packs/overworld/objects'
GENERATED = f'{DATA}/world/generated'
DATA_VERSION = 4671          # Paper 1.21.11 (level.dat kuzni)
_FACING = ['north', 'east', 'south', 'west']


def _norm(b):
    return b if ':' in b.split('[')[0] else 'minecraft:' + b


def obroc_stan(stan, obrot):
    k = (obrot // 90) % 4
    if not k or '[' not in stan:
        return stan
    nazwa, wl = stan[:-1].split('[', 1)
    out = []
    for para in wl.split(','):
        a, v = para.split('=')
        if a == 'facing' and v in _FACING:
            v = _FACING[(_FACING.index(v) + k) % 4]
        elif a == 'axis' and v in ('x', 'z') and k % 2:
            v = 'z' if v == 'x' else 'x'
        elif a == 'rotation':
            v = str((int(v) + 4 * k) % 16)
        elif a in _FACING:        # plotki/sciany: north=true itd.
            a = _FACING[(_FACING.index(a) + k) % 4]
        out.append(f'{a}={v}')
    return f'{nazwa}[{",".join(out)}]'


def obroc_poz(x, z, obrot):
    k = (obrot // 90) % 4
    for _ in range(k):
        x, z = -z, x
    return x, z


class Budowla:
    def __init__(self):
        self.bloki = {}

    def postaw(self, pos, blok, nadpisz=True):
        if nadpisz or pos not in self.bloki:
            self.bloki[pos] = _norm(blok)

    def obiekt(self, sciezka, przesun=(0, 0, 0), obrot=0, podmiany=None, bez=()):
        """Wstaw obiekt Iris. podmiany: {'fragment_nazwy': 'nowy_blok'} (np. omszenie); bez: pomijane bloki."""
        o = iob.czytaj(sciezka if os.path.isabs(sciezka) else os.path.join(OBIEKTY, sciezka))
        ys = [p[1] for p in o['bloki']]
        y0 = min(ys)
        for (x, y, z), s in o['bloki'].items():
            nazwa = s.split('[')[0].split(':')[-1]
            if nazwa in bez or nazwa in ('air', 'cave_air', 'void_air', 'structure_void'):
                continue
            if podmiany:
                for frag, nowy in podmiany.items():
                    if nazwa == frag or (frag.startswith('*') and frag[1:] in nazwa):
                        stany = s[s.index('['):] if '[' in s and '[' not in nowy else ''
                        s = _norm(nowy) + stany
                        break
            rx, rz = obroc_poz(x, z, obrot)
            self.postaw((przesun[0] + rx, przesun[1] + y - y0, przesun[2] + rz), obroc_stan(s, obrot))
        return self

    def mur(self, a, b, wys, paleta, grub=1, ubytek=0.0, ziarno=0, zwienczenie=None, pelne_dol=3):
        """Mur po linii a->b (poziomo). ubytek: 0..1 - im wyzej, tym czesciej brak bloku (ruina).
        pelne_dol: tyle dolnych rzedow zawsze pelnych - inaczej ubytki robia przejscia na wysokosci gracza (zmierzone)."""
        rnd = random.Random(ziarno)
        (ax, ay, az), (bx, by, bz) = a, b
        n = max(abs(bx - ax), abs(bz - az)) or 1
        wagi = list(paleta.items())
        for i in range(n + 1):
            x = round(ax + (bx - ax) * i / n); z = round(az + (bz - az) * i / n)
            h = max(pelne_dol, wys - (rnd.random() < ubytek) * rnd.randint(1, max(1, wys - 1)))
            for y in range(h):
                if y >= pelne_dol and rnd.random() < ubytek * (y / wys) * 0.6:
                    continue
                for g in range(grub):
                    dx, dz = (0, g) if abs(bx - ax) >= abs(bz - az) else (g, 0)
                    self.postaw((x + dx, ay + y, z + dz), rnd.choices([k for k, _ in wagi], [w for _, w in wagi])[0])
            if zwienczenie and h == wys:
                self.postaw((x, ay + h, z), zwienczenie)
        return self

    def kopiec(self, srodek, r, wys, paleta, ziarno=0):
        """Nieregularny glaz/kopiec (elipsoida z szumem)."""
        rnd = random.Random(ziarno)
        cx, cy, cz = srodek
        wagi = list(paleta.items())
        for x in range(-r, r + 1):
            for z in range(-r, r + 1):
                for y in range(0, wys):
                    e = (x / r) ** 2 + (z / r) ** 2 + (y / wys) ** 2
                    if e <= 1.0 - rnd.random() * 0.35:
                        self.postaw((cx + x, cy + y, cz + z), rnd.choices([k for k, _ in wagi], [w for _, w in wagi])[0])
        return self

    def warstwy(self, warstwy, legenda, poczatek=(0, 0, 0)):
        """Rysunek: lista warstw od DOLU; warstwa = lista wierszy (z rosnie w dol wiersza, x w prawo). '.' i ' ' = nic."""
        x0, y0, z0 = poczatek
        for y, warstwa in enumerate(warstwy):
            wiersze = warstwa if isinstance(warstwa, list) else warstwa.strip('\n').split('\n')
            dl = {len(w) for w in wiersze}
            if len(dl) > 1:
                raise ValueError(f'warstwa {y}: wiersze roznej dlugosci {sorted(dl)} - przesuniety rysunek')
            for z, wiersz in enumerate(wiersze):
                for x, ch in enumerate(wiersz):
                    if ch in '. ':
                        continue
                    if ch not in legenda:
                        raise ValueError(f'warstwa {y} wiersz {z}: znak {ch!r} bez legendy')
                    self.postaw((x0 + x, y0 + y, z0 + z), legenda[ch])
        return self

    def zapisz(self, ns, nazwa):
        nazwa = nazwa.lower()          # id szablonu MC: tylko male litery (kwiat_A -> blad skladni 'Expected')
        d = os.path.join(GENERATED, ns, 'structures')
        os.makedirs(d, exist_ok=True)
        info = struktura.zapisz(self.bloki, os.path.join(d, nazwa + '.nbt'), DATA_VERSION)
        info['plik'] = os.path.join(d, nazwa + '.nbt')
        return info
