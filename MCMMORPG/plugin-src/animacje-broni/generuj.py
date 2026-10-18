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
# garda = Shift+PPM blokuje ciosy z przodu (redukcja 0..1 obrazen, kat, opoznienie w s, spowolnienie ruchu przy gardzie).
# wzor = kopia zestawu vanillowej broni (IRON_SPEAR: pchniecie trafiajace wszystko w linii, zasieg 2-4,5, szarza PPM).
# drzewce = halabarda: trzecie ogniwo lekkiego lancucha uderza trzonkiem.
# garda_oburacz = bron dwureczna: Shift+PPM w pozie kuszy (obie rece z przodu), a model w trakcie PPM (using_item) obraca
# bron poziomo w poprzek ciala - zaslona trzymana oburacz. Katy dobrane na zrzutach (qa/pozy.sh, warianty b i s).
GARDA_OBURACZ = {'tp_rot': [90, 0, -45], 'tp_trans': [0, 4, 0], 'fp_rot': [-30, 0, -45], 'fp_trans': [-3, 4, -3]}
# katy 3. osoby dobrane na zrzutach 2.10 (qa: warianty a-f, wybrany e - glowica wyraznie nad glowa)
UNIESIONY_MLOT = {'tp_rot': [-90, 0, 45], 'tp_trans': [0, 6, 0],
                 'fp_rot': [-15, 0, -12], 'fp_trans': [-2, 8, -4]}
