# Agent B — runda 3, raport końcowy

**Stan 12.10.2026:** testy rundy 3 wykonano na klonie `klon-zaklecia`. Kontrolny build `c-01` potwierdził źródła Zaklęcia `b8a73aa` (0 zmian) i Księga `1433dfb` (0 zmian), JAR-y md5 `db09862ac082` i `1621ad3e757c`, `debug:false`. Stare wyniki `b-3-01`…`b-3-10` pochodzą z tych samych commitów A. Próby `b-3-20`… wykonał wspólny bot, a nowe błędy logu podał runner w sekcji `BLEDY-LOGU`.

**Werdykt:** akceptuję poprawkę N2 i regułę slotu 0 w sprawdzonym zakresie. Odrzucam obecną integrację DwieBronie z Zaklęciami: LPM z prawym mieczem i lewym czarem nie zadaje obrażeń, przełączenie chowa miecz, a lewego miecza nie udało się wyposażyć zwykłymi gestami. Nowy blocker to `ConcurrentModificationException` w `Rzucanie.tick` przy śmierci rzucającego w walce wielu graczy. Potwierdziłem 10 i 20 trwałych strumieni przy `debug:false`, z zastrzeżeniem zmiennego obciążenia hosta i dwóch nieustalonych encji po ostatnim sprzątaniu. Trzy ziarna fuzzera wykonały po 1000 kroków; jedno dało odtwarzalną rozbieżność ekwipunku. Proszę Claude o osobny build bazowy do A/B wydajności.

## Recenzja końcowych diffów A

Porównałem `af49d5b..b8a73aa` i `1e1c108..1433dfb`; oba `git diff --check` mają kod 0. **AKCEPT N2 w sprawdzonym zakresie:** `Bron.damage` traktuje brak metadanych jak zwykły cios; dopuszcza wyjątek tylko przy jawnych metadanych innych niż `MeleeAttackMetadata`. Zasada broni nie zależy już od kolejności listenerów MythicLib. W A/B `a-r4-02` baza → `a-r4-04` A Płomienie z mieczem w slocie 5 przeszły z 20→17 HP (sam ogień) na 20→9,9632; Odmrożenie z 20→20 na 20→11,2580. Zwykły cios slot 5 pozostał 20→20, slot 0 20→18,12. Na końcowym buildzie `a-r4-16`: `N_damage`, `N_invalid`, `N_melee` po 3/3 PASS. `BR3` potwierdziło pocisk/kulę z mieczem slot 5 (20→15,818 i 20→14,145201 przy poborze many). Każdy czar z mieczem slot 5 i skille klas MMOCore wymagają jeszcze osobnej kontroli.

**AKCEPT statycznie N9/N4, wykonawczo z ograniczeniami:** `Nosnik` i komenda walidują id przed usunięciem starego zaklęcia; `N_invalid` PASS. `Skille.cast/cele/punkt` zwracają boolean MythicMobs, `RzutyOdrzutowe` oddaje manę po `false`; nie ma sondy przypadku `castSkill=true` bez efektu. `Wybor` wiąże opóźniony LPM z konkretną sesją i zamyka pasek na quit/death/world change, `Rzucanie` czyści stan po zmianie świata. N5 `support.party_other:true` jest decyzją właściciela i zostaje. **N7 warunkowo:** drugi `ItemDisplay` dla rzucającego jest krótszy/węższy i w kodzie usuwany razem z pełnym. Każdy strumień tworzy dwa byty i teleportuje oba co tik; bez MSPT/pakietów przy load <8 i klienta graficznego nie oceniam wydajności ani wyglądu.

## N3, DwieBronie i reguła slotu 0

