# Agent A — runda 7

Praca bez commitów. Klon pozostaje uruchomiony. Baza na nowym klonie: Zaklęcia `7b62798`, Księga `ec4fa21`; build `a-r7-base` kod 0, MD5 JAR `b99ea611efca` / `542a81073176`. Ostatni build po zmianach `a-r7-26` kod 0, MD5 JAR Zaklęć `3ba0f666241a`, Księgi `2145341974fa`, `KLON START OK`; `debug: false`.

## 1. Zwykłe przedmioty w slocie 5

Sprzeczność `C`/`N3B` wynikała z przygotowania testu, zanim Księga uporządkowała ekwipunek. `/give` wkładał przedmiot do zarezerwowanego slotu 0; Księga przenosiła go do plecaka (np. slot 19), a sonda próbowała kliknąć już pusty slot klienta. Nośnik wypełniał pusty slot 5. Zdarzenie zachodziło przed PPM. Odczyt pełnych 41 slotów w `R7C` przed i po PPM potwierdza, że chleb, mikstura, kilof i blok nie znikają, lecz pozostają w plecaku; w przypadku łuku widać również wirtualny obraz w slocie 13, nie drugą prawdziwą broń. Sonda `C` ustawia zwykłe przedmioty bezpośrednio w docelowym slocie 5, a `R7C` sprawdza wszystkie sloty serwera i liczbę kopii.

- FAIL bazowy `a-r7-04`: wszystkie sześć przypadków zgłaszało pusty slot 5; pełny odczyt pokazał przedmioty już poza nim **przed PPM**.
- PASS `a-r7-11` i po ostatnim buildzie `a-r7-27`: chleb, mikstura, łuk, kilof, blok i mapa pozostają w slocie 5 po PPM, bez zmiany liczby kopii w 41 slotach. W regresji `C` (`a-r7-15`) tych sześć przypadków też przeszło.

## 2. Synchronizacja ekwipunku

`Nosnik.klik` anulował przenoszenie przedmiotu zaklęcia bez późniejszego `updateInventory`. Klient może przewidzieć ruch kursora lub slotu; dodałem synchronizację dwa tiki po odmowie, gdy Księga zakończy odświeżenie. Nie dotyczy zwykłego przewijania paska. To zabezpieczenie anulowanego kliknięcia, a nie potwierdzona przyczyna starego kroku 185.

Oryginalny ślad z krytyka zachowano w `fuzz-271828-r4-original.json`. Na **nowej bazie** powtórka 190 kroków `a-r7-09` nie odtworzyła różnicy `stone_sword` w kroku 185. Zgłosiła inny przedmiot w kroku 80. Pełny przebieg po ostatnim buildzie `a-r7-29` wykazał, że przy kroku 80 klient miał otwarte GUI `minecraft:generic_9x6`; przy kroku 185 także było otwarte to GUI, bez rozbieżności. Porównanie `bot.inventory.slots` z 41 slotami encji przy otwartym GUI nie jest wiarygodne, bo okno kontenera ma inny układ. Po pominięciu takiego porównania ostatnia pełna powtórka `a-r7-34` osiągnęła 190/190 kroków bez sygnatur rozjazdu (PASS). Po zamknięciu GUI, jeszcze przed rozłączeniem klienta, sonda odczytała wszystkie 41 slotów i uzyskała 0 różnic oraz 0 innych naruszeń. Dokładnej przyczyny starego zgłoszenia nie da się dowieść z oryginalnego śladu, który nie zapisał stanu okna; na obecnej bazie rozjazd nie występuje.

## 3. Przywołany miecz oburącz

Kod `RzutyOdrzutowe.wystrzel` już pobiera `mana × manaOburacz`, gdy oba ładunki połączą się przed strzałem. Izolowana sonda `R7Mana` na bazie (`a-r7-07`) zmierzyła 2,0 many z jednej dłoni i 5,6 many oburącz (×2,8), gdy obie dłonie rzeczywiście doszły do stanu `gotowe`. Powtórka `a-r7-28` kliknęła LPM zaraz po operacjach Księgi: filtr gestu Q w `Rzucanie.swiezeQ` odrzucił klik, prawa ręka pozostała `gotowa`, więc poprawnie naliczono tylko ×1. Po odczekaniu filtra Q (`a-r7-30`) obie ręce się ładowały, koszt wyniósł ×2,8, test przeszedł. Sonda B czeka teraz na ustąpienie filtra i gotowość obu dłoni. Nie zmieniałem mnożnika ani opisu zaklęcia, ponieważ naliczanie ×2,8 działa.

## 4. Pakiety paska

`Bron.held` w Księdze odświeżał cały ekwipunek również po anulowanym przez pasek zaklęć `PlayerItemHeldEvent`. Teraz pomija anulowaną zmianę slotu. Sonda `R7UI`: przy pojedynczym przewinięciu `set_slot` spadło z 12 (`a-r7-05`) do 0 (`a-r7-12`). W regresji A, po oddzieleniu pomiaru od przygotowawczej zmiany slotu, A03 otrzymało jeden `set_slot` (offhand) i przeszło (`a-r7-15b`).

BossBar: `Wybor.odswiez` już aktualizuje oba własne paski wyłącznie po zmianie tekstu. Fizyczny ruch sieciowy nadal jest większy: `R7UI` po zmianie zobaczył 15 pakietów BossBar podczas jednego przewinięcia (`a-r7-12`), a potwierdzenie po ostatnim buildzie `a-r7-32` pokazało 43; widoczne są powtórzenia pasków wyboru oraz pozostałe paski MythicHUD. MythicHUD wysyła kilka identycznych `ADD` dla tych samych UUID, również dla paska huska. Cel ≤2 **wszystkich** pakietów BossBar na akcję nie jest osiągnięty; wymaga zmiany sposobu łączenia pasków przez MythicHUD, poza kodem tych dwóch pluginów. Globalnej częstotliwości całego HUD nie zmieniałem.

## Regresje

| Grupa | Przebieg | Wynik |
|---|---|---|
| A | `a-r7-15b`, potwierdzenie `a-r7-24` | 9 PASS, 0 FAIL; A03 jeden `set_slot` |
| C | `a-r7-15` | 19 PASS, 3 FAIL; sześć zwykłych przedmiotów PASS; pozostałe błędy: blok w lewej przy LPM, kontrola zwykłym mieczem, brak Sakwy w slocie 18 |
| D | `a-r7-16` | 5 PASS |
| F | `a-r7-17` | 4 PASS, 1 SKIP |
| DB3 | `a-r7-18` | 7 PASS, 1 FAIL przy zakładaniu broni MMOItems do lewej podczas aktywnego nośnika, 5 zależnych SKIP |
| N_melee2 | `a-r7-19` | 5 PASS |
| N_damage | `a-r7-20` | 3 PASS |
| STAT3 | `a-r7-25` | 23 PASS po ustabilizowaniu wyboru miecza w sondzie |

Nierozwiązane pozostają 3 inne asercje `C`, przygotowanie `DB3` i fizyczne pakiety BossBar MythicHUD. Nie są skutkiem zmian w tej rundzie, ale pełna regresja nie jest zielona.
