# Agent A — runda 6

## Zmiany (bez commitów)

- N6: `Placeholdery.java` udostępnia `%zaklecia_prawa_model%` i `%zaklecia_lewa_model%`. Zwraca ten sam identyfikator `zaklecia:<id>` co pozycja siatki Księgi; dla pustej dłoni zawsze zwraca istniejący model `zaklecia:pusta`. Konfiguracja DeluxeMenus ma teraz osobne sloty 37 (prawa) i 38 (lewa), oba z dynamicznym `item_model`, odświeżanym tekstem i dotychczasowymi poleceniami odkładania czaru. Żaden `item_model` nie jest pustym identyfikatorem.
- N7: tylko osobny `ItemDisplay` widoczny dla rzucającego startuje 0,6 bloku dalej od dotychczasowego punktu dłoni. Jego szerokość spadła z 0,42 do 0,3, maksymalna długość z 1,2 do 0,9 bloku. Obiekt widoczny dla innych i ray trace trafień pozostają bez zmian.
- `Start_Obie`: cztery cząsteczki ognia/śniegu startują 1,0 bloku przed graczem zamiast 0,4; wysokość pozostaje 1,3 bloku. Przy oku na ok. 1,62 bloku i rozrzucie poziomym 0,15 najbliższy punkt emisji jest ok. 0,85 bloku przed kamerą.

## Weryfikacja

- `a-r6-01`: build obu pluginów i restart klonu — kod 0, `KLON START OK`, Zaklecia JAR MD5 `3242dee8a327`, Księga JAR MD5 `d43c9c526572`.
- `a-r6-06`: sonda `N6panel` — kod 0. Sloty 37/38 miały modele `zaklecia:pusta` / `zaklecia:pusta`, po przypisaniu `zaklecia:plomienie` / `zaklecia:odmrozenie`, po zdjęciu ponownie oba `zaklecia:pusta`. Treść podpowiedzi odpowiadała stanowi dłoni. Otwarte menu działało przez cały przebieg. Brak błędów pluginów w logu.
- `a-r6-05`: wymagana regresja `A,N_r2` — kod 1. A01, A02, A04–A08, kontrola dodatnia i N6 przeszły. A03 poprawnie przypisał Odmrożenie do lewej dłoni, ale przekroczył próg `setSlotPerSec` (32,8 przy progu 20); ten sam wynik był w rundzie 5 (`a-r5-25`: 34,8). N4 w `N_r2` nie zobaczył pakietów bossbar `REMOVE` po Q. Oba punkty są poza zakresem polecenia r6. W logu serwera brak nowych ostrzeżeń/błędów Zaklęć i Księgi.
- N7 i emisję cząsteczek sprawdzono przez geometrię kodu/konfiguracji oraz poprawny build i kontrolę dodatnią rzucania na klonie. Ocena wyglądu w rzeczywistym kliencie pozostaje do zrzutu po stronie Claude; bot nie renderuje obrazu.

`debug: false` w źródłowym configu i nakładce. Ostatni build klonu wykonał Agent A (`a-r6-01`). Klon pozostawiony uruchomiony zgodnie z poleceniem. Bez commitów.