Trzy diagnostyczne C A (`a-r4-06..08`) wskazały, że `KsiegaPostaci.Bron.refresh` rezerwuje slot 0 na broń: zwykły przedmiot przenosi do plecaka, a `Nosnik.odswiezPusta` dopiero potem wstawia nośnik w pusty slot. Zgadza się to z `Bron.java:316–323` oraz `ZAKDBG slot/pusta`. `N3B` bez awaryjnego cyklicznego przełączania slotów miało 3/3 PASS: chleb, kamień i łuk pozostały w slocie 5 po PPM. **Feeling N3: 3/10** — gracz nie dostaje jasnego wyjaśnienia, czemu przedmiot ze slotu 0 znika z dłoni do plecaka, a pojawia się nośnik.

Pierwotne `DB3` wykonało 12 gestów, lecz miało wadliwą kontrolę wyposażenia; odrzucam 11 PASS/1 FAIL jako dowód integracji. Poprawione `DB3R` (`b-3-21`) potwierdziło prawdziwy miecz MMOItems w prawej dłoni, ale nie udało się go przenieść do lewej: po kliknięciu slotu 45 miecz pozostał w plecaku, offhand był pusty. `DB3F` (`b-3-25a`) także nie doprowadziło do offhandu: próba wyposażenia w slot 1 została przechwycona przez ikonę Księgi, więc F nie zamieniło broni. **Układ broń lewa + zaklęcie prawa nie jest dostępny przez sprawdzone zwykłe gesty**; nie wyciągam z niego wniosków o działaniu PPM/LPM po wyposażeniu.

`DB3S` (`b-3-25`) sprawdziło 15 czarów × 2 orientacje. Wszystkie 15 prób z mieczem w prawej dłoni utrzymało broń po rzuceniu z lewej; nie każdy czar rzeczywiście zadziałał (np. Wielkie uzdrowienie w tej sondzie miało manę 20→20 i HP 14→14,2), więc PASS oznacza tylko brak utraty miecza. Wszystkie 15 prób z mieczem w lewej zatrzymało się przed rzutem na kontroli wyposażenia.

Poprawione `DB3` (`b-3-28f`, load 4,95) przy potwierdzonym mieczu w prawej i Odmrożeniu w lewej: PPM rzuca (HP huska 20→14,171974; mana 20→19,2), lecz jawny atak LPM w nowo zespawnowanego, potwierdzonego huska zadaje **0** (20→20), choć `BR3M` tym samym mieczem bez zaklęcia w offhand zadało 9,84 HP. To jest powtarzalny konflikt gestu/filtra ciosu, nie zgubiony cel. Shift+Q, F i śmierć pozostawiają miecz wybrany; po przełączeniu na slot 6 i z powrotem wybranym przedmiotem jest nośnik, więc miecz znika z aktywnej dłoni. Te dwa zachowania **ODRZUĆ** jako integrację DwieBronie × Zaklęcia.

## Kontrola aparatury na nowym klonie

`c-02` i `b-3-20` miały fałszywy niezmiennik BossBar: MythicHUD zamyka pasek pustym `ADD` na tym samym UUID zamiast `REMOVE`. Po poprawce `b-3-23` nie wykazał już takiego naruszenia. Metryka `setSlotPerSec` rozróżnia teraz pakiety okna Księgi i ekwipunku gracza. W `b-3-23` A02 ma 33 pakiety okna oraz 1 pakiet gracza (1,5/s) i PASS; A03 ma 13 pakietów gracza (32,7/s), zero okna i nadal przekracza próg 20/s. To rzeczywisty szum aktualizacji przy wyborze z paska, a nie przerysowanie menu. Nowe błędy Zaklęć/Księgi w `BLEDY-LOGU` dla tych prób: zero.

## STAT3 i próby broni

`STAT3` na buildzie A: Płomienie mediana 6,6912 HP (Wiara 0) → 11,414402 (40); Odmrożenie 5,828026 → 9,9876. PAPI `skill_damage` -15→45, czyli +60 punktów procentowych. Jedna próbka nie miała poprawnego wybranego miecza; pozostałe obrażenia są identyczne z bazą `STAT2`. Poprawka N2 nie usunęła skalowania Wiary. Izolowana próba r2 przy `skill_damage` 0→60 wykazała 1,60× (`skalowanie-r2.md`).

