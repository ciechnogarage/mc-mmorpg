#!/usr/bin/env python3
"""Opisy kosztu Zaklec = koszt z configu (F3, paczka P4, 25.10).

Zrodlo prawdy: Zaklecia/config.yml (mana-za-tik, mana, czas-ladowania-t, oburacz.{efekt,mana}).
Przepisuje linie kosztu w:
  - MythicMobs/items/zaklecia/*.yml  (lore przedmiotu)
  - DeluxeMenus/gui_menus/ksiega_zaklec.yml  (lore Ksiegi)
Reguly (jak Wybor.koszt, Wybor.java:143, i Rzucanie.java:505-519):
  - koncentracja (strumien/tarcza): mana-za-tik placone co 10 ticków = 0,5 s -> "X many/s", X = 2 * mana-za-tik
  - pozostale: "X many" jednorazowo (klucz mana), dla ladowanych dopisek "Ladowanie: N tickow"
  - linia "Oburacz": mnozniki z oburacz.efekt / oburacz.mana
  - slowo "testowy" znika; brakujaca linia kosztu jest dopisywana.

Uzycie:
  generuj_koszty.py                  przepisz pliki w v2/src
  generuj_koszty.py --sprawdz        exit 1, gdy ktorykolwiek tekst != config (nic nie zapisuje)
  --root DIR                         katalog z Zaklecia/, MythicMobs/, DeluxeMenus/ (domyslnie v2/src;
                                     na serwerze: .../plugins, wtedy MythicMobs/items/zaklecia i gui_menus)
Nie zmienia kosztow ani bramki klas (D7): tylko opisuje to, co robi config.
"""
import argparse
import re
import sys
from pathlib import Path


def fnum(x):
    """4.0 -> '4', 0.5 -> '0,5' (polski zapis)."""
    x = round(float(x), 4)
    return str(int(x)) if x == int(x) else ('%g' % x).replace('.', ',')


def wczytaj_config(path):
    """Minimalny parser sekcji zaklecia: (brak PyYAML na hoscie). Zwraca {id: {klucz: wartosc}}."""
    zakl, cur, in_sec = {}, None, False
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = re.split(r'\s#', raw, maxsplit=1)[0].rstrip()
        if not line.strip():
            continue
        if re.match(r'^zaklecia:\s*$', line):
            in_sec = True
            continue
        if in_sec and re.match(r'^\S', line):
            break
        if not in_sec:
            continue
        m = re.match(r'^  ([a-z_0-9]+):\s*$', line)
        if m:
            cur = zakl.setdefault(m.group(1), {})
            continue
        m = re.match(r'^    ([a-z_-]+):\s*(.*)$', line)
        if m and cur is not None:
            k, v = m.group(1), m.group(2).strip()
            mo = re.match(r'^\{(.*)\}$', v)
            if mo:
                v = {a.split(':')[0].strip(): a.split(':')[1].strip() for a in mo.group(1).split(',')}
            cur[k] = v
    return zakl


def oczekiwane(z):
    """Zwraca dict: koszt (tekst 'X many' / 'X many/s'), strumien (bool), ladowanie_t, obur_efekt, obur_mana."""
    typ = z.get('typ')
    if 'mana-za-tik' in z:
        if typ not in ('koncentracja', 'tarcza'):
            raise SystemExit('config: mana-za-tik przy typie ' + str(typ))
        koszt, strumien = fnum(2 * float(z['mana-za-tik'])) + ' many/s', True
    elif 'mana' in z:
        koszt, strumien = fnum(z['mana']) + ' many', False
    else:
        raise SystemExit('config: zaklecie bez kosztu')
    ob = z.get('oburacz') or {}
    return dict(koszt=koszt, strumien=strumien,
                ladowanie=int(z['czas-ladowania-t']) if 'czas-ladowania-t' in z and typ == 'ladowany' else None,
                ob_efekt=fnum(ob['efekt']) if 'efekt' in ob else None,
                ob_mana=fnum(ob['mana']) if 'mana' in ob else None)


# stary zapis kosztu w tekscie: "1 many/s", "0,5 many / 0,5 s", "3 many"
RE_KOSZT = re.compile(r'(?:(?<=[&§][0-9a-fk-or])|(?<![&§\d,]))\d+(?:,\d+)? many(?: ?/ ?(?:0,5 )?s)?')
RE_MAN = re.compile(r'mana ×\d+(?:,\d+)?')
RE_EF = re.compile(r'(obrażenia|efekt|leczenie) ×\d+(?:,\d+)?')


