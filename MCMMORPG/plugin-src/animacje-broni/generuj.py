#!/usr/bin/env python3
"""Zrodlo prawdy animacji broni: tabela TYPY -> config.yml pluginu + modele trzymania w packu Nexo.

    python3 generuj.py [--plugins /opt/docker/mc/data/world1/plugins] [--client client.jar] [--out <katalog>]

Pisze do <out>/: config.yml (dla plugins/AnimacjeBroni/) i pack/assets/bron/ (dla plugins/Nexo/pack/assets/).
Materialy broni zbiera z plikow MMOItems (item/<typ>.yml) danego serwera, bierze tylko te, ktorych vanillowy
model trzyma sie jak narzedzie (parent handheld / handheld_mace) - inne (ksiazka, lancuch, end_rod) sa pomijane.
"""
import argparse, json, math, os, re, sys, zipfile

# typ MMOItems -> (zamach whack|stab|none, czas w tickach, skala trzymania lub None)
# Czas: vanilla 6. Lzejsza bron szybciej (4-5), ciezka wolniej (10-12). Skala: rozmiar w dloni (1 = vanilla).
TYPY = {
    'SWORD':           ('whack', 6,  None),
    'LONG_SWORD':      ('whack', 7,  None),
    'KATANA':          ('whack', 5,  None),
    'AXE':             ('whack', 8,  None),
    'GREATSWORD':      ('whack', 10, 1.30),
    'GREATAXE':        ('whack', 10, 1.30),
    'HALBERD':         ('whack', 10, 1.30),
    'LANCE':           ('stab',  8,  1.30),
    'DAGGER':          ('stab',  4,  0.80),
    'THRUSTING_SWORD': ('stab',  5,  None),
    'WHIP':            ('whack', 7,  None),
    'WAND':            ('stab',  5,  0.90),
    'STAFF':           ('stab',  7,  1.25),
    'GREATSTAFF':      ('stab',  9,  1.40),
    'TOME':            ('stab',  5,  None),
    'TALISMAN':        ('stab',  5,  None),
    'FILAKTERIUM':     ('stab',  5,  None),
    'HAMMER':          ('whack', 8,  None),
    'GREATHAMMER':     ('whack', 12, 1.30),
    'SIERP':           ('whack', 5,  None),
    'KOSA':            ('whack', 10, 1.40),
    'SHIELD':          ('whack', 7,  None),
    'GAUNTLET':        ('stab',  4,  None),
}
# SPEAR, BOW, CROSSBOW, GREATBOW, MUSKET celowo poza tabela: wlocznie (IRON_SPEAR itd.) maja wlasny vanillowy
# zestaw (pchniecie + szarza), luki i kusze - animacje naciagania. Plugin ich nie rusza.

PRZESTRZEN = 'bron'
# punkt chwytu: lewy dolny rog tekstury 16x16 (rekojesc narzedzi i broni vanilli), wzgledem srodka modelu
CHWYT = (-6.5, -6.5, 0.0)
KONTEKSTY = ('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')


def skala_nazwa(s):
    return 's%03d' % round(s * 100)


def obrot_xyz(v, r):
    """JOML rotationXYZ: R = Rx * Ry * Rz, stopnie."""
    x, y, z = v
    a, b, c = (math.radians(t) for t in r)
    # Rz
    x, y = x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c)
    # Ry
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    # Rx
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    return (x, y, z)


def przeskaluj(display, s):
    """Nowe transformacje rak: skala x s, translacja poprawiona tak, by punkt chwytu zostal w miejscu.
    Poza = t + R*(skala*v); chcemy t' + R*(skala*s*v) == t + R*(skala*v)  ->  t' = t + R*(skala*(1-s)*v)."""
    wynik = {}
    for k in KONTEKSTY:
        d = display[k]
        sk = d.get('scale', [1, 1, 1])
        t = d.get('translation', [0, 0, 0])
        r = d.get('rotation', [0, 0, 0])
        v = tuple(CHWYT[i] * sk[i] * (1 - s) for i in range(3))
        if 'lefthand' in k:
            # gra dla lewej reki bierze translacje (-x, y, z) i obrot (x, -y, -z) z JSON-a
            dv = obrot_xyz(v, (r[0], -r[1], -r[2]))
            dv = (-dv[0], dv[1], dv[2])
        else:
            dv = obrot_xyz(v, r)
        wynik[k] = {
            'rotation': r,
            'translation': [round(t[i] + dv[i], 3) for i in range(3)],
            'scale': [round(sk[i] * s, 4) for i in range(3)],
        }
    return wynik