`BR3`: miecz slot 5 0 obrażeń, łuk slot 5 0, pocisk/kula ze slotu 5 działają. Wcześniejsze próby miecza i łuku w slocie 0 miały pusty `SelectedItem` i nie były rozstrzygające. Poprawione `BR3M` (`b-3-24`) potwierdziło faktycznie trzymany miecz MMOItems z modelem `bron:s100c/stone_sword`: dwa ciosy ze slotu 0 zadały 9,84 HP obrażeń (20→10,16), ze slotu 5 zero (20→20), obie kontrole PASS. `BR3B` (`b-3-34`) potwierdziło wybrany łuk MMOItems i zasadę slotów: strzał ze slotu 0 zadał 5,0184 HP (20→14,9816), ze slotu 5 zero (20→20), obie kontrole PASS. Klasowe skille MMOCore nie zostały wykonawczo sprawdzone.

## Szum — 10 akcji, tylko dwa UUID pasków Zaklęć

`b-3-10 NOISE` liczy pakiety `boss_bar` tylko dla dwóch UUID wykrytych po tytule Zaklęć; inne paski MythicHUD są wyłączone. Liczby to pakiety przez 300 ms po akcji, także puste `ADD` używane przez MythicHUD jako zamknięcie.

| Akcja | Pakiety pasków | Dźwięki | Komunikaty |
|---|---:|---:|---:|
| Otwórz pasek | 16 | 1 | 1 |
| Kółko | 4 | 1 | 0 |
| Cyfra | 4 | 1 | 0 |
| F | 8 | 0 | 0 |
| Zamknij pasek | 4 | 1 | 0 |
| Przypisz Płomienie | 8 | 0 | 1 |
| PPM strumień | 8 | 8 | 0 |
| Przypisz prawa | 2 | 1 | 1 |
| Oburącz | 0 | 9 | 0 |
| Zmień slot | 0 | 0 | 0 |

Wszystkie aktualizacje miały action 0 (`ADD`), także przy pustym tytule. Otwieranie i przewijanie paska jest pakietowo głośne; PPM/oburącz dają 8–9 dźwięków na krótką akcję. `NOISE` ma 9 FAIL według progu ≤1, 1 PASS — to ocena szumu, nie awaria efektu.

## Opóźnienia startu i stopu, n=10 na komórkę

Start liczony od pakietu gestu do pierwszego widocznego znacznika efektu, stop do ostatniego; wyniki w tikach (50 ms), mediana/p95. Dla leczenia znacznikiem jest wzrost HP, dla tarczy efekt/atrybut, dla koncentracji ostatni pakiet efektu, dla przywołań spawn encji. „—” oznacza brak takiego znacznika, nie zero opóźnienia. `LAT1` powtórka `b-3-28b` (load 3,38→3,08), `LAT2` `b-3-28c` (5,15→9,42), przywołania poprawiona scena `b-3-28h` (5,32→5,07), `LAT3` powtórka `b-3-28d` (7,01→5,64). W trakcie `LAT2` i `LAT3` load chwilami przekraczał 8; ich mediany są obciążone warunkami hosta.

