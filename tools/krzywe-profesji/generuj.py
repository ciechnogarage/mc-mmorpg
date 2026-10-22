#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generator krzywych XP profesji dla MMOCore (ListExperienceCurve).

DLACZEGO TO ISTNIEJE
--------------------
Do 2026-09-22 wszystkie profesje mialy krzywa-formule:
    exp-curve: '150 + {level}*75 + {level}*{level}*12'
W MMOCore {level} w formule to poziom KLASY gracza, nie profesji
(FormulaExperienceCurve$LevelPlaceholder.parse() -> playerData.getLevel()).
Skutki: w obrebie jednego poziomu klasy krzywa jest PLASKA, a awans klasy
1->50 podnosi koszt kolejnego poziomu profesji ~143x, retroaktywnie.

ListExperienceCurve.getExperience(PlayerData, int level) w ogole nie dotyka
PlayerData - indeksuje liste poziomem PROFESJI i przycina do ostatniej
pozycji (Math.min(level, size) - 1). Krzywa z pliku jest wiec z definicji
niezalezna od poziomu klasy.

FORMAT PLIKU (potwierdzony bajtkodem + docs.phoenixdevt.fr/mmocore/level/curves)
  - katalog: plugins/MMOCore/exp-curves/<nazwa>.txt
  - jedna liczba calkowita na linie, BEZ naglowkow i komentarzy
    (ListExperienceCurve robi Long.valueOf(linia) na KAZDEJ linii)
  - linia N = XP potrzebne, by przejsc z poziomu N na N+1 (NIE kumulatywnie)
  - w yml profesji: exp-curve: <nazwa>   (bez .txt - manager obcina 4 znaki)
  - UWAGA: ExperienceCurve.fromConfig() przy nieznanej nazwie NIE krzyczy,
    tylko po cichu traktuje napis jako formule. Literowka = powrot buga.

MODEL TEMPA
-----------
Nie zgadujemy kosztow - zgadujemy CZAS, a koszt z niego wynika:
    koszt(L) = m(L) * rate(L)          # rate w XP/h, m w godzinach
gdzie m(L) to godziny na przejscie L->L+1, a rate(L) to XP/h dla archetypu.
m(L) = A * L^p * q^L  - trzy parametry dopasowane do trzech kotwic czasowych.
Rodzina jest z definicji gladka i rosnaca (p>=0, q>=1), wiec nie ma skokow.

