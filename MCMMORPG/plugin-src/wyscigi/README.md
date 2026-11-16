# Wyscigi (`plugin-src/wyscigi`)
Wersja w źródle: `${version}` w `src/main/resources/plugin.yml:3`, podstawiana z `gradle.properties` (`kbVersion=0.1.0`) i rewizji (`build.gradle.kts`, `build.sh`) -> jar `wyscigi-0.1.0-<rev>.jar`.
Jar na world1: `/opt/docker/mc/data/world1/plugins/wyscigi-0.1.0-202610201644.jar`. Zgodność źródła z jarem: zgodna (`plugin.yml` w jarze różni się tylko podstawioną wersją `0.1.0-202610201644`; `config.yml` jar = źródło = world1, `diff` bez różnic).
Opis stanu z dnia: 2026-10-25, commit `8c79ff8` (HEAD worktree `docs-pluginy`; ostatnia zmiana w pluginie: `d24c1c8`, 2026-10-20).

## Po co (co widzi gracz)
Minigra: wyścigi chowańców (wierzchowiec-wilk z modelem ModelEngine) na torze "Sadowy Krąg" w osobnym świecie `wyscigi`. Gracz wchodzi z menu (DeluxeMenus wywołuje komendę z konsoli), wybiera gatunek wierzchowca i tryb: czasówka (sam, bez zderzeń) albo wyścig (do 8 graczy, z poczekalnią). Steruje W/S/A/D, Shift+A/D to poślizg ładujący turbo, F = zebrany bonus, LPM = cios, Q = pauza i wyjście. Rekordy toru są zapisywane i pokazywane po mecie.
Zasada projektu (wierzchowiec z wypożyczalni, równa baza statystyk): `~/.claude/skills/mc-systemy-gracza/chowance.md:27`.

## Komendy i uprawnienia
Jedna komenda `wyscigi` (parser `plugin.yml`: 1 komenda, 1 uprawnienie `wyscigi.console`, domyślnie op; `plugin.yml:7-14`). Kodu sprawdzającego uprawnienie w `onCommand` nie ma, polega się na Bukkit (`WyscigiPlugin.java:536-543`). W praktyce wołana z konsoli przez menu: `[console] wyscigi czasowka %player_name% wilkor` (`wdrozenie/menu/gui_menus/wyscigi*.yml`, wiersze 19, 35, 51, ...).

| Podkomenda | Kto | Co robi | Źródło |
|---|---|---|---|
| `czasowka <gracz> <gatunek>` | konsola/OP (menu) | start czasówki; odmowa daje komunikat graczowi i "Odmowa" nadawcy | `WyscigiPlugin.java:548-557` |
| `wyscig <gracz> <gatunek>` | j.w. | start/dołączenie do wyścigu w poczekalni | `WyscigiPlugin.java:548-557`, `Wyscig.java:50` |
| `wyjdz <gracz>` | j.w. | wyjście z wyścigu | `WyscigiPlugin.java:600-605` |
| `rekordy [gracz]` | j.w. | rekordy graczowi; bez gracza top 10 do konsoli | `WyscigiPlugin.java:606-610` |
| `zbuduj` | OP/konsola | buduje tor w świecie `swiat`, ustawia spawn świata | `WyscigiPlugin.java:611-623` |
| `stan` | OP/konsola | lista wyścigów i kierowców (diagnostyka) | `WyscigiPlugin.java:624-630` |
| `odnowa <gracz>` | konsola (menu `wyscigi.yml:31`) | restart czasówki tym samym gatunkiem (w wyścigu z innymi odmawia) | `WyscigiPlugin.java:586-599` |
| `poza`, `przechyl`, `obserwuj`, `poz`, `zdarzenia`, `tor`, `przeladuj` | testowe/diagnostyczne | `poza`/`przechyl` ustawiają pozę modelu do pomiaru, `tor` wypisuje próbki, `przeladuj` ładuje config (tylko gdy nikt nie jedzie) | `WyscigiPlugin.java:558-585,631-655` |