| Zaklęcie | Gest | Start n | Start med/p95 | Stop n | Stop med/p95 |
|---|---|---:|---:|---:|---:|
| plomienie | prawa | 10 | 4/5 | 10 | 21/22 |
| plomienie | lewa | 10 | 1/1 | 10 | 24/24 |
| plomienie | obie | 10 | 1/1 | 10 | 22/22 |
| odmrozenie | prawa | 10 | 5/5 | 10 | 22/22 |
| odmrozenie | lewa | 10 | 1/1 | 10 | 24/24 |
| odmrozenie | obie | 10 | 1/1 | 10 | 22/22 |
| ognisty_pocisk | prawa | 10 | 11/11 | 10 | 41/41 |
| ognisty_pocisk | lewa | 10 | 31/31 | 10 | 41/41 |
| ognisty_pocisk | obie | 10 | 32/33 | 10 | 32/33 |
| kula_ognia | prawa | 10 | 17/17 | 10 | 37/37 |
| kula_ognia | lewa | 10 | 39/39 | 10 | 39/39 |
| kula_ognia | obie | 10 | 36/36 | 10 | 36/36 |
| leczenie | prawa | 10 | 5/5 | 10 | 18/27 |
| leczenie | lewa | 10 | 0/1 | 10 | 21/26 |
| leczenie | obie | 10 | 0/1 | 10 | 20/25 |
| runa_ognia | prawa | 10 | 16/16 | 10 | 16/16 |
| runa_ognia | lewa | 10 | 36/36 | 10 | 36/36 |
| runa_ognia | obie | 10 | 37/38 | 10 | 37/38 |
| oslona | prawa | 0 | — | 0 | — |
| oslona | lewa | 0 | — | 0 | — |
| oslona | obie | 0 | — | 0 | — |
| przywolany_miecz | prawa | 7 | 0/1 | 0 | — |
| przywolany_miecz | lewa | 10 | 0/0 | 0 | — |
| przywolany_miecz | obie | 10 | 0/0 | 0 | — |
| duch_wilka | prawa | 10 | 12/12 | 10 | 12/12 |
| duch_wilka | lewa | 10 | 32/32 | 10 | 32/32 |
| duch_wilka | obie | 10 | 33/33 | 10 | 33/33 |
| atronach_ognia | prawa | 10 | 12/12 | 10 | 12/12 |
| atronach_ognia | lewa | 10 | 32/32 | 10 | 32/32 |
| atronach_ognia | obie | 10 | 33/34 | 10 | 33/34 |
| leczenie_dloni | prawa | 10 | 5/5 | 10 | 21/27 |
| leczenie_dloni | lewa | 10 | 1/1 | 10 | 23/28 |
| leczenie_dloni | obie | 10 | 1/1 | 10 | 21/28 |
| uspokojenie | prawa | 0 | — | 0 | — |
| uspokojenie | lewa | 0 | — | 0 | — |
| uspokojenie | obie | 0 | — | 0 | — |
| uzdrowienie_innego | prawa | 10 | 10/12 | 10 | 12/12 |
| uzdrowienie_innego | lewa | 10 | 7/18 | 10 | 32/32 |
| uzdrowienie_innego | obie | 10 | 10/19 | 10 | 33/34 |
| zasklepienie_ran | prawa | 10 | 6/13 | 10 | 14/14 |
| zasklepienie_ran | lewa | 10 | 10/19 | 10 | 36/36 |
| zasklepienie_ran | obie | 10 | 9/19 | 10 | 33/34 |
| wielkie_uzdrowienie | prawa | 10 | 6/18 | 10 | 24/24 |
| wielkie_uzdrowienie | lewa | 10 | 9/18 | 10 | 44/44 |
| wielkie_uzdrowienie | obie | 10 | 11/20 | 10 | 45/46 |

Osłona pobiera manę (np. 20→19,3), lecz nie ma pakietu efektu/atrybutu w żadnym z 30 powtórzeń. Uspokojenie pobiera manę (np. 20→17,2), lecz na nieruchomym husku nie daje obserwowalnego znacznika. Przywołany miecz czasem pokazuje zmianę slotu, ale nie daje mierzalnego końca. Pierwsza scena przywołań celowała w huska i nie rzucała czarów; te próby odrzuciłem, a powtórka z wolnym miejscem ma 10/10 w sześciu komórkach.

## Wydajność

Pierwsze `PERF2` z 20 oddzielnymi botami odrzucił limit `max-players=20`. Następne próby wykorzystywały 10 postaci z jedną lub dwiema dłońmi i jednego obserwatora. Początkowe wyniki odrzuciłem: mur sceny przecinał promień pierwszego bota, a przy dwóch dłoniach boty zabijały się nawzajem. Po usunięciu muru `b-3-35e PERF10` przy load 6,75→6,28 utrzymał **10/10 pojedynczych strumieni**; każdy bot zużył 8,8 many w 10 sekund. Obserwator otrzymał 45,6 pakietów/s w bezczynności i 237,0 pakietów/s podczas rzucania. Dominowały `world_particles` 127,4/s i `sound_effect` 45,5/s. Pięciosekundowa średnia MSPT wzrosła z 7,3 do 13,3; maksimum 52,7 ms.

