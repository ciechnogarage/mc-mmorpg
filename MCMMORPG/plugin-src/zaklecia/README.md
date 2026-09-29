# Zaklecia — ciche zasoby MMOCore dla zaklęć MythicMobs

Mechanika `zakZasob{zasob=mana|stamina;a=<ile>}` (zabiera zasób celowi-graczowi) i warunek
`zakMaZasob{zasob=..;a=..}` (czy ma co najmniej tyle). Zastępują `mmocore admin resource-mana take`,
które przy każdym wywołaniu wpisuje linię do logu serwera. Używane przez zaklęcia w dłoni
(Płomienie, Odmrożenie) — konfiguracja, wdrożenie i testy: `world1/tools/zaklecia/README.md`.

Build: `./build.sh` (gradle w kontenerze, jary world1 tylko do odczytu) → `build/libs/zaklecia-*.jar`.
Wymaga restartu serwera; po nim `tools/zaklecia/wdroz.sh --bez-packa` przełącza skille na wariant cichy.
