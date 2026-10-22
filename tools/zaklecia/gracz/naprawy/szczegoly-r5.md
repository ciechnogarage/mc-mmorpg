# Agent A — szczegóły rundy 5

Stan roboczy bez commitów. Źródła: worktree `zaklecia-naprawy` i nakładka `naprawy/plugins-overlay`. Worktree `ksiega-naprawy-zaklecia` pozostaje bez zmian.

## 1. ConcurrentModificationException

W `Rzucanie.tick` iterator `rzucajacy.entrySet()` obejmował wywołania skillów MythicMobs. Śmiertelne trafienie synchronicznie wywołuje `PlayerDeathEvent`; `stop` usuwa wpis ofiary z `rzucajacy` i unieważnia iterator. Zastąpiłem iterację migawką kluczy i warunkowym `remove(u, s)`. Podobny wzorzec przy synchronicznym wywołaniu skilla usunąłem z pętli przywołańców `EfektyCzasowe.tik` i run `RzutyOdrzutowe.runy`. `Wybor` i ładowanie już używały migawek; `Strumien.stopWszystkie` nie wywołuje skilla podczas iteracji.

Sonda `N_cme` używa `MagPX01` i `MagPX05`: obaj rzucają, PvP `ALLOW`, cel ma 4 HP. Na buildzie A `a-r5-14` i końcowym `a-r5-31` log klona stwierdza `MagPX05 was slain by MagPX01 using [Płomienie]`; wynik PASS, sekcja `BLEDY-LOGU` pusta. Na bazie `a-r5-21` ta sama śmierć dała `ConcurrentModificationException` w `Rzucanie.tick(Rzucanie.java:409)` i nowe ostrzeżenie `Task #2270 ... generated an exception`. Wcześniejsze `a-r5-04`/`05`/`06` na `MagTest` i `MagCME` nie miały dozwolonego celu (`mmoCanTarget=false`), więc nie stanowią dowodu reprodukcji.

## 2–3. Broń i DwieBronie

`DiagnostykaObrazen` włączono wyłącznie do builda `a-r5-01`; `a-r5-02 N_melee2` potwierdził event ciosu `ENTITY_ATTACK` bez anulowania na każdym priorytecie. Faktyczny miecz MMOItems w slocie 0, z wymaganą biegłością, zadał 9,84 HP; Odmrożenie w lewej po PPM działało. Końcowy `a-r5-32` dał 20→11,636 HP z LPM i 11,636→5,808 HP z PPM, przy poborze many. Po zmianie slotu 0→5→0 miecz pozostał aktywny. Sonda B `DB3` wracała z 5 do **slotu 1** (`k.cyfra(2)`), czyli do obrazu lewej dłoni Księgi, a nie do miecza w slocie 0. `BR3M` nadawał wymaganą `bieglosc-miecz: 1`, podczas gdy `DB3` nie. Miecz `MIECZ_KAMIENNY` wymaga tej biegłości w konfiguracji MMOItems. Bazowa `N_melee2` nie dotarła do ciosu (timeout przypisania Odmrożenia), więc nie ma tu wiarygodnego FAIL→PASS samego LPM między bazą a rundą 5. Nie wprowadzałem zmiany kodu ciosu bez reprodukcji po skorygowaniu gestu i biegłości.

Wyposażenie broni w lewej przy aktywnym nośniku jest obecnie blokowane przez `Nosnik.klik`/`zamianaRak`, które anulują manipulację nośnikiem, oraz reguły `KsiegaPostaci.Bron.validPair`/`swap`, gdzie nośnik nie jest bronią. Księga zajmuje także slot 1 obrazem lewej dłoni. Warianty do decyzji właściciela:

1. Zakładanie broni do lewej odkłada zaklęcie z lewej z krótkim komunikatem; gest F z nośnikiem też zamienia tylko po świadomym odłożeniu czaru.
2. Zaklęcie i broń współdzielą lewą dłoń w stanie przełączanym; F aktywuje drugi stan, z osobnym zapisem obu przedmiotów i jasnym wskaźnikiem. To wymaga zmian w regułach nosnika i Księgi oraz testów antyduplikacyjnych.

Nie zmieniałem tej reguły bez decyzji właściciela. Gałąź `dwie-bronie` była tylko czytana.

## 4. Osłona i Uspokojenie

Skille MM już mają oprawę: `oslona.yml` odtwarza dźwięki startu/stopu oraz `enchanted_hit` w tikach, `uspokojenie.yml` ma `witch` i dźwięk gotowości. B mierzył pakiet efektu/atrybutu, a te mechaniki działają bez takiego pakietu. `N_effects` na A: zwykłe obrażenia 4 HP, przy Osłonie 2,63–2,70 HP (mana 20→18,7). Uspokojenie 20→17 many; kontrolny AI husk zadał około 4,37 HP przez 2,4 s, po rzuceniu nie zadawał obrażeń w takim samym czasie (`a-r5-13`). `NoAI` w NBT nie jest wskaźnikiem Bukkit `setAware(false)`, dlatego końcowy test porównuje obrażenia.

