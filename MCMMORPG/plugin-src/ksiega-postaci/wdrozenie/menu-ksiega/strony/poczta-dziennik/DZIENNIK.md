# Dziennik przygód — propozycje pozycji (10)

Każdy placeholder poniżej występuje już w konfiguracji na tym serwerze. Wystąpienie w pliku dowodzi użycia identyfikatora, ale wynik dla konkretnego gracza trzeba sprawdzić na działającym serwerze. Ścieżki źródeł są względem `/opt/docker/mc/data/world1/plugins/`.

| # | Pozycja | Placeholder(y) | Lokalny dowód |
|---|---|---|---|
| 1 | Klasa | `%mmocore_class%` | `DeluxeMenus/gui_menus/hub_glowne.yml:56` |
| 2 | Poziom | `%mmocore_level%` | `DeluxeMenus/gui_menus/hub_glowne.yml:57` |
| 3 | EXP do poziomu | `%mmocore_experience%`, `%mmocore_next_level%` | `KsiegaPostaci/config.yml:81` |
| 4 | Aktywne zadanie | `%mmocore_quest%` | `KsiegaPostaci/config.yml:55-56` |
| 5 | Postęp zadania | `%mmocore_quest_progress%` | `KsiegaPostaci/config.yml:55` |
| 6 | Aktywny chowaniec | `%chowance_aktywny_nazwa%`, `%chowance_aktywny_poziom%` | `DeluxeMenus/gui_menus/chowance_menazeria.yml:289` |
| 7 | Bestiariusz | `%chowance_bestiariusz_widziane%`, `%chowance_bestiariusz_zlapane%`, `%chowance_bestiariusz_wszystkie%`, `%chowance_bestiariusz_procent%` | `DeluxeMenus/gui_menus/chowance_bestiariusz.yml:1384-1386` |
| 8 | Odznaki mistrzów | `%chowance_odznaki_liczba%` | `DeluxeMenus/gui_menus/chowance_lochy.yml:40` |
| 9 | Rekord wieży | `%chowance_wieza_WIEZA_CHOWANCOW_rekord%` | `DeluxeMenus/gui_menus/chowance_lochy.yml:54` |
| 10 | Wiara i patron | `%wiary_patron_imie%`, `%wiary_wiara%`, `%wiary_laska%`, `%wiary_laska_czas%` | `DeluxeMenus/gui_menus/wiara.yml:16-20` |

Zadania mają istniejące wejście menu `[player] quests` (`DeluxeMenus/gui_menus/hub_glowne.yml:32`), bez ręcznego wpisywania komendy. Nie ma tu oceny lochów per gracz: w `chowance_lochy.yml` są odznaki i rekord wieży, ale nie ma placeholdera takiej oceny. Przed użyciem sprawdzić w grze, czy `mmocore_quest*` zwracają wartości lub pusty stan, ponieważ znaleziono je w `KsiegaPostaci/config.yml`, nie w menu DeluxeMenus.