G_MIECZ = {'redukcja': 0.4, 'kat': 90, 'opoznienie': 0.15, 'spowolnienie': 0.5}
# topor i bulawa (18.10 wlasciciel: "popraw blok dla toporow i bulaw"): zaslona trzonkiem, bez jelca -> slabiej niz miecz, bez parowania
G_TOPOR = {'redukcja': 0.35, 'kat': 90, 'opoznienie': 0.15, 'spowolnienie': 0.5}
PAROWANIE = {'okno': 6, 'riposta_ticki': 30, 'riposta_mnoznik': 1.5}
KRWAWIENIE = {'procent': 0.25, 'czas': 60, 'stosy': 1}
PANCERZ = {'procent': 0.30}
PCHNIECIE_MIECZA = {'gdy': ['wykonczenie'], 'zamach': 'stab', 'rodzaj': 'klute'}
TYPY = {
    'SWORD':           {'zamach': 'whack', 'czas': 6,  'rodzaj': 'ciete', 'atak_pchniecie': PCHNIECIE_MIECZA, 'garda': G_MIECZ, 'reakcja': 'krytyk', 'parowanie': PAROWANIE},
    'LONG_SWORD':      {'zamach': 'whack', 'czas': 7,  'rodzaj': 'ciete', 'atak_pchniecie': PCHNIECIE_MIECZA, 'garda': G_MIECZ, 'reakcja': 'krytyk', 'parowanie': PAROWANIE},
    'KATANA':          {'zamach': 'whack', 'czas': 5,  'rodzaj': 'ciete', 'atak_pchniecie': PCHNIECIE_MIECZA, 'garda': {'redukcja': 0.3, 'kat': 90, 'opoznienie': 0.1, 'spowolnienie': 0.6}, 'reakcja': 'krytyk', 'parowanie': PAROWANIE},
    'AXE':             {'zamach': 'whack', 'czas': 8,  'rodzaj': 'ciete', 'ladowanie': 0.6, 'reakcja': 'krwawienie', 'krwawienie': KRWAWIENIE,
                        'garda': G_TOPOR},
    'GREATSWORD':      {'zamach': 'whack', 'czas': 10, 'skala': 1.30, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8, 'zasieg': 3.3,
                        'poza': 'crossbow', 'garda_oburacz': True,
                        'garda': {'redukcja': 0.55, 'kat': 90, 'opoznienie': 0.3, 'spowolnienie': 0.35}, 'reakcja': 'krytyk', 'parowanie': PAROWANIE},
    'GREATAXE':        {'zamach': 'whack', 'czas': 10, 'skala': 1.30, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8, 'reakcja': 'krwawienie', 'krwawienie': KRWAWIENIE,
                        'poza': 'crossbow', 'garda_oburacz': True,
                        'garda': {'redukcja': 0.5, 'kat': 90, 'opoznienie': 0.3, 'spowolnienie': 0.35}},
    'HALBERD':         {'zamach': 'stab',  'czas': 8,  'skala': 1.30, 'rodzaj': 'klute', 'ciezka': True, 'wzor': 'IRON_SPEAR',
                        'zasieg': 4.5,
                        'poza': 'crossbow', 'garda_oburacz': True,
                        'garda': {'redukcja': 0.35, 'kat': 90, 'opoznienie': 0.2, 'spowolnienie': 0.5},
                        'drzewce': True},
    'LANCE':           {'zamach': 'stab',  'czas': 8,  'skala': 1.30, 'rodzaj': 'klute', 'wzor': 'IRON_SPEAR'},
    'SPEAR':           {'rodzaj': 'klute'},   # materialy *_SPEAR maja juz caly vanillowy zestaw wloczni
    'DAGGER':          {'zamach': 'stab',  'czas': 4,  'skala': 0.80, 'rodzaj': 'klute', 'zasieg': 2.6, 'reakcja': 'z_ukrycia',
                        'z_ukrycia': {'plecy': 2.0, 'ukrycie': 3.0}, 'krwawienie': {'procent': 0.15, 'czas': 60, 'stosy': 1}},
    'THRUSTING_SWORD': {'zamach': 'stab',  'czas': 5,  'rodzaj': 'klute', 'garda': G_MIECZ, 'reakcja': 'krytyk', 'parowanie': PAROWANIE},
    'WHIP':            {'zamach': 'whack', 'czas': 7,  'rodzaj': 'ciete'},
    'WAND':            {'zamach': 'stab',  'czas': 5,  'skala': 0.90},
    'STAFF':           {'zamach': 'stab',  'czas': 7,  'skala': 1.25},
    'GREATSTAFF':      {'zamach': 'whack', 'czas': 9,  'skala': 1.40, 'rodzaj': 'obuchowe', 'ladowanie': 0.6, 'zasieg': 3.3, 'reakcja': 'pancerz', 'pancerz': PANCERZ},
    'TOME':            {'zamach': 'stab',  'czas': 5},
    'TALISMAN':        {'zamach': 'stab',  'czas': 5},
    'FILAKTERIUM':     {'zamach': 'stab',  'czas': 5},
    'HAMMER':          {'zamach': 'whack', 'czas': 8,  'rodzaj': 'obuchowe', 'ladowanie': 0.6, 'reakcja': 'pancerz', 'pancerz': PANCERZ,
                        'garda': G_TOPOR},
    'GREATHAMMER':     {'zamach': 'whack', 'czas': 12, 'skala': 1.30, 'rodzaj': 'obuchowe', 'ciezka': True, 'ladowanie': 0.9,
                        'reakcja': 'pancerz', 'pancerz': PANCERZ, 'poza': 'trident', 'uderzenie': {'promien': 3.5, 'mnoznik': 1.6}},
    'SIERP':           {'zamach': 'whack', 'czas': 5,  'rodzaj': 'ciete', 'zasieg': 2.8, 'reakcja': 'krwawienie', 'krwawienie': {'procent': 0.25, 'czas': 60, 'stosy': 3}},
    'KOSA':            {'zamach': 'whack', 'czas': 10, 'skala': 1.40, 'rodzaj': 'ciete', 'ciezka': True, 'ladowanie': 0.8, 'zasieg': 3.5, 'reakcja': 'krwawienie', 'krwawienie': KRWAWIENIE},
    'SHIELD':          {'zamach': 'whack', 'czas': 7,  'rodzaj': 'obuchowe'},
    'GAUNTLET':        {'zamach': 'stab',  'czas': 4,  'rodzaj': 'obuchowe'},
}
# Kierunkowe ciosy i rytm sa cechami typu, a nie domyslnym zachowaniem kazdej broni.
MIECZE = ('SWORD', 'LONG_SWORD', 'KATANA', 'THRUSTING_SWORD')
for typ in MIECZE:
    TYPY[typ]['ciezkie'] = {'przod': 'wypad', 'bok': 'zamach_boczny', 'tyl': 'odepchniecie', 'bieg': 'z_rozbiegu', 'koszt': 12}
TYPY['GREATSWORD']['ciezkie'] = {'przod': 'znad_glowy', 'bok': 'zamach_boczny', 'tyl': 'odepchniecie', 'bieg': 'z_rozbiegu', 'koszt': 12}
for typ in ('AXE', 'GREATAXE'):
    TYPY[typ]['ciezkie'] = {'przod': 'znad_glowy', 'bok': 'zamach_boczny', 'bieg': 'z_rozbiegu', 'koszt': 12}
