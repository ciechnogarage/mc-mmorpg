# Agent A — runda 10

Stan rundy 10 przed rozszerzeniem 10b: **część poprawki gotowa, próg skrajnej widoczności blokował warunek MythicMobs poza ówczesnym zakresem**. Bez commitów. Zmienione w worktree Zaklęć tylko `Zaslanianie.java`, `Strumien.java`, `Rzucanie.java`; worktree Księgi czyste. Dodałem dwie sondy w `bot/scenariusze/`: `N_zaslona_r10.js` i `R10Cost.js`. Nakładkę debug przywróciłem do `debug: false`; finalny build r10 `a-r10-z6-final-build` zakończył się kodem 0, `KLON START OK`, Zaklęcia MD5 `8596791f947b`, Księga MD5 `6f89792f56a4`. Wynik po rozszerzeniu zakresu jest w sekcji 10b poniżej.

## Zmiany

- `Zaslanianie.pierwszyCel`: najpierw jeden promień celownika od oczu do zasięgu stożka. Następnie 7 kolumn przekroju hitboxu (także ±0,99 półszerokości) × 3 wysokości; punkty sortowane od najmniejszego kąta względem celownika. Filtr kąta jest liczony do każdego punktu. Pierwszy hit istoty na promieniu wygrywa; po trafieniu dalsze promienie nie są badane. Promienie próbek również są ograniczone zasięgiem stożka.
- `Rzucanie.stozek`: wstępny filtr zasięgu i kąta uwzględnia pełną przekątną hitboxu, żeby trafienie celownikiem w głowę wysokiego celu nie odpadło przez położenie środka.
- `Strumien.tick`: promień przeszkód i długość są liczone od oczu wzdłuż kierunku patrzenia; model nadal zaczyna się przy dłoni. Przy celowaniu ponad wilkiem model ma 4,56 bloku i cel otrzymuje obrażenia. Przy celowaniu w środek wilk zatrzymuje go na 1,56 bloku.

## Przebiegi i rozpoznanie ograniczenia

Build bazowy przed edycją: `a-r10-base-build`, kod 0, Zaklęcia HEAD `3c60585`, MD5 JAR **`00b226915f57`**, Księga HEAD `76d0cec`, MD5 **`2f17c063680e`**. Pierwszy build zmiany `a-r10-build1` kod 0, MD5 Zaklęć `f90501324e78`; build z dodatkowymi skrajami i pełną przekątną `a-r10-build2` kod 0, MD5 `e34751911d18`; diagnostyczny build z `debug: true` `a-r10-z3-debug-build` kod 0, MD5 `8634c6d5f606`.

`B6Edge` po `build2` (`a-r10-z2-edge`) zgłasza PASS, ale jego warunek sprawdza tylko powtarzalność, nie wymagany próg. Faktyczne wyniki z murem: x=0,95 **0**, x=0,975 **0**, x=1,0 **11,808** obrażeń. Otwarte x=0,95: **11,808**. Cel całkowicie za murem przy x=0,5 w `B6Occlusion`: **0**. Próg ≥~10% odsłonięcia **nie jest spełniony**.

Kluczowy wynik debug `a-r10-z4-edge-debug`: dla skrajnego huska `ZAKDBG stozek ... los=true wynik=CEL`, więc siatka już wykrywa widoczny fragment i przekazuje cel do skilla. Następny log `ZAKDBG traf ... mmoCanTarget=true losMM=false`, a `ZAKDBG wynik ... obrazenia=0.0`. Skill MythicMobs `Zak_Plomienie_Traf` w `/opt/docker/mc/data/world1/plugins/MythicMobs/skills/zaklecia/plomienie.yml:98` ma osobny `TargetConditions: - lineofsight true`, oparty na linii do środka celu. Podobny warunek jest w `plomienie.yml:107` i `odmrozenie.yml:97,109`. Ta druga kontrola odrzuca trafienia widocznej krawędzi mimo wyniku naszej siatki. Pliki YAML są poza zakresem r10; nie zmieniałem ich. Aby spełnić wymóg obrażeń na krawędzi, nadzorca musi rozszerzyć zakres o te skille lub wskazać inny sposób ominięcia drugiej kontroli bez utraty ich efektów.

`B6Occlusion`: `a-r10-test-B6Occlusion` **4 PASS**; po `build2` `a-r10-z2-occlusion` 3 PASS / 1 FAIL (próba PvP po odsunięciu postaci dała 0 obrażeń); powtórka debug `a-r10-z5-occ-debug` **4 PASS**, PvP za graczem 0, po odsunięciu 7,8. Wilk: środek 0, odsłonięta głowa 7,872. Półmurek x=0,5: 0. Druga próba PvP wygląda na niestabilność sondy/stanu, nie stałą zmianę kodu.

