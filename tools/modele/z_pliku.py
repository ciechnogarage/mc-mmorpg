"""Generator na bazie ISTNIEJACEGO pliku modelu (np. odzyskanej wersji): wczytuje .bbmodel 4.10 do struktur bb.py,
pozwala zmieniac kostki/kosci, przemalowac teksture materialami (tekstura.atlas) i podmienic pojedyncze animacje;
pozostale animacje zostaja 1:1 (sa juz w konwencji ME).

Uzycie w generatorze moba:
    import z_pliku as Z
    Z.wczytaj('/opt/.../wersje/korzeniak_w1.bbmodel')
    Z.usun('oko_l'); Z.dodaj('hi_glowa', S('rog', ...)); Z.zmien('pien_dol', do=(4, 9, 4))
    Z.zapisz_model(sys.argv[1], 'pniaczek', MATERIALY, material, naklejki=..., animacje_nowe=..., usun_animacje=...)
"""
import json

import animacje as A
import bb
import tekstura

_BAZA = {}


def wczytaj(sciezka):
    m = json.load(open(sciezka))
    assert str(m['meta'].get('format_version', '')).startswith('4'), 'najpierw dobij_wersje.py (BB5 -> 4.10)'
    bb.wyczysc()
    bb.ELEMENTY.extend(e for e in m['elements'] if 'from' in e)
    for e in bb.ELEMENTY:  # BB zapisuje zerowy obrot i domyslny origin jako brak pola
        e.setdefault('rotation', [0, 0, 0])
        e.setdefault('origin', [(e['from'][i] + e['to'][i]) / 2 for i in range(3)])
    _BAZA.clear()
    _BAZA.update(outliner=m['outliner'], animacje=m.get('animations', []), elementy={e['uuid']: e for e in bb.ELEMENTY})
    for k in kosci():
        bb.KOSCI[k['name']] = k['uuid']
    return m


def kosci(o=None):
    for x in (_BAZA['outliner'] if o is None else o):
        if isinstance(x, dict):
            yield x
            yield from kosci(x['children'])


def kosc(nazwa):
    return next(k for k in kosci() if k['name'] == nazwa)


def element(nazwa):
    return next(e for e in bb.ELEMENTY if e['name'] == nazwa)


def usun(*nazwy):
    """Usuwa kostki po nazwie (kostka i kosc moga sie nazywac tak samo - do kosci sluzy usun_kosc)."""
    for n in nazwy:
        e = element(n)
        bb.ELEMENTY.remove(e)
        for k in kosci():
            if e['uuid'] in k['children']:
                k['children'].remove(e['uuid'])


def usun_kosc(*nazwy):
    """Usuwa cale kosci razem z kostkami i koscmi-dziecmi."""
    for n in nazwy:
        k = kosc(n)
        for kk in [k] + list(kosci(k['children'])):
            for c in kk['children']:
                if isinstance(c, str):
                    bb.ELEMENTY[:] = [e for e in bb.ELEMENTY if e['uuid'] != c]
            bb.KOSCI.pop(kk['name'], None)
        for rodzic in [{'children': _BAZA['outliner']}] + list(kosci()):
            if k in rodzic['children']:
                rodzic['children'].remove(k)


def dodaj(kosc_nazwa, *dzieci):
    """Dodaje do kosci kostki (uuid z bb.szescian) lub kosci (dict z bb.kosc)."""
    kosc(kosc_nazwa)['children'].extend(dzieci)


def zmien(nazwa, od=None, do=None, origin=None, rot=None, przesun=None):
    e = element(nazwa)
    if przesun:
        for i in range(3):
            e['from'][i] += przesun[i]; e['to'][i] += przesun[i]; e['origin'][i] += przesun[i]
    if od:
        e['from'] = list(od)
    if do:
        e['to'] = list(do)
    if origin:
        e['origin'] = list(origin)
    if rot is not None:
        e['rotation'] = list(rot)