for typ in ('HAMMER', 'GREATHAMMER', 'GREATSTAFF'):
    TYPY[typ]['ciezkie'] = {'przod': 'znad_glowy', 'tyl': 'odepchniecie', 'bieg': 'z_rozbiegu', 'koszt': 12}
TYPY['DAGGER']['ciezkie'] = {'przod': 'wypad', 'koszt': 12}
TYPY['SIERP']['ciezkie'] = {'bok': 'zamach_boczny', 'koszt': 12}
TYPY['KOSA']['ciezkie'] = {'przod': 'znad_glowy', 'bok': 'zamach_boczny', 'koszt': 12}
# Kosa tnie lukiem kazdym lekkim ciosem (18.10: rozprysk MMOItems trafial wszystkich albo jednego - ukryty cooldown 3 s);
# 'on-attack' KOSA zdjety z MMOItems/item-types.yml, wiec luk jest jedynym zrodlem trafien obok.
TYPY['KOSA']['luk_lekki'] = {'kat': 180, 'mnoznik': 0.5}
# mlot dwureczny ma wlasny model uniesienia (poza trident) - postawy kierunkowe by go rozjechaly
TYPY['GREATHAMMER']['pozy_kierunkowe'] = False
for typ in (*MIECZE, 'GREATSWORD', 'AXE', 'GREATAXE', 'HAMMER', 'GREATHAMMER', 'GREATSTAFF', 'HALBERD',
            'DAGGER', 'SIERP', 'KOSA', 'SHIELD', 'GAUNTLET', 'WHIP'):
    TYPY[typ]['rytm'] = True
    TYPY[typ]['kroki'] = [0.08, 0.08, 0.12]
for typ in ('DAGGER',):
    TYPY[typ]['kroki'] = [0.18, 0.18, 0.24]
for typ in ('AXE', 'GREATAXE', 'KOSA', 'SIERP'):
    TYPY[typ]['kroki'] = [0.04, 0.04, 0.10]
for typ in ('HAMMER', 'GREATHAMMER', 'GREATSTAFF'):
    TYPY[typ]['kroki'] = [0.06, 0.06, 0.10]
TYPY['HALBERD']['kroki'] = [0.14, 0.14, -0.10]
# waga = mnoznik kosztu staminy (model jednostek): ciezka bron meczy bardziej takze w przeliczeniu na sekunde
# walki (lekka ~12,5 s, srednia 10 s, ciezka ~7 s przy zestawie 10). W zamian ciezka ma obrazenia na cios,
# przebicie pancerza, zachwianie i ciosy obszarowe. Lekka: sztylet, piesciak, bicz; ciezka: flaga ciezka
# oraz dwureczne kostur bojowy i kopia.
for typ, u in TYPY.items():
    u['waga'] = 0.8 if typ in ('DAGGER', 'GAUNTLET', 'WHIP') else 1.4 if u.get('ciezka') or typ in ('GREATSTAFF', 'LANCE') else 1.0
for typ in ('SHIELD', 'GAUNTLET', 'WHIP'):
    TYPY[typ]['kroki'] = [0, 0, 0]
    TYPY[typ]['prog_rytmu'] = 0.9
    TYPY[typ]['okno_rytmu'] = 8

MOCNY = {'prog': 6, 'szturchniecie_mnoznik': 0.5, 'szturchniecie_odnowienie': 20,
         'odnowienie': 20, 'zmeczenie': 60, 'bazowy_mnoznik': 0.8,
         'szerokosc_celu': 0.65, 'szerokosc_luku': 1.45,
         'krok_szturchniecia': 0.18, 'dystans_kroku': 1.5, 'krok_wypadu': 0.4, 'krok_biegu': 0.65,
         'krok_boczny': 0.32, 'odskok': 0.35, 'odrzut_szturchniecia': 0.45,
         'brak_staminy': 'Za mało staminy na mocny cios'}
