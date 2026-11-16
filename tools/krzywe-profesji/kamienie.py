#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kamienie milowe profesji 51-100 -> plugins/MMOCore/exp-tables/mistrzostwa.yml

Natywny mechanizm MMOCore (docs.phoenixdevt.fr/mmocore/level/tables + /misc/triggers):
profesja z `exp-table: <id>` odpala triggery pozycji tabeli przy awansie.
Pozycja z `level: N` odpala sie DOKLADNIE przy osiagnieciu N (bajtkod
ExperienceItem.roll: fixedLevel == level; Profession.updateAdvancement dostaje
NOWY poziom z petli PlayerProfessions.giveExperience). Poziom nigdy nie spada
(plugin Mistrzostwa go nie dotyka), wiec kamien nie powtorzy sie przy zmianie
mistrzostwa.

Co daje kamien (tylko to, co realnie dziala na naszej tresci — statystyki typu
FORTUNE/GATHERING_HASTE dotycza wanilii i custom-mining MMOCore, ktorych NIE
uzywamy: nasze zbieractwo to wezly MythicMobs i bloki Nexo):
  co 5 poziomow  message + sound + money (waluta Vault = CoreTools dollar, per profil)
  60/75/90/100   command -> `mistrzostwo kamien` (tytul; od 75 ogloszenie na serwer)

Uzycie:
  python3 kamienie.py            # podglad
  python3 kamienie.py --zapisz   # pisze exp-tables/mistrzostwa.yml
  python3 kamienie.py --zapisz --podepnij   # + `exp-table: mistrz_<id>` w 13 yml
"""
import argparse
import os
import re

NAZWY = {
    "mining": "Górnictwo", "woodworking": "Drwalstwo i stolarstwo", "herbalism": "Zielarstwo",
    "farming": "Farmerstwo", "fishing": "Rybactwo", "hunting": "Myślistwo",
    "jewelcrafting": "Jubilerstwo", "smithing": "Kowalstwo", "tailoring": "Krawiectwo",
    "alchemy": "Alchemia", "enchanting": "Zaklinanie", "cooking": "Gotowanie", "building": "Budownictwo",
}
POZIOMY = list(range(55, 101, 5))
TYTULY = {60, 75, 90, 100}
# drobny zastrzyk waluty: 9 x 100 + 500 = 1400 $ na profesje za ~250 h gry (~6 $/h)
KWOTA = {lvl: (500 if lvl == 100 else 100) for lvl in POZIOMY}


def tabela():
    out = ["# WYGENEROWANE przez tools/krzywe-profesji/kamienie.py — nie edytowac recznie.",
           "# Kamienie milowe 55..100 (co 5). Opis i uzasadnienie w naglowku generatora.", ""]
    for pid, nazwa in NAZWY.items():
        out.append(f"mistrz_{pid}:")
        for lvl in POZIOMY:
            out.append(f"  poziom_{lvl}:")
            out.append(f"    level: {lvl}")
            out.append("    triggers:")
            out.append(f"      - 'message{{format=\"&6&l✦ &f{nazwa} &7osiąga poziom &f{lvl}&7 — kamień milowy. &a+{KWOTA[lvl]} $\"}}'")
            out.append("      - 'sound{sound=UI_TOAST_CHALLENGE_COMPLETE;volume=0.8;pitch=1.2}'")
            out.append(f"      - 'money{{operation=GIVE;amount={KWOTA[lvl]}}}'")
            if lvl in TYTULY:
                out.append(f"      - 'command{{format=\"mistrzostwo kamien %player_name% {pid} {lvl}\"}}'")
        out.append("")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mmocore", default="/opt/docker/mc/data/world1/plugins/MMOCore")
    ap.add_argument("--zapisz", action="store_true")
    ap.add_argument("--podepnij", action="store_true")
    a = ap.parse_args()
    t = tabela()
    if not a.zapisz:
        print(t[:1500]); return
    sc = os.path.join(a.mmocore, "exp-tables", "mistrzostwa.yml")
    with open(sc, "w", encoding="utf-8") as f:
        f.write(t + "\n")
    print("zapisano", sc)
    if a.podepnij:
        for pid in NAZWY:
            p = os.path.join(a.mmocore, "professions", f"{pid}.yml")
            s = open(p, encoding="utf-8").read()
            if re.search(r"(?m)^exp-table:", s):
                s = re.sub(r"(?m)^exp-table:.*$", f"exp-table: mistrz_{pid}", s)
            else:
                s = re.sub(r"(?m)^(exp-curve:.*)$", rf"\1\nexp-table: mistrz_{pid}", s, count=1)
            open(p, "w", encoding="utf-8").write(s)
            print(f"  {pid}.yml -> exp-table: mistrz_{pid}")


if __name__ == "__main__":
    main()
