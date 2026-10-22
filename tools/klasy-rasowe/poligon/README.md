# Poligon klas — zaczątek (etap 1 przebudowy klas, 2026-09-30)

- `start.sh [1]` — klon world1 w kontenerze `klasy-etap0` (sieć `klasy-net`, RCON `etap0klon`); `1` = env
  `CC_TEST_WEJSCIE=1` (odblokowuje `/cc testpostac` — TYLKO klony). Katalog danych klonu wskazuje zmienna K
  w skrypcie (był w tmp joba 51c41ca8 — przy nowym klonie zbuduj dane od nowa: tar pluginów world1 bez
  Iris/userdata/baz, jar kreatora z gałęzi cc-testpostac, `test-wejscie.enabled: true`, velocity off).
- `testpostac.js` — bot wchodzi, próby z czatu (bez/z OP) muszą być zablokowane, konsola zakłada postać,
  odczyt klasy/HP/ruchu/szybkości ataku z bronią, relog. `archetyp.js` — zmienna klasa_archetyp.
  `pomiar.js`, `cmd.js` (RCON). Uruchamiać w node:20 z harnessem kreatora zamontowanym jako /e
  (MC/MCMMORPG/plugin-src/character-creator/eval) i tym katalogiem jako /s.
- Klon ma płaski świat: `forceload add 143 191 155 203` + `fill 143 97 191 155 97 203 stone` przed wejściem.
- Zasada zasobów: max 3 testowe serwery MC naraz, `docker stop` zaraz po evalu.
