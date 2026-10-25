# LochyPoprawki (`plugin-src/lochy-poprawki`)
Wersja w źródle: `${version}` (`src/main/resources/plugin.yml:2`) = `pmVersion=0.1.0` (`gradle.properties:1`) + rewizja `pmRev` z build.sh (`build.gradle.kts:26`, `build.sh:7`). Jar na world1: `/opt/docker/mc/data/world1/plugins/lochy-poprawki-0.1.0-202610242333.jar` (plugin.yml w jarze: `version: '0.1.0-202610242333'`, ten sam `main`, uprawnienie `lochypoprawki.sygnal` obecne). Zgodność źródła z jarem: zgodna co do wersji, main i uprawnienia; zawartość klas nie porównywana. Skill podaje wdrożenie z `main` 2c0c3e8 (zamiatacz 8da6c66): `~/.claude/skills/mc-swiat-lochy/SKILL.md:68`.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (zawiera 8da6c66 zamiatacz instancji i d5d5d87 komenda `lochy sygnal`).

## Po co (co widzi gracz)
Łata na błędy MythicDungeons 2.0.1-SNAPSHOT (cudzy plugin, bez zmiany jego jara). Gracz solo (bez party) rozłączony w lochu nie trzymał już instancji do restartu: po czasie MD wyrzuca go i zamyka pustą instancję. Dodatkowo: zamiatacz zamyka instancje osierocone w innych sytuacjach, a komenda `lochy sygnal` pozwala walkom Chowańców zakończyć loch po wygranej (Czarnobór, wieża fal). Gracz nie widzi samego pluginu.

## Komendy i uprawnienia
| komenda | kto | co robi | źródło |
|---|---|---|---|
| `/lochy sygnal <gracz> <nazwa> [ticki]` | konsola/OP, uprawnienie `lochypoprawki.sygnal` (default op) | po opóźnieniu wysyła do instancji gracza `RemoteTriggerEvent` o nazwie `<nazwa>` (odbiera ją trigger `TriggerRemote` w functions.yml mapy); gracz offline, poza instancją lub mapa bez odbiornika = nic | `LochyPoprawki.java:102-118, 133-142`, `plugin.yml:6-12` |
| `/lochy stan` | j.w. | jedna linia: liczba instancji i gracze (online@świat, offline+tracker, offline) | `LochyPoprawki.java:98-101, 193-208` |
Jedyna komenda w plugin.yml to `lochy` (kontrola 1 spełniona). Kod sprawdza uprawnienie `lochypoprawki.sygnal` nadawcy (`LochyPoprawki.java:94-97`, drugie zabezpieczenie obok plugin.yml; kto może wykonać komendę, bez zmian: konsola, RCON, OP) i nie zawęża jej do samej konsoli. Błędne argumenty = komunikat użycia (`LochyPoprawki.java:102-105`). Wywołują ją Chowańce z konsoli: `/opt/docker/mc/data/world1/plugins/Chowance/zdarzenia.yml:26`, `.../Chowance/trenerzy/mistrzowie.yml:66`.

## Konfiguracja
Brak pliku configu: na world1 nie ma katalogu `plugins/LochyPoprawki` (listing `plugins/`), a kod nie woła `saveDefaultConfig` ani `getConfig` własnego pluginu (`LochyPoprawki.java:76-89`). Stałe w kodzie: zamiatanie co 30 s (`:69`), kick po `General.KickOfflinePlayersDelay` z configu KAŻDEJ mapy MD (domyślnie 300 s w kodzie `:250`), `General.KickOfflinePlayers` (domyślnie true, `:172, 245`). Czas i flagi ustawia się więc w configu mapy MythicDungeons, nie tu.

