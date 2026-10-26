#!/usr/bin/env python3
"""Bramka lore umiejętności klas rasowych (paczka P2, PLAN klasy-poprawki2 F7/F8, 25.10).

Sprawdza pliki MythicLib/skill/*.yml z katalogów akolita, mag, ninja, wojownik, specjalizacje:
  F8  linia kosztu = koszt skilla (parameters.mana) w poprawnej formie: „1 Dusza / 2 Dusze / 5 Dusz”,
      „30 Szału”, „20 Skupienia”, „25 Many”. Koszt zależny od rasy (EXTRA_SKILLE w generuj_klasy.py)
      opisany wprost, np. „Koszt: 1 Dusza (nieumarły: za darmo)”.
  F7  każda liczba w lore jest {parametrem} albo stoi na białej liście ZGODNE (liczba stała w mechanice,
      sprawdzona ręcznie: czas, zasięg, limit, stała wartość) - obca liczba = błąd.

Użycie:  python3 sprawdz_lore.py            # sprawdzenie, exit 1 przy błędzie
         python3 sprawdz_lore.py --napraw   # przepisuje linie kosztu (tylko F8), potem sprawdza
         python3 sprawdz_lore.py --lore     # wypisuje lore każdego skilla z wartościami na poz. 1/5/10

MMOCore (bajtkod 1.13.1, ClassSkill.calculateLore): {param} w lore = wartość parametru na bieżącym poziomie
(base + per-level*(poz-1)), {mana_name} = mana.name klasy (zostaje w mianowniku, bo MythicHUD go porównuje).
Bez pyyaml (brak na hoście) - mały parser podzbioru YAML poniżej (mapy/listy blokowe i flow, cudzysłowy).
"""
import os, re, sys

TU = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TU)
import generuj_klasy as G  # tylko dane (KLASY, EXTRA_SKILLE, SYGNATURY, SPEC_SKILLE); import nic nie zapisuje

PLIKI = [os.path.join(TU, a, 'MythicLib', 'skill', a + '.yml')
         for a in ('akolita', 'mag', 'ninja', 'wojownik', 'specjalizacje')]

# ---- odmiana -------------------------------------------------------------------------------------
# (lp mianownik, lm mianownik, lm dopełniacz); rzeczowniki niepoliczalne mają zawsze dopełniacz lp.
FORMY = {'Dusze': ('Dusza', 'Dusze', 'Dusz'), 'Szał': ('Szału',) * 3, 'Skupienie': ('Skupienia',) * 3,
         'Mana': ('Many',) * 3}
ZASOB = {'akolita': 'Dusze', 'wojownik': 'Szał', 'ninja': 'Skupienie', 'mag': 'Mana'}  # mana.name klas
RASA_DOP = {'nieumarly': 'nieumarły', 'ork': 'ork', 'elf': 'elf', 'czlowiek': 'człowiek'}


def odmien(n, zasob):
    """Liczba + rzeczownik w poprawnej formie: 1 Dusza, 2-4 Dusze (bez 12-14), reszta Dusz."""
    lp, lm, dop = FORMY[zasob]
    if n == 1:
        forma = lp
    elif n % 10 in (2, 3, 4) and n % 100 not in (12, 13, 14):
        forma = lm
    else:
        forma = dop
    return f'{liczba(n)} {forma}'


def liczba(x):
    return str(int(x)) if float(x) == int(x) else ('%.2f' % x).rstrip('0').rstrip('.').replace('.', ',')


# ---- mały parser YAML (podzbiór używany w plikach skilli) ----------------------------------------
def _skalar(s):
    s = s.strip()
    if not s:
        return None
    if s[0] in '"\'':
        q = s[0]
        if s[-1] != q:
            raise ValueError('niedomknięty cudzysłów: ' + s)
        t = s[1:-1]
        return t.replace("''", "'") if q == "'" else t.replace('\\"', '"')
    if re.fullmatch(r'-?\d+', s):
        return int(s)
    if re.fullmatch(r'-?\d*\.\d+', s):
        return float(s)
    if s in ('true', 'false'):
        return s == 'true'
    return s


