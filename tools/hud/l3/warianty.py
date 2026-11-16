#!/usr/bin/env python3
"""HUD v5 l3: kolumna (efekty, towarzysze, druzyna) rozbita na 12 layoutow-wariantow, po jednym na stan %hud_uklad%.

Po co (25.10): MythicHUD 1.3.4 co check-update tikow liczy KAZDY element kazdego aktywnego layoutu, takze ukryty
(ActiveElement.update: warunki i warstwy bez sprawdzenia isHidden). Stara l3 trzymala 12 polozen paneli jako
148 elementow z warunkiem %hud_uklad% - gracz placil za 12 kopii, widzial jedna. Tu kazdy wariant ma tylko
elementy mozliwe w swoim stanie, bez warunkow polozenia; HudDane (Uklady.java) podpina graczowi biezacy wariant.
Wyglad bez zmian: te same assety, pozycje i kolejnosc elementow w layoucie.

Wejscie: tools/hud/l3/zrodlo/ = l3 z jednym layoutem, jak stal na world1 do 26.10 (zrodlo prawdy; v3.py bywa za nim). Wyjscie: katalog z layouts/l3.yml,
hud_assets/hud/l3-kolumna.yml (assety bez warunkow polozenia) i liste kluczy (do sprawdzenia z HudDane).

  python3 warianty.py zrodlo/l3.yml zrodlo/l3-kolumna.yml <katalog-wyjscia>
"""
import re
import sys
from pathlib import Path

# Klucze wariantow = Uklady.WARIANTY w HudDane. Sufiks litery dobrany tak, by klucz trafil do kubelka 11-13
# ConcurrentHashMap (HudHolder.layouts, 16 i 32 kubelki) jak 'l3-kolumna-layout' (13): HudHolder.send() skleja
# layouty w kolejnosci iteracji tej mapy, wiec kolumna rysuje sie w tym samym miejscu stosu co dotad (po l1, przed l5).
KLUCZE = {
    'e0c0t0': 'l3-e0c0t0a', 'e0c0t1': 'l3-e0c0t1a', 'e0c0t2': 'l3-e0c0t2b',
    'e0c1t0': 'l3-e0c1t0a', 'e0c1t1': 'l3-e0c1t1b', 'e0c1t2': 'l3-e0c1t2c',
    'e1c0t0': 'l3-e1c0t0f', 'e1c0t1': 'l3-e1c0t1g', 'e1c0t2': 'l3-e1c0t2h',
    'e1c1t0': 'l3-e1c1t0g', 'e1c1t1': 'l3-e1c1t1h', 'e1c1t2': 'l3-e1c1t2i',
}
STARY_KLUCZ = 'l3-kolumna-layout'

# Warunki wyznaczone przez stan (HudDane Placeholdery: uklad = e<efekty niepuste>c<zadanieWidac>t<pet+slugi>);
# te same zrodla co %hud_efekty_liczba% i %hud_zadanie_widac%, wiec w wariancie sa zawsze spelnione -> usuwane.
UKLAD = re.compile(r'^placeholder\{ph=%hud_uklad%;val=(e[01]c[01]t[012])\} (true|false) hide$')
EFEKTY1 = re.compile(r'^placeholder\{ph=%hud_efekty_liczba%;v=1;o=>=\} (true|false) hide$')
ZADANIE = re.compile(r'^placeholder\{ph=%hud_zadanie_widac%;val=tak\} (true|false) hide$')
# Zostaja (nie wynikaja ze stanu jednoznacznie), ale wykluczaja warianty: efekty >= N>1, pet, slugi.
EFEKTYN = re.compile(r'^placeholder\{ph=%hud_efekty_liczba%;v=([2-9]);o=>=\} true hide$')
PET = re.compile(r'^placeholder\{ph=%chowance_aktywny_istnieje%;val=tak\} (true|false) hide$')
SLUGI = re.compile(r'^placeholder\{ph=%hud_slugi_liczba%;v=([1-9]);o=>=\} true hide$')


def wczytaj_assety(path):
    bloki, kolej, cur = {}, [], None
    for ln in Path(path).read_text(encoding='utf-8').split('\n'):
        m = re.match(r'^([A-Za-z0-9_.-]+):\s*$', ln)
        if m:
            cur = m.group(1); bloki[cur] = [ln]; kolej.append(cur)
        elif cur is None:
            bloki.setdefault(None, []).append(ln)
        else:
            bloki[cur].append(ln)
    return bloki, kolej


