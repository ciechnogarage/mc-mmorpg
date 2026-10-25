# Chowance (`plugin-src/chowance`)
Wersja w źródle: `0.1.0` (`gradle.properties` `chwVersion`; plugin.yml `version: ${version}`, rewizja `chwRev` = data builda). Jar na world1: `plugins/chowance-0.1.0-202610242306.jar` (zawiera zmianę 4a0c6fb: w jarze są `komendyPoWalce` w `Trener.class` i `LadowaczTresci.class`). Zgodność źródła z jarem: zgodna (plugin.yml w jarze = źródło poza podstawieniem wersji; pełnego diffu bajtkodu nie robiono).
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (worktree docs-pluginy). Wiedza zespołu (daty wdrożeń, md5 jarów): `~/.claude/skills/mc-systemy-gracza/chowance.md`.

## Po co (co widzi gracz)
System chowańców (pety w stylu Pokemon): gracz łapie dzikie stwory pieczęcią, ma drużynę do 6 i magazyn, walczy turowo (dzikie, trenerzy, Wieża, Mistrzowie z odznakami) lub w czasie rzeczywistym z innymi graczami (pojedynki z ELO, turnieje), hoduje jaja, handluje, ewoluuje i uczy ruchów. Walka toczy się na arenie wybranej z terenu, z kamerą-siedziskiem i HUD (MythicHUD lub BossBar). Mistrzowie dają odznaki; progi poziomu zależą od odznak.
Wyścigi NIE są tutaj (brak trafień na "wyscig" w kodzie): to osobny plugin `plugin-src/wyscigi`. Tu jest tylko config `przywolanie.swiaty_zabronione` zawierający `wyscigi`.

## Komendy i uprawnienia
plugin.yml: jedna komenda `chw` (aliasy `chowance`, `chowaniec`, permission `chowance.gracz`); uprawnienia `chowance.gracz` (default true) i `chowance.admin` (default op). Podkomendy w switchu `Komendy.java:73-110`; konsola tylko dla admina, komendy gracza wymagają Player. Admin: sprawdzenie `chowance.admin` w `Komendy.java:546`.

| Komenda (`/chw ...`) | Kto | Co robi | Źródło |
|---|---|---|---|
| pomoc / help | gracz | lista komend (pomija pojedynek-stan, pojedynek-npc, npc) | Komendy.java:73 |
| menu | gracz | konsola: `dm open <menu.deluxemenus> <gracz>` | Komendy.java:74 |
| starter | gracz | wskaż zagrodę/starter | Komendy.java:75 |
| druzyna, info/karta, gracze | gracz | okno drużyny, karta chowańca, lista graczy | Komendy.java:76 |
| przywolaj, odwolaj, zamien | gracz | przywołanie/odwołanie aktywnego, zamiana miejsc | Komendy.java:73-110 |
| magazyn (strona/wloz/wyjmij) | gracz | magazyn | Komendy.java:261-290 |
| nazwij, ewoluuj, naucz, uwolnij | gracz | uwolnienie wymaga `potwierdz`, nie wolno uwolnić ostatniego | Komendy.java:73-110 |
| walka [gracz] | gracz | dziki cel w 16 blokach albo zaproszenie PvP | Komendy.java:73-110 |
| pojedynek, handel, akceptuj, odrzuc, poddaj, ruch 1-4 | gracz | pojedynek RT, handel, ruch w walce | Komendy.java:73-110 |
| okno, zamknij, bestiariusz, ranking | gracz | okna Dialog, bestiariusz, ranking ELO | Komendy.java:73-110 |
| hodowla (wloz/wybierz/wyjmij) | gracz | hodowla jaj | Komendy.java:434-472 |
| turniej zapisz/wypisz | gracz | turniej | Komendy.java:73-110 |
| admin: przeladuj, zapisz, diag, info, lecz, status, sprzatnij | OP/konsola | przeładowanie contentu/configu, zapis danych, diagnostyka | Komendy.java:546 |
| admin: daj `<gracz> <GATUNEK> [poz] [lsniacy]`, exp, odznaka `<gracz> <ID> [zabierz]`, jajo | OP/konsola | nadawanie | Komendy.java:546 |
| admin: walka-npc, pojedynek-stan, pojedynek-npc, wieza, turniej start/stop/stan | OP/konsola | testy walk, Wieża, turniej | Komendy.java:546 |
| admin: npc (tutaj/usun/`<swiat x y z yaw>`), spawn, spawnw, ranking usun, sezon nowy | OP/konsola | NPC trenerów, spawn dzikich, ranking | Komendy.java:546 |