def _flow(s, i=0):
    """Parsuje wartość flow od pozycji i; zwraca (wartość, nowa pozycja)."""
    while s[i] == ' ':
        i += 1
    if s[i] == '{':
        d, i = {}, i + 1
        while True:
            while s[i] in ' ,':
                i += 1
            if s[i] == '}':
                return d, i + 1
            j = s.index(':', i)
            k = _skalar(s[i:j])
            v, i = _flow(s, j + 1)
            d[k] = v
    if s[i] == '[':
        a, i = [], i + 1
        while True:
            while s[i] in ' ,':
                i += 1
            if s[i] == ']':
                return a, i + 1
            v, i = _flow(s, i)
            a.append(v)
    if s[i] in '"\'':
        q, j = s[i], i + 1
        while True:
            j = s.index(q, j)
            if q == "'" and j + 1 < len(s) and s[j + 1] == "'":
                j += 2
                continue
            if q == '"' and s[j - 1] == '\\':
                j += 1
                continue
            return _skalar(s[i:j + 1]), j + 1
    j = i
    while j < len(s) and s[j] not in ',}]':
        j += 1
    return _skalar(s[i:j]), j


def _bez_komentarza(l):
    q = None
    for i, c in enumerate(l):
        if q:
            if c == q:
                q = None
        elif c in '"\'':
            q = c
        elif c == '#' and (i == 0 or l[i - 1] == ' '):
            return l[:i].rstrip()
    return l.rstrip()


def wczytaj(path):
    """Zwraca (dane, numery_linii): numery_linii[(skill, 'lore', i)] = nr linii w pliku (dla --napraw)."""
    linie = [(n + 1, _bez_komentarza(l)) for n, l in enumerate(open(path, encoding='utf-8').read().split('\n'))]
    linie = [(n, l) for n, l in linie if l.strip()]
    nr = {}

    def blok(i, wciecie, sciezka):
        if i < len(linie) and linie[i][1].lstrip().startswith('- ') or (i < len(linie) and linie[i][1].strip() == '-'):
            w = len(linie[i][1]) - len(linie[i][1].lstrip())
            a = []
            while i < len(linie):
                n, l = linie[i]
                if len(l) - len(l.lstrip()) != w or not l.lstrip().startswith('-'):
                    break
                tekst = l.lstrip()[1:].strip()
                nr[sciezka + (len(a),)] = n
                a.append(_flow(tekst)[0] if tekst[:1] in '{["\'' else _skalar(tekst))
                i += 1
            return a, i
        d = {}
        while i < len(linie):
            n, l = linie[i]
            w = len(l) - len(l.lstrip())
            if w < wciecie:
                break
            if w > wciecie:
                raise ValueError(f'{path}:{n}: nieoczekiwane wcięcie')
            k, _, reszta = l.strip().partition(':')
            k = _skalar(k)
            reszta = reszta.strip()
            i += 1
            if reszta:
                if reszta[0] in '{[':
                    v = _flow(reszta)[0]
                    if isinstance(v, list):
                        for j in range(len(v)):
                            nr[sciezka + (k, j)] = n
                    d[k] = v
                else:
                    d[k] = _skalar(reszta)
            elif i < len(linie):
                w2 = len(linie[i][1]) - len(linie[i][1].lstrip())
                if w2 > w or (w2 == w and linie[i][1].lstrip().startswith('-')):
                    d[k], i = blok(i, w2, sciezka + (k,))
                else:
                    d[k] = None
        return d, i

    dane, _ = blok(0, 0, ())
    return dane, nr


# ---- dane klas -----------------------------------------------------------------------------------
def archetypy_skilli():
    """{SKILL: archetyp} z danych generatora (skille archetypu, sygnatury, specjalizacje)."""
    out = {}
    for arch, lista in G.EXTRA_SKILLE.items():
        for s, _, _ in lista:
            if s != G.SYGNATURA:
                out[s] = arch
    for (_, arch), s in G.SYGNATURY.items():
        out[s] = arch
    for _, arch, *_, specy in G.KLASY:
        for spec in specy:
            out[G.SPEC_SKILLE[spec[0]][0]] = arch
    return out


def koszt_rasy():
    """{SKILL: {rasa: koszt}} dla skilli z kosztem nadpisanym per rasa w EXTRA_SKILLE."""
    return {s: p['mana'] for lista in G.EXTRA_SKILLE.values() for s, _, p in lista if 'mana' in p}


