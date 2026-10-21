#!/usr/bin/env python3
"""Zielarz v7 na KLONIE: przykladowe receptury z wybornymi surowcami + ceny skupu wybornych.
Wstawia bloki miedzy znacznikami (idempotentnie). Uzycie: klon_v7.py <katalog danych klonu>.
TRESC DO PRZEGLADU WLASCICIELA (receptury, ceny) — na world1 docelowo przez generuj.py."""
import re
import sys

D = sys.argv[1]
OD, DO = "# >>> ZIELARZ v7", "# <<< ZIELARZ v7"


def wstaw(plik, po, blok, znak):
    """Blok miedzy znacznikami "<OD> <znak>" ... "<DO> <znak>" — kazde miejsce w pliku ma wlasny znak, inaczej
    drugie wstawienie do tego samego pliku kasowaloby pierwsze."""
    od, do = f"{OD} {znak}", f"{DO} {znak}"
    blok = blok.replace(OD, od).replace(DO, do)
    s = open(plik, encoding="utf-8").read()
    s = re.sub(r"\n[ ]*" + re.escape(od) + r"\n.*?" + re.escape(do) + r"[^\n]*", "", s, flags=re.S)
    s = re.sub(r"\n[ ]*" + re.escape(OD) + r"\n.*?" + re.escape(DO) + r"\n", "\n", s, flags=re.S)   # stary format bez znaku
    m = list(re.finditer(po, s, flags=re.M))
    assert len(m) == 1, (plik, po, len(m))
    i = m[0].end()
    s = s[:i] + "\n" + blok.rstrip("\n") + s[i:]
    open(plik, "w", encoding="utf-8").write(s)


def receptura(rid, out, ile, czas, prof, lvl, skladniki):
    sk = "".join(f"    - mmoitem{{type=MATERIAL,id={i},amount={n}}}\n" for i, n in skladniki)
    return (f"  {rid}:\n    output: mmoitem{{type=CONSUMABLE,id={out},amount={ile}}}\n    crafting-time: {czas}\n"
            f"    conditions:\n    - profession{{profession={prof},level={lvl}}}\n    ingredients:\n{sk}"
            f"    options:\n      hide-when-locked: false\n")


S = D + "/plugins/MMOItems/crafting-stations/"
# na poczatku listy: stacje t1 maja 8 slotow receptur bez przewijania stron (alchemia ma 12 receptur — 4 nieosiagalne)
wstaw(S + "profession-alchemy.yml", r"^recipes:",
      f"  {OD}\n"
      + receptura("alchemy_t1_wyborna_mikstura", "PROF_MIKSTURA_LECZNICZA_2", 1, 3, "alchemy", 3,
                  [("PROF_MEDICINAL_HERB_WYBORNE", 3), ("PROF_CLAM", 1)])
      + receptura("alchemy_t1_wyborny_eliksir", "PROF_ELIKSIR_UMYSLU_2", 1, 4, "alchemy", 7,
                  [("PROF_KORZEN_KOZLKA_WYBORNE", 1), ("PROF_HERBAL_EXTRACT", 1)])
      + f"  {DO}", "alchemia")
wstaw(S + "profession-cooking.yml", r"^recipes:",
      f"  {OD}\n"
      + receptura("cooking_t1_wyborny_prowiant", "PROF_POSILEK_PROWIANT_2", 2, 2, "cooking", 3,
                  [("PROF_FRESH_HARVEST", 2), ("PROF_WILD_SPICE_WYBORNE", 1)])
      + f"  {DO}", "kuchnia")


def towar(klucz, mid, cena):
    return (f"      {klucz}:\n        options: {{sell: false, buy: true}}\n        prices:\n          buy:\n            dollar: {cena}\n"
            f"        display:\n          source: mmoitems\n          type: MATERIAL\n          id: {mid}\n"
            f"        item:\n          source: mmoitems\n          type: MATERIAL\n          id: {mid}\n")


K = D + "/plugins/CoreTools/Shops/skup.yml"
# wyborne x1.75 ceny zwyklego; korzen kozlka (tylko klon) 0.6 / wyborny 1.05
wstaw(K, r"^    ziola:\n      name: [^\n]*",
      f"      {OD}\n" + towar("prof_medicinal_herb_wyborne", "PROF_MEDICINAL_HERB_WYBORNE", 0.7)
      + towar("prof_korzen_kozlka", "PROF_KORZEN_KOZLKA", 0.6)
      + towar("prof_korzen_kozlka_wyborne", "PROF_KORZEN_KOZLKA_WYBORNE", 1.05) + f"      {DO}", "skup-ziola")
wstaw(K, r"^    kuchnia:\n      name: [^\n]*",
      f"      {OD}\n" + towar("prof_wild_spice_wyborne", "PROF_WILD_SPICE_WYBORNE", 0.7) + f"      {DO}", "skup-kuchnia")
print("klon_v7: receptury i skup wstawione")