`b-3-36b PERF20` przy load 3,95→2,53 (start próbki 2,90) utrzymał **9/10 postaci rzucających oburącz**, czyli co najmniej 18 trwałych strumieni; dziesiąta straciła tylko 1,6 many. Przyczyną była śmierć botów od ognia innych botów, opisana niżej. Po nadaniu odporności wszystkim botom `b-3-36d` przy load 5,60→5,09 (start próbki 5,75) utrzymał **10/10 postaci i 20/20 strumieni**; każdy zużył 18,4 many w 10 sekund. Obserwator miał 44,1 pakietów/s w bezczynności i 331,3/s w próbce. Dominowały `world_particles` 198,5/s i `sound_effect` 43,9/s. Pięciosekundowa średnia MSPT wzrosła z 3,7 do 7,7, maksimum 32,7 ms. Mimo load 1-minutowego <8, load 5-minutowy na hoście wynosił 8,56 przy starcie próbki; dlatego nie traktuję różnicy MSPT względem `PERF10` jako precyzyjnego skalowania. Spark zapisał profil m.in. w `plugins/spark/profile-2026-10-12_16.34.19.sparkprofile`; RCON nie zwrócił ścieżki.

**Nowy błąd krytyczny z próby 20:** w `b-3-36` i `b-3-36b` log odnotował śmierć `MagPX05` od Płomieni `MagPX01`, a w tym samym czasie `java.util.ConcurrentModificationException` w `pl.mcmmorpg.zaklecia.Rzucanie.tick(Rzucanie.java:418)`. Pętla iteruje po `rzucajacy.entrySet()`, a `smierc(PlayerDeathEvent)` wywołuje `stop`, który usuwa wpis z tej mapy. Zdarzenie powtórzyło się w `b-3-36c`, lecz tę próbę wyłączam z pomiaru wydajności, bo load przed próbką przekroczył 8. Ten wyjątek przerywa co najmniej bieżący tik Zaklęć i wymaga naprawy A; dalszego działania zadania nie oceniłem. Nie przypisuję częściowego poboru many samej wydajności: boty padały od ognia innych botów. `b-3-36d` z odpornością nie wykazał nowych błędów Zaklęć/Księgi w `BLEDY-LOGU`, lecz kontrola sprzątania zwróciła **2 nieustalone encje w arenie** po podwójnym `kill`; przyczynę tego FAIL trzeba ustalić osobno.

**Porównanie z `build bazowy`: proszę Claude o osobne zlecenie bazowego buildu.** Nie przełączałem wersji klona w tej rundzie.

## Fuzzer

| Ziarno | Kroki | Wynik | Odtworzenie |
|---|---:|---|---|
| 271828 (`b-3-33`, czysta powtórka) | 1000/1000 | 1 sygnatura `stone_sword` ×1 serwer / pusty klient, 4 potwierdzone naruszenia, 0 przejściowych | Prefiks 810 kroków; pełny ślad zapisany, minimalizacja pominięta przez limit kosztu |
| 314159 (`b-3-31`) | 1000/1000 | 3 krótkie rozbieżności slotu 0 (`stone_sword` ×1, `paper` ×2/×1) | Pierwsze dwie nieodtworzone po pełnych prefiksach 510 i 575 akcji |
| 161803 (`b-3-32`) | 1000/1000 | 3 sygnatury; 10 potwierdzonych naruszeń, 0 przejściowych po odczycie +250 ms | `paper` ×1 serwer / pusty klient **odtworzone**, ślad skrócony z 70 do 52 akcji |

