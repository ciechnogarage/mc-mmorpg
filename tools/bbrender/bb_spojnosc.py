"""Offline bramka spójności .bbmodel. Użycie: python3 bb_spojnosc.py model.bbmodel [--tol 0.1]."""
import argparse
import json
import re
from collections import defaultdict
from pathlib import Path

import numpy as np
from render_bbmodel import zbierz, rot_matrix, rot_of, ukryte_przez_animacje

BAZA = Path('/opt/docker/mc/data/world1/output/lochy/bagna')
KONTRAKT = BAZA / '_zespol/KONTRAKT_STANOW.md'


def odleglosc(a, b):
    return sum(max(0, a[i] - b[i + 3], b[i] - a[i + 3]) ** 2 for i in range(3)) ** 0.5


def komponenty(boxy, tol=0.1):
    """Graf styku AABB; rekord = (element, kość, (min xyz, max xyz))."""
    rodzice = list(range(len(boxy)))
    def root(i):
        while rodzice[i] != i:
            rodzice[i] = rodzice[rodzice[i]]
            i = rodzice[i]
        return i
    for i in range(len(boxy)):
        for j in range(i + 1, len(boxy)):
            if odleglosc(boxy[i][2], boxy[j][2]) <= tol:
                rodzice[root(i)] = root(j)
    wynik = defaultdict(list)
    for i, box in enumerate(boxy):
        wynik[root(i)].append(box)
    return sorted(wynik.values(), key=lambda x: (-len(x), x[0][0]))


def drzewo(model):
    elementy = {e['uuid']: e for e in model.get('elements', [])}
    kosci, geo, przypisanie, dzieci = {}, defaultdict(list), {}, defaultdict(list)
    def walk(nodes, parent=None):
        for node in nodes:
            if isinstance(node, dict):
                uid = node['uuid']
                kosci[uid] = node['name']
                if parent:
                    dzieci[parent].append(uid)
                walk(node.get('children', []), uid)
            elif node in elementy:
                geo[parent].append(node)
                przypisanie[node] = parent
    walk(model.get('outliner', []))
    return kosci, geo, przypisanie, dzieci


def wartosc_kanalu(animator, kanal, czas):
    default = np.ones(3) if kanal == 'scale' else np.zeros(3)
    if not animator:
        return default
    klatki = sorted((k for k in animator.get('keyframes', []) if k.get('channel') == kanal),
                    key=lambda k: float(k.get('time', 0)))
    if not klatki:
        return default
    def vec(k):
        p = (k.get('data_points') or [{}])[0]
        return np.array([float(p.get(a, default[i])) for i, a in enumerate('xyz')])
    if czas <= float(klatki[0].get('time', 0)):
        return vec(klatki[0])
    for lewa, prawa in zip(klatki, klatki[1:]):
        koniec = float(prawa.get('time', 0))
        if czas <= koniec:
            start = float(lewa.get('time', 0))
            udzial = (czas - start) / max(koniec - start, 1e-9)
            return vec(lewa) * (1 - udzial) + vec(prawa) * udzial
    return vec(klatki[-1])


