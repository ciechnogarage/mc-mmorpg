# Stolica Wyspy — przebudowa po ocenie pierwszej wersji

## Powód

Pierwszy świat `stolica` był zbyt małym, symetrycznym placem. Nie odzwierciedlał aktywnych systemów world1. Punkty kreatora i MMOProfiles wróciły na poprzedni sprawdzony płaskowyż, zanim rozpoczęto przebudowę.

## Źródła prawdy

- Kierunek produktu: `docs/product/00-tier-0-product-identity.md` w głównym repozytorium. Gracz wybiera własną ścieżkę; miasto ma ją pokazywać w świecie.
- Hub i odkrywanie: `docs/stolica-wyspy-hub-foundation-v0.0.1.md`, `docs/ai/city-hub-mvp-concept.md`, `docs/contracts/travel-and-discovery.md`.
- Aktywny runtime world1: `plugins/CharacterCreator/config.yml`, `plugins/DeluxeMenus/gui_menus/`, `plugins/MMOCore/professions/`, `plugins/MMOItems/crafting-stations/`, `plugins/CoreTools/`, `plugins/Wiary/`, `plugins/MythicDungeons/maps/`.

Dokumenty foundation opisują starszy wybór klasy na poziomie 10 i inny zestaw profesji. Bieżący kreator wybiera jedną z czterech klas od razu (`wojownik`, `mag`, `akolita`, `ninja`), a bieżące menu profesji udostępnia 13 warsztatów. Nowa Stolica zachowuje ten stan zamiast przywracać dawny proces.

## Mapa i usługi

Świat `stolica_odnowa` jest odseparowany od istniejących map. Ma przystań wejściową, główną ulicę, centralny Nexus, dzielnice rynku, profesji, postaci, chowańców, wiary, treningu i drużyn. Drogi prowadzą pieszo do wszystkich NPC; skrypt `tools/stolica_v2/build.py` zapisuje odtwarzalny datapack, a `npc.py` zapisuje spawnery i komendy.

| Miejsce | Usługa world1 | Stan |
| --- | --- | --- |
| Nexus | `md play loch_001` przez menu | dostępny |
| Inne wyspy i lochy | brak uruchomionej trasy | oznaczone jako zamknięte |
| Rynek | sklep, skup, aukcje CoreTools | dostępne |
| Przygotowanie | naprawa i rozbiórka CoreTools, sakwa surowców | dostępne dla postaci z profilem |
| Dziedziniec rzemiosła | 13 stacji `profession-*` MMOItems | dostępne |
| Hala postaci | obecne menu postaci i umiejętności | dostępne, bez ponownego wyboru klasy |
| Menażeria | obecne menu chowańców | dostępne |
| Świątynia | obecny wybór patrona i łaski | dostępne; trzy ołtarze Starszych i cztery symbole ras |
| Dom drużyn | obecne menu party, znajomych, gildii | dostępne |
| Tablica | dziennik zadań i drogowskazy | dostępne; wejście do lochu jest w Nexusie |

## Weryfikacja

- 415 745 bloków w 101 szablonach, 28 NPC MythicMobs, 24 menu DeluxeMenus załadowane łącznie.
- Po dobudowaniu sześciu małych budynków sprawdzono ponownie: żadne z 28 stanowisk NPC nie ma zablokowanych nóg, głowy ani pustego podłoża; trener został przesunięty na wejście do placu ćwiczeń.
- Oba nowe menu używają istniejących ikon Nexo. Opis handlu poprawiono zgodnie z aktywnym systemem 13 profesji.
- Blok pod spawnem i powietrze na spawnie potwierdzone komendami serwera.
- Weryfikacja siatki pieszej: wszystkie 28 stanowisk osiągalne od przystani bez teleportu.
- Klient testowy otworzył oba nowe menu, naprawę, rozbiórkę, warsztat kowala i menażerię.
- Sakwa rzuca wyjątek na testowym koncie **bez wybranego profilu**. Jej identyczna komenda jest już używana przez działający przycisk sakwy w CoreTools. Trzeba jeszcze potwierdzić ją na aktywnym profilu.
- Pełny cykl kreator → miasto → Loch 001 → powrót wymaga klienta z aktywnym profilem. Bot protokołowy pozostaje w niestandardowej scenie wyboru postaci i nie może potwierdzić tego cyklu.

## Stan przełączenia i pozostały test

W bieżącej konfiguracji `creator-room.exit`, `profile-selection-location` i `new-profile-spawn-point` wskazują razem na `stolica_odnowa` (0.5, 101, 59.5). Log potwierdził wejście konta `Miau` w tym punkcie. Następnie kreator otworzył scenę wyboru profilu, więc samo zalogowanie nie potwierdza jeszcze pełnego przejścia po utworzeniu postaci. Pełny cykl kreator → miasto → Loch 001 → powrót oraz widok w prawdziwym kliencie pozostają do sprawdzenia na aktywnym profilu. Poprzedni świat `stolica` pozostaje zachowany.
