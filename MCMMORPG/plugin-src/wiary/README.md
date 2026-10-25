# Wiary (`plugin-src/wiary`)
Wersja w źródle: `${version}` z `src/main/resources/plugin.yml:2`, składana z `gradle.properties:2` (`0.1.0`) + rewizja z build.sh (`build.gradle.kts:33`). Jar na world1: `/opt/docker/mc/data/world1/plugins/wiary-0.1.0-202609301200.jar`. Zgodność źródła z jarem: zgodna co do `main` i listy klas (jar: 11 wpisów `class`, w źródle 6 plików .java + klasy zagnieżdżone; nie porównywano bajtkodu), `config.yml` na world1 identyczny ze źródłem (diff = brak różnic).
Opis stanu z dnia: 2026-10-25, commit 8c79ff8.

## Po co (co widzi gracz)
Gracz wybiera w menu Q (Postać, slot 14, menu DeluxeMenus `wiara`) patrona: jednego z 3 Starszych (wspólni dla wszystkich ras) albo jednego z 3 bóstw swojej rasy. Modlitwa daje "łaskę" na 30 minut: tymczasowe bonusy do statystyk, skalowane atrybutem Wiara. Patrona można zmienić raz na 24 h (zdejmuje łaskę). Wiara jest per postać (profil), nie per konto.