def boxy_modelu(model, animacja=None, czas=0):
    kosci, _, przypisanie, _ = drzewo(model)
    wynik = []
    if animacja is None:
        elementy = zbierz(model)
    else:
        source = {e['uuid']: e for e in model.get('elements', [])}
        ukryte = ukryte_przez_animacje(model)
        elementy = []
        def wezel(nodes, chain=()):
            for n in nodes:
                if isinstance(n, dict):
                    if n['uuid'] in ukryte or n['name'].lower().startswith(('hitbox', 'shadow', 'ob_', 'fx_')):
                        continue
                    o = np.array(n.get('origin', [0, 0, 0]), float)
                    a = animacja.get('animators', {}).get(n['uuid'])
                    rot = rot_matrix(np.array(rot_of(n), float) + wartosc_kanalu(a, 'rotation', czas))
                    pos = wartosc_kanalu(a, 'position', czas)
                    scale = wartosc_kanalu(a, 'scale', czas)
                    def transform(p, o=o, rot=rot, pos=pos, scale=scale):
                        return ((p - o) * scale) @ rot.T + o + pos
                    wezel(n.get('children', []), (transform,) + chain)
                elif n in source:
                    e = source[n]
                    o = np.array(e.get('origin', [0, 0, 0]), float)
                    rot = rot_matrix(rot_of(e))
                    def tf(p, o=o, rot=rot, chain=chain):
                        p = (p - o) @ rot.T + o
                        for transform in chain:
                            p = transform(p)
                        return p
                    elementy.append((e, tf))
        wezel(model.get('outliner', []))
    for e, tf in elementy:
        uid = przypisanie.get(e['uuid'])
        if uid not in kosci or kosci[uid].lower().startswith(('hitbox', 'shadow', 'ob_', 'fx_')) or 'from' not in e or 'to' not in e:
            continue
        a = np.array(e['from'], float) - float(e.get('inflate', 0))
        b = np.array(e['to'], float) + float(e.get('inflate', 0))
        rogi = np.array([tf(np.array([x, y, z])) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])])
        lo, hi = rogi.min(0), rogi.max(0)
        wynik.append((e.get('name', e['uuid']), kosci[uid], tuple(lo) + tuple(hi)))
    return wynik


def kontrola_ruchu(model, emitery=()):
    kosci, geo, _, dzieci = drzewo(model)
    emitery = set(emitery)
    def ile_geometrii(uid):
        return len(geo[uid]) + sum(ile_geometrii(ch) for ch in dzieci[uid])
    animowane, nieznane = set(), set()
    for anim in model.get('animations', []):
        for uid, animator in anim.get('animators', {}).items():
            if animator.get('type') == 'effect':
                continue
            if uid not in kosci:
                nieznane.add(animator.get('name', uid))
            elif any(k.get('channel') in ('position', 'rotation', 'scale') for k in animator.get('keyframes', [])):
                animowane.add(uid)
    return {
        'animowane_bez_geometrii': sorted(kosci[u] for u in animowane if not ile_geometrii(u) and kosci[u] not in emitery and not kosci[u].startswith('fx_')),
        'geometria_bez_animacji': sorted(kosci[u] for u in kosci if geo[u] and u not in animowane
                                        and not kosci[u].lower().startswith(('hitbox', 'shadow', 'ob_', 'fx_'))),
        'nieznane_animatory': sorted(nieznane),
    }


def wymagania_kontraktu(nazwa, tekst):
    match = re.search(r'^## ' + re.escape(nazwa) + r'\b(.*?)(?=^## |\Z)', tekst, re.M | re.S)
    if not match:
        return None
    sekcja = match.group(1)
    animacje = re.findall(r'^\| `([^`]+)` \|', sekcja, re.M)
    wiersz = re.search(r'^Kości specjalne: (.*)$', sekcja, re.M)
    specjalne = re.findall(r'`([^`]+)`', wiersz.group(1)) if wiersz else []
    return animacje, specjalne