Przestrojenie tempa = zmiana KOTWIC ponizej albo --kotwice w CLI.
"""

import argparse
import math
import os
import sys

# --- parametry tempa ---------------------------------------------------

# (poziom, skumulowane godziny skupionej gry do osiagniecia tego poziomu)
KOTWICE_DOMYSLNE = [(15, 2.0), (30, 10.0), (50, 45.0)]

MAX_LEVEL = 50          # prog swobodnego rozwoju; do niego kotwice i RATE rzemiosla

# --- mistrzostwo 50 -> 100 (decyzja wlasciciela 2026-09-23) ------------
# Linie 1..49 plikow MUSZA zostac bajt w bajt (postep istniejacych graczy).
# Linie 50..99 to przedluzenie C1 liczone na KOSZCIE XP (gladkosc pliku):
#   g(L) = ln(koszt(L)/koszt(L-1)) startuje z wartosci przy L=49 (+6.1% zbier.,
#   +7.6% rzem.) i wygasa wykladniczo do podlogi G_INF (+1%/poziom);
#   stala tau dobrana bisekcja tak, by 50->100 zajelo GODZINY_MISTRZA.
# Bez wygaszania wzor m(L) dawalby 836 h (q^L eksploduje); bez podlogi
# krzywa robi sie plaska od ~70 i koniec gry nie rosnie.
MAX_LEVEL_MISTRZ = 100
GODZINY_MISTRZA = 250.0
G_INF = math.log(1.01)

# Tempo zdobywania XP, wyprowadzone z realnych plikow (patrz README.md):
#   zbieractwo: wezel daje srednio ~18 XP, cykl (dojscie + kanalowanie) ~25 s
#               -> 18/25 = 0.72 XP/s = ~2600 XP/h, PLASKIE (wezly nie skaluja
#               XP z poziomem profesji)
#   rzemioslo:  XP za receptury rosnie z tierem (12/28/55/95/120), ale wejscia
#               trzeba najpierw zebrac. Tier 1: 12 XP / ~35 s = ~1230 XP/h.
#               Tier 5: 120 XP / ~115 s = ~3760 XP/h. Tempo ROSNIE z poziomem.
RATE = {
    "zbieractwo": lambda L: 2600.0,
    # powyzej 50 plasko 3760 XP/h: nie ma receptur powyzej tieru 5 (bramka 45)
    "rzemioslo": lambda L: 1230.0 + (3760.0 - 1230.0) * (min(L, MAX_LEVEL) - 1) / (MAX_LEVEL - 1),
}

# Przypisanie profesji do archetypu.
# building i cooking sa "mieszane" (wezel + warsztat) - ida jak zbieractwo,
# bo wezel jest ich glowna petla i ma plaskie XP.
PROFESJE = {
    "mining": "zbieractwo",
    "herbalism": "zbieractwo",
    "woodworking": "zbieractwo",
    "farming": "zbieractwo",
    "fishing": "zbieractwo",
    "hunting": "zbieractwo",
    "jewelcrafting": "zbieractwo",
    "building": "zbieractwo",
    "cooking": "zbieractwo",
    "smithing": "rzemioslo",
    "tailoring": "rzemioslo",
    "alchemy": "rzemioslo",
    "enchanting": "rzemioslo",
}


# --- dopasowanie m(L) = A * L^p * q^L ----------------------------------

def czas_skumulowany(A, p, q, poziom):
    """Godziny potrzebne, by z poziomu 1 dojsc do `poziom`."""
    return sum(A * (i ** p) * (q ** i) for i in range(1, poziom))


def reszty(params, kotwice):
    p, q = params
    if p < 0 or q < 1.0:
        return 1e9
    # A wyznaczone z ostatniej kotwicy -> zostaja dwie reszty na dwa parametry
    lvl_ost, godz_ost = kotwice[-1]
    baza = czas_skumulowany(1.0, p, q, lvl_ost)
    if baza <= 0:
        return 1e9
    A = godz_ost / baza
    err = 0.0
    for lvl, godz in kotwice[:-1]:
        err += (math.log(czas_skumulowany(A, p, q, lvl) / godz)) ** 2
    return err


def nelder_mead(f, start, krok=0.3, iteracje=4000, tol=1e-14):
    """Maly Nelder-Mead bez zaleznosci zewnetrznych (brak numpy/scipy na hoscie)."""
    n = len(start)
    simpleks = [list(start)]
    for i in range(n):
        pkt = list(start)
        pkt[i] += krok * (abs(pkt[i]) + 0.1)
        simpleks.append(pkt)
    wart = [f(s) for s in simpleks]
    for _ in range(iteracje):
        kolejnosc = sorted(range(n + 1), key=lambda i: wart[i])
        simpleks = [simpleks[i] for i in kolejnosc]
        wart = [wart[i] for i in kolejnosc]
        if abs(wart[-1] - wart[0]) < tol:
            break
        centroid = [sum(s[i] for s in simpleks[:-1]) / n for i in range(n)]
        odbity = [centroid[i] + (centroid[i] - simpleks[-1][i]) for i in range(n)]
        f_odb = f(odbity)
        if f_odb < wart[0]:
            rozciag = [centroid[i] + 2.0 * (centroid[i] - simpleks[-1][i]) for i in range(n)]
            f_roz = f(rozciag)
            simpleks[-1], wart[-1] = (rozciag, f_roz) if f_roz < f_odb else (odbity, f_odb)
        elif f_odb < wart[-2]:
            simpleks[-1], wart[-1] = odbity, f_odb
        else:
            sciag = [centroid[i] + 0.5 * (simpleks[-1][i] - centroid[i]) for i in range(n)]
            f_sc = f(sciag)
            if f_sc < wart[-1]:
                simpleks[-1], wart[-1] = sciag, f_sc
            else:
                for i in range(1, n + 1):
                    simpleks[i] = [simpleks[0][j] + 0.5 * (simpleks[i][j] - simpleks[0][j])
                                   for j in range(n)]
                    wart[i] = f(simpleks[i])
    naj = min(range(n + 1), key=lambda i: wart[i])
    return simpleks[naj], wart[naj]


def dopasuj(kotwice):
    (p, q), err = nelder_mead(lambda x: reszty(x, kotwice), [1.5, 1.03])
    lvl_ost, godz_ost = kotwice[-1]
    A = godz_ost / czas_skumulowany(1.0, p, q, lvl_ost)
    return A, p, q, err


# --- budowa krzywej ----------------------------------------------------

def przedluz(baza, tau, max_level):
    """Koszty linii MAX_LEVEL..max_level-1 (C1 wzgledem ostatnich dwoch linii bazy)."""
    g0 = math.log(baza[-1] / baza[-2])
    L0 = MAX_LEVEL - 1
    c, wynik = float(baza[-1]), []
    for L in range(MAX_LEVEL, max_level):
        c *= math.exp(G_INF + (g0 - G_INF) * math.exp(-(L - L0) / tau))
        wynik.append(c)
    return wynik


def dobierz_tau(baza, rate, max_level, cel):
    godz = lambda tau: sum(c / rate(L) for L, c in zip(range(MAX_LEVEL, max_level), przedluz(baza, tau, max_level)))
    lo, hi = 1e-3, 1e3
    if godz(lo) > cel:
        raise SystemExit(f"cel {cel} h ponizej minimum {godz(lo):.1f} h dla podlogi G_INF")
    for _ in range(200):
        mid = (lo + hi) / 2
        if godz(mid) > cel:
            hi = mid
        else:
            lo = mid
    return (lo + hi) / 2


def zbuduj_krzywa(archetyp, kotwice, max_level, cel_mistrza=GODZINY_MISTRZA):
    A, p, q, err = dopasuj(kotwice)
    rate = RATE[archetyp]
    koszty = []
    for L in range(1, min(max_level, MAX_LEVEL)):   # linia L = przejscie L -> L+1
        godziny = A * (L ** p) * (q ** L)
        xp = godziny * rate(L)
        koszty.append(int(round(xp)))
    if max_level > MAX_LEVEL:
        tau = dobierz_tau(koszty, rate, max_level, cel_mistrza)
        koszty += [int(round(c)) for c in przedluz(koszty, tau, max_level)]
    # gwarancja scislej monotonicznosci po zaokragleniu
    for i in range(1, len(koszty)):
        if koszty[i] <= koszty[i - 1]:
            koszty[i] = koszty[i - 1] + 1
    return koszty, (A, p, q, err)


def tabela(koszty, archetyp, max_level):
    rate = RATE[archetyp]
    wiersze = []
    skum_xp = 0
    skum_h = 0.0
    for idx, koszt in enumerate(koszty):
        L = idx + 1
        skum_xp += koszt
        skum_h += koszt / rate(L)
        wiersze.append((L + 1, koszt, skum_xp, skum_h))
    return wiersze


def main():
    ap = argparse.ArgumentParser(description="Generator krzywych XP profesji MMOCore")
    ap.add_argument("--exp-curves", default="/opt/docker/mc/data/world1/plugins/MMOCore/exp-curves")
    ap.add_argument("--professions", default="/opt/docker/mc/data/world1/plugins/MMOCore/professions")
    ap.add_argument("--max-level", type=int, default=MAX_LEVEL_MISTRZ)
    ap.add_argument("--godziny-mistrza", type=float, default=GODZINY_MISTRZA,
                    help="skupione godziny na 50->100 (domyslnie %(default)s)")
    ap.add_argument("--ustaw-max-level", action="store_true",
                    help="przy --zapisz podmien tez max-level w 13 yml (WYMAGA pluginu Mistrzostwa)")
    ap.add_argument("--kotwice", default=None,
                    help="np. '15:2,30:10,50:45' - poziom:godziny")
    ap.add_argument("--zapisz", action="store_true", help="zapisz pliki (domyslnie tylko podglad)")
    args = ap.parse_args()

    if args.kotwice:
        kotwice = []
        for czesc in args.kotwice.split(","):
            lvl, godz = czesc.split(":")
            kotwice.append((int(lvl), float(godz)))
        kotwice.sort()
    else:
        kotwice = list(KOTWICE_DOMYSLNE)

    for archetyp in sorted(RATE):
        koszty, (A, p, q, err) = zbuduj_krzywa(archetyp, kotwice, args.max_level, args.godziny_mistrza)
        wiersze = tabela(koszty, archetyp, args.max_level)
        print(f"\n=== {archetyp} ===")
        print(f"m(L) = {A:.6g} * L^{p:.4f} * {q:.6f}^L    (blad dopasowania {err:.3e})")
        print(f"{'poziom':>7} {'koszt XP':>10} {'skum. XP':>11} {'skum. h':>9}")
        for lvl, koszt, sxp, sh in wiersze:
            if lvl in (2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100):
                print(f"{lvl:>7} {koszt:>10} {sxp:>11} {sh:>9.2f}")
        for lvl_k, godz_k in kotwice:
            trafione = next(sh for lvl, _, _, sh in wiersze if lvl == lvl_k)
            print(f"  kotwica L{lvl_k}: cel {godz_k:.1f} h, wyszlo {trafione:.2f} h")

        if args.zapisz:
            sciezka = os.path.join(args.exp_curves, f"{archetyp}.txt")
            with open(sciezka, "w", encoding="utf-8") as fh:
                fh.write("\n".join(str(k) for k in koszty) + "\n")
            print(f"  zapisano {sciezka} ({len(koszty)} linii)")

    if args.zapisz:
        import re
        for prof, archetyp in sorted(PROFESJE.items()):
            sciezka = os.path.join(args.professions, f"{prof}.yml")
            if not os.path.exists(sciezka):
                print(f"  UWAGA: brak {sciezka}", file=sys.stderr)
                continue
            with open(sciezka, encoding="utf-8") as fh:
                tresc = fh.read()
            nowa, ile = re.subn(r"(?m)^exp-curve:.*$", f"exp-curve: {archetyp}", tresc)
            if args.ustaw_max_level:
                nowa, ile_ml = re.subn(r"(?m)^max-level:.*$", f"max-level: {args.max_level}", nowa)
                if ile_ml != 1:
                    print(f"  UWAGA: {prof}.yml - podmieniono {ile_ml} linii max-level", file=sys.stderr)
            if ile != 1:
                print(f"  UWAGA: {prof}.yml - podmieniono {ile} linii exp-curve", file=sys.stderr)
            with open(sciezka, "w", encoding="utf-8") as fh:
                fh.write(nowa)
            print(f"  {prof}.yml -> exp-curve: {archetyp}")


if __name__ == "__main__":
    main()
