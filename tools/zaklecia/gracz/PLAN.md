# Plan testu zaklęć z perspektywy gracza — etap 1

Stan odniesienia: JAR `zaklecia-0.1.0-202610031137.jar`, config klona z `data/plugins/`, Minecraft 1.21.11. Wyniki runtime pozostają **do sprawdzenia w etapie 2**. Bot bez OP; RCON tylko przygotowuje scenę i czyta stan. Skróty: P=prawa, L=lewa, B=bot, K=prawdziwy klient. Każda próba ma zapis `PASS/FAIL`, `FEEL`, próg oraz metrykę w JSON. `FAIL` bez wiarygodnego sygnału serwera nie zmienia się na PASS.

| ID | Co robi gracz | Oczekiwanie | Pomiar | Próg feelingu | Kto |
|---|---|---|---|---|---|
| A01 | Q→Umiejętności→Księga | Księga otwiera się ze zwykłego konta | pakiety okna, tytuł; slot 22 ze zlecenia, klon ma 25 | ≤40 t | B+K |
| A02 | LPM na czarze w Księdze | Czar w P, bez zniknięcia innych rzeczy | `%zaklecia_prawa%`, hash EQ | ≤8 t; ≤20 zmian slotu/s | B+K |
| A03 | Shift+LPM ★, Shift+Q, kółko, PPM | ★ pierwsze; L bez użycia komendy przypisz przez test | BossBar, placeholder, kolejność | ≤8 t; dźwięk 1/akcję | B+K |
| A04 | Pasek: ostatnia „pusta”, LPM/PPM | Dłoń staje się pusta | placeholder, item slotu | ≤8 t | B+K |
| A05 | P pusta, otwórz pasek, PPM | Wymuszony nośnik pozwala wybrać L | SelectedItem przed PPM, placeholder L | ≤8 t | B+K |
| A06 | Q przy pasku, Shift+Q ponownie | Zamknięcie, potem ponowne otwarcie | BossBar show/hide | ≤10 t | B+K |
| A07 | E i Shift+Q, Q; okno Księgi | Skróty nie rzucają i nie gubią | pakiety, hash EQ | 0 rzutów | B+K |
| A08 | Q nad slotem Księgi | DeluxeMenus zamyka się, nośnik nie wypada | okno, hash EQ | ≤10 t; 0 strat | B+K |
| B01–15 | Każdy z 15 czarów z configu, L przez Księgę i PPM | Odpowiedni efekt typu; odporny na MISS | mana, HP, encje, efekty, chat | pierwszy efekt ≤10 t; koszt config/lore | B+K |
| B16 | Każdy czar w P, LPM | Prawa reaguje zgodnie z typem | placeholder, mana, HP, encje | ≤10 t; ≤3 komunikaty | B+K |
| B17 | To samo w obu, PPM+LPM | Jeden efekt podwójny, koszt ×2,8 | mana, HP, liczba encji | ±0,1 many; ≤10 t | B+K |
| B18 | Dwa różne czary, obie ręce | Oba efekty, sumowane koszty | mana, HP, identyfikator efektu | ±0,1 many; ≤10 t | B+K |
| B19 | Mana 0 w środku trzymania | Strumień i obciążenie zgasną | mana, entity_destroy, ruch | koniec ≤8 t | B+K |
| B20 | Cel husk, gracz PvP/poza PvP, ściana, powietrze | Trafienie tylko dozwolonego celu | HP, PvP, log, odmowa | odmowa ≤8 t; bez spamu | B+K |
| B21 | Runa ×2; wilk/atronach ×2 | Limit 1, stary byt usunięty | liczba armor_stand/slug | max 1 po 8 t | B+K |
| C01 | Broń MMOItems P + czar L, LPM seria/PPM ładowanie/Shift+PPM blok | Priorytet wejścia przewidywalny, bez podwójnego kosztu | mana/stamina, HP, dźwięki | ≤1 reakcja na gest | B+K |
| C02 | Tarcza, jedzenie, mikstura, łuk w L | Vanilla używa przedmiotu, czar nie blokuje | isUsingItem, ekwipunek, mana | ≤8 t; bez przypadkowego czaru | B+K |
| C03 | Narzędzie, blok, mapa P; czar L | Interakcja bloku/mapy oraz L zgodna z regułami klienta | pakiet use/interact, mapa, stan bloku | ≤8 t; bez duplikatu | B+K |
| C04 | Dziennik slot 8, Sakwa slot 18 | Stałe sloty zachowane, poprawne menu | item ID, okno, hash EQ | ≤10 t; 0 strat | B+K |
| D01 | F w trakcie rzucania | Nośnik zostaje, skille F nie powodują dubla | placeholder, hash, mana | 0 utrat | B+K |
| D02 | Kółko/cyfry w trakcie | Strumień i koszt gasną | mana, entity_destroy | ≤8 t | B+K |
| D03 | Q/Ctrl+Q na nośniku | Nie wypada, brak ducha na ekranie | dropy, set_slot, hash | 0 dropów; korekta ≤8 t | B+K |
| D04 | E: nośnik kursorem, shift-klik, skrzynia, Sakwa | Każde przeniesienie odrzucone | hash EQ i pojemnika, cursor | 0 duplikatów | B+K |
| D05 | E/hub w trakcie, śmierć, relog ≥1 s | Zadania/strumień znikają; stan dłoni spójny | mana, encje, log, hash | koniec ≤8 t; 0 wyjątków | B+K |
| E01 | Bieg, sprint, skok, skradanie, woda | Rzut ciągły, ruch naturalny | player_input, mana, zmiana pozycji | ≤10 t; brak zgubionych tików | B+K |
| E02 | 5 LPM, spam Shift+Q | Bez mnożenia dźwięków i komunikatów | liczba chat/actionbar/sound | ≤1 sygnał na gest | B+K |
| F01 | Przytrzymaj i puść PPM | Efekt szybko startuje i gaśnie | timestamp pakietu→efekt/koniec | start ≤4 t, koniec ≤8 t | B+K |
| F02 | Obserwuj nośnik i HUD | Dłoń, placeholder i mana zgodne | set_slot/equipment, actionbar, PAPI | ≤20 zmian/s, koszt ±0,1 | B+K |
| F-ruch | Przejdź ten sam dystans bez czaru i z nim | Podczas czaru szybkość około połowy zwykłej | pozycja bot/900 ms, stosunek | 0,35–0,75 | B+K |
| F03 | Porównaj log z `log-bazowy.log` | Brak nowych wyjątków Zaklęć | różnica logów | 0 nowych ERROR | B |
| K01 | Kamera 1. osoby: poza, model, strumień, HUD | Czytelność i brak migotania | zrzut/film klienta | 0 zerwań >0,4 s | K |

