# HudDane (`plugin-src/hud-dane`)
Wersja w źródle: `${version}` (`src/main/resources/plugin.yml:2`) = `hdVersion=0.1.0` (`gradle.properties:2`) + rewizja `hdRev` z build.sh (`build.gradle.kts:29`, `build.sh:7`). Jar na world1: `/opt/docker/mc/data/world1/plugins/hud-dane-0.1.0-202610250016.jar` (plugin.yml w jarze: `version: 0.1.0-202610250016`, ten sam `main`). Zgodność źródła z jarem: zgodna co do wersji i `main`; `config.yml` na world1 identyczny ze źródłem (diff = brak różnic). Zawartość klas nie porównywana.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (ostatnie zmiany pluginu: 2c0c3e8 cele glow drużyny w cache MythicHUD, 4b357b3, 444c2e1).

## Po co (co widzi gracz)
Plugin niczego nie rysuje sam. Dostarcza placeholdery PAPI `%hud_...%` dla MythicHUD (HUD gracza): drużyna (HP, zasób, poziom, zasięg), przywołańce z odliczaniem życia, efekty mikstur z czasem, ramka celu walki, kanał zdobyczy (EXP, przedmioty), cel zadania ze strzałką i odległością, kierunek obrażeń, pasek awansu, kafelki umiejętności, tryb przygaszenia okna (`Placeholdery.java:9-24`).

