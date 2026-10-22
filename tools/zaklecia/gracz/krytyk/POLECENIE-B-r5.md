# Polecenie Claude dla Agenta B — runda 5 (recenzja rundy 7 A)

Klon `klon-zaklecia` działa z buildem A rundy 7. Kolejka jak dotąd (`kolejka/b-r5-NN.req`, `test <filtr>`; NIE `build`,
chyba że A/B: `build bazowy` i potem obowiązkowo `build`). Klonu nie zwijasz.

Nowe commity (A runda 7, przegląd i commit Claude): Zaklecia `428760e` (synchronizacja ekwipunku 2 tiki po odrzuconym kliknięciu
nośnika), KsiegaPostaci `264ee80` na gałęzi `ksiega-naprawy-zaklecia-2` (= 107b60a reguły rąk z world1 + ec4fa21 filtr broni +
264ee80: bez odświeżania Księgi przy anulowanej zmianie slotu). Szczegóły A: `naprawy/szczegoly-r7.md`.

A twierdzi, że 3 z Twoich otwartych błędów to artefakty sond: (1) zwykłe przedmioty w slocie 5 — `/give` wkładał je do
zarezerwowanego slotu 0, Księga przenosiła do plecaka przed PPM (sonda `R7C`, pełne 41 slotów); (2) przywołany miecz oburącz
×2,8 działa, sonda klikała w oknie filtra Q (`R7Mana`); (3) rozjazd fuzzera nie odtwarza się na nowej bazie (190/190).
**Zweryfikuj niezależnie, bez wiary na słowo:**
1. Recenzja diffów `7b62798..428760e` i `ec4fa21..264ee80` — werdykty. Szczególnie: czy pominięcie `refresh` Księgi przy
   anulowanym `PlayerItemHeldEvent` nie zostawia złego układu rąk (reguły rąk 107b60a!) w żadnej ścieżce.
2. Twoje własne sondy na te 3 tezy (nie sondy A): przedmioty w slocie 5 wkładane tak, jak robi to gracz (z plecaka kliknięciem),
   pełny odczyt 41 slotów przed/po PPM; miecz oburącz z odczekaniem; fuzzer 271828 × 1000 kroków (pełny, nie 190).
3. Regresja na tym buildzie: A, C, D, F, DB3, N_melee2, N_damage, STAT3, NOISE, PW — osobne przebiegi.
4. Reguły rąk z world1 (107b60a): broń dwuręczna/łuk tylko w prawej, tarcza w lewej — czy zaklęcia ich nie łamią (np.
   zaklęcie w lewej przy łuku w prawej, pasek Shift+Q przy dwuręcznej).
Raport `krytyk/raport-r5.md` (sam plik, max 40 linii), -o `koniec-r5.txt`. STOP.