Tab-complete GRACZ (`Komendy.java:711`) pomija pomoc i zamknij; lista ADMIN (`:713`) ma 20 wpisów.

## Konfiguracja
Pliki na world1: `/opt/docker/mc/data/world1/plugins/Chowance/` (`config.yml`, `typy.yml`, `charaktery.yml`, `startery.yml`, `przedmioty.yml`, `wieza.yml`, `zdarzenia.yml`, `bestiariusz-nagrody.yml`, katalogi `gatunki/`, `ruchy/`, `cechy/`, `trenerzy/`). Szablony kopiowane z jaru tylko gdy pliku brak (`ChowancePlugin.java:205`).
**`config.yml` na world1 jest starszy niż jar** (mtime 20.10). Brak tam kluczy: `towarzysz`, `pierwsze_kroki`, `wybieg`, `testowe_gatunki` oraz `walka.kamera|arena_szukaj|arena_zasieg|widok_odslaniaj|widok_bez_reki|hud_mythichud|odtwarzanie_ticki`, `pojedynek.auto|bez_ai_mm|mnoznik_obrazen|odstep_ruchow_ms|wstep_ms`, `poziomy.exp_dzielony` (domyślnie 0.5). Działają wtedy domyślne z kodu (`Ustawienia.java`).
Klucze najwyższego poziomu na world1 (22), wszystkie czytane w `Ustawienia.java`: bestiariusz (176-177), debug (203), druzyna (114-117: `limit_druzyny` 0:3,10:4,20:5,30:6; `limit_magazynu` 0:30...40:1000), handel (174-175), hodowla (136-140; czas_min 30, jaja_max 3, probkowanie_s 5), karmienie, lapanie (96-104), legendarne (110-111), lsniace, menu (201-202), oswajanie, pojedynek (153-165), poziomy (127-133), profesja (195-200, "poskramiacz"), przywolanie (118-123), ranking (169-171; elo_start 1000, k 32), regeneracja (124-125), starter (178), statystyki, turniej (172-173; min_graczy 4, czas_na_mecz_s 600), walka (141-152), zagroda (179-187; świat `stolica_odnowa`). Klucze gatunkowe (`lapanie.wymagana_pieczec`, hodowla, oswajanie): `LadowaczTresci.java:374-407`. karmienie/lsniace/statystyki/oswajanie: czytane w kodzie, numery linii nie sprawdzone.
Wartości world1 różne od `src/main/resources/config.yml`: `regeneracja.procent_na_minute` 20.0 (źródło 5.0); `poziomy.exp_pve_mnoznik` 2.0 (1.0); `przywolanie.swiaty_zabronione` [creator_limbo, wyscigi, licho] ([creator_limbo]); `lapanie.szal_sekundy` 0 (kod 10). Domyślne w kodzie różne od pliku źródłowego: `walka.odtwarzanie_ticki` 24 (plik 30), `pojedynek.odstep_ruchow_ms` 2400 (plik 2000). `pierwsze_kroki.nagrody.krokN` czytane dynamicznie: `Ustawienia.java:240`. `testowe_gatunki: true` potrzebne na klonach (skill chowance.md:9).
Treść na world1 (policzone regexem po id): gatunki 24 (`1_startowe` 9, `2_dzikie` 12, `3_legendarne_sezonowe` 3), ruchy 49, cechy 30, trenerzy: mistrzowie 4 (odznaki ZARU, FALI, KORONY + MISTRZ_CZARNOBOR), ogrod 1, probny 1, wieza 10; zdarzenia 9 (ZLAPANIE ... FALA_WIEZY). Klucze trenera: nazwa, rodzaj (MISTRZ|TRENER), odznaka, wymagane_odznaki, druzyna (1-6), nagroda_komendy, komendy_po_walce (`LadowaczTresci.java` ok. 505-570). Limit poziomu = `limitBazowy + zaOdznake*odznaki` do `poziomMax` (`Ustawienia.java:258`).