## Komendy i uprawnienia
Brak: `plugin.yml` nie ma sekcji `commands` ani `permissions` (`src/main/resources/plugin.yml:1-6`). Kontrola 1: 0 komend w plugin.yml, 0 w tabeli.

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/HudDane/config.yml` (identyczny ze źródłem `src/main/resources/config.yml`). Klucze najwyższego poziomu, wszystkie czytane w `HudDane.java:36-47`:
- `druzyna.zasieg` 64 (domyślna w kodzie 64), `druzyna.niskie-hp` 0.3 (0.3): `HudDane.java:38-39`; zasięg w blokach, dalej lub inny świat = stan "daleko" (`Migawka.java:55`). `niskie-hp` wczytywane do pola `niskieHp`; użycie: stan "nisko" w `Placeholdery.java:145`.
- `slugi.limit` 3 (3): `HudDane.java:40`; `slugi.czas-zycia` mapa typ moba MythicMobs -> ticki życia (10 wpisów: AKO_SLUGA_OBRONCA 300, AKO_OGNIK_KSIEZYCA 160, AKO_PRZODEK_MLOTA 200, AKO_KOSIARZ_CISZY 200, SPEC_WILK_STEPU(+_BRON) 200, SPEC_PRZODEK(+_BRON) 240, SPEC_SLUGA_KURHANU(+_BRON) 240): `HudDane.java:41-42`. Mob spoza listy nie jest pokazywany (`Migawka.java:69-70`).
- `efekty` mapa klucz efektu Minecrafta -> id ikony (10 wpisów), kolejność = priorytet: `HudDane.java:43-44`, `Migawka.java:80-89` (limit pętli 64).
- `zadania.max-krok` 38 (38) i `zadania.miejsca.<id>` {fragment, swiat, x, y, z} (3 miejsca: straznicza, trener, wybieg, wszystkie w świecie `stolica_odnowa`): `Nawigacja.java:36-43`. Strzałka pokazuje się, gdy tekst kroku zawiera `fragment` (`Nawigacja.java:62-73`).
Config czytany tylko przy starcie (`onEnable`); komendy reload brak, zmiana wymaga restartu pluginu/serwera.

## Jak działa
- Migawka na gracza: `HudDane.migawka()` liczy stan leniwie i trzyma 250 ms (`HudDane.java:25, 53-61`); wiele placeholderów w jednym ticku HUD-a dzieli jedno zebranie danych. Czyszczona na wyjściu (`HudDane.java:63`).
- Drużyna: `PlayerData.get(p).getParty()` MMOCore, lider na górze, reszta alfabetycznie (`Migawka.java:47-62`). Przywołańce: aktywne moby MythicMobs z rodzicem = gracz i typem z configu; pozostały czas liczony od pierwszego zobaczenia, bo MythicMobs go nie podaje (`Migawka.java:29-30, 64-78`). Efekty: `getActivePotionEffects` (`Migawka.java:80-89`).
- Zdarzenia (MONITOR, ignoreCancelled): cel walki 8 s i zdobycz 4,5 s max 4 wpisy, sumowanie EXP w 1,5 s (`Zdarzenia.java:130, 137-149, 178-193`); kierunek obrażeń 1,2 s (`Nawigacja.java:30, 83-97`); `PlayerExperienceGainEvent` i `EntityPickupItemEvent` (`Zdarzenia.java:156-176`). Tylko RAM, nic nie jest zapisywane (`Zdarzenia.java:126-127`).
- Wejście gracza: wpisuje cele głowy drużyny `%hud_druzyna_1..7_nazwa%` do prywatnego cache `PlaceholderAPIParser.cachedPlaceholders` MythicHUD przez refleksję (naprawa "Name cannot be longer than 16", `HudDane.java:65-93`); przy innej wersji MythicHUD wyłącza się z ostrzeżeniem.
- Lista placeholderów i ich wartości: nagłówek `Placeholdery.java:9-24` (druzyna_liczba, druzyna_<n>_*, slugi_*, towarzysze, uklad, cel_*, zdobycz_*, ostrzezenie, loch, zadanie_*, obrazenia_kierunek, awans_*, umiejetnosc_<n>_*, okno, pet_typ, efekt_<n>_*). Obsługa: `Placeholdery.java:36-176`. Dla offline zwraca "" (`:37`).
- Integracje czytane przez PAPI: `%chowance_*%` (zadanie samouczka, pet, walka), `%mmocore_*%` (quest, poziom, umiejętności, in_combat), `%luckperms_meta_ui_hud%` i `ui_goal` (tryby HUD), `%md_dungeon_name%` (MythicDungeons, `loch`): `Nawigacja.java:47-52`, `Placeholdery.java:63-67, 115-127, 184-190`. Zależności twarde: PlaceholderAPI, MMOCore, MythicMobs (`plugin.yml:6`); MythicHUD tylko miękko przez refleksję. Ekspansja `hud` z `persist()` — nie sprawdzone.
- Wykrywane zużycie broni >= 85% i pełny ekwipunek: `ostrzezenie` (`Placeholdery.java:108-114`).

## Koszt dla serwera
Nie mierzone. Z kodu: brak własnych zadań okresowych; praca przy odczycie placeholderów przez MythicHUD (migawka 250 ms na gracza, `HudDane.java:25`), trzy listenery MONITOR na obrażenia, EXP, podniesienie przedmiotu; `Migawka.zbierzSlugi` filtruje wszystkie aktywne moby MythicMobs lambdą przy każdej nowej migawce (`Migawka.java:66`) — koszt rośnie z liczbą aktywnych mobów, nie mierzony. Mapa `pierwszyRaz` czyszczona po przekroczeniu 512 wpisów (`Migawka.java:77`).

## Dane
Brak zapisu na dysk i do bazy. Stan w RAM po UUID gracza (`HudDane.java:27`, `Zdarzenia.java:134-135`, `Nawigacja.java:34`), czyszczony na wyjściu (`HudDane.java:63`, `Zdarzenia.java:213`); `obrazenia` w Nawigacji nie jest czyszczone na wyjściu (`Nawigacja.java:34`, brak handlera quit) — drobny wyciek, lista podejrzanych. UUID konta vs profil: nie dotyczy zapisu; `PlayerData` MMOCore dotyczy aktywnej postaci.

## Znane pułapki i decyzje
- Zasada: zacznij od logu, nie od layoutu (spam WARN MythicHUD): `~/.claude/skills/mc-systemy-gracza/menu-ui.md:26`; historia wdrożeń HUD v0-v5: `menu-ui.md:25-33`; pozycje HUD nie wiązać z rozdzielczością: `menu-ui.md:34`; odnowienia = umiejętności klasowe MMOCore, nie Zaklęcia: `menu-ui.md:35`.
- Strażnik placeholdera w MythicHUD: `~/.claude/skills/mc-mythicmobs/mythichud-i-bronie.md:18`.
- Teksty bez tagów MiniMessage, bo MythicHUD liczy szerokość z surowego tekstu (`Placeholdery.java:12, 115`).

## Budowanie i wdrożenie
Build: `./build.sh` (kontener `gradle:8.12-jdk21`, `--memory 2g`, jary world1 tylko do odczytu; `build.sh:7-12`); jary przypięte po nazwie w `build.gradle.kts:10-21`. Jar w `build/libs/`. Testów jednostkowych brak (nie ma katalogu `src/test`). Wdrożenie: wg wpisów HUD w `menu-ui.md:25-33` (paczki `tools/hud/*`, `wdroz.sh`); w tym katalogu skryptu wdrożenia brak.
