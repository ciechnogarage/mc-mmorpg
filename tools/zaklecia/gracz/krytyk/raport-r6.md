# Agent B — runda 6, niezależna recenzja A9

**Baza:** klon `klon-zaklecia`, Zaklęcia `3c60585` (JAR MD5 `31d0832b9f6c`), Księga `76d0cec` (`11ff4ed2905b`); bez builda. Dwa żądane diffy mają `git diff --check` = 0. Wyniki: `../kolejka/b-r6-*.out` i `.serwer.log`.

## Bezpieczeństwo przedmiotów — AKCEPT

- `Nosnik.klik` przepuszcza wyłącznie wejście zwykłego przedmiotu na lewą pozycję/kursor lub F do walidacji Księgi; pozostałe dotknięcia nośnika nadal anuluje. `Bron.allowed/validPair` traktuje lewy nośnik jako pusty tylko dla planu ruchu. `putAwayLeftSpell` wykonuje się po pozytywnej walidacji, przed zapisem broni, usuwa znacznik i daje jeden komunikat. Przejrzałem `clickWeapon`, `hotbarSwap`, `shift`, `swapLeft`, `swapRight/moveSpareLeft`, `PlayerSwapHandItemsEvent` i `SWAP_OFFHAND` przy kliknięciu innego slotu. Nie stwierdziłem ścieżki, która usuwa realny przedmiot przed odmową.
- Własna `B6Safety` (`b-r6-22`): przeciągnięcie w E, F (miecz z prawej do lewej; prawa pusta), 8 szybkich F, shift-klik, numer, zapas lewej i wymiana zestawu PASS. Pełna liczba realnych mieczy przed/po stała, odczyt serwera = klienta, lewy czar odłożony. Łuk i miecz dwuręczny do lewej odrzucone: broń zachowana, nośnik/zaklęcie nadal aktywne. Wcześniejszy FAIL zestawu `b-r6-20/21` był sondą wybierającą ponownie pierwszy miecz z zapasu; po wskazaniu drugiego źródła przygotowanie zawierało oba zapasy i próba PASS.
- Własny fuzzer `b-r6-16/17/18`: ziarna **271828, 314159, 161803 × 1000/1000**, każde 0 naruszeń i 0 nowych błędów pluginów. Pula obejmowała przypisanie lewego czaru komendą i Księgą, kursor, F, shift, numer, lewy zapas, wymianę zestawu, odrzucony łuk/dwuręczną, śmierć i 3 relogi na ziarno. Stałe realne `3×stone_sword + 1×netherite_sword + 1×bow`; po zamknięciu GUI pełny odczyt 41 slotów: 0 różnic serwer–klient. Fuzzer pomija klony wizualne i nośniki.
- Pierwsze przebiegi `b-r6-11..13b` są nieważne: błędna próbka przed synchronizacją `/mi give`, stara nazwa reguły śmierci oraz dwa OOM procesu bota przy nieograniczonych pakietach. OOM pozostawił sesję `MagTest` w stanie `CLOSING`; finalne przebiegi użyły odrębnych kont i ograniczonego bufora zdarzeń. Nie przypisuję tych awarii pluginom.

## Zasłanianie — ODRZUĆ obecną regułę pięciu promieni

- Własna `B6Occlusion` (`b-r6-10a`): husk za własnym wilkiem 0 obrażeń przy celowaniu w środek, **6,6912** w odsłoniętą głowę. Dwóch graczy przy PvP `allow`: tylny bez obrażeń za przednim, po odsunięciu przedniego **6,7**; flaga przywrócona. Statycznie `Strumien.tick` nadal skraca model do pierwszej przeszkody na osi, także gdy celowanie w głowę trafia huska.
- `B6Edge` (`b-r6-19`) po wyciszeniu strumienia przed każdą próbą: husk przy x=0,95 za murem 0, bez muru **11,808**; x=0,975 za murem 0; x=1,0 za murem **11,808** w dwóch powtórkach. Przy x=1,1 mur 0, bez muru **11,808**. To próg zależny od pięciu stałych punktów: widoczny skraj sylwetki może być nietrafialny mimo skierowania celownika na ten skraj. Krok położenia 0,025 bloku zawęża granicę; nie wyznacza jej w pikselach dla konkretnej rozdzielczości.
- Statycznie `Zaslanianie.pierwszyCel` celuje w środek/górę/dół/boki po 0,4 szerokości hitboxu; nie próbuje skrajnej widocznej powierzchni. Dla x=0,95 prawa krawędź hitboxu huska jest około x=1,25 i widać ją zza bloku kończącego się na x=1, lecz skrajny próbkowany punkt leży około x=1,19 i jego promień przecina blok. Kandydat jest też najpierw filtrowany po kącie środka (`Rzucanie.stozek`). Własne przywołania są przeszkodą (`wlasne-przywolania-zaslaniaja: true` w klonie). Nie potwierdziłem przenikania przez pełny mur; odrzucenie dotyczy zbyt trudnego trafienia widocznej części.

## Regresja osobnymi procesami

- `b-r6-01 A` 9 PASS; `02 C` 20 PASS/2 FAIL: zwykły kamienny miecz 0 HP (znany z bazowego A9), test Sakwy/slotu 18 poza zakresem tej rundy; `03 D` 5 PASS; `04 F` 4 PASS/1 SKIP (log sprawdził runner).
- `05 DB3` 13 PASS; `06 N_ppm1` 3 PASS; `07 N_damage` 3 PASS; `08 STAT3` 23 PASS; `09 PW` 3 PASS. Nowe `BLEDY-LOGU` pluginów we wszystkich dziewięciu przebiegach: 0.

**Werdykty:** bezpieczeństwo odkładania lewego czaru **AKCEPT** w sprawdzonych drogach, z zachowaniem dalszych testów interakcji; zasłanianie własnym wilkiem/graczem **AKCEPT**, lecz całe `3c60585` w regule „widoczna część” **ODRZUĆ** do poprawy próbkowania skraju. Klon pozostaje uruchomiony; `keep_inventory=false`, PvP przywrócone do `deny` (`b-r6-24`). STOP.