`N_zaslona` na pierwszym buildzie: 6 PASS / 1 FAIL; jedyny FAIL jest nieaktualnym oczekiwaniem `head.visualLength < 2.5` przy celowaniu ponad wilkiem. Sonda `N_zaslona_r10` ze zmienionym tylko tym progiem na `>3.5` po `build2`: **7 PASS**. Głowa: 7,872 obrażeń, model 4,563 bloku; środek: 0 obrażeń, model 1,564 bloku.

Regresje na pierwszym buildzie: `A` 9 PASS, `D` 5 PASS, `F` 4 PASS / 1 SKIP (znany brak montowania logu w bocie), `N_ppm1` 3 PASS, `STAT3` 23 PASS, `PW` 3 PASS. W logach runnera nie ma nowych `BLEDY-LOGU` pluginów. `git diff --check` czysty.

## Koszt debug

`R10Cost` (`a-r10-z4-cost`) z 1, 5 i 10 kandydatami: 4 PASS razem z kontrolą. Skumulowany licznik `ZAKDBG zaslona` (sondy / promienie / średnie µs na sondę): **20 / 23 / 77**, **60 / 166 / 95**, **120 / 534 / 89**. Różnice między etapami dają orientacyjnie: 1 kandydat ~1,15 promienia i 77 µs, 5 kandydatów ~3,58 promienia i 104 µs na kandydata, 10 kandydatów ~6,13 promienia i 83 µs na kandydata. To pomiar diagnostyczny na klonie z logowaniem, nie gwarancja czasu najgorszego przypadku.

Rundę 10 zatrzymałem po raporcie, zgodnie z ówczesnym poleceniem Claude.

## 10b — usunięcie drugiej kontroli linii wzroku

Claude rozszerzył zakres na skille stożkowe w nakładce. Skopiowałem z aktualnego world1 `MythicMobs/skills/zaklecia/plomienie.yml` i `odmrozenie.yml` do `naprawy/plugins-overlay/`. Diff względem aktualnych plików produkcyjnych to dokładnie usunięcie czterech linii `- lineofsight true`: po dwie w `Zak_Plomienie_Traf` / `Traf2` i `Zak_Odmrozenie_Traf` / `Traf2`. `mmoCanTarget` i wszystkie pozostałe warunki oraz efekty pozostały. Przeszukanie całego `MythicMobs/skills/zaklecia/` wykazało tylko te cztery wystąpienia `lineofsight true`; innych skilli blokowanych takim samym warunkiem nie znalazłem. Produkcyjnych plików world1 nie zmieniałem.

Wzmocniłem `B6Edge.js`: sprawdza obrażenia >1 dla x=0,95 i 0,975 za murem, 0 przy pełnej zasłonie x=0,5, oraz powtarzalność x=1,0. `a-r10b-01-B6Edge`: **3 PASS**. Z murem x=0,95 i 0,975 otrzymały po **11,808001** obrażeń, x=0,5 otrzymał **0**. Przed usunięciem warunku MythicMobs oba skrajne cele otrzymywały 0 mimo `Zaslanianie.los=true`.

`B6Occlusion.js` miał niestabilny pomiar PvP, gdy ponownie użyty MagCel zaczynał z niskim HP i odradzał się podczas liczenia różnicy. Pierwszy przebieg `a-r10b-02-B6Occlusion`: 3 PASS / 1 FAIL, MagCel 3,7→18 HP w otwartej próbie (odrodzenie). Przed pomiarem dodałem uzdrowienie celu. Powtórka `a-r10b-09-B6Occlusion`: **4 PASS**. Za przednim graczem 0 obrażeń, po odsunięciu 7,899999; za wilkiem środek 0, odsłonięta głowa 7,872001; x=0,5 za murem 0.

Pozostałe regresje po zmianie nakładki: `N_zaslona_r10` **7 PASS**, `N_ppm1` **3 PASS**, `A` **9 PASS**, `F` **4 PASS / 1 SKIP** (znany brak montowania `latest.log` w bocie), `STAT3` **23 PASS**, `PW` **3 PASS**. Runner nie podał nowych `BLEDY-LOGU` pluginów. Build `a-r10b-00-build`: kod 0, `KLON START OK`, Zaklęcia MD5 **`ea899c332cf2`**, Księga MD5 **`30069161d291`**. `git diff --check` czysty; bez commitów. Worktree Księgi czyste, Zaklęcia mają tylko trzy zmiany Java z r10. Po raporcie STOP.
