# Ocena końcowa gry zaklęciami — runda 4

Skala 1–10: 10 oznacza szybkie, czytelne i przewidywalne sterowanie. Dowody: po dwa niezależne przebiegi A–F i sond A/B na końcowym buildzie Zaklęcia `7b62798`, Księga `1433dfb`, nakładka r6, `debug:false`; szczegóły i ograniczenia w [`krytyk/raport-r4.md`](krytyk/raport-r4.md). Zrzuty prawdziwego klienta: `klient/captures/M01k.png` i `porownanie-N7-r6.png`.

## Czynności

| Czynność | Ocena | Dowód i odczucie |
|---|---:|---|
| Księga, wybór i przypisanie | 8 | Ikony obu dłoni widoczne, panel się odświeża. Otwieranie paska nadal wysyła za dużo aktualizacji BossBar. |
| PPM: strumień ognia/mrozu | 7 | Mana i obrażenia działają także z mieczem MMOItems w prawej. Wąski strumień nie zasłania celu na prawdziwym kliencie. |
| LPM: czar lub miecz | 7 | Miecz MMOItems z biegłością i czarem w lewej trafia: `N_melee2` dwa razy 20→11,636 HP; PPM dalej działa. Lewy czar wywołuje 41–54 `setSlot`/s. |
| Ładowanie i puszczenie PPM | 7 | Ładowane czary trafiają; `F01` zwalnia po 3 tikach. Zachowanie prawej dłoni po puszczeniu PPM wymaga decyzji. |
| Ruch i seria | 6 | Sprint, skok i skradanie nie blokują czarów. Cztery gesty ruchu około 65 `setSlot`/s, pięć działań paska 374 BossBar. |
| Zmiana slotu, F, Shift+Q, śmierć, relog | 7 | Przypisania trwają; 0→5→0 zachowuje miecz. F nie pozwala na lewą broń przy aktywnym czarze. |
| Zwykłe przedmioty przy czarze w lewej | 3 | Chleb, mikstura, łuk, kilof, blok i mapa trafiają na chwilę do slotu 5, po czym serwer przywraca `debug_stick`; tarcza i miecz MMOItems są kontrolami pozytywnymi. |
| Sygnał wizualny i dźwiękowy | 6 | Komunikaty są spolszczone, ale BossBar odświeża się nadmiernie. Strumień ma cztery dźwięki zamiast ośmiu w bazie; surowa sonda `NOISE` nadal oznacza je FAIL przy progu jeden. |

## Typy zaklęć

| Typ | Ocena | Dowód |
|---|---:|---|
| Koncentracja: Płomienie i Odmrożenie | 7 | Działają obrażenia, koszt i obie dłonie; klient widzi cel za węższym strumieniem. |
| Pociski ładowane: Ognisty Pocisk, Kula Ognia | 7 | Trafienie i mnożnik oburącz sprawdzone w B. |
| Leczenie własne i innych | 7 | Samoleczenie i leczenie MagCel działają w końcowym B; dawny FAIL ładowanego uzdrowienia nie odtwarza się. |
| Osłona i Uspokojenie | 7 | `N_effects` dwukrotnie: Osłona redukuje cios około 4→2,8 HP, husk przestaje atakować po Uspokojeniu. Brak pakietu efektu nie oznacza braku działania. |
| Runa Ognia | 7 | Tworzenie, koszt i limit aktywnych run potwierdzone. |
| Przywołany Miecz | 5 | Działa w obu dłoniach; koszt oburącz około 1,7 many zamiast deklarowanego mnożnika ×2,8 w obu B. |
| Wilk i Atronach | 7 | Przywołania i limity działają. `PW` dwa razy potwierdził cios PvP z aktywnym wilkiem bez dawnej rekurencji; szerszej macierzy PvP nie badano. |

## TOP pozostałych problemów

1. Zwykłe przedmioty zamieniane na nośnik: sześć typów w C daje ten sam powrót slotu 5 do `debug_stick` w dwóch przebiegach.
2. Nadmierny ruch UI: 41–54 `setSlot`/s przy lewych czarach, około 65/s przy ruchu, 123–135 BossBar w krótkiej sekwencji N4; `NOISE` wskazuje też otwarcie, kółko, cyfrę, zamknięcie i PPM. A03 powtarza 32,9 `setSlot`/s.
3. Koszt oburącz Przywołanego Miecza: około 1,7 many zamiast oczekiwanego 2,8× kosztu jednej dłoni.
4. Sakwa nie pojawia się w slocie 18: serwer pokazuje tam papier×3; potrzebna decyzja, czy ma być nadawana automatycznie.
5. Broń w lewej przy zaklęciu: reguły Nośnika i Księgi blokują wyposażenie. Prawa broń przy lewym czarze działa.
6. Fuzzer 271828 × 1000 dwa razy odtwarza rozbieżność ekwipunku: od kroku 185 serwer ma miecz w slocie 0, Mineflayer pusty slot; inne sekwencje pokazują podobny przypadek z papierem. Kontrola powtarza odczyt po 250 ms. Nie ma dowodu trwałej duplikacji, ale integracja Nośnika/Księgi i synchronizacja klienta wymagają śladu pakietów i próby na prawdziwym kliencie.

Historyczna rekurencja `pomocSlug → MythicLib.canInteract → DamageCheckEvent → pomocSlug` nie odtworzyła się w końcowym `PW` ×2. Zostaje ryzykiem poza sprawdzoną ścieżką; nie jest potwierdzonym błędem obecnego builda.

## Decyzje właściciela

- Czy strumień ma trafiać przez sojusznika, czy sojusznik ma go zasłaniać? Zwężenie obrazu poprawiło widoczność, ale nie ustala tej reguły celu.
- Po jednoczesnym PPM+LPM prawa dłoń pozostaje aktywna po puszczeniu PPM i wymaga kolejnego LPM. Wybrać gaszenie obu dłoni przy puszczeniu albo zachowanie przełącznika prawej; ujednolicić opis i UI.
- Czy nowy gracz ma automatycznie dostawać Sakwę w slocie 18? Jeśli tak, dodać inicjalizację i migrację istniejących postaci.
- Lewa broń przy zaklęciu: **wariant 1** zakładanie broni odkłada czar z krótką informacją; F zamienia po świadomym odłożeniu. **Wariant 2** zapisuje broń i czar osobno, F przełącza aktywny stan z widocznym wskaźnikiem; wymaga zmian reguł Nośnika/Księgi i testów antyduplikacyjnych. Oba warianty opisuje `naprawy/szczegoly-r5.md`.