def materialy_typu(plugins, typ):
    f = os.path.join(plugins, 'MMOItems', 'item', typ.lower() + '.yml')
    if not os.path.isfile(f):
        return set()
    return {m.upper() for m in re.findall(r'^\s{4}material:\s*[\'"]?([A-Za-z_]+)', open(f, encoding='utf-8').read(), re.M)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--plugins', default='/opt/docker/mc/data/world1/plugins')
    ap.add_argument('--client', default='/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar')
    ap.add_argument('--out', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), 'wynik'))
    a = ap.parse_args()

    z = zipfile.ZipFile(a.client)
    wersja = json.loads(z.read('version.json'))['id']
    rodzice = {}

    def model(nazwa):
        return json.loads(z.read('assets/minecraft/models/item/%s.json' % nazwa))

    for p in ('handheld', 'handheld_mace'):
        rodzice[p] = model(p)['display']

    materialy = set()
    pominiete = {}
    modele_skal = {}  # skala -> set materialow
    for typ, (_, _, s) in TYPY.items():
        if s is None:
            continue
        for m in sorted(materialy_typu(a.plugins, typ)):
            nazwa = m.lower()
            try:
                par = model(nazwa).get('parent', '').replace('minecraft:', '').replace('item/', '')
            except KeyError:
                pominiete[m] = 'brak item/%s.json' % nazwa
                continue
            if par not in rodzice:
                pominiete[m] = 'parent %s' % par
                continue
            materialy.add(m)
            modele_skal.setdefault(s, set()).add((m, par))

    baza = os.path.join(a.out, 'pack', 'assets', PRZESTRZEN)
    n = 0
    for s, zb in sorted(modele_skal.items()):
        for m, par in sorted(zb):
            nazwa = m.lower()
            sc = skala_nazwa(s)
            mdl = {'parent': 'minecraft:item/' + nazwa, 'display': przeskaluj(rodzice[par], s)}
            p = os.path.join(baza, 'models', 'item', sc, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump(mdl, open(p, 'w'), indent=1)
            p = os.path.join(baza, 'items', sc, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump({'model': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, sc, nazwa)}}, open(p, 'w'), indent=1)
            n += 1

    linie = ['# WYGENEROWANE przez plugin-src/animacje-broni/generuj.py (klient %s) - nie edytowac recznie.' % wersja,
             '# typ MMOItems -> zamach (whack|stab|none), czas (ticki), skala = model trzymania <przestrzen>:<skala>/<material>',
             'przestrzen-modeli: %s' % PRZESTRZEN, 'typy:']
    for typ, (zam, czas, s) in TYPY.items():
        linie.append('  %s:' % typ)
        linie.append('    zamach: %s' % zam)
        linie.append('    czas: %d' % czas)
        if s is not None:
            linie.append('    skala: %s' % skala_nazwa(s))
    linie.append('materialy-z-modelami:')
    linie += ['  - %s' % m for m in sorted(materialy)]
    os.makedirs(a.out, exist_ok=True)
    open(os.path.join(a.out, 'config.yml'), 'w').write('\n'.join(linie) + '\n')

    print('klient %s: %d modeli trzymania (%d materialow), %d typow' % (wersja, n, len(materialy), len(TYPY)))
    for m, pow_ in sorted(pominiete.items()):
        print('  pominiety %s: %s' % (m, pow_))


if __name__ == '__main__':
    sys.exit(main())