def napraw_oburacz(linia, e):
    if 'Oburącz' not in linia:
        return linia
    if e['ob_mana']:
        linia = RE_MAN.sub('mana ×' + e['ob_mana'], linia)
    if e['ob_efekt']:
        linia = RE_EF.sub(lambda m: m.group(1) + ' ×' + e['ob_efekt'], linia)
    return linia


def napraw_przedmiot(tekst, e):
    out, mial = [], False
    for l in tekst.split('\n'):
        if 'many' in l and re.search(r'Koszt', l):
            mial = True
            l = re.sub(r'Koszt(?: testowy)?: <#6ab4ff>' + RE_KOSZT.pattern,
                       'Koszt: <#6ab4ff>' + e['koszt'], l)
        out.append(napraw_oburacz(l, e))
    if not mial:   # dopisz przed linia "Oburacz" (albo przed Options:)
        pre = ('Ładowanie: %d ticków · ' % e['ladowanie']) if e['ladowanie'] else ''
        nowa = "  - '<!i><gray>%sKoszt: <#6ab4ff>%s'" % (pre, e['koszt'])
        for i, l in enumerate(out):
            if 'Oburącz' in l:
                out.insert(i, nowa)
                break
        else:
            i = next(i for i, l in enumerate(out) if l.startswith('  Options:'))
            out.insert(i, nowa)
    return '\n'.join(out)


def napraw_ksiege(tekst, zakl, bledy):
    """Linie lore w bloku "'id':" - zamien koszt i mnozniki oburacz."""
    out, cur = [], None
    for l in tekst.split('\n'):
        m = re.match(r"^  '([a-z_0-9]+)':\s*$", l)
        if m:
            cur = m.group(1)
        elif re.match(r'^  \S', l):
            cur = None
        if cur in zakl and l.startswith('      - '):
            e = oczekiwane(zakl[cur])
            if RE_KOSZT.search(l):
                l = RE_KOSZT.sub(e['koszt'], l)
            l = napraw_oburacz(l, e)
        out.append(l)
    # kazde zaklecie z configu musi miec liczbe kosztu w lore Ksiegi
    for zid, z in zakl.items():
        blok = re.search(r"^  '%s':\n(.*?)(?=^  '|\Z)" % zid, '\n'.join(out), re.S | re.M)
        if blok and not RE_KOSZT.search(blok.group(1)):
            bledy.append('Ksiega: %s bez kosztu w lore' % zid)
    return '\n'.join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sprawdz', action='store_true')
    ap.add_argument('--root')
    a = ap.parse_args()
    tu = Path(__file__).resolve().parent
    root = Path(a.root) if a.root else tu / 'src'
    zakl = wczytaj_config(root / 'Zaklecia' / 'config.yml')
    if not zakl:
        raise SystemExit('config bez sekcji zaklecia')
    # id zaklecia -> plik przedmiotu (nazwa pliku = id)
    katalog = root / 'MythicMobs' / 'items' / 'zaklecia'
    zmiany, bledy = [], []
    for zid, z in zakl.items():
        p = katalog / (zid + '.yml')
        if not p.exists():
            bledy.append('brak przedmiotu ' + str(p))
            continue
        s = p.read_text(encoding='utf-8')
        n = napraw_przedmiot(s, oczekiwane(z))
        if n != s:
            zmiany.append((p, n))
    kp = root / 'DeluxeMenus' / 'gui_menus' / 'ksiega_zaklec.yml'
    s = kp.read_text(encoding='utf-8')
    n = napraw_ksiege(s, zakl, bledy)
    if n != s:
        zmiany.append((kp, n))
    for p, _ in zmiany:
        print(('ROZNICA ' if a.sprawdz else 'zapisano ') + str(p.relative_to(root)))
    for b in bledy:
        print('BLAD ' + b)
    if a.sprawdz:
        if zmiany or bledy:
            print('niezgodnosc: %d plikow != config' % len(zmiany))
            sys.exit(1)
        print('OK: opisy kosztu zgodne z configiem (%d zaklec)' % len(zakl))
        return
    for p, n in zmiany:
        p.write_text(n, encoding='utf-8')
    if bledy:
        sys.exit(1)
    print('gotowe: %d plikow' % len(zmiany))


if __name__ == '__main__':
    main()
