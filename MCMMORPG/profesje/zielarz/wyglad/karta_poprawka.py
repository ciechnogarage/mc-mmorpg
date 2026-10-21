#!/usr/bin/env python3
"""Karta postaci (MythicHUD l1-okno) bez surowych %wiary_*% i bez angielskiej klasy domyslnej.

Uzycie: karta_poprawka.py <katalog plugins> [--sprawdz]
Idempotentny (znacznik w komentarzu). Zmienia:
  1. MythicHUD/hud_assets/hud/l1-okno.yml: tlo ikony rasy (okno pelne i przygaszone), linia "Wiara N" i "Laska ..."
     pokazuja sie tylko, gdy %wiary_wiara% jest liczba (ekspansja Wiar zarejestrowana). Bez Wiar (np. klon bez
     --z-kreatorem: brak MMOProfiles -> Wiary nie wstaja) MythicHUD pokazywal surowe "Laska %wiary_laska_czas%".
  2. MMOCore/classes/human.yml: klasa-poczekalnia przed kreatorem ma nazwe 'Bez klasy' zamiast 'Human'.
Cofniecie: kopie w <plugins>/../karta-kopie/ (tworzone przy pierwszej zmianie, poza katalogami pluginow).
"""
import shutil, sys, pathlib

ZNAK = "# karta-bez-wiar"
WARUNEK = "placeholder{ph=%wiary_wiara%;v=0;o=>=} true hide"

def hud(t: str) -> str:
    if ZNAK in t:
        return t
    t = t.replace("        - placeholder{ph=%wiary_rasa%;val=brak} false hide\n",
                  "        - placeholder{ph=%wiary_rasa%;val=brak} false hide\n        - " + WARUNEK + "   " + ZNAK + "\n")
    for el in ("l1-pl-wiara:", "l1-pl-laska:"):
        i = t.index("\n" + el + "\n")
        j = t.index("  layers:", i)
        t = t[:j] + "    - " + WARUNEK + "   " + ZNAK + "\n" + t[j:]
    return t

def klasa(t: str) -> str:
    return t.replace("    name: 'Human'\n", "    name: 'Bez klasy'\n")

def main():
    pl = pathlib.Path(sys.argv[1])
    sprawdz = "--sprawdz" in sys.argv
    zmiany = 0
    for rel, fn in (("MythicHUD/hud_assets/hud/l1-okno.yml", hud), ("MMOCore/classes/human.yml", klasa)):
        p = pl / rel
        stare = p.read_text(encoding="utf-8")
        nowe = fn(stare)
        if nowe == stare:
            print("bez zmian:", rel); continue
        zmiany += 1
        if sprawdz:
            print("DO ZMIANY:", rel); continue
        kopia = pl.parent / "karta-kopie" / rel
        kopia.parent.mkdir(parents=True, exist_ok=True)
        if not kopia.exists():
            shutil.copy2(p, kopia)
        p.write_text(nowe, encoding="utf-8")
        print("zmieniono:", rel, "(kopia", str(kopia) + ")")
    if not sprawdz:
        n = (pl / "MythicHUD/hud_assets/hud/l1-okno.yml").read_text(encoding="utf-8").count(ZNAK)
        assert n in (0, 4), f"oczekiwano 4 warunkow, jest {n}"
    return 0

if __name__ == "__main__":
    sys.exit(main())