def wartosc(param, poz):
    p = param.get('player', 0) if isinstance(param, dict) else param
    if isinstance(p, dict):
        return float(p.get('base', 0)) + float(p.get('per-level', 0)) * (poz - 1)
    return float(p or 0)


def tekst_kosztu(skill, params, arch, rasowe):
    zasob = ZASOB[arch]
    if skill in rasowe:
        koszty = rasowe[skill]
        glowny = max(set(koszty.values()), key=list(koszty.values()).count)
        inne = [f'{RASA_DOP[r]}: ' + ('za darmo' if k == 0 else odmien(k, zasob))
                for r, k in koszty.items() if k != glowny]
        return 'Koszt: ' + odmien(glowny, zasob) + (f' ({", ".join(inne)})' if inne else '')
    m = params.get('mana')
    if m is None:
        return None
    if wartosc(m, 1) != wartosc(m, 10):
        return None  # koszt rosnący z poziomem: zostaje {mana} (odmiana niemożliwa statycznie)
    return 'Koszt: ' + odmien(wartosc(m, 1), zasob)


# ---- F7: liczby w lore ---------------------------------------------------------------------------
# Liczby dozwolone bez {parametru}: (SKILL, fragment lore) -> źródło w mechanice. Fragment musi wystąpić
# w lore dokładnie; każdą pozycję sprawdzono w pliku skilla (linia w komentarzu = stała w mechanice).
# Skróty źródeł: A/M/N/W/S = {akolita,mag,ninja,wojownik,specjalizacje}/MythicLib/skill/*.yml,
# AS = akolita/MythicMobs/skills/Akolita_Slugi_skills.yml, AM = akolita/MythicMobs/mobs/Akolita_Slugi.yml,
# SS = specjalizacje/MythicMobs/skills/Specjalizacje_skills.yml, SM = specjalizacje/MythicMobs/mobs/Specjalizacje_mobs.yml.
ZGODNE = {
    'AKO_ZNIWO': {'do 4 sług': 'AS:5 @Children limit=4', 'do 2 wrogów': 'AS:42 limit=2', '5-7': 'AS:59-62 ako_dmg_teraz 5/6/7'},
    'AKO_STRAZ_RELIKWII': {'40 HP': 'AM:81 AKO_SLUGA_OBRONCA Health 40'},
    'AKO_OPIEKA': {'płaci 2 HP': 'A:70 sethealth caster.hp-2', 'osłonę 4 HP': 'A:101 shield 4'},
    'AKO_SIERP_KSIEZYCA': {'leczy 1 HP': 'AS:122 heal 1', 'ognik leczy 2 HP': 'AS:123 heal 2', 'limit 3 sług': 'A:175 children<3'},
    'AKO_PRZODEK_MLOTA': {'22 HP': 'AM:116', '4 obrażenia': 'AM:129 ako_dmg 4', 'limit 3 sług': 'A:215 children<3'},
    'AKO_KOSIARZ_CISZY': {'14 HP': 'AM:135', '3 obrażenia': 'AM:148 ako_dmg 3', 'limit 3 sług': 'A:282 children<3'},
    'MAG_BARIERA': {'10 obrażeń': 'M:23 shield 10 (D8: stałe)', '13 obrażeń': 'M:28 shield 13'},
    'MAG_MRUGNIECIE': {'człowiek 6': 'M:69 @Forward{f=6}'},
    'MAG_NIC_ARKANOW': {'do 3 wrogów': 'M:201-202 limit=3'},
    'MAG_SPLOT_DUSZY': {'1/3': 'M:296 ×0.333', 'odzyskuje 1 HP': 'M:297 heal 1'},
    'NIN_CIECIE': {'1 Znamię': 'N:32 variableadd 1', 'maks. 5': 'N:42 znamiona>5 → 5'},
    'NIN_KROK': {'do 10 bloków': 'N:58 distance 0to10'},
    'NIN_EGZEKUCJA': {'strzelcy do 8': 'N:128 distance 3to8'},
    'NIN_PETLA_GROBU': {'1 Znamię': 'N:227 NIN_Znamie_Dodaj'},
    'NIN_BELT_LISTU': {'1 Znamię': 'N:300 NIN_Znamie_Dodaj', '2 Znamiona': 'N:305 drugie NIN_Znamie_Dodaj'},
    'NIN_STRZALA_KORONY': {'1 Znamię': 'N:391 NIN_Znamie_Dodaj'},
    'WOJ_OKRZYK': {'w 8 blokach': 'W:61 r=8', '20 Szału': 'W:62 give 20'},
    'WOJ_GNIEW': {'osłona 4 HP': 'W:115 shield 4'},
    'WOJ_PRZELOT_GAJU': {'15 Szału': 'W:163 give 15'},
    'WOJ_PCHNIECIE_KORONY': {'15 Szału': 'W:206 give 15'},
    'WOJ_NATARCIE_WODZA': {'do 3 wrogów': 'W:278 licznik<3', '15 Szału': 'W:252 give 15'},
    'WOJ_MARSZ_KRYPTY': {'do 3 wrogów': 'W:351 licznik<3', 'do 6 HP': 'W:319 limit 6', 'leczenia 8 HP': 'W:320 limit 8', '15 Szału': 'W:328 give 15'},
    'SPEC_CZEMPION_WYZWANIE': {'osłona 4 HP': 'S:65 shield 4'},
    'SPEC_ELEMENTALISTA_KRAG': {'1/3': 'SS:286 SpecElementalistaSeal ×0.333'},
    'SPEC_SERAFIN_ZASLONA': {'Osłona 7 HP': 'S:752 / S:124 shield 7', '+3 HP': 'S:757 shield 10', 'relikwii: 5 HP': 'S:SpecSerafinGuardian -> SpecOslonaSlugi5 shield 5, delay 200'},
    'SPEC_HIEROFANT_CHORAGIEW': {'4 HP': 'S:141 shield 4', '3 HP': 'S:131 shield 3', '+2 HP': 'S:146 shield 6'},
    'SPEC_TANCERZ_CIECIE': {'15 Szału': 'S:238 give 15'},
    'SPEC_TKACZ_ZYWIOLOW_REFREN': {'do 3 wrogów': 'S:257-260 limit=3'},
    'SPEC_STRAZNIK_ZYWIOLOW_ZWIERCIADLO': {'8 HP': 'S:962 shield 8', '2/3': 'S:263 Reflect ×0.667 (kostur ×1)'},
    'SPEC_OPIEKUN_DUCHOW_SWIT': {'2 sojuszników': 'S:1023 limit=2', 'po 2 HP': 'S:288-293 heal 2 ×3 co 40 t.', 'leczenie +2': 'S:289 heal 4'},
    'SPEC_TYRAN_ROZKAZ': {'5 wrogów': 'S:1056 limit=5', '5 Szału do 25': 'S:306 give 5, S:1057 charges=5'},
    'SPEC_KRWIOBURCA_SZAL': {'10 Szału': 'S:327 give 10'},
    'SPEC_LOWCA_BESTII_WILK': {'wilka 18 HP': 'SM:5 SPEC_WILK_STEPU', 'limit 3': 'S:1114 children<3', 'wilk +4 HP': 'SM:83 _BRON 22'},
    'SPEC_TOTEMISTA_RYTM': {'osłona 4 HP': 'S:1203 / S:363 shield 4', '+2 HP': 'S:368 shield 6'},
    'SPEC_SZEPT_PRZODKOW_WEZWANIE': {'przodka 18 HP': 'SM:30 SPEC_PRZODEK', 'limit 3 sług': 'S:1260 children<3', 'przodek +4 HP': 'SM:108 _BRON 22'},
    'SPEC_ZNIWIARZ_CIECIE': {'2,25 HP': 'S:401 heal 2.25', '3,25 HP': 'S:402 heal 3.25'},
    'SPEC_DUSZOZERCA_WIEZ': {'1/3': 'S:462-471 ×0.333', '1,5 HP': 'S:459 heal 1.5', '0,5 HP': 'S:462 heal 0.5'},
    'SPEC_AWATAR_OTCHLANI_GLOD': {'leczenie 2 HP': 'S:487 heal 2', 'leczenie 3 HP': 'S:488 heal 3'},
    'SPEC_PASTERZ_UMARLYCH_GLOS': {'sługę 16 HP': 'SM:56 SPEC_SLUGA_KURHANU', 'limit 3': 'S:1495 children<3', 'sługa +4 HP': 'SM:134 _BRON 20'},
}
# Linie kosztu opisowe (koszt MMOCore = minimum, mechanika zużywa więcej): zamiast tekst_kosztu().
KOSZT_OPIS = {'WOJ_GNIEW': 'Koszt: cały Szał (min. 40)'}  # W:79 mana 40, W:102 resource-mana set 0
# Liczby zawsze dozwolone: czasy (s), zasięgi (bl., r), procenty, limity sztuk przy słowach „do N”, krotności.
OGOLNE = re.compile(r'''(?x)
    \d+(?:[.,]\d+)?\s*(?:s|sek|bl\.?|%|x|×)(?![a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ])   # 5 s, 10 bl., 40%
  | [rR]\d+                                                               # r6 (promień)
''')


