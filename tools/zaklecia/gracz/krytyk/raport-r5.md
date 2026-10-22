# Agent B — runda 5, recenzja builda A7

**Stan:** klon `klon-zaklecia`, Zaklęcia `428760e`, Księga `264ee80` (`ksiega-naprawy-zaklecia-2`), JAR md5 `3ba0f666241a` / `2145341974fa`, `debug:false`. Bez builda i bez zmiany klona. Runner uruchamiał jeden filtr na proces bota; `BLEDY-LOGU` oceniono osobno.

## Niezależna kontrola trzech tez A

- **Zwykłe przedmioty: potwierdzam artefakt starej C.** `b-r5-15f B5Items` 7 PASS: `/give`, rzeczywiste kliknięcia z plecaka (slot 20) do hotbar.5, wybranie slotu, pełny odczyt 41 slotów przed/po PPM. Chleb, mikstura, łuk, kilof i kamień zostają w slocie 5 z niezmienioną liczbą kopii. `map` przechodzi w `filled_map` w slocie 5, też bez zmiany liczby. Pierwsze BLAD tej sondy były błędnym przygotowaniem (źródło tylko w plecaku, niewybrany slot 5, pozostała mapa); po poprawkach kontrola dodatnia i sześć prób PASS. Regresja C na tym buildzie potwierdza wszystkie sześć.
- **Przywołany Miecz oburącz: potwierdzam artefakt starego B.** Własna `B5Mana` `b-r5-14/15b`: po zamknięciu GUI, odczekaniu filtra Q i potwierdzeniu `gotowe` obu dłoni koszt wynosi 2,0 jednej i 5,6 many oburącz, dokładnie ×2,8, dwukrotnie PASS. Pierwszy BLAD był zbyt krótkim podtrzymaniem PPM przez sondę.
- **Fuzzer 271828 × 1000: potwierdzam brak dawnego rozjazdu na nowym buildzie.** Własny `b-r5-16 B5Fuzz` 1000/1000, 0 sygnatur, 3 PASS, 0 BLAD/logów. Dawne kroki 185 i 365 miały `windowOpen:true`, więc porównanie slotów otwartego GUI z encją gracza było fałszywym alarmem harnessu. Po zamknięciu GUI sonda odpytała 41 slotów (12 niepustych), 0 różnic i naruszeń. To nie dowodzi, że `428760e` było przyczyną poprawy starego objawu.

## Recenzja commitów

- `7b62798..428760e`: jeden diff, `Nosnik.klik` po anulowaniu kliknięcia nośnika planuje `updateInventory()` po 2 tikach i sprawdza `p.isOnline()`. **Akcept warunkowy:** Księga robi `refreshLater` w następnym tiku, więc kolejność jest właściwa; zmiana nie obejmuje anulowanego `InventoryDragEvent`. Fuzzer jest czysty, lecz dawny FAIL był błędem porównania przy GUI, więc nie przypisuję mu poprawy do tego commitu. `git diff --check` = 0.
- `ec4fa21..264ee80`: jeden diff, `Bron.held` przy `event.isCancelled()` wraca bez `canonical/refreshLater`. **Akcept dla legalnych gestów:** `Wybor.kolko` anuluje już na `LOWEST`, zmienia indeks paska, nie ekwipunek; inne kliknięcia/swap mają własne odświeżenie. `A03` na poprawionej scenie PASS; łuk+lewy czar po prawdziwym kliknięciu odkłada lewy nośnik, Shift+Q/kółko/Q zachowuje łuk. Nie gwarantuje samonaprawy układu już nieprawidłowego. `git diff --check` = 0.

## Reguły rąk 107b60a i osobna wada konfiguracji

- `B5Hands` dwa przebiegi: łuk i miecz dwuręczny nie dają się założyć do lewej; prawy łuk usuwa aktywny lewy nośnik, a anulowane kółko nie psuje tego stanu. Tarcza w lewej bez czaru PASS; przypisanie prawego czaru **po tarczy** zachowuje ją. Próba założenia tarczy **po prawym czarze** nie dochodzi do skutku — zależność od kolejności, wynik gry.
- Tarcza MMOItems po kliknięciu z plecaka trafia do prawego slotu 0 i pozostaje tam po anulowanym kółku, a nawet po zwykłej zmianie slotu 0→5. Przyczyna: aktywny `KsiegaPostaci/config.yml` klona i world1 ma `typy-tylko-lewa: [OFF_CATALYST]`, więc nadpisuje domyślne `[SHIELD, OFF_CATALYST]` z 107b60a/source config. To dryf konfiguracji, nie skutek `264ee80`; sam `refresh` przy zaakceptowanej zmianie też nie usuwa tarczy.

## Regresja końcowa

- `b-r5-12a A` 9 PASS. Pierwsze A05 FAIL (`b-r5-04`) było skażeniem po sondzie dwuręcznej; czyszczenie sceny nie usuwało `netherite_sword`, poprawione przed powtórką.
- `b-r5-12b C` 19 PASS, 3 FAIL: LPM z blokiem w lewej bez kosztu many, kontrola zwykłego miecza 0 HP, Sakwa nieobecna w slocie 18. Pierwsze C (`05`) było skażone pozostałym mieczem dwuręcznym; sześć zwykłych przedmiotów PASS.
- `06 D` 5 PASS; `07 F` 4 PASS/1 SKIP (`latest.log` poza botem, runner sprawdza); `08 DB3` 7 PASS/1 FAIL lewa broń przy nośniku/5 zależnych SKIP; `09 N_melee2` 5 PASS; `10 N_damage` 3 PASS; `11 STAT3` 23 PASS.
- `12 NOISE` 4 PASS/7 FAIL: nadmiar BossBar przy otwieraniu/kółku/cyfrze/zamykaniu i 4 dźwięki strumienia; `A03` `set_slot` jest już poprawiony. `17 PW` 3 PASS: MagCel 20→15,75 bez wilka i 20→18,75 z wilkiem, bez rekurencji/logów; flaga PvP wróciła do `deny`.

**Werdykt:** Przyjmuję zmiany `428760e`/`264ee80` w sprawdzonych ścieżkach, z zastrzeżeniem drag i niesprawdzonej pełnej macierzy interakcji. Trzy moje stare zgłoszenia (przedmioty, koszt, fuzzer) były artefaktami pomiaru. Pozostają błędy reguł tarczy wynikające z konfiguracji i kolejności zakładania, trzy znane FAIL C, lewej broni DB3 oraz szum UI. Klon zostaje. Szczegóły w `../kolejka/b-r5-*.out` i `.serwer.log`.
