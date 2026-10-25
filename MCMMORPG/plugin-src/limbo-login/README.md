# LimboLogin (`plugin-src/limbo-login`)
Wersja w źródle: `0.1.0-<rewizja git>` (`gradle.properties:2`, `build.gradle.kts:29`, plugin.yml:2 = `${version}`). Jar: NIE na world1; stoi na serwerze limbo: `/opt/docker/mc/data/limbo/plugins/limbo-login-0.1.0-d5a7bdd.dirty.jar`. Zgodność źródła z jarem: zgodna co do plugin.yml (name, main, api-version, description identyczne; wersja `0.1.0-d5a7bdd.dirty` = build z niezacommitowanych źródeł, więc skrót d5a7bdd nie wskazuje dokładnie źródeł); bajtkodu nie porównywałem.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (worktree docs-pluginy).

## Po co (co widzi gracz)
Zamiast wpisywać hasło na czacie (`/login <hasło>`), gracz wchodzący na serwer `limbo` dostaje po 1 s natywne okno klienta (Paper Dialog) "Logowanie" z polem "Haslo" i przyciskami "Zaloguj" i "Utworz konto" (`src/main/java/pl/mcmmorpg/limbologin/LimboLoginPlugin.java:62, 73-81, 92-129`). Okno nie zamyka się Escape i zostaje po kliknięciu (linie 110-117). STAN PRODUKCYJNY: logowanie LibreLogin jest od 2026-09-20 wyłączone, kontener limbo zatrzymany; plugin jest gotowy, ale nieużywany (`~/.claude/skills/mc-host-testy-wdrozenia/serwer-stan.md:9`, `~/.claude/skills/mc-gui-pack-dialogi/historia.md:78`).

## Komendy i uprawnienia
Brak własnych komend i uprawnień (`src/main/resources/plugin.yml:1-5`). Przyciski wysyłają z KLIENTA komendy LibreLogin (poniżej).

| Przycisk | Komenda wysyłana przez klienta | Źródło |
|---|---|---|
| Zaloguj | `/login $(haslo)` | LimboLoginPlugin.java:93-97 |
| Utworz konto | `/register $(haslo) $(haslo)` (hasło dwa razy, tak wymaga składnia LibreLogin) | LimboLoginPlugin.java:102-106 |

## Konfiguracja
Brak pliku configu (nie ma `saveDefaultConfig`; na limbo w `plugins/` są tylko jar, `bStats`, `spark`). Stałe w kodzie: opóźnienie okna `SHOW_DELAY_TICKS = 20` (linia 62), nazwa pola `haslo` (65), `maxLength(64)` pola (123), szerokość pola 200, przycisków 120, 2 kolumny (linie 95, 104, 124, 128), tekst "minimum 6 znakow" (103, 120; to tekst pomocniczy, plugin nie waliduje).

## Jak działa
- Listener: `PlayerJoinEvent` (domyślny priorytet), `runTaskLater` 20 ticków; pokazuje okno, jeśli gracz nadal online (linie 73-81).
- Dlaczego komenda z klienta, a nie API: LibreLogin działa na PROXY (Velocity) i widzi tylko komendy wysłane przez klienta; `performCommand`/`chat` z backendu nie dochodzą (javadoc, linie 34-40). Dlatego `DialogAction.commandTemplate`.
- Dlaczego `pause(false)`: Paper odrzuca dialog, który pauzuje grę i zostaje otwarty po akcji (linie 111-116).
- Ograniczenie: pole jest zwykłym tekstem (Paper nie ma typu "hasło"), hasło jest widoczne na ekranie gracza, ale nie trafia na czat (linie 42-46).
- Integracje: tylko pośrednio LibreLogin (proxy). Plugin nie zna jego API ani bazy; dlatego dwa przyciski zamiast jednego (linie 48-51). Zależności pluginów brak (plugin.yml nie ma `depend`).

## Koszt dla serwera
Jednorazowe zadanie 20 ticków na każde wejście gracza, brak zadań okresowych, brak zapisu. Nie mierzone (limbo jest puste).

## Dane
Nie zapisuje nic; hasła obsługuje wyłącznie LibreLogin na proxy (baza SQLite obok proxy, `serwer-stan.md:11`). Brak odwołań po UUID.

## Znane pułapki i decyzje
- Komendy dla pluginu na proxy tylko z klienta, `pause(false)`, okno nie do zamknięcia: `~/.claude/skills/mc-gui-pack-dialogi/SKILL.md:61`.
- Historia i weryfikacja klientem QA: `~/.claude/skills/mc-gui-pack-dialogi/historia.md:64`; status "gotowe, nieużywane": `historia.md:78`, `SKILL.md:74`.
- Stan logowania i pułapka LibreLogin 0.24 + Velocity 4 (`use-titles`): `~/.claude/skills/mc-host-testy-wdrozenia/serwer-stan.md:9,13`.
- Brak obsługi błędnych haseł po stronie pluginu: komunikat o błędzie pokazuje LibreLogin; okno zostaje otwarte (linie 86-91).

## Budowanie i wdrożenie
`./build.sh`: kontener `gradle:8.12-jdk21`, `--memory 2g`; rewizja gita liczona na hoście i wstrzykiwana jako `-PllGitRev`, `.dirty` gdy są niezacommitowane zmiany (build.sh:9-17; wspólny gradle-home z character-creator: build.sh:21; paper-api: build.gradle.kts:26). Zależność: tylko `paper-api:1.21.11-R0.1-SNAPSHOT` (`build.gradle.kts:25`), Java 21. Gradle home współdzielony z `../character-creator/.gradle-home` (build.sh, ostatnie linie). Wynik: `build/libs/limbo-login-<wersja>.jar` do `data/limbo/plugins/`. Testy: brak.
