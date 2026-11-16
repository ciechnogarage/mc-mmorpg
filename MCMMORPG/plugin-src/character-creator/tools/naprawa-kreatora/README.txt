NAPRAWA KREATORA — czesc KREATOR (2026-09-30, galaz cc-naprawa-kreator repo MC, commity 08f63a6..11490d0)
NIEWDROZONE. Uruchomienie: bash wdroz.sh (czeka na 0 graczy, restart world1). Cofniecie: bash wycofaj.sh.

Co wchodzi:
- character-creator-0.2.0-11490d0.jar — jar TYLKO z galezi KREATOR. Galaz WYBOR scala nadzorca; jesli ma
  wejsc jar scalony, podmien go tu (w katalogu ma byc dokladnie jeden jar).
- apply.py (idempotentny) na plugins/CharacterCreator/config.yml:
  * names: wieksza pula sylab (~560-610 imion na rase) + names.blocked (filtr wulgaryzmow),
  * scene.flyover: PRZYWROCONY przelot — zniknal z configu przy cofnieciu "proste" 30.09 rano (69351e4),
  * scene.fade: 3/4/6 tickow (czern i tak trzyma sie do gotowosci kadru — kod w SceneManager#fade).
- generator/karta_kreatora.py -> tools/klasy-rasowe/: osobny skrypt po generatorze; pisze role, difficulty,
  start-skill(-level/-text) do classes.*.races.* i races.*.traits. generuj_klasy.py NIE jest ruszany ani
  uruchamiany (jego kreator() zachowuje obce klucze — sprawdzone na klonie). Po kazdej zmianie klas:
  python3 tools/klasy-rasowe/generuj_klasy.py && python3 tools/klasy-rasowe/karta_kreatora.py (idempotentny).
Kopie przed wdrozeniem: pliki/ (stary jar, config.yml.przed).
Znane: MMOProfiles profile-names max_length 20 zostaje — MMOProfiles dostaje nazwe "Postac N", imie trzyma
nasz rekord (limit 16 w NameRules), wiec 20 nie jest widoczne dla gracza.