## Jak działa
- Błąd MD i naprawa (opis w komentarzu): przy `PlayerQuitEvent` MD usuwa obiekt `MythicPlayer` gracza solo przed dyspozytorem instancji, który tworzy nowy obiekt i nie planuje kicka (`LochyPoprawki.java:31-43`). Poprawka: `zapamietaj` na LOWEST zapamiętuje obiekt (`:144-150`), `napraw` na MONITOR przywraca go do `PlayerManager.players` refleksją i wywołuje `onPlayerDisconnectDungeon` instancji (`:152-180`).
- Zamiatacz co 30 s (`:232-279`): (a) offline bez trackera dłużej niż kick-delay: `removePlayer` (`:244-254`); (b) online w innym świecie przez 2 przejścia z rzędu: zwolnienie wpisu MD bez teleportu (`:255-269`); (c) pusta instancja: `dispose()` (`:272-275`).
- Co 1 s: pamięta aktywne instancje (słabe referencje) i jeśli instancja zniknęła z `activeInstances` bez zamknięcia (timeout wczytania 5 s), a nie ma graczy, woła `dispose()` (`:210-230`, `runTaskTimer` 20 ticków `:88`). `WorldInitEvent` dopisuje instancje do zbioru już w trakcie tworzenia świata (`:182-190`).
- Odporność: brak pola `PlayerManager.players` (inna wersja MD) = plugin sam się wyłącza z komunikatem (`:77-86`). Zależność: tylko MythicDungeons (`plugin.yml:5`), jar przypięty `MythicDungeons-2.0.1-SNAPSHOT.jar` (`build.gradle.kts:14-16`).

## Koszt dla serwera
Nie mierzone. Z kodu: zadanie co 1 s (porównanie kilku instancji, pętla po `znane`) i co 30 s (pętla po instancjach i ich graczach, bez I/O); listenery tylko na PlayerQuit i WorldInit; sygnał to jedno zdarzenie na żądanie. Pamięć: mapy `offlineOd`/`pozaSwiatem` czyszczone `retainAll(widziane)` (`:277-278`), `znane` na słabych kluczach (`:71`).

## Dane
Brak zapisu na dysk i w bazie, tylko RAM (mapy w `LochyPoprawki.java:62-73`). Klucze po UUID gracza/instancji; UUID gracza to UUID konta (MMOProfiles daje je wszystkim postaciom), co tu nie szkodzi, bo dotyczy bieżącej sesji.

## Znane pułapki i decyzje
- Po zmianie wersji MythicDungeons sprawdzić poprawkę testem `test/instancja-mapa.js` na klonie (oczekiwane 9/9), opis błędu z dekompilacji: `~/.claude/skills/mc-swiat-lochy/SKILL.md:47`.
- Decyzja właściciela 22.10: instancja zamyka się po ukończeniu i braku graczy, po rozsądnym czasie (dziś 300 s): `~/.claude/skills/mc-swiat-lochy/SKILL.md:59`.
- Stan wdrożenia i cofnięcie (paczka `/opt/docker/mc/backups/nagroda-mistrza-20261024/`): `SKILL.md:68`.
- Najdłuższe zamknięcie osieroconej instancji po rozłączeniu: 300 s + 2 x 30 s = 360 s (`test/sierota.js:1-5`).
- Poprawione 25.10: kontrola uprawnienia w kodzie (`:94-97`); `wyslijSygnal` loguje WARN (gracz offline / nie w aktywnej instancji), nie częściej niż raz na 30 s dla tej samej pary gracz+sygnał (`ostrzez`, `:121-131`). Do decyzji właściciela: czy zawęzić komendę do samej konsoli (dziś OP-gracz może ją wywołać).

## Budowanie i wdrożenie
Build: `./build.sh` (kontener `gradle:8.12-jdk21`, `--memory 2g`; `build.sh:7-12`), jar w `build/libs/`. Testy (scenariusze botów na klonie, nie jednostkowe): `test/instancja-mapa.js`, `test/sierota.js`, `test/koniec-chowance.js` (uruchomienie: `run.sh <klon --z-kreatorem> <plik>` wg `test/koniec-chowance.js:4`). Wdrożenie: kopia + `cofnij.sh` wg reguł hosta (skrypt wdrożenia w katalogu: brak).