## Jak działa
Start: `ChowancePlugin.java:81-203` (config, zasoby, `LadowaczTresci.wczytaj()` z walidacją, `MagazynDanych`, moduł MMOProfiles `:168`, komenda `:170`, PAPI `:174-177`, most ModelEngine tylko gdy plugin włączony `:159`). Przeładowanie: `przeladuj` `:223`.
Podsystemy (pakiet `gra/`): Przywolania (aktywny chowaniec/wierzchowiec, MONITOR/HIGH/LOWEST), Dzikie (spawn przez `MythicMobSpawnEvent` MONITOR `Dzikie.java:92`, blokada lootu HIGHEST), Lapanie (pieczęć, HIGH/LOWEST), Zagroda i Hodowla (jaja), Towarzysz, PierwszeKroki (4 zadania), Bestiariusz, Handel (atomowa wymiana), Startery (oferta wg rasy/klasy).
Walka turowa: `WalkiTurowe.java` (rodzaje PVP, DZIKI, TRENER, WIEZA, TURNIEJ: `:51`; `startPvp` 159, `startDziki` 175, `startTrener` 195, wymóg odznak 200; HIGH `PlayerItemHeldEvent` 550; LOW quit/śmierć 1144-1148), silnik `logika/SilnikWalki`. Arena z terenu (`Arena`), Kamera-siedzisko (gracz zostaje w swoim trybie, by klawisze 1-8 szły na serwer), Widok (fałszywe bloki po stronie klienta), Okna (Paper Dialog API, przyciski `chowance:klik` z payloadem `{o,b}`, obsługa `PlayerCustomClickEvent`; callback Papera nie działał: PROBLEMY A-007), HudWalki (`walka.hud_mythichud`, domyślnie true; inaczej BossBar).
Pojedynki: czas rzeczywisty, ruchy z hotbaru 1-4 lub `/chw ruch`, ELO (`logika/Elo`), Ranking per profil + sezony, Turnieje (stan w pamięci, restart przerywa turniej).
Mistrzowie/odznaki: `ProfilDane.odznaki`; od 4a0c6fb `nagroda_komendy` MISTRZA wykonuje się tylko przy pierwszym zdobyciu odznaki (`d.odznaki.add()` true), przy powtórce komunikat "nagroda mistrza jest jednorazowa"; `komendy_po_walce` biegną po każdej wygranej (`WalkiTurowe.java` ok. 1040-1051; world1 `trenerzy/mistrzowie.yml:65` używa `lochy sygnal %gracz% sala_mistrzow_koniec 100`).
Zadania okresowe (`ChowancePlugin.java:182-196`): tik co 20 (przywołania, walki, pojedynki, turnieje, `:275`); `pojedynki.kamery` co 4 (186); `kamera.podtrzymaj` co 40 (187); `trenerNpc.pokazPoWalce` co 40 (191); `zagroda.pilnuj` co 100 (192); `zagroda.patrz` co 5 (193); `dzikie.skan` co `skan_s*20` (194); `hodowla.probkuj` co `probkowanie_s*20` (195); `minuta` co 1200 (196; jaja + `dane.zapiszBrudne`, `:283`).
Integracje: Mythic (`integracja/MythicRejestracja.java`, mechaniki SYNC_ONLY): mechaniki chwobrazenia, chwruchauto, chwruch, chwstatus, chwlap, chwwalkatrener, chwleczdruzyne (94-118); warunki chwchowaniec, chwoswojony, chwdziki, chwlsniacy, chwgatunek, chwtyp, chwmasiodlo, chwwwalce, chwstatus, chwmaaktywnego (371-390); targetery chwaktywny, chwwlasciciel, chwprzeciwnik (410-412). PAPI `%chowance_...%` (`Placeholdery.java:50`): walka_*, kroki_*, druzyna_N_*, magazyn_*, bestiariusz_*, odznaki_liczba, limit_poziomu, ranking_elo/pozycja/top_*, oswajanie_postep, hodowla_*, starter_wybrany, w_walce, aktywny_*. MMOCore: profesja "poskramiacz" (`integracja/MmoCoreHook.java`; bez MMOCore poziom 0 i jedno ostrzeżenie). MythicDungeons: `integracja/Lochy`. ModelEngine: `MostModelEngine` (HIGH `BaseEntityInteractEvent`). Zdarzenia API: `api/event/` (WalkaZakonczona, ChowaniecZlapany, ChowaniecPrzywolany, ChowaniecEwolucja).

