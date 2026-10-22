# System walki PvE/PvP — wersja pierwsza

## Cel

Wdrożyć walkę 50/50: przygotowanie postaci i ekwipunku daje przewagę, ale aktywny unik, blok, pozycjonowanie i reakcja na zapowiedzi ataków pozostają konieczne. Wersja pierwsza obejmuje PvE oraz PvP w regionach oznaczonych jako strefy PvP/areny.

Rasy i klasy są osobnymi warstwami. CharacterCreator przechowuje rasę, a MMOCore klasę. Dostępne klasy bazowe to wyłącznie Wojownik, Ninja, Mag i Akolita; rasa nie nadaje skilli klasy.

## Zakres

- MythicLib pozostaje źródłem typów obrażeń, obrony, krytyków, blokowania, parowania, uniku i zasobów.
- MMOItems dostarcza statystyki broni, pancerza i tarcz; przedmioty mogą mieć osobne `pve-damage`, `pvp-damage` oraz odporności.
- MMOCore dostarcza zdrowie, manę, staminę, klasy i zasady interakcji.
- MythicMobs dostarcza ataki potworów, statusy i wzorce bossów.
- WorldGuard wyznacza regiony PvP. Poza nimi ataki gracz–gracz i skille ofensywne przeciw graczom są blokowane.

## Reguły walki

Zwykły atak zużywa staminę. Mocny atak pod prawym przyciskiem myszy zużywa jej więcej i zadaje większe obrażenia. Unik kosztuje 25% maksymalnej staminy i daje krótki okres niewrażliwości; regeneracja staminy rozpoczyna się 1,5 s po ostatnim wydatku. Blok/defensywna umiejętność działa tylko z przodu, redukuje obrażenia zgodnie ze statystyką tarczy i zużywa staminę zależnie od siły ciosu. Wyczerpanie staminy podczas bloku otwiera gracza na 1 s. Każda klasa ma cztery aktywne skille i jeden slot defensywny.

Obrażenia są fizyczne albo żywiołowe. Żywioły startowe to ogień, woda, ziemia, powietrze, jasność i ciemność; magia używa jednego z tych żywiołów i nie ma osobnej kategorii obrażeń. Pancerz redukuje obrażenia fizyczne, a odporności żywiołowe redukują odpowiedni żywioł. Trucizna jest efektem ziemi i zadaje ziemne obrażenia okresowe; krwawienie jest efektem fizycznym i zadaje fizyczne obrażenia okresowe. Kolejność: trafienie → krytyk → redukcja PvE/PvP → pancerz albo odporność żywiołu → tarcza/blok → status i efekty po trafieniu.

Statusy startowe: podpalenie (4 s, odnowienie), krwawienie (6 s, maks. 3 kumulacje, obrażenia fizyczne), trucizna (6 s, odnowienie i redukcja leczenia, obrażenia ziemi), wychłodzenie (20% spowolnienia przez 3 s), ogłuszenie (maks. 1 s, z odpornością po zakończeniu) i osłabienie (15% mniej zadawanych obrażeń przez 5 s). Parametry statusów są celowo wydzielone do późniejszego balansu. Bossy używają paska równowagi zamiast pełnego ogłuszenia.

## PvP

PvP jest aktywne wyłącznie w regionach WorldGuard z flagą `pvp: allow` (w tym w przyszłych arenach). Regiony startowe, miasta i pozostałe obszary mają `pvp: deny`. Party i sojusznicy gildii nie zadają sobie obrażeń; neutralni i wrogowie mogą walczyć w aktywnej strefie. W PvP stosowane są osobne mnożniki obrażeń z MMOItems oraz skrócone/ograniczone efekty kontroli tłumu, aby pojedyncze trafienie nie kończyło walki bez reakcji.

## Pierwszy zestaw testowy

Trzy archetypy broni (miecz, łuk, kostur), jedna tarcza, zwykły atak, mocny atak pod prawym przyciskiem, cztery skille aktywne i jeden defensywny, dwa zwykłe moby oraz jeden miniboss. Test równowagi: zwykły mob ginie po 4–6 trafieniach, gracz przeżywa 5–7 zwykłych trafień, a ciężki zapowiedziany atak zabiera 25–35% zdrowia. PvP zaczyna się od osobnych współczynników obrażeń i blokady statusów, które trwałyby dłużej niż 3 s.

## Konfiguracja i weryfikacja

Wartości strojenia pozostają w istniejących plikach `plugins/MythicLib/config.yml`, `elements.yml`, `mitigation_types.yml`, `plugins/MMOCore/config.yml` i statystykach MMOItems. Statusy i wzorce ataków trafiają do nowych, nazwanych plików MythicMobs zamiast do przykładowych definicji. Po zmianach trzeba wykonać walidację YAML, przeładować konfigurację na serwerze testowym i sprawdzić: PvE, PvP w regionie, blokadę PvP poza regionem, party-friendly-fire, krytyk, blok, unik, każdy status i śmierć/odrodzenie.