def liczby_obce(skill, lore_tekst):
    t = re.sub(r'\{[a-z_]+\}', '', lore_tekst)
    t = re.sub(r'&[0-9a-fk-or]', '', t)
    t = re.sub(r'Koszt: .*', '', t)
    for frag in ZGODNE.get(skill, {}):
        t = t.replace(frag, '')
    t = OGOLNE.sub('', t)
    return re.findall(r'(?<![\w{])\d+(?:[.,]\d+)?', t)


def main():
    napraw = '--napraw' in sys.argv
    pokaz = '--lore' in sys.argv
    arch = archetypy_skilli()
    rasowe = koszt_rasy()
    bledy = []
    for path in PLIKI:
        dane, nr = wczytaj(path)
        zmiany = {}
        for skill, d in dane.items():
            if not isinstance(d, dict) or 'lore' not in d:
                continue
            lore = d.get('lore') or []
            params = d.get('parameters') or {}
            a = arch.get(skill)
            if a is None:
                bledy.append(f'{skill}: brak w danych generatora (EXTRA_SKILLE/SYGNATURY/SPEC_SKILLE)')
                continue
            if pokaz:
                print(f'== {skill} ({a})')
                for poz in (1, 5, 10):
                    wart = {k: liczba(wartosc(v, poz)) for k, v in params.items() if isinstance(v, dict) or isinstance(v, (int, float))}
                    print(f'   poz.{poz}: ' + ' | '.join(re.sub(r'\{([a-z_]+)\}', lambda m: wart.get(m.group(1), m.group(0)), l) for l in lore))
            oczek = KOSZT_OPIS.get(skill) or tekst_kosztu(skill, params, a, rasowe)
            koszt_idx = [i for i, l in enumerate(lore) if 'Koszt:' in str(l)]
            if oczek and not koszt_idx and wartosc(params.get('mana', 0), 1) > 0:
                bledy.append(f'{skill}: brak linii kosztu (oczekiwano „{oczek}”)')
            for i in koszt_idx:
                l = str(lore[i])
                kolor = re.match(r'^((?:&[0-9a-fk-or])*)', l).group(1)
                if oczek is None:
                    if wartosc(params.get('mana', 0), 1) == 0:
                        bledy.append(f'{skill}: linia kosztu przy koszcie 0: „{l}”')
                    continue
                if l != kolor + oczek:
                    if napraw:
                        zmiany[nr[(skill, 'lore', i)]] = (l, kolor + oczek)
                    else:
                        bledy.append(f'{skill}: koszt „{l}” zamiast „{kolor + oczek}”')
            obce = liczby_obce(skill, ' | '.join(str(x) for x in lore))
            if obce:
                bledy.append(f'{skill}: liczby bez parametru/ZGODNE: {", ".join(obce)}  [{" | ".join(map(str, lore))}]')
        if zmiany:
            linie = open(path, encoding='utf-8').read().split('\n')
            for n, (stare, nowe) in zmiany.items():
                for q in ('"', "'"):
                    if q + stare + q in linie[n - 1]:
                        linie[n - 1] = linie[n - 1].replace(q + stare + q, q + nowe.replace(q, q * 2 if q == "'" else '\\"') + q, 1)
                        break
                else:
                    raise SystemExit(f'{path}:{n}: nie znalazłem „{stare}” do podmiany')
            open(path, 'w', encoding='utf-8').write('\n'.join(linie))
            print(f'{os.path.relpath(path, TU)}: poprawiono {len(zmiany)} linii kosztu')
    if bledy:
        print('BŁĘDY LORE (%d):' % len(bledy))
        for b in bledy:
            print('  ' + b)
        return 1
    print('lore OK: koszty odmienione, liczby = parametry albo ZGODNE')
    return 0


if __name__ == '__main__':
    sys.exit(main())
