#!/usr/bin/env python3
"""Model ekonomii handlu world1 — ceny skupu NPC liczone z CZASU zdobycia.

Wejscie: receptury.json (tools/handel/receptury.py czyta stacje MMOItems).
Wyjscie: ceny.json + tabela na stdout.

ZALOZENIA (kazde z liczba i zrodlem):
  W  = 0.20 $/s  — rynkowa wartosc sekundy pracy (720 $/h). To jest skala waluty,
                   wybrana tak, zeby najtanszy surowiec (Swiezy zbior, 5 s/szt.)
                   mial cene skupu 0.40 $ — ponizej tego ceny robia sie nieczytelne.
  K  = 0.40      — skup NPC placi 40% wartosci. Sprzedaz graczowi na aukcji po
                   wartosci daje po podatku ~90%, czyli ~2.2x wiecej niz NPC.
  cykl wezla 25 s — tools/krzywe-profesji/README.md (dojscie + kanalowanie).
  wyrob: WARTOSC = suma(skladniki) * (1 + m) + (8 s + czas kolejki) * W
       m wg bramki poziomu receptury: L0-1 15%, L15 25%, L30 35%, L45 50%
       (premia za dostep — zeby gracz na L45 zarabial wiecej niz na L1).
       SKUP wyrobu = K * [suma(skladniki) * (1 + m/2) + interakcja]: NPC oddaje
       tylko POLOWE premii. Wersja ze stala premia (np. 180 s za L45) byla bledna:
       tonik bojowy z 1 woreczka jadu dawal w skupie 10x wartosci surowca, czyli
       kran pieniedzy zalezny od liczby klikniec, nie od pracy.
  Wartosc przedmiotu = MIN po drogach zdobycia (rynek placi za najtansza droge).
"""
import json, os, sys

W = 0.20
K = 0.40
K_GEAR = 0.40          # sprzet = ten sam wspolczynnik; przy 0.35 skup narzedzia bywal o grosze nizszy niz skup jego skladnikow
CYKL = 25.0
M = {0: 0.15, 1: 0.15, 15: 0.25, 30: 0.35, 45: 0.50}
INTERAKCJA = 8.0   # s na klikniecie receptury i odbior z kolejki

HERE = os.path.dirname(os.path.abspath(__file__))
data = json.load(open(os.path.join(HERE, 'receptury.json')))
items = data['items']

# ---------------------------------------------------------------- ZRODLA SUROWCOW
# czas_na_cykl [s], oczekiwany urobek na cykl {id: sztuk}. Produkty laczne dziela
# czas proporcjonalnie do "wagi rzadkosci" w = 1/sqrt(p) (rzadszy = cenniejszy,
# ale nie liniowo — inaczej perla bylaby warta 30 zwyklych polowow).
ZRODLA = {
    # --- wezly MythicMobs (WezlySkills.yml), cykl 25 s
    'wezel_dab':      (CYKL, {'PROF_OAK_TIMBER': 3}),
    'wezel_swierk':   (CYKL, {'PROF_SPRUCE_RESIN': 3}),
    'wezel_brzoza':   (CYKL, {'PROF_BIRCH_LATH': 3}),
    'wezel_ziola':    (CYKL, {'PROF_MEDICINAL_HERB': 0.70*1 + 0.25*2 + 0.05*2, 'PROF_MOONLEAF': 0.05}),
    'wezel_ksiezyc':  (CYKL, {'PROF_MOONLEAF': 0.80*1 + 0.20*2}),
    'wezel_grzyby':   (CYKL, {'PROF_REDCAP': 0.75*2 + 0.25*3}),
    'wezel_poletko':  (CYKL, {'PROF_FRESH_HARVEST': 5}),
    'wezel_druza':    (CYKL, {'PROF_ROUGH_CRYSTAL': 0.75*2, 'PROF_ROUGH_STONE': 0.25*2}),
    'wezel_glina':    (CYKL, {'PROF_CLAY_LUMP': 4}),
    'wezel_przyprawy':(CYKL, {'PROF_WILD_SPICE': 0.75*2 + 0.25*3}),
    # --- rybactwo MMOCore (professions/fishing.yml): wagi 60/20/8/3/2, 25 s na polow
    'polow':          (CYKL, {'PROF_RAW_CATCH': 60/93*1.5, 'PROF_CLAM': 20/93, 'PROF_RARE_FISH': 8/93,
                              'PROF_WATER_ESSENCE': 3/93, 'PROF_PEARL': 2/93}),
    # --- bloki Nexo (Nexo/items/pakiet3_bloki.yml). Czas = szukanie + kopanie;
    #     rzadsze bloki trzeba dluzej szukac (ZALOZENIE do potwierdzenia telemetria).
    'ruda_urobku':    (25, {'PROF_ORE_CHUNK': 1.5}),
    'zyla_srebra':    (40, {'PROF_SILVER_ORE': 1.5}),
    'kamieniolom':    (20, {'PROF_ROUGH_STONE': 2.0}),
    'zyla_klejnotow': (60, {'PROF_RAW_GEM': 1.0, 'PROF_ROUGH_STONE': 0.5}),
    'druza_arkany':   (150, {'PROF_ARCANE_CRYSTAL': 1.0}),
    # --- bestie (MythicMobs/droptables/Bestiariusz.yml), srednia z T1/T2; 30 s na zabicie
    'bestie':         (30, {'PROF_BEAST_SCRAP': 0.9, 'PROF_BEAST_BONE': 0.45, 'PROF_RAW_FLEECE': 0.35,
                            'PROF_VENOM_SAC': 0.25, 'PROF_MONSTER_TROPHY': 0.02}),
}

