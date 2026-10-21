#!/usr/bin/env python3
"""Wybor postaci bez kosza (2026-10-01): porzadki w sekcji `teksty:` i `wybor-postaci:` config.yml.

Usuwa klucze tekstow kosza (po starych napisach "trafia do kosza" gracz dostawalby klamstwa)
i dopisuje nowe klucze (limit postaci, usuwanie na zawsze). Bez pyyaml (brak na hoscie) —
edycja po liniach, z wyliczaniem sciezki klucza z wciec. Idempotentny.

Uzycie: python3 bez-kosza-teksty.py <config.yml> [<config.yml> ...]
"""
import sys

USUN = {
    "teksty.podpowiedz.usun",
    "teksty.wybor.brak-miejsc",
    "teksty.wybor.kosz.dymek", "teksty.wybor.kosz.opis", "teksty.wybor.kosz.przeniesiono",
    "teksty.wybor.kosz.przywroc-zaznaczone", "teksty.wybor.kosz.przywrocono", "teksty.wybor.kosz.pusty",
    "teksty.wybor.kosz.pusty-dymek", "teksty.wybor.kosz.tytul", "teksty.wybor.kosz.usun-na-zawsze",
    "teksty.wybor.kosz.zostalo",
    "teksty.wybor.lista.brak-miejsc", "teksty.wybor.lista.brak-miejsc-kosz", "teksty.wybor.lista.w-koszu",
    "teksty.wybor.lista.w-koszu-1", "teksty.wybor.lista.wszystkie-w-koszu", "teksty.wybor.lista.zarzadzaj-dymek",
    "teksty.wybor.panel.naglowek",
    "teksty.wybor.potwierdz.do-kosza", "teksty.wybor.potwierdz.przycisk-do-kosza",
    "teksty.wybor.potwierdz.tytul-do-kosza",
    "teksty.wybor.usun.karta", "teksty.wybor.usun.w-koszu",
    "teksty.wybor.zarzadzaj.kosz", "teksty.wybor.zarzadzaj.opis",
}

# rodzic -> [(klucz, wartosc)]
DODAJ = {
    "teksty.podpowiedz": [("usun-postac", '"Usuń postać"')],
    "teksty.wybor": [("brak-miejsc-licznik", '"&eLimit postaci: {zajete}/{wszystkie} — usuń postać, żeby stworzyć nową."')],
    "teksty.wybor.lista": [
        ("brak-miejsc-licznik", '"&8Limit postaci {zajete}/{wszystkie}"'),
        ("zarzadzaj-dymek-usun", '"Usuń jedną albo kilka postaci naraz"'),
    ],
    "teksty.wybor.nowa": [("karta-limit",
        '"&c&lLimit postaci: {zajete}/{wszystkie}\\n\\n&r&fMasz już wszystkie miejsca na postacie. Usuń którąś, żeby stworzyć nową."')],
    "teksty.wybor.panel": [
        ("naglowek-licznik", '"&fTwoje postacie &7{zajete}/{wszystkie}  &6[A/D]"'),
        ("nowa-limit", '"+ Nowa postać (limit)"'),
    ],
    "teksty.wybor.usun": [("karta-na-zawsze",
        '"&c&lUsuń postacie\\n\\n&r&fZaznacz jedną lub kilka postaci. Usunięcie jest ostateczne — postać znika razem z ekwipunkiem i postępem."')],
    "teksty.wybor.zarzadzaj": [("opis-na-zawsze",
        '"&fKliknij postacie, żeby je zaznaczyć. Usunięcie jest ostateczne ({zajete}/{wszystkie} miejsc zajętych)."')],
}

# Linie spoza `teksty:` (dokladne dopasowanie po strip) -> zamiennik (None = usun).
ZAMIEN = {
    "# WYBOR POSTACI (scena wyboru, lista pod PPM i w menu Q, zarzadzanie, kosz) — 2026-09-30":
        "# WYBOR POSTACI (scena wyboru, lista pod PPM i w menu Q, zarzadzanie) — 2026-09-30; kosz usuniety 2026-10-01",
    "# Ile dni postac lezy w koszu, zanim zniknie naprawde (usuwana przy nastepnym wejsciu": None,
    "# wlasciciela do wyboru postaci). Do tego czasu zajmuje miejsce na postac.": None,
    "kosz-dni: 7": None,
    "# Od ktorego poziomu usuniecie (do kosza i na zawsze) wymaga wpisania imienia postaci.":
        "# Od ktorego poziomu usuniecie (zawsze ostateczne) wymaga wpisania imienia postaci.",
}


def key_of(line):
    body = line.strip()
    if not body or body.startswith("#") or ":" not in body:
        return None
    return body.split(":", 1)[0].strip().strip('"')


def process(path):
    lines = open(path, encoding="utf-8").read().split("\n")
    out = []
    stack = []  # (indent, key)
    removed = added = replaced = 0
    present = set()
    headers = {}  # sciezka rodzica -> indeks w out i wciecie dzieci
    for line in lines:
        stripped = line.strip()
        if stripped in ZAMIEN:
            new = ZAMIEN[stripped]
            replaced += 1
            if new is not None:
                out.append(line[: len(line) - len(line.lstrip())] + new)
            continue
        k = key_of(line)
        if k is None:
            out.append(line)
            continue
        indent = len(line) - len(line.lstrip())
        while stack and stack[-1][0] >= indent:
            stack.pop()
        path_k = ".".join([s[1] for s in stack] + [k])
        is_section = stripped.endswith(":")
        stack.append((indent, k))
        if path_k in USUN:
            removed += 1
            continue
        present.add(path_k)
        out.append(line)
        if is_section:
            headers[path_k] = (len(out) - 1, indent + 2)
    # dopisywanie od konca, zeby indeksy sie nie przesuwaly
    inserts = []
    for parent, items in DODAJ.items():
        if parent not in headers:
            print(f"  {path}: brak sekcji {parent} — pomijam {len(items)} kluczy")
            continue
        at, ind = headers[parent]
        for key, val in items:
            if f"{parent}.{key}" in present:
                continue
            inserts.append((at, " " * ind + f"{key}: {val}"))
    for at, text in sorted(inserts, key=lambda x: -x[0]):
        out.insert(at + 1, text)
        added += 1
    open(path, "w", encoding="utf-8").write("\n".join(out))
    print(f"{path}: usuniete {removed}, dopisane {added}, zamienione linie {replaced}")


for p in sys.argv[1:]:
    process(p)