## 5. Dźwięki

W nakładce `plomienie.yml` i `odmrozenie.yml` pozostał jeden dźwięk startu, jeden dźwięk na tik efektu (co 10 tików) i jeden końca. Dodatkowy dźwięk `Start_Obie` zastąpiłem niewielką cząsteczką. Baza `a-r5-23 NOISE`: PPM 8, oburącz 9 dźwięków. Po pierwszym ograniczeniu `a-r5-08`: 4 i 5. Końcowy `a-r5-33`: 4 i 4. Sonda ma próg najwyżej 1 dźwięk całkowicie, więc obie komórki pozostają jej FAIL mimo spełnienia budżetu z polecenia (start + najwyżej jeden dźwięk pętli na 10 tików + koniec).

## 6. Fuzzer

`N_fuzz` odtworzył 52 akcje z `fuzz-161803.json` na `a-r5-09`: w kroku 52 serwer miał `paper ×1` w slocie 0, bot pokazywał pusty slot. W śladzie pakietowym pozostawało otwarte okno Księgi (`open_window id=5`, bez następującego `close_window`), którego dolną część `UkrywanieEkwipunku` celowo maskuje. Powtórka `a-r5-12` nie wykazała rozjazdu. Nie ma tu jeszcze dowodu, że przedmiot znika po zamknięciu okna ani który plugin wysyła błędny stan. Nie wprowadziłem zmiany mogącej uszkodzić synchronizację ekwipunku bez ustalenia źródła.

## 7. Encje po sprzątaniu

`a-r5-11 N_entities` powtórzył 20 strumieni (10/10 aktywnych) i wykonał podwójne sprzątanie. Odczyt encji tuż po nim i po sekundzie był pusty. Dwa byty z `b-3-36d` nie powtórzyły się, zatem ich typ/tag pozostaje nieustalony. W danej powtórce nie ma dowodu wycieku Zaklęć.

## Końcowe A/B

Próba uruchomienia całego filtra `ABCDEF,N_cme,N_melee2,NOISE` w jednym procesie na bazie (`a-r5-16`) zakończyła się `FATAL ERROR: ... JavaScript heap out of memory` bota (512 MB, kod 139) w grupie C. A i B zdążyły skończyć; pozostałe grupy uruchomiłem osobno w świeżych procesach. Analogicznie podzieliłem test końcowy. Ostatni build to A (`a-r5-24`, JAR Zaklęcia md5 `02623dddf32e`, Księga `feb82c47bb61`), `debug:false`.

| Grupa | Baza PASS/FAIL/BLAD | A PASS/FAIL/BLAD | Uwagi |
|---|---:|---:|---|
| A | 8/1/0 | 8/1/0 | identycznie |
| B | 28/26/2 | 24/31/1 | szeroki harness ma zmienne wyniki, m.in. timeout przypisania na obu buildach |
| C | 11/11/0 | 7/10/1 | powtórka A `a-r5-34`: 6/11/1; scenariusz `equip` często wybiera slot 1 obrazu Księgi, `N_melee2` kontroluje faktyczny slot 0 |
| D | 5/0/0 | 5/0/0 | identycznie |
| E | 2/6/0 | 2/6/0 | identycznie |
| F | 4/1/0 | 4/1/0 | identycznie |
| N_cme | 3/0/0 + wyjątek logu | 3/0/0, 0 błędów logu | baza: śmierć i CME; A: ta sama śmierć bez CME |
| N_melee2 | 1/0/1 | 5/0/0 | baza: timeout przypisania, brak pomiaru LPM |
| NOISE | 3/8/0 | 4/7/0 | strumień 8→4, oburącz 9→4 dźwięków |

Wszystkie końcowe testy A (`a-r5-25`…`34`) mają pustą sekcję `BLEDY-LOGU` dla Zaklęć i Księgi. `git diff --check` obu worktree: kod 0. Błędy grup B/C z szerokiego harnessu pozostają do odrębnej analizy; ich wyniki nie dowodzą poprawnego działania każdego gestu.

## Sprzątanie klona

Po testach wywołałem `/opt/docker/mc/narzedzia/klon.sh usun zaklecia`, zgodnie z zasadą właściciela hosta. Skrypt zakończył się kodem 1: Docker jest niedostępny z sandboxu, a `/opt/docker/mc/klony/zaklecia/data` jest tylko do odczytu. Katalog klona pozostał. Claude/nadzorca z dostępem do Dockera powinien wykonać tę samą komendę po odebraniu raportu. Nie uruchamiałem ręcznego usuwania ani zatrzymywania kontenera poza `klon.sh`.