**Uzasadnienie progów:** 4 tiki to odczuwalne ~200 ms na start animacji, 8–10 tików dopuszcza cykl kosztu co 10 tików i sieć. Limit 20 zmian slotu/s jest jednym odświeżeniem na tik; więcej zwykle oznacza migotanie i nadmiar pakietów. Tolerancja many 0,1 pokrywa zaokrąglenie PAPI, a zero duplikatów i błędów jest warunkiem poprawności.

**Ograniczenie klona:** Wiary i Chowance nie ładują się bez MMOProfiles (`log-bazowy.log:268,278`). Wiara daje `skill_damage` i `max_mana` (`MMOCore/attributes/default_attributes.yml:33–40`), a laska skaluje się Wiarą według zlecenia. Dlatego balans czarów i laski nie jest miarodajny na klonie. Porównanie z produkcją wymaga etapu 3. Ostrzeżenia `CHW*` w logu bazowym są tłem, nie nowym błędem Zaklęć.

**Rozbieżność wejścia:** zlecenie wskazuje slot 22 w `hub_umiejetnosci`, lecz klon ma Księgę w slotach 25/26/34/35 (`hub_umiejetnosci.yml:43`). Bot najpierw próbuje 22, następnie 25, a rozbieżność zapisuje jako obserwację A01.