## Koszt dla serwera
Nie mierzone. Z kodu: stałe zadania wg listy wyżej (najgęstsze: `zagroda.patrz` co 5 ticków i `pojedynki.kamery` co 4, nawet bez graczy w walce: czy wychodzą wcześnie, nie sprawdzone). Zapis na żądanie na jednym wątku `Chowance-IO`, serializacja JSON na głównym, zapis atomowy tmp+move (`dane/MagazynDanych.java`), autozapis co minutę tylko brudnych. Cache tylko online'owych profili.

## Dane
`plugins/Chowance/dane/profile/<UUID PROFILU MMOProfiles>.json` (302 plików na world1). Klucz = UUID PROFILU (postaci), nie gracza, więc kilka postaci jednego gracza ma osobne dane. Profile-duchy (id == UUID gracza) nie dostają danych (javadoc `ProfilModul`). `dane/archiwum/` (`*.uszkodzony-*`, `*.usuniety-*`), `dane/ranking.json` (nick/elo/w/l po UUID profilu). Wg javadoc także `sezon.txt`, `handel.log`, `turnieje.log` (na dysku world1 nie ma ich; nie sprawdzone dlaczego). Dzikie stwory: dane w PDC ciała, ciała nietrwałe. Pola `ProfilDane`: druzyna, magazyn, bestiariusz, lsniace, nagrody, odznaki, starter, pierwszeKroki, elo, sezon, wygrane/przegrane, wieza, hodowla, jaja.

## Znane pułapki i decyzje
- Oś czasu wdrożeń, jar md5, `testowe_gatunki` na klonach: `~/.claude/skills/mc-systemy-gracza/chowance.md:7-24` (zagroda 22, nagroda mistrza 23).
- Mechaniki własne w MythicMobs (SYNC_ONLY, ciche błędy): `~/.claude/skills/mc-mythicmobs/SKILL.md`.
- Kamera/interakcje: `~/.claude/skills/mc-gui-pack-dialogi/swiat-kamera-wynn.md`.
- Komentarz `ChowancePlugin.java:182-183` mówi "Nic co tick", a zadania co 4 i 5 ticków istnieją (186, 193).
- Help admina pomija pojedynek-stan, pojedynek-npc, npc.

## Budowanie i wdrożenie
`build.sh` (kontener gradle 8.12-jdk21, `-PchwRev=$(date +%Y%m%d%H%M)`, jary przypięte po nazwie w `build.gradle.kts:14-23`: MythicMobs 5.11.2, MMOProfiles 1.2, MythicLib 1.7.1, MMOCore 1.13.1, PAPI 2.12.2, MythicDungeons 2.0.1, ModelEngine 4.0.9; `chwServerPlugins=/serverplugins`). Testy: `src/test/java/.../` 12 plików, JUnit 5.11.4 (`build.gradle.kts`). Wdrożenie: `wdrozenie/{wdroz.sh,cofnij.sh,latka_configu.py,latka_menazerii.py}`; ostatnie: `/opt/docker/mc/backups/nagroda-mistrza-20261024/`.
