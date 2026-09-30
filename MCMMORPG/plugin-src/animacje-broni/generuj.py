#!/usr/bin/env python3
"""Zrodlo prawdy animacji broni: tabela TYPY -> config.yml pluginu + modele trzymania w packu Nexo.

    python3 generuj.py [--plugins /opt/docker/mc/data/world1/plugins] [--client client.jar] [--out <katalog>]

Pisze do <out>/: config.yml (dla plugins/AnimacjeBroni/) i pack/assets/bron/ (dla plugins/Nexo/pack/assets/).
Materialy broni zbiera z plikow MMOItems (item/<typ>.yml) danego serwera, bierze tylko te, ktorych vanillowy
model trzyma sie jak narzedzie (parent handheld / handheld_mace) - inne (ksiazka, lancuch, end_rod) sa pomijane.
"""
import argparse, json, math, os, re, sys, zipfile

# FEELING BRONI: typ MMOItems -> ustawienia. Rodzaj obrazen: ciete (ostrze tnie), klute (grot/szpic kluje),
# obuchowe (tepa bron miazdzy). Waga: czas zamachu (vanilla 6 tickow), ladowanie = minimalne naladowanie ciosu (ciezka
# bron nie da sie "klikac"), ciezka = nizszy, glebszy dzwiek trafienia. zasieg = max zasieg ciosu (vanilla 3 bloki).
# garda = PPM blokuje ciosy z przodu (redukcja 0..1 obrazen, kat, opoznienie w s, spowolnienie ruchu przy gardzie).
# wzor = kopia zestawu vanillowej broni (IRON_SPEAR: pchniecie trafiajace wszystko w linii, zasieg 2-4,5, szarza PPM).
G_MIECZ = {'redukcja': 0.4, 'kat': 90, 'opoznienie': 0.15, 'spowolnienie': 0.5}
TYPY = {
    'SWORD':           {'zamach': 'whack', 'czas': 6,  'rodzaj': 'ciete', 'garda': G_MIECZ},
    'LONG_SWORD':      {'zamach': 'whack', 'czas': 7,  'rodzaj': 'ciete', 'garda': G_MIECZ},
    'KATANA':          {'zamach': 'whack', 'czas': 5,  'rodzaj': 'ciete', 'garda': {'redukcja': 0.3, 'kat': 90, 'opoznienie': 0.1, 'spowolnienie': 0.6}},
    'AXE':             {'zamach': 'whack', 'czas': 8,  'rodzaj': 'ciete', 'ladowanie': 0.6},
    'GREATSWORD':      {'zamach': 'whack', 'czas': 10, 'skala': 1.30, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8, 'zasieg': 3.3,
                        'garda': {'redukcja': 0.55, 'kat': 90, 'opoznienie': 0.3, 'spowolnienie': 0.35}},
    'GREATAXE':        {'zamach': 'whack', 'czas': 10, 'skala': 1.30, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8},
    'HALBERD':         {'zamach': 'stab',  'czas': 8,  'skala': 1.30, 'rodzaj': 'klute', 'ciezka': True, 'wzor': 'IRON_SPEAR'},
    'LANCE':           {'zamach': 'stab',  'czas': 8,  'skala': 1.30, 'rodzaj': 'klute', 'wzor': 'IRON_SPEAR'},
    'SPEAR':           {'rodzaj': 'klute'},   # materialy *_SPEAR maja juz caly vanillowy zestaw wloczni
    'DAGGER':          {'zamach': 'stab',  'czas': 4,  'skala': 0.80, 'rodzaj': 'klute', 'zasieg': 2.6},
    'THRUSTING_SWORD': {'zamach': 'stab',  'czas': 5,  'rodzaj': 'klute', 'garda': G_MIECZ},
    'WHIP':            {'zamach': 'whack', 'czas': 7,  'rodzaj': 'ciete'},
    'WAND':            {'zamach': 'stab',  'czas': 5,  'skala': 0.90},
    'STAFF':           {'zamach': 'stab',  'czas': 7,  'skala': 1.25},
    'GREATSTAFF':      {'zamach': 'whack', 'czas': 9,  'skala': 1.40, 'rodzaj': 'obuchowe', 'ladowanie': 0.6, 'zasieg': 3.3},
    'TOME':            {'zamach': 'stab',  'czas': 5},
    'TALISMAN':        {'zamach': 'stab',  'czas': 5},
    'FILAKTERIUM':     {'zamach': 'stab',  'czas': 5},
    'HAMMER':          {'zamach': 'whack', 'czas': 8,  'rodzaj': 'obuchowe', 'ladowanie': 0.6},
    'GREATHAMMER':     {'zamach': 'whack', 'czas': 12, 'skala': 1.30, 'rodzaj': 'obuchowe', 'ciezka': True, 'ladowanie': 0.9},
    'SIERP':           {'zamach': 'whack', 'czas': 5,  'rodzaj': 'ciete', 'zasieg': 2.8},
    'KOSA':            {'zamach': 'whack', 'czas': 10, 'skala': 1.40, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8, 'zasieg': 3.5},
    'SHIELD':          {'zamach': 'whack', 'czas': 7,  'rodzaj': 'obuchowe'},
    'GAUNTLET':        {'zamach': 'stab',  'czas': 4,  'rodzaj': 'obuchowe'},
}
# BOW, CROSSBOW, GREATBOW, MUSKET celowo poza tabela (vanillowe naciaganie). Plugin ich nie rusza.

PRZESTRZEN = 'bron'
# punkt chwytu: lewy dolny rog tekstury 16x16 (rekojesc narzedzi i broni vanilli), wzgledem srodka modelu
CHWYT = (-6.5, -6.5, 0.0)
# w 1. osobie bron jest blisko kamery: zmiana rozmiaru o polowe mniejsza (x1,3 -> x1,15), inaczej zaslania ekran
TLUMIENIE_1OS = 0.5
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
    s3 = s
    for k in KONTEKSTY:
        s = 1 + (s3 - 1) * TLUMIENIE_1OS if k.startswith('firstperson') else s3
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
    for typ, u in TYPY.items():
        s = u.get('skala')
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
             '# typ MMOItems -> feeling broni; opis pol w TYPY w generuj.py. skala = model trzymania <przestrzen>:<skala>/<material>',
             'przestrzen-modeli: %s' % PRZESTRZEN, 'typy:']
    for typ, u in TYPY.items():
        linie.append('  %s:' % typ)
        for k in ('zamach', 'czas', 'rodzaj', 'ladowanie', 'zasieg', 'wzor'):
            if k in u:
                linie.append('    %s: %s' % (k, u[k]))
        if u.get('ciezka'):
            linie.append('    ciezka: true')
        if 'skala' in u:
            linie.append('    skala: %s' % skala_nazwa(u['skala']))
        if 'garda' in u:
            linie.append('    garda: {%s}' % ', '.join('%s: %s' % kv for kv in u['garda'].items()))
    linie.append('materialy-z-modelami:')
    linie += ['  - %s' % m for m in sorted(materialy)]
    os.makedirs(a.out, exist_ok=True)
    open(os.path.join(a.out, 'config.yml'), 'w').write('\n'.join(linie) + '\n')

    print('klient %s: %d modeli trzymania (%d materialow), %d typow' % (wersja, n, len(materialy), len(TYPY)))
    for m, pow_ in sorted(pominiete.items()):
        print('  pominiety %s: %s' % (m, pow_))


if __name__ == '__main__':
    sys.exit(main())