import math
wart = {}          # id -> (wartosc $, opis drogi)
npcv = {}          # id -> wartosc bazowa skupu (przed K)
def ustaw(i, v, opis, nv=None):
    if i not in wart or v < wart[i][0] - 1e-9:
        wart[i] = (v, opis)
        npcv[i] = v if nv is None else nv

# Przydzial czasu produktow lacznych
for src, (t, out) in ZRODLA.items():
    wagi = {}
    for i, n in out.items():
        p = n  # oczekiwana liczba na cykl ~ czestosc
        wagi[i] = n / math.sqrt(max(p, 1e-6))
    suma = sum(wagi.values())
    for i, n in out.items():
        czas_szt = t * (wagi[i] / suma) / n
        ustaw(i, czas_szt * W, f'{src}: {czas_szt:.1f} s/szt.')

# ---------------------------------------------------------------- RZEMIOSLO
PROF_STACJE = [r for r in data['recipes'] if r['station'].startswith('profession-') or r['station'] == 'chowance']
def m_of(r):
    lvl = r['lvl'] or 0
    return M[max(k for k in M if k <= lvl)]
def inter(r):
    return (INTERAKCJA + (r['time'] or 0)) * W

for _ in range(30):   # iteracja do punktu stalego (lancuchy do ~6 krokow)
    zmiana = False
    for r in PROF_STACJE:
        o = r['out']['id']
        if not o or not o.startswith('PROF_'):
            continue
        try:
            koszt = sum(wart[x['id']][0] * x['amount'] for x in r['ing'])
        except KeyError:
            continue
        v = (koszt * (1 + m_of(r)) + inter(r)) / (r['out']['amount'] or 1)
        przed = wart.get(o, (1e18,))[0]
        nkoszt = sum(npcv[x['id']] * x['amount'] for x in r['ing'])
        nv = (nkoszt * (1 + m_of(r) / 2) + inter(r)) / (r['out']['amount'] or 1)
        ustaw(o, v, f"{r['station'].replace('profession-','')} L{r['lvl']}: " +
              ' + '.join(f"{x['amount']}x{x['id'][5:]}" for x in r['ing']), nv)
        if wart[o][0] < przed - 1e-9:
            zmiana = True
    if not zmiana:
        break

# ---------------------------------------------------------------- CENY SKUPU
GEAR_TYPES = {'TOOL', 'GLOVES', 'BELT', 'RING', 'NECKLACE', 'CHARM', 'STAFF'}
POMIN = {'PROF_ESENCJA_WIEZI', 'PROF_TEST_PRZYSMAK', 'PROF_RAW_TIMBER', 'PROF_PRZYSMAK_MISTRZA'} | {i for i in items if i.startswith('PROF_KARMA_')}
# ^ karmy i przysmak = system Chowancow (darmowe zrodla w zdarzeniach/wiezy) — nie moje, poza skupem   # brak zrodla czasowego / martwe

def zaokr(x):
    # do 0.05 $ ponizej 10 $, do 0.5 $ ponizej 100 $, dalej do 1 $
    if x < 10:  return round(round(x / 0.05) * 0.05, 2)
    if x < 100: return round(round(x / 0.5) * 0.5, 1)
    return float(round(x))

ceny = {}
for i, (v, opis) in sorted(wart.items()):
    if i in POMIN or i not in items:
        continue
    typ = items[i]['type']
    k = K_GEAR if typ in GEAR_TYPES else K
    ceny[i] = {'id': i, 'typ': typ, 'nazwa': items[i]['name'].replace('&f', '').replace('§f', ''),
               'wartosc': round(v, 2), 'skup': max(0.05, zaokr(npcv[i] * k)), 'k': k, 'droga': opis}

json.dump(ceny, open(os.path.join(HERE, 'ceny.json'), 'w'), ensure_ascii=False, indent=1)

if '--tabela' in sys.argv or len(sys.argv) == 1:
    print(f"{'id':30} {'typ':10} {'wartosc':>9} {'skup':>7}  droga")
    for c in sorted(ceny.values(), key=lambda c: c['wartosc']):
        print(f"{c['id']:30} {c['typ']:10} {c['wartosc']:9.2f} {c['skup']:7.2f}  {c['droga']}")
    brak = [i for i in items if i.startswith('PROF_') and i not in ceny and i not in POMIN]
    print('\nBEZ CENY (brak drogi):', brak)