# stamina = koszt wariantu w lekkich ciosach TEJ broni (model jednostek, sekcja STAMINA):
# placisz za to, co mocny daje ponad lekki - skumulowane obrazenia i efekt. Wypad x1,3 + przebicie linii,
# znad glowy x1,4 (mlot: fala), zamach boczny x1,0 ale wiele celow -> 2,5; odepchniecie slabe, placisz za
# przestrzen -> 2,0; z rozbiegu/z powietrza x1,5 + odrzut/zachwianie, a bieg/skok sam meczy -> 3,0.
# Wyglad wariantu (18.10 wlasciciel: "nie zmienia sie kierunek, wyglad"): kierunek przy wcisnieciu PPM = postawa, czyli
# poza ladowania (poza: animacja uzycia), zamach po puszczeniu (whack|stab) i mnoznik jego czasu (czas). Pozy dobrane zrzutami.
CIEZKIE_WARIANTY = {
    'wypad': {'mnoznik': 1.3, 'sila': 0.45, 'linia_mnoznik': 0.6, 'stamina': 2.5, 'poza': 'spear', 'zamach': 'stab', 'czas': 1.0},
    'znad_glowy': {'mnoznik': 1.4, 'stamina': 2.5, 'poza': 'trident', 'zamach': 'whack', 'czas': 1.6},
    'zamach_boczny': {'mnoznik': 1.0, 'luk_mnoznik': 0.7, 'kat': 120, 'kat_kosa': 180, 'kat_topor': 360, 'stamina': 2.5,
                      'poza': 'crossbow', 'zamach': 'whack', 'czas': 0.8},
    'odepchniecie': {'mnoznik': 1.0, 'sila': 1.1, 'stamina': 2.0, 'poza': 'block', 'zamach': 'stab', 'czas': 0.7},
    'z_rozbiegu': {'mnoznik': 1.5, 'sila': 1.1, 'stamina': 3.0, 'poza': 'spear', 'zamach': 'stab', 'czas': 1.0},
    'z_powietrza': {'mnoznik': 1.5, 'stamina': 3.0, 'poza': 'trident', 'zamach': 'whack', 'czas': 1.3},
}
# Model staminy w jednostkach L = lekki cios mieczem. zestaw = sekundy ciaglej walki na pelnym pasku (cel
# projektowy do wyboru wlasciciela z zestawy); pula = zestaw * ciosy_na_sekunde L; regeneracja L/s po
# oddech_ticki bez wysilku (nie w gardzie). Blok: od blok_min do blok_max L wg sily ciosu (blok_max przy
# blok_prog_hp maks. zdrowia), tarcza x0,5; parowanie oddaje parowanie_zwrot L. Zmeczony bije za
# zmeczenie_obrazenia. wspolczynnik broni = ciosy_na_sekunde / szybkosc ataku, przyciety do min/max.
STAMINA = {'wlaczona': 'true', 'zestaw': 10, 'zestawy': '[7, 10, 14]', 'ciosy_na_sekunde': 1.6,
           'regeneracja': 1.6, 'oddech_ticki': 20, 'zmeczenie_obrazenia': 0.6, 'szturchniecie': 1.0,
           'blok_min': 0.5, 'blok_max': 3.0, 'blok_prog_hp': 0.25, 'tarcza': 0.5, 'parowanie_zwrot': 1.0,
           'peknieta_garda_ticki': 40, 'wspolczynnik_min': 0.4, 'wspolczynnik_max': 2.5}
KOMBO = {'mnoznik_1': 1.2, 'mnoznik_2': 1.4}
RYTM = {'prog_rytmu': 0.7, 'okno_rytmu': 24, 'koszt_rytmu': 2, 'dlugosc': 3,
        'mnoznik': 1.4, 'luk_mnoznik': 0.5, 'krok_boczny_mnoznik': 0.35}
for typ, u in TYPY.items():
    if 'ciezkie' in u:
        u['ciezkie']['pelne'] = 24 if u.get('ciezka') else 12
        u['ciezkie']['spadanie'] = 'z_powietrza'
        u['poza_mocnego'] = 'trident'   # Skyrim: bron uniesiona nad glowe, po puszczeniu ciecie
TYPY['HALBERD']['ciezkie'] = {'przod': 'wypad', 'bok': 'zamach_boczny', 'tyl': 'odepchniecie',
                              'bieg': 'z_rozbiegu', 'spadanie': 'z_powietrza', 'koszt': 12, 'pelne': 24}
TYPY['HALBERD']['poza_mocnego'] = 'trident'
TYPY['GREATHAMMER']['poza_mocnego'] = 'trident'
TYPY['DAGGER']['ciezkie'].update({'bok': 'zamach_boczny', 'tyl': 'odepchniecie', 'bieg': 'z_rozbiegu'})
TYPY['SIERP']['ciezkie'].update({'przod': 'znad_glowy', 'tyl': 'odepchniecie'})
TYPY['KOSA']['ciezkie'].update({'tyl': 'odepchniecie', 'bieg': 'z_rozbiegu'})
# 18.10: kazdy kierunek ma swoj wariant (wlasciciel: "nie zmienia sie kierunek") - topory nie mialy S, buzdygany/kostur boku
for typ in ('AXE', 'GREATAXE'):
    TYPY[typ]['ciezkie']['tyl'] = 'odepchniecie'