Dla `271828` pełny ślad jest w `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/fuzz-271828.json`; dla `161803` pełny ślad i skrócona sekwencja są w `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/fuzz-161803.json`. Sekwencja zawiera relogi, śmierć i zmianę dłoni. Ta rozbieżność jest odtwarzalnym błędem synchronizacji ekwipunku (nie samą chwilową kolejnością pakietów). Pojedynczy brak zamknięcia BossBar po Q przy kroku 995 wymaga osobnej kontroli; wcześniejsze błędy „brak REMOVE” zostały usunięte przez rozpoznawanie pustego `ADD`. Fuzzer 314159 i nowsze próby wykonują każdą akcję, sprawdzają niezmienniki co piąty krok oraz bezpośrednio po ryzykownych gestach; w 161803 rozbieżność była ponownie odczytana po 250 ms. Ta metoda może pominąć krótsze stany między kontrolami. Wszystkie trzy końcowe ziarna miały 0 nowych błędów Zaklęć/Księgi w `BLEDY-LOGU`. Stary `b-3-30` zakończył 1000 kroków, ale miał błąd samej akcji `bron_lewa`; zastąpiła go czysta powtórka `b-3-33`.

## Otwarte sprawy

- Sojusznik zasłaniający stożek: wcześniejsza próba wykazała identyczne obrażenia huska za sojusznikiem i bez niego; decyzja projektowa właściciela. Leczenie neutralnych graczy w PvP zostało już przez właściciela zatwierdzone.
- Skille klas MMOCore wymagają osobnej sondy wykonawczej; przetestowałem za to zwykłe ciosy, pociski Zaklęć i łuk MMOItems względem zasady slotu 0.
- Pojedynczy przypadek BossBar po Q w FZ6 wymaga śladu pakietowego. N7 wymaga rzeczywistego oglądu w kliencie graficznym.
- Wyjątek `ConcurrentModificationException` przy śmierci rzucającego wymaga poprawki A; dwa byty pozostałe po sprzątaniu w `b-3-36d` wymagają identyfikacji. Osobny `build bazowy` i porównanie A/B wymaga zlecenia Claude.
- `krytyk/jary` zawiera tylko `.txt` (652 KB); dysk `/` ma 29 GB wolnego.

## Feeling — ocena 1–10

Oceny łączą działanie czarów, gesty, szum `NOISE` i pomiar LAT. Nie obejmują wyglądu animacji oglądanej w kliencie graficznym. Część czasu LAT była obciążona skokami load >8, więc oceny nie są precyzyjnym pomiarem płynności.

| Zaklęcie | Typ | Ocena | Przyczyna |
|---|---|---:|---|
| Płomienie | koncentracja | 7 | Obrażenia są potwierdzone, lecz 8 dźwięków przy krótkim PPM męczy. |
| Odmrożenie | koncentracja | 7 | Po N2 znowu trafia; powtarzany PPM dzieli szum strumieni. |
| Ognisty pocisk | ładowany | 6 | Trafia z mieczem w slocie 5; w lewej ręce znacznik startu dopiero po medianie 31 tików. |
| Kula ognia | ładowany | 5 | Duży efekt i koszt około 5, lecz w lewej ręce znacznik startu dopiero po 39 tikach. |
| Leczenie | koncentracja | 7 | Stopniowy wzrost HP jest czytelny; spam przy trzymaniu przycisku pozostaje. |
| Runa ognia | runa | 5 | Podłożenie działa; lewa/obie dłonie dają znacznik po 36–38 tikach i wymagają celowania w podłoże. |
| Osłona | tarcza | 3 | Mana schodzi, ale w 30 próbach brak widocznego znacznika efektu lub końca; siły ochrony nie potwierdzono. |
| Przywołany miecz | broń | 4 | Pojawienie się w slocie bywa widoczne, brak mierzalnego końca; z prawdziwą bronią w lewej układ niedostępny. |
| Duch wilka | przywołanie | 5 | Spawn i limit działają; sonda PvP-wilk potrafiła przeciążyć klon. |
| Atronach ognia | przywołanie | 6 | Spawn i limit potwierdzone, podobny problem celowania w miejsce. |
| Leczenie dłoni | koncentracja celowana | 5 | Sojusznika leczy, ale brak celu daje mało informacji o przyczynie braku efektu. |
| Uspokojenie | na cel | 3 | Pobiera manę, ale na nieruchomym husku w 30 próbach nie ma widocznego znacznika efektu. |
| Uzdrowienie innego | ładowany celowany | 6 | Realnie leczy sojusznika; bez celu nie pobiera many, lecz sygnał jest słaby. |
| Zasklepienie ran | ładowany | 7 | Pełne leczenie 14→20 po naładowaniu, stabilniejszy efekt niż małe tikowe uzdrawianie. |
| Wielkie uzdrowienie | ładowany | 5 | Pełne leczenie, lecz w lewej/obu dłoniach stop po medianie 44–45 tików od gestu. |