## Komendy i uprawnienia
| komenda | kto | co robi | źródło |
|---|---|---|---|
| `/wiary wybierz <gracz> <bóstwo>` | konsola (menu DM `[console]`) lub OP | ustawia patrona | `Komenda.java:47-53`, menu `.../DeluxeMenus/gui_menus/wiara.yml:46` |
| `/wiary modlitwa <gracz>` | j.w. | nakłada łaskę na 30 min | `Komenda.java:54` |
| `/wiary info <gracz>` | j.w. | wypisuje patrona, łaskę, staty | `Komenda.java:56-59, 71-86` |
| `/wiary reset <gracz>` | j.w. | kasuje patrona i łaskę | `Komenda.java:55` |
| `/wiary reload` | j.w. | przeładowuje config i panteon, zwraca liczbę problemów | `Komenda.java:31-35` |
Jedyna komenda w plugin.yml to `wiary`, uprawnienie `wiary.admin` (default op): `src/main/resources/plugin.yml:9-14`. Gracz nie wpisuje komendy sam (`Komenda.java:15-16`).
Placeholdery PAPI `%wiary_...%`: rasa, rasa_nazwa, patron, patron_imie, wiara, laska, laska_czas, modlitwa, zmiana, opis_patrona, dostepny_/wybrany_/opis_/domena_/status_`<id>` (`Placeholdery.java:8-11, 33-70`). Zwracają "" dla offline.

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/Wiary/config.yml` (identyczny ze źródłem `src/main/resources/config.yml`). Klucze najwyższego poziomu:
- `laska` — `czas-minut` (dziś 30, domyślna w kodzie 30), `wiara-limit` (40, łaska liczy min(wiara, limit)), `atrybut` (`wiara`, domyślna `wiara`): `WiaryPlugin.java:90, 92`; limit i efekty w `Panteon.java`.
- `zmiana-patrona.odnowienie-godzin` — 24 (domyślna w kodzie 24): `WiaryPlugin.java:91`.
- `rasy` — mapa id rasy na nazwę wyświetlaną (czlowiek, elf, ork, nieumarly); rasa gracza = folder klasy w `plugins/MMOCore/classes/<rasa>/` (`WiaryPlugin.java:86-113`).
- `bostwa` — 3 Starszych (ordhun, nysara, vharun) + po 3 bóstwa na rasę (solenna/valdris/heldra; aelyra/sylvaen/iridel; gorrath/kharzuk/mogra; morvhena/vhorax/ossyra). Efekty = staty MythicLib FLAT/RELATIVE, wartość = baza + na-punkt * min(wiara, limit) (`Panteon.java`).
Reload ostrzega, gdy rasa nie ma 6 bóstw do wyboru (`WiaryPlugin.java:86-113`). Wszystkie 4 klucze są czytane w kodzie. Pełny opis panteonu: `PANTEON.md`.

## Jak działa
- Start: `saveDefaultConfig`, rejestracja modułu `ProfilModul` w MMOProfiles, listenery, komenda, ekspansja PAPI (`WiaryPlugin.java:60-69`).
- `ProfilModul` (zdarzenia MMOProfiles): Create/Select/Unload/Remove; każde kończy się `validate(this)`, inaczej MMOProfiles czeka (`ProfilModul.java:13-14, 29-63`). Select wczytuje stan i nakłada łaskę, Unload zdejmuje, Remove archiwizuje plik do `profile/usuniete/` (`ProfilModul.java:37, 48, 59`).
- Łaska to `TemporaryStatModifier` MythicLib o kluczu `wiary_laska` (`WiaryPlugin.java:40`), nakładany z opóźnieniem 20 ticków po wejściu (`WiaryPlugin.java:165-177, 226-239`). Zdejmowanie: `zdejmijMody` `WiaryPlugin.java:189-223` (komentarz o pułapce podwójnego `close()`).
- `wybierz` (`WiaryPlugin.java:246-270`): sprawdza odnowienie, zdejmuje łaskę. `modlitwa` (`:272-292`): wiara liczona w chwili modlitwy, nowa dopiero po wygaśnięciu. `reset` (`:295-305`).
- Profil-duch (id profilu równe UUID gracza) nie dostaje wiary (`WiaryPlugin.java:121, 167`).
- Integracje: MythicLib/MMOCore/MMOProfiles (depend), PlaceholderAPI (softdepend), DeluxeMenus (menu wołają komendy z konsoli). Arena czyta `%wiary_patron%` (`plugin-src/arena` MenuGracza.java:194).
Zadań okresowych brak, poza jednorazowym `runTaskLater` (`WiaryPlugin.java:174`); wygasanie łaski liczone znacznikiem czasu (`laska.do`), nie tickiem — nie sprawdzone, czy coś zdejmuje modyfikator w chwili wygaśnięcia przy graczu online.

## Koszt dla serwera
Nie mierzone. Z kodu: brak tykających zadań, praca na zdarzeniach profilu i na żądanie (menu/PAPI); małe pliki czytane na wątku głównym (`ProfilModul.java:15`).

## Dane
`plugins/Wiary/profile/<uuid-profilu>.yml` (pola: gracz, patron, zmiana, laska.do, laska.wiara, laska.bostwo), zapis tmp+rename (`Magazyn.java`). Klucz = UUID PROFILU, więc dane są per postać, a nie per UUID konta (MMOProfiles daje wszystkim postaciom jeden UUID gracza, profil ma własny). Usunięte postacie: `profile/usuniete/` (na world1 dziś 2 pliki).

## Znane pułapki i decyzje
- Wdrożone 30.09 jako 0.1.0-202609301200; lekcje: DM przy równym priority sprawdza jeden przedmiot; nie wołać `close()` TemporaryStatModifier przed `unregister`; generator `menu/generuj_menu.py` STARY: `~/.claude/skills/mc-systemy-gracza/stan.md:22-23`.
- Surowe `%wiary_*%` na klonie bez `--z-kreatorem` (brak MMOProfiles): `~/.claude/skills/mc-gui-pack-dialogi/SKILL.md:10`.
- Strażnik placeholdera w MythicHUD: `~/.claude/skills/mc-mythicmobs/mythichud-i-bronie.md:18`.
- Podejrzane: `info` nie sprawdza pozycji `profil=brak` poza wypisem; `reset` i `wybierz` wymagają gracza online (`Komenda.java:40-44`).

## Budowanie i wdrożenie
Build: `build.sh` (Gradle w kontenerze, `build.gradle.kts:10-42`, jary MMOProfiles/MythicLib/MMOCore/PlaceholderAPI przypięte po nazwie z `/serverplugins`). Test: `src/test/java/pl/mcmmorpg/wiary/PanteonTest.java` (7 testów @Test). Wdrożenie: `menu/wdroz-world1.sh` (kopia, stop przy 0 graczy, `instaluj.py`, start, kontrola logu; cofnięcie `backups/wiary-<czas>/cofnij.sh`; `menu/wdroz-world1.sh:1-4`). Eval na klonie: `eval/run-klon.sh`.
