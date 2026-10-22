# Zaklecia — zaklęcia w dłoni (Skyrim), obie ręce

Plugin prowadzi rzucanie zaklęć koncentracji, a MythicMobs rysuje efekty i zadaje obrażenia.

- **Rzucanie** (`Rzucanie.java`): PPM trzyma zaklęcie lewej dłoni, LPM włącza lub wyłącza strumień prawej albo ładuje i strzela,
  tiki co 1 tick na wątku głównym, mana co 10 ticków, cele stożka liczone tutaj i podawane skillowi MM
  jako cele. Tryby wynikają z tego, co jest w dłoniach: prawa, podwójne (to samo w obu: mana ×2,5),
  dwa różne (suma), broń w prawej + zaklęcie w lewej.
- **Nośnik** (`Nosnik.java`): `zaklecia przypisz <gracz> <id|nic> <prawa|lewa>` (Księga zaklęć w DeluxeMenus,
  jako konsola) lub pasek Shift+Q. Nośnika nie da się wyrzucić, przenieść, włożyć do ramki, doniczki, półki ani skarbca.
- **MM**: warunki `zakDlon{reka;z}`, `zakRece{z;min;max}`, mechanika `zakZasob`, warunek `zakMaZasob`.
- Konfiguracja: `config.yml` (zaklęcia, koszt many, stożek, nazwy skilli).

Treść (przedmioty, skille, menu), wdrożenie i evale: `tools/zaklecia/v2/ (repo MC)` i `tools/zaklecia/README.md`.
Build: `./build.sh` → `build/libs/zaklecia-*.jar`.
