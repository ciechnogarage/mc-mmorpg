# HUD world1 — pakiet do testu i wdrożenia

## Układ po rundzie 2

- **Karta Wędrowca**: lewy górny róg, 142×66 px GUI od `(6, 8)`; dwie tekstury tła 124+18 GUI w gęstości 2×, bo jedna 284 px przekroczyłaby limit 256. Skóra, granatowa plakietka i złote okucia pochodzą z palety Księgi. Aktualna głowa ma 32×32 GUI, od `(6,24)` wewnątrz karty; okrągły medalion 20×20 zaczyna się w `(24,45)`; ta średnica mieści poziom 100. HP 8 px GUI, mana/stamina 4 px, EXP 2 px; paski mają wnękę i trzy tony. Osobne tekstury HP dla trucizny i uschnięcia. Przy niskim HP cienka czerwona krawędź portretu.
- **Długie napisy**: nick ma szerokość 130 GUI; 16-znakowy test `Wędrowiec1234567` zajmuje 92 px w bitmapowym foncie MC. Najdłuższa nazwa klasy z aktualnych plików, `Wołający Przodków`, ma 98 px i mieści się po przesunięciu początku klasy do x=43. Nowe, dłuższe klasy wymagają sprawdzenia.
- **Chowaniec**: zwykły pasek i panel przeciwnika zaczynają się 84 GUI od góry, panel własnego stwora 134 GUI. Karta kończy się na 74 GUI, więc jest 10 GUI odstępu.
- **Minimapa**: NMinimap nadal `side: right`, offset 60/60 fizycznych pikseli i scale 2. Ramka dostała te same okucia. Tabliczka współrzędnych ma 64×14 GUI i nie jest szersza od mapy przy GUI 4. MythicHUD używa pikseli GUI, więc odstęp tabliczki od mapy zależy od skali; sprawdzić w kliencie.
- **Dolny pasek, wariant B wdrożeniowy**: vanilla `hotbar.png` 182×22, tła grup 0–1 / 2–4 / 5–6 / 7–8 z cienkimi przegrodami. Bez napisów i zakładek; te ostatnie pokazuje wariant A makiet. W makietach sloty mają przykładowe przedmioty, w kliencie leżą prawdziwe przedmioty. Po lewej tarcza i liczba zbroi, po prawej udziec i trójtonowy pasek głodu. Nazwy zaklęć zostają nad hotbarem.

## Źródła danych

`%player_name%`, `%player_gamemode%`, `%player_x%`, `%player_z%`, `%player_absorption%`; `%mmocore_level%`, `%mmocore_class%`, `%mmocore_mana%`, `%mmocore_stat_max_mana%`, `%mmocore_stamina%`, `%mmocore_stat_max_stamina%`, `%mmocore_experience%`, `%mmocore_next_level%`; wbudowane listenery `health`, `food`, `armor`, `air`. Mana i stamina są w lokalnych plikach MMOCore, a PAPI Player Expansion jest zainstalowany. Głowa bez `target` pokazuje HudHoldera według dokumentacji MythicHUD; jar 1.3.4 zawiera `HeadLayer` i `PlayerHeadProvider`. Zmiana skórki przez CMI może wymagać ponownego pobrania cache głowy — do testu w kliencie.

Przydział 7/8 potwierdza `CoreTools/Scripts/stale-sloty.yml`; nazwy ikon rąk i mikstur są w `KsiegaPostaci/config.yml`. Dokładne zachowanie slotów 0–6 w hotbarze trzeba sprawdzić w kliencie. Dusze Akolity i Wiara nie mają potwierdzonego licznika PAPI, więc pasek zasobu pokazuje manę. Pomarańczowe HP przy samym paleniu nie zostało włączone: `effect{}` sprawdza efekty mikstur, a `fire_resistance` oznacza odporność, nie płonięcie. Nie ma potwierdzonego placeholdera płonięcia.

## Pliki i wdrożenie

- `praca/config.yml`, `praca/layouts/{karta,hotbar,chowance}.yml`, `praca/hud_assets/hud/{karta,hotbar}.yml`, `praca/listeners/karta.yml`.
- `praca/pack/assets/mythichud/textures/assets/karta/` zawiera nowe grafiki 2× z `scale: 0.5` oraz przezroczystą teksturę danych dla liczby zbroi. Vanilla hotbar musi być 1×, bo 182×22 przy 2× przekracza limit Nexo 256 px. `hotbar_selection.png` zostaje vanilla.
- `praca/NMinimap/config.yml`, `praca/NMinimap/frames/karta_square.png` oraz `praca/MMOCore/config.yml` (`action-bar.enabled: false`). Komentarz MMOCore mówi, że ta opcja wyłącza tylko domyślny action bar, nie inne action bary MMOCore.
- `makiety2_gen.py` generuje tekstury i 23 PNG w `makiety2/` (oba warianty dołu, GUI 3/4, pięć stanów, karta i dół ×4). Skrypt czyta bitmapowy font Minecrafta z `mcfont/`, w tym polskie znaki, i skaluje tylko przez NEAREST. Makiety są symulacją z przykładową twarzą i przedmiotami; rzeczywisty klient pokaże aktualną skórkę i ekwipunek.
- `wdroz.sh` **nie został uruchomiony**. Przygotowuje pełną kopię MythicHUD `config.yml`, `hud_assets`, `layouts`, `listeners`, `popups`, `source-pack`, NMinimap config/frames/SQLite i MMOCore config w `/opt/docker/mc/data/world1/backups/hud-<UTC>/` z `cofnij.sh`; kopiuje pliki i ustawia `isRight=1` w bazie NMinimap dla istniejących graczy. Operator powinien uruchomić go przy zatrzymanym serwerze, potem uruchomić serwer i przebudowaną paczkę. Cofanie bazy przywraca stan z chwili kopii.

## Rzeczy do sprawdzenia w kliencie

1. GUI 3 i 4: odległość tabliczki od minimapy (NMinimap liczy piksele ekranu, MythicHUD piksele GUI); poprawność pozycji HotBar selection i brak kolizji nazw zaklęć.
2. Aktualizacja głowy po `/cmi skin`, wartości EXP/staminy i absorpcji, zmiana koloru HP przez poison/wither, próg 25%/20%; sprawdzenie najdłuższych nazw i rozmiaru poziomu 1/10/100 w realnym foncie gry.
3. SPECTATOR w kreatorze, walka Chowańców, oddychanie pod wodą, komunikaty action bar innych pluginów, sloty 0–6, mapa po przeniesieniu na prawej.

Dokumentacja: [MythicHUD Asset Layers](https://wiki.mythiccraft.io/mythichud/Components/Asset-Layers), [Listeners](https://wiki.mythiccraft.io/mythichud/Listeners), [Conditions](https://wiki.mythiccraft.io/mythichud/Conditions), [Plugin Configuration](https://wiki.mythiccraft.io/mythichud/Plugin-Configuration), [MMOCore Placeholders](https://gitlab.com/phoenix-dvpmt/mmocore/-/wikis/Placeholders), [NMinimap](https://github.com/NezuShin/NMinimap), [Player placeholders](https://wiki.placeholderapi.com/users/placeholder-list/minecraft/).
