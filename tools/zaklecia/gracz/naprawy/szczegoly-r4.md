# Runda 4 — szczegóły Agenta A

## Kod

- `KsiegaPostaci.Bron.damage`: zasada slotu 0/1 obowiązuje również wtedy, gdy `getRegisteredAttackMetadata(victim)` zwraca `null` (listener Księgi może wykonać się przed listenerem MythicLib na tym samym HIGHEST). Wyjątek wyłącznie dla zarejestrowanego ataku, który **nie** jest `MeleeAttackMetadata`. To przepuszcza `mmodamage`, a blokuje zwykły cios nawet bez metadanych.
- N9 `RzutyOdrzutowe`: koszt many jest pobierany przed wywołaniem skilla, a przy `false` z `Skille`/`EfektyCzasowe` zwracany. Poprzednia wersja pobierała manę dopiero po skillu i przesuwała mierzony przez B moment reakcji. Ostatni JAR sprawdziłem bajtkodem: `giveMana` przy offset 347, skill przy 437/464/488, `giveMana` zwrotne przy 523.
- Pozostałe zmiany r3 (N4, N7, walidacja id, strażnik sesji, world change) zachowane. `support.party_other: true` zostaje według decyzji właściciela. Odrzucone YAML `damage{dc=MAGIC}` nie są w nakładce.
- Bez commitów. `git diff --check` obu worktree: exit 0.

## Główne A/B — identyczny filtr

Filtr: `ABCDEF,N_damage,N_invalid,N_melee,N_r1,N_r2,N_heal,STAT2`. `a-r4-01 build bazowy` kod 0, `a-r4-02 test` kod 1; `a-r4-03 build` kod 0, `a-r4-04 test` kod 1. Oba testy zawierają te same 13 grup i identyfikatory. Kod 1 całego przebiegu wynika też z wielu FAIL obecnych na bazie; nie oznacza niepowodzenia wszystkich poprawek.

| Sonda | Bazowy `a-r4-02` | Nowy `a-r4-04` |
|---|---|---|
| N2 Płomienie, miecz slot 5 | FAIL, HP 20→17 (ogień wtórny) | PASS, HP 20→9,9632 |
| N2 Odmrożenie, miecz slot 5 | FAIL, HP 20→20 | PASS, HP 20→11,2580 |
| N2 zwykły miecz slot 5 | PASS, HP 20→20 | PASS, HP 20→20 |
| N2 zwykły miecz slot 0 | PASS, HP 20→18,12 | PASS, HP 20→18,12 |
| N9 błędne id, prawa | FAIL, Płomienie→pusta | PASS, Płomienie→Płomienie |
| N9 błędne id, lewa | PASS | PASS |
| N5 leczenie innego, 3 próby | 3× FAIL | 3× PASS |
| N6 panel dłoni | FAIL | PASS |
| STAT2 | 22 PASS, 1 FAIL w obu | identyczne wartości obrażeń |

`STAT2`: Płomienie 0 Wiary 6,6912 i 40 Wiary 11,414402 (5/5 identycznych próbek w obu wersjach); Odmrożenie 5,828026 i 9,9876 (również 5/5). Skalowanie nie zostało pominięte przez poprawkę N2. Jedyny FAIL `STAT2` w obu przebiegach pochodzi z kontroli wybranego przedmiotu w jednej próbie (chwilowo `debug_stick`).

Grupy A/D/E/F nie mają nowych identyfikatorów FAIL względem bazy. W B głównego A/B poprawiły się m.in. `B-cel-gracz` i `B-uzdrowienie-innego-cel`; nowe FAIL `B14-zasklepienie_ran-prawa` oraz `B15-wielkie_uzdrowienie-prawa` dotyczyły wyłącznie progu `latencyTicks <=10` (12 i 11), przy pełnym wyleczeniu i koszcie many. W C `C-blok-PPM` przeszedł z FAIL na PASS; `C-jedzenie` raz przeszło z PASS na FAIL, co sprawdzono trzykrotnie z debugiem poniżej.

## N3 — trzy przebiegi C z debugiem