for typ in ('HAMMER', 'GREATSTAFF'):
    TYPY[typ]['ciezkie']['bok'] = 'zamach_boczny'
# Bez klawisza kierunku = cios znad glowy, W = wypad do przodu (wczesniej oba dawaly to samo).
for typ, u in TYPY.items():
    c = u.get('ciezkie')
    if c:
        c['brak'] = 'znad_glowy'
        c['przod'] = 'wypad'
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
    garda_skal = {}   # skala -> materialy broni z garda oburacz (model w trakcie PPM)
    mlot_skal = {}    # skala -> mlot dwureczny uniesiony podczas using_item
    mocny_skal = {}   # skala -> odciagniecie broni przy ladowaniu
    pelne_skal = {}   # skala -> czas pelnego ladowania; progi use_duration jak u vanillowego luku
    for typ, u in TYPY.items():
        s = u.get('skala', 1.0) if 'ciezkie' in u else u.get('skala')
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
            if u.get('garda_oburacz'):
                garda_skal.setdefault(s, set()).add(m)
            if typ == 'GREATHAMMER':
                mlot_skal.setdefault(s, set()).add(m)
            elif 'ciezkie' in u:
                mocny_skal.setdefault(s, set()).add(m)
                pelne_skal[s] = max(pelne_skal.get(s, 0), u['ciezkie']['pelne'])

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

    for s, zb in sorted(garda_skal.items()):
        for m in sorted(zb):
            nazwa = m.lower(); sc = skala_nazwa(s); scg = sc + 'g'
            g = GARDA_OBURACZ
            disp = {}
            for k in KONTEKSTY:
                fp = k.startswith('firstperson')
                r = g['fp_rot'] if fp else g['tp_rot']
                if 'lefthand' in k:
                    r = [r[0], -r[1], -r[2]]
                sk = round((0.62 if fp else 0.92) * s, 4)
                disp[k] = {'rotation': r, 'translation': g['fp_trans'] if fp else g['tp_trans'], 'scale': [sk] * 3}
            p = os.path.join(baza, 'models', 'item', scg, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump({'parent': 'minecraft:item/' + nazwa, 'display': disp}, open(p, 'w'), indent=1)
            p = os.path.join(baza, 'items', scg, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump({'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                                 'on_true': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, scg, nazwa)},
                                 'on_false': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, sc, nazwa)}}},
                      open(p, 'w'), indent=1)
            n += 1

    for s, zb in sorted(mlot_skal.items()):
        for m in sorted(zb):
            nazwa = m.lower(); sc = skala_nazwa(s); scu = sc + 'u'
            disp = {}
            for k in KONTEKSTY:
                fp = k.startswith('firstperson')
                r = UNIESIONY_MLOT['fp_rot'] if fp else UNIESIONY_MLOT['tp_rot']
                if 'lefthand' in k:
                    r = [r[0], -r[1], -r[2]]
                sk = round((0.72 if fp else 0.92) * s, 4)
                disp[k] = {'rotation': r, 'translation': UNIESIONY_MLOT['fp_trans'] if fp else UNIESIONY_MLOT['tp_trans'], 'scale': [sk] * 3}
            p = os.path.join(baza, 'models', 'item', scu, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump({'parent': 'minecraft:item/' + nazwa, 'display': disp}, open(p, 'w'), indent=1)
            p = os.path.join(baza, 'items', scu, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump({'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                                 'on_true': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, scu, nazwa)},
                                 'on_false': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, sc, nazwa)}}},
                      open(p, 'w'), indent=1)
            n += 1

    for s, zb in sorted(mocny_skal.items()):
        for m in sorted(zb):
            nazwa = m.lower(); sc = skala_nazwa(s); scc = sc + 'c'
            for stopien in range(4):
                disp = {}
                for k in KONTEKSTY:
                    fp = k.startswith('firstperson')
                    rot = ([-8 - 10 * stopien, 8 + 9 * stopien, 5 + 5 * stopien] if fp
                           else [-12 - 10 * stopien, 8 + 8 * stopien, 10 + 7 * stopien])
                    if 'lefthand' in k:
                        rot = [rot[0], -rot[1], -rot[2]]
                    disp[k] = {'rotation': rot,
                               'translation': [-0.7 * stopien, 1.5 + stopien, -1.5 - 1.3 * stopien] if fp
                                              else [0, 1.5 + stopien, -1 - 1.1 * stopien],
                               'scale': [round(s * (0.8 if fp else 0.95), 4)] * 3}
                nazwa_stopnia = scc + (str(stopien) if stopien else '')
                p = os.path.join(baza, 'models', 'item', nazwa_stopnia, nazwa + '.json')
                os.makedirs(os.path.dirname(p), exist_ok=True)
                json.dump({'parent': 'minecraft:item/' + nazwa, 'display': disp}, open(p, 'w'), indent=1)
            p = os.path.join(baza, 'items', scc, nazwa + '.json')
            os.makedirs(os.path.dirname(p), exist_ok=True)
            stopnie = [{'threshold': round(pelne_skal[s] * i / 3 * 0.05, 4),
                        'model': {'type': 'minecraft:model', 'model': '%s:item/%s%d/%s' % (PRZESTRZEN, scc, i, nazwa)}}
                       for i in range(1, 4)]
            json.dump({'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                                 'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration',
                                             'scale': 0.05, 'entries': stopnie,
                                             'fallback': {'type': 'minecraft:model',
                                                          'model': '%s:item/%s/%s' % (PRZESTRZEN, scc, nazwa)}},
                                 'on_false': {'type': 'minecraft:model', 'model': '%s:item/%s/%s' % (PRZESTRZEN, sc, nazwa)}}},
                      open(p, 'w'), indent=1)
            n += 1

    linie = ['# WYGENEROWANE przez plugin-src/animacje-broni/generuj.py (klient %s) - nie edytowac recznie.' % wersja,
             '# typ MMOItems -> feeling broni; opis pol w TYPY w generuj.py. skala = model trzymania <przestrzen>:<skala>/<material>',
             'przestrzen-modeli: %s' % PRZESTRZEN, 'debug: false']
    for nazwa, dane in (('mocny', MOCNY), ('rytm', RYTM), ('kombo', KOMBO), ('stamina', STAMINA)):
        linie.append('%s: {%s}' % (nazwa, ', '.join('%s: %s' % kv for kv in dane.items())))
    linie.append('ciezkie_warianty:')
    for nazwa, dane in CIEZKIE_WARIANTY.items():
        linie.append('  %s: {%s}' % (nazwa, ', '.join('%s: %s' % kv for kv in dane.items())))
    linie.append('typy:')
    for typ, u in TYPY.items():
        linie.append('  %s:' % typ)
        for k in ('zamach', 'czas', 'rodzaj', 'reakcja', 'ladowanie', 'zasieg', 'wzor', 'poza', 'poza_mocnego', 'waga'):
            if k in u:
                linie.append('    %s: %s' % (k, u[k]))
        if u.get('ciezka'):
            linie.append('    ciezka: true')
        if u.get('rytm'):
            linie.append('    rytm: true')
            linie.append('    kroki: [%s]' % ', '.join(str(x) for x in u['kroki']))
            for k in ('prog_rytmu', 'okno_rytmu'):
                if k in u:
                    linie.append('    %s: %s' % (k, u[k]))
        if u.get('drzewce'):
            linie.append('    drzewce: true')
        if 'skala' in u:
            linie.append('    skala: %s' % skala_nazwa(u['skala']))
        elif 'ciezkie' in u:
            linie.append('    skala: s100')
        if u.get('pozy_kierunkowe') is False:
            linie.append('    pozy_kierunkowe: false')
        for k in ('garda', 'parowanie', 'uderzenie', 'z_ukrycia', 'krwawienie', 'pancerz', 'ciezkie', 'luk_lekki'):
            if k in u:
                linie.append('    %s: {%s}' % (k, ', '.join('%s: %s' % kv for kv in u[k].items())))
        if 'atak_pchniecie' in u:
            atak = u['atak_pchniecie']
            linie.append('    atak_pchniecie: {gdy: [%s], zamach: %s, rodzaj: %s}' %
                         (', '.join(atak['gdy']), atak['zamach'], atak['rodzaj']))
    linie.append('materialy-z-modelami:')
    linie += ['  - %s' % m for m in sorted(materialy)]
    os.makedirs(a.out, exist_ok=True)
    open(os.path.join(a.out, 'config.yml'), 'w').write('\n'.join(linie) + '\n')

    print('klient %s: %d modeli trzymania (%d materialow), %d typow' % (wersja, n, len(materialy), len(TYPY)))
    for m, pow_ in sorted(pominiete.items()):
        print('  pominiety %s: %s' % (m, pow_))


if __name__ == '__main__':
    sys.exit(main())