def audyt(model, nazwa, tol=0.1, kontrakt=None):
    boxy = boxy_modelu(model)
    kosci, geo, _, _ = drzewo(model)
    # Iskry Ognika orbitują poza bryłą i są celowym wyjątkiem od bramki styku.
    wyjatek_iskier = nazwa == 'ognik'
    istotne = lambda bs: [b for b in bs if not (wyjatek_iskier and b[1] == 'iskry')]
    calosc = komponenty(istotne(boxy), tol)
    iskry = komponenty([b for b in boxy if b[1] == 'iskry'], tol) if wyjatek_iskier else []
    na_kosc = {}
    for k in {b[1] for b in boxy}:
        pod = komponenty([b for b in boxy if b[1] == k], tol)
        if len(pod) > 1:
            na_kosc[k] = pod
    wymagane = wymagania_kontraktu(nazwa, kontrakt) if kontrakt else None
    brak_animacji, brak_kosci, obce_animacje, obce_kosci = [], [], [], []
    if wymagane:
        animacje, specjalne = wymagane
        aktualne = {a['name'] for a in model.get('animations', [])}
        brak_animacji = sorted(set(animacje) - aktualne)
        obce_animacje = sorted(aktualne - set(animacje))
        brak_kosci = sorted(set(specjalne) - set(kosci.values()))
        obce_kosci = sorted(n for n in set(kosci.values())
                            if n.startswith(('swiatlo_', 'p_', 'fx_')) and n not in specjalne)
    emitery = {n for n in kosci.values() if n.startswith('p_')}
    puste_swiatlo = sorted(n for uid, n in kosci.items() if n.startswith('swiatlo_') and not geo[uid])
    pozy_odlaczone = {}
    for anim in model.get('animations', []):
        dl = float(anim.get('length', 0))
        czasy = [round(i * .1, 2) for i in range(int(dl / .1) + 1)]
        if not czasy or czasy[-1] < dl - 1e-6:
            czasy.append(dl)
        worst = None
        for t in czasy:
            klatka = boxy_modelu(model, anim, t)
            ile = len(komponenty(istotne(klatka), tol))
            problemy = [k for k in {b[1] for b in klatka}
                        if len(komponenty([b for b in klatka if b[1] == k], tol)) > 1]
            if ile > 1 or problemy:
                wynik = {'t': t, 'calosc': ile, 'kosci': sorted(problemy)}
                if worst is None or (ile, len(problemy)) > (worst['calosc'], len(worst['kosci'])):
                    worst = wynik
        if worst:
            pozy_odlaczone[anim['name']] = worst
    return dict(boxy=len(boxy), calosc=calosc, na_kosc=na_kosc, iskry=iskry,
                ruch=kontrola_ruchu(model, emitery), brak_animacji=brak_animacji,
                obce_animacje=obce_animacje, brak_kosci=brak_kosci, obce_kosci=obce_kosci,
                puste_swiatlo=puste_swiatlo,
                zla_nazwa=model.get('name') not in (None, f'bagna_{nazwa}'),
                brak_kontraktu=wymagane is None, pozy_odlaczone=pozy_odlaczone)


def blad(w):
    return (len(w['calosc']) > 1 or any(p['calosc'] > 1 for p in w['pozy_odlaczone'].values()) or
            bool(w['ruch']['animowane_bez_geometrii']) or
            bool(w['ruch']['nieznane_animatory']) or
            bool(w['brak_animacji']) or bool(w['obce_animacje']) or bool(w['brak_kosci']) or
            bool(w['obce_kosci']) or bool(w['puste_swiatlo']) or w['zla_nazwa'] or w['brak_kontraktu'])


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    parser.add_argument('--tol', type=float, default=0.1)
    args = parser.parse_args(argv)
    model = json.loads(args.model.read_text())
    nazwa = args.model.stem.removeprefix('bagna_')
    w = audyt(model, nazwa, args.tol, KONTRAKT.read_text())
    print(f'{nazwa}: {w["boxy"]} kostek, całość {len(w["calosc"])} komponentów, kości z odłączeniem {len(w["na_kosc"])}')
    for k, gr in sorted(w['na_kosc'].items()):
        print(f'  INFO {k}: {len(gr)} komponentów wewnątrz kości; ' + ' | '.join(', '.join(b[0] for b in grupa[:4]) for grupa in gr[:5]))
    if w['iskry']:
        print(f'  INFO iskry Ognika: {len(w["iskry"])} orbitujących komponentów (wyjątek)')
    for a, p in sorted(w['pozy_odlaczone'].items()):
        print(f'  {"BŁĄD" if p["calosc"] > 1 else "INFO"} {a} t={p["t"]}: całość {p["calosc"]}, kości {", ".join(p["kosci"]) or "—"}')
    for key in ('animowane_bez_geometrii', 'nieznane_animatory', 'geometria_bez_animacji'):
        print(f'  {"INFO " if key == "geometria_bez_animacji" else ""}{key}: {", ".join(w["ruch"][key]) or "—"}')
    for key in ('brak_animacji', 'obce_animacje', 'brak_kosci', 'obce_kosci'):
        print(f'  {key}: {", ".join(w[key]) or "—"}')
    print(f'  puste_swiatlo: {", ".join(w["puste_swiatlo"]) or "—"}')
    if w['zla_nazwa']:
        print(f'  zła nazwa modelu: {model.get("name")}')
    return int(blad(w))


if __name__ == '__main__':
    raise SystemExit(main())