`a-r4-05 build` z `debug:true` kod 0; `a-r4-06`, `07`, `08 test C` wykonane kolejno, każdy kod 1 z powodu wielu znanych FAIL C oraz inwariantu harnessu, który traktuje sam log ZAKDBG jako naruszenie. Pełny wyciąg: `ZAKDBG-r4-C.log`; dodatkowo `.serwer.log` każdego przebiegu.

W przebiegu 06 `C-tarcza` pochodziła z indeksu inwentarza 36 (hotbar 0), po PPM slot 0 zawierał `debug_stick`; log pokazuje `stary=5 nowy=0 przedmiot=SHIELD` i następnie `pusta slot=0 przed=null`. Analogicznie `POTION` i inne zwykłe przedmioty trafiały do slotu 0. `KsiegaPostaci.Bron.refresh` (linie 316–323) usuwa z zarezerwowanego slotu RIGHT=0 przedmiot, który nie jest bronią ani zaklęciem, i oddaje go do ekwipunku przez `Migracja.give`. `Nosnik.odswiezPusta` tworzy nośnik **dopiero gdy** aktualny slot jest null/AIR. Kod DwieBronie nie zmienia wybranego slotu (`setHeldItemSlot`); jego odświeżanie dotyczy wyłącznie przedmiotów oznaczonych przez ten plugin.

Gdy chleb był w slocie 5 (źródło 41), w każdym z 06/07/08 po PPM pozostał `bread` w slocie 5. W 07/08 `C-jedzenie` miało status FAIL mimo poprawnej treści slotu: w 07 `latencyTicks=null`, w 08 `setSlotPerSec=24,5` (>20). Dodatkowy przebieg `a-r4-10` przy `debug:false` ponownie dał `C-jedzenie` PASS w slocie 5. Nie zmieniałem źródeł dla N3, bo źródłem widocznego powrotu nośnika jest zarezerwowany slot 0 lub metryki harnessu, a nie nadpisanie zwykłego przedmiotu przez Nosnik.

## Ostatni build i dodatkowe sondy

`a-r4-09 build` z `debug:false` kod 0; `a-r4-10 test B,C,N_damage,N_melee` kod 1, kontrole N2 wszystkie PASS. Dla rozstrzygnięcia zmian czasu manny: `a-r4-11 build bazowy` kod 0, `a-r4-12 test B` kod 1; po korekcie kosztu `a-r4-13 build` kod 0, `a-r4-14 test B` kod 1. Ostateczny `a-r4-15 build` kod 0, `a-r4-16 test B,N_invalid,N_damage,N_melee` kod 1. **Ostatni build jest wersją Agenta A**, `debug:false`, Zaklecia md5 `b4520dd5ddcd`, Ksiega md5 `9b0f9ac658c6`.

Na ostatecznym JAR: `N_damage` 3/3 PASS, `N_invalid` 3/3 PASS, `N_melee` 3/3 PASS. B14 prawa po korekcie kosztu: PASS, 7 ticków i pełne leczenie. B15 prawa pozostaje zmienne w identycznym kodzie: 9 ticków/PASS w `a-r4-10` i `a-r4-14`, 12 ticków/FAIL w `a-r4-16`; efekt i mana prawidłowe w każdym. Podobne nowe FAIL B02/B07/B12 w ostatniej próbie dotyczą wyłącznie `setSlotPerSec` lub `latencyTicks` przy zadziałaniu zaklęcia. Ze względu na obciążenie maszyny i zmienność tych progów nie uznaję tych wyników za dowód regresji funkcjonalnej; **cały zestaw B nie jest zielony**. Nie zmieniałem progu ani nie ukrywałem wyników.

## Ograniczenia

- Sonda N2 z mieczem w slocie 5 obejmuje bezpośrednio oba stożki. Grupa B obejmuje typy pocisk/kula/runa/przywołanie/broń przywołana, lecz nie wymusza miecza w slocie 5 dla każdego. Nie twierdzę, że każda kombinacja otrzymała osobną sondę A/B.
- N7 wymaga oceny wizualnej Claude w prawdziwym kliencie. N9 zwrot many przy skilu odrzuconym nie dostał osobnej sondy negatywnej; skompilowano kod i sprawdzono bajtkod, ale nie oznaczam tego przypadku jako PASS w grze.
