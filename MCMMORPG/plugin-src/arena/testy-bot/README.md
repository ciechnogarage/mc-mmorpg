# Testy botem (Claude, 17.10)

Uruchom: `/opt/docker/mc/narzedzia/bot/run.sh <klon> $PWD/testy-bot/<plik>.js` (klon z `klon.sh start <n> --z-kreatorem` + `wdrozenie/klon-przygotuj.sh <n>`).
Kazdy scenariusz konczy sie 11 s czekania - wylogowanie w walce daje 60 s zakazu ringu i psuje nastepny przebieg.
Pulapki: Ksiega liczy cios/strzal tylko ze slotu 0; CMI daje 5 s nietykalnosci po teleporcie (czekaj 6,5 s);
klasa Human nie ma many; z klasa maga bot nie rzuca ognistego pocisku (MMOCore przejmuje klik) - pocisk sprawdzac klientem.