| Czynność | Ocena | Uzasadnienie |
|---|---:|---|
| Otwórz pasek | 5 | Jasna podpowiedź i dźwięk, ale 16 pakietów do dwóch pasków. |
| Przewiń kółkiem | 5 | Jeden dźwięk, cztery ADD pasków na ruch. |
| Wybierz cyfrą | 5 | Jak kółko; przy szybkim wyborze dużo aktualizacji. |
| F w pasku | 4 | Osiem aktualizacji paska, brak dźwięku potwierdzenia. |
| Zamknij pasek | 5 | Dźwięk jest, lecz puste ADD wymaga specjalnego rozpoznania zamknięcia. |
| Przypisz zaklęcie | 6 | Komunikat potwierdza wybór; osiem pakietów pasków przy pierwszym przypisaniu. |
| Rzuć lewą dłonią PPM | 6 | Efekt działa, lecz krótki strumień generuje osiem dźwięków. |
| Rzuć oburącz | 5 | Większy koszt/efekt, dziewięć dźwięków; część typów oburącz nie działała też na bazie. |
| Zmień slot | 7 | W NOISE brak dodatkowego dźwięku/komunikatu/paska. |
| Użyj zwykłego przedmiotu w slocie 0 | 3 | Księga po cichu przenosi go do plecaka i odsłania nośnik. |

**10 największych irytacji (kolejność):**

1. Miecz w prawej dłoni i czar w lewej: PPM rzuca, ale dwa potwierdzone ataki LPM nie zadają obrażeń.
2. Miecza MMOItems nie udało się wyposażyć w lewej dłoni przez slot ekwipunku ani F przy aktywnym czarze.
3. Po przełączeniu slotu i powrocie miecz w prawej dłoni ustępuje nośnikowi bez jasnego komunikatu.
4. Zwykły przedmiot w slocie 0 po PPM trafia do plecaka, a w dłoni pojawia się nośnik.
5. Osłona pobiera manę bez widocznego w pakietach znacznika działania i zakończenia (30 prób).
6. Uspokojenie pobiera manę, lecz na nieruchomym celu nie daje rozpoznawalnego znacznika (30 prób).
7. Krótki strumień i oburącz generują po osiem–dziewięć dźwięków, które mogą zagłuszać walkę.
8. Otwieranie paska wysyła 16 aktualizacji dwóch UUID; pojedyncze przewinięcie lub cyfra po cztery.
9. W lewej dłoni Ognisty pocisk, Kula ognia i Runa ognia mają znacznik startu po medianie 31–39 tików.
10. Osiem z piętnastu opisów nie ma jawnego wiersza `Koszt:`, a czary celowane bez celu słabo wyjaśniają brak efektu.

Ocena wizualna N7 nadal wymaga prawdziwego klienta graficznego. Integrację DwieBronie oceniam 2/10: prawy miecz przetrwa rzucenie z lewej, lecz cios LPM nie trafia w poprawnej scenie, przełączenie chowa miecz, a lewego miecza nie udało się wyposażyć zwykłymi gestami.