def zapisz_model(sciezka, nazwa, materialy, material, naklejki=None, animacje_nowe=None, usun_animacje=(), zmien_nazwy_animacji=None,
                 ziarno=7, animowane_o=(), fx_tylko=None):
    """animacje_nowe: slownik jak w animacje.py (prawdziwy ruch) - ZASTEPUJE animacje o tych nazwach.
    fx_tylko: {kosc_fx: (animacje,...)} - w KAZDEJ innej animacji (takze odziedziczonej) kosc dostaje skale 0."""
    animacje_nowe = dict(animacje_nowe or {})
    for fx, gdzie in (fx_tylko or {}).items():
        for n, a in animacje_nowe.items():
            if n not in gdzie:
                a['kosci'][fx] = {'scale': [(0, (0, 0, 0)), (a['dlugosc'], (0, 0, 0))]}
    anim_nowe = A.zbuduj(animacje_nowe, bb.KOSCI, do_me=True) if animacje_nowe else []
    nowe_nazwy = {a['name'] for a in anim_nowe}
    zostaja = [a for a in _BAZA['animacje'] if a['name'] not in nowe_nazwy and a['name'] not in usun_animacje]
    # animatory kosci, ktorych juz nie ma, wypadaja (Blockbench i ME by je ignorowaly, ale smieca plik)
    uuidy = {k['uuid'] for k in kosci()}
    for a in zostaja:
        a['animators'] = {u: an for u, an in a.get('animators', {}).items() if u in uuidy}
        if zmien_nazwy_animacji and a['name'] in zmien_nazwy_animacji:
            a['name'] = zmien_nazwy_animacji[a['name']]
    for fx, gdzie in (fx_tylko or {}).items():
        for a in zostaja:
            if a['name'] not in gdzie:  # nazwy juz po zmianie (zmien_nazwy_animacji)
                ukryte = A.zbuduj({'_': {'petla': 'once', 'dlugosc': a['length'], 'kosci': {fx: {
                    'scale': [(0, (0, 0, 0)), (a['length'], (0, 0, 0))]}}}}, bb.KOSCI, do_me=True)[0]['animators']
                a['animators'].update(ukryte)
    animowane = {an['name'] for a in zostaja + anim_nowe for an in a['animators'].values()} | set(animowane_o)
    bb.waliduj(animowane)
    tex, _ = tekstura.atlas(bb.ELEMENTY, materialy, material, ziarno=ziarno, naklejki=naklejki)
    bb.zapisz(sciezka, nazwa, _BAZA['outliner'], tex, 128, zostaja + anim_nowe)


def do_siatki(pomin=()):
    """Przyciaga kazda kostke do siatki 1:1 (LR): wymiar > 0 zaokraglony do pelnych px (min 1), plaszczyzna (0) zostaje;
    srodek kostki zostaje w miejscu (+-0.5 px). Zwraca liste zmienionych nazw."""
    zm = []
    for e in bb.ELEMENTY:
        if e['name'] in pomin:
            continue
        stary = (list(e['from']), list(e['to']))
        for i in range(3):
            d = e['to'][i] - e['from'][i]
            if abs(d) < 1e-6:
                continue
            c = (e['from'][i] + e['to'][i]) / 2
            n = max(1, round(abs(d)))
            lo = c - n / 2
            lo = round(lo * 2) / 2      # krawedzie na pol-pikselu wystarcza (LR tez tak ma przy nieparzystych)
            e['from'][i], e['to'][i] = lo, lo + n
        if (e['from'], e['to']) != stary:
            zm.append(e['name'])
    return zm


def przenies(kostki, do_kosci):
    """Przenosi kostki (po nazwie) do innej kosci (pozycje absolutne w pliku - nic sie nie przesuwa w spoczynku)."""
    for n in kostki:
        u = element(n)['uuid']
        for k in kosci():
            if u in k['children']:
                k['children'].remove(u)
        kosc(do_kosci)['children'].append(u)
