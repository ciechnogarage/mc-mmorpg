# Zaklecia — zaklęcia w dłoni (Skyrim), obie ręce

Plugin prowadzi rzucanie zaklęć koncentracji, a MythicMobs rysuje efekty i zadaje obrażenia.

- **Rzucanie** (`Rzucanie.java`): PPM trzyma zaklęcie lewej dłoni, LPM włącza lub wyłącza strumień prawej albo ładuje i strzela,
  tiki co 1 tick na wątku głównym, mana co 10 ticków, cele stożka liczone tutaj i podawane skillowi MM
  jako cele. Tryby wynikają z tego, co jest w dłoniach: prawa, podwójne (to samo w obu: mana ×2,5),
  dwa różne (suma), broń w prawej + zaklęcie w lewej.
- **Nośnik** (`Nosnik.java`): `zaklecia przypisz <gracz> <id|nic> <prawa|lewa>` (Księga zaklęć w DeluxeMenus,
  jako konsola) lub pasek Shift+Q. Nośnika nie da się wyrzucić, przenieść, włożyć do ramki, doniczki, półki ani skarbca.
- **Bramka klasy** (`Bramka.java`, decyzja właściciela 21.10): rzucać i brać zaklęcie do dłoni mogą tylko klasy MMOCore
  z `plugins/Zaklecia/klasy.yml` → `dozwolone-klasy` (id klas, wielkość liter bez znaczenia). Plik pisze
  `tools/klasy-rasowe/generuj_klasy.py` (wszystkie klasy archetypów mag i akolita). Brak pliku lub pusta lista = bramka wyłączona.
  Odmowa: `przypisz`/koło Shift+Q oraz LPM/PPM z nośnikiem w dłoni → pasek „Twoja klasa nie włada zaklęciami.”.
  Plik czytany tylko przy starcie pluginu (zmiana = restart).
- **MM**: warunki `zakDlon{reka;z}`, `zakRece{z;min;max}`, mechanika `zakZasob`, warunek `zakMaZasob`.
- Konfiguracja: `config.yml` (zaklęcia, koszt many, stożek, nazwy skilli).

Treść (przedmioty, skille, menu), wdrożenie i evale: `tools/zaklecia/v2/ (repo MC)` i `tools/zaklecia/README.md`.
Build: `./build.sh` → `build/libs/zaklecia-*.jar`.
