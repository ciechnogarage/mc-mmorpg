# System przedmiotów (etap 6 planu klas)

Bazy 5/slot na nadklasę, broń wspólna + 4 rasowe, 5 typów ozdób × 5, rzadkość (tiery MMOItems), affiksy prefiks/sufiks
z tierami T1–T3, jakość 0–20% (ulepszanie MMOItems + kamienie z profesji), 8 setów (zbroja 2/4, regalia 2/3/4), łup
w droptabelach Bestiariusza. Research: `docs/research/itemizacja/01..03`.

| Plik | Co robi |
|---|---|
| `tresc.py` | **źródło prawdy**: nazwy, statystyki, pule affiksów, sety, rzadkość, łup, etykiety PL |
| `generuj.py` | pisze bloki `# >>> PRZEDMIOTY` w plikach MMOItems/MythicMobs + cały `modifiers/przedmioty.yml` i `droptables/Przedmioty.yml`; `katalog.json` |
| `sprawdz.py` | bramka (docker, pyyaml): duplikaty kluczy, odwołania do modyfikatorów/setów/tierów/ulepszeń, id w droptabelach |
| `symulacja.py` | odwzorowanie losowania MMOItems: ile affiksów i jakie tiery na rarity × ilvl |
| `klon.sh` | klon world1 do testów (`itm-test`, sieć `itm-net`, bez portów) |
| `run.sh` + `evale/` | evale botem bez OP: `sonda` (lore z gry), `gra` (wymagania, sety, jakość), `rozklad` (affiksy w grze) |
| `strona.py` | strona przeglądu z katalogu, symulacji, sondy i evali |
| `wdroz.sh` / `wycofaj.sh` | world1: kopia → generator → bramka → `mi reload` + `mm reload`; wycofanie z kopii |

Zmiana treści: edytuj `tresc.py` → `generuj.py --plugins <klon>` → `sprawdz.py` → evale → `wdroz.sh`.
Nowa statystyka MMOItems (np. `CUSTOM_ILVL`) i nowy atrybut MMOCore wymagają restartu, nie reloadu.

Współpraca z `tools/klasy-rasowe`: id startowe (`START_BRON`, `START_ZBROJA`, zestawy zbroi) są nietykalne.
Szablony zaczynają się od `option:`, więc auto-waga z `tresc_startowa.py` ich nie dotyka. `required-wiara` wchodzi
automatycznie, gdy atrybut `wiara` istnieje w `MMOCore/attributes`. Łup pod klasę: `ARCHETYP_PH` w `tresc.py`.

Pułapki zmierzone: jeden modyfikator = prefiks ALBO sufiks; `mi generate` zawsze losuje rzadkość (części setów
podawać z `-tier:ZESTAW` albo `mi give`); `<tier-color>` działa w prefiksie; materiał łańcucha to `IRON_CHAIN`;
błędy szablonów MMOItems idą do logu jako INFO, nie WARN.