def wczytaj_layout(path):
    naglowek, elementy, cur, w_l3 = [], [], None, False
    for ln in Path(path).read_text(encoding='utf-8').split('\n'):
        if re.match(r'^[A-Za-z0-9_.-]+:\s*$', ln):
            if ln.rstrip(':') != STARY_KLUCZ:
                sys.exit(f'nieoczekiwany layout w l3.yml: {ln}')
            w_l3 = True; continue
        m = re.match(r'^    ([A-Za-z0-9_.-]+):\s*$', ln)
        if m:
            cur = {'nazwa': m.group(1), 'linie': [ln]}; elementy.append(cur)
        elif cur is None:
            if w_l3 and ln.strip() == 'elements:':
                continue
            naglowek.append(ln)
        else:
            cur['linie'].append(ln)
    for e in elementy:
        while e['linie'] and not e['linie'][-1].strip():
            e['linie'].pop()
        e['asset'] = next(re.match(r'^\s+asset:\s*(\S+)', l).group(1)
                          for l in e['linie'] if re.match(r'^\s+asset:', l))
    return naglowek, elementy


def warunki(linie):
    return [l.strip()[2:] for l in linie if re.match(r'^    - ', l)]


def stany_elementu(war):
    """Zbior stanow (e,c,t), w ktorych element moze byc widoczny, i lista warunkow do usuniecia."""
    stany = {(e, c, t) for e in (0, 1) for c in (0, 1) for t in (0, 1, 2)}
    usun = []
    pet_wym, slugi_wym = None, None   # None = obojetne, True = wymagane, False = zakazane
    for w in war:
        if m := UKLAD.match(w):
            st = m.group(1); s = (int(st[1]), int(st[3]), int(st[5]))
            stany = {s} & stany if m.group(2) == 'true' else stany - {s}
            usun.append(w)
        elif m := EFEKTY1.match(w):
            e = 1 if m.group(1) == 'true' else 0
            stany = {s for s in stany if s[0] == e}; usun.append(w)
        elif m := ZADANIE.match(w):
            c = 1 if m.group(1) == 'true' else 0
            stany = {s for s in stany if s[1] == c}; usun.append(w)
        elif EFEKTYN.match(w):
            stany = {s for s in stany if s[0] == 1}
        elif m := PET.match(w):
            pet_wym = m.group(1) == 'true'
        elif SLUGI.match(w):
            slugi_wym = True
    def mozliwe_t(t):
        for pet in (0, 1):
            for sl in (0, 1):
                if pet + sl != t: continue
                if pet_wym is not None and bool(pet) != pet_wym: continue
                if slugi_wym is not None and bool(sl) != slugi_wym: continue
                return True
        return False
    stany = {s for s in stany if mozliwe_t(s[2])}
    return stany, usun


def main(l3, assety, wyj):
    bloki, kolej = wczytaj_assety(assety)
    naglowek, elementy = wczytaj_layout(l3)
    if len({e['asset'] for e in elementy}) != len(elementy):
        sys.exit('asset uzyty przez kilka elementow - transformacja w miejscu niebezpieczna')
    plan = {}
    for e in elementy:
        if e['asset'] not in bloki:
            sys.exit(f'brak assetu {e["asset"]}')
        stany, usun = stany_elementu(warunki(bloki[e['asset']]))
        if not stany:
            sys.exit(f'element {e["nazwa"]} niewidoczny w zadnym stanie')
        plan[e['nazwa']] = stany
        if usun:
            bloki[e['asset']] = [l for l in bloki[e['asset']] if l.strip()[2:] not in usun or not re.match(r'^    - ', l)]

    wyj = Path(wyj)
    (wyj / 'layouts').mkdir(parents=True, exist_ok=True)
    (wyj / 'hud_assets/hud').mkdir(parents=True, exist_ok=True)
    out = ['# HUD v5 l3 w 12 wariantach (tools/hud/l3/warianty.py, 25.10): jeden layout na stan %hud_uklad%,',
           '# HudDane (Uklady.java) podpina graczowi tylko biezacy. Nie edytowac recznie - zrodlo: wdrozone l3 + warianty.py.']
    out += [l for l in naglowek if l.strip()]
    liczby = {}
    for st, klucz in KLUCZE.items():
        s = (int(st[1]), int(st[3]), int(st[5]))
        out.append(f'{klucz}:')
        out.append('  elements:')
        n = 0
        for e in elementy:
            if s in plan[e['nazwa']]:
                out += e['linie']; n += 1
        liczby[st] = n
    (wyj / 'layouts/l3.yml').write_text('\n'.join(out) + '\n', encoding='utf-8')
    tekst = []
    if None in bloki:
        tekst += bloki[None]
    for k in kolej:
        tekst += bloki[k]
    (wyj / 'hud_assets/hud/l3-kolumna.yml').write_text('\n'.join(tekst), encoding='utf-8')
    (wyj / 'klucze.txt').write_text('\n'.join(f'{st} {k}' for st, k in KLUCZE.items()) + '\n', encoding='utf-8')
    print('elementow w l3:', len(elementy), '| na wariant:', liczby)


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
