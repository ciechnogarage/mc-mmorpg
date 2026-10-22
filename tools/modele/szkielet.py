"""Przeguby i segmentowe bryły do generatorów .bbmodel.

Współrzędne są w pikselach modelu. Wynikiem jest kość bb.kosc;
helpery dopisują kostki do bieżącego bb.ELEMENTY. Każdy przegub
ma własną kość i pivot dokładnie w podanym punkcie.
"""
from __future__ import annotations

import math
import bb


def _punkt(p):
    if len(p) != 3 or not all(math.isfinite(float(v)) for v in p):
        raise ValueError('punkt musi zawierać trzy skończone współrzędne')
    return tuple(float(v) for v in p)


def _grubosc(w):
    if float(w) < 1 or not math.isfinite(float(w)):
        raise ValueError('grubość segmentu musi wynosić co najmniej 1 px')
    return float(w)


def segment(nazwa, start, koniec, grubosc=3, walor=2, kroki=None):
    """Łączy punkty profilem krótkich legalnych, osiowych kostek.

    Segment ukośny jest schodkowany; liczba kroków domyślnie wynika
    z długości, więc nie wymaga niedozwolonego obrotu kostki.
    Zwraca listę UUID kostek. Kostki pozostają dziećmi kości wywołującej.
    """
    a, b = _punkt(start), _punkt(koniec)
    w = _grubosc(grubosc)
    odleglosc = math.dist(a, b)
    if odleglosc < 1e-6:
        raise ValueError('segment wymaga różnych punktów')
    n = int(kroki) if kroki is not None else max(1, math.ceil(odleglosc / max(w, 2)))
    if n < 1:
        raise ValueError('kroki musi być dodatnie')
    wynik = []
    for i in range(n):
        p = tuple(a[j] + (b[j] - a[j]) * i / n for j in range(3))
        q = tuple(a[j] + (b[j] - a[j]) * (i + 1) / n for j in range(3))
        lo = tuple(min(p[j], q[j]) - w / 2 for j in range(3))
        hi = tuple(max(p[j], q[j]) + w / 2 for j in range(3))
        wynik.append(bb.szescian(f'{nazwa}_{i}', lo, hi, walor))
    return wynik


def lancuch(nazwa, punkty, grubosci=3, walor=2):
    """Hierarchia kości z pivotem w każdym punkcie (od podstawy do końca)."""
    pts = [_punkt(p) for p in punkty]
    if len(pts) < 2:
        raise ValueError('łańcuch wymaga co najmniej dwóch punktów')
    szer = [grubosci] * (len(pts) - 1) if isinstance(grubosci, (int, float)) else list(grubosci)
    if len(szer) != len(pts) - 1:
        raise ValueError('liczba grubości musi odpowiadać liczbie segmentów')
    dziecko = bb.kosc(f'{nazwa}_koniec', pts[-1], [])
    for i in reversed(range(len(pts) - 1)):
        czesc = segment(f'{nazwa}_{i}', pts[i], pts[i + 1], szer[i], walor)
        dziecko = bb.kosc(f'{nazwa}_{i}', pts[i], czesc + [dziecko])
    return dziecko


def konczyna(nazwa, staw_start, staw_srodkowy, staw_koncowy,
             grubosc_gorna=4, grubosc_dolna=3, walor=2, stopa=None):
    """Noga biodro-kolano-stopa lub ręka bark-łokieć-dłoń.

    ``stopa`` to opcjonalne (od, do) kostki dłoni/stopy osadzonej
    w końcowym pivocie. Zwraca kość początkową gotową do outlinera.
    """
    a, b, c = map(_punkt, (staw_start, staw_srodkowy, staw_koncowy))
    koniec = []
    if stopa is not None:
        lo, hi = map(_punkt, stopa)
        koniec.append(bb.szescian(f'{nazwa}_stopa', lo, hi, walor))
    dlon = bb.kosc(f'{nazwa}_koniec', c, koniec)
    podudzie = bb.kosc(f'{nazwa}_srodek', b,
                       segment(f'{nazwa}_dol', b, c, grubosc_dolna, walor) + [dlon])
    return bb.kosc(f'{nazwa}_start', a,
                   segment(f'{nazwa}_gora', a, b, grubosc_gorna, walor) + [podudzie])


def fazuj(nazwa, od, do, walor=2, os='y', skos=2, kat=0):
    """Profil z trzech kostek: zwężone końce i pełny środek.

    Dopuszcza pojedynczy legalny obrót 22.5° lub 45° wokół osi profilu.
    """
    lo, hi = _punkt(od), _punkt(do)
    if any(hi[i] <= lo[i] for i in range(3)):
        raise ValueError('od musi leżeć poniżej do w każdej osi')
    if os not in 'xyz':
        raise ValueError('os musi być x, y albo z')
    if kat not in bb.DOZWOLONE_KATY:
        raise ValueError('kąt kostki musi być legalny dla ModelEngine')
    k = 'xyz'.index(os)
    inne = [j for j in range(3) if j != k]
    s = float(skos)
    if s < 0 or 2 * s >= min(hi[j] - lo[j] for j in inne):
        raise ValueError('skos jest zbyt duży')
    dl = (hi[k] - lo[k]) / 3
    if dl < 1:
        raise ValueError('każda warstwa musi mieć co najmniej 1 px')
    rot = [0, 0, 0]
    rot[k] = kat
    wynik = []
    for i in range(3):
        f, t = list(lo), list(hi)
        f[k], t[k] = lo[k] + i * dl, lo[k] + (i + 1) * dl
        if i != 1:
            for j in inne:
                f[j] += s
                t[j] -= s
        wynik.append(bb.szescian(f'{nazwa}_{i}', f, t, walor,
                                   rot=rot))
    return wynik