`usage` w `plugin.yml:10-12`, tekst pomocy (`WyscigiPlugin.java:520`, stała `UZYCIE`) i tab-complete (`:522-534`, lista `PODKOMENDY`, nick gracza, gatunki) obejmują wszystkie podkomendy `switch` (`czasowka`, `wyscig`, `odnowa`, `wyjdz`, `rekordy`, `obserwuj`, `zbuduj`, `stan`, `poz`, `zdarzenia`, `tor`, `przeladuj`, `poza`, `przechyl`; `czasowka` i `wyscig` dzielą jeden `case`, stąd 13 `case`ów). Błędna liczba w argumencie (`poza`, `przechyl`, `obserwuj`) daje komunikat z użyciem, bez wyjątku w logu (`WyscigiPlugin.java:540`).

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/Wyscigi/config.yml` (4674 B, identyczny ze źródłem `src/main/resources/config.yml`). Odczyt w jednym miejscu: `Ustawienia.java:27-72` (brak wartości w pliku = domyślna z kodu, równa wartości z pliku; brak sekcji `gatunki` = ostrzeżenie w logu i pusta lista gatunków, `start` odmawia "Nieznany wierzchowiec", `Ustawienia.java:30-35`; brak sekcji `tory.<tor>` = czytelny `IllegalArgumentException`, a przy `przeladuj` zostają stare ustawienia, `:76-78`). Przeładowanie: `/wyscigi przeladuj` (`WyscigiPlugin.java:646-655`) albo restart.
9 kluczy najwyższego poziomu (parser): wszystkie opisane.

| Klucz | Znaczenie, wartość na world1 | Domyślna w kodzie | Źródło |
|---|---|---|---|
| `swiat` | świat toru: `wyscigi` | `wyscigi` | `Ustawienia.java:28` |
| `debug` | log zdarzeń + bufor 40 ostatnich na nick: `false` | `false` | `WyscigiPlugin.java:126-132` |
| `swiaty-awaryjne` | gdzie wrócić, gdy nie ma zapisanego powrotu: `stolica_odnowa, stolica, world` (pierwszy istniejący, jego spawn) | brak | `Ustawienia.java:29`, `WyscigiPlugin.java:388-392` |
| `swiaty-wejscia` | z jakich światów wolno wystartować: `world, stolica_odnowa, stolica` | brak | `WyscigiPlugin.java:195` |
| `gatunki` | 6 gatunków (wilkor, pozogar, glebiniec, puszczorog, glazogrzbiet, zelaznorog): `nazwa`, `model`, `ikona`, `predkosc` (0.54-0.575), `przyspieszenie` (0.022-0.034), `skret` (7.6-10.0), `opis` | `nazwa`=id, `ikona`=model | `Ustawienia.java:30-35` |
| `jazda` | fizyka: `hamowanie` 0.05, `wybieg` 0.012, `wsteczny` 0.14, `poza-torem` 0.55, `skret-przy-predkosci` 0.4, `skret-klawisz` 0.6, `przesuniecie-boczne` 0.07, `zderzenie-sciana` 0.45; `poslizg.{min-predkosc, min-kat, skret, kat-uslizgu, turbo-1/2, turbo-1/2-czas, turbo-mnoznik, przechyl}`; `pole-przyspieszenia.{czas 26, mnoznik 1.38}`; `start-turbo.{okno 10, czas 30, mnoznik 1.3, za-wczesnie 32, kara 24}` | `skret-klawisz` 0.6, `poslizg.przechyl` 18, reszta 0 | `Ustawienia.java:36-61` |
| `wyscig` | `poczekalnia-sekundy` 20, `poczekalnia-dokladka` 5, `poczekalnia-max` 40, `max-graczy` 8, `po-zwyciezcy-sekundy` 30, `limit-sekundy` 420, `po-mecie-sekundy` 6, `wyjscie-shift-ticki` 40, `skala-modelu` 1.4 | `skala-modelu` 1.4, reszta 0 | `Ustawienia.java:59,62-69` |
| `tor` | id aktywnego toru: `sad` | `sad` | `Ustawienia.java:70-71` |
| `tory.<id>` | `nazwa` "Sadowy Krąg", `y` 64, `polszerokosc` 5.5, `pobocze` 3.5, `okrazenia` 3, `punkty` (18 par x,z; min. 4), `skrzynki` (s: 80,170,270,365), `przyspieszenia` (pola: s, bok) | `y` 64, `polszerokosc` 5.5, `pobocze` 3.5, `okrazenia` 3 | `Tor.java:34-43,79-81` |

Kluczy nieużywanych w kodzie nie znaleziono (porównanie listy kluczy `Ustawienia.java` z configiem, `jazda.*`, `wyscig.*`, `gatunki.*.*`). Stałe poza configiem (wartości w kodzie): lista dozwolonych komend w wyścigu `/msg /w /tell /r /reply /party /p /friends /guild /g` (`WyscigiPlugin.java:61`), odliczanie 80 ticków (`Wyscig.java:26`), parametry bonusów (np. iskra 34 ticki x1.38, skorupa 200 ticków, cooldown ciosu 40 ticków, zasięg ciosu 3.4, odnowa skrzynki 60 ticków: `Bonusy.java:153-225,281-283,144`).

## Jak działa
- Start: `start()` wymaga świata z `swiaty-wejscia`, gracza żywego, nie spektatora, nie w pojeździe (`WyscigiPlugin.java:189-235`). Zapisuje punkt powrotu, tworzy Wilka (mob) z modelem ModelEngine i sadza gracza (do 10 prób dosiadu, `:245-288`). Wyścig dołącza do istniejącego w poczekalni (`:199`), inaczej powstaje nowy (`:201`).
- Zadanie okresowe: `runTaskTimer(20L, 1L)` czyli co tick tyka każdy `Wyscig.tick()` i `Bonusy.tick()` (`WyscigiPlugin.java:94,141-162`). Fazy: POCZEKALNIA, ODLICZANIE (80 ticków), JAZDA, KONIEC (`Wyscig.java:23,74-109`).
- Fizyka: własny kontroler ModelEngine `wyscigi_wierzchowiec` (`KontrolerWierzchowca.java:19-23`), prędkość liczy `Kierowca.krok` z wejść WASD/skok/Shift (`Kierowca.java:78-190`). Gaz nie może przekraczać vmax (`Kierowca.java:148`).
- Tor: splajn Catmull-Rom z `punkty`, próbkowanie co 0.5 bloku, rzut pozycji na tor (`Tor.java:46-70,103-124`). Okrążenia, pola przyspieszenia, wypadnięcie z toru (powrót), zła strona: `Wyscig.java:148-201`.
- Bonusy (tylko wyścig z innymi): skrzynki na torze (4 pozycje z configu), losowanie zależne od pozycji (ostatni częściej dostaje mocniejsze); iskra, kula ognia, błoto, skorupa, ryk (`Bonusy.java:34,153-225`). LPM = cios w najbliższego w 3.4 bloku (`:281-299`).
- Klawisze: listener PacketEvents LOWEST przejmuje Q (pauza: konsola `dm open wyscigi_pauza <nick>`, `WyscigiPlugin.java:477-479`), F (bonus), animację ręki (cios); anuluje kopanie, interakcje, użycie przedmiotu (`Klawisze.java:30-45`).
- Zdarzenia Bukkit (`WyscigiPlugin.java`): Quit `:401`, Join `:406`, Teleport (MONITOR) `:411`, ChangedWorld `:419`, Damage (LOWEST) `:425`, EntityTarget `:431`, CommandPreprocess (LOWEST, blokada poza listą) `:434-441`, Interact/InteractEntity/Drop/FoodLevel (anulowane dla jadących) `:443-452`, BlockBreak/Place (anulowane w świecie wyścigów, poza creative) `:454-458`, EntitiesLoad (usuwa osierocone wierzchowce) `:460-470`.
- Co 20 ticków: zmiana postaci (MMOProfiles) kończy wyścig, a obcy gracz w świecie wyścigów jest odsyłany (`WyscigiPlugin.java:152-161`).
- Integracje: ModelEngine, MythicLib (action bar `MMOPlayerData`, `Wyscig.java:310`), PacketEvents (twarde `depend`, `plugin.yml:5`), MMOProfiles (miękko, refleksja, `WyscigiPlugin.java:662-674`), DeluxeMenus (menu i `dm open` z konsoli), Multiverse-Core (softdepend, użycie nie sprawdzone).
- Bossbar HUD z ograniczeniem częstotliwości (`WyscigiPlugin.java:492-499`), action bar co 4 ticki (`Wyscig.java:278`).

## Koszt dla serwera
Tyka co tick tylko gdy jest aktywny wyścig (lista `wyscigi` pusta = pętla pusta; `Bonusy.tick` iteruje skrzynki zawsze gdy stoją, `Bonusy.java:117-131`). Tor (~117 chunków) jest force-loaded tylko na czas wyścigu (`WyscigiPlugin.java:134-138`, komentarz tamże). Zapis na dysk: `rekordy.yml` po każdej mecie i `powroty.yml` przy starcie/końcu przejazdu: zrzut `saveToString()` na wątku głównym, plik w osobnym wątku tła (`ZapisAsync.java`, jeden wątek na plik = kolejność zapisów zachowana, plik tmp + przeniesienie); `onDisable` kończy kolejkę i zapisuje stan końcowy synchronicznie (`Rekordy.java:70-78`, `WyscigiPlugin.java:108,687-700`). Pomiar MSPT: nie mierzone w tym opisie (testy botów: `testy-bot/`, nie sprawdzałem wyników).

## Dane
- `/opt/docker/mc/data/world1/plugins/Wyscigi/rekordy.yml` (307 B): `tory.<tor>.przejazd|okrazenie.<uuid>.{nick,gatunek,ms,data}` (`Rekordy.java:31-50`). Klucz = `Player.getUniqueId()` (`Kierowca.java:66`); UWAGA: przy MMOProfiles wszystkie postacie konta mają jeden UUID, więc rekordy są per konto, nie per postać (`Kierowca` tworzony w `WyscigiPlugin.java:212`).
- `powroty.yml` (0 B w dniu opisu): punkt powrotu `Powrot` kluczowany po UUID gracza, z UUID profilu MMOProfiles (`WyscigiPlugin.java:58,68,662-674`).

## Znane pułapki i decyzje
- Wdrożenia v1 (20.10) i v2 (21.10 14:44), cofnięcie z kopii, opis v2, testy i otwarte sprawy wizualne: `~/.claude/skills/mc-systemy-gracza/chowance.md:28-33`.
- Własny kontroler ME: gaz nie może przekraczać vmax, inaczej bez limitu prędkości: `chowance.md:32`.
- Poprawione (25.10): pomoc/usage/tab-complete pełne, `Float.parseFloat` w `poza`/`przechyl` i inne liczby w komendach obsłużone, brak `gatunki` nie wywraca startu, zapis `rekordy.yml`/`powroty.yml` w tle. Do decyzji właściciela (nie zmieniane): rekordy kluczowane UUID konta, nie postaci (`Kierowca.java:66`).

## Budowanie i wdrożenie
Budowa: `./build.sh` w katalogu pluginu (kontener `wyscigi-build-$$`, `--memory 2g`, przypięte jary ModelEngine-4.0.9, MythicLib-dist-1.7.1, packetevents-spigot-2.11.2: `build.gradle.kts`, `build.sh`). Wynik: `build/libs/wyscigi-0.1.0-<rev>.jar`. Testów jednostkowych w `src` brak; testy zewnętrzne: `testy-bot/` (bonusy, czasówka, odporność, wejście z menu, wyścig dwóch), `testy-klient/qa_jazda.py`. Wdrożenie: `wdrozenie/wdroz.sh` (kontrole, kopia świata, restart, budowa toru; `--sucho` tylko kontrole; kopia do `data/world1/backups/wyscigi-<stamp>`, cofnięcie z niej, `wdroz.sh:27-36`). Na klonie: `wdrozenie/klon-jar.sh`, `klon-przygotuj.sh`.
